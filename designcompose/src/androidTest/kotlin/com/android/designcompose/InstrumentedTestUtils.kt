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

package com.android.designcompose

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.ComposeContentTestRule

fun ComposeContentTestRule.onDCDoc(genDoc: Any) =
    onNode(SemanticsMatcher.expectValue(docClassSemanticsKey, genDoc.javaClass.name))

fun SemanticsNodeInteraction.assertRenderStatus(status: DocRenderStatus) =
    assert(SemanticsMatcher.expectValue(docRenderStatusSemanticsKey, status))

fun ComposeContentTestRule.assertDCRenderStatus(status: DocRenderStatus) =
    onNode(SemanticsMatcher.expectValue(docRenderStatusSemanticsKey, status)).assertExists()
