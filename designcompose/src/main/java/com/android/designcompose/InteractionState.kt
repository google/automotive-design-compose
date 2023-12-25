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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.android.designcompose.common.VariantPropertyMap
import com.android.designcompose.serdegen.Action
import com.android.designcompose.serdegen.Navigation
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.serdegen.Transition
import com.android.designcompose.serdegen.View

// In order to differentiate multiple instances of a component, we use a combination of the node ID
// and an optional key to uniquely identify a component instance. This allows us to apply changes
// to a specific component instance instead of all of them, such as changing to a different variant.
private fun getInstanceIdWithKey(instanceId: String, key: String?): String {
    var varKey = instanceId
    if (key != null) varKey += key
    return varKey
}

/// Some triggers only apply their action temporarily, for example the "while pressing"
/// trigger applies its action while a touch point is pressed, and then reverts it. To
/// implement this we maintain an "undo memory" which maps from the node ID that is
/// applying the temporary action to an `DeferredAction` which when applied undoes the
/// initial action.
///
/// We also use `DeferredAction` in our `TransitionState` struct so we know what to do
/// when a transition ends (normally closing an overlay or doing nothing).
///
/// These actions are similar to the ones that we get directly from Figma, however they
/// don't have to set up any of the "undo memory" or disambiguate targets (between overlays
/// and root navigation) so they are simpler implementations.
internal open class DeferredAction {
    /// Navigate back, and restore the `overlay_memory`. This is used when a "navigate"
    /// action has to be undone.
    class NavigateBack(val overlayMemory: List<String>) : DeferredAction() {}

    /// The temporary action was a "swap navigation" (where the top of the navigation
    /// stack is swapped, rather than being appended to), so we need to swap back to
    /// the previous entry. We keep a NodeQuery instead of a String, because we could
    /// be swapping the first entry in the navigation stack which comes from application
    /// code and is normally a node name (and NodeQuery contains names or IDs).
    class SwapNavigation(val nodeQuery: NodeQuery) : DeferredAction() {}

    /// Close the top-most overlay
    class CloseTopOverlay(val transition: Transition?) : DeferredAction() {}

    /// The temporary action was to open an overlay, so we need to close the same overlay
    class CloseOverlay(val overlayId: String, val transition: Transition?) : DeferredAction() {}

    /// The temporary action was to close an overlay, so we need to open the overlay
    /// again to undo the action (and the ID of the overlay to open is the String).
    class OpenOverlay(val overlayId: String, val transition: Transition?) : DeferredAction() {}

    /// The temporary action was a "swap overlay", so we need to swap back to the
    /// previously displayed overlay.
    class SwapOverlay(val overlayId: String, val transition: Transition?) : DeferredAction() {}

    /// The temporary action was a "change to" on a component variant. We need to change
    /// back to the variant we were showing before, which could be the default (in which
    /// case the value is null), or could be some other specific variant (in which case
    /// the value will be the node ID).
    class ChangeTo(val nodeId: String?) : DeferredAction() {}
}

/// Execute a DeferredAction. An optional key is used to differentiate multiple component instances
/// that share the same targetInstanceId.
internal fun DeferredAction.apply(
    state: InteractionState,
    targetInstanceId: String?,
    key: String?
) {
    // XXX: track start time for animations? Would like to implement
    when (this) {
        is DeferredAction.NavigateBack -> {
            state.navigationHistory.removeLast()
            state.overlayMemory = ArrayList(this.overlayMemory)
            state.invalNavOverlay()
        }
        is DeferredAction.SwapNavigation -> {
            state.navigationHistory.removeLast()
            state.navigationHistory.add(this.nodeQuery)
            state.invalNavOverlay()
        }
        is DeferredAction.CloseTopOverlay -> {
            // Current Figma behavior is to just close the frontmost overlay, so we don't
            // care which overlay is frontmost. This leads to some unexpected situations
            // if a "open-while-pressed" overlay opens another overlay on a timeout.
            val overlayId = state.overlayMemory.removeLastOrNull()
            if (overlayId != null) {
                // XXX: Handle the transition if present
            }
            state.invalNavOverlay()
        }
        is DeferredAction.CloseOverlay -> {
            state.overlayMemory.remove(this.overlayId)
            state.invalNavOverlay()
        }
        is DeferredAction.OpenOverlay -> {
            if (state.overlayMemory.lastIndexOf(this.overlayId) != state.overlayMemory.size - 1) {
                state.overlayMemory.add(this.overlayId)
                state.invalNavOverlay()
            }
        }
        is DeferredAction.SwapOverlay -> {
            state.overlayMemory.removeLast()
            state.overlayMemory.add(this.overlayId)
            state.invalNavOverlay()
        }
        is DeferredAction.ChangeTo -> {
            if (targetInstanceId == null) return
            val varKey = getInstanceIdWithKey(targetInstanceId, key)
            if (this.nodeId != null) {
                state.variantMemory[varKey] = this.nodeId
            } else {
                state.variantMemory.remove(varKey)
            }
            state.invalVariant(targetInstanceId)
        }
    }
}

