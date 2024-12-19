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

package com.android.designcompose.utils

import android.graphics.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.designcompose.LayoutManager
import com.android.designcompose.definition.view.ViewStyle
import com.android.designcompose.serdegen.Dimension
import kotlin.math.roundToInt

/** Multiply out a dimension against available space */
internal fun Dimension.resolve(available: Int, density: Float): Int? {
    return when (this) {
        is Dimension.Percent -> (available * value).roundToInt()
        is Dimension.Points -> (value * density).roundToInt()
        else -> null
    }
}

internal fun Dimension.pointsAsDp(density: Float): Dp {
    return when (this) {
        is Dimension.Points -> (value * density).dp
        else -> 0.dp
    }
}

internal fun Dimension.isFixed(): Boolean {
    return this is Dimension.Points
}

/** Evaluate an absolute layout within the given constraints */
internal fun absoluteLayout(style: ViewStyle, constraints: Constraints, density: Float): Rect {
    val pw =
        if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            0
        }
    val ph =
        if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            0
        }

    val left = style.layoutStyle.left.getDim().resolve(pw, density)
    val top = style.layoutStyle.top.getDim().resolve(ph, density)
    // Right and bottom are insets from the right/bottom edge, so convert them to be relative to
    // the top/left corner.
    val right = style.layoutStyle.right.getDim().resolve(pw, density)?.let { r -> pw - r }
    val bottom = style.layoutStyle.bottom.getDim().resolve(ph, density)?.let { b -> ph - b }
    val width = style.layoutStyle.width.getDim().resolve(pw, density)
    val height = style.layoutStyle.height.getDim().resolve(ph, density)
    // We use the top and left margins for center anchored items, so they can be safely applied
    // as an offset here.
    val leftMargin = style.layoutStyle.margin.start.resolve(pw, density) ?: 0
    val topMargin = style.layoutStyle.margin.end.resolve(ph, density) ?: 0

    // XXX: Need layoutDirection; when left, right and width are specified we use left and
    //      width in LtoR direction, and use right and width in RtoL direction.
    val x =
        leftMargin +
            (left
                ?: if (right != null && width != null) {
                    right - width
                } else {
                    0
                })
    val y =
        topMargin +
            (top
                ?: if (bottom != null && height != null) {
                    bottom - height
                } else {
                    0
                })
    var w =
        width
            ?: if (left != null && right != null) {
                right - left
            } else {
                0
            }
    var h =
        height
            ?: if (top != null && bottom != null) {
                bottom - top
            } else {
                0
            }

    val minWidth = style.layoutStyle.min_width.getDim().resolve(pw, density)
    val minHeight = style.layoutStyle.min_height.getDim().resolve(ph, density)
    if (minWidth != null && w < minWidth) {
        w = minWidth
    }
    if (minHeight != null && h < minHeight) {
        h = minHeight
    }

    return Rect(x, y, x + w, y + h)
}

/** Evaluate a relative layout against the given constraints */
internal fun relativeLayout(style: ViewStyle, constraints: Constraints, density: Float): Rect {
    val pw =
        if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            0
        }
    val ph =
        if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            0
        }

    var w = style.layoutStyle.width.getDim().resolve(pw, density) ?: 0
    var h = style.layoutStyle.height.getDim().resolve(ph, density) ?: 0
    // We use the top and left margins for center anchored items, so they can be safely applied
    // as an offset here.
    val x = style.layoutStyle.margin.start.resolve(pw, density) ?: 0
    val y = style.layoutStyle.margin.top.resolve(ph, density) ?: 0

    val minWidth = style.layoutStyle.min_width.getDim().resolve(pw, density)
    val minHeight = style.layoutStyle.min_height.getDim().resolve(ph, density)
    if (minWidth != null && w < minWidth) {
        w = minWidth
    }
    if (minHeight != null && h < minHeight) {
        h = minHeight
    }

    return Rect(x, y, x + w, y + h)
}

// Return the size of a node used to render the node.
internal fun getNodeRenderSize(
    overrideSize: Size?,
    layoutSize: Size,
    style: ViewStyle,
    layoutId: Int,
    density: Float,
): Size {
    // If an override size exists, use it. This is typically a size programmatically set for a dial
    // or gauge.
    if (overrideSize != null) return overrideSize
    // If the layout manager has saved this node as one whose size has been modified, or if the size
    // in the style of the node is not fixed, use the layout size. Otherwise, use the fixed size
    // specified in the style so that we respect rotations, since the layout size is the bounding
    // box for a rotated node. We do not yet support rotated nodes with non-fixed constraints.
    val hasModifiedSize = LayoutManager.hasModifiedSize(layoutId)
    val width =
        if (hasModifiedSize || style.layoutStyle.width.getDim() !is Dimension.Points)
            layoutSize.width
        else style.layoutStyle.width.getDim().pointsAsDp(density).value
    val height =
        if (hasModifiedSize || style.layoutStyle.height.getDim() !is Dimension.Points)
            layoutSize.height
        else style.layoutStyle.height.getDim().pointsAsDp(density).value
    return Size(width, height)
}
