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

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.Shader
import android.os.Build
import android.util.Log
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isFinite
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.isIdentity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.designcompose.proto.end
import com.android.designcompose.proto.getDim
import com.android.designcompose.proto.isDefault
import com.android.designcompose.proto.newDimensionProtoUndefined
import com.android.designcompose.proto.newDimensionRectPointsZero
import com.android.designcompose.proto.start
import com.android.designcompose.proto.toOptDimProto
import com.android.designcompose.proto.top
import com.android.designcompose.serdegen.AffineTransform
import com.android.designcompose.serdegen.AlignContent
import com.android.designcompose.serdegen.AlignItems
import com.android.designcompose.serdegen.AlignSelf
import com.android.designcompose.serdegen.Background
import com.android.designcompose.serdegen.BlendMode
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.Display
import com.android.designcompose.serdegen.Easing
import com.android.designcompose.serdegen.FlexDirection
import com.android.designcompose.serdegen.FlexWrap
import com.android.designcompose.serdegen.FontStretch
import com.android.designcompose.serdegen.FontStyle
import com.android.designcompose.serdegen.FontWeight
import com.android.designcompose.serdegen.ItemSpacing
import com.android.designcompose.serdegen.JustifyContent
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.LayoutSizing
import com.android.designcompose.serdegen.LayoutStyle
import com.android.designcompose.serdegen.LayoutTransform
import com.android.designcompose.serdegen.LineHeight
import com.android.designcompose.serdegen.NodeStyle
import com.android.designcompose.serdegen.NumOrVar
import com.android.designcompose.serdegen.Overflow
import com.android.designcompose.serdegen.PointerEvents
import com.android.designcompose.serdegen.PositionType
import com.android.designcompose.serdegen.ScaleMode
import com.android.designcompose.serdegen.Stroke
import com.android.designcompose.serdegen.StrokeAlign
import com.android.designcompose.serdegen.StrokeCap
import com.android.designcompose.serdegen.StrokeWeight
import com.android.designcompose.serdegen.StyledTextRun
import com.android.designcompose.serdegen.TextAlign
import com.android.designcompose.serdegen.TextAlignVertical
import com.android.designcompose.serdegen.TextDecoration
import com.android.designcompose.serdegen.TextOverflow
import com.android.designcompose.serdegen.Transition
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewShape
import com.android.designcompose.serdegen.ViewStyle
import java.util.Optional
import kotlin.math.roundToInt
import kotlin.math.sqrt

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

    val left = style.layout_style.left.getDim().resolve(pw, density)
    val top = style.layout_style.top.getDim().resolve(ph, density)
    // Right and bottom are insets from the right/bottom edge, so convert them to be relative to
    // the top/left corner.
    val right = style.layout_style.right.getDim().resolve(pw, density)?.let { r -> pw - r }
    val bottom = style.layout_style.bottom.getDim().resolve(ph, density)?.let { b -> ph - b }
    val width = style.layout_style.width.getDim().resolve(pw, density)
    val height = style.layout_style.height.getDim().resolve(ph, density)
    // We use the top and left margins for center anchored items, so they can be safely applied
    // as an offset here.
    val leftMargin = style.layout_style.margin.start.resolve(pw, density) ?: 0
    val topMargin = style.layout_style.margin.end.resolve(ph, density) ?: 0

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

    val minWidth = style.layout_style.min_width.getDim().resolve(pw, density)
    val minHeight = style.layout_style.min_height.getDim().resolve(ph, density)
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

    var w = style.layout_style.width.getDim().resolve(pw, density) ?: 0
    var h = style.layout_style.height.getDim().resolve(ph, density) ?: 0
    // We use the top and left margins for center anchored items, so they can be safely applied
    // as an offset here.
    val x = style.layout_style.margin.start.resolve(pw, density) ?: 0
    val y = style.layout_style.margin.top.resolve(ph, density) ?: 0

    val minWidth = style.layout_style.min_width.getDim().resolve(pw, density)
    val minHeight = style.layout_style.min_height.getDim().resolve(ph, density)
    if (minWidth != null && w < minWidth) {
        w = minWidth
    }
    if (minHeight != null && h < minHeight) {
        h = minHeight
    }

    return Rect(x, y, x + w, y + h)
}

// We need this fudge factor to make the blurry shadow bounds match up roughly
// with Figma (which probably computes shadows entirely differently to Skia).
// It looks a bit like 1 / sqrt(2), but isn't.
internal const val blurFudgeFactor = 0.72f

