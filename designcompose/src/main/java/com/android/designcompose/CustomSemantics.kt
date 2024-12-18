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

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

// Custom Compose Semantic that identifies a DesignCompose Composable
// The semantic will be set to the generated doc's `javaClass.name`.
// For example, to match the generated doc a DesignDoc named "HelloWorld", match on
// `HelloWorldDoc.javaClass.name`
val docClassSemanticsKey = SemanticsPropertyKey<String>("DocClass")
var SemanticsPropertyReceiver.sDocClass by docClassSemanticsKey

enum class DocRenderStatus {
    Rendered,
    Fetching,
    NodeNotFound,
    NotAvailable,
}

val docRenderStatusSemanticsKey = SemanticsPropertyKey<DocRenderStatus>("DocRenderStatus")
var SemanticsPropertyReceiver.sDocRenderStatus by docRenderStatusSemanticsKey

val docRenderTextSemanticsKey = SemanticsPropertyKey<HashSet<String>>("DocRenderText")
var SemanticsPropertyReceiver.sDocRenderText by docRenderTextSemanticsKey
