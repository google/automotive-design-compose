/*
 * Copyright 2024 Google LLC
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

package com.android.designcompose

import android.os.Build
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.IntState
import androidx.compose.runtime.State
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.android.designcompose.definition.element.ShaderUniform
import com.android.designcompose.definition.element.ShaderUniformValueKt.floatVec
import com.android.designcompose.definition.element.floatColor
import com.android.designcompose.definition.element.shaderUniform
import com.android.designcompose.definition.element.shaderUniformValue

data class ShaderUniformCustomizations(
    val backgroundShaderUniforms: MutableList<ShaderUniform> = mutableListOf(),
    val backgroundShaderUniformStates: MutableList<State<ShaderUniform>> = mutableListOf(),
    val strokeShaderUniforms: MutableList<ShaderUniform> = mutableListOf(),
    val strokeShaderUniformStates: MutableList<State<ShaderUniform>> = mutableListOf(),
)

fun ShaderUniformCustomizations.customBackgroundShaderUniforms(
    vararg shaderUniforms: ShaderUniform
): ShaderUniformCustomizations {
    backgroundShaderUniforms.addAll(shaderUniforms)
    return this
}

fun ShaderUniformCustomizations.customStrokeShaderUniforms(
    vararg shaderUniforms: ShaderUniform
): ShaderUniformCustomizations {
    strokeShaderUniforms.addAll(shaderUniforms)
    return this
}

fun ShaderUniformCustomizations.customBackgroundShaderUniformStates(
    vararg shaderUniformStates: State<ShaderUniform>
): ShaderUniformCustomizations {
    backgroundShaderUniformStates.addAll(shaderUniformStates)
    return this
}

fun ShaderUniformCustomizations.customStrokeShaderUniformStates(
    vararg shaderUniformStates: State<ShaderUniform>
): ShaderUniformCustomizations {
    strokeShaderUniformStates.addAll(shaderUniformStates)
    return this
}

/** Helper functions to create shader uniforms from float or color objects. */
object ShaderHelper {
    // Preset uniform name for time.
    const val UNIFORM_TIME = "iTime"

    /**
     * Creates an infinite animating float state for time.
     *
     * `iTime` is a preset uniform which always present in the shader code with declaration `uniform
     * float iTime;`. For shader code that animates over `iTime`, we need to provide a state of
     * shader uniform customization for the `iTime` uniform.
     */
    @Composable
    fun getShaderUniformTimeFloatState(): FloatState {
        return if ("robolectric" != Build.FINGERPRINT)
            produceState(0f) {
                    while (true) {
                        withInfiniteAnimationFrameMillis { value = it / 1000f }
                    }
                }
                .asFloatState()
        else remember { mutableFloatStateOf(3.0f) }
    }

    /** Creates a shader uniform from a float value. */
    fun createShaderFloatUniform(name: String, value: Float): ShaderUniform {
        return shaderUniform {
            this.name = name
            this.value = shaderUniformValue { floatValue = value }
        }
    }

    /** Creates a state of shader uniform from a float state. */
    @Composable
    fun FloatState.toShaderUniformState(name: String): State<ShaderUniform> {
        return remember { derivedStateOf { createShaderFloatUniform(name, floatValue) } }
    }

    /** Creates a shader uniform from a color object. */
    fun androidx.compose.ui.graphics.Color.toShaderUniform(name: String): ShaderUniform {
        return shaderUniform {
            this.name = name
            this.value = shaderUniformValue {
                floatColorValue = floatColor {
                    r = red
                    g = green
                    b = blue
                    a = alpha
                }
            }
        }
    }

    /** Creates a state of shader uniform from a color state. */
    @Composable
    fun State<androidx.compose.ui.graphics.Color>.toShaderUniformState(
        name: String
    ): State<ShaderUniform> {
        return remember { derivedStateOf { value.toShaderUniform(name) } }
    }

    /** Creates a shader uniform from an int value. */
    fun createShaderIntUniform(name: String, value: Int): ShaderUniform {
        return shaderUniform {
            this.name = name
            this.value = shaderUniformValue { intValue = value }
        }
    }

    /** Creates a state of shader uniform from an int state. */
    @Composable
    fun IntState.toShaderUniformState(name: String): State<ShaderUniform> {
        return remember { derivedStateOf { createShaderIntUniform(name, intValue) } }
    }

    /** Create a shader uniform from float array. */
    fun createShaderFloatArrayUniform(name: String, value: FloatArray): ShaderUniform {
        return shaderUniform {
            this.name = name
            this.value = shaderUniformValue {
                floatVecValue = floatVec { floats.addAll(value.toList()) }
            }
        }
    }

    @Composable
    fun createShaderFloatArrayUniformState(
        name: String,
        state: State<FloatArray>,
    ): State<ShaderUniform> {
        return remember { derivedStateOf { createShaderFloatArrayUniform(name, state.value) } }
    }
}
