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
import android.util.SizeF
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.android.designcompose.AnimatedAction
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.ComputedPathCache
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DebugNodeManager
import com.android.designcompose.DesignComposeCallbacks
import com.android.designcompose.DesignScrollState
import com.android.designcompose.DesignSettings
import com.android.designcompose.DesignSwitcher
import com.android.designcompose.DesignSwitcherPolicy
import com.android.designcompose.DocRenderStatus
import com.android.designcompose.DocServer
import com.android.designcompose.DocumentSwitcher
import com.android.designcompose.InteractionState
import com.android.designcompose.InteractionStateManager
import com.android.designcompose.KeyEventTracker
import com.android.designcompose.KeyInjectManager
import com.android.designcompose.LayoutManager
import com.android.designcompose.LiveUpdateMode
import com.android.designcompose.LocalDesignDocSettings
import com.android.designcompose.LocalVariableState
import com.android.designcompose.ShaderBrushCache
import com.android.designcompose.VariableState
import com.android.designcompose.branches
import com.android.designcompose.clonedWithAnimatedActionsApplied
import com.android.designcompose.close
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.common.DocumentServerParams
import com.android.designcompose.common.NodeQuery
import com.android.designcompose.definition.element.dimensionProto
import com.android.designcompose.definition.element.size
import com.android.designcompose.definition.layout.copy
import com.android.designcompose.definition.plugin.OverlayBackgroundInteraction
import com.android.designcompose.definition.plugin.OverlayPositionType
import com.android.designcompose.definition.plugin.colorOrNull
import com.android.designcompose.definition.plugin.overlayBackgroundOrNull
import com.android.designcompose.definition.view.copy
import com.android.designcompose.definition.view.scrollInfoOrNull
import com.android.designcompose.dispatch
import com.android.designcompose.doc
import com.android.designcompose.getContent
import com.android.designcompose.getKey
import com.android.designcompose.getOpenLinkCallback
import com.android.designcompose.getScrollCallbacks
import com.android.designcompose.getTapCallback
import com.android.designcompose.layout_interface.Layout
import com.android.designcompose.registerOpenLinkCallback
import com.android.designcompose.rootNode
import com.android.designcompose.rootOverlays
import com.android.designcompose.sDocRenderStatus
import com.android.designcompose.sDocRenderText
import com.android.designcompose.setMeterState
import com.android.designcompose.squooshAnimatedActions
import com.android.designcompose.squooshCompleteAnimatedAction
import com.android.designcompose.squooshFailedAnimatedAction
import com.android.designcompose.squooshVariantMemory
import com.android.designcompose.stateForDoc
import com.android.designcompose.unregisterOpenLinkCallback
import java.io.InputStream
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    var delayTimeMs: Int = 0,
)

/// Apply layout constraints to a node; this is only used for the root node and gives the DC
/// layout system the context of what it is being embedded in.
private fun SquooshResolvedNode.applyLayoutConstraints(constraints: Constraints, density: Float) {
    // This function mutates `this.style` in different ways depending on the constraints. Therefore,
    // we should always start with `view.style` (immutable, from the DCF) and apply constraints to
    // that. Otherwise, we end up progressively adding different style rules to the root style as we
    // get different layout queries (especially during the intrinsic width/height query process).
    val layoutStyle =
        view.style.layoutStyle.copy {
            if (constraints.minWidth != 0)
                minWidth = dimensionProto { points = constraints.minWidth.toFloat() / density }

            if (constraints.maxWidth != Constraints.Infinity)
                maxWidth = dimensionProto { points = constraints.maxWidth.toFloat() / density }

            if (constraints.hasFixedWidth) {
                width = dimensionProto { points = constraints.minWidth.toFloat() / density }
                // Layout implementation looks for width/height being set and then uses bounding
                // box.
                val h = boundingBox.height
                boundingBox = size {
                    width = constraints.minWidth.toFloat() / density
                    height = h
                }
            }

            if (constraints.minHeight != 0)
                minHeight = dimensionProto { points = constraints.minHeight.toFloat() / density }

            if (constraints.maxHeight != Constraints.Infinity)
                maxHeight = dimensionProto { points = constraints.maxHeight.toFloat() / density }

            if (constraints.hasFixedHeight) {
                height = dimensionProto { points = constraints.minHeight.toFloat() / density }
                // Layout implementation looks for width/height being set and then uses bounding
                // box.
                val w = boundingBox.width
                boundingBox = size {
                    width = w
                    height = constraints.minHeight.toFloat() / density
                }
            }
        }

    val rootStyleBuilder = style.copy { this.layoutStyle = layoutStyle }
    style = rootStyleBuilder
}

