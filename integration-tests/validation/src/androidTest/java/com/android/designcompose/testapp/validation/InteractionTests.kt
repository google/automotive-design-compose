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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.test.assertDoesNotHaveText
import com.android.designcompose.test.assertHasText
import com.android.designcompose.test.onDCDocAnyNode
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.InteractionTest
import com.android.designcompose.testapp.validation.examples.InteractionTestDoc
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InteractionTests {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val interFontRule = InterFontTestRule()

    @Test
    // Exercises the Timeout Triggers of the Interaction Doc
    fun testTriggersTimeouts() {
        composeTestRule.setContent { InteractionTest() }

        with(composeTestRule) {
            with(onDCDocAnyNode(InteractionTestDoc)) {
                // Navigate to the Triggers -> Timeouts menu
                onNodeWithTag("Triggers").performClick()
                assertHasText("While Pressed")
                onNodeWithTag("While Pressed").performClick()
                assertHasText("Timeouts")
                onNodeWithTag("Timeouts").performClick()

                // Confirm initial state
                assertDoesNotHaveText("pressed")
                assertHasText("idle")

                // Hold the button down for half a second and release

                onNodeWithTag("idle").performTouchInput { down(center) }
                assertDoesNotHaveText("idle")

                assertHasText("pressed")
                onRoot().performTouchInput { cancel(500) }
                assertDoesNotHaveText("pressed")
                assertHasText("idle")

                // Hold the button down for more than a second, make sure it switches to display
                // "timeout",
                onNodeWithTag("idle").performTouchInput { down(center) }
                assertDoesNotHaveText("idle")
                assertHasText("pressed")
                mainClock.advanceTimeBy(1250)
                assertDoesNotHaveText("pressed")
                assertHasText("timeout")

                // Release the button, make sure it returns to idle.
                onRoot().performTouchInput { cancel() }
                assertDoesNotHaveText("timeout")
                assertHasText("idle")
            }
        }
    }
}
