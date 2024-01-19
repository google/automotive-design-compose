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

package com.android.designcompose.testapp.validation.examples

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.designcompose.GetDesignNodeData
import com.android.designcompose.ListContent
import com.android.designcompose.ListContentData
import com.android.designcompose.ParentLayoutInfo
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignContentTypes
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignPreviewContent
import com.android.designcompose.annotation.DesignVariant
import com.android.designcompose.annotation.PreviewNode

// TEST Grid Layout
@DesignDoc(id = "JOSOEvsrjvMqanyQa5OpNR")
interface GridLayoutTest {
    @DesignComponent(node = "#MainFrame")
    fun MainFrame(
        @DesignContentTypes(nodes = ["#SectionTitle", "#Item"])
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(6, "#Item=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(4, "#Item=Grid")
                ]
        )
        @Design(node = "#VerticalGrid1")
        vertical1: ListContent,
        @DesignContentTypes(nodes = ["#SectionTitle", "#VItem"])
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(6, "#VItem=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(4, "#VItem=Grid")
                ]
        )
        @Design(node = "#HorizontalGrid1")
        horizontal1: ListContent,
        @DesignContentTypes(nodes = ["#SectionTitle", "#Item"])
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(6, "#Item=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(8, "#Item=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(6, "#Item=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(10, "#Item=List")
                ]
        )
        @Design(node = "#VerticalGrid2")
        vertical2: ListContent,
        @DesignContentTypes(nodes = ["#SectionTitle", "#VItem"])
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(6, "#VItem=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(6, "#VItem=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(6, "#VItem=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(11, "#VItem=List")
                ]
        )
        @Design(node = "#HorizontalGrid2")
        horizontal2: ListContent,
    )

    @DesignComponent(node = "#Item")
    fun Item(
        @DesignVariant(property = "#Item") type: ItemType,
        @Design(node = "#Title") title: String,
    )

    @DesignComponent(node = "#VItem")
    fun VItem(
        @DesignVariant(property = "#VItem") type: ItemType,
        @Design(node = "#Title") title: String,
    )

    @DesignComponent(node = "#SectionTitle")
    fun SectionTitle(@Design(node = "#Title") title: String)

    @DesignComponent(node = "#VSectionTitle")
    fun VSectionTitle(@Design(node = "#Title") title: String)
}

@Preview()
@Composable
fun GridLayoutTest() {
    val vertItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..20) vertItems.add(Pair(GridItemType.RowGrid, "Item $i"))
    for (i in 21..40) vertItems.add(Pair(GridItemType.RowList, "Row Item $i"))
    vertItems.add(0, Pair(GridItemType.SectionTitle, "Group One"))
    vertItems.add(7, Pair(GridItemType.SectionTitle, "Group Two"))
    vertItems.add(12, Pair(GridItemType.SectionTitle, "Group Three"))
    vertItems.add(20, Pair(GridItemType.SectionTitle, "Group Four"))

    val horizItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..20) horizItems.add(Pair(GridItemType.ColGrid, "Item $i"))
    for (i in 21..40) horizItems.add(Pair(GridItemType.ColList, "Row Item $i"))
    horizItems.add(0, Pair(GridItemType.SectionTitle, "Group One"))
    horizItems.add(7, Pair(GridItemType.SectionTitle, "Group Two"))
    horizItems.add(14, Pair(GridItemType.SectionTitle, "Group Three"))
    horizItems.add(20, Pair(GridItemType.SectionTitle, "Group Four"))

    fun getNodeData(items: ArrayList<Pair<GridItemType, String>>, index: Int): GetDesignNodeData {
        return when (items[index].first) {
            GridItemType.SectionTitle -> {
                { GridLayoutTestDoc.SectionTitleDesignNodeData() }
            }
            GridItemType.VSectionTitle -> {
                { GridLayoutTestDoc.VSectionTitleDesignNodeData() }
            }
            GridItemType.RowGrid -> {
                { GridLayoutTestDoc.ItemDesignNodeData(type = ItemType.Grid) }
            }
            GridItemType.RowList -> {
                { GridLayoutTestDoc.ItemDesignNodeData(type = ItemType.List) }
            }
            GridItemType.ColGrid -> {
                { GridLayoutTestDoc.VItemDesignNodeData(type = ItemType.Grid) }
            }
            GridItemType.ColList -> {
                { GridLayoutTestDoc.VItemDesignNodeData(type = ItemType.List) }
            }
        }
    }

    @Composable
    fun itemComposable(
        items: ArrayList<Pair<GridItemType, String>>,
        index: Int,
        parentLayout: ParentLayoutInfo
    ) {
        when (items[index].first) {
            GridItemType.SectionTitle -> GridLayoutTestDoc.SectionTitle(title = items[index].second)
            GridItemType.VSectionTitle ->
                GridLayoutTestDoc.VSectionTitle(
                    title = items[index].second,
                    parentLayout = parentLayout
                )
            GridItemType.RowGrid ->
                GridLayoutTestDoc.Item(
                    type = ItemType.Grid,
                    title = items[index].second,
                    parentLayout = parentLayout
                )
            GridItemType.RowList ->
                GridLayoutTestDoc.Item(
                    type = ItemType.List,
                    title = items[index].second,
                    parentLayout = parentLayout
                )
            GridItemType.ColGrid ->
                GridLayoutTestDoc.VItem(
                    type = ItemType.Grid,
                    title = items[index].second,
                    parentLayout = parentLayout
                )
            GridItemType.ColList ->
                GridLayoutTestDoc.VItem(
                    type = ItemType.List,
                    title = items[index].second,
                    parentLayout = parentLayout
                )
        }
    }

    GridLayoutTestDoc.MainFrame(
        modifier = Modifier.fillMaxSize(),
        vertical1 = { spanFunc ->
            ListContentData(
                count = vertItems.size,
                span = { index ->
                    val nodeData = getNodeData(vertItems, index)
                    spanFunc(nodeData)
                },
            ) { index, parentLayout ->
                itemComposable(vertItems, index, parentLayout)
            }
        },
        vertical2 = { spanFunc ->
            ListContentData(
                count = vertItems.size,
                span = { index ->
                    val nodeData = getNodeData(vertItems, index)
                    spanFunc(nodeData)
                },
            ) { index, parentLayout ->
                itemComposable(vertItems, index, parentLayout)
            }
        },
        horizontal1 = { spanFunc ->
            ListContentData(
                count = horizItems.size,
                span = { index ->
                    val nodeData = getNodeData(horizItems, index)
                    spanFunc(nodeData)
                },
            ) { index, parentLayout ->
                itemComposable(horizItems, index, parentLayout)
            }
        },
        horizontal2 = { spanFunc ->
            ListContentData(
                count = horizItems.size,
                span = { index ->
                    val nodeData = getNodeData(horizItems, index)
                    spanFunc(nodeData)
                },
            ) { index, parentLayout ->
                itemComposable(horizItems, index, parentLayout)
            }
        },
    )
}
