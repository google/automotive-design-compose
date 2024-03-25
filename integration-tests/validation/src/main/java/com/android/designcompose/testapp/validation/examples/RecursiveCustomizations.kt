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
import com.android.designcompose.ReplacementContent
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST Custom content children
@DesignDoc(id = "o0GWzcqdOWEgzj4kIeIlAu")
interface RecursiveCustomizations {
    @DesignComponent(node = "#MainFrame")
    fun MainFrame(
        @Design(node = "#Name") name: String,
        @Design(node = "#ChildFrame") child: ReplacementContent,
        @Design(node = "#Content") content: ReplacementContent,
    )

    @DesignComponent(node = "#NameFrame") fun NameFrame()

    @DesignComponent(node = "#TitleFrame")
    fun TitleFrame(
        @Design(node = "#Name") title: String,
    )
}

@Composable
fun RecursiveCustomizations() {
    RecursiveCustomizationsDoc.MainFrame(
        name = "Google",
        child =
            ReplacementContent(count = 1, content = { { RecursiveCustomizationsDoc.NameFrame() } }),
        content =
            ReplacementContent(
                count = 3,
                content = { index ->
                    {
                        when (index) {
                            0 -> RecursiveCustomizationsDoc.TitleFrame(title = "First")
                            1 -> RecursiveCustomizationsDoc.TitleFrame(title = "Second")
                            else -> RecursiveCustomizationsDoc.TitleFrame(title = "Third")
                        }
                    }
                }
            )
    )
}