// Marking this as immutable indicates that it produces immutable instances. Doing this allows
// OpenLinkCallback to be a stable parameter instead of an unstable one in @Composable functions,
// which in turns allows the function to be skippable when doing recomposition. For more info:
// https://github.com/androidx/androidx/blob/androidx-main/compose/compiler/design/compiler-metrics.md
@Immutable
fun interface OpenLinkCallback {
    fun openLink(url: String)
}


/// This refers to an action which hasn't been committed to the state, and is held in a separate
/// list so that the renderer can animate from the state without this action committed to the state
/// with this animation committed.
internal class AnimatedAction(
    val instanceNodeId: String,
    val key: String?,
    val newVariantId: String,
    val undoInstanceId: String?,
    val transition: Transition,
    val interruptedId: Int?,
    val id: Int // just a counted value
)
// XXX: Add subscriptions? Use Kotlin setters to trigger invalidations? How to batch invals?

internal class InteractionState {
    /// Top-level navigation history. This gets appended to by the
    /// NAVIGATE action, and popped by the BACK action. The initial
    /// frame to show is not included in the history, so when this
    /// list is empty we show the root FigmaComponent's initial frame.
    var navigationHistory: ArrayList<NodeQuery> = ArrayList()

    /// Node-level memory of current state. This is used by
    /// instances of component variants to determine which variant
    /// to show. The mapping is from component instance node id to
    /// variant node id.
    var variantMemory: HashMap<String, String> = HashMap()

    /// Used by overlays to determine if they should be visible or
    /// not, and manipulated by the SHOW_OVERLAY, SWAP_OVERLAY and
    /// CLOSE_OVERLAY actions. The overlay's final position and
    /// animation details are contained in the Action. This list is
    /// of the node ids of the overlays.
    var overlayMemory: ArrayList<String> = ArrayList()

    /// The "ON_PRESS" (called "While Pressed" in the Figma UI) is
    /// reverted when the component is unpressed. That means we need
    /// to remember some values so that we can undo actions. The actions
    /// where we need memory are:
    ///
    ///  * Close (what we closed)
    ///  * ChangeTo (what variant we started from)
    ///  * Swap (what we swapped from)
    ///
    /// Those dispatch methods take an "undoable" parameter which causes
    /// them to also save a record here. We record the id for undoing
    /// along with the instance id that performed the undoable action.
    ///
    /// Even though we only support one trigger that's undoable (ON_PRESS)
    /// we support multitouch input, so we could have multiple undoable
    /// things at once. The key is the node id of the trigger.
    var undoMemory: HashMap<String, DeferredAction> = HashMap()

    /// A hash of open link callbacks. All generated functions have the
    /// option of registering a callback for open-link actions. When one of
    /// these actions is executed, all registered callbacks are called
    var openLinkCallbacks: HashSet<OpenLinkCallback> = HashSet()

    // Classic DesignCompose does not support animated transitions, so when
    // running there, we just default to always immediately applying the
    // change.
    var supportAnimations: Boolean = false

    /// A list of animated transitions that are currently in play.
    var animations: ArrayList<AnimatedAction> = ArrayList()
    var lastAnimationId: Int = 0

    /// Subscriptions...
    var navOverlaySubscriptions: ArrayList<() -> Unit> = ArrayList()
    var variantSubscriptions: HashMap<String, ArrayList<() -> Unit>> = HashMap()
    var animationSubscriptions: ArrayList<() -> Unit> = ArrayList()
}

