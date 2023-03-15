/*
 * Copyright 2023 Google LLC
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

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.android.designcompose.serdegen.AlignItems
import com.android.designcompose.serdegen.AlignSelf
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.FlexDirection
import com.android.designcompose.serdegen.GridLayoutType
import com.android.designcompose.serdegen.GridSpan
import com.android.designcompose.serdegen.ItemSpacing
import com.android.designcompose.serdegen.JustifyContent
import com.android.designcompose.serdegen.OverflowDirection
import com.android.designcompose.serdegen.PositionType
import com.android.designcompose.serdegen.TextAlignVertical
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewStyle
import kotlin.math.ceil

@Composable
internal inline fun DesignLayout(
    modifier: Modifier = Modifier,
    style: ViewStyle,
    name: String = "unnamed",
    content: @Composable () -> Unit
) {
    val measurePolicy = rememberDesignMeasurePolicy(name, style)
    Layout(content = content, measurePolicy = measurePolicy, modifier = modifier)
}

// We need to put the parent layout properties in here; but for now
// since we're doing absolute positioning only, we don't have anything
// to consult.
//
// Do we need width/height and min/max variants? Or do we just hope the
// parent layout does the right thing with them?
@Composable
internal fun rememberDesignMeasurePolicy(name: String, style: ViewStyle) =
    remember(name, style) { designMeasurePolicy(name, style) }

internal fun designMeasurePolicy(name: String, style: ViewStyle) =
    MeasurePolicy { measurables, constraints ->
        var selfWidth = constraints.minWidth
        var selfHeight = constraints.minHeight

        // If we're absolutely positioned then we can extract our bounds directly from our style.
        when (style.position_type) {
            is PositionType.Absolute -> {
                val absBounds = absoluteLayout(style, constraints, density)
                selfWidth = absBounds.width()
                selfHeight = absBounds.height()
            }
            is PositionType.Relative -> {
                val relBounds = relativeLayout(style, constraints, density)
                if (style.width !is Dimension.Undefined) {
                    selfWidth = relBounds.width()
                }
                if (style.height !is Dimension.Undefined) {
                    selfHeight = relBounds.height()
                }
            }
        }

        // Restrict to the parent constraints; we do not honor max constraints; if we exceed them
        // then it's because we are required to by the specified layout.
        if (constraints.minWidth > selfWidth) selfWidth = constraints.minWidth
        if (constraints.minHeight > selfHeight) selfHeight = constraints.minHeight

        // If we have no children, then bail out here.
        if (measurables.isEmpty()) {
            return@MeasurePolicy layout(selfWidth, selfHeight) {}
        }

        val placeables = arrayOfNulls<Placeable>(measurables.size)
        val childBounds = arrayOfNulls<Rect>(measurables.size)

        measurables.forEachIndexed { index, measurable ->
            val childConstraints =
                Constraints(
                    minWidth = 0,
                    minHeight = 0,
                    maxWidth = selfWidth,
                    maxHeight = selfHeight
                )
            val designChildData = measurable.designChildData

            if (designChildData != null) {
                // This block replicates some of the logic in `absoluteLayout` and `relativeLayout`
                // because we use the width and height from `measurable.measure` instead of whatever
                // is in the style (which allows for elements to take more or less space at runtime
                // than in the design -- e.g.: if a text node had its content replaced with
                // something
                // shorter or longer, and it is set to have Auto Width or Auto Width and Height in
                // the design tool).
                val top = designChildData.style.top.resolve(selfHeight, density)
                val left = designChildData.style.left.resolve(selfWidth, density)
                val bottom = designChildData.style.bottom.resolve(selfHeight, density)
                val right = designChildData.style.right.resolve(selfWidth, density)
                var childWidth = designChildData.style.width.resolve(selfWidth, density)
                var childHeight = designChildData.style.height.resolve(selfHeight, density)

                if (left != null && right != null && childWidth == null) {
                    childWidth = (selfWidth - right) - left
                }
                if (top != null && bottom != null && childHeight == null) {
                    childHeight = (selfHeight - bottom) - top
                }

                // If we have a specified width/height then don't bother with min/max constraints.
                val minChildWidth =
                    childWidth ?: (designChildData.style.min_width.resolve(selfWidth, density) ?: 0)
                val minChildHeight =
                    childHeight
                        ?: (designChildData.style.min_height.resolve(selfHeight, density) ?: 0)
                val maxChildWidth =
                    childWidth
                        ?: (designChildData.style.max_width.resolve(selfWidth, density)
                            ?: selfWidth)
                val maxChildHeight =
                    childHeight
                        ?: (designChildData.style.max_height.resolve(selfHeight, density)
                            ?: selfHeight)
                val placeable =
                    measurable.measure(
                        Constraints(
                            minWidth = minChildWidth,
                            // Avoid asserting on impossible constraints; in general we shouldn't
                            // encounter this
                            // situation.
                            maxWidth = maxOf(maxChildWidth, minChildWidth),
                            minHeight = minChildHeight,
                            maxHeight = maxOf(maxChildHeight, minChildHeight),
                        )
                    )

                val leftMargin = designChildData.style.margin.start.resolve(selfWidth, density) ?: 0
                val topMargin = designChildData.style.margin.top.resolve(selfHeight, density) ?: 0

                val bounds = Rect()
                bounds.left = leftMargin
                bounds.top = topMargin
                bounds.right = leftMargin + placeable.measuredWidth
                bounds.bottom = topMargin + placeable.measuredHeight
                if (designChildData.style.position_type is PositionType.Absolute) {
                    if (right != null) {
                        bounds.right = selfWidth - right
                        bounds.left = bounds.right - placeable.measuredWidth
                    }
                    if (bottom != null) {
                        bounds.bottom = selfHeight - bottom
                        bounds.top = bounds.bottom - placeable.measuredHeight
                    }
                    if (left != null) {
                        bounds.left = left + leftMargin
                        bounds.right = bounds.left + placeable.measuredWidth
                    }
                    if (top != null) {
                        bounds.top = top + topMargin
                        bounds.bottom = bounds.top + placeable.measuredHeight
                    }
                }
                placeables[index] = placeable
                childBounds[index] = bounds
            } else {
                val placeable = measurable.measure(childConstraints)
                val bounds = Rect(0, 0, placeable.measuredWidth, placeable.measuredHeight)

                placeables[index] = placeable
                childBounds[index] = bounds
            }
        }

        // In order to work nicely with the scrollable modifiers, we report our width and height as
        // the extents of our children; that way the scrollable layout modifier knows the extents to
        // scroll to.
        var horizontalExtent = 0
        var verticalExtent = 0

        placeables.forEachIndexed { index, placeable ->
            placeable!!
            val bounds = childBounds[index]!!
            val designChildData = measurables[index].designChildData
            // Use the child style if we have it; absolutely positioned elements get
            // placed as requested and all others go into the flow.
            if (
                designChildData != null &&
                    designChildData.style.position_type is PositionType.Absolute
            ) {
                val undoCenteringOffsetX = -(placeable.width - placeable.measuredWidth) / 2
                val undoCenteringOffsetY = -(placeable.height - placeable.measuredHeight) / 2

                bounds.offsetTo(
                    bounds.left + undoCenteringOffsetX,
                    bounds.top + undoCenteringOffsetY
                )
            } else {
                bounds.offsetTo(0, 0)
            }
            horizontalExtent = horizontalExtent.coerceAtLeast(bounds.right)
            verticalExtent = verticalExtent.coerceAtLeast(bounds.bottom)
        }

        if (horizontalExtent > selfWidth && constraints.maxWidth == Constraints.Infinity)
            selfWidth = horizontalExtent
        if (verticalExtent > selfHeight && constraints.maxHeight == Constraints.Infinity)
            selfHeight = verticalExtent

        layout(selfWidth, selfHeight) {
            placeables.forEachIndexed { index, placeable ->
                val bounds = childBounds[index]!!
                placeable!!.place(bounds.left, bounds.top)
            }
        }
    }

private class LayoutDeltaCanvas :
    android.graphics.Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)) {
    var maxHeight = 0
    var maxAscent = 0

    override fun drawTextRun(
        text: CharSequence,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        isRtl: Boolean,
        paint: Paint
    ) {

        val fontMetricsInt = paint.fontMetricsInt
        val height = fontMetricsInt.descent - fontMetricsInt.ascent
        val ascent = -fontMetricsInt.ascent

        maxHeight = maxHeight.coerceAtLeast(height)
        maxAscent = maxAscent.coerceAtLeast(ascent)
    }
}

/// TextLayoutData is used so that a parent can perform a height-for-width calculation on
internal class TextLayoutData(
    val annotatedString: AnnotatedString,
    val textStyle: androidx.compose.ui.text.TextStyle,
    val resourceLoader: Font.ResourceLoader
)

internal fun TextLayoutData.boundsForWidth(
    inWidth: Int,
    maxLines: Int,
    density: Density
): Pair<Rect, Int> {
    // Create a text layout so we can figure out the size we need to allocate for the
    // text box. It will be taller than the Figma box because Compose measures text
    // quite differently.
    val firstLineTextLayout =
        Paragraph(
            text = annotatedString.text,
            style = textStyle,
            spanStyles = annotatedString.spanStyles,
            width = inWidth + 5.0f,
            density = density,
            resourceLoader = resourceLoader,
            maxLines = 1
        )
    val textLayout =
        Paragraph(
            text = annotatedString.text,
            style = textStyle,
            spanStyles = annotatedString.spanStyles,
            width = inWidth + 5.0f,
            density = density,
            resourceLoader = resourceLoader,
            maxLines = maxLines
        )

    // The design tool allocates height for text based on the "ascent" and "descent"
    // glyph values. Compose uses "top" and "bottom", which are bigger and provide inclusive
    // bounds. We make it work by determining where the baseline would be in the design
    // tool, and then offsetting the text in Compose to match up.
    //
    // We use this canvas to get the glyph metrics of the largest glyph in the first line.
    val measureCanvas = LayoutDeltaCanvas()
    firstLineTextLayout.paint(Canvas(measureCanvas))
    var designLineHeight = measureCanvas.maxHeight
    var designBaseline = measureCanvas.maxAscent

    // If there's an explicit line height, then figure out where we lie within it
    if (textStyle.lineHeight.isSp) {
        val lineHeightPx = textStyle.lineHeight.value * density.density

        // center within measured height
        val dy = (lineHeightPx - designLineHeight) / 2
        designBaseline += dy.toInt()
        designLineHeight = lineHeightPx.toInt()
    }

    val baselineDelta = (firstLineTextLayout.firstBaseline - designBaseline).toInt()

    // Sometimes the line width is much less than the layout width; we want to
    // have the tightest bounds possible that preserve the line breaking that
    // Figma produces. So we take the max line width, and then the minimum of
    // that against the layout width.
    //
    // The interaction test doc has several text elements that reproduce bad
    // behavior here.
    var maxLineWidth = 0.0f
    for (i in 0 until textLayout.lineCount) {
        maxLineWidth = textLayout.getLineWidth(i).coerceAtLeast(maxLineWidth)
    }
    val width = ceil(maxLineWidth.coerceAtMost(textLayout.width)).toInt()
    val height = ceil(textLayout.height).toInt()

    // Now we can place the text, giving it exactly the constraints it needs to
    // lay out, and offsetting in y by the computed layout delta.
    return Pair(
        Rect(0, -baselineDelta, width, height - baselineDelta),
        designLineHeight * textLayout.lineCount
    )
}

@Composable
internal inline fun DesignTextLayout(
    modifier: Modifier = Modifier,
    style: ViewStyle,
    textLayoutData: TextLayoutData,
    name: String = "unnamed",
    content: @Composable () -> Unit
) {
    val measurePolicy = rememberDesignTextMeasurePolicy(name, textLayoutData, style)
    Layout(content = content, measurePolicy = measurePolicy, modifier = modifier)
}

@Composable
internal fun rememberDesignTextMeasurePolicy(
    name: String,
    textLayoutData: TextLayoutData,
    style: ViewStyle
) = remember(name, textLayoutData, style) { designTextMeasurePolicy(name, textLayoutData, style) }

/**
 * Compose measures text differently to Figma. Compose allocates space from the glyph "top" to
 * "bottom", where Figma ignores the "top" and "bottom" and just uses the "ascent" and "descent"
 * which define the vertical bounds for most latin glyphs (but glyphs are allowed to draw outside of
 * the bounds defined by Figma using the ascent and descent).
 *
 * Because text from Figma can be embedded anywhere (e.g: it could be the top level of a component
 * and get laid out by Compose, or it could be nested in an Auto Layout, or positioned absolutely)
 * we want to have a Composable that matches the Figma measurements, and appropriately offsets the
 * text it contains.
 *
 * If we make a text view with tight bounds (to match the Figma bounds) then text gets clipped. We
 * could adjust that with a transform, but that might break accessibility and selection rect
 * rendering.
 *
 * So instead we make a parent container that takes the smaller text size, and offsets its text
 * child to be in the correct vertical location.
 */
