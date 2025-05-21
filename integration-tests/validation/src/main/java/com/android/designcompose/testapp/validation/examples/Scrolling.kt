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

import android.util.Log
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.DesignScrollCallbacks
import com.android.designcompose.ReplacementContent
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.testapp.validation.TestButton
import kotlinx.coroutines.launch

// TEST scrolling support
@DesignDoc(id = "1kyFRMyqZt6ylhOUKUvI2J")
interface ScrollingTest {
    @DesignComponent(node = "#stage")
    fun Main(
        @Design(node = "#VerticalScrollCustom") verticalContents: ReplacementContent,
        @Design(node = "#HorizontalScrollCustom") horizontalContents: ReplacementContent,
        @Design(node = "#ManualVerticalScroll") verticalScrollCallbacks: DesignScrollCallbacks,
        @Design(node = "#ManualHorizontalScroll") horizontalScrollCallbacks: DesignScrollCallbacks,
    )

    @DesignComponent(node = "#square1") fun Square1()

    @DesignComponent(node = "#square2") fun Square2()

    @DesignComponent(node = "#square-drag-vertical") fun SquareDragVertical()

    @DesignComponent(node = "#square-drag-horizontal") fun SquareDragHorizontal()

    @DesignComponent(node = "#square-tap")
    fun SquareTap(@Design(node = "#square-tap") tap: TapCallback)
}

@Composable
fun ScrollingTest(onTap: TapCallback? = null) {
    val verticalScrollableState = remember { mutableStateOf<ScrollableState?>(null) }
    val horizontalScrollableState = remember { mutableStateOf<ScrollableState?>(null) }
    val scope = rememberCoroutineScope()
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
                        else if (idx == 4) ScrollingTestDoc.SquareTap(tap = onTap ?: {})
                        else if (idx % 2 == 0) ScrollingTestDoc.Square2()
                        else ScrollingTestDoc.Square1()
                    }
                },
            ),
        verticalScrollCallbacks =
            DesignScrollCallbacks(
                setScrollableState = { scrollableState ->
                    verticalScrollableState.value = scrollableState
                },
                scrollStateChanged = { scrollState ->
                    Log.i(
                        "DesignCompose",
                        "Vertical scroll state changed: ${scrollState.value} max ${scrollState.maxValue} size ${scrollState.containerSize} contentSize ${scrollState.contentSize}",
                    )
                },
            ),
        horizontalScrollCallbacks =
            DesignScrollCallbacks(
                setScrollableState = { scrollableState ->
                    horizontalScrollableState.value = scrollableState
                },
                scrollStateChanged = { scrollState ->
                    Log.i(
                        "DesignCompose",
                        "Horizontal scroll state changed: ${scrollState.value} max ${scrollState.maxValue} size ${scrollState.containerSize} contentSize ${scrollState.contentSize}",
                    )
                },
            ),
    )

    Column(Modifier.offset(10.dp, 800.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
}
