/*
 * Copyright 2026 Google LLC
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
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.testapp.validation.examples.GradientOpacityTest
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

val GRADIENT_OPACITY_ROBORAZZI_OPTIONS =
    RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.05f))

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GradientOpacityValidation {
    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)

    @Test
    fun testGradientOpacity() {
        with(composeTestRule) {
            setContent { GradientOpacityTest() }
            onRoot()
                .captureRoboImage(
                    filePath = "gradient-opacity.png",
                    roborazziOptions = GRADIENT_OPACITY_ROBORAZZI_OPTIONS,
                )
        }
    }
}
