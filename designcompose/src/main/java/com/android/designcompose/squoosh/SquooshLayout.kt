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

package com.android.designcompose.squoosh

import android.util.Log
import com.android.designcompose.Jni
import com.android.designcompose.LayoutManager
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.LayoutChangedResponse
import com.android.designcompose.serdegen.LayoutNode
import com.android.designcompose.serdegen.LayoutNodeList
import com.android.designcompose.serdegen.LayoutParentChildren
import com.novi.bincode.BincodeDeserializer
import com.novi.bincode.BincodeSerializer
import java.util.Optional


internal object SquooshLayout {
    private var nextLayoutId: Int = 0

    internal fun getNextLayoutId(): Int {
        return ++nextLayoutId
    }

    internal fun removeNode(rootLayoutId: Int, layoutId: Int) {
        Jni.jniRemoveNode(layoutId, rootLayoutId, false)
    }

    internal fun keepJniBits() {
        Jni.jniSetNodeSize(0, 0, 0, 0)
    }

    internal fun doLayout(rootLayoutId: Int, layoutNodeList: LayoutNodeList): Map<Int, Layout> {
        val serializedNodes = serialize(layoutNodeList)
        val response = Jni.jniAddNodes(rootLayoutId, serializedNodes) ?: return emptyMap()
        val layoutChangedResponse = LayoutChangedResponse.deserialize(BincodeDeserializer(response))
        return layoutChangedResponse.changed_layouts
    }

    private fun serialize(layoutNodeList: LayoutNodeList): ByteArray {
        val nodeListSerializer = BincodeSerializer()
        layoutNodeList.serialize(nodeListSerializer)
        return nodeListSerializer._bytes
    }
}

internal class SquooshLayoutIdAllocator(
    private var lastAllocatedId: Int = 1,
    private val idMap: HashMap<ParentComponentData, Int> = HashMap(),
    // We can also track referenced layout IDs from one generation to the next, which lets us
    // build the set of nodes to remove from the native layout tree.
    private var visitedSet: HashSet<Int> = HashSet(),
    private var remainingSet: HashSet<Int> = HashSet(),
)
{
    /// Return a new "root layout id" for a tree node that is an instance of a component. This
    /// ensures that component instance children get unique layout ids even though there might
    /// be many instances of the same component in one tree.
    fun componentLayoutId(component: ParentComponentData): Int {
        val maybeId = idMap[component]
        if (maybeId != null) return maybeId
        val id = lastAllocatedId++
        idMap[component] = id
        return id
    }

    /// Note that we've visited a layout node; this protects it from removal and adds it to the
    /// set of nodes that might get removed next iteration.
    fun visitLayoutId(id: Int) {
        visitedSet.add(id)
        remainingSet.remove(id)
    }

    /// Get the set of layout nodes to remove.
    fun removalNodes(): Set<Int> {
        val removalSet = remainingSet
        remainingSet = visitedSet
        visitedSet = HashSet()
        return removalSet
    }
}


/// Takes a `SquooshResolvedNode` and recursively builds or updates a native layout tree via
/// the `SquooshLayout` wrapper of `JniLayout`.
internal fun updateLayoutTree(
    resolvedNode: SquooshResolvedNode,
    layoutCache: HashMap<Int, Int>,
    layoutNodes: ArrayList<LayoutNode>,
    layoutParentChildren: ArrayList<LayoutParentChildren>,
    parentLayoutId: Int = 0,
): Boolean {
    // Make a unique layout id for this node by taking the root's unique id and adding the
    // file specific unique id (which is a u16).
    val layoutId = resolvedNode.layoutId

    // Compute a cache key for the layout; we use this to determine if we need to update the
    // node with a new layout value or not.
    val layoutCacheKey = resolvedNode.style.hashCode() + resolvedNode.textInfo.hashCode()
    val needsLayoutUpdate = layoutCache[layoutId] != layoutCacheKey
    val layoutChildren: ArrayList<Int> = arrayListOf()

    if (needsLayoutUpdate) {
        var useMeasureFunc = false

        // Text needs some additional work to measure, and to let layout measure interactively
        // to account for wrapping.
        if (resolvedNode.textInfo != null) {
            // We need layout to measure this text.
            useMeasureFunc = true

            // This is used by the callback logic in DesignText.kt to compute width-for-height
            // computations for the layout implementation in Rust.
            LayoutManager.squooshSetTextMeasureData(
                layoutId,
                resolvedNode.textInfo
            )
        }

        layoutNodes.add(
            LayoutNode(
                layoutId,
                parentLayoutId,
                -1, // not childIdx!
                resolvedNode.style,
                resolvedNode.view.name,
                useMeasureFunc,
                Optional.empty(),
                Optional.empty()
            )
        )
        layoutCache[layoutId] = layoutCacheKey
    }
    // XXX: We might want separate (cheaper) calls to assert the tree structure.
    // XXX XXX: This code doesn't ever update the tree structure.

    var updateLayoutChildren = needsLayoutUpdate
    var child = resolvedNode.firstChild
    while (child != null) {
        layoutChildren.add(child.layoutId)
        updateLayoutChildren = updateLayoutTree(child, layoutCache, layoutNodes, layoutParentChildren, layoutId) || updateLayoutChildren
        child = child.nextSibling
    }

    if (updateLayoutChildren) {
        layoutParentChildren.add(LayoutParentChildren(layoutId, layoutChildren))
    }

    return needsLayoutUpdate
}

/// Iterate over a `SquooshResolvedNode` tree and populate the computed layout values
/// so that the nodes can be used for presentation or interaction (hit testing).
internal fun populateComputedLayout(
    resolvedNode: SquooshResolvedNode,
    layoutValueCache: HashMap<Int, Layout>
)
{
    val layoutId = resolvedNode.layoutId
    val layoutValue = layoutValueCache[layoutId]
    if (layoutValue == null) {
        Log.d(TAG, "Unable to fetch computed layout for ${resolvedNode.view.name} and its children")
    }
    resolvedNode.computedLayout = layoutValue

    var child = resolvedNode.firstChild
    while (child != null) {
        populateComputedLayout(child, layoutValueCache)
        child = child.nextSibling
    }
}

/// Take a freshly computed `SquooshResolvedNode` tree and compute and populate layout for it.
internal fun layoutTree(
    root: SquooshResolvedNode,
    rootLayoutId: Int,
    removalNodes: Set<Int>,
    layoutCache: HashMap<Int, Int>,
    layoutValueCache: HashMap<Int, Layout>)
{
    // Remove any nodes that are no longer needed in this iteration
    for (layoutId in removalNodes) {
        SquooshLayout.removeNode(rootLayoutId, layoutId)
        layoutValueCache.remove(layoutId)
        layoutCache.remove(layoutId)
    }

    // Update the layout tree which the Rust JNI code is maintaining
    val layoutNodes = arrayListOf<LayoutNode>()
    val layoutParentChildren = arrayListOf<LayoutParentChildren>()
    updateLayoutTree(root, layoutCache, layoutNodes, layoutParentChildren)
    val layoutNodeList = LayoutNodeList(layoutNodes, layoutParentChildren)

    // Now we can give the new layoutNodeList to the Rust JNI layout implementation
    val updatedLayouts = SquooshLayout.doLayout(
        root.layoutId,
        layoutNodeList
    )
    // Save the updated layouts and quickly iterate the tree and populate the layout values.
    layoutValueCache.putAll(updatedLayouts)
    populateComputedLayout(root, layoutValueCache)
}