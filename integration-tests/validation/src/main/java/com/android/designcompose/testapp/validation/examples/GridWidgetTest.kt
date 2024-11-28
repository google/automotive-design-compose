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

import android.util.Log
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.DesignScrollCallbacks
import com.android.designcompose.GetDesignNodeData
import com.android.designcompose.LazyContentSpan
import com.android.designcompose.ListContent
import com.android.designcompose.ListContentData
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignContentTypes
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignPreviewContent
import com.android.designcompose.annotation.DesignVariant
import com.android.designcompose.annotation.PreviewNode
import com.android.designcompose.testapp.validation.TestButton
import kotlinx.coroutines.launch

// TEST Grid Preview Widget
@DesignDoc(id = "OBhNItd9i9J2LwVYuLxEIx")
interface GridWidgetTest {
    @DesignComponent(node = "#Main")
    fun MainFrame(
        @DesignContentTypes(nodes = ["#SectionTitle", "#Item", "#LoadingPage", "#ErrorPage"])
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(2, "#Item=Grid, #Playing=Off"),
                    PreviewNode(1, "#Item=Grid, #Playing=On"),
                    PreviewNode(6, "#Item=Grid, #Playing=Off"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(1, "#Item=List, #Playing=On"),
                    PreviewNode(3, "#Item=List, #Playing=Off"),
                ],
        )
        @DesignPreviewContent(
            name = "Album",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(1, "#Item=List, #Playing=On"),
                    PreviewNode(16, "#Item=List, #Playing=Off"),
                ],
        )
        @DesignPreviewContent(name = "Loading", nodes = [PreviewNode(1, "#LoadingPage")])
        @DesignPreviewContent(name = "Error", nodes = [PreviewNode(1, "#ErrorPage")])
        @Design(node = "#column-auto-content")
        columns: ListContent,
        @DesignContentTypes(nodes = ["#SectionTitle", "#Item", "#LoadingPage", "#ErrorPage"])
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(2, "#Item=Grid, #Playing=Off"),
                    PreviewNode(1, "#Item=Grid, #Playing=On"),
                    PreviewNode(6, "#Item=Grid, #Playing=Off"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(1, "#Item=List, #Playing=On"),
                    PreviewNode(3, "#Item=List, #Playing=Off"),
                ],
        )
        @DesignPreviewContent(
            name = "Album",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(1, "#Item=List, #Playing=On"),
                    PreviewNode(16, "#Item=List, #Playing=Off"),
                ],
        )
        @DesignPreviewContent(name = "Loading", nodes = [PreviewNode(1, "#LoadingPage")])
        @DesignPreviewContent(name = "Error", nodes = [PreviewNode(1, "#ErrorPage")])
        @Design(node = "#column-scroll-auto-content")
        columnsScroll: ListContent,
        @Design(node = "#column-scroll-auto-content")
        verticalScrollCallbacks: DesignScrollCallbacks,
        @DesignContentTypes(nodes = ["#VSectionTitle", "#VItem", "#LoadingPage", "#ErrorPage"])
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#VSectionTitle"),
                    PreviewNode(2, "#VItem=Grid, #Playing=Off"),
                    PreviewNode(1, "#VItem=Grid, #Playing=On"),
                    PreviewNode(4, "#VItem=Grid, #Playing=Off"),
                    PreviewNode(1, "#VSectionTitle"),
                    PreviewNode(1, "#VItem=List, #Playing=On"),
                    PreviewNode(3, "#VItem=List, #Playing=Off"),
                ],
        )
        @DesignPreviewContent(name = "Loading", nodes = [PreviewNode(1, "#LoadingPage")])
        @DesignPreviewContent(name = "Error", nodes = [PreviewNode(1, "#ErrorPage")])
        @Design(node = "#row-auto-content")
        rows: ListContent,
        @DesignContentTypes(nodes = ["#VSectionTitle", "#VItem", "#LoadingPage", "#ErrorPage"])
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#VSectionTitle"),
                    PreviewNode(2, "#VItem=Grid, #Playing=Off"),
                    PreviewNode(1, "#VItem=Grid, #Playing=On"),
                    PreviewNode(4, "#VItem=Grid, #Playing=Off"),
                    PreviewNode(1, "#VSectionTitle"),
                    PreviewNode(1, "#VItem=List, #Playing=On"),
                    PreviewNode(3, "#VItem=List, #Playing=Off"),
                ],
        )
        @DesignPreviewContent(name = "Loading", nodes = [PreviewNode(1, "#LoadingPage")])
        @DesignPreviewContent(name = "Error", nodes = [PreviewNode(1, "#ErrorPage")])
        @Design(node = "#row-scroll-auto-content")
        rowsScroll: ListContent,
        @Design(node = "#row-scroll-auto-content") horizontalScrollCallbacks: DesignScrollCallbacks,
        @DesignContentTypes(nodes = ["#Item"])
        @DesignPreviewContent(name = "List", nodes = [PreviewNode(10, "#Item=Grid, #Playing=Off")])
        @Design(node = "#list-auto-content")
        items: ListContent,
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

