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

import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.Shader
import android.os.Build
import android.util.Log
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
import com.android.designcompose.serdegen.AlignContent
import com.android.designcompose.serdegen.AlignItems
import com.android.designcompose.serdegen.AlignSelf
import com.android.designcompose.serdegen.Background
import com.android.designcompose.serdegen.BlendMode
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.Display
import com.android.designcompose.serdegen.FlexDirection
import com.android.designcompose.serdegen.FlexWrap
import com.android.designcompose.serdegen.FontStyle
import com.android.designcompose.serdegen.ItemSpacing
import com.android.designcompose.serdegen.JustifyContent
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.LayoutSizing
import com.android.designcompose.serdegen.LineHeight
import com.android.designcompose.serdegen.Overflow
import com.android.designcompose.serdegen.PointerEvents
import com.android.designcompose.serdegen.PositionType
import com.android.designcompose.serdegen.ScaleMode
import com.android.designcompose.serdegen.StrokeWeight
import com.android.designcompose.serdegen.TextAlign
import com.android.designcompose.serdegen.TextAlignVertical
import com.android.designcompose.serdegen.TextOverflow
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewShape
import com.android.designcompose.serdegen.ViewStyle
import com.android.designcompose.serdegen.WindingRule
import kotlin.math.roundToInt

