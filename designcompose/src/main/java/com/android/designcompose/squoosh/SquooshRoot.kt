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

import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.android.designcompose.AnimatedAction
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DebugNodeManager
import com.android.designcompose.DesignComposeCallbacks
import com.android.designcompose.DesignSettings
import com.android.designcompose.DesignSwitcher
import com.android.designcompose.DesignSwitcherPolicy
import com.android.designcompose.DocRenderStatus
import com.android.designcompose.DocServer
import com.android.designcompose.DocumentSwitcher
import com.android.designcompose.InteractionStateManager
import com.android.designcompose.LiveUpdateMode
import com.android.designcompose.LocalDesignDocSettings
import com.android.designcompose.VariableState
import com.android.designcompose.asBuilder
import com.android.designcompose.branches
import com.android.designcompose.clonedWithAnimatedActionsApplied
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.common.DocumentServerParams
import com.android.designcompose.doc
import com.android.designcompose.rootNode
import com.android.designcompose.rootOverlays
import com.android.designcompose.sDocRenderStatus
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.serdegen.Size
import com.android.designcompose.squooshAnimatedActions
import com.android.designcompose.squooshCompleteAnimatedAction
import com.android.designcompose.squooshFailedAnimatedAction
import com.android.designcompose.squooshVariantMemory
import com.android.designcompose.stateForDoc
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val TAG: String = "DC_SQUOOSH"

// We want to provide the node to a Compose layout customization, and we do that using
// the ParentDataModifier.
private data class SquooshParentData(val node: SquooshResolvedNode) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any {
        return this@SquooshParentData
    }
}

// We need to share some information on animations with the `squooshRender` Modifier so
// that it can apply the correct values.
internal class SquooshAnimationRenderingInfo(
    var control: SquooshAnimationControl,
    val animation: TargetBasedAnimation<Float, AnimationVector1D>,
    val action: AnimatedAction?,
    val variant: VariantAnimationInfo?,
    var startTimeNanos: Long = 0L,
)

/// Apply layout constraints to a node; this is only used for the root node and gives the DC
/// layout system the context of what it is being embedded in.
private fun SquooshResolvedNode.applyLayoutConstraints(constraints: Constraints, density: Density) {
    val rootStyleBuilder = style.asBuilder()
    val layoutStyleBuilder = style.layout_style.asBuilder()
    if (constraints.minWidth != 0)
        layoutStyleBuilder.min_width =
            Dimension.Points(constraints.minWidth.toFloat() / density.density)
    if (constraints.maxWidth != Constraints.Infinity)
        layoutStyleBuilder.max_width =
            Dimension.Points(constraints.maxWidth.toFloat() / density.density)
    if (constraints.hasFixedWidth) {
        layoutStyleBuilder.width =
            Dimension.Points(constraints.minWidth.toFloat() / density.density)
        // Layout implementation looks for width/height being set and then uses bounding box.
        layoutStyleBuilder.bounding_box =
            Size(
                constraints.minWidth.toFloat() / density.density,
                layoutStyleBuilder.bounding_box.height
            )
    }
    if (constraints.minHeight != 0)
        layoutStyleBuilder.min_height =
            Dimension.Points(constraints.minHeight.toFloat() / density.density)
    if (constraints.maxHeight != Constraints.Infinity)
        layoutStyleBuilder.max_height =
            Dimension.Points(constraints.maxHeight.toFloat() / density.density)
    if (constraints.hasFixedHeight) {
        layoutStyleBuilder.height =
            Dimension.Points(constraints.minHeight.toFloat() / density.density)
        // Layout implementation looks for width/height being set and then uses bounding box.
        layoutStyleBuilder.bounding_box =
            Size(
                layoutStyleBuilder.bounding_box.width,
                constraints.minHeight.toFloat() / density.density
            )
    }
    rootStyleBuilder.layout_style = layoutStyleBuilder.build()
    style = rootStyleBuilder.build()
}

// Mutable field to remember animation information that is computed during the layout phase which
// shouldn't invalidate anything as it mutates.
internal class AnimationValueHolder(
    var animationJob: Job?,
)

// Track if we're the root component, or (in the near future) if we are a list item so that we can
// share information across similar list items.
internal data class SquooshIsRoot(val isRoot: Boolean)

