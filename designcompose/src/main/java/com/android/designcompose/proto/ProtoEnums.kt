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
import com.android.designcompose.serdegen.BlendMode
import com.android.designcompose.serdegen.Display
import com.android.designcompose.serdegen.FlexDirection
import com.android.designcompose.serdegen.FontStyle
import com.android.designcompose.serdegen.JustifyContent
import com.android.designcompose.serdegen.Overflow
import com.android.designcompose.serdegen.PositionType
import com.android.designcompose.serdegen.RenderMethod
import com.android.designcompose.serdegen.ScaleMode
import com.android.designcompose.serdegen.StrokeCap
import com.android.designcompose.serdegen.TextAlign
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
@Deprecated("This function will be removed in the future.")
enum class StrokeAlignType {
    Unspecified,
    Inside,
    Center,
    Outside,
}

@Deprecated("This function will be removed in the future.")
internal fun strokeAlignFromInt(int: Int): StrokeAlignType {
    return when (int) {
        1 -> StrokeAlignType.Inside
        2 -> StrokeAlignType.Center
        3 -> StrokeAlignType.Outside
        else -> StrokeAlignType.Unspecified
    }
}

@Deprecated("This function will be removed in the future.")
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
@Deprecated("This function will be removed in the future.")
internal fun scaleModeFromInt(value: Int): ScaleMode {
    return when (value) {
        1 -> ScaleMode.Fill()
        2 -> ScaleMode.Fit()
        3 -> ScaleMode.Tile()
        4 -> ScaleMode.Stretch()
        else -> ScaleMode.Unspecified()
    }
}

@Deprecated("This function will be removed in the future.")
enum class WindingRuleType {
    NonZero,
    EvenOdd,
}

@Deprecated("This function will be removed in the future.")
// 1 -> NonZero,
// 2 -> EvenOdd
internal fun windingRuleFromInt(value: Int): WindingRuleType {
    return when (value) {
        1 -> WindingRuleType.NonZero
        else -> WindingRuleType.EvenOdd
    }
}

@Deprecated("This function will be removed in the future.")
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

@Deprecated("This function will be removed in the future.")
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

@Deprecated("This function will be removed in the future.")
enum class OverlayBackgroundInteractionEnum {
    UNSPECIFIED,
    NONE,
    CLOSE_ON_CLICK_OUTSIDE,
}

@Deprecated("This function will be removed in the future.")
internal fun overlayBackgroundInteractionFromInt(value: Int): OverlayBackgroundInteractionEnum {
    return when (value) {
        1 -> OverlayBackgroundInteractionEnum.NONE
        2 -> OverlayBackgroundInteractionEnum.CLOSE_ON_CLICK_OUTSIDE
        else -> OverlayBackgroundInteractionEnum.UNSPECIFIED
    }
}

@Deprecated("This function will be removed in the future.")
enum class NavigationType {
    Unspecified,
    Navigate,
    Swap,
    Overlay,
    ScrollTo,
    ChangeTo,
}

@Deprecated("This function will be removed in the future.")
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

@Deprecated("This function will be removed in the future.")
// Getter for Trigger which returns it's TriggerType if it's not null.
val Optional<Trigger>.type: TriggerType?
    get() {
        return this.getOrNull()?.trigger_type?.getOrNull()
    }

@Deprecated("This function will be removed in the future.")
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

