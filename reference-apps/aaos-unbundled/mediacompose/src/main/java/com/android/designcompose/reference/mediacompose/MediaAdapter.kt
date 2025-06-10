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

package com.android.designcompose.reference.mediacompose

import android.app.Application
import android.app.PendingIntent
import android.car.media.CarMediaManager.MEDIA_SOURCE_MODE_BROWSE
import android.car.media.CarMediaManager.MEDIA_SOURCE_MODE_PLAYBACK
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.util.LruCache
import android.util.Size
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.media.utils.MediaConstants
import com.android.car.apps.common.imaging.ImageBinder
import com.android.car.apps.common.util.FutureData
import com.android.car.media.common.MediaItemMetadata
import com.android.car.media.common.browse.MediaBrowserViewModelImpl
import com.android.car.media.common.playback.PlaybackProgress
import com.android.car.media.common.playback.PlaybackViewModel
import com.android.car.media.common.playback.PlaybackViewModel.RawCustomPlaybackAction
import com.android.car.media.common.source.CarMediaManagerHelper
import com.android.car.media.common.source.MediaBrowserConnector
import com.android.car.media.common.source.MediaBrowserConnector.BrowsingState
import com.android.car.media.common.source.MediaModels
import com.android.car.media.common.source.MediaSource
import com.android.car.media.common.source.MediaSourceViewModel
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.DesignNodeData
import com.android.designcompose.EmptyListContent
import com.android.designcompose.ImageReplacementContext
import com.android.designcompose.ListContent
import com.android.designcompose.ListContentData
import com.android.designcompose.TapCallback
import java.lang.reflect.Field
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

private const val TAG = "DesignCompose_Media"

// Specifies that the corresponding items should be presented as grids.
const val CONTENT_STYLE_GRID_ITEM_HINT_VALUE = 2

// Specifies that the corresponding items should be presented as grids and are
// represented by a vector icon. This adds a small margin around the icons
// instead of filling the full available area.
const val CONTENT_STYLE_CATEGORY_GRID_ITEM_HINT_VALUE = 4

private data class MediaArtKey(val uri: String?, val width: Int, val height: Int, val color: Int?) {
    fun isValid(): Boolean {
        return !uri.isNullOrEmpty() && width > 0 && height > 0
    }
}

private class MediaArtCache {
    private var cache: LruCache<String, Bitmap> = LruCache(512)

    fun insert(mediaArtKey: MediaArtKey, img: Bitmap) {
        if (mediaArtKey.isValid()) cache.put(mediaArtKey.toString(), img)
    }

    fun get(mediaArtKey: MediaArtKey): Bitmap? {
        return if (mediaArtKey.isValid()) cache.get(mediaArtKey.toString()) else null
    }
}

private class ItemData(
    var nodeData: DesignNodeData = DesignNodeData(),
    var composable: @Composable () -> Unit = {},
    var key: String = "",
)

class MediaNowPlaying {
    var title: String = ""
    var artist: String = ""
    var album: String = ""
    var albumArt: (ImageReplacementContext) -> Bitmap? = { null }
    var showPlay: Boolean = false
    var showPause: Boolean = false
    var showPrev: Boolean = false
    var showNext: Boolean = false
    var customActions: ListContent = EmptyListContent()
    var playController: PlaybackViewModel.PlaybackController? = null
    var upNextTitle: String = ""
    var upNextList: ListContent = EmptyListContent()
    var showUpNext: Boolean = false
    var hasError: Boolean = false
    var errorFrame: @Composable (ComponentReplacementContext) -> Unit = {}
}

class MediaBrowse {
    var navContent: ListContent = EmptyListContent()
    var sourceButtons: ListContent = EmptyListContent()
    var supportsSearch: Boolean = false
    var showSettings: Boolean = false
    var onTapSettings: TapCallback = {}
    var headerContent: ListContent = EmptyListContent()
    var title: String = ""
    var onTapBack: TapCallback = {}
    var content: ListContent = EmptyListContent()
}

class MediaSearch {
    var query: String = ""
    var setQuery: (String) -> Unit = {}
    var searchFunc: (String) -> Unit = {}
    var searchField: @Composable (ComponentReplacementContext) -> Unit = {}
    var showHeader: Boolean = false
    var title: String = ""
    var onTapBack: TapCallback = {}
    var resultsContent: ListContent = EmptyListContent()
}

