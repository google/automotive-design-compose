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

// Hello World with override to change doc ID
@DesignDoc(id = "pxVlixodJqZL95zo2RzTHl")
interface HelloBye {
    @DesignComponent(node = "#MainFrame") fun Main(@Design(node = "#Name") name: String)
}

@Composable
fun HelloBye() {
    DesignDocOverride("MCHaMYcIEnRpbvU9Ms7a0o") { HelloByeDoc.Main(name = "World") }
}