// Merge styles; any non-default properties of the override style are copied over the base style.
internal fun mergeStyles(base: ViewStyle, override: ViewStyle): ViewStyle {
    val style = ViewStyle.Builder()
    val nodeStyle = NodeStyle.Builder()
    val layoutStyle = LayoutStyle.Builder()
    nodeStyle.text_color =
        if (override.node_style.text_color !is Background.None) {
            override.node_style.text_color
        } else {
            base.node_style.text_color
        }
    nodeStyle.font_size =
        if (override.node_style.font_size != NumOrVar.Num(18.0f)) {
            override.node_style.font_size
        } else {
            base.node_style.font_size
        }
    nodeStyle.font_weight =
        if (override.node_style.font_weight.value != NumOrVar.Num(400.0f)) {
            override.node_style.font_weight
        } else {
            base.node_style.font_weight
        }
    nodeStyle.font_style =
        if (override.node_style.font_style !is FontStyle.Normal) {
            override.node_style.font_style
        } else {
            base.node_style.font_style
        }
    nodeStyle.text_decoration =
        if (override.node_style.text_decoration !is TextDecoration.None) {
            override.node_style.text_decoration
        } else {
            base.node_style.text_decoration
        }
    nodeStyle.letter_spacing =
        if (override.node_style.letter_spacing.isPresent) {
            override.node_style.letter_spacing
        } else {
            base.node_style.letter_spacing
        }
    nodeStyle.font_family =
        if (override.node_style.font_family.isPresent) {
            override.node_style.font_family
        } else {
            base.node_style.font_family
        }
    nodeStyle.font_stretch =
        if (override.node_style.font_stretch.value != 1.0f) {
            override.node_style.font_stretch
        } else {
            base.node_style.font_stretch
        }
    nodeStyle.background =
        if (
            override.node_style.background.size > 0 &&
                override.node_style.background[0] !is Background.None
        ) {
            override.node_style.background
        } else {
            base.node_style.background
        }
    nodeStyle.box_shadow =
        if (override.node_style.box_shadow.size > 0) {
            override.node_style.box_shadow
        } else {
            base.node_style.box_shadow
        }
    nodeStyle.stroke =
        if (override.node_style.stroke.strokes.size > 0) {
            override.node_style.stroke
        } else {
            base.node_style.stroke
        }
    nodeStyle.opacity =
        if (override.node_style.opacity.isPresent) {
            override.node_style.opacity
        } else {
            base.node_style.opacity
        }
    nodeStyle.transform =
        if (override.node_style.transform.isPresent) {
            override.node_style.transform
        } else {
            base.node_style.transform
        }
    nodeStyle.relative_transform =
        if (override.node_style.relative_transform.isPresent) {
            override.node_style.relative_transform
        } else {
            base.node_style.relative_transform
        }
    nodeStyle.text_align =
        if (override.node_style.text_align !is TextAlign.Left) {
            override.node_style.text_align
        } else {
            base.node_style.text_align
        }
    nodeStyle.text_align_vertical =
        if (override.node_style.text_align_vertical !is TextAlignVertical.Top) {
            override.node_style.text_align_vertical
        } else {
            base.node_style.text_align_vertical
        }
    nodeStyle.text_overflow =
        if (override.node_style.text_overflow !is TextOverflow.Clip) {
            override.node_style.text_overflow
        } else {
            base.node_style.text_overflow
        }
    nodeStyle.text_shadow =
        if (override.node_style.text_shadow.isPresent) {
            override.node_style.text_shadow
        } else {
            base.node_style.text_shadow
        }
    nodeStyle.node_size =
        if (
            override.node_style.node_size.width != 0.0f ||
                override.node_style.node_size.height != 0.0f
        ) {
            override.node_style.node_size
        } else {
            base.node_style.node_size
        }
    nodeStyle.line_height =
        if (!override.node_style.line_height.equals(LineHeight.Percent(1.0f))) {
            override.node_style.line_height
        } else {
            base.node_style.line_height
        }
    nodeStyle.line_count =
        if (override.node_style.line_count.isPresent) {
            override.node_style.line_count
        } else {
            base.node_style.line_count
        }
    nodeStyle.font_features =
        if (override.node_style.font_features.size > 0) {
            override.node_style.font_features
        } else {
            base.node_style.font_features
        }
    nodeStyle.filter =
        if (override.node_style.filter.size > 0) {
            override.node_style.filter
        } else {
            base.node_style.filter
        }
    nodeStyle.backdrop_filter =
        if (override.node_style.backdrop_filter.size > 0) {
            override.node_style.backdrop_filter
        } else {
            base.node_style.backdrop_filter
        }
    nodeStyle.blend_mode =
        if (override.node_style.blend_mode is BlendMode.PassThrough) {
            override.node_style.blend_mode
        } else {
            base.node_style.blend_mode
        }
    nodeStyle.hyperlink =
        if (override.node_style.hyperlink.isPresent) {
            override.node_style.hyperlink
        } else {
            base.node_style.hyperlink
        }
    nodeStyle.display_type =
        if (override.node_style.display_type !is Display.flex) {
            override.node_style.display_type
        } else {
            base.node_style.display_type
        }
    layoutStyle.position_type =
        if (override.layout_style.position_type !is PositionType.Relative) {
            override.layout_style.position_type
        } else {
            base.layout_style.position_type
        }
    layoutStyle.flex_direction =
        if (override.layout_style.flex_direction !is FlexDirection.Row) {
            override.layout_style.flex_direction
        } else {
            base.layout_style.flex_direction
        }
    nodeStyle.flex_wrap =
        if (override.node_style.flex_wrap !is FlexWrap.NoWrap) {
            override.node_style.flex_wrap
        } else {
            base.node_style.flex_wrap
        }
    nodeStyle.grid_layout =
        if (override.node_style.grid_layout.isPresent) {
            override.node_style.grid_layout
        } else {
            base.node_style.grid_layout
        }
    nodeStyle.grid_columns_rows =
        if (override.node_style.grid_columns_rows > 0) {
            override.node_style.grid_columns_rows
        } else {
            base.node_style.grid_columns_rows
        }
    nodeStyle.grid_adaptive_min_size =
        if (override.node_style.grid_adaptive_min_size > 1) {
            override.node_style.grid_adaptive_min_size
        } else {
            base.node_style.grid_adaptive_min_size
        }
    nodeStyle.grid_span_content =
        override.node_style.grid_span_content.ifEmpty { base.node_style.grid_span_content }
    nodeStyle.overflow =
        if (override.node_style.overflow !is Overflow.Visible) {
            override.node_style.overflow
        } else {
            base.node_style.overflow
        }
    nodeStyle.max_children =
        if (override.node_style.max_children.isPresent) {
            override.node_style.max_children
        } else {
            base.node_style.max_children
        }
    nodeStyle.overflow_node_id =
        if (override.node_style.overflow_node_id.isPresent) {
            override.node_style.overflow_node_id
        } else {
            base.node_style.overflow_node_id
        }
    nodeStyle.overflow_node_name =
        if (override.node_style.overflow_node_name.isPresent) {
            override.node_style.overflow_node_name
        } else {
            base.node_style.overflow_node_name
        }
    layoutStyle.align_items =
        if (override.layout_style.align_items !is AlignItems.Stretch) {
            override.layout_style.align_items
        } else {
            base.layout_style.align_items
        }
    layoutStyle.align_self =
        if (override.layout_style.align_self !is AlignSelf.Auto) {
            override.layout_style.align_self
        } else {
            base.layout_style.align_self
        }
    layoutStyle.align_content =
        if (override.layout_style.align_content !is AlignContent.Stretch) {
            override.layout_style.align_content
        } else {
            base.layout_style.align_content
        }
    layoutStyle.justify_content =
        if (override.layout_style.justify_content !is JustifyContent.FlexStart) {
            override.layout_style.justify_content
        } else {
            base.layout_style.justify_content
        }
    layoutStyle.top =
        if (override.layout_style.top.getDim() !is Dimension.Undefined) {
            override.layout_style.top
        } else {
            base.layout_style.top
        }
    layoutStyle.left =
        if (override.layout_style.left.getDim() !is Dimension.Undefined) {
            override.layout_style.left
        } else {
            base.layout_style.left
        }
    layoutStyle.bottom =
        if (override.layout_style.bottom.getDim() !is Dimension.Undefined) {
            override.layout_style.bottom
        } else {
            base.layout_style.bottom
        }
    layoutStyle.right =
        if (override.layout_style.right.getDim() !is Dimension.Undefined) {
            override.layout_style.right
        } else {
            base.layout_style.right
        }

    layoutStyle.margin =
        if (!override.layout_style.margin.isDefault()) {
            override.layout_style.margin
        } else {
            base.layout_style.margin
        }
    layoutStyle.padding =
        if (!override.layout_style.padding.isDefault()) {
            override.layout_style.padding
        } else {
            base.layout_style.padding
        }
    fun ItemSpacing.isDefault(): Boolean {
        return this is ItemSpacing.Fixed && this.value == 0
    }
    layoutStyle.item_spacing =
        if (!override.layout_style.item_spacing.isDefault()) {
            override.layout_style.item_spacing
        } else {
            base.layout_style.item_spacing
        }
    nodeStyle.cross_axis_item_spacing =
        if (override.node_style.cross_axis_item_spacing != 0.0f) {
            override.node_style.cross_axis_item_spacing
        } else {
            base.node_style.cross_axis_item_spacing
        }
    layoutStyle.flex_grow =
        if (override.layout_style.flex_grow != 0.0f) {
            override.layout_style.flex_grow
        } else {
            base.layout_style.flex_grow
        }
    layoutStyle.flex_shrink =
        if (override.layout_style.flex_shrink != 0.0f) {
            override.layout_style.flex_shrink
        } else {
            base.layout_style.flex_shrink
        }
    layoutStyle.flex_basis =
        if (override.layout_style.flex_basis.getDim() !is Dimension.Undefined) {
            override.layout_style.flex_basis
        } else {
            base.layout_style.flex_basis
        }
    layoutStyle.bounding_box =
        if (
            override.layout_style.bounding_box.width != 0.0f ||
                override.layout_style.bounding_box.height != 0.0f
        ) {
            override.layout_style.bounding_box
        } else {
            base.layout_style.bounding_box
        }
    nodeStyle.horizontal_sizing =
        if (override.node_style.horizontal_sizing !is LayoutSizing.FIXED) {
            override.node_style.horizontal_sizing
        } else {
            base.node_style.horizontal_sizing
        }
    nodeStyle.vertical_sizing =
        if (override.node_style.vertical_sizing !is LayoutSizing.FIXED) {
            override.node_style.vertical_sizing
        } else {
            base.node_style.vertical_sizing
        }
    layoutStyle.width =
        if (override.layout_style.width.getDim() !is Dimension.Undefined) {
            override.layout_style.width
        } else {
            base.layout_style.width
        }
    layoutStyle.height =
        if (override.layout_style.height.getDim() !is Dimension.Undefined) {
            override.layout_style.height
        } else {
            base.layout_style.height
        }
    layoutStyle.min_width =
        if (override.layout_style.min_width.getDim() !is Dimension.Undefined) {
            override.layout_style.min_width
        } else {
            base.layout_style.min_width
        }
    layoutStyle.min_height =
        if (override.layout_style.min_height.getDim() !is Dimension.Undefined) {
            override.layout_style.min_height
        } else {
            base.layout_style.min_height
        }
    layoutStyle.max_width =
        if (override.layout_style.max_width.getDim() !is Dimension.Undefined) {
            override.layout_style.max_width
        } else {
            base.layout_style.max_width
        }
    layoutStyle.max_height =
        if (override.layout_style.max_height.getDim() !is Dimension.Undefined) {
            override.layout_style.max_height
        } else {
            base.layout_style.max_height
        }
    nodeStyle.aspect_ratio =
        if (
            override.node_style.aspect_ratio !is com.android.designcompose.serdegen.Number.Undefined
        ) {
            override.node_style.aspect_ratio
        } else {
            base.node_style.aspect_ratio
        }
    nodeStyle.pointer_events =
        if (override.node_style.pointer_events !is PointerEvents.Auto) {
            override.node_style.pointer_events
        } else {
            base.node_style.pointer_events
        }
    nodeStyle.meter_data =
        if (override.node_style.meter_data.isPresent) {
            override.node_style.meter_data
        } else {
            base.node_style.meter_data
        }
    style.layout_style = layoutStyle.build()
    style.node_style = nodeStyle.build()
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
    builder.position_type = PositionType.Relative()
    builder.flex_direction = FlexDirection.Row()
    builder.align_items = AlignItems.FlexStart()
    builder.align_self = AlignSelf.Auto()
    builder.align_content = AlignContent.FlexStart()
    builder.justify_content = JustifyContent.FlexStart()
    builder.top = newDimensionProtoUndefined()
    builder.left = newDimensionProtoUndefined()
    builder.bottom = newDimensionProtoUndefined()
    builder.right = newDimensionProtoUndefined()
    builder.margin = newDimensionRectPointsZero()
    builder.padding = newDimensionRectPointsZero()
    builder.item_spacing = ItemSpacing.Auto(0, 0)
    builder.flex_grow = 1.0f
    builder.flex_shrink = 0.0f
    builder.flex_basis = newDimensionProtoUndefined()
    builder.bounding_box = com.android.designcompose.serdegen.Size(0f, 0f)
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
    builder.background = background
    builder.box_shadow = box_shadow
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
    builder.filter = filter
    builder.backdrop_filter = backdrop_filter
    builder.blend_mode = blend_mode
    builder.hyperlink = hyperlink
    builder.display_type = display_type
    builder.flex_wrap = flex_wrap
    builder.grid_layout = grid_layout
    builder.grid_columns_rows = grid_columns_rows
    builder.grid_adaptive_min_size = grid_adaptive_min_size
    builder.grid_span_content = grid_span_content
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
    builder.text_color = Background.None()
    builder.font_size = NumOrVar.Num(0f)
    builder.font_family = Optional.empty()
    builder.font_weight = FontWeight(NumOrVar.Num(0f))
    builder.font_style = FontStyle.Normal()
    builder.font_stretch = FontStretch(0f)
    builder.background = emptyList()
    builder.box_shadow = emptyList()
    builder.stroke = Stroke(StrokeAlign.Center(), StrokeWeight.Uniform(0f), emptyList())
    builder.opacity = Optional.empty()
    builder.transform = Optional.empty()
    builder.relative_transform = Optional.empty()
    builder.text_decoration = TextDecoration.None()
    builder.text_align = TextAlign.Left()
    builder.text_align_vertical = TextAlignVertical.Top()
    builder.text_overflow = TextOverflow.Clip()
    builder.text_shadow = Optional.empty()
    builder.node_size = com.android.designcompose.serdegen.Size(0f, 0f)
    builder.line_height = LineHeight.Percent(1.0f)
    builder.line_count = Optional.empty()
    builder.letter_spacing = Optional.empty()
    builder.font_features = emptyList()
    builder.filter = emptyList()
    builder.backdrop_filter = emptyList()
    builder.blend_mode = BlendMode.PassThrough()
    builder.display_type = Display.flex()
    builder.flex_wrap = FlexWrap.NoWrap()
    builder.grid_layout = Optional.empty()
    builder.grid_columns_rows = 0
    builder.grid_adaptive_min_size = 0
    builder.grid_span_content = emptyList()
    builder.overflow = Overflow.Visible()
    builder.max_children = Optional.empty()
    builder.overflow_node_id = Optional.empty()
    builder.overflow_node_name = Optional.empty()
    builder.cross_axis_item_spacing = 0f
    builder.horizontal_sizing = LayoutSizing.HUG()
    builder.vertical_sizing = LayoutSizing.HUG()
    builder.aspect_ratio = com.android.designcompose.serdegen.Number.Undefined()
    builder.pointer_events = PointerEvents.Auto()
    builder.meter_data = Optional.empty()
    builder.hyperlink = Optional.empty()
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

// Take the external layout fields of ViewStyle and put them into an ExternalLayoutData object.
internal fun ViewStyle.externalLayoutData(): ExternalLayoutData {
    return ExternalLayoutData(
        layout_style.margin.orElseThrow { NoSuchFieldException("Malformed data: Margin unset") },
        layout_style.top.getDim(),
        layout_style.left.getDim(),
        layout_style.bottom.getDim(),
        layout_style.right.getDim(),
        layout_style.width.getDim(),
        layout_style.height.getDim(),
        layout_style.min_width.getDim(),
        layout_style.min_height.getDim(),
        layout_style.max_width.getDim(),
        layout_style.max_height.getDim(),
        node_style.node_size,
        layout_style.bounding_box,
        layout_style.flex_grow,
        layout_style.flex_basis.getDim(),
        layout_style.align_self,
        layout_style.position_type,
        node_style.transform,
        node_style.relative_transform,
    )
}

// Take the external layout data and merge it into this ViewStyle, overriding its values and
// returning a new ViewStyle.
internal fun ViewStyle.withExternalLayoutData(data: ExternalLayoutData): ViewStyle {
    val layoutStyle = layout_style.asBuilder()
    val nodeStyle = node_style.asBuilder()
    layoutStyle.margin = Optional.of(data.margin)
    layoutStyle.top = data.top.toOptDimProto()
    layoutStyle.left = data.left.toOptDimProto()
    layoutStyle.bottom = data.bottom.toOptDimProto()
    layoutStyle.right = data.right.toOptDimProto()
    nodeStyle.node_size = data.nodeSize
    layoutStyle.bounding_box = data.boundingBox
    layoutStyle.width = data.width.toOptDimProto()
    layoutStyle.height = data.height.toOptDimProto()

    layoutStyle.min_width = data.minWidth.toOptDimProto()
    layoutStyle.min_height = data.minHeight.toOptDimProto()
    layoutStyle.max_width = data.maxWidth.toOptDimProto()
    layoutStyle.max_height = data.maxHeight.toOptDimProto()
    layoutStyle.flex_grow = data.flexGrow
    layoutStyle.flex_basis = data.flexBasis.toOptDimProto()
    layoutStyle.align_self = data.alignSelf
    layoutStyle.position_type = data.positionType
    nodeStyle.transform = data.transform
    nodeStyle.relative_transform = data.relativeTransform

    val overrideStyle = this.asBuilder()
    overrideStyle.layout_style = layoutStyle.build()
    overrideStyle.node_style = nodeStyle.build()
    return overrideStyle.build()
}

// Get the raw width in a view style from the width property if it is a fixed size, or from the
// node_size property if not.
internal fun ViewStyle.fixedWidth(density: Float): Float {
    return if (layout_style.width.getDim() is Dimension.Points)
        layout_style.width.getDim().pointsAsDp(density).value
    else node_style.node_size.width * density
}

// Get the raw height in a view style from the height property if it is a fixed size, or from the
// node_size property if not.
internal fun ViewStyle.fixedHeight(density: Float): Float {
    return if (layout_style.height.getDim() is Dimension.Points)
        layout_style.height.getDim().pointsAsDp(density).value
    else node_style.node_size.height * density
}

internal fun validateFigmaDocId(id: String): Boolean {
    val alphanumericRegex = "[a-zA-Z0-9]+".toRegex()
    return alphanumericRegex.matches(id)
}

/** Convert a BlendMode to the Compose form. */
internal fun BlendMode.asComposeBlendMode() =
    when (this) {
        is BlendMode.PassThrough -> androidx.compose.ui.graphics.BlendMode.SrcOver
        is BlendMode.Normal -> androidx.compose.ui.graphics.BlendMode.SrcOver
        is BlendMode.Darken -> androidx.compose.ui.graphics.BlendMode.Darken
        is BlendMode.Multiply -> androidx.compose.ui.graphics.BlendMode.Multiply
        is BlendMode.ColorBurn -> androidx.compose.ui.graphics.BlendMode.ColorBurn
        is BlendMode.Lighten -> androidx.compose.ui.graphics.BlendMode.Lighten
        is BlendMode.Screen -> androidx.compose.ui.graphics.BlendMode.Screen
        is BlendMode.ColorDodge -> androidx.compose.ui.graphics.BlendMode.ColorDodge
        is BlendMode.Overlay -> androidx.compose.ui.graphics.BlendMode.Overlay
        is BlendMode.SoftLight -> androidx.compose.ui.graphics.BlendMode.Softlight
        is BlendMode.HardLight -> androidx.compose.ui.graphics.BlendMode.Hardlight
        is BlendMode.Difference -> androidx.compose.ui.graphics.BlendMode.Difference
        is BlendMode.Exclusion -> androidx.compose.ui.graphics.BlendMode.Exclusion
        is BlendMode.Hue -> androidx.compose.ui.graphics.BlendMode.Hue
        is BlendMode.Saturation -> androidx.compose.ui.graphics.BlendMode.Saturation
        is BlendMode.Color -> androidx.compose.ui.graphics.BlendMode.Color
        is BlendMode.Luminosity -> androidx.compose.ui.graphics.BlendMode.Luminosity
        // Unsupported
        is BlendMode.LinearBurn -> androidx.compose.ui.graphics.BlendMode.ColorBurn
        is BlendMode.LinearDodge -> androidx.compose.ui.graphics.BlendMode.ColorDodge
        else -> androidx.compose.ui.graphics.BlendMode.SrcOver
    }

/** Does the BlendMode need a layer? */
internal fun BlendMode.useLayer() =
    when (this) {
        is BlendMode.PassThrough -> false
        else -> true
    }

internal fun View.hasChildMask(): Boolean {
    if (data is ViewData.Container) {
        (data as ViewData.Container).children.forEach { child -> if (child.isMask()) return true }
    }
    return false
}

internal fun View.isMask(): Boolean {
    return this.data is ViewData.Container && (this.data as ViewData.Container).shape.isMask()
}

// Returns whether this view should use infinite constraints on its children. This is true if two
// things are true:
// First, the view has a child that has a grid_layout field in its style, meaning it was created
// using the list preview widget.
// Second, the position_type is relative, which is only set if the widget layout parameters are set
// to hug contents.
internal fun View.useInfiniteConstraints(): Boolean {
    if (style.layout_style.position_type !is PositionType.Relative) return false

    if (data !is ViewData.Container) return false

    val container = data as ViewData.Container
    if (container.children.size != 1) return false

    val child = container.children.first()
    return child.style.node_style.grid_layout.isPresent
}

internal fun ViewShape.isMask(): Boolean {
    when (this) {
        is ViewShape.Rect -> return is_mask
        is ViewShape.RoundRect -> return is_mask
        is ViewShape.Path -> return is_mask
        is ViewShape.Arc -> return is_mask
        is ViewShape.VectorRect -> return is_mask
    }
    return false
}

// Figma expresses gradients in relative terms (offsets are between 0..1), but the built-in
// LinearGradient and RadialGradient types in Compose use absolute pixel offsets. These
// classes re-implement the Compose gradient brush types, but multiply out the offsets at
// draw time.
//
// DesignCompose doesn't use the intrinsic brush size for layout, so we don't need to know
// the gradient pixel size in advance as Compose apps generally do.
internal class RelativeLinearGradient
internal constructor(
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
    private val start: Offset,
    private val end: Offset,
    private val tileMode: TileMode = TileMode.Clamp,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val startX = if (start.x == Float.POSITIVE_INFINITY) size.width else start.x * size.width
        val startY = if (start.y == Float.POSITIVE_INFINITY) size.height else start.y * size.height
        val endX = if (end.x == Float.POSITIVE_INFINITY) size.width else end.x * size.width
        val endY = if (end.y == Float.POSITIVE_INFINITY) size.height else end.y * size.height
        return androidx.compose.ui.graphics.LinearGradientShader(
            colors = colors,
            colorStops = stops,
            from = Offset(startX, startY),
            to = Offset(endX, endY),
            tileMode = tileMode,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelativeLinearGradient) return false

        if (colors != other.colors) return false
        if (stops != other.stops) return false
        if (start != other.start) return false
        if (end != other.end) return false
        if (tileMode != other.tileMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = colors.hashCode()
        result = 31 * result + (stops?.hashCode() ?: 0)
        result = 31 * result + start.hashCode()
        result = 31 * result + end.hashCode()
        result = 31 * result + tileMode.hashCode()
        return result
    }

    override fun toString(): String {
        val startValue = if (start.isFinite) "start=$start, " else ""
        val endValue = if (end.isFinite) "end=$end, " else ""
        return "RelativeLinearGradient(colors=$colors, " +
            "stops=$stops, " +
            startValue +
            endValue +
            "tileMode=$tileMode)"
    }
}

internal class RelativeRadialGradient
internal constructor(
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
    private val center: Offset,
    private val radiusX: Float,
    private val radiusY: Float,
    private val angle: Float,
    private val tileMode: TileMode = TileMode.Clamp,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val centerX: Float
        val centerY: Float
        if (center.isUnspecified) {
            val drawCenter = size.center
            centerX = drawCenter.x
            centerY = drawCenter.y
        } else {
            centerX = if (center.x == Float.POSITIVE_INFINITY) size.width else center.x * size.width
            centerY =
                if (center.y == Float.POSITIVE_INFINITY) size.height else center.y * size.height
        }

        // Don't let radius be 0
        val radius =
            if (radiusX == Float.POSITIVE_INFINITY) size.minDimension / 2
            else if (size.width > 0F) radiusX * size.width else 0.01F
        val shader =
            RadialGradientShader(
                colors = colors,
                colorStops = stops,
                center = Offset(centerX, centerY),
                radius = radius,
                tileMode = tileMode,
            )

        val transform = Matrix()
        transform.postScale(1.0f, radiusY / radiusX, center.x * size.width, center.y * size.height)
        transform.postRotate(
            angle * 180.0f / Math.PI.toFloat() - 90.0f,
            center.x * size.width,
            center.y * size.height,
        )
        shader.setLocalMatrix(transform)

        return shader
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelativeRadialGradient) return false

        if (colors != other.colors) return false
        if (stops != other.stops) return false
        if (center != other.center) return false
        if (radiusX != other.radiusX) return false
        if (radiusY != other.radiusY) return false
        if (angle != other.angle) return false
        if (tileMode != other.tileMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = colors.hashCode()
        result = 31 * result + (stops?.hashCode() ?: 0)
        result = 31 * result + center.hashCode()
        result = 31 * result + radiusX.hashCode()
        result = 31 * result + radiusY.hashCode()
        result = 31 * result + angle.hashCode()
        result = 31 * result + tileMode.hashCode()
        return result
    }

    override fun toString(): String {
        val centerValue = if (center.isSpecified) "center=$center, " else ""
        val radiusXValue = if (radiusX.isFinite()) "radiusX=$radiusX, " else ""
        val radiusYValue = if (radiusY.isFinite()) "radiusY=$radiusY, " else ""
        val angleValue = if (angle.isFinite()) "angle=$angle, " else ""
        return "RelativeRadialGradient(" +
            "colors=$colors, " +
            "stops=$stops, " +
            centerValue +
            radiusXValue +
            radiusYValue +
            angleValue +
            "tileMode=$tileMode)"
    }
}

