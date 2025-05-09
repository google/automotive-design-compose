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
import android.util.Log
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.isIdentity
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DebugNodeManager
import com.android.designcompose.TAG
import com.android.designcompose.definition.element.Color
import com.android.designcompose.definition.element.Path
import com.android.designcompose.definition.element.ViewShape
import com.android.designcompose.definition.interaction.Easing.EasingTypeCase
import com.android.designcompose.definition.interaction.Transition
import com.android.designcompose.definition.layout.OverflowDirection
import com.android.designcompose.definition.modifier.BlendMode
import com.android.designcompose.definition.modifier.LayoutTransform
import com.android.designcompose.definition.plugin.ProgressBarMeterData
import com.android.designcompose.definition.plugin.ProgressMarkerMeterData
import com.android.designcompose.definition.plugin.progressBarDataOrNull
import com.android.designcompose.definition.plugin.progressMarkerDataOrNull
import com.android.designcompose.definition.view.StyledTextRun
import com.android.designcompose.definition.view.View
import com.android.designcompose.definition.view.ViewData
import com.android.designcompose.definition.view.ViewStyle
import com.android.designcompose.definition.view.containerOrNull
import com.android.designcompose.definition.view.dataOrNull
import com.android.designcompose.definition.view.meterDataOrNull
import com.android.designcompose.definition.view.nodeStyleOrNull
import com.android.designcompose.definition.view.scrollInfoOrNull
import com.android.designcompose.definition.view.shapeOrNull
import com.android.designcompose.definition.view.styledTextRun
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
        BlendMode.BLEND_MODE_PASS_THROUGH -> androidx.compose.ui.graphics.BlendMode.SrcOver
        BlendMode.BLEND_MODE_NORMAL -> androidx.compose.ui.graphics.BlendMode.SrcOver
        BlendMode.BLEND_MODE_DARKEN -> androidx.compose.ui.graphics.BlendMode.Darken
        BlendMode.BLEND_MODE_MULTIPLY -> androidx.compose.ui.graphics.BlendMode.Multiply
        BlendMode.BLEND_MODE_COLOR_BURN -> androidx.compose.ui.graphics.BlendMode.ColorBurn
        BlendMode.BLEND_MODE_LIGHTEN -> androidx.compose.ui.graphics.BlendMode.Lighten
        BlendMode.BLEND_MODE_SCREEN -> androidx.compose.ui.graphics.BlendMode.Screen
        BlendMode.BLEND_MODE_COLOR_DODGE -> androidx.compose.ui.graphics.BlendMode.ColorDodge
        BlendMode.BLEND_MODE_OVERLAY -> androidx.compose.ui.graphics.BlendMode.Overlay
        BlendMode.BLEND_MODE_SOFT_LIGHT -> androidx.compose.ui.graphics.BlendMode.Softlight
        BlendMode.BLEND_MODE_HARD_LIGHT -> androidx.compose.ui.graphics.BlendMode.Hardlight
        BlendMode.BLEND_MODE_DIFFERENCE -> androidx.compose.ui.graphics.BlendMode.Difference
        BlendMode.BLEND_MODE_EXCLUSION -> androidx.compose.ui.graphics.BlendMode.Exclusion
        BlendMode.BLEND_MODE_HUE -> androidx.compose.ui.graphics.BlendMode.Hue
        BlendMode.BLEND_MODE_SATURATION -> androidx.compose.ui.graphics.BlendMode.Saturation
        BlendMode.BLEND_MODE_COLOR -> androidx.compose.ui.graphics.BlendMode.Color
        BlendMode.BLEND_MODE_LUMINOSITY -> androidx.compose.ui.graphics.BlendMode.Luminosity
        // Unsupported
        BlendMode.BLEND_MODE_LINEAR_BURN -> androidx.compose.ui.graphics.BlendMode.ColorBurn
        BlendMode.BLEND_MODE_LINEAR_DODGE -> androidx.compose.ui.graphics.BlendMode.ColorDodge
        else -> androidx.compose.ui.graphics.BlendMode.SrcOver
    }

/** Does the BlendMode need a layer? */
internal fun BlendMode.useLayer() =
    when (this) {
        BlendMode.BLEND_MODE_PASS_THROUGH -> false
        else -> true
    }

internal fun View.hasScrolling(): Boolean {
    return when (scrollInfoOrNull?.overflow) {
        OverflowDirection.OVERFLOW_DIRECTION_HORIZONTAL_SCROLLING -> true
        OverflowDirection.OVERFLOW_DIRECTION_VERTICAL_SCROLLING -> true
        OverflowDirection.OVERFLOW_DIRECTION_HORIZONTAL_AND_VERTICAL_SCROLLING -> true
        else -> false
    }
}

internal fun View.getProgressChildWithTouch(customizations: CustomizationContext): View? {
    val children = dataOrNull?.containerOrNull?.childrenList
    if (children.isNullOrEmpty()) return null
    for (child in children) {
        val progressData = child.style.getProgressBarData()
        val markerData = child.style.getProgressMarkerData()
        if (progressData?.draggable == true || markerData?.draggable == true) return child
    }
    return null
}