/** Convert a serialized color to a Compose color */
internal fun convertColor(color: com.android.designcompose.serdegen.Color): Color {
    val a = color.color[3].toInt()
    val r = color.color[0].toInt()
    val g = color.color[1].toInt()
    val b = color.color[2].toInt()
    return Color(r, g, b, a)
}

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

    val left = style.left.resolve(pw, density)
    val top = style.top.resolve(ph, density)
    // Right and bottom are insets from the right/bottom edge, so convert them to be relative to
    // the top/left corner.
    val right = style.right.resolve(pw, density)?.let { r -> pw - r }
    val bottom = style.bottom.resolve(ph, density)?.let { b -> ph - b }
    val width = style.width.resolve(pw, density)
    val height = style.height.resolve(ph, density)
    // We use the top and left margins for center anchored items, so they can be safely applied
    // as an offset here.
    val leftMargin = style.margin.start.resolve(pw, density) ?: 0
    val topMargin = style.margin.top.resolve(ph, density) ?: 0

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

    val minWidth = style.min_width.resolve(pw, density)
    val minHeight = style.min_height.resolve(ph, density)
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

    var w = style.width.resolve(pw, density) ?: 0
    var h = style.height.resolve(ph, density) ?: 0
    // We use the top and left margins for center anchored items, so they can be safely applied
    // as an offset here.
    val x = style.margin.start.resolve(pw, density) ?: 0
    val y = style.margin.top.resolve(ph, density) ?: 0

    val minWidth = style.min_width.resolve(pw, density)
    val minHeight = style.min_height.resolve(ph, density)
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
    style.text_color =
        if (override.text_color !is Background.None) {
            override.text_color
        } else {
            base.text_color
        }
    style.font_size =
        if (override.font_size != 18.0f) {
            override.font_size
        } else {
            base.font_size
        }
    style.font_weight =
        if (override.font_weight.value != 400.0f) {
            override.font_weight
        } else {
            base.font_weight
        }
    style.font_style =
        if (override.font_style !is FontStyle.Normal) {
            override.font_style
        } else {
            base.font_style
        }
    style.font_family =
        if (override.font_family.isPresent) {
            override.font_family
        } else {
            base.font_family
        }
    style.font_stretch =
        if (override.font_stretch.value != 1.0f) {
            override.font_stretch
        } else {
            base.font_stretch
        }
    style.background =
        if (override.background.size > 0 && override.background[0] !is Background.None) {
            override.background
        } else {
            base.background
        }
    style.box_shadow =
        if (override.box_shadow.size > 0) {
            override.box_shadow
        } else {
            base.box_shadow
        }
    style.stroke =
        if (override.stroke.strokes.size > 0) {
            override.stroke
        } else {
            base.stroke
        }
    style.opacity =
        if (override.opacity.isPresent) {
            override.opacity
        } else {
            base.opacity
        }
    style.transform =
        if (override.transform.isPresent) {
            override.transform
        } else {
            base.transform
        }
    style.relative_transform =
        if (override.relative_transform.isPresent) {
            override.relative_transform
        } else {
            base.relative_transform
        }
    style.text_align =
        if (override.text_align !is TextAlign.Left) {
            override.text_align
        } else {
            base.text_align
        }
    style.text_align_vertical =
        if (override.text_align_vertical !is TextAlignVertical.Top) {
            override.text_align_vertical
        } else {
            base.text_align_vertical
        }
    style.text_overflow =
        if (override.text_overflow !is TextOverflow.Clip) {
            override.text_overflow
        } else {
            base.text_overflow
        }
    style.text_shadow =
        if (override.text_shadow.isPresent) {
            override.text_shadow
        } else {
            base.text_shadow
        }
    style.node_size =
        if (override.node_size.width != 0.0f || override.node_size.height != 0.0f) {
            override.node_size
        } else {
            base.node_size
        }
    style.line_height =
        if (!override.line_height.equals(LineHeight.Percent(1.0f))) {
            override.line_height
        } else {
            base.line_height
        }
    style.line_count =
        if (override.line_count.isPresent) {
            override.line_count
        } else {
            base.line_count
        }
    style.font_features =
        if (override.font_features.size > 0) {
            override.font_features
        } else {
            base.font_features
        }
    style.filter =
        if (override.filter.size > 0) {
            override.filter
        } else {
            base.filter
        }
    style.backdrop_filter =
        if (override.backdrop_filter.size > 0) {
            override.backdrop_filter
        } else {
            base.backdrop_filter
        }
    style.blend_mode =
        if (override.blend_mode is BlendMode.PassThrough) {
            override.blend_mode
        } else {
            base.blend_mode
        }
    style.display_type =
        if (override.display_type !is Display.flex) {
            override.display_type
        } else {
            base.display_type
        }
    style.position_type =
        if (override.position_type !is PositionType.Relative) {
            override.position_type
        } else {
            base.position_type
        }
    style.flex_direction =
        if (override.flex_direction !is FlexDirection.Row) {
            override.flex_direction
        } else {
            base.flex_direction
        }
    style.flex_wrap =
        if (override.flex_wrap !is FlexWrap.NoWrap) {
            override.flex_wrap
        } else {
            base.flex_wrap
        }
    style.grid_layout =
        if (override.grid_layout.isPresent) {
            override.grid_layout
        } else {
            base.grid_layout
        }
    style.grid_columns_rows =
        if (override.grid_columns_rows > 0) {
            override.grid_columns_rows
        } else {
            base.grid_columns_rows
        }
    style.grid_adaptive_min_size =
        if (override.grid_adaptive_min_size > 1) {
            override.grid_adaptive_min_size
        } else {
            base.grid_adaptive_min_size
        }
    style.grid_span_content = override.grid_span_content.ifEmpty { base.grid_span_content }
    style.overflow =
        if (override.overflow !is Overflow.Visible) {
            override.overflow
        } else {
            base.overflow
        }
    style.max_children =
        if (override.max_children.isPresent) {
            override.max_children
        } else {
            base.max_children
        }
    style.overflow_node_id =
        if (override.overflow_node_id.isPresent) {
            override.overflow_node_id
        } else {
            base.overflow_node_id
        }
    style.overflow_node_name =
        if (override.overflow_node_name.isPresent) {
            override.overflow_node_name
        } else {
            base.overflow_node_name
        }
    style.align_items =
        if (override.align_items !is AlignItems.Stretch) {
            override.align_items
        } else {
            base.align_items
        }
    style.align_self =
        if (override.align_self !is AlignSelf.Auto) {
            override.align_self
        } else {
            base.align_self
        }
    style.align_content =
        if (override.align_content !is AlignContent.Stretch) {
            override.align_content
        } else {
            base.align_content
        }
    style.justify_content =
        if (override.justify_content !is JustifyContent.FlexStart) {
            override.justify_content
        } else {
            base.justify_content
        }
    style.top =
        if (override.top !is Dimension.Undefined) {
            override.top
        } else {
            base.top
        }
    style.left =
        if (override.left !is Dimension.Undefined) {
            override.left
        } else {
            base.left
        }
    style.bottom =
        if (override.bottom !is Dimension.Undefined) {
            override.bottom
        } else {
            base.bottom
        }
    style.right =
        if (override.right !is Dimension.Undefined) {
            override.right
        } else {
            base.right
        }
    fun com.android.designcompose.serdegen.Rect.isDefault(): Boolean {
        return this.start is Dimension.Undefined &&
            this.end is Dimension.Undefined &&
            this.top is Dimension.Undefined &&
            this.bottom is Dimension.Undefined
    }
    style.margin =
        if (!override.margin.isDefault()) {
            override.margin
        } else {
            base.margin
        }
    style.padding =
        if (!override.padding.isDefault()) {
            override.padding
        } else {
            base.padding
        }
    fun ItemSpacing.isDefault(): Boolean {
        return this is ItemSpacing.Fixed && this.value == 0
    }
    style.item_spacing =
        if (!override.item_spacing.isDefault()) {
            override.item_spacing
        } else {
            base.item_spacing
        }
    style.cross_axis_item_spacing =
        if (override.cross_axis_item_spacing != 0.0f) {
            override.cross_axis_item_spacing
        } else {
            base.cross_axis_item_spacing
        }
    style.flex_grow =
        if (override.flex_grow != 0.0f) {
            override.flex_grow
        } else {
            base.flex_grow
        }
    style.flex_shrink =
        if (override.flex_shrink != 0.0f) {
            override.flex_shrink
        } else {
            base.flex_shrink
        }
    style.flex_basis =
        if (override.flex_basis !is Dimension.Undefined) {
            override.flex_basis
        } else {
            base.flex_basis
        }
    style.bounding_box =
        if (override.bounding_box.width != 0.0f || override.bounding_box.height != 0.0f) {
            override.bounding_box
        } else {
            base.bounding_box
        }
    style.horizontal_sizing =
        if (override.horizontal_sizing !is LayoutSizing.FIXED) {
            override.horizontal_sizing
        } else {
            base.horizontal_sizing
        }
    style.vertical_sizing =
        if (override.vertical_sizing !is LayoutSizing.FIXED) {
            override.vertical_sizing
        } else {
            base.vertical_sizing
        }
    style.width =
        if (override.width !is Dimension.Undefined) {
            override.width
        } else {
            base.width
        }
    style.height =
        if (override.height !is Dimension.Undefined) {
            override.height
        } else {
            base.height
        }
    style.min_width =
        if (override.min_width !is Dimension.Undefined) {
            override.min_width
        } else {
            base.min_width
        }
    style.min_height =
        if (override.min_height !is Dimension.Undefined) {
            override.min_height
        } else {
            base.min_height
        }
    style.max_width =
        if (override.max_width !is Dimension.Undefined) {
            override.max_width
        } else {
            base.max_width
        }
    style.max_height =
        if (override.max_height !is Dimension.Undefined) {
            override.max_height
        } else {
            base.max_height
        }
    style.aspect_ratio =
        if (override.aspect_ratio !is com.android.designcompose.serdegen.Number.Undefined) {
            override.aspect_ratio
        } else {
            base.aspect_ratio
        }
    style.pointer_events =
        if (override.pointer_events !is PointerEvents.Auto) {
            override.pointer_events
        } else {
            base.pointer_events
        }
    style.meter_data =
        if (override.meter_data.isPresent) {
            override.meter_data
        } else {
            base.meter_data
        }
    return style.build()
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