internal class RelativeSweepGradient
internal constructor(
    private val center: Offset,
    private val angle: Float,
    private val scale: Float,
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
) : ShaderBrush() {

    override fun createShader(size: Size): Shader {
        val shader =
            SweepGradientShader(
                if (center.isUnspecified) {
                    size.center
                } else {
                    Offset(
                        if (center.x == Float.POSITIVE_INFINITY) size.width
                        else center.x * size.width,
                        if (center.y == Float.POSITIVE_INFINITY) size.height
                        else center.y * size.height,
                    )
                },
                colors,
                stops,
            )

        // Use a local transform to apply the scale and rotation factors.
        val transform = Matrix()
        transform.postScale(scale, 1.0f, center.x * size.width, center.y * size.height)
        transform.postRotate(
            angle * 180.0f / Math.PI.toFloat() - 90.0f,
            center.x * size.width,
            center.y * size.height,
        )
        shader.setLocalMatrix(transform)

        return shader
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelativeSweepGradient) return false

        if (center != other.center) return false
        if (colors != other.colors) return false
        if (stops != other.stops) return false
        if (scale != other.scale) return false
        if (angle != other.angle) return false

        return true
    }

    override fun hashCode(): Int {
        var result = center.hashCode()
        result = 31 * result + colors.hashCode()
        result = 31 * result + (stops?.hashCode() ?: 0)
        result = 31 * result + scale.hashCode()
        result = 31 * result + angle.hashCode()
        return result
    }

    override fun toString(): String {
        val centerValue = if (center.isSpecified) "center=$center, " else ""
        val angleValue = if (angle.isFinite()) "angle=$angle, " else ""
        return "RelativeSweepGradient(" + centerValue + angleValue + "colors=$colors, stops=$stops)"
    }
}

