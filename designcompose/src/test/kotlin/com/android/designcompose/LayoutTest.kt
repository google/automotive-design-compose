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

import androidx.compose.ui.Alignment
import com.android.designcompose.definition.layout.AlignItems
import com.android.designcompose.definition.layout.GridLayoutType
import com.android.designcompose.definition.layout.ItemSpacing
import com.android.designcompose.definition.layout.LayoutStyle
import com.android.designcompose.definition.view.NodeStyle
import com.android.designcompose.definition.view.View
import com.android.designcompose.definition.view.ViewStyle
import com.android.designcompose.layout_interface.Layout
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LayoutTest {
    @Test
    fun testItemSpacingAbs() {
        val fixedSpacing = ItemSpacing.newBuilder().setFixed(10).build()
        assertThat(itemSpacingAbs(fixedSpacing)).isEqualTo(10)

        val autoSpacing =
            ItemSpacing.newBuilder()
                .setAuto(ItemSpacing.Auto.newBuilder().setWidth(20).build())
                .build()
        assertThat(itemSpacingAbs(autoSpacing)).isEqualTo(20)

        val defaultSpacing = ItemSpacing.getDefaultInstance()
        assertThat(itemSpacingAbs(defaultSpacing)).isEqualTo(0)
    }

    @Test
    fun testCalcLayoutInfoAbsolute() {
        val view = View.getDefaultInstance()
        val style = ViewStyle.getDefaultInstance()
        val layoutInfo = calcLayoutInfo(view, style)
        assertThat(layoutInfo).isInstanceOf(LayoutInfoAbsolute::class.java)
    }

    @Test
    fun testCalcLayoutInfoRow() {
        val view = View.getDefaultInstance()
        val nodeStyle =
            NodeStyle.newBuilder().setGridLayoutType(GridLayoutType.GRID_LAYOUT_TYPE_HORIZONTAL)
        val layoutStyle =
            LayoutStyle.newBuilder().setAlignItems(AlignItems.ALIGN_ITEMS_CENTER).build()
        val style =
            ViewStyle.newBuilder().setNodeStyle(nodeStyle).setLayoutStyle(layoutStyle).build()
        val layoutInfo = calcLayoutInfo(view, style)
        assertThat(layoutInfo).isInstanceOf(LayoutInfoRow::class.java)
        assertThat((layoutInfo as LayoutInfoRow).alignment).isEqualTo(Alignment.CenterVertically)
    }

    @Test
    fun testCalcLayoutInfoColumn() {
        val view = View.getDefaultInstance()
        val nodeStyle =
            NodeStyle.newBuilder().setGridLayoutType(GridLayoutType.GRID_LAYOUT_TYPE_VERTICAL)
        val layoutStyle =
            LayoutStyle.newBuilder().setAlignItems(AlignItems.ALIGN_ITEMS_FLEX_END).build()
        val style =
            ViewStyle.newBuilder().setNodeStyle(nodeStyle).setLayoutStyle(layoutStyle).build()
        val layoutInfo = calcLayoutInfo(view, style)
        assertThat(layoutInfo).isInstanceOf(LayoutInfoColumn::class.java)
        assertThat((layoutInfo as LayoutInfoColumn).alignment).isEqualTo(Alignment.End)
    }

    @Test
    fun testLayoutExtensionFunctions() {
        val layout =
            Layout.newBuilder().setWidth(10.5f).setHeight(20.2f).setLeft(5.8f).setTop(15.1f).build()

        assertThat(layout.width()).isEqualTo(11)
        assertThat(layout.height()).isEqualTo(20)
        assertThat(layout.left()).isEqualTo(6)
        assertThat(layout.top()).isEqualTo(15)
    }
}
