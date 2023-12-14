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

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import com.android.designcompose.serdegen.ArcMeterData
import com.android.designcompose.serdegen.BoxShadow
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.MeterData
import com.android.designcompose.serdegen.Overflow
import com.android.designcompose.serdegen.ProgressBarMeterData
import com.android.designcompose.serdegen.ProgressMarkerMeterData
import com.android.designcompose.serdegen.RotationMeterData
import com.android.designcompose.serdegen.ViewShape
import com.android.designcompose.serdegen.ViewStyle
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// Calculate the x and y offset of this node from its parent when it has a rotation of 0. Since
// the node in Figma may already be rotated, we need to do some math to basically undo the rotation
// to figure out the offsets. We extract the angle from the matrix then use trig and the offsets
// already provided by Figma in style.left, style.top, and style.transform to do the calculations.
private fun calculateParentOffsets(
    style: ViewStyle,
    nodeWidth: Double,
    nodeHeight: Double,
    density: Float,
): Pair<Double, Double> {
    val decomposed = style.transform.decompose(density)

    // X Node position offset by the X translation value of the transform matrix
    val nodeX = style.margin.start.pointsAsDp(density).value.toDouble() + decomposed.translateX
    // Y Node position offset by the Y translation value of the transform matrix
    val nodeY = style.margin.top.pointsAsDp(density).value.toDouble() + decomposed.translateY

    // Radius of the circle encapsulating the node
    val r = sqrt(nodeWidth * nodeWidth + nodeHeight * nodeHeight) / 2
    // Angle of the top left corner when not rotated
    val topLeftAngle = Math.toDegrees(atan(nodeHeight / -nodeWidth))
    // Current angle, offset by the top left corner angle
    val angleFromTopLeft =
        Math.toRadians(decomposed.angle.toDouble()) + Math.toRadians(topLeftAngle)
    val cos = abs(cos(angleFromTopLeft))
    val sin = abs(sin(angleFromTopLeft))

    var xOffset = nodeX - nodeWidth / 2
    if (decomposed.angle >= -90 - topLeftAngle && decomposed.angle < 90 - topLeftAngle)
        xOffset += r * cos
    else xOffset -= r * cos

    var yOffset = nodeY - nodeHeight / 2
    if (decomposed.angle <= -topLeftAngle && decomposed.angle >= -topLeftAngle - 180)
        yOffset += r * sin
    else yOffset -= r * sin
    return Pair(xOffset, yOffset)
}

private fun ViewStyle.getTransform(density: Float): androidx.compose.ui.graphics.Matrix {
    return transform.asComposeTransform(density) ?: androidx.compose.ui.graphics.Matrix()
}

private fun lerp(start: Float, end: Float, percent: Float, density: Float): Float {
    return start * density + percent / 100F * (end - start) * density
}

private fun calculateRotationData(
    rotationData: RotationMeterData,
    meterValue: Float,
    style: ViewStyle,
    density: Float
): androidx.compose.ui.graphics.Matrix {
    val rotation =
        (rotationData.start + meterValue / 100f * (rotationData.end - rotationData.start))
            .coerceDiscrete(rotationData.discrete, rotationData.discreteValue)

    val nodeWidth =
        if (style.width is Dimension.Points) style.width.pointsAsDp(density).value
        else style.node_size.width
    val nodeHeight =
        if (style.height is Dimension.Points) style.height.pointsAsDp(density).value
        else style.node_size.height

    // Calculate offsets from parent when the rotation is 0
    val offsets =
        calculateParentOffsets(style, nodeWidth.toDouble(), nodeHeight.toDouble(), density)
    val xOffsetParent = offsets.first
    val yOffsetParent = offsets.second

    // Calculate a rotation transform that rotates about the center of the
    // node and then moves by xOffset and yOffset
    val overrideTransform = androidx.compose.ui.graphics.Matrix()
    val moveX = nodeWidth / 2
    val moveY = nodeHeight / 2

    // First translate so we rotate about the center
    val translateOrigin = androidx.compose.ui.graphics.Matrix()
    translateOrigin.translate(-moveX, -moveY, 0f)
    overrideTransform.timesAssign(translateOrigin)

    // Perform the rotation
    val rotate = androidx.compose.ui.graphics.Matrix()
    rotate.rotateZ(-rotation)
    overrideTransform.timesAssign(rotate)

    // Translate back, with an additional offset from the parent
    val translateBack = androidx.compose.ui.graphics.Matrix()
    translateBack.translate(
        moveX - style.margin.start.pointsAsDp(density).value + xOffsetParent.toFloat(),
        moveY - style.margin.top.pointsAsDp(density).value + yOffsetParent.toFloat(),
        0f
    )
    overrideTransform.timesAssign(translateBack)
    return overrideTransform
}

