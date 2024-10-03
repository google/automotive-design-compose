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

package com.android.designcompose.testapp.validation.examples

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.DesignMaterialThemeProvider
import com.android.designcompose.DesignVariableCollection
import com.android.designcompose.DesignVariableModeValues
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

enum class LightDarkMode {
    Default,
    Light,
    Dark,
}

enum class Theme(val themeName: String) {
    Material("material-theme"),
    MyTheme("my-theme"),
}

@DesignDoc(id = "HhGxvL4aHhP8ALsLNz56TP")
interface VariablesTest {
    @DesignComponent(node = "#stage")
    fun Main(
        @Design(node = "#TopRight") topRight: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#BottomRight")
        bottomRight: @Composable (ComponentReplacementContext) -> Unit,
    )

    @DesignComponent(node = "#Box") fun Box(@Design(node = "#name") name: String)
}

@Composable
fun VariableModesTest() {
    // The variable theme (collection) override to use from Figma
    val theme = remember { mutableStateOf<Theme?>(null) }
    // The mode override
    val mode = remember { mutableStateOf(LightDarkMode.Default) }
    // If true, override any variable using material theme with the device's material theme
    val useMaterialOverride = remember { mutableStateOf(false) }
    // Top right node theme override
    val trTheme = remember { mutableStateOf<Theme?>(null) }
    // Top right node mode override
    val trMode = remember { mutableStateOf(LightDarkMode.Default) }
    // Bottom right node theme override
    val brTheme = remember { mutableStateOf<Theme?>(null) }
    // Bottom right node mode override
    val brMode = remember { mutableStateOf(LightDarkMode.Default) }

    val themeName = theme.value?.themeName
    val modeValues =
        if (themeName != null && mode.value != LightDarkMode.Default)
            hashMapOf(themeName to mode.value.name)
        else null

    val trThemeName = trTheme.value?.themeName
    val trModeValues =
        if (trThemeName != null && trMode.value != LightDarkMode.Default)
            hashMapOf(trThemeName to trMode.value.name)
        else null

    val brThemeName = brTheme.value?.themeName
    val brModeValues =
        if (brThemeName != null && brMode.value != LightDarkMode.Default)
            hashMapOf(brThemeName to brMode.value.name)
        else null

    DesignVariableCollection(themeName) {
        DesignVariableModeValues(modeValues) {
            DesignMaterialThemeProvider(useMaterialTheme = useMaterialOverride.value) {
                VariablesTestDoc.Main(
                    topRight = {
                        DesignVariableCollection(trThemeName) {
                            DesignVariableModeValues(trModeValues) {
                                VariablesTestDoc.Box(name = "Top Right")
                            }
                        }
                    },
                    bottomRight = {
                        DesignVariableCollection(brThemeName) {
                            DesignVariableModeValues(brModeValues) {
                                VariablesTestDoc.Box(name = "Bottom Right")
                            }
                        }
                    },
                )
            }
        }
    }

    Column(Modifier.offset(10.dp, 800.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Root Theme", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.TestButton(
                "None",
                "RootThemeNone",
                theme.value == null,
            ) {
                theme.value = null
                useMaterialOverride.value = false
            }
            com.android.designcompose.testapp.validation.TestButton(
                "Material (Figma)",
                "MaterialFigma",
                theme.value == Theme.Material,
            ) {
                theme.value = Theme.Material
                useMaterialOverride.value = false
            }
            com.android.designcompose.testapp.validation.TestButton(
                "MyTheme (Figma)",
                "MyThemeFigma",
                theme.value == Theme.MyTheme,
            ) {
                theme.value = Theme.MyTheme
                useMaterialOverride.value = false
            }
            com.android.designcompose.testapp.validation.TestButton(
                "Material (Device)",
                "MaterialDevice",
                useMaterialOverride.value,
            ) {
                theme.value = null
                useMaterialOverride.value = true
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Root Mode", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.TestButton(
                "Default",
                "RootModeDefault",
                mode.value == LightDarkMode.Default,
            ) {
                mode.value = LightDarkMode.Default
            }
            com.android.designcompose.testapp.validation.TestButton(
                "Light",
                "RootModeLight",
                mode.value == LightDarkMode.Light,
            ) {
                mode.value = LightDarkMode.Light
            }
            com.android.designcompose.testapp.validation.TestButton(
                "Dark",
                "RootModeDark",
                mode.value == LightDarkMode.Dark,
            ) {
                mode.value = LightDarkMode.Dark
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Top Right Theme", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.TestButton(
                "None",
                "TopRightNone",
                trTheme.value == null,
            ) {
                trTheme.value = null
            }
            com.android.designcompose.testapp.validation.TestButton(
                "Material",
                "TopRightMaterial",
                trTheme.value == Theme.Material,
            ) {
                trTheme.value = Theme.Material
            }
            com.android.designcompose.testapp.validation.TestButton(
                "MyTheme",
                "TopRightMyTheme",
                trTheme.value == Theme.MyTheme,
            ) {
                trTheme.value = Theme.MyTheme
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Top Right Mode", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.TestButton(
                "Default",
                "TopRightDefault",
                trMode.value == LightDarkMode.Default,
            ) {
                trMode.value = LightDarkMode.Default
            }
            com.android.designcompose.testapp.validation.TestButton(
                "Light",
                "TopRightLight",
                trMode.value == LightDarkMode.Light,
            ) {
                trMode.value = LightDarkMode.Light
            }
            com.android.designcompose.testapp.validation.TestButton(
                "Dark",
                "TopRightDark",
                trMode.value == LightDarkMode.Dark,
            ) {
                trMode.value = LightDarkMode.Dark
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bottom Right Theme", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button("None", brTheme.value == null) {
                brTheme.value = null
            }
            com.android.designcompose.testapp.validation.Button(
                "Material",
                brTheme.value == Theme.Material,
            ) {
                brTheme.value = Theme.Material
            }
            com.android.designcompose.testapp.validation.Button(
                "MyTheme",
                brTheme.value == Theme.MyTheme,
            ) {
                brTheme.value = Theme.MyTheme
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bottom Right Mode", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button(
                "Default",
                brMode.value == LightDarkMode.Default,
            ) {
                brMode.value = LightDarkMode.Default
            }
            com.android.designcompose.testapp.validation.Button(
                "Light",
                brMode.value == LightDarkMode.Light,
            ) {
                brMode.value = LightDarkMode.Light
            }
            com.android.designcompose.testapp.validation.Button(
                "Dark",
                brMode.value == LightDarkMode.Dark,
            ) {
                brMode.value = LightDarkMode.Dark
            }
        }
    }
}