// Mutable field to remember animation information that is computed during the layout phase which
// shouldn't invalidate anything as it mutates.
internal class AnimationValueHolder(var animationJob: Job?)

// Track if we're the root component, or (in the near future) if we are a list item so that we can
// share information across similar list items.
internal data class SquooshIsRoot(val isRoot: Boolean)

internal val LocalSquooshIsRootContext = compositionLocalOf { SquooshIsRoot(true) }

class RootRecurseParams(
    // true if SquooshRoot() called from a scrollable child
    val isScrollComponent: Boolean = false,

    // Hash of text nodes seen in entire tree of nodes, for testing purposes
    val textHash: HashSet<String> = HashSet(),
) {
    fun withScrolling(): RootRecurseParams {
        return RootRecurseParams(true, textHash)
    }
}

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
    rootRecurseParams: RootRecurseParams = RootRecurseParams(),
    dcfInputStream: InputStream? = null,
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
            liveUpdateMode == LiveUpdateMode.OFFLINE,
            dcfInputStream,
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

    val startFrame =
        interactionState.rootNode(
            initialNode = rootNodeQuery,
            doc = doc,
            isRoot = isRoot,
            customizationContext,
        )

    if (startFrame == null) {
        Log.d(TAG, "No start frame $docName / $incomingDocId")
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
    val fontResolver = LocalFontFamilyResolver.current

    val variantParentName =
        when (rootNodeQuery) {
            is NodeQuery.NodeVariant -> rootNodeQuery.parent
            else -> ""
        }

    val layoutManager = remember(docId) { SquooshLayout.newLayoutManager() }
    val layoutIdAllocator = remember(docId) { SquooshLayoutIdAllocator() }
    val layoutCache = remember(docId) { HashMap<Int, Int>() }
    val layoutValueCache = remember(docId) { HashMap<Int, Layout>() }
    val keyEventTracker = remember(docId, rootNodeQuery) { KeyEventTracker() }
    DisposableEffect(docId, rootNodeQuery) {
        KeyInjectManager.addTracker(keyEventTracker)
        onDispose { KeyInjectManager.removeTracker(keyEventTracker) }
    }

    // Register an open link callback function if one was specified
    val openLinkCallback = customizationContext.getOpenLinkCallback(startFrame.name)
    DisposableEffect(docId, rootNodeQuery, openLinkCallback) {
        openLinkCallback?.let { interactionState.registerOpenLinkCallback(it) }
        onDispose { openLinkCallback?.let { interactionState.unregisterOpenLinkCallback(it) } }
    }

    // When device configuration changes or debug setting of using local string resource changes,
    // invalidate the text measure cache.
    val textMeasureCache =
        remember(
            docId,
            LocalContext.current.resources.configuration,
            DebugNodeManager.getUseLocalRes().value,
        ) {
            TextMeasureCache()
        }
    val computedPathCache = remember(docId) { ComputedPathCache() }
    val shaderBrushCache = remember(docId, doc.c.header.lastModified) { ShaderBrushCache() }

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
    val composableList = ComposableList()
    keyEventTracker.clearListeners()
    val variableState = VariableState.create()
    val root =
        resolveVariantsRecursively(
            startFrame,
            doc,
            customizationContext,
            variantTransitions,
            interactionState,
            keyEventTracker,
            null,
            density,
            fontResolver,
            composableList,
            layoutIdAllocator,
            variantParentName,
            isRoot,
            variableState,
            LocalContext.current,
            LocalDesignDocSettings.current.customVariantTransition,
            textMeasureCache,
            rootRecurseParams.textHash,
            overlays,
            rootRecurseParams.isScrollComponent,
        ) ?: return
    val rootRemovalNodes = layoutIdAllocator.removalNodes()

    // Get the list of actions that currently need to be animated from the interaction state.
    val animatedActions = interactionState.squooshAnimatedActions(doc)

    // We maintain a list of animations (animation id, info) that we're currently running so
    // that we can just update the key info when recomposing mid-animation.
    val currentAnimations = remember { HashMap<Int, SquooshAnimationRenderingInfo>() }

    // Animation play times go into a `MutableState`. This way we can modify them in a coroutine
    // and only invalidate rendering. So animation progression doesn't cause or require
    // recomposition, resulting in a good performance uplift.
    val animPlayTimeNanosState: MutableState<Map<Int, Long>> = remember { mutableStateOf(mapOf()) }

    // Get the interaction state with all of the animated actions applied. We use this to generate
    // the tree with all actions applied (which is then used as the "dest" in the animation).
    val transitionedInteractionState = interactionState.clonedWithAnimatedActionsApplied()

    var transitionRoot: SquooshResolvedNode? = null
    var transitionRootRemovalNodes: HashSet<Int>? = null
    // Now see if we need to compute a transition root and generate animated transitions.
    if (
        isRoot && (transitionedInteractionState != null && animatedActions.isNotEmpty()) ||
            variantTransitions.needsTransitionPhase()
    ) {
        variantTransitions.treeBuildPhase = TreeBuildPhase.TransitionTargetPhase

        // We need to make a new root with this interaction state applied, and then compute the
        // animation control between the trees.
        composableList.clearChildren()

        transitionRoot =
            resolveVariantsRecursively(
                startFrame,
                doc,
                customizationContext,
                variantTransitions,
                transitionedInteractionState ?: interactionState,
                keyEventTracker,
                null,
                density,
                fontResolver,
                composableList,
                layoutIdAllocator,
                variantParentName,
                isRoot,
                VariableState.create(),
                LocalContext.current,
                LocalDesignDocSettings.current.customVariantTransition,
                textMeasureCache,
                rootRecurseParams.textHash,
                overlays,
                rootRecurseParams.isScrollComponent,
            )
        transitionRootRemovalNodes = layoutIdAllocator.removalNodes()
    }

    variantTransitions.afterRenderPhases()

    textMeasureCache.collect()

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
                    fromNodeId = animatedAction.instanceNodeId,
                    animationId = animatedAction.id,
                    interruptedId = animatedAction.interruptedId,
                    transition = animatedAction.transition,
                    action = animatedAction,
                    variant = null,
                )
        }
        for (variantTransition in variantTransitions.transitions.values) {
            requestedAnimations[variantTransition.fromNodeId] =
                SquooshAnimationRequest(
                    toNodeId = variantTransition.toNodeId,
                    fromNodeId = null,
                    animationId = variantTransition.id,
                    interruptedId = variantTransition.interruptedId,
                    transition = variantTransition.transition,
                    action = null,
                    variant = variantTransition,
                )
        }

        // Update the root that we're rendering.
        presentationRoot =
            createMergedAnimationTree(
                root,
                transitionRoot,
                requestedAnimations,
                LocalDesignDocSettings.current.customVariantTransition,
                variableState,
            )

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
                        val interruptedAnimationPlayTime =
                            animPlayTimeNanosState.value[animationRequest.interruptedId]
                        val interruptedAnimation = currentAnimations[animationRequest.interruptedId]
                        if (
                            interruptedAnimation?.animation != null &&
                                interruptedAnimationPlayTime != null
                        ) {
                            val playTimeNanos =
                                interruptedAnimationPlayTime -
                                    interruptedAnimation.delayTimeMs * 1000000L
                            interruptedAnimation.animation.getValueFromNanos(
                                interruptedAnimationPlayTime
                            )
                        } else {
                            1f
                        }
                    } else {
                        1f
                    }

                val animation =
                    TargetBasedAnimation(
                        animationSpec = animationRequest.transition.animationSpec(),
                        typeConverter = Float.VectorConverter,
                        initialValue = 1f - initialValue,
                        targetValue = 1f,
                    )
                animationControl.animation = animation
                nextAnimations[animationRequest.animationId] =
                    SquooshAnimationRenderingInfo(
                        control = animationControl,
                        animation = animation,
                        action = animationRequest.action,
                        variant = animationRequest.variant,
                        delayTimeMs = animationRequest.transition.delayMillis(),
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

    // If this view is scrollable, scrollOffset keeps track of the scroll position and is used to
    // translate child contents.
    val scrollOffset = remember { mutableStateOf(Offset.Zero) }

    val orientation =
        when (presentationRoot.view.scrollInfoOrNull?.overflow) {
            com.android.designcompose.definition.layout.OverflowDirection
                .OVERFLOW_DIRECTION_VERTICAL_SCROLLING -> Orientation.Vertical
            com.android.designcompose.definition.layout.OverflowDirection
                .OVERFLOW_DIRECTION_HORIZONTAL_SCROLLING -> Orientation.Horizontal
            else -> null
        }

    // If a scroll callback customization exists, call it when the scroll state changes
    val scrollStateChangedCallback =
        customizationContext.getScrollCallbacks(presentationRoot.view.name)?.scrollStateChanged
    val setScrollState = { delta: Float ->
        when (orientation) {
            Orientation.Vertical -> {
                // Bound the scroll offset to the area bounded by content_height
                val contentHeight =
                    (presentationRoot.computedLayout?.contentHeight ?: 0F) * density.density
                val frameHeight = (presentationRoot.computedLayout?.height ?: 0F) * density.density
                val maxScroll = (contentHeight - frameHeight).coerceAtLeast(0F)
                val scrollValue = (scrollOffset.value.y + delta).coerceIn(0F, maxScroll)
                scrollOffset.value = Offset(0F, scrollValue)
                scrollStateChangedCallback?.invoke(
                    DesignScrollState(scrollValue, frameHeight, contentHeight, maxScroll)
                )
            }
            Orientation.Horizontal -> {
                // Bound the scroll offset to the area bounded by content_width
                val contentWidth =
                    (presentationRoot.computedLayout?.contentWidth ?: 0F) * density.density
                val frameWidth = (presentationRoot.computedLayout?.width ?: 0F) * density.density
                val maxScroll = (contentWidth - frameWidth).coerceAtLeast(0F)
                val scrollValue = (scrollOffset.value.x + delta).coerceIn(0F, maxScroll)
                scrollOffset.value = Offset(scrollValue, 0F)
                scrollStateChangedCallback?.invoke(
                    DesignScrollState(scrollValue, frameWidth, contentWidth, maxScroll)
                )
            }
            else -> {}
        }
    }
    val scrollState = rememberScrollableState { delta ->
        setScrollState(delta)
        delta
    }

    // Create a Modifier to handle scroll inputs if this is a scrollable view
    val scrollModifier =
        if (rootRecurseParams.isScrollComponent && orientation != null) {
            // If a callback to get the scrollable state exists, call it
            val setScrollableStateCallback =
                customizationContext
                    .getScrollCallbacks(presentationRoot.view.name)
                    ?.setScrollableState
            LaunchedEffect(scrollState, setScrollableStateCallback) {
                setScrollableStateCallback?.invoke(scrollState)
                setScrollState(0F)
            }
            Modifier.scrollable(
                orientation = orientation,
                reverseDirection = true,
                state = scrollState,
            )
        } else {
            Modifier
        }

    // If we are currently being pressed, then we must continue to include a `pointerInput` block,
    // even if the variant we're currently showing doesn't have any click or press interactions. If
    // we don't do this, then when we CHANGE_TO a variant that has no interactions but is an
    // ON_PRESS target, then Compose will think we've lost interest in the gesture and cancel it for
    // us. Even though we re-run this block on recompose, our `onPress` closure runs with its scope
    // for the duration of the gesture.
    //
    // This is covered in the interaction test document's "Combos" screen; the purple button has no
    // interactions in its ON_PRESS variant.
    val isPressed = remember { mutableStateOf(false) }
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
                        animPlayTimeNanosState,
                        VariableState.create(),
                        LocalVariableState.hasOverrideModeValues(),
                        computedPathCache,
                        shaderBrushCache,
                        appContext = LocalContext.current,
                        scrollOffset,
                    )
                    .semantics {
                        sDocRenderStatus = DocRenderStatus.Rendered
                        sDocRenderText = rootRecurseParams.textHash
                    }
                    .then(scrollModifier),
            measurePolicy =
                squooshLayoutMeasurePolicy(
                    presentationRoot,
                    root,
                    rootRemovalNodes,
                    transitionRoot,
                    transitionRootRemovalNodes,
                    layoutCache,
                    layoutValueCache,
                    layoutManager,
                    animationJob,
                    animPlayTimeNanosState,
                    currentAnimations,
                    interactionState,
                    interactionScope,
                    variantTransitions,
                    scrollOffset,
                ),
            content = {
                // Now render all of the children
                for (child in composableList.childComposables) {
                    var needsComposition = true

                    var composableChildModifier =
                        Modifier.drawWithContent {
                                if (
                                    child.node.layoutId ==
                                        childRenderSelector.selectedRenderChild?.layoutId
                                ) {
                                    drawContent()
                                }
                            }
                            .then(SquooshParentData(node = child.node))
                            .then(Modifier.testTag(child.node.view.name))

                    if (child.scrollView != null) {
                        // Compose a scrollable view as a separate composable in order to detect
                        // scroll input for it and translate its children. Construct a NodeQuery
                        // using the ID of the scrollview and compose SquooshRoot() with it.
                        val scrollNodeQuery = NodeQuery.NodeId(child.scrollView.id)
                        child.component = {
                            SquooshRoot(
                                docName,
                                incomingDocId,
                                scrollNodeQuery,
                                Modifier,
                                customizationContext,
                                serverParams,
                                setDocId,
                                designSwitcherPolicy = DesignSwitcherPolicy.SHOW_IF_ROOT,
                                liveUpdateMode,
                                designComposeCallbacks,
                                rootRecurseParams.withScrolling(), // Is scroll component
                            )
                        }
                    } else if (child.component == null) {
                        // If there are press or click reactions, composition is needed
                        var hasPressClick =
                            customizationContext.getTapCallback(child.node.view) != null
                        child.node.view.reactionsList.forEach {
                            if (it.trigger.hasPress() || it.trigger.hasClick()) hasPressClick = true
                            else if (it.trigger.hasAfterTimeout()) {
                                // If there is a timeout trigger, execute the reaction in a suspend
                                // function after the timeout
                                val timeoutTrigger = it.trigger.afterTimeout
                                val timeoutMs = timeoutTrigger.timeout
                                LaunchedEffect(child.node.view.id, it) {
                                    delay((timeoutMs * 1000.0).toLong())
                                    interactionState.dispatch(
                                        it.action,
                                        findTargetInstanceId(
                                            doc,
                                            child.parentComponents,
                                            it.action,
                                        ),
                                        customizationContext.getKey(),
                                        null,
                                    )
                                }
                            }
                        }

                        if (hasPressClick || isPressed.value) {
                            val interactionSource = remember { MutableInteractionSource() }
                            composableChildModifier =
                                composableChildModifier.squooshInteraction(
                                    doc,
                                    interactionState,
                                    interactionScope,
                                    customizationContext,
                                    child,
                                    isPressed,
                                    interactionSource,
                                )
                        } else if (child.node.textInfo?.hyperlinkOffsetMap?.isNotEmpty() == true) {
                            composableChildModifier =
                                composableChildModifier.hyperlinkHandler(
                                    child.node,
                                    LocalUriHandler.current,
                                )
                        } else if (
                            child.node.findProgressBarChild() != null ||
                                child.node.findProgressMarkerDescendant() != null
                        ) {
                            // Get the progress bar or marker node and set a meter state on the node
                            // so that it can be set whenever it is dragged.
                            val pbChild = child.node.findProgressBarChild()
                            val pmChild = child.node.findProgressMarkerDescendant()
                            val interactionSource = remember { MutableInteractionSource() }
                            val meter = remember { mutableStateOf<Float?>(null) }
                            pbChild?.let { customizationContext.setMeterState(it.view.name, meter) }
                            pmChild?.let { customizationContext.setMeterState(it.view.name, meter) }
                            // Give this node a Modifier that tracks tap/drag
                            composableChildModifier =
                                composableChildModifier.progressBarSlider(
                                    child.node,
                                    interactionScope,
                                    interactionSource,
                                    customizationContext,
                                    meter,
                                )
                        } else {
                            needsComposition = false
                        }
                    }

                    // If composition is not needed, continue to the next child
                    if (!needsComposition) continue

                    // We use a custom layout for children that just passes through the layout
                    // constraints that we calculate in our layout glue code above. This gives
                    // children a way to report their sizes to the Rust layout implementation.
                    val isListItem =
                        child.node.parent?.view?.name?.let {
                            customizationContext.getContent(it) != null
                        } ?: false
                    // Use the key() function to avoid recomposition when list items are reordered.
                    // However, don't call key() for component replacements to avoid recomposition.
                    // This is due to a bug where recomposition a component replacement that uses
                    // AndroidView can cause a crash. See bug
                    // https://github.com/google/automotive-design-compose/issues/1605
                    if (isListItem)
                        key(child.node.layoutId) {
                            SquooshChildLayout(
                                modifier = composableChildModifier,
                                child = child,
                                scrollOffset,
                            )
                        }
                    else
                        SquooshChildLayout(
                            modifier = composableChildModifier,
                            child = child,
                            scrollOffset,
                        )
                }
            },
        )
        designSwitcher()

        for (overlay in composableList.overlayNodes) {
            val contentAlignment: Alignment =
                when (overlay.extras.overlayPositionType) {
                    OverlayPositionType.OVERLAY_POSITION_TYPE_TOP_LEFT -> Alignment.TopStart
                    OverlayPositionType.OVERLAY_POSITION_TYPE_TOP_CENTER -> Alignment.TopCenter
                    OverlayPositionType.OVERLAY_POSITION_TYPE_TOP_RIGHT -> Alignment.TopEnd
                    OverlayPositionType.OVERLAY_POSITION_TYPE_BOTTOM_LEFT -> Alignment.BottomStart
                    OverlayPositionType.OVERLAY_POSITION_TYPE_BOTTOM_CENTER ->
                        Alignment.BottomCenter
                    OverlayPositionType.OVERLAY_POSITION_TYPE_BOTTOM_RIGHT -> Alignment.BottomEnd
                    else -> Alignment.Center
                }
            val backgroundMod =
                overlay.extras.overlayBackgroundOrNull?.colorOrNull?.let {
                    Modifier.background(Color(it.r, it.g, it.b, it.a))
                } ?: Modifier
            val closeMod =
                if (
                    overlay.extras.overlayBackgroundInteraction ==
                        OverlayBackgroundInteraction
                            .OVERLAY_BACKGROUND_INTERACTION_CLOSE_ON_CLICK_OUTSIDE
                ) {
                    Modifier.clickable { interactionState.close(null) }
                } else Modifier
            Box(
                modifier = Modifier.fillMaxSize().then(backgroundMod).then(closeMod),
                contentAlignment = contentAlignment,
            ) {
                val overlayNodeQuery = NodeQuery.NodeId(overlay.nodeId)
                SquooshRoot(
                    docName,
                    incomingDocId,
                    overlayNodeQuery,
                    Modifier,
                    customizationContext,
                    serverParams,
                    setDocId,
                    DesignSwitcherPolicy.HIDE,
                    liveUpdateMode,
                    designComposeCallbacks,
                    RootRecurseParams(textHash = rootRecurseParams.textHash),
                )
            }
        }
    }
}

