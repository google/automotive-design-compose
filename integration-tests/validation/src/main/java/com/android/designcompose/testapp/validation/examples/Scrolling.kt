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

package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.android.designcompose.ReplacementContent
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST scrolling support
@DesignDoc(id = "1kyFRMyqZt6ylhOUKUvI2J")
interface ScrollingTest {
    @DesignComponent(node = "#stage")
    fun Main(
        @Design(node = "#VerticalScrollCustom") verticalContents: ReplacementContent,
        @Design(node = "#HorizontalScrollCustom") horizontalContents: ReplacementContent,
    )

    @DesignComponent(node = "#square1") fun Square1()

    @DesignComponent(node = "#square2") fun Square2()

    @DesignComponent(node = "#square-drag-vertical") fun SquareDragVertical()

    @DesignComponent(node = "#square-drag-horizontal") fun SquareDragHorizontal()
}

@Composable
fun ScrollingTest() {
    ScrollingTestDoc.Main(
        verticalContents =
            ReplacementContent(
                count = 5,
                content = { idx ->
                    @Composable {
                        if (idx == 0)
                            ScrollingTestDoc.SquareDragVertical(
                                modifier = Modifier.testTag("DragVertical")
                            )
                        else if (idx % 2 == 0) ScrollingTestDoc.Square1()
                        else ScrollingTestDoc.Square2()
                    }
                },
            ),
        horizontalContents =
            ReplacementContent(
                count = 5,
                content = { idx ->
                    @Composable {
                        if (idx == 0)
                            ScrollingTestDoc.SquareDragHorizontal(
                                modifier = Modifier.testTag("DragHorizontal")
                            )
                        else if (idx % 2 == 0) ScrollingTestDoc.Square2()
                        else ScrollingTestDoc.Square1()
                    }
                },
            ),
    )
}
