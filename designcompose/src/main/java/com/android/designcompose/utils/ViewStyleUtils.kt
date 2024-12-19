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

import com.android.designcompose.definition.layout.LayoutStyle
import com.android.designcompose.definition.view.NodeStyle
import com.android.designcompose.definition.view.ViewStyle
import com.android.designcompose.proto.StrokeAlignType
import com.android.designcompose.proto.alignContentFromInt
import com.android.designcompose.proto.alignItemsFromInt
import com.android.designcompose.proto.alignSelfFromInt
import com.android.designcompose.proto.blendModeFromInt
import com.android.designcompose.proto.displayFromInt
import com.android.designcompose.proto.flexDirectionFromInt
import com.android.designcompose.proto.flexWrapFromInt
import com.android.designcompose.proto.fontStyleFromInt
import com.android.designcompose.proto.justifyContentFromInt
import com.android.designcompose.proto.layoutSizingFromInt
import com.android.designcompose.proto.newDimensionProtoUndefined
import com.android.designcompose.proto.newDimensionRectPointsZero
import com.android.designcompose.proto.newFontWeight
import com.android.designcompose.proto.newNumOrVar
import com.android.designcompose.proto.overflowFromInt
import com.android.designcompose.proto.pointerEventsFromInt
import com.android.designcompose.proto.positionTypeFromInt
import com.android.designcompose.proto.strokeAlignTypeToInt
import com.android.designcompose.proto.textAlignFromInt
import com.android.designcompose.proto.textAlignVerticalFromInt
import com.android.designcompose.proto.textDecorationFromInt
import com.android.designcompose.proto.textOverflowFromInt
import com.android.designcompose.proto.toInt
import com.android.designcompose.proto.type
import com.android.designcompose.serdegen.AlignContent
import com.android.designcompose.serdegen.AlignItems
import com.android.designcompose.serdegen.AlignSelf
import com.android.designcompose.serdegen.Auto
import com.android.designcompose.serdegen.Background
import com.android.designcompose.serdegen.BackgroundType
import com.android.designcompose.serdegen.BlendMode
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.Display
import com.android.designcompose.serdegen.FlexDirection
import com.android.designcompose.serdegen.FlexWrap
import com.android.designcompose.serdegen.FontStretch
import com.android.designcompose.serdegen.FontStyle
import com.android.designcompose.serdegen.ItemSpacing
import com.android.designcompose.serdegen.ItemSpacingType
import com.android.designcompose.serdegen.JustifyContent
import com.android.designcompose.serdegen.LayoutSizing
import com.android.designcompose.serdegen.LineHeight
import com.android.designcompose.serdegen.LineHeightType
import com.android.designcompose.serdegen.NumOrVarType
import com.android.designcompose.serdegen.Overflow
import com.android.designcompose.serdegen.PointerEvents
import com.android.designcompose.serdegen.PositionType
import com.android.designcompose.serdegen.Stroke
import com.android.designcompose.serdegen.StrokeWeight
import com.android.designcompose.serdegen.StrokeWeightType
import com.android.designcompose.serdegen.TextAlign
import com.android.designcompose.serdegen.TextAlignVertical
import com.android.designcompose.serdegen.TextDecoration
import com.android.designcompose.serdegen.TextOverflow
import java.util.Optional

