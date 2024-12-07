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

import android.os.Build
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.android.designcompose.ShaderUniformTimeState
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

@DesignDoc(id = "TkgjNl81e5joWeAivmIdzm")
interface BrushFromShaderPluginTest {
    @DesignComponent(node = "#stage")
    fun MainFrame(@Design(node = "#stage") backgroundShaderUniformTimeState: ShaderUniformTimeState)
}

@Composable
fun BrushFromShaderPluginTest() {
    val movingValue =
        if ("robolectric" != Build.FINGERPRINT) {
            produceState(0f) {
                while (true) {
                    withInfiniteAnimationFrameMillis { value = it / 1000f }
                }
            }
        } else {
            remember { mutableFloatStateOf(3.0f) }
        }
    BrushFromShaderPluginTestDoc.MainFrame(
        backgroundShaderUniformTimeState = movingValue.asFloatState()
    )
}
