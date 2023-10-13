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

package com.android.designcompose.testapp.helloworld

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.android.designcompose.DesignSettings
import com.android.designcompose.TestUtils
import com.android.designcompose.docClassSemanticsKey
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TestFetchAndRender {
    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        InstrumentationRegistry.getInstrumentation().context.filesDir.deleteRecursively()
    }

    @Test
    fun testHello() {
        composeTestRule.setContent { HelloWorldDoc.mainFrame(name = "Testers!") }
        TestUtils.triggerLiveUpdate()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNode(SemanticsMatcher.expectValue(docClassSemanticsKey, helloWorldDocId))
            .assertExists()

        with(DesignSettings.testOnlyFigmaFetchStatus(helloWorldDocId)) {
            assertNotNull(this)
            assertNotNull(lastLoadFromDisk)
            assertNotNull(lastFetch)
            assertNotNull(lastUpdateFromFetch)
        }

        composeTestRule.onNodeWithText("Testers!", substring = true).assertExists()
    }
}
