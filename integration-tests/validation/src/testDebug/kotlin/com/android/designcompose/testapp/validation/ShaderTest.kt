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

package com.android.designcompose.testapp.validation

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.DesignDocSettings
import com.android.designcompose.LocalDesignDocSettings
import com.android.designcompose.TestUtils
import com.android.designcompose.test.Fetchable
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.test.waitForContent
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.BrushFromShaderPluginTest
import com.android.designcompose.testapp.validation.examples.BrushFromShaderPluginTestDoc
import com.android.designcompose.testapp.validation.examples.CustomBrushTest
import com.android.designcompose.testapp.validation.examples.CustomBrushTestDoc
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w1920dp-h1500dp-xlarge-long-notround-any-xhdpi-keyshidden-nonav")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Category(Fetchable::class)
class ShaderTest {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontTestRule = InterFontTestRule()
    @get:Rule val liveUpdateTestRule = TestUtils.LiveUpdateTestRule()

    @Before
    fun setUp() {
        System.setProperty("robolectric.pixelCopyRenderMode", "hardware")
    }

    @Test
    @Config(sdk = [35])
    fun customBrush() {
        with(composeTestRule) {
            setContent { CustomBrushTest() }

            liveUpdateTestRule.performLiveFetch()
            composeTestRule.waitForContent(CustomBrushTestDoc.javaClass.name)
            captureRootRoboImage("CustomBrush-Shader")
        }
    }

    @Test
    @Config(sdk = [35])
    fun customBrush_SQUOOSH() {
        with(composeTestRule) {
            setContent {
                CompositionLocalProvider(
                    LocalDesignDocSettings provides DesignDocSettings(useSquoosh = true)
                ) {
                    CustomBrushTest()
                }
            }
            captureRootRoboImage("CustomBrush-Shader_SQUOOSH")
        }
    }

    @Test
    @Config(sdk = [32, 35])
    fun shaderPluginBrush() {
        with(composeTestRule) {
            setContent { BrushFromShaderPluginTest() }

            liveUpdateTestRule.performLiveFetch()
            composeTestRule.waitForContent(BrushFromShaderPluginTestDoc.javaClass.name)
            captureRootRoboImage("ShaderPluginBrush_${Build.VERSION.SDK_INT}")
        }
    }

    @Test
    @Config(sdk = [32, 35])
    fun shaderPluginBrush_SQUOOSH() {
        with(composeTestRule) {
            setContent {
                CompositionLocalProvider(
                    LocalDesignDocSettings provides DesignDocSettings(useSquoosh = true)
                ) {
                    BrushFromShaderPluginTest()
                }
            }
            captureRootRoboImage("ShaderPluginBrush_SQUOOSH_${Build.VERSION.SDK_INT}")
        }
    }
}