// Returns whether this view is the parent view of a list widget preview node. Since the list
// widget preview node also has a child that is the actual parent of the custom content, we need
// to check that the grandchild of this view has the grid_layout set in its style.
internal fun View.isWidgetParent(): Boolean {
    if (data !is ViewData.Container) return false

    val container = data as ViewData.Container
    if (container == null || container.children.size != 1) return false

    val child = container.children.first()
    if (child.data !is ViewData.Container) return false

    val childContainer = child.data as ViewData.Container
    if (childContainer == null || childContainer.children.size != 1) return false

    val grandChild = childContainer.children.first()
    return grandChild.style.grid_layout.isPresent
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
    private val tileMode: TileMode = TileMode.Clamp
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
            tileMode = tileMode
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
    private val tileMode: TileMode = TileMode.Clamp
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
                tileMode = tileMode
            )

        val transform = Matrix()
        transform.postScale(1.0f, radiusY / radiusX, center.x * size.width, center.y * size.height)
        transform.postRotate(
            angle * 180.0f / Math.PI.toFloat() - 90.0f,
            center.x * size.width,
            center.y * size.height
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
    private val stops: List<Float>? = null
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
                        else center.y * size.height
                    )
                },
                colors,
                stops
            )

        // Use a local transform to apply the scale and rotation factors.
        val transform = Matrix()
        transform.postScale(scale, 1.0f, center.x * size.width, center.y * size.height)
        transform.postRotate(
            angle * 180.0f / Math.PI.toFloat() - 90.0f,
            center.x * size.width,
            center.y * size.height
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
                    -(image.height.toFloat() * scale - size.height) / 2.0f
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
                    -(image.height.toFloat() * scale - size.height) / 2.0f
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
internal fun java.util.Optional<List<Float>>.asComposeTransform(
    density: Float
): androidx.compose.ui.graphics.Matrix? {
    return map {
            if (it.size != 16) {
                null
            } else {
                val transform = androidx.compose.ui.graphics.Matrix(it.toFloatArray())
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
        }
        .orElse(null)
}

internal fun java.util.Optional<List<Float>>.asSkiaMatrix(): Matrix? {
    return map {
            if (it.size != 6) {
                null
            } else {
                val skMatrix = Matrix()
                skMatrix.setValues(
                    floatArrayOf(
                        it[0],
                        it[1],
                        it[4],
                        it[2],
                        it[3],
                        it[5],
                        0.0f,
                        0.0f,
                        1.0f,
                    )
                )
                skMatrix
            }
        }
        .orElse(null)
}

/** Convert a Background to a Brush, returning a Pair of Brush and Opacity */
internal fun Background.asBrush(document: DocContent, density: Float): Pair<Brush, Float>? {
    when (this) {
        is Background.Solid -> {
            return Pair(SolidColor(convertColor(value)), 1.0f)
        }
        is Background.Image -> {
            val backgroundImage = this
            val imageFillAndDensity =
                backgroundImage.key.orElse(null)?.let { document.image(it.value, density) }
            // val imageFilters = backgroundImage.filters;
            val imageTransform = backgroundImage.transform.asSkiaMatrix()
            if (imageFillAndDensity != null) {
                val (imageFill, imageDensity) = imageFillAndDensity
                return Pair(
                    RelativeImageFill(
                        image = imageFill,
                        imageDensity = imageDensity,
                        displayDensity = density,
                        imageTransform = imageTransform,
                        scaleMode = backgroundImage.scale_mode
                    ),
                    backgroundImage.opacity
                )
            }
        }
        is Background.LinearGradient -> {
            val linearGradient = this
            return Pair(
                RelativeLinearGradient(
                    linearGradient.color_stops.map { convertColor(it.field1) }.toList(),
                    linearGradient.color_stops.map { it.field0 }.toList(),
                    start = Offset(linearGradient.start_x, linearGradient.start_y),
                    end = Offset(linearGradient.end_x, linearGradient.end_y)
                ),
                1.0f
            )
        }
        is Background.RadialGradient -> {
            val radialGradient = this
            return Pair(
                RelativeRadialGradient(
                    colors = radialGradient.color_stops.map { convertColor(it.field1) },
                    stops = radialGradient.color_stops.map { it.field0 },
                    center = Offset(radialGradient.center_x, radialGradient.center_y),
                    radiusX = radialGradient.radius[0],
                    radiusY = radialGradient.radius[1],
                    angle = radialGradient.angle
                ),
                1.0f
            )
        }
        is Background.AngularGradient -> {
            val angularGradient = this
            return Pair(
                RelativeSweepGradient(
                    center = Offset(angularGradient.center_x, angularGradient.center_y),
                    angle = angularGradient.angle,
                    scale = angularGradient.scale,
                    colors = angularGradient.color_stops.map { convertColor(it.field1) },
                    stops = angularGradient.color_stops.map { it.field0 }
                ),
                1.0f
            )
        }
    }
    return null
}

internal fun com.android.designcompose.serdegen.Path.asPath(
    density: Float,
    scaleX: Float,
    scaleY: Float
): Path {
    val MOVE_TO: Byte = 0
    val LINE_TO: Byte = 1
    val CUBIC_TO: Byte = 2
    val QUAD_TO: Byte = 3
    val CLOSE: Byte = 4

    val p = Path()
    p.fillType =
        when (this.winding_rule) {
            is WindingRule.EVENODD -> PathFillType.EvenOdd
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
                    this.data[idx++] * density * scaleY
                )
            }
            CLOSE -> {
                p.close()
            }
        }
    }
    return p
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
                    "Cubic To ${this.data[idx++]}+${this.data[idx++]} ${this.data[idx++]}+${this.data[idx++]} ${this.data[idx++]}+${this.data[idx++]}"
                )
            }
            QUAD_TO -> {
                Log.e(
                    TAG,
                    "Quad To ${this.data[idx++]}+${this.data[idx++]} ${this.data[idx++]}+${this.data[idx++]}"
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
    width is Dimension.Auto && horizontal_sizing !is LayoutSizing.FILL

internal fun Layout.withDensity(density: Float): Layout {
    return Layout(
        this.order,
        this.width * density,
        this.height * density,
        this.left * density,
        this.top * density
    )
}
