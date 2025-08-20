/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.designcompose

import androidx.compose.ui.geometry.Size
import com.android.designcompose.definition.element.NumOrVar
import com.android.designcompose.definition.element.ViewShape
import com.android.designcompose.definition.view.ViewStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComputePathsTest {

    @Test
    fun testComputeRectPathsFast() {
        val style = ViewStyle.getDefaultInstance()
        val density = 1f
        val frameSize = Size(100f, 100f)
        val computedPaths =
            (ViewShape.newBuilder().setRect(ViewShape.Box.getDefaultInstance()).build())
                .computePaths(
                    style,
                    density,
                    frameSize,
                    null,
                    false,
                    0,
                    VariableState(),
                    ComputedPathCache(),
                )
        assertThat(computedPaths).isNotNull()
        assertThat(computedPaths.fills).hasSize(1)
        assertThat(computedPaths.strokes).hasSize(1)
        assertThat(computedPaths.shadowClips).hasSize(1)
        assertThat(computedPaths.shadowFills).isEmpty()
        assertThat(computedPaths.strokeCap).isNull()
    }

    @Test
    fun testComputeRoundRectPathsFast() {
        val style = ViewStyle.getDefaultInstance()
        val density = 1f
        val frameSize = Size(100f, 100f)

        // The method expects a NumOrVar object. Create one and set its "num" field.
        val cornerRadiusValue = NumOrVar.newBuilder().setNum(10f).build()

        // Build the RoundRect with the correctly typed corner radius value.
        val roundRectWithRadii =
            ViewShape.RoundRect.newBuilder()
                .addCornerRadii(cornerRadiusValue)
                .addCornerRadii(cornerRadiusValue)
                .addCornerRadii(cornerRadiusValue)
                .addCornerRadii(cornerRadiusValue)
                .build()

        val computedPaths =
            (ViewShape.newBuilder().setRoundRect(roundRectWithRadii).build()).computePaths(
                style,
                density,
                frameSize,
                null,
                false,
                0,
                VariableState(),
                ComputedPathCache(),
            )
        assertThat(computedPaths).isNotNull()
        assertThat(computedPaths.fills).hasSize(1)
        assertThat(computedPaths.strokes).hasSize(1)
        assertThat(computedPaths.shadowClips).hasSize(1)
        assertThat(computedPaths.shadowFills).isEmpty()
        assertThat(computedPaths.strokeCap).isNull()
    }
}
