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
import com.android.designcompose.CustomizationContext
import com.android.designcompose.serdegen.Bezier
import com.android.designcompose.serdegen.Transition

// We want to perform an animated transition when a variant customization causes a presented
// variant to change (but only when the component has an animation configured).
//
// The incoming customization context has a variant selection (property -> value) which is
// matched against all components in the render tree, in case one of those components has the
// same property.
//
// Since the incoming customization context always has the "true" values of the variant properties,
// the SquooshRoot has to remember what it previously rendered in order to construct transitions
// from the previous variant to the current one. If a property that is animated is changing rapidly
// then for each update, we need to cancel any previous animations on that property. If the
// property is oscillating between two values then we can re-use the current position as the start
// point for the new animation.
//
// So what do we need to track?
//  - When we resolveVariantsRecursively, we check to see if we rendered a different variant on
//    the previous cycle, and if the component has a transition defined between those two variants.
//  - If so, resolveVariantsRecursively makes a record that we should do a transition and records
//    the transition object from the design.
//  - Once resolveVariantsRecurisvely has completed, we remember the variants in the customization
//    context, so that we can find the transitions for the next update.
//  - Once we have a list of the SquooshResolvedNodes that need to be transitioned, we walk it,
//    cancel any transitions that are on the same nodes/properties, and create the "from", "to"
//    that we need for the actual transition.
//  - Now we can enter the transition logic in SquooshRoot, and build a new tree with all of the
//    destination nodes, and then run the regular animation code.
//
// The code in this file has utilities to do the following things:

internal class VariantAnimationInfo(
    /// A unique id to use for this animation request
    val id: Int,

    /// The id of the variant animation that was interrupted to create this one, if any.
    val interruptedId: Int?,

    /// The node id of the component in the tree that got varied by a customization.
    val nodeId: String,

    /// The node id that we're transitioning from. This is the same as the nodeId when
    /// we're doing a transition from the default state, otherwise it could be any two
    /// node ids of components in the variant set.
    val fromNodeId: String,

    /// The node id that we're transitioning to.
    val toNodeId: String,

    /// The details on the transition that we are running.
    val transition: Transition
)

// Sometimes we have a "null" variantNodeName, and we need to remember that and detect
// it next time. HashMap can't store "null" as a value
private const val NullNodeName = "<<null>>"

// Which phase are we in? On the base phase, we have to look at the variant selections,
// compare them with the previous phase, and if there's a transition from the old state
// to the new one then we need to render the old variant in the first phase and ask for
// a transition render phase.
internal enum class TreeBuildPhase {
    BasePhase,
    TransitionTargetPhase,
}

internal class SquooshVariantTransition {
    // First phase render: (id -> variant node name) for ongoing transitions.
    // First phase render: (id -> variant node name) for the last round, so we can
    //                     see if this round is different.
    //
    // List of transitions that need to be started or continued for the second phase.

    /// This maps from the main component node id to the active transition. In the first phase
    /// we will always return
    internal val transitions: HashMap<String, VariantAnimationInfo> = HashMap()
        get() = field

    /// This map has the "from id -> variant node name" mapping from the previous render. If
    /// we get a different variant replacement id this time then we need to create a transition.
    private var lastState: HashMap<String, String> = HashMap()

    /// This is the map of "from id -> variant node name" for the current render; this data
    /// structure is in the process of being built and is moved into `lastState` as soon as it is
    /// complete.
    private var nextState: HashMap<String, String> = HashMap()

    /// The phase we're building a render tree for.
    internal var treeBuildPhase: TreeBuildPhase = TreeBuildPhase.BasePhase

    /// The set of view ids that we need to create transitions for on this iteration. We'll make
    /// the transitions during the second phase. In the first phase we just record the "from" id
    /// after the variant name has been resolved to a view.
    private val newTransitions: HashMap<String, String> = HashMap()

    /// Some counter of ids for transitions. We count down instead of up, to be unique from the
    /// interaction state ids.
    private var transitionId: Int = -1

