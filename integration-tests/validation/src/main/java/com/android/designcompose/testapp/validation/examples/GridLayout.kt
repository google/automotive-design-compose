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

import androidx.compose.runtime.Composable
import com.android.designcompose.ListContent
import com.android.designcompose.ListContentData
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignContentTypes
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignPreviewContent
import com.android.designcompose.annotation.DesignVariant
import com.android.designcompose.annotation.PreviewNode

// TEST Grid Layout for Documentation
@DesignDoc(id = "MBNjjSbzzKeN7nBjVoewsl")
interface GridLayout {
    @DesignComponent(node = "#stage")
    fun MainFrame(
        @Design(node = "#ListTitle") title: String,
        @DesignContentTypes(nodes = ["#SectionTitle", "#Item", "#Loading"])
        @DesignPreviewContent(name = "Loading", nodes = [PreviewNode(1, "#Loading")])
        @DesignPreviewContent(
            name = "LoadedList",
            nodes = [PreviewNode(1, "#SectionTitle"), PreviewNode(8, "#Item=Grid")],
        )
        @Design(node = "#BrowseList")
        items: ListContent,
    )

    @DesignComponent(node = "#SectionTitle")
    fun SectionTitle(@Design(node = "#Title") title: String)

    @DesignComponent(node = "#LoadingPage") fun LoadingPage()

    @DesignComponent(node = "#Item")
    fun Item(
        @DesignVariant(property = "#Item") itemType: ItemType,
        @Design(node = "#Title") title: String,
    )
}

@Composable
fun GridLayoutDocumentation() {
    GridLayoutDoc.MainFrame(title = "Media Browse") { spanFunc ->
        ListContentData(
            count = 9,
            span = { index ->
                if (index == 0) spanFunc { GridLayoutDoc.SectionTitleDesignNodeData() }
                else spanFunc { GridLayoutDoc.ItemDesignNodeData(itemType = ItemType.Grid) }
            },
        ) { index ->
            if (index == 0) GridLayoutDoc.SectionTitle(title = "Recently Played")
            else GridLayoutDoc.Item(itemType = ItemType.Grid, title = "Item $index")
        }
    }
}
