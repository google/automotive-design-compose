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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant

enum class SquareBorder {
    Sharp,
    Curved
}

enum class Shape {
    Circle,
    Square,
}
// TEST Variant Extra Properties Test
// This tests that even though the Figma doc has four variant properties for the component named
// #SquareBorder, we can only use two in the code and pick a variant that matches the two.

@DesignDoc(id = "4P7zDdrQxj7FZsKJoIQcx1")
interface VariantPropertiesTest {
    @DesignComponent(node = "#MainFrame")
    fun MainFrame(
        @Design(node = "#Square1") square1: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#Square2") square2: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#Square3") square3: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#Square4") square4: @Composable (ComponentReplacementContext) -> Unit,
        @DesignVariant(property = "#bg1") bg1: Shape,
        @DesignVariant(property = "#bg2") bg2: Shape,
        @DesignVariant(property = "#SquareBorder") type: SquareBorder,
        @DesignVariant(property = "#SquareColor") color: SquareColor,
    )

    @DesignComponent(node = "#SquareBorder")
    fun Square(
        @DesignVariant(property = "#SquareBorder") type: SquareBorder,
        @DesignVariant(property = "#SquareColor") color: SquareColor,
    )
}

@Composable
fun VariantPropertiesTest() {
    val (bg1, setBg1) = remember { mutableStateOf(Shape.Circle) }
    val (bg2, setBg2) = remember { mutableStateOf(Shape.Circle) }
    val (borderType, setBorderType) = remember { mutableStateOf(SquareBorder.Sharp) }
    val (color, setColor) = remember { mutableStateOf(SquareColor.Green) }

    VariantPropertiesTestDoc.MainFrame(
        square1 = {
            VariantPropertiesTestDoc.Square(
                modifier = it.layoutModifier.then(it.appearanceModifier),
                parentLayout = it.parentLayout,
                type = SquareBorder.Sharp,
                color = SquareColor.Blue
            )
        },
        square2 = {
            VariantPropertiesTestDoc.Square(
                modifier = it.layoutModifier.then(it.appearanceModifier),
                parentLayout = it.parentLayout,
                type = SquareBorder.Sharp,
                color = SquareColor.Green
            )
        },
        square3 = {
            VariantPropertiesTestDoc.Square(
                modifier = it.layoutModifier.then(it.appearanceModifier),
                parentLayout = it.parentLayout,
                type = SquareBorder.Curved,
                color = SquareColor.Blue
            )
        },
        square4 = {
            VariantPropertiesTestDoc.Square(
                modifier = it.layoutModifier.then(it.appearanceModifier),
                parentLayout = it.parentLayout,
                type = SquareBorder.Curved,
                color = SquareColor.Green
            )
        },
        bg1 = bg1,
        bg2 = bg2,
        type = borderType,
        color = color,
    )

    Column(modifier = Modifier.absoluteOffset(x = 20.dp, y = 600.dp)) {
        Row {
            Text("Background 1 ", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button("Square", bg1 == Shape.Square) {
                setBg1(Shape.Square)
            }
            com.android.designcompose.testapp.validation.Button("Circle", bg1 == Shape.Circle) {
                setBg1(Shape.Circle)
            }
        }
        Row {
            Text("Background 2 ", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button("Square", bg2 == Shape.Square) {
                setBg2(Shape.Square)
            }
            com.android.designcompose.testapp.validation.Button("Circle", bg2 == Shape.Circle) {
                setBg2(Shape.Circle)
            }
        }
        Row {
            Text("Border ", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button(
                "Sharp",
                borderType == SquareBorder.Sharp
            ) {
                setBorderType(SquareBorder.Sharp)
            }
            com.android.designcompose.testapp.validation.Button(
                "Curved",
                borderType == SquareBorder.Curved
            ) {
                setBorderType(SquareBorder.Curved)
            }
        }
        Row {
            Text("Color ", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button(
                "Green",
                color == SquareColor.Green
            ) {
                setColor(SquareColor.Green)
            }
            com.android.designcompose.testapp.validation.Button("Blue", color == SquareColor.Blue) {
                setColor(SquareColor.Blue)
            }
        }
    }
}
