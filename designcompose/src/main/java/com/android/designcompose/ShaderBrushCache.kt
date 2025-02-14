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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.collection.LruCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush

class ShaderBrushCache {
    private val cache = LruCache<String, Brush>(64)

    fun get(layoutId: Int?, viewId: String): Brush? {
        if (layoutId == null) return null
        return cache["$layoutId:$viewId"]
    }

    fun put(layoutId: Int, viewId: String, shaderBrush: Brush) {
        cache.put("$layoutId:$viewId", shaderBrush)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class SizingShaderBrush(val shader: RuntimeShader) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        shader.setFloatUniform("iResolution", size.width, size.height, 0.0f)
        return shader
    }
}