internal class RelativeImageFill
internal constructor(
    private val image: android.graphics.Bitmap,
    private val imageDensity: Float,
    private val displayDensity: Float,
    private val imageTransform: Matrix?,
    private val scaleMode: ScaleMode,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        // The transform depends on the scale mode; we use it to position and rotate the
        // image.
        var tileMode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Shader.TileMode.DECAL
            } else {
                Shader.TileMode.CLAMP
            }
        val m = Matrix()
        when (scaleMode) {
            is ScaleMode.Tile -> {
                // Scale the image such that normalized coordinates are device coordinates.
                m.setScale(displayDensity / imageDensity, displayDensity / imageDensity)
                tileMode = Shader.TileMode.REPEAT
                if (imageTransform != null) m.preConcat(imageTransform)
            }
            is ScaleMode.Fill -> {
                // Scale the image such that it fills the `size` completely, cropping one
                // axis if required.
                var scale = size.width / image.width.toFloat()
                if (scale * image.height.toFloat() < size.height) {
                    scale = size.height / image.height.toFloat()
                }
                m.setScale(scale, scale)
                m.postTranslate(
                    -(image.width.toFloat() * scale - size.width) / 2.0f,
                    -(image.height.toFloat() * scale - size.height) / 2.0f,
                )
                if (imageTransform != null) m.preConcat(imageTransform)
            }
            is ScaleMode.Fit -> {
                // Scale the image such that it fits the `size`, with empty space instead
                // of cropping.
                var scale = size.width / image.width.toFloat()
                if (scale * image.height.toFloat() > size.height) {
                    scale = size.height / image.height.toFloat()
                }
                m.setScale(scale, scale)
                m.postTranslate(
                    -(image.width.toFloat() * scale - size.width) / 2.0f,
                    -(image.height.toFloat() * scale - size.height) / 2.0f,
                )
                if (imageTransform != null) m.preConcat(imageTransform)
            }
            is ScaleMode.Stretch -> {
                // An identity imageTransform means that the fill should fit the bounds of the
                // container, so the last matrix operation is to scale the image to stretch to
                // the container bounds. Otherwise, the imageTransform here is in a normalized
                // image space, so we need to scale the transform by the image dimensions for
                // it to apply correctly.
                m.postScale(1.0f / image.width.toFloat(), 1.0f / image.height.toFloat())
                if (imageTransform != null) m.postConcat(imageTransform)
                m.postScale(size.width, size.height)
            }
        }

        val bitmapShader = BitmapShader(image, tileMode, tileMode)
        if (!m.isIdentity) bitmapShader.setLocalMatrix(m)
        return bitmapShader
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelativeImageFill) return false

        if (image != other.image) return false
        if (imageDensity != other.imageDensity) return false
        if (displayDensity != other.displayDensity) return false
        if (imageTransform != other.imageTransform) return false
        if (scaleMode != other.scaleMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = image.hashCode()
        result = 31 * result + imageDensity.hashCode()
        result = 31 * result + displayDensity.hashCode()
        result = 31 * result + (imageTransform?.hashCode() ?: 0)
        result = 31 * result + scaleMode.hashCode()
        return result
    }

    override fun toString(): String {
        return "RelativeImageFill"
    }
}

