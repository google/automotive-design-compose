/*
 * Copyright 2024 Google LLC
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
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.TestUtils.ClearStateTestRule
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.test.R
import com.android.designcompose.test.assertRenderStatus
import com.android.designcompose.test.onDCDoc
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

const val helloWorldDocId = "pxVlixodJqZL95zo2RzTHl"

@DesignDoc(id = helloWorldDocId)
interface HelloWorld {
    @DesignComponent(node = "#MainFrame", hideDesignSwitcher = true)
    fun mainFrame(@Design(node = "#Name") name: String)
}

@Composable
fun HelloWorld() {
    HelloWorldDoc.mainFrame(name = "Testers!")
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.SmallPhone, sdk = [34])
class DesignRawResourceTest {
    // Must reset the DocServer and DesignSettings state to prevent reusing cached files
    @get:Rule val clearStateTestRule = ClearStateTestRule()
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testHelloWorldDoc_setRawResourceId_passes() {
        with(composeTestRule) {
            DesignSettings.setRawResourceId(R.raw.raw_resource_test_hello_world_doc)
            composeTestRule.setContent { HelloWorld() }

            onDCDoc(HelloWorldDoc).assertRenderStatus(DocRenderStatus.Rendered)
            onNodeWithText("Testers!", substring = true).assertExists()
            onNodeWithText("Hello", substring = true).assertExists()
        }
    }

    @Test
    fun testHelloWorldDoc_noRawResource_fails() {
        with(composeTestRule) {
            composeTestRule.setContent { HelloWorld() }

            onDCDoc(HelloWorldDoc).assertDoesNotExist()
        }
    }
}