internal val LocalSquooshIsRootContext = compositionLocalOf { SquooshIsRoot(true) }

// Experiment -- minimal DesignCompose root node; no switcher, no interactions, etc.
@Composable
fun SquooshRoot(
    docName: String,
    incomingDocId: DesignDocId,
    rootNodeQuery: NodeQuery,
    modifier: Modifier = Modifier,
    customizationContext: CustomizationContext = CustomizationContext(),
    serverParams: DocumentServerParams = DocumentServerParams(),
    setDocId: (DesignDocId) -> Unit = {},
    designSwitcherPolicy: DesignSwitcherPolicy = DesignSwitcherPolicy.SHOW_IF_ROOT,
    liveUpdateMode: LiveUpdateMode = LiveUpdateMode.LIVE,
    designComposeCallbacks: DesignComposeCallbacks? = null,
) {
    // Basic init and doc loading.
    val isRoot = LocalSquooshIsRootContext.current.isRoot
    val docId = DocumentSwitcher.getSwitchedDocId(incomingDocId)
    val doc =
        DocServer.doc(
            docName,
            docId,
            serverParams,
            designComposeCallbacks?.newDocDataCallback,
            liveUpdateMode == LiveUpdateMode.OFFLINE
        )

    LaunchedEffect(docName) { Log.i(TAG, "Squooshing $docName") }

    // Design Switcher support
    val showDesignSwitcher =
        isRoot &&
            designSwitcherPolicy == DesignSwitcherPolicy.SHOW_IF_ROOT &&
            DesignSettings.liveUpdatesEnabled

    val originalDocId = remember {
        DocumentSwitcher.subscribe(docId, setDocId)
        docId
    }
    val switchDocId: (DesignDocId) -> Unit = { newDocId: DesignDocId ->
        run { DocumentSwitcher.switch(originalDocId, newDocId) }
    }

    val designSwitcher: @Composable () -> Unit = {
        if (showDesignSwitcher) {
            val branchHash = DocServer.branches(docId)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                DesignSwitcher(doc, docId, branchHash, switchDocId)
            }
        }
    }

    if (doc == null) {
        Log.d(TAG, "No doc! $docName / $incomingDocId")
        designSwitcher()
        return
    }

    val interactionState = InteractionStateManager.stateForDoc(docId)

    // We're starting to support animated transitions
    interactionState.supportAnimations = true

    val layoutManager = remember(docId) { SquooshLayout.newLayoutManager() }

    val startFrame =
        interactionState.rootNode(
            initialNode = rootNodeQuery,
            doc = doc,
            isRoot = isRoot,
            customizationContext
        )

    if (startFrame == null) {
        Log.d(TAG, "No start frame $docName / $incomingDocId")
        SquooshLayout
            .keepJniBits() // XXX: Must call this from somewhere otherwise it gets stripped and the
        // jni lib won't link.
        return
    }
    LaunchedEffect(docId) { designComposeCallbacks?.docReadyCallback?.invoke(docId) }

    // Ensure we get invalidated when the variant memory is updated from an interaction.
    interactionState.squooshVariantMemory(doc)
    val overlays =
        if (isRoot || designSwitcherPolicy == DesignSwitcherPolicy.IS_DESIGN_SWITCHER) {
            interactionState.rootOverlays(doc)
        } else {
            null
        }

    val density = LocalDensity.current
    val fontLoader = LocalFontLoader.current

    val variantParentName =
        when (rootNodeQuery) {
            is NodeQuery.NodeVariant -> rootNodeQuery.field1
            else -> ""
        }

    val layoutIdAllocator = remember { SquooshLayoutIdAllocator() }
    val rootLayoutId = remember(docId) { SquooshLayout.getNextLayoutId() * 100000000 }
    val layoutCache = remember(docId) { HashMap<Int, Int>() }
    val layoutValueCache = remember(docId) { HashMap<Int, Layout>() }

    // We need to remember the previous set of variant properties that we rendered
    // with so we can see if there are any transitions caused by changing variant props.
    val variantTransitions = remember(docId) { SquooshVariantTransition() }
    variantTransitions.treeBuildPhase = TreeBuildPhase.BasePhase

    // Bogus -- needs refactor -- this ensures we don't have a bad frame at the end of
    // an enum transition (we generate a new tree with the enum change fully applied).
    val bogusUnusedVariantTransitions = variantTransitions.transitions()

    // Ok, now we have done the dull stuff, we need to build a tree applying
    // the correct variants etc and then build/update the tree. How do we know
    // what's different from last time? Does the Rust side track
    val childComposables: ArrayList<SquooshChildComposable> = arrayListOf()
    val useLocalStringRes: Boolean? by DebugNodeManager.getUseLocalStringRes().observeAsState()
    val root =
        resolveVariantsRecursively(
            startFrame,
            rootLayoutId,
            doc,
            customizationContext,
            variantTransitions,
            interactionState,
            null,
            density,
            fontLoader,
            childComposables,
            layoutIdAllocator,
            variantParentName,
            isRoot,
            VariableState.create(),
            overlays,
            appContext = LocalContext.current,
            useLocalStringRes = useLocalStringRes,
            LocalDesignDocSettings.current.customVariantTransition,
        ) ?: return
    val rootRemovalNodes = layoutIdAllocator.removalNodes()

    // Get the list of actions that currently need to be animated from the interaction state.
    val animatedActions = interactionState.squooshAnimatedActions(doc)

    // We maintain a list of animations (animation id, info) that we're currently running so
    // that we can just update the key info when recomposing mid-animation.
    val currentAnimations = remember { HashMap<Int, SquooshAnimationRenderingInfo>() }

    // Animation timecodes go into a `MutableState`. This way we can modify them in a coroutine
    // and only invalidate rendering. So animation progression doesn't cause or require
    // recomposition, resulting in a good performance uplift.
    val animationValues: MutableState<Map<Int, Float>> = remember { mutableStateOf(mapOf()) }

    // Get the interaction state with all of the animated actions applied. We use this to generate
    // the tree with all actions applied (which is then used as the "dest" in the animation).
    val transitionedInteractionState = interactionState.clonedWithAnimatedActionsApplied()

    var transitionRoot: SquooshResolvedNode? = null
    var transitionRootRemovalNodes: Set<Int>? = null
    // Now see if we need to compute a transition root and generate animated transitions.
    if (
        (transitionedInteractionState != null && animatedActions.isNotEmpty()) ||
            variantTransitions.needsTransitionPhase()
    ) {
        variantTransitions.treeBuildPhase = TreeBuildPhase.TransitionTargetPhase
        Log.d(
            TAG,
            "$docName: creating a new root with transitions applied... ${variantTransitions.transitions.size} variant transitions; variant needs second phase: ${variantTransitions.needsTransitionPhase()} (${variantTransitions.transitions.size} transitions); ${animatedActions.size} animated actions;"
        )

        // We need to make a new root with this interaction state applied, and then compute the
        // animation control between the trees.
        childComposables.clear()
        transitionRoot =
            resolveVariantsRecursively(
                startFrame,
                rootLayoutId,
                doc,
                customizationContext,
                variantTransitions,
                transitionedInteractionState ?: interactionState,
                null,
                density,
                fontLoader,
                childComposables,
                layoutIdAllocator,
                variantParentName,
                isRoot,
                VariableState.create(),
                overlays,
                appContext = LocalContext.current,
                useLocalStringRes = useLocalStringRes,
                LocalDesignDocSettings.current.customVariantTransition,
            )
        transitionRootRemovalNodes = layoutIdAllocator.removalNodes()
    }

    variantTransitions.afterRenderPhases()

    var presentationRoot = root

    if (transitionRoot != null) {
        // Now that we've created a "base tree" with no transitions/interactions applied, and a
        // "transition tree" that has all transitions/interactions applied, we can create a new
        // tree that combines both.
        //
        // The combined tree clones nodes from the base tree up to where a node that has a
        // transition
        // or interaction applied exists. Then it does some work to either have parts of both trees
        // or combined nodes. There are some animation control objects created that can take a value
        // between 0..1 and update styles and layouts in the combined tree.
        //
        // The combined tree doesn't have working layout, instead we perform layout on both of the
        // source trees and then populate it into the combined tree.
        // Record all of the animations in progress or to be started in
        // `nextAnimations`.
        val nextAnimations = HashMap<Int, SquooshAnimationRenderingInfo>()

        // Make a map of "from ids" to "to ids", and then create all of the
        // AnimationControls in one go, resulting in a new tree that can be used for
        // drawing.
        val requestedAnimations = HashMap<String, SquooshAnimationRequest>()

        for (animatedAction in animatedActions) {
            requestedAnimations[animatedAction.instanceNodeId] =
                SquooshAnimationRequest(
                    toNodeId = animatedAction.newVariantId,
                    animationId = animatedAction.id,
                    interruptedId = animatedAction.interruptedId,
                    transition = animatedAction.transition,
                    action = animatedAction,
                    variant = null
                )
        }
        for (variantTransition in variantTransitions.transitions.values) {
            requestedAnimations[variantTransition.fromNodeId] =
                SquooshAnimationRequest(
                    toNodeId = variantTransition.toNodeId,
                    animationId = variantTransition.id,
                    interruptedId = variantTransition.interruptedId,
                    transition = variantTransition.transition,
                    action = null,
                    variant = variantTransition
                )
        }

        // Update the root that we're rendering.
        presentationRoot = createMergedAnimationTree(root, transitionRoot, requestedAnimations)

        // Now create Compose animations for all of the requestedAnimations that
        // have animation controls populated.
        for (animationRequest in requestedAnimations.values) {
            val animationControl = animationRequest.animationControl
            if (animationControl == null) {
                if (animationRequest.action != null)
                    interactionState.squooshFailedAnimatedAction(animationRequest.action)
                if (animationRequest.variant != null)
                    variantTransitions.failedAnimatedVariant(animationRequest.variant)
                continue
            }

            // Now that we have an animation control instance, we can create or update the animation
            // information that we advance in a Compose coroutine. We handle three cases here:
            //
            //  - this is a new animation: record that it's starting with the appropriate
            // curve/duration
            //  - this is a new animation, interrupting another on this node: start the new
            // animation from
            //    the current/interrupted animation position.
            //  - this is an existing animation, just continue with it (but using new nodes in the
            // new tree)
            val animationRenderingInfo = currentAnimations[animationRequest.animationId]
            if (animationRenderingInfo == null) {
                // If this transition interrupted another one operating on the same element, then
                // sample the position of the interrupted animation.
                // XXX: This won't be correct for more than two states.
                val initialValue: Float =
                    if (animationRequest.interruptedId != null) {
                        // XXX: This is a read of volatile animation state during Composition; this
                        //      means we will do a recompose on the next animation frame. We might
                        //      need a shadow cache of last animation values that Compose isn't
                        //      tracking to avoid this.
                        val interruptedAnimation =
                            animationValues.value[animationRequest.interruptedId]
                        interruptedAnimation ?: 1f
                    } else {
                        1f
                    }
                val animatable =
                    TargetBasedAnimation(
                        animationSpec = animationRequest.transition.animationSpec(),
                        typeConverter = Float.VectorConverter,
                        initialValue = 1f - initialValue,
                        targetValue = 1f
                    )
                nextAnimations[animationRequest.animationId] =
                    SquooshAnimationRenderingInfo(
                        control = animationControl,
                        animation = animatable,
                        action = animationRequest.action,
                        variant = animationRequest.variant
                    )
            } else {
                // Update the control with one that knows about new nodes.
                animationRenderingInfo.control = animationControl
                nextAnimations[animationRequest.animationId] = animationRenderingInfo
            }
        }

        // Evolve the animations list; it would be better to just remove everything that
        // doesn't
        // have a key in transitions...
        currentAnimations.clear()
        currentAnimations.putAll(nextAnimations)

        Log.d(TAG, "Updating animations resulted in ${nextAnimations.size} animations")
    }

    // The presentationRoot tree doesn't have any layout values. It has to copy them all
    // from the two trees it was based on.

    // We run the animation in a coroutine; remember the job so we can see if the coroutine is
    // still running.
    val animationJob: AnimationValueHolder = remember { AnimationValueHolder(null) }

    // Run interactions in a dedicated scope. This means that events continue to be handled by a
    // given coroutine after its hosting Composable has been removed from the tree. Some
    // interactions (e.g.: "close an overlay, while pressed" applied to the overlay itself) will
    // result in the hosting Composable being removed from the tree on touch start, but the
    // event handler still wants to receive the drag (for press-cancel) and release events in
    // order to run the appropriate "undo" action.
    //
    // We also run animated transitions in this coroutine scope.
    val interactionScope = rememberCoroutineScope()

    // Select which child to draw using this holder.
    val childRenderSelector = SquooshChildRenderSelector()

    CompositionLocalProvider(LocalSquooshIsRootContext provides SquooshIsRoot(false)) {
        androidx.compose.ui.layout.Layout(
            modifier =
                modifier
                    .squooshRender(
                        presentationRoot,
                        doc,
                        docName,
                        customizationContext,
                        childRenderSelector,
                        // Is there a nicer way of passing these two?
                        currentAnimations,
                        animationValues,
                        VariableState.create(),
                    )
                    .semantics { sDocRenderStatus = DocRenderStatus.Rendered },
            measurePolicy = { measurables, constraints ->
                // Update the root node style to have the incoming width/height from our parent
                // Composable.
                root.applyLayoutConstraints(constraints, density)

                // Perform layout on our resolved tree.
                layoutTree(
                    root,
                    layoutManager,
                    rootLayoutId,
                    rootRemovalNodes,
                    layoutCache,
                    layoutValueCache
                )

                // If we have a transition root, then lay it out and compute any animations that
                // are needed.
                //
                // Yuck - we should really not be destructive of the tree when creating animations,
                //        then we could still perform layout on both trees during a transition.
                val tRoot = transitionRoot
                if (tRoot != null && transitionRootRemovalNodes != null) {
                    // Ensure that the transition target also has the incoming width/height applied.
                    tRoot.applyLayoutConstraints(constraints, density)

                    // Layout the transition root tree.
                    layoutTree(
                        tRoot,
                        layoutManager,
                        rootLayoutId,
                        transitionRootRemovalNodes,
                        layoutCache,
                        layoutValueCache
                    )
                    updateDerivedLayout(presentationRoot)
                }

                // Move this stuff out of layout?
                val currentAnimationJob = animationJob.animationJob
                val needsAnimationJob =
                    interactionState.animations.isNotEmpty() ||
                        variantTransitions.transitions.isNotEmpty()
                val hasAnimationJob = currentAnimationJob != null && currentAnimationJob.isActive
                if (needsAnimationJob && !hasAnimationJob) {
                    animationJob.animationJob =
                        interactionScope.launch(start = CoroutineStart.UNDISPATCHED) {
                            // While there are transitions to be run, we should run them; we just
                            // update the floats
                            // in the mutable state. Those are then used by the render function, and
                            // we thus avoid
                            // needing to recompose in order to propagate the animation state.
                            //
                            // We also complete transitions in this block, and that action does
                            // cause a recomposition
                            // via the subscription that SquooshRoot makes to the InteractionState's
                            // list of transitions.
                            while (
                                interactionState.animations.isNotEmpty() ||
                                    variantTransitions.transitions.isNotEmpty()
                            ) {
                                // withFrameNanos runs its closure outside of this recomposition
                                // phase -- because the
                                // body of the code only changes mutable state which is only used at
                                // render time,
                                // Compose only re-runs the render block.
                                withFrameNanos { frameTimeNanos ->
                                    val animState = HashMap(animationValues.value)
                                    val animsToRemove = HashSet<Int>()
                                    for ((id, anim) in currentAnimations) {
                                        // If we haven't started this animation yet, then start it
                                        // now.
                                        if (anim.startTimeNanos == 0L) {
                                            anim.startTimeNanos = frameTimeNanos
                                        }

                                        val playTimeNanos = frameTimeNanos - anim.startTimeNanos

                                        // Compute where it's meant to be, and update the value in
                                        // animState.
                                        val position =
                                            anim.animation.getValueFromNanos(playTimeNanos)
                                        animState[id] = position

                                        // If the animation is complete, then we need to remove it
                                        // from the transitions
                                        // list, and apply it to the base interaction state.
                                        if (anim.animation.isFinishedFromNanos(playTimeNanos)) {
                                            animState.remove(id)
                                            if (anim.action != null) {
                                                animsToRemove.add(id)
                                                interactionState.squooshCompleteAnimatedAction(
                                                    anim.action
                                                )
                                            }
                                            if (anim.variant != null) {
                                                animsToRemove.add(id)
                                                variantTransitions.completedAnimatedVariant(
                                                    anim.variant
                                                )
                                            }
                                        }
                                    }
                                    for (id in animsToRemove) currentAnimations.remove(id)
                                    animationValues.value = animState
                                }
                            }
                        }
                }

                val placeables =
                    measurables.map { measurable ->
                        val squooshData = measurable.parentData as? SquooshParentData
                        if (squooshData == null || squooshData.node.computedLayout == null) {
                            // Oops! No data, just lay it out however it wants.
                            Pair(measurable.measure(constraints), null)
                        } else {
                            // Ok, we can get some layout data. This lets us determine a width
                            // and height from layout. We also need to extract a transform, then
                            // we can position this view appropriately, and create a layer for it
                            // if it has a rotation applied (unfortunately Compose doesn't seem to
                            // accept a full matrix transform, so we can't support shears).
                            val w =
                                (squooshData.node.computedLayout!!.width * density.density)
                                    .roundToInt()
                            val h =
                                (squooshData.node.computedLayout!!.height * density.density)
                                    .roundToInt()

                            Pair(
                                measurable.measure(
                                    Constraints(
                                        minWidth = w,
                                        maxWidth = w,
                                        minHeight = h,
                                        maxHeight = h
                                    )
                                ),
                                squooshData.node
                            )
                        }
                    }

                layout(
                    (root.computedLayout!!.width * density.density).roundToInt(),
                    (root.computedLayout!!.height * density.density).roundToInt()
                ) {
                    // Place children in the parent layout
                    placeables.forEach { (placeable, node) ->
                        // If we don't have a node, then just place this and finish.
                        if (node == null) {
                            placeable.placeRelative(x = 0, y = 0)
                        } else {
                            // Ok, we can look up the position and transform by iterating over the
                            // parents. We don't support transforms here yet. Child composables will
                            // be rendered with transforms, but won't use them for input.
                            //
                            // We always take the offset from the root, but if there are layers of
                            // custom composables (containing each other) then this will give the
                            // wrong offset.
                            //
                            // XXX XXX: Create ticket to implement transformed input.
                            val offsetFromRoot = node.offsetFromAncestor()

                            placeable.placeRelative(
                                x = (offsetFromRoot.x * density.density).roundToInt(),
                                y = (offsetFromRoot.y * density.density).roundToInt()
                            )
                        }
                    }
                }
            },
            content = {
                // Now render all of the children
                val debugRenderChildComposablesStartTime = SystemClock.elapsedRealtime()
                for (child in childComposables) {
                    var composableChildModifier =
                        Modifier.drawWithContent {
                                if (child.node == childRenderSelector.selectedRenderChild)
                                    drawContent()
                            }
                            .then(SquooshParentData(node = child.node))

                    if (child.component == null && child.content == null) {
                        composableChildModifier =
                            composableChildModifier.squooshInteraction(
                                doc,
                                interactionState,
                                interactionScope,
                                customizationContext,
                                child
                            )
                    }

                    // We use a custom layout for children that just passes through the layout
                    // constraints that we calculate in our layout glue code above. This gives
                    // children a way to report their sizes to the Rust layout implementation.
                    androidx.compose.ui.layout.Layout(
                        modifier = composableChildModifier,
                        content = {
                            if (child.component != null) {
                                child.component.invoke(
                                    object : ComponentReplacementContext {
                                        override val layoutModifier: Modifier = Modifier
                                        override val textStyle: TextStyle? = null
                                    }
                                )
                            } else if (child.content != null) {
                                Log.d(TAG, "Unimplemented: child.content")
                            }
                        },
                        measurePolicy = { measurables, constraints ->
                            // Just overlay everything, like a box, but with exactly the incoming
                            // constraints applied.
                            val placeables = measurables.map { child -> child.measure(constraints) }
                            val layoutWidth =
                                (placeables.maxByOrNull { it.width }?.width ?: constraints.minWidth)
                            val layoutHeight =
                                (placeables.maxByOrNull { it.height }?.height
                                    ?: constraints.minHeight)
                            // Place all children at the origin (with the same size constraints that
                            // we were given).
                            layout(layoutWidth, layoutHeight) {
                                placeables.forEach { child -> child.placeRelative(0, 0) }
                            }
                        }
                    )
                }
                Log.d(
                    TAG,
                    "$docName generate child composables took ${SystemClock.elapsedRealtime() - debugRenderChildComposablesStartTime}ms"
                )
            }
        )
        designSwitcher()
    }
}
