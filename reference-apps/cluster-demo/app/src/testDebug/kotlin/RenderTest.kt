/*
 * Copyright 2025 Google LLC
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

package com.android.designcompose.tutorial

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.DocRenderStatus
import com.android.designcompose.TestUtils
import com.android.designcompose.test.Fetchable
import com.android.designcompose.test.assertRenderStatus
import com.android.designcompose.test.onDCDocAnyNode
import com.android.designcompose.testapp.clusterdemo.ClusterDoc
import com.android.designcompose.testapp.clusterdemo.MainFrame
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziRule
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Category(Fetchable::class)
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = RobolectricDeviceQualifiers.LargeDesktop, sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RenderTest {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val liveUpdateTestRule = TestUtils.LiveUpdateTestRule()

    @get:Rule
    val roborazziRule =
        RoborazziRule(
            composeRule = composeTestRule,
            // Specify the node to capture for the last image
            captureRoot = composeTestRule.onRoot(),
            options =
                RoborazziRule.Options(
                    outputDirectoryPath = "src/testDebug/roborazzi",
                    // Always capture the last image of the test
                    captureType = RoborazziRule.CaptureType.LastImage(),
                ),
        )

    @Test
    fun testLiveUpdate() {
        with(composeTestRule) {
            setContent { MainFrame(null) }

            liveUpdateTestRule.performLiveFetch()
            onDCDocAnyNode(ClusterDoc).onFirst().assertRenderStatus(DocRenderStatus.Rendered)
            onDCDocAnyNode(ClusterDoc).onLast().assertRenderStatus(DocRenderStatus.Rendered)
        }
    }
}
