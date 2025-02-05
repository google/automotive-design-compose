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
import com.android.designcompose.definition.element.DimensionProto
import com.android.designcompose.definition.element.DimensionRect
import com.android.designcompose.definition.element.Size
import com.android.designcompose.definition.layout.AlignItems
import com.android.designcompose.definition.layout.AlignSelf
import com.android.designcompose.definition.layout.GridLayoutType
import com.android.designcompose.definition.layout.GridSpan
import com.android.designcompose.definition.layout.ItemSpacing
import com.android.designcompose.definition.layout.OverflowDirection
import com.android.designcompose.definition.layout.PositionType
import com.android.designcompose.definition.modifier.LayoutTransform
import com.android.designcompose.definition.view.View
import com.android.designcompose.definition.view.ViewStyle
import com.android.designcompose.layout_interface.Layout
import java.util.Optional
import kotlin.math.roundToInt

internal data class TextMeasureData(
    val textHash: Int,
    val paragraph: ParagraphIntrinsics,
    val density: Density,
    val maxLines: Int,
    val autoWidth: Boolean,
    val hyperlinkOffsetMap: HashMap<Int, String?>,
) {
    override fun hashCode(): Int {
        // Don't hash all of TextLayoutData because it's derived from style, which is
        // already hashed everywhere we use TextMeasureData's hashCode.
        var result = density.hashCode()
        result = 31 * result + textHash
        result = 31 * result + maxLines.hashCode()
        result = 31 * result + autoWidth.hashCode()
        result = 31 * result + hyperlinkOffsetMap.hashCode()
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
        if (hyperlinkOffsetMap != other.hyperlinkOffsetMap) return false

        return true
    }
}

// ExternalLayoutData holds layout properties of a node that affect its layout with respect to its
// parent. When doing component replacement, these properties are saved from the node being
// replaced so that the new node can use its values.
data class ExternalLayoutData(
    val margin: DimensionRect,
    val top: DimensionProto,
    val left: DimensionProto,
    val bottom: DimensionProto,
    val right: DimensionProto,
    val width: DimensionProto,
    val height: DimensionProto,
    val minWidth: DimensionProto,
    val minHeight: DimensionProto,
    val maxWidth: DimensionProto,
    val maxHeight: DimensionProto,
    val nodeSize: Size,
    val boundingBox: Size,
    val flexGrow: Float,
    val flexBasis: DimensionProto,
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
    return when (itemSpacing.itemSpacingTypeCase) {
        ItemSpacing.ItemSpacingTypeCase.FIXED -> itemSpacing.fixed
        ItemSpacing.ItemSpacingTypeCase.AUTO -> itemSpacing.auto.width
        else -> 0
    }
}

internal fun calcLayoutInfo(view: View, style: ViewStyle): SimplifiedLayoutInfo {
    if (!style.nodeStyle.hasGridLayoutType()) return LayoutInfoAbsolute()

    when (val gridLayout = style.nodeStyle.gridLayoutType) {
        GridLayoutType.GRID_LAYOUT_TYPE_HORIZONTAL -> {
            return LayoutInfoRow(
                alignment =
                    when (style.layoutStyle.alignItems) {
                        AlignItems.ALIGN_ITEMS_FLEX_START -> Alignment.Top
                        AlignItems.ALIGN_ITEMS_CENTER -> Alignment.CenterVertically
                        AlignItems.ALIGN_ITEMS_FLEX_END -> Alignment.Bottom
                        else -> Alignment.Top
                    }
            )
        }

        GridLayoutType.GRID_LAYOUT_TYPE_VERTICAL -> {
            return LayoutInfoColumn(
                alignment =
                    when (style.layoutStyle.alignItems) {
                        AlignItems.ALIGN_ITEMS_FLEX_START -> Alignment.Start
                        AlignItems.ALIGN_ITEMS_CENTER -> Alignment.CenterHorizontally
                        AlignItems.ALIGN_ITEMS_FLEX_END -> Alignment.End
                        else -> Alignment.End
                    }
            )
        }

        else -> {
            val isColumnLayout =
                (gridLayout == GridLayoutType.GRID_LAYOUT_TYPE_FIXED_COLUMNS ||
                    gridLayout == GridLayoutType.GRID_LAYOUT_TYPE_AUTO_COLUMNS)
            val scrollingEnabled =
                when (view.scrollInfo.overflow) {
                    OverflowDirection.OVERFLOW_DIRECTION_VERTICAL_SCROLLING -> isColumnLayout
                    OverflowDirection.OVERFLOW_DIRECTION_HORIZONTAL_SCROLLING -> !isColumnLayout
                    OverflowDirection.OVERFLOW_DIRECTION_HORIZONTAL_AND_VERTICAL_SCROLLING -> true
                    else -> false
                }
            return LayoutInfoGrid(
                layout = style.nodeStyle.gridLayoutType,
                minColumnRowSize = style.nodeStyle.gridAdaptiveMinSize,
                mainAxisSpacing = style.layoutStyle.itemSpacing,
                crossAxisSpacing = style.nodeStyle.crossAxisItemSpacing.toInt(),
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
                numColumnsRows = style.nodeStyle.gridColumnsRows,
                gridSpanContent = style.nodeStyle.gridSpanContentsList,
                scrollingEnabled = scrollingEnabled,
                padding = style.layoutStyle.padding,
            )
        }
    }
}

fun Layout.width() = this.width.roundToInt()

fun Layout.height() = this.height.roundToInt()

fun Layout.left() = this.left.roundToInt()

fun Layout.top() = this.top.roundToInt()
