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

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.android.designcompose.DesignSettings.addFontFamily
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.testapp.validation.R

@DesignDoc(id = "iufp6QFCOK9ClvTNlIuua7")
interface HelloCursive {
    @DesignComponent(node = "#MainFrame") fun Main(@Design(node = "#Name") name: String)
}

@Composable
fun HelloCursive() {
    val myFont =
        FontFamily(
            Font(
                DeviceFontFamilyName("cursive"),
                FontWeight.Normal,
                FontStyle.Normal,
                FontVariation.Settings(),
            )
        )
    addFontFamily("Cedarville Cursive", myFont)

    HelloCursiveDoc.Main(name = stringResource(id = R.string.label_world))
}