internal fun designTextMeasurePolicy(
    name: String,
    textLayoutData: TextLayoutData,
    style: ViewStyle
) = MeasurePolicy { measurables, constraints ->
    var selfWidth = constraints.minWidth
    var selfHeight = constraints.minHeight
    var hasWidth = false
    var hasHeight = false
    var minWidth = constraints.minWidth
    var minHeight = constraints.minHeight

    // If we're absolutely positioned then we can extract our bounds directly from our style.
    when (style.position_type) {
        is PositionType.Absolute -> {
            val absBounds = absoluteLayout(style, constraints, density)
            selfWidth = absBounds.width()
            selfHeight = absBounds.height()
            if (style.width !is Dimension.Undefined && selfWidth > 0) hasWidth = true
            if (style.height !is Dimension.Undefined && selfHeight > 0) hasHeight = true
        }
        is PositionType.Relative -> {
            val relBounds = relativeLayout(style, constraints, density)
            if (style.min_width !is Dimension.Undefined && relBounds.width() > minWidth) {
                minWidth = relBounds.width()
            }
            if (style.width !is Dimension.Undefined) {
                selfWidth = relBounds.width()
                hasWidth = true
            }
            if (style.min_height !is Dimension.Undefined && relBounds.height() > minHeight) {
                minHeight = relBounds.height()
            }
            if (style.height !is Dimension.Undefined) {
                selfHeight = relBounds.height()
                hasHeight = true
            }
        }
    }

    // If we don't have a width in style, then we can measure within the width given to us
    // in our constraints.
    val measureWidth =
        if (hasWidth) {
            selfWidth
        } else {
            constraints.maxWidth
        }

    // Measure our text and figure out the offset to match baselines, and the intrinsic
    // height of the layout.
    val maxLines = if (style.line_count.isPresent) style.line_count.get().toInt() else Int.MAX_VALUE
    val (textBounds, textHeight) = textLayoutData.boundsForWidth(measureWidth, maxLines, this)

    // If the style doesn't define a fixed width or height, then use the bounds we just
    // measured. We use the height from our own measurement, which matches the height that
    // the design tool would have assigned to text (rather than the more generous height
    // that Compose would assign).
    if (!hasHeight) selfHeight = textHeight
    if (!hasWidth && textBounds.width() > selfWidth) selfWidth = textBounds.width()

    // Restrict to the parent constraints; we do not honor max constraints; if we exceed them
    // then it's because we are required to by the specified layout.
    if (minWidth > selfWidth) selfWidth = minWidth
    if (minHeight > selfHeight) selfHeight = minHeight

    // If our allocated width is wider than our measured width, then give the extra width
    // when we place our text child. This allows it to implement horizontal alignment.
    val placeWidth =
        if (selfWidth > textBounds.width()) {
            selfWidth
        } else {
            textBounds.width()
        }

    // Allocate the Compose-derived space to our text element child.
    val placeables = arrayOfNulls<Placeable>(measurables.size)
    measurables.forEachIndexed { index, measurable ->
        placeables[index] = measurable.measure(Constraints.fixed(placeWidth, textBounds.height()))
    }

    // Perform vertical alignment
    val verticalAlignmentOffset =
        when (style.text_align_vertical) {
            is TextAlignVertical.Center -> (selfHeight - textHeight) / 2
            is TextAlignVertical.Bottom -> (selfHeight - textHeight)
            else -> 0
        }

    // Ensure we don't overfill, causing crazy alignment.
    selfWidth = selfWidth.coerceAtMost(constraints.maxWidth)

    // We take the design tool allocated space for ourselves, so that our parent view
    // gets the correct amount of space subtracted from its layout.
    layout(selfWidth, selfHeight) {
        placeables.forEach { placeable ->
            placeable!!

            val undoCenteringOffsetX = -(placeable.width - placeable.measuredWidth) / 2
            val undoCenteringOffsetY = -(placeable.height - placeable.measuredHeight) / 2

            placeable.place(
                textBounds.left + undoCenteringOffsetX,
                textBounds.top + undoCenteringOffsetY + verticalAlignmentOffset
            )
        }
    }
}

