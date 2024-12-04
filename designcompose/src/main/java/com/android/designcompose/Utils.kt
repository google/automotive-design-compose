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
import androidx.annotation.RequiresApi
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
import com.android.designcompose.proto.StrokeAlignType
import com.android.designcompose.proto.WindingRuleType
import com.android.designcompose.proto.alignContentFromInt
import com.android.designcompose.proto.alignItemsFromInt
import com.android.designcompose.proto.alignSelfFromInt
import com.android.designcompose.proto.blendModeFromInt
import com.android.designcompose.proto.displayFromInt
import com.android.designcompose.proto.end
import com.android.designcompose.proto.flexDirectionFromInt
import com.android.designcompose.proto.flexWrapFromInt
import com.android.designcompose.proto.fontStyleFromInt
import com.android.designcompose.proto.get
import com.android.designcompose.proto.getDim
import com.android.designcompose.proto.getType
import com.android.designcompose.proto.isDefault
import com.android.designcompose.proto.isType
import com.android.designcompose.proto.justifyContentFromInt
import com.android.designcompose.proto.layoutSizingFromInt
import com.android.designcompose.proto.layoutStyle
import com.android.designcompose.proto.newDimensionProtoUndefined
import com.android.designcompose.proto.newDimensionRectPointsZero
import com.android.designcompose.proto.newFontWeight
import com.android.designcompose.proto.newNumOrVar
import com.android.designcompose.proto.nodeStyle
import com.android.designcompose.proto.overflowDirectionFromInt
import com.android.designcompose.proto.overflowFromInt
import com.android.designcompose.proto.pointerEventsFromInt
import com.android.designcompose.proto.positionTypeFromInt
import com.android.designcompose.proto.scaleModeFromInt
import com.android.designcompose.proto.start
import com.android.designcompose.proto.strokeAlignTypeToInt
import com.android.designcompose.proto.textAlignFromInt
import com.android.designcompose.proto.textAlignVerticalFromInt
import com.android.designcompose.proto.textDecorationFromInt
import com.android.designcompose.proto.textOverflowFromInt
import com.android.designcompose.proto.toInt
import com.android.designcompose.proto.toOptDimProto
import com.android.designcompose.proto.top
import com.android.designcompose.proto.type
import com.android.designcompose.proto.windingRuleFromInt
import com.android.designcompose.serdegen.AffineTransform
import com.android.designcompose.serdegen.AlignContent
import com.android.designcompose.serdegen.AlignItems
import com.android.designcompose.serdegen.AlignSelf
import com.android.designcompose.serdegen.Auto
import com.android.designcompose.serdegen.Background
import com.android.designcompose.serdegen.BackgroundType
import com.android.designcompose.serdegen.BlendMode
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.Display
import com.android.designcompose.serdegen.EasingType
import com.android.designcompose.serdegen.FlexDirection
import com.android.designcompose.serdegen.FlexWrap
import com.android.designcompose.serdegen.FontStretch
import com.android.designcompose.serdegen.FontStyle
import com.android.designcompose.serdegen.ItemSpacing
import com.android.designcompose.serdegen.ItemSpacingType
import com.android.designcompose.serdegen.JustifyContent
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.LayoutSizing
import com.android.designcompose.serdegen.LayoutStyle
import com.android.designcompose.serdegen.LayoutTransform
import com.android.designcompose.serdegen.LineHeight
import com.android.designcompose.serdegen.LineHeightType
import com.android.designcompose.serdegen.NodeStyle
import com.android.designcompose.serdegen.NumOrVarType
import com.android.designcompose.serdegen.Overflow
import com.android.designcompose.serdegen.OverflowDirection
import com.android.designcompose.serdegen.PointerEvents
import com.android.designcompose.serdegen.PositionType
import com.android.designcompose.serdegen.ScaleMode
import com.android.designcompose.serdegen.Shape
import com.android.designcompose.serdegen.Stroke
import com.android.designcompose.serdegen.StrokeCap
import com.android.designcompose.serdegen.StrokeWeight
import com.android.designcompose.serdegen.StrokeWeightType
import com.android.designcompose.serdegen.StyledTextRun
import com.android.designcompose.serdegen.TextAlign
import com.android.designcompose.serdegen.TextAlignVertical
import com.android.designcompose.serdegen.TextDecoration
import com.android.designcompose.serdegen.TextOverflow
import com.android.designcompose.serdegen.Transition
import com.android.designcompose.serdegen.TransitionType
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewDataType
import com.android.designcompose.serdegen.ViewShape
import com.android.designcompose.serdegen.ViewStyle
import java.util.Optional
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull
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

