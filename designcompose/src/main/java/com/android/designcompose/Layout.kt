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
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.android.designcompose.serdegen.AlignItems
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.GridLayoutType
import com.android.designcompose.serdegen.GridSpan
import com.android.designcompose.serdegen.ItemSpacing
import com.android.designcompose.serdegen.JustifyContent
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.LayoutChangedResponse
import com.android.designcompose.serdegen.LayoutNode
import com.android.designcompose.serdegen.LayoutNodeList
import com.android.designcompose.serdegen.OverflowDirection
import com.android.designcompose.serdegen.Size
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewStyle
import com.novi.bincode.BincodeDeserializer
import com.novi.bincode.BincodeSerializer
import java.util.Optional
import kotlin.math.ceil
import kotlin.math.roundToInt

/// TextLayoutData is used so that a parent can perform a height-for-width calculation on
internal data class TextLayoutData(
    val annotatedString: AnnotatedString,
    val textStyle: androidx.compose.ui.text.TextStyle,
    val resourceLoader: Font.ResourceLoader,
    val textBoxSize: Size,
)

internal data class TextMeasureData(
    val textLayout: TextLayoutData,
    val density: Density,
    val maxLines: Int,
    val styleWidth: Float,
)

internal object LayoutManager {
    private val subscribers: HashMap<Int, (Int) -> Unit> = HashMap()
    private val layoutsInProgress: HashSet<Int> = HashSet()
    private var textMeasures: HashMap<Int, TextMeasureData> = HashMap()
    private var nextLayoutId: Int = 0
    private var performLayoutComputation: Boolean = false
    private var density: Float = 1F
    private var layoutNodes: ArrayList<LayoutNode> = arrayListOf()
    private var layoutCache: HashMap<Int, Layout> = HashMap()

    internal fun getNextLayoutId(): Int {
        return ++nextLayoutId
    }

    internal fun deferComputations() {
        // Defer layout computations when removing views. This is an optimization to prevent layout
        // from being calculated after every view unsubscribe. Currently we only defer computations
        // when performing an navigation interaction that changes the root view.
        Log.d(TAG, "deferComputations")
        performLayoutComputation = false
    }

    private fun resumeComputations() {
        // Resume layout computations. This happens as soon as we start adding (subscribing) views.
        performLayoutComputation = true
    }

    internal fun setDensity(pixelDensity: Float) {
        Log.i(TAG, "setDensity $pixelDensity")
        density = pixelDensity
    }

    internal fun getDensity(): Float {
        return density
    }

    internal fun subscribeFrame(
        layoutId: Int,
        setLayoutState: (Int) -> Unit,
        parentLayoutId: Int,
        childIndex: Int,
        style: ViewStyle,
        name: String,
    ) {
        // Frames can have children so call beginLayout() to optimize layout computation until all
        // children have been added.
        beginLayout(layoutId)
        subscribe(layoutId, setLayoutState, parentLayoutId, childIndex, style, name, false)
    }

    internal fun subscribeText(
        layoutId: Int,
        setLayoutState: (Int) -> Unit,
        parentLayoutId: Int,
        rootLayoutId: Int,
        childIndex: Int,
        style: ViewStyle,
        name: String,
        fixedWidth: Int,
        fixedHeight: Int,
    ) {
        val adjustedWidth = (fixedWidth.toFloat() / density).roundToInt()
        val adjustedHeight = (fixedHeight.toFloat() / density).roundToInt()
        subscribe(
            layoutId,
            setLayoutState,
            parentLayoutId,
            childIndex,
            style,
            name,
            false,
            Optional.ofNullable(adjustedWidth),
            Optional.ofNullable(adjustedHeight)
        )

        // Text cannot have children, so call computeLayoutIfComplete() here so that if this is the
        // text or text style changed when no other nodes changed, we recompute layout
        computeLayoutIfComplete(layoutId, rootLayoutId)
    }

