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
/** Temporarily disabled: GH-1945

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.TestUtils
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.test.waitForContent
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.VariantAnimationTimelineTest
import com.android.designcompose.testapp.validation.examples.VariantAnimationTimelineTestDoc
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1920dp-h1500dp-xlarge-long-notround-any-xhdpi-keyshidden-nonav")
class AnimationTimelines {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontRule = InterFontTestRule()

    @Test
    fun oneByOne() {
        // Because we're testing animation, we will manually advance the animation clock.
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent { VariantAnimationTimelineTest() }

        composeTestRule.waitForContent(VariantAnimationTimelineTestDoc.javaClass.name)

        composeTestRule.onNodeWithTag("OneByOne").performClick()
        composeTestRule.onNodeWithTag("Spread").performClick()

        recordAnimation("OneByOne")
    }

    @Test
    fun differentDurations() {
        // Because we're testing animation, we will manually advance the animation clock.
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent { VariantAnimationTimelineTest() }

        composeTestRule.waitForContent(VariantAnimationTimelineTestDoc.javaClass.name)

        composeTestRule.onNodeWithTag("DifferentDurations").performClick()
        composeTestRule.onNodeWithTag("Spread").performClick()

        recordAnimation("DifferentDurations")
    }

    @Test
    fun childCustomAnim() {
        // Because we're testing animation, we will manually advance the animation clock.
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent { VariantAnimationTimelineTest() }

        composeTestRule.waitForContent(VariantAnimationTimelineTestDoc.javaClass.name)

        composeTestRule.onNodeWithTag("OneByOne").performClick()
        composeTestRule.onNodeWithTag("ChildEarly").performClick()
        composeTestRule.onNodeWithTag("Spread").performClick()

        recordAnimation("ChildCustomAnim")
    }

    private fun recordAnimation(name: String) {
        // We need to both "advanceTimeByFrame" and then "waitForIdle" to capture the output after
        // recomposition.
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()

        composeTestRule.captureRootRoboImage("${name}Animation-Start")

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeBy(1000L)
        composeTestRule.waitForIdle()

        composeTestRule.captureRootRoboImage("${name}Animation-MidPoint")

        composeTestRule.mainClock.advanceTimeBy(2000L)
        composeTestRule.waitForIdle()
        composeTestRule.captureRootRoboImage("${name}Animation-End")
    }
}
*/
