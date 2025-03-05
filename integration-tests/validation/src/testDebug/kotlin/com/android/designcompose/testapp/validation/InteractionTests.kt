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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.TestUtils
import com.android.designcompose.test.assertDoesNotHaveText
import com.android.designcompose.test.assertHasText
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.test.onDCDocAnyNode
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.InteractionTest
import com.android.designcompose.testapp.validation.examples.InteractionTestDoc
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

    @Test
    fun whilePressedTimeoutTimesOut() {
        with(composeTestRule) {
            with(onDCDocAnyNode(InteractionTestDoc)) {
                onNodeWithTag("Triggers").performClick()
                onNodeWithTag("While Pressed").performClick()
                onNodeWithTag("Timeouts").performClick()
                captureRootRoboImage("whilePressed-start")

                onNodeWithTag("idle").performTouchInput { down(Offset.Zero) }
                assertHasText("pressed")
                captureRootRoboImage("whilePressed-pressed")
                mainClock.advanceTimeBy(1000)

                assertDoesNotHaveText("pressed")
                assertHasText("timeout")
                captureRootRoboImage("whilePressed-timedOut")

                onRoot().performTouchInput { up() }
                assertHasText("idle")
                captureRootRoboImage("whilePressed-final")
            }
        }
    }

    @Test
    fun overlayPositionsTests() {
        with(composeTestRule) {
            with(onDCDocAnyNode(InteractionTestDoc)) {
                onNodeWithTag("Overlay Tests").performClick()
                onNodeWithTag("Overlay Positions").performClick()
                captureRootRoboImage("overlay-positions-start")

                // Test Top Left
                assertDoesNotHaveText("Top Left")
                onNodeWithTag("TL").performClick()
                assertHasText("Top Left")
                captureRootRoboImage("overlay-positions-tl")

                // Close the overlay. Need to use the unmerged tree to click the node
                onNodeWithTag("Overlay Top Left").performClick()
                assertDoesNotHaveText("Top Left")
                captureRootRoboImage("overlay-positions-tl-final")

                // Test Top Right
                assertDoesNotHaveText("Top Right")
                onNodeWithTag("TR").performClick()
                assertHasText("Top Right")
                captureRootRoboImage("overlay-positions-tr")
                onNodeWithTag("Overlay Top Right").performClick()
                assertDoesNotHaveText("Top Right")
                captureRootRoboImage("overlay-positions-tr-final")

                // Test Manual
                assertDoesNotHaveText("Manual")
                onNodeWithTag("M").performClick()
                assertHasText("Manual")
                captureRootRoboImage("overlay-positions-manual")
                onNodeWithTag("Overlay Manual").performClick()
                assertDoesNotHaveText("Manual")
                captureRootRoboImage("overlay-positions-manual-final")
            }
        }
    }

    @Test
    fun navigationTests() {
        with(composeTestRule) {
            with(onDCDocAnyNode(InteractionTestDoc)) {
                onNodeWithTag("Navigation Tests").performClick()
                captureRootRoboImage("navigation-start")

                // Initial state
                assertHasText("Navigation #1")

                onNodeWithTag("Nav 2").performClick()
                assertHasText("Navigation #2")
                assertDoesNotHaveText("Navigation #1")
                captureRootRoboImage("navigation-nav2")

                onNodeWithTag("Nav 1").performClick()
                assertHasText("Navigation #1")
                assertDoesNotHaveText("Navigation #2")
                captureRootRoboImage("navigation-nav1")

                // Test the back button
                onNodeWithTag("<--").performClick()
                assertHasText("Navigation #2")
                assertDoesNotHaveText("Navigation #1")
                captureRootRoboImage("navigation-back1")

                onNodeWithTag("<--").performClick()
                assertHasText("Navigation #1")
                assertDoesNotHaveText("Navigation #2")
                captureRootRoboImage("navigation-back2")
            }
        }
    }
}
