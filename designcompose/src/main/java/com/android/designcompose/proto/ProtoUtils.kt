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
import com.android.designcompose.serdegen.Box
import com.android.designcompose.serdegen.FontWeight
import com.android.designcompose.serdegen.ItemSpacing
import com.android.designcompose.serdegen.LayoutStyle
import com.android.designcompose.serdegen.NodeStyle
import com.android.designcompose.serdegen.NumOrVar
import com.android.designcompose.serdegen.NumOrVarType
import com.android.designcompose.serdegen.Shape
import com.android.designcompose.serdegen.StrokeWeight
import com.android.designcompose.serdegen.StrokeWeightType
import com.android.designcompose.serdegen.Text
import com.android.designcompose.serdegen.Trigger
import com.android.designcompose.serdegen.TriggerType
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewDataType
import com.android.designcompose.serdegen.ViewShape
import com.android.designcompose.serdegen.ViewStyle
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

//
// StrokeWeight Helper Functions
//

// Return a "uniform" stroke weight even if we have individual weights. This is used for stroking
// vectors that don't have sides.
@Deprecated("This function will be removed in the future.")
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
@Deprecated("This function will be removed in the future.")
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

@Deprecated("This function will be removed in the future.")
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

@Deprecated("This function will be removed in the future.")
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

@Deprecated("This function will be removed in the future.")
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

@Deprecated("This function will be removed in the future.")
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

@Deprecated("This function will be removed in the future.")
internal fun Background.getType(): BackgroundType {
    val bgType = background_type.getOrNull()
    return bgType ?: BackgroundType.None(com.novi.serde.Unit())
}

@Deprecated("This function will be removed in the future.")
internal fun ViewShape.get(): Shape {
    return this.shape.orElseThrow {
        NoSuchFieldException("Malformed data: ViewShape has no shape field")
    }
}

@Deprecated("This function will be removed in the future.")
internal fun newViewShapeRect(isMask: Boolean) = ViewShape(Optional.of(Shape.Rect(Box(isMask))))

@Deprecated("This function will be removed in the future.")
internal fun newFontWeight(weight: Float) =
    FontWeight(Optional.of(NumOrVar(Optional.of(NumOrVarType.Num(weight)))))

@Deprecated("This function will be removed in the future.")
internal fun ItemSpacing.type() = item_spacing_type.getOrNull()

@Deprecated("This function will be removed in the future.")
internal fun newNumOrVar(value: Float) = NumOrVar(Optional.of(NumOrVarType.Num(value)))

@Deprecated("This function will be removed in the future.")
internal val ViewStyle.nodeStyle: NodeStyle
    get() =
        this.node_style.orElseThrow {
            NoSuchFieldException("Malformed data: ViewStyle has no node_style field")
        }

@Deprecated("This function will be removed in the future.")
internal val ViewStyle.layoutStyle: LayoutStyle
    get() =
        this.layout_style.orElseThrow {
            NoSuchFieldException("Malformed data: ViewStyle has no layout_style field")
        }

@Deprecated("This function will be removed in the future.")
internal fun Optional<ViewData>.getType() = this.get().view_data_type.get()

@Deprecated("This function will be removed in the future.")
internal fun Optional<ViewData>.ifContainerGetShape(): Shape? {
    return (this.get().view_data_type.get() as? ViewDataType.Container)?.value?.shape?.get()?.get()
}

@Deprecated("This function will be removed in the future.")
internal fun Optional<ViewData>.ifTextGetText(): Text? {
    return (this.get().view_data_type.get() as? ViewDataType.Text)?.value
}

internal fun Optional<Trigger>.isSupportedInteraction() =
    this.type is TriggerType.AfterTimeout ||
        this.type is TriggerType.Click ||
        this.type is TriggerType.Press

internal fun Optional<Trigger>.isPressOrClick() =
    this.type is TriggerType.Press || this.type is TriggerType.Click

internal fun Optional<Trigger>.isTimeout() = this.type is TriggerType.AfterTimeout
