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
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.test.internal.ROBO_CAPTURE_DIR
import com.android.designcompose.test.internal.captureImg
import com.android.designcompose.test.internal.defaultRoborazziRule
import com.android.designcompose.test.onDCDoc
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
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
    @get:Rule val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(outputDirectoryPath = "$ROBO_CAPTURE_DIR/${javaClass.simpleName}")
    )

    @Before
    fun resetState() {
        // Must reset the interaction state so that the DesignSwitcher starts off consistently
        // closed.
        InteractionStateManager.states.clear()
    }

    @Test
    fun designSwitcherCanClickExpandButton() {
        with(composeTestRule) {
            setContent { DesignSwitcherTest(testName) }
            captureImg(testName.methodName, "CollapsedSwitcher")
            onDCDoc(DesignSwitcherDoc).performClick()
            val DesignSwitcherMatcher =  onNodeWithText("Design Switcher").fetchSemanticsNode()
            DesignSwitcherMatcher.onNodeWithText("Change").assertExists()
           captureImg(testName.methodName, "ExpandedSwitcher")
        }
        composeTestRule.onRoot().captureRoboImage("filemane.png")
    }

    @Test
    fun showChangeScreen() {
        with(composeTestRule) {
            setContent { DesignSwitcherTest(testName) }
            onDCDoc(DesignSwitcherDoc).performClick()
            onNodeWithText("Change", useUnmergedTree = true).performClick()
            onNodeWithText("Load", useUnmergedTree = true).assertExists()
            captureImg( testName.methodName, "ChangeFileScreen")
        }
    }

    @Test
    fun designSwitcherCheckBoxRespondsToClick() {
        with(composeTestRule) {
            setContent { DesignSwitcherTest(testName) }
            onDCDoc(DesignSwitcherDoc).performClick()
            onNodeWithText("Options", useUnmergedTree = true).performClick()
            captureImg(testName.methodName, "OptionsScreenBeforeClick")
            onAllNodes(hasClickAction())[2].performClick() // Currently the only way to find it
        }
    }
}