/**
 * Convert a LayoutTransform to a Compose transformation matrix, adjusted to operate on pixels at
 * the given display density.
 *
 * XXX: Doesn't consider transform origin.
 */
internal fun Optional<LayoutTransform>.asComposeTransform(
    density: Float
): androidx.compose.ui.graphics.Matrix? {
    return map {
            val transform =
                androidx.compose.ui.graphics.Matrix(
                    floatArrayOf(
                        it.m11,
                        it.m12,
                        it.m13,
                        it.m14,
                        it.m21,
                        it.m22,
                        it.m23,
                        it.m24,
                        it.m31,
                        it.m32,
                        it.m33,
                        it.m34,
                        it.m41,
                        it.m42,
                        it.m43,
                        it.m44
                    )
                )
            if (transform.isIdentity()) {
                null
            } else {
                val adjust = androidx.compose.ui.graphics.Matrix()
                adjust.scale(1.0f / density, 1.0f / density, 1.0f / density)
                adjust.timesAssign(transform)
                val unadjust = androidx.compose.ui.graphics.Matrix()
                unadjust.scale(density, density, density)
                adjust.timesAssign(unadjust)
                adjust
            }
        }
        .orElse(null)
}

internal fun Optional<AffineTransform>.asSkiaMatrix(): Matrix? {
    return map {
            val skMatrix = Matrix()
            skMatrix.setValues(
                floatArrayOf(it.m11, it.m12, it.m31, it.m21, it.m22, it.m32, 0.0f, 0.0f, 1.0f)
            )
            skMatrix
        }
        .orElse(null)
}

