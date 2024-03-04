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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.test.onDCDoc
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Composable
fun DesignSwitcherTest(testName: TestName) {
    val idState = remember { mutableStateOf(testName.methodName) }
    DesignSwitcher(doc = null, currentDocId = idState.value, branchHash = null, setDocId = {})
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.SmallPhone, sdk = [34])
class DesignSwitcherBasicTests {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val testName = TestName()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)

    @Before
    fun resetState() {
        // Must reset the interaction state so that the DesignSwitcher starts off closed.
        InteractionStateManager.states.clear()
        composeTestRule.setContent { DesignSwitcherTest(testName = testName) }
    }

    @Test
    fun canExpandDesignSwitcher() {
        with(composeTestRule) {
            captureRootRoboImage("CollapsedSwitcher")
            onDCDoc(DesignSwitcherDoc).performClick()
            onNodeWithText("Change").assertExists()
            captureRootRoboImage("ExpandedSwitcher")
        }
    }

    @Test
    fun showChangeScreen() {
        with(composeTestRule) {
            onDCDoc(DesignSwitcherDoc).performClick()
            onNodeWithText("Change", useUnmergedTree = true).performClick()
            onNodeWithText("Load", useUnmergedTree = true).assertExists()
            captureRootRoboImage("ChangeFileScreen")
        }
    }

    @Test
    fun canCheckACheckBox() {
        with(composeTestRule) {
            onDCDoc(DesignSwitcherDoc).performClick()
            onNodeWithText("Options", useUnmergedTree = true).performClick()
            captureRootRoboImage("OptionsScreenBeforeCheckingBox")
            onAllNodes(hasClickAction())[2].performClick() // Currently the only way to find it
            captureRootRoboImage("OptionsScreenAfterCheckingBox")
        }
    }
}