// Merge styles; any non-default properties of the override style are copied over the base style.
internal fun mergeStyles(base: ViewStyle, override: ViewStyle): ViewStyle {
    val style = ViewStyle.Builder()
    val nodeStyle = NodeStyle.Builder()
    val layoutStyle = LayoutStyle.Builder()
    nodeStyle.text_color =
        if (
            override.nodeStyle.text_color.getOrNull()?.getType() ==
            BackgroundType.None(com.novi.serde.Unit())
        ) {
            override.nodeStyle.text_color
        } else {
            base.nodeStyle.text_color
        }
    nodeStyle.font_size =
        if (
            override.nodeStyle.font_size.getOrNull()?.num_or_var_type?.get() ==
            NumOrVarType.Num(18.0f)
        ) {
            override.nodeStyle.font_size
        } else {
            base.nodeStyle.font_size
        }
    nodeStyle.font_weight =
        if (
            override.nodeStyle.font_weight.getOrNull()?.weight?.get()?.num_or_var_type?.get() !=
            NumOrVarType.Num(400.0f)
        ) {
            override.nodeStyle.font_weight
        } else {
            base.nodeStyle.font_weight
        }
    nodeStyle.font_style =
        if (fontStyleFromInt(override.nodeStyle.font_style) !is FontStyle.Normal) {
            override.nodeStyle.font_style
        } else {
            base.nodeStyle.font_style
        }
    nodeStyle.text_decoration =
        if (textDecorationFromInt(override.nodeStyle.text_decoration) !is TextDecoration.None) {
            override.nodeStyle.text_decoration
        } else {
            base.nodeStyle.text_decoration
        }
    nodeStyle.letter_spacing =
        if (override.nodeStyle.letter_spacing.isPresent) {
            override.nodeStyle.letter_spacing
        } else {
            base.nodeStyle.letter_spacing
        }
    nodeStyle.font_family =
        if (override.nodeStyle.font_family.isPresent) {
            override.nodeStyle.font_family
        } else {
            base.nodeStyle.font_family
        }
    nodeStyle.font_stretch =
        if (override.nodeStyle.font_stretch.getOrNull()?.value != 1.0f) {
            override.nodeStyle.font_stretch
        } else {
            base.nodeStyle.font_stretch
        }
    nodeStyle.backgrounds =
        if (
            override.nodeStyle.backgrounds.size > 0 &&
            !override.nodeStyle.backgrounds[0].isType<BackgroundType.None>()
        ) {
            override.nodeStyle.backgrounds
        } else {
            base.nodeStyle.backgrounds
        }
    nodeStyle.box_shadows =
        if (override.nodeStyle.box_shadows.size > 0) {
            override.nodeStyle.box_shadows
        } else {
            base.nodeStyle.box_shadows
        }
    nodeStyle.stroke =
        if ((override.nodeStyle.stroke.getOrNull()?.strokes?.size ?: 0) > 0) {
            override.nodeStyle.stroke
        } else {
            base.nodeStyle.stroke
        }
    nodeStyle.opacity =
        if (override.nodeStyle.opacity.isPresent) {
            override.nodeStyle.opacity
        } else {
            base.nodeStyle.opacity
        }
    nodeStyle.transform =
        if (override.nodeStyle.transform.isPresent) {
            override.nodeStyle.transform
        } else {
            base.nodeStyle.transform
        }
    nodeStyle.relative_transform =
        if (override.nodeStyle.relative_transform.isPresent) {
            override.nodeStyle.relative_transform
        } else {
            base.nodeStyle.relative_transform
        }
    nodeStyle.text_align =
        if (textAlignFromInt(override.nodeStyle.text_align) !is TextAlign.Left) {
            override.nodeStyle.text_align
        } else {
            base.nodeStyle.text_align
        }
    nodeStyle.text_align_vertical =
        if (
            textAlignVerticalFromInt(override.nodeStyle.text_align_vertical)
                    !is TextAlignVertical.Top
        ) {
            override.nodeStyle.text_align_vertical
        } else {
            base.nodeStyle.text_align_vertical
        }
    nodeStyle.text_overflow =
        if (textOverflowFromInt(override.nodeStyle.text_overflow) !is TextOverflow.Clip) {
            override.nodeStyle.text_overflow
        } else {
            base.nodeStyle.text_overflow
        }
    nodeStyle.text_shadow =
        if (override.nodeStyle.text_shadow.isPresent) {
            override.nodeStyle.text_shadow
        } else {
            base.nodeStyle.text_shadow
        }
    nodeStyle.node_size =
        if (
            override.nodeStyle.node_size.getOrNull().let { it?.width != 0.0f || it.height != 0.0f }
        ) {
            override.nodeStyle.node_size
        } else {
            base.nodeStyle.node_size
        }
    nodeStyle.line_height =
        if (!override.nodeStyle.line_height.equals(LineHeightType.Percent(1.0f))) {
            override.nodeStyle.line_height
        } else {
            base.nodeStyle.line_height
        }
    nodeStyle.line_count =
        if (override.nodeStyle.line_count.isPresent) {
            override.nodeStyle.line_count
        } else {
            base.nodeStyle.line_count
        }
    nodeStyle.font_features =
        if (override.nodeStyle.font_features.size > 0) {
            override.nodeStyle.font_features
        } else {
            base.nodeStyle.font_features
        }
    nodeStyle.filters =
        if (override.nodeStyle.filters.size > 0) {
            override.nodeStyle.filters
        } else {
            base.nodeStyle.filters
        }
    nodeStyle.backdrop_filters =
        if (override.nodeStyle.backdrop_filters.size > 0) {
            override.nodeStyle.backdrop_filters
        } else {
            base.nodeStyle.backdrop_filters
        }
    nodeStyle.blend_mode =
        if (blendModeFromInt(override.nodeStyle.blend_mode) is BlendMode.PassThrough) {
            override.nodeStyle.blend_mode
        } else {
            base.nodeStyle.blend_mode
        }
    nodeStyle.hyperlinks =
        if (override.nodeStyle.hyperlinks.isPresent) {
            override.nodeStyle.hyperlinks
        } else {
            base.nodeStyle.hyperlinks
        }
    nodeStyle.display_type =
        if (displayFromInt(override.nodeStyle.display_type) !is Display.Flex) {
            override.nodeStyle.display_type
        } else {
            base.nodeStyle.display_type
        }
    layoutStyle.position_type =
        if (positionTypeFromInt(override.layoutStyle.position_type) !is PositionType.Relative) {
            override.layoutStyle.position_type
        } else {
            base.layoutStyle.position_type
        }
    layoutStyle.flex_direction =
        if (flexDirectionFromInt(override.layoutStyle.flex_direction) !is FlexDirection.Row) {
            override.layoutStyle.flex_direction
        } else {
            base.layoutStyle.flex_direction
        }
    nodeStyle.flex_wrap =
        if (flexWrapFromInt(override.nodeStyle.flex_wrap) !is FlexWrap.NoWrap) {
            override.nodeStyle.flex_wrap
        } else {
            base.nodeStyle.flex_wrap
        }
    nodeStyle.grid_layout_type =
        if (override.nodeStyle.grid_layout_type.isPresent) {
            override.nodeStyle.grid_layout_type
        } else {
            base.nodeStyle.grid_layout_type
        }
    nodeStyle.grid_columns_rows =
        if (override.nodeStyle.grid_columns_rows > 0) {
            override.nodeStyle.grid_columns_rows
        } else {
            base.nodeStyle.grid_columns_rows
        }
    nodeStyle.grid_adaptive_min_size =
        if (override.nodeStyle.grid_adaptive_min_size > 1) {
            override.nodeStyle.grid_adaptive_min_size
        } else {
            base.nodeStyle.grid_adaptive_min_size
        }
    nodeStyle.grid_span_contents =
        override.nodeStyle.grid_span_contents.ifEmpty { base.nodeStyle.grid_span_contents }
    nodeStyle.overflow =
        if (overflowFromInt(override.nodeStyle.overflow) !is Overflow.Visible) {
            override.nodeStyle.overflow
        } else {
            base.nodeStyle.overflow
        }
    nodeStyle.max_children =
        if (override.nodeStyle.max_children.isPresent) {
            override.nodeStyle.max_children
        } else {
            base.nodeStyle.max_children
        }
    nodeStyle.overflow_node_id =
        if (override.nodeStyle.overflow_node_id.isPresent) {
            override.nodeStyle.overflow_node_id
        } else {
            base.nodeStyle.overflow_node_id
        }
    nodeStyle.overflow_node_name =
        if (override.nodeStyle.overflow_node_name.isPresent) {
            override.nodeStyle.overflow_node_name
        } else {
            base.nodeStyle.overflow_node_name
        }
    layoutStyle.align_items =
        if (alignItemsFromInt(override.layoutStyle.align_items) !is AlignItems.Stretch) {
            override.layoutStyle.align_items
        } else {
            base.layoutStyle.align_items
        }
    layoutStyle.align_self =
        if (alignSelfFromInt(override.layoutStyle.align_self) !is AlignSelf.Auto) {
            override.layoutStyle.align_self
        } else {
            base.layoutStyle.align_self
        }
    layoutStyle.align_content =
        if (alignContentFromInt(override.layoutStyle.align_content) !is AlignContent.Stretch) {
            override.layoutStyle.align_content
        } else {
            base.layoutStyle.align_content
        }
    layoutStyle.justify_content =
        if (
            justifyContentFromInt(override.layoutStyle.justify_content) !is JustifyContent.FlexStart
        ) {
            override.layoutStyle.justify_content
        } else {
            base.layoutStyle.justify_content
        }
    layoutStyle.top =
        if (override.layoutStyle.top.getDim() !is Dimension.Undefined) {
            override.layoutStyle.top
        } else {
            base.layoutStyle.top
        }
    layoutStyle.left =
        if (override.layoutStyle.left.getDim() !is Dimension.Undefined) {
            override.layoutStyle.left
        } else {
            base.layoutStyle.left
        }
    layoutStyle.bottom =
        if (override.layoutStyle.bottom.getDim() !is Dimension.Undefined) {
            override.layoutStyle.bottom
        } else {
            base.layoutStyle.bottom
        }
    layoutStyle.right =
        if (override.layoutStyle.right.getDim() !is Dimension.Undefined) {
            override.layoutStyle.right
        } else {
            base.layoutStyle.right
        }

    layoutStyle.margin =
        if (!override.layoutStyle.margin.isDefault()) {
            override.layoutStyle.margin
        } else {
            base.layoutStyle.margin
        }
    layoutStyle.padding =
        if (!override.layoutStyle.padding.isDefault()) {
            override.layoutStyle.padding
        } else {
            base.layoutStyle.padding
        }
    fun ItemSpacing.isDefault(): Boolean {
        return (type() as? ItemSpacingType.Fixed)?.value == 0
    }
    layoutStyle.item_spacing =
        if (!override.layoutStyle.item_spacing.get().isDefault()) {
            override.layoutStyle.item_spacing
        } else {
            base.layoutStyle.item_spacing
        }
    nodeStyle.cross_axis_item_spacing =
        if (override.nodeStyle.cross_axis_item_spacing != 0.0f) {
            override.nodeStyle.cross_axis_item_spacing
        } else {
            base.nodeStyle.cross_axis_item_spacing
        }
    layoutStyle.flex_grow =
        if (override.layoutStyle.flex_grow != 0.0f) {
            override.layoutStyle.flex_grow
        } else {
            base.layoutStyle.flex_grow
        }
    layoutStyle.flex_shrink =
        if (override.layoutStyle.flex_shrink != 0.0f) {
            override.layoutStyle.flex_shrink
        } else {
            base.layoutStyle.flex_shrink
        }
    layoutStyle.flex_basis =
        if (override.layoutStyle.flex_basis.getDim() !is Dimension.Undefined) {
            override.layoutStyle.flex_basis
        } else {
            base.layoutStyle.flex_basis
        }
    layoutStyle.bounding_box =
        if (
            override.layoutStyle.bounding_box.get().width != 0.0f ||
            override.layoutStyle.bounding_box.get().height != 0.0f
        ) {
            override.layoutStyle.bounding_box
        } else {
            base.layoutStyle.bounding_box
        }
    nodeStyle.horizontal_sizing =
        if (layoutSizingFromInt(override.nodeStyle.horizontal_sizing) !is LayoutSizing.Fixed) {
            override.nodeStyle.horizontal_sizing
        } else {
            base.nodeStyle.horizontal_sizing
        }
    nodeStyle.vertical_sizing =
        if (layoutSizingFromInt(override.nodeStyle.vertical_sizing) !is LayoutSizing.Fixed) {
            override.nodeStyle.vertical_sizing
        } else {
            base.nodeStyle.vertical_sizing
        }
    layoutStyle.width =
        if (override.layoutStyle.width.getDim() !is Dimension.Undefined) {
            override.layoutStyle.width
        } else {
            base.layoutStyle.width
        }
    layoutStyle.height =
        if (override.layoutStyle.height.getDim() !is Dimension.Undefined) {
            override.layoutStyle.height
        } else {
            base.layoutStyle.height
        }
    layoutStyle.min_width =
        if (override.layoutStyle.min_width.getDim() !is Dimension.Undefined) {
            override.layoutStyle.min_width
        } else {
            base.layoutStyle.min_width
        }
    layoutStyle.min_height =
        if (override.layoutStyle.min_height.getDim() !is Dimension.Undefined) {
            override.layoutStyle.min_height
        } else {
            base.layoutStyle.min_height
        }
    layoutStyle.max_width =
        if (override.layoutStyle.max_width.getDim() !is Dimension.Undefined) {
            override.layoutStyle.max_width
        } else {
            base.layoutStyle.max_width
        }
    layoutStyle.max_height =
        if (override.layoutStyle.max_height.getDim() !is Dimension.Undefined) {
            override.layoutStyle.max_height
        } else {
            base.layoutStyle.max_height
        }
    nodeStyle.aspect_ratio =
        if (override.nodeStyle.aspect_ratio.isPresent) {
            override.nodeStyle.aspect_ratio
        } else {
            base.nodeStyle.aspect_ratio
        }
    nodeStyle.pointer_events =
        if (pointerEventsFromInt(override.nodeStyle.pointer_events) !is PointerEvents.Auto) {
            override.nodeStyle.pointer_events
        } else {
            base.nodeStyle.pointer_events
        }
    nodeStyle.meter_data =
        if (override.nodeStyle.meter_data.isPresent) {
            override.nodeStyle.meter_data
        } else {
            base.nodeStyle.meter_data
        }
    style.layout_style = Optional.of(layoutStyle.build())
    style.node_style = Optional.of(nodeStyle.build())
    return style.build()
}

