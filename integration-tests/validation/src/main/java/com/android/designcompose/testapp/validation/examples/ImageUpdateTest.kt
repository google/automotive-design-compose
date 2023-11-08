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

import androidx.compose.runtime.Composable
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST Image Update Test. After this loads, rename #Stage in the Figma doc. After the app
// updates,
// rename it back to #Stage. The image should reload correctly.
@DesignDoc(id = "oQw7kiy94fvdVouCYBC9T0")
interface ImageUpdateTest {
    @DesignComponent(node = "#Stage") fun Main() {}
}

@Composable
fun ImageUpdateTest() {
    ImageUpdateTestDoc.Main()
}
