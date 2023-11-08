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

package com.android.designcompose.testapp.validation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import com.android.designcompose.DesignSettings
import com.android.designcompose.DocRenderStatus
import com.android.designcompose.docClassSemanticsKey
import com.android.designcompose.docRenderStatusSemanticsKey
import com.github.takahirom.roborazzi.RoborazziRule
import java.io.File
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Enable Robolectric Native Graphics (RNG)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1920dp-h1500dp-xlarge-long-notround-any-xhdpi-keyshidden-nonav", sdk = [34])
@RunWith(ParameterizedRobolectricTestRunner::class)
class RenderAllExamples(private val config: TestConfig) {
    data class TestConfig(
        internal val fileName: String,
        internal val fileComposable: @Composable () -> Unit,
        internal val fileClass: String
    )

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test(expected = AssertionError::class) // TODO: GH-483
    fun testRender() {
        composeTestRule.setContent(config.fileComposable)
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodes(SemanticsMatcher.expectValue(docClassSemanticsKey, config.fileClass))
            .onFirst()
            .assert(
                SemanticsMatcher.expectValue(docRenderStatusSemanticsKey, DocRenderStatus.Rendered)
            )
    }

    @get:Rule
    val roborazziRule =
        RoborazziRule(
            composeRule = composeTestRule,
            captureRoot = composeTestRule.onRoot(),
            options =
                RoborazziRule.Options(
                    RoborazziRule.CaptureType.LastImage(),
                    outputDirectoryPath = "src/testDebug/roborazzi/RenderAllExamples",
                    outputFileProvider = { _, outputDir, fileExtension ->
                        val fileName = config.fileName.replace("[\\s*]".toRegex(), "-")
                        File(outputDir, "$fileName.$fileExtension")
                    }
                ),
        )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return EXAMPLES.filter { it.third != null }
                .map { TestConfig(it.first, it.second, it.third!!) }
        }

        @JvmStatic
        @BeforeClass
        fun setUp() {
            DesignSettings.addFontFamily("Inter", interFont)
        }
    }
}
