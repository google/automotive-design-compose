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

package com.android.designcompose.proto

import com.android.designcompose.proto.layout.LayoutManager.Layout
import com.android.designcompose.proto.layout.LayoutStyleOuterClass
import com.android.designcompose.proto.layout.Styles
import com.android.designcompose.proto.layout.Styles.AlignContentEnum.AlignContent
import com.android.designcompose.proto.layout.Styles.AlignItemsEnum.AlignItems
import com.android.designcompose.proto.layout.Styles.AlignSelfEnum.AlignSelf
import com.android.designcompose.proto.layout.Styles.FlexDirectionEnum.FlexDirection
import com.android.designcompose.proto.layout.Styles.JustifyContentEnum.JustifyContent
import com.android.designcompose.proto.layout.Styles.PositionTypeEnum.PositionType
import com.android.designcompose.proto.layout.Types
import com.android.designcompose.proto.layout.Types.DimensionProto
import com.android.designcompose.proto.layout.Types.FloatSize
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.LayoutStyle
import com.android.designcompose.serdegen.Rect
import com.android.designcompose.serdegen.Size

internal fun Layout.intoSerde() =
    com.android.designcompose.serdegen.Layout(order, width, height, left, top)

/* height = */ internal
/* left = */ fun DimensionProto.intoSerde() =
    when (dimensionCase) {
        DimensionProto.DimensionCase.UNDEFINED -> Dimension.Undefined()
        DimensionProto.DimensionCase.AUTO -> Dimension.Auto()
        DimensionProto.DimensionCase.POINTS -> Dimension.Points(points)
        DimensionProto.DimensionCase.PERCENT -> Dimension.Percent(percent)
        else ->
            throw IllegalArgumentException("Unknown DimensionProto: $this") // Should never happen.
    }

internal fun Types.DimensionRect.intoSerde() =
    Rect(start.intoSerde(), end.intoSerde(), top.intoSerde(), bottom.intoSerde())

internal fun Styles.ItemSpacing.intoSerde() =
    when (typeCase) {
        Styles.ItemSpacing.TypeCase.FIXED ->
            com.android.designcompose.serdegen.ItemSpacing.Fixed(fixed)
        Styles.ItemSpacing.TypeCase.AUTO ->
            com.android.designcompose.serdegen.ItemSpacing.Auto(auto.width, auto.height)
        else -> throw IllegalArgumentException("Unknown ItemSpacing: $this") // Should never happen.
    }

internal fun FloatSize.intoSerde() = Size(width, height)

internal fun AlignSelf.intoSerde() =
    when (this) {
        AlignSelf.AUTO -> com.android.designcompose.serdegen.AlignSelf.Auto()
        AlignSelf.FLEX_START -> com.android.designcompose.serdegen.AlignSelf.FlexStart()
        AlignSelf.FLEX_END -> com.android.designcompose.serdegen.AlignSelf.FlexEnd()
        AlignSelf.CENTER -> com.android.designcompose.serdegen.AlignSelf.Center()
        AlignSelf.BASELINE -> com.android.designcompose.serdegen.AlignSelf.Baseline()
        AlignSelf.STRETCH -> com.android.designcompose.serdegen.AlignSelf.Stretch()
        else -> throw IllegalArgumentException("Unknown AlignSelf: $this") // Should never happen
    }

internal fun AlignContent.intoSerde() =
    when (this) {
        AlignContent.FLEX_START -> com.android.designcompose.serdegen.AlignContent.FlexStart()
        AlignContent.FLEX_END -> com.android.designcompose.serdegen.AlignContent.FlexEnd()
        AlignContent.CENTER -> com.android.designcompose.serdegen.AlignContent.Center()
        AlignContent.STRETCH -> com.android.designcompose.serdegen.AlignContent.Stretch()
        AlignContent.SPACE_BETWEEN -> com.android.designcompose.serdegen.AlignContent.SpaceBetween()
        AlignContent.SPACE_AROUND -> com.android.designcompose.serdegen.AlignContent.SpaceAround()
        else -> throw IllegalArgumentException("Unknown AlignContent: $this") // Should never happen
    }

internal fun AlignItems.intoSerde() =
    when (this) {
        AlignItems.FLEX_START -> com.android.designcompose.serdegen.AlignItems.FlexStart()
        AlignItems.FLEX_END -> com.android.designcompose.serdegen.AlignItems.FlexEnd()
        AlignItems.CENTER -> com.android.designcompose.serdegen.AlignItems.Center()
        AlignItems.BASELINE -> com.android.designcompose.serdegen.AlignItems.Baseline()
        AlignItems.STRETCH -> com.android.designcompose.serdegen.AlignItems.Stretch()
        else -> throw IllegalArgumentException("Unknown AlignItems: $this") // Should never happen
    }

internal fun FlexDirection.intoSerde() =
    when (this) {
        FlexDirection.ROW -> com.android.designcompose.serdegen.FlexDirection.Row()
        FlexDirection.COLUMN -> com.android.designcompose.serdegen.FlexDirection.Column()
        FlexDirection.ROW_REVERSE -> com.android.designcompose.serdegen.FlexDirection.RowReverse()
        FlexDirection.COLUMN_REVERSE ->
            com.android.designcompose.serdegen.FlexDirection.ColumnReverse()
        else ->
            throw IllegalArgumentException("Unknown FlexDirection: $this") // Should never happen
    }

internal fun JustifyContent.intoSerde() =
    when (this) {
        JustifyContent.FLEX_START -> com.android.designcompose.serdegen.JustifyContent.FlexStart()
        JustifyContent.FLEX_END -> com.android.designcompose.serdegen.JustifyContent.FlexEnd()
        JustifyContent.CENTER -> com.android.designcompose.serdegen.JustifyContent.Center()
        JustifyContent.SPACE_BETWEEN ->
            com.android.designcompose.serdegen.JustifyContent.SpaceBetween()
        JustifyContent.SPACE_AROUND ->
            com.android.designcompose.serdegen.JustifyContent.SpaceAround()
        JustifyContent.SPACE_EVENLY ->
            com.android.designcompose.serdegen.JustifyContent.SpaceEvenly()
        else ->
            throw IllegalArgumentException("Unknown JustifyContent: $this") // Should never happen
    }

internal fun PositionType.intoSerde() =
    when (this) {
        PositionType.RELATIVE -> com.android.designcompose.serdegen.PositionType.Relative()
        PositionType.ABSOLUTE -> com.android.designcompose.serdegen.PositionType.Absolute()
        else -> throw IllegalArgumentException("Unknown PositionType: $this") // Should never happen
    }

/** Temporary (I hope) conversion from the Proto layout style to the Serde layout style. */
internal fun LayoutStyleOuterClass.LayoutStyle.intoSerde() =
    LayoutStyle(
        margin.intoSerde(),
        padding.intoSerde(),
        itemSpacing.intoSerde(),
        top.intoSerde(),
        left.intoSerde(),
        bottom.intoSerde(),
        right.intoSerde(),
        width.intoSerde(),
        height.intoSerde(),
        minWidth.intoSerde(),
        maxWidth.intoSerde(),
        minHeight.intoSerde(),
        maxHeight.intoSerde(),
        boundingBox.intoSerde(),
        flexGrow,
        flexShrink,
        flexBasis.intoSerde(),
        alignSelf.intoSerde(),
        alignContent.intoSerde(),
        alignItems.intoSerde(),
        flexDirection.intoSerde(),
        justifyContent.intoSerde(),
        positionType.intoSerde()
    )
