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
import androidx.compose.ui.tooling.preview.Preview
import com.android.designcompose.ReplacementContent
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST Item Spacing
@DesignDoc(id = "YXrHBp6C6OaW5ShcCYeGJc")
interface ItemSpacingTest {
    @DesignComponent(node = "#Main")
    fun MainFrame(
        @Design(node = "#HorizontalCustom") horizontalItems: ReplacementContent,
        @Design(node = "#VerticalCustom") verticalItems: ReplacementContent,
    )

    @DesignComponent(node = "#Square") fun Square()
}

@Preview
@Composable
fun ItemSpacingTest() {
    ItemSpacingTestDoc.MainFrame(
        horizontalItems =
            ReplacementContent(count = 3, content = { { ItemSpacingTestDoc.Square() } }),
        verticalItems = ReplacementContent(count = 3, content = { { ItemSpacingTestDoc.Square() } }),
    )
}
