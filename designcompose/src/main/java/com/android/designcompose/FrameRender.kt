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
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import com.android.designcompose.proto.StrokeAlignType
import com.android.designcompose.proto.blendModeFromInt
import com.android.designcompose.proto.getDim
import com.android.designcompose.proto.layoutStyle
import com.android.designcompose.proto.max
import com.android.designcompose.proto.nodeStyle
import com.android.designcompose.proto.overflowFromInt
import com.android.designcompose.proto.start
import com.android.designcompose.proto.strokeAlignFromInt
import com.android.designcompose.proto.toUniform
import com.android.designcompose.proto.top
import com.android.designcompose.serdegen.ArcMeterData
import com.android.designcompose.serdegen.MeterDataType
import com.android.designcompose.serdegen.Overflow
import com.android.designcompose.serdegen.ProgressBarMeterData
import com.android.designcompose.serdegen.ProgressMarkerMeterData
import com.android.designcompose.serdegen.ProgressVectorMeterData
import com.android.designcompose.serdegen.RotationMeterData
import com.android.designcompose.serdegen.ShadowBox
import com.android.designcompose.serdegen.Shape
import com.android.designcompose.serdegen.VectorArc
import com.android.designcompose.serdegen.ViewShape
import com.android.designcompose.serdegen.ViewStyle
import com.android.designcompose.squoosh.SquooshResolvedNode
import java.lang.Float.max
import java.util.Optional
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
    val decomposed = style.nodeStyle.transform.decompose(density)

    // X Node position offset by the X translation value of the transform matrix
    val nodeX =
        style.layoutStyle.margin.start.pointsAsDp(density).value.toDouble() + decomposed.translateX
    // Y Node position offset by the Y translation value of the transform matrix
    val nodeY =
        style.layoutStyle.margin.top.pointsAsDp(density).value.toDouble() + decomposed.translateY

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
    return nodeStyle.transform.asComposeTransform(density) ?: androidx.compose.ui.graphics.Matrix()
}

private fun lerp(start: Float, end: Float, percent: Float, density: Float): Float {
    return start * density + percent / 100F * (end - start) * density
}

private fun calculateRotationData(
    rotationData: RotationMeterData,
    meterValue: Float,
    style: ViewStyle,
    density: Float,
): androidx.compose.ui.graphics.Matrix {
    val rotation =
        (rotationData.start + meterValue / 100f * (rotationData.end - rotationData.start))
            .coerceDiscrete(rotationData.discrete, rotationData.discrete_value)

    val nodeWidth = style.fixedWidth(density)
    val nodeHeight = style.fixedHeight(density)

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
        moveX - style.layoutStyle.margin.start.pointsAsDp(density).value + xOffsetParent.toFloat(),
        moveY - style.layoutStyle.margin.top.pointsAsDp(density).value + yOffsetParent.toFloat(),
        0f,
    )
    overrideTransform.timesAssign(translateBack)
    return overrideTransform
}

private fun calculateProgressBarData(
    progressBarData: ProgressBarMeterData,
    meterValue: Float,
    style: ViewStyle,
    parent: SquooshResolvedNode?,
    density: Float,
): Pair<Size, androidx.compose.ui.graphics.Matrix?> {
    // Progress bar discrete values are done by percentage
    val discretizedMeterValue =
        meterValue.coerceDiscrete(progressBarData.discrete, progressBarData.discrete_value)

    // Resize the progress bar by interpolating between 0 and endX or endY depending on whether it
    // is a horizontal or vertical progress bar
    if (progressBarData.vertical) {
        val width = style.layoutStyle.width.getDim().pointsAsDp(density).value
        // Calculate bar extents from the parent layout if it exists, or from the progress bar data
        // if not.
        var endY = progressBarData.end_y
        parent?.let { p ->
            val parentSize = p.computedLayout?.let { Size(it.width, it.height) }
            parentSize?.let { pSize ->
                val parentRenderSize = getNodeRenderSize(null, parentSize, p.style, p.layoutId, 1f)
                endY = parentRenderSize.height
            }
        }
        val barHeight = lerp(0F, endY, discretizedMeterValue, density)
        val moveY = (endY * density - barHeight)
        val topOffset = style.layoutStyle.margin.top.pointsAsDp(density).value
        val overrideTransform = style.getTransform(density)
        overrideTransform.setYTranslation(moveY - topOffset)
        return Pair(Size(width, barHeight), overrideTransform)
    } else {
        val height = style.layoutStyle.height.getDim().pointsAsDp(density).value
        // Calculate bar extents from the parent layout if it exists, or from the progress bar data
        // if not.
        var endX = progressBarData.end_x
        parent?.let { p ->
            val parentSize = p.computedLayout?.let { Size(it.width, it.height) }
            parentSize?.let { pSize ->
                val parentRenderSize = getNodeRenderSize(null, parentSize, p.style, p.layoutId, 1f)
                endX = parentRenderSize.width
            }
        }
        val barWidth = lerp(0F, endX, discretizedMeterValue, density)
        return Pair(Size(barWidth, height), null)
    }
}

