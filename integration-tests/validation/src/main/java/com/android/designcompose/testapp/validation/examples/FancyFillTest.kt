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

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.android.designcompose.DesignDocSettings
import com.android.designcompose.LocalDesignDocSettings
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST fancy fill types (solid color, gradients, images) on text, frames, and strokes
@DesignDoc(id = "xQ9cunHt8VUm6xqJJ2Pjb2")
interface FancyFillTest {
    @DesignComponent(node = "#stage") fun MainFrame(@Design(node = "#xyz") onTap: TapCallback)
}

@Composable
fun FancyFillTest() {
    FancyFillTestDoc.MainFrame(onTap = { Log.e("onTap", "frame clicked!") })
}

@Composable
fun SquooshFancyFillTest() {
    CompositionLocalProvider(LocalDesignDocSettings provides DesignDocSettings(useSquoosh = true)) {
        FancyFillTestDoc.MainFrame(onTap = { Log.e("onTap", "frame clicked!") })
    }
}
