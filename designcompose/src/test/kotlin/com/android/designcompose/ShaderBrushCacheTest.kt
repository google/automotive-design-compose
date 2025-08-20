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

import android.graphics.RuntimeShader
import androidx.compose.ui.graphics.Brush
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShaderBrushCacheTest {
    @Test
    fun testGetAndPut() {
        val cache = ShaderBrushCache()
        val brush = Brush.horizontalGradient()
        cache.put(1, "view1", brush)
        assertThat(cache.get(1, "view1")).isEqualTo(brush)
    }

    @Test
    fun testSizingShaderBrush() {
        val shader = RuntimeShader("void main() {}")
        val brush = SizingShaderBrush(shader)
        assertThat(brush.shader).isEqualTo(shader)
    }
}
