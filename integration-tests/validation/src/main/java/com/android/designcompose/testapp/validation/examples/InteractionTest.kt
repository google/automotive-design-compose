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
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignKeyAction
import com.android.designcompose.annotation.DesignMetaKey

// TEST Interactions
@DesignDoc(id = "8Zg9viyjYTnyN29pbkR1CE")
interface InteractionTest {
    @DesignComponent(node = "Start Here")
    fun MainFrame(
        @Design(node = "#KeyButtonB") onTapKeyButtonB: TapCallback,
        @Design(node = "#KeyButtonC") onTapKeyButtonC: TapCallback,
        @Design(node = "#KeyInjectA") onTapInjectA: TapCallback,
        @Design(node = "#KeyInjectB") onTapInjectB: TapCallback,
        @Design(node = "#KeyInjectC") onTapInjectC: TapCallback,
        @Design(node = "#KeyInjectAB") onTapInjectAB: TapCallback,
        @Design(node = "#KeyInjectBC") onTapInjectBC: TapCallback,
    )

    // Inject a ctrl-shift-B key when the 'clickedB()' function is called
    @DesignKeyAction(key = 'B', metaKeys = [DesignMetaKey.MetaShift, DesignMetaKey.MetaCtrl])
    fun clickedShiftCtrlB()
    // Inject a meta-C key when the 'clickedC()' function is called
    @DesignKeyAction(key = 'C', metaKeys = [DesignMetaKey.MetaMeta]) fun clickedMetaC()

    @DesignKeyAction(key = 'A', metaKeys = []) fun clickedA()

    @DesignKeyAction(key = 'B', metaKeys = []) fun clickedB()

    @DesignKeyAction(key = 'C', metaKeys = []) fun clickedC()
}

@Composable
fun InteractionTest() {
    InteractionTestDoc.MainFrame(
        onTapKeyButtonB = { InteractionTestDoc.clickedShiftCtrlB() },
        onTapKeyButtonC = { InteractionTestDoc.clickedMetaC() },
        onTapInjectA = { InteractionTestDoc.clickedA() },
        onTapInjectB = { InteractionTestDoc.clickedB() },
        onTapInjectC = { InteractionTestDoc.clickedC() },
        onTapInjectAB = {
            InteractionTestDoc.clickedA()
            InteractionTestDoc.clickedB()
        },
        onTapInjectBC = {
            InteractionTestDoc.clickedB()
            InteractionTestDoc.clickedC()
        },
    )
}