private fun measureExternal(
    measurable: IntrinsicMeasurable,
    width: Float,
    height: Float,
    availableWidth: Float,
    availableHeight: Float,
): SizeF {
    val isBoundDefined = { dimension: Float -> dimension > 0f && dimension < Float.MAX_VALUE }
    // Figure out what we're being asked; we can't just do a measure with all
    // of the constraints we've been given, because we're not allowed to measure
    // twice in one layout pass.
    return if (isBoundDefined(width) && !isBoundDefined(height)) {
        // We have a width, but no height, so ask for the height.
        SizeF(width, measurable.minIntrinsicHeight(width.toInt()).toFloat())
    } else if (isBoundDefined(height) && !isBoundDefined(width)) {
        SizeF(measurable.minIntrinsicWidth(height.toInt()).toFloat(), height)
    } else if (isBoundDefined(width) && isBoundDefined(height)) {
        SizeF(width, height)
    } else {
        // Nothing is defined, can we lay out within the available space?
        val w =
            measurable.minIntrinsicWidth(
                if (isBoundDefined(availableHeight)) {
                    availableHeight.toInt()
                } else {
                    Constraints.Infinity
                }
            )
        val h =
            measurable.minIntrinsicHeight(
                if (isBoundDefined(availableWidth)) {
                    availableWidth.toInt()
                } else {
                    Constraints.Infinity
                }
            )
        SizeF(w.toFloat(), h.toFloat())
    }
}

