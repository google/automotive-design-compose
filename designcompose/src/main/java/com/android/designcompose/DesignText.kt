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
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.unit.Constraints
import com.android.designcompose.DesignTextMeasure.AVAILABLE_SIZE_CONTENT_MAXIMUM
import com.android.designcompose.DesignTextMeasure.AVAILABLE_SIZE_CONTENT_MINIMUM
import com.android.designcompose.proto.blendModeFromInt
import com.android.designcompose.proto.fontStyleFromInt
import com.android.designcompose.proto.layoutStyle
import com.android.designcompose.proto.nodeStyle
import com.android.designcompose.proto.textAlignFromInt
import com.android.designcompose.proto.textAlignVerticalFromInt
import com.android.designcompose.proto.textDecorationFromInt
import com.android.designcompose.proto.textOverflowFromInt
import com.android.designcompose.proto.toUniform
import com.android.designcompose.serdegen.FontStyle
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.LineHeightType
import com.android.designcompose.serdegen.StyledTextRun
import com.android.designcompose.serdegen.TextAlign
import com.android.designcompose.serdegen.TextAlignVertical
import com.android.designcompose.serdegen.TextDecoration
import com.android.designcompose.definition.view.View
import com.android.designcompose.definition.view.ViewStyle
import java.util.Optional
import kotlin.math.ceil

// Measure text height given a width. Called from Rust as a measure function for text that has auto
// height and variable width. Layout computes the width, then calls this function to get the
// corresponding text height.
fun measureTextBoundsFunc(
    layoutId: Int,
    width: Float,
    @Suppress("unused") height: Float,
    availableWidth: Float,
    availableHeight: Float,
): Pair<Float, Float> {
    // We currently don't support vertical text, only horizontal, so this function just performs
    // height-for-width queries on text, and ignores the `height` and `availableHeight` args.

    // Look up the measure data -- this map is created/updated when building the layout tree.
    val textMeasureData = LayoutManager.getTextMeasureData(layoutId)
    if (textMeasureData == null) {
        // Maybe there's a custom measure function. We could do something with id namespaces to
        // avoid a double-hashmap lookup if this shows up on a profiler.
        val customMeasureFunc = LayoutManager.getCustomMeasure(layoutId)
        if (customMeasureFunc != null) {
            val size = customMeasureFunc(width, height, availableWidth, availableHeight)
            if (size != null) return Pair(size.width, size.height)
        }
        Log.d(TAG, "measureTextBoundsFunc() error: no textMeasureData for layoutId $layoutId")
        return Pair(0F, 0F)
    }
    val density = textMeasureData.density.density

    val layoutConstraints =
        Constraints(
            minWidth = 0,
            maxWidth =
                if (textMeasureData.autoWidth) {
                    Constraints.Infinity
                } else if (width > 0.0f) {
                    (width * density).toInt()
                } else if (
                    availableWidth <= AVAILABLE_SIZE_CONTENT_MINIMUM ||
                        availableWidth >= AVAILABLE_SIZE_CONTENT_MAXIMUM
                ) {
                    // Just tell it there's infinite width available.
                    Constraints.Infinity
                } else {
                    (availableWidth * density).toInt()
                },
            minHeight = 0,
            maxHeight =
                if (
                    availableHeight <= AVAILABLE_SIZE_CONTENT_MINIMUM ||
                        availableHeight >= AVAILABLE_SIZE_CONTENT_MAXIMUM
                ) {
                    Constraints.Infinity
                } else {
                    (availableHeight * density).toInt()
                },
        )

    // Perform a layout using the given width.
    val textLayout =
        Paragraph(
            paragraphIntrinsics = textMeasureData.paragraph,
            constraints = layoutConstraints,
            maxLines = textMeasureData.maxLines,
        )

    // The `textLayout.width` field doesn't give the tightest bounds.
    var maxLineWidth = 0.0f
    for (i in 0 until textLayout.lineCount) {
        maxLineWidth = textLayout.getLineWidth(i).coerceAtLeast(maxLineWidth)
    }

    return Pair(ceil(maxLineWidth / density), ceil(textLayout.height / density))
}
