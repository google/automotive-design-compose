/*
 * Copyright 2025 Google LLC
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

import com.android.designcompose.definition.element.shaderUniform
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShaderHelperTest {
    @Test
    fun testShaderUniformCustomizations() {
        val customizations = ShaderUniformCustomizations()
        val uniform1 = shaderUniform { name = "uniform1" }
        val uniform2 = shaderUniform { name = "uniform2" }

        customizations.customBackgroundShaderUniforms(uniform1)
        assertThat(customizations.backgroundShaderUniforms).containsKey("uniform1")
        assertThat(customizations.backgroundShaderUniforms["uniform1"]).isEqualTo(uniform1)

        customizations.customStrokeShaderUniforms(uniform2)
        assertThat(customizations.strokeShaderUniforms).containsKey("uniform2")
        assertThat(customizations.strokeShaderUniforms["uniform2"]).isEqualTo(uniform2)
    }

    @Test
    fun testCreateShaderFloatUniform() {
        val uniform = ShaderHelper.createShaderFloatUniform("test", 1.0f)
        assertThat(uniform.name).isEqualTo("test")
        assertThat(uniform.value.floatValue).isEqualTo(1.0f)
    }
}
