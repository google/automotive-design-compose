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

package com.android.designcompose.proto

import com.android.designcompose.proto.layout.ItemSpacingKt.auto
import com.android.designcompose.proto.layout.Styles
import com.android.designcompose.proto.layout.dimensionProto
import com.android.designcompose.proto.layout.dimensionRect
import com.android.designcompose.proto.layout.floatSize
import com.android.designcompose.proto.layout.itemSpacing
import com.android.designcompose.proto.layout.layoutNode
import com.android.designcompose.proto.layout.layoutNodeList
import com.android.designcompose.proto.layout.layoutParentChildren
import com.android.designcompose.serdegen.AlignContent
import com.android.designcompose.serdegen.AlignItems
import com.android.designcompose.serdegen.AlignSelf
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.Dimension.Undefined
import com.android.designcompose.serdegen.FlexDirection
import com.android.designcompose.serdegen.ItemSpacing
import com.android.designcompose.serdegen.ItemSpacing.Auto
import com.android.designcompose.serdegen.ItemSpacing.Fixed
import com.android.designcompose.serdegen.JustifyContent
import com.android.designcompose.serdegen.LayoutNode
import com.android.designcompose.serdegen.LayoutNodeList
import com.android.designcompose.serdegen.LayoutParentChildren
import com.android.designcompose.serdegen.LayoutStyle
import com.android.designcompose.serdegen.PositionType
import com.android.designcompose.serdegen.Rect
import com.android.designcompose.serdegen.Size

internal fun LayoutNode.intoProto() = layoutNode {
    val s = this@intoProto
    layoutId = s.layout_id
    parentLayoutId = s.parent_layout_id
    childIndex = s.child_index
    style = s.style.intoProto()
    name = s.name
    useMeasureFunc = s.use_measure_func
    if (s.fixed_width.isPresent) fixedWidth = s.fixed_width.get()
    if (s.fixed_height.isPresent) fixedHeight = s.fixed_height.get()
}

internal fun LayoutParentChildren.intoProto() = layoutParentChildren {
    val s = this@intoProto
    parentLayoutId = s.parent_layout_id
    childLayoutIds.addAll(s.child_layout_ids)
}

internal fun LayoutNodeList.intoProto() = layoutNodeList {
    layoutNodes.addAll(this@intoProto.layout_nodes.map { it.intoProto() })
    parentChildren.addAll(this@intoProto.parent_children.map { it.intoProto() })
}

internal fun Dimension.intoProto() = dimensionProto {
    when (val s = this@intoProto) {
        is Undefined -> undefined = true
        is Dimension.Auto -> auto = true
        is Dimension.Points -> points = s.value.toFloat()
        is Dimension.Percent -> percent = s.value.toFloat()
    }
}

internal fun Rect.intoProto() = dimensionRect {
    val s = this@intoProto
    start = s.start.intoProto()
    end = s.end.intoProto()
    top = s.top.intoProto()
    bottom = s.bottom.intoProto()
}

internal fun ItemSpacing.intoProto() = itemSpacing {
    when (val s = this@intoProto) {
        is Fixed -> fixed = s.value
        is Auto -> auto = auto {
                width = s.field0
                height = s.field1
            }
        else -> throw IllegalArgumentException("Unknown ItemSpacing: $this") // Should never happen.
    }
}

internal fun Size.intoProto() = floatSize {
    val s = this@intoProto
    width = s.width
    height = s.height
}

internal fun AlignSelf.intoProto() =
    when (this) {
        is AlignSelf.Auto -> Styles.AlignSelfEnum.AlignSelf.AUTO
        is AlignSelf.FlexStart -> Styles.AlignSelfEnum.AlignSelf.FLEX_START
        is AlignSelf.FlexEnd -> Styles.AlignSelfEnum.AlignSelf.FLEX_END
        is AlignSelf.Center -> Styles.AlignSelfEnum.AlignSelf.CENTER
        is AlignSelf.Baseline -> Styles.AlignSelfEnum.AlignSelf.BASELINE
        is AlignSelf.Stretch -> Styles.AlignSelfEnum.AlignSelf.STRETCH
        else -> throw IllegalArgumentException("Unknown AlignSelf: $this") // Should never happen.
    }

