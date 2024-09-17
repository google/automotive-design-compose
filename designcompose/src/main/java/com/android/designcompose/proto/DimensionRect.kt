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

import com.android.designcompose.definition.element.dimensionRect
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.DimensionRect
import java.util.Optional

val Optional<DimensionRect>.start: Dimension
    get() =
        this.orElseThrow { NoSuchFieldException("Malformed data: DimensionRect unset") }
            .start
            .getDim()

val Optional<DimensionRect>.end: Dimension
    get() =
        this.orElseThrow { NoSuchFieldException("Malformed data: DimensionRect unset") }
            .end
            .getDim()

val Optional<DimensionRect>.top: Dimension
    get() =
        this.orElseThrow { NoSuchFieldException("Malformed data: DimensionRect unset") }
            .top
            .getDim()

val Optional<DimensionRect>.bottom: Dimension
    get() =
        this.orElseThrow { NoSuchFieldException("Malformed data: DimensionRect unset") }
            .bottom
            .getDim()

fun Optional<DimensionRect>.isDefault(): Boolean {
    return this.start is Dimension.Undefined &&
        this.end is Dimension.Undefined &&
        this.top is Dimension.Undefined &&
        this.bottom is Dimension.Undefined
}

fun newDimensionRectPointsZero(): Optional<DimensionRect> =
    Optional.of(
        DimensionRect(
            newDimensionProtoPoints(0f),
            newDimensionProtoPoints(0f),
            newDimensionProtoPoints(0f),
            newDimensionProtoPoints(0f),
        )
    )

internal fun com.android.designcompose.definition.element.DimensionRect.intoSerde() =
    com.android.designcompose.serdegen.DimensionRect(
        Optional.of(
            com.android.designcompose.serdegen.DimensionProto(Optional.of(start.intoSerde()))
        ),
        Optional.of(
            com.android.designcompose.serdegen.DimensionProto(Optional.of(end.intoSerde()))
        ),
        Optional.of(
            com.android.designcompose.serdegen.DimensionProto(Optional.of(top.intoSerde()))
        ),
        Optional.of(
            com.android.designcompose.serdegen.DimensionProto(Optional.of(bottom.intoSerde()))
        ),
    )

internal fun DimensionRect.intoProto() = dimensionRect {
    val s = this@intoProto
    start = s.start.getDim().intoProto()
    end = s.end.getDim().intoProto()
    top = s.top.getDim().intoProto()
    bottom = s.bottom.getDim().intoProto()
}
