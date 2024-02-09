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

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.test.internal.ROBO_CAPTURE_DIR
import com.android.designcompose.test.internal.captureImg
import com.android.designcompose.testapp.validation.examples.InteractionTest
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet, sdk = [34])
class InteractionTriggers {
    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule =
        RoborazziRule(
            options =
                RoborazziRule.Options(outputDirectoryPath = "$ROBO_CAPTURE_DIR/interactionTriggers")
        )

    @get:Rule var tn = TestName()

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
            captureImg(tn, "start")

            onNodeWithText("idle").performTouchInput { down(Offset.Zero) }
            onNodeWithText("pressed").assertExists()
            captureImg(tn, "pressed")

            waitUntilDoesNotExist(hasText("pressed"), 1000)
            onNodeWithText("timeout").assertExists()
            captureImg(tn, "timedOut")

            onRoot().performTouchInput { cancel() }
            onNodeWithText("idle").assertExists()
            captureImg(tn, "final")
        }
    }
}
