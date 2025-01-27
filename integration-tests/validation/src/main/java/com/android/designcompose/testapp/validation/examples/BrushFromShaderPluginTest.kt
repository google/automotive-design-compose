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
import androidx.compose.runtime.State
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.android.designcompose.ShaderHelper
import com.android.designcompose.ShaderHelper.toShaderUniform
import com.android.designcompose.ShaderHelper.toShaderUniformState
import com.android.designcompose.ShaderUniformList
import com.android.designcompose.ShaderUniformStateList
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.definition.element.ShaderUniform
import com.android.designcompose.testapp.validation.R

@DesignDoc(id = "TkgjNl81e5joWeAivmIdzm")
interface BrushFromShaderPluginTest {
    @DesignComponent(node = "#stage")
    fun MainFrame(
        @Design(node = "#stage") rootShaderUniformStates: ShaderUniformStateList,
        @Design(node = "#stage") rootShaderUniforms: ShaderUniformList,
        @Design(node = "#color-custom") customColors: ShaderUniformList,
        @Design(node = "#text-color-custom") customTextColors: ShaderUniformList,
        @Design(node = "#color-state-custom") customColorStates: ShaderUniformStateList,
        @Design(node = "#int-state-custom") customIntStates: ShaderUniformStateList,
    )
}

@Composable
fun BrushFromShaderPluginTest() {
    // Create a animate state for iTime so the shader can animate over time.
    val iTimeState = ShaderHelper.getShaderUniformTimeState()
    val rootShaderUniformStates = ArrayList<State<ShaderUniform>>()
    rootShaderUniformStates.add(iTimeState.toShaderUniformState(ShaderHelper.UNIFORM_TIME))
    // DEV BRANCH: 5oAVZxBQCLCf7spBLLFRHw. This list tests multi-doc support.
    val rootShaderUniforms = ArrayList<ShaderUniform>()
    rootShaderUniforms.add(ShaderHelper.createShaderFloatUniform("speed", 0.05f))
    rootShaderUniforms.add(ShaderHelper.createShaderFloatUniform("clouddark", 0.5f))
    rootShaderUniforms.add(ShaderHelper.createShaderFloatUniform("cloudcover", 0.23f))

    val customColors = ArrayList<ShaderUniform>()
    customColors.add(
        Color(LocalContext.current.getColor(R.color.purple_200)).toShaderUniform("iColor")
    )

    val customColorStates = ArrayList<State<ShaderUniform>>()
    val colors =
        listOf(
            Color(LocalContext.current.getColor(R.color.purple_200)),
            Color(LocalContext.current.getColor(R.color.purple_700)),
        )
    val colorState = remember { derivedStateOf { colors[iTimeState.floatValue.toInt() % 2] } }
    customColorStates.add(colorState.toShaderUniformState("iColor"))

    val customIntStates = ArrayList<State<ShaderUniform>>()
    val intState = remember { derivedStateOf { iTimeState.floatValue.toInt() % 5 }.asIntState() }
    customIntStates.add(intState.toShaderUniformState("iCase"))
    BrushFromShaderPluginTestDoc.MainFrame(
        rootShaderUniformStates = rootShaderUniformStates,
        rootShaderUniforms = rootShaderUniforms,
        customColors = customColors,
        customTextColors = customColors,
        customColorStates = customColorStates,
        customIntStates = customIntStates,
    )
}
