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

package com.android.designcompose.squoosh

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.designcompose.CustomizationContext
import com.android.designcompose.LayoutInfoColumn
import com.android.designcompose.LayoutInfoGrid
import com.android.designcompose.LayoutInfoRow
import com.android.designcompose.LazyContentSpan
import com.android.designcompose.ListContent
import com.android.designcompose.calcLayoutInfo
import com.android.designcompose.getCustomComposable
import com.android.designcompose.proto.nodeStyle
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.serdegen.ViewStyle

internal fun addRowColumnContent(
    listWidgetContent: ListContent,
    resolvedView: SquooshResolvedNode,
    style: ViewStyle, customizations: CustomizationContext,
    layoutIdAllocator: SquooshLayoutIdAllocator,
    parentComps: ParentComponentData?,
    composableList: ArrayList<SquooshChildComposable>
) {
    val content = listWidgetContent { LazyContentSpan() }
    var count = content.count

    var overflowNodeId: String? = null
    if (
        style.nodeStyle.max_children.isPresent &&
        style.nodeStyle.max_children.get() < count
    ) {
        count = style.nodeStyle.max_children.get()
        if (style.nodeStyle.overflow_node_id.isPresent)
            overflowNodeId = style.nodeStyle.overflow_node_id.get()
    }

    var previousReplacementChild: SquooshResolvedNode? = null
    for (idx in 0..<count) {
        val childComponent = @Composable {
            if (overflowNodeId != null && idx == count - 1) {
                // This is the last item we can show and there are more, and there
                // is an
                // overflow node, so show the overflow node here
                val customComposable = customizations.getCustomComposable()
                if (customComposable != null) {
                    customComposable(
                        Modifier,
                        style.nodeStyle.overflow_node_name.get(),
                        NodeQuery.NodeId(style.nodeStyle.overflow_node_id.get()),
                        listOf(), //parentComponents,
                        null,
                    )
                }
            } else {
                content.itemContent(idx)
            }
        }
        val replacementChild =
            generateReplacementListChildNode(resolvedView, idx, layoutIdAllocator)
        if (previousReplacementChild != null)
            previousReplacementChild.nextSibling = replacementChild
        else resolvedView.firstChild = replacementChild
        previousReplacementChild = replacementChild

        composableList.add(
            SquooshChildComposable(
                component = @Composable { childComponent() },
                node = replacementChild,
                parentComponents = parentComps,
            )
        )
    }
}


