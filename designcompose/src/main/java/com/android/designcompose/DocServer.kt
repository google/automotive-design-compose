/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.designcompose

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.core.util.Consumer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.designcompose.common.DocumentServerParams
import com.google.common.annotations.VisibleForTesting
import io.grpc.StatusRuntimeException
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.lang.ref.WeakReference
import java.util.Optional
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal const val TAG = "DesignCompose"

internal class LiveDocSubscription(
    val id: String,
    val docId: String,
    val onUpdate: (DocContent?) -> Unit,
)

internal class LiveDocSubscriptions(
    val serverParams: DocumentServerParams,
    val saveFile: File?,
    val subscribers: ArrayList<LiveDocSubscription> = ArrayList(),
)

object DesignSettings {
    internal var liveUpdatesEnabled = false
    private var parentActivity = WeakReference<ComponentActivity>(null)
    internal var liveUpdateSettings: LiveUpdateSettingsRepository? = null
    private var figmaApiKeyFlow: Flow<String?>? = null
    internal var figmaApiKeyStateFlow: StateFlow<String?>? = null
    internal var isDocumentLive: Flow<Boolean>? = null
    private var fontDb: HashMap<String, FontFamily> = HashMap()

    @VisibleForTesting internal var defaultIODispatcher = Dispatchers.IO

    @OptIn(ExperimentalCoroutinesApi::class)
    fun enableLiveUpdates(
        activity: ComponentActivity,
    ) {
        liveUpdatesEnabled = true
        // TODO: Holding onto references to activities isn't great, replace this (make
        // DesignSettings into a ViewModel?)
        parentActivity = WeakReference<ComponentActivity>(activity)

        // Observe the activity's lifecycle to stop/start the periodic document refresh
        activity.lifecycle.addObserver(ActivityLifecycleObserver())

        // LiveUpdateSettings uses androidx.datastore to store the ApiKey. Datastore uses coroutines
        // to store the data, and notifies users of the data via a Kotlin Flows.
        // This sets that all up.
        liveUpdateSettings =
            LiveUpdateSettingsRepository(activity.applicationContext.liveUpdateSettings)
        figmaApiKeyFlow = liveUpdateSettings!!.settingsUpdateFlow
        figmaApiKeyStateFlow =
            figmaApiKeyFlow!!.stateIn(activity.lifecycleScope, SharingStarted.Eagerly, null)

        isDocumentLive =
            figmaApiKeyStateFlow!!.mapLatest { latestKey ->
                latestKey != null && liveUpdatesEnabled
            }

        // Start listening for the setApiKey intent on the main activity
        activity.addOnNewIntentListener(setApiKeyListener)

        DocServer.initializeLiveUpdate()
    }

    // Intent consumer that checks for a new API Key and stores it.
    private val setApiKeyListener =
        Consumer<Intent> { intent ->
            if (intent?.action == ACTION_SET_API_KEY) {
                val activity = parentActivity.get()
                if (activity == null) {
                    Log.e(TAG, "Cannot set API Key, LiveUpdate not fully initialized")
                    return@Consumer
                } else {
                    intent.getStringExtra(EXTRA_SET_API_KEY)?.let { newKey ->
                        // Launch the coroutine to save the new value
                        activity.lifecycleScope.launch(defaultIODispatcher) {
                            // Grab an instance and set the key
                            activity.applicationContext.let {
                                liveUpdateSettings?.setFigmaApiKey(newKey)
                            }
                        }
                    }
                }
            }
        }

    fun addFontFamily(name: String, family: FontFamily) {
        fontDb[name] = family
    }

    fun showMessageInToast(msg: String, duration: Int) {
        Log.i(TAG, "Raising toast: $msg")
        // Toast.makeText dies in AAOS with "display must not be null" on secondary displays.
        // Just log the message till that issue is resolved.
        /*
        val activity = parentActivity.get()
        activity?.runOnUiThread { Toast.makeText(activity, msg, duration).show() }
         */
    }

    internal fun fontFamily(
        fontFamily: Optional<String>,
        fallback: FontFamily = FontFamily.Default,
    ): FontFamily {
        val family = fontFamily.map { fam -> fontDb[fam] }.orElse(null)
        if (family == null) {
            // TODO find a better to show this font error. Currently it spams too much
            // fontFamily.ifPresent { familyName -> Log.w("DesignCompose", "Unable to find font
            // family
            // \"$familyName\"") }
            return fallback
        }
        return family
    }
}

internal class ActivityLifecycleObserver : DefaultLifecycleObserver {
    override fun onResume(owner: LifecycleOwner) {
        Log.d(TAG, "onResume.  Starting live updates.")
        super.onResume(owner)
        DocServer.startLiveUpdates()
    }

    override fun onPause(owner: LifecycleOwner) {
        Log.d(TAG, "onPause.  Stopping live updates.")
        super.onPause(owner)
        DocServer.stopLiveUpdates()
    }
}