private fun squooshLayoutMeasurePolicy(
    presentationRoot: SquooshResolvedNode,
    root: SquooshResolvedNode,
    rootRemovalNodes: HashSet<Int>,
    transitionRoot: SquooshResolvedNode?,
    transitionRootRemovalNodes: HashSet<Int>?,
    layoutCache: HashMap<Int, Int>,
    layoutValueCache: HashMap<Int, Layout>,
    layoutManager: SquooshLayoutManager,
    animationJob: AnimationValueHolder,
    animPlayTimeNanosState: MutableState<Map<Int, Long>>,
    currentAnimations: HashMap<Int, SquooshAnimationRenderingInfo>,
    interactionState: InteractionState,
    interactionScope: CoroutineScope,
    variantTransitions: SquooshVariantTransition,
    scrollOffset: State<Offset>,
): MeasurePolicy =
    object : MeasurePolicy {
        private fun subscribeIntrinsicMeasurables(measurables: List<IntrinsicMeasurable>) {
            // Go through the measurables and find the ones that need layout measurement so
            // we can make a function for layout measure.
            measurables.forEach { measurable ->
                val squooshData = measurable.parentData as? SquooshParentData
                if (squooshData != null && squooshData.node.needsChildLayout) {
                    LayoutManager.squooshSetCustomMeasure(squooshData.node.layoutId) {
                        width,
                        height,
                        availableWidth,
                        availableHeight ->
                        measureExternal(measurable, width, height, availableWidth, availableHeight)
                    }
                }
            }
        }

        private fun unsubscribeIntrinsicMeasurables(measurables: List<IntrinsicMeasurable>) {
            // Clear our custom layout, and release references to measurables.
            measurables.forEach { measurable ->
                val squooshData = measurable.parentData as? SquooshParentData
                if (squooshData != null && squooshData.node.needsChildLayout) {
                    LayoutManager.squooshClearCustomMeasure(squooshData.node.layoutId)
                }
            }
        }

        private fun doNativeLayout(constraints: Constraints, density: Float) {
            // Update the root node style to have the incoming width/height from our parent
            // Composable.
            root.applyLayoutConstraints(constraints, density)

            // Ensure that the transition target also has the incoming width/height applied.
            transitionRoot?.applyLayoutConstraints(constraints, density)

            // Perform layout on our resolved tree.
            layoutTree(root, layoutManager, rootRemovalNodes, layoutCache, layoutValueCache)

            // If we have a transition root, then lay it out and compute any animations that
            // are needed.
            val tRoot = transitionRoot
            if (tRoot != null && transitionRootRemovalNodes != null) {
                // Layout the transition root tree.
                layoutTree(
                    tRoot,
                    layoutManager,
                    transitionRootRemovalNodes,
                    layoutCache,
                    layoutValueCache,
                )
                updateDerivedLayout(presentationRoot)
            }
        }

        override fun MeasureScope.measure(
            measurables: List<Measurable>,
            constraints: Constraints,
        ): MeasureResult {
            // Perform the "real" layout, now that we're done with
            // intrinsic queries and our parent has called the measure
            // method on us.
            subscribeIntrinsicMeasurables(measurables)
            doNativeLayout(constraints, density)
            unsubscribeIntrinsicMeasurables(measurables)

            // Launch or update the animation coroutine. We do this in
            // layout because we need to run animations based on updated
            // layout positions of items.
            updateAnimations()

            // Now we can copy the values computed by the native layout
            // using intrinsic queries into our Compose children. This is
            // how Composable child nodes (used for input and hosting
            // external Composables) get placed.
            val placeables = squooshMeasure(measurables, constraints)
            return squooshLayout(root, density, placeables, scrollOffset)
        }

        // These intrinsic calculations could be optimized to only copy out
        // the root's width/height.
        override fun IntrinsicMeasureScope.minIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int,
        ): Int {
            val constraints =
                if (height < Constraints.Infinity) {
                    Constraints(maxHeight = height)
                } else {
                    Constraints()
                }
            // Perform layout on our resolved tree.
            subscribeIntrinsicMeasurables(measurables)
            doNativeLayout(constraints, density)

            return root.computedLayout!!.width.roundToInt()
        }

        override fun IntrinsicMeasureScope.minIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int,
        ): Int {
            val constraints =
                if (width < Constraints.Infinity) {
                    Constraints(maxWidth = width)
                } else {
                    Constraints()
                }
            // Perform layout on our resolved tree.
            subscribeIntrinsicMeasurables(measurables)
            doNativeLayout(constraints, density)

            return root.computedLayout!!.height.roundToInt()
        }

        private fun updateAnimations() {
            // Only update animations if we need an animation job and don't
            // have one. The job itself, once running, will keep itself
            // up-to-date, and will eventually remove itself once complete.
            val needsAnimationJob =
                interactionState.animations.isNotEmpty() ||
                    variantTransitions.transitions.isNotEmpty()
            if (!needsAnimationJob) return

            val currentAnimationJob = animationJob.animationJob
            val hasAnimationJob = currentAnimationJob != null && currentAnimationJob.isActive
            if (hasAnimationJob) return

            // We need an animation job and don't have one, so let's launch one!
            animationJob.animationJob =
                interactionScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    // While there are transitions to be run, we should run them; we just
                    // update the floats in the mutable state. Those are then used by the
                    // render function, and we thus avoid needing to recompose in order
                    // to propagate the animation state.
                    //
                    // We also complete transitions in this block, and that action does
                    // cause a recomposition via the subscription that SquooshRoot makes
                    // to the InteractionState's list of transitions.
                    while (
                        interactionState.animations.isNotEmpty() ||
                            variantTransitions.transitions.isNotEmpty()
                    ) {
                        // withFrameNanos runs its closure outside of this recomposition
                        // phase -- because the body of the code only changes mutable
                        // state which is only used at render time, Compose only re-runs
                        // the render block.
                        withFrameNanos { frameTimeNanos ->
                            val nanos = HashMap(animPlayTimeNanosState.value)
                            val animsToRemove = HashSet<Int>()
                            for ((id, anim) in currentAnimations) {
                                // If we haven't started this animation yet, then start it
                                // now.
                                if (anim.startTimeNanos == 0L) {
                                    anim.startTimeNanos =
                                        frameTimeNanos + anim.delayTimeMs * 1000000
                                }

                                val playTimeNanos = frameTimeNanos - anim.startTimeNanos
                                if (playTimeNanos < 0L) continue

                                // Update the value in animPlayTimeNanosState with the current play
                                // time
                                nanos[id] = playTimeNanos

                                // If the animation is complete, then we need to remove it
                                // from the transitions list, and apply it to the base
                                // interaction state.
                                if (anim.control.isFinishedFromNanos(playTimeNanos)) {
                                    nanos.remove(id)
                                    if (anim.action != null) {
                                        animsToRemove.add(id)
                                        interactionState.squooshCompleteAnimatedAction(anim.action)
                                    }
                                    if (anim.variant != null) {
                                        animsToRemove.add(id)
                                        variantTransitions.completedAnimatedVariant(anim.variant)
                                    }
                                }
                            }
                            for (id in animsToRemove) currentAnimations.remove(id)
                            animPlayTimeNanosState.value = nanos
                        }
                    }
                }
        }
    }

