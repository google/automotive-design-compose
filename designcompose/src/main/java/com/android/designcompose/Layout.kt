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

import androidx.compose.ui.Alignment
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.unit.Density
import com.android.designcompose.proto.alignItemsFromInt
import com.android.designcompose.proto.gridLayoutTypeFromInt
import com.android.designcompose.proto.layoutStyle
import com.android.designcompose.proto.nodeStyle
import com.android.designcompose.proto.overflowDirectionFromInt
import com.android.designcompose.proto.type
import com.android.designcompose.serdegen.AlignItems
import com.android.designcompose.serdegen.AlignSelf
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.DimensionRect
import com.android.designcompose.serdegen.GridLayoutType
import com.android.designcompose.serdegen.GridSpan
import com.android.designcompose.serdegen.ItemSpacing
import com.android.designcompose.serdegen.ItemSpacingType
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.LayoutTransform
import com.android.designcompose.serdegen.OverflowDirection
import com.android.designcompose.serdegen.PositionType
import com.android.designcompose.serdegen.Size
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewStyle
import java.util.Optional
import kotlin.math.roundToInt

internal data class TextMeasureData(
    val textHash: Int,
    val paragraph: ParagraphIntrinsics,
    val density: Density,
    val maxLines: Int,
    val autoWidth: Boolean,
) {
    override fun hashCode(): Int {
        // Don't hash all of TextLayoutData because it's derived from style, which is
        // already hashed everywhere we use TextMeasureData's hashCode.
        var result = density.hashCode()
        result = 31 * result + textHash
        result = 31 * result + maxLines.hashCode()
        result = 31 * result + autoWidth.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextMeasureData

        if (density != other.density) return false
        if (textHash != other.textHash) return false
        if (maxLines != other.maxLines) return false
        if (autoWidth != other.autoWidth) return false

        return true
    }
}

// ExternalLayoutData holds layout properties of a node that affect its layout with respect to its
// parent. When doing component replacement, these properties are saved from the node being
// replaced so that the new node can use its values.
data class ExternalLayoutData(
    val margin: DimensionRect,
    val top: Dimension,
    val left: Dimension,
    val bottom: Dimension,
    val right: Dimension,
    val width: Dimension,
    val height: Dimension,
    val minWidth: Dimension,
    val minHeight: Dimension,
    val maxWidth: Dimension,
    val maxHeight: Dimension,
    val nodeSize: Size,
    val boundingBox: Size,
    val flexGrow: Float,
    val flexBasis: Dimension,
    val alignSelf: AlignSelf,
    val positionType: PositionType,
    val transform: Optional<LayoutTransform>,
    val relativeTransform: Optional<LayoutTransform>,
)

internal open class SimplifiedLayoutInfo

internal class LayoutInfoAbsolute : SimplifiedLayoutInfo()

internal class LayoutInfoRow(val alignment: Alignment.Vertical) : SimplifiedLayoutInfo()

internal class LayoutInfoColumn(val alignment: Alignment.Horizontal) : SimplifiedLayoutInfo()

internal class LayoutInfoGrid(
    val layout: GridLayoutType,
    val minColumnRowSize: Int,
    val mainAxisSpacing: ItemSpacing,
    val crossAxisSpacing: Int,
    val numColumnsRows: Int,
    val gridSpanContent: List<GridSpan>,
    val scrollingEnabled: Boolean,
    val padding: DimensionRect,
) : SimplifiedLayoutInfo()

internal fun itemSpacingAbs(itemSpacing: ItemSpacing): Int {
    return when (val type = itemSpacing.type()) {
        is ItemSpacingType.Fixed -> type.value
        is ItemSpacingType.Auto -> type.value.width
        else -> 0
    }
}

internal fun calcLayoutInfo(view: View, style: ViewStyle): SimplifiedLayoutInfo {
    if (style.nodeStyle.grid_layout_type.isPresent) {
        val gridLayout = gridLayoutTypeFromInt(style.nodeStyle.grid_layout_type.get())
        val isHorizontalLayout = gridLayout is GridLayoutType.Horizontal
        val isVerticalLayout = gridLayout is GridLayoutType.Vertical
        if (isHorizontalLayout) {
            return LayoutInfoRow(
                alignment =
                    when (alignItemsFromInt(style.layoutStyle.align_items)) {
                        is AlignItems.FlexStart -> Alignment.Top
                        is AlignItems.Center -> Alignment.CenterVertically
                        is AlignItems.FlexEnd -> Alignment.Bottom
                        else -> Alignment.Top
                    }
            )
        } else if (isVerticalLayout) {
            return LayoutInfoColumn(
                alignment =
                    when (alignItemsFromInt(style.layoutStyle.align_items)) {
                        is AlignItems.FlexStart -> Alignment.Start
                        is AlignItems.Center -> Alignment.CenterHorizontally
                        is AlignItems.FlexEnd -> Alignment.End
                        else -> Alignment.End
                    }
            )
        } else {
            val isColumnLayout =
                gridLayout is GridLayoutType.FixedColumns ||
                    gridLayout is GridLayoutType.AutoColumns
            val scrollingEnabled =
                when (overflowDirectionFromInt(view.scroll_info.get().overflow)) {
                    is OverflowDirection.VerticalScrolling -> isColumnLayout
                    is OverflowDirection.HorizontalScrolling -> !isColumnLayout
                    is OverflowDirection.HorizontalAndVerticalScrolling -> true
                    else -> false
                }
            return LayoutInfoGrid(
                layout = gridLayoutTypeFromInt(style.nodeStyle.grid_layout_type.get()),
                minColumnRowSize = style.nodeStyle.grid_adaptive_min_size,
                mainAxisSpacing = style.layoutStyle.item_spacing.get(),
                crossAxisSpacing = style.nodeStyle.cross_axis_item_spacing.toInt(),
                // TODO support these other alignments?
                /*
                mainAxisAlignment =
                when (style.justify_content) {
                  is JustifyContent.FlexStart -> MainAxisAlignment.Start
                  is JustifyContent.Center -> MainAxisAlignment.Center
                  is JustifyContent.FlexEnd -> MainAxisAlignment.End
                  is JustifyContent.SpaceAround -> MainAxisAlignment.SpaceAround
                  is JustifyContent.SpaceBetween -> MainAxisAlignment.SpaceBetween
                  is JustifyContent.SpaceEvenly -> MainAxisAlignment.SpaceEvenly
                  else -> MainAxisAlignment.Start
                },
                crossAxisAlignment =
                when (style.align_items) {
                  is AlignItems.FlexStart -> FlowCrossAxisAlignment.Start
                  is AlignItems.Center -> FlowCrossAxisAlignment.Center
                  is AlignItems.FlexEnd -> FlowCrossAxisAlignment.End
                  else -> FlowCrossAxisAlignment.Start
                },
                */
                numColumnsRows = style.nodeStyle.grid_columns_rows,
                gridSpanContent = style.nodeStyle.grid_span_contents,
                scrollingEnabled = scrollingEnabled,
                padding = style.layoutStyle.padding.get(),
            )
        }
    } else {
        return LayoutInfoAbsolute()
    }
}

internal fun Layout.width() = this.width.roundToInt()

internal fun Layout.height() = this.height.roundToInt()

internal fun Layout.left() = this.left.roundToInt()

internal fun Layout.top() = this.top.roundToInt()
