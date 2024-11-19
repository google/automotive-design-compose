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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.designcompose.DesignDocSettings
import com.android.designcompose.LocalDesignDocSettings
import com.android.designcompose.TestUtils
import com.android.designcompose.test.Fetchable
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.test.waitForContent
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.CustomBrushTestDoc
import com.android.designcompose.testapp.validation.examples.DEFAULT_RENDERER_ONLY_EXAMPLES
import com.android.designcompose.testapp.validation.examples.EXAMPLES
import com.android.designcompose.testapp.validation.examples.SQUOOSH_ONLY_EXAMPLES
import com.android.designcompose.testapp.validation.examples.StateCustomizationsDoc
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Category(Fetchable::class)
// Enable Robolectric Native Graphics (RNG)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w1920dp-h1500dp-xlarge-long-notround-any-xhdpi-keyshidden-nonav")
class RenderAllExamples(private val config: TestConfig) {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontRule = InterFontTestRule()
    @get:Rule val liveUpdateTestRule = TestUtils.LiveUpdateTestRule()

    data class TestConfig(
        internal val fileName: String,
        internal val fileComposable: @Composable () -> Unit,
        internal val fileClass: String,
        // This value doesn't do anything if there is already one set in the test example.
        internal val useSquoosh: Boolean = false,
    )

    @Test
    fun testRender() {
        if (config.useSquoosh) {
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalDesignDocSettings provides DesignDocSettings(useSquoosh = true)
                ) {
                    config.fileComposable()
                }
            }
        } else {
            composeTestRule.setContent(config.fileComposable)
        }

        liveUpdateTestRule.performLiveFetch()

        composeTestRule.waitForContent(config.fileClass).captureRoboImage("${config.fileName}.png")
    }

    companion object {
        // Separate tests due to different set up. Please also make the new tests fetch-able
        // to fetch dcf files.
        private val disabledTests =
            listOf(StateCustomizationsDoc.javaClass.name, CustomBrushTestDoc.javaClass.name)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            val testConfigs = mutableListOf<TestConfig>()
            val defaultRendererExamples =
                mutableListOf<Triple<String, @Composable () -> Unit, String?>>()
            defaultRendererExamples.addAll(EXAMPLES)
            defaultRendererExamples.addAll(DEFAULT_RENDERER_ONLY_EXAMPLES)
            testConfigs.addAll(
                defaultRendererExamples
                    .filter { it.third != null && !disabledTests.contains(it.third!!) }
                    .map {
                        TestConfig(it.first.replace("[\\s*]".toRegex(), "-"), it.second, it.third!!)
                    }
            )

            val squooshRendererExamples =
                mutableListOf<Triple<String, @Composable () -> Unit, String?>>()
            squooshRendererExamples.addAll(SQUOOSH_ONLY_EXAMPLES)
            squooshRendererExamples.addAll(EXAMPLES)

            testConfigs.addAll(
                squooshRendererExamples
                    .filter { it.third != null && !disabledTests.contains(it.third!!) }
                    .map {
                        TestConfig(
                            it.first.replace("[\\s*]".toRegex(), "-").plus("_SQUOOSH"),
                            it.second,
                            it.third!!,
                            useSquoosh = true,
                        )
                    }
            )
            return testConfigs
        }
    }
}
