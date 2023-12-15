package com.android.designcompose.testapp.validation

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.test.internal.ROBO_CAPTURE_DIR
import com.android.designcompose.testapp.validation.examples.InteractionTest
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet, sdk = [34])
class InteractionTriggers {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val roborazziRule =
        RoborazziRule(
            options =
                RoborazziRule.Options(outputDirectoryPath = "$ROBO_CAPTURE_DIR/interactionTriggers")
        )

    @get:Rule var testName = TestName()

    fun ComposeTestRule.captureImg(name: String) {
        onRoot().captureRoboImage("${testName.methodName}/$name.png")
    }

    @Before
    fun setup() {
        with(composeTestRule) { setContent { InteractionTest() } }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun whilePressedTimeoutTimesOut() {
        with(composeTestRule) {
            onNodeWithText("Triggers").performClick()
            onNodeWithText("While Pressed").performClick()
            onNodeWithText("Timeouts").performClick()
            captureImg("start")

            onNodeWithText("idle").performTouchInput { down(Offset.Zero) }
            onNodeWithText("pressed").assertExists()
            captureImg("pressed")

            waitUntilDoesNotExist(hasText("pressed"), 1000)
            onNodeWithText("timeout").assertExists()
            captureImg("timedOut")

            onRoot().performTouchInput { cancel() }
            onNodeWithText("idle").assertExists()
            captureImg("final")
        }
    }
}
