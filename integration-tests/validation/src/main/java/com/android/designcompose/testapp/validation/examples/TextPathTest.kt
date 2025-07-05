/*
 * Copyright 2025 Google LLC
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// https://www.figma.com/design/W3l1E3SlwMPyhEwuMpoGGJ/Text-Path?node-id=0-1&t=mgqhI0wNoKU3hMJG-1
@DesignDoc(id = "W3l1E3SlwMPyhEwuMpoGGJ")
interface TextPath {
    @DesignComponent(node = "#stage") fun Main()
}

@Composable
fun TextPathTest() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) { TextPathDoc.Main() }
}
