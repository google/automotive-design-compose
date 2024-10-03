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

package com.android.designcompose.proto

import com.android.designcompose.serdegen.Background
import com.android.designcompose.serdegen.BackgroundType
import com.android.designcompose.serdegen.StrokeWeight
import com.android.designcompose.serdegen.StrokeWeightType
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

//
// StrokeWeight Helper Functions
//

// Return a "uniform" stroke weight even if we have individual weights. This is used for stroking
// vectors that don't have sides.
internal fun Optional<StrokeWeight>.toUniform(): Float {
    if (this.isPresent) {
        val weightTypeOpt = this.get().stroke_weight_type
        if (weightTypeOpt.isPresent) {
            when (val weightType = weightTypeOpt.get()) {
                is StrokeWeightType.Uniform -> return weightType.value
                is StrokeWeightType.Individual -> return weightType.value.top
            }
        }
    }
    return 0.0f
}

// Return a maximum stroke weight. This is used for computing the layer bounds when creating a
// layer for compositing (transparency, blend modes, etc).
internal fun Optional<StrokeWeight>.max(): Float {
    if (this.isPresent) {
        val weightTypeOpt = this.get().stroke_weight_type
        if (weightTypeOpt.isPresent) {
            when (val weightType = weightTypeOpt.get()) {
                is StrokeWeightType.Uniform -> return weightType.value
                is StrokeWeightType.Individual ->
                    return maxOf(
                        weightType.value.top,
                        weightType.value.left,
                        weightType.value.bottom,
                        weightType.value.right,
                    )
            }
        }
    }
    return 0.0f
}

internal fun Optional<StrokeWeight>.top(): Float {
    if (this.isPresent) {
        val weightTypeOpt = this.get().stroke_weight_type
        if (weightTypeOpt.isPresent) {
            when (val weightType = weightTypeOpt.get()) {
                is StrokeWeightType.Uniform -> return weightType.value
                is StrokeWeightType.Individual -> return weightType.value.top
            }
        }
    }
    return 0.0f
}

internal fun Optional<StrokeWeight>.left(): Float {
    if (this.isPresent) {
        val weightTypeOpt = this.get().stroke_weight_type
        if (weightTypeOpt.isPresent) {
            when (val weightType = weightTypeOpt.get()) {
                is StrokeWeightType.Uniform -> return weightType.value
                is StrokeWeightType.Individual -> return weightType.value.left
            }
        }
    }
    return 0.0f
}

internal fun Optional<StrokeWeight>.bottom(): Float {
    if (this.isPresent) {
        val weightTypeOpt = this.get().stroke_weight_type
        if (weightTypeOpt.isPresent) {
            when (val weightType = weightTypeOpt.get()) {
                is StrokeWeightType.Uniform -> return weightType.value
                is StrokeWeightType.Individual -> return weightType.value.bottom
            }
        }
    }
    return 0.0f
}

internal fun Optional<StrokeWeight>.right(): Float {
    if (this.isPresent) {
        val weightTypeOpt = this.get().stroke_weight_type
        if (weightTypeOpt.isPresent) {
            when (val weightType = weightTypeOpt.get()) {
                is StrokeWeightType.Uniform -> return weightType.value
                is StrokeWeightType.Individual -> return weightType.value.right
            }
        }
    }
    return 0.0f
}

//
// Background Helper functions
//

internal inline fun <reified T> Background.isType(): Boolean {
    val bgType = background_type.getOrNull()
    bgType?.let {
        return it is T
    }
    return false
}

internal fun Background.getType(): BackgroundType {
    val bgType = background_type.getOrNull()
    return bgType ?: BackgroundType.None(com.novi.serde.Unit())
}
