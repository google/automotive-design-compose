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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
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
import com.android.designcompose.serdegen.ViewShape
import com.android.designcompose.serdegen.ViewStyle
import java.util.Optional
import kotlin.math.roundToInt

@Composable
internal fun DesignFrame(
    modifier: Modifier = Modifier,
    style: ViewStyle,
    shape: ViewShape,
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
            .frameRender(style, shape, customImage, document, name, customizations)
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
    shape: ViewShape,
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

            // Fill then stroke.
            val (fills, precomputedStrokes) =
                when (shape) {
                    is ViewShape.RoundRect -> {
                        val path = Path()
                        path.addRoundRect(
                            RoundRect(
                                0.0f,
                                0.0f,
                                drawContext.size.width,
                                drawContext.size.height,
                                CornerRadius(
                                    shape.corner_radius[0] * density,
                                    shape.corner_radius[0] * density
                                ),
                                CornerRadius(
                                    shape.corner_radius[1] * density,
                                    shape.corner_radius[1] * density
                                ),
                                CornerRadius(
                                    shape.corner_radius[2] * density,
                                    shape.corner_radius[2] * density
                                ),
                                CornerRadius(
                                    shape.corner_radius[3] * density,
                                    shape.corner_radius[3] * density
                                )
                            )
                        )
                        Pair(listOf(path), listOf<Path>())
                    }
                    is ViewShape.Path -> {
                        Pair(
                            shape.path.map { path -> path.asPath(density) },
                            shape.stroke.map { path -> path.asPath(density) }
                        )
                    }
                    else -> {
                        val path = Path()
                        path.addRect(
                            Rect(0.0f, 0.0f, drawContext.size.width, drawContext.size.height)
                        )
                        Pair(listOf(path), listOf<Path>())
                    }
                }
            // XXX: drawContext.size is wrong; we need the node size.
            val fillBrush =
                style.background.mapNotNull { background ->
                    val p = Paint()
                    val b = background.asBrush(document, density)
                    if (b != null) {
                        val (brush, opacity) = b
                        brush.applyTo(drawContext.size, p, opacity)
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
                        brush.applyTo(drawContext.size, p, opacity)
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
                                strokePath.op(
                                    fill.asAndroidPath(),
                                    android.graphics.Path.Op.DIFFERENCE
                                )
                            is StrokeAlign.Inside ->
                                strokePath.op(
                                    fill.asAndroidPath(),
                                    android.graphics.Path.Op.INTERSECT
                                )
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
                shadowBoundsPaint.strokeWidth =
                    shadowStrokeWidth + shadow.spread_radius * 2.0f * density
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
                    drawContext.canvas.nativeCanvas.drawPath(
                        shadowPath.asAndroidPath(),
                        shadowPaint
                    )
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
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
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
                if (!needSpreadStroke)
                    shadowOutlinePaint.strokeWidth = shadowStrokeWidth - spreadWidth
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
                    drawContext.canvas.nativeCanvas.drawPath(
                        shadowPath.asAndroidPath(),
                        shadowPaint
                    )
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
    )