internal fun LayoutStyle.asBuilder(): LayoutStyle.Builder {
    val builder = LayoutStyle.Builder()
    builder.position_type = position_type
    builder.flex_direction = flex_direction
    builder.align_items = align_items
    builder.align_self = align_self
    builder.align_content = align_content
    builder.justify_content = justify_content
    builder.top = top
    builder.left = left
    builder.bottom = bottom
    builder.right = right
    builder.margin = margin
    builder.padding = padding
    builder.item_spacing = item_spacing
    builder.flex_grow = flex_grow
    builder.flex_shrink = flex_shrink
    builder.flex_basis = flex_basis
    builder.bounding_box = bounding_box
    builder.width = width
    builder.height = height
    builder.min_width = min_width
    builder.min_height = min_height
    builder.max_width = max_width
    builder.max_height = max_height
    return builder
}

internal fun defaultLayoutStyle(): LayoutStyle.Builder {
    val builder = LayoutStyle.Builder()
    builder.position_type = PositionType.Relative().toInt()
    builder.flex_direction = FlexDirection.Row().toInt()
    builder.align_items = AlignItems.FlexStart().toInt()
    builder.align_self = AlignSelf.Auto().toInt()
    builder.align_content = AlignContent.FlexStart().toInt()
    builder.justify_content = JustifyContent.FlexStart().toInt()
    builder.top = newDimensionProtoUndefined()
    builder.left = newDimensionProtoUndefined()
    builder.bottom = newDimensionProtoUndefined()
    builder.right = newDimensionProtoUndefined()
    builder.margin = newDimensionRectPointsZero()
    builder.padding = newDimensionRectPointsZero()
    builder.item_spacing = Optional.of(ItemSpacing(Optional.of(ItemSpacingType.Auto(Auto(0, 0)))))
    builder.flex_grow = 0.0f
    builder.flex_shrink = 0.0f
    builder.flex_basis = newDimensionProtoUndefined()
    builder.bounding_box = Optional.of(com.android.designcompose.serdegen.Size(0f, 0f))
    builder.width = newDimensionProtoUndefined()
    builder.height = newDimensionProtoUndefined()
    builder.min_width = newDimensionProtoUndefined()
    builder.min_height = newDimensionProtoUndefined()
    builder.max_width = newDimensionProtoUndefined()
    builder.max_height = newDimensionProtoUndefined()
    return builder
}

