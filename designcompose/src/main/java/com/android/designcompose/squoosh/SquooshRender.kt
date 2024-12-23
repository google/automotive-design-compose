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

package com.android.designcompose.squoosh

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.unit.Density
import com.android.designcompose.ComputedPathCache
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DocContent
import com.android.designcompose.TextMeasureData
import com.android.designcompose.VariableState
import com.android.designcompose.android_interface.LayoutChangedResponse
import com.android.designcompose.definition.modifier.TextAlignVertical
import com.android.designcompose.definition.modifier.TextOverflow
import com.android.designcompose.definition.view.ViewStyle
import com.android.designcompose.definition.view.strokeOrNull
import com.android.designcompose.definition.view.textColorOrNull
import com.android.designcompose.definition.view.transformOrNull
import com.android.designcompose.getBrush
import com.android.designcompose.getBrushFunction
import com.android.designcompose.squooshShapeRender
import com.android.designcompose.utils.asBrush
import com.android.designcompose.utils.asComposeBlendMode
import com.android.designcompose.utils.asComposeTransform
import com.android.designcompose.utils.isMask
import com.android.designcompose.utils.toUniform
import com.android.designcompose.utils.useLayer
import kotlin.system.measureTimeMillis

// This is a holder for the current child composable that we want to draw. Compose doesn't
// let us draw children individually, so instead, any time we want to draw a child we set it
// in an instance of the holder, and draw all children, and have each child filter if it draws
// or not. Terrible hack, but it's not clear what the alternatives are.
internal class SquooshChildRenderSelector(var selectedRenderChild: SquooshResolvedNode? = null)

