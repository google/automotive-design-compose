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
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.designcompose.serdegen.GridLayoutType
import com.android.designcompose.serdegen.GridSpan
import com.android.designcompose.serdegen.ItemSpacing
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewShape
import com.android.designcompose.serdegen.ViewStyle

@Composable
internal fun DesignFrame(
    modifier: Modifier = Modifier,
    view: View,
    style: ViewStyle,
    layoutInfo: SimplifiedLayoutInfo,
    document: DocContent,
    customizations: CustomizationContext,
    parentLayout: ParentLayoutInfo?,
    layoutId: Int,
    parentComponents: List<ParentComponentInfo>,
    maskInfo: MaskInfo?,
    content: @Composable () -> Unit,
): Boolean {
    val name = view.name
    if (!customizations.getVisible(name)) return false

    var m = Modifier as Modifier
    m = m.then(modifier)
    val customModifier = customizations.getModifier(name)
    if (customModifier != null) {
        // We may need more control over where a custom modifier is placed in the list
        // than just always adding at the end.
        m = m.then(customModifier)
    }

    // Check for a customization that replaces this component completely
    // If we're replaced, then invoke the replacement here. We may want to pass more layout info
    // (row/column/etc) to the replacement component at some point.
    val replacementComponent = customizations.getComponent(name)
    if (replacementComponent != null) {
        val myParentLayout = parentLayout?.withBaseView(view)
        replacementComponent(
            object : ComponentReplacementContext {
                override val layoutModifier = layoutInfo.selfModifier
                override val appearanceModifier = m
                @Composable
                override fun Content() {
                    content()
                }
                override val textStyle: TextStyle? = null
                override val parentLayout = myParentLayout
            }
        )
        return true
    }

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

    // Since the meter function is a composable, we need to call it here even though we don't need
    // it until frameRender() since that function is not a composable.
    val meterValue = customizations.getMeterFunction(name)?.let { it() }
    meterValue?.let { customizations.setMeterValue(name, it) }

    // Keep track of the layout state, which changes whenever this view's layout changes
    val (layoutState, setLayoutState) = remember { mutableStateOf(0) }
    // Subscribe for layout changes whenever the view changes. The view can change if it is a
    // component instance that changes to another variant. It can also change due to a live update.
    // Subscribing when already subscribed simply updates the view in the layout system.
    DisposableEffect(view) {
        val parentLayoutId = parentLayout?.parentLayoutId ?: -1
        val childIndex = parentLayout?.childIndex ?: -1
        Log.d(
            TAG,
            "Subscribe Frame ${view.name} layoutId $layoutId parent $parentLayoutId index $childIndex"
        )
        // Subscribe to layout changes when the view changes or is added
        LayoutManager.subscribeFrame(
            layoutId,
            setLayoutState,
            parentLayoutId,
            childIndex,
            view,
            parentLayout?.baseView
        )
        onDispose {}
    }
    DisposableEffect(Unit) {
        onDispose {
            // Unsubscribe to layout changes when the view is removed
            Log.d(TAG, "Unsubscribe ${view.name} layoutId $layoutId")
            LayoutManager.unsubscribe(layoutId)
        }
    }

    val finishLayout =
        @Composable {
            // This must be called at the end of DesignFrame just before returning, after adding all
            // children. This lets the LayoutManager know that this frame has completed, and so if
            // there are no other parent frames performing layout, layout computation can be
            // performed.
            DisposableEffect(view) {
                LayoutManager.finishLayout(layoutId)
                onDispose {}
            }
        }

    // Only render the frame if we don't have a replacement node
    val shape = (view.data as ViewData.Container).shape
    if (replacementComponent == null)
        m = m.frameRender(style, shape, customImage, document, name, customizations, maskInfo)

    val lazyContent = customizations.getListContent(name)

    // Select the appropriate representation for ourselves based on our layout style;
    // row or column (with or without wrapping/flow), or absolute positioning (similar to the CSS2
    // model).
    val layout = LayoutManager.getLayoutWithDensity(layoutId)
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
                val rowSizeModifier = Modifier.layoutSizeToModifier(layout)
                Row(
                    rowSizeModifier
                        .then(layoutInfo.selfModifier)
                        .then(m)
                        .then(layoutInfo.marginModifier),
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
                    layoutInfo.selfModifier.then(m).then(layoutInfo.marginModifier),
                    horizontalArrangement = layoutInfo.arrangement,
                    verticalAlignment = layoutInfo.alignment
                ) {
                    content()
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
                val columnSizeModifier = Modifier.layoutSizeToModifier(layout)
                Column(
                    columnSizeModifier
                        .then(layoutInfo.selfModifier)
                        .then(m)
                        .then(layoutInfo.marginModifier),
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
                    layoutInfo.selfModifier.then(m).then(layoutInfo.marginModifier),
                    verticalArrangement = layoutInfo.arrangement,
                    horizontalAlignment = layoutInfo.alignment
                ) {
                    content()
                }
            }
        }
        is LayoutInfoGrid -> {
            if (lazyContent == null) {
                finishLayout()
                return false
            }

            // Given the list of possible content that goes into this grid layout, try to find a
            // matching
            // item based on node name and variant properties, and return its span
            fun getSpan(
                gridSpanContent: List<GridSpan>,
                getDesignNodeData: GetDesignNodeData,
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
                val lContent = lazyContent { nodeData ->
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
                spacing: Int,
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

            val gridSizeModifier = Modifier.layoutSizeToModifier(layout)

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
                    modifier = gridSizeModifier.then(layoutInfo.selfModifier).then(m),
                    columns =
                        object : GridCells {
                            override fun Density.calculateCrossAxisCellSizes(
                                availableSize: Int,
                                spacing: Int,
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
                                outPositions: IntArray,
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
                            layoutInfo.padding.start.pointsAsDp(density),
                            layoutInfo.padding.top.pointsAsDp(density),
                            layoutInfo.padding.end.pointsAsDp(density),
                            layoutInfo.padding.bottom.pointsAsDp(density),
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
                    modifier = layoutInfo.selfModifier.then(gridSizeModifier).then(m),
                    rows =
                        object : GridCells {
                            override fun Density.calculateCrossAxisCellSizes(
                                availableSize: Int,
                                spacing: Int,
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
                                outPositions: IntArray,
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
            if (parentLayout?.isWidgetChild == true) {
                // For direct children of a widget, render the frame as a box with the calculated
                // layout size, then compose the frame's children with our custom layout
                Box(m.layoutSizeToModifier(layout)) {
                    DesignFrameLayout(modifier, name, layoutId, layoutState) { content() }
                }
            } else {
                // Otherwise, use our custom layout to render the frame and to place its children
                m = m.then(Modifier.layoutStyle(name, layoutId))
                DesignFrameLayout(m, name, layoutId, layoutState) {
                    Box(Modifier) // Need this for some reason
                    content()
                }
            }
        }
    }

    finishLayout()
    return true
}

internal fun Modifier.frameRender(
    style: ViewStyle,
    frameShape: ViewShape,
    customImageWithContext: Bitmap?,
    document: DocContent,
    name: String,
    customizations: CustomizationContext,
    maskInfo: MaskInfo?,
): Modifier =
    this.then(
        Modifier.drawWithContent {
            fun render() =
                render(
                    this@frameRender,
                    style,
                    frameShape,
                    customImageWithContext,
                    document,
                    name,
                    customizations,
                )

            when (maskInfo?.type ?: MaskViewType.None) {
                MaskViewType.MaskNode -> {
                    // When rendering a mask, call saveLayer with blendmode DSTIN so that we blend
                    // the masks's alpha with what has already been rendered. We also need to adjust
                    // the rectangle to be the size and position of the parent because masks affect
                    // the whole area of the parent
                    val paint = Paint()
                    paint.blendMode = BlendMode.DstIn
                    val offset =
                        Offset(
                            -style.margin.start.pointsAsDp(density).value,
                            -style.margin.top.pointsAsDp(density).value
                        )
                    val parentSize = maskInfo?.parentSize?.value ?: size
                    drawContext.canvas.withSaveLayer(Rect(offset, parentSize), paint) { render() }
                }
                MaskViewType.MaskParent -> {
                    // When rendering a node that has a child mask, call saveLayer so that its
                    // children are all rendered to a separate target. Save the size of this
                    // drawing context so that the child mask can adjust its drawing size to mask
                    // the entire parent
                    maskInfo?.parentSize?.value = size
                    drawContext.canvas.withSaveLayer(size.toRect(), Paint()) { render() }
                }
                else -> {
                    render()
                }
            }
        }
    )
