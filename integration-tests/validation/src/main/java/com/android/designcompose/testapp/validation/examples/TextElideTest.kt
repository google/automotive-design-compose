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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST Text Elide Test
// This test tests that text max line count and eliding with ellipsis works
@DesignDoc(id = "oQ7nK49Ya5PJ3GpjI5iy8d")
interface TextElideTest {
    @DesignComponent(node = "#stage") fun MainFrame()

    @DesignComponent(node = "#stage2") fun stage2()

    @DesignComponent(node = "#stage3") fun stage3()

    @DesignComponent(node = "#stage4") fun stage4()

    @DesignComponent(node = "#stage5") fun stage5()
}

@Composable
fun TextElideTest() {
    val list = mutableListOf("#stage", "#stage2", "#stage3", "#stage4", "#stage5")
    val stageState = remember { mutableStateOf("#stage") }

    val containerHeight = remember { mutableFloatStateOf(61f) }

    when (stageState.value) {
        "#stage" -> TextElideTestDoc.MainFrame()
        "#stage2" -> TextElideTestDoc.stage2()
        "#stage3" ->
            TextElideTestDoc.stage3(modifier = Modifier.height(containerHeight.floatValue.dp))
        "#stage4" ->
            TextElideTestDoc.stage4(modifier = Modifier.height(containerHeight.floatValue.dp))
        "#stage5" ->
            TextElideTestDoc.stage5(modifier = Modifier.height(containerHeight.floatValue.dp))
    }

    Column(modifier = Modifier.offset(y = 360.dp)) {
        list.forEach {
            Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = (it == stageState.value),
                    onClick = { stageState.value = it }
                )
                Text(text = it, modifier = Modifier.padding(start = 4.dp))
            }
        }

        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
        Text(text = "Height of #stage3, #stage4 and #stage5 for squoosh (min: 0dp, max: 200dp):")
        Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            ElevatedButton(
                onClick = {
                    containerHeight.floatValue = (containerHeight.floatValue - 2f).coerceAtLeast(0f)
                }
            ) {
                Text("-")
            }

            Slider(value = containerHeight, min = 0f, max = 200f)

            ElevatedButton(
                onClick = {
                    containerHeight.floatValue =
                        (containerHeight.floatValue + 2f).coerceAtMost(200f)
                }
            ) {
                Text("+")
            }

            Text(
                modifier = Modifier.padding(4.dp),
                text = "%.1f".format(containerHeight.floatValue)
            )
        }
    }
}
