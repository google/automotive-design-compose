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

import android.util.Log
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.tracing.trace
import com.android.designcompose.serdegen.AlignItems
import com.android.designcompose.serdegen.AlignSelf
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
import com.android.designcompose.serdegen.PositionType
import com.android.designcompose.serdegen.Rect
import com.android.designcompose.serdegen.Size
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewStyle
import com.novi.bincode.BincodeDeserializer
import com.novi.bincode.BincodeSerializer
import java.util.Optional
import kotlin.math.roundToInt

internal data class TextMeasureData(
    val textHash: Int,
    val paragraph: ParagraphIntrinsics,
    val density: Density,
    val maxLines: Int,
    val autoWidth: Boolean,
) {
    override fun hashCode(): Int {
        // Don't hash all of TextLayoutData because it's derived from style, which is
        // already hashed everywhere we use TextMeasureData's hashCode.
        var result = density.hashCode()
        result = 31 * result + textHash
        result = 31 * result + maxLines.hashCode()
        result = 31 * result + autoWidth.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextMeasureData

        if (density != other.density) return false
        if (textHash != other.textHash) return false
        if (maxLines != other.maxLines) return false
        if (autoWidth != other.autoWidth) return false

        return true
    }
}

internal object LayoutManager {
    private var managerId: Int = 0
    private val subscribers: HashMap<Int, (Int) -> Unit> = HashMap()
    private val layoutsInProgress: HashSet<Int> = HashSet()
    private var textMeasures: HashMap<Int, TextMeasureData> = HashMap()
    private var modifiedSizes: HashSet<Int> = HashSet()
    private var nextLayoutId: Int = 0
    private var performLayoutComputation: Boolean = false
    private var density: Float = 1F
    private var layoutNodes: ArrayList<LayoutNode> = arrayListOf()
    private var layoutCache: HashMap<Int, Layout> = HashMap()
    private var layoutStateCache: HashMap<Int, Int> = HashMap()

    init {
        managerId = Jni.jniCreateLayoutManager()
    }

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

