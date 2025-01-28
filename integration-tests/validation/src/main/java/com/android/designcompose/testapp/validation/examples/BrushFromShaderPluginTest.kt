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
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.android.designcompose.ShaderHelper
import com.android.designcompose.ShaderHelper.toShaderUniform
import com.android.designcompose.ShaderHelper.toShaderUniformState
import com.android.designcompose.ShaderUniformCustomizations
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.customBackgroundShaderUniformStates
import com.android.designcompose.customBackgroundShaderUniforms
import com.android.designcompose.customStrokeShaderUniformStates
import com.android.designcompose.testapp.validation.R

@DesignDoc(id = "TkgjNl81e5joWeAivmIdzm")
interface BrushFromShaderPluginTest {
    @DesignComponent(node = "#stage")
    fun MainFrame(
        @Design(node = "#stage") rootShaderUniformCustomizations: ShaderUniformCustomizations,
        @Design(node = "#color-custom") customColors: ShaderUniformCustomizations,
        @Design(node = "#text-color-custom") customTextColors: ShaderUniformCustomizations,
        @Design(node = "#color-state-custom") customColorStates: ShaderUniformCustomizations,
        @Design(node = "#int-state-custom") customIntStates: ShaderUniformCustomizations,
        @Design(node = "#stroke") customStrokeStates: ShaderUniformCustomizations,
        @Design(node = "#text-stroke") customTextStrokeStates: ShaderUniformCustomizations,
    )
}

@Composable
fun BrushFromShaderPluginTest() {
    // Create a animate state for iTime so the shader can animate over time.
    val iTimeState = ShaderHelper.getShaderUniformTimeState()
    val iTimeShaderUniformState = iTimeState.toShaderUniformState(ShaderHelper.UNIFORM_TIME)

    // DEV BRANCH: 5oAVZxBQCLCf7spBLLFRHw. This list tests multi-doc support.
    val rootShaderUniformCustomizations = ShaderUniformCustomizations()
    rootShaderUniformCustomizations
        .customBackgroundShaderUniforms(
            ShaderHelper.createShaderFloatUniform("speed", 0.05f),
            ShaderHelper.createShaderFloatUniform("clouddark", 0.5f),
            ShaderHelper.createShaderFloatUniform("cloudcover", 0.23f),
        )
        .customBackgroundShaderUniformStates(iTimeShaderUniformState)

    val customColors = ShaderUniformCustomizations()
    customColors.customBackgroundShaderUniforms(
        Color(LocalContext.current.getColor(R.color.purple_200)).toShaderUniform("iColor")
    )

    val colors =
        listOf(
            Color(LocalContext.current.getColor(R.color.purple_200)),
            Color(LocalContext.current.getColor(R.color.purple_700)),
        )
    val colorState = remember { derivedStateOf { colors[iTimeState.floatValue.toInt() % 2] } }
    val customColorStates = ShaderUniformCustomizations()
    customColorStates.customBackgroundShaderUniformStates(colorState.toShaderUniformState("iColor"))

    val intState = remember { derivedStateOf { iTimeState.floatValue.toInt() % 5 }.asIntState() }
    val customIntStates = ShaderUniformCustomizations()
    customIntStates.customBackgroundShaderUniformStates(intState.toShaderUniformState("iCase"))
    BrushFromShaderPluginTestDoc.MainFrame(
        rootShaderUniformCustomizations = rootShaderUniformCustomizations,
        customColors = customColors,
        customTextColors = customColors,
        customColorStates = customColorStates,
        customIntStates = customIntStates,
        customStrokeStates =
            ShaderUniformCustomizations().customStrokeShaderUniformStates(iTimeShaderUniformState),
        customTextStrokeStates =
            ShaderUniformCustomizations().customStrokeShaderUniformStates(iTimeShaderUniformState),
    )
}