/// Perform the "navigate" action, by appending the given node id to
/// the end of the stack.
internal fun InteractionState.navigate(targetNodeId: String, undoInstanceId: String?) {
    // If there's an undo instance id then make an undo record of this action.
    if (undoInstanceId != null) {
        this.undoMemory[undoInstanceId] = DeferredAction.NavigateBack(this.overlayMemory.toList())
    }
    this.navigationHistory.add(NodeQuery.NodeId(targetNodeId))
    // Close overlays when navigating the top-level frame.
    this.overlayMemory.clear()

    // When we navigate, the root view changes. Defer computations so that we
    // don't compute layout after every view in the previous root view is
    // removed.
    LayoutManager.deferComputations()
    invalNavOverlay()
}

/// Perform the "overlay" action, by adding the given node id to the end
/// of the overlays list.
internal fun InteractionState.overlay(
    targetNodeId: String,
    transition: Transition?,
    undoInstanceId: String?
) {
    // Remember which overlay to close -- Figma itself just closes whatever the
    // top overlay is (so if you have a timeout that swaps or opens another overlay
    // then the frontmost overlay is closed). We replicate that behavior.
    if (undoInstanceId != null) {
        this.undoMemory[undoInstanceId] = DeferredAction.CloseTopOverlay(transition)
    }

    // Open the new overlay
    this.overlayMemory.add(targetNodeId)

    // XXX: transition handling

    invalNavOverlay()
}

/// Perform a "back" action, by removing the last node name in the navigation stack.
internal fun InteractionState.back() {
    if (this.navigationHistory.size > 0) {
        this.navigationHistory.removeLast()
        this.overlayMemory.clear()
        // When we navigate, the root view changes. Defer computations so that
        // we don't compute layout after every view in the previous root view
        // is removed.
        LayoutManager.deferComputations()
    } else {
        Log.i(TAG, "Unable to go back because the navigation stack is empty")
    }
    invalNavOverlay()
}

/// Perform a "close" action, by removing the last item in the overlay_memory.
internal fun InteractionState.close(undoInstanceId: String?) {
    val closedOverlayId = this.overlayMemory.removeLastOrNull()
    if (closedOverlayId != null) {
        // XXX: Transitions

        // Save this in undo memory if we need to undo it.
        if (undoInstanceId != null) {
            this.undoMemory[undoInstanceId] = DeferredAction.OpenOverlay(closedOverlayId, null)
        }
    } else {
        Log.i(TAG, "Unable to close overlay because no overlays are open.")
    }
    invalNavOverlay()
}

/// Perform a "swap" action, either replacing the last item in the overlay_memory,
/// or replacing the last item in the navigation stack if the overlay_memory is empty.
internal fun InteractionState.swap(
    targetNodeId: String,
    transition: Transition?,
    undoInstanceId: String?
) {
    val previousOverlayId = overlayMemory.removeLastOrNull()
    if (previousOverlayId != null) {
        // XXX: Transitions

        // Open the new overlay we're swapping to.
        overlayMemory.add(targetNodeId)

        // XXX: Remember the close transition

        // If we're swapping for a while-pressed trigger then remember what we need to undo when
        // the press is released.
        if (undoInstanceId != null) {
            undoMemory[undoInstanceId] = DeferredAction.SwapOverlay(previousOverlayId, null)
        }

        invalNavOverlay()
        return
    }
    val previousNavigationId = navigationHistory.removeLastOrNull()
    if (previousNavigationId != null) {
        // Swap in the new target frame
        navigationHistory.add(NodeQuery.NodeId(targetNodeId))
        // Remember how to undo this if we will need to.
        if (undoInstanceId != null) {
            undoMemory[undoInstanceId] = DeferredAction.SwapNavigation(previousNavigationId)
        }

        invalNavOverlay()
        return
    }
    Log.i(
        TAG,
        "Unable to swap overlay or navigation because no overlays are active and the navigation stack is empty."
    )
}

/// Perform a "ChangeTo" action, replacing whatever the component instance was rendering with the
/// specified node id. An optional key is used to differentiate multiple component instances that
/// share the same instanceNodeId.
internal fun InteractionState.changeTo(
    instanceNodeId: String,
    key: String?,
    newVariantId: String,
    undoInstanceId: String?
) {
    val varKey = getInstanceIdWithKey(instanceNodeId, key)
    val previousVariant = this.variantMemory.put(varKey, newVariantId)
    if (undoInstanceId != null) {
        val undoKey = getInstanceIdWithKey(undoInstanceId, key)
        this.undoMemory[undoKey] = DeferredAction.ChangeTo(previousVariant)
    }
    invalVariant(instanceNodeId)
}

