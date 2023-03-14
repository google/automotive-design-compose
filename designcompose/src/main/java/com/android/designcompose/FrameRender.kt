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
import android.graphics.PointF
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.minus
import androidx.core.graphics.plus
import com.android.designcompose.serdegen.BoxShadow
import com.android.designcompose.serdegen.MeterData
import com.android.designcompose.serdegen.Overflow
import com.android.designcompose.serdegen.StrokeAlign
import com.android.designcompose.serdegen.StrokeCap
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
    val nodeX = style.left.pointsAsDp().value.toDouble() + decomposed.translateX
    // Y Node position offset by the Y translation value of the transform matrix
    val nodeY = style.top.pointsAsDp().value.toDouble() + decomposed.translateY

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

// Draw a path around the shape of the given arc and return the path. This does this with the
// following steps:
// Draw outer arc
// Draw cubic bezier to segment joining outer arc to inner
// Draw segment joining outer arc to inner
// Draw cubic bezier from segment to inner arc
// Draw inner arc
// Draw cubic bezier to segment joining inner arc to outer
// Draw segment joining inner arc to outer
// Draw cubic bezier from segment to outer arc (starting point)
// Close path
private fun computeArcPath(frameSize: Size, shape: ViewShape.Arc): Pair<List<Path>, List<Path>> {
    val fWidth = frameSize.width
    val fHeight = frameSize.height
    val startAngle = shape.start_angle_degrees
    val sweepAngle = shape.sweep_angle_degrees
    val endAngle = startAngle + sweepAngle
    val cornerRadius = shape.corner_radius
    val angleDirection = if (endAngle > startAngle) 1.0F else -1.0F

    val outerRadius = PointF(fWidth / 2F, fHeight / 2F)
    val outerCircumference = outerRadius.ellipseCircumferenceFromRadius()
    val outerArcLength = sweepAngle / 360F * outerCircumference
    val shapeInnerRadius = shape.inner_radius.coerceAtLeast(0.001F)
    val innerSize = Size(fWidth * shapeInnerRadius, fHeight * shapeInnerRadius)
    val innerRadius = PointF(innerSize.width / 2.0F, innerSize.height / 2.0F)
    val innerCircumference = innerRadius.ellipseCircumferenceFromRadius()
    val innerArcLength = sweepAngle / 360F * innerCircumference
    val strokeWidth = (outerRadius.x - innerRadius.x).coerceAtMost(outerRadius.y - innerRadius.y)
    val outerCircumferenceOffset =
        (Math.PI.toFloat() * cornerRadius / 2F)
            .coerceAtMost(strokeWidth / 2F)
            .coerceAtMost(outerArcLength / 2F)
    val innerCircumferenceOffset =
        (Math.PI.toFloat() * cornerRadius / 2F)
            .coerceAtMost(strokeWidth / 2F)
            .coerceAtMost(innerArcLength / 2F)
    val outerTangentOffset = outerCircumferenceOffset * 0.6F
    val innerTangentOffset = innerCircumferenceOffset * 0.6F
    val outerAngleOffset = outerCircumferenceOffset / outerCircumference * 360F
    val innerAngleOffset = (innerCircumferenceOffset / innerCircumference * 360F)
    val outerStartAngle = startAngle + outerAngleOffset * angleDirection
    val outerEndAngle = endAngle - outerAngleOffset * angleDirection
    val innerStartAngle = startAngle + innerAngleOffset * angleDirection
    val innerEndAngle = endAngle - innerAngleOffset * angleDirection

    val path = Path()

    // 1. Draw outer arc from start to end
    val outerRect = Rect(0.0F, 0.0F, fWidth, fHeight)
    path.arcTo(outerRect, outerStartAngle, outerEndAngle - outerStartAngle, false)

    // 2. Draw end outer bezier curve for rounded corners
    // End outer rounded corner bezier start point
    var cubicPoint1 = outerEndAngle.pointAtAngle(frameSize, outerRadius)

    // End outer rounded corner bezier first control point
    val endOuterArcTangent = (outerEndAngle + 90F).unitVectorFromAngle()
    var control1 = cubicPoint1 + endOuterArcTangent * outerTangentOffset

    // End outer rounded corner end point
    val endAngleVector = endAngle.unitVectorFromAngle()
    val outerCornerPoint = endAngle.pointAtAngle(frameSize, outerRadius)
    var cubicPoint2 = outerCornerPoint - endAngleVector * outerCircumferenceOffset

    // End outer rounded corner bezier second control point
    var control2 = cubicPoint2 + endAngleVector * outerTangentOffset
    path.cubicTo(control1.x, control1.y, control2.x, control2.y, cubicPoint2.x, cubicPoint2.y)

    // 3. Draw connecting line and end inner bezier curve for rounded corners
    // End inner rounded corner bezier start point
    val endInnerCornerPoint = endAngle.pointAtAngle(frameSize, innerRadius)
    cubicPoint1 = endInnerCornerPoint + endAngleVector * outerCircumferenceOffset

    // Draw line connecting end outer to end inner rounded corner
    path.lineTo(cubicPoint1.x, cubicPoint1.y)

    // End inner rounded corner bezier first control point
    control1 = cubicPoint1 - endAngleVector * innerTangentOffset

    // End inner rounded corner end point
    cubicPoint2 = innerEndAngle.pointAtAngle(frameSize, innerRadius)

    // End inner rounded corner bezier second control point
    val endInnerArcTangent = (innerEndAngle + 90F).unitVectorFromAngle()
    control2 = cubicPoint2 + endInnerArcTangent * innerTangentOffset
    path.cubicTo(control1.x, control1.y, control2.x, control2.y, cubicPoint2.x, cubicPoint2.y)

    // 4. Draw inner arc from end to start
    val innerX = (fWidth - innerSize.width) / 2
    val innerY = (fHeight - innerSize.height) / 2
    val innerRect = Rect(innerX, innerY, innerX + innerSize.width, innerY + innerSize.height)
    path.arcTo(innerRect, innerEndAngle, innerStartAngle - innerEndAngle, false)

    // 5. Draw start inner bezier curve for rounded corners
    // Start inner rounded corner bezier start point
    cubicPoint1 = innerStartAngle.pointAtAngle(frameSize, innerRadius)

    // Start inner rounded corner bezier first control point
    val startInnerArcTangent = (innerStartAngle - 90F).unitVectorFromAngle()
    control1 = cubicPoint1 + startInnerArcTangent * innerTangentOffset

    // Start inner rounded corner end point
    val startAngleVector = startAngle.unitVectorFromAngle()
    val startInnerCornerPoint = startAngle.pointAtAngle(frameSize, innerRadius)
    cubicPoint2 = startInnerCornerPoint + startAngleVector * innerCircumferenceOffset

    // Start inner rounded corner bezier second control point
    control2 = cubicPoint2 - startAngleVector * innerTangentOffset
    path.cubicTo(control1.x, control1.y, control2.x, control2.y, cubicPoint2.x, cubicPoint2.y)

    // 6. Draw connecting line and start outer bezier curve for rounded corners
    // start outer rounded corner bezier start point
    val startOuterCornerPoint = startAngle.pointAtAngle(frameSize, outerRadius)
    cubicPoint1 = startOuterCornerPoint - startAngleVector * outerCircumferenceOffset

    // start outer rounded corner bezier first control point
    control1 = cubicPoint1 + startAngleVector * outerTangentOffset

    // start outer rounded corner end point
    cubicPoint2 = outerStartAngle.pointAtAngle(frameSize, outerRadius)

    // start outer rounded corner bezier second control point
    val startOuterArcTangent = (outerStartAngle - 90F).unitVectorFromAngle()
    control2 = cubicPoint2 + startOuterArcTangent * outerTangentOffset
    path.cubicTo(control1.x, control1.y, control2.x, control2.y, cubicPoint2.x, cubicPoint2.y)

    path.close()
    return Pair(listOf(path), listOf())
}

