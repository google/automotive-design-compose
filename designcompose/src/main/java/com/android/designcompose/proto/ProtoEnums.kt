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

import com.android.designcompose.serdegen.ScaleMode
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
