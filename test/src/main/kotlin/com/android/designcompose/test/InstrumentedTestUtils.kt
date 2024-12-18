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

package com.android.designcompose.test

import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onFirst
import com.android.designcompose.DocRenderStatus
import com.android.designcompose.docClassSemanticsKey
import com.android.designcompose.docRenderStatusSemanticsKey
import com.android.designcompose.docRenderTextSemanticsKey

fun ComposeContentTestRule.onDCDocRoot(genDoc: Any) =
    onAllNodes(SemanticsMatcher.expectValue(docClassSemanticsKey, genDoc.javaClass.name)).onFirst()

fun ComposeContentTestRule.onDCDocAnyNode(genDoc: Any) =
    onAllNodes(SemanticsMatcher.expectValue(docClassSemanticsKey, genDoc.javaClass.name))

fun ComposeContentTestRule.onDCDoc(genDoc: Any) =
    onNode(SemanticsMatcher.expectValue(docClassSemanticsKey, genDoc.javaClass.name))

fun SemanticsNodeInteraction.assertRenderStatus(status: DocRenderStatus) =
    assert(SemanticsMatcher.expectValue(docRenderStatusSemanticsKey, status))

fun ComposeContentTestRule.assertDCRenderStatus(status: DocRenderStatus) =
    onNode(SemanticsMatcher.expectValue(docRenderStatusSemanticsKey, status)).assertExists()

fun ComposeContentTestRule.waitForContent(name: String): SemanticsNodeInteraction {
    waitForIdle()
    return onAllNodes(SemanticsMatcher.expectValue(docClassSemanticsKey, name))
        .onFirst()
        .assertRenderStatus(DocRenderStatus.Rendered)
}

fun SemanticsNodeInteraction.assertHasText(text: String, substring: Boolean = false) =
    assert(
        SemanticsMatcher("hasText: $text") {
            val textList = it.config.getOrNull(docRenderTextSemanticsKey)
            if (substring) {
                var found = false
                textList?.forEach { renderedText ->
                    if (renderedText.contains(text)) {
                        found = true
                    }
                }
                found
            } else {
                textList?.contains(text) ?: false
            }
        }
    )

fun SemanticsNodeInteractionCollection.assertHasText(text: String) =
    assertAny(
        SemanticsMatcher("hasText: $text") {
            it.config.getOrNull(docRenderTextSemanticsKey)?.contains(text) ?: false
        }
    )

fun SemanticsNodeInteractionCollection.assertDoesNotHaveText(text: String) =
    assertAll(
        SemanticsMatcher("hasText: $text") {
            it.config.getOrNull(docRenderTextSemanticsKey)?.contains(text) == false
        }
    )

fun hasDesignText(text: String) =
    SemanticsMatcher("hasDesignText: $text") {
        it.config.getOrNull(docRenderTextSemanticsKey)?.contains(text) ?: false
    }
