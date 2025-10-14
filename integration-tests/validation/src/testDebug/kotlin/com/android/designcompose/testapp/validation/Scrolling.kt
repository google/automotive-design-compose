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

package com.android.designcompose.testapp.validation

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.DesignScrollCallbacks
import com.android.designcompose.ReplacementContent
import com.android.designcompose.TestUtils
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.test.onDCDocRoot
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.GridWidgetTest
import com.android.designcompose.testapp.validation.examples.GridWidgetTestContent
import com.android.designcompose.testapp.validation.examples.ScrollingTest
import com.android.designcompose.testapp.validation.examples.ScrollingTestDoc
import com.android.designcompose.testapp.validation.examples.TapHoldDrag
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1200dp-h1920dp-xlarge-long-notround-any-xhdpi-keyshidden-nonav")
class Scrolling {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontRule = InterFontTestRule()

    @Test
    fun dragScroll() {
        with(composeTestRule) {
            setContent { ScrollingTest(onTap = { println(" ### Hello") }) }
            onNodeWithTag("DragVertical").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("DragVertical").performTouchInput { moveTo(Offset(0f, -200F)) }
            onNodeWithTag("DragVertical").performTouchInput { cancel() }
            captureRootRoboImage("scroll-vertical")

            onNodeWithTag("DragHorizontal").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("DragHorizontal").performTouchInput { moveTo(Offset(-200f, 0F)) }
            onNodeWithTag("DragHorizontal").performTouchInput { cancel() }
            captureRootRoboImage("scroll-horizontal")
        }
    }

    @Test
    fun dragScrollTap() {
        with(composeTestRule) {
            val tapped = mutableStateOf(false)
            setContent { ScrollingTest(onTap = { tapped.value = true }) }
            onNodeWithTag("DragHorizontal").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("DragHorizontal").performTouchInput { moveTo(Offset(-500f, 0F)) }
            onNodeWithTag("DragHorizontal").performTouchInput { cancel() }
            captureRootRoboImage("scroll-then-tap")
            // Scrolling should put the clickable green square at the coordinates below, so
            // clicking it should set tapped to true
            onDCDocRoot(ScrollingTestDoc).performTouchInput { click(Offset(1100f, 775f)) }
            assert(tapped.value)
        }
    }

    @Test
    fun dragScrollClickables() {
        with(composeTestRule) {
            setContent { ScrollingTest() }
            // Check that onPress interaction works when in a scrollable view
            onNodeWithTag("clickable1").performTouchInput { down(Offset.Zero) }
            captureRootRoboImage("scroll-clickable-onpress")
            onNodeWithTag("clickable1=pressed").performTouchInput { up() }

            // Check that clicking a node with a click interaction works when in a scrollable view
            onNodeWithTag("clickable2").performTouchInput { click() }
            captureRootRoboImage("scroll-clickable-click")

            // Check that scrolling by dragging a clickable node works
            onNodeWithTag("clickable2=pressed").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("clickable2=pressed").performTouchInput { moveTo(Offset(-200f, -0F)) }
            captureRootRoboImage("scroll-clickable-scrolled")
            onNodeWithTag("clickable2=pressed").performTouchInput { up() }
        }
    }

    @Test
    fun manualScroll() = runTest {
        with(composeTestRule) {
            val verticalScrollableState = mutableStateOf<ScrollableState?>(null)
            val horizontalScrollableState = mutableStateOf<ScrollableState?>(null)
            setContent {
                ScrollingTestDoc.Main(
                    verticalContents = ReplacementContent(content = { {} }),
                    horizontalContents = ReplacementContent(content = { {} }),
                    verticalScrollCallbacks =
                        DesignScrollCallbacks(
                            setScrollableState = { scrollableState ->
                                verticalScrollableState.value = scrollableState
                            }
                        ),
                    horizontalScrollCallbacks =
                        DesignScrollCallbacks(
                            setScrollableState = { scrollableState ->
                                horizontalScrollableState.value = scrollableState
                            }
                        ),
                )
            }
            waitUntil(1000) {
                verticalScrollableState.value != null && horizontalScrollableState.value != null
            }
            assertNotNull(verticalScrollableState.value)
            assertNotNull(horizontalScrollableState.value)

            verticalScrollableState.value!!.scrollBy(50F)
            captureRootRoboImage("scroll-manual-vertical")

            horizontalScrollableState.value!!.scrollBy(50F)
            captureRootRoboImage("scroll-manual-horizontal")
        }
    }