private fun calculateProgressMarkerData(
    markerData: ProgressMarkerMeterData,
    meterValue: Float,
    style: ViewStyle,
    node: SquooshResolvedNode?,
    parent: SquooshResolvedNode?,
    density: Float,
): androidx.compose.ui.graphics.Matrix {
    // Progress marker discrete values are done by percentage
    val discretizedMeterValue =
        meterValue.coerceDiscrete(markerData.discrete, markerData.discrete_value)

    // Calculate node and parent render sizes if available. These will only be available for
    // squoosh, and will be used to calculate the progress sizes and extents
    val mySize =
        node?.let { l ->
            val mySize = l.computedLayout?.let { Size(it.width, it.height) }
            mySize?.let { getNodeRenderSize(null, it, style, l.layoutId, 1f) }
        }
    val parentSize =
        parent?.let { p ->
            val pSize = p.computedLayout?.let { Size(it.width, it.height) }
            pSize?.let { getNodeRenderSize(null, it, p.style, p.layoutId, 1f) }
        }

    // The indicator mode means we don't resize the node; we just move it
    // along the x or y axis depending on whether it is horizontal or vertical
    val overrideTransform = style.getTransform(density)
    if (markerData.vertical) {
        var startY =
            parentSize?.let { it.height - (mySize?.height ?: 0f) / 2f } ?: markerData.start_y
        val endY = mySize?.let { -it.height / 2f } ?: markerData.end_y
        val moveY = lerp(startY, endY, discretizedMeterValue, density)
        val topOffset = style.layoutStyle.margin.top.pointsAsDp(density).value
        overrideTransform.setYTranslation(moveY - topOffset)
    } else {
        var startX = mySize?.let { -it.width / 2f } ?: markerData.start_x
        var endX = parentSize?.let { it.width - (mySize?.width ?: 0f) / 2f } ?: markerData.end_x
        val moveX = lerp(startX, endX, discretizedMeterValue, density)
        val leftOffset = style.layoutStyle.margin.start.pointsAsDp(density).value
        overrideTransform.setXTranslation(moveX - leftOffset)
    }

    return overrideTransform
}

private fun calculateArcData(
    arcData: ArcMeterData,
    meterValue: Float,
    shape: ViewShape,
): ViewShape {
    // Max out the arc to just below a full circle to avoid having the
    // path completely disappear
    val arcMeterValue = meterValue.coerceAtMost(99.999F)
    val arcAngleMeter =
        (arcMeterValue / 100f * (arcData.end - arcData.start)).coerceDiscrete(
            arcData.discrete,
            arcData.discrete_value,
        )
    if (shape.shape.get() is Shape.Arc) {
        val arc = (shape.shape.get() as Shape.Arc).value
        return ViewShape(
            Optional.of(
                Shape.Arc(
                    VectorArc(
                        listOf(),
                        listOf(),
                        arc.stroke_cap,
                        arcData.start,
                        arcAngleMeter,
                        arc.inner_radius,
                        arcData.corner_radius,
                        arc.is_mask,
                    )
                )
            )
        )
    } else {
        return shape
    }
}

