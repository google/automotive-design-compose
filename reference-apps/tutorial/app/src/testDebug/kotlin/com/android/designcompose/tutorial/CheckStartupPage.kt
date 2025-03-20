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

package com.android.designcompose.tutorial

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.DesignDocOverride
import com.android.designcompose.TestUtils
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.test.Fetchable
import com.android.designcompose.test.assertHasText
import com.android.designcompose.test.onDCDoc
import com.android.designcompose.test.waitForContent
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Category(Fetchable::class)
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet, sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CheckStartupPage {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val liveUpdateTestRule = TestUtils.LiveUpdateTestRule()

    @get:Rule
    val roborazziRule =
        RoborazziRule(
            options = RoborazziRule.Options(outputDirectoryPath = "src/testDebug/roborazzi")
        )

    @Test
    fun testStartupPage() {
        with(composeTestRule) {
            setContent { TutorialMain() }
            with(onDCDoc(TutorialDoc)) {
                assertHasText(
                    "Congratulations on running the Automotive Design for Compose Tutorial app!",
                    substring = true,
                )
            }
            waitForContent(TutorialDoc.javaClass.name)
                .captureRoboImage(
                    "com.android.designcompose.tutorial.CheckStartupPage.testStartupPage.png"
                )
        }
    }

    /**
     * This test performs live update only because there is no design docs for
     * BX9UyUa5lkuSP3dEnqBdJf.
     */
    @Test
    fun testLiveUpdate() {
        with(composeTestRule) {
            setContent {
                DesignDocOverride(DesignDocId("BX9UyUa5lkuSP3dEnqBdJf")) { TutorialMain() }
            }

            liveUpdateTestRule
                .overrideDcfFileId("BX9UyUa5lkuSP3dEnqBdJf", "3z4xExq0INrL9vxPhj9tl7")
                .performLiveFetch()
            // There is no verification in the test. Because BX9UyUa5lkuSP3dEnqBdJf.dcf file is
            // missing. It will not render when it is not running live update.
        }
    }
}
