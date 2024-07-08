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
import androidx.compose.runtime.CompositionLocalProvider
import com.android.designcompose.DesignDocSettings
import com.android.designcompose.LocalDesignDocSettings
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant

enum class TestState {
    A,
    B
}

@DesignDoc(id = "RW3lFurXCoVDeqY2Y7bf4v")
interface SmartAnimateTest {
    @DesignComponent(node = "#MainFrame") fun MainFrame()

    @DesignComponent(node = "#RotationAnimationStage")
    fun RotationTest(
        @DesignVariant(property = "#RotationTestState") state: TestState,
    )

    @DesignComponent(node = "#OneInstance") fun OneInstance()
}

@Composable
fun SmartAnimateTest() {
    CompositionLocalProvider(LocalDesignDocSettings provides DesignDocSettings(useSquoosh = true)) {
        SmartAnimateTestDoc.MainFrame()
    }
}

@Composable
fun SmartAnimateOneInstanceTest() {
    SmartAnimateTestDoc.OneInstance()
}
