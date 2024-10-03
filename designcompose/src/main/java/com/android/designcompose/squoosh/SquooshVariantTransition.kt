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
import androidx.annotation.Discouraged
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.android.designcompose.asAnimationSpec
import com.android.designcompose.serdegen.Easing
import com.android.designcompose.serdegen.EasingType
import com.android.designcompose.serdegen.SmartAnimate
import com.android.designcompose.serdegen.Spring
import com.android.designcompose.serdegen.Transition
import com.android.designcompose.serdegen.TransitionType
import com.android.designcompose.serdegen.View
import java.util.Optional

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

// This interface describes an animation in terms of its easing function, duration, and time to
// delay before starting the animation
interface AnimationTransition {
    // Return the AnimationSpec, which includes the easing function and duration
    fun animationSpec(): AnimationSpec<Float>

    // Return the time to delay before starting the animation, in milliseconds.
    fun delayMillis(): Int
}

// This class is an AnimationTransition that performs a smart animate. A smart animate transition
// compares the source and destination node trees and transitions similar nodes when possible, and
// transitions other nodes with a cross fade.
class SmartAnimateTransition(
    // The AnimationSpec, which includes the easing function and duration
    private val transition: AnimationSpec<Float>,
    // The time to delay before starting the animation, in milliseconds.
    private val delayMillis: Int = 0,
) : AnimationTransition {
    override fun animationSpec(): AnimationSpec<Float> {
        return transition
    }

    override fun delayMillis(): Int {
        return delayMillis
    }
}

val DEFAULT_TRANSITION =
    SmartAnimateTransition(
        Transition(
                // Too many Optional.ofs. Will go away when we get off Serdegen.
                Optional.of(
                    TransitionType.SmartAnimate(
                        SmartAnimate(
                            Optional.of(
                                Easing(Optional.of(EasingType.Spring(Spring(1.0f, 200.0f, 30.0f))))
                            ),
                            1f,
                        )
                    )
                )
            )
            .asAnimationSpec()
    )

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

    /// The variant name we're transitioning from.
    val fromName: String?,

    /// The variant name we're transitioning to.
    val toName: String?,

    /// The details on the transition that we are running.
    val transition: AnimationTransition,
)

class VariantTransitionContext(val from: View?, val to: View?) {
    @Discouraged(
        message =
            "This helper function will return true if the transition is within a variant from the " +
                "specified component set. This function is intended to temporarily help with" +
                "creating custom variant transitions, but may be deprecated or removed in the future."
    )
    fun fromComponentSet(componentSetName: String): Boolean {
        if (
            from?.component_info?.isPresent == true &&
                from.component_info.get().component_set_name == componentSetName
        )
            return true
        if (
            to?.component_info?.isPresent == true &&
                to.component_info.get().component_set_name == componentSetName
        )
            return true
        return false
    }

    @Discouraged(
        message =
            "This helper function will return true if the transition is within a variant with the " +
                "given variant property name. This function is intended to temporarily help with " +
                "creating custom variant transitions, but may be deprecated or removed in the future."
    )
    fun hasVariantProperty(propertyName: String): Boolean {
        if (
            from?.component_info?.isPresent == true &&
                from.component_info.get().name.contains("$propertyName=")
        )
            return true
        if (
            to?.component_info?.isPresent == true &&
                to.component_info.get().name.contains("$propertyName=")
        )
            return true
        return false
    }
}

typealias CustomVariantTransition = (context: VariantTransitionContext) -> AnimationTransition?