// SpanCache maps node names to grid spans, which are needed for lazy grid layouts.
// We use a cache because the lazy grid functions request the span very often while
// scrolling. We clear the cache whenever there is an update from the server.
internal object SpanCache {
    private val nodeSpanHash: HashMap<DesignNodeData, LazyContentSpan> = HashMap()
    internal fun getSpan(nodeData: DesignNodeData): LazyContentSpan? {
        return nodeSpanHash[nodeData]
    }

    internal fun setSpan(nodeData: DesignNodeData, span: LazyContentSpan) {
        nodeSpanHash[nodeData] = span
    }

    internal fun clear() {
        nodeSpanHash.clear()
    }
}

internal object DocServer {
    internal const val FETCH_INTERVAL_MILLIS: Long = 5000L
    internal val documents: HashMap<String, DocContent> = HashMap()
    internal val subscriptions: HashMap<String, LiveDocSubscriptions> = HashMap()
    internal val branchHash: HashMap<String, HashMap<String, String>> =
        HashMap() // doc ID -> { docID -> docName }
    internal val mainHandler = Handler(Looper.getMainLooper())
    internal var periodicFetchRunnable: Runnable? = null
    internal var firstFetch = true
    internal var pauseUpdates = false
}

internal fun DocServer.initializeLiveUpdate() {
    periodicFetchRunnable =
        Runnable() {
            thread {
                try {
                    if (!fetchDocuments(firstFetch)) {
                        Log.e(TAG, "Error occurred while fetching or decoding documents.")
                        return@thread
                    }
                    firstFetch = false
                } catch (e: StatusRuntimeException) {
                    Log.e(TAG, "API error.  $e")
                    DesignSettings.showMessageInToast(
                        "Error accessing the Design Compose API.",
                        Toast.LENGTH_LONG
                    )
                    return@thread
                } finally {
                    // Schedule another run after some time even if there were any
                    // errors.
                    scheduleLiveUpdate()
                }
            }
        }
    // Kickstart periodic updates of documents
    scheduleLiveUpdate()
}

internal fun DocServer.stopLiveUpdates() {
    if (DesignSettings.liveUpdatesEnabled) {
        pauseUpdates = true
        removeScheduledPeriodicFetchRunnables()
    }
}

internal fun DocServer.startLiveUpdates() {
    if (DesignSettings.liveUpdatesEnabled) {
        pauseUpdates = false
        scheduleLiveUpdate()
    }
}

internal fun DocServer.removeScheduledPeriodicFetchRunnables() {
    mainHandler.removeCallbacks(periodicFetchRunnable!!)
}

internal fun DocServer.scheduleLiveUpdate() {
    if (DesignSettings.liveUpdatesEnabled && !pauseUpdates) {
        // Stop any live updates that have already been scheduled.
        removeScheduledPeriodicFetchRunnables()
        mainHandler.postDelayed(periodicFetchRunnable!!, FETCH_INTERVAL_MILLIS)
    }
}

internal fun DocServer.fetchDocuments(
    firstFetch: Boolean,
): Boolean {

    val figmaApiKey = DesignSettings.figmaApiKeyStateFlow?.value
    if (figmaApiKey == null) {
        DesignSettings.showMessageInToast(
            "No Figma API Key Set - LiveUpdate Disabled",
            Toast.LENGTH_LONG
        )
        return false
    }
    val docIds =
        synchronized(subscriptions) {
            // Collect the docs
            subscriptions.keys.toList()
        }
    for (id in docIds) {
        val previousDoc = synchronized(documents) { documents[id] }
        Feedback.startLiveUpdate(id)

        val params =
            synchronized(subscriptions) {
                subscriptions[id]?.serverParams ?: DocumentServerParams()
            }
        val saveFile = synchronized(subscriptions) { subscriptions[id]?.saveFile }
        try {
            val postData = constructPostJson(figmaApiKey, previousDoc?.c, params, firstFetch)

            val documentData: ByteArray? = LiveUpdateJni.fetchDocBytes(id, postData)

            if (documentData != null) {
                Feedback.documentDecodeReadBytes(documentData.size, id)
                val doc = decodeServerDoc(documentData, previousDoc, id, saveFile, Feedback)
                if (doc == null) {
                    Feedback.documentDecodeError(id)
                    Log.e(TAG, "Error decoding doc.")
                    break
                }
                updateBranches(id, doc)

                // Remember the new document
                synchronized(documents) { documents[id] = doc }

                // Get the list of subscribers to this document id
                val subs: Array<LiveDocSubscription> =
                    synchronized(subscriptions) {
                        subscriptions[id]?.subscribers?.toTypedArray() ?: arrayOf()
                    }

                // On the main thread, tell all of the subscribers about the new document.
                mainHandler.post {
                    SpanCache.clear()
                    for (subscriber in subs) {
                        subscriber.onUpdate(doc)
                    }
                }
                Feedback.documentUpdated(id, subs.size)
            } else {
                Feedback.documentUnchanged(id)
            }
            // TODO: expand exception handling. Need to have the
            // JNI raise descriptive exceptions.
        } catch (exception: Exception) {
            if (DocumentSwitcher.isNotOriginalDocId(id)) {
                Feedback.documentUpdateErrorRevert(id, exception)
                DocumentSwitcher.revertToOriginal(id)
            } else {
                Feedback.documentUpdateError(id, exception)
            }
        }
    }
    return true
}

