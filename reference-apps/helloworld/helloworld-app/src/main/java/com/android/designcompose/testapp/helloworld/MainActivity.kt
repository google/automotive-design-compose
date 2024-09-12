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

package com.android.designcompose.testapp.helloworld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.DesignDocOverride
import com.android.designcompose.DesignDocSettings
import com.android.designcompose.DesignMaterialThemeProvider
import com.android.designcompose.DesignSettings
import com.android.designcompose.DesignVariableCollection
import com.android.designcompose.DesignVariableModeValues
import com.android.designcompose.LocalDesignDocSettings
import com.android.designcompose.Meter
import com.android.designcompose.TapCallback
import com.android.designcompose.VariableModeValues
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.common.DesignDocId
import kotlin.math.roundToInt

const val helloWorldDocId = "uXeofwnITH51MgsYGfG4Yi"

@DesignDoc(id = helloWorldDocId)
interface HelloWorld {
    @DesignComponent(node = "#stage") fun mainFrame(
        @Design(node = "#Name") name: String,
        @Design(node = "#temp/left") tempLeft: String,
        @Design(node = "#temp/left/marker") tempLeftProgress: Meter,
        @Design(node = "#fan/speed") fanSpeed: String,
        @Design(node = "#fan/speed-progress") fanSpeedProgress: Meter,
        @Design(node = "#temp/left/button-left") tempLeftButtonDecrement: TapCallback,
        @Design(node = "#temp/left/button-right") tempLeftButtonIncrement: TapCallback,
        @Design(node = "#fan/button-down") fanButtonDown: TapCallback,
        @Design(node = "#fan/button-up") fanButtonUp: TapCallback,
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignSettings.enableLiveUpdates(this)
        setContent { Main() }
    }
}

enum class Mode {
    Light,
    Dark,
}

@Composable
fun Main() {
    val tempMin = 10f
    val tempMax = 30f
    val tempLeft = remember { mutableFloatStateOf(20f) }
    val fanLevelMin = 1f
    val fanLevelMax = 5f
    val fanLevel = remember { mutableFloatStateOf(1f) }
    val mode = remember { mutableStateOf(Mode.Light) }
    val docId = remember { mutableStateOf("uXeofwnITH51MgsYGfG4Yi") }

    val modeValues: VariableModeValues = hashMapOf(
        "material-theme" to mode.value.name
    )

    CompositionLocalProvider(LocalDesignDocSettings provides
            DesignDocSettings(useSquoosh = true)
    ) {
        DesignVariableCollection(null) {
            DesignVariableModeValues(modeValues) {
                DesignMaterialThemeProvider(useMaterialTheme = false) {
                    DesignDocOverride(docId = DesignDocId(docId.value)) {
                        HelloWorldDoc.mainFrame(
                            name = "World!",
                            tempLeft = tempLeft.value.roundToInt().toString(),
                            tempLeftProgress = (tempLeft.value - tempMin) / (tempMax - tempMin) * 100f,
                            fanSpeed = fanLevel.value.roundToInt().toString(),
                            fanSpeedProgress = (fanLevel.value - fanLevelMin) / (fanLevelMax - fanLevelMin) * 100f,
                            tempLeftButtonDecrement = {
                                tempLeft.value = (tempLeft.value - 1f).coerceAtLeast(tempMin)
                            },
                            tempLeftButtonIncrement = {
                                tempLeft.value = (tempLeft.value + 1f).coerceAtMost(tempMax)
                            },
                            fanButtonDown = {
                                println("### Down")
                                fanLevel.value = (fanLevel.value - 1f).coerceAtLeast(fanLevelMin)
                            },
                            fanButtonUp = {
                                println("### Up")
                                fanLevel.value = (fanLevel.value + 1f).coerceAtMost(fanLevelMax)
                            }
                        )
                    }
                }
            }
        }
    }
    Column(Modifier.offset(10.dp, 800.dp)) {
        Row {
            Button("Light", mode.value == Mode.Light) {
                mode.value = Mode.Light
            }
            Button("Dark", mode.value == Mode.Dark) {
                mode.value = Mode.Dark
            }
            Button("Hvac1", docId.value == "uXeofwnITH51MgsYGfG4Yi") {
                docId.value = "uXeofwnITH51MgsYGfG4Yi"
            }
            Button("Hvac2", docId.value == "rSJ736PPreaPSGwUHT2D3W") {
                docId.value = "rSJ736PPreaPSGwUHT2D3W"
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("  Fan: ", fontSize = 30.sp)
            Slider(fanLevel, fanLevelMin, fanLevelMax)
        }
    }
}

@Composable
internal fun Button(name: String, selected: Boolean, select: () -> Unit) {
    val textColor = if (selected) Color.Black else Color.Gray
    val borderColor = if (selected) Color.Black else Color.Gray
    var modifier =
        Modifier
            .padding(10.dp)
            .clickable { select() }
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .absolutePadding(10.dp, 2.dp, 10.dp, 2.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Text(name, fontSize = 30.sp, color = textColor)
    }
}

@Composable
internal fun Slider(value: MutableState<Float>, min: Float, max: Float, testTag: String? = null) {
    val density = LocalDensity.current.density
    val sliderMax = 400f * density
    val v = remember { mutableStateOf(sliderMax * (value.value - min) / (max - min)) }
    val testTagModifier = testTag?.let { Modifier.testTag(it) } ?: Modifier
    Box(
        modifier =
        Modifier.width(440.dp)
            .height(40.dp)
            .border(width = 2.dp, color = Color.Gray, shape = RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier =
            Modifier.offset {
                IntOffset(
                    v.value.roundToInt() + (5 * density).toInt(),
                    (5 * density).toInt()
                )
            }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state =
                    rememberDraggableState { delta ->
                        v.value =
                            java.lang.Float.max(
                                java.lang.Float.min(v.value + delta, sliderMax),
                                0f
                            )
                        value.value = min + (max - min) * v.value / sliderMax
                    }
                )
                .size(30.dp)
                .border(width = 25.dp, color = Color.Black, shape = RoundedCornerShape(5.dp))
                .then(testTagModifier)
        )
    }
}

@Preview(showBackground = true, widthDp = 700)
@Composable
fun ComposePreview() {
    //helloworldTheme { HelloWorldDoc.mainFrame(name = "Developer!") }
}
