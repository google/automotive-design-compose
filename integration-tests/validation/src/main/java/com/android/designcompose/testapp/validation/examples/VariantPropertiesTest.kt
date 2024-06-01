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

enum class Shadow {
    On,
    Off
}

enum class Shape {
    Circle,
    Square,
}

enum class CompType {
    one,
    two,
    three,
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
        @DesignVariant(property = "BorderType") type: SquareBorder,
        @DesignVariant(property = "#SquareColor") color: SquareColor,
        @DesignVariant(property = "#comp1") comp1: CompType,
        @DesignVariant(property = "#comp2") comp2: CompType,
        @DesignVariant(property = "#comp3") comp3: CompType,
        @DesignVariant(property = "#border") border: Shape,
        @DesignVariant(property = "#shade") shade: CompType,
    )

    @DesignComponent(node = "#SquareBorder")
    fun Square(
        @DesignVariant(property = "BorderType") type: SquareBorder,
        @DesignVariant(property = "#SquareColor") color: SquareColor,
        @DesignVariant(property = "#SquareShadow") shadow: Shadow,
    )
}

@Composable
fun VariantPropertiesTest() {
    val (bg1, setBg1) = remember { mutableStateOf(Shape.Circle) }
    val (bg2, setBg2) = remember { mutableStateOf(Shape.Circle) }
    val (innerBorder, setInnerBorder) = remember { mutableStateOf(SquareBorder.Sharp) }

    val (color, setColor) = remember { mutableStateOf(SquareColor.Blue) }
    val (comp1, setComp1) = remember { mutableStateOf(CompType.two) }
    val (comp2, setComp2) = remember { mutableStateOf(CompType.two) }
    val (comp3, setComp3) = remember { mutableStateOf(CompType.two) }
    val (border, setBorder) = remember { mutableStateOf(Shape.Square) }
    val (shade, setShade) = remember { mutableStateOf(CompType.three) }

    VariantPropertiesTestDoc.MainFrame(
        square1 = {
            VariantPropertiesTestDoc.Square(
                type = SquareBorder.Sharp,
                color = SquareColor.Blue,
                shadow = Shadow.Off,
            )
        },
        square2 = {
            VariantPropertiesTestDoc.Square(
                type = SquareBorder.Sharp,
                color = SquareColor.Green,
                shadow = Shadow.On,
            )
        },
        square3 = {
            VariantPropertiesTestDoc.Square(
                type = SquareBorder.Curved,
                color = SquareColor.Blue,
                shadow = Shadow.Off,
            )
        },
        square4 = {
            VariantPropertiesTestDoc.Square(
                type = SquareBorder.Curved,
                color = SquareColor.Blue,
                shadow = Shadow.On,
            )
        },
        bg1 = bg1,
        bg2 = bg2,
        type = innerBorder,
        color = color,
        comp1 = comp1,
        comp2 = comp2,
        comp3 = comp3,
        border = border,
        shade = shade,
    )

    Column(modifier = Modifier.absoluteOffset(x = 20.dp, y = 600.dp)) {
        Row {
            Text("Background 1", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button("BG1 Square", bg1 == Shape.Square) {
                setBg1(Shape.Square)
            }
            com.android.designcompose.testapp.validation.Button("BG1 Circle", bg1 == Shape.Circle) {
                setBg1(Shape.Circle)
            }
        }
        Row {
            Text("Background 2", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button("BG2 Square", bg2 == Shape.Square) {
                setBg2(Shape.Square)
            }
            com.android.designcompose.testapp.validation.Button("BG2 Circle", bg2 == Shape.Circle) {
                setBg2(Shape.Circle)
            }
        }
        Row {
            Text("Inner Border", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button(
                "Inner Sharp",
                innerBorder == SquareBorder.Sharp
            ) {
                setInnerBorder(SquareBorder.Sharp)
            }
            com.android.designcompose.testapp.validation.Button(
                "Inner Curved",
                innerBorder == SquareBorder.Curved
            ) {
                setInnerBorder(SquareBorder.Curved)
            }
        }
        Row {
            Text("Color", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button(
                "Green",
                color == SquareColor.Green
            ) {
                setColor(SquareColor.Green)
            }
            com.android.designcompose.testapp.validation.Button("Blue", color == SquareColor.Blue) {
                setColor(SquareColor.Blue)
            }
            com.android.designcompose.testapp.validation.Button("Red", color == SquareColor.Red) {
                setColor(SquareColor.Red)
            }
        }
        Row {
            Text("Comp 1", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button(
                "Comp1 One",
                comp1 == CompType.one
            ) {
                setComp1(CompType.one)
            }
            com.android.designcompose.testapp.validation.Button(
                "Comp1 Two",
                comp1 == CompType.two
            ) {
                setComp1(CompType.two)
            }
        }
        Row {
            Text("Comp 2 ", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button(
                "Comp2 One",
                comp2 == CompType.one
            ) {
                setComp2(CompType.one)
            }
            com.android.designcompose.testapp.validation.Button(
                "Comp2 Two",
                comp2 == CompType.two
            ) {
                setComp2(CompType.two)
            }
        }
        Row {
            Text("Comp 3 ", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button(
                "Comp3 One",
                comp3 == CompType.one
            ) {
                setComp3(CompType.one)
            }
            com.android.designcompose.testapp.validation.Button(
                "Comp3 Two",
                comp3 == CompType.two
            ) {
                setComp3(CompType.two)
            }
        }
        Row {
            Text("Border ", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button("Curved", border == Shape.Circle) {
                setBorder(Shape.Circle)
            }
            com.android.designcompose.testapp.validation.Button("Square", border == Shape.Square) {
                setBorder(Shape.Square)
            }
        }
        Row {
            Text("Shade ", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.Button(
                "Shade One",
                shade == CompType.one
            ) {
                setShade(CompType.one)
            }
            com.android.designcompose.testapp.validation.Button(
                "Shade Two",
                shade == CompType.two
            ) {
                setShade(CompType.two)
            }
            com.android.designcompose.testapp.validation.Button(
                "Shade Three",
                shade == CompType.three
            ) {
                setShade(CompType.three)
            }
        }
    }
}