// Draw a stroke path for the given arc by simply drawing a single arc, then giving it a stroke
// width and converting it to a fill path using android.graphics.Paint.getFillPath()
private fun computeArcStrokePath(
    frameSize: Size,
    shape: ViewShape.Arc,
    style: ViewStyle,
    density: Float
): Pair<List<Path>, List<Path>> {
    val path = Path()
    var left = 0.0f
    var top = 0.0f
    var width = frameSize.width
    var height = frameSize.height
    val strokeWidth = style.stroke.stroke_weight * density
    val halfStrokeWidth = strokeWidth * 0.5f

    when (style.stroke.stroke_align) {
        is StrokeAlign.Inside -> {
            left += halfStrokeWidth
            top += halfStrokeWidth
            width -= halfStrokeWidth
            height -= halfStrokeWidth
        }
        is StrokeAlign.Outside -> {
            left -= halfStrokeWidth
            top -= halfStrokeWidth
            width += halfStrokeWidth
            height += halfStrokeWidth
        }
    }
    path.addArc(
        Rect(left, top, width, height),
        shape.start_angle_degrees,
        shape.sweep_angle_degrees
    )

    val arcPaint = android.graphics.Paint()
    arcPaint.style = android.graphics.Paint.Style.STROKE
    arcPaint.strokeWidth = strokeWidth
    arcPaint.strokeCap =
        when (shape.stroke_cap) {
            is StrokeCap.ROUND -> android.graphics.Paint.Cap.ROUND
            is StrokeCap.SQUARE -> android.graphics.Paint.Cap.SQUARE
            else -> android.graphics.Paint.Cap.BUTT
        }
    val arcStrokePath = android.graphics.Path()
    arcPaint.getFillPath(path.asAndroidPath(), arcStrokePath)
    return Pair(listOf(), listOf(arcStrokePath.asComposePath()))
}

