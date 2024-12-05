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
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DesignListLayout
import com.android.designcompose.DesignParentLayout
import com.android.designcompose.LayoutInfoColumn
import com.android.designcompose.LayoutInfoGrid
import com.android.designcompose.LayoutInfoRow
import com.android.designcompose.LayoutManager
import com.android.designcompose.LazyContentSpan
import com.android.designcompose.ListContent
import com.android.designcompose.ListLayoutType
import com.android.designcompose.SimplifiedLayoutInfo
import com.android.designcompose.calcLayoutInfo
import com.android.designcompose.getCustomComposable
import com.android.designcompose.layoutSizeToModifier
import com.android.designcompose.proto.getDim
import com.android.designcompose.proto.layoutStyle
import com.android.designcompose.proto.nodeStyle
import com.android.designcompose.rootParentLayoutInfo
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewStyle

internal fun addListWidget(listWidgetContent: ListContent, resolvedView: SquooshResolvedNode, style: ViewStyle, customizations: CustomizationContext, layoutIdAllocator: SquooshLayoutIdAllocator, parentComps: ParentComponentData?, composableList: ArrayList<SquooshChildComposable>) {

    val layoutInfo = calcLayoutInfo(Modifier, resolvedView.view, resolvedView.style)
    when (layoutInfo) {
        is LayoutInfoRow -> {
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
        is LayoutInfoColumn -> {
            // TODO
        }
        is LayoutInfoGrid -> {
            // TODO
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
@Composable
internal fun ListWidget(listData: SquooshChildComposable, customizations: CustomizationContext) { //} lazyContent: ListContent, style: ViewStyle, layoutInfo: SimplifiedLayoutInfo) {
    val lazyContent = listData.listWidgetContent!!.listContent
    val style = listData.node.style
    val layoutInfo = listData.listWidgetContent.layoutInfo!!

    val content = lazyContent { LazyContentSpan() }

    when (layoutInfo) {
        is LayoutInfoRow -> {
            val hugContents = style.layoutStyle.width.getDim() is Dimension.Auto
            val rowModifier = layoutInfo.marginModifier.then(
                if (hugContents)
                    Modifier.onSizeChanged {
                        println("### onSizeChanged ${listData.node.view.name}: w ${it.width} h ${it.height}, ${listData.node.computedLayout?.width}, ${listData.node.computedLayout?.height}")
                        /*
                        LayoutManager.setNodeSize(
                            layoutId,
                            rootLayoutId,
                            it.width,
                            it.height,
                        )
                        */
                    }
                else {
                    println("### No Hug ${listData.node.view.name}")
                    Modifier.layoutSizeToModifier(listData.node.computedLayout)
                }
            )
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
            println("### Row ${listData.node.view.name} H ${layoutInfo.arrangement} V ${layoutInfo.alignment}")
            Row(
                modifier = rowModifier,
                horizontalArrangement = layoutInfo.arrangement,
                verticalAlignment = layoutInfo.alignment,
            ) {
                for (i in 0 until count) {
                    if (overflowNodeId != null && i == count - 1) {
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
                        content.itemContent(i)
                    }
                }
            }
        }
        else -> {
            println("### UNKNOWN LAYOUT")
        }
    }
}
        /*
    Row(
        rowModifier
            .then(layoutInfo.selfModifier)
            .then(m)
            .then(layoutInfo.marginModifier),
        horizontalArrangement = layoutInfo.arrangement,
        verticalAlignment = layoutInfo.alignment,
            if (overflowNodeId != null && i == count - 1) {
                // This is the last item we can show and there are more, and there
                // is an
                // overflow node, so show the overflow node here
                val customComposable = customizations.getCustomComposable()
                if (customComposable != null) {
                    customComposable(
                        Modifier,
                        style.node_style.overflow_node_name.get(),
                        NodeQuery.NodeId(style.node_style.overflow_node_id.get()),
                        parentComponents,
                        null,
                    )
                }
            } else {
                DesignListLayout(ListLayoutType.Row) { content.itemContent(i) }
            }
        }
        */
/*
internal fun addListWidget(lazyContent: ListContent, view: View, composableList: ArrayList<SquooshChildComposable>) {
    val style = view.style
    val content = lazyContent { LazyContentSpan() }
    var count = content.count
    var overflowNodeId: String? = null
    if (
        style.node_style.max_children.isPresent &&
        style.node_style.max_children.get() < count
    ) {
        count = style.node_style.max_children.get()
        if (style.node_style.overflow_node_id.isPresent)
            overflowNodeId = style.node_style.overflow_node_id.get()
    }
    // If the widget is set to hug contents, don't give Row() a size and let it size
    // itself. Then when the size is determined, inform the layout manager.
    // Otherwise,
    // get the fixed size from the layout manager and use it in a Modifier.
    val hugContents = style.layout_style.width.getDim() is Dimension.Auto
    val rowModifier =
        if (hugContents)
            Modifier.onSizeChanged {
                LayoutManager.setNodeSize(
                    layoutId,
                    rootLayoutId,
                    it.width,
                    it.height,
                )
            }
        else Modifier.layoutSizeToModifier(layout)

    composableList.add(
        SquooshChildComposable(

            component = replacementComponent,
            node = resolvedView,
            parentComponents = parentComps,
        )

    var previousReplacementChild: SquooshResolvedNode? = null
    for (idx in 0..<content.count) {
        val childComponent = @Composable { content.itemContent(idx) }
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

    Row(
        rowModifier
            .then(layoutInfo.selfModifier)
            .then(m)
            .then(layoutInfo.marginModifier),
        horizontalArrangement = layoutInfo.arrangement,
        verticalAlignment = layoutInfo.alignment,
    ) {
        for (i in 0 until count) {
            if (overflowNodeId != null && i == count - 1) {
                // This is the last item we can show and there are more, and there
                // is an
                // overflow node, so show the overflow node here
                val customComposable = customizations.getCustomComposable()
                if (customComposable != null) {
                    customComposable(
                        Modifier,
                        style.node_style.overflow_node_name.get(),
                        NodeQuery.NodeId(style.node_style.overflow_node_id.get()),
                        parentComponents,
                        null,
                    )
                }
            } else {
                DesignListLayout(ListLayoutType.Row) { content.itemContent(i) }
            }
        }
    }
}
    */
