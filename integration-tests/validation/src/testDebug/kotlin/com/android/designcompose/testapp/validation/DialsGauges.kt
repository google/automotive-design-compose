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
import com.android.designcompose.testapp.validation.examples.DialsGaugesTest
import com.android.designcompose.testapp.validation.examples.ProgressConstraintsTest
import com.android.designcompose.testapp.validation.examples.ProgressVectorTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1920dp-h1500dp-xlarge-long-notround-any-xhdpi-keyshidden-nonav")
class DialsGauges {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontRule = InterFontTestRule()

    @Test
    fun progressTests() {
        with(composeTestRule) {
            setContent { DialsGaugesTest() }
            onNodeWithTag("angle").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("angle").performTouchInput { moveTo(Offset(-200f, 0f)) }
            onNodeWithTag("angle").performTouchInput { cancel() }
            captureRootRoboImage("angle-low")

            onNodeWithTag("angle").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("angle").performTouchInput { moveTo(Offset(400f, 0f)) }
            onNodeWithTag("angle").performTouchInput { cancel() }
            captureRootRoboImage("angle-high")

            onNodeWithTag("rotation").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("rotation").performTouchInput { moveTo(Offset(-200f, 0f)) }
            onNodeWithTag("rotation").performTouchInput { cancel() }
            captureRootRoboImage("rotation-low")

            onNodeWithTag("rotation").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("rotation").performTouchInput { moveTo(Offset(400f, 0f)) }
            onNodeWithTag("rotation").performTouchInput { cancel() }
            captureRootRoboImage("rotation-high")

            onNodeWithTag("progress-bar").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-bar").performTouchInput { moveTo(Offset(-200f, 0f)) }
            onNodeWithTag("progress-bar").performTouchInput { cancel() }
            captureRootRoboImage("progress-bar-low")

            onNodeWithTag("progress-bar").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-bar").performTouchInput { moveTo(Offset(400f, 0f)) }
            onNodeWithTag("progress-bar").performTouchInput { cancel() }
            captureRootRoboImage("progress-bar-high")

            onNodeWithTag("progress-indicator").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-indicator").performTouchInput { moveTo(Offset(-200f, 0f)) }
            onNodeWithTag("progress-indicator").performTouchInput { cancel() }
            captureRootRoboImage("progress-indicator-low")

            onNodeWithTag("progress-indicator").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-indicator").performTouchInput { moveTo(Offset(400f, 0f)) }
            onNodeWithTag("progress-indicator").performTouchInput { cancel() }
            captureRootRoboImage("progress-indicator-high")
        }
    }

    @Test
    fun progressVectorTests() {
        with(composeTestRule) {
            setContent { ProgressVectorTest() }

            onNodeWithTag("progress-bar").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-bar").performTouchInput { moveTo(Offset(-200f, 0f)) }
            onNodeWithTag("progress-bar").performTouchInput { cancel() }
            captureRootRoboImage("progress-vector-low")

            onNodeWithTag("progress-bar").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-bar").performTouchInput { moveTo(Offset(400f, 0f)) }
            onNodeWithTag("progress-bar").performTouchInput { cancel() }
            captureRootRoboImage("progress-vector-high")
        }
    }

    @Test
    fun progressConstraintsTests() {
        with(composeTestRule) {
            setContent {
                CompositionLocalProvider(
                    LocalDesignDocSettings provides DesignDocSettings(useSquoosh = true)
                ) {
                    ProgressConstraintsTest()
                }
            }

            onNodeWithTag("progress-bar").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-bar").performTouchInput { moveTo(Offset(-200f, 0f)) }
            onNodeWithTag("progress-bar").performTouchInput { cancel() }
            onNodeWithTag("progress-indicator").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-indicator").performTouchInput { moveTo(Offset(-200f, 0f)) }
            onNodeWithTag("progress-indicator").performTouchInput { cancel() }
            captureRootRoboImage("progress-constraints-low-small")

            onNodeWithTag("progress-bar").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-bar").performTouchInput { moveTo(Offset(400f, 0f)) }
            onNodeWithTag("progress-bar").performTouchInput { cancel() }
            onNodeWithTag("progress-indicator").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-indicator").performTouchInput { moveTo(Offset(400f, 0f)) }
            onNodeWithTag("progress-indicator").performTouchInput { cancel() }
            captureRootRoboImage("progress-constraints-high-small")

            onNodeWithTag("main-width").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("main-width").performTouchInput { moveTo(Offset(200f, 0f)) }
            onNodeWithTag("main-width").performTouchInput { cancel() }
            onNodeWithTag("main-height").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("main-height").performTouchInput { moveTo(Offset(200f, 0f)) }
            onNodeWithTag("main-height").performTouchInput { cancel() }
            captureRootRoboImage("progress-constraints-high-big")

            onNodeWithTag("progress-bar").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-bar").performTouchInput { moveTo(Offset(-400f, 0f)) }
            onNodeWithTag("progress-bar").performTouchInput { cancel() }
            onNodeWithTag("progress-indicator").performTouchInput { down(Offset.Zero) }
            onNodeWithTag("progress-indicator").performTouchInput { moveTo(Offset(-400f, 0f)) }
            onNodeWithTag("progress-indicator").performTouchInput { cancel() }
            captureRootRoboImage("progress-constraints-low-big")
        }
    }
}
