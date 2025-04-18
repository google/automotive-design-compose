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

import android.app.Activity
import android.app.Instrumentation
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.HyperlinkTest
import kotlin.test.Ignore
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

/** Integration test on clicking on the urls in texts. */
@RunWith(AndroidJUnit4::class)
class HyperlinkTests {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val interFontRule = InterFontTestRule()
    @get:Rule var intentsRule: IntentsRule = IntentsRule()

    @Test
    fun testClickSingleNodeLink() {
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        val url = "https://github.com/google/automotive-design-compose"
        // When the intent with the given url is sent, respond with the given result so it
        // won't start the url in the browser in the test. Otherwise, this test takes longer
        // to execute.
        intending(hasData(url)).respondWith(result)

        composeTestRule.setContent { HyperlinkTest() }
        composeTestRule.onNodeWithContentDescription(url).performClick()
        // Verify that the intent with the given url was sent.
        intended(hasData(url))
    }

    /** Disabled due to Squoosh's implementation not using the TextLayout */
    @Ignore
    @Test
    fun testClickEmbeddedLink() {
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        var url = "https://developer.android.com/jetpack/compose"
        var text = "Jetpack Compose"
        intending(hasData(url)).respondWith(result)

        composeTestRule.setContent { HyperlinkTest() }
        composeTestRule.onNodeWithText(text, substring = true).clickUrl(url)

        intended(hasData(url))

        ///// Second link /////
        url = "https://www.figma.com/"
        text = "Figma"
        intending(hasData(url)).respondWith(result)

        composeTestRule.onNodeWithText(text, substring = true).clickUrl(url)

        intended(hasData(url))
    }

    /** Use BoundsAssertions.getPartialBoundsOfLinks when it is available */
    @OptIn(ExperimentalTextApi::class)
    private fun SemanticsNodeInteraction.clickUrl(url: String): SemanticsNodeInteraction {
        val node = fetchSemanticsNode("Failed to retrieve bounds of the node.")
        val textLayoutResults = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(textLayoutResults)

        var boundOfLink: Rect? = null
        if (textLayoutResults.isNotEmpty()) {
            val text = textLayoutResults[0].layoutInput.text
            val urls = text.getUrlAnnotations(0, text.length).filter { it.item.url == url }
            if (urls.isNotEmpty()) {
                boundOfLink = textLayoutResults[0].getBoundingBox(urls[0].start)
            }
        }
        if (boundOfLink != null) {
            return this.performTouchInput { click(boundOfLink.center) }
        } else {
            throw AssertionError("Failed to click on the url $url")
        }
    }
}
