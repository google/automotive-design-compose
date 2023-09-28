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

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.designcompose.serdegen.FrameExtras
import com.android.designcompose.serdegen.OverlayBackgroundInteraction
import com.android.designcompose.serdegen.OverlayPositionType

@Composable
internal fun DesignOverlay(
    overlay: FrameExtras,
    interactionState: InteractionState,
    content: @Composable () -> Unit
) {
    val alignment =
        when (overlay.overlayPositionType) {
            is OverlayPositionType.TOP_LEFT -> Alignment.TopStart
            is OverlayPositionType.TOP_CENTER -> Alignment.TopCenter
            is OverlayPositionType.TOP_RIGHT -> Alignment.TopEnd
            is OverlayPositionType.CENTER -> Alignment.Center
            is OverlayPositionType.BOTTOM_LEFT -> Alignment.BottomStart
            is OverlayPositionType.BOTTOM_CENTER -> Alignment.BottomCenter
            is OverlayPositionType.BOTTOM_RIGHT -> Alignment.BottomEnd
            else -> Alignment.TopStart
        }
    val closeOnTapOutside =
        overlay.overlayBackgroundInteraction is OverlayBackgroundInteraction.CLOSE_ON_CLICK_OUTSIDE
    var boxModifier =
        Modifier.fillMaxSize().clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            // Always be clickable, because it also prevents taps and drags from going thru to views
            // behind the overlay background.
            if (closeOnTapOutside) interactionState.close(null)
        }
    overlay.overlayBackground.color.ifPresent { color ->
        boxModifier = boxModifier.background(Color(color.r, color.g, color.b, color.a))
    }

    DisposableEffect(Unit) {
        // Similar to a root view, tell the layout manager to defer layout computations until all
        // child views have been added to the overlay
        LayoutManager.deferComputations()
        Log.d(TAG, "Overlay start")
        onDispose {}
    }
    Box(boxModifier, contentAlignment = alignment) { content() }
    DisposableEffect(Unit) {
        // Similar to a root view, tell the layout manager to that child views have been added so
        // that layout can be computed
        LayoutManager.resumeComputations()
        Log.d(TAG, "Overlay end")
        onDispose {}
    }
}
