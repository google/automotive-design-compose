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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

@DesignDoc(id = "kAoYvgHkPzA4J4pALZ3Xhg")
interface TextResizingTest {
    @DesignComponent(node = "#MainFrame") fun Main(@Design(node = "#Text") text: String)
}

val SAMPLE_TEXTS = listOf(
    "",
    "Hi",
    "Hello",
    "Hello World!",
    "X",
    "Goodness me!",
    "Y",
    "This is much longer! Let's see what happens!"
)

@Composable
fun TextResizingTest() {
    val (textIndex, setTextIndex) = remember { mutableStateOf(0) }
    Box(modifier = Modifier.fillMaxSize()) {
        TextResizingTestDoc.Main(text = SAMPLE_TEXTS[textIndex % SAMPLE_TEXTS.size])
        com.android.designcompose.testapp.validation.Button(name = "Next", selected = false) {
            setTextIndex(textIndex + 1)
        }
    }
}