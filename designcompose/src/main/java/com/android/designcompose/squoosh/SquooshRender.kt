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

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.unit.Density
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DocContent
import com.android.designcompose.TextMeasureData
import com.android.designcompose.VariableState
import com.android.designcompose.asComposeBlendMode
import com.android.designcompose.asComposeTransform
import com.android.designcompose.getBrush
import com.android.designcompose.getBrushFunction
import com.android.designcompose.isMask
import com.android.designcompose.pointsAsDp
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.TextAlignVertical
import com.android.designcompose.serdegen.TextOverflow
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewStyle
import com.android.designcompose.squooshShapeRender
import com.android.designcompose.useLayer
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
    animationValues: State<Map<Int, Float>>,
    variableState: VariableState,
): Modifier =
    this.then(
        Modifier.drawWithContent {
            val animValues = animationValues.value
            for ((id, transition) in animations) {
                val animationOffset = animValues[id]
                if (animationOffset == null) {
                    // This happens if we render before the coroutine that updates the animation
                    // values has run; we simply supply an additional frame with the start value.
                    transition.control.apply(transition.animation.initialValue)
                    continue
                }

                transition.control.apply(animationOffset)
            }

            var nodeRenderCount = 0
            val renderTime = measureTimeMillis {
                fun renderNode(node: SquooshResolvedNode) {
                    val computedLayout = node.computedLayout ?: return
                    val shape =
                        when (node.view.data) {
                            is ViewData.Container -> (node.view.data as ViewData.Container).shape
                            else -> {
                                if (node.textInfo != null) {
                                    squooshTextRender(
                                        drawContext,
                                        this,
                                        node.textInfo,
                                        node.style,
                                        computedLayout,
                                        customizations,
                                        node.view.name,
                                    )
                                    nodeRenderCount++
                                }
                                return
                            }
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
                            -offsetFromRoot.y * density
                        )
                        drawContent()
                        drawContext.canvas.translate(
                            offsetFromRoot.x * density,
                            offsetFromRoot.y * density
                        )
                        childRenderSelector.selectedRenderChild = null
                    }

                    val style = node.style
                    val nodeWidth =
                        if (style.layout_style.width is Dimension.Points)
                            style.layout_style.width.pointsAsDp(density).value
                        else style.node_style.node_size.width
                    val nodeHeight =
                        if (style.layout_style.height is Dimension.Points)
                            style.layout_style.height.pointsAsDp(density).value
                        else style.node_style.node_size.height

                    // If we have masked children, then we need to do create a layer for the parent
                    // and have the child draw into a layer that's blended with DstIn.
                    //
                    // XXX: We could take the smallest of the mask size and common parent size, and
                    //      then transform children appropriately.
                    val nodeSize =
                        //Size(computedLayout.width * density, computedLayout.height * density)
                        Size(nodeWidth, nodeHeight)


                    squooshShapeRender(
                        drawContext,
                        density,
                        nodeSize,
                        node.style,
                        shape,
                        null, // customImageWithContext
                        document,
                        node.view.name,
                        customizations,
                        variableState,
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
                                        pendingMask.computedLayout!!.top * density
                                    ) {
                                        renderNode(pendingMask!!)
                                    }

                                    drawContext.canvas.restore()

                                    // Restore the layer that got saved for the mask and content.
                                    drawContext.canvas.restore()
                                }

                                // We're starting a mask operation, so save a layer, and go on to
                                // render children. If we encounter another mask, or if we get to
                                // the end of the children, then we need to pop the mask.
                                pendingMask = child
                                child = child.nextSibling

                                drawContext.canvas.saveLayer(nodeSize.toRect(), Paint())
                            } else {
                                val childLayout = child.computedLayout
                                if (childLayout != null) {
                                    translate(
                                        childLayout.left * density,
                                        childLayout.top * density
                                    ) {
                                        renderNode(child!!)
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
                                pendingMask.computedLayout!!.top * density
                            ) {
                                renderNode(pendingMask)
                            }

                            drawContext.canvas.restore()

                            // Restore the layer that got saved for the mask and content.
                            drawContext.canvas.restore()
                        }
                    }
                    nodeRenderCount++
                }
                renderNode(node)
            }
            if (renderTime > 16)
                Log.d(TAG, "$docName rendered $nodeRenderCount nodes in ${renderTime}ms")
        }
    )

@OptIn(ExperimentalTextApi::class)
private fun squooshTextRender(
    drawContext: DrawContext,
    density: Density,
    textInfo: TextMeasureData,
    style: ViewStyle,
    computedLayout: Layout,
    customizations: CustomizationContext,
    nodeName: String,
) {
    val paragraph =
        Paragraph(
            paragraphIntrinsics = textInfo.paragraph,
            width = computedLayout.width * density.density,
            maxLines = textInfo.maxLines,
            ellipsis = style.node_style.text_overflow is TextOverflow.Ellipsis
        )

    // Apply any styled transform or blend mode.
    // XXX: transform customization?
    val transform = style.node_style.transform.asComposeTransform(density.density)
    val blendMode = style.node_style.blend_mode.asComposeBlendMode()
    val useBlendModeLayer = style.node_style.blend_mode.useLayer()
    val opacity = style.node_style.opacity.orElse(1.0f)

    if (useBlendModeLayer || opacity < 1.0f) {
        val paint = Paint()
        paint.blendMode = blendMode
        paint.alpha = opacity
        drawContext.canvas.saveLayer(
            Rect(
                0f,
                0f,
                computedLayout.width * density.density,
                computedLayout.height * density.density
            ),
            paint
        )
    } else if (transform != null) {
        drawContext.canvas.save()
    }

    if (transform != null) drawContext.transform.transform(transform)

    // Apply vertical centering; this would be better done in layout.
    val verticalCenterOffset =
        when (style.node_style.text_align_vertical) {
            is TextAlignVertical.Center ->
                (computedLayout.height * density.density - paragraph.height) / 2f
            is TextAlignVertical.Bottom ->
                computedLayout.height * density.density - paragraph.height
            else -> 0.0f
        }

    val customFillBrushFunction = customizations.getBrushFunction(nodeName)
    val customFillBrush =
        if (customFillBrushFunction != null) {
            customFillBrushFunction()
        } else {
            customizations.getBrush(nodeName)
        }

    drawContext.canvas.translate(0.0f, verticalCenterOffset)
    if (customFillBrush != null) paragraph.paint(drawContext.canvas, brush = customFillBrush)
    else paragraph.paint(drawContext.canvas)
    drawContext.canvas.translate(0.0f, -verticalCenterOffset)

    if (useBlendModeLayer || opacity < 1.0f || transform != null) drawContext.canvas.restore()
}