internal fun ViewStyle.getProgressBarData(): ProgressBarMeterData? {
    return with(nodeStyleOrNull?.meterDataOrNull?.progressBarDataOrNull) {
        if (this?.enabled == true) this else null
    }
}

internal fun ViewStyle.getProgressMarkerData(): ProgressMarkerMeterData? {
    return with(nodeStyleOrNull?.meterDataOrNull?.progressMarkerDataOrNull) {
        if (this?.enabled == true) this else null
    }
}

internal fun View.isMask(): Boolean {
    val containerData = dataOrNull
    return if (containerData?.hasContainer() == true) {
        containerData.containerOrNull?.shapeOrNull?.isMask() ?: false
    } else false
}

internal fun ViewShape.isMask(): Boolean {
    if (hasRect()) {
        return rect.isMask
    }
    if (hasRoundRect()) {
        return roundRect.isMask
    }
    if (hasVectorRect()) {
        return vectorRect.isMask
    }
    if (hasPath()) {
        return path.isMask
    }
    if (hasArc()) {
        return arc.isMask
    }
    return false
}

internal fun ViewShape.StrokeCap.toComposeStrokeCap(): androidx.compose.ui.graphics.StrokeCap {
    return when (this) {
        ViewShape.StrokeCap.STROKE_CAP_ROUND -> androidx.compose.ui.graphics.StrokeCap.Round
        ViewShape.StrokeCap.STROKE_CAP_SQUARE -> androidx.compose.ui.graphics.StrokeCap.Square
        else -> androidx.compose.ui.graphics.StrokeCap.Butt
    }
}

internal fun Path.asPath(
    density: Float,
    scaleX: Float,
    scaleY: Float,
): androidx.compose.ui.graphics.Path {
    val MOVE_TO: Byte = 0
    val LINE_TO: Byte = 1
    val CUBIC_TO: Byte = 2
    val QUAD_TO: Byte = 3
    val CLOSE: Byte = 4

    val p = androidx.compose.ui.graphics.Path()
    p.fillType =
        when (windingRule) {
            Path.WindingRule.WINDING_RULE_EVEN_ODD -> PathFillType.EvenOdd
            else -> PathFillType.NonZero
        }
    var idx = 0
    for (cmd in this.commands) {
        when (cmd) {
            MOVE_TO -> {
                p.moveTo(
                    this.getData(idx++) * density * scaleX,
                    this.getData(idx++) * density * scaleY,
                )
            }
            LINE_TO -> {
                p.lineTo(
                    this.getData(idx++) * density * scaleX,
                    this.getData(idx++) * density * scaleY,
                )
            }
            CUBIC_TO -> {
                p.cubicTo(
                    this.getData(idx++) * density * scaleX,
                    this.getData(idx++) * density * scaleY,
                    this.getData(idx++) * density * scaleX,
                    this.getData(idx++) * density * scaleY,
                    this.getData(idx++) * density * scaleX,
                    this.getData(idx++) * density * scaleY,
                )
            }
            QUAD_TO -> {
                p.quadraticTo(
                    this.getData(idx++) * density * scaleX,
                    this.getData(idx++) * density * scaleY,
                    this.getData(idx++) * density * scaleX,
                    this.getData(idx++) * density * scaleY,
                )
            }
            CLOSE -> {
                p.close()
            }
        }
    }
    return p
}