@Deprecated("This function will be removed in the future.")
internal fun alignItemsFromInt(value: Int) =
    when (value) {
        1 -> AlignItems.FlexStart()
        2 -> AlignItems.FlexEnd()
        3 -> AlignItems.Center()
        4 -> AlignItems.Baseline()
        5 -> AlignItems.Stretch()
        else -> AlignItems.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun AlignItems.toInt() =
    when (this) {
        is AlignItems.FlexStart -> 1
        is AlignItems.FlexEnd -> 2
        is AlignItems.Center -> 3
        is AlignItems.Baseline -> 4
        is AlignItems.Stretch -> 5
        else -> 0
    }

@Deprecated("This function will be removed in the future.")
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

@Deprecated("This function will be removed in the future.")
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

@Deprecated("This function will be removed in the future.")
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

@Deprecated("This function will be removed in the future.")
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

@Deprecated("This function will be removed in the future.")
internal fun flexDirectionFromInt(value: Int) =
    when (value) {
        1 -> FlexDirection.Row()
        2 -> FlexDirection.Column()
        3 -> FlexDirection.RowReverse()
        4 -> FlexDirection.ColumnReverse()
        5 -> FlexDirection.None()
        else -> FlexDirection.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun FlexDirection.toInt() =
    when (this) {
        is FlexDirection.Row -> 1
        is FlexDirection.Column -> 2
        is FlexDirection.RowReverse -> 3
        is FlexDirection.ColumnReverse -> 4
        is FlexDirection.None -> 5
        else -> 0
    }

@Deprecated("This function will be removed in the future.")
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

@Deprecated("This function will be removed in the future.")
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

@Deprecated("This function will be removed in the future.")
internal fun positionTypeFromInt(value: Int) =
    when (value) {
        1 -> PositionType.Relative()
        2 -> PositionType.Absolute()
        else -> PositionType.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun PositionType.toInt() =
    when (this) {
        is PositionType.Relative -> 1
        is PositionType.Absolute -> 2
        else -> 0
    }

@Deprecated("This function will be removed in the future.")
internal fun overflowDirectionFromInt(value: Int) =
    when (value) {
        1 -> com.android.designcompose.serdegen.OverflowDirection.None()
        2 -> com.android.designcompose.serdegen.OverflowDirection.HorizontalScrolling()
        3 -> com.android.designcompose.serdegen.OverflowDirection.VerticalScrolling()
        4 -> com.android.designcompose.serdegen.OverflowDirection.HorizontalAndVerticalScrolling()
        else -> com.android.designcompose.serdegen.OverflowDirection.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun com.android.designcompose.serdegen.OverflowDirection.toInt() =
    when (this) {
        is com.android.designcompose.serdegen.OverflowDirection.None -> 1
        is com.android.designcompose.serdegen.OverflowDirection.HorizontalScrolling -> 2
        is com.android.designcompose.serdegen.OverflowDirection.VerticalScrolling -> 3
        is com.android.designcompose.serdegen.OverflowDirection.HorizontalAndVerticalScrolling -> 4
        else -> 0
    }

@Deprecated("This function will be removed in the future.")
internal fun layoutSizingFromInt(value: Int) =
    when (value) {
        1 -> com.android.designcompose.serdegen.LayoutSizing.Fixed()
        2 -> com.android.designcompose.serdegen.LayoutSizing.Hug()
        3 -> com.android.designcompose.serdegen.LayoutSizing.Fill()
        else -> com.android.designcompose.serdegen.LayoutSizing.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun com.android.designcompose.serdegen.LayoutSizing.toInt() =
    when (this) {
        is com.android.designcompose.serdegen.LayoutSizing.Fixed -> 1
        is com.android.designcompose.serdegen.LayoutSizing.Hug -> 2
        is com.android.designcompose.serdegen.LayoutSizing.Fill -> 3
        else -> 0
    }

@Deprecated("This function will be removed in the future.")
internal fun fontStyleFromInt(value: Int) =
    when (value) {
        1 -> com.android.designcompose.serdegen.FontStyle.Normal()
        2 -> com.android.designcompose.serdegen.FontStyle.Italic()
        3 -> com.android.designcompose.serdegen.FontStyle.Oblique()
        else -> com.android.designcompose.serdegen.FontStyle.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun FontStyle.toInt() =
    when (this) {
        is FontStyle.Normal -> 1
        is FontStyle.Italic -> 2
        is FontStyle.Oblique -> 3
        else -> 0
    }

@Deprecated("This function will be removed in the future.")
internal fun textDecorationFromInt(value: Int) =
    when (value) {
        1 -> com.android.designcompose.serdegen.TextDecoration.None()
        2 -> com.android.designcompose.serdegen.TextDecoration.Underline()
        3 -> com.android.designcompose.serdegen.TextDecoration.Strikethrough()
        else -> com.android.designcompose.serdegen.TextDecoration.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun com.android.designcompose.serdegen.TextDecoration.toInt() =
    when (this) {
        is com.android.designcompose.serdegen.TextDecoration.None -> 1
        is com.android.designcompose.serdegen.TextDecoration.Underline -> 2
        is com.android.designcompose.serdegen.TextDecoration.Strikethrough -> 3
        else -> 0
    }

@Deprecated("This function will be removed in the future.")
internal fun blendModeFromInt(value: Int) =
    when (value) {
        1 -> com.android.designcompose.serdegen.BlendMode.PassThrough()
        2 -> com.android.designcompose.serdegen.BlendMode.Normal()
        3 -> com.android.designcompose.serdegen.BlendMode.Darken()
        4 -> com.android.designcompose.serdegen.BlendMode.Multiply()
        5 -> com.android.designcompose.serdegen.BlendMode.LinearBurn()
        6 -> com.android.designcompose.serdegen.BlendMode.ColorBurn()
        7 -> com.android.designcompose.serdegen.BlendMode.Lighten()
        8 -> com.android.designcompose.serdegen.BlendMode.Screen()
        9 -> com.android.designcompose.serdegen.BlendMode.LinearDodge()
        10 -> com.android.designcompose.serdegen.BlendMode.ColorDodge()
        11 -> com.android.designcompose.serdegen.BlendMode.Overlay()
        12 -> com.android.designcompose.serdegen.BlendMode.SoftLight()
        13 -> com.android.designcompose.serdegen.BlendMode.HardLight()
        14 -> com.android.designcompose.serdegen.BlendMode.Difference()
        15 -> com.android.designcompose.serdegen.BlendMode.Exclusion()
        16 -> com.android.designcompose.serdegen.BlendMode.Hue()
        17 -> com.android.designcompose.serdegen.BlendMode.Saturation()
        18 -> com.android.designcompose.serdegen.BlendMode.Color()
        19 -> com.android.designcompose.serdegen.BlendMode.Luminosity()
        else -> com.android.designcompose.serdegen.BlendMode.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun BlendMode.toInt() =
    when (this) {
        is BlendMode.PassThrough -> 1
        is BlendMode.Normal -> 2
        is BlendMode.Darken -> 3
        is BlendMode.Multiply -> 4
        is BlendMode.LinearBurn -> 5
        is BlendMode.ColorBurn -> 6
        is BlendMode.Lighten -> 7
        is BlendMode.Screen -> 8
        is BlendMode.LinearDodge -> 9
        is BlendMode.ColorDodge -> 10
        is BlendMode.Overlay -> 11
        is BlendMode.SoftLight -> 12
        is BlendMode.HardLight -> 13
        is BlendMode.Difference -> 14
        is BlendMode.Exclusion -> 15
        is BlendMode.Hue -> 16
        is BlendMode.Saturation -> 17
        is BlendMode.Color -> 18
        is BlendMode.Luminosity -> 19

        else -> 0
    }

@Deprecated("This function will be removed in the future.")
internal fun overflowFromInt(value: Int) =
    when (value) {
        1 -> Overflow.Visible()
        2 -> Overflow.Hidden()
        3 -> Overflow.Scroll()
        else -> Overflow.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun Overflow.toInt() =
    when (this) {
        is Overflow.Visible -> 1
        is Overflow.Hidden -> 2
        is Overflow.Scroll -> 3
        else -> 0
    }

@Deprecated("This function will be removed in the future.")
internal fun textAlignFromInt(value: Int) =
    when (value) {
        1 -> TextAlign.Left()
        2 -> TextAlign.Center()
        3 -> TextAlign.Right()
        else -> TextAlign.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun TextAlign.toInt() =
    when (this) {
        is TextAlign.Left -> 1
        is TextAlign.Center -> 2
        is TextAlign.Right -> 3
        else -> 0
    }

@Deprecated("This function will be removed in the future.")
internal fun textAlignVerticalFromInt(value: Int) =
    when (value) {
        1 -> com.android.designcompose.serdegen.TextAlignVertical.Top()
        2 -> com.android.designcompose.serdegen.TextAlignVertical.Center()
        3 -> com.android.designcompose.serdegen.TextAlignVertical.Bottom()
        else -> com.android.designcompose.serdegen.TextAlignVertical.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun com.android.designcompose.serdegen.TextAlignVertical.toInt() =
    when (this) {
        is com.android.designcompose.serdegen.TextAlignVertical.Top -> 1
        is com.android.designcompose.serdegen.TextAlignVertical.Center -> 2
        is com.android.designcompose.serdegen.TextAlignVertical.Bottom -> 3
        else -> 0
    }

@Deprecated("This function will be removed in the future.")
internal fun textOverflowFromInt(value: Int) =
    when (value) {
        1 -> com.android.designcompose.serdegen.TextOverflow.Clip()
        2 -> com.android.designcompose.serdegen.TextOverflow.Ellipsis()
        else -> com.android.designcompose.serdegen.TextOverflow.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun com.android.designcompose.serdegen.TextOverflow.toInt() =
    when (this) {
        is com.android.designcompose.serdegen.TextOverflow.Clip -> 1
        is com.android.designcompose.serdegen.TextOverflow.Ellipsis -> 2
        else -> 0
    }

@Deprecated("This function will be removed in the future.")
internal fun pointerEventsFromInt(value: Int) =
    when (value) {
        1 -> com.android.designcompose.serdegen.PointerEvents.Auto()
        2 -> com.android.designcompose.serdegen.PointerEvents.None()
        3 -> com.android.designcompose.serdegen.PointerEvents.Inherit()
        else -> com.android.designcompose.serdegen.PointerEvents.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun com.android.designcompose.serdegen.PointerEvents.toInt() =
    when (this) {
        is com.android.designcompose.serdegen.PointerEvents.Auto -> 1
        is com.android.designcompose.serdegen.PointerEvents.None -> 2
        is com.android.designcompose.serdegen.PointerEvents.Inherit -> 3
        else -> 0
    }

@Deprecated("This function will be removed in the future.")
internal fun displayFromInt(value: Int) =
    when (value) {
        1 -> Display.Flex()
        2 -> Display.None()
        else -> Display.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun Display.toInt() =
    when (this) {
        is Display.Flex -> 1
        is Display.None -> 2
        else -> 0
    }

@Deprecated("This function will be removed in the future.")
internal fun flexWrapFromInt(value: Int) =
    when (value) {
        1 -> com.android.designcompose.serdegen.FlexWrap.NoWrap()
        2 -> com.android.designcompose.serdegen.FlexWrap.Wrap()
        3 -> com.android.designcompose.serdegen.FlexWrap.WrapReverse()
        else -> com.android.designcompose.serdegen.FlexWrap.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun com.android.designcompose.serdegen.FlexWrap.toInt() =
    when (this) {
        is com.android.designcompose.serdegen.FlexWrap.NoWrap -> 1
        is com.android.designcompose.serdegen.FlexWrap.Wrap -> 2
        is com.android.designcompose.serdegen.FlexWrap.WrapReverse -> 3
        else -> 0
    }

@Deprecated("This function will be removed in the future.")
internal fun gridLayoutTypeFromInt(value: Int) =
    when (value) {
        1 -> com.android.designcompose.serdegen.GridLayoutType.FixedColumns()
        2 -> com.android.designcompose.serdegen.GridLayoutType.FixedRows()
        3 -> com.android.designcompose.serdegen.GridLayoutType.AutoColumns()
        4 -> com.android.designcompose.serdegen.GridLayoutType.AutoRows()
        5 -> com.android.designcompose.serdegen.GridLayoutType.Horizontal()
        6 -> com.android.designcompose.serdegen.GridLayoutType.Vertical()
        else -> com.android.designcompose.serdegen.GridLayoutType.Unspecified()
    }

@Deprecated("This function will be removed in the future.")
internal fun RenderMethod.toInt() =
    when (this) {
        is RenderMethod.None -> 1
        is RenderMethod.PixelPerfect -> 2
        else -> 0
    }
