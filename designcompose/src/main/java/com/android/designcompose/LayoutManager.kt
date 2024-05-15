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

package com.android.designcompose

import android.util.Log
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException
import androidx.tracing.trace
import com.android.designcompose.proto.intoProto
import com.android.designcompose.proto.intoSerde
import com.android.designcompose.proto.layout.LayoutManager
import com.android.designcompose.proto.layout.LayoutStyleOuterClass
import com.android.designcompose.proto.layout.LayoutStyleOuterClass.LayoutStyle
import com.android.designcompose.proto.layout.layoutNode
import com.android.designcompose.proto.layout.layoutNodeList
import com.android.designcompose.serdegen.Layout
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import kotlin.math.roundToInt

internal object LayoutManager {
    private var managerId: Int = 0
    private val subscribers: HashMap<Int, (Int) -> Unit> = HashMap()
    private val layoutsInProgress: HashSet<Int> = HashSet()
    private var textMeasures: HashMap<Int, TextMeasureData> = HashMap()
    private var modifiedSizes: HashSet<Int> = HashSet()
    private var nextLayoutId: Int = 0
    private var performLayoutComputation: Boolean = false
    private var density: Float = 1F
    private var layoutNodes: ArrayList<LayoutManager.LayoutNode> = arrayListOf()
    private var layoutCache: HashMap<Int, LayoutManager.Layout> = HashMap()
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
        style: com.android.designcompose.serdegen.LayoutStyle,
        name: String,
    ) {
        // Frames can have children so call beginLayout() to optimize layout computation until all
        // children have been added.
        beginLayout(layoutId)
        subscribe(
            layoutId,
            setLayoutState,
            parentLayoutId,
            childIndex,
            style.intoProto(),
            name,
            false
        )
    }

    internal fun subscribeWithMeasure(
        layoutId: Int,
        setLayoutState: (Int) -> Unit,
        parentLayoutId: Int,
        rootLayoutId: Int,
        childIndex: Int,
        style: com.android.designcompose.serdegen.LayoutStyle,
        name: String,
        textMeasureData: TextMeasureData,
    ) {
        textMeasures[layoutId] = textMeasureData
        subscribe(
            layoutId,
            setLayoutState,
            parentLayoutId,
            childIndex,
            style.intoProto(),
            name,
            true
        )

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
        style: LayoutStyleOuterClass.LayoutStyle,
        name: String,
        useMeasureFunc: Boolean,
        fixedWidth: Optional<Int> = Optional.empty(),
        fixedHeight: Optional<Int> = Optional.empty(),
    ) {
        subscribers[layoutId] = setLayoutState
        // Add the node to a list of nodes
        layoutNodes.add(
            // Convert the serde layout node to the proto layout node
            layoutNode {
                this.layoutId = layoutId
                this.parentLayoutId = parentLayoutId
                this.childIndex = childIndex
                this.style = style
                this.name = name
                this.useMeasureFunc = useMeasureFunc
                fixedWidth.getOrNull()?.let { this.fixedWidth = it }
                fixedHeight.getOrNull()?.let { this.fixedHeight = it }
            }
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
                val layoutNodeList = layoutNodeList {
                    layoutNodes.addAll(this@LayoutManager.layoutNodes)
                }
                val responseBytes =
                    Jni.jniAddNodes(managerId, rootLayoutId, layoutNodeList.toByteArray())
                handleResponse(responseBytes)
                layoutNodes.clear()
            }
        }
    }

    private fun handleResponse(responseBytes: ByteArray?) {
        if (responseBytes != null) {
            val response =
                try {
                    LayoutManager.LayoutChangedResponse.parseFrom(responseBytes)
                } catch (e: InvalidProtocolBufferException) {
                    Log.d(TAG, "HandleResponse error")
                    return
                }
            // Add all the layouts to our cache
            response.changedLayoutsMap.forEach { (layoutId, layout) ->
                layoutCache[layoutId] = layout
                layoutStateCache[layoutId] = response.layoutState
            }
            notifySubscribers(response.changedLayoutsMap.keys, response.layoutState)
            if (response.changedLayoutsMap.isNotEmpty())
                Log.d(
                    TAG,
                    "HandleResponse ${response.layoutState}, changed: ${response.changedLayoutsMap.keys}"
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
    internal fun getLayout(layoutId: Int): Layout? = layoutCache[layoutId]?.intoSerde()

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
