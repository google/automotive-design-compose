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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

@DesignDoc(id = "yX3mcO0qFnvG6ruUYhHBMR")
interface TextReplace {
    @DesignComponent(node = "#stage")
    fun Main(
        @Design(node = "#textfield") textField: @Composable (ComponentReplacementContext) -> Unit
    )
}

@Composable
fun TextReplaceTest() {
    val (docIdText, setDocIdText) = remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        TextReplaceDoc.Main(
            textField = { context ->
                BasicTextField(
                    value = docIdText,
                    onValueChange = setDocIdText,
                    textStyle = context.textStyle ?: TextStyle.Default,
                    modifier = Modifier.testTag("TextInput"),
                )
            }
        )
    }
}
