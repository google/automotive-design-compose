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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.DesignComposeCallbacks
import com.android.designcompose.ListContent
import com.android.designcompose.ListContentData
import com.android.designcompose.ReplacementContent
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignContentTypes
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignPreviewContent
import com.android.designcompose.annotation.DesignVariant
import com.android.designcompose.annotation.PreviewNode

// TEST Various layout tests for new Rust based layout system
@DesignDoc(id = "Gv63fYTzpeH2ZtxP4go31E")
interface LayoutTests {
    @DesignComponent(node = "#stage")
    fun Main(
        @Design(node = "#Name") name: String,
        @Design(node = "#NameAutoWidth") nameAutoWidth: String,
        @Design(node = "#NameAutoHeight") nameAutoHeight: String,
        @Design(node = "#NameFixed") nameFixed: String,
        @Design(node = "#NameFillWidthAutoHeight") nameFillWidthAutoHeight: String,
        @DesignVariant(property = "#ButtonSquare") buttonSquare: ButtonSquare,
        @Design(node = "#HorizontalContent") horizontalContent: ReplacementContent,
        @Design(node = "#Parent") parent: ReplacementContent,
        @DesignContentTypes(nodes = ["#SectionTitle", "#Item", "#LoadingPage", "#ErrorPage"])
        @DesignPreviewContent(
            name = "Items",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(2, "#Item=Grid, #Playing=Off"),
                    PreviewNode(1, "#Item=Grid, #Playing=On"),
                    PreviewNode(6, "#Item=Grid, #Playing=Off"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(1, "#Item=List, #Playing=On"),
                    PreviewNode(3, "#Item=List, #Playing=Off")
                ]
        )
        @Design(node = "#column-auto-content")
        widgetItems: ListContent,
        @Design(node = "#Rect1") showRect1: Boolean,
        @Design(node = "#Rect2") showRect2: Boolean,
        @Design(node = "#Replacement1")
        replacement1: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#Replacement2")
        replacement2: @Composable (ComponentReplacementContext) -> Unit,
    )

    @DesignComponent(node = "#BlueSquare") fun BlueSquare()

    @DesignComponent(node = "#RedSquare") fun RedSquare()

    @DesignComponent(node = "#fill") fun Fill()

    @DesignComponent(node = "#topleft") fun TopLeft()

    @DesignComponent(node = "#bottomright") fun BottomRight()

    @DesignComponent(node = "#center") fun Center()

    @DesignComponent(node = "#ButtonSquare")
    fun ButtonSquare(
        @DesignVariant(property = "#ButtonSquare") type: ButtonSquare,
    )

    @DesignComponent(node = "#Item")
    fun Item(
        @DesignVariant(property = "#Item") type: ItemType,
        @Design(node = "#Title") title: String,
    )
}

