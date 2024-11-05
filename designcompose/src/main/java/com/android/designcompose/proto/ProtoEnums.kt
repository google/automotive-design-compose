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

import com.android.designcompose.serdegen.AlignContent
import com.android.designcompose.serdegen.AlignItems
import com.android.designcompose.serdegen.AlignSelf
import com.android.designcompose.serdegen.FlexDirection
import com.android.designcompose.serdegen.JustifyContent
import com.android.designcompose.serdegen.PositionType
import com.android.designcompose.serdegen.ScaleMode
import com.android.designcompose.serdegen.StrokeCap
import com.android.designcompose.serdegen.Trigger
import com.android.designcompose.serdegen.TriggerType
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

// We don't have enum values due to the java code being generated from Rust, which was
// generated from proto. This file contains functions to translate between integers and enum values.
// Restore use of enums once java is generated from proto.

// StrokeAlign
// Unspecified = 0,
// Inside = 1,
// Center = 2,
// Outside = 3,
enum class StrokeAlignType {
    Unspecified,
    Inside,
    Center,
    Outside,
}

internal fun strokeAlignFromInt(int: Int): StrokeAlignType {
    return when (int) {
        1 -> StrokeAlignType.Inside
        2 -> StrokeAlignType.Center
        3 -> StrokeAlignType.Outside
        else -> StrokeAlignType.Unspecified
    }
}

internal fun strokeAlignTypeToInt(align: StrokeAlignType): Int {
    return when (align) {
        StrokeAlignType.Inside -> 1
        StrokeAlignType.Center -> 2
        StrokeAlignType.Outside -> 3
        StrokeAlignType.Unspecified -> 0
    }
}

// Unspecified = 0,
// Fill = 1,
// Fit = 2,
// Tile = 3,
// Stretch = 4,
internal fun scaleModeFromInt(value: Int): ScaleMode {
    return when (value) {
        1 -> ScaleMode.Fill()
        2 -> ScaleMode.Fit()
        3 -> ScaleMode.Tile()
        4 -> ScaleMode.Stretch()
        else -> ScaleMode.Unspecified()
    }
}

enum class WindingRuleType {
    NonZero,
    EvenOdd,
}

// 1 -> NonZero,
// 2 -> EvenOdd
internal fun windingRuleFromInt(value: Int): WindingRuleType {
    return when (value) {
        1 -> WindingRuleType.NonZero
        else -> WindingRuleType.EvenOdd
    }
}

enum class OverlayPositionEnum {
    UNSPECIFIED,
    CENTER,
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    MANUAL,
}

internal fun overlayPositionEnumFromInt(value: Int): OverlayPositionEnum {
    return when (value) {
        1 -> OverlayPositionEnum.CENTER
        2 -> OverlayPositionEnum.TOP_LEFT
        3 -> OverlayPositionEnum.TOP_CENTER
        4 -> OverlayPositionEnum.TOP_RIGHT
        5 -> OverlayPositionEnum.BOTTOM_LEFT
        6 -> OverlayPositionEnum.BOTTOM_CENTER
        7 -> OverlayPositionEnum.BOTTOM_RIGHT
        8 -> OverlayPositionEnum.MANUAL
        else -> OverlayPositionEnum.UNSPECIFIED
    }
}

enum class OverlayBackgroundInteractionEnum {
    UNSPECIFIED,
    NONE,
    CLOSE_ON_CLICK_OUTSIDE,
}

internal fun overlayBackgroundInteractionFromInt(value: Int): OverlayBackgroundInteractionEnum {
    return when (value) {
        1 -> OverlayBackgroundInteractionEnum.NONE
        2 -> OverlayBackgroundInteractionEnum.CLOSE_ON_CLICK_OUTSIDE
        else -> OverlayBackgroundInteractionEnum.UNSPECIFIED
    }
}

enum class NavigationType {
    Unspecified,
    Navigate,
    Swap,
    Overlay,
    ScrollTo,
    ChangeTo,
}

internal fun navigationTypeFromInt(value: Int): NavigationType {
    return when (value) {
        1 -> NavigationType.Navigate
        2 -> NavigationType.Swap
        3 -> NavigationType.Overlay
        4 -> NavigationType.ScrollTo
        5 -> NavigationType.ChangeTo
        else -> NavigationType.Unspecified
    }
}

// Getter for Trigger which returns it's TriggerType if it's not null.
val Optional<Trigger>.type: TriggerType?
    get() {
        return this.getOrNull()?.trigger_type?.getOrNull()
    }

internal fun strokeCapFromInt(value: Int): StrokeCap {
    return when (value) {
        1 -> StrokeCap.None()
        2 -> StrokeCap.Round()
        3 -> StrokeCap.Square()
        4 -> StrokeCap.LineArrow()
        5 -> StrokeCap.TriangleArrow()
        6 -> StrokeCap.CircleFilled()
        7 -> StrokeCap.DiamondFilled()
        else -> StrokeCap.Unspecified()
    }
}

