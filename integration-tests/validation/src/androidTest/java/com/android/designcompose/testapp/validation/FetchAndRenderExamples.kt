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

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.android.designcompose.DesignSettings
import com.android.designcompose.TestUtils
import com.android.designcompose.docIdSemanticsKey
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertNotNull

// Give up to 40 seconds for the doc to load
const val fetchTimeoutMS: Long = 40000

@RunWith(Parameterized::class)
class FetchAndRenderExamples(
    private val fileName: String,
    private val fileComposable: @Composable () -> Unit,
    private val fileId: String
) {
    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        DesignSettings.addFontFamily("Inter", interFont)
        TestUtils.enableTestingLiveUpdate()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<Array<Any>> {
            return EXAMPLES.filter { it.third != null }
                .map { arrayOf(it.first, it.second, it.third!!) }
        }

        @JvmStatic
        @BeforeClass
        fun setUpLiveUpdate(): Unit {
            // Clear any previously fetched files (doesn't clear files form assets)
            InstrumentationRegistry.getInstrumentation().context.filesDir.deleteRecursively()
            TestUtils.enableTestingLiveUpdate()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFetchAndRender() {
        composeTestRule.setContent(fileComposable)

        // Wait until the doc is displayed and we've received the first fetch of the file.
        composeTestRule.waitUntilAtLeastOneExists(
            SemanticsMatcher.expectValue(docIdSemanticsKey, fileId),
            timeoutMillis = fetchTimeoutMS
        )
        composeTestRule.waitUntil(timeoutMillis = fetchTimeoutMS){
            DesignSettings.designDocStatuses[fileId]?.lastUpdateFromFetch != null
        }

        with(DesignSettings.designDocStatuses[fileId]) {
            assertNotNull(this)
            assertNotNull(lastFetch)
            assertThat(isRendered).isTrue()
        }
    }
}