@Composable
fun LayoutTests() {
    val loremText =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
    val (autoWidthLen, setAutoWidthLen) = remember { mutableStateOf(17) }
    val (autoHeightLen, setAutoHeightLen) = remember { mutableStateOf(17) }
    val (fixedSizeLen, setFixedSizeLen) = remember { mutableStateOf(17) }
    val (fillWidthAutoHeightLen, setFillWidthAutoHeightLen) = remember { mutableStateOf(89) }
    val (buttonSquare, setButtonSquare) = remember { mutableStateOf(ButtonSquare.Off) }
    val (numChildren, setNumChildren) = remember { mutableStateOf(2) }
    val (showRect1, setShowRect1) = remember { mutableStateOf(false) }
    val (showRect2, setShowRect2) = remember { mutableStateOf(true) }

    LayoutTestsDoc.Main(
        modifier = Modifier.fillMaxWidth(0.5F),
        name = "LongerText",
        nameAutoWidth = loremText.subSequence(0, autoWidthLen).toString(),
        nameAutoHeight = loremText.subSequence(0, autoHeightLen).toString(),
        nameFixed = loremText.subSequence(0, fixedSizeLen).toString(),
        nameFillWidthAutoHeight = loremText.subSequence(0, fillWidthAutoHeightLen).toString(),
        buttonSquare = buttonSquare,
        horizontalContent =
            ReplacementContent(
                count = numChildren,
                content = { index ->
                    {
                        if (index % 2 == 0) LayoutTestsDoc.BlueSquare()
                        else LayoutTestsDoc.RedSquare()
                    }
                }
            ),
        parent = ReplacementContent(count = 3, content = { { LayoutTestsDoc.BlueSquare() } }),
        widgetItems = { spanFunc ->
            ListContentData(
                count = 4,
                span = {
                    val nodeData = { LayoutTestsDoc.ItemDesignNodeData(type = ItemType.Grid) }
                    spanFunc(nodeData)
                },
            ) {
                LayoutTestsDoc.Item(
                    title = "Hello",
                    type = ItemType.Grid,
                )
            }
        },
        showRect1 = showRect1,
        showRect2 = showRect2,
        replacement1 = { LayoutTestsDoc.BlueSquare() },
        replacement2 = { LayoutTestsDoc.BlueSquare() },
        designComposeCallbacks =
            DesignComposeCallbacks(
                docReadyCallback = { id ->
                    Log.i("DesignCompose", "HelloWorld Ready: doc ID = $id")
                },
                newDocDataCallback = { docId, data ->
                    Log.i(
                        "DesignCompose",
                        "HelloWorld Updated doc ID $docId: ${data?.size ?: 0} bytes"
                    )
                },
            )
    )
    Column(Modifier.offset(10.dp, 820.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AutoWidth", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button("-", false) {
                val len = (autoWidthLen - 1).coerceAtLeast(1)
                setAutoWidthLen(len)
            }
            com.android.designcompose.testapp.validation.Button("+", false) {
                val len = (autoWidthLen + 1).coerceAtMost(loremText.length)
                setAutoWidthLen(len)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AutoHeight", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button("-", false) {
                val len = (autoHeightLen - 1).coerceAtLeast(1)
                setAutoHeightLen(len)
            }
            com.android.designcompose.testapp.validation.Button("+", false) {
                val len = (autoHeightLen + 1).coerceAtMost(loremText.length)
                setAutoHeightLen(len)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Fixed", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button("-", false) {
                val len = (fixedSizeLen - 1).coerceAtLeast(1)
                setFixedSizeLen(len)
            }
            com.android.designcompose.testapp.validation.Button("+", false) {
                val len = (fixedSizeLen + 1).coerceAtMost(loremText.length)
                setFixedSizeLen(len)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("FillWidth AutoHeight", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button("-", false) {
                val len = (fillWidthAutoHeightLen - 1).coerceAtLeast(1)
                setFillWidthAutoHeightLen(len)
            }
            com.android.designcompose.testapp.validation.Button("+", false) {
                val len = (fillWidthAutoHeightLen + 1).coerceAtMost(loremText.length)
                setFillWidthAutoHeightLen(len)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ButtonSquare", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button("Change Variant", false) {
                setButtonSquare(
                    when (buttonSquare) {
                        ButtonSquare.Off -> ButtonSquare.On
                        ButtonSquare.On -> ButtonSquare.Blue
                        ButtonSquare.Blue -> ButtonSquare.Green
                        ButtonSquare.Green -> ButtonSquare.Off
                    }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Replacement Content", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button("-", false) {
                val num = (numChildren - 1).coerceAtLeast(0)
                setNumChildren(num)
            }
            com.android.designcompose.testapp.validation.Button("+", false) {
                setNumChildren(numChildren + 1)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Visibility", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button("Rect1", false) {
                setShowRect1(!showRect1)
            }
            com.android.designcompose.testapp.validation.Button("Rect2", false) {
                setShowRect2(!showRect2)
            }
        }
    }
}
