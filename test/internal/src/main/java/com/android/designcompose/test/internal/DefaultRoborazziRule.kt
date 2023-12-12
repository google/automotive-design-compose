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

package com.android.designcompose.test.internal

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.takahirom.roborazzi.RoborazziRule

fun defaultRoborazziRule(
    composeTestRule:
        AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>
) =
    RoborazziRule(
        composeRule = composeTestRule,
        // Specify the node to capture for the last image
        captureRoot = composeTestRule.onRoot(),
        options =
            RoborazziRule.Options(
                outputDirectoryPath = "src/testDebug/roborazzi",
                // Always capture the last image of the test
                captureType = RoborazziRule.CaptureType.LastImage()
            )
    )
