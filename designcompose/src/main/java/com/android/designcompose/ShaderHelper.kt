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

import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.IntState
import androidx.compose.runtime.State
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.android.designcompose.definition.element.ShaderUniform
import com.android.designcompose.definition.element.ShaderUniformValueKt.floatVec
import com.android.designcompose.definition.element.ShaderUniformValueKt.intVec
import com.android.designcompose.definition.element.floatColor
import com.android.designcompose.definition.element.shaderUniform
import com.android.designcompose.definition.element.shaderUniformValue
import kotlinx.coroutines.delay

data class ShaderUniformCustomizations(
    val backgroundShaderUniforms: HashMap<String, ShaderUniform> = HashMap(),
    val backgroundShaderUniformStates: HashMap<String, State<ShaderUniform>> = HashMap(),
    val strokeShaderUniforms: HashMap<String, ShaderUniform> = HashMap(),
    val strokeShaderUniformStates: HashMap<String, State<ShaderUniform>> = HashMap(),
)

fun ShaderUniformCustomizations.customBackgroundShaderUniforms(
    vararg shaderUniforms: ShaderUniform
): ShaderUniformCustomizations {
    for (shaderUniform in shaderUniforms) {
        backgroundShaderUniforms[shaderUniform.name] = shaderUniform
    }
    return this
}

fun ShaderUniformCustomizations.customStrokeShaderUniforms(
    vararg shaderUniforms: ShaderUniform
): ShaderUniformCustomizations {
    for (shaderUniform in shaderUniforms) {
        strokeShaderUniforms[shaderUniform.name] = shaderUniform
    }
    return this
}

fun ShaderUniformCustomizations.customBackgroundShaderUniformStates(
    vararg shaderUniformStates: State<ShaderUniform>
): ShaderUniformCustomizations {
    for (shaderUniformState in shaderUniformStates) {
        backgroundShaderUniformStates[shaderUniformState.value.name] = shaderUniformState
    }
    return this
}

fun ShaderUniformCustomizations.customStrokeShaderUniformStates(
    vararg shaderUniformStates: State<ShaderUniform>
): ShaderUniformCustomizations {
    for (shaderUniformState in shaderUniformStates) {
        strokeShaderUniformStates[shaderUniformState.value.name] = shaderUniformState
    }
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
        val context = LocalContext.current
        val frameInterval =
            context.resources.getInteger(R.integer.config_shader_frame_interval_ms).toLong()
        val animationDurationScale = remember {
            mutableFloatStateOf(getAnimatorDurationScale(context))
        }

        DisposableEffect(context) {
            val observer =
                object : ContentObserver(null) {
                    override fun onChange(selfChange: Boolean) {
                        // Update the setting value
                        animationDurationScale.floatValue = getAnimatorDurationScale(context)
                    }
                }

            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                true,
                observer,
            )

            // Cleanup when the composable leaves the composition
            onDispose { context.contentResolver.unregisterContentObserver(observer) }
        }
        if ("robolectric" != Build.FINGERPRINT) {
            return produceState(0f, animationDurationScale.floatValue) {
                    if (animationDurationScale.floatValue == 0f) {
                        return@produceState
                    }
                    val startTime = withInfiniteAnimationFrameMillis { it }
                    while (true) {
                        val currentTime = withInfiniteAnimationFrameMillis { it }
                        value =
                            (currentTime - startTime) / 1000f / animationDurationScale.floatValue
                        delay(frameInterval * animationDurationScale.floatValue.toLong())
                    }
                }
                .asFloatState()
        } else return remember { mutableFloatStateOf(3.0f) }
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

    /** Creates a shader uniform from float array. */
    fun createShaderFloatArrayUniform(name: String, value: FloatArray): ShaderUniform {
        return shaderUniform {
            this.name = name
            this.value = shaderUniformValue {
                floatVecValue = floatVec { floats.addAll(value.toList()) }
            }
        }
    }

    /** Creates a state of shader uniform from a float array state. */
    @Composable
    fun createShaderFloatArrayUniformState(
        name: String,
        state: State<FloatArray>,
    ): State<ShaderUniform> {
        return remember { derivedStateOf { createShaderFloatArrayUniform(name, state.value) } }
    }

    /** Creates a shader uniform from int array. */
    fun createShaderIntArrayUniform(name: String, value: IntArray): ShaderUniform {
        return shaderUniform {
            this.name = name
            this.value = shaderUniformValue { intVecValue = intVec { ints.addAll(value.toList()) } }
        }
    }

    /** Creates a state of shader uniform from an int array state. */
    @Composable
    fun createShaderIntArrayUniformState(
        name: String,
        state: State<IntArray>,
    ): State<ShaderUniform> {
        return remember { derivedStateOf { createShaderIntArrayUniform(name, state.value) } }
    }

    /**
     * Returns animator duration scale on the device. Applying the duration scale to the animation
     * to keep the shader behavior consistent across the BrushTest which uses a [InfiniteTransition]
     * and the BrushFromShaderPluginTest which simulates iTime using
     * [withInfiniteAnimationFrameMillis].
     */
    private fun getAnimatorDurationScale(context: Context): Float {
        return try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
            )
        } catch (e: Settings.SettingNotFoundException) {
            // If the setting is not found, return default value of 1.0f.
            1f
        }
    }
}
