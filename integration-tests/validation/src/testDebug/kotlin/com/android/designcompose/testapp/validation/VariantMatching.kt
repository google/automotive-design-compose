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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.testapp.validation.examples.VariantPropertiesTest
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet, sdk = [34])
class VariantMatching {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)

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
            captureRootRoboImage("Background1Square")

            onNodeWithText("BG1 Circle").performClick()
            onNodeWithText("Background1Circle").assertExists()
            captureRootRoboImage("Background1Circle")

            onNodeWithText("BG2 Square").performClick()
            onNodeWithText("Background2Square").assertExists()
            captureRootRoboImage("Background2Square")

            onNodeWithText("BG2 Circle").performClick()
            onNodeWithText("Background2Circle").assertExists()
            captureRootRoboImage("Background2Circle")

            onNodeWithText("Inner Sharp").performClick()
            onNodeWithText("SquareBorder").assertExists()
            captureRootRoboImage("SquareBorder")

            onNodeWithText("Inner Curved").performClick()
            onNodeWithText("CurvedBorder").assertExists()
            captureRootRoboImage("CurvedBorder")
        }
    }

    @Test
    fun variantNestedMatches() {
        with(composeTestRule) {
            onNodeWithText("Curved").performClick()
            onNodeWithText("Comp1 One").performClick()
            onNodeWithText("Comp1One").assertExists()
            captureRootRoboImage("Comp1One")

            onNodeWithText("Comp1 Two").performClick()
            onNodeWithText("Comp1Two").assertExists()
            captureRootRoboImage("Comp1Two")

            onNodeWithText("Comp2 One").performClick()
            onNodeWithText("Comp2One").assertExists()
            captureRootRoboImage("Comp2One")

            onNodeWithText("Comp2 Two").performClick()
            onNodeWithText("Comp2Two").assertExists()
            captureRootRoboImage("Comp2Two")

            onNodeWithText("Comp3 One").performClick()
            onNodeWithText("C31").assertExists()
            captureRootRoboImage("C31")

            onNodeWithText("Comp3 Two").performClick()
            onNodeWithText("C32").assertExists()
            captureRootRoboImage("C32")

            onNodeWithText("Square").performClick()
            onNodeWithText("C34").assertExists()
            captureRootRoboImage("C34")

            onNodeWithText("Comp3 One").performClick()
            onNodeWithText("C33").assertExists()
            captureRootRoboImage("C33")
        }
    }

    @Test
    fun variantComponentSetsFound() {
        with(composeTestRule) {
            onNodeWithText("Green").performClick()
            onNodeWithText("Shade One").performClick()
            onNodeWithText("GreenOne").assertExists()
            captureRootRoboImage("GreenOne")

            onNodeWithText("Shade Two").performClick()
            onNodeWithText("GreenTwo").assertExists()
            captureRootRoboImage("GreenTwo")

            onNodeWithText("Shade Three").performClick()
            onNodeWithText("GreenThree").assertExists()
            captureRootRoboImage("GreenThree")

            onNodeWithText("Blue").performClick()
            onNodeWithText("Shade One").performClick()
            onNodeWithText("BlueOne").assertExists()
            captureRootRoboImage("BlueOne")

            onNodeWithText("Shade Two").performClick()
            onNodeWithText("BlueTwo").assertExists()
            captureRootRoboImage("BlueTwo")

            onNodeWithText("Shade Three").performClick()
            onNodeWithText("BlueThree").assertExists()
            captureRootRoboImage("BlueThree")

            onNodeWithText("Red").performClick()
            onNodeWithText("Shade One").performClick()
            onNodeWithText("RedOne").assertExists()
            captureRootRoboImage("RedOne")

            onNodeWithText("Shade Two").performClick()
            onNodeWithText("RedTwo").assertExists()
            captureRootRoboImage("RedTwo")

            onNodeWithText("Shade Three").performClick()
            onNodeWithText("RedThree").assertExists()
            captureRootRoboImage("RedThree")
        }
    }
}
