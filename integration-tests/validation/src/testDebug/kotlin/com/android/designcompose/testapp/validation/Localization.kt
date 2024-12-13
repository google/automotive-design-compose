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

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.TestUtils
import com.android.designcompose.test.assertHasText
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.test.onDCDoc
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.HelloWorld
import com.android.designcompose.testapp.validation.examples.HelloWorldDoc
import com.android.designcompose.testapp.validation.examples.ImageUpdateTest
import com.android.designcompose.testapp.validation.examples.StyledTextRunsTest
import com.android.designcompose.testapp.validation.examples.VariantAsteriskTest
import com.android.designcompose.testapp.validation.examples.VariantAsteriskTestDoc
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w1920dp-h1500dp-xlarge-long-notround-any-xhdpi-keyshidden-nonav")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Localization {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontTestRule = InterFontTestRule()

    @Test
    @Config(qualifiers = "+zh")
    fun helloWorldLocaleZh() {
        with(composeTestRule) {
            setContent { HelloWorld() }
            with(onDCDoc(HelloWorldDoc)) {
                assertHasText("你好，")
                assertHasText("世界")
            }
            captureRootRoboImage("HelloWorld-zh")
        }
    }

    @Test
    @Config(qualifiers = "+es")
    fun helloWorldLocaleEs() {
        with(composeTestRule) {
            setContent { HelloWorld() }
            with(onDCDoc(HelloWorldDoc)) {
                assertHasText("Hola,").assertExists()
                assertHasText("Mundo").assertExists()
            }
            captureRootRoboImage("HelloWorld-es")
        }
    }

    /** Test local components with localization works. */
    @Test
    @Config(qualifiers = "+es")
    fun variantAsteriskLocaleEs() {
        with(composeTestRule) {
            setContent { VariantAsteriskTest() }
            with(onDCDoc(VariantAsteriskTestDoc)) {
                assertHasText("Aparcado")
                onNodeWithTag("R").performClick()
                assertHasText("Inverso")
                captureRootRoboImage("VariantAsteriskTest-es")
            }
        }
    }

    /** Test styled text runs in a single text node works. */
    @Test
    @Config(qualifiers = "+ja")
    fun styledTextRunsLocaleJa() {
        with(composeTestRule) {
            setContent { StyledTextRunsTest() }
            captureRootRoboImage("StyledTextRuns-ja")
        }
    }

    @Test
    @Config(qualifiers = "+fr")
    fun imageUpdateTestLocaleFr() {
        with(composeTestRule) {
            setContent { ImageUpdateTest() }
            captureRootRoboImage("ImageUpdateTest-fr")
        }
    }
}
