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

package com.android.designcompose.figmaIntegration

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.android.designcompose.TestUtils
import org.junit.BeforeClass
import org.junit.Rule

const val fetchTimeoutMS: Long = 30000

/**
 * Base class for the DesignCompose library Live Update tests. Ensures that any previously cached
 * files are deleted and enables Live Update.
 */
open class BaseLiveUpdateTest {
    @get:Rule val composeTestRule = createComposeRule()
    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpLiveUpdate(): Unit {
            // Clear any previously fetched files (doesn't clear files form assets)
            InstrumentationRegistry.getInstrumentation().context.filesDir.deleteRecursively()
            TestUtils.enableTestingLiveUpdate()
        }
    }
}
