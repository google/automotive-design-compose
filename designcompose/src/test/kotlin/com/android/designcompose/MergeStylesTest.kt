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

import com.android.designcompose.definition.element.FontStyle
import com.android.designcompose.definition.element.TextDecoration
import com.android.designcompose.definition.element.dimensionProto
import com.android.designcompose.definition.interaction.PointerEvents
import com.android.designcompose.definition.layout.AlignItems
import com.android.designcompose.definition.layout.FlexDirection
import com.android.designcompose.definition.layout.PositionType
import com.android.designcompose.definition.layout.layoutStyle
import com.android.designcompose.definition.modifier.BlendMode
import com.android.designcompose.definition.modifier.TextAlign
import com.android.designcompose.definition.modifier.TextAlignVertical
import com.android.designcompose.definition.modifier.TextOverflow
import com.android.designcompose.definition.view.Display
import com.android.designcompose.definition.view.nodeStyle
import com.android.designcompose.definition.view.viewStyle
import com.android.designcompose.utils.mergeStyles
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MergeStylesTest {

    // Helper to create a "default" ViewStyle (all fields at their default/zero values)
    private fun defaultViewStyle() = viewStyle {}

    @Test
    fun testMergeDefaultOverrideReturnsBase() {
        // Merging a default override into any base should return the base unchanged
        val base = viewStyle {
            layoutStyle = layoutStyle {
                flexDirection = FlexDirection.FLEX_DIRECTION_COLUMN
                flexGrow = 2.0f
            }
        }
        val override = defaultViewStyle()
        val merged = mergeStyles(base, override)

        assertThat(merged.layoutStyle.flexDirection).isEqualTo(FlexDirection.FLEX_DIRECTION_COLUMN)
        assertThat(merged.layoutStyle.flexGrow).isEqualTo(2.0f)
    }

    @Test
    fun testMergeOverrideFlexDirection() {
        val base = viewStyle {
            layoutStyle = layoutStyle { flexDirection = FlexDirection.FLEX_DIRECTION_ROW }
        }
        val override = viewStyle {
            layoutStyle = layoutStyle { flexDirection = FlexDirection.FLEX_DIRECTION_COLUMN }
        }
        val merged = mergeStyles(base, override)
        assertThat(merged.layoutStyle.flexDirection).isEqualTo(FlexDirection.FLEX_DIRECTION_COLUMN)
    }

    @Test
    fun testMergeOverrideAlignItems() {
        val base = defaultViewStyle()
        val override = viewStyle {
            layoutStyle = layoutStyle { alignItems = AlignItems.ALIGN_ITEMS_CENTER }
        }
        val merged = mergeStyles(base, override)
        assertThat(merged.layoutStyle.alignItems).isEqualTo(AlignItems.ALIGN_ITEMS_CENTER)
    }

    @Test
    fun testMergeOverridePositionType() {
        val base = defaultViewStyle()
        val override = viewStyle {
            layoutStyle = layoutStyle { positionType = PositionType.POSITION_TYPE_ABSOLUTE }
        }
        val merged = mergeStyles(base, override)
        assertThat(merged.layoutStyle.positionType).isEqualTo(PositionType.POSITION_TYPE_ABSOLUTE)
    }

    @Test
    fun testMergeOverrideFlexGrow() {
        val base = viewStyle { layoutStyle = layoutStyle { flexGrow = 1.0f } }
        val override = viewStyle { layoutStyle = layoutStyle { flexGrow = 3.0f } }
        val merged = mergeStyles(base, override)
        assertThat(merged.layoutStyle.flexGrow).isEqualTo(3.0f)
    }

    @Test
    fun testMergeOverrideFontStyle() {
        val base = defaultViewStyle()
        val override = viewStyle {
            nodeStyle = nodeStyle { fontStyle = FontStyle.FONT_STYLE_ITALIC }
        }
        val merged = mergeStyles(base, override)
        assertThat(merged.nodeStyle.fontStyle).isEqualTo(FontStyle.FONT_STYLE_ITALIC)
    }

    @Test
    fun testMergeOverrideTextDecoration() {
        val base = defaultViewStyle()
        val override = viewStyle {
            nodeStyle = nodeStyle { textDecoration = TextDecoration.TEXT_DECORATION_UNDERLINE }
        }
        val merged = mergeStyles(base, override)
        assertThat(merged.nodeStyle.textDecoration)
            .isEqualTo(TextDecoration.TEXT_DECORATION_UNDERLINE)
    }

    @Test
    fun testMergeOverrideTextAlign() {
        val base = defaultViewStyle()
        val override = viewStyle {
            nodeStyle = nodeStyle { textAlign = TextAlign.TEXT_ALIGN_CENTER }
        }
        val merged = mergeStyles(base, override)
        assertThat(merged.nodeStyle.textAlign).isEqualTo(TextAlign.TEXT_ALIGN_CENTER)
    }

    @Test
    fun testMergeOverrideTextAlignVertical() {
        val base = defaultViewStyle()
        val override = viewStyle {
            nodeStyle = nodeStyle {
                textAlignVertical = TextAlignVertical.TEXT_ALIGN_VERTICAL_CENTER
            }
        }
        val merged = mergeStyles(base, override)
        assertThat(merged.nodeStyle.textAlignVertical)
            .isEqualTo(TextAlignVertical.TEXT_ALIGN_VERTICAL_CENTER)
    }

    @Test
    fun testMergeOverrideTextOverflow() {
        val base = defaultViewStyle()
        val override = viewStyle {
            nodeStyle = nodeStyle { textOverflow = TextOverflow.TEXT_OVERFLOW_ELLIPSIS }
        }
        val merged = mergeStyles(base, override)
        assertThat(merged.nodeStyle.textOverflow).isEqualTo(TextOverflow.TEXT_OVERFLOW_ELLIPSIS)
    }

    @Test
    fun testMergeOverrideBlendMode() {
        val base = defaultViewStyle()
        val override = viewStyle {
            nodeStyle = nodeStyle { blendMode = BlendMode.BLEND_MODE_MULTIPLY }
        }
        val merged = mergeStyles(base, override)
        assertThat(merged.nodeStyle.blendMode).isEqualTo(BlendMode.BLEND_MODE_MULTIPLY)
    }

    @Test
    fun testMergeOverrideDisplayType() {
        val base = defaultViewStyle()
        val override = viewStyle { nodeStyle = nodeStyle { displayType = Display.DISPLAY_NONE } }
        val merged = mergeStyles(base, override)
        assertThat(merged.nodeStyle.displayType).isEqualTo(Display.DISPLAY_NONE)
    }

    @Test
    fun testMergeOverridePointerEvents() {
        val base = defaultViewStyle()
        val override = viewStyle {
            nodeStyle = nodeStyle { pointerEvents = PointerEvents.POINTER_EVENTS_NONE }
        }
        val merged = mergeStyles(base, override)
        assertThat(merged.nodeStyle.pointerEvents).isEqualTo(PointerEvents.POINTER_EVENTS_NONE)
    }

    @Test
    fun testMergeOverrideDimensionWidth() {
        val base = viewStyle {
            layoutStyle = layoutStyle { width = dimensionProto { points = 100.0f } }
        }
        val override = viewStyle {
            layoutStyle = layoutStyle { width = dimensionProto { points = 200.0f } }
        }
        val merged = mergeStyles(base, override)
        assertThat(merged.layoutStyle.width.points).isEqualTo(200.0f)
    }

    @Test
    fun testMergePreservesBaseWhenOverrideIsDefault() {
        // Verify that non-default base values are preserved when override has defaults
        val base = viewStyle {
            layoutStyle = layoutStyle {
                flexDirection = FlexDirection.FLEX_DIRECTION_COLUMN
                alignItems = AlignItems.ALIGN_ITEMS_CENTER
                flexGrow = 2.0f
            }
            nodeStyle = nodeStyle {
                fontStyle = FontStyle.FONT_STYLE_ITALIC
                textDecoration = TextDecoration.TEXT_DECORATION_UNDERLINE
                blendMode = BlendMode.BLEND_MODE_MULTIPLY
            }
        }
        val override = defaultViewStyle()
        val merged = mergeStyles(base, override)

        // Layout style should be preserved
        assertThat(merged.layoutStyle.flexDirection).isEqualTo(FlexDirection.FLEX_DIRECTION_COLUMN)
        assertThat(merged.layoutStyle.alignItems).isEqualTo(AlignItems.ALIGN_ITEMS_CENTER)
        assertThat(merged.layoutStyle.flexGrow).isEqualTo(2.0f)
        // Node style should be preserved
        assertThat(merged.nodeStyle.fontStyle).isEqualTo(FontStyle.FONT_STYLE_ITALIC)
        assertThat(merged.nodeStyle.textDecoration)
            .isEqualTo(TextDecoration.TEXT_DECORATION_UNDERLINE)
        assertThat(merged.nodeStyle.blendMode).isEqualTo(BlendMode.BLEND_MODE_MULTIPLY)
    }

    @Test
    fun testMergeMultiplePropertiesSimultaneously() {
        val base = viewStyle {
            layoutStyle = layoutStyle {
                flexDirection = FlexDirection.FLEX_DIRECTION_ROW
                flexGrow = 1.0f
            }
        }
        val override = viewStyle {
            layoutStyle = layoutStyle {
                flexDirection = FlexDirection.FLEX_DIRECTION_COLUMN
                alignItems = AlignItems.ALIGN_ITEMS_FLEX_END
            }
            nodeStyle = nodeStyle { blendMode = BlendMode.BLEND_MODE_SCREEN }
        }
        val merged = mergeStyles(base, override)

        // Overridden values
        assertThat(merged.layoutStyle.flexDirection).isEqualTo(FlexDirection.FLEX_DIRECTION_COLUMN)
        assertThat(merged.layoutStyle.alignItems).isEqualTo(AlignItems.ALIGN_ITEMS_FLEX_END)
        assertThat(merged.nodeStyle.blendMode).isEqualTo(BlendMode.BLEND_MODE_SCREEN)
        // Base value preserved
        assertThat(merged.layoutStyle.flexGrow).isEqualTo(1.0f)
    }
}