internal fun NodeStyle.asBuilder(): NodeStyle.Builder {
    val builder = NodeStyle.Builder()
    builder.text_color = text_color
    builder.font_size = font_size
    builder.font_family = font_family
    builder.font_weight = font_weight
    builder.font_style = font_style
    builder.text_decoration = text_decoration
    builder.letter_spacing = letter_spacing
    builder.font_stretch = font_stretch
    builder.backgrounds = backgrounds
    builder.box_shadows = box_shadows
    builder.stroke = stroke
    builder.opacity = opacity
    builder.transform = transform
    builder.relative_transform = relative_transform
    builder.text_align = text_align
    builder.text_align_vertical = text_align_vertical
    builder.text_overflow = text_overflow
    builder.text_shadow = text_shadow
    builder.node_size = node_size
    builder.line_height = line_height
    builder.line_count = line_count
    builder.font_features = font_features
    builder.filters = filters
    builder.backdrop_filters = backdrop_filters
    builder.blend_mode = blend_mode
    builder.hyperlinks = hyperlinks
    builder.display_type = display_type
    builder.flex_wrap = flex_wrap
    builder.grid_layout_type = grid_layout_type
    builder.grid_columns_rows = grid_columns_rows
    builder.grid_adaptive_min_size = grid_adaptive_min_size
    builder.grid_span_contents = grid_span_contents
    builder.overflow = overflow
    builder.max_children = max_children
    builder.overflow_node_id = overflow_node_id
    builder.overflow_node_name = overflow_node_name
    builder.cross_axis_item_spacing = cross_axis_item_spacing
    builder.horizontal_sizing = horizontal_sizing
    builder.vertical_sizing = vertical_sizing
    builder.aspect_ratio = aspect_ratio
    builder.pointer_events = pointer_events
    builder.meter_data = meter_data
    return builder
}