/// Perform a "Url" action
internal fun InteractionState.openLink(url: String) {
    openLinkCallbacks.forEach { it.openLink(url) }
}

/// Register a open-link callback to be called when a Url action is performed
internal fun InteractionState.registerOpenLinkCallback(callback: OpenLinkCallback) {
    openLinkCallbacks.add(callback)
}

/// Unregister a open-link callback
internal fun InteractionState.unregisterOpenLinkCallback(callback: OpenLinkCallback) {
    openLinkCallbacks.remove(callback)
}

/// Dispatch an interaction. An optional key is used to differentiate multiple component instances
/// that share the same targetInstanceId.
internal fun InteractionState.dispatch(
    action: Action,
    targetInstanceId: String?,
    key: String?,
    undoInstanceId: String?
) {
    when (action) {
        is Action.Back -> this.back()
        is Action.Close -> this.close(undoInstanceId)
        is Action.Url -> this.openLink(action.url)
        is Action.Node ->
            when (action.navigation) {
                is Navigation.NAVIGATE -> {
                    if (action.destination_id.isPresent)
                        this.navigate(action.destination_id.get(), undoInstanceId)
                    else Log.i(TAG, "Unable to dispatch NAVIGATE; missing destination id")
                }
                is Navigation.OVERLAY -> {
                    if (action.destination_id.isPresent)
                        this.overlay(
                            action.destination_id.get(),
                            action.transition.orElse(null),
                            undoInstanceId
                        )
                    else Log.i(TAG, "Unable to dispatch OVERLAY; missing destination id")
                }
                is Navigation.SWAP -> {
                    if (action.destination_id.isPresent)
                        this.swap(
                            action.destination_id.get(),
                            action.transition.orElse(null),
                            undoInstanceId
                        )
                    else Log.i(TAG, "Unable to dispatch SWAP; missing destination id")
                }
                is Navigation.CHANGE_TO -> {
                    if (action.destination_id.isPresent && targetInstanceId != null) {
                        // If animated transitions are supported, and there's an animation on this
                        // action, then queue up the animation and notify.
                        if (action.transition.isPresent && supportAnimations) {
                            // If we already have a transition running for the target instance id
                            // then we need to stop it, and tell the animation system to start the
                            // new transition from the point where the previous one was interrupted.
                            var interruptedId: Int? = null
                            animations.removeIf { anim ->
                                if (anim.instanceNodeId == targetInstanceId) {
                                    this.changeTo(anim.instanceNodeId, anim.key, anim.newVariantId, anim.undoInstanceId)
                                    interruptedId = anim.id
                                    true
                                } else {
                                    false
                                }
                            }
                            animations.add(AnimatedAction(
                                targetInstanceId,
                                key,
                                action.destination_id.get(),
                                undoInstanceId,
                                action.transition.get(),
                                interruptedId,
                                lastAnimationId++,
                            ))
                            invalAnimations()
                        } else {
                            this.changeTo(
                                targetInstanceId,
                                key,
                                action.destination_id.get(),
                                undoInstanceId
                            )
                        }
                    } else {
                        Log.i(
                            TAG,
                            "Unable to dispatch CHANGE_TO; missing instance id or destination id"
                        )
                    }
                }
                else -> Log.i(TAG, "Unsupported node action")
            }
        else -> Log.i(TAG, "Unsupported action")
    }
}

/// Undo a dispatch interaction. An optional key is used to differentiate multiple component
/// instances that share the same undoInstanceId.
internal fun InteractionState.undoDispatch(
    targetNodeId: String?,
    undoInstanceId: String,
    key: String?
) {
    val undoKey = getInstanceIdWithKey(undoInstanceId, key)
    val undoAction = undoMemory.remove(undoKey)
    undoAction?.apply(this, targetNodeId, key)
}

