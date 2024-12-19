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
import android.graphics.Shader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isFinite
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.TileMode
import com.android.designcompose.BitmapFactoryWithCache
import com.android.designcompose.DebugNodeManager
import com.android.designcompose.DocContent
import com.android.designcompose.TAG
import com.android.designcompose.VariableState
import com.android.designcompose.proto.getType
import com.android.designcompose.proto.scaleModeFromInt
import com.android.designcompose.serdegen.AffineTransform
import com.android.designcompose.serdegen.Background
import com.android.designcompose.serdegen.BackgroundType
import com.android.designcompose.serdegen.ScaleMode
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

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
                backgroundImage.key.takeIf { it.isNotEmpty() }?.let { document.image(it, density) }
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