internal fun addGridContent(
    layoutInfo: LayoutInfoGrid,
    listWidgetContent: ListContent,
    resolvedView: SquooshResolvedNode,
    style: ViewStyle,
    customizations: CustomizationContext,
    layoutIdAllocator: SquooshLayoutIdAllocator,
    parentComps: ParentComponentData?,
    composableList: ArrayList<SquooshChildComposable>)
{
    val layoutInfo = calcLayoutInfo(Modifier, resolvedView.view, resolvedView.style)
    val content = listWidgetContent { LazyContentSpan() }

    val lazyItemContent: LazyGridScope.() -> Unit = {
        val lContent = listWidgetContent { nodeData ->
            LazyContentSpan(span = 1)
        }

        var count = lContent.count
        items(
            count,
            key = { index ->
                lContent.key?.invoke(index) ?: index
            },
            span = { index ->
                val span = lContent.span?.invoke(index) ?: LazyContentSpan()
                GridItemSpan(if (span.maxLineSpan) maxLineSpan else span.span)
            },
            contentType = { index ->
                lContent.contentType.invoke(index)
            },
            itemContent = { index ->
                lContent.itemContent(index)
            }
        )
    }
    val gridComposable = @Composable {
        LazyVerticalGrid(
            modifier = Modifier, //gridSizeModifier.then(layoutInfo.selfModifier).then(m),
            columns = GridCells.Fixed(5),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 16.dp,
                end = 12.dp,
                bottom = 16.dp
            ),
            userScrollEnabled = true,
        ) {

        }
    }
    /*
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
                                        maxLineSpan = item.max_span,
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

    val (gridMainAxisSize, setGridMainAxisSize) = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            0
        )
    }

    // Content for the lazy content parameter. This uses the grid layout but also supports
    // limiting the number of children to style.max_children, and using an overflow node if
    // one is specified.
    val lazyItemContent: LazyGridScope.() -> Unit = {
        val lContent = lazyContent { nodeData ->
            getSpan(layoutInfo.gridSpanContent, nodeData)
        }

        // If the main axis size has not yet been set, and spacing is set to auto, show the
        // initial content composable. This avoids rendering the content in one position
        // for the first frame and then in another on the second frame after the main axis
        // size has been set.
        val showInitContent =
            (gridMainAxisSize <= 0 &&
                layoutInfo.mainAxisSpacing.type() is ItemSpacingType.Auto)
        if (showInitContent)
            items(
                count = 1,
                span = {
                    val span = lContent.initialSpan?.invoke() ?: LazyContentSpan()
                    GridItemSpan(if (span.maxLineSpan) maxLineSpan else span.span)
                },
            ) {
                DesignListLayout(ListLayoutType.Grid) { lContent.initialContent() }
            }
        else {
            var count = lContent.count
            var overflowNodeId: String? = null
            if (
                style.nodeStyle.max_children.isPresent &&
                style.nodeStyle.max_children.get() < count
            ) {
                count = style.nodeStyle.max_children.get()
                if (style.nodeStyle.overflow_node_id.isPresent)
                    overflowNodeId = style.nodeStyle.overflow_node_id.get()
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
                                style.nodeStyle.overflow_node_name.get(),
                                NodeQuery.NodeId(style.nodeStyle.overflow_node_id.get()),
                                parentComponents,
                                null,
                            )
                        }
                    } else {
                        DesignListLayout(ListLayoutType.Grid) {
                            lContent.itemContent(index)
                        }
                    }
                },
            )
        }
    }

    // Given the frame size, number of columns/rows, and spacing between them, return a list
    // of column/row widths/heights
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
            (layoutInfo.mainAxisSpacing.type() as? ItemSpacingType.Fixed)?.value ?: 0
        val verticalSpacing = layoutInfo.crossAxisSpacing

        DesignParentLayout(rootParentLayoutInfo) {
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
                            spacing,
                        )
                    }
                },
                horizontalArrangement =
                Arrangement.spacedBy(
                    (when (val spacing = layoutInfo.mainAxisSpacing.type()) {
                        is ItemSpacingType.Fixed -> spacing.value
                        is ItemSpacingType.Auto -> {
                            if (columnCount > 1)
                                (gridMainAxisSize -
                                    (spacing.value.height * columnCount)) /
                                    (columnCount - 1)
                            else spacing.value.width
                        }
                        else -> horizontalSpacing
                    })
                        .dp
                ),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing.dp),
                userScrollEnabled = layoutInfo.scrollingEnabled,
                contentPadding =
                PaddingValues(
                    layoutInfo.padding.start.getDim().pointsAsDp(density),
                    layoutInfo.padding.top.getDim().pointsAsDp(density),
                    layoutInfo.padding.end.getDim().pointsAsDp(density),
                    layoutInfo.padding.bottom.getDim().pointsAsDp(density),
                ),
            ) {
                lazyItemContent()
            }
        }
    } else {
        val rowCount = calculateColumnRowCount(layoutInfo, gridMainAxisSize)
        val horizontalSpacing = layoutInfo.crossAxisSpacing
        val verticalSpacing =
            (layoutInfo.mainAxisSpacing.type() as? ItemSpacingType.Fixed)?.value ?: 0

        DesignParentLayout(rootParentLayoutInfo) {
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
                            spacing,
                        )
                    }
                },
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing.dp),
                verticalArrangement =
                Arrangement.spacedBy(
                    (when (val spacing = layoutInfo.mainAxisSpacing.type()) {
                        is ItemSpacingType.Fixed -> spacing.value
                        is ItemSpacingType.Auto -> {
                            if (rowCount > 1)
                                (gridMainAxisSize -
                                    (spacing.value.height * rowCount)) /
                                    (rowCount - 1)
                            else spacing.value.width
                        }
                        else -> verticalSpacing
                    })
                        .dp
                ),
                userScrollEnabled = layoutInfo.scrollingEnabled,
            ) {
                lazyItemContent()
            }
        }
    }
     */
}

internal fun addListWidget(
    listWidgetContent: ListContent,
    resolvedView: SquooshResolvedNode,
    style: ViewStyle,
    customizations: CustomizationContext,
    layoutIdAllocator: SquooshLayoutIdAllocator,
    parentComps: ParentComponentData?,
    composableList: ArrayList<SquooshChildComposable>)
{
    val layoutInfo = calcLayoutInfo(Modifier, resolvedView.view, resolvedView.style)
    when (layoutInfo) {
        is LayoutInfoRow -> {
            addRowColumnContent(
                listWidgetContent,
                resolvedView,
                style,
                customizations,
                layoutIdAllocator,
                parentComps,
                composableList
            )
        }
        is LayoutInfoColumn -> {
            addRowColumnContent(
                listWidgetContent,
                resolvedView,
                style,
                customizations,
                layoutIdAllocator,
                parentComps,
                composableList
            )
        }
        is LayoutInfoGrid -> {
            addGridContent(
                layoutInfo,
                listWidgetContent,
                resolvedView,
                style,
                customizations,
                layoutIdAllocator,
                parentComps,
                composableList
            )
        }
        else -> {
            Log.e(TAG, "Invalid layout for node ${resolvedView.view.name}")
        }

        /*
            composableList.add(
                SquooshChildComposable(
                    listWidgetContent = ListWidgetContent(listWidgetContent, layoutInfo, view),
                    node = resolvedView,
                    parentComponents = parentComps,
                )
            )
            resolvedView.needsChildRender = true
        */
    }
}
