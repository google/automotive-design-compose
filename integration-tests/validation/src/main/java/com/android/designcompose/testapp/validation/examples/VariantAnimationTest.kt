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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant

enum class State {
    X,
    Y
}

@DesignDoc(id = "pghyUUhlzJNoxxSK86ngiw")
interface VariantAnimationTest {
    @DesignComponent(node = "#MainFrame")
    fun MainFrame(
        @DesignVariant(property = "#state") state: State
    )
}

@Composable
fun VariantAnimationTest() {
    val (state, setState) = remember { mutableStateOf(State.X) }

    VariantAnimationTestDoc.MainFrame(state = state)

    Column(modifier = Modifier.absoluteOffset(x = 20.dp, y = 600.dp)) {
        Row {
            Text("State ", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button("X", state == State.X) {
                setState(State.X)
            }
            com.android.designcompose.testapp.validation.Button("Y", state == State.Y) {
                setState(State.Y)
            }
        }
    }
}