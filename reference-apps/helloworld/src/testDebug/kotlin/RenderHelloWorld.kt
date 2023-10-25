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
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.DocRenderStatus
import com.android.designcompose.docClassSemanticsKey
import com.android.designcompose.docRenderStatusSemanticsKey
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Render hello world
 *
 * Basic test that uses Robolectric's native graphics to test that HelloWorld renders.
 *
 * Includes Roborazzi for Screenshot tests,
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet, sdk = [33])
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

    @Test(expected = AssertionError::class)
    fun testHello() {
        with(composeTestRule) {
            setContent { HelloWorldDoc.mainFrame(name = "Testers!") }
            onNode(SemanticsMatcher.expectValue(docClassSemanticsKey, HelloWorldDoc.javaClass.name))
                .assert(
                    SemanticsMatcher.expectValue(
                        docRenderStatusSemanticsKey,
                        DocRenderStatus.Rendered
                    )
                )
            onNodeWithText("Testers!", substring = true).assertExists()
        }
    }
}