private fun calculateProgressBarData(
    progressBarData: ProgressBarMeterData,
    meterValue: Float,
    height: Float,
    density: Float
): Size {
    // Progress bar discrete values are done by percentage
    val discretizedMeterValue =
        meterValue.coerceDiscrete(progressBarData.discrete, progressBarData.discreteValue)

    // Resize the progress bar by interpolating between 0 and endX
    val barWidth = lerp(0F, progressBarData.endX, discretizedMeterValue, density)
    return Size(barWidth, height)
}

private fun calculateProgressMarkerData(
    markerData: ProgressMarkerMeterData,
    meterValue: Float,
    style: ViewStyle,
    density: Float
): androidx.compose.ui.graphics.Matrix {
    // Progress marker discrete values are done by percentage
    val discretizedMeterValue =
        meterValue.coerceDiscrete(markerData.discrete, markerData.discreteValue)

    // The indicator mode means we don't resize the node; we just move it
    // along the x axis
    val moveX = lerp(markerData.startX, markerData.endX, discretizedMeterValue, density)
    val overrideTransform = style.getTransform(density)
    val leftOffset = style.margin.start.pointsAsDp(density).value
    overrideTransform.setXTranslation(moveX - leftOffset)

    return overrideTransform
}

private fun calculateArcData(
    arcData: ArcMeterData,
    meterValue: Float,
    shape: ViewShape
): ViewShape {
    // Max out the arc to just below a full circle to avoid having the
    // path completely disappear
    val arcMeterValue = meterValue.coerceAtMost(99.999F)
    val arcAngleMeter =
        (arcMeterValue / 100f * (arcData.end - arcData.start)).coerceDiscrete(
            arcData.discrete,
            arcData.discreteValue
        )
    var returnShape = shape
    if (shape is ViewShape.Arc) {
        returnShape =
            ViewShape.Arc(
                listOf(),
                listOf(),
                shape.stroke_cap,
                arcData.start,
                arcAngleMeter,
                shape.inner_radius,
                arcData.cornerRadius,
                shape.size,
                shape.is_mask,
            )
    }
    return returnShape
}

private fun renderPaths(drawContext: DrawContext, paths: List<Path>, brushes: List<Paint>) {
    for (path in paths) {
        for (paint in brushes) {
            drawContext.canvas.drawPath(path, paint)
        }
    }
}

