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

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.android.designcompose.DesignSettings
import com.android.designcompose.ImageReplacementContext
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignModule
import com.android.designcompose.annotation.DesignVariant

enum class PlayState {
    Play,
    Pause
}

enum class BrowseItemType {
    Grid,
    List
}

enum class CurrentlyPlaying {
    On,
    Off
}

enum class PageHeaderType {
    Nav,
    DrillDown
}

enum class SourceButtonType {
    Selected,
    Unselected
}

enum class NavButtonType {
    Selected,
    Unselected
}

// Media 1 7rvM6aVWe0jZCm7jhO9ITx
// Media 2 S3n4mhNgoHzNxCCHhmrVcR
// Media 4 5n0LhOQ6wOiDxrH0YUVhJS
// Media 5 dui99iAKZ273s7RN11Z9Ak
// Media Nova 2DQtQOf6U26mA8dqBie3gT
@DesignDoc(id = "7rvM6aVWe0jZCm7jhO9ITx", customizationInterfaceVersion = "0.1")
interface CenterDisplay {
    @DesignComponent(node = "#stage", isRoot = true)
    fun MainFrame(
        // Now Playing
        @DesignModule nowPlayingModule: NowPlayingModule,

        // Playback controls
        @DesignModule playbackControlsModule: PlaybackControlsModule,

        // Error frame and authentication button
        @DesignModule errorFrameModule: ErrorFrameModule,

        // Browse source button
        @DesignModule browseSourceButtonModule: BrowseSourceButtonModule,

        // Browse page header, title and back button
        @DesignModule browsePageHeaderModule: BrowsePageHeaderModule,

        // Browse content
        @DesignModule browseContentModule: BrowseContentModule,

        // Browse root level button list
        @DesignModule browsePageHeaderNavButtonsModule: BrowsePageHeaderNavButtonsModule,

        // Up next queue
        @DesignModule upNextQueueModule: UpNextQueueModule,

        // All settings/search buttons in the document
        @DesignModule menuBarModule: MenuBarModule,

        // Search overlay
        @DesignModule searchOverlayModule: SearchOverlayModule,
    )

    @DesignComponent(node = "#media/now-playing/custom-action-button")
    fun CustomActionButton(
        @Design(node = "#icon") icon: @Composable (ImageReplacementContext) -> Bitmap?,
        @Design(node = "#media/now-playing/custom-action-button") onTap: TapCallback
    )

    @DesignComponent(node = "#media/browse/loading") fun LoadingPage()

    @DesignComponent(node = "#media/browse/section-title")
    fun GroupHeader(@Design(node = "#title") title: String)

    @DesignComponent(node = "#media/source-button")
    fun SourceButton(
        @DesignVariant(property = "#media/source-button") sourceButtonType: SourceButtonType,
        @Design(node = "#media/source-button") onTap: TapCallback,
        @Design(node = "#title") title: String,
        @Design(node = "#icon") icon: Bitmap?,
        @Design(node = "#num-results-container") showResults: Boolean,
        @Design(node = "#num-results") numResults: String,
    )

    @DesignComponent(node = "#media/page-header/nav-button")
    fun PageHeaderNavButton(
        @DesignVariant(property = "#media/page-header/nav-button") navButtonType: NavButtonType,
        @Design(node = "#media/page-header/nav-button") onTap: TapCallback,
        @Design(node = "#name") name: String,
        @Design(node = "#icon") icon: @Composable (ImageReplacementContext) -> Bitmap?,
    )

    @DesignComponent(node = "#media/browse/item")
    fun BrowseItem(
        @DesignVariant(property = "#media/browse/item") browseType: BrowseItemType,
        @DesignVariant(property = "#media/currently-playing") currentlyPlaying: CurrentlyPlaying,
        @Design(node = "#media/browse/item") onTap: TapCallback,
        @Design(node = "#title") title: String,
        @Design(node = "#subtitle") subtitle: String,
        @Design(node = "#subtitle") showSubtitle: Boolean,
        @Design(node = "#icon") icon: @Composable (ImageReplacementContext) -> Bitmap?,
    )

    @DesignComponent(node = "#media/error/frame")
    fun ErrorFrame(
        @Design(node = "#media/error/message") errorMessage: String,
        @Design(node = "#media/error/button-text") errorButtonText: String,
        @Design(node = "#media/error/button") showErrorButton: Boolean,
        @Design(node = "#media/error/button") onTapErrorButton: TapCallback
    )

    @DesignComponent(node = "#media/browse/header-nav") fun BrowseHeaderNav()

