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

package com.android.designcompose.testapp.helloworld

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.docIdSemanticsKey
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Render hello world
 *
 * Basic test that uses Robolectrict's native graphics to test that HelloWorld renders.
 *
 * Includes Roborazzi for Screenshot tests,
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RenderHelloWorld {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val roborazziRule =
        RoborazziRule(
            composeRule = composeTestRule,
            // Specify the node to capture for the last image
            captureRoot = composeTestRule.onRoot(),
            options =
                RoborazziRule.Options(
                    outputDirectoryPath = "src/testDebug/roborazzi",
                    // Always capture the last image of the test
                    captureType = RoborazziRule.CaptureType.LastImage()
                )
        )

    @Test
    fun testHello() {
        with(composeTestRule) {
            setContent { HelloWorldDoc.mainFrame(name = "Test 3") }
            onNode(SemanticsMatcher.expectValue(docIdSemanticsKey, helloWorldDocId)).assertExists()
            onNodeWithText("Test 3", substring = true)
                .also { it.assertExists() }
                .captureRoboImage("theText.png")
        }
    }

    @Test
    fun testError() {
        with(composeTestRule) {
            setContent { HelloWorldDoc.mainFrame(name = "Test Error") }
            onNode(SemanticsMatcher.expectValue(docIdSemanticsKey, helloWorldDocId)).assertExists()
            onNodeWithText("Test Error", substring = true)
                .also { it.assertExists() }
                .captureRoboImage("theTestErrorText.png")
        }
    }
}
