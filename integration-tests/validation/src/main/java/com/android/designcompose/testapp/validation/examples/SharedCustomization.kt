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

package com.android.designcompose.testapp.validation.examples

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DesignComposeCallbacks
import com.android.designcompose.DesignDoc
import com.android.designcompose.DesignSwitcherPolicy
import com.android.designcompose.ImageReplacementContext
import com.android.designcompose.LocalCustomizationContext
import com.android.designcompose.OpenLinkCallback
import com.android.designcompose.ParentComponentInfo
import com.android.designcompose.ReplacementContent
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.Design2
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignModule
import com.android.designcompose.annotation.DesignModule2
import com.android.designcompose.annotation.DesignModuleClass
import com.android.designcompose.annotation.DesignVariant
import com.android.designcompose.common.DocumentServerParams
import com.android.designcompose.mergeFrom
import com.android.designcompose.sDocClass
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.setComponent
import com.android.designcompose.setContent
import com.android.designcompose.setCustomComposable
import com.android.designcompose.setImage
import com.android.designcompose.setImageWithContext
import com.android.designcompose.setKey
import com.android.designcompose.setOpenLinkCallback
import com.android.designcompose.setTapCallback
import com.android.designcompose.setText
import com.android.designcompose.setTextFunction
import com.android.designcompose.setVariantProperties
import com.android.designcompose.setVisible
import java.lang.Thread.sleep
import kotlin.concurrent.thread

@DesignModuleClass
class TextModuleOne(
    @Design2(node = "#a") val a: String,
    @Design2(node = "#b") val b: String,
    val blah: String,
)

@DesignModuleClass
class TextModuleTwo(
    @Design2(node = "#c") val c: String,
    @Design2(node = "#d") val d: String,
    @Design2(node = "#i") val i: Bitmap?,
)

@DesignModuleClass
class TextModuleCombined(
    @DesignModule2 val one: TextModuleOne,
    @DesignModule2 val two: TextModuleTwo,
    @Design2(node = "#e") val e: String,
)

@DesignDoc(id = "hPEGkrF0LUqNYEZObXqjXZ")
interface ModuleExample {
    @DesignComponent(node = "#stage")
    fun Main(
        @Design(node = "#a") a: String,
        @Design(node = "#b") b: String,
        @DesignModule customText: TextModuleCombined,
    )
}

private fun getCustomText(): TextModuleCombined {
    return TextModuleCombined(
        one =
        TextModuleOne(
            a = "Hello",
            b = "World,",
            blah = "blah",
        ),
        two =
        TextModuleTwo(
            c = "and",
            d = "Goodbye",
            i = null,
        ),
        e = "World!",
    )
}

