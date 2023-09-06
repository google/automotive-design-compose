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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

const val helloWorldDocId = "pxVlixodJqZL95zo2RzTHl"
const val helloWorldFileName = "HelloWorldDoc_$helloWorldDocId.dcf"
const val fetchTimeoutMS: Long = 30000

@DesignDoc(id = helloWorldDocId)
interface HelloWorld {
    @DesignComponent(node = "#MainFrame") fun mainFrame(@Design(node = "#Name") name: String)
}

class FetchAndRender {
    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        TestUtils.enableTestingLiveUpdate()
    }

    @Test
    fun loadDesignSwitcherFromDisk() {
        composeTestRule.setContent {
            DesignSwitcher(doc = null, currentDocId = "DEADBEEF", branchHash = null, setDocId = {})
        }
        composeTestRule.waitForIdle()
        with(DesignSettings.fileStatuses[designSwitcherDocId()]) {
            assertNotNull(this)
            assertNotNull(lastLoadFromDisk)
            assertNull(lastFetch)
            assertNull(lastUpdateFromFetch)
        }
    }

    /**
     * Fetch hello world
     *
     * Tests that HelloWorld can be fetched from Figma,
     *
     * This test will fail if you add HelloWorld to the assets for designcompose. Don't do that.
     */
    @Test
    fun fetchHelloWorld() {
        // Clear any previous HelloWorld file (doesn't clear files form assets)
        InstrumentationRegistry.getInstrumentation().context.deleteFile(helloWorldFileName)
        composeTestRule.setContent { HelloWorldDoc.mainFrame(name = "Testers!") }
        composeTestRule.waitUntil(fetchTimeoutMS) {
            DesignSettings.fileStatuses[helloWorldDocId]?.lastFetch != null
        }
        with(DesignSettings.fileStatuses[helloWorldDocId]) {
            assertNotNull(this)
            assertNull(lastLoadFromDisk)
            assertNotNull(lastUpdateFromFetch)
        }
    }
}
