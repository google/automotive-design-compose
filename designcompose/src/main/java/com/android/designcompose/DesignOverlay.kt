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
import com.android.designcompose.proto.OverlayBackgroundInteractionEnum
import com.android.designcompose.proto.OverlayPositionEnum
import com.android.designcompose.proto.overlayBackgroundInteractionFromInt
import com.android.designcompose.proto.overlayPositionEnumFromInt
import com.android.designcompose.serdegen.FrameExtras

@Composable
internal fun DesignOverlay(
    overlay: FrameExtras,
    interactionState: InteractionState,
    content: @Composable () -> Unit
) {
    val alignment =
        when (overlayPositionEnumFromInt(overlay.overlay_position_type)) {
            OverlayPositionEnum.TOP_LEFT -> Alignment.TopStart
            OverlayPositionEnum.TOP_CENTER -> Alignment.TopCenter
            OverlayPositionEnum.TOP_RIGHT -> Alignment.TopEnd
            OverlayPositionEnum.CENTER -> Alignment.Center
            OverlayPositionEnum.BOTTOM_LEFT -> Alignment.BottomStart
            OverlayPositionEnum.BOTTOM_CENTER -> Alignment.BottomCenter
            OverlayPositionEnum.BOTTOM_RIGHT -> Alignment.BottomEnd
            else -> Alignment.TopStart
        }

    val closeOnTapOutside =
        overlayBackgroundInteractionFromInt(overlay.overlay_background_interaction) ==
            OverlayBackgroundInteractionEnum.CLOSE_ON_CLICK_OUTSIDE
    var boxModifier =
        Modifier.fillMaxSize().clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            // Always be clickable, because it also prevents taps and drags from going thru to views
            // behind the overlay background.
            if (closeOnTapOutside) interactionState.close(null)
        }
    overlay.overlay_background.ifPresent {
        it.color.ifPresent { color ->
            boxModifier = boxModifier.background(Color(color.r, color.g, color.b, color.a))
        }
    }

    DisposableEffect(Unit) {
        // Similar to a root view, tell the layout manager to defer layout computations until all
        // child views have been added to the overlay
        Log.d(TAG, "Overlay start")
        onDispose {}
    }
    Box(boxModifier, contentAlignment = alignment) { content() }
    DisposableEffect(Unit) {
        // Similar to a root view, tell the layout manager to that child views have been added so
        // that layout can be computed
        Log.d(TAG, "Overlay end")
        onDispose {}
    }
}
