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
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.designcompose.DesignSettings
import com.android.designcompose.TestUtils
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

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
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<Array<Any>> {
            return EXAMPLES.filter { it.third != null }
                .map { arrayOf(it.first, it.second, it.third!!) }
        }
    }

    @Test
    fun testFetchAndRender() {
        composeTestRule.setContent(fileComposable)
        TestUtils.triggerLiveUpdate()

        composeTestRule.waitForIdle()
        with(DesignSettings.designDocStatuses[fileId]) {
            assertNotNull(this)
            assertNotNull(lastUpdateFromFetch)
            assertNotNull(lastFetch)
            assertThat(isRendered).isTrue()
        }
    }
}
