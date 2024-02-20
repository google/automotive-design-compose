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

package com.android.designcompose.reference.media

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.designcompose.DesignComposeCallbacks
import com.android.designcompose.DesignNodeData
import com.android.designcompose.ImageReplacementContext
import com.android.designcompose.ListContent
import com.android.designcompose.OpenLinkCallback
import com.android.designcompose.ParentLayoutInfo
import com.android.designcompose.TapCallback

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

interface MediaInterface {
    @Composable
    fun CustomActionButton(
        modifier: Modifier = Modifier,
        openLinkCallback: OpenLinkCallback? = null,
        designComposeCallbacks: DesignComposeCallbacks? = null,
        key: String? = null,
        parentLayout: ParentLayoutInfo?,
        icon: @Composable (ImageReplacementContext) -> Bitmap?,
        onTap: TapCallback,
    ) {}

    fun CustomActionButtonDesignNodeData(): DesignNodeData {
        return DesignNodeData()
    }

    @Composable
    fun BrowsePage(
        modifier: Modifier = Modifier,
        openLinkCallback: OpenLinkCallback? = null,
        designComposeCallbacks: DesignComposeCallbacks? = null,
        key: String? = null,
        parentLayout: ParentLayoutInfo?,
        pageHeaderType: PageHeaderType,
        showHeader: Boolean,
        title: String,
        onTapBack: TapCallback,
        browseContent: ListContent,
        show: Boolean,
    ) {}

    fun BrowsePageDesignNodeData(pageHeaderType: PageHeaderType): DesignNodeData {
        return DesignNodeData()
    }

    @Composable
    fun LoadingPage(
        modifier: Modifier = Modifier,
        openLinkCallback: OpenLinkCallback? = null,
        designComposeCallbacks: DesignComposeCallbacks? = null,
        key: String? = null,
        parentLayout: ParentLayoutInfo?,
    ) {}

    fun LoadingPageDesignNodeData(): DesignNodeData {
        return DesignNodeData()
    }

    @Composable
    fun GroupHeader(
        modifier: Modifier = Modifier,
        openLinkCallback: OpenLinkCallback? = null,
        designComposeCallbacks: DesignComposeCallbacks? = null,
        key: String? = null,
        parentLayout: ParentLayoutInfo?,
        title: String,
    ) {}

    fun GroupHeaderDesignNodeData(): DesignNodeData {
        return DesignNodeData()
    }

    @Composable
    fun SourceButton(
        modifier: Modifier = Modifier,
        openLinkCallback: OpenLinkCallback? = null,
        designComposeCallbacks: DesignComposeCallbacks? = null,
        key: String? = null,
        parentLayout: ParentLayoutInfo?,
        sourceButtonType: SourceButtonType,
        onTap: TapCallback,
        title: String,
        icon: Bitmap?,
        showResults: Boolean,
        numResults: String,
    ) {}

    fun SourceButtonDesignNodeData(sourceButtonType: SourceButtonType): DesignNodeData {
        return DesignNodeData()
    }

    @Composable
    fun PageHeaderNavButton(
        modifier: Modifier = Modifier,
        openLinkCallback: OpenLinkCallback? = null,
        designComposeCallbacks: DesignComposeCallbacks? = null,
        key: String? = null,
        parentLayout: ParentLayoutInfo?,
        navButtonType: NavButtonType,
        onTap: TapCallback,
        name: String,
        icon: @Composable (ImageReplacementContext) -> Bitmap?,
    ) {}

    fun PageHeaderNavButtonDesignNodeData(navButtonType: NavButtonType): DesignNodeData {
        return DesignNodeData()
    }

    @Composable
    fun BrowseItem(
        modifier: Modifier = Modifier,
        openLinkCallback: OpenLinkCallback? = null,
        designComposeCallbacks: DesignComposeCallbacks? = null,
        key: String? = null,
        parentLayout: ParentLayoutInfo?,
        browseType: BrowseItemType,
        currentlyPlaying: CurrentlyPlaying,
        onTap: TapCallback,
        title: String,
        subtitle: String,
        showSubtitle: Boolean,
        icon: @Composable (ImageReplacementContext) -> Bitmap?,
    ) {}

    fun BrowseItemDesignNodeData(
        browseType: BrowseItemType,
        currentlyPlaying: CurrentlyPlaying,
    ): DesignNodeData {
        return DesignNodeData()
    }

    @Composable
    fun ErrorFrame(
        modifier: Modifier = Modifier,
        openLinkCallback: OpenLinkCallback? = null,
        designComposeCallbacks: DesignComposeCallbacks? = null,
        key: String? = null,
        parentLayout: ParentLayoutInfo?,
        errorMessage: String,
        errorButtonText: String,
        showErrorButton: Boolean,
        onTapErrorButton: TapCallback,
    ) {}

    fun ErrorFrameDesignNodeData(): DesignNodeData {
        return DesignNodeData()
    }

    @Composable
    fun BrowseHeaderNav(
        modifier: Modifier = Modifier,
        openLinkCallback: OpenLinkCallback? = null,
        designComposeCallbacks: DesignComposeCallbacks? = null,
        key: String? = null,
        parentLayout: ParentLayoutInfo?,
    ) {}

    fun BrowseHeaderNavDesignNodeData(): DesignNodeData {
        return DesignNodeData()
    }

    @Composable
    fun BrowseHeaderDrillDown(
        modifier: Modifier = Modifier,
        openLinkCallback: OpenLinkCallback? = null,
        designComposeCallbacks: DesignComposeCallbacks? = null,
        key: String? = null,
        parentLayout: ParentLayoutInfo?,
    ) {}

    fun BrowseHeaderDrillDownDesignNodeData(): DesignNodeData {
        return DesignNodeData()
    }
}
