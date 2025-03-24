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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.TestUtils
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.test.onDCDocAnyNode
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.ComponentOverridesTest
import com.android.designcompose.testapp.validation.examples.ComponentOverridesTestDoc
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1920dp-h1500dp-xlarge-long-notround-any-xhdpi-keyshidden-nonav")
class ComponentOverrides {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontRule = InterFontTestRule()

    @Before
    fun setup() {
        with(composeTestRule) { setContent { ComponentOverridesTest() } }
    }

    @Test
    fun testComponentSameText_overrideText() {
        with(composeTestRule) {
            with(onDCDocAnyNode(ComponentOverridesTestDoc)) {
                onNodeWithTag("#hello").performClick()
                captureRootRoboImage("sameText_overrideText")
            }
        }
    }

    @Test
    fun testComponentDifferentTexts_overrideText() {
        with(composeTestRule) {
            with(onDCDocAnyNode(ComponentOverridesTestDoc)) {
                onNodeWithTag("#bye-auto-layout-hug-override-text").performClick()
                captureRootRoboImage("differentTexts_overrideText")
            }
        }
    }

    @Test
    fun testComponentHug_overrideToFixedWidth() {
        with(composeTestRule) {
            with(onDCDocAnyNode(ComponentOverridesTestDoc)) {
                onNodeWithTag("#bye-auto-layout-hug-override-to-fixed-size").performClick()
                captureRootRoboImage("hug_overrideToFixedWidth")
            }
        }
    }

    @Test
    fun testComponentFixedWidth_overrideWidth() {
        with(composeTestRule) {
            with(onDCDocAnyNode(ComponentOverridesTestDoc)) {
                onNodeWithTag("#yes-auto-layout-fixed-width-override-width").performClick()
                captureRootRoboImage("fixedWidth_overrideWidth")
            }
        }
    }

    @Test
    fun testFrameComponent_overrideSize() {
        with(composeTestRule) {
            with(onDCDocAnyNode(ComponentOverridesTestDoc)) {
                onNodeWithTag("#frame").performClick()
                captureRootRoboImage("frame_overrideSize")
            }
        }
    }

    @Test
    fun testFrameComponent_overrideStroke() {
        with(composeTestRule) {
            with(onDCDocAnyNode(ComponentOverridesTestDoc)) {
                onNodeWithTag("#frame2").performClick()
                captureRootRoboImage("frame_overrideStroke")
            }
        }
    }

    @Test
    fun testFrameComponent_overrideFills() {
        with(composeTestRule) {
            with(onDCDocAnyNode(ComponentOverridesTestDoc)) {
                onNodeWithTag("#frame3").performClick()
                captureRootRoboImage("frame_overrideFills")
            }
        }
    }

    @Test
    fun testFrameComponent_overrideChildFills() {
        with(composeTestRule) {
            with(onDCDocAnyNode(ComponentOverridesTestDoc)) {
                onNodeWithTag("#frame-nested").performClick()
                captureRootRoboImage("frame_overrideChildFills")
            }
        }
    }

    @Test
    fun testComponent_overrideTextProperty() {
        with(composeTestRule) {
            with(onDCDocAnyNode(ComponentOverridesTestDoc)) {
                onNodeWithTag("#button").performTouchInput { down(center) }
                captureRootRoboImage("button_overrideTextProperty")
            }
        }
    }
}
