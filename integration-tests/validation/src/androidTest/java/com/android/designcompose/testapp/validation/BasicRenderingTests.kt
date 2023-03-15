/*
 * Copyright 2023 Google LLC
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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.DesignSettings
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BasicRenderingTests {
    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        DesignSettings.addFontFamily("Inter", interFont)
    }

    @Test
    fun testHello() {
        composeTestRule.setContent { HelloWorld() }
        composeTestRule.onNodeWithText("World", substring = true).assertIsDisplayed()
    }

    @Test
    fun testTelltale() {
        composeTestRule.setContent { TelltaleTest() }
        composeTestRule.onNodeWithText("Frames").assertIsDisplayed()
    }
}