internal fun DocServer.subscribe(
    doc: LiveDocSubscription,
    serverParams: DocumentServerParams,
    saveFile: File?,
) {
    Feedback.addSubscriber(doc.docId)
    synchronized(subscriptions) {
        val subList = subscriptions[doc.docId] ?: LiveDocSubscriptions(serverParams, saveFile)
        subList.subscribers.add(doc)
        subscriptions[doc.docId] = subList
    }
}

internal fun DocServer.unsubscribe(doc: LiveDocSubscription) {
    Feedback.removeSubscriber(doc.docId)
    synchronized(subscriptions) {
        val subList = subscriptions[doc.docId] ?: return
        subList.subscribers.remove(doc)
        if (subList.subscribers.size == 0) {
            subscriptions.remove(doc.docId)
        }
    }
}

private fun DocServer.updateBranches(docId: String, doc: DocContent) {
    val docBranches = branchHash[docId] ?: HashMap<String, String>()
    doc.c.branches?.forEach { if (!docBranches.containsKey(it.id)) docBranches[it.id] = it.name }

    // Create a "Main" branch for this doc ID so that we can go back to it after switching to a
    // branch
    if (doc.c.branches?.isNotEmpty() == true && !docBranches.containsKey(docId))
        docBranches[docId] = "Main"
    // Update the branch list for this ID as well as all branches of this ID
    branchHash[docId] = docBranches
    doc.c.branches?.forEach { branchHash[it.id] = docBranches }
}

@Composable
internal fun DocServer.doc(
    resourceName: String,
    docId: String,
    serverParams: DocumentServerParams,
    disableLiveMode: Boolean,
): DocContent? {
    // Check that the document ID is valid
    if (!validateFigmaDocId(docId)) {
        Log.w(TAG, "Invalid Figma document ID: $docId")
        return null
    }

    val id = "${resourceName}_$docId"

    // Create a state var to remember the document contents and update it when the doc changes
    val (liveDoc, setLiveDoc) = remember { mutableStateOf<DocContent?>(null) }

    // See if we've already loaded this doc
    val preloadedDoc = synchronized(documents) { documents[docId] }

    val context = LocalContext.current
    val saveFile = context.getFileStreamPath(id)

    // We can manage the subscription lifecycle using a DisposableEffect (which
    // will run any time id changes, and also on first execution; and runs the
    // onDispose closure prior to running a change, or when the parent Composable
    // is no longer in use).
    DisposableEffect(id) {
        // DisposableEffect can cause execution of the hosting Composable to be
        // suspended, OR the closure we're in here is run later. That means that
        // another Composable could have loaded `docId` before we were run, so
        // we should try to fetch the doc again here.
        //
        // If we don't check to see if the doc was loaded again, then we can end
        // up loading it many times in a row, eventually leading to heap exhaustion.
        val targetDoc =
            synchronized(documents) {
                var targetDoc = documents[docId]
                if (targetDoc == null) {
                    try {
                        // Attempt to load it from disk synchronously.
                        targetDoc =
                            decodeDiskDoc(
                                BufferedInputStream(FileInputStream(saveFile)),
                                null,
                                docId,
                                Feedback
                            )
                        if (targetDoc != null) {
                            documents[docId] = targetDoc
                        }
                    } catch (error: Throwable) {
                        Feedback.diskLoadFail(id, docId)
                    }
                }
                targetDoc
            }
        setLiveDoc(targetDoc)

        // Subscribe to live updates, if we have an access token.
        var subscription: LiveDocSubscription? = null
        if (!disableLiveMode) {
            subscription = LiveDocSubscription(id, docId, setLiveDoc)
            subscribe(subscription, serverParams, saveFile)
        }
        onDispose { if (subscription != null) unsubscribe(subscription) }
    }

    // Don't return a doc with the wrong ID.
    if (liveDoc != null && liveDoc.c.docId == docId) return liveDoc
    if (preloadedDoc != null && preloadedDoc.c.docId == docId) return preloadedDoc

    // Use the LocalContext to locate this doc in the precompiled serializedDesignDocuments
    val assetManager = context.assets
    try {
        val assetDoc = assetManager.open("figma/$id")
        val decodedDoc = decodeDiskDoc(assetDoc, null, docId, Feedback)
        if (decodedDoc != null) {
            synchronized(documents) { documents[docId] = decodedDoc }
            return decodedDoc
        }
    } catch (error: Throwable) {
        Feedback.assetLoadFail(id, docId)
    }

    // We didn't manage to load the doc. Hopefully there's an access token
    // and it'll get loaded live.
    return null
}

internal fun DocServer.branches(docId: String): HashMap<String, String>? {
    return branchHash[docId]
}
