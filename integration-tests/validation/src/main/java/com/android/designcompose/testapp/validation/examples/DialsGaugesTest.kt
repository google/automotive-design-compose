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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.Meter
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST dials and gauges
@DesignDoc(id = "lZj6E9GtIQQE4HNLpzgETw")
interface DialsGaugesTest {
    @DesignComponent(node = "#stage")
    fun MainFrame(
        @Design(node = "#arc-angle") arcAngle: Meter,
        @Design(node = "#needle-rotation") needleRotation: Meter,
        @Design(node = "#progress-bar") progressBar: Meter,
        @Design(node = "#progress-indicator") progressIndicator: Meter,
    )
}

@Composable
fun DialsGaugesTest() {
    val angle = remember { mutableStateOf(50f) }
    val rotation = remember { mutableStateOf(50f) }
    val progress = remember { mutableStateOf(50f) }
    val progressIndicator = remember { mutableStateOf(50f) }
    DialsGaugesTestDoc.MainFrame(
        arcAngle = angle.value,
        needleRotation = rotation.value,
        progressBar = progress.value,
        progressIndicator = progressIndicator.value,
    )

    Row(
        Modifier.absoluteOffset(0.dp, 1410.dp).height(50.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Dial angle: ", Modifier.width(120.dp), fontSize = 20.sp)
        Slider(angle, 0f, 100f)
        Text(angle.value.toString(), fontSize = 20.sp)
    }
    Row(
        Modifier.absoluteOffset(0.dp, 1460.dp).height(50.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Dial rotation: ", Modifier.width(120.dp), fontSize = 20.sp)
        Slider(rotation, 0f, 100f)
        Text(rotation.value.toString(), fontSize = 20.sp)
    }
    Row(
        Modifier.absoluteOffset(0.dp, 1510.dp).height(50.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Progress Bar: ", Modifier.width(120.dp), fontSize = 20.sp)
        Slider(progress, 0f, 100f)
        Text(progress.value.toString(), fontSize = 20.sp)
    }
    Row(
        Modifier.absoluteOffset(0.dp, 1560.dp).height(50.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Progress Indicator: ", Modifier.width(120.dp), fontSize = 20.sp)
        Slider(progressIndicator, 0f, 100f)
        Text(progressIndicator.value.toString(), fontSize = 20.sp)
    }
}
