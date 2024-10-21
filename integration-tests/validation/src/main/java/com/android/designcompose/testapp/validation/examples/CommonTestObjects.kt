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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun FakeBrowseItem(name: String) {
    Column(Modifier.border(1.dp, Color.Red).width(200.dp).height(240.dp)) {
        Box(Modifier.border(1.dp, Color.Black).background(Color.Blue).width(200.dp).height(200.dp))
        Text(name, fontSize = 20.sp)
    }
}

@Composable
fun AdaptiveButton(setAdaptive: (Boolean) -> Unit) {
    Box(
        modifier = Modifier.border(1.dp, Color.Black).width(130.dp).height(30.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier.clickable { setAdaptive(true) },
            text = "Adaptive",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
    Box(Modifier.width(10.dp))
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
                            (5 * density).toInt(),
                        )
                    }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state =
                            rememberDraggableState { delta ->
                                v.value =
                                    java.lang.Float.max(
                                        java.lang.Float.min(v.value + delta, sliderMax),
                                        0f,
                                    )
                                value.value = min + (max - min) * v.value / sliderMax
                            },
                    )
                    .size(30.dp)
                    .border(width = 25.dp, color = Color.Black, shape = RoundedCornerShape(5.dp))
                    .then(testTagModifier)
        )
    }
}

enum class ItemType {
    Grid,
    List,
}

enum class GridItemType {
    SectionTitle,
    VSectionTitle,
    RowGrid,
    RowList,
    ColGrid,
    ColList,
}

enum class PlayState {
    Play,
    Pause,
}

enum class ButtonState {
    On,
    Off,
    Blue,
    Green,
}

@Composable
fun NumColumnButton(num: Int, setNum: (Int) -> Unit, setAdaptive: (Boolean) -> Unit) {
    Box(
        modifier = Modifier.border(1.dp, Color.Black).width(30.dp).height(30.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier =
                Modifier.clickable {
                    setNum(num)
                    setAdaptive(false)
                },
            text = num.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
    Box(Modifier.width(10.dp))
}

@Composable
fun LazyGridItemSpans() {
    val (numColumns, setNumColumns) = remember { mutableStateOf(3) }
    val (adaptive, setAdaptive) = remember { mutableStateOf(false) }
    val horizontalSpacing = remember { mutableStateOf(10f) }
    val verticalSpacing = remember { mutableStateOf(10f) }
    val adaptiveMin = remember { mutableStateOf(200f) }
    Column(Modifier.offset(20.dp, 10.dp)) {
        Row(Modifier.height(50.dp)) {
            Text("Number of Columns: ", fontSize = 20.sp)
            NumColumnButton(1, setNumColumns, setAdaptive)
            NumColumnButton(2, setNumColumns, setAdaptive)
            NumColumnButton(3, setNumColumns, setAdaptive)
            NumColumnButton(4, setNumColumns, setAdaptive)
            NumColumnButton(5, setNumColumns, setAdaptive)
            AdaptiveButton(setAdaptive)
        }
        Row(Modifier.height(50.dp)) {
            Text("Horizontal Item Spacing: ", fontSize = 20.sp)
            Slider(horizontalSpacing, 0f, 200f)
            Text(horizontalSpacing.value.toString(), fontSize = 20.sp)
        }
        Row(Modifier.height(50.dp)) {
            Text("Vertical Item Spacing: ", fontSize = 20.sp)
            Slider(verticalSpacing, 0f, 200f)
            Text(verticalSpacing.value.toString(), fontSize = 20.sp)
        }
        Row(Modifier.height(50.dp)) {
            Text("Adaptive Min Spacing: ", fontSize = 20.sp)
            Slider(adaptiveMin, 1f, 400f)
            Text(adaptiveMin.value.toString(), fontSize = 20.sp)
        }
    }

    val adaptiveMinDp = (adaptiveMin.value.toInt()).dp
    val sections = (0 until 100).toList().chunked(5)
    Box(Modifier.offset(20.dp, 230.dp).border(2.dp, Color.Black)) {
        LazyVerticalGrid(
            modifier =
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp, 0.dp).height(1000.dp),
            columns =
                if (adaptive) GridCells.Adaptive(adaptiveMinDp) else GridCells.Fixed(numColumns),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing.value.toInt().dp),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing.value.toInt().dp),
        ) {
            sections.forEachIndexed { index, items ->
                item(span = { GridItemSpan(if (adaptive) maxLineSpan else numColumns) }) {
                    Text(
                        "This is section $index",
                        Modifier.border(2.dp, Color.Black).height(80.dp).wrapContentSize(),
                        fontSize = 26.sp,
                    )
                }
                items(
                    items,
                    // not required as it is the default
                    span = { GridItemSpan(1) },
                ) {
                    FakeBrowseItem("Item $it")
                }
            }
        }
    }
}
