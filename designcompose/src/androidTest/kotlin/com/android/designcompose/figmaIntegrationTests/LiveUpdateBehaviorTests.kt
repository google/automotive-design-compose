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

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.android.designcompose.DesignSettings
import com.android.designcompose.HelloWorldDoc
import com.android.designcompose.TestUtils
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.docIdSemanticsKey
import com.android.designcompose.helloWorldDocId
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@DesignDoc(id = helloWorldDocId)
interface HelloWorldWrongNode {
    @DesignComponent(node = "#NonExistentNode", hideDesignSwitcher = true) fun nonExistentFrame()
}

/**
 * Tests different DesignDoc loading situations
 *
 * All tests require a Figma Token
 *
 * These tests can be excluded by running Gradle with:
 * -Pandroid.testInstrumentationRunnerArguments.notPackage=com.android.designcompose.figmaIntegrationTests
 */
class LiveUpdateBehaviorTests {
    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        InstrumentationRegistry.getInstrumentation().context.filesDir.deleteRecursively()
    }

    /**
     * Tests that HelloWorld can be fetched from Figma,
     *
     * This test will fail if you add HelloWorld to the assets for designcompose. Don't do that.
     */
    @Test
    fun fetchHelloWorld() {
        composeTestRule.setContent { HelloWorldDoc.mainFrame(name = "Testers!") }
        TestUtils.triggerLiveUpdate()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNode(SemanticsMatcher.expectValue(docIdSemanticsKey, helloWorldDocId))
            .assertExists()

        with(DesignSettings.testOnlyFigmaFetchStatus(helloWorldDocId)) {
            assertNotNull(this)
            assertNull(lastLoadFromDisk)
            assertNotNull(lastFetch)
            assertNotNull(lastUpdateFromFetch)
        }
    }

    /** Test for the correct behavior when a doc is loaded with a missing root node */
    @Test
    fun wrongNodeCausesRenderFailure() {
        composeTestRule.setContent { HelloWorldWrongNodeDoc.nonExistentFrame() }
        TestUtils.triggerLiveUpdate()
        composeTestRule.waitForIdle()

        // Check that...
        // No doc is rendered with this ID
        composeTestRule
            .onNode(SemanticsMatcher.keyIsDefined(docIdSemanticsKey))
            .assertDoesNotExist()

        // The Node not found screen is shown
        composeTestRule
            .onNodeWithText("Node \"#NonExistentNode\" not found", substring = true)
            .assertExists()

        // The doc was fetched, but doesn't report it was rendered.
        with(DesignSettings.testOnlyFigmaFetchStatus(helloWorldDocId)) {
            assertNotNull(this)
            assertNotNull(lastUpdateFromFetch)
            assertNotNull(lastFetch)
        }
    }
}