// These are common functions used by every Squoosh MeasurePolicy
private fun MeasureScope.squooshMeasure(
    measurables: List<Measurable>,
    constraints: Constraints,
): List<Placeable> {
    return measurables.map { measurable ->
        val squooshData = measurable.parentData as? SquooshParentData
        if (squooshData == null || squooshData.node.computedLayout == null) {
            // Oops! No data, just lay it out however it wants, using whatever
            // constraints we were given.
            measurable.measure(constraints)
        } else {
            // Ok, we can get some layout data. This lets us determine a width
            // and height from layout. We also need to extract a transform, then
            // we can position this view appropriately, and create a layer for it
            // if it has a rotation applied (unfortunately Compose doesn't seem to
            // accept a full matrix transform, so we can't support shears).
            val w = (squooshData.node.computedLayout!!.width * density).roundToInt()
            val h = (squooshData.node.computedLayout!!.height * density).roundToInt()

            // Child composables that want child layout have well defined bounds that
            // we measured during layout (and some don't like being given a minimum size),
            // so we pass 0 to those. Others are just present for event handling, and they
            // must have the required width/height assigned here, because they have no
            // intrinsic minimum size.
            val minWidth =
                if (squooshData.node.needsChildLayout) {
                    0
                } else {
                    w
                }
            val minHeight =
                if (squooshData.node.needsChildLayout) {
                    0
                } else {
                    h
                }
            measurable.measure(
                Constraints(minWidth = minWidth, minHeight = minHeight, maxWidth = w, maxHeight = h)
            )
        }
    }
}

