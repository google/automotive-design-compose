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
package com.android.designcompose.squoosh

import com.android.designcompose.definition.view.View
import com.android.designcompose.definition.view.ViewStyle
import com.android.designcompose.layout_interface.Layout
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SquooshResolvedNodeTest {
    @Test
    fun testOffsetFromAncestor() {
        val view = mockk<View>()
        val style = mockk<ViewStyle>()
        val layout = mockk<Layout>()
        every { layout.left } returns 10f
        every { layout.top } returns 20f
        val parent = SquooshResolvedNode(view, style, 1, null, "parent")
        parent.computedLayout = layout
        val child = SquooshResolvedNode(view, style, 2, null, "child", parent = parent)
        child.computedLayout = layout
        val offset = child.offsetFromAncestor(parent)
        assertThat(offset.x).isEqualTo(10f)
        assertThat(offset.y).isEqualTo(20f)
    }
}
