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

import androidx.compose.ui.geometry.Size
import com.android.designcompose.LayoutManager
import com.android.designcompose.definition.view.ViewStyle
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config // Import the Config annotation

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Specify the SDK version here
class LayoutUtilsTest {
    @Before
    fun setup() {
        mockkObject(LayoutManager)
    }

    @Test
    fun testGetNodeRenderSize() {
        val overrideSize = Size(1f, 2f)
        val layoutSize = Size(3f, 4f)
        val style = mockk<ViewStyle>()

        // Test override size
        var size = getNodeRenderSize(overrideSize, layoutSize, style, 1, 1f)
        assertThat(size).isEqualTo(overrideSize)

        // Test modified size
        every { LayoutManager.hasModifiedSize(1) } returns true
        size = getNodeRenderSize(null, layoutSize, style, 1, 1f)
        assertThat(size).isEqualTo(layoutSize)
    }
}
