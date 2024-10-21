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

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import org.intellij.lang.annotations.Language

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
class SizingShaderBrush(private val shader: RuntimeShader) : ShaderBrush() {
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
                    repeatMode = RepeatMode.Reverse,
                ),
        )

    // Android T introduces AGSL and RuntimeShader. Robolectric (part of our test infrastructure)
    // only supports software rendering.
    val brush: () -> Brush =
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                "robolectric" != Build.FINGERPRINT
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
