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
