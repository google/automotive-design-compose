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

package com.android.designcompose.figmaIntegrationTests

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithText
import com.android.designcompose.DesignSettings
import com.android.designcompose.HelloWorldDoc
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.docIdSemanticsKey
import com.android.designcompose.helloWorldDocId
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test

/**
 * Tests different DesignDoc loading situations
 *
 * All tests require a Figma Token
 */
class LiveUpdateBehaviorTests : BaseLiveUpdateTest() {

    /**
     * Tests that HelloWorld can be fetched from Figma,
     *
     * This test will fail if you add HelloWorld to the assets for designcompose. Don't do that.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun LiveUpdateFetchHellowWorldTest() {
        composeTestRule.setContent { HelloWorldDoc.mainFrame(name = "Testers!") }
        composeTestRule.waitUntilExactlyOneExists(
            SemanticsMatcher.expectValue(docIdSemanticsKey, helloWorldDocId),
            timeoutMillis = fetchTimeoutMS
        )
        with(DesignSettings.designDocStatuses[helloWorldDocId]) {
            assertNotNull(this)
            assertNull(lastLoadFromDisk)
            assertNotNull(lastFetch)
            assertNotNull(lastUpdateFromFetch)
            assertThat(isRendered).isTrue()
        }
    }
}

@DesignDoc(id = helloWorldDocId)
interface HelloWorldWrongNode {
    @DesignComponent(node = "#NonExistentNode", hideDesignSwitcher = true) fun nonExistentFrame()
}

/**
 * Test for the correct behavior when a doc is loaded with a missing root node
 *
 * This needs to be a separate class than the DesignDocStatusTests. It also uses the HelloWorld ID,
 * which means that it would pick up and use a previously cached version of it. (I tried to figure
 * out how to clean up the cached docs from an earlier test run, but ran into problems trying to
 * wait for the DocServer thread to finish so that I could clear the file
 */
class LiveUpdateWrongNodeTest : BaseLiveUpdateTest() {
    @Test
    fun wrongNodeCausesRenderFailure() {
        composeTestRule.setContent { HelloWorldWrongNodeDoc.nonExistentFrame() }
        composeTestRule.waitUntil(fetchTimeoutMS) {
            DesignSettings.designDocStatuses[helloWorldDocId]?.lastFetch != null
        }

        composeTestRule.waitForIdle()
        // No doc is rendered with this ID
        composeTestRule
            .onNode(SemanticsMatcher.keyIsDefined(docIdSemanticsKey))
            .assertDoesNotExist()

        // The Node not found screen is shown
        composeTestRule
            .onNodeWithText("Node \"#NonExistentNode\" not found", substring = true)
            .assertExists()

        // Make sure isRendered is false
        assertThat(DesignSettings.designDocStatuses[helloWorldDocId]?.isRendered).isFalse()
    }
}
