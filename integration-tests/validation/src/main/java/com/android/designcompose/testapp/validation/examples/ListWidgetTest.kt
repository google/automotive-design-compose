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

// TEST List Preview Widget
@DesignDoc(id = "9ev0MBNHFrgTqJOrAGcEpV")
interface ListWidgetTest {
    @DesignComponent(node = "#Main")
    fun MainFrame(
        @DesignContentTypes(nodes = ["#Item"])
        @DesignPreviewContent(
            name = "Items",
            nodes =
                [
                    PreviewNode(4, "#Item=Grid, #Playing=Off"),
                    PreviewNode(1, "#Item=Grid, #Playing=On"),
                    PreviewNode(6, "#Item=Grid, #Playing=Off"),
                ]
        )
        @Design(node = "#row-content")
        rowItems: ListContent,
        @DesignContentTypes(nodes = ["#Item"])
        @DesignPreviewContent(
            name = "Items",
            nodes =
                [
                    PreviewNode(4, "#Item=Grid, #Playing=Off"),
                    PreviewNode(1, "#Item=Grid, #Playing=On"),
                    PreviewNode(6, "#Item=Grid, #Playing=Off"),
                ]
        )
        @Design(node = "#row-content-scrolling")
        rowScrollItems: ListContent,
        @DesignContentTypes(nodes = ["#VItem"])
        @DesignPreviewContent(
            name = "Items",
            nodes =
                [
                    PreviewNode(4, "#VItem=Grid, #Playing=Off"),
                    PreviewNode(1, "#VItem=Grid, #Playing=On"),
                    PreviewNode(6, "#VItem=Grid, #Playing=Off"),
                ]
        )
        @Design(node = "#col-content")
        colItems: ListContent,
        @DesignContentTypes(nodes = ["#VItem"])
        @DesignPreviewContent(
            name = "Items",
            nodes =
                [
                    PreviewNode(4, "#VItem=Grid, #Playing=Off"),
                    PreviewNode(1, "#VItem=Grid, #Playing=On"),
                    PreviewNode(6, "#VItem=Grid, #Playing=Off"),
                ]
        )
        @Design(node = "#col-content-scrolling")
        colScrollItems: ListContent,
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
}

@Composable
fun ListWidgetTest() {
    val rowItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..6) rowItems.add(Pair(GridItemType.RowGrid, "Item $i"))
    val rowScrollItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..30) rowScrollItems.add(Pair(GridItemType.RowGrid, "Item $i"))

    val colItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..6) colItems.add(Pair(GridItemType.ColGrid, "Item $i"))
    val colScrollItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..30) colScrollItems.add(Pair(GridItemType.ColGrid, "Item $i"))

    fun getNodeData(items: ArrayList<Pair<GridItemType, String>>, index: Int): GetDesignNodeData {
        return when (items[index].first) {
            GridItemType.RowGrid -> {
                { ListWidgetTestDoc.ItemDesignNodeData(type = ItemType.Grid) }
            }
            else -> {
                { ListWidgetTestDoc.VItemDesignNodeData(type = ItemType.Grid) }
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
            GridItemType.RowGrid ->
                ListWidgetTestDoc.Item(
                    type = ItemType.Grid,
                    title = items[index].second,
                    parentLayout = parentLayout
                )
            else ->
                ListWidgetTestDoc.VItem(
                    type = ItemType.Grid,
                    title = items[index].second,
                    parentLayout = parentLayout
                )
        }
    }

    ListWidgetTestDoc.MainFrame(
        modifier = Modifier.fillMaxSize(),
        rowItems = { spanFunc ->
            ListContentData(
                count = rowItems.size,
                span = { index ->
                    val nodeData = getNodeData(rowItems, index)
                    spanFunc(nodeData)
                },
            ) { index, parentLayout ->
                itemComposable(rowItems, index, parentLayout)
            }
        },
        rowScrollItems = { spanFunc ->
            ListContentData(
                count = rowScrollItems.size,
                span = { index ->
                    val nodeData = getNodeData(rowScrollItems, index)
                    spanFunc(nodeData)
                },
            ) { index, parentLayout ->
                itemComposable(rowScrollItems, index, parentLayout)
            }
        },
        colItems = { spanFunc ->
            ListContentData(
                count = colItems.size,
                span = { index ->
                    val nodeData = getNodeData(colItems, index)
                    spanFunc(nodeData)
                },
            ) { index, parentLayout ->
                itemComposable(colItems, index, parentLayout)
            }
        },
        colScrollItems = { spanFunc ->
            ListContentData(
                count = colScrollItems.size,
                span = { index ->
                    val nodeData = getNodeData(colScrollItems, index)
                    spanFunc(nodeData)
                },
            ) { index, parentLayout ->
                itemComposable(colScrollItems, index, parentLayout)
            }
        },
    )
}
