/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law of an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.designcompose

import android.util.SizeF
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LayoutManagerTest {
    @Before
    fun setup() {
        mockkObject(Jni)
        every { Jni.createLayoutManager() } returns 1
    }

    @After
    fun tearDown() {
        // Clean up all mocks created during the test.
        unmockkAll()
    }

    @Test
    fun testLayoutManagerFunctions() {
        val layoutId = 1
        val paragraphIntrinsics = mockk<ParagraphIntrinsics>()
        val density = mockk<Density>()

        // Test TextMeasureData
        val textMeasureData =
            TextMeasureData(layoutId, paragraphIntrinsics, density, 1, false, HashMap())
        LayoutManager.squooshSetTextMeasureData(layoutId, textMeasureData)
        assertThat(LayoutManager.getTextMeasureData(layoutId)).isEqualTo(textMeasureData)
        LayoutManager.squooshClearTextMeasureData(layoutId)
        assertThat(LayoutManager.getTextMeasureData(layoutId)).isNull()

        // Test CustomMeasure
        val measureFunc: ((Float, Float, Float, Float) -> SizeF?) = { _, _, _, _ -> null }
        LayoutManager.squooshSetCustomMeasure(layoutId, measureFunc)
        assertThat(LayoutManager.getCustomMeasure(layoutId)).isEqualTo(measureFunc)
        LayoutManager.squooshClearCustomMeasure(layoutId)
        assertThat(LayoutManager.getCustomMeasure(layoutId)).isNull()

        // Test hasModifiedSize (default case)
        assertThat(LayoutManager.hasModifiedSize(layoutId)).isFalse()
    }
}