/** Convert a Background to a Brush, returning a Pair of Brush and Opacity */
internal fun Background.asBrush(
    appContext: Context,
    document: DocContent,
    density: Float,
    variableState: VariableState,
): Pair<Brush, Float>? {
    when (this) {
        is Background.Solid -> {
            val color = value.getValue(variableState)
            return color?.let { Pair(SolidColor(color), 1.0f) }
        }
        is Background.Image -> {
            val backgroundImage = this
            val imageTransform = backgroundImage.transform.asSkiaMatrix()
            if (DebugNodeManager.getUseLocalRes().value) {
                backgroundImage.res_name.orElse(null)?.let {
                    val resId =
                        appContext.resources.getIdentifier(it, "drawable", appContext.packageName)
                    if (resId != Resources.ID_NULL) {
                        val bitmap =
                            BitmapFactoryWithCache.loadResource(appContext.resources, resId)
                        return Pair(
                            RelativeImageFill(
                                image = bitmap,
                                imageDensity = density,
                                displayDensity = density,
                                imageTransform = imageTransform,
                                scaleMode = backgroundImage.scale_mode
                            ),
                            backgroundImage.opacity
                        )
                    } else {
                        Log.w(TAG, "No drawable resource $it found")
                    }
                }
            }
            val imageFillAndDensity =
                backgroundImage.key.orElse(null)?.let { document.image(it.value, density) }
            // val imageFilters = backgroundImage.filters;
            if (imageFillAndDensity != null) {
                val (imageFill, imageDensity) = imageFillAndDensity
                return Pair(
                    RelativeImageFill(
                        image = imageFill,
                        imageDensity = imageDensity,
                        displayDensity = density,
                        imageTransform = imageTransform,
                        scaleMode = backgroundImage.scale_mode,
                    ),
                    backgroundImage.opacity,
                )
            }
        }
        is Background.LinearGradient -> {
            val linearGradient = this
            when (linearGradient.color_stops.size) {
                0 -> {
                    Log.e(TAG, "No stops found for the linear gradient")
                    return null
                }
                1 -> {
                    Log.w(TAG, "Single stop found for the linear gradient and do it as a fill")
                    val color =
                        linearGradient.color_stops[0].field1.getValue(variableState)
                            ?: Color.Transparent
                    return Pair(SolidColor(color), 1.0f)
                }
                else ->
                    return Pair(
                        RelativeLinearGradient(
                            linearGradient.color_stops
                                .map { it.field1.getValue(variableState) ?: Color.Transparent }
                                .toList(),
                            linearGradient.color_stops.map { it.field0 }.toList(),
                            start = Offset(linearGradient.start_x, linearGradient.start_y),
                            end = Offset(linearGradient.end_x, linearGradient.end_y),
                        ),
                        1.0f,
                    )
            }
        }
        is Background.RadialGradient -> {
            val radialGradient = this
            return when (radialGradient.color_stops.size) {
                0 -> {
                    Log.e(TAG, "No stops found for the radial gradient")
                    return null
                }
                1 -> {
                    Log.w(TAG, "Single stop found for the radial gradient and do it as a fill")
                    return Pair(
                        SolidColor(
                            radialGradient.color_stops[0].field1.getValue(variableState)
                                ?: Color.Transparent
                        ),
                        1.0f,
                    )
                }
                else ->
                    return Pair(
                        RelativeRadialGradient(
                            colors =
                                radialGradient.color_stops.map {
                                    it.field1.getValue(variableState) ?: Color.Transparent
                                },
                            stops = radialGradient.color_stops.map { it.field0 },
                            center = Offset(radialGradient.center_x, radialGradient.center_y),
                            radiusX = radialGradient.radius[0],
                            radiusY = radialGradient.radius[1],
                            angle = radialGradient.angle,
                        ),
                        1.0f,
                    )
            }
        }
        is Background.AngularGradient -> {
            val angularGradient = this
            return when (angularGradient.color_stops.size) {
                0 -> {
                    Log.e(TAG, "No stops found for the angular gradient")
                    return null
                }
                1 -> {
                    Log.w(TAG, "Single stop found for the angular gradient and do it as a fill")
                    return Pair(
                        SolidColor(
                            angularGradient.color_stops[0].field1.getValue(variableState)
                                ?: Color.Transparent
                        ),
                        1.0f,
                    )
                }
                else ->
                    return Pair(
                        RelativeSweepGradient(
                            center = Offset(angularGradient.center_x, angularGradient.center_y),
                            angle = angularGradient.angle,
                            scale = angularGradient.scale,
                            colors =
                                angularGradient.color_stops.map {
                                    it.field1.getValue(variableState) ?: Color.Transparent
                                },
                            stops = angularGradient.color_stops.map { it.field0 },
                        ),
                        1.0f,
                    )
            }
        }
    }
    return null
}

