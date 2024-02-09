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
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.test.internal.ROBO_CAPTURE_DIR
import com.android.designcompose.test.internal.defaultRoborazziRule
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Composable
fun DesignSwitcherDeadbeef() {
    DesignSwitcher(doc = null, currentDocId = "DEADBEEF", branchHash = null, setDocId = {})
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.SmallPhone, sdk = [34])
class DesignSwitcherBasicTests {
    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = defaultRoborazziRule(composeTestRule)

    @Test
    fun displaysMiniVersion() {
        with(composeTestRule) { setContent { DesignSwitcherDeadbeef() } }
    }

    private fun ComposeContentTestRule.openSettingsView() {
        setContent { DesignSwitcherDeadbeef() }
        onNodeWithTag("#SettingsView").performClick()
    }

    @Test
    fun expandMiniVersion() {
        with(composeTestRule) {
            openSettingsView()
            onNodeWithTag("#ChangeButtonFrame", true).assertExists()
        }
    }

    @Test
    fun changeButtonOpensFileLoader() {
        with(composeTestRule) {
            openSettingsView()
            onNodeWithTag("#GoButton").assertDoesNotExist()
            onNodeWithTag("#ChangeButtonFrame", true).performClick()
            onNodeWithTag("#GoButton").assertExists()
        }
    }
}
