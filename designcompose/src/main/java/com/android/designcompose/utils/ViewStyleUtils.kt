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
import com.android.designcompose.definition.interaction.PointerEvents
import com.android.designcompose.definition.layout.AlignContent
import com.android.designcompose.definition.layout.AlignItems
import com.android.designcompose.definition.layout.AlignSelf
import com.android.designcompose.definition.layout.FlexDirection
import com.android.designcompose.definition.layout.FlexWrap
import com.android.designcompose.definition.layout.ItemSpacing
import com.android.designcompose.definition.layout.JustifyContent
import com.android.designcompose.definition.layout.LayoutSizing
import com.android.designcompose.definition.layout.LayoutStyle
import com.android.designcompose.definition.layout.Overflow
import com.android.designcompose.definition.layout.PositionType
import com.android.designcompose.definition.modifier.BlendMode
import com.android.designcompose.definition.modifier.TextAlign
import com.android.designcompose.definition.modifier.TextAlignVertical
import com.android.designcompose.definition.modifier.TextOverflow
import com.android.designcompose.definition.view.Display
import com.android.designcompose.definition.view.NodeStyle
import com.android.designcompose.definition.view.ViewStyle
import com.android.designcompose.definition.view.fontStretchOrNull
import com.android.designcompose.definition.view.nodeSizeOrNull
import com.android.designcompose.definition.view.strokeOrNull

