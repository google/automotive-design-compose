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

import com.android.designcompose.proto.android_interface.layoutNodeList
import com.android.designcompose.proto.definition.element.dimensionProto
import com.android.designcompose.proto.definition.element.dimensionRect
import com.android.designcompose.proto.definition.element.size
import com.android.designcompose.proto.definition.layout.AlignContent as ProtoAlignContent
import com.android.designcompose.proto.definition.layout.AlignItems as ProtoAlignItems
import com.android.designcompose.proto.definition.layout.AlignSelf as ProtoAlignSelf
import com.android.designcompose.proto.definition.layout.FlexDirection as ProtoFlexDirection
import com.android.designcompose.proto.definition.layout.ItemSpacingKt.auto
import com.android.designcompose.proto.definition.layout.JustifyContent as ProtoJustifyContent
import com.android.designcompose.proto.definition.layout.PositionType as ProtoPositionType
import com.android.designcompose.proto.definition.layout.itemSpacing
import com.android.designcompose.proto.definition.layout.layoutNode
import com.android.designcompose.proto.definition.layout.layoutParentChildren
import com.android.designcompose.proto.definition.layout.layoutStyle
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
import com.google.protobuf.Empty

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
        // These are empty types so we need to set them to default instances
        is Undefined -> undefined = Empty.getDefaultInstance()
        is Dimension.Auto -> auto = Empty.getDefaultInstance()
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

internal fun Size.intoProto() = size {
    val s = this@intoProto
    width = s.width
    height = s.height
}

internal fun AlignSelf.intoProto() =
    when (this) {
        is AlignSelf.Auto -> ProtoAlignSelf.ALIGN_SELF_AUTO
        is AlignSelf.FlexStart -> ProtoAlignSelf.ALIGN_SELF_FLEX_START
        is AlignSelf.FlexEnd -> ProtoAlignSelf.ALIGN_SELF_FLEX_END
        is AlignSelf.Center -> ProtoAlignSelf.ALIGN_SELF_CENTER
        is AlignSelf.Baseline -> ProtoAlignSelf.ALIGN_SELF_BASELINE
        is AlignSelf.Stretch -> ProtoAlignSelf.ALIGN_SELF_STRETCH
        else -> throw IllegalArgumentException("Unknown AlignSelf: $this") // Should never happen.
    }

internal fun AlignContent.intoProto() =
    when (this) {
        is AlignContent.FlexStart -> ProtoAlignContent.ALIGN_CONTENT_FLEX_START
        is AlignContent.FlexEnd -> ProtoAlignContent.ALIGN_CONTENT_FLEX_END
        is AlignContent.Center -> ProtoAlignContent.ALIGN_CONTENT_CENTER
        is AlignContent.Stretch -> ProtoAlignContent.ALIGN_CONTENT_STRETCH
        is AlignContent.SpaceBetween -> ProtoAlignContent.ALIGN_CONTENT_SPACE_BETWEEN
        is AlignContent.SpaceAround -> ProtoAlignContent.ALIGN_CONTENT_SPACE_AROUND
        else ->
            throw IllegalArgumentException("Unknown AlignContent: $this") // Should never happen.
    }

internal fun AlignItems.intoProto() =
    when (this) {
        is AlignItems.FlexStart -> ProtoAlignItems.ALIGN_ITEMS_FLEX_START
        is AlignItems.FlexEnd -> ProtoAlignItems.ALIGN_ITEMS_FLEX_END
        is AlignItems.Center -> ProtoAlignItems.ALIGN_ITEMS_CENTER
        is AlignItems.Baseline -> ProtoAlignItems.ALIGN_ITEMS_BASELINE
        is AlignItems.Stretch -> ProtoAlignItems.ALIGN_ITEMS_STRETCH
        else -> throw IllegalArgumentException("Unknown AlignItems: $this") // Should never happen.
    }

internal fun FlexDirection.intoProto() =
    when (this) {
        is FlexDirection.Row -> ProtoFlexDirection.FLEX_DIRECTION_ROW
        is FlexDirection.RowReverse -> ProtoFlexDirection.FLEX_DIRECTION_ROW_REVERSE
        is FlexDirection.Column -> ProtoFlexDirection.FLEX_DIRECTION_COLUMN
        is FlexDirection.ColumnReverse -> ProtoFlexDirection.FLEX_DIRECTION_COLUMN_REVERSE
        is FlexDirection.None -> ProtoFlexDirection.FLEX_DIRECTION_NONE
        else ->
            throw IllegalArgumentException("Unknown FlexDirection: $this") // Should never happen.
    }

internal fun JustifyContent.intoProto() =
    when (this) {
        is JustifyContent.FlexStart -> ProtoJustifyContent.JUSTIFY_CONTENT_FLEX_START
        is JustifyContent.FlexEnd -> ProtoJustifyContent.JUSTIFY_CONTENT_FLEX_END
        is JustifyContent.Center -> ProtoJustifyContent.JUSTIFY_CONTENT_CENTER
        is JustifyContent.SpaceBetween -> ProtoJustifyContent.JUSTIFY_CONTENT_SPACE_BETWEEN
        is JustifyContent.SpaceAround -> ProtoJustifyContent.JUSTIFY_CONTENT_SPACE_AROUND
        is JustifyContent.SpaceEvenly -> ProtoJustifyContent.JUSTIFY_CONTENT_SPACE_EVENLY
        else ->
            throw IllegalArgumentException("Unknown JustifyContent: $this") // Should never happen.
    }

internal fun PositionType.intoProto() =
    when (this) {
        is PositionType.Relative -> ProtoPositionType.POSITION_TYPE_RELATIVE
        is PositionType.Absolute -> ProtoPositionType.POSITION_TYPE_ABSOLUTE
        else ->
            throw IllegalArgumentException("Unknown PositionType: $this") // Should never happen.
    }

/** Temporary (I hope) conversion from the Serde layout style to the proto layout style. */
internal fun LayoutStyle.intoProto() = layoutStyle {
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
