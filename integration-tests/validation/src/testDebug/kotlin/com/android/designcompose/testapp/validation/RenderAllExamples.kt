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

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.test.core.app.ApplicationProvider
import com.android.designcompose.DesignDocSettings
import com.android.designcompose.DocRenderStatus
import com.android.designcompose.LocalDesignDocSettings
import com.android.designcompose.TestUtils
import com.android.designcompose.docClassSemanticsKey
import com.android.designcompose.test.assertRenderStatus
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.testapp.common.Fetchable
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.DEFAULT_RENDERER_ONLY_EXAMPLES
import com.android.designcompose.testapp.validation.examples.EXAMPLES
import com.android.designcompose.testapp.validation.examples.SQUOOSH_ONLY_EXAMPLES
import com.android.designcompose.testapp.validation.examples.StateCustomizationsDoc
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

fun performLiveFetch(dcfOutPath: String?) {
    if (dcfOutPath == null) throw RuntimeException("designcompose.test.dcfOutPath not set")

    TestUtils.triggerLiveUpdate()
    val context = ApplicationProvider.getApplicationContext<Context>()

    context
        .fileList()
        .filter { it.endsWith(".dcf") }
        .forEach {
            val filepath = File(context.filesDir.absolutePath, it)
            filepath.copyTo(File(dcfOutPath, it), overwrite = true)
        }
}

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

    data class TestConfig(
        internal val fileName: String,
        internal val fileComposable: @Composable () -> Unit,
        internal val fileClass: String,
        // This value doesn't do anything if there is already one set in the test example.
        internal val useSquoosh: Boolean = false,
    )

    val dcfOutPath = System.getProperty("designcompose.test.dcfOutPath")
    val runFigmaFetch = System.getProperty("designcompose.test.fetchFigma")

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

        if (runFigmaFetch == "true") performLiveFetch(dcfOutPath)

        composeTestRule
            .onAllNodes(SemanticsMatcher.expectValue(docClassSemanticsKey, config.fileClass))
            .onFirst()
            .assertRenderStatus(DocRenderStatus.Rendered)
            .captureRoboImage("${config.fileName}.png")
    }

    companion object {
        private val disabledTests =
            listOf(
                StateCustomizationsDoc.javaClass.name // Separate test due to different set up
            )

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