// Surface the layout data to our parent container.
private class DesignChildData(val style: ViewStyle, val name: String) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@DesignChildData

    override fun hashCode(): Int = style.hashCode()

    override fun toString(): String = "DesignChildData($name, $style)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? DesignChildData ?: return false

        return style == otherModifier.style
    }
}

private val Measurable.designChildData: DesignChildData?
    get() = parentData as? DesignChildData

internal fun Modifier.layoutStyle(name: String, style: ViewStyle) =
    this.then(DesignChildData(style, name))

internal enum class LayoutType {
    Compose,
    Row,
    Column
}

internal class ParentLayoutInfo(
    val type: LayoutType,
    val isRoot: Boolean,
    val weight: (weight: Float) -> Modifier
)

internal val rootParentLayoutInfo = ParentLayoutInfo(LayoutType.Compose, true) { Modifier }
internal val absoluteParentLayoutInfo = ParentLayoutInfo(LayoutType.Compose, false) { Modifier }
internal val flowRowParentLayoutInfo = ParentLayoutInfo(LayoutType.Row, false) { Modifier }
internal val flowColumnParentLayoutInfo = ParentLayoutInfo(LayoutType.Column, false) { Modifier }

internal open class SimplifiedLayoutInfo(val selfModifier: Modifier, val marginModifier: Modifier)

