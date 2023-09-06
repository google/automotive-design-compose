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

package com.android.designcompose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Rule
import org.junit.Test

class RenderTests {

    @get:Rule val composeTestRule = createComposeRule()

    /**
     * Test that the DesignSwitcher will load from the disk and render. Test will fail if the doc
     * fails to deserialize
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loadDesignSwitcherFromDisk() {
        composeTestRule.setContent {
            DesignSwitcher(doc = null, currentDocId = "DEADBEEF", branchHash = null, setDocId = {})
        }
        composeTestRule.waitUntilExactlyOneExists(
            SemanticsMatcher.expectValue(docIdSemanticsKey, designSwitcherDocId()),
            timeoutMillis = 1000
        )
        with(DesignSettings.designDocStatuses[designSwitcherDocId()]) {
            assertNotNull(this)
            assertNotNull(lastLoadFromDisk)
            assertNull(lastFetch)
            assertNull(lastUpdateFromFetch)
            assertThat(isRendered).isTrue()
        }
    }

    /** Test that a missing serialized file will cause the correct behavior */
    @Test
    fun missingSerializedFileDoesNotRender() {
        // Clear any previous HelloWorld file (doesn't clear files form assets)
        InstrumentationRegistry.getInstrumentation().context.filesDir.deleteRecursively()

        composeTestRule.setContent { HelloWorldDoc.mainFrame(name = "No one") }
        composeTestRule.waitUntil(timeoutMillis = 1000) {
            DesignSettings.designDocStatuses[helloWorldDocId] != null
        }

        composeTestRule.waitForIdle()
        // No doc is rendered with this ID
        composeTestRule
            .onNode(SemanticsMatcher.keyIsDefined(docIdSemanticsKey))
            .assertDoesNotExist()

        // The Node not found screen is shown
        composeTestRule
            .onNodeWithText("Document $helloWorldDocId not available", substring = true)
            .assertExists()

        // It was not loaded from disk and did not render
        with(DesignSettings.designDocStatuses[helloWorldDocId]) {
            assertNotNull(this)
            // What we're testing
            assertNull(lastLoadFromDisk)
            assertNull(lastFetch)
            assertNull(lastUpdateFromFetch)
            assertThat(isRendered).isFalse()
        }
    }
}
