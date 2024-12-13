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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.testapp.common.InterFontTestRule
import org.junit.Rule
import org.junit.runner.RunWith

/** Integration test on clicking on the urls in texts. */
@RunWith(AndroidJUnit4::class)
class HyperlinkTests {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val interFontRule = InterFontTestRule()
    @get:Rule var intentsRule: IntentsRule = IntentsRule()

    // These are disabled because squoosh does not support clickable hyperlink text
    /*
    @Test
    fun testClickSingleNodeLink() {
        val url = "https://github.com/google/automotive-design-compose"
        composeTestRule.setContent { HyperlinkTest() }
        composeTestRule.onNodeWithText(url).performClick()
        intended(hasData(url))
    }

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
    */

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
