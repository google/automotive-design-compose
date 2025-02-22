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

package com.android.designcompose.tutorial

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.DesignSettings
import com.android.designcompose.test.assertHasText
import com.android.designcompose.test.onDCDoc
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CheckStartupPage {
    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        DesignSettings.addFontFamily("GoogleSans", googleSansFont)
    }

    @Test
    fun testStartupPage() {
        composeTestRule.setContent { TutorialMain() }
        composeTestRule.onRoot().printToLog("TAG")

        with(composeTestRule) {
            with(onDCDoc(TutorialDoc)) {
                assertHasText(
                    "Congratulations on running the Automotive Design for Compose Tutorial app!",
                    substring = true,
                )
            }
        }
    }
}