/// Render an entire squoosh tree, starting with `node`.
internal fun Modifier.squooshRender(
    node: SquooshResolvedNode,
    document: DocContent,
    docName: String,
    customizations: CustomizationContext,
    childRenderSelector: SquooshChildRenderSelector,
    animations: Map<Int, SquooshAnimationRenderingInfo>,
    animPlayTimeNanosState: State<Map<Int, Long>>,
    variableState: VariableState,
    hasModeOverride: Boolean,
    computedPathCache: ComputedPathCache,
    appContext: Context,
    scrollOffset: State<Offset>,
): Modifier =
    this.then(
        Modifier.drawWithContent {
            val parentNode = node.view
            computedPathCache.collect()
            val animPlayTimesMap = animPlayTimeNanosState.value
            for ((id, transition) in animations) {
                val animPlayTimeNanos = animPlayTimesMap[id]
                if (animPlayTimeNanos == null) {
                    // This happens if we render before the coroutine that updates the animation
                    // values has run; we simply supply an additional frame with the start value.
                    transition.control.apply(transition.control.animation?.initialValue ?: 0F)
                    continue
                }

                transition.control.updateValuesFromNanos(animPlayTimeNanos)
                transition.control.apply()
            }

            var nodeRenderCount = 0
            val renderTime = measureTimeMillis {
                fun renderNode(node: SquooshResolvedNode, parentVariableState: VariableState) {
                    // If there is no programmatic mode override and this node has explicitly set
                    // mode values, update variablestate with these values
                    val variableState =
                        if (!hasModeOverride && node.view.explicitVariableModesMap.isNotEmpty()) {
                            val explicitModeValues = node.view.explicitVariableModesMap
                            parentVariableState.copyWithModeValues(explicitModeValues)
                        } else {
                            parentVariableState
                        }
                    val computedLayout = node.computedLayout ?: return
                    // Translate by the scroll amount if the parent node is the scroll view.
                    // Call drawContext.canvas.restore() if scrolling is enabled in order to restore
                    // the canvas matrix/clip state. This is a good candidate for RAII but this is
                    // not supported by Kotlin.
                    val scroll = node.parent?.view == parentNode
                    if (scroll) {
                        drawContext.canvas.save()
                        drawContext.canvas.translate(-scrollOffset.value.x, -scrollOffset.value.y)
                    }
                    val shape =
                        if (node.view.data.hasContainer()) {
                            node.view.data.container.shape
                        } else {
                            if (node.textInfo != null) {
                                squooshTextRender(
                                    document,
                                    drawContext,
                                    this,
                                    node.textInfo,
                                    node.style,
                                    computedLayout,
                                    customizations,
                                    node.view.name,
                                    variableState,
                                    appContext = appContext,
                                )
                                nodeRenderCount++
                            }
                            if (scroll) drawContext.canvas.restore()
                            return
                        }

                    if (node.needsChildRender) {
                        // We need to offset the translation that we did to position the child
                        // Composable for Compose's layout phase. We lay the child out in the
                        // correct position so that hit testing works, but we've already got the
                        // full transform computed here... so we need to invert that.
                        val offsetFromRoot = node.offsetFromAncestor()
                        childRenderSelector.selectedRenderChild = node
                        drawContext.canvas.translate(
                            -offsetFromRoot.x * density,
                            -offsetFromRoot.y * density,
                        )
                        drawContent()
                        drawContext.canvas.translate(
                            offsetFromRoot.x * density,
                            offsetFromRoot.y * density,
                        )
                        childRenderSelector.selectedRenderChild = null
                    } else {
                        // If we need to do a child render, then don't render the content defined
                        // in the view tree, and just let the content render everything.

                        // If we have masked children, then we need to do create a layer for the
                        // parent
                        // and have the child draw into a layer that's blended with DstIn.
                        //
                        // XXX: We could take the smallest of the mask size and common parent size,
                        // and
                        //      then transform children appropriately.
                        val nodeSize =
                            Size(computedLayout.width * density, computedLayout.height * density)

                        squooshShapeRender(
                            drawContext,
                            density,
                            nodeSize,
                            node,
                            shape,
                            document,
                            customizations,
                            variableState,
                            computedPathCache,
                            appContext,
                        ) {
                            var child = node.firstChild
                            var pendingMask: SquooshResolvedNode? = null

                            while (child != null) {
                                if (child.view.isMask()) {
                                    // We were already drawing a mask! Wrap it up...
                                    if (pendingMask != null) {
                                        val dstInPaint = Paint()
                                        dstInPaint.blendMode = BlendMode.DstIn

                                        // Draw the mask as DstIn
                                        drawContext.canvas.saveLayer(nodeSize.toRect(), dstInPaint)
                                        translate(
                                            pendingMask.computedLayout!!.left * density,
                                            pendingMask.computedLayout!!.top * density,
                                        ) {
                                            renderNode(pendingMask!!, variableState)
                                        }

                                        drawContext.canvas.restore()

                                        // Restore the layer that got saved for the mask and
                                        // content.
                                        drawContext.canvas.restore()
                                    }

                                    // We're starting a mask operation, so save a layer, and go on
                                    // to
                                    // render children. If we encounter another mask, or if we get
                                    // to
                                    // the end of the children, then we need to pop the mask.
                                    pendingMask = child
                                    child = child.nextSibling

                                    drawContext.canvas.saveLayer(nodeSize.toRect(), Paint())
                                } else {
                                    val childLayout = child.computedLayout
                                    if (childLayout != null) {
                                        translate(
                                            childLayout.left * density,
                                            childLayout.top * density,
                                        ) {
                                            renderNode(child!!, variableState)
                                        }
                                    }
                                    child = child.nextSibling
                                }
                            }

                            // XXX: This logic is duplicated above; it needs to be factored out
                            //      somehow.
                            if (pendingMask != null) {
                                val dstInPaint = Paint()
                                dstInPaint.blendMode = BlendMode.DstIn

                                // Draw the mask as DstIn
                                drawContext.canvas.saveLayer(nodeSize.toRect(), dstInPaint)
                                translate(
                                    pendingMask.computedLayout!!.left * density,
                                    pendingMask.computedLayout!!.top * density,
                                ) {
                                    renderNode(pendingMask, variableState)
                                }

                                drawContext.canvas.restore()

                                // Restore the layer that got saved for the mask and content.
                                drawContext.canvas.restore()
                            }
                        }
                    }
                    nodeRenderCount++
                    if (scroll) drawContext.canvas.restore()
                }
                renderNode(node, variableState)
            }
            if (renderTime > 16)
                Log.d(TAG, "$docName rendered $nodeRenderCount nodes in ${renderTime}ms")
        }
    )