    internal fun subscribeWithMeasure(
        layoutId: Int,
        setLayoutState: (Int) -> Unit,
        parentLayoutId: Int,
        rootLayoutId: Int,
        childIndex: Int,
        style: ViewStyle,
        name: String,
        textMeasureData: TextMeasureData,
    ) {
        textMeasures[layoutId] = textMeasureData
        subscribe(layoutId, setLayoutState, parentLayoutId, childIndex, style, name, true)

        // Text cannot have children, so call computeLayoutIfComplete() here so that if this is the
        // text or text style changed when no other nodes changed, we recompute layout
        computeLayoutIfComplete(layoutId, rootLayoutId)
    }

    private fun subscribe(
        layoutId: Int,
        setLayoutState: (Int) -> Unit,
        parentLayoutId: Int,
        childIndex: Int,
        style: ViewStyle,
        name: String,
        useMeasureFunc: Boolean,
        fixedWidth: Optional<Int> = Optional.empty(),
        fixedHeight: Optional<Int> = Optional.empty(),
    ) {
        subscribers[layoutId] = setLayoutState

        // Add the node to a list of nodes
        layoutNodes.add(
            LayoutNode(
                layoutId,
                parentLayoutId,
                childIndex,
                style,
                name,
                useMeasureFunc,
                fixedWidth,
                fixedHeight,
            )
        )
    }

    internal fun unsubscribe(layoutId: Int) {
        subscribers.remove(layoutId)
        textMeasures.remove(layoutId)
        layoutCache.remove(layoutId)
        val responseBytes = Jni.jniRemoveNode(layoutId, performLayoutComputation)
        handleResponse(responseBytes)

        // TODO for children of a lazy grid view, don't recompute layout when removing views since
        // they are completely gone from the grid view. Need to be able to detect, when removing the
        // view, if it is an ancestor of the child of a lazy grid view.
    }

    private fun beginLayout(layoutId: Int) {
        // Add a layout ID to a set of IDs that are in progress. As a view recursively calls its
        // children, this set grows. Each time a view has finished calling its children it calls
        // finishLayout().
        layoutsInProgress.add(layoutId)
        resumeComputations()
    }

    internal fun finishLayout(layoutId: Int, rootLayoutId: Int) {
        // Remove a layout ID from the set of IDs that are in progress.
        layoutsInProgress.remove(layoutId)
        computeLayoutIfComplete(layoutId, rootLayoutId)
    }

    // Check if we are ready to compute layout. If layoutId is the same as rootLayoutId, then a root
    // node has completed adding all of its children and we can compute layout on this root node. If
    // layoutsInProgress is empty, this could also be true -- or, a node that is the child of a
    // widget has completed adding all of its children and we can compute layout on the widget
    // child.
    private fun computeLayoutIfComplete(layoutId: Int, rootLayoutId: Int) {
        if (layoutsInProgress.isEmpty() || layoutId == rootLayoutId) {
            val layoutNodeList = LayoutNodeList(layoutNodes)
            val nodeListSerializer = BincodeSerializer()
            layoutNodeList.serialize(nodeListSerializer)
            val serializedNodeList = nodeListSerializer._bytes.toUByteArray().asByteArray()
            val responseBytes = Jni.jniAddNodes(rootLayoutId, serializedNodeList)
            handleResponse(responseBytes)
            layoutNodes.clear()
        }
    }

    private fun handleResponse(responseBytes: ByteArray?) {
        if (responseBytes != null) {
            val deserializer = BincodeDeserializer(responseBytes)
            val response: LayoutChangedResponse = LayoutChangedResponse.deserialize(deserializer)

            // Add all the layouts to our cache
            response.changed_layouts.forEach { (layoutId, layout) ->
                layoutCache[layoutId] = layout
            }
            notifySubscribers(response.changed_layouts.keys, response.layout_state)
            if (response.changed_layouts.isNotEmpty())
                Log.d(
                    TAG,
                    "HandleResponse ${response.layout_state}, changed: ${response.changed_layouts.keys}"
                )
        } else {
            Log.d(TAG, "HandleResponse NULL")
        }
    }

