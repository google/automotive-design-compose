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
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.Meter
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant

enum class TestState {
    A,
    B,
}

@DesignDoc(id = "RW3lFurXCoVDeqY2Y7bf4v", designFeatures = ["shader"])
interface SmartAnimateTest {
    @DesignComponent(node = "#MainFrame")
    fun MainFrame(
        @Design(node = "#ReplaceMe") replacement: @Composable (ComponentReplacementContext) -> Unit
    )

    @DesignComponent(node = "#BlueBox") fun BlueBox()

    @DesignComponent(node = "#RotationAnimationStage")
    fun RotationTest(@DesignVariant(property = "#RotationTestState") state: TestState)

    @DesignComponent(node = "#OpacityAnimationStage")
    fun OpacityTest(@DesignVariant(property = "#OpacityTestState") state: TestState)

    @DesignComponent(node = "#TextAnimationStage")
    fun TextTest(@DesignVariant(property = "#TextTestState") state: TestState)

    @DesignComponent(node = "#VectorAnimationStage")
    fun VectorTest(@DesignVariant(property = "#VectorTestState") state: TestState)

    @DesignComponent(node = "#CrossFadeOpacityAnimationStage")
    fun CrossFadeOpacityTest(
        @DesignVariant(property = "#CrossFadeOpacityTestState") state: TestState
    )

    @DesignComponent(node = "#ArcAnimationStage")
    fun ArcTest(@DesignVariant(property = "#ArcTestState") state: TestState)

    @DesignComponent(node = "#ShaderAnimationStage")
    fun ShaderTest(@DesignVariant(property = "#ShaderTestState") state: TestState)

    @DesignComponent(node = "#OnPressAnimationStage") fun OnPressTest()

    @DesignComponent(node = "#ProgressBarAnimationStage")
    fun ProgressBarTest(
        @Design(node = "#progress-bar") progress: Meter,
        @Design(node = "#progress-indicator") progressIndicator: Meter,
    )

    @DesignComponent(node = "#OneInstance") fun OneInstance()
}

@Composable
fun SmartAnimateTest() {
    SmartAnimateTestDoc.MainFrame(replacement = { SmartAnimateTestDoc.BlueBox() })
}

@Composable
fun SmartAnimateOneInstanceTest() {
    SmartAnimateTestDoc.OneInstance()
}