// Take the external layout fields of ViewStyle and put them into an ExternalLayoutData object.
internal fun ViewStyle.externalLayoutData(): ExternalLayoutData {
    return ExternalLayoutData(
        layoutStyle.margin.orElseThrow { NoSuchFieldException("Malformed data: Margin unset") },
        layoutStyle.top.getDim(),
        layoutStyle.left.getDim(),
        layoutStyle.bottom.getDim(),
        layoutStyle.right.getDim(),
        layoutStyle.width.getDim(),
        layoutStyle.height.getDim(),
        layoutStyle.min_width.getDim(),
        layoutStyle.min_height.getDim(),
        layoutStyle.max_width.getDim(),
        layoutStyle.max_height.getDim(),
        nodeStyle.node_size.getOrDefault(com.android.designcompose.serdegen.Size(0f, 0f)),
        layoutStyle.bounding_box.get(),
        layoutStyle.flex_grow,
        layoutStyle.flex_basis.getDim(),
        alignSelfFromInt(layoutStyle.align_self),
        positionTypeFromInt(layoutStyle.position_type),
        nodeStyle.transform,
        nodeStyle.relative_transform,
    )
}

// Take the external layout data and merge it into this ViewStyle, overriding its values and
// returning a new ViewStyle.
internal fun ViewStyle.withExternalLayoutData(data: ExternalLayoutData): ViewStyle {
    val layoutStyle = layoutStyle.asBuilder()
    val nodeStyle = nodeStyle.asBuilder()
    layoutStyle.margin = Optional.of(data.margin)
    layoutStyle.top = data.top.toOptDimProto()
    layoutStyle.left = data.left.toOptDimProto()
    layoutStyle.bottom = data.bottom.toOptDimProto()
    layoutStyle.right = data.right.toOptDimProto()
    nodeStyle.node_size = Optional.of(data.nodeSize)
    layoutStyle.bounding_box = Optional.of(data.boundingBox)
    layoutStyle.width = data.width.toOptDimProto()
    layoutStyle.height = data.height.toOptDimProto()

    layoutStyle.min_width = data.minWidth.toOptDimProto()
    layoutStyle.min_height = data.minHeight.toOptDimProto()
    layoutStyle.max_width = data.maxWidth.toOptDimProto()
    layoutStyle.max_height = data.maxHeight.toOptDimProto()
    layoutStyle.flex_grow = data.flexGrow
    layoutStyle.flex_basis = data.flexBasis.toOptDimProto()
    layoutStyle.align_self = data.alignSelf.toInt()
    layoutStyle.position_type = data.positionType.toInt()
    nodeStyle.transform = data.transform
    nodeStyle.relative_transform = data.relativeTransform

    val overrideStyle = this.asBuilder()
    overrideStyle.layout_style = Optional.of(layoutStyle.build())
    overrideStyle.node_style = Optional.of(nodeStyle.build())
    return overrideStyle.build()
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
    val containerData = data.get()?.view_data_type?.get()
    if (containerData is ViewDataType.Container) {
        return containerData.value.children.any { it.isMask() }
    }
    return false
}

internal fun View.isMask(): Boolean {
    val containerData = data.get()?.view_data_type?.get()
    return if (containerData is ViewDataType.Container) {
        containerData.value.shape.get().isMask()
    } else false
}

// Returns whether this view should use infinite constraints on its children. This is true if two
// things are true:
// First, the view has a child that has a grid_layout field in its style, meaning it was created
// using the list preview widget.
// Second, the position_type is relative, which is only set if the widget layout parameters are set
// to hug contents.
internal fun View.useInfiniteConstraints(): Boolean {
    if (positionTypeFromInt(style.get().layoutStyle.position_type) !is PositionType.Relative)
        return false

    val containerData = data.get()?.view_data_type?.get()
    if (containerData !is ViewDataType.Container) return false

    val container = containerData as ViewDataType.Container
    if (container.value.children.size != 1) return false

    val child = container.value.children.first()
    return child.style.get().nodeStyle.grid_layout_type.isPresent
}

internal fun View.hasScrolling(): Boolean {
    return when (scroll_info.getOrNull()?.overflow?.let { overflowDirectionFromInt(it) }) {
        is OverflowDirection.HorizontalScrolling -> true
        is OverflowDirection.VerticalScrolling -> true
        else -> false
    }
}