// Set up the paint object to render a vector path as a stroke with a single dash that matches the
// length of the current progress within the vector.
private fun calculateProgressVectorData(
    data: ProgressVectorMeterData,
    paths: ComputedPaths,
    p: Paint,
    style: ViewStyle,
    meterValue: Float,
    density: Float,
) {
    val strokeWidth = style.nodeStyle.stroke.get().stroke_weight.toUniform() * density
    val discretizedMeterValue = meterValue.coerceDiscrete(data.discrete, data.discrete_value)

    // Get full length of path
    var pathLen = 0f
    paths.strokes.forEach {
        val measure = PathMeasure()
        measure.setPath(it, false)
        pathLen += measure.length
    }
    // Create intervals for dashed effect so that the first interval (the solid dash portion) is
    // equal to the length of the vector multiplied by the meter value. The second interval
    // (the empty portion of the dash) is large enough to cover the rest of the path.
    val intervals = floatArrayOf(discretizedMeterValue / 100f * pathLen, pathLen)
    p.pathEffect = PathEffect.dashPathEffect(intervals, 0f)
    p.strokeWidth = strokeWidth
    p.style = PaintingStyle.Stroke
    paths.strokeCap?.let { p.strokeCap = it }
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
    layoutId: Int,
    variableState: VariableState,
    appContext: Context,
) {
    if (size.width <= 0F && size.height <= 0F) return

    drawContext.canvas.save()

    var overrideTransform: androidx.compose.ui.graphics.Matrix? = null
    var rectSize: Size? = null
    var shape = frameShape
    var customArcAngle = false
    var progressVectorMeterData: ProgressVectorMeterData? = null

    val meterValue =
        customizations.getMeterValue(name) ?: customizations.getMeterState(name)?.floatValue
    if (meterValue != null) {
        // Check if there is meter data for a dial/gauge/progress bar
        if (style.nodeStyle.meter_data.isPresent) {
            when (val meterData = style.nodeStyle.meter_data.get().meter_data_type.get()) {
                is MeterDataType.RotationData -> {
                    val rotationData = meterData.value
                    if (rotationData.enabled) {
                        overrideTransform =
                            calculateRotationData(rotationData, meterValue, style, density)
                    }
                }
                is MeterDataType.ProgressBarData -> {
                    val progressBarData = meterData.value
                    if (progressBarData.enabled) {
                        val progressBarSizeTransform =
                            calculateProgressBarData(
                                progressBarData,
                                meterValue,
                                style,
                                null,
                                density,
                            )
                        rectSize = progressBarSizeTransform.first
                        overrideTransform = progressBarSizeTransform.second
                    }
                }
                is MeterDataType.ProgressMarkerData -> {
                    val progressMarkerData = meterData.value
                    if (progressMarkerData.enabled) {
                        overrideTransform =
                            calculateProgressMarkerData(
                                progressMarkerData,
                                meterValue,
                                style,
                                null,
                                null,
                                density,
                            )
                    }
                }
                is MeterDataType.ArcData -> {
                    val arcData = meterData.value
                    if (arcData.enabled) {
                        shape = calculateArcData(arcData, meterValue, shape)
                        customArcAngle = true
                    }
                }
                is MeterDataType.ProgressVectorData -> {
                    // If this is a vector path progress bar, save it here so we can convert it to a
                    // set of path instructions and render it instead of the normal stroke.
                    if (meterData.value.enabled) progressVectorMeterData = meterData.value
                }
            }
        }
    }

    // Push any transforms
    val transform = overrideTransform ?: style.nodeStyle.transform.asComposeTransform(density)
    var vectorScaleX = 1F
    var vectorScaleY = 1F
    if (transform != null) {
        val decomposed = style.nodeStyle.transform.decompose(density)
        vectorScaleX = abs(decomposed.scaleX)
        vectorScaleY = abs(decomposed.scaleY)
        drawContext.transform.transform(transform)
    }

    // Blend mode
    val blendMode = blendModeFromInt(style.nodeStyle.blend_mode).asComposeBlendMode()
    val useBlendMode = blendModeFromInt(style.nodeStyle.blend_mode).useLayer()
    val opacity = style.nodeStyle.opacity.orElse(1.0f)

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
            layoutId,
            variableState,
            ComputedPathCache(),
        )

    val customFillBrushFunction = customizations.getBrushFunction(name)
    val customFillBrush =
        if (customFillBrushFunction != null) {
            customFillBrushFunction()
        } else {
            customizations.getBrush(name)
        }

    val brushSize = getNodeRenderSize(rectSize, size, style, layoutId, density)
    val fillBrush: List<Paint> =
        if (customFillBrush != null) {
            val p = Paint()
            customFillBrush.applyTo(brushSize, p, 1.0f)
            listOf(p)
        } else {
            style.nodeStyle.backgrounds.mapNotNull { background ->
                val p = Paint()
                val b = background.asBrush(appContext, document, density, variableState)
                if (b != null) {
                    val (brush, fillOpacity) = b
                    brush.applyTo(brushSize, p, fillOpacity)
                    p
                } else {
                    null
                }
            }
        }

    val strokeBrush =
        style.nodeStyle.stroke.get().strokes.mapNotNull { background ->
            val p = Paint()
            progressVectorMeterData?.let {
                calculateProgressVectorData(it, shapePaths, p, style, meterValue!!, density)
            }
            val b = background.asBrush(appContext, document, density, variableState)
            if (b != null) {
                val (brush, strokeOpacity) = b
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
        val shadowBox =
            (shadow.shadowStyle.shadow_box.get() as? ShadowBox.Outset)?.value ?: return@forEach

        // Make an appropriate paint.
        val shadowPaint = Paint().asFrameworkPaint()
        shadowPaint.color =
            shadowBox.color.get().getValue(variableState)?.toArgb() ?: return@forEach
        if (shadowBox.blur_radius > 0.0f) {
            shadowPaint.maskFilter =
                BlurMaskFilter(
                    shadowBox.blur_radius * density * blurFudgeFactor,
                    BlurMaskFilter.Blur.NORMAL,
                )
        }
        drawContext.canvas.translate(shadowBox.offset_x * density, shadowBox.offset_y * density)
        shadow.fills.forEach { shadowPath ->
            drawContext.canvas.nativeCanvas.drawPath(shadowPath.asAndroidPath(), shadowPaint)
        }
        drawContext.canvas.translate(-shadowBox.offset_x * density, -shadowBox.offset_y * density)
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
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
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
        val shadowBox =
            (shadow.shadowStyle.shadow_box.get() as? ShadowBox.Inset)?.value ?: return@forEach

        // Make an appropriate paint.
        val shadowPaint = Paint().asFrameworkPaint()
        shadowPaint.color =
            shadowBox.color.get().getValue(variableState)?.toArgb() ?: return@forEach
        if (shadowBox.blur_radius > 0.0f) {
            shadowPaint.maskFilter =
                BlurMaskFilter(
                    shadowBox.blur_radius * density * blurFudgeFactor,
                    BlurMaskFilter.Blur.NORMAL,
                )
        }
        drawContext.canvas.translate(shadowBox.offset_x * density, shadowBox.offset_y * density)
        shadow.fills.forEach { shadowPath ->
            drawContext.canvas.nativeCanvas.drawPath(shadowPath.asAndroidPath(), shadowPaint)
        }
        drawContext.canvas.translate(-shadowBox.offset_x * density, -shadowBox.offset_y * density)
    }
    drawContext.canvas.restore()

    // Now draw our stroke and our children. The order of drawing the stroke and the
    // children is different depending on whether we clip children.
    val shouldClip = overflowFromInt(style.nodeStyle.overflow) is Overflow.Hidden
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

internal fun ContentDrawScope.squooshShapeRender(
    drawContext: DrawContext,
    density: Float,
    size: Size,
    node: SquooshResolvedNode,
    frameShape: ViewShape,
    document: DocContent,
    customizations: CustomizationContext,
    variableState: VariableState,
    computedPathCache: ComputedPathCache,
    appContext: Context,
    drawContent: () -> Unit,
) {
    if (size.width <= 0F && size.height <= 0F) return
    val overrideLayoutSize = node.overrideLayoutSize
    val style = node.style
    val name = node.view.name

    drawContext.canvas.save()

    var overrideTransform: androidx.compose.ui.graphics.Matrix? = null
    var rectSize: Size? = if (overrideLayoutSize) size else null
    var shape = frameShape
    var customArcAngle = false
    var progressVectorMeterData: ProgressVectorMeterData? = null

    val meterValue =
        customizations.getMeterValue(name) ?: customizations.getMeterState(name)?.floatValue
    if (meterValue != null) {
        // Check if there is meter data for a dial/gauge/progress bar
        if (style.nodeStyle.meter_data.isPresent) {
            when (val meterData = style.nodeStyle.meter_data.get().meter_data_type.get()) {
                is MeterDataType.RotationData -> {
                    val rotationData = meterData.value
                    if (rotationData.enabled) {
                        overrideTransform =
                            calculateRotationData(rotationData, meterValue, style, density)
                    }
                }
                is MeterDataType.ProgressBarData -> {
                    val progressBarData = meterData.value
                    if (progressBarData.enabled) {
                        val progressBarSizeTransform =
                            calculateProgressBarData(
                                progressBarData,
                                meterValue,
                                style,
                                node.parent,
                                density,
                            )
                        rectSize = progressBarSizeTransform.first
                        overrideTransform = progressBarSizeTransform.second
                    }
                }
                is MeterDataType.ProgressMarkerData -> {
                    val progressMarkerData = meterData.value
                    if (progressMarkerData.enabled) {
                        overrideTransform =
                            calculateProgressMarkerData(
                                progressMarkerData,
                                meterValue,
                                style,
                                node,
                                node.parent,
                                density,
                            )
                    }
                }
                is MeterDataType.ArcData -> {
                    val arcData = meterData.value
                    if (arcData.enabled) {
                        shape = calculateArcData(arcData, meterValue, shape)
                        customArcAngle = true
                    }
                }
                is MeterDataType.ProgressVectorData -> {
                    // If this is a vector path progress bar, save it here so we can convert it to a
                    // set of path instructions and render it instead of the normal stroke.
                    if (meterData.value.enabled) progressVectorMeterData = meterData.value
                }
            }
        }
    }

    // Push any transforms
    val transform = overrideTransform ?: style.nodeStyle.transform.asComposeTransform(density)
    var vectorScaleX = 1F
    var vectorScaleY = 1F
    if (transform != null) {
        val decomposed = style.nodeStyle.transform.decompose(density)
        vectorScaleX = abs(decomposed.scaleX)
        vectorScaleY = abs(decomposed.scaleY)
        drawContext.transform.transform(transform)
    }

    // Compute the paths we will render from the shape.
    // This could benefit from more optimization:
    //  - Extract from the "draw" phase, or cache across draws (as the path generally doesn't
    // change)
    //  - Generate "rect" and "rounded rect" as special cases, because Skia has fastpaths for those.
    val shapePaths =
        shape.computePaths(
            style,
            density,
            size,
            // Pass in the actual layout-calculated size to ensure that layout size animations work
            // correctly. This likely breaks size calculation for rotated nodes, because
            // DesignCompose weirdly considers the size to be "post rotation bounding box" but
            // layout doesn't actually consider rotation yet.
            rectSize,
            customArcAngle,
            node.layoutId,
            variableState,
            computedPathCache,
        )

    // Blend mode
    val blendMode = blendModeFromInt(style.nodeStyle.blend_mode).asComposeBlendMode()
    val useBlendMode = blendModeFromInt(style.nodeStyle.blend_mode).useLayer()
    val opacity = style.nodeStyle.opacity.orElse(1.0f)

    // Always use saveLayer for opacity; no graphicsLayer since we're not in
    // Compose.
    if (useBlendMode || opacity < 1.0f) {
        val paint = Paint()
        paint.alpha = opacity
        paint.blendMode = blendMode
        // Compute the outset of the layer - it must include the bounds of any outset
        // stroke or shadow.
        var shadowOutset = 0.0f
        for (shadow in shapePaths.shadowFills) {
            (shadow.shadowStyle.shadow_box.get() as? ShadowBox.Outset)?.value?.let { shadowBox ->
                shadowOutset =
                    max(
                        shadowOutset,
                        shadowBox.blur_radius * blurFudgeFactor +
                            shadowBox.spread_radius +
                            max(shadowBox.offset_x, shadowBox.offset_y),
                    )
            }
        }
        var strokeOutset = 0.0f
        val strokeStyle = style.nodeStyle.stroke.get()
        if (strokeStyle.strokes.isNotEmpty()) {
            strokeOutset =
                max(
                    strokeOutset,
                    when (strokeAlignFromInt(strokeStyle.stroke_align)) {
                        StrokeAlignType.Outside -> strokeStyle.stroke_weight.max()
                        StrokeAlignType.Center -> strokeStyle.stroke_weight.max() / 2.0f
                        else -> 0.0f
                    },
                )
        }
        // The shadow outset is additive to the stroke outset, as shadows are applied to the stroke
        // bounds, not the node bounds.
        val outset = strokeOutset + shadowOutset
        // Now we can save the layer with the appropriate bounds.
        drawContext.canvas.saveLayer(Rect(Offset.Zero, size).inflate(outset * density), paint)
    }

    val customFillBrushFunction = customizations.getBrushFunction(name)
    val customFillBrush =
        if (customFillBrushFunction != null) {
            customFillBrushFunction()
        } else {
            customizations.getBrush(name)
        }

    val brushSize = getNodeRenderSize(rectSize, size, style, node.layoutId, density)
    val fillBrush: List<Paint> =
        if (customFillBrush != null) {
            val p = Paint()
            customFillBrush.applyTo(brushSize, p, 1.0f)
            listOf(p)
        } else {
            style.nodeStyle.backgrounds.mapNotNull { background ->
                val p = Paint()
                val b = background.asBrush(appContext, document, density, variableState)
                if (b != null) {
                    val (brush, fillOpacity) = b
                    brush.applyTo(brushSize, p, fillOpacity)
                    p
                } else {
                    null
                }
            }
        }
    val strokeBrush =
        style.nodeStyle.stroke.get().strokes.mapNotNull { background ->
            val p = Paint()
            progressVectorMeterData?.let {
                calculateProgressVectorData(it, shapePaths, p, style, meterValue!!, density)
            }
            val b = background.asBrush(appContext, document, density, variableState)
            if (b != null) {
                val (brush, strokeOpacity) = b
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
        val shadowBox =
            (shadow.shadowStyle.shadow_box.get() as? ShadowBox.Outset)?.value ?: return@forEach

        // Make an appropriate paint.
        val shadowPaint = Paint().asFrameworkPaint()
        shadowPaint.color =
            shadowBox.color.get().getValue(variableState)?.toArgb() ?: return@forEach
        if (shadowBox.blur_radius > 0.0f) {
            shadowPaint.maskFilter =
                BlurMaskFilter(
                    shadowBox.blur_radius * density * blurFudgeFactor,
                    BlurMaskFilter.Blur.NORMAL,
                )
        }
        drawContext.canvas.translate(shadowBox.offset_x * density, shadowBox.offset_y * density)
        shadow.fills.forEach { shadowPath ->
            drawContext.canvas.nativeCanvas.drawPath(shadowPath.asAndroidPath(), shadowPaint)
        }
        drawContext.canvas.translate(-shadowBox.offset_x * density, -shadowBox.offset_y * density)
    }
    drawContext.canvas.restore()

    // Now draw the actual shape, or fill it with an image if we have an image
    // replacement; we might want to do image replacement as a Brush in the
    // future.
    var customImage = customizations.getImage(name)
    if (customImage == null) {
        // Check for an image customization with context. If it exists, call the custom image
        // function and provide it with the frame's background and size.
        customizations.getImageWithContext(node.view.name)?.let {
            customImage =
                it(
                    object : ImageReplacementContext {
                        override val imageContext =
                            ImageContext(
                                background = node.style.node_style.get().backgrounds,
                                minWidth = node.style.layout_style.get().min_width.getDim(),
                                maxWidth = node.style.layout_style.get().max_width.getDim(),
                                width = node.style.layout_style.get().width.getDim(),
                                minHeight = node.style.layout_style.get().min_height.getDim(),
                                maxHeight = node.style.layout_style.get().max_height.getDim(),
                                height = node.style.layout_style.get().height.getDim(),
                            )
                    }
                )
        }
    }
    if (customImage != null) {
        // Apply custom image as background
        drawContext.canvas.save()
        for (fill in shapePaths.fills) {
            drawContext.canvas.clipPath(fill)
        }
        drawImage(
            customImage!!.asImageBitmap(),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
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
        val shadowBox =
            (shadow.shadowStyle.shadow_box.get() as? ShadowBox.Inset)?.value ?: return@forEach

        // Make an appropriate paint.
        val shadowPaint = Paint().asFrameworkPaint()
        shadowPaint.color =
            shadowBox.color.get().getValue(variableState)?.toArgb() ?: return@forEach
        if (shadowBox.blur_radius > 0.0f) {
            shadowPaint.maskFilter =
                BlurMaskFilter(
                    shadowBox.blur_radius * density * blurFudgeFactor,
                    BlurMaskFilter.Blur.NORMAL,
                )
        }
        drawContext.canvas.translate(shadowBox.offset_x * density, shadowBox.offset_y * density)
        shadow.fills.forEach { shadowPath ->
            drawContext.canvas.nativeCanvas.drawPath(shadowPath.asAndroidPath(), shadowPaint)
        }
        drawContext.canvas.translate(-shadowBox.offset_x * density, -shadowBox.offset_y * density)
    }
    drawContext.canvas.restore()

    // Now draw our stroke and our children. The order of drawing the stroke and the
    // children is different depending on whether we clip children.
    val shouldClip = overflowFromInt(style.nodeStyle.overflow) is Overflow.Hidden
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

    if (useBlendMode || opacity < 1.0f) {
        drawContext.canvas.restore()
    }
    drawContext.canvas.restore()
}
