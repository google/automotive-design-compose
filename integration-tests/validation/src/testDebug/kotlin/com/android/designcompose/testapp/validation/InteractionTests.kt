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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.TestUtils
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.InteractionTest
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.SmallPhone)
class InteractionTests {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontRule = InterFontTestRule()

    @Before
    fun setup() {
        with(composeTestRule) { setContent { InteractionTest() } }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun whilePressedTimeoutTimesOut() {
        with(composeTestRule) {
            onNodeWithText("Triggers").performClick()
            onNodeWithText("While Pressed").performClick()
            onNodeWithText("Timeouts").performClick()
            captureRootRoboImage("whilePressed-start")

            onNodeWithText("idle").performTouchInput { down(Offset.Zero) }
            onNodeWithText("pressed").assertExists()
            captureRootRoboImage("whilePressed-pressed")

            waitUntilDoesNotExist(hasText("pressed"), 1000)
            onNodeWithText("timeout").assertExists()
            captureRootRoboImage("whilePressed-timedOut")

            onRoot().performTouchInput { cancel() }
            onNodeWithText("idle").assertExists()
            captureRootRoboImage("whilePressed-final")
        }
    }

    @Test
    fun overlayPositionsTests() {
        with(composeTestRule) {
            onNodeWithText("Overlay Tests").performClick()
            onNodeWithText("Overlay Positions").performClick()
            captureRootRoboImage("overlay-positions-start")

            // Test Top Left
            onNodeWithText("Top Left").assertDoesNotExist()
            onNodeWithText("TL").performClick()
            onNodeWithText("Top Left").assertExists()
            captureRootRoboImage("overlay-positions-tl")

            // Close the overlay. Need to use the unmerged tree to click the node
            onNodeWithText("Click To Close", useUnmergedTree = true).performClick()
            onNodeWithText("Top Left").assertDoesNotExist()
            captureRootRoboImage("overlay-positions-tl-final")

            // Test Top Right
            onNodeWithText("Top Right").assertDoesNotExist()
            onNodeWithText("TR").performClick()
            onNodeWithText("Top Right").assertExists()
            captureRootRoboImage("overlay-positions-tr")
            onNodeWithText("Click To Close", useUnmergedTree = true).performClick()
            onNodeWithText("Top Right").assertDoesNotExist()
            captureRootRoboImage("overlay-positions-tr-final")

            // Test Manual
            onNodeWithText("Manual").assertDoesNotExist()
            onNodeWithText("M").performClick()
            onNodeWithText("Manual").assertExists()
            captureRootRoboImage("overlay-positions-manual")
            onNodeWithText("Click To Close", useUnmergedTree = true).performClick()
            onNodeWithText("Manual").assertDoesNotExist()
            captureRootRoboImage("overlay-positions-manual-final")
        }
    }

    @Test
    fun navigationTests() {
        with(composeTestRule) {
            onNodeWithText("Navigation Tests").performClick()
            captureRootRoboImage("navigation-start")

            // Initial state
            onNodeWithText("Navigation #1").assertExists()

            onNodeWithText("Nav 2").performClick()
            onNodeWithText("Navigation #2").assertExists()
            onNodeWithText("Navigation #1").assertDoesNotExist()
            captureRootRoboImage("navigation-nav2")

            onNodeWithText("Nav 1").performClick()
            onNodeWithText("Navigation #1").assertExists()
            onNodeWithText("Navigation #2").assertDoesNotExist()
            captureRootRoboImage("navigation-nav1")

            // Test the back button
            onNodeWithText("<--").performClick()
            onNodeWithText("Navigation #2").assertExists()
            onNodeWithText("Navigation #1").assertDoesNotExist()
            captureRootRoboImage("navigation-back1")

            onNodeWithText("<--").performClick()
            onNodeWithText("Navigation #1").assertExists()
            onNodeWithText("Navigation #2").assertDoesNotExist()
            captureRootRoboImage("navigation-back2")
        }
    }
}
