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

package com.android.designcompose.testapp.validation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.DesignSettings
import com.android.designcompose.testapp.validation.examples.InteractionTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InteractionTests {
    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        DesignSettings.addFontFamily("Inter", interFont)
    }

    @Test
    // Exercises the Timeout Triggers of the Interaction Doc
    fun testTriggersTimeouts() {
        composeTestRule.setContent { InteractionTest() }

        // Navigate to the Triggers -> Timeouts menu
        composeTestRule.onNodeWithText("Triggers").performClick()
        composeTestRule.onNodeWithText("While Pressed").assertExists().performClick()
        composeTestRule.onNodeWithText("Timeouts").assertExists().performClick()

        // Confirm initial state
        composeTestRule.onNodeWithText("pressed").assertDoesNotExist()
        composeTestRule.onNodeWithText("idle").assertExists()

        // Hold the button down for half a second and release
        composeTestRule
            .onNodeWithText("idle")
            .performTouchInput { down(center) }
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("pressed").assertExists()
        composeTestRule.onRoot().performTouchInput { cancel(500) }
        composeTestRule.onNodeWithText("pressed").assertDoesNotExist()
        composeTestRule.onNodeWithText("idle").assertExists()

        // Hold the button down for more than a second, make sure it switches to display "timeout",
        composeTestRule
            .onNodeWithText("idle")
            .performTouchInput { down(center) }
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("pressed").assertExists()
        composeTestRule.mainClock.advanceTimeBy(1250)
        composeTestRule.onNodeWithText("pressed").assertDoesNotExist()
        composeTestRule.onNodeWithText("timeout").assertExists()

        // Release the button, make sure it returns to idle.
        composeTestRule.onRoot().performTouchInput { cancel() }
        composeTestRule.onNodeWithText("timeout").assertDoesNotExist()
        composeTestRule.onNodeWithText("idle").assertExists()
    }
}
