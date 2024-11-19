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

package com.android.designcompose.testapp.validation

import android.annotation.TargetApi
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.TestUtils
import com.android.designcompose.test.Fetchable
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.test.waitForContent
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.StateCustomizationsDoc
import com.android.designcompose.testapp.validation.examples.StateCustomizationsTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Category(Fetchable::class)
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w1920dp-h1500dp-xlarge-long-notround-any-xhdpi-keyshidden-nonav")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StateCustomizationsUnitTest {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)

    @get:Rule val interFontTestRule = InterFontTestRule()

    @get:Rule val liveUpdateTestRule = TestUtils.LiveUpdateTestRule()

    @Test
    @TargetApi(26)
    fun testTextCustomization() {
        with(composeTestRule) {
            val fixedClock: Clock =
                Clock.fixed(Instant.parse("2023-12-25T10:15:30Z"), ZoneId.of("UTC"))
            setContent { StateCustomizationsTest(fixedClock) }

            liveUpdateTestRule.performLiveFetch()
            waitForContent(StateCustomizationsDoc.javaClass.name)

            captureRootRoboImage("State-Customizations")
        }
    }
}
