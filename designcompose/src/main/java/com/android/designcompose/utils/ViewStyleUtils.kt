/*
 * Copyright 2024 Google LLC
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

import com.android.designcompose.definition.element.DimensionRect
import com.android.designcompose.definition.element.FontStyle
import com.android.designcompose.definition.element.TextDecoration
import com.android.designcompose.definition.element.shaderDataOrNull
import com.android.designcompose.definition.interaction.PointerEvents
import com.android.designcompose.definition.layout.AlignContent
import com.android.designcompose.definition.layout.AlignItems
import com.android.designcompose.definition.layout.AlignSelf
import com.android.designcompose.definition.layout.FlexDirection
import com.android.designcompose.definition.layout.FlexWrap
import com.android.designcompose.definition.layout.ItemSpacing
import com.android.designcompose.definition.layout.JustifyContent
import com.android.designcompose.definition.layout.LayoutSizing
import com.android.designcompose.definition.layout.Overflow
import com.android.designcompose.definition.layout.PositionType
import com.android.designcompose.definition.layout.copy
import com.android.designcompose.definition.modifier.BlendMode
import com.android.designcompose.definition.modifier.TextAlign
import com.android.designcompose.definition.modifier.TextAlignVertical
import com.android.designcompose.definition.modifier.TextOverflow
import com.android.designcompose.definition.view.Display
import com.android.designcompose.definition.view.ViewStyle
import com.android.designcompose.definition.view.copy
import com.android.designcompose.definition.view.fontStretchOrNull
import com.android.designcompose.definition.view.nodeSizeOrNull
import com.android.designcompose.definition.view.strokeOrNull
import com.android.designcompose.definition.view.viewStyle

// Merge styles; any non-default properties of the override style are copied over the base style.
// TODO: look into if we can use proto's own merge functionalities.
internal fun mergeStyles(base: ViewStyle, override: ViewStyle): ViewStyle {
    var mergedNodeStyle =
        base.nodeStyle.copy {
            if (!override.nodeStyle.textColor.hasNone()) textColor = override.nodeStyle.textColor

            if (!override.nodeStyle.fontSize.hasNum() || override.nodeStyle.fontSize.num != 18.0f)
                fontSize = override.nodeStyle.fontSize

            if (
                !override.nodeStyle.fontWeight.weight.hasNum() ||
                    override.nodeStyle.fontWeight.weight.num != 400.0f
            )
                fontWeight = override.nodeStyle.fontWeight
            if (
                override.nodeStyle.fontStyle != FontStyle.FONT_STYLE_UNSPECIFIED &&
                    override.nodeStyle.fontStyle != FontStyle.FONT_STYLE_NORMAL
            )
                fontStyle = override.nodeStyle.fontStyle

            if (
                override.nodeStyle.textDecoration != TextDecoration.TEXT_DECORATION_UNSPECIFIED &&
                    override.nodeStyle.textDecoration != TextDecoration.TEXT_DECORATION_NONE
            )
                textDecoration = override.nodeStyle.textDecoration

            if (override.nodeStyle.hasLetterSpacing())
                letterSpacing = override.nodeStyle.letterSpacing

            if (override.nodeStyle.hasFontFamily()) fontFamily = override.nodeStyle.fontFamily

            if (override.nodeStyle.fontStretchOrNull?.value != 1.0f)
                fontStretch = override.nodeStyle.fontStretch

            if (
                override.nodeStyle.backgroundsCount > 0 &&
                    !override.nodeStyle.getBackgrounds(0).hasNone()
            ) {
                backgrounds.clear()
                backgrounds.addAll(override.nodeStyle.backgroundsList)
            }

            if (override.nodeStyle.boxShadowsCount > 0) {
                boxShadows.clear()
                boxShadows.addAll(override.nodeStyle.boxShadowsList)
            }

            if (
                override.nodeStyle.strokeOrNull?.shaderDataOrNull != null ||
                    (override.nodeStyle.strokeOrNull?.strokesCount ?: 0) > 0
            )
                stroke = override.nodeStyle.stroke

            if (override.nodeStyle.hasOpacity()) opacity = override.nodeStyle.opacity

            if (override.nodeStyle.hasTransform()) transform = override.nodeStyle.transform

            if (override.nodeStyle.hasRelativeTransform())
                relativeTransform = override.nodeStyle.relativeTransform

            if (
                override.nodeStyle.textAlign != TextAlign.TEXT_ALIGN_UNSPECIFIED &&
                    override.nodeStyle.textAlign != TextAlign.TEXT_ALIGN_LEFT
            )
                textAlign = override.nodeStyle.textAlign

            if (
                override.nodeStyle.textAlignVertical !=
                    TextAlignVertical.TEXT_ALIGN_VERTICAL_UNSPECIFIED &&
                    override.nodeStyle.textAlignVertical !=
                        TextAlignVertical.TEXT_ALIGN_VERTICAL_TOP
            )
                textAlignVertical = override.nodeStyle.textAlignVertical

            if (
                override.nodeStyle.textOverflow != TextOverflow.TEXT_OVERFLOW_UNSPECIFIED &&
                    override.nodeStyle.textOverflow != TextOverflow.TEXT_OVERFLOW_CLIP
            )
                textOverflow = override.nodeStyle.textOverflow

            if (override.nodeStyle.hasTextShadow()) textShadow = override.nodeStyle.textShadow

            if (override.nodeStyle.nodeSizeOrNull.let { it?.width != 0.0f || it.height != 0.0f })
                nodeSize = override.nodeStyle.nodeSize

            if (
                !override.nodeStyle.lineHeight.hasPercent() ||
                    override.nodeStyle.lineHeight.percent != 1.0f
            )
                lineHeight = override.nodeStyle.lineHeight

            if (override.nodeStyle.hasLineCount()) lineCount = override.nodeStyle.lineCount

            if (override.nodeStyle.fontFeaturesCount > 0) {
                fontFeatures.clear()
                fontFeatures.addAll(override.nodeStyle.fontFeaturesList)
            }

            if (override.nodeStyle.filtersCount > 0) {
                filters.clear()
                filters.addAll(override.nodeStyle.filtersList)
            }

            if (override.nodeStyle.backdropFiltersCount > 0) {
                backdropFilters.clear()
                backdropFilters.addAll(override.nodeStyle.backdropFiltersList)
            }

            if (
                override.nodeStyle.blendMode != BlendMode.BLEND_MODE_UNSPECIFIED &&
                    override.nodeStyle.blendMode != BlendMode.BLEND_MODE_PASS_THROUGH
            )
                blendMode = override.nodeStyle.blendMode

            if (override.nodeStyle.hasHyperlink()) hyperlink = override.nodeStyle.hyperlink

            if (
                override.nodeStyle.displayType != Display.DISPLAY_UNSPECIFIED &&
                    override.nodeStyle.displayType != Display.DISPLAY_FLEX
            )
                displayType = override.nodeStyle.displayType

            if (
                override.nodeStyle.flexWrap != FlexWrap.FLEX_WRAP_UNSPECIFIED &&
                    override.nodeStyle.flexWrap != FlexWrap.FLEX_WRAP_NO_WRAP
            )
                flexWrap = override.nodeStyle.flexWrap

            if (override.nodeStyle.hasGridLayoutType())
                gridLayoutType = override.nodeStyle.gridLayoutType

            if (override.nodeStyle.gridColumnsRows > 0)
                gridColumnsRows = override.nodeStyle.gridColumnsRows

            if (override.nodeStyle.gridAdaptiveMinSize > 1)
                gridAdaptiveMinSize = override.nodeStyle.gridAdaptiveMinSize

            if (override.nodeStyle.gridSpanContentsCount > 0) {
                gridSpanContents.clear()
                gridSpanContents.addAll(override.nodeStyle.gridSpanContentsList)
            }

            if (
                override.nodeStyle.overflow != Overflow.OVERFLOW_UNSPECIFIED &&
                    override.nodeStyle.overflow != Overflow.OVERFLOW_VISIBLE
            )
                overflow = override.nodeStyle.overflow

            if (override.nodeStyle.hasMaxChildren()) maxChildren = override.nodeStyle.maxChildren

            if (override.nodeStyle.hasOverflowNodeId())
                overflowNodeId = override.nodeStyle.overflowNodeId

            if (override.nodeStyle.hasOverflowNodeName())
                overflowNodeName = override.nodeStyle.overflowNodeName

            if (override.nodeStyle.crossAxisItemSpacing != 0.0f)
                crossAxisItemSpacing = override.nodeStyle.crossAxisItemSpacing

            if (
                override.nodeStyle.horizontalSizing != LayoutSizing.LAYOUT_SIZING_UNSPECIFIED &&
                    override.nodeStyle.horizontalSizing != LayoutSizing.LAYOUT_SIZING_FIXED
            )
                horizontalSizing = override.nodeStyle.horizontalSizing

            if (
                override.nodeStyle.verticalSizing != LayoutSizing.LAYOUT_SIZING_UNSPECIFIED &&
                    override.nodeStyle.verticalSizing != LayoutSizing.LAYOUT_SIZING_FIXED
            )
                verticalSizing = override.nodeStyle.verticalSizing

            if (override.nodeStyle.hasAspectRatio()) aspectRatio = override.nodeStyle.aspectRatio

            if (
                override.nodeStyle.pointerEvents != PointerEvents.POINTER_EVENTS_AUTO &&
                    override.nodeStyle.pointerEvents != PointerEvents.POINTER_EVENTS_UNSPECIFIED
            )
                pointerEvents = override.nodeStyle.pointerEvents

            if (override.nodeStyle.hasMeterData()) meterData = override.nodeStyle.meterData
            if (override.nodeStyle.hasShaderData()) shaderData = override.nodeStyle.shaderData
        }

    val mergedLayoutStyle =
        base.layoutStyle.copy {
            if (
                override.layoutStyle.positionType != PositionType.POSITION_TYPE_UNSPECIFIED &&
                    override.layoutStyle.positionType != PositionType.POSITION_TYPE_RELATIVE
            )
                positionType = override.layoutStyle.positionType

            if (
                override.layoutStyle.flexDirection != FlexDirection.FLEX_DIRECTION_UNSPECIFIED &&
                    override.layoutStyle.flexDirection != FlexDirection.FLEX_DIRECTION_ROW
            )
                flexDirection = override.layoutStyle.flexDirection

            if (
                override.layoutStyle.alignItems != AlignItems.ALIGN_ITEMS_UNSPECIFIED &&
                    override.layoutStyle.alignItems != AlignItems.ALIGN_ITEMS_STRETCH
            )
                alignItems = override.layoutStyle.alignItems

            if (
                override.layoutStyle.alignSelf != AlignSelf.ALIGN_SELF_UNSPECIFIED &&
                    override.layoutStyle.alignSelf != AlignSelf.ALIGN_SELF_AUTO
            )
                alignSelf = override.layoutStyle.alignSelf

            if (
                override.layoutStyle.alignContent != AlignContent.ALIGN_CONTENT_UNSPECIFIED &&
                    override.layoutStyle.alignContent != AlignContent.ALIGN_CONTENT_STRETCH
            )
                alignContent = override.layoutStyle.alignContent

            if (
                override.layoutStyle.justifyContent != JustifyContent.JUSTIFY_CONTENT_UNSPECIFIED &&
                    override.layoutStyle.justifyContent != JustifyContent.JUSTIFY_CONTENT_FLEX_START
            )
                justifyContent = override.layoutStyle.justifyContent

            if (!override.layoutStyle.top.hasUndefined()) top = override.layoutStyle.top

            if (!override.layoutStyle.left.hasUndefined()) left = override.layoutStyle.left

            if (!override.layoutStyle.bottom.hasUndefined()) bottom = override.layoutStyle.bottom

            if (!override.layoutStyle.right.hasUndefined()) right = override.layoutStyle.right

            fun DimensionRect.isDefault(): Boolean {
                return start.hasUndefined() &&
                    end.hasUndefined() &&
                    top.hasUndefined() &&
                    bottom.hasUndefined()
            }

            if (!override.layoutStyle.margin.isDefault()) margin = override.layoutStyle.margin

            if (!override.layoutStyle.padding.isDefault()) padding = override.layoutStyle.padding

            fun ItemSpacing.isDefault(): Boolean {
                return itemSpacingTypeCase ==
                    ItemSpacing.ItemSpacingTypeCase.ITEMSPACINGTYPE_NOT_SET ||
                    itemSpacingTypeCase == ItemSpacing.ItemSpacingTypeCase.FIXED
            }

            if (!override.layoutStyle.itemSpacing.isDefault())
                itemSpacing = override.layoutStyle.itemSpacing

            if (override.layoutStyle.flexGrow != 0.0f) flexGrow = override.layoutStyle.flexGrow

            if (override.layoutStyle.flexShrink != 0.0f)
                flexShrink = override.layoutStyle.flexShrink

            if (!override.layoutStyle.flexBasis.hasUndefined())
                flexBasis = override.layoutStyle.flexBasis

            if (
                override.layoutStyle.boundingBox.width != 0.0f ||
                    override.layoutStyle.boundingBox.height != 0.0f
            )
                boundingBox = override.layoutStyle.boundingBox

            if (!override.layoutStyle.width.hasUndefined()) width = override.layoutStyle.width

            if (!override.layoutStyle.height.hasUndefined()) height = override.layoutStyle.height

            if (!override.layoutStyle.minWidth.hasUndefined())
                minWidth = override.layoutStyle.minWidth

            if (!override.layoutStyle.minHeight.hasUndefined())
                minHeight = override.layoutStyle.minHeight

            if (!override.layoutStyle.maxWidth.hasUndefined())
                maxWidth = override.layoutStyle.maxWidth

            if (!override.layoutStyle.maxHeight.hasUndefined())
                maxHeight = override.layoutStyle.maxHeight
        }

    return viewStyle {
        layoutStyle = mergedLayoutStyle
        nodeStyle = mergedNodeStyle
    }
}

// Get the raw width in a view style from the width property if it is a fixed size, or from the
// node_size property if not.
internal fun ViewStyle.fixedWidth(density: Float): Float {
    return if (layoutStyle.width.hasPoints()) layoutStyle.width.pointsAsDp(density).value
    else nodeStyle.nodeSize.width * density
}

// Get the raw height in a view style from the height property if it is a fixed size, or from the
// node_size property if not.
internal fun ViewStyle.fixedHeight(density: Float): Float {
    return if (layoutStyle.height.hasPoints()) layoutStyle.height.pointsAsDp(density).value
    else nodeStyle.nodeSize.height * density
}

// Return whether a text node is auto width without a FILL sizing mode. This is a check used by the
// text measure func that, when it returns true, means the text can expand past the available width
// passed into it.
internal fun ViewStyle.isAutoWidthText() =
    layoutStyle.width.hasAuto() &&
        nodeStyle.horizontalSizing != LayoutSizing.LAYOUT_SIZING_FILL &&
        nodeStyle.horizontalSizing != LayoutSizing.LAYOUT_SIZING_UNSPECIFIED
