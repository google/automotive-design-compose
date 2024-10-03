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
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.android.designcompose.ImageReplacementContext
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST Color tint test
@DesignDoc(id = "MCtUD3yjONxK6rQm65yqM5")
interface ColorTintTest {
    @DesignComponent(node = "#MainFrame")
    fun MainFrame(
        @Design(node = "#Square")
        squareReplacement: @Composable (ImageReplacementContext) -> Bitmap?
    )
}

@Composable
fun ColorTintTest() {
    ColorTintTestDoc.MainFrame(
        squareReplacement = { context ->
            val color =
                ((context.imageContext.getBackgroundColor() ?: Color.Red.toArgb()) * 0.5).toInt()
            val width = context.imageContext.getPixelWidth() ?: 50
            val height = context.imageContext.getPixelHeight() ?: 50
            val colors = IntArray(width * height)
            for (i in 0 until width * height) {
                colors[i] = color
            }
            Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888)
        }
    )
}