internal fun defaultNodeStyle(): NodeStyle.Builder {
    val builder = NodeStyle.Builder()
    builder.text_color =
        Optional.of(Background(Optional.of(BackgroundType.None(com.novi.serde.Unit()))))
    builder.font_size = Optional.of(newNumOrVar(0f))
    builder.font_family = Optional.empty()
    builder.font_weight = Optional.of(newFontWeight(0f))
    builder.font_style = FontStyle.Normal().toInt()
    builder.font_stretch = Optional.of(FontStretch(0f))
    builder.backgrounds = emptyList()
    builder.box_shadows = emptyList()
    builder.stroke =
        Optional.of(
            Stroke(
                strokeAlignTypeToInt(StrokeAlignType.Center),
                Optional.of(StrokeWeight(Optional.of(StrokeWeightType.Uniform(0f)))),
                emptyList(),
            )
        )
    builder.opacity = Optional.empty()
    builder.transform = Optional.empty()
    builder.relative_transform = Optional.empty()
    builder.text_decoration = TextDecoration.None().toInt()
    builder.text_align = TextAlign.Left().toInt()
    builder.text_align_vertical = TextAlignVertical.Top().toInt()
    builder.text_overflow = TextOverflow.Clip().toInt()
    builder.text_shadow = Optional.empty()
    builder.node_size = Optional.of(com.android.designcompose.serdegen.Size(0f, 0f))
    builder.line_height = Optional.of(LineHeight(Optional.of(LineHeightType.Percent(1.0f))))
    builder.line_count = Optional.empty()
    builder.letter_spacing = Optional.empty()
    builder.font_features = emptyList()
    builder.filters = emptyList()
    builder.backdrop_filters = emptyList()
    builder.blend_mode = BlendMode.PassThrough().toInt()
    builder.display_type = Display.Flex().toInt()
    builder.flex_wrap = FlexWrap.NoWrap().toInt()
    builder.grid_layout_type = Optional.empty()
    builder.grid_columns_rows = 0
    builder.grid_adaptive_min_size = 0
    builder.grid_span_contents = emptyList()
    builder.overflow = Overflow.Visible().toInt()
    builder.max_children = Optional.empty()
    builder.overflow_node_id = Optional.empty()
    builder.overflow_node_name = Optional.empty()
    builder.cross_axis_item_spacing = 0f
    builder.horizontal_sizing = LayoutSizing.Hug().toInt()
    builder.vertical_sizing = LayoutSizing.Hug().toInt()
    builder.aspect_ratio = Optional.empty()
    builder.pointer_events = PointerEvents.Auto().toInt()
    builder.meter_data = Optional.empty()
    builder.hyperlinks = Optional.empty()
    return builder
}