// Merge styles; any non-default properties of the override style are copied over the base style.
internal fun mergeStyles(base: ViewStyle, override: ViewStyle): ViewStyle {
    val style = ViewStyle.newBuilder()
    val nodeStyle = NodeStyle.newBuilder()
    val layoutStyle = LayoutStyle.newBuilder()
    nodeStyle.textColor =
        if (!override.nodeStyle.textColor.hasNone()) {
            override.nodeStyle.textColor
        } else {
            base.nodeStyle.textColor
        }
    nodeStyle.fontSize =
        if (!override.nodeStyle.fontSize.hasNum() || override.nodeStyle.fontSize.num != 18.0f) {
            override.nodeStyle.fontSize
        } else {
            base.nodeStyle.fontSize
        }
    nodeStyle.fontWeight =
        if (
            !override.nodeStyle.fontWeight.weight.hasNum() ||
                override.nodeStyle.fontWeight.weight.num != 400.0f
        ) {
            override.nodeStyle.fontWeight
        } else {
            base.nodeStyle.fontWeight
        }
    nodeStyle.fontStyle =
        if (
            override.nodeStyle.fontStyle != FontStyle.FONT_STYLE_UNSPECIFIED &&
                override.nodeStyle.fontStyle != FontStyle.FONT_STYLE_NORMAL
        ) {
            override.nodeStyle.fontStyle
        } else {
            base.nodeStyle.fontStyle
        }
    nodeStyle.textDecoration =
        if (
            override.nodeStyle.textDecoration != TextDecoration.TEXT_DECORATION_UNSPECIFIED &&
                override.nodeStyle.textDecoration != TextDecoration.TEXT_DECORATION_NONE
        ) {
            override.nodeStyle.textDecoration
        } else {
            base.nodeStyle.textDecoration
        }
    nodeStyle.letterSpacing =
        if (override.nodeStyle.hasLetterSpacing()) {
            override.nodeStyle.letterSpacing
        } else {
            base.nodeStyle.letterSpacing
        }
    nodeStyle.fontFamily =
        if (override.nodeStyle.hasFontFamily()) {
            override.nodeStyle.fontFamily
        } else {
            base.nodeStyle.fontFamily
        }
    nodeStyle.fontStretch =
        if (override.nodeStyle.fontStretchOrNull?.value != 1.0f) {
            override.nodeStyle.fontStretch
        } else {
            base.nodeStyle.fontStretch
        }
    if (
        override.nodeStyle.backgroundsCount > 0 && !override.nodeStyle.getBackgrounds(0).hasNone()
    ) {
        nodeStyle.backgroundsList.addAll(override.nodeStyle.backgroundsList)
    } else {
        nodeStyle.backgroundsList.addAll(base.nodeStyle.backgroundsList)
    }

    if (override.nodeStyle.boxShadowsCount > 0) {
        nodeStyle.boxShadowsList.addAll(override.nodeStyle.boxShadowsList)
    } else {
        nodeStyle.boxShadowsList.addAll(base.nodeStyle.boxShadowsList)
    }
    nodeStyle.stroke =
        if ((override.nodeStyle.strokeOrNull?.strokesCount ?: 0) > 0) {
            override.nodeStyle.stroke
        } else {
            base.nodeStyle.stroke
        }
    nodeStyle.opacity =
        if (override.nodeStyle.hasOpacity()) {
            override.nodeStyle.opacity
        } else {
            base.nodeStyle.opacity
        }
    nodeStyle.transform =
        if (override.nodeStyle.hasTransform()) {
            override.nodeStyle.transform
        } else {
            base.nodeStyle.transform
        }
    nodeStyle.relativeTransform =
        if (override.nodeStyle.hasRelativeTransform()) {
            override.nodeStyle.relativeTransform
        } else {
            base.nodeStyle.relativeTransform
        }
    nodeStyle.textAlign =
        if (
            override.nodeStyle.textAlign != TextAlign.TEXT_ALIGN_UNSPECIFIED &&
                override.nodeStyle.textAlign != TextAlign.TEXT_ALIGN_LEFT
        ) {
            override.nodeStyle.textAlign
        } else {
            base.nodeStyle.textAlign
        }
    nodeStyle.textAlignVertical =
        if (
            override.nodeStyle.textAlignVertical !=
                TextAlignVertical.TEXT_ALIGN_VERTICAL_UNSPECIFIED &&
                override.nodeStyle.textAlignVertical != TextAlignVertical.TEXT_ALIGN_VERTICAL_TOP
        ) {
            override.nodeStyle.textAlignVertical
        } else {
            base.nodeStyle.textAlignVertical
        }
    nodeStyle.textOverflow =
        if (
            override.nodeStyle.textOverflow != TextOverflow.TEXT_OVERFLOW_UNSPECIFIED &&
                override.nodeStyle.textOverflow != TextOverflow.TEXT_OVERFLOW_CLIP
        ) {
            override.nodeStyle.textOverflow
        } else {
            base.nodeStyle.textOverflow
        }
    nodeStyle.textShadow =
        if (override.nodeStyle.hasTextShadow()) {
            override.nodeStyle.textShadow
        } else {
            base.nodeStyle.textShadow
        }
    nodeStyle.nodeSize =
        if (override.nodeStyle.nodeSizeOrNull.let { it?.width != 0.0f || it.height != 0.0f }) {
            override.nodeStyle.nodeSize
        } else {
            base.nodeStyle.nodeSize
        }
    nodeStyle.lineHeight =
        if (
            !override.nodeStyle.lineHeight.hasPercent() ||
                override.nodeStyle.lineHeight.percent != 1.0f
        ) {
            override.nodeStyle.lineHeight
        } else {
            base.nodeStyle.lineHeight
        }
    nodeStyle.lineCount =
        if (override.nodeStyle.hasLineCount()) {
            override.nodeStyle.lineCount
        } else {
            base.nodeStyle.lineCount
        }
    nodeStyle.fontFeaturesList.addAll(
        if (override.nodeStyle.fontFeaturesCount > 0) override.nodeStyle.fontFeaturesList
        else base.nodeStyle.fontFeaturesList
    )
    nodeStyle.filtersList.addAll(
        if (override.nodeStyle.filtersList.isNotEmpty()) override.nodeStyle.filtersList
        else base.nodeStyle.filtersList
    )
    nodeStyle.backdropFiltersList.addAll(
        if (override.nodeStyle.backdropFiltersList.isNotEmpty())
            override.nodeStyle.backdropFiltersList
        else base.nodeStyle.backdropFiltersList
    )
    nodeStyle.blendMode =
        if (
            override.nodeStyle.blendMode != BlendMode.BLEND_MODE_UNSPECIFIED &&
                override.nodeStyle.blendMode != BlendMode.BLEND_MODE_PASS_THROUGH
        ) {
            override.nodeStyle.blendMode
        } else {
            base.nodeStyle.blendMode
        }
    nodeStyle.hyperlinks =
        if (override.nodeStyle.hasHyperlinks()) {
            override.nodeStyle.hyperlinks
        } else {
            base.nodeStyle.hyperlinks
        }
    nodeStyle.displayType =
        if (
            override.nodeStyle.displayType != Display.DISPLAY_UNSPECIFIED &&
                override.nodeStyle.displayType != Display.DISPLAY_FLEX
        ) {
            override.nodeStyle.displayType
        } else {
            base.nodeStyle.displayType
        }
    layoutStyle.positionType =
        if (
            override.layoutStyle.positionType != PositionType.POSITION_TYPE_UNSPECIFIED &&
                override.layoutStyle.positionType != PositionType.POSITION_TYPE_RELATIVE
        ) {
            override.layoutStyle.positionType
        } else {
            base.layoutStyle.positionType
        }
    layoutStyle.flexDirection =
        if (
            override.layoutStyle.flexDirection != FlexDirection.FLEX_DIRECTION_UNSPECIFIED &&
                override.layoutStyle.flexDirection != FlexDirection.FLEX_DIRECTION_ROW
        ) {
            override.layoutStyle.flexDirection
        } else {
            base.layoutStyle.flexDirection
        }
    nodeStyle.flexWrap =
        if (
            override.nodeStyle.flexWrap != FlexWrap.FLEX_WRAP_UNSPECIFIED &&
                override.nodeStyle.flexWrap != FlexWrap.FLEX_WRAP_NO_WRAP
        ) {
            override.nodeStyle.flexWrap
        } else {
            base.nodeStyle.flexWrap
        }
    nodeStyle.gridLayoutType =
        if (override.nodeStyle.hasGridLayoutType()) {
            override.nodeStyle.gridLayoutType
        } else {
            base.nodeStyle.gridLayoutType
        }
    nodeStyle.gridColumnsRows =
        if (override.nodeStyle.gridColumnsRows > 0) {
            override.nodeStyle.gridColumnsRows
        } else {
            base.nodeStyle.gridColumnsRows
        }
    nodeStyle.gridAdaptiveMinSize =
        if (override.nodeStyle.gridAdaptiveMinSize > 1) {
            override.nodeStyle.gridAdaptiveMinSize
        } else {
            base.nodeStyle.gridAdaptiveMinSize
        }
    if (override.nodeStyle.gridSpanContentsList.isNotEmpty()) {
        nodeStyle.gridSpanContentsList.addAll(override.nodeStyle.gridSpanContentsList)
    } else {
        nodeStyle.gridSpanContentsList.addAll(base.nodeStyle.gridSpanContentsList)
    }
    nodeStyle.overflow =
        if (
            override.nodeStyle.overflow != Overflow.OVERFLOW_UNSPECIFIED &&
                override.nodeStyle.overflow != Overflow.OVERFLOW_VISIBLE
        ) {
            override.nodeStyle.overflow
        } else {
            base.nodeStyle.overflow
        }
    nodeStyle.maxChildren =
        if (override.nodeStyle.hasMaxChildren()) {
            override.nodeStyle.maxChildren
        } else {
            base.nodeStyle.maxChildren
        }
    nodeStyle.overflowNodeId =
        if (override.nodeStyle.hasOverflowNodeId()) {
            override.nodeStyle.overflowNodeId
        } else {
            base.nodeStyle.overflowNodeId
        }
    nodeStyle.overflowNodeName =
        if (override.nodeStyle.hasOverflowNodeName()) {
            override.nodeStyle.overflowNodeName
        } else {
            base.nodeStyle.overflowNodeName
        }
    layoutStyle.alignItems =
        if (
            override.layoutStyle.alignItems != AlignItems.ALIGN_ITEMS_UNSPECIFIED &&
                override.layoutStyle.alignItems != AlignItems.ALIGN_ITEMS_STRETCH
        ) {
            override.layoutStyle.alignItems
        } else {
            base.layoutStyle.alignItems
        }
    layoutStyle.alignSelf =
        if (
            override.layoutStyle.alignSelf != AlignSelf.ALIGN_SELF_UNSPECIFIED &&
                override.layoutStyle.alignSelf != AlignSelf.ALIGN_SELF_AUTO
        ) {
            override.layoutStyle.alignSelf
        } else {
            base.layoutStyle.alignSelf
        }
    layoutStyle.alignContent =
        if (
            override.layoutStyle.alignContent != AlignContent.ALIGN_CONTENT_UNSPECIFIED &&
                override.layoutStyle.alignContent != AlignContent.ALIGN_CONTENT_STRETCH
        ) {
            override.layoutStyle.alignContent
        } else {
            base.layoutStyle.alignContent
        }
    layoutStyle.justifyContent =
        if (
            override.layoutStyle.justifyContent != JustifyContent.JUSTIFY_CONTENT_UNSPECIFIED &&
                override.layoutStyle.justifyContent != JustifyContent.JUSTIFY_CONTENT_FLEX_START
        ) {
            override.layoutStyle.justifyContent
        } else {
            base.layoutStyle.justifyContent
        }
    layoutStyle.top =
        if (!override.layoutStyle.top.hasUndefined()) {
            override.layoutStyle.top
        } else {
            base.layoutStyle.top
        }
    layoutStyle.left =
        if (!override.layoutStyle.left.hasUndefined()) {
            override.layoutStyle.left
        } else {
            base.layoutStyle.left
        }
    layoutStyle.bottom =
        if (!override.layoutStyle.bottom.hasUndefined()) {
            override.layoutStyle.bottom
        } else {
            base.layoutStyle.bottom
        }
    layoutStyle.right =
        if (!override.layoutStyle.right.hasUndefined()) {
            override.layoutStyle.right
        } else {
            base.layoutStyle.right
        }

    fun DimensionRect.isDefault(): Boolean {
        return start.hasUndefined() &&
            end.hasUndefined() &&
            top.hasUndefined() &&
            bottom.hasUndefined()
    }
    layoutStyle.margin =
        if (!override.layoutStyle.margin.isDefault()) {
            override.layoutStyle.margin
        } else {
            base.layoutStyle.margin
        }
    layoutStyle.padding =
        if (!override.layoutStyle.padding.isDefault()) {
            override.layoutStyle.padding
        } else {
            base.layoutStyle.padding
        }

    fun ItemSpacing.isDefault(): Boolean {
        return itemSpacingTypeCase == ItemSpacing.ItemSpacingTypeCase.ITEMSPACINGTYPE_NOT_SET ||
            itemSpacingTypeCase == ItemSpacing.ItemSpacingTypeCase.FIXED
    }
    layoutStyle.itemSpacing =
        if (!override.layoutStyle.itemSpacing.isDefault()) {
            override.layoutStyle.itemSpacing
        } else {
            base.layoutStyle.itemSpacing
        }
    nodeStyle.crossAxisItemSpacing =
        if (override.nodeStyle.crossAxisItemSpacing != 0.0f) {
            override.nodeStyle.crossAxisItemSpacing
        } else {
            base.nodeStyle.crossAxisItemSpacing
        }
    layoutStyle.flexGrow =
        if (override.layoutStyle.flexGrow != 0.0f) {
            override.layoutStyle.flexGrow
        } else {
            base.layoutStyle.flexGrow
        }
    layoutStyle.flexShrink =
        if (override.layoutStyle.flexShrink != 0.0f) {
            override.layoutStyle.flexShrink
        } else {
            base.layoutStyle.flexShrink
        }
    layoutStyle.flexBasis =
        if (!override.layoutStyle.flexBasis.hasUndefined()) {
            override.layoutStyle.flexBasis
        } else {
            base.layoutStyle.flexBasis
        }
    layoutStyle.boundingBox =
        if (
            override.layoutStyle.boundingBox.width != 0.0f ||
                override.layoutStyle.boundingBox.height != 0.0f
        ) {
            override.layoutStyle.boundingBox
        } else {
            base.layoutStyle.boundingBox
        }
    nodeStyle.horizontalSizing =
        if (
            override.nodeStyle.horizontalSizing != LayoutSizing.LAYOUT_SIZING_UNSPECIFIED &&
                override.nodeStyle.horizontalSizing != LayoutSizing.LAYOUT_SIZING_FIXED
        ) {
            override.nodeStyle.horizontalSizing
        } else {
            base.nodeStyle.horizontalSizing
        }
    nodeStyle.verticalSizing =
        if (
            override.nodeStyle.verticalSizing != LayoutSizing.LAYOUT_SIZING_UNSPECIFIED &&
                override.nodeStyle.verticalSizing != LayoutSizing.LAYOUT_SIZING_FIXED
        ) {
            override.nodeStyle.verticalSizing
        } else {
            base.nodeStyle.verticalSizing
        }
    layoutStyle.width =
        if (!override.layoutStyle.width.hasUndefined()) {
            override.layoutStyle.width
        } else {
            base.layoutStyle.width
        }
    layoutStyle.height =
        if (!override.layoutStyle.height.hasUndefined()) {
            override.layoutStyle.height
        } else {
            base.layoutStyle.height
        }
    layoutStyle.minWidth =
        if (!override.layoutStyle.minWidth.hasUndefined()) {
            override.layoutStyle.minWidth
        } else {
            base.layoutStyle.minWidth
        }
    layoutStyle.minHeight =
        if (!override.layoutStyle.minHeight.hasUndefined()) {
            override.layoutStyle.minHeight
        } else {
            base.layoutStyle.minHeight
        }
    layoutStyle.maxWidth =
        if (!override.layoutStyle.maxWidth.hasUndefined()) {
            override.layoutStyle.maxWidth
        } else {
            base.layoutStyle.maxWidth
        }
    layoutStyle.maxHeight =
        if (!override.layoutStyle.maxHeight.hasUndefined()) {
            override.layoutStyle.maxHeight
        } else {
            base.layoutStyle.maxHeight
        }
    nodeStyle.aspectRatio =
        if (override.nodeStyle.hasAspectRatio()) {
            override.nodeStyle.aspectRatio
        } else {
            base.nodeStyle.aspectRatio
        }
    nodeStyle.pointerEvents =
        if (
            override.nodeStyle.pointerEvents != PointerEvents.POINTER_EVENTS_AUTO &&
                override.nodeStyle.pointerEvents != PointerEvents.POINTER_EVENTS_UNSPECIFIED
        ) {
            override.nodeStyle.pointerEvents
        } else {
            base.nodeStyle.pointerEvents
        }
    nodeStyle.meterData =
        if (override.nodeStyle.hasMeterData()) {
            override.nodeStyle.meterData
        } else {
            base.nodeStyle.meterData
        }
    style.layoutStyle = layoutStyle.build()
    style.nodeStyle = nodeStyle.build()
    return style.build()
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
