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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.android.designcompose.DocRenderStatus
import com.android.designcompose.HelloWorldDoc
import com.android.designcompose.HelloWorldWrongNodeDoc
import com.android.designcompose.TestUtils
import com.android.designcompose.test.assertDCRenderStatus
import com.android.designcompose.test.assertRenderStatus
import com.android.designcompose.test.onDCDoc
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DocRenderSemanticBehavior {
    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        // Clear any previously cached files
        InstrumentationRegistry.getInstrumentation().context.filesDir.deleteRecursively()
    }

    @Test
    fun docIsRenderedAfterLoad() {
        with(composeTestRule) {
            setContent { HelloWorldDoc.mainFrame(name = "Testers!") }
            TestUtils.triggerLiveUpdate()
            onDCDoc(HelloWorldDoc).assertRenderStatus(DocRenderStatus.Rendered)
        }
    }

    @Test
    fun docWithBadNodeSaysNodeNotFound() {
        with(composeTestRule) {
            setContent { HelloWorldWrongNodeDoc.nonExistentFrame() }
            TestUtils.triggerLiveUpdate()
            onDCDoc(HelloWorldWrongNodeDoc).assertDoesNotExist()
            assertDCRenderStatus(DocRenderStatus.NodeNotFound)
        }
    }
}
