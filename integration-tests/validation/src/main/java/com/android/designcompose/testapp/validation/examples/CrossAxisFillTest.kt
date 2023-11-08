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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.designcompose.ParentLayoutInfo
import com.android.designcompose.ReplacementContent
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST Cross Axis Fill
@DesignDoc(id = "GPr1cx4n3zBPwLhqlSL1ba")
interface CrossAxisFillTest {
    @DesignComponent(node = "#stage")
    fun MainFrame(
        @Design(node = "#FixedWidth") fixedWidth: Modifier,
        @Design(node = "#OuterColumn") outerColumnContents: ReplacementContent,
    )

    @DesignComponent(node = "#LargeFixedWidth") fun LargeFixedWidth()

    @DesignComponent(node = "#FillParentWidth") fun FillParentWidth()
}

@Composable
fun CrossAxisFillTest() {
    CrossAxisFillTestDoc.MainFrame(
        modifier = Modifier.fillMaxWidth(),
        fixedWidth = Modifier.width(200.dp),
        outerColumnContents =
            ReplacementContent(
                count = 2,
                content = { index ->
                    { rc ->
                        if (index == 0)
                            CrossAxisFillTestDoc.LargeFixedWidth(
                                parentLayout =
                                    ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId),
                                modifier = Modifier.width(200.dp)
                            )
                        else
                            CrossAxisFillTestDoc.FillParentWidth(
                                parentLayout =
                                    ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId)
                            )
                    }
                }
            )
    )
}
