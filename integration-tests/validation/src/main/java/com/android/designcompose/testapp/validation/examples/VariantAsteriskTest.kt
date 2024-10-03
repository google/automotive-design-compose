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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant

enum class PrndState {
    P,
    R,
    N,
    D,
}

enum class ChargingState {
    off,
    on,
}

enum class RegenState {
    off,
    on,
}

data class Controls(
    val shiftState: MutableState<PrndState>,
    val charging: MutableState<ChargingState>,
    val regen: MutableState<RegenState>,
)

// TEST Variant Asterisk Test. Tests the @DesignVariant annotation and the ability to match
// variant nodes whose property names and values match the current state passed in.
// The Figma doc contains a sparse component that has three variant properties. The component is
// setup with several wildcard variant names. This example lets you toggle between different values
// for the three properties, and when a node name with those specific values cannot be found, it
// will look for wildcard variants names and use those instead.

@Composable
private fun Button(name: String, selected: Boolean, select: () -> Unit) {
    val textColor = if (selected) Color.Black else Color.Gray
    val borderColor = if (selected) Color.Black else Color.Gray
    var modifier =
        Modifier.padding(10.dp)
            .clickable { select() }
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .absolutePadding(10.dp, 2.dp, 10.dp, 2.dp)

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Text(name, fontSize = 30.sp, color = textColor)
    }
}

@Composable
private fun Controls(controls: Controls) {
    Box(Modifier.absoluteOffset(x = 10.dp, y = 300.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Shift State:   ", fontSize = 30.sp, color = Color.Black)
                Button("P", controls.shiftState.value == PrndState.P) {
                    controls.shiftState.value = PrndState.P
                }
                Button("R", controls.shiftState.value == PrndState.R) {
                    controls.shiftState.value = PrndState.R
                }
                Button("N", controls.shiftState.value == PrndState.N) {
                    controls.shiftState.value = PrndState.N
                }
                Button("D", controls.shiftState.value == PrndState.D) {
                    controls.shiftState.value = PrndState.D
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Charging:   ", fontSize = 30.sp, color = Color.Black)
                Button("On", controls.charging.value == ChargingState.on) {
                    controls.charging.value = ChargingState.on
                }
                Button("Off", controls.charging.value == ChargingState.off) {
                    controls.charging.value = ChargingState.off
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Regen:   ", fontSize = 30.sp, color = Color.Black)
                Button("On", controls.regen.value == RegenState.on) {
                    controls.regen.value = RegenState.on
                }
                Button("Off", controls.regen.value == RegenState.off) {
                    controls.regen.value = RegenState.off
                }
            }
        }
    }
}

@DesignDoc(id = "gQeYHGCSaBE4zYSFpBrhre")
interface VariantAsteriskTest {
    @DesignComponent(node = "#Main")
    fun Main(
        @DesignVariant(property = "prnd") prnd: PrndState,
        @DesignVariant(property = "charging") charging: ChargingState,
        @DesignVariant(property = "regen") regen: RegenState,
    )
}

@Composable
fun VariantAsteriskTest() {
    val shiftState = remember { mutableStateOf(PrndState.P) }
    val charging = remember { mutableStateOf(ChargingState.off) }
    val regen = remember { mutableStateOf(RegenState.off) }
    val controls = remember { Controls(shiftState, charging, regen) }

    Controls(controls)
    VariantAsteriskTestDoc.Main(
        Modifier.absoluteOffset(x = 10.dp, y = 10.dp),
        prnd = shiftState.value,
        charging = charging.value,
        regen = regen.value,
    )
}