private fun MeasureScope.squooshLayout(
    root: SquooshResolvedNode,
    density: Float,
    placeables: List<Placeable>,
    scrollOffset: State<Offset>,
): MeasureResult {
    return layout(
        (root.computedLayout!!.width * density).roundToInt(),
        (root.computedLayout!!.height * density).roundToInt(),
    ) {
        // Place children in the parent layout
        placeables.forEach { placeable ->
            val squooshData = placeable.parentData as? SquooshParentData
            val node = squooshData?.node
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

                var x = (offsetFromRoot.x * density).roundToInt()
                var y = (offsetFromRoot.y * density).roundToInt()
                if (!squooshData.node.skipLayoutScroll) {
                    x -= scrollOffset.value.x.roundToInt()
                    y -= scrollOffset.value.y.roundToInt()
                }
                placeable.placeRelative(x, y)
            }
        }
    }
}

@Composable
private fun SquooshChildLayout(
    modifier: Modifier,
    child: SquooshChildComposable,
    scrollOffset: State<Offset>,
) {
    androidx.compose.ui.layout.Layout(
        modifier = modifier,
        content = {
            child.component?.invoke(
                object : ComponentReplacementContext {
                    override val layoutModifier: Modifier = Modifier
                    override val textStyle: TextStyle? = child.textStyle
                }
            )
        },
        measurePolicy = { measurables, constraints ->
            // Just overlay everything, like a box, but with exactly the incoming
            // constraints applied.
            val placeables = measurables.map { child -> child.measure(constraints) }
            val layoutWidth = (placeables.maxByOrNull { it.width }?.width ?: constraints.minWidth)
            val layoutHeight =
                (placeables.maxByOrNull { it.height }?.height ?: constraints.minHeight)
            // Place all children at the origin (with the same size constraints that
            // we were given).
            layout(layoutWidth, layoutHeight) {
                val x = -scrollOffset.value.x.roundToInt()
                val y = -scrollOffset.value.y.roundToInt()
                placeables.forEach { child -> child.placeRelative(x, y) }
            }
        },
    )
}