private fun computeRoundedRect(
    frameSize: Size,
    cornerRadius: List<Float>,
    density: Float
): Pair<List<Path>, List<Path>> {
    val path = Path()
    path.addRoundRect(
        RoundRect(
            0.0f,
            0.0f,
            frameSize.width,
            frameSize.height,
            CornerRadius(cornerRadius[0] * density, cornerRadius[0] * density),
            CornerRadius(cornerRadius[1] * density, cornerRadius[1] * density),
            CornerRadius(cornerRadius[2] * density, cornerRadius[2] * density),
            CornerRadius(cornerRadius[3] * density, cornerRadius[3] * density)
        )
    )
    return Pair(listOf(path), listOf<Path>())
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
    drawContext.canvas.save()

    var overrideTransform: androidx.compose.ui.graphics.Matrix? = null
    var frameSize = size
    var shape = frameShape
    var customArcAngle = false
    var customProgressBar = false

    var meterValue = customizations.getMeterValue(name)
    if (meterValue != null) {
        // Check if there is meter data for a dial/gauge/progress bar
        if (style.meter_data.isPresent) {
            when (val meterData = style.meter_data.get()) {
                is MeterData.rotationData -> {
                    val rotationData = meterData.value
                    if (rotationData.enabled) {
                        val rotation =
                            (rotationData.start +
                                    meterValue / 100f * (rotationData.end - rotationData.start))
                                .coerceDiscrete(rotationData.discrete, rotationData.discreteValue)

                        // Calculate offsets from parent when the rotation is 0
                        val offsets =
                            calculateParentOffsets(
                                style,
                                frameSize.width.toDouble(),
                                frameSize.height.toDouble(),
                                density
                            )
                        val xOffsetParent = offsets.first
                        val yOffsetParent = offsets.second

                        // Calculate a rotation transform that rotates about the center of the
                        // node and then moves by xOffset and yOffset
                        overrideTransform = androidx.compose.ui.graphics.Matrix()
                        val moveX = size.width / 2
                        val moveY = size.height / 2

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
                            moveX - style.left.pointsAsDp().value + xOffsetParent.toFloat(),
                            moveY - style.top.pointsAsDp().value + yOffsetParent.toFloat(),
                            0f
                        )
                        overrideTransform.timesAssign(translateBack)
                    }
                }
                is MeterData.progressBarData -> {
                    val progressBarData = meterData.value
                    if (progressBarData.enabled) {
                        val start = progressBarData.start
                        val end = progressBarData.end
                        val progress =
                            (start + meterValue / 100f * (end - start) * density).coerceDiscrete(
                                progressBarData.discrete,
                                progressBarData.discreteValue
                            )
                        if (progressBarData.vertical) {
                            frameSize = Size(size.width, progress)
                            val topOffset = style.top.pointsAsDp().value
                            overrideTransform = androidx.compose.ui.graphics.Matrix()
                            overrideTransform.translate(0F, -topOffset + end - progress)
                        } else frameSize = Size(progress, size.height)
                        customProgressBar = true
                    }
                }
                is MeterData.arcData -> {
                    val arcData = meterData.value
                    if (arcData.enabled) {
                        meterValue =
                            meterValue.coerceDiscrete(arcData.discrete, arcData.discreteValue)
                        // Max out the arc to just below a full circle to avoid having the
                        // path completely disappear
                        val arcMeterValue = meterValue.coerceAtMost(99.999F)
                        val arcAngleMeter =
                            (arcMeterValue / 100f * (arcData.end - arcData.start)).coerceDiscrete(
                                arcData.discrete,
                                arcData.discreteValue
                            )
                        customArcAngle = true
                        if (shape is ViewShape.Arc)
                            shape =
                                ViewShape.Arc(
                                    listOf(),
                                    listOf(),
                                    shape.stroke_cap,
                                    arcData.start,
                                    arcAngleMeter,
                                    shape.inner_radius,
                                    arcData.cornerRadius,
                                )
                    }
                }
            }
        }
    }

    // Push any transforms
    val transform = overrideTransform ?: style.transform.asComposeTransform(density)
    if (transform != null) drawContext.transform.transform(transform)

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
        drawContext.canvas.saveLayer(Rect(Offset.Zero, frameSize), paint)
    }

    fun getPaths(
        path: List<com.android.designcompose.serdegen.Path>,
        stroke: List<com.android.designcompose.serdegen.Path>
    ): Pair<List<Path>, List<Path>> {
        return Pair(
            path.map { path -> path.asPath(density) },
            stroke.map { path -> path.asPath(density) }
        )
    }
    // Fill then stroke.
    val (fills, precomputedStrokes) =
        when (shape) {
            is ViewShape.RoundRect -> {
                computeRoundedRect(frameSize, shape.corner_radius, density)
            }
            is ViewShape.Path -> {
                getPaths(shape.path, shape.stroke)
            }
            is ViewShape.VectorRect -> {
                if (!customProgressBar) {
                    // Render normally with Figma provided fill/stroke path
                    getPaths(shape.path, shape.stroke)
                } else {
                    computeRoundedRect(frameSize, shape.corner_radius, density)
                }
            }
            is ViewShape.Arc -> {
                if (!customArcAngle) {
                    // Render normally with Figma provided fill/stroke path
                    getPaths(shape.path, shape.stroke)
                } else {
                    // We have a custom angle set by a meter customization, so we can't use
                    // the path provided by Figma. Instead, we construct our own path given
                    // the arc parameters
                    if (shape.inner_radius < 1.0F) {
                        computeArcPath(frameSize, shape)
                    } else {
                        computeArcStrokePath(frameSize, shape, style, density)
                    }
                }
            }
            else -> {
                val path = Path()
                path.addRect(Rect(0.0f, 0.0f, frameSize.width, frameSize.height))
                Pair(listOf(path), listOf<Path>())
            }
        }
    val fillBrush =
        style.background.mapNotNull { background ->
            val p = Paint()
            val b = background.asBrush(document, density)
            if (b != null) {
                val (brush, opacity) = b
                brush.applyTo(frameSize, p, opacity)
                p
            } else {
                null
            }
        }
    val strokeBrush =
        style.stroke.strokes.mapNotNull { background ->
            val p = Paint()
            val b = background.asBrush(document, density)
            if (b != null) {
                val (brush, opacity) = b
                brush.applyTo(frameSize, p, opacity)
                p
            } else {
                null
            }
        }
    // We need to make the stroke path if there is any stroke.
    // * Center stroke -> just use the stroke width.
    // * Outer stroke -> double the stroke width, clip out the inner bit
    // * Inner stroke -> double the stroke width, clip out the outer bit.
    val rawStrokeWidth =
        when (style.stroke.stroke_align) {
            is StrokeAlign.Center -> style.stroke.stroke_weight * density
            is StrokeAlign.Inside -> style.stroke.stroke_weight * 2.0f * density
            is StrokeAlign.Outside -> style.stroke.stroke_weight * 2.0f * density
            else -> style.stroke.stroke_weight * density
        }
    val shadowStrokeWidth =
        when (style.stroke.stroke_align) {
            is StrokeAlign.Center -> style.stroke.stroke_weight * density
            is StrokeAlign.Outside -> style.stroke.stroke_weight * 2.0f * density
            else -> 0.0f
        }
    // Build a list of stroke paths, and also build a set of filled paths for shadow
    // painting.
    val strokePaint = android.graphics.Paint()
    strokePaint.style = android.graphics.Paint.Style.STROKE
    strokePaint.strokeWidth = rawStrokeWidth

    val strokes =
        if (fills.isEmpty()) {
            // Sometimes an object has no fill at all (it has no area, because it is
            // just a stroke or line), in which case we use the strokes from the shape.
            precomputedStrokes
        } else {
            // Normally we generate the stroke from the fill path. This lets us have
            // runtime determined width/height for things we're stroking.
            fills.map { fill ->
                val strokePath = android.graphics.Path()
                strokePaint.getFillPath(fill.asAndroidPath(), strokePath)
                when (style.stroke.stroke_align) {
                    is StrokeAlign.Outside ->
                        strokePath.op(fill.asAndroidPath(), android.graphics.Path.Op.DIFFERENCE)
                    is StrokeAlign.Inside ->
                        strokePath.op(fill.asAndroidPath(), android.graphics.Path.Op.INTERSECT)
                    else -> {}
                }
                strokePath.asComposePath()
            }
        }

    // XXX: handle mask layers

    val shadowBoundsPaint = android.graphics.Paint()
    shadowBoundsPaint.style = android.graphics.Paint.Style.FILL_AND_STROKE
    shadowBoundsPaint.strokeWidth = shadowStrokeWidth
    val shadowPaths =
        fills.map { fill ->
            val shadowPath = android.graphics.Path()
            shadowBoundsPaint.getFillPath(fill.asAndroidPath(), shadowPath)
            shadowPath.asComposePath()
        }

    // Outset shadows
    // XXX: only do this if there are shadows.
    drawContext.canvas.save()
    // Don't draw shadows under objects.
    shadowPaths.forEach { path -> drawContext.canvas.clipPath(path, ClipOp.Difference) }

    // Now paint the outset shadows.
    style.box_shadow.forEach { shadow ->
        // Only outset.
        if (shadow !is BoxShadow.Outset) return@forEach

        // To calculate the outset path, we must inflate our outer bounds (our fill
        // path plus the stroke width) plus the shadow spread. Since Skia always
        // centers strokes, we do this by adding double the spread to the shadow
        // stroke width.
        shadowBoundsPaint.strokeWidth = shadowStrokeWidth + shadow.spread_radius * 2.0f * density
        val shadowOutlines =
            fills.map { fill ->
                val shadowPath = android.graphics.Path()
                shadowBoundsPaint.getFillPath(fill.asAndroidPath(), shadowPath)
                shadowPath.asComposePath()
            }

        // Make an appropriate paint.
        val shadowPaint = Paint().asFrameworkPaint()
        shadowPaint.color = convertColor(shadow.color).toArgb()
        if (shadow.blur_radius > 0.0f) {
            shadowPaint.maskFilter =
                BlurMaskFilter(
                    shadow.blur_radius * density * blurFudgeFactor,
                    BlurMaskFilter.Blur.NORMAL
                )
        }
        drawContext.canvas.translate(shadow.offset[0] * density, shadow.offset[1] * density)
        shadowOutlines.forEach { shadowPath ->
            drawContext.canvas.nativeCanvas.drawPath(shadowPath.asAndroidPath(), shadowPaint)
        }
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
        for (fill in fills) {
            drawContext.canvas.clipPath(fill)
        }
        drawImage(
            customImage.asImageBitmap(),
            dstSize = IntSize(frameSize.width.roundToInt(), frameSize.height.roundToInt())
        )
        drawContext.canvas.restore()
    } else {
        for (fill in fills) {
            for (paint in fillBrush) {
                drawContext.canvas.drawPath(fill, paint)
            }
        }
    }

    // Now do inset shadows
    drawContext.canvas.save()
    // Don't draw inset shadows outside of the stroke bounds.
    shadowPaths.forEach { path -> drawContext.canvas.clipPath(path) }
    val shadowOutlinePaint = android.graphics.Paint()
    shadowOutlinePaint.style = android.graphics.Paint.Style.FILL_AND_STROKE
    val shadowSpreadPaint = android.graphics.Paint()
    shadowSpreadPaint.style = android.graphics.Paint.Style.STROKE

    style.box_shadow.forEach { shadow ->
        // Only inset.
        if (shadow !is BoxShadow.Inset) return@forEach

        // Inset shadows are applied to the "stroke bounds", not the fill bounds. So we
        // must inflate our fill bounds out to the stroke bounds by applying a stroke and
        // taking the fill path.
        //
        // We then invert the fill path so that we're filling the area that's not the stroke
        // bounds. Then we offset it and blur it to make the inset shadow.
        //
        // If we have a spread that's larger than what we use to expand to make the fill
        // then we stroke the excess spread and subtract it from the fill to make the path.

        val spreadWidth = shadow.spread_radius * 2.0f * density
        val needSpreadStroke = spreadWidth > shadowStrokeWidth
        if (!needSpreadStroke) shadowOutlinePaint.strokeWidth = shadowStrokeWidth - spreadWidth
        else shadowSpreadPaint.strokeWidth = spreadWidth - shadowStrokeWidth

        val shadowOutlines =
            fills.map { fill ->
                val shadowPath = android.graphics.Path()
                if (!needSpreadStroke) {
                    shadowOutlinePaint.getFillPath(fill.asAndroidPath(), shadowPath)
                } else {
                    val spreadStroke = android.graphics.Path()
                    shadowSpreadPaint.getFillPath(fill.asAndroidPath(), spreadStroke)
                    shadowPath.op(
                        fill.asAndroidPath(),
                        spreadStroke,
                        android.graphics.Path.Op.DIFFERENCE
                    )
                }

                shadowPath.toggleInverseFillType()

                shadowPath.asComposePath()
            }

        // Make an appropriate paint.
        val shadowPaint = Paint().asFrameworkPaint()
        shadowPaint.color = convertColor(shadow.color).toArgb()
        if (shadow.blur_radius > 0.0f) {
            shadowPaint.maskFilter =
                BlurMaskFilter(
                    shadow.blur_radius * density * blurFudgeFactor,
                    BlurMaskFilter.Blur.NORMAL
                )
        }
        drawContext.canvas.translate(shadow.offset[0] * density, shadow.offset[1] * density)
        shadowOutlines.forEach { shadowPath ->
            drawContext.canvas.nativeCanvas.drawPath(shadowPath.asAndroidPath(), shadowPaint)
        }
    }
    drawContext.canvas.restore()

    val shouldClip = style.overflow is Overflow.Hidden
    if (shouldClip) {
        // Clip children, and paint our stroke on top of them.
        drawContext.canvas.save()
        for (fill in fills) {
            drawContext.canvas.clipPath(fill)
        }
        drawContent()
        drawContext.canvas.restore()
        for (stroke in strokes) {
            for (paint in strokeBrush) {
                drawContext.canvas.drawPath(stroke, paint)
            }
        }
    } else {
        // No clipping; paint our stroke first and then paint
        // our children.
        for (stroke in strokes) {
            for (paint in strokeBrush) {
                drawContext.canvas.drawPath(stroke, paint)
            }
        }
        drawContent()
    }

    if (useBlendMode) {
        drawContext.canvas.restore()
    }
    drawContext.canvas.restore()
}