/// Make a clone of this InteractionState, and apply all of the transition values to it. The
/// cloned InteractionState can then be used to generate a tree of the "post transition" world
/// which can be used as a target to transition to.
internal fun InteractionState.clonedWithAnimatedActionsApplied(): InteractionState? {
    if (animations.size == 0) return null
    val deltaInteractionState = InteractionState()
    deltaInteractionState.variantMemory = HashMap(variantMemory)
    deltaInteractionState.navigationHistory = ArrayList(navigationHistory)
    deltaInteractionState.overlayMemory = ArrayList(overlayMemory)
    deltaInteractionState.undoMemory = HashMap(undoMemory)
    // Apply all of our transition actions.
    for (anim in animations) {
        deltaInteractionState.changeTo(
            instanceNodeId = anim.instanceNodeId,
            key = anim.key,
            newVariantId = anim.newVariantId,
            undoInstanceId = anim.undoInstanceId
        )
    }
    return deltaInteractionState
}

internal fun InteractionState.invalNavOverlay() {
    // Clone the list while we iterate it to avoid any reentrancy issues.
    for (sub in navOverlaySubscriptions.toList()) {
        sub()
    }
}

internal fun InteractionState.invalVariant(id: String) {
    // Just for squoosh, because it doesn't subscribe to individual variant invals; this will
    // completely break the performance of regular DC.
    this.invalNavOverlay()

    val list = variantSubscriptions[id] ?: return
    for (sub in list.toList()) {
        sub()
    }
}

internal fun InteractionState.invalAnimations() {
    for (sub in animationSubscriptions.toList()) {
        sub()
    }
}

/// A node can be indexed by a NodeId, NodeName, or NodeVariant query, so if we don't find the node
/// using the query, it may still be present but indexed using the variant map if the node is a
/// variant, or by ID (if queried by name) or name (if queried by ID). So we do a linear search if
/// the hash lookup and variant lookup fails.
private fun searchNodes(
    q: NodeQuery,
    nodes: Map<NodeQuery, View>,
    parentViewMap: HashMap<String, HashMap<String, View>>,
    variantPropertyMap: VariantPropertyMap
): View? {
    nodes[q]?.let {
        return it
    }

    if (q is NodeQuery.NodeVariant) {
        val nodeName = q.field0
        val parentNodeName = q.field1
        val variantViewMap = parentViewMap[parentNodeName]
        if (variantViewMap != null) {
            // Using the properties and variant names in the node name, try to find a view that
            // matches
            val view = variantPropertyMap.resolveVariantNameToView(nodeName, variantViewMap)
            if (view != null) return view
        }
    }

    for (node in nodes.values) {
        when (q) {
            is NodeQuery.NodeId ->
                if (node.id == q.value) {
                    return node
                }
            is NodeQuery.NodeName -> {
                if (node.name == q.value) {
                    return node
                }
            }
        }
    }
    return null
}

@Composable
internal fun InteractionState.rootNode(
    initialNode: NodeQuery,
    doc: DocContent,
    isRoot: Boolean
): View? {
    val findRootNode = {
        if (isRoot) {
            navigationHistory.lastOrNull() ?: initialNode
        } else {
            initialNode
        }
    }
    val (query, setQuery) = remember { mutableStateOf(findRootNode()) }
    val updateQuery = { setQuery(findRootNode()) }

    DisposableEffect(initialNode, doc.c.docId, isRoot) {
        updateQuery()
        navOverlaySubscriptions.add(updateQuery)
        onDispose { navOverlaySubscriptions.remove(updateQuery) }
    }
    return searchNodes(query, doc.c.document.views, doc.c.variantViewMap, doc.c.variantPropertyMap)
}

@Composable
internal fun InteractionState.rootOverlays(doc: DocContent): List<View> {
    // findRootOverlays maps from our overlayMemory to node queries.
    val findRootOverlays = { overlayMemory.mapNotNull { nodeId -> NodeQuery.NodeId(nodeId) } }
    val (rootOverlays, setRootOverlays) = remember { mutableStateOf(findRootOverlays()) }
    val updateOverlays = { setRootOverlays(findRootOverlays()) }

    DisposableEffect(doc.c.docId) {
        navOverlaySubscriptions.add(updateOverlays)
        onDispose { navOverlaySubscriptions.remove(updateOverlays) }
    }

    // We really want to do the node lookup here, and not in any remembered block
    // because the incoming doc could have changed (and we want to immediately return
    // the latest doc nodes, rather than causing an invalidation here and returning
    // an updated value later).
    return rootOverlays.mapNotNull { query ->
        searchNodes(query, doc.c.document.views, doc.c.variantViewMap, doc.c.variantPropertyMap)
    }
}

