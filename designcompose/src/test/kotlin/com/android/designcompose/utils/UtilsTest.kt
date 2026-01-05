/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.designcompose.utils

import androidx.compose.ui.graphics.isIdentity
import com.android.designcompose.definition.element.FloatColor as DesignColor
import com.android.designcompose.definition.layout.OverflowDirection
import com.android.designcompose.definition.layout.ScrollInfo
import com.android.designcompose.definition.modifier.BlendMode
import com.android.designcompose.definition.modifier.LayoutTransform
import com.android.designcompose.definition.view.View
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UtilsTest {
    @Test
    fun testValidateFigmaDocId() {
        assertThat(validateFigmaDocId("12345")).isTrue()
        assertThat(validateFigmaDocId("abcde")).isTrue()
        assertThat(validateFigmaDocId("1a2b3c")).isTrue()
        assertThat(validateFigmaDocId("1a-2b")).isFalse()
        assertThat(validateFigmaDocId("1a_2b")).isFalse()
    }

    @Test
    fun testBlendModeAsComposeBlendMode() {
        assertThat(BlendMode.BLEND_MODE_PASS_THROUGH.asComposeBlendMode())
            .isEqualTo(androidx.compose.ui.graphics.BlendMode.SrcOver)
        assertThat(BlendMode.BLEND_MODE_NORMAL.asComposeBlendMode())
            .isEqualTo(androidx.compose.ui.graphics.BlendMode.SrcOver)
        assertThat(BlendMode.BLEND_MODE_DARKEN.asComposeBlendMode())
            .isEqualTo(androidx.compose.ui.graphics.BlendMode.Darken)
    }

    @Test
    fun testBlendModeUseLayer() {
        assertThat(BlendMode.BLEND_MODE_PASS_THROUGH.useLayer()).isFalse()
        assertThat(BlendMode.BLEND_MODE_NORMAL.useLayer()).isTrue()
    }

    @Test
    fun testViewHasScrolling() {
        val scrollInfo =
            ScrollInfo.newBuilder()
                .setOverflow(OverflowDirection.OVERFLOW_DIRECTION_HORIZONTAL_SCROLLING)
                .build()
        val view = View.newBuilder().setScrollInfo(scrollInfo).build()
        assertThat(view.hasScrolling()).isTrue()

        val noScrollInfo =
            ScrollInfo.newBuilder().setOverflow(OverflowDirection.OVERFLOW_DIRECTION_NONE).build()
        val noScrollView = View.newBuilder().setScrollInfo(noScrollInfo).build()
        assertThat(noScrollView.hasScrolling()).isFalse()
    }

    @Test
    fun testColorToColor() {
        val color = DesignColor.newBuilder().setR(0.1f).setG(0.2f).setB(0.3f).setA(0.4f).build()
        val composeColor = color.toColor()
        // Using the androidx.compose.ui.graphics.Color constructor directly.
        // This should now resolve correctly without the type mismatch.
        val expectedColor =
            androidx.compose.ui.graphics.Color(red = 0.1f, green = 0.2f, blue = 0.3f, alpha = 0.4f)
        assertThat(composeColor).isEqualTo(expectedColor)
    }

    @Test
    fun testAsComposeTransform() {
        val identityTransform = LayoutTransform.newBuilder().build()
        assertThat(identityTransform.asComposeTransform(1.0f)).isNull()

        val layoutTransform =
            LayoutTransform.newBuilder()
                .setM11(1f)
                .setM12(0f)
                .setM13(0f)
                .setM14(10f)
                .setM21(0f)
                .setM22(1f)
                .setM23(0f)
                .setM24(20f)
                .setM31(0f)
                .setM32(0f)
                .setM33(1f)
                .setM34(0f)
                .setM41(0f)
                .setM42(0f)
                .setM43(0f)
                .setM44(1f)
                .build()
        val composeMatrix = layoutTransform.asComposeTransform(1f)
        assertThat(composeMatrix).isNotNull()
        assertThat(composeMatrix!!.isIdentity()).isFalse()
    }
}
