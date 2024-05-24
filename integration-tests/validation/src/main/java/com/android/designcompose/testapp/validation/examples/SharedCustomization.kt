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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignModule
import com.android.designcompose.annotation.DesignModuleClass
import com.android.designcompose.annotation.DesignModuleProperty
import com.android.designcompose.annotation.DesignProperty
import com.android.designcompose.annotation.DesignVariantProperty

// No change

enum class TextStyle {
    big,
    small,
}

enum class FakeVariant {
    fakeOne,
    fakeTwo,
}

@DesignModuleClass
class TextModuleOne(
    @DesignProperty(node = "#textA") val textA: String,
    @DesignProperty(node = "#textB") val textB: String,
    // This node should be added an ignored image in the generated ignoredImages() function
    @DesignProperty(node = "#replace")
    val replaceNode: @Composable (ComponentReplacementContext) -> Unit,
    // This property should be added as a node query in the generated queries() function
    @DesignVariantProperty(property = "#fake-property") val fakeVariant: FakeVariant,
    // This property should not be used in the generated customizations() function
    val unused: String,
)

@DesignModuleClass
class TextModuleTwo(
    @DesignProperty(node = "#textC") val textC: String,
    @DesignProperty(node = "#textD") val textD: String,
    // This node should be added an ignored image in the generated ignoredImages() function
    @DesignProperty(node = "#imgOne") val imgOne: Bitmap?,
)

@DesignModuleClass
class TextModuleCombined(
    @DesignModuleProperty val one: TextModuleOne,
    @DesignModuleProperty val two: TextModuleTwo,
    @DesignProperty(node = "#textE") val textE: String,
    // This node should be added an ignored image in the generated ignoredImages() function
    @DesignProperty(node = "#imgTwo") val imgTwo: Bitmap?,
    // This property should be added as a node query in the generated queries() function
    @DesignVariantProperty(property = "#style") val style: TextStyle,
)

@DesignDoc(id = "hPEGkrF0LUqNYEZObXqjXZ")
interface ModuleExample {
    @DesignComponent(node = "#stage")
    fun Main(
        @Design(node = "#Header") headerText: String,
        @Design(node = "#ButtonBig") onTapButtonBig: TapCallback,
        @Design(node = "#ButtonSmall") onTapButtonSmall: TapCallback,
        @DesignModule moduleCustomizations: TextModuleCombined,
    )

    @DesignComponent(node = "#PurpleSquare") fun PurpleSquare()
}

@Composable
private fun getCustomizations(
    style: TextStyle,
    replaceNode: @Composable (ComponentReplacementContext) -> Unit
): TextModuleCombined {
    val blueBmp = remember {
        val bmp = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.BLUE)
        bmp
    }
    val greenBmp = remember {
        val bmp = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.GREEN)
        bmp
    }

    return TextModuleCombined(
        one =
            TextModuleOne(
                textA = "Hello",
                textB = "World,",
                replaceNode = replaceNode,
                fakeVariant = FakeVariant.fakeOne,
                unused = "unused",
            ),
        two =
            TextModuleTwo(
                textC = "and",
                textD = "Goodbye",
                imgOne = if (style == TextStyle.big) blueBmp else greenBmp,
            ),
        textE = "World!",
        imgTwo = if (style == TextStyle.big) blueBmp else greenBmp,
        style = style,
    )
}

@Composable
fun ModuleExample() {
    val textStyle = remember { mutableStateOf(TextStyle.big) }
    ModuleExampleDoc.Main(
        headerText = if (textStyle.value == TextStyle.big) "Big Variant" else "Small Variant",
        onTapButtonBig = { textStyle.value = TextStyle.big },
        onTapButtonSmall = { textStyle.value = TextStyle.small },
        moduleCustomizations =
            getCustomizations(textStyle.value) { ModuleExampleDoc.PurpleSquare() },
    )
}