/// Hacky hack to give squoosh something to subscribe to that invalidates when the variant
/// memory changes.
@Composable
internal fun InteractionState.squooshVariantMemory(doc: DocContent): Map<String, String> {
    val (vm, setVm) = remember { mutableStateOf(variantMemory.toMap()) }
    val updateVm = { setVm(variantMemory.toMap())}

    DisposableEffect(doc.c.docId) {
        navOverlaySubscriptions.add(updateVm)
        onDispose { navOverlaySubscriptions.remove(updateVm) }
    }

    return vm
}

/// Hacky hack to give squoosh something to subscribe to for transitions.
@Composable
internal fun InteractionState.squooshAnimatedActions(doc: DocContent): List<AnimatedAction> {
    val (anims, setAnims) = remember { mutableStateOf(animations.toList()) }
    val updateAnims = { setAnims(animations.toList()) }

    DisposableEffect(doc.c.docId) {
        animationSubscriptions.add(updateAnims)
        onDispose { animationSubscriptions.remove(updateAnims) }
    }

    return anims
}

/// An animated action completed successfully.
internal fun InteractionState.squooshCompleteAnimatedAction(transition: AnimatedAction) {
    if (!animations.remove(transition)) return
    changeTo(
        instanceNodeId = transition.instanceNodeId,
        key = transition.key,
        newVariantId = transition.newVariantId,
        undoInstanceId = transition.undoInstanceId
    )
    invalAnimations()
}

/// An animated action failed; log it and advance the interaction state appropriately.
internal fun InteractionState.squooshFailedAnimatedAction(transition: AnimatedAction) {
    Log.w(TAG, "Failed to complete animated action on: ${transition.instanceNodeId}")
    squooshCompleteAnimatedAction(transition)
}

/// Find the variant to use for the specified instanceId, in case a CHANGE_TO interaction has
/// modified it. An optional key is used to differentiate multiple component instances that share
/// the same instanceId.
@Composable
internal fun InteractionState.nodeVariant(
    instanceId: String,
    key: String?,
    doc: DocContent
): View? {
    val varKey = getInstanceIdWithKey(instanceId, key)
    val (variant, setVariant) = remember(instanceId) { mutableStateOf(variantMemory[varKey]) }
    val updateVariant = { setVariant(variantMemory[varKey]) }

    DisposableEffect(instanceId) {
        val subList = variantSubscriptions[instanceId] ?: ArrayList()
        subList.add(updateVariant)
        variantSubscriptions[instanceId] = subList
        onDispose {
            val subSubList = variantSubscriptions[instanceId]
            if (subSubList != null) {
                subSubList.remove(updateVariant)
                if (subSubList.size == 0) variantSubscriptions.remove(instanceId)
            }
        }
    }

    if (variant == null) return null
    return searchNodes(
        NodeQuery.NodeId(variant),
        doc.c.document.views,
        doc.c.variantViewMap,
        doc.c.variantPropertyMap
    )
}

internal fun InteractionState.squooshNodeVariant(
    instanceId: String,
    key: String?,
    doc: DocContent
): View?
{
    val varKey = getInstanceIdWithKey(instanceId, key)
    val variant = variantMemory[varKey] ?: return null
    return searchNodes(
        NodeQuery.NodeId(variant),
        doc.c.document.views,
        doc.c.variantViewMap,
        doc.c.variantPropertyMap
    )
}
internal fun InteractionState.squooshRootNode(
    initialNode: NodeQuery,
    doc: DocContent,
    isRoot: Boolean
): View? {
    val findRootNode = {
        if (isRoot) {
            navigationHistory.lastOrNull() ?: initialNode
        } else {
            initialNode
        }
    }
    val query = findRootNode()
    return searchNodes(query, doc.c.document.views, doc.c.variantViewMap, doc.c.variantPropertyMap)
}

/// InteractionState is managed in a global, per document. We don't pass it down via a
/// LocalComposition, because we want to support usage models where there are multiple
/// roots (e.g.: many ComposeViews are used to mix design elements in with existing
/// Android Views, which are all included in some root Compose view tree). So we just track
/// InteractionState as a global ensuring that views can access the correct state regardless
/// of the view tree organization.
internal object InteractionStateManager {
    val states: HashMap<String, InteractionState> = HashMap()
}

internal fun InteractionStateManager.stateForDoc(docId: String): InteractionState {
    return states.getOrPut(docId) { InteractionState() }
}
