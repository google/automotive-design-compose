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

import com.android.designcompose.definition.element.dimensionProto
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.Dimension.Undefined
import com.android.designcompose.serdegen.DimensionProto
import com.google.protobuf.Empty
import com.novi.serde.Unit
import java.util.Optional

/** @deprecated This function will be removed in the future. */
@Deprecated("This function will be removed in the future.")
fun Optional<DimensionProto>.getDim(): Dimension =
    this.orElseThrow { NoSuchFieldException("Malformed data: DimensionProto unset") }
        .dimension
        .orElseThrow { NoSuchFieldException("Malformed data: DimensionProto's dimension unset") }

/** @deprecated This function will be removed in the future. */
@Deprecated("This function will be removed in the future.")
fun newDimensionProtoPoints(value: Float = 0f) =
    Optional.of(DimensionProto(Optional.of(Dimension.Points(value))))

/** @deprecated This function will be removed in the future. */
@Deprecated("This function will be removed in the future.")
fun newDimensionProtoPercent(value: Float = 0f) =
    Optional.of(DimensionProto(Optional.of(Dimension.Percent(value))))

@Deprecated("This function will be removed in the future.")
fun newDimensionProtoUndefined() =
    Optional.of(DimensionProto(Optional.of(Dimension.Undefined(com.novi.serde.Unit()))))

@Deprecated("This function will be removed in the future.")
fun Dimension.toOptDimProto() = Optional.of(DimensionProto(Optional.of(this)))

@Deprecated("This function will be removed in the future.")
internal fun com.android.designcompose.definition.element.DimensionProto.intoSerde(): Dimension =
    when (dimensionCase) {
        com.android.designcompose.definition.element.DimensionProto.DimensionCase.UNDEFINED ->
            Dimension.Undefined(Unit())
        com.android.designcompose.definition.element.DimensionProto.DimensionCase.AUTO ->
            Dimension.Auto(Unit())
        com.android.designcompose.definition.element.DimensionProto.DimensionCase.POINTS ->
            Dimension.Points(points)
        com.android.designcompose.definition.element.DimensionProto.DimensionCase.PERCENT ->
            Dimension.Percent(percent)
        else ->
            throw IllegalArgumentException("Unknown DimensionProto: $this") // Should never happen.
    }

/** @deprecated This function will be removed in the future. */
@Deprecated("This function will be removed in the future.")
fun com.android.designcompose.definition.element.DimensionProto.into(): DimensionProto =
    com.android.designcompose.serdegen.DimensionProto(Optional.of(this.intoSerde()))

/** @deprecated This function will be removed in the future. */
@Deprecated("This function will be removed in the future.")
internal fun Dimension.intoProto(): com.android.designcompose.definition.element.DimensionProto =
    dimensionProto {
        when (val s = this@intoProto) {
            // These are empty types so we need to set them to default instances
            is Undefined -> undefined = Empty.getDefaultInstance()
            is Dimension.Auto -> auto = Empty.getDefaultInstance()
            is Dimension.Points -> points = s.value.toFloat()
            is Dimension.Percent -> percent = s.value.toFloat()
        }
    }