internal fun ViewShape.isMask(): Boolean {
    when (val shape = this.get()) {
        is Shape.Rect -> return shape.value.is_mask
        is Shape.RoundRect -> return shape.value.is_mask
        is Shape.Path -> return shape.value.is_mask
        is Shape.Arc -> return shape.value.is_mask
        is Shape.VectorRect -> return shape.value.is_mask
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
                        it.m44,
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
@RequiresApi(Build.VERSION_CODES.N)
internal fun Background.asBrush(
    appContext: Context,
    document: DocContent,
    density: Float,
    variableState: VariableState,
): Pair<Brush, Float>? {
    when (val bgType = getType()) {
        is BackgroundType.Solid -> {
            val color = bgType.value.getValue(variableState)
            return color?.let { Pair(SolidColor(color), 1.0f) }
        }
        is BackgroundType.Image -> {
            val backgroundImage = bgType.value
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
                                scaleMode = scaleModeFromInt(backgroundImage.scale_mode),
                            ),
                            backgroundImage.opacity,
                        )
                    } else {
                        Log.w(TAG, "No drawable resource $it found")
                    }
                }
            }
            val imageFillAndDensity =
                backgroundImage.key.orElse(null)?.let { document.image(it.key, density) }
            // val imageFilters = backgroundImage.filters;
            if (imageFillAndDensity != null) {
                val (imageFill, imageDensity) = imageFillAndDensity
                return Pair(
                    RelativeImageFill(
                        image = imageFill,
                        imageDensity = imageDensity,
                        displayDensity = density,
                        imageTransform = imageTransform,
                        scaleMode = scaleModeFromInt(backgroundImage.scale_mode),
                    ),
                    backgroundImage.opacity,
                )
            }
        }
        is BackgroundType.LinearGradient -> {
            val linearGradient = bgType.value
            when (linearGradient.color_stops.size) {
                0 -> {
                    Log.e(TAG, "No stops found for the linear gradient")
                    return null
                }
                1 -> {
                    Log.w(TAG, "Single stop found for the linear gradient and do it as a fill")
                    val color =
                        linearGradient.color_stops[0].color.getOrNull()?.getValue(variableState)
                            ?: Color.Transparent
                    return Pair(SolidColor(color), 1.0f)
                }
                else ->
                    return Pair(
                        RelativeLinearGradient(
                            linearGradient.color_stops
                                .map {
                                    it.color.getOrNull()?.getValue(variableState)
                                        ?: Color.Transparent
                                }
                                .toList(),
                            linearGradient.color_stops.map { it.position }.toList(),
                            start = Offset(linearGradient.start_x, linearGradient.start_y),
                            end = Offset(linearGradient.end_x, linearGradient.end_y),
                        ),
                        1.0f,
                    )
            }
        }
        is BackgroundType.RadialGradient -> {
            val radialGradient = bgType.value
            return when (radialGradient.color_stops.size) {
                0 -> {
                    Log.e(TAG, "No stops found for the radial gradient")
                    return null
                }
                1 -> {
                    Log.w(TAG, "Single stop found for the radial gradient and do it as a fill")
                    return Pair(
                        SolidColor(
                            radialGradient.color_stops[0].color.getOrNull()?.getValue(variableState)
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
                                    it.color.getOrNull()?.getValue(variableState)
                                        ?: Color.Transparent
                                },
                            stops = radialGradient.color_stops.map { it.position },
                            center = Offset(radialGradient.center_x, radialGradient.center_y),
                            radiusX = radialGradient.radius_x,
                            radiusY = radialGradient.radius_y,
                            angle = radialGradient.angle,
                        ),
                        1.0f,
                    )
            }
        }
        is BackgroundType.AngularGradient -> {
            val angularGradient = bgType.value
            return when (angularGradient.color_stops.size) {
                0 -> {
                    Log.e(TAG, "No stops found for the angular gradient")
                    return null
                }
                1 -> {
                    Log.w(TAG, "Single stop found for the angular gradient and do it as a fill")
                    return Pair(
                        SolidColor(
                            angularGradient.color_stops[0]
                                .color
                                .getOrNull()
                                ?.getValue(variableState) ?: Color.Transparent
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
                                    it.color.getOrNull()?.getValue(variableState)
                                        ?: Color.Transparent
                                },
                            stops = angularGradient.color_stops.map { it.position },
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
        when (windingRuleFromInt(winding_rule.toInt())) {
            WindingRuleType.EvenOdd -> PathFillType.EvenOdd
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
        is StrokeCap.Round -> androidx.compose.ui.graphics.StrokeCap.Round
        is StrokeCap.Square -> androidx.compose.ui.graphics.StrokeCap.Square
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

// Return whether a text node is auto width without a FILL sizing mode. This is a check used by the
// text measure func that, when it returns true, means the text can expand past the available width
// passed into it.
internal fun ViewStyle.isAutoWidthText() =
    layoutStyle.width.getDim() is Dimension.Auto &&
        layoutSizingFromInt(nodeStyle.horizontal_sizing) !is LayoutSizing.Fill

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

internal fun com.android.designcompose.serdegen.Size.isValid(): Boolean = width >= 0 && height >= 0

internal fun Layout.withDensity(density: Float): Layout {
    return Layout(
        this.order,
        this.width * density,
        this.height * density,
        this.left * density,
        this.top * density,
        this.content_width * density,
        this.content_height * density,
    )
}

// Convert a DesignCompose animation transition into a Jetpack Compose animationSpec.
internal fun Transition.asAnimationSpec(): AnimationSpec<Float> {
    val transitionType = this.transition_type.getOrNull()
    val easing =
        when (transitionType) {
            is TransitionType.SmartAnimate -> transitionType.value.easing.get().easing_type.get()
            is TransitionType.ScrollAnimate -> transitionType.value.easing.get().easing_type.get()
            is TransitionType.Push -> transitionType.value.easing.get().easing_type.get()
            is TransitionType.MoveIn -> transitionType.value.easing.get().easing_type.get()
            is TransitionType.MoveOut -> transitionType.value.easing.get().easing_type.get()
            is TransitionType.Dissolve -> transitionType.value.easing.get().easing_type.get()
            is TransitionType.SlideIn -> transitionType.value.easing.get().easing_type.get()
            is TransitionType.SlideOut -> transitionType.value.easing.get().easing_type.get()
            else -> return snap(0)
        }
    val duration =
        when (transitionType) {
            is TransitionType.SmartAnimate -> transitionType.value.duration
            is TransitionType.ScrollAnimate -> transitionType.value.duration
            is TransitionType.Push -> transitionType.value.duration
            is TransitionType.MoveIn -> transitionType.value.duration
            is TransitionType.MoveOut -> transitionType.value.duration
            is TransitionType.Dissolve -> transitionType.value.duration
            is TransitionType.SlideIn -> transitionType.value.duration
            is TransitionType.SlideOut -> transitionType.value.duration
            else -> return snap(0)
        }
    return when (easing) {
        is EasingType.Spring -> {
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
        is EasingType.Bezier -> {
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

internal fun getTextContent(context: Context, textData: ViewDataType.Text): String {
    if (DebugNodeManager.getUseLocalRes().value && textData.value.res_name.isPresent) {
        val resName = textData.value.res_name.get()
        val resId = context.resources.getIdentifier(resName, "string", context.packageName)
        if (resId != Resources.ID_NULL) {
            return context.getString(resId)
        } else {
            Log.w(TAG, "No string resource $resName found")
        }
    }
    return textData.value.content
}

internal fun getTextContent(
    context: Context,
    styledTextData: ViewDataType.StyledText,
): List<StyledTextRun> {
    if (DebugNodeManager.getUseLocalRes().value && styledTextData.value.res_name.isPresent) {
        val resName = styledTextData.value.res_name.get()
        val strArrayResId = context.resources.getIdentifier(resName, "array", context.packageName)
        if (strArrayResId != Resources.ID_NULL) {
            val textArray = context.resources.getStringArray(strArrayResId)
            if (textArray.size == styledTextData.value.styled_texts.size) {
                val output = mutableListOf<StyledTextRun>()
                for (i in textArray.indices) {
                    output.add(
                        StyledTextRun(textArray[i], styledTextData.value.styled_texts[i].style)
                    )
                }
                return output
            } else {
                Log.w(TAG, "String array size mismatched the styled runs")
            }
        }
        Log.w(TAG, "No string array resource $resName found for styled runs")
        if (styledTextData.value.styled_texts.size == 1) {
            val strResId = context.resources.getIdentifier(resName, "string", context.packageName)
            if (strResId != Resources.ID_NULL) {
                Log.w(TAG, "Single style found, fallback to string resource")
                return mutableListOf(
                    StyledTextRun(
                        context.getString(strResId),
                        styledTextData.value.styled_texts[0].style,
                    )
                )
            } else {
                Log.w(TAG, "No string resource $resName found for styled runs")
            }
        }
    }
    return styledTextData.value.styled_texts
}
