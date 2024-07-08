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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.DesignDocSettings
import com.android.designcompose.DocRenderStatus
import com.android.designcompose.LocalDesignDocSettings
import com.android.designcompose.TestUtils
import com.android.designcompose.docClassSemanticsKey
import com.android.designcompose.test.assertRenderStatus
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.SmartAnimateTestDoc
import com.android.designcompose.testapp.validation.examples.State
import com.android.designcompose.testapp.validation.examples.TestState
import com.android.designcompose.testapp.validation.examples.VariantAnimationTestDoc
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
class AnimationMidpoints {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontRule = InterFontTestRule()

    @Test
    fun variantAnimation() {
        // Because we're testing animation, we will manually advance the animation clock.
        composeTestRule.mainClock.autoAdvance = false

        val state = mutableStateOf(State.X)
        val text = mutableStateOf("X")

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDesignDocSettings provides DesignDocSettings(useSquoosh = true)
            ) {
                VariantAnimationTestDoc.MainFrame(state = state.value, text = text.value)
            }
        }

        waitForContent(VariantAnimationTestDoc.javaClass.name)

        state.value = State.Y
        text.value = "Y"

        recordAnimation("Variant")
    }

    @Test
    fun allSameName() {
        // Because we're testing animation, we will manually advance the animation clock.
        composeTestRule.mainClock.autoAdvance = false

        val state = mutableStateOf(State.X)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDesignDocSettings provides DesignDocSettings(useSquoosh = true)
            ) {
                VariantAnimationTestDoc.AllSameName(state = state.value)
            }
        }

        waitForContent(VariantAnimationTestDoc.javaClass.name)

        state.value = State.Y

        recordAnimation("AllSameName")
    }

    @Test
    fun rotationAnimation() {
        // Because we're testing animation, we will manually advance the animation clock.
        composeTestRule.mainClock.autoAdvance = false

        val state = mutableStateOf(TestState.A)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDesignDocSettings provides DesignDocSettings(useSquoosh = true)
            ) {
                SmartAnimateTestDoc.RotationTest(state = state.value)
            }
        }

        waitForContent(SmartAnimateTestDoc.javaClass.name)

        state.value = TestState.B

        recordAnimation("Rotation")
    }

    private fun waitForContent(name: String) {
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodes(
                SemanticsMatcher.expectValue(
                    docClassSemanticsKey,
                    name,
                )
            )
            .onFirst()
            .assertRenderStatus(DocRenderStatus.Rendered)
    }

    private fun recordAnimation(name: String) {
        // We need to both "advanceTimeByFrame" and then "waitForIdle" to capture the output after
        // recomposition.
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()

        composeTestRule.captureRootRoboImage("${name}Animation-Start")

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeBy(100L)
        composeTestRule.waitForIdle()

        composeTestRule.captureRootRoboImage("${name}Animation-MidPoint")

        composeTestRule.mainClock.advanceTimeBy(900L)
        composeTestRule.waitForIdle()
        composeTestRule.captureRootRoboImage("${name}Animation-End")
    }
}
