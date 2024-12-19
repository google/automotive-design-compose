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
import com.android.designcompose.BitmapFactoryWithCache
import com.android.designcompose.DebugNodeManager
import com.android.designcompose.DocContent
import com.android.designcompose.LayoutManager
import com.android.designcompose.TAG
import com.android.designcompose.VariableState
import com.android.designcompose.getValue
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
import com.android.designcompose.serdegen.LayoutSizing
import com.android.designcompose.definition.layout.LayoutStyle
import com.android.designcompose.serdegen.LayoutTransform
import com.android.designcompose.serdegen.LineHeight
import com.android.designcompose.serdegen.LineHeightType
import com.android.designcompose.definition.view.NodeStyle
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
import com.android.designcompose.definition.view.View
import com.android.designcompose.serdegen.ViewDataType
import com.android.designcompose.serdegen.ViewShape
import com.android.designcompose.definition.view.ViewStyle
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import kotlin.math.roundToInt
import kotlin.math.sqrt

// We need this fudge factor to make the blurry shadow bounds match up roughly
// with Figma (which probably computes shadows entirely differently to Skia).
// It looks a bit like 1 / sqrt(2), but isn't.
internal const val blurFudgeFactor = 0.72f

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

internal fun View.isMask(): Boolean {
    val containerData = data.get()?.view_data_type?.get()
    return if (containerData is ViewDataType.Container) {
        containerData.value.shape.get().isMask()
    } else false
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
