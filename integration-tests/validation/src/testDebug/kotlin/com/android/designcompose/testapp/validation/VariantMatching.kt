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
import com.android.designcompose.TestUtils
import com.android.designcompose.test.assertHasText
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.test.onDCDocRoot
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.VariantPropertiesTest
import com.android.designcompose.testapp.validation.examples.VariantPropertiesTestDoc
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
class VariantMatching {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontRule = InterFontTestRule()

    @Before
    fun setup() {
        with(composeTestRule) { setContent { VariantPropertiesTest() } }
    }

    @Test
    fun variantBackgroundMatches() {
        with(composeTestRule) {
            with(onDCDocRoot(VariantPropertiesTestDoc)) {
                onNodeWithText("Green").performClick()
                onNodeWithText("BG1 Square").performClick()
                assertHasText("Background1Square")
                captureRootRoboImage("Background1Square")

                onNodeWithText("BG1 Circle").performClick()
                assertHasText("Background1Circle")
                captureRootRoboImage("Background1Circle")

                onNodeWithText("BG2 Square").performClick()
                assertHasText("Background2Square")
                captureRootRoboImage("Background2Square")

                onNodeWithText("BG2 Circle").performClick()
                assertHasText("Background2Circle")
                captureRootRoboImage("Background2Circle")

                onNodeWithText("Inner Sharp").performClick()
                assertHasText("SquareBorder")
                captureRootRoboImage("SquareBorder")

                onNodeWithText("Inner Curved").performClick()
                assertHasText("CurvedBorder")
                captureRootRoboImage("CurvedBorder")
            }
        }
    }

    @Test
    fun variantNestedMatches() {
        with(composeTestRule) {
            with(onDCDocRoot(VariantPropertiesTestDoc)) {
                onNodeWithText("Curved").performClick()
                onNodeWithText("Comp1 One").performClick()
                assertHasText("Comp1One")
                captureRootRoboImage("Comp1One")

                onNodeWithText("Comp1 Two").performClick()
                assertHasText("Comp1Two")
                captureRootRoboImage("Comp1Two")

                onNodeWithText("Comp2 One").performClick()
                assertHasText("Comp2One")
                captureRootRoboImage("Comp2One")

                onNodeWithText("Comp2 Two").performClick()
                assertHasText("Comp2Two")
                captureRootRoboImage("Comp2Two")

                onNodeWithText("Comp3 One").performClick()
                assertHasText("C31")
                captureRootRoboImage("C31")

                onNodeWithText("Comp3 Two").performClick()
                assertHasText("C32")
                captureRootRoboImage("C32")

                onNodeWithText("Square").performClick()
                assertHasText("C34")
                captureRootRoboImage("C34")

                onNodeWithText("Comp3 One").performClick()
                assertHasText("C33")
                captureRootRoboImage("C33")
            }
        }
    }

    @Test
    fun variantComponentSetsFound() {
        with(composeTestRule) {
            with(onDCDocRoot(VariantPropertiesTestDoc)) {
                onNodeWithText("Green").performClick()
                onNodeWithText("Shade One").performClick()
                assertHasText("GreenOne")
                captureRootRoboImage("GreenOne")

                onNodeWithText("Shade Two").performClick()
                assertHasText("GreenTwo")
                captureRootRoboImage("GreenTwo")

                onNodeWithText("Shade Three").performClick()
                assertHasText("GreenThree")
                captureRootRoboImage("GreenThree")

                onNodeWithText("Blue").performClick()
                onNodeWithText("Shade One").performClick()
                assertHasText("BlueOne")
                captureRootRoboImage("BlueOne")

                onNodeWithText("Shade Two").performClick()
                assertHasText("BlueTwo")
                captureRootRoboImage("BlueTwo")

                onNodeWithText("Shade Three").performClick()
                assertHasText("BlueThree")
                captureRootRoboImage("BlueThree")

                onNodeWithText("Red").performClick()
                onNodeWithText("Shade One").performClick()
                assertHasText("RedOne")
                captureRootRoboImage("RedOne")

                onNodeWithText("Shade Two").performClick()
                assertHasText("RedTwo")
                captureRootRoboImage("RedTwo")

                onNodeWithText("Shade Three").performClick()
                assertHasText("RedThree")
                captureRootRoboImage("RedThree")
            }
        }
    }
}
