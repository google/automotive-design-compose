package com.android.designcompose.testapp.validation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.test.internal.ROBO_CAPTURE_DIR
import com.android.designcompose.testapp.validation.examples.VariantInteractionsTest
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziRule
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
class VariantInteractions {
    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule =
        RoborazziRule(
            options =
                RoborazziRule.Options(outputDirectoryPath = "$ROBO_CAPTURE_DIR/interactionTriggers")
        )

    @get:Rule var tn = TestName()

    @Before
    fun setup() {
        with(composeTestRule) { setContent { VariantInteractionsTest() } }
    }

    @Test
    fun holdAndRelease() {
        with(composeTestRule) {
            println(onRoot().printToString())
            val nodeOne = onNode(hasText("One")).onParent()
            nodeOne.assert(hasTestTag("#ButtonVariant1=List"))
            nodeOne.performTouchInput { down(Offset.Zero) }
            nodeOne.assert(hasTestTag("#ButtonVariant1=ListPressed"))
        }
    }
}