internal class LayoutInfoAbsolute(selfModifier: Modifier, marginModifier: Modifier) :
    SimplifiedLayoutInfo(selfModifier, marginModifier)

internal class LayoutInfoRow(
    val arrangement: Arrangement.Horizontal,
    val alignment: Alignment.Vertical,
    selfModifier: Modifier,
    val childModifier: Modifier,
    marginModifier: Modifier,
    val scrollingEnabled: Boolean,
) : SimplifiedLayoutInfo(selfModifier, marginModifier)

internal class LayoutInfoColumn(
    val arrangement: Arrangement.Vertical,
    val alignment: Alignment.Horizontal,
    selfModifier: Modifier,
    val childModifier: Modifier,
    marginModifier: Modifier,
    val scrollingEnabled: Boolean,
) : SimplifiedLayoutInfo(selfModifier, marginModifier)

internal class LayoutInfoGrid(
    val layout: GridLayoutType,
    val minColumnRowSize: Int,
    val mainAxisSpacing: ItemSpacing,
    val crossAxisSpacing: Int,
    val numColumnsRows: Int,
    val gridSpanContent: List<GridSpan>,
    selfModifier: Modifier,
    val childModifier: Modifier,
    marginModifier: Modifier,
    val scrollingEnabled: Boolean,
    val padding: com.android.designcompose.serdegen.Rect,
) : SimplifiedLayoutInfo(selfModifier, marginModifier)

