/*
 * Copyright 2024 Google LLC
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
import androidx.compose.runtime.Composable
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.ImageReplacementContext
import com.android.designcompose.ListContent
import com.android.designcompose.MeterFunction
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.DesignContentTypesProperty
import com.android.designcompose.annotation.DesignModuleClass
import com.android.designcompose.annotation.DesignModuleProperty
import com.android.designcompose.annotation.DesignPreviewContentProperty
import com.android.designcompose.annotation.DesignProperty
import com.android.designcompose.annotation.DesignVariantProperty
import com.android.designcompose.annotation.PreviewNode

@DesignModuleClass
class NowPlayingModule(
    @DesignProperty(node = "#media/now-playing/title") val title: String,
    @DesignProperty(node = "#media/now-playing/subtitle") val artist: String,
    @DesignProperty(node = "#media/now-playing/album") val album: String,
    @DesignProperty(node = "#media/now-playing/artwork")
    val albumArt: @Composable (ImageReplacementContext) -> Bitmap?,
    @DesignProperty(node = "#media/now-playing/source-icon") val appIcon: Bitmap?,
    @DesignProperty(node = "#media/now-playing/source-name") val appName: String,
    @DesignModuleProperty val nowPlayingProgressBarModule: NowPlayingProgressBarModule,
)

@DesignModuleClass
class NowPlayingProgressBarModule(
    // These track progress customizations change frequently, so make sure their customizations
    // are functions so that we don't need to recompose the whole main frame
    @DesignProperty(node = "#media/now-playing/time-elapsed")
    val timeElapsed: @Composable () -> String,
    @DesignProperty(node = "#media/now-playing/time-duration")
    val timeDuration: @Composable () -> String,
    @DesignProperty(node = "#media/now-playing/progress-bar") val progress: MeterFunction,
)

@DesignModuleClass
class PlaybackControlsModule(
    @DesignVariantProperty(property = "#media/now-playing/play-state-button")
    val playState: PlayState,
    @DesignProperty(node = "#media/now-playing/play-state-button") val onPlayPauseTap: TapCallback,
    @DesignProperty(node = "#media/now-playing/skip-prev-button") val onPrevTap: TapCallback,
    @DesignProperty(node = "#media/now-playing/skip-prev-button") val showPrev: Boolean,
    @DesignProperty(node = "#media/now-playing/skip-next-button") val onNextTap: TapCallback,
    @DesignProperty(node = "#media/now-playing/skip-next-button") val showNext: Boolean,
    @DesignContentTypesProperty(nodes = ["#media/now-playing/custom-action-button"])
    @DesignPreviewContentProperty(
        name = "Buttons",
        nodes = [PreviewNode(3, "#media/now-playing/custom-action-button")]
    )
    @DesignProperty(node = "#media/now-playing/custom-buttons/auto-content")
    val customActions: ListContent,
)

@DesignModuleClass
class ErrorFrameModule(
    @DesignProperty(node = "#media/error/auto-content") val showErrorFrame: Boolean,
    @DesignContentTypesProperty(nodes = ["#media/error/frame"])
    @DesignPreviewContentProperty(
        name = "Error Page",
        nodes = [PreviewNode(1, "#media/error/frame")]
    )
    @DesignProperty(node = "#media/error/auto-content")
    val errorFrameContents: @Composable (ComponentReplacementContext) -> Unit,
)

@DesignModuleClass
class BrowseSourceButtonModule(
    @DesignContentTypesProperty(nodes = ["#media/source-button"])
    @DesignPreviewContentProperty(
        name = "Sources",
        nodes =
            [
                PreviewNode(1, "#media/source-button=Unselected"),
                PreviewNode(1, "#media/source-button=Selected"),
                PreviewNode(3, "#media/source-button=Unselected")
            ]
    )
    @DesignProperty(node = "#media/browse/source-list/auto-content")
    val browseSourceList: ListContent,
)

@DesignModuleClass
class BrowsePageHeaderModule(
    @DesignContentTypesProperty(
        nodes = ["#media/browse/header-drill-down", "#media/browse/header-nav"]
    )
    @DesignPreviewContentProperty(
        name = "Root Nav",
        nodes = [PreviewNode(1, "#media/browse/header-nav")]
    )
    @DesignPreviewContentProperty(
        name = "Drill Down",
        nodes = [PreviewNode(1, "#media/browse/header-drill-down")]
    )
    @DesignProperty(node = "#media/browse/page-header")
    val browseHeader: ListContent,
    @DesignProperty(node = "#media/browse/title") val browseTitle: String,
    @DesignProperty(node = "#media/browse/back") val onTapBackBrowse: TapCallback,
)

@DesignModuleClass
class BrowseContentModule(
    @DesignContentTypesProperty(
        nodes = ["#media/browse/loading", "#media/browse/section-title", "#media/browse/item"]
    )
    @DesignPreviewContentProperty(
        name = "Loading Page",
        nodes = [PreviewNode(1, "#media/browse/loading")]
    )
    @DesignPreviewContentProperty(
        name = "Browse",
        nodes =
            [
                PreviewNode(1, "#media/browse/section-title"),
                PreviewNode(6, "#media/currently-playing=Off, #media/browse/item=Grid"),
                PreviewNode(1, "#media/browse/section-title"),
                PreviewNode(6, "#media/currently-playing=Off, #media/browse/item=Grid")
            ]
    )
    @DesignPreviewContentProperty(
        name = "Album",
        nodes =
            [
                PreviewNode(1, "#media/browse/section-title"),
                PreviewNode(3, "#media/currently-playing=Off, #media/browse/item=List"),
                PreviewNode(1, "#media/browse/currently-playing=On, #media/browse/item=List"),
                PreviewNode(5, "#media/currently-playing=Off, #media/browse/item=List")
            ]
    )
    @DesignProperty(node = "#media/browse/auto-content")
    val browseContent: ListContent,
)

@DesignModuleClass
class BrowsePageHeaderNavButtonsModule(
    @DesignContentTypesProperty(nodes = ["#media/page-header/nav-button"])
    @DesignPreviewContentProperty(
        name = "Nav Buttons",
        nodes =
            [
                PreviewNode(1, "#media/page-header/nav-button=Unselected"),
                PreviewNode(1, "#media/page-header/nav-button=Selected"),
                PreviewNode(2, "#media/page-header/nav-button=Unselected")
            ]
    )
    @DesignProperty(node = "#nav/auto-content")
    val nav: ListContent,
)

@DesignModuleClass
class UpNextQueueModule(
    @DesignProperty(node = "#media/up-next/title") val upNextTitle: String,
    @DesignContentTypesProperty(
        nodes = ["#media/browse/loading", "#media/browse/section-title", "#media/browse/item"]
    )
    @DesignPreviewContentProperty(
        name = "Loading Page",
        nodes = [PreviewNode(1, "#media/browse/loading")]
    )
    @DesignPreviewContentProperty(
        name = "Browse",
        nodes =
            [
                PreviewNode(1, "#media/browse/section-title"),
                PreviewNode(6, "#media/currently-playing=Off, #media/browse/item=Grid"),
                PreviewNode(1, "#media/browse/section-title"),
                PreviewNode(6, "#media/currently-playing=Off, #media/browse/item=Grid")
            ]
    )
    @DesignPreviewContentProperty(
        name = "Album",
        nodes =
            [
                PreviewNode(1, "#media/browse/section-title"),
                PreviewNode(3, "#media/currently-playing=Off, #media/browse/item=List"),
                PreviewNode(1, "#media/browse/currently-playing=On, #media/browse/item=List"),
                PreviewNode(5, "#media/currently-playing=Off, #media/browse/item=List")
            ]
    )
    @DesignProperty(node = "#media/up-next/auto-content")
    val upNextList: ListContent,
    @DesignProperty(node = "#media/up-next-button") val showUpNext: Boolean,
)

@DesignModuleClass
class MenuBarModule(
    @DesignProperty(node = "#media/search-button") val showSearch: Boolean,
    @DesignProperty(node = "#media/settings-button") val showSettings: Boolean,
    @DesignProperty(node = "#media/settings-button") val onTapSettings: TapCallback,
)

@DesignModuleClass
class SearchOverlayModule(
    @DesignProperty(node = "#media/search/text-edit")
    val searchField: @Composable (ComponentReplacementContext) -> Unit,
    @DesignProperty(node = "#media/search/help-text") val showSearchHelp: Boolean,
    @DesignProperty(node = "#media/search/clear-button") val onClearSearch: TapCallback,
    @DesignProperty(node = "#media/search/clear-button") val showClearSearch: Boolean,

    // Search page header, title and back button
    @DesignProperty(node = "#media/search/page-header") val searchShowHeader: Boolean,
    @DesignProperty(node = "#media/search/title") val searchTitle: String,
    @DesignProperty(node = "#media/search/back") val onTapBackSearch: TapCallback,

    // Search results content
    @DesignContentTypesProperty(
        nodes = ["#media/browse/loading", "#media/browse/section-title", "#media/browse/item"]
    )
    @DesignPreviewContentProperty(
        name = "Loading Page",
        nodes = [PreviewNode(1, "#media/browse/loading")]
    )
    @DesignPreviewContentProperty(
        name = "Browse",
        nodes =
            [
                PreviewNode(1, "#media/browse/section-title"),
                PreviewNode(6, "#media/currently-playing=Off, #media/browse/item=Grid"),
                PreviewNode(1, "#media/browse/section-title"),
                PreviewNode(6, "#media/currently-playing=Off, #media/browse/item=Grid")
            ]
    )
    @DesignPreviewContentProperty(
        name = "Album",
        nodes =
            [
                PreviewNode(1, "#media/browse/section-title"),
                PreviewNode(3, "#media/currently-playing=Off, #media/browse/item=List"),
                PreviewNode(1, "#media/browse/currently-playing=On, #media/browse/item=List"),
                PreviewNode(5, "#media/currently-playing=Off, #media/browse/item=List")
            ]
    )
    @DesignProperty(node = "#media/search/auto-content")
    val searchResultsContent: ListContent,
)
