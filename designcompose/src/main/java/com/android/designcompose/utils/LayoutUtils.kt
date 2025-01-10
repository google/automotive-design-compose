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

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.designcompose.LayoutManager
import com.android.designcompose.definition.element.DimensionProto
import com.android.designcompose.definition.view.ViewStyle

internal fun DimensionProto.pointsAsDp(density: Float): Dp {
    return if (hasPoints()) (points * density).dp else 0.dp
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
        if (hasModifiedSize || !style.layoutStyle.width.hasPoints()) layoutSize.width
        else style.layoutStyle.width.pointsAsDp(density).value
    val height =
        if (hasModifiedSize || !style.layoutStyle.height.hasPoints()) layoutSize.height
        else style.layoutStyle.height.pointsAsDp(density).value
    return Size(width, height)
}
