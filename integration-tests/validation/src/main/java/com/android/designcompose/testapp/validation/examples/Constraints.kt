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

package com.android.designcompose.testapp.validation.examples

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST Constraints
@DesignDoc(id = "KuHLbsKA23DjZPhhgHqt71")
interface Constraints {
    @DesignComponent(node = "#Horizontal") fun HorizontalFrame()

    @DesignComponent(node = "#Vertical") fun VerticalFrame()
}

@Preview
@Composable
fun HConstraintsTest() {
    ConstraintsDoc.HorizontalFrame(Modifier.fillMaxSize())
}

@Preview
@Composable
fun VConstraintsTest() {
    ConstraintsDoc.VerticalFrame(Modifier.fillMaxSize())
}
