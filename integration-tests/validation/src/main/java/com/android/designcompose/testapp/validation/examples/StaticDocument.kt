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

package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import com.android.designcompose.DesignDocOverride
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

//Static document with override to change doc ID
@DesignDoc(id = "Gv63fYTzpeH2ZtxP4go31E")
interface StaticDocument {
    @DesignComponent(node = "#stage") fun Main(@Design(node = "#Name") name: String)
}

// TODO: how can I pass a node ID to the doc that isn't from generated code?
@Composable
fun StaticDocument(doc_id: String, node: String ) {
    DesignDocOverride(doc_id) { StaticDocumentDoc.Main(name = "World") }
}