@OptIn(ExperimentalTextApi::class)
private fun squooshTextRender(
    document: DocContent,
    drawContext: DrawContext,
    density: Density,
    textInfo: TextMeasureData,
    style: ViewStyle,
    computedLayout: LayoutChangedResponse.Layout,
    customizations: CustomizationContext,
    nodeName: String,
    variableState: VariableState,
    appContext: Context,
) {
    val layoutWidth = computedLayout.width * density.density
    val paragraph =
        Paragraph(
            paragraphIntrinsics = textInfo.paragraph,
            width = layoutWidth,
            maxLines = textInfo.maxLines,
            ellipsis = style.nodeStyle.textOverflow == TextOverflow.TEXT_OVERFLOW_ELLIPSIS,
        )

    val layoutHeight = computedLayout.height * density.density

    // Apply any styled transform or blend mode.
    // XXX: transform customization?
    val transform = style.nodeStyle.transformOrNull?.asComposeTransform(density.density)
    val blendMode = style.nodeStyle.blendMode.asComposeBlendMode()
    val useBlendModeLayer = style.nodeStyle.blendMode.useLayer()
    val opacity = style.nodeStyle.takeIf { it.hasOpacity() }?.opacity ?: 1.0f

    if (useBlendModeLayer || opacity < 1.0f) {
        val paint = Paint()
        paint.blendMode = blendMode
        paint.alpha = opacity
        drawContext.canvas.saveLayer(Rect(0f, 0f, layoutWidth, layoutHeight), paint)
    } else if (transform != null) {
        drawContext.canvas.save()
    }

    if (transform != null) drawContext.transform.transform(transform)

    // Apply vertical centering; this would be better done in layout.
    val verticalCenterOffset =
        when (style.nodeStyle.textAlignVertical) {
            TextAlignVertical.TEXT_ALIGN_VERTICAL_CENTER ->
                (layoutHeight - paragraph.height).coerceAtLeast(0f) / 2f
            TextAlignVertical.TEXT_ALIGN_VERTICAL_BOTTOM ->
                (layoutHeight - paragraph.height).coerceAtLeast(0f)
            else -> 0.0f
        }

    val customFillBrushFunction = customizations.getBrushFunction(nodeName)
    val customFillBrush =
        if (customFillBrushFunction != null) {
            customFillBrushFunction()
        } else {
            customizations.getBrush(nodeName)
        }

    drawContext.canvas.save()
    drawContext.canvas.translate(0.0f, verticalCenterOffset)

    val strokeWidth = style.nodeStyle.stroke.strokeWeight.toUniform() * density.density
    // Only drop shadow is supported now.
    val shadow = style.nodeStyle.textShadow
    val blurRadius = shadow.blurRadius * density.density
    val xOffset = shadow.offsetX * density.density
    val yOffset = shadow.offsetY * density.density

    // Right now only centered stroke is supported
    val extraSpaceForStroke = strokeWidth / 2
    val extraSpaceAtBottom = (blurRadius + yOffset + extraSpaceForStroke).coerceAtLeast(0f)
    val clipBottom =
        if (paragraph.height > layoutHeight) {
            layoutHeight
        } else if (paragraph.height + extraSpaceAtBottom > layoutHeight) {
            paragraph.height + extraSpaceAtBottom
        } else {
            layoutHeight
        }
    drawContext.canvas.clipRect(
        (xOffset - blurRadius - extraSpaceForStroke).coerceAtMost(0f),
        (yOffset - blurRadius - extraSpaceForStroke).coerceAtMost(0f),
        (xOffset + blurRadius + extraSpaceForStroke).coerceAtLeast(0f) + layoutWidth,
        clipBottom,
    )

    // Every time calling paragraph.paint will save the new brush, alpha and drawStyle to the
    // paragraph. We need to pass the text brush, opacity and draw style explicitly to make sure
    // it uses the right brush to draw the fill color.
    if (customFillBrush != null) {
        paragraph.paint(drawContext.canvas, brush = customFillBrush, alpha = 1.0f, drawStyle = Fill)
    } else {
        val textBrushAndOpacity =
            style.nodeStyle.textColorOrNull?.asBrush(
                appContext = appContext,
                document = document,
                density = density.density,
                variableState = variableState,
            )
        paragraph.paint(
            drawContext.canvas,
            brush = textBrushAndOpacity?.first ?: SolidColor(Color.Transparent),
            alpha = textBrushAndOpacity?.second ?: 1.0f,
            drawStyle = Fill,
        )
    }

    style.nodeStyle.strokeOrNull?.strokesList?.forEach {
        val strokeBrushAndOpacity =
            it.asBrush(
                appContext = appContext,
                document = document,
                variableState = variableState,
                density = density.density,
            )
        strokeBrushAndOpacity?.first?.let { brush ->
            paragraph.paint(
                drawContext.canvas,
                brush = brush,
                alpha = strokeBrushAndOpacity.second,
                drawStyle = Stroke(width = strokeWidth),
            )
        }
    }

    drawContext.canvas.restore()
    if (useBlendModeLayer || opacity < 1.0f || transform != null) drawContext.canvas.restore()
}
