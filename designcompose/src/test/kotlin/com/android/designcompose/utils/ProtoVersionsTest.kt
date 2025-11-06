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

package com.android.designcompose.utils

import com.android.designcompose.definition.element.Background
import com.android.designcompose.definition.element.Color
import com.android.designcompose.definition.element.ColorOrVar
import com.android.designcompose.definition.view.NodeStyle
import com.android.designcompose.definition.view.ViewStyle
import com.android.designcompose.definition.view.fontColorOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProtoVersionsTest {

    @Test
    fun testMergeStyles() {
        val color = Color.newBuilder().setR(255).setG(0).setB(0).setA(255).build()
        val colorOrVar = ColorOrVar.newBuilder().setColor(color).build()
        val background = Background.newBuilder().setSolid(colorOrVar).build()
        val nodeStyle = NodeStyle.newBuilder().setTextColor(background).build()
        val viewStyle = ViewStyle.newBuilder().setNodeStyle(nodeStyle).build()
        val mergedNodeStyle = NodeStyle.newBuilder().build()

        val result = protoVersionsMergeStyles(viewStyle, mergedNodeStyle)
        assertEquals(background, result.fontColor)
    }

    @Test
    fun testMergeStylesNoTextColor() {
        val nodeStyle = NodeStyle.newBuilder().build()
        val viewStyle = ViewStyle.newBuilder().setNodeStyle(nodeStyle).build()
        val mergedNodeStyle = NodeStyle.newBuilder().build()

        val result = protoVersionsMergeStyles(viewStyle, mergedNodeStyle)
        assertNull(result.fontColorOrNull)
    }

    @Test
    fun testFontColor() {
        val color = Color.newBuilder().setR(255).setG(0).setB(0).setA(255).build()
        val colorOrVar = ColorOrVar.newBuilder().setColor(color).build()
        val background = Background.newBuilder().setSolid(colorOrVar).build()
        val nodeStyle = NodeStyle.newBuilder().setFontColor(background).build()
        val viewStyle = ViewStyle.newBuilder().setNodeStyle(nodeStyle).build()

        val result = protoVersionsFontColor(viewStyle)
        assertEquals(background, result)
    }

    @Test
    fun testFontColorTextColor() {
        val color = Color.newBuilder().setR(255).setG(0).setB(0).setA(255).build()
        val colorOrVar = ColorOrVar.newBuilder().setColor(color).build()
        val background = Background.newBuilder().setSolid(colorOrVar).build()
        val nodeStyle = NodeStyle.newBuilder().setTextColor(background).build()
        val viewStyle = ViewStyle.newBuilder().setNodeStyle(nodeStyle).build()

        val result = protoVersionsFontColor(viewStyle)
        assertEquals(background, result)
    }

    @Test
    fun testFontColorNoColor() {
        val nodeStyle = NodeStyle.newBuilder().build()
        val viewStyle = ViewStyle.newBuilder().setNodeStyle(nodeStyle).build()

        val result = protoVersionsFontColor(viewStyle)
        assertNull(result)
    }
}
