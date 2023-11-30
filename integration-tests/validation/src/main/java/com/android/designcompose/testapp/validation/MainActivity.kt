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

package com.android.designcompose.testapp.validation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.DesignSettings
import com.android.designcompose.test.internal.interFont
import com.android.designcompose.testapp.validation.examples.EXAMPLES

const val TAG = "DesignCompose"

@DesignDoc(id = "oetCBVw8gCAxmCNllXx7zO")
interface CustomBrushTest {
    @DesignComponent(node = "#MainFrame") fun Main(@Design(node = "#CustomBrush") fill: () -> Brush)
}

// From: https://shaders.skia.org/
@Language("AGSL")
val CUSTOM_SHADER =
    """
uniform float3 iResolution;      // Viewport resolution (pixels)
uniform float  iTime;            // Shader playback time (s)
// Source: @notargs https://twitter.com/notargs/status/1250468645030858753
float f(vec3 p) {
    p.z -= iTime * 10.;
    float a = p.z * .1;
    p.xy *= mat2(cos(a), sin(a), -sin(a), cos(a));
    return .1 - length(cos(p.xy) + sin(p.yz));
}

half4 main(vec2 fragcoord) {
    vec3 d = .5 - fragcoord.xy1 / iResolution.y;
    vec3 p=vec3(0);
    for (int i = 0; i < 32; i++) {
      p += f(p) * d;
    }
    return ((sin(p) + vec3(2, 5, 9)) / length(p)).xyz1;
}
"""
        .trimIndent()

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class SizingShaderBrush(val shader: RuntimeShader) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        shader.setFloatUniform("iResolution", size.width, size.height, 0.0f)
        return shader
    }
}

@Composable
fun CustomBrushTest() {
    val infiniteTransition = rememberInfiniteTransition(label = "animate shader")
    val movingValue =
        infiniteTransition.animateFloat(
            label = "moving value for shader",
            initialValue = 0.0f,
            targetValue = 10.0f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(10 * 1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
        )

    // Android T introduces AGSL and RuntimeShader. Robolectric (part of our test infrastructure)
    // only supports software rendering.
    val brush: () -> Brush =
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !"robolectric".equals(Build.FINGERPRINT)
        ) {
            val shader = RuntimeShader(CUSTOM_SHADER)
            val shaderBrush = SizingShaderBrush(shader)
            // The kotlin compiler seems to get confused without semicolons on these statements.
            shader.setFloatUniform("iTime", 0.0f);

            {
                // Only sample the state in the generator function; this means that we avoid
                // recomposition and only do the redraw phase.
                shader.setFloatUniform("iTime", movingValue.value)
                shaderBrush
            }
        } else {
            { SolidColor(Color.Blue) }
        }

    CustomBrushTestDoc.Main(fill = brush)
}
// Main Activity class. Setup auth token and font, then build the UI with buttons for each test
// on the left and the currently selected test on the right.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignSettings.addFontFamily("Inter", interFont)
        DesignSettings.enableLiveUpdates(this)

        setContent {
            val index = remember { mutableStateOf(0) }
            Row {
                TestButtons(index)
                Divider(color = Color.Black, modifier = Modifier.fillMaxHeight().width(1.dp))
                TestContent(index.value)
            }
        }
    }
}

// Draw all the buttons on the left side of the screen, one for each test
@Composable
fun TestButtons(index: MutableState<Int>) {

    Column(Modifier.width(110.dp).verticalScroll(rememberScrollState())) {
        var count = 0
        EXAMPLES.forEach {
            TestButton(it.first, index, count)
            Divider(color = Color.Black, modifier = Modifier.height(1.dp))
            ++count
        }
    }
}

// Draw a single button
@Composable
fun TestButton(name: String, currentIndex: MutableState<Int>, myIndex: Int) {
    val weight = if (currentIndex.value == myIndex) FontWeight.Bold else FontWeight.Normal

    Text(
        modifier =
            Modifier.clickable {
                Log.i(TAG, "Button $name")
                currentIndex.value = myIndex
            },
        text = name,
        fontSize = 20.sp,
        fontWeight = weight
    )
}

// Draw the content for the current test
@Composable
fun TestContent(index: Int) {
    Box { EXAMPLES[index].second() }
}
