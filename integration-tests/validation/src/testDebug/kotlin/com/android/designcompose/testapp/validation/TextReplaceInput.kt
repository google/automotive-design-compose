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

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.TestUtils
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.TextReplaceTest
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
class TextReplaceInput {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontRule = InterFontTestRule()

    @Test
    fun textReplaceInput() {
        with(composeTestRule) {
            setContent { TextReplaceTest() }
            onNodeWithTag("TextInput").performTouchInput { click() }
            onNodeWithTag("TextInput")
                .performKeyPress(KeyEvent(NativeKeyEvent(0, Key.T.nativeKeyCode)))
            onNodeWithTag("TextInput")
                .performKeyPress(KeyEvent(NativeKeyEvent(0, Key.E.nativeKeyCode)))
            onNodeWithTag("TextInput")
                .performKeyPress(KeyEvent(NativeKeyEvent(0, Key.S.nativeKeyCode)))
            onNodeWithTag("TextInput")
                .performKeyPress(KeyEvent(NativeKeyEvent(0, Key.T.nativeKeyCode)))
            onNodeWithTag("TextInput")
                .performKeyPress(KeyEvent(NativeKeyEvent(0, Key.Spacebar.nativeKeyCode)))
            onNodeWithTag("TextInput")
                .performKeyPress(KeyEvent(NativeKeyEvent(0, Key.P.nativeKeyCode)))
            onNodeWithTag("TextInput")
                .performKeyPress(KeyEvent(NativeKeyEvent(0, Key.A.nativeKeyCode)))
            onNodeWithTag("TextInput")
                .performKeyPress(KeyEvent(NativeKeyEvent(0, Key.S.nativeKeyCode)))
            onNodeWithTag("TextInput")
                .performKeyPress(KeyEvent(NativeKeyEvent(0, Key.S.nativeKeyCode)))
            captureRootRoboImage("text-replace-input")
        }
    }
}
