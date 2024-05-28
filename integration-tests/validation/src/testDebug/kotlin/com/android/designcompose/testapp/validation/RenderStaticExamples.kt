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
 //import com.android.designcompose.testapp.validation.examples.EXAMPLES
 import com.android.designcompose.testapp.validation.examples.STATIC_EXAMPLES
 import com.android.designcompose.testapp.validation.performLiveFetch;
 import java.io.File
 import org.junit.Before
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
 class RenderStaticExamples(private val config: TestConfig) {
     @get:Rule val composeTestRule = createComposeRule()
     @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
 
     data class TestConfig(
         internal val fileName: String,
         internal val fileComposable: @Composable () -> Unit,
         internal val fileClass: String
     )
 
     val dcfOutPath = System.getProperty("designcompose.test.dcfOutPath")
     val runFigmaFetch = System.getProperty("designcompose.test.fetchFigma")
 
     @Before
     fun setUp() {
         DesignSettings.addFontFamily("Inter", interFont)
     }
 
     @Test
     fun testRender() {
         composeTestRule.setContent(config.fileComposable)
 
         if (runFigmaFetch == "true") performLiveFetch(dcfOutPath)
 
         composeTestRule
             .onAllNodes(SemanticsMatcher.expectValue(docClassSemanticsKey, config.fileClass))
             .onFirst()
             .assertRenderStatus(DocRenderStatus.Rendered)
         composeTestRule.captureRootRoboImage(config.fileName)
     }
 
     companion object {
         @JvmStatic
         @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
         fun createTestSet(): List<TestConfig> {
             return STATIC_EXAMPLES.filter { it.third != null }
                 .map {
                     TestConfig(it.first.replace("[\\s*]".toRegex(), "-"), it.second, it.third!!)
                 }
         }
     }
 }
 