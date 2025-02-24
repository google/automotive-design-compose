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
import androidx.compose.animation.core.spring
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.DesignDocSettings
import com.android.designcompose.LocalDesignDocSettings
import com.android.designcompose.TestUtils
import com.android.designcompose.squoosh.SmartAnimateTransition
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.test.waitForContent
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.SmartAnimateTestDoc
import com.android.designcompose.testapp.validation.examples.State
import com.android.designcompose.testapp.validation.examples.TestState
import com.android.designcompose.testapp.validation.examples.VariantAnimationTestDoc
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlin.math.sqrt
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
            VariantAnimationTestDoc.MainFrame(state = state.value, text = text.value)
        }

        composeTestRule.waitForContent(VariantAnimationTestDoc.javaClass.name)

        state.value = State.Y
        text.value = "Y"

        recordAnimation("Variant")
    }

    @Test
    fun customVariantAnimation() {
        // Because we're testing animation, we will manually advance the animation clock.
        composeTestRule.mainClock.autoAdvance = false

        val state = mutableStateOf(State.X)
        val text = mutableStateOf("X")

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDesignDocSettings provides
                    DesignDocSettings(
                        customVariantTransition = {
                            val mass = 1.0f
                            val stiffness = 120.0f
                            val critical = sqrt(4.0f * stiffness * mass)
                            val damping = 30.0f
                            SmartAnimateTransition(
                                spring(dampingRatio = damping / critical, stiffness = stiffness)
                            )
                        }
                    )
            ) {
                VariantAnimationTestDoc.MainFrame(state = state.value, text = text.value)
            }
        }

        composeTestRule.waitForContent(VariantAnimationTestDoc.javaClass.name)

        state.value = State.Y
        text.value = "Y"

        recordAnimation("CustomVariantTransition")
    }

    @Test
    fun allSameName() {
        // Because we're testing animation, we will manually advance the animation clock.
        composeTestRule.mainClock.autoAdvance = false

        val state = mutableStateOf(State.X)

        composeTestRule.setContent { VariantAnimationTestDoc.AllSameName(state = state.value) }

        composeTestRule.waitForContent(VariantAnimationTestDoc.javaClass.name)

        state.value = State.Y

        recordAnimation("AllSameName")
    }

    @Test
    fun rotationAnimation() {
        // Because we're testing animation, we will manually advance the animation clock.
        composeTestRule.mainClock.autoAdvance = false

        val state = mutableStateOf(TestState.A)

        composeTestRule.setContent { SmartAnimateTestDoc.RotationTest(state = state.value) }

        composeTestRule.waitForContent(SmartAnimateTestDoc.javaClass.name)

        state.value = TestState.B

        recordAnimation("Rotation")
    }

    @Test
    fun opacityAnimation() {
        // Because we're testing animation, we will manually advance the animation clock.
        composeTestRule.mainClock.autoAdvance = false

        val state = mutableStateOf(TestState.A)

        composeTestRule.setContent { SmartAnimateTestDoc.OpacityTest(state = state.value) }

        composeTestRule.waitForContent(SmartAnimateTestDoc.javaClass.name)

        state.value = TestState.B

        recordAnimation("Opacity")
    }

    @Test
    fun textAnimation() {
        // Because we're testing animation, we will manually advance the animation clock.
        composeTestRule.mainClock.autoAdvance = false

        val state = mutableStateOf(TestState.A)

        composeTestRule.setContent { SmartAnimateTestDoc.TextTest(state = state.value) }

        composeTestRule.waitForContent(SmartAnimateTestDoc.javaClass.name)

        state.value = TestState.B

        recordAnimation("Text")
    }

    @Test
    fun vectorAnimation() {
        // Because we're testing animation, we will manually advance the animation clock.
        composeTestRule.mainClock.autoAdvance = false

        val state = mutableStateOf(TestState.A)

        composeTestRule.setContent { SmartAnimateTestDoc.VectorTest(state = state.value) }

        composeTestRule.waitForContent(SmartAnimateTestDoc.javaClass.name)

        state.value = TestState.B

        recordAnimation("VectorTest")
    }

    @Test
    fun crossFadeOpacityAnimation() {
        // Because we're testing animation, we will manually advance the animation clock.
        composeTestRule.mainClock.autoAdvance = false

        val state = mutableStateOf(TestState.A)

        composeTestRule.setContent { SmartAnimateTestDoc.CrossFadeOpacityTest(state = state.value) }

        composeTestRule.waitForContent(SmartAnimateTestDoc.javaClass.name)

        state.value = TestState.B

        recordAnimation("CrossFadeOpacityTest")
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

    @Test
    fun OnPressAnimation() {
        with(composeTestRule) {
            // Because we're testing animation, we will manually advance the animation clock.
            mainClock.autoAdvance = false

            setContent { SmartAnimateTestDoc.OnPressTest() }

            waitForContent(SmartAnimateTestDoc.javaClass.name)

            // Touch red square to start onPress anim, capture screen mid anim changing to blue
            onNodeWithTag("V1").performTouchInput { down(Offset.Zero) }
            mainClock.advanceTimeBy(100L)
            captureRootRoboImage("VariantOnPress-anim1")

            // Capture screen after anim done (blue)
            mainClock.advanceTimeBy(400L)
            captureRootRoboImage("VariantOnPress-anim2")

            // Click to change variant (green)
            onNodeWithTag("#varcolor=blue").performTouchInput { up() }
            mainClock.advanceTimeByFrame()
            captureRootRoboImage("VariantOnPress-anim3")

            // Touch purple square to start onPress anim, which changes to green while pressed.
            // Capture this mid animation to ensure there is no layout animation from the other
            // green square
            onNodeWithTag("V2").performTouchInput { down(Offset.Zero) }
            mainClock.advanceTimeBy(100L)
            captureRootRoboImage("VariantOnPress-anim4")
        }
    }
}
