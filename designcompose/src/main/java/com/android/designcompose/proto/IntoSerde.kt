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

import com.android.designcompose.android_interface.LayoutChangedResponse
import com.android.designcompose.definition.layout.AlignContent
import com.android.designcompose.definition.layout.AlignItems
import com.android.designcompose.definition.layout.AlignSelf
import com.android.designcompose.definition.layout.FlexDirection
import com.android.designcompose.definition.layout.ItemSpacing
import com.android.designcompose.definition.layout.JustifyContent
import com.android.designcompose.definition.layout.PositionType
import com.android.designcompose.serdegen.Size
import java.util.Optional

internal fun LayoutChangedResponse.Layout.intoSerde() =
    com.android.designcompose.serdegen.Layout(
        order,
        width,
        height,
        left,
        top,
        contentWidth,
        contentHeight,
    )

internal fun ItemSpacing.intoSerde() =
    when (itemSpacingTypeCase) {
        ItemSpacing.ItemSpacingTypeCase.FIXED ->
            com.android.designcompose.serdegen.ItemSpacing(
                Optional.of(com.android.designcompose.serdegen.ItemSpacingType.Fixed(fixed))
            )
        ItemSpacing.ItemSpacingTypeCase.AUTO ->
            com.android.designcompose.serdegen.ItemSpacing(
                Optional.of(
                    com.android.designcompose.serdegen.ItemSpacingType.Auto(
                        com.android.designcompose.serdegen.Auto(auto.width, auto.height)
                    )
                )
            )
        else -> throw IllegalArgumentException("Unknown ItemSpacing: $this") // Should never happen.
    }

internal fun com.android.designcompose.definition.element.Size.intoSerde() = Size(width, height)

internal fun AlignSelf.intoSerde() =
    when (this) {
        AlignSelf.ALIGN_SELF_AUTO -> com.android.designcompose.serdegen.AlignSelf.Auto()
        AlignSelf.ALIGN_SELF_FLEX_START -> com.android.designcompose.serdegen.AlignSelf.FlexStart()
        AlignSelf.ALIGN_SELF_FLEX_END -> com.android.designcompose.serdegen.AlignSelf.FlexEnd()
        AlignSelf.ALIGN_SELF_CENTER -> com.android.designcompose.serdegen.AlignSelf.Center()
        AlignSelf.ALIGN_SELF_BASELINE -> com.android.designcompose.serdegen.AlignSelf.Baseline()
        AlignSelf.ALIGN_SELF_STRETCH -> com.android.designcompose.serdegen.AlignSelf.Stretch()
        else -> throw IllegalArgumentException("Unknown AlignSelf: $this") // Should never happen
    }

internal fun AlignContent.intoSerde() =
    when (this) {
        AlignContent.ALIGN_CONTENT_FLEX_START ->
            com.android.designcompose.serdegen.AlignContent.FlexStart()
        AlignContent.ALIGN_CONTENT_FLEX_END ->
            com.android.designcompose.serdegen.AlignContent.FlexEnd()
        AlignContent.ALIGN_CONTENT_CENTER ->
            com.android.designcompose.serdegen.AlignContent.Center()
        AlignContent.ALIGN_CONTENT_STRETCH ->
            com.android.designcompose.serdegen.AlignContent.Stretch()
        AlignContent.ALIGN_CONTENT_SPACE_BETWEEN ->
            com.android.designcompose.serdegen.AlignContent.SpaceBetween()
        AlignContent.ALIGN_CONTENT_SPACE_AROUND ->
            com.android.designcompose.serdegen.AlignContent.SpaceAround()
        else -> throw IllegalArgumentException("Unknown AlignContent: $this") // Should never happen
    }

internal fun AlignItems.intoSerde() =
    when (this) {
        AlignItems.ALIGN_ITEMS_FLEX_START ->
            com.android.designcompose.serdegen.AlignItems.FlexStart()
        AlignItems.ALIGN_ITEMS_FLEX_END -> com.android.designcompose.serdegen.AlignItems.FlexEnd()
        AlignItems.ALIGN_ITEMS_CENTER -> com.android.designcompose.serdegen.AlignItems.Center()
        AlignItems.ALIGN_ITEMS_BASELINE -> com.android.designcompose.serdegen.AlignItems.Baseline()
        AlignItems.ALIGN_ITEMS_STRETCH -> com.android.designcompose.serdegen.AlignItems.Stretch()
        else -> throw IllegalArgumentException("Unknown AlignItems: $this") // Should never happen
    }

internal fun FlexDirection.intoSerde() =
    when (this) {
        FlexDirection.FLEX_DIRECTION_ROW -> com.android.designcompose.serdegen.FlexDirection.Row()
        FlexDirection.FLEX_DIRECTION_COLUMN ->
            com.android.designcompose.serdegen.FlexDirection.Column()
        FlexDirection.FLEX_DIRECTION_ROW_REVERSE ->
            com.android.designcompose.serdegen.FlexDirection.RowReverse()
        FlexDirection.FLEX_DIRECTION_COLUMN_REVERSE ->
            com.android.designcompose.serdegen.FlexDirection.ColumnReverse()
        else ->
            throw IllegalArgumentException("Unknown FlexDirection: $this") // Should never happen
    }

internal fun JustifyContent.intoSerde() =
    when (this) {
        JustifyContent.JUSTIFY_CONTENT_FLEX_START ->
            com.android.designcompose.serdegen.JustifyContent.FlexStart()
        JustifyContent.JUSTIFY_CONTENT_FLEX_END ->
            com.android.designcompose.serdegen.JustifyContent.FlexEnd()
        JustifyContent.JUSTIFY_CONTENT_CENTER ->
            com.android.designcompose.serdegen.JustifyContent.Center()
        JustifyContent.JUSTIFY_CONTENT_SPACE_BETWEEN ->
            com.android.designcompose.serdegen.JustifyContent.SpaceBetween()
        JustifyContent.JUSTIFY_CONTENT_SPACE_AROUND ->
            com.android.designcompose.serdegen.JustifyContent.SpaceAround()
        JustifyContent.JUSTIFY_CONTENT_SPACE_EVENLY ->
            com.android.designcompose.serdegen.JustifyContent.SpaceEvenly()
        else ->
            throw IllegalArgumentException("Unknown JustifyContent: $this") // Should never happen
    }

internal fun PositionType.intoSerde() =
    when (this) {
        PositionType.POSITION_TYPE_RELATIVE ->
            com.android.designcompose.serdegen.PositionType.Relative()
        PositionType.POSITION_TYPE_ABSOLUTE ->
            com.android.designcompose.serdegen.PositionType.Absolute()
        else -> throw IllegalArgumentException("Unknown PositionType: $this") // Should never happen
    }