internal fun alignItemsFromInt(value: Int) =
    when (value) {
        1 -> AlignItems.FlexStart()
        2 -> AlignItems.FlexEnd()
        3 -> AlignItems.Center()
        4 -> AlignItems.Baseline()
        5 -> AlignItems.Stretch()
        else -> AlignItems.Unspecified()
    }

internal fun AlignItems.toInt() =
    when (this) {
        is AlignItems.FlexStart -> 1
        is AlignItems.FlexEnd -> 2
        is AlignItems.Center -> 3
        is AlignItems.Baseline -> 4
        is AlignItems.Stretch -> 5
        else -> 0
    }

internal fun alignSelfFromInt(value: Int) =
    when (value) {
        1 -> AlignSelf.Auto()
        2 -> AlignSelf.FlexStart()
        3 -> AlignSelf.FlexEnd()
        4 -> AlignSelf.Center()
        5 -> AlignSelf.Baseline()
        6 -> AlignSelf.Stretch()
        else -> AlignSelf.Unspecified()
    }

internal fun AlignSelf.toInt() =
    when (this) {
        is AlignSelf.Auto -> 1
        is AlignSelf.FlexStart -> 2
        is AlignSelf.FlexEnd -> 3
        is AlignSelf.Center -> 4
        is AlignSelf.Baseline -> 5
        is AlignSelf.Stretch -> 6
        else -> 0
    }

internal fun alignContentFromInt(value: Int) =
    when (value) {
        1 -> AlignContent.FlexStart()
        2 -> AlignContent.FlexEnd()
        3 -> AlignContent.Center()
        4 -> AlignContent.Stretch()
        5 -> AlignContent.SpaceBetween()
        6 -> AlignContent.SpaceAround()
        else -> AlignContent.Unspecified()
    }

internal fun AlignContent.toInt() =
    when (this) {
        is AlignContent.FlexStart -> 1
        is AlignContent.FlexEnd -> 2
        is AlignContent.Center -> 3
        is AlignContent.Stretch -> 4
        is AlignContent.SpaceBetween -> 5
        is AlignContent.SpaceAround -> 6
        else -> 0
    }

internal fun flexDirectionFromInt(value: Int) =
    when (value) {
        1 -> FlexDirection.Row()
        2 -> FlexDirection.Column()
        3 -> FlexDirection.RowReverse()
        4 -> FlexDirection.ColumnReverse()
        5 -> FlexDirection.None()
        else -> FlexDirection.Unspecified()
    }

internal fun FlexDirection.toInt() =
    when (this) {
        is FlexDirection.Row -> 1
        is FlexDirection.Column -> 2
        is FlexDirection.RowReverse -> 3
        is FlexDirection.ColumnReverse -> 4
        is FlexDirection.None -> 5
        else -> 0
    }

internal fun justifyContentFromInt(value: Int) =
    when (value) {
        1 -> JustifyContent.FlexStart()
        2 -> JustifyContent.FlexEnd()
        3 -> JustifyContent.Center()
        4 -> JustifyContent.SpaceBetween()
        5 -> JustifyContent.SpaceAround()
        6 -> JustifyContent.SpaceEvenly()
        else -> JustifyContent.Unspecified()
    }

internal fun JustifyContent.toInt() =
    when (this) {
        is JustifyContent.FlexStart -> 1
        is JustifyContent.FlexEnd -> 2
        is JustifyContent.Center -> 3
        is JustifyContent.SpaceBetween -> 4
        is JustifyContent.SpaceAround -> 5
        is JustifyContent.SpaceEvenly -> 6
        else -> 0
    }

internal fun positionTypeFromInt(value: Int) =
    when (value) {
        1 -> PositionType.Relative()
        2 -> PositionType.Absolute()
        else -> PositionType.Unspecified()
    }

internal fun PositionType.toInt() =
    when (this) {
        is PositionType.Relative -> 1
        is PositionType.Absolute -> 2
        else -> 0
    }

internal fun overflowDirectionFromInt(value: Int) =
    when (value) {
        1 -> com.android.designcompose.serdegen.OverflowDirection.None()
        2 -> com.android.designcompose.serdegen.OverflowDirection.HorizontalScrolling()
        3 -> com.android.designcompose.serdegen.OverflowDirection.VerticalScrolling()
        4 -> com.android.designcompose.serdegen.OverflowDirection.HorizontalAndVerticalScrolling()
        else -> com.android.designcompose.serdegen.OverflowDirection.Unspecified()
    }

internal fun layoutSizingFromInt(value: Int) =
    when (value) {
        1 -> com.android.designcompose.serdegen.LayoutSizing.Fixed()
        2 -> com.android.designcompose.serdegen.LayoutSizing.Hug()
        3 -> com.android.designcompose.serdegen.LayoutSizing.Fill()
        else -> com.android.designcompose.serdegen.LayoutSizing.Unspecified()
    }