    internal fun getTextMeasureData(layoutId: Int): TextMeasureData? {
        return textMeasures[layoutId]
    }

    // Ask for the layout for the associated node via JNI
    internal fun getLayoutWithDensity(layoutId: Int): Layout? {
        return getLayout(layoutId)?.withDensity(density)
    }

    // Ask for the layout for the associated node via JNI
    internal fun getLayout(layoutId: Int): Layout? {
        return layoutCache[layoutId]
    }

    // Tell the Rust layout manager that a node size has changed. In the returned response, get all
    // the nodes that have changed and notify subscribers of this change.
    internal fun setNodeSize(layoutId: Int, rootLayoutId: Int, width: Int, height: Int) {
        val adjustedWidth = (width.toFloat() / density).roundToInt()
        val adjustedHeight = (height.toFloat() / density).roundToInt()
        val responseBytes =
            Jni.jniSetNodeSize(layoutId, rootLayoutId, adjustedWidth, adjustedHeight)
        handleResponse(responseBytes)
    }

    // For every node in nodes, inform the subscribers of the new layout ID
    private fun notifySubscribers(nodes: Set<Int>, layoutState: Int) {
        for (layoutId in nodes) {
            val updateLayout = subscribers[layoutId]
            updateLayout?.let { it.invoke(layoutState) }
        }
    }

