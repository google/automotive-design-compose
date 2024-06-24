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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.android.designcompose.ReplacementContent
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

@DesignDoc(id = "qIh0IOQTCtgeAWZFF5gYSk")
interface ComponentReplaceRelayout {
    @DesignComponent(node = "#Main")
    fun Main(
        @Design(node = "#ListA") a: ReplacementContent,
        @Design(node = "#ListB") b: ReplacementContent,
    )

    @DesignComponent(node = "#Item") fun Item(@Design(node = "#Label") label: String)
}

val RELAYOUT_SAMPLE_TEXTS =
    listOf(
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
fun ComponentReplaceRelayoutTest() {
    val (textIndex, setTextIndex) = remember { mutableStateOf(0) }

    ComponentReplaceRelayoutDoc.Main(
        modifier = Modifier.fillMaxWidth(),
        a =
            ReplacementContent(
                count = 5,
                content = { idx ->
                    @Composable {
                        ComponentReplaceRelayoutDoc.Item(
                            label =
                                "${RELAYOUT_SAMPLE_TEXTS[textIndex % RELAYOUT_SAMPLE_TEXTS.size]} $idx"
                        )
                    }
                }
            ),
        b =
            ReplacementContent(
                count = 10,
                content = { idx ->
                    @Composable {
                        ComponentReplaceRelayoutDoc.Item(
                            label =
                                "Hi! ${RELAYOUT_SAMPLE_TEXTS[textIndex % RELAYOUT_SAMPLE_TEXTS.size]} $idx"
                        )
                    }
                }
            )
    )
    com.android.designcompose.testapp.validation.Button(name = "Next", selected = false) {
        setTextIndex(textIndex + 1)
    }
}
