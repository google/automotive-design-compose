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

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.tooling.preview.Preview
import androidx.test.platform.app.InstrumentationRegistry
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.test.assertDCRenderStatus
import com.android.designcompose.test.onDCDoc
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@Preview
@Composable
fun DesignSwitcherDeadbeef() {
    DesignSwitcher(
        doc = null,
        currentDocId = DesignDocId("DEADBEEF"),
        branchHash = null,
        setDocId = {},
    )
}

/**
 * Render tests
 *
 * Test behavior of loading and rendering files from storage Does not use live update or a Figma
 * token
 */
class RenderTests {
    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        // Clear any files from previous test runs
        InstrumentationRegistry.getInstrumentation().context.filesDir.deleteRecursively()
    }

    /**
     * Test that the DesignSwitcher will load from the disk and render. Test will fail if the doc
     * fails to deserialize
     */
    @Test
    fun loadDesignSwitcherFromDisk() {
        composeTestRule.setContent { DesignSwitcherDeadbeef() }

        composeTestRule.waitForIdle()

        composeTestRule.onDCDoc(DesignSwitcherDoc).assertExists()
        with(DesignSettings.testOnlyFigmaFetchStatus(designSwitcherDocId())) {
            assertNotNull(this)
            assertNotNull(lastLoadFromDisk)
            assertNull(lastFetch)
            assertNull(lastUpdateFromFetch)
        }
    }

    /** Test that a missing serialized file will cause the correct behavior */
    @Test
    fun missingSerializedFileDoesNotRender() {
        with(composeTestRule) {
            setContent { HelloWorldDoc.mainFrame(name = "No one") }

            onDCDoc(HelloWorldDoc).assertDoesNotExist()
            assertDCRenderStatus(DocRenderStatus.NotAvailable)

            // The Node not found screen is shown
            // TODO resurrect this test once squoosh renders something for missing nodes
            // onDCDoc(HelloWorldDoc).assertHasText("Document $helloWorldDocId not available",
            // substring = true)
        }
        // It was not loaded from disk and did not render
        with(DesignSettings.testOnlyFigmaFetchStatus(DesignDocId(helloWorldDocId))) {
            assertNotNull(this)
            assertNull(lastLoadFromDisk)
            assertNull(lastFetch)
            assertNull(lastUpdateFromFetch)
        }
    }
}