    internal fun notifyAllSubscribers(layoutState: Int) {
        subscribers.values.forEach { it(layoutState) }
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
            width = inWidth.toFloat(),
            density = density,
            resourceLoader = resourceLoader,
            maxLines = 1
        )
    val textLayout =
        Paragraph(
            text = annotatedString.text,
            style = textStyle,
            spanStyles = annotatedString.spanStyles,
            width = inWidth.toFloat(),
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

class ParentLayoutInfo(
    val parentLayoutId: Int = -1,
    val childIndex: Int = 0,
    val rootLayoutId: Int = -1,
    val isWidgetChild: Boolean = false,
)

internal fun ParentLayoutInfo.withRootIdIfNone(rootLayoutId: Int): ParentLayoutInfo {
    val rootLayoutId = if (this.rootLayoutId == -1) rootLayoutId else this.rootLayoutId
    return ParentLayoutInfo(
        this.parentLayoutId,
        this.childIndex,
        rootLayoutId,
        this.isWidgetChild,
    )
}

internal val rootParentLayoutInfo = ParentLayoutInfo()
val widgetParent = ParentLayoutInfo(isWidgetChild = true)

internal open class SimplifiedLayoutInfo(val selfModifier: Modifier) {
    internal fun shouldRender(): Boolean {
        // We only want to render if the layout is an absolute layout, meaning we are using our
        // custom Rust layout implementation. All other layout types mean we are using the list
        // widget, and any special visual styles applied in Figma should be on the widget's parent.
        return this is LayoutInfoAbsolute
    }
}

internal class LayoutInfoAbsolute(selfModifier: Modifier) : SimplifiedLayoutInfo(selfModifier)

internal class LayoutInfoRow(
    val arrangement: Arrangement.Horizontal,
    val alignment: Alignment.Vertical,
    selfModifier: Modifier,
    val marginModifier: Modifier,
    val padding: com.android.designcompose.serdegen.Rect,
) : SimplifiedLayoutInfo(selfModifier)

internal class LayoutInfoColumn(
    val arrangement: Arrangement.Vertical,
    val alignment: Alignment.Horizontal,
    selfModifier: Modifier,
    val marginModifier: Modifier,
    val padding: com.android.designcompose.serdegen.Rect,
) : SimplifiedLayoutInfo(selfModifier)

internal class LayoutInfoGrid(
    val layout: GridLayoutType,
    val minColumnRowSize: Int,
    val mainAxisSpacing: ItemSpacing,
    val crossAxisSpacing: Int,
    val numColumnsRows: Int,
    val gridSpanContent: List<GridSpan>,
    selfModifier: Modifier,
    val scrollingEnabled: Boolean,
    val padding: com.android.designcompose.serdegen.Rect,
) : SimplifiedLayoutInfo(selfModifier)

internal fun itemSpacingAbs(itemSpacing: ItemSpacing): Int {
    return when (itemSpacing) {
        is ItemSpacing.Fixed -> itemSpacing.value
        is ItemSpacing.Auto -> itemSpacing.field0
        else -> 0
    }
}

internal fun calcLayoutInfo(
    modifier: Modifier,
    view: View,
    style: ViewStyle,
): SimplifiedLayoutInfo {
    if (style.grid_layout.isPresent) {
        val gridLayout = style.grid_layout.get()
        val isHorizontalLayout = gridLayout is GridLayoutType.Horizontal
        val isVerticalLayout = gridLayout is GridLayoutType.Vertical
        val itemSpacing = itemSpacingAbs(style.item_spacing)
        val marginModifier =
            Modifier.padding(
                if (style.padding.start is Dimension.Points)
                    (style.padding.start as Dimension.Points).value.dp
                else 0.dp,
                if (style.padding.top is Dimension.Points)
                    (style.padding.top as Dimension.Points).value.dp
                else 0.dp,
                if (style.padding.end is Dimension.Points)
                    (style.padding.end as Dimension.Points).value.dp
                else 0.dp,
                if (style.padding.bottom is Dimension.Points)
                    (style.padding.bottom as Dimension.Points).value.dp
                else 0.dp,
            )
        if (isHorizontalLayout) {
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
                            if (itemSpacing != 0)
                                Arrangement.spacedBy(itemSpacing.dp, Alignment.End)
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
                selfModifier = modifier,
                marginModifier = marginModifier,
                padding = style.padding,
            )
        } else if (isVerticalLayout) {
            return LayoutInfoColumn(
                arrangement =
                    when (style.justify_content) {
                        is JustifyContent.FlexStart ->
                            if (itemSpacing != 0) Arrangement.spacedBy(itemSpacing.dp)
                            else Arrangement.Top
                        is JustifyContent.Center ->
                            if (itemSpacing != 0)
                                Arrangement.spacedBy(itemSpacing.dp, Alignment.CenterVertically)
                            else Arrangement.Center
                        is JustifyContent.FlexEnd ->
                            if (itemSpacing != 0)
                                Arrangement.spacedBy(itemSpacing.dp, Alignment.Bottom)
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
                selfModifier = modifier,
                marginModifier = marginModifier,
                padding = style.padding,
            )
        } else {
            val isColumnLayout =
                gridLayout is GridLayoutType.FixedColumns ||
                    gridLayout is GridLayoutType.AutoColumns
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
                selfModifier = modifier,
                scrollingEnabled = scrollingEnabled,
                padding = style.padding,
            )
        }
    } else {
        return LayoutInfoAbsolute(modifier)
    }
}

// Converts a layout from Rust into a width/height Modifier
internal fun Modifier.layoutSizeToModifier(layout: Layout?) =
    this.then(Modifier.width(layout?.width?.dp ?: 0.dp).height(layout?.height?.dp ?: 0.dp))

internal fun Modifier.sizeToModifier(width: Int, height: Int) =
    this.then(Modifier.width(width.dp).height(height.dp))

// Surface the layout data to our parent container.
internal class DesignLayoutData(val name: String, val layoutId: Int) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@DesignLayoutData

    override fun hashCode(): Int = layoutId // style.hashCode()

    override fun toString(): String = "DesignLayoutData($name, $layoutId)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? DesignLayoutData ?: return false
        return name == otherModifier.name && layoutId == otherModifier.layoutId
    }
}