    @DesignComponent(node = "#media/browse/header-drill-down") fun BrowseHeaderDrillDown()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignSettings.enableLiveUpdates(this)
        DesignSettings.addFontFamily(name = "Inter", family = NotosansFont)

        setContent { MainFrame() }
    }

    @Composable
    fun MainFrame() {
        val mediaAdapter = remember { MediaAdapter(application, applicationContext, this) }

        val nowPlaying = mediaAdapter.getNowPlaying(CenterDisplayDoc)
        val nowPlayingMediaSource = mediaAdapter.getNowPlayingMediaSource()
        val browseMediaSource = mediaAdapter.getBrowseMediaSource()
        val browse = mediaAdapter.getBrowse(CenterDisplayDoc)
        val search = mediaAdapter.getSearch(CenterDisplayDoc)

        val nowPlayingProgressBarModule =
            NowPlayingProgressBarModule(
                timeElapsed = { mediaAdapter.getNowPlayingProgress().currentTimeText },
                timeDuration = { mediaAdapter.getNowPlayingProgress().maxTimeText },
                progress = { mediaAdapter.getNowPlayingProgress().progressWidth * 100F }
            )
        val nowPlayingModule =
            NowPlayingModule(
                title = nowPlaying.title,
                artist = nowPlaying.artist,
                album = nowPlaying.album,
                albumArt = nowPlaying.albumArt,
                appIcon = nowPlayingMediaSource?.croppedPackageIcon,
                appName = nowPlayingMediaSource?.getDisplayName(applicationContext) as String,
                nowPlayingProgressBarModule = nowPlayingProgressBarModule,
            )
        val playbackControlsModule =
            PlaybackControlsModule(
                playState = if (nowPlaying.showPlay) PlayState.Play else PlayState.Pause,
                onPlayPauseTap = {
                    if (nowPlaying.showPlay) nowPlaying.playController?.play()
                    else nowPlaying.playController?.pause()
                },
                onPrevTap = { nowPlaying.playController?.skipToPrevious() },
                onNextTap = { nowPlaying.playController?.skipToNext() },
                showPrev = nowPlaying.showPrev,
                showNext = nowPlaying.showNext,
                customActions = nowPlaying.customActions,
            )
        val errorFrameModule =
            ErrorFrameModule(
                showErrorFrame = nowPlaying.hasError,
                errorFrameContents = nowPlaying.errorFrame,
            )
        val browseSourceButtonModule =
            BrowseSourceButtonModule(
                browseAppIcon = browseMediaSource?.croppedPackageIcon,
                browseAppName = browseMediaSource?.getDisplayName(application) as String,
                browseSourceList = browse.sourceButtons,
            )
        val browsePageHeaderModule =
            BrowsePageHeaderModule(
                browseHeader = browse.headerContent,
                browseTitle = browse.title,
                onTapBackBrowse = browse.onTapBack,
            )
        val browseContentModule =
            BrowseContentModule(
                browseContent = browse.content,
            )
        val browsePageHeaderNavButtonsModule =
            BrowsePageHeaderNavButtonsModule(
                nav = browse.navContent,
            )
        val upNextQueueModule =
            UpNextQueueModule(
                upNextTitle = nowPlaying.upNextTitle,
                upNextList = nowPlaying.upNextList,
                showUpNext = nowPlaying.showUpNext,
            )
        val menuBarModule =
            MenuBarModule(
                showSearch = browse.supportsSearch,
                showSettings = browse.showSettings,
                onTapSettings = browse.onTapSettings,
            )
        val searchOverlayModule =
            SearchOverlayModule(
                searchField = search.searchField,
                showSearchHelp = search.query.isEmpty(),
                onClearSearch = {
                    search.searchFunc("")
                    search.setQuery("")
                },
                showClearSearch = search.query.isNotEmpty(),
                searchShowHeader = search.showHeader,
                searchTitle = search.title,
                onTapBackSearch = search.onTapBack,
                searchResultsContent = search.resultsContent,
            )

        CenterDisplayDoc.MainFrame(
            modifier = Modifier.fillMaxSize(),
            nowPlayingModule = nowPlayingModule,
            playbackControlsModule = playbackControlsModule,
            errorFrameModule = errorFrameModule,
            browseSourceButtonModule = browseSourceButtonModule,
            browsePageHeaderModule = browsePageHeaderModule,
            browseContentModule = browseContentModule,
            browsePageHeaderNavButtonsModule = browsePageHeaderNavButtonsModule,
            upNextQueueModule = upNextQueueModule,
            menuBarModule = menuBarModule,
            searchOverlayModule = searchOverlayModule,
        )
    }
}
