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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import org.intellij.lang.annotations.Language

@DesignDoc(id = "TkgjNl81e5joWeAivmIdzm")
interface ShaderTest {
    @DesignComponent(node = "#stage") fun Main(@Design(node = "#stage") fill: () -> Brush)
}

@Composable
fun ShaderTest() {
    val infiniteTransition = rememberInfiniteTransition(label = "animate shader")
    val movingValue =
        infiniteTransition.animateFloat(
            label = "moving value for shader",
            initialValue = 0.0f,
            targetValue = 10.0f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(10 * 1000, easing = LinearEasing),
                )
        )

    // Android T introduces AGSL and RuntimeShader. Robolectric (part of our test infrastructure)
    // only supports software rendering.
    val brush: () -> Brush =
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                "robolectric" != Build.FINGERPRINT
        ) {
            val shader = RuntimeShader(SHADER_ROAD)
            val shaderBrush = SizingShaderBrush(shader)
            // The kotlin compiler seems to get confused without semicolons on these statements.
            shader.setFloatUniform("iTime", 0.0f);

                // Only sample the state in the generator function; this means that we avoid
                // recomposition and only do the redraw phase.
            {
                shader.setFloatUniform("iTime", movingValue.value)
                shaderBrush
            }
        } else {
            { SolidColor(Color.Blue) }
        }

    ShaderTestDoc.Main(fill = brush)
}

// Source: https://www.shadertoy.com/view/XtlGW4
@Language("AGSL")
val SHADER_ROAD : String = """
uniform float iTime;
uniform float3 iResolution;
vec4 main(vec2 fragCoord) {
    vec3 q=iResolution,d=vec3(fragCoord.x-.5*q.x,fragCoord.y,q.y)/q.y,c=vec3(0,.2,.2);
    q=d/(.1 + .3 * d.y);
    float a=iTime, k=sin(.2*a), w = q.x *= q.x-=.005*q.z*q.z;
  
  	vec4 f;

    f.xyz=
    	sin(5.*q.z+20.*a)<0.?
        w>2.?c.xyx:w>1.4?d.zzz:c.yyy:
	    w>2.?c.xzx:w>1.4?c.zzz:(w>.004?c:d).zzz;
  return f;
}
""".trimIndent()

@Language("AGSL")
val SHADER_DOT_GRID : String = """
uniform float iTime;
uniform float3 iResolution;
uniform float iTime;
uniform float3 iResolution;
vec4 main(vec2 fragCoord) {
    float minDimension = min(iResolution.x, iResolution.y); 
    vec2 st = fragCoord / minDimension;

    vec2 gridSize = vec2(10.0, 16.0);

    // Calculate cell coordinates
    vec2 cellCoord = floor(st * gridSize); 

    // Center of each cell
    vec2 cellCenter = (cellCoord + vec2(0.5)) / gridSize;

    // Distance from pixel to cell center
    float distToCenter = distance(st, cellCenter);

    vec3 color = vec3(0.2);
    if (distToCenter < 0.002 + cellCenter.y * 0.001) {
        color = vec3(0.8); // Lighter gray for the dots
    }

    if (cellCoord.y < 0.05 && st.y > 0.02 && st.y < 0.04 && st.x < 2.0 / 3.0) { 
        color = vec3(0, 0, 1);
    }

    return vec4(color, 1.0);
}
""".trimIndent()
