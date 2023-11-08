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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.android.designcompose.ContentReplacementContext
import com.android.designcompose.ParentLayoutInfo
import com.android.designcompose.ReplacementContent
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST Layout Replacement
// This test places components (#fill, #topleft, #bottomright, #center) which have Layout
// Constraints applied to them inside of various parent elements which are larger than
// the components.
// We should see that the layout constraints are applied to the components and that they
// resize.
@DesignDoc(id = "dwk2GF7RiNvlbbAKPjqldx")
interface LayoutReplacementTest {
    @DesignComponent(node = "#stage")
    fun MainFrame(
        @Design(node = "#next") onNext: TapCallback,
        @Design(node = "#parent1") parent1: ReplacementContent,
        @Design(node = "#parent2") parent2: ReplacementContent,
        @Design(node = "#parent3") parent3: ReplacementContent,
    )

    @DesignComponent(node = "#fill") fun Fill()

    @DesignComponent(node = "#topleft") fun TopLeft()

    @DesignComponent(node = "#bottomright") fun BottomRight()

    @DesignComponent(node = "#center") fun Center()
}

@Composable
fun LayoutReplacementTestCase(idx: Int, rc: ContentReplacementContext) {
    if (idx == 0) {
        LayoutReplacementTestDoc.Fill(
            parentLayout = ParentLayoutInfo(rc.parentLayoutId, 0, rc.rootLayoutId)
        )
    } else if (idx == 1) {
        LayoutReplacementTestDoc.TopLeft(
            parentLayout = ParentLayoutInfo(rc.parentLayoutId, 0, rc.rootLayoutId)
        )
    } else if (idx == 2) {
        LayoutReplacementTestDoc.BottomRight(
            parentLayout = ParentLayoutInfo(rc.parentLayoutId, 0, rc.rootLayoutId)
        )
    } else if (idx == 3) {
        LayoutReplacementTestDoc.Center(
            parentLayout = ParentLayoutInfo(rc.parentLayoutId, 0, rc.rootLayoutId)
        )
    }
}

@Composable
fun LayoutReplacementTest() {
    val (idx, setIdx) = remember { mutableStateOf(0) }
    LayoutReplacementTestDoc.MainFrame(
        onNext = { setIdx((idx + 1) % 4) },
        parent1 =
            ReplacementContent(
                count = 1,
                content = { { rc -> LayoutReplacementTestCase(idx, rc) } }
            ),
        parent2 =
            ReplacementContent(
                count = 1,
                content = { { rc -> LayoutReplacementTestCase(idx, rc) } }
            ),
        parent3 =
            ReplacementContent(
                count = 1,
                content = { { rc -> LayoutReplacementTestCase(idx, rc) } }
            ),
    )
}
