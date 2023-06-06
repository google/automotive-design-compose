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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.DesignSettings
import com.android.designcompose.ImageReplacementContext
import com.android.designcompose.ListContent
import com.android.designcompose.MeterFunction
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignContentTypes
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignPreviewContent
import com.android.designcompose.annotation.DesignVariant
import com.android.designcompose.annotation.PreviewNode
import com.android.designcompose.reference.media.BrowseItemType
import com.android.designcompose.reference.media.CurrentlyPlaying
import com.android.designcompose.reference.media.MediaAdapter
import com.android.designcompose.reference.media.NavButtonType
import com.android.designcompose.reference.media.PlayState
import com.android.designcompose.reference.media.SourceButtonType

val notosansFont =
    FontFamily(
        Font(R.font.notosans_blackitalic, FontWeight.Black, FontStyle.Italic),
        Font(R.font.notosans_black, FontWeight.Black, FontStyle.Normal),
        Font(R.font.notosans_bolditalic, FontWeight.Bold, FontStyle.Italic),
        Font(R.font.notosans_bold, FontWeight.Bold, FontStyle.Normal),
        Font(R.font.notosans_extrabolditalic, FontWeight.ExtraBold, FontStyle.Italic),
        Font(R.font.notosans_extrabold, FontWeight.ExtraBold, FontStyle.Normal),
        Font(R.font.notosans_extralightitalic, FontWeight.ExtraLight, FontStyle.Italic),
        Font(R.font.notosans_extralight, FontWeight.ExtraLight, FontStyle.Normal),
        Font(R.font.notosans_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.notosans_lightitalic, FontWeight.Light, FontStyle.Italic),
        Font(R.font.notosans_light, FontWeight.Light, FontStyle.Normal),
        Font(R.font.notosans_mediumitalic, FontWeight.Medium, FontStyle.Italic),
        Font(R.font.notosans_medium, FontWeight.Medium, FontStyle.Normal),
        Font(R.font.notosans_regular, FontWeight.Normal, FontStyle.Normal),
        Font(R.font.notosans_semibolditalic, FontWeight.SemiBold, FontStyle.Italic),
        Font(R.font.notosans_semibold, FontWeight.SemiBold, FontStyle.Normal),
        Font(R.font.notosans_thinitalic, FontWeight.Thin, FontStyle.Italic),
        Font(R.font.notosans_thin, FontWeight.Thin, FontStyle.Normal),
    )