internal fun com.android.designcompose.serdegen.Path.asPath(
    density: Float,
    scaleX: Float,
    scaleY: Float,
): Path {
    val MOVE_TO: Byte = 0
    val LINE_TO: Byte = 1
    val CUBIC_TO: Byte = 2
    val QUAD_TO: Byte = 3
    val CLOSE: Byte = 4

    val p = Path()
    p.fillType =
        when (this.winding_rule.toInt()) {
            // winding_rule is defined in path.proto as an enum, but the generated Rust file has it
            // as an i32. Since we generate a java file from the Rust from using serdegen, we lose
            // the original enum values and need to hardcode them here. This can be fixed once we
            // generate java directly from the proto file.
            // 1 -> NonZero,
            // 2 -> EvenOdd
            2 -> PathFillType.EvenOdd
            else -> PathFillType.NonZero
        }
    var idx = 0
    for (cmd in this.commands) {
        when (cmd) {
            MOVE_TO -> {
                p.moveTo(this.data[idx++] * density * scaleX, this.data[idx++] * density * scaleY)
            }
            LINE_TO -> {
                p.lineTo(this.data[idx++] * density * scaleX, this.data[idx++] * density * scaleY)
            }
            CUBIC_TO -> {
                p.cubicTo(
                    this.data[idx++] * density * scaleX,
                    this.data[idx++] * density * scaleY,
                    this.data[idx++] * density * scaleX,
                    this.data[idx++] * density * scaleY,
                    this.data[idx++] * density * scaleX,
                    this.data[idx++] * density * scaleY,
                )
            }
            QUAD_TO -> {
                p.quadraticBezierTo(
                    this.data[idx++] * density * scaleX,
                    this.data[idx++] * density * scaleY,
                    this.data[idx++] * density * scaleX,
                    this.data[idx++] * density * scaleY,
                )
            }
            CLOSE -> {
                p.close()
            }
        }
    }
    return p
}

internal fun StrokeCap.toComposeStrokeCap(): androidx.compose.ui.graphics.StrokeCap {
    return when (this) {
        is StrokeCap.ROUND -> androidx.compose.ui.graphics.StrokeCap.Round
        is StrokeCap.SQUARE -> androidx.compose.ui.graphics.StrokeCap.Square
        else -> androidx.compose.ui.graphics.StrokeCap.Butt
    }
}

internal fun com.android.designcompose.serdegen.Path.log() {
    val MOVE_TO: Byte = 0
    val LINE_TO: Byte = 1
    val CUBIC_TO: Byte = 2
    val QUAD_TO: Byte = 3
    val CLOSE: Byte = 4

    var idx = 0
    for (cmd in this.commands) {
        when (cmd) {
            MOVE_TO -> {
                Log.e(TAG, "Move To ${this.data[idx++]}+${this.data[idx++]}")
            }
            LINE_TO -> {
                Log.e(TAG, "Line To ${this.data[idx++]}+${this.data[idx++]}")
            }
            CUBIC_TO -> {
                Log.e(
                    TAG,
                    "Cubic To ${this.data[idx++]}+${this.data[idx++]} ${this.data[idx++]}+${this.data[idx++]} ${this.data[idx++]}+${this.data[idx++]}",
                )
            }
            QUAD_TO -> {
                Log.e(
                    TAG,
                    "Quad To ${this.data[idx++]}+${this.data[idx++]} ${this.data[idx++]}+${this.data[idx++]}",
                )
            }
            CLOSE -> {
                Log.e(TAG, "Close")
            }
        }
    }
}

