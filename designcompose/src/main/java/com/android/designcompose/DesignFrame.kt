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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.designcompose.serdegen.BoxShadow
import com.android.designcompose.serdegen.ComponentInfo
import com.android.designcompose.serdegen.GridLayoutType
import com.android.designcompose.serdegen.GridSpan
import com.android.designcompose.serdegen.ItemSpacing
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.serdegen.Overflow
import com.android.designcompose.serdegen.StrokeAlign
import com.android.designcompose.serdegen.ViewStyle
import java.util.Optional
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
internal fun DesignFrame(
    modifier: Modifier = Modifier,
    style: ViewStyle,
    name: String,
    variantParentName: String,
    layoutInfo: SimplifiedLayoutInfo,
    document: DocContent,
    customizations: CustomizationContext,
    componentInfo: Optional<ComponentInfo>,
    parentComponents: List<ParentComponentInfo>,
    content: @Composable (parentLayoutInfo: ParentLayoutInfo) -> Unit,
) {
    if (!customizations.getVisible(name)) return

    // Check for an image customization with context. If it exists, call the custom image function
    // and provide it with the frame's background and size.
    val customImageWithContext = customizations.getImageWithContext(name)
    var customImage: Bitmap? = null
    if (customImageWithContext != null) {
        customImage =
            customImageWithContext(
                object : ImageReplacementContext {
                    override val imageContext =
                        ImageContext(
                            background = style.background,
                            minWidth = style.min_width,
                            maxWidth = style.max_width,
                            width = style.width,
                            minHeight = style.min_height,
                            maxHeight = style.max_height,
                            height = style.height,
                        )
                }
            )
    }

    var m =
        Modifier.layoutStyle(name, style)
            .frameRender(style, customImage, document, name, customizations)
            .then(modifier)

    val customModifier = customizations.getModifier(name)
    if (customModifier != null) {
        // We may need more control over where a custom modifier is placed in the list
        // than just always adding at the end.
        m = m.then(customModifier)
    }

    // If we're replaced, then invoke the replacement here. We may want to pass more layout info
    // (row/column/etc) to the replacement component at some point.
    val replacementComponent = customizations.getComponent(name)
    if (replacementComponent != null) {
        replacementComponent(
            object : ComponentReplacementContext {
                override val layoutModifier = layoutInfo.selfModifier
                override val appearanceModifier = m
                @Composable
                override fun Content() {
                    content(absoluteParentLayoutInfo)
                }
                override val textStyle: TextStyle? = null
            }
        )
        return
    }

    // Check to see if this node is part of a component set with variants and if any @DesignVariant
    // annotations set variant properties that match. If so, variantNodeName will be set to the
    // name of the node with all the variants set to the @DesignVariant parameters
    val variantNodeName = customizations.getMatchingVariant(componentInfo)
    if (variantNodeName != null) {
        // Get the generated CustomVariantComponent() function and call it with variantNodeName
        val customComposable = customizations.getCustomComposable()
        if (customComposable != null) {
            val tapCallback = customizations.getTapCallback(name)
            customComposable(
                layoutInfo.selfModifier.then(m),
                variantNodeName,
                NodeQuery.NodeVariant(variantNodeName, variantParentName.ifEmpty { name }),
                parentComponents,
                tapCallback
            )
            return
        }
    }

    val lazyContent = customizations.getListContent(name)

    // Select the appropriate representation for ourselves based on our layout style;
    // row or column (with or without wrapping/flow), or absolute positioning (similar to the CSS2
    // model).
    when (layoutInfo) {
        is LayoutInfoRow -> {
            if (lazyContent != null) {
                val content = lazyContent { LazyContentSpan() }
                var count = content.count
                var overflowNodeId: String? = null
                if (style.max_children.isPresent && style.max_children.get() < count) {
                    count = style.max_children.get()
                    if (style.overflow_node_id.isPresent)
                        overflowNodeId = style.overflow_node_id.get()
                }
                Row(
                    layoutInfo.selfModifier.then(m).then(layoutInfo.childModifier),
                    horizontalArrangement = layoutInfo.arrangement,
                    verticalAlignment = layoutInfo.alignment,
                ) {
                    for (i in 0 until count) {
                        if (overflowNodeId != null && i == count - 1) {
                            // This is the last item we can show and there are more, and there is an
                            // overflow node, so show the overflow node here
                            val customComposable = customizations.getCustomComposable()
                            if (customComposable != null) {
                                customComposable(
                                    Modifier,
                                    style.overflow_node_name.get(),
                                    NodeQuery.NodeId(style.overflow_node_id.get()),
                                    parentComponents,
                                    null
                                )
                            }
                        } else {
                            content.itemContent(i)
                        }
                    }
                }
            } else {
                Row(
                    layoutInfo.selfModifier.then(m).then(layoutInfo.childModifier),
                    horizontalArrangement = layoutInfo.arrangement,
                    verticalAlignment = layoutInfo.alignment
                ) {
                    val childLayoutInfo =
                        ParentLayoutInfo(
                            type = LayoutType.Row,
                            isRoot = false,
                            weight = { w -> Modifier.weight(w) }
                        )
                    content(childLayoutInfo)
                }
            }
        }
        is LayoutInfoColumn -> {
            if (lazyContent != null) {
                val content = lazyContent { LazyContentSpan() }
                var count = content.count
                var overflowNodeId: String? = null
                if (style.max_children.isPresent && style.max_children.get() < count) {
                    count = style.max_children.get()
                    if (style.overflow_node_id.isPresent)
                        overflowNodeId = style.overflow_node_id.get()
                }
                Column(
                    layoutInfo.selfModifier.then(m).then(layoutInfo.childModifier),
                    verticalArrangement = layoutInfo.arrangement,
                    horizontalAlignment = layoutInfo.alignment,
                ) {
                    for (i in 0 until count) {
                        if (overflowNodeId != null && i == count - 1) {
                            // This is the last item we can show and there are more, and there is an
                            // overflow node, so show the overflow node here
                            val customComposable = customizations.getCustomComposable()
                            if (customComposable != null) {
                                customComposable(
                                    Modifier,
                                    style.overflow_node_name.get(),
                                    NodeQuery.NodeId(style.overflow_node_id.get()),
                                    parentComponents,
                                    null
                                )
                            }
                        } else {
                            content.itemContent(i)
                        }
                    }
                }
            } else {
                Column(
                    layoutInfo.selfModifier.then(m).then(layoutInfo.childModifier),
                    verticalArrangement = layoutInfo.arrangement,
                    horizontalAlignment = layoutInfo.alignment
                ) {
                    val childLayoutInfo =
                        ParentLayoutInfo(
                            type = LayoutType.Column,
                            isRoot = false,
                            weight = { w -> Modifier.weight(w) }
                        )
                    content(childLayoutInfo)
                }
            }
        }
        is LayoutInfoGrid -> {
            if (lazyContent == null) return

            // Given the list of possible content that goes into this grid layout, try to find a
            // matching
            // item based on node name and variant properties, and return its span
            fun getSpan(
                gridSpanContent: List<GridSpan>,
                getDesignNodeData: GetDesignNodeData
            ): LazyContentSpan {
                val nodeData = getDesignNodeData()
                val cachedSpan = SpanCache.getSpan(nodeData)
                if (cachedSpan != null) return cachedSpan

                gridSpanContent.forEach { item ->
                    // If not looking for a variant, just find a node name match
                    if (nodeData.variantProperties.isEmpty()) {
                        if (nodeData.nodeName == item.node_name)
                            return LazyContentSpan(span = item.span, maxLineSpan = item.max_span)
                    } else {
                        var spanFound: LazyContentSpan? = null
                        var matchesLeft = nodeData.variantProperties.size
                        item.node_variant.forEach {
                            val property = it.key.trim()
                            val value = it.value.trim()
                            val variantPropertyValue = nodeData.variantProperties[property]
                            if (value == variantPropertyValue) {
                                // We have a match. Decrement the number of matches left we are
                                // looking for
                                --matchesLeft
                                // If we matched everything, we have a possible match. If the number
                                // of properties
                                // and values in propertyValueList is the same as the number of
                                // variant properties
                                // then we are done. Otherwise, this is a possible match, and save
                                // it in spanFound.
                                // If we don't have any exact matches, return spanFound
                                if (matchesLeft == 0) {
                                    if (nodeData.variantProperties.size == item.node_variant.size) {
                                        val span =
                                            if (item.max_span) LazyContentSpan(maxLineSpan = true)
                                            else LazyContentSpan(span = item.span)
                                        SpanCache.setSpan(nodeData, span)
                                        return span
                                    } else
                                        spanFound =
                                            LazyContentSpan(
                                                span = item.span,
                                                maxLineSpan = item.max_span
                                            )
                                }
                            }
                        }
                        if (spanFound != null) {
                            SpanCache.setSpan(nodeData, spanFound!!)
                            return spanFound!!
                        }
                    }
                }
                SpanCache.setSpan(nodeData, LazyContentSpan(span = 1))
                return LazyContentSpan(span = 1)
            }

            val (gridMainAxisSize, setGridMainAxisSize) = remember { mutableStateOf(0) }

            // Content for the lazy content parameter. This uses the grid layout but also supports
            // limiting the number of children to style.max_children, and using an overflow node if
            // one
            // is specified.
            val lazyItemContent: LazyGridScope.() -> Unit = {
                val lContent = lazyContent!! { nodeData ->
                    getSpan(layoutInfo.gridSpanContent, nodeData)
                }

                // If the main axis size has not yet been set, and spacing is set to auto, show the
                // initial content composable. This avoids rendering the content in one position
                // for the first frame and then in another on the second frame after the main axis
                // size has been set.
                val showInitContent =
                    (gridMainAxisSize <= 0 && layoutInfo.mainAxisSpacing is ItemSpacing.Auto)
                if (showInitContent)
                    items(
                        count = 1,
                        span = {
                            val span = lContent.initialSpan?.invoke() ?: LazyContentSpan()
                            GridItemSpan(if (span.maxLineSpan) maxLineSpan else span.span)
                        }
                    ) {
                        lContent.initialContent()
                    }
                else {
                    var count = lContent.count
                    var overflowNodeId: String? = null
                    if (style.max_children.isPresent && style.max_children.get() < count) {
                        count = style.max_children.get()
                        if (style.overflow_node_id.isPresent)
                            overflowNodeId = style.overflow_node_id.get()
                    }
                    items(
                        count,
                        key = { index ->
                            if (overflowNodeId != null && index == count - 1)
                            // Overflow node key
                            "overflow"
                            else lContent.key?.invoke(index) ?: index
                        },
                        span = { index ->
                            if (overflowNodeId != null && index == count - 1)
                            // Overflow node always spans 1 column/row for now
                            GridItemSpan(1)
                            else {
                                val span = lContent.span?.invoke(index) ?: LazyContentSpan()
                                GridItemSpan(if (span.maxLineSpan) maxLineSpan else span.span)
                            }
                        },
                        contentType = { index ->
                            if (overflowNodeId != null && index == count - 1)
                            // Overflow node content type
                            "overflow"
                            else lContent.contentType.invoke(index)
                        },
                        itemContent = { index ->
                            if (overflowNodeId != null && index == count - 1) {
                                // This is the last item we can show and there are more, and there
                                // is an
                                // overflow node, so show the overflow node here
                                val customComposable = customizations.getCustomComposable()
                                if (customComposable != null) {
                                    customComposable(
                                        Modifier,
                                        style.overflow_node_name.get(),
                                        NodeQuery.NodeId(style.overflow_node_id.get()),
                                        parentComponents,
                                        null
                                    )
                                }
                            } else {
                                lContent.itemContent(index)
                            }
                        }
                    )
                }
            }

            // Given the frame size, number of columns/rows, and spacing between them, return a list
            // of
            // column/row widths/heights
            fun calculateCellsCrossAxisSizeImpl(
                gridSize: Int,
                slotCount: Int,
                spacing: Int
            ): List<Int> {
                val gridSizeWithoutSpacing = gridSize - spacing * (slotCount - 1)
                val slotSize = gridSizeWithoutSpacing / slotCount
                val remainingPixels = gridSizeWithoutSpacing % slotCount
                return List(slotCount) { slotSize + if (it < remainingPixels) 1 else 0 }
            }

            // Given the grid layout type and main axis size, return the number of columns/rows
            fun calculateColumnRowCount(layoutInfo: LayoutInfoGrid, gridMainAxisSize: Int): Int {
                val count: Int
                if (
                    layoutInfo.layout is GridLayoutType.FixedColumns ||
                        layoutInfo.layout is GridLayoutType.FixedRows
                ) {
                    count = layoutInfo.numColumnsRows
                } else {
                    count =
                        gridMainAxisSize /
                            (layoutInfo.minColumnRowSize +
                                itemSpacingAbs(layoutInfo.mainAxisSpacing))
                }
                return if (count > 0) count else 1
            }

            val density = LocalDensity.current.density
            if (
                layoutInfo.layout is GridLayoutType.FixedColumns ||
                    layoutInfo.layout is GridLayoutType.AutoColumns
            ) {
                val columnCount = calculateColumnRowCount(layoutInfo, gridMainAxisSize)
                val horizontalSpacing =
                    if (layoutInfo.mainAxisSpacing is ItemSpacing.Fixed)
                        layoutInfo.mainAxisSpacing.value
                    else 0
                val verticalSpacing = layoutInfo.crossAxisSpacing

                LazyVerticalGrid(
                    modifier = layoutInfo.selfModifier.then(m),
                    columns =
                        object : GridCells {
                            override fun Density.calculateCrossAxisCellSizes(
                                availableSize: Int,
                                spacing: Int
                            ): List<Int> {
                                val mainAxisSize = (availableSize.toFloat() / density).toInt()
                                setGridMainAxisSize(mainAxisSize)
                                return calculateCellsCrossAxisSizeImpl(
                                    availableSize,
                                    columnCount,
                                    spacing
                                )
                            }
                        },
                    horizontalArrangement =
                        object : Arrangement.Horizontal {
                            var customSpacing: Int = horizontalSpacing
                            init {
                                if (layoutInfo.mainAxisSpacing is ItemSpacing.Fixed) {
                                    customSpacing = layoutInfo.mainAxisSpacing.value
                                } else if (layoutInfo.mainAxisSpacing is ItemSpacing.Auto) {
                                    customSpacing =
                                        if (columnCount > 1)
                                            (gridMainAxisSize -
                                                (layoutInfo.mainAxisSpacing.field1 * columnCount)) /
                                                (columnCount - 1)
                                        else layoutInfo.mainAxisSpacing.field0
                                }
                            }
                            override fun Density.arrange(
                                totalSize: Int,
                                sizes: IntArray,
                                layoutDirection: LayoutDirection,
                                outPositions: IntArray
                            ) {
                                // Apparently this function does not get called
                                println(
                                    "horizontalArrangement arrange() totalSize $totalSize " +
                                        "sizes $sizes layout $layoutDirection out $outPositions"
                                )
                            }
                            override val spacing = customSpacing.dp
                        },
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing.dp),
                    userScrollEnabled = layoutInfo.scrollingEnabled,
                    contentPadding =
                        PaddingValues(
                            layoutInfo.padding.start.pointsAsDp(),
                            layoutInfo.padding.top.pointsAsDp(),
                            layoutInfo.padding.end.pointsAsDp(),
                            layoutInfo.padding.bottom.pointsAsDp(),
                        ),
                ) {
                    lazyItemContent()
                }
            } else {
                val rowCount = calculateColumnRowCount(layoutInfo, gridMainAxisSize)
                val horizontalSpacing = layoutInfo.crossAxisSpacing
                val verticalSpacing =
                    if (layoutInfo.mainAxisSpacing is ItemSpacing.Fixed)
                        layoutInfo.mainAxisSpacing.value
                    else 0
                LazyHorizontalGrid(
                    modifier = layoutInfo.selfModifier.then(m).then(layoutInfo.childModifier),
                    rows =
                        object : GridCells {
                            override fun Density.calculateCrossAxisCellSizes(
                                availableSize: Int,
                                spacing: Int
                            ): List<Int> {
                                val mainAxisSize = (availableSize.toFloat() / density).toInt()
                                setGridMainAxisSize(mainAxisSize)
                                return calculateCellsCrossAxisSizeImpl(
                                    availableSize,
                                    rowCount,
                                    spacing
                                )
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(horizontalSpacing.dp),
                    verticalArrangement =
                        object : Arrangement.Vertical {
                            var customSpacing: Int = verticalSpacing
                            init {
                                if (layoutInfo.mainAxisSpacing is ItemSpacing.Fixed) {
                                    customSpacing = layoutInfo.mainAxisSpacing.value
                                } else if (layoutInfo.mainAxisSpacing is ItemSpacing.Auto) {
                                    customSpacing =
                                        if (rowCount > 1)
                                            (gridMainAxisSize -
                                                (layoutInfo.mainAxisSpacing.field1 * rowCount)) /
                                                (rowCount - 1)
                                        else layoutInfo.mainAxisSpacing.field0
                                }
                            }
                            override fun Density.arrange(
                                totalSize: Int,
                                sizes: IntArray,
                                outPositions: IntArray
                            ) {
                                println("verticalArrangement arrange")
                            }
                            override val spacing = customSpacing.dp
                        },
                    userScrollEnabled = layoutInfo.scrollingEnabled,
                ) {
                    lazyItemContent()
                }
            }
        }
        is LayoutInfoAbsolute -> {
            DesignLayout(modifier = layoutInfo.selfModifier.then(m), style = style, name = name) {
                content(absoluteParentLayoutInfo)
            }
        }
    }
}

internal fun Modifier.frameRender(
    style: ViewStyle,
    customImageWithContext: Bitmap?,
    document: DocContent,
    name: String,
    customizations: CustomizationContext
): Modifier =
    this.then(
        Modifier.drawWithContent {
            drawContext.canvas.save()

            // Push any transforms
            val transform = style.transform.asComposeTransform(density)
            if (transform != null) drawContext.transform.transform(transform)

            // Blend mode
            val blendMode = style.blend_mode.asComposeBlendMode()
            val useBlendMode = style.blend_mode.useLayer()
            val opacity = style.opacity.orElse(1.0f)

            // Either use a graphicsLayer to apply the opacity effect, or use saveLayer if
            // we have a blend mode.
            if (!useBlendMode && opacity < 1.0f) {
                alpha(opacity)
            }
            if (useBlendMode) {
                val paint = Paint()
                paint.alpha = opacity
                paint.blendMode = blendMode
                drawContext.canvas.saveLayer(Rect(Offset.Zero, drawContext.size), paint)
            }

            // Compute fill and stroke shapes
            var shape = RectangleShape
            if (
                style.border_radius[0] > 0.1 ||
                    style.border_radius[1] > 0.1 ||
                    style.border_radius[2] > 0.1 ||
                    style.border_radius[3] > 0.1
            ) {
                shape =
                    RoundedCornerShape(
                        style.border_radius[0] * density,
                        style.border_radius[1] * density,
                        style.border_radius[3] * density,
                        style.border_radius[2] * density
                    )
            }
            val outline = shape.createOutline(size, layoutDirection, this)
            val outlinePath = Path()
            outlinePath.addOutline(outline)

            // To compute the stroke shape, we might have to offset rounded corners.
            val strokeOutset =
                when (style.stroke.stroke_align) {
                    is StrokeAlign.Inside -> style.stroke.stroke_weight * density / 2.0f
                    is StrokeAlign.Center -> 0.0f
                    // The 0.5f is a fudge factor to try to mask conflation errors in
                    // Skia.
                    is StrokeAlign.Outside -> -style.stroke.stroke_weight * density / 2.0f + 0.5f
                    else -> 0.0f
                }
            // Adjust the radius to fit the adjusted bounds
            fun offsetRadius(radius: Float, offset: Float): Float {
                if (radius <= 0.0f) {
                    // Hard corners always remain hard corners
                    return 0.0f
                }
                // If the offset is negative then we're growing outwards and so the radius
                // must grow. If it's positive then we're shrinking the radius down to 0.0.
                return max(radius - offset, 0.0f)
            }
            var strokeShape = RectangleShape
            if (
                style.border_radius[0] > 0.1 ||
                    style.border_radius[1] > 0.1 ||
                    style.border_radius[2] > 0.1 ||
                    style.border_radius[3] > 0.1
            ) {
                strokeShape =
                    RoundedCornerShape(
                        offsetRadius(style.border_radius[0] * density, strokeOutset),
                        offsetRadius(style.border_radius[1] * density, strokeOutset),
                        offsetRadius(style.border_radius[3] * density, strokeOutset),
                        offsetRadius(style.border_radius[2] * density, strokeOutset)
                    )
            }
            val strokeOutline =
                strokeShape.createOutline(
                    Size(size.width - strokeOutset * 2.0f, size.height - strokeOutset * 2.0f),
                    layoutDirection,
                    this
                )
            val strokePath = Path()
            strokePath.addOutline(strokeOutline)
            strokePath.translate(Offset(strokeOutset, strokeOutset))

            // If we have any outset shadows then clip out our own shape, so we don't fill shadow
            // behind
            // it (Figma recently added the option to fill behind a shape or not for a given shadow,
            // but
            // we're not propagating that information yet).
            val hasOutsetShadows =
                style.box_shadow.find { shadow -> shadow is BoxShadow.Outset } != null
            if (hasOutsetShadows) {
                drawContext.canvas.save()
                drawContext.canvas.clipPath(outlinePath, ClipOp.Difference)

                // We can render outset shadows at this point. We do this by creating a path that
                // corresponds
                // to the solid part of the object (taking shadow spread into account, which expands
                // the
                // solid
                // region) and then render it applying the blur.
                //
                // This is a fairly low-level way to render a shadow, and we also have to clip out
                // the
                // region
                // of the shape itself, but Skia doesn't provide any high level interface for
                // creating
                // shadows.
                for (shadow in style.box_shadow) {
                    if (shadow is BoxShadow.Outset) {
                        // We only care about the stroke outset when it's outset (center or
                        // outside), otherwise
                        // it doesn't change the shadow.
                        val shadowStrokeOutset = strokeOutset.coerceAtMost(0.0f)
                        val spreadOffset = shadow.spread_radius * density - shadowStrokeOutset
                        var outsetShadowShape = RectangleShape
                        if (
                            style.border_radius[0] > 0.1 ||
                                style.border_radius[1] > 0.1 ||
                                style.border_radius[2] > 0.1 ||
                                style.border_radius[3] < 0.1
                        ) {
                            outsetShadowShape =
                                RoundedCornerShape(
                                    offsetRadius(style.border_radius[0] * density, -spreadOffset),
                                    offsetRadius(style.border_radius[1] * density, -spreadOffset),
                                    offsetRadius(style.border_radius[3] * density, -spreadOffset),
                                    offsetRadius(style.border_radius[2] * density, -spreadOffset)
                                )
                        }
                        val outsetShadowOutline =
                            outsetShadowShape.createOutline(
                                Size(
                                    size.width + spreadOffset * 2.0f - shadowStrokeOutset * 2.0f,
                                    size.height + spreadOffset * 2.0f - shadowStrokeOutset * 2.0f
                                ),
                                layoutDirection,
                                this
                            )
                        val outsetShadowPath = Path()
                        outsetShadowPath.addOutline(outsetShadowOutline)
                        outsetShadowPath.translate(
                            Offset(
                                -spreadOffset + shadowStrokeOutset + shadow.offset[0] * density,
                                -spreadOffset + shadowStrokeOutset + shadow.offset[1] * density
                            )
                        )

                        // Ok, now we need to draw this shape, and have it cast a blurry shadow the
                        // appropriate size.
                        val shadowPaint = Paint().asFrameworkPaint()
                        shadowPaint.color = convertColor(shadow.color).toArgb()
                        if (shadow.blur_radius > 0.0f) {
                            shadowPaint.maskFilter =
                                BlurMaskFilter(
                                    shadow.blur_radius * density * blurFudgeFactor,
                                    BlurMaskFilter.Blur.NORMAL
                                )
                        }

                        drawContext.canvas.nativeCanvas.drawPath(
                            outsetShadowPath.asAndroidPath(),
                            shadowPaint
                        )
                    }
                }

                // Restore the clip
                drawContext.canvas.restore()
            }

            /*
            // Color filters, blur, background blur (unsupported)
            for (filterOp in style.filter) {
                when (filterOp) {
                    is FilterOp.Blur -> {
                        // Only supported on Android 12, and not supported on this version
                        // of Jetpack.
                    }
                    is FilterOp.Brightness -> {

                    }
                    is FilterOp.Contrast -> {


                    }
                    is FilterOp.Grayscale -> {

                    }
                }
            }*/

            // Probably this is where we should do save or saveLayer depending on
            // blend modes, filters, etc. We need to pass our current blend mode down
            // to our children, too, so they can break out if they're not doing Passthru.

            // Now we can do the fill

            var customImage = customImageWithContext
            if (customImage == null) customImage = customizations.getImage(name)
            if (customImage != null) {
                // Apply custom image as background
                drawContext.canvas.save()
                drawContext.canvas.clipPath(outlinePath)
                drawImage(
                    customImage.asImageBitmap(),
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                )
                drawContext.canvas.restore()
            } else {
                for (fill in style.background) {
                    val brushAndOpacity = fill.asBrush(document, density)
                    if (brushAndOpacity != null) {
                        val (brush, opacity) = brushAndOpacity
                        drawOutline(outline, brush, alpha = opacity)
                    }
                }
            }

            // Paint inset shadows after painting the background, before painting children
            // and before painting the stroke (regardless of which order children and stroke
            // are in).
            //
            // This logic is very similar to the outset shadow logic, except we create an inverted
            // path, and we worry about inset strokes (center and inside).
            val hasInsetShadows =
                style.box_shadow.find { shadow -> shadow is BoxShadow.Inset } != null
            if (hasInsetShadows) {
                drawContext.canvas.save()
                drawContext.canvas.clipPath(outlinePath)

                for (shadow in style.box_shadow) {
                    if (shadow is BoxShadow.Inset) {
                        // Only inset strokes change the inset shadow bounds.
                        val shadowStrokeOutset = strokeOutset.coerceAtLeast(0.0f)
                        val spreadOffset =
                            shadow.spread_radius * density / 2.0f + shadowStrokeOutset
                        var outsetShadowShape = RectangleShape
                        if (
                            style.border_radius[0] > 0.1 ||
                                style.border_radius[1] > 0.1 ||
                                style.border_radius[2] > 0.1 ||
                                style.border_radius[3] > 0.1
                        ) {
                            outsetShadowShape =
                                RoundedCornerShape(
                                    offsetRadius(style.border_radius[0] * density, spreadOffset),
                                    offsetRadius(style.border_radius[1] * density, spreadOffset),
                                    offsetRadius(style.border_radius[3] * density, spreadOffset),
                                    offsetRadius(style.border_radius[2] * density, spreadOffset)
                                )
                        }
                        val outsetShadowOutline =
                            outsetShadowShape.createOutline(
                                Size(
                                    size.width - spreadOffset * 2.0f - shadowStrokeOutset * 2.0f,
                                    size.height - spreadOffset * 2.0f - shadowStrokeOutset * 2.0f
                                ),
                                layoutDirection,
                                this
                            )
                        val outsetShadowPath = Path()
                        outsetShadowPath.addOutline(outsetShadowOutline)
                        outsetShadowPath.translate(
                            Offset(
                                spreadOffset + shadowStrokeOutset + shadow.offset[0] * density,
                                spreadOffset + shadowStrokeOutset + shadow.offset[1] * density
                            )
                        )
                        outsetShadowPath.asAndroidPath().toggleInverseFillType()

                        // Ok, now we need to draw this shape, and have it cast a blurry shadow the
                        // appropriate size.
                        val shadowPaint = Paint().asFrameworkPaint()
                        shadowPaint.color = convertColor(shadow.color).toArgb()
                        if (shadow.blur_radius > 0.0f) {
                            shadowPaint.maskFilter =
                                BlurMaskFilter(
                                    shadow.blur_radius * density * blurFudgeFactor,
                                    BlurMaskFilter.Blur.NORMAL
                                )
                        }
                        drawContext.canvas.nativeCanvas.drawPath(
                            outsetShadowPath.asAndroidPath(),
                            shadowPaint
                        )
                    }
                }

                drawContext.canvas.restore()
            }

            val shouldClip =
                when (style.overflow) {
                    is Overflow.Hidden -> true
                    else -> false
                }

            // We paint the stroke before we paint the content if we're not clipping, and we paint
            // it
            // after the content if we are clipping. Visually this makes sense, but it's a bit messy
            // logically.
            if (!shouldClip) {
                for (stroke in style.stroke.strokes) {
                    val brushAndOpacity = stroke.asBrush(document, density)
                    if (brushAndOpacity != null) {
                        val (brush, opacity) = brushAndOpacity
                        drawPath(
                            strokePath,
                            brush = brush,
                            style = Stroke(width = style.stroke.stroke_weight * density),
                            alpha = opacity
                        )
                    }
                }
            }
            if (shouldClip) {
                drawContext.canvas.save()
                drawContext.canvas.clipPath(outlinePath)
            }

            drawContent()

            if (shouldClip) {
                drawContext.canvas.restore()
                for (stroke in style.stroke.strokes) {
                    val brushAndOpacity = stroke.asBrush(document, density)
                    if (brushAndOpacity != null) {
                        val (brush, opacity) = brushAndOpacity
                        drawPath(
                            strokePath,
                            brush = brush,
                            style = Stroke(width = style.stroke.stroke_weight * density),
                            alpha = opacity
                        )
                    }
                }
            }

            // Restore the layer if we have a blend mode.
            if (useBlendMode) {
                drawContext.canvas.restore()
            }
            drawContext.canvas.restore()
        }
    )
