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

package com.android.designcompose.testapp.validation.figmaIntegrationTests

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.test.platform.app.InstrumentationRegistry
import com.android.designcompose.DesignSettings
import com.android.designcompose.TestUtils
import com.android.designcompose.docClassSemanticsKey
import com.android.designcompose.testapp.validation.EXAMPLES
import com.android.designcompose.testapp.validation.interFont
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Fetch and render examples
 *
 * Fetches and renders each example in Validation's example list.
 *
 * Properties are set by the Junit4 Parameterized runner via initializeTestData()
 *
 * @constructor Create empty Fetch and render examples
 * @property testName Human readable name for a the test
 * @property testComposable The composable to run
 * @property fileClass The classname of the DesignCompose DesignDoc that is being tested file)
 */
@RunWith(Parameterized::class)
class FetchAndRenderExamples(
    private val testName: String,
    private val testComposable: @Composable () -> Unit,
    internal val fileClass: String
) {
    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        // Clear any previously cached files
        InstrumentationRegistry.getInstrumentation().context.filesDir.deleteRecursively()
        DesignSettings.addFontFamily("Inter", interFont)
    }

    companion object {
        /**
         * initializeTestData
         *
         * Convert Validation's examples list of triples into a list of arrays, so that they can be
         * used as parameters for the tests
         *
         * @return List of test parameters for the Parameterized runner
         */
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initializeTestData(): List<Array<Any>> =
            EXAMPLES.filter { it.third != null }.map { arrayOf(it.first, it.second, it.third!!) }
    }

    @Test
    fun testFetchAndRender() {
        composeTestRule.setContent(testComposable)
        TestUtils.triggerLiveUpdate()

        composeTestRule.waitForIdle()
        // Check that at least one node has the doc's class set correctly (some tests use display
        // multiple instances)
        composeTestRule
            .onAllNodes(SemanticsMatcher.expectValue(docClassSemanticsKey, config.fileClass))
            .onFirst()
            .assertExists()

        with(DesignSettings.testOnlyFigmaFetchStatus(fileId)) {
            assertNotNull(this)
            assertNotNull(lastUpdateFromFetch)
            assertNotNull(lastFetch)
        }
    }
}
