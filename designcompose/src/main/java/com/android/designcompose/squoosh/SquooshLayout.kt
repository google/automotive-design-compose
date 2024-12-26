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

package com.android.designcompose.squoosh

import android.util.Log
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException
import com.android.designcompose.Jni
import com.android.designcompose.LayoutManager
import com.android.designcompose.android_interface.LayoutChangedResponse
import com.android.designcompose.android_interface.LayoutNodeList
import com.android.designcompose.android_interface.layoutNodeList
import com.android.designcompose.definition.layout.LayoutNode
import com.android.designcompose.definition.layout.LayoutParentChildren
import com.android.designcompose.definition.layout.layoutNode
import com.android.designcompose.definition.layout.layoutParentChildren

internal class SquooshLayoutManager(val id: Int)

internal object SquooshLayout {

    internal fun newLayoutManager(): SquooshLayoutManager {
        return SquooshLayoutManager(Jni.jniCreateLayoutManager())
    }

    internal fun removeNode(manager: SquooshLayoutManager, rootLayoutId: Int, layoutId: Int) {
        Jni.jniRemoveNode(manager.id, layoutId, rootLayoutId, false)
    }

    internal fun markDirty(manager: SquooshLayoutManager, layoutId: Int) {
        Jni.jniMarkDirty(manager.id, layoutId)
    }

    internal fun doLayout(
        manager: SquooshLayoutManager,
        rootLayoutId: Int,
        layoutNodeList: LayoutNodeList,
    ): Map<Int, LayoutChangedResponse.Layout> {
        val serializedNodes = layoutNodeList.toByteArray()
        val response =
            Jni.jniAddNodes(manager.id, rootLayoutId, serializedNodes) ?: return emptyMap()
        val layoutChangedResponse =
            try {
                LayoutChangedResponse.parseFrom(response)
            } catch (e: InvalidProtocolBufferException) {
                Log.e("SquooshLayout", "Failed to parse layout changed response", e)
                throw e
            }
        return layoutChangedResponse.changedLayoutsMap
    }
}

internal class SquooshLayoutIdAllocator(
    private var lastAllocatedId: Int = 1,
    private val idMap: HashMap<ParentComponentData, Int> = HashMap(),
    private val listIdMap: HashMap<Int, Int> = HashMap(),
    // We can also track referenced layout IDs from one generation to the next, which lets us
    // build the set of nodes to remove from the native layout tree.
    private var visitedSet: HashSet<Int> = HashSet(),
    private var remainingSet: HashSet<Int> = HashSet(),
) {
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

    /// Return a new root layout id for a list parent. This is pretty clumsy, since if we add
    /// or remove lists from a doc, we'll end up invalidating all of the lists that come after
    /// the one that was added/removed.
    fun listLayoutId(parentLayoutId: Int): Int {
        val maybeId = listIdMap[parentLayoutId]
        if (maybeId != null) return maybeId
        val id = lastAllocatedId++
        listIdMap[parentLayoutId] = id
        return id
    }

    /// Note that we've visited a layout node; this protects it from removal and adds it to the
    /// set of nodes that might get removed next iteration.
    fun visitLayoutId(id: Int) {
        if (visitedSet.contains(id)) {
            Log.e(TAG, "Duplicate layout ID: $id; cannot proceed...")
            throw RuntimeException("Duplicate layout ID: $id")
        }
        visitedSet.add(id)
        remainingSet.remove(id)
    }

    /// Get the set of layout nodes to remove.
    fun removalNodes(): HashSet<Int> {
        val removalSet = remainingSet
        remainingSet = visitedSet
        visitedSet = HashSet()
        return removalSet
    }
}

// These functions manage all of the Layout ID arithmetic to ensure that we have unique layout
// IDs and that we're able to compute them fast.
//
// When the DCF file is generated, each node is assigned a unique 16bit ID which can be used
// for layout. Squoosh generates an entirely new tree every time it's invalidated, but uses the
// same layout tree in Rust layout. Therefore, it's important that the same nodes get the same
// IDs each time the Squoosh tree is built, and we achieve this using the unique 16bit ID plus
// another 16 bits for component instances and other situations where we have the same nodes from
// the DCF in the same tree.
//
// These functions are all about the upper 16bits of a Layout ID (which are generated at runtime,
// rather than the lower 16bits, which are generated with the DCF file and are just sequential).
//
//  2 bits:
//   00 - not used
//   01 - component instance
//   10 - overlay
//   11 - list item
// 14 bits:
//   sequence
// 16 bits:
//   DCF unique ID
private const val ID_USAGE_MASK = 0x3FFF0000
private const val ID_USAGE_COMPONENT = 0x40000000
private const val ID_USAGE_OVERLAY = 0x80000000.toInt()
private const val ID_USAGE_LIST_ITEM = 0xc0000000.toInt()

/// Compute a "componentLayoutId" from an incoming root layout ID, and a component instance ID.
internal fun computeComponentLayoutId(rootLayoutId: Int, componentLayoutId: Int): Int {
    return (rootLayoutId + componentLayoutId.shl(16)).and(ID_USAGE_MASK) + ID_USAGE_COMPONENT
}