// Helper class that keeps track of subscribers of requests to get album art. Some views show the
// same album art multiple times, so to avoid having each one request the same art, only the first
// one requests it, but they all subscribe to get updated when the art request comes back.
private class MediaArtRequestManager {
    private var artRequestSubscriptions: HashMap<MediaArtKey, HashSet<(Bitmap?) -> Unit>> =
        HashMap()
    // Maps a bitmap setter function to the MediaArtKey that initiated the request
    private var setBitmapSubscriptions: HashMap<(Bitmap?) -> Unit, MediaArtKey> = HashMap()

    fun subscribe(key: MediaArtKey, func: (Bitmap?) -> Unit) {
        val set = artRequestSubscriptions[key] ?: HashSet()
        set.add(func)
        artRequestSubscriptions[key] = set

        // Save the MediaArtKey associated with this setter function so we can unsubscribe later
        setBitmapSubscriptions[func] = key
    }

    fun unsubscribe(func: (Bitmap?) -> Unit) {
        // Remove the subscription associated with this setter function
        val key = setBitmapSubscriptions[func]
        key?.let {
            val subscriptions = artRequestSubscriptions[it]
            subscriptions?.remove(func)
        }
    }

    fun updateAll(key: MediaArtKey, img: Bitmap?) {
        val subscribers = artRequestSubscriptions[key]
        subscribers?.forEach { it.invoke(img) }
    }
}

// Helper class that loads art for MediaItemMetadata. Create this with a callback
// that takes the loaded bitmap, then provide the artwork key by calling setImage().
private class MediaArtworkBinder(
    width: Int,
    height: Int,
    color: Int?,
    threadPool: ThreadPoolExecutor,
    setBitmap: (b: Bitmap?) -> Unit,
) :
    ImageBinder<MediaItemMetadata.ArtworkRef?>(
        PlaceholderType.BACKGROUND,
        Size(width, height),
        Consumer { drawable: Drawable? ->
            if (drawable == null) {
                setBitmap(null)
            } else {
                threadPool.execute(
                    Runnable {
                        val buffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val c = Canvas(buffer)
                        drawable.setBounds(0, 0, width, height)
                        if (color != null) {
                            when (drawable) {
                                is ColorDrawable -> {
                                    drawable.color = color
                                }
                                is VectorDrawable -> {
                                    drawable.setTint(color)
                                }
                            }
                        }
                        drawable.draw(c)
                        setBitmap(buffer)
                    }
                )
            }
        },
    ) {}

private fun getMediaUrl(metadata: MediaItemMetadata?): String {
    val descriptionField: Field =
        MediaItemMetadata::class.java.getDeclaredField("mMediaDescription")
    descriptionField.isAccessible = true
    if (metadata != null) {
        val fieldValue = descriptionField.get(metadata) as MediaDescriptionCompat
        return fieldValue.mediaUri?.toString() ?: ""
    }
    return ""
}

private fun isMetadataSame(item1: MediaItemMetadata?, item2: MediaItemMetadata?): Boolean {
    if (item1 == null || item2 == null) return false

    val item1Url = getMediaUrl(item1)
    val item2Url = getMediaUrl(item2)
    if (item1Url.isEmpty() || item2Url.isEmpty()) return false
    if (item1Url == item2Url) return true
    return (item1.id != null &&
        item2.id != null &&
        (item1.id == item2.id ||
            item1.id!!.contains(item2.id!!) ||
            item2.id!!.contains(item1.id!!)))
}