        // Text cannot have children, so call computeLayoutIfComplete() here so that if this is
        // the
        // text or text style changed when no other nodes changed, we recompute layout
        computeLayoutIfComplete(layoutId, rootLayoutId)
    }

    internal fun squooshSetTextMeasureData(layoutId: Int, textMeasureData: TextMeasureData) {
        textMeasures[layoutId] = textMeasureData
    }

    internal fun squooshClearTextMeasureData(layoutId: Int) {
        textMeasures.remove(layoutId)
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

    internal fun unsubscribe(layoutId: Int, rootLayoutId: Int, isWidgetAncestor: Boolean) {
        subscribers.remove(layoutId)
        textMeasures.remove(layoutId)
        layoutCache.remove(layoutId)
        layoutStateCache.remove(layoutId)
        modifiedSizes.remove(layoutId)

        // Perform layout computation after removing the node only if performLayoutComputation is
        // true, or if we are not a widget ancestor. We don't want to compute layout when ancestors
        // of a widget are removed because this happens constantly in a lazy grid view.
        val computeLayout = performLayoutComputation && !isWidgetAncestor
        val responseBytes = Jni.jniRemoveNode(managerId, layoutId, rootLayoutId, computeLayout)
        handleResponse(responseBytes)
        if (computeLayout) Log.d(TAG, "Unsubscribe $layoutId, compute layout")
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
            trace(DCTraces.LAYOUTMANAGER_COMPUTELAYOUTIFCOMPLETE) {
                val layoutNodeList = LayoutNodeList(layoutNodes, arrayListOf())
                val nodeListSerializer = BincodeSerializer()
                layoutNodeList.serialize(nodeListSerializer)
                val serializedNodeList = nodeListSerializer._bytes.toUByteArray().asByteArray()
                val responseBytes = Jni.jniAddNodes(managerId, rootLayoutId, serializedNodeList)
                handleResponse(responseBytes)
                layoutNodes.clear()
            }
        }
    }

    private fun handleResponse(responseBytes: ByteArray?) {
        if (responseBytes != null) {
            val deserializer = BincodeDeserializer(responseBytes)
            val response: LayoutChangedResponse = LayoutChangedResponse.deserialize(deserializer)

            // Add all the layouts to our cache
            response.changed_layouts.forEach { (layoutId, layout) ->
                layoutCache[layoutId] = layout
                layoutStateCache[layoutId] = response.layout_state
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

    internal fun getTextMeasureData(layoutId: Int): TextMeasureData? = textMeasures[layoutId]

    // Ask for the layout for the associated node via JNI
    internal fun getLayoutWithDensity(layoutId: Int): Layout? =
        getLayout(layoutId)?.withDensity(density)

    // Ask for the layout for the associated node via JNI
    internal fun getLayout(layoutId: Int): Layout? = layoutCache[layoutId]

    internal fun getLayoutState(layoutId: Int): Int? = layoutStateCache[layoutId]

    // Tell the Rust layout manager that a node size has changed. In the returned response, get all
    // the nodes that have changed and notify subscribers of this change.
    internal fun setNodeSize(layoutId: Int, rootLayoutId: Int, width: Int, height: Int) {
        modifiedSizes.add(layoutId)
        val adjustedWidth = (width.toFloat() / density).roundToInt()
        val adjustedHeight = (height.toFloat() / density).roundToInt()
        val responseBytes =
            Jni.jniSetNodeSize(managerId, layoutId, rootLayoutId, adjustedWidth, adjustedHeight)
        handleResponse(responseBytes)
    }

    internal fun hasModifiedSize(layoutId: Int): Boolean = modifiedSizes.contains(layoutId)

    // For every node in nodes, inform the subscribers of the new layout ID
    private fun notifySubscribers(nodes: Set<Int>, layoutState: Int) {
        for (layoutId in nodes) {
            val updateLayout = subscribers[layoutId]
            updateLayout?.let { it.invoke(layoutState) }
        }
    }
}

// ExternalLayoutData holds layout properties of a node that affect its layout with respect to its
// parent. When doing component replacement, these properties are saved from the node being
// replaced so that the new node can use its values.
data class ExternalLayoutData(
    val margin: Rect,
    val top: Dimension,
    val left: Dimension,
    val bottom: Dimension,
    val right: Dimension,
    val width: Dimension,
    val height: Dimension,
    val minWidth: Dimension,
    val minHeight: Dimension,
    val maxWidth: Dimension,
    val maxHeight: Dimension,
    val nodeSize: Size,
    val boundingBox: Size,
    val flexGrow: Float,
    val flexBasis: Dimension,
    val alignSelf: AlignSelf,
    val positionType: PositionType,
    val transform: Optional<List<Float>>,
    val relativeTransform: Optional<List<Float>>,
)

// ParentLayoutInfo holds data necessary to perform layout. When a node subscribes to layout, it
// needs to know it's own layout ID as well as its parent's and root node's. The other bits of data
// are used for list widgets that perform their own layout and replacement data that take the layout
// of the original node being replaced.
class ParentLayoutInfo(
    val parentLayoutId: Int = -1,
    val childIndex: Int = 0,
    val rootLayoutId: Int = -1,
    val listLayoutType: ListLayoutType = ListLayoutType.None,
    val isWidgetAncestor: Boolean = false,
    val replacementLayoutData: ExternalLayoutData? = null,
    var designComposeRendered: Boolean = false,
)

internal fun ParentLayoutInfo.withRootIdIfNone(rootLayoutId: Int): ParentLayoutInfo {
    val rootLayoutId = if (this.rootLayoutId == -1) rootLayoutId else this.rootLayoutId
    return ParentLayoutInfo(
        this.parentLayoutId,
        this.childIndex,
        rootLayoutId,
        this.listLayoutType,
        this.isWidgetAncestor,
        this.replacementLayoutData,
        this.designComposeRendered,
    )
}

internal fun ParentLayoutInfo.withReplacementLayoutData(
    replacementLayoutData: ExternalLayoutData
): ParentLayoutInfo {
    return ParentLayoutInfo(
        this.parentLayoutId,
        this.childIndex,
        this.rootLayoutId,
        this.listLayoutType,
        this.isWidgetAncestor,
        replacementLayoutData,
        false,
    )
}

// Construct a ParentLayoutInfo object for the root node
internal val rootParentLayoutInfo = ParentLayoutInfo()

// A CompositionLocal ParentLayoutInfo object to be used in the UI tree
internal val LocalParentLayoutInfo = compositionLocalOf<ParentLayoutInfo?> { ParentLayoutInfo() }

// Declare a CompositionLocal object of the specified ParentLayoutInfo
@Composable
internal fun DesignParentLayout(parentLayout: ParentLayoutInfo?, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalParentLayoutInfo provides parentLayout) { content() }

// Declare a CompositionLocal object of the specified ParentLayoutInfo meant for list widget layouts
@Composable
internal fun DesignListLayout(listLayoutType: ListLayoutType, content: @Composable () -> Unit) {
    val parentLayout = ParentLayoutInfo(listLayoutType = listLayoutType, isWidgetAncestor = true)
    CompositionLocalProvider(LocalParentLayoutInfo provides parentLayout) { content() }
}

internal open class SimplifiedLayoutInfo(val selfModifier: Modifier) {
    internal fun shouldRender(): Boolean {
        // We only want to render if the layout is an absolute layout, meaning we are using our
        // custom Rust layout implementation. All other layout types mean we are using the list
        // widget, and any special visual styles applied in Figma should be on the widget's parent.
        return this is LayoutInfoAbsolute
    }
}

internal class LayoutInfoAbsolute(
    selfModifier: Modifier,
    val horizontalScroll: Boolean,
    val verticalScroll: Boolean,
) : SimplifiedLayoutInfo(selfModifier)

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

enum class ListLayoutType {
    None,
    Grid,
    Row,
    Column,
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
        var horizontalScroll = false
        var verticalScroll = false
        when (view.scroll_info.overflow) {
            is OverflowDirection.VERTICAL_SCROLLING -> {
                verticalScroll = true
            }
            is OverflowDirection.HORIZONTAL_SCROLLING -> {
                horizontalScroll = true
            }
            is OverflowDirection.HORIZONTAL_AND_VERTICAL_SCROLLING -> {
                // Currently we don't support both horizontal and vertical scrolling so disable both
                verticalScroll = false
                horizontalScroll = false
            }
        }
        return LayoutInfoAbsolute(modifier, horizontalScroll, verticalScroll)
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

internal val Measurable.designLayoutData: DesignLayoutData?
    get() = parentData as? DesignLayoutData

internal val Placeable.designLayoutData: DesignLayoutData?
    get() = parentData as? DesignLayoutData

internal fun Modifier.layoutStyle(name: String, layoutId: Int) =
    this.then(DesignLayoutData(name, layoutId))

internal fun Layout.width() = this.width.roundToInt()

internal fun Layout.height() = this.height.roundToInt()

internal fun Layout.left() = this.left.roundToInt()

internal fun Layout.top() = this.top.roundToInt()

internal class DesignScroll(
    val scrollOffset: Float,
    val orientation: Orientation,
    val scrollMax: MutableFloatState,
)

// Layout function for DesignFrame
@Composable
internal inline fun DesignFrameLayout(
    modifier: Modifier,
    view: View,
    layoutId: Int,
    rootLayoutId: Int,
    layoutState: Int,
    designScroll: DesignScroll?,
    content: @Composable () -> Unit,
) {
    val measurePolicy =
        remember(layoutState, designScroll) {
            designMeasurePolicy(view, layoutId, rootLayoutId, layoutState, designScroll)
        }
    Layout(content = content, measurePolicy = measurePolicy, modifier = modifier)
}

// Measure policy for DesignFrame.
internal fun designMeasurePolicy(
    view: View,
    layoutId: Int,
    rootLayoutId: Int,
    layoutState: Int,
    designScroll: DesignScroll?,
) = MeasurePolicy { measurables, constraints ->
    val name = view.name
    val placeables =
        measurables.map { measurable ->
            val layoutData = measurable.designLayoutData
            // Initialize constraints to those passed in. If the view should use infinite
            // constraints for its children because it has a child that uses a built-in
            // container such as a Row() or Column(), construct infinite contraints. Otherwise,
            // if we have layout data for the child, use them to create fixed constraints.
            var childConstraints = constraints
            if (view.useInfiniteConstraints()) childConstraints = Constraints()
            else if (layoutData != null) {
                val childLayout = LayoutManager.getLayoutWithDensity(layoutData.layoutId)
                if (childLayout != null) {
                    val childWidth = childLayout.width()
                    val childHeight = childLayout.height()
                    childConstraints = Constraints(childWidth, childWidth, childHeight, childHeight)
                }
            }
            measurable.measure(childConstraints)
        }

    var myLayout = LayoutManager.getLayoutWithDensity(layoutId)
    if (myLayout == null)
        Log.d(TAG, "designMeasurePolicy error: null layout $name layoutId $layoutId")
    // Get width and height from constraints if they are fixed. Otherwise get them from layout.
    val myWidth =
        if (constraints.hasFixedWidth && constraints.maxWidth != 0) constraints.maxWidth
        else myLayout?.width() ?: 0
    val myHeight =
        if (constraints.hasFixedHeight && constraints.maxHeight != 0) constraints.maxHeight
        else myLayout?.height() ?: 0

    // If constraints have forced a size that does not match our layout size, set this size into
    // the layout manager so that it can set this size in the Rust layout manager and then
    // recalculate any layouts that many have changed. However, only do this if the layout state
    // has not changed after computing child layouts. This is necessary in the case where
    // calling setNodeSize on a node causes an ancestor to resize.
    val layoutState2 = LayoutManager.getLayoutState(layoutId)
    if (
        layoutState == layoutState2 &&
            myLayout != null &&
            (myWidth != myLayout?.width() || myHeight != myLayout?.height())
    )
        LayoutManager.setNodeSize(layoutId, rootLayoutId, myWidth, myHeight)
    layout(myWidth, myHeight) {
        // Place children in the parent layout
        placeables.forEachIndexed { index, placeable ->
            val layoutData = placeable.designLayoutData
            if (layoutData == null) {
                // A null layout should only happen if the child is a node that uses one of
                // Compose's built in layouts such as Row, Column, or LazyGrid.
                placeable.place(0, 0)
                if (index != 0) Log.d(TAG, "Place error no layoutData: $name index $index")
            } else {
                val childLayout = LayoutManager.getLayoutWithDensity(layoutData.layoutId)
                if (childLayout == null) {
                    Log.d(TAG, "Place error null layout: $name child $index layoutId $layoutId")
                } else {
                    var hScrollOffset = 0
                    var vScrollOffset = 0
                    if (designScroll != null) {
                        // If designScroll is not null, scroll is enabled on this node. Get the
                        // scroll offset and offset all children
                        if (designScroll.orientation == Orientation.Horizontal)
                            hScrollOffset = designScroll.scrollOffset.roundToInt()
                        else vScrollOffset = designScroll.scrollOffset.roundToInt()
                        if (index == placeables.size - 1) {
                            // Calculate the extents of the children by using the last item's size
                            // and position
                            val density = LayoutManager.getDensity()
                            if (designScroll.orientation == Orientation.Horizontal) {
                                val hMargin = view.style.padding.end.pointsAsDp(density).value
                                designScroll.scrollMax.value =
                                    (childLayout.left + childLayout.width - myWidth + hMargin)
                                        .coerceAtLeast(0F)
                            } else {
                                val vMargin = view.style.padding.bottom.pointsAsDp(density).value
                                designScroll.scrollMax.value =
                                    (childLayout.top + childLayout.height - myHeight + vMargin)
                                        .coerceAtLeast(0F)
                            }
                        }
                    }
                    placeable.placeWithLayer(
                        x = childLayout.left() + hScrollOffset,
                        y = childLayout.top() + vScrollOffset
                    )
                }
            }
        }
    }
}

@Composable
internal inline fun DesignTextLayout(
    modifier: Modifier,
    layout: Layout?,
    layoutState: Int,
    renderTop: Int?,
    content: @Composable () -> Unit,
) {
    val measurePolicy =
        remember(layoutState, layout, renderTop) { designTextMeasurePolicy(layout, renderTop) }
    Layout(content = content, measurePolicy = measurePolicy, modifier = modifier)
}

internal fun designTextMeasurePolicy(layout: Layout?, renderTop: Int?) =
    MeasurePolicy { measurables, constraints ->
        val placeables = measurables.map { measurable -> measurable.measure(constraints) }

        val myWidth = if (constraints.hasFixedWidth) constraints.maxWidth else layout?.width() ?: 0
        val myHeight =
            if (constraints.hasFixedHeight) constraints.maxHeight else layout?.height() ?: 0
        val myX = 0
        val myY = renderTop ?: layout?.top() ?: 0
        layout(myWidth, myHeight) {
            // Text has no children, so placeables is always just a list of 1 for this text, which
            // we place at the calculated offset.
            // There are two offsets that we need to consider. The offset used here myX, myY
            // take into account the text's vertical offset, but not layout offset from its parent.
            // The layout offset is used when this text node is placed by its parent.
            if (layout != null) placeables.forEach { placeable -> placeable.place(myX, myY) }
        }
    }