@Composable
fun GridWidgetTest() {
    val verticalScrollableState = remember { mutableStateOf<LazyGridState?>(null) }
    val horizontalScrollableState = remember { mutableStateOf<LazyGridState?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(
        verticalScrollableState.value?.firstVisibleItemIndex,
        verticalScrollableState.value?.firstVisibleItemScrollOffset,
    ) {
        Log.i(
            "DesignCompose",
            "Vertical scroll state changed: offset ${verticalScrollableState.value?.firstVisibleItemScrollOffset}, index ${verticalScrollableState.value?.firstVisibleItemIndex}",
        )
    }
    LaunchedEffect(
        horizontalScrollableState.value?.firstVisibleItemIndex,
        horizontalScrollableState.value?.firstVisibleItemScrollOffset,
    ) {
        Log.i(
            "DesignCompose",
            "Horizontal scroll state changed: offset ${horizontalScrollableState.value?.firstVisibleItemScrollOffset}, index ${horizontalScrollableState.value?.firstVisibleItemIndex}",
        )
    }

    GridWidgetTestContent(verticalScrollableState, horizontalScrollableState)

    Column(Modifier.offset(860.dp, 50.dp)) {
        Text("Manual Scroll", fontSize = 30.sp, color = Color.Black)
        TestButton("Up", "ScrollUp", true) {
            verticalScrollableState.value?.let { scope.launch { it.scrollBy(-10F) } }
        }
        TestButton("Down", "ScrollDown", true) {
            verticalScrollableState.value?.let { scope.launch { it.scrollBy(10F) } }
        }
        TestButton("Left", "ScrollLeft", true) {
            horizontalScrollableState.value?.let { scope.launch { it.scrollBy(-10F) } }
        }
        TestButton("Right", "ScrollRight", true) {
            horizontalScrollableState.value?.let { scope.launch { it.scrollBy(10F) } }
        }
    }
}

@Composable
fun GridWidgetTestContent(
    verticalScrollableState: MutableState<LazyGridState?>,
    horizontalScrollableState: MutableState<LazyGridState?>,
) {
    val vertItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..20) vertItems.add(Pair(GridItemType.RowGrid, "Item $i"))
    for (i in 21..40) vertItems.add(Pair(GridItemType.RowList, "Row Item $i"))
    vertItems.add(0, Pair(GridItemType.SectionTitle, "Group One"))
    vertItems.add(15, Pair(GridItemType.SectionTitle, "Group Two"))
    vertItems.add(20, Pair(GridItemType.SectionTitle, "Group Three"))

    val horizItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..20) horizItems.add(Pair(GridItemType.ColGrid, "Item $i"))
    for (i in 21..40) horizItems.add(Pair(GridItemType.ColList, "Row Item $i"))
    horizItems.add(0, Pair(GridItemType.VSectionTitle, "Group One"))
    horizItems.add(7, Pair(GridItemType.VSectionTitle, "Group Two"))
    horizItems.add(14, Pair(GridItemType.VSectionTitle, "Group Three"))
    horizItems.add(20, Pair(GridItemType.VSectionTitle, "Group Four"))

    fun getNodeData(items: ArrayList<Pair<GridItemType, String>>, index: Int): GetDesignNodeData {
        return when (items[index].first) {
            GridItemType.SectionTitle -> {
                { GridWidgetTestDoc.SectionTitleDesignNodeData() }
            }
            GridItemType.VSectionTitle -> {
                { GridWidgetTestDoc.VSectionTitleDesignNodeData() }
            }
            GridItemType.RowGrid -> {
                { GridWidgetTestDoc.ItemDesignNodeData(type = ItemType.Grid) }
            }
            GridItemType.RowList -> {
                { GridWidgetTestDoc.ItemDesignNodeData(type = ItemType.List) }
            }
            GridItemType.ColGrid -> {
                { GridWidgetTestDoc.VItemDesignNodeData(type = ItemType.Grid) }
            }
            GridItemType.ColList -> {
                { GridWidgetTestDoc.VItemDesignNodeData(type = ItemType.List) }
            }
        }
    }

    @Composable
    fun itemComposable(items: ArrayList<Pair<GridItemType, String>>, index: Int) {
        when (items[index].first) {
            GridItemType.SectionTitle -> GridWidgetTestDoc.SectionTitle(title = items[index].second)
            GridItemType.VSectionTitle ->
                GridWidgetTestDoc.VSectionTitle(title = items[index].second)
            GridItemType.RowGrid ->
                GridWidgetTestDoc.Item(type = ItemType.Grid, title = items[index].second)
            GridItemType.RowList ->
                GridWidgetTestDoc.Item(type = ItemType.List, title = items[index].second)
            GridItemType.ColGrid ->
                GridWidgetTestDoc.VItem(type = ItemType.Grid, title = items[index].second)
            GridItemType.ColList ->
                GridWidgetTestDoc.VItem(type = ItemType.List, title = items[index].second)
        }
    }

    GridWidgetTestDoc.MainFrame(
        modifier = Modifier.fillMaxSize(),
        columns = { spanFunc ->
            ListContentData(
                count = vertItems.size,
                span = { index ->
                    val nodeData = getNodeData(vertItems, index)
                    spanFunc(nodeData)
                },
            ) { index ->
                itemComposable(vertItems, index)
            }
        },
        columnsScroll = { spanFunc ->
            ListContentData(
                count = vertItems.size,
                span = { index ->
                    val nodeData = getNodeData(vertItems, index)
                    spanFunc(nodeData)
                },
            ) { index ->
                if (index == 5)
                    GridWidgetTestDoc.Item(
                        modifier = Modifier.testTag("DragVertical"),
                        type = ItemType.Grid,
                        title = vertItems[index].second,
                    )
                else itemComposable(vertItems, index)
            }
        },
        verticalScrollCallbacks =
            DesignScrollCallbacks(
                setScrollableState = { scrollableState ->
                    if (scrollableState is LazyGridState) {
                        verticalScrollableState.value = scrollableState
                    }
                },
                scrollStateChanged = { scrollState ->
                    Log.i(
                        "DesignCompose",
                        "Vertical scroll state changed: ${scrollState.value} max ${scrollState.maxValue} size ${scrollState.containerSize} contentSize ${scrollState.contentSize}",
                    )
                },
            ),
        rows = { spanFunc ->
            ListContentData(
                count = horizItems.size,
                span = { index ->
                    val nodeData = getNodeData(horizItems, index)
                    spanFunc(nodeData)
                },
            ) { index ->
                itemComposable(horizItems, index)
            }
        },
        rowsScroll = { spanFunc ->
            ListContentData(
                count = horizItems.size,
                span = { index ->
                    val nodeData = getNodeData(horizItems, index)
                    spanFunc(nodeData)
                },
            ) { index ->
                if (index == 5)
                    GridWidgetTestDoc.VItem(
                        modifier = Modifier.testTag("DragHorizontal"),
                        type = ItemType.Grid,
                        title = horizItems[index].second,
                    )
                else itemComposable(horizItems, index)
            }
        },
        horizontalScrollCallbacks =
            DesignScrollCallbacks(
                setScrollableState = { scrollableState ->
                    if (scrollableState is LazyGridState) {
                        horizontalScrollableState.value = scrollableState
                    }
                },
                scrollStateChanged = { scrollState ->
                    Log.i(
                        "DesignCompose",
                        "Horizontal scroll state changed: ${scrollState.value} max ${scrollState.maxValue} size ${scrollState.containerSize} contentSize ${scrollState.contentSize}",
                    )
                },
            ),
        items = {
            ListContentData(count = 10, span = { LazyContentSpan(1) }) { index ->
                GridWidgetTestDoc.Item(type = ItemType.Grid, title = "Item $index")
            }
        },
    )
}