internal val Placeable.designLayoutData: DesignLayoutData?
    get() = parentData as? DesignLayoutData

internal fun Modifier.layoutStyle(name: String, layoutId: Int) =
    this.then(DesignLayoutData(name, layoutId))

internal fun Layout.width() = this.width.roundToInt()

internal fun Layout.height() = this.height.roundToInt()

internal fun Layout.left() = this.left.roundToInt()

internal fun Layout.top() = this.top.roundToInt()

// Layout function for DesignFrame
@Composable
internal inline fun DesignFrameLayout(
    modifier: Modifier,
    name: String,
    layoutId: Int,
    layoutState: Int,
    content: @Composable () -> Unit
) {
    val measurePolicy = remember(layoutState) { designMeasurePolicy(name, layoutId) }
    Layout(content = content, measurePolicy = measurePolicy, modifier = modifier)
}

// Measure policy for DesignFrame.
internal fun designMeasurePolicy(name: String, layoutId: Int) =
    MeasurePolicy { measurables, constraints ->
        val placeables = measurables.map { measurable -> measurable.measure(constraints) }

        var myLayout = LayoutManager.getLayoutWithDensity(layoutId)
        if (myLayout == null) {
            Log.d(TAG, "designMeasurePolicy error: null layout $name layoutId $layoutId")
        }
        val myWidth = myLayout?.width() ?: 0
        val myHeight = myLayout?.height() ?: 0
        Log.d(TAG, "Layout $name $myWidth, $myHeight")
        layout(myWidth, myHeight) {
            // Place children in the parent layout
            placeables.forEachIndexed { index, placeable ->
                val layoutData = placeable.designLayoutData
                if (layoutData == null) {
                    // This should only be null for index == 0, this frame
                    val myX = myLayout?.left() ?: 0
                    val myY = myLayout?.top() ?: 0
                    Log.d(TAG, "Place $name index $index: $myX, $myY}")
                    placeable.place(myX, myY)
                } else {
                    val childLayout = LayoutManager.getLayoutWithDensity(layoutData.layoutId)
                    if (childLayout == null) {
                        Log.d(
                            TAG,
                            "Place error null layout: parent $name child $index layoutId $layoutId"
                        )
                    } else {
                        placeable.place(x = childLayout.left(), y = childLayout.top())
                    }
                }
            }
        }
    }

@Composable
internal inline fun DesignTextLayout(
    modifier: Modifier,
    name: String,
    layout: Layout?,
    layoutState: Int,
    renderHeight: Int?,
    renderTop: Int?,
    content: @Composable () -> Unit
) {
    val measurePolicy =
        remember(layoutState, layout, renderHeight, renderTop) {
            designTextMeasurePolicy(name, layout, renderHeight, renderTop)
        }
    Layout(content = content, measurePolicy = measurePolicy, modifier = modifier)
}

internal fun designTextMeasurePolicy(
    name: String,
    layout: Layout?,
    renderHeight: Int?,
    renderTop: Int?
) = MeasurePolicy { measurables, constraints ->
    val placeables = measurables.map { measurable -> measurable.measure(constraints) }

    val myWidth = layout?.width() ?: 0
    val myHeight = renderHeight ?: layout?.height() ?: 0
    val myX = 0
    val myY = renderTop ?: layout?.top() ?: 0
    Log.d(TAG, "LayoutText $name w $myWidth h $myHeight x $myX y $myY left ${layout?.left}")
    layout(myWidth, myHeight) {
        // Text has no children, so placeables is always just a list of 1 for this text, which
        // we place at the calculated offset.
        // There are two offsets that we need to consider. The offset used here myX, myY
        // take into account the text's vertical offset, but not layout offset from its parent.
        // The layout offset is used when this text node is placed by its parent.
        placeables.forEach { placeable -> placeable.place(myX, myY) }
    }
}