// Return a "uniform" stroke weight even if we have individual weights. This is used for stroking
// vectors that don't have sides.
internal fun StrokeWeight.toUniform(): Float {
    when (this) {
        is StrokeWeight.Uniform -> return this.value
        is StrokeWeight.Individual -> return this.top
    }
    return 0.0f
}

// Return a maximum stroke weight. This is used for computing the layer bounds when creating a
// layer for compositing (transparency, blend modes, etc).
internal fun StrokeWeight.max(): Float {
    when (this) {
        is StrokeWeight.Uniform -> return this.value
        is StrokeWeight.Individual -> return maxOf(this.top, this.left, this.bottom, this.right)
    }
    return 0.0f
}

internal fun StrokeWeight.top(): Float {
    when (this) {
        is StrokeWeight.Uniform -> return this.value
        is StrokeWeight.Individual -> return this.top
    }
    return 0.0f
}

internal fun StrokeWeight.left(): Float {
    when (this) {
        is StrokeWeight.Uniform -> return this.value
        is StrokeWeight.Individual -> return this.left
    }
    return 0.0f
}

internal fun StrokeWeight.bottom(): Float {
    when (this) {
        is StrokeWeight.Uniform -> return this.value
        is StrokeWeight.Individual -> return this.bottom
    }
    return 0.0f
}

internal fun StrokeWeight.right(): Float {
    when (this) {
        is StrokeWeight.Uniform -> return this.value
        is StrokeWeight.Individual -> return this.right
    }
    return 0.0f
}

// Return whether a text node is auto width without a FILL sizing mode. This is a check used by the
// text measure func that, when it returns true, means the text can expand past the available width
// passed into it.
internal fun ViewStyle.isAutoWidthText() =
    layout_style.width.getDim() is Dimension.Auto &&
        node_style.horizontal_sizing !is LayoutSizing.FILL

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
        if (hasModifiedSize || style.layout_style.width.getDim() !is Dimension.Points)
            layoutSize.width
        else style.layout_style.width.getDim().pointsAsDp(density).value
    val height =
        if (hasModifiedSize || style.layout_style.height.getDim() !is Dimension.Points)
            layoutSize.height
        else style.layout_style.height.getDim().pointsAsDp(density).value
    return Size(width, height)
}

internal fun com.android.designcompose.serdegen.Size.isValid(): Boolean = width >= 0 && height >= 0

internal fun Layout.withDensity(density: Float): Layout {
    return Layout(
        this.order,
        this.width * density,
        this.height * density,
        this.left * density,
        this.top * density,
    )
}

// Convert a DesignCompose animation transition into a Jetpack Compose animationSpec.
internal fun Transition.asAnimationSpec(): AnimationSpec<Float> {
    val easing =
        when (this) {
            is Transition.SmartAnimate -> this.easing
            is Transition.ScrollAnimate -> this.easing
            is Transition.Push -> this.easing
            is Transition.MoveIn -> this.easing
            is Transition.MoveOut -> this.easing
            is Transition.Dissolve -> this.easing
            is Transition.SlideIn -> this.easing
            is Transition.SlideOut -> this.easing
            else -> return snap(0)
        }
    val duration =
        when (this) {
            is Transition.SmartAnimate -> this.duration
            is Transition.ScrollAnimate -> this.duration
            is Transition.Push -> this.duration
            is Transition.MoveIn -> this.duration
            is Transition.MoveOut -> this.duration
            is Transition.Dissolve -> this.duration
            is Transition.SlideIn -> this.duration
            is Transition.SlideOut -> this.duration
            else -> return snap(0)
        }
    return when (easing) {
        is Easing.Spring -> {
            // Compose takes damping as a fraction of the amount required for critical damping,
            // rather than as the actual damping value. So, we must calculate the damping required
            // for criticality with the given stiffness and mass.
            //
            // Reference implementation of a simple spring based on integrating Hooke's law:
            //  https://github.com/iamralpht/gravitas-rs/blob/master/src/spring.rs#L23

            val critical = sqrt(4.0f * easing.value.stiffness * easing.value.mass)
            spring(
                dampingRatio = easing.value.damping / critical,
                stiffness = easing.value.stiffness,
            )
        }
        is Easing.Bezier -> {
            tween(
                durationMillis = (duration * 1000.0).roundToInt(),
                easing =
                    CubicBezierEasing(
                        easing.value.x1,
                        easing.value.y1,
                        easing.value.x2,
                        easing.value.y2,
                    ),
            )
        }
        else -> snap(0)
    }
}

/** Convert a serialized color to a Compose color */
internal fun com.android.designcompose.serdegen.Color.toColor(): Color {
    val a = a.toInt()
    val r = r.toInt()
    val g = g.toInt()
    val b = b.toInt()
    return Color(r, g, b, a)
}

internal fun getTextContent(
    context: Context,
    textData: ViewData.Text,
): String {
    if (DebugNodeManager.getUseLocalRes().value && textData.res_name.isPresent) {
        val resName = textData.res_name.get()
        val resId = context.resources.getIdentifier(resName, "string", context.packageName)
        if (resId != Resources.ID_NULL) {
            return context.getString(resId)
        } else {
            Log.w(TAG, "No string resource $resName found")
        }
    }
    return textData.content
}

internal fun getTextContent(
    context: Context,
    styledTextData: ViewData.StyledText,
): List<StyledTextRun> {
    if (DebugNodeManager.getUseLocalRes().value && styledTextData.res_name.isPresent) {
        val resName = styledTextData.res_name.get()
        val strArrayResId = context.resources.getIdentifier(resName, "array", context.packageName)
        if (strArrayResId != Resources.ID_NULL) {
            val textArray = context.resources.getStringArray(strArrayResId)
            if (textArray.size == styledTextData.content.size) {
                val output = mutableListOf<StyledTextRun>()
                for (i in textArray.indices) {
                    output.add(StyledTextRun(textArray[i], styledTextData.content[i].style))
                }
                return output
            } else {
                Log.w(TAG, "String array size mismatched the styled runs")
            }
        }
        Log.w(TAG, "No string array resource $resName found for styled runs")
        if (styledTextData.content.size == 1) {
            val strResId = context.resources.getIdentifier(resName, "string", context.packageName)
            if (strResId != Resources.ID_NULL) {
                Log.w(TAG, "Single style found, fallback to string resource")
                return mutableListOf(
                    StyledTextRun(context.getString(strResId), styledTextData.content[0].style)
                )
            } else {
                Log.w(TAG, "No string resource $resName found for styled runs")
            }
        }
    }
    return styledTextData.content
}