internal fun itemSpacingAbs(itemSpacing: ItemSpacing): Int {
    return when (itemSpacing) {
        is ItemSpacing.Fixed -> itemSpacing.value
        is ItemSpacing.Auto -> itemSpacing.field0
        else -> 0
    }
}

// Layout utils to help use Compose layout primitives for AutoLayout frames
internal fun calcLayoutInfo(
    modifier: Modifier,
    view: View,
    style: ViewStyle,
    parentInfo: ParentLayoutInfo
): SimplifiedLayoutInfo {
    val viewData = view.data
    val name = view.name

    var isAbsolute = false

    if (viewData != null && viewData is ViewData.Rect) {
        for (child in viewData.children) {
            if (child.style.position_type is PositionType.Absolute) isAbsolute = true
        }
    }
    val isRow = style.flex_direction is FlexDirection.Row

    // First populate the common layout values into the modifier. These are used by either a Compose
    // layout parent, or by an absolute layout container.
    var selfModifier = modifier
    if (style.position_type is PositionType.Absolute)
        selfModifier = selfModifier.then(DesignChildData(style, name))

    // Apply our "align self" value to control our size in the cross space.
    //    if (style.align_self is AlignSelf.Stretch) {
    //        if (parentInfo.type == LayoutType.Row) {
    //            selfModifier = selfModifier.fillMaxHeight()
    //        }
    //        else if (parentInfo.type == LayoutType.Column)
    //            selfModifier = selfModifier.fillMaxWidth()
    //    }
    //
    // We can't implement "align-self: stretch" using Compose's Row and Column containers. Those
    // containers provide the parent constraints for the cross axis, so we end up growing elements
    // much more than desired (so instead of an element filling its parent, it actually causes its
    // parent to fill the root dimensions -- this happens in the Day Mode Standalone media document
    // "Search Overlay" popup.
    //
    // So instead we just leave "align-self: stretch" unimplemented for now. In the future, perhaps
    // we could extend Row/Column to use their children's intrinsic cross size and provide that as a
    // reasonable "stretch" cross size.

    // Apply any margin L T R B
    //
    // We only use top OR left within AutoLayout, and Absolute Layout applies it itself.
    var marginModifier = Modifier as Modifier
    if (style.margin.start is Dimension.Points)
        marginModifier =
            marginModifier.padding(
                (style.margin.start as Dimension.Points).value.dp,
                0.dp,
                0.dp,
                0.dp
            )
    if (style.margin.top is Dimension.Points)
        marginModifier =
            marginModifier.padding(
                0.dp,
                (style.margin.top as Dimension.Points).value.dp,
                0.dp,
                0.dp
            )

    // If we flex, then we need to add a weight modifier
    if (style.flex_grow > 0.0)
        marginModifier = marginModifier.then(parentInfo.weight(style.flex_grow))
    else if (parentInfo.type != LayoutType.Compose && !isAbsolute) {
        marginModifier =
            if (isRow) marginModifier.wrapContentHeight(unbounded = true)
            else marginModifier.wrapContentWidth(unbounded = true)
    }
    selfModifier = selfModifier.then(marginModifier)

    // If we have a given width and height then use them.
    if (style.width is Dimension.Points)
        selfModifier = selfModifier.width((style.width as Dimension.Points).value.dp)
    else if (style.min_width is Dimension.Points)
        selfModifier = selfModifier.widthIn(min = (style.min_width as Dimension.Points).value.dp)
    if (style.height is Dimension.Points)
        selfModifier = selfModifier.height((style.height as Dimension.Points).value.dp)
    else if (style.min_height is Dimension.Points)
        selfModifier = selfModifier.heightIn(min = (style.min_height as Dimension.Points).value.dp)

    // If our parent is a Compose view, then we probably don't have "weight" implementation.
    // We assume that it's a row, and map our align_self and flex properties appropriately.
    if (parentInfo.type == LayoutType.Compose) {
        if (style.flex_grow > 0.0) {
            selfModifier = selfModifier.fillMaxWidth()
        } else {
            // If this is a root element and it doesn't fill, then we need to enforce a width
            // constraint on it, otherwise Compose makes it fill.
            if (parentInfo.isRoot && style.min_width is Dimension.Points)
                selfModifier = selfModifier.width((style.min_width as Dimension.Points).value.dp)
        }
        // We use fillMaxHeight on root containers, because it's OK for them to occupy all of the
        // parent space.
        if (style.align_self is AlignSelf.Stretch) {
            selfModifier = selfModifier.fillMaxHeight()
        } else {
            if (parentInfo.isRoot && style.min_height is Dimension.Points)
                selfModifier = selfModifier.height((style.min_height as Dimension.Points).value.dp)
        }
    }

    // Absolute layout scheme applies padding itself. For rows and columns, we use a modifier
    // to apply padding.
    val childModifier =
        Modifier.padding(
            style.padding.start.pointsAsDp(),
            style.padding.top.pointsAsDp(),
            style.padding.end.pointsAsDp(),
            style.padding.bottom.pointsAsDp()
        )

    if (style.grid_layout.isPresent) {
        val gridLayout = style.grid_layout.get()
        val isColumnLayout =
            gridLayout is GridLayoutType.FixedColumns || gridLayout is GridLayoutType.AutoColumns
        val scrollingEnabled =
            when (view.scroll_info.overflow) {
                is OverflowDirection.VERTICAL_SCROLLING -> isColumnLayout
                is OverflowDirection.HORIZONTAL_SCROLLING -> !isColumnLayout
                is OverflowDirection.HORIZONTAL_AND_VERTICAL_SCROLLING -> true
                else -> false
            }
        return LayoutInfoGrid(
            layout = style.grid_layout.get(),
            minColumnRowSize = style.grid_adaptive_min_size,
            mainAxisSpacing = style.item_spacing,
            crossAxisSpacing = style.cross_axis_item_spacing.toInt(),
            // TODO support these other alignments?
            /*
            mainAxisAlignment =
            when (style.justify_content) {
              is JustifyContent.FlexStart -> MainAxisAlignment.Start
              is JustifyContent.Center -> MainAxisAlignment.Center
              is JustifyContent.FlexEnd -> MainAxisAlignment.End
              is JustifyContent.SpaceAround -> MainAxisAlignment.SpaceAround
              is JustifyContent.SpaceBetween -> MainAxisAlignment.SpaceBetween
              is JustifyContent.SpaceEvenly -> MainAxisAlignment.SpaceEvenly
              else -> MainAxisAlignment.Start
            },
            crossAxisAlignment =
            when (style.align_items) {
              is AlignItems.FlexStart -> FlowCrossAxisAlignment.Start
              is AlignItems.Center -> FlowCrossAxisAlignment.Center
              is AlignItems.FlexEnd -> FlowCrossAxisAlignment.End
              else -> FlowCrossAxisAlignment.Start
            },
            */
            numColumnsRows = style.grid_columns_rows,
            gridSpanContent = style.grid_span_content,
            selfModifier = selfModifier,
            childModifier = childModifier,
            marginModifier = marginModifier,
            scrollingEnabled = scrollingEnabled,
            padding = style.padding,
        )
    }

    if (isAbsolute || style.flex_direction is FlexDirection.None) {
        // Apply our "align self" value to control our size in the cross space.
        if (style.align_self is AlignSelf.Stretch) {
            if (parentInfo.type == LayoutType.Row) selfModifier = selfModifier.fillMaxHeight()
            else if (parentInfo.type == LayoutType.Column)
                selfModifier = selfModifier.fillMaxWidth()
        }
        return LayoutInfoAbsolute(selfModifier, marginModifier)
    }

    val itemSpacing = itemSpacingAbs(style.item_spacing)
    if (isRow) {
        return LayoutInfoRow(
            arrangement =
                when (style.justify_content) {
                    is JustifyContent.FlexStart ->
                        if (itemSpacing != 0) Arrangement.spacedBy(itemSpacing.dp)
                        else Arrangement.Start
                    is JustifyContent.Center ->
                        if (itemSpacing != 0)
                            Arrangement.spacedBy(itemSpacing.dp, Alignment.CenterHorizontally)
                        else Arrangement.Center
                    is JustifyContent.FlexEnd ->
                        if (itemSpacing != 0) Arrangement.spacedBy(itemSpacing.dp, Alignment.End)
                        else Arrangement.End
                    is JustifyContent.SpaceAround -> Arrangement.SpaceAround
                    is JustifyContent.SpaceBetween -> Arrangement.SpaceBetween
                    is JustifyContent.SpaceEvenly -> Arrangement.SpaceEvenly
                    else -> Arrangement.Start
                },
            alignment =
                when (style.align_items) {
                    is AlignItems.FlexStart -> Alignment.Top
                    is AlignItems.Center -> Alignment.CenterVertically
                    is AlignItems.FlexEnd -> Alignment.Bottom
                    else -> Alignment.Top
                },
            selfModifier = selfModifier,
            childModifier = childModifier,
            marginModifier = marginModifier,
            scrollingEnabled =
                when (view.scroll_info.overflow) {
                    is OverflowDirection.HORIZONTAL_SCROLLING -> true
                    is OverflowDirection.HORIZONTAL_AND_VERTICAL_SCROLLING -> true
                    else -> false
                },
        )
    }
    return LayoutInfoColumn(
        arrangement =
            when (style.justify_content) {
                is JustifyContent.FlexStart ->
                    if (itemSpacing != 0) Arrangement.spacedBy(itemSpacing.dp) else Arrangement.Top
                is JustifyContent.Center ->
                    if (itemSpacing != 0)
                        Arrangement.spacedBy(itemSpacing.dp, Alignment.CenterVertically)
                    else Arrangement.Center
                is JustifyContent.FlexEnd ->
                    if (itemSpacing != 0) Arrangement.spacedBy(itemSpacing.dp, Alignment.Bottom)
                    else Arrangement.Bottom
                is JustifyContent.SpaceAround -> Arrangement.SpaceAround
                is JustifyContent.SpaceBetween -> Arrangement.SpaceBetween
                is JustifyContent.SpaceEvenly -> Arrangement.SpaceEvenly
                else -> Arrangement.Top
            },
        alignment =
            when (style.align_items) {
                is AlignItems.FlexStart -> Alignment.Start
                is AlignItems.Center -> Alignment.CenterHorizontally
                is AlignItems.FlexEnd -> Alignment.End
                else -> Alignment.End
            },
        selfModifier = selfModifier,
        childModifier = childModifier,
        marginModifier = marginModifier,
        scrollingEnabled =
            when (view.scroll_info.overflow) {
                is OverflowDirection.VERTICAL_SCROLLING -> true
                is OverflowDirection.HORIZONTAL_AND_VERTICAL_SCROLLING -> true
                else -> false
            },
    )
}