    /// resolveVariantsRecursively asks us which variant to render. If we're in the base phase
    /// AND we previously rendered a different variant AND there's a transition defined between
    /// the variants, then we add the transition reocrd to transitions and tell resolveVariants
    /// to use the "from" variant.
    internal fun selectVariant(viewId: String, targetNodeName: String?): String? {
        if (treeBuildPhase == TreeBuildPhase.BasePhase) {
            // Remember this for next time.
            nextState[viewId] = targetNodeName ?: NullNodeName
            // Did we know about this from before?
            val lastVariantName = lastState[viewId]
            // XXX: There might be something that we should do when a node is being transitioned;
            //      currently we'll just fail to generate the correct variants, and the transition
            //      will be removed via `failedAnimatedTransition` below. Ideally we'd be able to
            //      do interruption gracefully if it's between two states.
            if (lastVariantName == targetNodeName || (targetNodeName == null && lastVariantName == NullNodeName) || lastVariantName == null) {
                // Nothing to do; it didn't change.
                Log.d(TAG, "selectVariant base to ${targetNodeName} no change! (given target for ${viewId})")
                return targetNodeName
            }
            // Ok, we've observed a change. If the old variant has a transition to this one
            // then let's use it. We remember the transitions from the old view because otherwise
            // we have to do a much slower lookup. We really shouldn't do this because we are
            // keeping content from one doc iteration to another, so a live update at the same
            // time as a variant enum update could skip a transition or show an old transition.

            // We need the old View, we go through the reactions, find one with a keypress
            // Trigger, and a Node Action that navigates to the new target.

            // But, we don't have any of the plumbing to do that yet. SO, we'll just make *every*
            // variant change a smart animate.

            // We need to record that we're starting a transition, but we aren't ready to say what
            // it is yet, because we don't know the "to" id.
            newTransitions[viewId] = NullNodeName // Use NullNodeName because we don't know the node id yet.

            if (lastVariantName == NullNodeName) {
                Log.d(TAG, "selectVariant old variant on base phase to null (previous was null) for $viewId")
                return null
            }
            Log.d(TAG, "selectVariant base to $lastVariantName for $viewId")
            return lastVariantName
        }

        Log.d(TAG, "selectVariant transition to target $targetNodeName for $viewId")
        // Ok, now we're in the second phase. We always transition *to* the current value from
        // whatever the old value was. So we just return the targetNodeName here.
        return targetNodeName
    }

    /// We only need a second phase if we found new things that need to be transitioned or
    /// if we're currently running some transitions.
    internal fun needsTransitionPhase(): Boolean {
        return newTransitions.isNotEmpty() || transitions.isNotEmpty()
    }

    /// Once a variant has been selected, resolveVariantsRecursively tells us about it.
    internal fun selectedVariant(viewId: String, variantViewId: String) {
        if (treeBuildPhase == TreeBuildPhase.BasePhase) {
            if (newTransitions.contains(viewId)) {
                newTransitions[viewId] = variantViewId // This is the "from" id.
            }
            return
        }
        // If we're planning on doing a transition for this node, then build it now.
        val fromId = newTransitions.remove(viewId)
        if (fromId != null) {
            val toId = variantViewId
            // This will wipe out any existing transition; we probably want to make a smooth
            // function here, like we do for the interaction-based transitions.
            if (toId != fromId) {
                // Record the id of the animation that we interrupted, if any.
                transitions[viewId] = VariantAnimationInfo(
                    id = transitionId--,
                    interruptedId = transitions[viewId]?.id,
                    nodeId = viewId,
                    fromNodeId = fromId,
                    toNodeId = toId,
                    transition = Transition.SmartAnimate(
                        Bezier(0f, 0f, 1f, 1f),
                        1f
                    )
                )
            } else {
                // Hmm! What do we do here? We probably need to remember a different
                // "fromId", but what could that be?

            }
            return
        }
        // If we're already doing a transition for this one, do we need to keep doing it?
    }

    internal fun afterRenderPhases() {
        lastState = nextState
        nextState = HashMap()
    }

    internal fun completedAnimatedVariant(anim: VariantAnimationInfo) {
        transitions.remove(anim.nodeId)
        Log.d(TAG, "completed variant animation: ${anim.nodeId}, now there are ${transitions.size} active transitions")
    }

    internal fun failedAnimatedVariant(anim: VariantAnimationInfo) {
        transitions.remove(anim.nodeId)
        Log.d(TAG, "failed to execute variant anim: ${anim.nodeId}, now there are ${transitions.size} active transitions")
    }
}