    @Test
    fun dragGridScroll() {
        with(composeTestRule) {
            setContent { GridWidgetTest() }
            onNodeWithTag("DragVertical").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("DragVertical").performTouchInput { moveTo(Offset(0f, -200F)) }
            onNodeWithTag("DragVertical").performTouchInput { cancel() }
            captureRootRoboImage("scroll-grid-vertical")

            onNodeWithTag("DragHorizontal").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("DragHorizontal").performTouchInput { moveTo(Offset(-200f, 0F)) }
            onNodeWithTag("DragHorizontal").performTouchInput { cancel() }
            captureRootRoboImage("scroll-grid-horizontal")
        }
    }

    @Test
    fun manualGridScroll() = runTest {
        with(composeTestRule) {
            val verticalScrollableState = mutableStateOf<LazyGridState?>(null)
            val horizontalScrollableState = mutableStateOf<LazyGridState?>(null)
            setContent { GridWidgetTestContent(verticalScrollableState, horizontalScrollableState) }

            waitUntil(1000) {
                verticalScrollableState.value != null && horizontalScrollableState.value != null
            }
            assertNotNull(verticalScrollableState.value)
            assertNotNull(horizontalScrollableState.value)

            verticalScrollableState.value!!.scrollBy(100F)
            captureRootRoboImage("scroll-manual-grid-vertical")

            horizontalScrollableState.value!!.scrollBy(100F)
            captureRootRoboImage("scroll-manual-grid-horizontal")
        }
    }

    @Test
    fun scrollInitCallback() = runTest {
        with(composeTestRule) {
            val initCalled = mutableStateOf(false)
            setContent {
                ScrollingTestDoc.Main(
                    verticalContents = ReplacementContent(content = { {} }),
                    horizontalContents = ReplacementContent(content = { {} }),
                    verticalScrollCallbacks = DesignScrollCallbacks(),
                    horizontalScrollCallbacks =
                        DesignScrollCallbacks(scrollStateChanged = { initCalled.value = true }),
                )
            }
            assert(initCalled.value)
        }
    }

    @Test
    fun tapHoldDragScrollTest() {
        with(composeTestRule) {
            setContent { TapHoldDrag() }
            waitForIdle()

            // -- Test plain tap on Item0 --
            onNodeWithTag("Item0").performTouchInput { down(Offset.Zero) }
            waitForIdle()
            // Assert Item0 is now in the tapped state
            onNodeWithTag("state=pressed").assertExists()
            onNodeWithTag("Item0").assertDoesNotExist()

            // Click again to reset Item0 state
            onNodeWithTag("state=pressed").performTouchInput { up() }
            waitForIdle()
            onNodeWithTag("Item0").assertExists()
            onNodeWithTag("state=pressed").assertDoesNotExist()

            // -- Test Tap, Hold, Drag on Item0 --
            onNodeWithTag("Item0").performTouchInput {
                down(Offset.Zero)
                mainClock.advanceTimeBy(500) // Hold
                moveTo(Offset(0f, -200f)) // Drag
            }
            waitForIdle()
            // Assert Item0 did NOT change to the tapped state
            onNodeWithTag("Item0").assertExists()
            onNodeWithTag("state=pressed").assertDoesNotExist()

            onNodeWithTag("Item0").performTouchInput { up() }
            waitForIdle()
        }
    }
}
