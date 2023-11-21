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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.DocRenderStatus
import com.android.designcompose.test.internal.assertRenderStatus
import com.android.designcompose.test.internal.defaultRoborazziRule
import com.android.designcompose.test.internal.onDCDoc
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
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
@Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet, sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RenderHelloWorld {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val roborazziRule = defaultRoborazziRule(composeTestRule)

    @Test(expected = AssertionError::class) // TODO: GH-483
    fun testHello() {
        with(composeTestRule) {
            setContent { HelloWorldDoc.mainFrame(name = "Testers!") }
            onDCDoc(HelloWorldDoc).assertRenderStatus(DocRenderStatus.Rendered)
            onNodeWithText("Testers!", substring = true).assertExists()
        }
    }
}