// We make a note that we saw a node in the first phase, before we populate it with details
// in the second phase.
private const val PendingNodeName = "<<pending>>"

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

    /// Did we add or update transitions on the most recent pass? If so, we might need to invalidate
    /// the list of transitions that our clients are listening to.
    private var didUpdateTransitions = false

    /// resolveVariantsRecursively asks us which variant to render. If we're in the base phase
    /// AND we previously rendered a different variant AND there's a transition defined between
    /// the variants, then we add the transition reocrd to transitions and tell resolveVariants
    /// to use the "from" variant.
    internal fun selectVariant(viewId: String, targetNodeName: String): String? {
        if (treeBuildPhase == TreeBuildPhase.BasePhase) {
            // Remember this for next time.
            nextState[viewId] = targetNodeName
            // Did we know about this from before?
            val lastVariantName = lastState[viewId]

            // If we're currently transitioning this view, then if the most recent update didn't
            // change the targetNodeName, we still want to run the transition.
            val transition = transitions[viewId]
            if (transition != null && targetNodeName == transition.toName)
                return transition.fromName

            // If this variant hasn't changed since the last render then just let it continue to
            // be.
            if (lastVariantName == targetNodeName || lastVariantName == null) {
                // Nothing to do; it didn't change.
                return targetNodeName
            }

            // We can filter the variant transitions that we animate on here, if desired. We have
            // the keywords in `lastVariantName` and `targetNodeName`, so we can see what changed.

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
            newTransitions[viewId] =
                PendingNodeName // Use NullNodeName because we don't know the node id yet.

            return lastVariantName
        }

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
    internal fun selectedVariant(
        view: View,
        variantView: View,
        customVariantTransition: CustomVariantTransition?,
    ) {
        val viewId = view.id
        val variantViewId = variantView.id
        if (treeBuildPhase == TreeBuildPhase.BasePhase) {
            if (newTransitions.contains(viewId)) {
                newTransitions[viewId] =
                    variantViewId // This is the "from" id, replacing PendingNodeName
            }
            return
        }
        // If we're planning on doing a transition for this node, then build it now.
        val fromId = newTransitions.remove(viewId)
        if (fromId != null && fromId != PendingNodeName) {
            val toId = variantViewId

            // Remember these in the transition; when a transition is running we want to
            // keep it going, even if there are other changes going on (due to re-rendering
            // from some other value changing, for example).
            val fromName = lastState[viewId]
            val toName = nextState[viewId]

            // This will wipe out any existing transition; we probably want to make a smooth
            // function here, like we do for the interaction-based transitions.
            if (toId != fromId) {
                val transition =
                    customVariantTransition?.let { it(VariantTransitionContext(view, variantView)) }
                        ?: DEFAULT_TRANSITION
                // Record the id of the animation that we interrupted, if any.
                transitions[viewId] =
                    VariantAnimationInfo(
                        id = transitionId--,
                        interruptedId = transitions[viewId]?.id,
                        nodeId = viewId,
                        fromNodeId = fromId,
                        toNodeId = toId,
                        fromName = fromName,
                        toName = toName,
                        transition = transition,
                    )
                didUpdateTransitions = true
            } else {
                // Hmm! What do we do here? We probably need to remember a different
                // "fromId", but what could that be?
                Log.w(TAG, "Not recording a transition for $viewId - no from id")
            }
            return
        }
        // If we're already doing a transition for this one, do we need to keep doing it?
    }

    internal fun afterRenderPhases() {
        lastState = nextState
        nextState = HashMap()
        newTransitions.clear()
        if (didUpdateTransitions) invalTransitions()
        didUpdateTransitions = false
    }

    internal fun completedAnimatedVariant(anim: VariantAnimationInfo) {
        transitions.remove(anim.nodeId)
        Log.d(
            TAG,
            "completed variant animation: ${anim.nodeId}, now there are ${transitions.size} active transitions",
        )
        invalTransitions()
    }

    internal fun failedAnimatedVariant(anim: VariantAnimationInfo) {
        transitions.remove(anim.nodeId)
        newTransitions.remove(anim.nodeId)
        Log.d(
            TAG,
            "failed to execute variant anim: ${anim.nodeId}, now there are ${transitions.size} active transitions",
        )
        invalTransitions()
    }

    private val transitionSubscriptions: ArrayList<() -> Unit> = ArrayList()

    @Composable
    internal fun transitions(): List<VariantAnimationInfo> {
        val (txs, setTxs) = remember { mutableStateOf(transitions.values.toList()) }
        val updateTxs = { setTxs(transitions.values.toList()) }

        DisposableEffect(0) {
            transitionSubscriptions.add(updateTxs)
            onDispose { transitionSubscriptions.remove(updateTxs) }
        }

        return txs
    }

    private fun invalTransitions() {
        for (sub in transitionSubscriptions.toList()) {
            sub()
        }
    }
}