internal fun Path.log() {
    val MOVE_TO: Byte = 0
    val LINE_TO: Byte = 1
    val CUBIC_TO: Byte = 2
    val QUAD_TO: Byte = 3
    val CLOSE: Byte = 4

    var idx = 0
    for (cmd in this.commands) {
        when (cmd) {
            MOVE_TO -> {
                Log.e(TAG, "Move To ${this.getData(idx++)}+${this.getData(idx++)}")
            }
            LINE_TO -> {
                Log.e(TAG, "Line To ${this.getData(idx++)}+${this.getData(idx++)}")
            }
            CUBIC_TO -> {
                Log.e(
                    TAG,
                    "Cubic To ${this.getData(idx++)}+${this.getData(idx++)} ${this.getData(idx++)}+${this.getData(idx++)} ${this.getData(idx++)}+${this.getData(idx++)}",
                )
            }
            QUAD_TO -> {
                Log.e(
                    TAG,
                    "Quad To ${this.getData(idx++)}+${this.getData(idx++)} ${this.getData(idx++)}+${this.getData(idx++)}",
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
    val transitionTypeCase = this.transitionTypeCase
    val easing =
        when (transitionTypeCase) {
            Transition.TransitionTypeCase.SMART_ANIMATE -> smartAnimate.easing
            Transition.TransitionTypeCase.SCROLL_ANIMATE -> scrollAnimate.easing
            Transition.TransitionTypeCase.PUSH -> push.easing
            Transition.TransitionTypeCase.MOVE_IN -> moveIn.easing
            Transition.TransitionTypeCase.MOVE_OUT -> moveOut.easing
            Transition.TransitionTypeCase.DISSOLVE -> dissolve.easing
            Transition.TransitionTypeCase.SLIDE_IN -> slideIn.easing
            Transition.TransitionTypeCase.SLIDE_OUT -> slideOut.easing
            else -> return snap(0)
        }
    val duration =
        when (transitionTypeCase) {
            Transition.TransitionTypeCase.SMART_ANIMATE -> smartAnimate.duration
            Transition.TransitionTypeCase.SCROLL_ANIMATE -> scrollAnimate.duration
            Transition.TransitionTypeCase.PUSH -> push.duration
            Transition.TransitionTypeCase.MOVE_IN -> moveIn.duration
            Transition.TransitionTypeCase.MOVE_OUT -> moveOut.duration
            Transition.TransitionTypeCase.DISSOLVE -> dissolve.duration
            Transition.TransitionTypeCase.SLIDE_IN -> slideIn.duration
            Transition.TransitionTypeCase.SLIDE_OUT -> slideOut.duration
            else -> return snap(0)
        }
    return when (easing.easingTypeCase) {
        EasingTypeCase.SPRING -> {
            // Compose takes damping as a fraction of the amount required for critical damping,
            // rather than as the actual damping value. So, we must calculate the damping required
            // for criticality with the given stiffness and mass.
            //
            // Reference implementation of a simple spring based on integrating Hooke's law:
            //  https://github.com/iamralpht/gravitas-rs/blob/master/src/spring.rs#L23

            val critical = sqrt(4.0f * easing.spring.stiffness * easing.spring.mass)
            spring(
                dampingRatio = easing.spring.damping / critical,
                stiffness = easing.spring.stiffness,
            )
        }
        EasingTypeCase.BEZIER -> {
            tween(
                durationMillis = (duration * 1000.0).roundToInt(),
                easing =
                    CubicBezierEasing(
                        easing.bezier.x1,
                        easing.bezier.y1,
                        easing.bezier.x2,
                        easing.bezier.y2,
                    ),
            )
        }
        else -> snap(0)
    }
}

/**
 * Convert a LayoutTransform to a Compose transformation matrix, adjusted to operate on pixels at
 * the given display density.
 *
 * XXX: Doesn't consider transform origin.
 */
internal fun LayoutTransform?.asComposeTransform(
    density: Float
): androidx.compose.ui.graphics.Matrix? {
    if (this == null) return null
    val transform =
        androidx.compose.ui.graphics.Matrix(
            floatArrayOf(
                m11,
                m12,
                m13,
                m14,
                m21,
                m22,
                m23,
                m24,
                m31,
                m32,
                m33,
                m34,
                m41,
                m42,
                m43,
                m44,
            )
        )
    if (transform.isIdentity()) {
        return null
    } else {
        val adjust = androidx.compose.ui.graphics.Matrix()
        adjust.scale(1.0f / density, 1.0f / density, 1.0f / density)
        adjust.timesAssign(transform)
        val unadjust = androidx.compose.ui.graphics.Matrix()
        unadjust.scale(density, density, density)
        adjust.timesAssign(unadjust)
        return adjust
    }
}

/** Convert a serialized color to a Compose color */
internal fun Color.toColor(): androidx.compose.ui.graphics.Color {
    return androidx.compose.ui.graphics.Color(r, g, b, a)
}

internal fun getTextContent(context: Context, textData: ViewData.Text): String {
    if (DebugNodeManager.getUseLocalRes().value && textData.hasResName()) {
        val resName = textData.resName
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
    styledTextData: ViewData.StyledTextRuns,
): List<StyledTextRun> {
    if (DebugNodeManager.getUseLocalRes().value && styledTextData.hasResName()) {
        val resName = styledTextData.resName
        val strArrayResId = context.resources.getIdentifier(resName, "array", context.packageName)
        if (strArrayResId != Resources.ID_NULL) {
            val textArray = context.resources.getStringArray(strArrayResId)
            if (textArray.size == styledTextData.styledTextsCount) {
                val output = mutableListOf<StyledTextRun>()
                for (i in textArray.indices) {
                    output.add(
                        styledTextRun {
                            text = textArray[i]
                            style = styledTextData.getStyledTexts(i).style
                        }
                    )
                }
                return output
            } else {
                Log.w(TAG, "String array size mismatched the styled runs")
            }
        }
        Log.w(TAG, "No string array resource $resName found for styled runs")
        if (styledTextData.styledTextsCount == 1) {
            val strResId = context.resources.getIdentifier(resName, "string", context.packageName)
            if (strResId != Resources.ID_NULL) {
                Log.w(TAG, "Single style found, fallback to string resource")
                return mutableListOf(
                    styledTextRun {
                        text = context.getString(strResId)
                        style = styledTextData.getStyledTexts(0).style
                    }
                )
            } else {
                Log.w(TAG, "No string resource $resName found for styled runs")
            }
        }
    }
    return styledTextData.styledTextsList
}