internal fun ContentDrawScope.render(
    modifier: Modifier,
    style: ViewStyle,
    frameShape: ViewShape,
    customImageWithContext: Bitmap?,
    document: DocContent,
    name: String,
    customizations: CustomizationContext,
) {
    if (size.width <= 0F && size.height <= 0F) return

    drawContext.canvas.save()

    var overrideTransform: androidx.compose.ui.graphics.Matrix? = null
    var rectSize: Size? = null
    var shape = frameShape
    var customArcAngle = false

    val meterValue = customizations.getMeterValue(name)
    if (meterValue != null) {
        // Check if there is meter data for a dial/gauge/progress bar
        if (style.meter_data.isPresent) {
            when (val meterData = style.meter_data.get()) {
                is MeterData.rotationData -> {
                    val rotationData = meterData.value
                    if (rotationData.enabled) {
                        overrideTransform =
                            calculateRotationData(rotationData, meterValue, style, density)
                    }
                }
                is MeterData.progressBarData -> {
                    val progressBarData = meterData.value
                    if (progressBarData.enabled) {
                        rectSize =
                            calculateProgressBarData(
                                progressBarData,
                                meterValue,
                                style.height.pointsAsDp(density).value,
                                density
                            )
                    }
                }
                is MeterData.progressMarkerData -> {
                    val progressMarkerData = meterData.value
                    if (progressMarkerData.enabled) {
                        overrideTransform =
                            calculateProgressMarkerData(
                                progressMarkerData,
                                meterValue,
                                style,
                                density
                            )
                    }
                }
                is MeterData.arcData -> {
                    val arcData = meterData.value
                    if (arcData.enabled) {
                        shape = calculateArcData(arcData, meterValue, shape)
                        customArcAngle = true
                    }
                }
            }
        }
    }

    // Push any transforms
    val transform = overrideTransform ?: style.transform.asComposeTransform(density)
    var vectorScaleX = 1F
    var vectorScaleY = 1F
    if (transform != null) {
        val decomposed = style.transform.decompose(density)
        vectorScaleX = abs(decomposed.scaleX)
        vectorScaleY = abs(decomposed.scaleY)
        drawContext.transform.transform(transform)
    }

    // Blend mode
    val blendMode = style.blend_mode.asComposeBlendMode()
    val useBlendMode = style.blend_mode.useLayer()
    val opacity = style.opacity.orElse(1.0f)

    // Either use a graphicsLayer to apply the opacity effect, or use saveLayer if
    // we have a blend mode.
    if (!useBlendMode && opacity < 1.0f) {
        modifier.alpha(opacity)
    }
    if (useBlendMode) {
        val paint = Paint()
        paint.alpha = opacity
        paint.blendMode = blendMode
        drawContext.canvas.saveLayer(Rect(Offset.Zero, size), paint)
    }
    val shapePaths =
        shape.computePaths(
            style,
            density,
            size,
            rectSize,
            customArcAngle,
            vectorScaleX,
            vectorScaleY
        )

    val customFillBrushFunction = customizations.getBrushFunction(name)
    val customFillBrush =
        if (customFillBrushFunction != null) {
            customFillBrushFunction()
        } else {
            customizations.getBrush(name)
        }

    val fillBrush: List<Paint> =
        if (customFillBrush != null) {
            val p = Paint()
            customFillBrush.applyTo(size, p, 1.0f)
            listOf(p)
        } else {
            style.background.mapNotNull { background ->
                val p = Paint()
                val b = background.asBrush(document, density)
                if (b != null) {
                    val (brush, fillOpacity) = b
                    val brushSize = getNodeRenderSize(rectSize, size, style, density)
                    brush.applyTo(brushSize, p, fillOpacity)
                    p
                } else {
                    null
                }
            }
        }

    val strokeBrush =
        style.stroke.strokes.mapNotNull { background ->
            val p = Paint()
            val b = background.asBrush(document, density)
            if (b != null) {
                val (brush, strokeOpacity) = b
                val brushSize = getNodeRenderSize(rectSize, size, style, density)
                brush.applyTo(brushSize, p, strokeOpacity)
                p
            } else {
                null
            }
        }

    // Outset shadows
    // XXX: only do this if there are shadows.
    drawContext.canvas.save()
    // Don't draw shadows under objects.
    shapePaths.shadowClips.forEach { path -> drawContext.canvas.clipPath(path, ClipOp.Difference) }

    // Now paint the outset shadows.
    shapePaths.shadowFills.forEach { shadow ->
        // Only outset.
        if (shadow.shadowStyle !is BoxShadow.Outset) return@forEach

        // Make an appropriate paint.
        val shadowPaint = Paint().asFrameworkPaint()
        shadowPaint.color = convertColor(shadow.shadowStyle.color).toArgb()
        if (shadow.shadowStyle.blur_radius > 0.0f) {
            shadowPaint.maskFilter =
                BlurMaskFilter(
                    shadow.shadowStyle.blur_radius * density * blurFudgeFactor,
                    BlurMaskFilter.Blur.NORMAL
                )
        }
        drawContext.canvas.translate(
            shadow.shadowStyle.offset[0] * density,
            shadow.shadowStyle.offset[1] * density
        )
        shadow.fills.forEach { shadowPath ->
            drawContext.canvas.nativeCanvas.drawPath(shadowPath.asAndroidPath(), shadowPaint)
        }
        drawContext.canvas.translate(
            -shadow.shadowStyle.offset[0] * density,
            -shadow.shadowStyle.offset[1] * density
        )
    }
    drawContext.canvas.restore()

    // Now draw the actual shape, or fill it with an image if we have an image
    // replacement; we might want to do image replacement as a Brush in the
    // future.
    var customImage = customImageWithContext
    if (customImage == null) customImage = customizations.getImage(name)
    if (customImage != null) {
        // Apply custom image as background
        drawContext.canvas.save()
        for (fill in shapePaths.fills) {
            drawContext.canvas.clipPath(fill)
        }
        drawImage(
            customImage.asImageBitmap(),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
        )
        drawContext.canvas.restore()
    } else {
        renderPaths(drawContext, shapePaths.fills, fillBrush)
    }

    // Now do inset shadows
    drawContext.canvas.save()
    // Don't draw inset shadows outside of the stroke bounds.
    shapePaths.shadowClips.forEach { path -> drawContext.canvas.clipPath(path) }
    val shadowOutlinePaint = android.graphics.Paint()
    shadowOutlinePaint.style = android.graphics.Paint.Style.FILL_AND_STROKE
    val shadowSpreadPaint = android.graphics.Paint()
    shadowSpreadPaint.style = android.graphics.Paint.Style.STROKE

    shapePaths.shadowFills.forEach { shadow ->
        // Only inset.
        if (shadow.shadowStyle !is BoxShadow.Inset) return@forEach

        // Make an appropriate paint.
        val shadowPaint = Paint().asFrameworkPaint()
        shadowPaint.color = convertColor(shadow.shadowStyle.color).toArgb()
        if (shadow.shadowStyle.blur_radius > 0.0f) {
            shadowPaint.maskFilter =
                BlurMaskFilter(
                    shadow.shadowStyle.blur_radius * density * blurFudgeFactor,
                    BlurMaskFilter.Blur.NORMAL
                )
        }
        drawContext.canvas.translate(
            shadow.shadowStyle.offset[0] * density,
            shadow.shadowStyle.offset[1] * density
        )
        shadow.fills.forEach { shadowPath ->
            drawContext.canvas.nativeCanvas.drawPath(shadowPath.asAndroidPath(), shadowPaint)
        }
        drawContext.canvas.translate(
            -shadow.shadowStyle.offset[0] * density,
            -shadow.shadowStyle.offset[1] * density
        )
    }
    drawContext.canvas.restore()

    // Now draw our stroke and our children. The order of drawing the stroke and the
    // children is different depending on whether we clip children.
    val shouldClip = style.overflow is Overflow.Hidden
    if (shouldClip) {
        // Clip children, and paint our stroke on top of them.
        drawContext.canvas.save()
        for (fill in shapePaths.fills) {
            drawContext.canvas.clipPath(fill)
        }
        drawContent()
        drawContext.canvas.restore()
        renderPaths(drawContext, shapePaths.strokes, strokeBrush)
    } else {
        // No clipping; paint our stroke first and then paint our children.
        renderPaths(drawContext, shapePaths.strokes, strokeBrush)
        drawContent()
    }

    if (useBlendMode) {
        drawContext.canvas.restore()
    }
    drawContext.canvas.restore()
}