/// Compute a final layout ID from a component layout ID (16bits) and a unique node ID from the DCF
internal fun computeLayoutId(componentLayoutId: Int, uniqueViewId: Int): Int {
    if (uniqueViewId !in 0..0xFFFF) {
        throw RuntimeException("View's unique ID must be in the range 0..0xFFFF")
    }
    return componentLayoutId + uniqueViewId
}

/// Compute a synthetic layout ID. We use these when we're synthesizing layout nodes at runtime
/// for things like list items and overlays.
internal fun computeSyntheticListItemLayoutId(listLayoutId: Int, childIndex: Int): Int {
    return listLayoutId.shl(16).and(ID_USAGE_MASK) + ID_USAGE_LIST_ITEM + childIndex
}

/// Compute a synthetic layout ID for an overlay.
internal fun computeSyntheticOverlayLayoutId(contentNodeLayoutId: Int, overlayIndex: Int): Int {
    return contentNodeLayoutId.shl(16).and(ID_USAGE_MASK) + ID_USAGE_OVERLAY + overlayIndex
}

/// Takes a `SquooshResolvedNode` and recursively builds or updates a native layout tree via
/// the `SquooshLayout` wrapper of `JniLayout`.
private fun updateLayoutTree(
    manager: SquooshLayoutManager,
    resolvedNode: SquooshResolvedNode,
    layoutCache: HashMap<Int, Int>,
    layoutNodes: ArrayList<LayoutNode>,
    layoutParentChildren: ArrayList<LayoutParentChildren>,
    parentLayoutId: Int = -1,
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
            LayoutManager.squooshSetTextMeasureData(layoutId, resolvedNode.textInfo)
        }

        // We need to measure some children during the Compose layout phase, because they're
        // actual Composables and not part of the DesignCompose layout tree. For those, we
        // use the measure func mechanism, and ask the child Composable for its intrinsic
        // size.
        if (resolvedNode.needsChildLayout) {
            useMeasureFunc = true
        }

        layoutNodes.add(
            layoutNode {
                this.layoutId = layoutId
                this.parentLayoutId = parentLayoutId
                this.childIndex = -1 // not childIdx!
                this.style = resolvedNode.style.layoutStyle
                this.name = resolvedNode.view.name
                this.useMeasureFunc = useMeasureFunc
                clearFixedWidth()
                clearFixedHeight()
            }
        )
        layoutCache[layoutId] = layoutCacheKey
    }
    // If this node is laid out externally then we need to re-measure it each layout.
    // Mark it as dirty.
    if (resolvedNode.needsChildLayout) SquooshLayout.markDirty(manager, layoutId)
    // XXX: We might want separate (cheaper) calls to assert the tree structure.
    // XXX XXX: This code doesn't ever update the tree structure.

    var updateLayoutChildren = needsLayoutUpdate
    var child = resolvedNode.firstChild
    while (child != null) {
        layoutChildren.add(child.layoutId)
        updateLayoutChildren =
            updateLayoutTree(
                manager,
                child,
                layoutCache,
                layoutNodes,
                layoutParentChildren,
                layoutId,
            ) || updateLayoutChildren
        child = child.nextSibling
    }

    if (updateLayoutChildren)
        layoutParentChildren.add(
            layoutParentChildren {
                layoutId
                layoutChildren
            }
        )

    return needsLayoutUpdate
}

/// Iterate over a `SquooshResolvedNode` tree and populate the computed layout values
/// so that the nodes can be used for presentation or interaction (hit testing).
private fun populateComputedLayout(
    resolvedNode: SquooshResolvedNode,
    layoutValueCache: HashMap<Int, LayoutChangedResponse.Layout>,
) {
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
    manager: SquooshLayoutManager,
    removalNodes: HashSet<Int>,
    layoutCache: HashMap<Int, Int>,
    layoutValueCache: HashMap<Int, LayoutChangedResponse.Layout>,
) {
    // Remove any nodes that are no longer needed in this iteration
    for (layoutId in removalNodes) {
        SquooshLayout.removeNode(manager, 0, layoutId)
        layoutValueCache.remove(layoutId)
        layoutCache.remove(layoutId)
    }
    // Clear removalNodes so that multiple calls to this don't try to remove the same nodes
    removalNodes.clear()

    // Update the layout tree which the Rust JNI code is maintaining
    val layoutNodes = arrayListOf<LayoutNode>()
    val layoutParentChildren = arrayListOf<LayoutParentChildren>()
    updateLayoutTree(manager, root, layoutCache, layoutNodes, layoutParentChildren)
    val layoutNodeList = layoutNodeList {
        this.layoutNodes.addAll(layoutNodes)
        this.parentChildren.addAll(layoutParentChildren)
    }

    // Now we can give the new layoutNodeList to the Rust JNI layout implementation
    val updatedLayouts = SquooshLayout.doLayout(manager, root.layoutId, layoutNodeList)
    // Save the updated layouts and quickly iterate the tree and populate the layout values.
    layoutValueCache.putAll(updatedLayouts)
    populateComputedLayout(root, layoutValueCache)
}

/// Go over a tree that layout can't be applied to, and copy the
/// layout values from whatever the source tree is.
internal fun updateDerivedLayout(n: SquooshResolvedNode) {
    if (n.layoutNode != null) n.computedLayout = n.layoutNode.computedLayout
    if (n.firstChild != null) updateDerivedLayout(n.firstChild!!)
    if (n.nextSibling != null) updateDerivedLayout(n.nextSibling!!)
}