// Get the bitmap associated with a custom action
private fun getCustomActionIcon(
    action: RawCustomPlaybackAction,
    context: Context?,
    width: Int?,
    height: Int?,
    color: Int?,
): Bitmap? {
    val playbackAction = action.fetchDrawable(context!!)
    if (playbackAction != null) {
        val bitmap = Bitmap.createBitmap(width ?: 60, height ?: 60, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        playbackAction.mIcon.setBounds(0, 0, width ?: 60, height ?: 60)
        if (color != null) playbackAction.mIcon.setTint(color)
        playbackAction.mIcon.draw(canvas)
        return bitmap
    }
    return null
}

class MediaAdapter(
    application: Application,
    private val context: Context,
    activity: ComponentActivity,
) {
    private val nowPlayingViewModels = MediaModels(application, MEDIA_SOURCE_MODE_PLAYBACK)
    // Data for the currently playing source
    private val nowPlayingMediaSource =
        MediaSourceViewModel.get(application, MEDIA_SOURCE_MODE_PLAYBACK)
    // Data for the currently playing track
    private val nowPlayingPlaybackViewModel: PlaybackViewModel =
        nowPlayingViewModels.playbackViewModel
    private val browseMediaModels = MediaModels(application, MEDIA_SOURCE_MODE_BROWSE)
    // We update the list of sources whenever the selected source for browsing changes.
    private val browseMediaSource = browseMediaModels.mediaSourceViewModel
    private val browsePlaybackViewModel: PlaybackViewModel = browseMediaModels.playbackViewModel

    private val mediaManagerHelper = CarMediaManagerHelper.getInstance(context)
    private val mediaSourcesProvider = MediaSourcesProvider.getInstance(context)
    // A repository that tracks media item children
    private val mediaRepo = browseMediaModels.mediaItemsRepository
    // A stack of browse pages
    private val browseStack: MediaBrowseStack = MediaBrowseStack(mediaRepo, activity)
    // A stack of search browse pages
    private val searchStack: MediaBrowseStack = MediaBrowseStack(mediaRepo, activity)
    // An intent to launch when the settings icon is tapped
    private var settingsIntent: Intent? = null
    // Cache of album artwork
    private var artCache: MediaArtCache = MediaArtCache()
    // Set of pending artwork requests, so that we don't send duplicate requests
    private var artRequests: MutableSet<MediaArtKey> = mutableSetOf()
    // Manager to keep track of which views need to be updated when artwork requests come back
    private var artRequestManager: MediaArtRequestManager = MediaArtRequestManager()
    // Whether the current source can be searched
    private val canSearch: MutableLiveData<Boolean> = MutableLiveData(false)

    private val artworkThreadPool: ThreadPoolExecutor =
        ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2,
            Runtime.getRuntime().availableProcessors() * 2,
            60L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue<Runnable>(),
        )

    private val mediaBrowsingObserver = Observer { newBrowsingState: BrowsingState? ->
        this.onMediaBrowsingStateChanged(newBrowsingState)
    }

    init {
        // Observe forever ensures the caches are destroyed even while the activity isn't resumed.
        mediaRepo.browsingState.observeForever(mediaBrowsingObserver)
    }

    private fun onMediaBrowsingStateChanged(newBrowsingState: BrowsingState?) {
        if (newBrowsingState == null) {
            Log.e(TAG, "Null browsing state (no media source!)")
            return
        }
        when (newBrowsingState.mConnectionStatus) {
            MediaBrowserConnector.ConnectionStatus.CONNECTING -> {
                canSearch.value = false
            }
            MediaBrowserConnector.ConnectionStatus.CONNECTED -> {
                Log.i(
                    TAG,
                    "Media Browse State CONNECTED ${newBrowsingState.mMediaSource.getDisplayName(context)}",
                )
                val browser = newBrowsingState.mBrowser
                canSearch.value = MediaBrowserViewModelImpl.getSupportsSearch(browser)

                val rootId: String? = browser?.root
                mediaRepo.getMediaChildren(rootId, Bundle()).observeForever { items ->
                    browseStack.resetTo(
                        if (items?.data?.isEmpty() == true) null else items?.data?.get(0)
                    )
                }
            }
            MediaBrowserConnector.ConnectionStatus.DISCONNECTING,
            MediaBrowserConnector.ConnectionStatus.REJECTED,
            MediaBrowserConnector.ConnectionStatus.SUSPENDED -> {
                browseStack.clear()
                canSearch.value = false
            }
            MediaBrowserConnector.ConnectionStatus.NONEXISTENT -> {}
        }
    }

    private fun getArtwork(
        mediaItemMetadata: MediaItemMetadata,
        width: Int,
        height: Int,
        color: Int?,
        setBitmap: (b: Bitmap?) -> Unit,
    ): Bitmap? {
        val mediaArtKey =
            MediaArtKey(mediaItemMetadata.artworkKey?.imageURI?.toString(), width, height, color)

        // Do nothing and return null if the key is not valid
        if (!mediaArtKey.isValid()) return null

        // Subscribe to be notified when this artwork is updated
        artRequestManager.subscribe(mediaArtKey, setBitmap)

        // If the image is cached, return it
        val cachedImage = artCache.get(mediaArtKey)
        if (cachedImage != null) return cachedImage

        // If there is already a request for this artwork in progress, abort
        if (artRequests.contains(mediaArtKey)) return null

        // Request the artwork
        val artBinder =
            MediaArtworkBinder(
                mediaArtKey.width,
                mediaArtKey.height,
                mediaArtKey.color,
                artworkThreadPool,
            ) { b: Bitmap? ->
                val mainHandler = Handler(context.mainLooper)
                mainHandler.post {
                    if (b != null) artCache.insert(mediaArtKey, b)
                    artRequests.remove(mediaArtKey)
                    artRequestManager.updateAll(mediaArtKey, b)
                }
            }
        artRequests.add(mediaArtKey)
        artBinder.setImage(context, mediaItemMetadata.artworkKey)
        return null
    }

    @Composable
    private fun MediaBrowseItem(
        item: MediaItemMetadata,
        grid: Boolean,
        currentMetadata: MediaItemMetadata?,
        onTap: TapCallback,
        key: String?,
        media: CenterDisplayGen,
    ) {
        val (icon, setIcon) = remember { mutableStateOf<Bitmap?>(null) }

        val currentlyPlaying =
            if (isMetadataSame(item, currentMetadata)) CurrentlyPlaying.On else CurrentlyPlaying.Off

        // Unsubscribe this setter function whenever this leaves the composition
        DisposableEffect(setIcon) { onDispose { artRequestManager.unsubscribe(setIcon) } }

        media.BrowseItem(
            modifier = Modifier,
            openLinkCallback = null,
            browseType = if (grid) BrowseItemType.Grid else BrowseItemType.List,
            currentlyPlaying = currentlyPlaying,
            onTap = onTap,
            title = item.title?.toString() ?: "",
            subtitle = item.subtitle?.toString() ?: "",
            showSubtitle = !item.subtitle.isNullOrEmpty(),
            icon = { context ->
                val color = context.imageContext.getBackgroundColor()
                val width = context.imageContext.getPixelWidth() ?: 200
                val height = context.imageContext.getPixelHeight() ?: 200
                val cachedIcon = getArtwork(item, width, height, color, setIcon)
                cachedIcon ?: icon
            },
            key = key,
        )
    }

    @Composable
    private fun MediaNavButton(
        item: MediaItemMetadata,
        browseStack: MediaBrowseStack,
        media: CenterDisplayGen,
    ) {
        val (icon, setIcon) = remember { mutableStateOf<Bitmap?>(null) }
        val navButtonType =
            if (item.id == browseStack.selectedRootId()) NavButtonType.Selected
            else NavButtonType.Unselected

        // Unsubscribe this setter function whenever this leaves the composition
        DisposableEffect(setIcon) { onDispose { artRequestManager.unsubscribe(setIcon) } }
        media.PageHeaderNavButton(
            modifier = Modifier,
            openLinkCallback = null,
            navButtonType = navButtonType,
            onTap = { browseStack.resetTo(item) },
            name = item.title.toString(),
            icon = { context ->
                val color: Int? = context.imageContext.getBackgroundColor()
                val width = context.imageContext.getPixelWidth() ?: 50
                val height = context.imageContext.getPixelHeight() ?: 50
                val cachedIcon = getArtwork(item, width, height, color, setIcon)
                cachedIcon ?: icon
            },
            key = item.id,
        )
    }

    @Composable
    fun getNowPlayingMediaSource(): MediaSource? {
        // Observe the currently playing media source
        val mediaSource: MediaSource? by nowPlayingMediaSource.primaryMediaSource.observeAsState()
        return mediaSource
    }

    @Composable
    fun getBrowseMediaSource(): MediaSource? {
        val mediaSource: MediaSource? by browseMediaSource.primaryMediaSource.observeAsState()
        return mediaSource
    }

    @Composable
    fun getNowPlaying(media: CenterDisplayGen): MediaNowPlaying {
        val nowPlaying = MediaNowPlaying()

        // Observe now playing metadata
        val metadata: MediaItemMetadata? by nowPlayingPlaybackViewModel.metadata.observeAsState()
        nowPlaying.title = metadata?.title?.toString() ?: ""
        nowPlaying.artist = metadata?.subtitle?.toString() ?: ""
        nowPlaying.album = metadata?.description?.toString() ?: ""

        // Observe playback controller to attach actions.
        val playController: PlaybackViewModel.PlaybackController? by
            nowPlayingPlaybackViewModel.playbackController.observeAsState()
        nowPlaying.playController = playController

        // Observe current playback state and get the button visibility
        val playbackState: PlaybackViewModel.PlaybackStateWrapper? by
            nowPlayingPlaybackViewModel.playbackStateWrapper.observeAsState()
        when (playbackState?.mainAction) {
            PlaybackViewModel.ACTION_PLAY -> {
                nowPlaying.showPlay = true
                nowPlaying.showPause = false
            }
            PlaybackViewModel.ACTION_PAUSE or PlaybackViewModel.ACTION_STOP -> {
                nowPlaying.showPlay = false
                nowPlaying.showPause = true
            }
            PlaybackViewModel.ACTION_DISABLED -> {
                nowPlaying.showPlay = false
                nowPlaying.showPause = false
            }
        }
        nowPlaying.showPrev = playbackState?.isSkipPreviousEnabled ?: false
        nowPlaying.showNext = playbackState?.isSkipNextEnabled ?: false

        // Show error frame if there are errors
        val extras = playbackState?.extras
        val hasAuthError =
            playbackState?.errorCode == PlaybackStateCompat.ERROR_CODE_AUTHENTICATION_EXPIRED
        nowPlaying.hasError = hasAuthError
        if (hasAuthError) {
            val hasActionLabel =
                extras?.containsKey(
                    MediaConstants.PLAYBACK_STATE_EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL
                ) ?: false
            val buttonText =
                if (!hasActionLabel) ""
                else {
                    extras?.getString(
                        MediaConstants.PLAYBACK_STATE_EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL
                    ) ?: ""
                }
            nowPlaying.errorFrame = { c ->
                media.ErrorFrame(
                    modifier = Modifier,
                    openLinkCallback = null,
                    errorMessage = playbackState?.errorMessage?.toString() ?: "",
                    errorButtonText = buttonText,
                    showErrorButton = hasActionLabel,
                    onTapErrorButton = {
                        val pendingIntent =
                            extras?.get(
                                MediaConstants
                                    .PLAYBACK_STATE_EXTRAS_KEY_ERROR_RESOLUTION_ACTION_INTENT
                            ) as PendingIntent?
                        if (pendingIntent == null) {
                            Log.i(TAG, "Unable to open null authentication PendingIntent")
                        } else {
                            val intent = Intent()
                            Log.i(TAG, "Opening authentication PendingIntent")
                            pendingIntent.send(context, 0, intent)
                        }
                    },
                )
            }
        }

        val itemData: ArrayList<ItemData> = arrayListOf()
        playbackState?.customActions?.forEach {
            val item = ItemData()
            item.nodeData = media.CustomActionButtonDesignNodeData()
            item.composable = {
                media.CustomActionButton(
                    modifier = Modifier,
                    openLinkCallback = null,
                    icon = { componentContext ->
                        val color = componentContext.imageContext.getBackgroundColor()
                        val width = componentContext.imageContext.getPixelWidth()
                        val height = componentContext.imageContext.getPixelHeight()
                        getCustomActionIcon(it, context, width, height, color)
                    },
                    onTap = { playController?.doCustomAction(it.mAction, null) },
                    key = it.mAction,
                )
            }
            itemData.add(item)
        }

        // Custom action buttons
        nowPlaying.customActions = {
            ListContentData(count = itemData.size) { index -> itemData[index].composable() }
        }

        // Album art
        val (albumBitmap, setAlbumBitmap) = remember { mutableStateOf<Bitmap?>(null) }
        // Unsubscribe this setter function whenever this leaves the composition
        DisposableEffect(setAlbumBitmap) {
            onDispose { artRequestManager.unsubscribe(setAlbumBitmap) }
        }
        nowPlaying.albumArt = { context ->
            val width = context.imageContext.getPixelWidth() ?: 300
            val height = context.imageContext.getPixelHeight() ?: 300
            var cachedIcon: Bitmap? = null
            if (metadata != null)
                cachedIcon = getArtwork(metadata!!, width, height, null, setAlbumBitmap)
            cachedIcon ?: albumBitmap
        }

        // Up next queue
        val upNextTitle: CharSequence? by nowPlayingPlaybackViewModel.queueTitle.observeAsState()
        nowPlaying.upNextTitle =
            if (hasAuthError) {
                ""
            } else {
                upNextTitle?.toString() ?: ""
            }
        val nextList: List<MediaItemMetadata>? by nowPlayingPlaybackViewModel.queue.observeAsState()
        nowPlaying.showUpNext =
            if (hasAuthError) {
                false
            } else {
                nextList?.isNotEmpty() ?: false
            }
        nowPlaying.upNextList =
            getBrowseLazyGridContent(
                nextList,
                {},
                { playController, item ->
                    if (item.queueId != null) playController?.skipToQueueItem(item.queueId!!)
                },
                media,
                false,
                nowPlayingPlaybackViewModel,
            )

        return nowPlaying
    }

    class MediaNowPlayingProgress {
        var currentTimeText: MutableState<String> = mutableStateOf("")
        var maxTimeText: MutableState<String> = mutableStateOf("")
        var progressWidth: MutableFloatState = mutableFloatStateOf(0F)
    }

    @Composable
    fun getNowPlayingProgress(): MediaNowPlayingProgress {
        val nowPlaying = MediaNowPlayingProgress()
        // Observe the current track progress
        val progress: PlaybackProgress? by nowPlayingPlaybackViewModel.progress.observeAsState()
        val maxProgress = progress?.maxProgress?.toFloat() ?: 0F
        nowPlaying.progressWidth.floatValue =
            if (maxProgress == 0F) 0F
            else (progress?.progress?.toFloat() ?: 0F) * 100F / maxProgress
        nowPlaying.currentTimeText.value = progress?.currentTimeText as String? ?: ""
        nowPlaying.maxTimeText.value = progress?.maxTimeText as String? ?: ""
        return nowPlaying
    }

    @Composable
    private fun getSourceButtons(
        sources: List<MediaSource>,
        primarySource: MediaSource?,
        media: CenterDisplayGen,
    ): ListContent {
        val getSourceButtonType = { index: Int ->
            val source = sources[index]
            if (source.browseServiceComponentName == primarySource?.browseServiceComponentName)
                SourceButtonType.Selected
            else SourceButtonType.Unselected
        }
        return { spanFunc ->
            ListContentData(
                count = sources.size,
                span = { index ->
                    spanFunc { media.SourceButtonDesignNodeData(getSourceButtonType(index)) }
                },
                key = { index -> sources[index].toString() },
            ) { index ->
                val source = sources[index]
                media.SourceButton(
                    modifier = Modifier,
                    onTap = {
                        mediaManagerHelper.setPrimaryMediaSource(source, MEDIA_SOURCE_MODE_BROWSE)
                    },
                    openLinkCallback = null,
                    sourceButtonType = getSourceButtonType(index),
                    title = source.getDisplayName(context) as String,
                    icon = source.croppedPackageIcon,
                    showResults = false,
                    numResults = "",
                    key = source.packageName,
                )
            }
        }
    }

    @Composable
    private fun getBrowseLazyGridContent(
        list: FutureData<List<MediaItemMetadata>>?,
        browseFunc: (MediaItemMetadata?) -> Unit,
        playFunc: (PlaybackViewModel.PlaybackController?, MediaItemMetadata) -> Unit,
        media: CenterDisplayGen,
        playbackViewModel: PlaybackViewModel,
        showSectionTitles: Boolean = true,
    ): ListContent {
        if (list == null) return { ListContentData { _ -> } }

        if (list.isLoading) {
            return { spanFunc ->
                ListContentData(
                    count = 1,
                    span = { spanFunc { media.LoadingPageDesignNodeData() } },
                ) {
                    media.LoadingPage(Modifier)
                }
            }
        }

        if (list.data == null) {
            return { ListContentData { _ -> } }
        }

        return getBrowseLazyGridContent(
            list.data,
            browseFunc,
            playFunc,
            media,
            showSectionTitles,
            playbackViewModel,
        )
    }

    @Composable
    private fun getBrowseLazyGridContent(
        list: List<MediaItemMetadata>?,
        browseFunc: (MediaItemMetadata?) -> Unit,
        playFunc: (PlaybackViewModel.PlaybackController?, MediaItemMetadata) -> Unit,
        media: CenterDisplayGen,
        showSectionTitles: Boolean = true,
        playbackViewModel: PlaybackViewModel,
    ): ListContent {
        if (list == null) return { ListContentData { _ -> } }
        val playbackController: PlaybackViewModel.PlaybackController? by
            playbackViewModel.playbackController.observeAsState()
        val metadata: MediaItemMetadata? by playbackViewModel.metadata.observeAsState()

        // Build out the groupGridType hash by iterating through the items and converting list type
        // items to grid types if there are a mix of list and grid types within a group
        val groupGridType: HashMap<String, Boolean> = HashMap()
        list.forEach {
            val group = if (it.titleGrouping == null) "" else it.titleGrouping

            // Continue if this group is already set to true
            if (groupGridType[group] == true) return@forEach

            // We want all items in a group to be the same type (list or grid). Using the browse
            // style
            // hints, sometimes we get a mix of grid and list items in the same group. In this
            // scenario,
            // we want to make all the items grid items, so we use groupGridType to keep track of
            // this.
            // We initialize entries in this hash to false, and set them to true if any item in the
            // group
            // is detected to be a grid item
            if (!groupGridType.containsKey(group)) groupGridType[group] = false

            // List item or grid item?
            var listType = true
            if (it.isBrowsable) {
                listType = false
                if (
                    it.browsableContentStyleHint != 0 &&
                        it.browsableContentStyleHint != CONTENT_STYLE_GRID_ITEM_HINT_VALUE &&
                        it.browsableContentStyleHint != CONTENT_STYLE_CATEGORY_GRID_ITEM_HINT_VALUE
                )
                    listType = true
            } else {
                if (
                    it.playableContentStyleHint == CONTENT_STYLE_GRID_ITEM_HINT_VALUE ||
                        it.playableContentStyleHint == CONTENT_STYLE_CATEGORY_GRID_ITEM_HINT_VALUE
                )
                    listType = false
            }

            if (!listType) groupGridType[group] = true
        }

        val itemKeyCount: HashMap<String, Int> = HashMap()
        var prevGroup = ""

        val itemData: ArrayList<ItemData> = arrayListOf()
        list.forEach {
            // Make a group header
            val group = if (it.titleGrouping == null) "" else it.titleGrouping
            if (showSectionTitles) {
                if (prevGroup != group) {
                    val groupItem = ItemData()
                    groupItem.nodeData = media.GroupHeaderDesignNodeData()
                    groupItem.composable = {
                        media.GroupHeader(
                            modifier = Modifier,
                            openLinkCallback = null,
                            title = group,
                        )
                    }
                    groupItem.key = group
                    itemData.add(groupItem)
                    prevGroup = group
                }
            }

            val item = ItemData()
            val currentlyPlaying =
                if (isMetadataSame(it, metadata)) CurrentlyPlaying.On else CurrentlyPlaying.Off
            item.nodeData =
                media.BrowseItemDesignNodeData(
                    browseType =
                        if (groupGridType[group]!!) BrowseItemType.Grid else BrowseItemType.List,
                    currentlyPlaying = currentlyPlaying,
                )
            item.composable = {
                // Use `key` to tell Compose not to try to morph one item into another
                // when processing an update. Compose will do this across multiple presented
                // frames, which leads to some very odd intermediate states (that our app
                // never presented!).
                key(it.id) {
                    // Make the item.
                    MediaBrowseItem(
                        it,
                        groupGridType[group]!!,
                        metadata,
                        onTap = {
                            if (it.isBrowsable) browseFunc(it) else playFunc(playbackController, it)
                        },
                        key = it.id,
                        media,
                    )
                }
            }

            // We can't have duplicate keys in the list, so keep count of how many times each key
            // has been
            // used. If it's been used before, then add a number to the end of the key in order to
            // ensure
            // uniqueness within the list.
            var key = it.id ?: ""
            var keyCount = itemKeyCount[key]
            if (keyCount == null) {
                itemKeyCount[key] = 1
            } else {
                itemKeyCount[key] = keyCount + 1
                key += keyCount.toString()
            }

            item.key = key
            itemData.add(item)
        }

        return { spanFunc ->
            // The lambda passed into the `span` parameter of the `items()` function below gets
            // called
            // very frequently as the gridview is scrolled, but this lambda (with the `spanFunc`
            // parameter) is not. Since each item's span doesn't change, we calculate them all and
            // store
            // them into an array for very fast access when the lambda is called.
            val spans = Array(itemData.size) { i -> spanFunc { itemData[i].nodeData } }
            ListContentData(
                count = itemData.size,
                span = { index -> spans[index] },
                key = { index -> itemData[index].key },
                initialSpan = { spanFunc { media.LoadingPageDesignNodeData() } },
                initialContent = { media.LoadingPage(Modifier) },
            ) { index ->
                itemData[index].composable()
            }
        }
    }

    @Composable
    fun getBrowse(media: CenterDisplayGen): MediaBrowse {
        val browse = MediaBrowse()

        // Observer the currently browsing media source
        val primarySource: MediaSource? = getBrowseMediaSource()

        // Get the browse source buttons
        val sources by mediaSourcesProvider.getMediaSources().observeAsState()
        browse.sourceButtons = getSourceButtons(sources!!, primarySource, media)

        // Setup the application settings intent
        val packageName = primarySource?.packageName
        if (packageName != null) {
            val prefsIntent = Intent(Intent.ACTION_APPLICATION_PREFERENCES)
            prefsIntent.setPackage(packageName)
            val info: ResolveInfo? = context.packageManager.resolveActivity(prefsIntent, 0)
            if (info?.activityInfo != null && info.activityInfo.exported) {
                settingsIntent =
                    Intent(Intent.ACTION_APPLICATION_PREFERENCES)
                        .setClassName(info.activityInfo.packageName, info.activityInfo.name)
                settingsIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        // The root items use a different browse view model than the actual content.
        val supportsSearch: Boolean? by canSearch.observeAsState()
        browse.supportsSearch = supportsSearch ?: false
        browse.showSettings = settingsIntent != null
        browse.onTapSettings = { if (settingsIntent != null) context.startActivity(settingsIntent) }
        val navItems: FutureData<List<MediaItemMetadata>>? by
            mediaRepo.rootMediaItems.observeAsState()

        if (navItems == null || navItems?.data == null || navItems?.isLoading == true) {
            browseStack.resetTo(null)
            browse.content = { spanFunc ->
                ListContentData(
                    count = 1,
                    span = { spanFunc { media.LoadingPageDesignNodeData() } },
                ) {
                    media.LoadingPage(Modifier)
                }
            }
            return browse
        }

        // If the currently selected root item isn't in this list, then reset to the first
        // item in this list.
        val navData = navItems!!.data
        var selectedRootItem = browseStack.selectedRootId()
        var selectionInList = false
        for (item: MediaItemMetadata in navData) {
            if (item.id == selectedRootItem) {
                selectionInList = true
                break
            }
        }
        if (!selectionInList && navData.isNotEmpty()) browseStack.resetTo(navData[0])

        // val pages: Stack<MediaBrowseStack.Page>? by browseStack.getPages().observeAsState()
        // Getting the top page from pages?.peek() doesn't work for some reason
        // val topPage = pages?.peek()
        val topPage: MediaBrowseStack.Page? by browseStack.getTopPage().observeAsState()
        if (topPage == null) return browse

        val itemList: FutureData<List<MediaItemMetadata>>? by
            browseStack.getTopItemList().observeAsState()

        val navItemData: ArrayList<ItemData> = arrayListOf()
        navData?.forEach {
            // Some sources show playable items in this view, which doesn't make a lot of sense. The
            // built in Media Center app does not show them, so here we hide them as well.
            if (it.isBrowsable) {
                val itemData = ItemData()
                val navButtonType =
                    if (it.id == browseStack.selectedRootId()) NavButtonType.Selected
                    else NavButtonType.Unselected
                itemData.nodeData = media.PageHeaderNavButtonDesignNodeData(navButtonType)
                itemData.composable = { MediaNavButton(it, browseStack, media) }
                navItemData.add(itemData)
            }
        }
        browse.navContent = {
            ListContentData(count = navItemData.size) { index -> navItemData[index].composable() }
        }

        val parent = topPage!!.parent
        val childPage = parent != null && browseStack.size() > 1
        browse.headerContent = {
            ListContentData(count = 1) {
                if (childPage) media.BrowseHeaderDrillDown(modifier = Modifier)
                else media.BrowseHeaderNav(modifier = Modifier)
            }
        }
        browse.title = parent?.title?.toString() ?: ""
        browse.onTapBack = { browseStack.navigateBack() }
        browse.content =
            getBrowseLazyGridContent(
                itemList,
                { item -> browseStack.browse(item) },
                { playController, item ->
                    run {
                        playController?.prepare()
                        playController?.playItem(item)
                    }
                },
                media,
                browsePlaybackViewModel,
            )

        return browse
    }

    @Composable
    fun getSearch(media: CenterDisplayGen): MediaSearch {
        var search = MediaSearch()

        val (query, setQuery) = remember { mutableStateOf("") }
        search.query = query
        search.setQuery = setQuery

        val searchFunc = remember {
            { query: String ->
                mediaRepo.setSearchQuery(query, Bundle.EMPTY)
                searchStack.resetToSearch()
            }
        }
        search.searchFunc = searchFunc

        val focusManager = LocalFocusManager.current
        search.searchField =
            @Composable { context: ComponentReplacementContext ->
                BasicTextField(
                    value = query,
                    onValueChange = {
                        searchFunc(it)
                        setQuery(it)
                    },
                    textStyle = context.textStyle ?: TextStyle.Default,
                    modifier =
                        context.layoutModifier.then(
                            Modifier.onKeyEvent {
                                if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                                    setQuery(query.substring(0, query.length - 1))
                                    focusManager.clearFocus()
                                    true
                                } else {
                                    false
                                }
                            }
                        ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )
            }

        val searchRootList: FutureData<List<MediaItemMetadata>>? by
            mediaRepo.searchMediaItems.observeAsState()
        val topPage: MediaBrowseStack.Page? by searchStack.getTopPage().observeAsState()
        val searchBrowseList: FutureData<List<MediaItemMetadata>>? by
            searchStack.getTopItemList().observeAsState()

        val parent = topPage?.parent
        val childPage = parent != null && searchStack.size() > 1
        var items: FutureData<List<MediaItemMetadata>>? = null
        if (query.isNotEmpty())
            items = if (searchStack.size() > 1) searchBrowseList else searchRootList

        search.showHeader = searchStack.size() > 1
        search.title = parent?.title?.toString() ?: ""
        search.onTapBack = { searchStack.navigateBack() }
        search.resultsContent =
            getBrowseLazyGridContent(
                items,
                { item -> searchStack.browse(item) },
                { playController, item ->
                    run {
                        playController?.prepare()
                        playController?.playItem(item)
                    }
                },
                media,
                browsePlaybackViewModel,
            )
        return search
    }
}
