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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CustomizationContextTest {

    @Test
    fun testSetAndGetText() {
        val context = CustomizationContext()
        context.setText("node1", "Hello")
        assertThat(context.getText("node1")).isEqualTo("Hello")
        context.setText("node1", "World")
        assertThat(context.getText("node1")).isEqualTo("World")
        context.setText("node2", "Hello")
        assertThat(context.getText("node2")).isEqualTo("Hello")
    }

    @Test
    fun testSetAndGetTextState() {
        val context = CustomizationContext()
        val state = mutableStateOf("Hello")
        context.setTextState("node1", state)
        assertThat(context.getTextState("node1")).isEqualTo(state)
    }

    @Test
    fun testSetAndGetBrush() {
        val context = CustomizationContext()
        val brush = SolidColor(Color.Red)
        context.setBrush("node1", brush)
        assertThat(context.getBrush("node1")).isEqualTo(brush)
    }

    @Test
    fun testSetAndGetBrushFunction() {
        val context = CustomizationContext()
        val brush1 = SolidColor(Color.Red)
        val brush2 = SolidColor(Color.Blue)
        var brushSelector = 1
        val brushFunction = { if (brushSelector == 1) brush1 else brush2 }
        context.setBrushFunction("node1", brushFunction)
        assertThat(context.getBrush("node1")).isEqualTo(brush1)
        brushSelector = 2
        assertThat(context.getBrush("node1")).isEqualTo(brush2)
    }

    @Test
    fun testSetAndGetModifier() {
        val context = CustomizationContext()
        val modifier = Modifier
        context.setModifier("node1", modifier)
        assertThat(context.getModifier("node1")).isEqualTo(modifier)
    }

    @Test
    fun testSetAndGetTapCallback() {
        val context = CustomizationContext()
        val callback = {}
        context.setTapCallback("node1", callback)
        assertThat(context.getTapCallback("node1")).isEqualTo(callback)
    }

    @Test
    fun testSetAndGetOnProgressChangedCallback() {
        val context = CustomizationContext()
        val callback =
            object : OnProgressChangedCallback {
                override fun onProgressChanged(progress: Float) {}
            }
        context.setOnProgressChangedCallback("node1", callback)
        assertThat(context.getOnProgressChangedCallback("node1")).isEqualTo(callback)
    }

    @Test
    fun testSetAndGetListContent() {
        val context = CustomizationContext()
        val listContent: ListContent = { ListContentData(count = 1) {} }
        context.setListContent("node1", listContent)
        assertThat(context.getListContent("node1")).isEqualTo(listContent)
    }

    @Test
    fun testSetAndGetOpenLinkCallback() {
        val context = CustomizationContext()
        val callback =
            object : OpenLinkCallback {
                override fun openLink(url: String) {}
            }
        context.setOpenLinkCallback("node1", callback)
        assertThat(context.getOpenLinkCallback("node1")).isEqualTo(callback)
    }

    @Test
    fun testSetAndGetVisible() {
        val context = CustomizationContext()
        context.setVisible("node1", false)
        assertThat(context.getVisible("node1")).isFalse()
    }

    @Test
    fun testSetAndGetTextStyle() {
        val context = CustomizationContext()
        val style = TextStyle(color = Color.Green)
        context.setTextStyle("node1", style)
        assertThat(context.getTextStyle("node1")).isEqualTo(style)
    }

    @Test
    fun testSetAndGetMeterValue() {
        val context = CustomizationContext()
        context.setMeterValue("node1", 50f)
        assertThat(context.getMeterValue("node1")).isEqualTo(50f)
    }

    @Test
    fun testGetMatchingVariant() {
        val context = CustomizationContext()
        context.setVariantProperties(hashMapOf("prop1" to "val2"))
        val componentInfo =
            com.android.designcompose.definition.view.ComponentInfo.newBuilder()
                .setName("prop1=val1")
                .build()
        val result = context.getMatchingVariant(componentInfo)
        assertThat(result).isEqualTo("prop1=val2")
    }

    @Test
    fun testGetMatchingVariantMultipleProperties() {
        val context = CustomizationContext()
        context.setVariantProperties(hashMapOf("prop1" to "val2", "prop2" to "val3"))
        val componentInfo =
            com.android.designcompose.definition.view.ComponentInfo.newBuilder()
                .setName("prop1=val1,prop2=val2")
                .build()
        val result = context.getMatchingVariant(componentInfo)
        assertThat(result).isEqualTo("prop1=val2,prop2=val3")
    }

    @Test
    fun testGetMatchingVariantPartialMatch() {
        val context = CustomizationContext()
        context.setVariantProperties(hashMapOf("prop1" to "val2"))
        val componentInfo =
            com.android.designcompose.definition.view.ComponentInfo.newBuilder()
                .setName("prop1=val1,prop2=val2")
                .build()
        val result = context.getMatchingVariant(componentInfo)
        assertThat(result).isEqualTo("prop1=val2,prop2=val2")
    }

    @Test
    fun testContentReplacement() {
        val context = CustomizationContext()
        val content = ReplacementContent(1) { {} }
        context.setContent("node1", content)
        assertThat(context.getContent("node1")).isEqualTo(content)
    }

    @Test
    fun testImageContext() {
        val imageContext =
            ImageContext(
                listOf(),
                com.android.designcompose.definition.element.DimensionProto.newBuilder()
                    .setPoints(10f)
                    .build(),
                com.android.designcompose.definition.element.DimensionProto.newBuilder()
                    .setPoints(20f)
                    .build(),
                com.android.designcompose.definition.element.DimensionProto.newBuilder()
                    .setPoints(15f)
                    .build(),
                com.android.designcompose.definition.element.DimensionProto.newBuilder()
                    .setPoints(30f)
                    .build(),
                com.android.designcompose.definition.element.DimensionProto.newBuilder()
                    .setPoints(40f)
                    .build(),
                com.android.designcompose.definition.element.DimensionProto.newBuilder()
                    .setPoints(35f)
                    .build(),
            )
        assertThat(imageContext.getPixelWidth()).isEqualTo(15)
        assertThat(imageContext.getPixelHeight()).isEqualTo(35)
    }
}
