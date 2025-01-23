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

package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant

enum class BoxType {
    Blue,
    Red,
}

enum class CircleType {
    Green,
    Purple,
}

@DesignDoc(id = "lpUWHv5gZh6WOx56pRnJ2s")
interface VariantNesting {
    @DesignComponent(node = "#stage")
    fun Main(@Design(node = "#replace") replace: @Composable (ComponentReplacementContext) -> Unit)

    @DesignComponent(node = "#Box")
    fun Box(
        @DesignVariant(property = "#BoxType") boxType: BoxType,
        @DesignVariant(property = "#CircleType") circleType: CircleType,
    )
}

@Composable
fun VariantNesting() {
    VariantNestingDoc.Main(
        replace = { VariantNestingDoc.Box(boxType = BoxType.Blue, circleType = CircleType.Purple) }
    )
}