@Composable
fun ModuleExample() {
    ModuleExampleDoc.Main(
        a = "aaa",
        b = "bbb",
        customText = getCustomText(),
    )
}
/*
enum class BrowseItemType {
    Grid,
    List,
}

// Shared customization example

@DesignModuleClass
class MediaTrackData(
    @Design(node = "#title") title: @Composable () -> String,
    @Design(node = "#artist") artist: @Composable () -> String,
    @Design(node = "#album") album: @Composable () -> String,
    @Design(node = "#album-art") albumArt: @Composable (ImageReplacementContext) -> Bitmap?,
)

@DesignDoc(id = "hPEGkrF0LUqNYEZObXqjXZ")
interface MediaExample {
    @DesignComponent(node = "#stage")
    fun Main(
        @Design(node = "#now-playing")
        nowPlaying: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#browse-list") list: ReplacementContent,
        @DesignModule customText: TextModuleCombined,
    )

    @DesignComponent(node = "#now-playing-item")
    fun NowPlaying(
        @DesignModule metadata: MediaTrackData,
        @Design(node = "#play-state-button") showPlay: Boolean,
        @Design(node = "#prev-button") showPrev: Boolean,
        @Design(node = "#next-button") showNext: Boolean,
    )

    @DesignComponent(node = "#BrowseItem")
    fun BrowseItem(
        @DesignModule metadata: MediaTrackData,
        @DesignVariant(property = "#browse-item") browseItemType: BrowseItemType,
    )
}

// START
// This is what the generated might look like
data class MediaTrackDataGen(
    val title: @Composable () -> String,
    val artist: @Composable () -> String,
    val album: @Composable () -> String,
    val albumArt: @Composable (ImageReplacementContext) -> Bitmap?,
) {
    val customizations = CustomizationContext()

    fun queries(): ArrayList<String> {
        return arrayListOf(
            "#title",
            "#artist",
            "#album",
            "#album-art",
        )
    }

    fun ignoredImages(): ArrayList<String> {
        return arrayListOf(
            "#album-art",
        )
    }

    init {
        customizations.setTextFunction("#title", title)
        customizations.setTextFunction("#artist", artist)
        customizations.setTextFunction("#album", album)
        customizations.setImageWithContext("#album-art", albumArt)

        // val variantProperties = HashMap<String, String>()
        // variantProperties["#browse-item"] = browseItemType.name
        // customizations.setVariantProperties(variantProperties)
    }
}

interface MediaExampleGenManual {
    @Composable
    fun Main(
        modifier: Modifier = Modifier,
        openLinkCallback: OpenLinkCallback? = null,
        designComposeCallbacks: DesignComposeCallbacks? = null,
        key: String? = null,
        nowPlaying: @Composable (ComponentReplacementContext) -> Unit,
        list: ReplacementContent,
        customText: TextModuleCombined,
    ) {
        val className = javaClass.name
        val customizations = remember { CustomizationContext() }
        customizations.setKey(key)
        customizations.mergeFrom(LocalCustomizationContext.current)

        // NEW: merge with customizations in generated class MediaTrackDataGen
        customizations.mergeFrom(customText.customizations())

        var nodeName = "#stage"
        val rootNodeQuery = NodeQuery.NodeName(nodeName)
        if (openLinkCallback != null) customizations.setOpenLinkCallback(nodeName, openLinkCallback)
        customizations.setContent("#browse-list", list)
        customizations.setComponent("#now-playing", nowPlaying)

        customizations.setCustomComposable { mod, name, query, parentComponents, tapCallback ->
            CustomComponent(mod, name, query, parentComponents, tapCallback)
        }

        val (docId, setDocId) = remember { mutableStateOf("hPEGkrF0LUqNYEZObXqjXZ") }

        // Add queries from any modules
        //val queries: ArrayList<String> = arrayListOf()
        //queries.addAll(customText.queries())
        //queries.addAll(queries())
        val queries = queries()

        // TODO Add ignored images
        val ignored: HashMap<String, ArrayList<String>> = hashMapOf()

        CompositionLocalProvider(LocalCustomizationContext provides customizations) {
            DesignDoc(
                "MediaExampleDoc",
                docId,
                rootNodeQuery,
                customizations = customizations,
                modifier = modifier.semantics { sDocClass = className },
                serverParams = DocumentServerParams(queries, ignoredImages()),
                setDocId = setDocId,
                designSwitcherPolicy = DesignSwitcherPolicy.SHOW_IF_ROOT,
                designComposeCallbacks = designComposeCallbacks,
            )
        }
    }

    @Composable
    fun BrowseItem(
        modifier: Modifier = Modifier,
        openLinkCallback: OpenLinkCallback? = null,
        designComposeCallbacks: DesignComposeCallbacks? = null,
        key: String? = null,
        metadata: MediaTrackDataGen,
        browseItemType: BrowseItemType,
    ) {
        val className = javaClass.name
        val customizations = remember { CustomizationContext() }
        customizations.setKey(key)
        customizations.mergeFrom(LocalCustomizationContext.current)

        // NEW: merge with customizations in generated class MediaTrackDataGen
        customizations.mergeFrom(metadata.customizations)

        val variantProperties = HashMap<String, String>()
        variantProperties["#browse-item"] = browseItemType.name
        customizations.setVariantProperties(variantProperties)

        var nodeName = "#browse-item"
        val rootNodeQuery = NodeQuery.NodeName(nodeName)
        if (openLinkCallback != null) customizations.setOpenLinkCallback(nodeName, openLinkCallback)

        customizations.setCustomComposable { mod, name, query, parentComponents, tapCallback ->
            CustomComponent(mod, name, query, parentComponents, tapCallback)
        }

        val (docId, setDocId) = remember { mutableStateOf("hPEGkrF0LUqNYEZObXqjXZ") }
        val queries = queries()
        CompositionLocalProvider(LocalCustomizationContext provides customizations) {
            DesignDoc(
                "MediaExampleDoc",
                docId,
                rootNodeQuery,
                customizations = customizations,
                modifier = modifier.semantics { sDocClass = className },
                serverParams = DocumentServerParams(queries, ignoredImages()),
                setDocId = setDocId,
                designSwitcherPolicy = DesignSwitcherPolicy.SHOW_IF_ROOT,
                designComposeCallbacks = designComposeCallbacks,
            )
        }
    }

    @Composable
    fun NowPlaying(
        modifier: Modifier = Modifier,
        openLinkCallback: OpenLinkCallback? = null,
        designComposeCallbacks: DesignComposeCallbacks? = null,
        key: String? = null,
        metadata: MediaTrackDataGen,
        showPlay: Boolean,
        showPrev: Boolean,
        showNext: Boolean,
    ) {
        val className = javaClass.name
        val customizations = remember { CustomizationContext() }
        customizations.setKey(key)
        customizations.mergeFrom(LocalCustomizationContext.current)
        var nodeName = "#now-playing-item"
        val rootNodeQuery = NodeQuery.NodeName(nodeName)
        if (openLinkCallback != null) customizations.setOpenLinkCallback(nodeName, openLinkCallback)
        customizations.setVisible("#play-state-button", showPlay)
        customizations.setVisible("#prev-button", showPrev)
        customizations.setVisible("#next-button", showNext)

        // NEW: merge with customizations in generated class MediaTrackDataGen
        customizations.mergeFrom(metadata.customizations)

        customizations.setCustomComposable { mod, name, query, parentComponents, tapCallback ->
            CustomComponent(mod, name, query, parentComponents, tapCallback)
        }

        val (docId, setDocId) = remember { mutableStateOf("hPEGkrF0LUqNYEZObXqjXZ") }
        val queries = queries()
        CompositionLocalProvider(LocalCustomizationContext provides customizations) {
            DesignDoc(
                "MediaExampleDoc",
                docId,
                rootNodeQuery,
                customizations = customizations,
                modifier = modifier.semantics { sDocClass = className },
                serverParams = DocumentServerParams(queries, ignoredImages()),
                setDocId = setDocId,
                designSwitcherPolicy = DesignSwitcherPolicy.SHOW_IF_ROOT,
                designComposeCallbacks = designComposeCallbacks,
            )
        }
    }

    @Composable
    fun CustomComponent(
        modifier: Modifier = Modifier,
        nodeName: String,
        rootNodeQuery: NodeQuery,
        parentComponents: List<ParentComponentInfo>,
        tapCallback: TapCallback?,
    ) {
        val customizations = remember { CustomizationContext() }
        if (tapCallback != null) customizations.setTapCallback(nodeName, tapCallback)
        customizations.mergeFrom(LocalCustomizationContext.current)
        val (docId, setDocId) = remember { mutableStateOf("hPEGkrF0LUqNYEZObXqjXZ") }
        val queries = queries()
        queries.add(nodeName)
        CompositionLocalProvider(LocalCustomizationContext provides customizations) {
            DesignDoc(
                "MediaExampleDoc",
                docId,
                rootNodeQuery,
                customizations = customizations,
                modifier = modifier,
                serverParams = DocumentServerParams(queries, ignoredImages()),
                setDocId = setDocId,
                designSwitcherPolicy = DesignSwitcherPolicy.SHOW_IF_ROOT,
                parentComponents = parentComponents
            )
        }
    }

    fun queries(): ArrayList<String> {
        return arrayListOf(
            "#stage",
            "#browse-item",
            "#now-playing-item",
            "#browse-list",
            "#play-state-button",
            "#prev-button",
            "#next-button",
            "#a",
            "#b",
            "#c",
            "#d",
            "#i",
            "#e",
        )
    }

    fun ignoredImages(): HashMap<String, Array<String>> {
        return hashMapOf(
            "#browse-item" to arrayOf(),
            "#now-playing-item" to arrayOf(),
            "#stage" to
                arrayOf(
                    "#browse-list",
                    "#now-playing-item",
                    "#i",
                ),
        )
    }
}

object MediaExampleDocManual : MediaExampleGenManual
// END

object MediaDataGenerator {
    private var title: MutableLiveData<String> = MutableLiveData("asdf")

    private var updateTitleRunnable: Runnable = Runnable {
        thread {
            println("### Thread!")
            var count = 0
            while (true) {
                title.postValue(title.value!! + count)
                count += 1
                title.postValue(if (count % 2 == 0) "Hello" else "Bye")
                sleep(1000)
            }
        }
    }

    internal fun getTitle(): LiveData<String> {
        return title
    }

    init {
        println("### Start")
        updateTitleRunnable.run()
    }
}

@Composable
private fun mediaTrackData(): MediaTrackDataGen {
    println("### mediaTrackData")
    val mediaTrackData =
        MediaTrackDataGen(
            title = {
                val title: String? by MediaDataGenerator.getTitle().observeAsState()
                println("### Title $title")
                title ?: ""
            },
            artist = { "Beatles 2" },
            album = { "Abbey Road 2" },
            albumArt = { null },
        )
    return mediaTrackData
}

fun getNowPlayingMetadata(): MediaTrackDataGen {
    return MediaTrackDataGen(
        title = { "TESTING" },
        artist = { "Max Maximum" },
        album = { "Selfies Everyday" },
        albumArt = { null },
    )
}

fun getBrowseItem(index: Int): MediaTrackDataGen {
    // TODO make a fake list of tracks
    return MediaTrackDataGen(
        title = { "Getting Older" },
        artist = { "Max Maximum" },
        album = { "Selfies Everyday" },
        albumArt = { null },
    )
}

@Composable
fun MediaExample() {
    println("### Main")
    MediaExampleDocManual.Main(
        nowPlaying = {
            MediaExampleDocManual.NowPlaying(
                metadata = getNowPlayingMetadata(),
                showPlay = true,
                showPrev = true,
                showNext = true,
            )
        },
        list =
            ReplacementContent(
                count = 2,
                content = { index ->
                    {
                        println("### MediaItem2")
                        val browseItem = getBrowseItem(index)
                        MediaExampleDocManual.BrowseItem(
                            metadata = browseItem,
                            browseItemType = BrowseItemType.Grid,
                        )
                    }
                }
            ),
        customText = getCustomText(),
    )
}
*/
