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
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.test.internal.ROBO_CAPTURE_DIR
import com.android.designcompose.testapp.validation.examples.VariantPropertiesTest
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
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
class VariantMatching {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val roborazziRule =
        RoborazziRule(
            options =
                RoborazziRule.Options(outputDirectoryPath = "$ROBO_CAPTURE_DIR/variantMatching")
        )

    @get:Rule var testName = TestName()

    fun ComposeTestRule.captureImg(name: String) {
        onRoot().captureRoboImage("${testName.methodName}/$name.png")
    }

    @Before
    fun setup() {
        with(composeTestRule) { setContent { VariantPropertiesTest() } }
    }

    @Test
    fun variantBackgroundMatches() {
        with(composeTestRule) {
            onNodeWithText("Green").performClick()
            onNodeWithText("BG1 Square").performClick()
            onNodeWithText("Background1Square").assertExists()
            captureImg("Background1Square")

            onNodeWithText("BG1 Circle").performClick()
            onNodeWithText("Background1Circle").assertExists()
            captureImg("Background1Circle")

            onNodeWithText("BG2 Square").performClick()
            onNodeWithText("Background2Square").assertExists()
            captureImg("Background2Square")

            onNodeWithText("BG2 Circle").performClick()
            onNodeWithText("Background2Circle").assertExists()
            captureImg("Background2Circle")

            onNodeWithText("Inner Sharp").performClick()
            onNodeWithText("SquareBorder").assertExists()
            captureImg("SquareBorder")

            onNodeWithText("Inner Curved").performClick()
            onNodeWithText("CurvedBorder").assertExists()
            captureImg("CurvedBorder")
        }
    }

    @Test
    fun variantNestedMatches() {
        with(composeTestRule) {
            onNodeWithText("Curved").performClick()
            onNodeWithText("Comp1 One").performClick()
            onNodeWithText("Comp1One").assertExists()
            captureImg("Comp1One")

            onNodeWithText("Comp1 Two").performClick()
            onNodeWithText("Comp1Two").assertExists()
            captureImg("Comp1Two")

            onNodeWithText("Comp2 One").performClick()
            onNodeWithText("Comp2One").assertExists()
            captureImg("Comp2One")

            onNodeWithText("Comp2 Two").performClick()
            onNodeWithText("Comp2Two").assertExists()
            captureImg("Comp2Two")

            onNodeWithText("Comp3 One").performClick()
            onNodeWithText("C31").assertExists()
            captureImg("C31")

            onNodeWithText("Comp3 Two").performClick()
            onNodeWithText("C32").assertExists()
            captureImg("C32")

            onNodeWithText("Square").performClick()
            onNodeWithText("C34").assertExists()
            captureImg("C34")

            onNodeWithText("Comp3 One").performClick()
            onNodeWithText("C33").assertExists()
            captureImg("C33")
        }
    }

    @Test
    fun variantComponentSetsFound() {
        with(composeTestRule) {
            onNodeWithText("Green").performClick()
            onNodeWithText("Shade One").performClick()
            onNodeWithText("GreenOne").assertExists()
            captureImg("GreenOne")

            onNodeWithText("Shade Two").performClick()
            onNodeWithText("GreenTwo").assertExists()
            captureImg("GreenTwo")

            onNodeWithText("Shade Three").performClick()
            onNodeWithText("GreenThree").assertExists()
            captureImg("GreenThree")

            onNodeWithText("Blue").performClick()
            onNodeWithText("Shade One").performClick()
            onNodeWithText("BlueOne").assertExists()
            captureImg("BlueOne")

            onNodeWithText("Shade Two").performClick()
            onNodeWithText("BlueTwo").assertExists()
            captureImg("BlueTwo")

            onNodeWithText("Shade Three").performClick()
            onNodeWithText("BlueThree").assertExists()
            captureImg("BlueThree")

            onNodeWithText("Red").performClick()
            onNodeWithText("Shade One").performClick()
            onNodeWithText("RedOne").assertExists()
            captureImg("RedOne")

            onNodeWithText("Shade Two").performClick()
            onNodeWithText("RedTwo").assertExists()
            captureImg("RedTwo")

            onNodeWithText("Shade Three").performClick()
            onNodeWithText("RedThree").assertExists()
            captureImg("RedThree")
        }
    }
}
