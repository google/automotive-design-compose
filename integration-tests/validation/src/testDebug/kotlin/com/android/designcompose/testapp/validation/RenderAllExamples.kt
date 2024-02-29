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
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.test.core.app.ApplicationProvider
import com.android.designcompose.DesignSettings
import com.android.designcompose.DocRenderStatus
import com.android.designcompose.TestUtils
import com.android.designcompose.docClassSemanticsKey
import com.android.designcompose.test.assertRenderStatus
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.testapp.common.interFont
import com.android.designcompose.testapp.validation.examples.EXAMPLES
import java.io.File
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
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

// Enable Robolectric Native Graphics (RNG)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1920dp-h1500dp-xlarge-long-notround-any-xhdpi-keyshidden-nonav", sdk = [34])
@RunWith(ParameterizedRobolectricTestRunner::class)
class RenderAllExamples(private val config: TestConfig) {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)

    data class TestConfig(
        internal val fileName: String,
        internal val fileComposable: @Composable () -> Unit,
        internal val fileClass: String
    )

    val dcfOutPath = System.getProperty("designcompose.test.dcfOutPath")
    val runFigmaFetch = System.getProperty("designcompose.test.fetchFigma")

    @Test
    fun testRender() {
        composeTestRule.setContent(config.fileComposable)

        if (runFigmaFetch == "true") performLiveFetch(dcfOutPath)

        composeTestRule
            .onAllNodes(SemanticsMatcher.expectValue(docClassSemanticsKey, config.fileClass))
            .onFirst()
            .assertRenderStatus(DocRenderStatus.Rendered)
        composeTestRule.captureRootRoboImage(config.fileName.replace(Regex("[^a-zA-Z0-9-_\\.]"), "_"))

    }

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
