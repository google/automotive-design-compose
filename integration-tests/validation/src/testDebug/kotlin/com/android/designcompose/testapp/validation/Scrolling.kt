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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.DesignDocSettings
import com.android.designcompose.LocalDesignDocSettings
import com.android.designcompose.TestUtils
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.ScrollingTest
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
class Scrolling {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontRule = InterFontTestRule()

    @Test
    fun scrollTests() {
        with(composeTestRule) {
            setContent {
                CompositionLocalProvider(
                    LocalDesignDocSettings provides DesignDocSettings(useSquoosh = true)
                ) {
                    ScrollingTest()
                }
            }
            onNodeWithTag("DragVertical").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("DragVertical").performTouchInput { moveTo(Offset(0f, -200F)) }
            onNodeWithTag("DragVertical").performTouchInput { cancel() }
            captureRootRoboImage("scroll-vertical")

            onNodeWithTag("DragHorizontal").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("DragHorizontal").performTouchInput { moveTo(Offset(-200f, 0F)) }
            onNodeWithTag("DragHorizontal").performTouchInput { cancel() }
            captureRootRoboImage("scroll-horizontal")
        }
    }
}