// XXX: Horrible code to deal with our terrible generated types. Maybe if style moves to proto then
//      we'll get some more egonomic generated classes.
internal fun ViewStyle.asBuilder(): ViewStyle.Builder {
    val builder = ViewStyle.Builder()
    builder.layout_style = layout_style
    builder.node_style = node_style
    return builder
}

// Get the raw width in a view style from the width property if it is a fixed size, or from the
// node_size property if not.
internal fun ViewStyle.fixedWidth(density: Float): Float {
    return if (layoutStyle.width.getDim() is Dimension.Points)
        layoutStyle.width.getDim().pointsAsDp(density).value
    else nodeStyle.node_size.get().width * density
}

// Get the raw height in a view style from the height property if it is a fixed size, or from the
// node_size property if not.
internal fun ViewStyle.fixedHeight(density: Float): Float {
    return if (layoutStyle.height.getDim() is Dimension.Points)
        layoutStyle.height.getDim().pointsAsDp(density).value
    else nodeStyle.node_size.get().height * density
}

// Return whether a text node is auto width without a FILL sizing mode. This is a check used by the
// text measure func that, when it returns true, means the text can expand past the available width
// passed into it.
internal fun ViewStyle.isAutoWidthText() =
    layoutStyle.width.getDim() is Dimension.Auto &&
            layoutSizingFromInt(nodeStyle.horizontal_sizing) !is LayoutSizing.Fill