internal fun AlignContent.intoProto() =
    when (this) {
        is AlignContent.FlexStart -> Styles.AlignContentEnum.AlignContent.FLEX_START
        is AlignContent.FlexEnd -> Styles.AlignContentEnum.AlignContent.FLEX_END
        is AlignContent.Center -> Styles.AlignContentEnum.AlignContent.CENTER
        is AlignContent.Stretch -> Styles.AlignContentEnum.AlignContent.STRETCH
        is AlignContent.SpaceBetween -> Styles.AlignContentEnum.AlignContent.SPACE_BETWEEN
        is AlignContent.SpaceAround -> Styles.AlignContentEnum.AlignContent.SPACE_AROUND
        else ->
            throw IllegalArgumentException("Unknown AlignContent: $this") // Should never happen.
    }

internal fun AlignItems.intoProto() =
    when (this) {
        is AlignItems.FlexStart -> Styles.AlignItemsEnum.AlignItems.FLEX_START
        is AlignItems.FlexEnd -> Styles.AlignItemsEnum.AlignItems.FLEX_END
        is AlignItems.Center -> Styles.AlignItemsEnum.AlignItems.CENTER
        is AlignItems.Baseline -> Styles.AlignItemsEnum.AlignItems.BASELINE
        is AlignItems.Stretch -> Styles.AlignItemsEnum.AlignItems.STRETCH
        else ->
            throw IllegalArgumentException("Unknown AlignContent: $this") // Should never happen.
    }

internal fun FlexDirection.intoProto() =
    when (this) {
        is FlexDirection.Row -> Styles.FlexDirectionEnum.FlexDirection.ROW
        is FlexDirection.Column -> Styles.FlexDirectionEnum.FlexDirection.COLUMN
        is FlexDirection.RowReverse -> Styles.FlexDirectionEnum.FlexDirection.ROW_REVERSE
        is FlexDirection.ColumnReverse -> Styles.FlexDirectionEnum.FlexDirection.COLUMN_REVERSE
        is FlexDirection.None -> Styles.FlexDirectionEnum.FlexDirection.NONE
        else ->
            throw IllegalArgumentException("Unknown FlexDirection: $this") // Should never happen.
    }

internal fun JustifyContent.intoProto() =
    when (this) {
        is JustifyContent.FlexStart -> Styles.JustifyContentEnum.JustifyContent.FLEX_START
        is JustifyContent.FlexEnd -> Styles.JustifyContentEnum.JustifyContent.FLEX_END
        is JustifyContent.Center -> Styles.JustifyContentEnum.JustifyContent.CENTER
        is JustifyContent.SpaceBetween -> Styles.JustifyContentEnum.JustifyContent.SPACE_BETWEEN
        is JustifyContent.SpaceAround -> Styles.JustifyContentEnum.JustifyContent.SPACE_AROUND
        is JustifyContent.SpaceEvenly -> Styles.JustifyContentEnum.JustifyContent.SPACE_EVENLY
        else ->
            throw IllegalArgumentException("Unknown JustifyContent: $this") // Should never happen.
    }

internal fun PositionType.intoProto() =
    when (this) {
        is PositionType.Relative -> Styles.PositionTypeEnum.PositionType.RELATIVE
        is PositionType.Absolute -> Styles.PositionTypeEnum.PositionType.ABSOLUTE
        else ->
            throw IllegalArgumentException("Unknown PositionType: $this") // Should never happen.
    }

/** Temporary (I hope) conversion from the Serde layout style to the proto layout style. */
internal fun LayoutStyle.intoProto() =
    com.android.designcompose.proto.layout.layoutStyle {
        val s = this@intoProto
        margin = s.margin.intoProto()
        padding = s.padding.intoProto()
        itemSpacing = s.item_spacing.intoProto()
        top = s.top.intoProto()
        left = s.left.intoProto()
        bottom = s.bottom.intoProto()
        right = s.right.intoProto()
        width = s.width.intoProto()
        height = s.height.intoProto()
        minWidth = s.min_width.intoProto()
        maxWidth = s.max_width.intoProto()
        minHeight = s.min_height.intoProto()
        maxHeight = s.max_height.intoProto()
        boundingBox = s.bounding_box.intoProto()
        flexGrow = s.flex_grow
        flexShrink = s.flex_shrink
        flexBasis = s.flex_basis.intoProto()
        alignSelf = s.align_self.intoProto()
        alignContent = s.align_content.intoProto()
        alignItems = s.align_items.intoProto()
        flexDirection = s.flex_direction.intoProto()
        justifyContent = s.justify_content.intoProto()
        positionType = s.position_type.intoProto()
    }
