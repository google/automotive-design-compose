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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.TestUtils
import com.android.designcompose.test.assertHasText
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.test.onDCDocAnyNode
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.ComponentTapCallbackDoc
import com.android.designcompose.testapp.validation.examples.ComponentTapCallbackTest
import com.android.designcompose.testapp.validation.examples.LayoutReplacementTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w1920dp-h1500dp-xlarge-long-notround-any-xhdpi-keyshidden-nonav")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TapCallbackTest {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontTestRule = InterFontTestRule()

    @Test
    fun componentInstanceTapCallbackTest() {
        with(composeTestRule) {
            setContent { ComponentTapCallbackTest() }
            with(onDCDocAnyNode(ComponentTapCallbackDoc)) {
                assertHasText("Parked")
                onNodeWithTag("Parked").performClick()
                assertHasText("Reverse")
                captureRootRoboImage("ComponentTapCallbackTest-tapped")
            }
        }
    }

    @Test
    fun layoutReplacementTapCallbackTest() {
        with(composeTestRule) {
            setContent { LayoutReplacementTest() }
            captureRootRoboImage("LayoutReplacement-1")
            onNodeWithTag("#next").performClick()
            captureRootRoboImage("LayoutReplacement-2")
            onNodeWithTag("#next").performClick()
            captureRootRoboImage("LayoutReplacement-3")
        }
    }
}
