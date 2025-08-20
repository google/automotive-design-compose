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
package com.android.designcompose

import android.content.Context
import android.util.Log
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DesignTextTest {
    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @Test
    fun testMeasureTextBoundsFunc() {
        val layoutId = 1
        val width = 100f
        val height = 50f
        val availableWidth = 200f
        val availableHeight = 100f
        val density = Density(1f)

        // 1. Get the application context from Robolectric.
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 2. Create a real FontFamily.Resolver, which is required by ParagraphIntrinsics.
        val fontFamilyResolver = createFontFamilyResolver(context)

        // 3. Create a real ParagraphIntrinsics instance instead of a mock.
        val paragraphIntrinsics =
            ParagraphIntrinsics(
                text = "test",
                style = TextStyle.Default,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
            )

        val textMeasureData = TextMeasureData(1, paragraphIntrinsics, density, 1, false, HashMap())

        mockkObject(LayoutManager)
        every { LayoutManager.getTextMeasureData(layoutId) } returns textMeasureData

        val (measuredWidth, measuredHeight) =
            measureTextBoundsFunc(layoutId, width, height, availableWidth, availableHeight)
        assertThat(measuredWidth).isGreaterThan(0f)
        assertThat(measuredHeight).isGreaterThan(0f)
    }
}