@DesignDoc(id = "7rvM6aVWe0jZCm7jhO9ITx", version = "0.1")
interface CenterDisplay : com.android.designcompose.reference.media.MediaInterface {
    @DesignComponent(node = "#stage", isRoot = true)
    fun MainFrame(
        // Now Playing
        @Design(node = "#media/now-playing/title") title: String,
        @Design(node = "#media/now-playing/subtitle") artist: String,
        @Design(node = "#media/now-playing/album") album: String,
        @Design(node = "#media/now-playing/artwork")
        albumArt: @Composable (ImageReplacementContext) -> Bitmap?,
        @Design(node = "#media/now-playing/source-icon") appIcon: Bitmap?,
        @Design(node = "#media/now-playing/source-name") appName: String,
        // These track progress customizations change frequently, so make sure their customizations
        // are functions so that we don't need to recompose the whole main frame
        @Design(node = "#media/now-playing/time-elapsed") timeElapsed: @Composable () -> String,
        @Design(node = "#media/now-playing/time-duration") timeDuration: @Composable () -> String,
        @Design(node = "#media/now-playing/progress-bar") progress: MeterFunction,

        // Playback controls
        @DesignVariant(property = "#media/now-playing/play-state-button") playState: PlayState,
        @Design(node = "#media/now-playing/play-state-button") onPlayPauseTap: TapCallback,
        @Design(node = "#media/now-playing/skip-prev-button") onPrevTap: TapCallback,
        @Design(node = "#media/now-playing/skip-prev-button") showPrev: Boolean,
        @Design(node = "#media/now-playing/skip-next-button") onNextTap: TapCallback,
        @Design(node = "#media/now-playing/skip-next-button") showNext: Boolean,
        @DesignContentTypes(nodes = ["#media/now-playing/custom-action-button"])
        @DesignPreviewContent(
            name = "Buttons",
            nodes = [PreviewNode(3, "#media/now-playing/custom-action-button")]
        )
        @Design(node = "#media/now-playing/custom-buttons/auto-content")
        customActions: ListContent,

        // Error frame and authentication button
        @Design(node = "#media/error/auto-content") showErrorFrame: Boolean,
        @DesignContentTypes(nodes = ["#media/error/frame"])
        @DesignPreviewContent(name = "Error Page", nodes = [PreviewNode(1, "#media/error/frame")])
        @Design(node = "#media/error/auto-content")
        errorFrameContents: @Composable (ComponentReplacementContext) -> Unit,

        // Browse source button
        @DesignContentTypes(nodes = ["#media/source-button"])
        @DesignPreviewContent(
            name = "Sources",
            nodes =
                [
                    PreviewNode(1, "#media/source-button=Unselected"),
                    PreviewNode(1, "#media/source-button=Selected"),
                    PreviewNode(3, "#media/source-button=Unselected")
                ]
        )
        @Design(node = "#media/browse/source-list/auto-content")
        browseSourceList: ListContent,

        // Browse page header, title and back buttoh
        @DesignContentTypes(nodes = ["#media/browse/header-drill-down", "#media/browse/header-nav"])
        @DesignPreviewContent(
            name = "Root Nav",
            nodes = [PreviewNode(1, "#media/browse/header-nav")]
        )
        @DesignPreviewContent(
            name = "Drill Down",
            nodes = [PreviewNode(1, "#media/browse/header-drill-down")]
        )
        @Design(node = "#media/browse/page-header")
        browseHeader: ListContent,
        @Design(node = "#media/browse/title") browseTitle: String,
        @Design(node = "#media/browse/back") onTapBackBrowse: TapCallback,

        // Browse content
        @DesignContentTypes(
            nodes = ["#media/browse/loading", "#media/browse/section-title", "#media/browse/item"]
        )
        @DesignPreviewContent(
            name = "Loading Page",
            nodes = [PreviewNode(1, "#media/browse/loading")]
        )
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#media/browse/section-title"),
                    PreviewNode(6, "#media/currently-playing=Off, #media/browse/item=Grid"),
                    PreviewNode(1, "#media/browse/section-title"),
                    PreviewNode(6, "#media/currently-playing=Off, #media/browse/item=Grid")
                ]
        )
        @DesignPreviewContent(
            name = "Album",
            nodes =
                [
                    PreviewNode(1, "#media/browse/section-title"),
                    PreviewNode(3, "#media/currently-playing=Off, #media/browse/item=List"),
                    PreviewNode(1, "#media/browse/currently-playing=On, #media/browse/item=List"),
                    PreviewNode(5, "#media/currently-playing=Off, #media/browse/item=List")
                ]
        )
        @Design(node = "#media/browse/auto-content")
        browseContent: ListContent,

        // Browse root level button list
        @DesignContentTypes(nodes = ["#media/page-header/nav-button"])
        @DesignPreviewContent(
            name = "Nav Buttons",
            nodes =
                [
                    PreviewNode(1, "#media/page-header/nav-button=Unselected"),
                    PreviewNode(1, "#media/page-header/nav-button=Selected"),
                    PreviewNode(2, "#media/page-header/nav-button=Unselected")
                ]
        )
        @Design(node = "#nav/auto-content")
        nav: ListContent,

        // Up next queue
        @Design(node = "#media/up-next/title") upNextTitle: String,
        @DesignContentTypes(
            nodes = ["#media/browse/loading", "#media/browse/section-title", "#media/browse/item"]
        )
        @DesignPreviewContent(
            name = "Loading Page",
            nodes = [PreviewNode(1, "#media/browse/loading")]
        )
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#media/browse/section-title"),
                    PreviewNode(6, "#media/currently-playing=Off, #media/browse/item=Grid"),
                    PreviewNode(1, "#media/browse/section-title"),
                    PreviewNode(6, "#media/currently-playing=Off, #media/browse/item=Grid")
                ]
        )
        @DesignPreviewContent(
            name = "Album",
            nodes =
                [
                    PreviewNode(1, "#media/browse/section-title"),
                    PreviewNode(3, "#media/currently-playing=Off, #media/browse/item=List"),
                    PreviewNode(1, "#media/browse/currently-playing=On, #media/browse/item=List"),
                    PreviewNode(5, "#media/currently-playing=Off, #media/browse/item=List")
                ]
        )
        @Design(node = "#media/up-next/auto-content")
        upNextList: ListContent,
        @Design(node = "#media/up-next-button") showUpNext: Boolean,

        // All settings/search buttons in the document
        @Design(node = "#media/search-button") showSearch: Boolean,
        @Design(node = "#media/settings-button") showSettings: Boolean,
        @Design(node = "#media/settings-button") onTapSettings: TapCallback,

        // Search overlay
        @Design(node = "#media/search/text-edit")
        searchField: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#media/search/help-text") showSearchHelp: Boolean,
        @Design(node = "#media/search/clear-button") onClearSearch: TapCallback,
        @Design(node = "#media/search/clear-button") showClearSearch: Boolean,

        // Search page header, title and back button
        @Design(node = "#media/search/page-header") searchShowHeader: Boolean,
        @Design(node = "#media/search/title") searchTitle: String,
        @Design(node = "#media/search/back") onTapBackSearch: TapCallback,

        // Search results content
        @DesignContentTypes(
            nodes = ["#media/browse/loading", "#media/browse/section-title", "#media/browse/item"]
        )
        @DesignPreviewContent(
            name = "Loading Page",
            nodes = [PreviewNode(1, "#media/browse/loading")]
        )
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#media/browse/section-title"),
                    PreviewNode(6, "#media/currently-playing=Off, #media/browse/item=Grid"),
                    PreviewNode(1, "#media/browse/section-title"),
                    PreviewNode(6, "#media/currently-playing=Off, #media/browse/item=Grid")
                ]
        )
        @DesignPreviewContent(
            name = "Album",
            nodes =
                [
                    PreviewNode(1, "#media/browse/section-title"),
                    PreviewNode(3, "#media/currently-playing=Off, #media/browse/item=List"),
                    PreviewNode(1, "#media/browse/currently-playing=On, #media/browse/item=List"),
                    PreviewNode(5, "#media/currently-playing=Off, #media/browse/item=List")
                ]
        )
        @Design(node = "#media/search/auto-content")
        searchResultsContent: ListContent,
    )
    @DesignComponent(node = "#media/now-playing/custom-action-button", override = true)
    fun CustomActionButton(
        @Design(node = "#icon") icon: @Composable (ImageReplacementContext) -> Bitmap?,
        @Design(node = "#media/now-playing/custom-action-button") onTap: TapCallback
    )
    @DesignComponent(node = "#media/browse/loading", override = true) fun LoadingPage()
    @DesignComponent(node = "#media/browse/section-title", override = true)
    fun GroupHeader(@Design(node = "#title") title: String)
    @DesignComponent(node = "#media/source-button", override = true)
    fun SourceButton(
        @DesignVariant(property = "#media/source-button") sourceButtonType: SourceButtonType,
        @Design(node = "#media/source-button") onTap: TapCallback,
        @Design(node = "#title") title: String,
        @Design(node = "#icon") icon: Bitmap?,
        @Design(node = "#num-results-container") showResults: Boolean,
        @Design(node = "#num-results") numResults: String,
    )
    @DesignComponent(node = "#media/page-header/nav-button", override = true)
    fun PageHeaderNavButton(
        @DesignVariant(property = "#media/page-header/nav-button") navButtonType: NavButtonType,
        @Design(node = "#media/page-header/nav-button") onTap: TapCallback,
        @Design(node = "#name") name: String,
        @Design(node = "#icon") icon: @Composable (ImageReplacementContext) -> Bitmap?,
    )
    @DesignComponent(node = "#media/browse/item", override = true)
    fun BrowseItem(
        @DesignVariant(property = "#media/browse/item") browseType: BrowseItemType,
        @DesignVariant(property = "#media/currently-playing") currentlyPlaying: CurrentlyPlaying,
        @Design(node = "#media/browse/item") onTap: TapCallback,
        @Design(node = "#title") title: String,
        @Design(node = "#subtitle") subtitle: String,
        @Design(node = "#subtitle") showSubtitle: Boolean,
        @Design(node = "#icon") icon: @Composable (ImageReplacementContext) -> Bitmap?,
    )
    @DesignComponent(node = "#media/error/frame", override = true)
    fun ErrorFrame(
        @Design(node = "#media/error/message") errorMessage: String,
        @Design(node = "#media/error/button-text") errorButtonText: String,
        @Design(node = "#media/error/button") showErrorButton: Boolean,
        @Design(node = "#media/error/button") onTapErrorButton: TapCallback
    )
    @DesignComponent(node = "#media/browse/header-nav", override = true) fun BrowseHeaderNav()
    @DesignComponent(node = "#media/browse/header-drill-down", override = true)
    fun BrowseHeaderDrillDown()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignSettings.enableLiveUpdates(this)
        DesignSettings.addFontFamily("Inter", notosansFont)

        setContent { MainFrame() }
    }

    @Composable
    fun MainFrame() {
        val mediaAdapter = remember { MediaAdapter(application, applicationContext, this) }

        val nowPlaying = mediaAdapter.getNowPlaying(CenterDisplayDoc)
        val mediaSource = mediaAdapter.getMediaSource()
        val browse = mediaAdapter.getBrowse(CenterDisplayDoc)
        val search = mediaAdapter.getSearch(CenterDisplayDoc)

        CenterDisplayDoc.MainFrame(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            title = nowPlaying.title,
            artist = nowPlaying.artist,
            album = nowPlaying.album,
            albumArt = nowPlaying.albumArt,
            appIcon = mediaSource?.croppedPackageIcon,
            appName = mediaSource?.displayName as String,
            timeElapsed = { mediaAdapter.getNowPlayingProgress().currentTimeText },
            timeDuration = { mediaAdapter.getNowPlayingProgress().maxTimeText },
            progress = { mediaAdapter.getNowPlayingProgress().progressWidth * 100F },
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
            upNextTitle = nowPlaying.upNextTitle,
            upNextList = nowPlaying.upNextList,
            showUpNext = nowPlaying.showUpNext,
            showErrorFrame = nowPlaying.hasError,
            errorFrameContents = nowPlaying.errorFrame,
            browseSourceList = browse.sourceButtons,
            browseHeader = browse.headerContent,
            browseTitle = browse.title,
            onTapBackBrowse = browse.onTapBack,
            browseContent = browse.content,
            nav = browse.navContent,
            showSearch = browse.supportsSearch,
            showSettings = browse.showSettings,
            onTapSettings = browse.onTapSettings,
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
    }
}
