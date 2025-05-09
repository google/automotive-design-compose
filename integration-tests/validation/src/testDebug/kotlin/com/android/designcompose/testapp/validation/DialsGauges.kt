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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.TestUtils
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.DialsGaugesTest
import com.android.designcompose.testapp.validation.examples.DraggableProgressTest
import com.android.designcompose.testapp.validation.examples.ProgressConstraintsTest
import com.android.designcompose.testapp.validation.examples.ProgressVectorTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1920dp-h1500dp-xlarge-long-notround-any-xhdpi-keyshidden-nonav", sdk = [35])
class DialsGauges {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontRule = InterFontTestRule()

    @Before
    fun setUp() {
        System.setProperty("robolectric.pixelCopyRenderMode", "hardware")
    }

    @Test
    fun progressTests() {
        with(composeTestRule) {
            setContent { DialsGaugesTest() }

            onNodeWithTag("angle").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("angle").performTouchInput { moveTo(Offset(-200f, 0f)) }
            onNodeWithTag("angle").performTouchInput { cancel() }
            captureRootRoboImage("angle-low")

            onNodeWithTag("angle").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("angle").performTouchInput { moveTo(Offset(400f, 0f)) }
            onNodeWithTag("angle").performTouchInput { cancel() }
            captureRootRoboImage("angle-high")

            onNodeWithTag("rotation").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("rotation").performTouchInput { moveTo(Offset(-200f, 0f)) }
            onNodeWithTag("rotation").performTouchInput { cancel() }
            captureRootRoboImage("rotation-low")

            onNodeWithTag("rotation").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("rotation").performTouchInput { moveTo(Offset(400f, 0f)) }
            onNodeWithTag("rotation").performTouchInput { cancel() }
            captureRootRoboImage("rotation-high")

            onNodeWithTag("progress-bar").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-bar").performTouchInput { moveTo(Offset(-200f, 0f)) }
            onNodeWithTag("progress-bar").performTouchInput { cancel() }
            captureRootRoboImage("progress-bar-low")

            onNodeWithTag("progress-bar").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-bar").performTouchInput { moveTo(Offset(400f, 0f)) }
            onNodeWithTag("progress-bar").performTouchInput { cancel() }
            captureRootRoboImage("progress-bar-high")

            onNodeWithTag("progress-indicator").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-indicator").performTouchInput { moveTo(Offset(-200f, 0f)) }
            onNodeWithTag("progress-indicator").performTouchInput { cancel() }
            captureRootRoboImage("progress-indicator-low")

            onNodeWithTag("progress-indicator").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-indicator").performTouchInput { moveTo(Offset(400f, 0f)) }
            onNodeWithTag("progress-indicator").performTouchInput { cancel() }
            captureRootRoboImage("progress-indicator-high")
        }
    }

    @Test
    fun progressVectorTests() {
        with(composeTestRule) {
            setContent { ProgressVectorTest() }

            onNodeWithTag("progress-bar").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-bar").performTouchInput { moveTo(Offset(-200f, 0f)) }
            onNodeWithTag("progress-bar").performTouchInput { cancel() }
            captureRootRoboImage("progress-vector-low")

            onNodeWithTag("progress-bar").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-bar").performTouchInput { moveTo(Offset(400f, 0f)) }
            onNodeWithTag("progress-bar").performTouchInput { cancel() }
            captureRootRoboImage("progress-vector-high")
        }
    }

    @Test
    fun progressConstraintsTests() {
        with(composeTestRule) {
            setContent { ProgressConstraintsTest() }

            onNodeWithTag("progress-bar").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-bar").performTouchInput { moveTo(Offset(-200f, 0f)) }
            onNodeWithTag("progress-bar").performTouchInput { cancel() }
            onNodeWithTag("progress-indicator").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-indicator").performTouchInput { moveTo(Offset(-200f, 0f)) }
            onNodeWithTag("progress-indicator").performTouchInput { cancel() }
            captureRootRoboImage("progress-constraints-low-small")

            onNodeWithTag("progress-bar").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-bar").performTouchInput { moveTo(Offset(400f, 0f)) }
            onNodeWithTag("progress-bar").performTouchInput { cancel() }
            onNodeWithTag("progress-indicator").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-indicator").performTouchInput { moveTo(Offset(400f, 0f)) }
            onNodeWithTag("progress-indicator").performTouchInput { cancel() }
            captureRootRoboImage("progress-constraints-high-small")

            onNodeWithTag("main-width").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("main-width").performTouchInput { moveTo(Offset(200f, 0f)) }
            onNodeWithTag("main-width").performTouchInput { cancel() }
            onNodeWithTag("main-height").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("main-height").performTouchInput { moveTo(Offset(200f, 0f)) }
            onNodeWithTag("main-height").performTouchInput { cancel() }
            captureRootRoboImage("progress-constraints-high-big")

            onNodeWithTag("progress-bar").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-bar").performTouchInput { moveTo(Offset(-400f, 0f)) }
            onNodeWithTag("progress-bar").performTouchInput { cancel() }
            onNodeWithTag("progress-indicator").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-indicator").performTouchInput { moveTo(Offset(-400f, 0f)) }
            onNodeWithTag("progress-indicator").performTouchInput { cancel() }
            captureRootRoboImage("progress-constraints-low-big")
        }
    }

    @Test
    fun draggableProgressTests() {
        with(composeTestRule) {
            setContent { DraggableProgressTest() }

            // Test clicking to move the progress low
            onNodeWithTag("HorizontalBar").performTouchInput { click(Offset(50F, 0F)) }
            onNodeWithTag("HorizontalIndicator").performTouchInput { click(Offset(50F, 0F)) }
            onNodeWithTag("VerticalBar").performTouchInput { click(Offset(0F, 120F)) }
            onNodeWithTag("VerticalIndicator").performTouchInput { click(Offset(0F, 120F)) }
            onNodeWithTag("SkewedVerticalBar").performTouchInput { click(Offset(0F, 300F)) }
            captureRootRoboImage("draggable-progress-low")

            // Test dragging to move the progress high
            onNodeWithTag("HorizontalBar").performTouchInput { down(Offset(50F, 0F)) }
            onNodeWithTag("HorizontalBar").performTouchInput { moveTo(Offset(900F, 0F)) }
            onNodeWithTag("HorizontalBar").performTouchInput { up() }
            onNodeWithTag("HorizontalIndicator").performTouchInput { down(Offset(50F, 0F)) }
            onNodeWithTag("HorizontalIndicator").performTouchInput { moveTo(Offset(900F, 0F)) }
            onNodeWithTag("HorizontalIndicator").performTouchInput { up() }
            onNodeWithTag("VerticalBar").performTouchInput { down(Offset(0F, 120F)) }
            onNodeWithTag("VerticalBar").performTouchInput { moveTo(Offset(0F, 10F)) }
            onNodeWithTag("VerticalBar").performTouchInput { up() }
            onNodeWithTag("VerticalIndicator").performTouchInput { down(Offset(0F, 120F)) }
            onNodeWithTag("VerticalIndicator").performTouchInput { moveTo(Offset(0F, 10F)) }
            onNodeWithTag("VerticalIndicator").performTouchInput { up() }
            onNodeWithTag("SkewedVerticalBar").performTouchInput { down(Offset(0F, 300F)) }
            onNodeWithTag("SkewedVerticalBar").performTouchInput { moveTo(Offset(0F, 20F)) }
            onNodeWithTag("SkewedVerticalBar").performTouchInput { up() }
            captureRootRoboImage("draggable-progress-high")
        }
    }
}
