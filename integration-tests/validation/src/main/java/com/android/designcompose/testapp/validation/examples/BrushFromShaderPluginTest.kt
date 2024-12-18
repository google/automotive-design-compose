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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import com.android.designcompose.ShaderFloatStateUniformMap
import com.android.designcompose.ShaderFloatUniformMap
import com.android.designcompose.ShaderHelper
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

@DesignDoc(id = "TkgjNl81e5joWeAivmIdzm")
interface BrushFromShaderPluginTest {
    @DesignComponent(node = "#stage")
    fun MainFrame(
        @Design(node = "#stage") shaderFloatStateUniformMap: ShaderFloatStateUniformMap,
        @Design(node = "#stage") shaderFloatUniformMap: ShaderFloatUniformMap,
    )
}

@Composable
fun BrushFromShaderPluginTest() {
    val shaderFloatStateUniformMap = HashMap<String, FloatState>()
    shaderFloatStateUniformMap[ShaderHelper.UNIFORM_TIME] = ShaderHelper.getShaderUniformTimeState()
    val shaderFloatUniformMap = HashMap<String, Float>()
    shaderFloatUniformMap["speed"] = 0.5f
    shaderFloatUniformMap["clouddark"] = 0.5f
    shaderFloatUniformMap["cloudcover"] = 0.23f
    BrushFromShaderPluginTestDoc.MainFrame(
        shaderFloatStateUniformMap = shaderFloatStateUniformMap,
        shaderFloatUniformMap = shaderFloatUniformMap,
    )
}
