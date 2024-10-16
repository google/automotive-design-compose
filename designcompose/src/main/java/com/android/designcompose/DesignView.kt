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

import android.os.Trace.beginSection
import android.os.Trace.endSection
import android.util.Log
import androidx.annotation.Discouraged
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.common.DocumentServerParams
import com.android.designcompose.proto.type
import com.android.designcompose.serdegen.Action
import com.android.designcompose.serdegen.ActionType
import com.android.designcompose.serdegen.ComponentInfo
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.serdegen.Overflow
import com.android.designcompose.serdegen.OverflowDirection
import com.android.designcompose.serdegen.Reaction
import com.android.designcompose.serdegen.TriggerType
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewStyle
import com.android.designcompose.squoosh.SquooshRoot
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// This debugging modifier draws a border around elements that are recomposing. The border increases
// in size and interpolates from red to green as more recompositions occur before a timeout. This
// code is borrowed from the Play Store
@Stable fun Modifier.recomposeHighlighter(): Modifier = this.then(recomposeModifier)

// Use a single instance + @Stable to ensure that recompositions can enable skipping optimizations
// Modifier.composed will still remember unique data per call site. If the FinskyPref is updated,
// the process is restarted from debug options.
private val recomposeModifier =
    Modifier.composed(inspectorInfo = debugInspectorInfo { name = "recomposeHighlighter" }) {
        // The total number of compositions that have occurred. We're not using a State<> here be
        // able
        // to read/write the value without invalidating (which would cause infinite recomposition).
        val totalCompositions = remember { arrayOf(0L) }
        totalCompositions[0]++

        // The value of totalCompositions at the last timeout.
        val totalCompositionsAtLastTimeout = remember { mutableStateOf(0L) }

        // Start the timeout, and reset everytime there's a recomposition. (Using totalCompositions
        // as
        // the key is really just to cause the timer to restart every composition).
        LaunchedEffect(totalCompositions[0]) {
            delay(3000)
            totalCompositionsAtLastTimeout.value = totalCompositions[0]
        }

        Modifier.drawWithCache {
            onDrawWithContent {
                // Draw actual content.
                drawContent()

                // Below is to draw the highlight, if necessary. A lot of the logic is copied from
                // Modifier.border
                val numCompositionsSinceTimeout =
                    totalCompositions[0] - totalCompositionsAtLastTimeout.value

                val hasValidBorderParams = size.minDimension > 0f
                if (!hasValidBorderParams || numCompositionsSinceTimeout <= 0) {
                    return@onDrawWithContent
                }

                val (color, strokeWidthPx) =
                    when (numCompositionsSinceTimeout) {
                        // We need at least one composition to draw, so draw the smallest border
                        // color in
                        // blue.
                        1L -> Color.Blue to 1f
                        // 2 compositions is _probably_ okay.
                        2L -> Color.Green to 2.dp.toPx()
                        // 3 or more compositions before timeout may indicate an issue. lerp the
                        // color from
                        // yellow to red, and continually increase the border size.
                        else -> {
                            lerp(
                                Color.Yellow.copy(alpha = 0.8f),
                                Color.Red.copy(alpha = 0.5f),
                                min(1f, (numCompositionsSinceTimeout - 1).toFloat() / 100f),
                            ) to numCompositionsSinceTimeout.toInt().dp.toPx()
                        }
                    }

                val halfStroke = strokeWidthPx / 2
                val topLeft = Offset(halfStroke, halfStroke)
                val borderSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

                val fillArea = (strokeWidthPx * 2) > size.minDimension
                val rectTopLeft = if (fillArea) Offset.Zero else topLeft
                val size = if (fillArea) size else borderSize
                val style = if (fillArea) Fill else Stroke(strokeWidthPx)

                drawRect(
                    brush = SolidColor(color),
                    topLeft = rectTopLeft,
                    size = size,
                    style = style,
                )
            }
        }
    }

data class ParentComponentInfo(val instanceId: String, val componentInfo: ComponentInfo)

// When rendering with masks, we need to differentiate nodes that are masks themselves, a node with
// a child that is a mask, and normal nodes with no masking involved.
internal enum class MaskViewType {
    None,
    MaskNode,
    MaskParent,
}

// Helper class that groups together data needed to support masks
internal data class MaskInfo(
    // Keep track of the size of a node that has a child that is a mask
    val parentSize: MutableState<Size>?,
    // The type of node with respect to masking
    val type: MaskViewType?,
)

@Composable
internal fun DesignView(
    modifier: Modifier = Modifier,
    v: View,
    variantParentName: String,
    docId: DesignDocId,
    document: DocContent,
    customizations: CustomizationContext,
    interactionState: InteractionState,
    interactionScope: CoroutineScope,
    parentComponents: List<ParentComponentInfo>,
    maskInfo: MaskInfo? = null,
): Boolean {
    var parentLayout = LocalParentLayoutInfo.current
    // Set the 'designComposeRendered' flag to true so that the caller knows that some child in
    // the call tree was composed with DesignView.
    LocalParentLayoutInfo.current?.designComposeRendered = true
    val parentComps =
        if (v.component_info.isPresent) {
            val pc = parentComponents.toMutableList()
            pc.add(ParentComponentInfo(v.id, v.component_info.get()))
            pc
        } else {
            parentComponents
        }

    // Do we have an override style? This is style data which we should apply to the final style
    // even if we're swapping out our view definition for a variant.
    var overrideStyle: ViewStyle? = null
    if (v.component_info.isPresent) {
        val componentInfo = v.component_info.get()
        if (componentInfo.overrides.isPresent) {
            val overrides = componentInfo.overrides.get()
            if (overrides.style.isPresent) {
                overrideStyle = overrides.style.get()
            }
        }
    }

    // See if we've got a replacement node from an interaction
    var view = interactionState.nodeVariant(v.id, customizations.getKey(), document) ?: v
    val hasVariantReplacement = view.name != v.name
    var variantParentName = variantParentName
    if (!hasVariantReplacement) {
        // If an interaction has not changed the current variant, then check to see if this node
        // is part of a component set with variants and if any @DesignVariant annotations
        // set variant properties that match. If so, variantNodeName will be set to the
        // name of the node with all the variants set to the @DesignVariant parameters
        var variantNodeName = customizations.getMatchingVariant(view.component_info)
        if (variantNodeName != null) {
            // Find the view associated with the variant name
            val variantNodeQuery =
                NodeQuery.NodeVariant(
                    variantNodeName,
                    variantParentName.ifEmpty {
                        // Get the component set name out of component_info and use it to construct
                        // the NodeVariant. We know component_info is present here since
                        // variantNodeName is not null.
                        view.component_info.get().component_set_name
                    },
                )
            val isRoot = LocalDesignIsRootContext.current.isRoot
            val variantView =
                interactionState.rootNode(variantNodeQuery, document, isRoot, customizations)
            if (variantView != null) {
                view = variantView
                variantView.component_info.ifPresent { variantParentName = it.component_set_name }
            }
        }
    }

    var m = Modifier as Modifier

    // Use the recompose highlighter to show what is being recomposed, if the design switcher
    // checkbox
    // is checked
    val showRecomposition: Boolean? by DebugNodeManager.getShowRecomposition().observeAsState()
    if (showRecomposition == true) m = m.recomposeHighlighter()

    // Look up the appropriate target instance by looking at the destination id
    // and finding a parent that is a member of a component set that contains that
    // id.
    // ``
    // This ensures that we change the appropriate component if the design has embedded
    // components within components, and has actions set on some instance of an inner
    // component that should change the outer component.
    val findTargetInstanceId: (Action) -> String? = { action ->
        val destinationId: String? =
            action.action_type.getOrNull()?.let { actionType ->
                when (actionType) {
                    is ActionType.Node -> actionType.value.destination_id.getOrNull()
                    else -> null
                }
            }

        var targetInstanceId: String? = null
        if (destinationId != null) {
            val componentSetId = document.c.document.component_sets[destinationId]
            if (componentSetId != null) {
                // Look up our list of parent components and try to find one that is a member of
                // this component set.
                for (parentComponentInfo in parentComps.reversed()) {
                    if (
                        componentSetId ==
                            document.c.document.component_sets[parentComponentInfo.componentInfo.id]
                    ) {
                        targetInstanceId = parentComponentInfo.instanceId
                        break
                    }
                }
            }
        }
        targetInstanceId
    }

    // Get a unique ID identifying this composable. We use this to register and unregister
    // this view for layout and as a parent ID for children
    val layoutId = remember { LayoutManager.getNextLayoutId() }

    var currentTimeout = Float.MAX_VALUE
    var onTimeout: Reaction? = null

    // Extend our modifier with any reactions
    val onClickReactions: MutableList<Reaction> = ArrayList()
    val onPressReactions: MutableList<Reaction> = ArrayList()
    val onDragReactions: MutableList<Reaction> = ArrayList()
    val onKeyReactions: MutableList<Reaction> = ArrayList()

    view.reactions.ifPresent { reactions ->
        for (reaction in reactions) {
            reaction.trigger.type?.run {
                when (this) {
                    is TriggerType.Click -> onClickReactions.add(reaction)
                    is TriggerType.Press -> onPressReactions.add(reaction)
                    is TriggerType.Drag -> onDragReactions.add(reaction)
                    is TriggerType.AfterTimeout -> {
                        if (value.timeout < currentTimeout) {
                            onTimeout = reaction
                            currentTimeout = value.timeout
                        } else { // TSILB - needed by `when
                        }
                    }
                    is TriggerType.KeyDown -> onKeyReactions.add(reaction)
                    else -> {}
                }
            }
        }
    }

    var tapCallback = customizations.getTapCallback(view.name)

    // If no tap callback was found but this is a variant of a component set, look for a tap
    // callback
    // in the component set, whose name is stored in variantParentName
    if (tapCallback == null && variantParentName.isNotEmpty())
        tapCallback = customizations.getTapCallback(variantParentName)

    // If we are currently being pressed, then we must continue to include a `pointerInput` block,
    // even if the variant we're currently showing doesn't have any click or press interactions. If
    // we don't do this, then when we CHANGE_TO a variant that has no interactions but is an
    // ON_PRESS
    // target, then Compose will think we've lost interest in the gesture and cancel it for us.
    //
    // Even though we re-run this block on recompose, our `onPress` closure runs with its scope for
    // the duration of the gesture.
    //
    // This is covered in the interaction test document's "Combos" screen; the purple button has no
    // interactions in its ON_PRESS variant.
    val (isPressed, setIsPressed) = remember { mutableStateOf(false) }
    if (
        onClickReactions.isNotEmpty() ||
            onPressReactions.isNotEmpty() ||
            isPressed ||
            tapCallback != null
    ) {
        m =
            m.then(
                Modifier.pointerInput(onClickReactions, onPressReactions, isPressed, tapCallback) {
                    // We get this scope from the DesignDocument. It means that our tap gesture
                    // detection
                    // doesn't get cancelled if this Composable gets destroyed before the touch is
                    // released (e.g.: because the action that the "touch down" event triggers hides
                    // this Composable; if the action is "while pressed" then we really need to
                    // release
                    // event so that the interaction state can revert the change).
                    //
                    // The interaction test document covers all of these cases.
                    interactionScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        beginSection("DesignView InteractionScope")
                        detectTapGestures(
                            onPress = {
                                onPressReactions
                                    .filter { it.action.isPresent }
                                    .forEach {
                                        val action = it.action.get()
                                        interactionState.dispatch(
                                            action,
                                            findTargetInstanceId(action),
                                            customizations.getKey(),
                                            v.id,
                                        )
                                    }

                                setIsPressed(true)
                                val dispatchClickEvent = tryAwaitRelease()
                                onPressReactions
                                    .filter { it.action.isPresent }
                                    .forEach {
                                        val action = it.action.get()
                                        interactionState.undoDispatch(
                                            findTargetInstanceId(action),
                                            v.id,
                                            customizations.getKey(),
                                        )
                                    }
                                if (dispatchClickEvent) {
                                    onClickReactions
                                        .filter { it.action.isPresent }
                                        .forEach {
                                            val action = it.action.get()
                                            interactionState.dispatch(
                                                action,
                                                findTargetInstanceId(action),
                                                customizations.getKey(),
                                                null,
                                            )
                                        }
                                    // Execute tap callback if one exists
                                    if (tapCallback != null) tapCallback()
                                }
                                setIsPressed(false)
                            }
                        )
                        endSection()
                    }
                }
            )
    }
    if (onDragReactions.isNotEmpty()) {
        m =
            m.then(
                Modifier.pointerInput(onDragReactions) {
                    detectDragGestures(
                        onDragStart = {
                            onDragReactions
                                .filter { it.action.isPresent }
                                .forEach {
                                    val action = it.action.get()
                                    interactionState.dispatch(
                                        action,
                                        findTargetInstanceId(action),
                                        customizations.getKey(),
                                        null,
                                    )
                                }
                        }
                    ) { change, _ ->
                        change.consumeAllChanges()
                    }
                }
            )
    }

    // Register to be a listener for key reactions on this node
    for (keyReaction in onKeyReactions) {
        val keyTrigger = keyReaction.trigger.get().trigger_type.get() as TriggerType.KeyDown
        val keyEvent = DesignKeyEvent.fromJsKeyCodes(keyTrigger.value.key_codes)
        DisposableEffect(keyEvent) {
            val keyAction =
                KeyAction(
                    interactionState,
                    keyReaction.action.get(),
                    findTargetInstanceId(keyReaction.action.get()),
                    customizations.getKey(),
                    null,
                )
            KeyInjectManager.addListener(keyEvent, keyAction)
            onDispose { KeyInjectManager.removeListener(keyEvent, keyAction) }
        }
    }

    // Use a coroutine delay to implement our timeout
    onTimeout?.let { timeout ->
        LaunchedEffect(timeout, view.id) {
            delay((currentTimeout * 1000.0).toLong())
            interactionState.dispatch(
                timeout.action.get(),
                findTargetInstanceId(timeout.action.get()),
                customizations.getKey(),
                null,
            )
        }
    }

    // Calculate the style we're going to use. If we have an override style then we have to apply
    // that on top of the view (or variant) style.
    val style =
        if (overrideStyle != null) {
            mergeStyles(view.style, overrideStyle)
        } else {
            view.style
        }

    val viewLayoutInfo = calcLayoutInfo(modifier, view, style)

    // Add various scroll modifiers depending on the overflow flag.
    // Only add scroll modifiers if not a grid layout because grid layout adds its own scrolling
    if (viewLayoutInfo !is LayoutInfoGrid && viewLayoutInfo !is LayoutInfoAbsolute) {
        when (view.scroll_info.overflow) {
            is OverflowDirection.VERTICAL_SCROLLING -> {
                m = Modifier.verticalScroll(rememberScrollState()).then(m)
            }
            is OverflowDirection.HORIZONTAL_SCROLLING -> {
                m = Modifier.horizontalScroll(rememberScrollState()).then(m)
            }
            is OverflowDirection.HORIZONTAL_AND_VERTICAL_SCROLLING -> {
                m =
                    Modifier.horizontalScroll(rememberScrollState())
                        .verticalScroll(rememberScrollState())
                        .then(m)
            }
            is OverflowDirection.NONE -> {}
        }
    }

    // When rendering debug node names, debugNodeId gets set to a non-zero value after we get
    // position information about the node and add it to the DebugNodeManager. This gets used in a
    // DisposableEffect to remove it from the DebugNodeManager when this view is no longer rendered.
    val (debugNodeId, setDebugNodeId) = remember { mutableStateOf(0) }
    DisposableEffect(debugNodeId) {
        onDispose {
            if (debugNodeId > 0) {
                DebugNodeManager.removeNode(debugNodeId)
            }
        }
    }

    // If we are rendering debug node names, positionModifierFunc takes a color and returns a
    // Modifier that adds the node debug data to the DebugNodeManager when it gets position info.
    val debugShowNodes: Boolean? by DebugNodeManager.getShowNodes().observeAsState()
    var positionModifierFunc: (color: Color) -> Modifier = { _ -> Modifier }
    if (debugShowNodes == true) {
        positionModifierFunc = { color ->
            Modifier.onGloballyPositioned { coordinates ->
                val node =
                    DebugNodeManager.NodePosition(
                        id = view.id,
                        nodeName = view.name,
                        position = coordinates.positionInRoot(),
                        size = coordinates.size,
                        color = color,
                    )
                val nodeId = DebugNodeManager.addNode(docId, debugNodeId, node)
                if (nodeId > 0) {
                    setDebugNodeId(nodeId)
                }
            }
        }
    }

    // Use blue for DesignFrame nodes and green for DesignText nodes
    m = positionModifierFunc(Color(0f, 0f, 0.8f, 0.7f)).then(m)

    parentLayout = parentLayout?.withRootIdIfNone(layoutId)
    var visible = false
    DesignParentLayout(parentLayout) {
        when (view.data) {
            is ViewData.Text ->
                visible =
                    DesignText(
                        modifier = positionModifierFunc(Color(0f, 0.6f, 0f, 0.7f)),
                        view = view,
                        text = getTextContent(LocalContext.current, view.data as ViewData.Text),
                        style = style,
                        document = document,
                        nodeName = view.name,
                        customizations = customizations,
                        layoutId = layoutId,
                    )
            is ViewData.StyledText ->
                visible =
                    DesignText(
                        modifier = positionModifierFunc(Color(0f, 0.6f, 0f, 0.7f)),
                        view = view,
                        runs =
                            getTextContent(LocalContext.current, view.data as ViewData.StyledText),
                        style = style,
                        document = document,
                        nodeName = view.name,
                        customizations = customizations,
                        layoutId = layoutId,
                    )
            is ViewData.Container -> {
                // Get the mask info from parameters unless we have a child that is a mask, in which
                // case we know the mask view type is MaskParent and we create a new parent size
                // mutable state.
                var maskViewType = maskInfo?.type
                var parentSize = maskInfo?.parentSize
                if (view.hasChildMask()) {
                    maskViewType = MaskViewType.MaskParent
                    parentSize = remember { mutableStateOf(Size(0F, 0F)) }
                }

                visible =
                    DesignFrame(
                        m,
                        view,
                        style,
                        viewLayoutInfo,
                        document,
                        customizations,
                        layoutId,
                        parentComponents,
                        MaskInfo(parentSize, maskViewType),
                    ) {
                        val customContent = customizations.getContent(view.name)
                        if (customContent != null) {
                            var rootLayoutId = parentLayout?.rootLayoutId ?: -1
                            if (rootLayoutId == -1) rootLayoutId = layoutId
                            for (i in 0 until customContent.count) {
                                DesignParentLayout(ParentLayoutInfo(layoutId, i, rootLayoutId)) {
                                    customContent.content(i)()
                                }
                            }
                        } else {
                            if ((view.data as ViewData.Container).children.isNotEmpty()) {
                                // Create  a list of views to render. If the view is a mask, the
                                // second item in the pair is a list of views that they mask. This
                                // lets us iterate through all the views that a mask affects first,
                                // render them to a layer, and then render the mask itself on top
                                // with appropriate alpha blending. Note that we currently only
                                // support one mask under a parent, and we don't support unmasked
                                // nodes under a parent when there exists a mask.
                                val viewList: ArrayList<Pair<View, ArrayList<View>>> = ArrayList()
                                var currentMask: View? = null
                                (view.data as ViewData.Container).children.forEach { child ->
                                    val shouldClip =
                                        child.style.node_style.overflow is Overflow.Hidden
                                    if (child.isMask()) {
                                        // Add the mask to the list and set the current mask
                                        viewList.add(Pair(child, ArrayList()))
                                        currentMask = child
                                    } else if (shouldClip) {
                                        // A node with clip contents ends the reach of the last
                                        // mask, so add this view to the list and clear the current
                                        // mask
                                        viewList.add(Pair(child, ArrayList()))
                                        currentMask = null
                                    } else {
                                        if (currentMask != null) {
                                            // This view is masked so add it to the mask's list
                                            viewList.last().second.add(child)
                                        } else {
                                            // This view is not masked so add it to the main list
                                            viewList.add(Pair(child, ArrayList()))
                                        }
                                    }
                                }
                                val rootLayoutId = parentLayout?.rootLayoutId ?: layoutId
                                val isWidgetAncestor =
                                    parentLayout?.listLayoutType != ListLayoutType.None ||
                                        parentLayout?.isWidgetAncestor == true
                                var childIndex = 0
                                viewList.forEach {
                                    val childView = it.first
                                    val maskedChildren = it.second
                                    var maskViewType = MaskViewType.None
                                    if (maskedChildren.isNotEmpty()) {
                                        maskedChildren.forEach { maskedChild ->
                                            val parentLayoutInfo =
                                                ParentLayoutInfo(
                                                    layoutId,
                                                    childIndex,
                                                    rootLayoutId,
                                                    isWidgetAncestor = isWidgetAncestor,
                                                )
                                            DesignParentLayout(parentLayoutInfo) {
                                                val show =
                                                    DesignView(
                                                        Modifier,
                                                        maskedChild,
                                                        "",
                                                        docId,
                                                        document,
                                                        customizations,
                                                        interactionState,
                                                        interactionScope,
                                                        parentComps,
                                                        MaskInfo(parentSize, maskViewType),
                                                    )
                                                if (show) ++childIndex
                                            }
                                        }
                                        maskViewType = MaskViewType.MaskNode
                                    }
                                    val parentLayoutInfo =
                                        ParentLayoutInfo(
                                            layoutId,
                                            childIndex,
                                            rootLayoutId,
                                            isWidgetAncestor = isWidgetAncestor,
                                        )
                                    DesignParentLayout(parentLayoutInfo) {
                                        val show =
                                            DesignView(
                                                Modifier,
                                                childView,
                                                "",
                                                docId,
                                                document,
                                                customizations,
                                                interactionState,
                                                interactionScope,
                                                parentComps,
                                                MaskInfo(parentSize, maskViewType),
                                            )
                                        if (show) ++childIndex
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
    return visible
}

// We want to know if we're the "root" component. For now, we'll do that using a local composition
// struct that indicates we're a "root" by default, and then gets propagated as "not a root" for
// children.
//
// This should work well if everything is a compose component, but might get confused (with multiple
// roots) when mixing Android Views and Compose views (where multiple Android Views have Compose
// children). In that case, we may need to extend the annotations to include the concept of a root.
internal data class DesignIsRoot(val isRoot: Boolean)

internal val LocalDesignIsRootContext = compositionLocalOf { DesignIsRoot(true) }

// Current customization context that contains all customizations passed through from any ancestor
val LocalCustomizationContext = compositionLocalOf { CustomizationContext() }

// Current document override ID that can be used to override the document ID specified from the
// @DesignDoc annotation
internal val LocalDocOverrideContext = compositionLocalOf { DesignDocId("") }

// Public function to set the document override ID
@Composable
@Discouraged(
    message =
        "Use of this function will override all document IDs in the tree. If more" +
            " than one root document is used, all will instead use this document ID. Use this function only" +
            " when there is no other way to set the document ID."
)
fun DesignDocOverride(docId: DesignDocId, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDocOverrideContext provides docId) { content() }
}

// A global object that keeps track of the current document ID we are subscribed to.
// When switching document IDs, we notify all subscribers of the change to trigger
// a recomposition.
internal object DocumentSwitcher {
    private val subscribers: HashMap<DesignDocId, ArrayList<(DesignDocId) -> Unit>> = HashMap()
    private val documentSwitchHash: HashMap<DesignDocId, DesignDocId> = HashMap()
    private val documentSwitchReverseHash: HashMap<DesignDocId, DesignDocId> = HashMap()

    internal fun subscribe(originalDocId: DesignDocId, setDocId: (DesignDocId) -> Unit) {
        val list = subscribers[originalDocId] ?: ArrayList()
        list.add(setDocId)
        subscribers[originalDocId] = list
    }

    internal fun switch(originalDocId: DesignDocId, newDocId: DesignDocId) {
        if (!newDocId.isValid()) return
        if (originalDocId != newDocId) {
            documentSwitchHash[originalDocId] = newDocId
            documentSwitchReverseHash[newDocId] = originalDocId
        } else {
            documentSwitchHash.remove(originalDocId)
            documentSwitchReverseHash.remove(originalDocId)
        }
        val list = subscribers[originalDocId]
        list?.forEach { it(newDocId) }
    }

    internal fun revertToOriginal(docId: DesignDocId) {
        val originalDocId = documentSwitchReverseHash[docId]
        if (originalDocId != null) {
            switch(originalDocId, originalDocId)
            documentSwitchReverseHash.remove(docId)
        }
    }

    internal fun isNotOriginalDocId(docId: DesignDocId): Boolean {
        val originalDocId = documentSwitchReverseHash[docId]
        return originalDocId != null
    }

    internal fun getSwitchedDocId(docId: DesignDocId): DesignDocId {
        return documentSwitchHash[docId] ?: docId
    }
}

enum class DesignSwitcherPolicy {
    SHOW_IF_ROOT, // Show the design switcher on root nodes
    HIDE, // Hide the design switcher
    IS_DESIGN_SWITCHER, // This is the design switcher, so don't show embed another one
}

enum class LiveUpdateMode {
    LIVE, // Live updates on
    OFFLINE, // Live updates off (load from serialized file)
}

class DesignComposeCallbacks(
    val docReadyCallback: ((DesignDocId) -> Unit)? = null,
    val newDocDataCallback: ((DesignDocId, ByteArray?) -> Unit)? = null,
)

@Composable
fun DesignDoc(
    docName: String,
    docId: DesignDocId,
    rootNodeQuery: NodeQuery,
    modifier: Modifier = Modifier,
    customizations: CustomizationContext = CustomizationContext(),
    serverParams: DocumentServerParams = DocumentServerParams(),
    setDocId: (DesignDocId) -> Unit = {},
    designSwitcherPolicy: DesignSwitcherPolicy = DesignSwitcherPolicy.SHOW_IF_ROOT,
    designComposeCallbacks: DesignComposeCallbacks? = null,
    parentComponents: List<ParentComponentInfo> = listOf(),
) {
    beginSection(DCTraces.DESIGNDOCINTERNAL)
    DesignDocInternal(
        docName,
        docId,
        rootNodeQuery,
        modifier = modifier,
        customizations = customizations,
        serverParams = serverParams,
        setDocId = setDocId,
        designSwitcherPolicy = designSwitcherPolicy,
        designComposeCallbacks = designComposeCallbacks,
        parentComponents = parentComponents,
    )
    endSection()
}

@Composable
internal fun DesignDocInternal(
    docName: String,
    incomingDocId: DesignDocId,
    rootNodeQuery: NodeQuery,
    modifier: Modifier = Modifier,
    customizations: CustomizationContext = CustomizationContext(),
    serverParams: DocumentServerParams = DocumentServerParams(),
    setDocId: (DesignDocId) -> Unit = {},
    designSwitcherPolicy: DesignSwitcherPolicy = DesignSwitcherPolicy.SHOW_IF_ROOT,
    liveUpdateMode: LiveUpdateMode = LiveUpdateMode.LIVE,
    designComposeCallbacks: DesignComposeCallbacks? = null,
    parentComponents: List<ParentComponentInfo> = listOf(),
) {
    val overrideDocId = LocalDocOverrideContext.current
    // Use the override document ID if it is not empty
    val currentDocId = if (overrideDocId.isValid()) overrideDocId else incomingDocId
    if (LocalDesignDocSettings.current.useSquoosh) {
        SquooshRoot(
            docName = docName,
            incomingDocId = currentDocId,
            rootNodeQuery = rootNodeQuery,
            modifier = modifier,
            customizationContext = customizations,
            serverParams = serverParams,
            setDocId = setDocId,
            designSwitcherPolicy = designSwitcherPolicy,
            liveUpdateMode = liveUpdateMode,
            designComposeCallbacks = designComposeCallbacks,
        )
        return
    }
    var docRenderStatus by remember { mutableStateOf(DocRenderStatus.NotAvailable) }
    val docId = DocumentSwitcher.getSwitchedDocId(currentDocId)
    val doc =
        DocServer.doc(
            docName,
            docId,
            serverParams,
            designComposeCallbacks?.newDocDataCallback,
            liveUpdateMode == LiveUpdateMode.OFFLINE,
        )
    val interactionState = InteractionStateManager.stateForDoc(docId)
    val interactionScope = rememberCoroutineScope()
    val isRoot = LocalDesignIsRootContext.current.isRoot
    val showDesignSwitcher =
        isRoot &&
            designSwitcherPolicy == DesignSwitcherPolicy.SHOW_IF_ROOT &&
            DesignSettings.liveUpdatesEnabled
    val rootFrameName =
        when (rootNodeQuery) {
            is NodeQuery.NodeName -> rootNodeQuery.value
            is NodeQuery.NodeId -> rootNodeQuery.value
            is NodeQuery.NodeVariant -> rootNodeQuery.field0
            else -> ""
        }

    var variantParentName =
        when (rootNodeQuery) {
            is NodeQuery.NodeVariant -> rootNodeQuery.field1
            else -> ""
        }

    val openLinkCallback = customizations.getOpenLinkCallback(rootFrameName)
    DisposableEffect(openLinkCallback) {
        if (openLinkCallback != null) interactionState.registerOpenLinkCallback(openLinkCallback)
        onDispose {
            if (openLinkCallback != null)
                interactionState.unregisterOpenLinkCallback(openLinkCallback)
        }
    }

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
            // Render debug node names, if turned on
            Box(Modifier.fillMaxSize()) { DebugNodeManager.DrawNodeNames() }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                DesignSwitcher(doc, docId, branchHash, switchDocId)
            }
        }
    }

    var noFrameErrorMessage = ""
    var noFrameBgColor = Color(0x00000000)

    // Reset the isRendered flag, only set it back to true if the DesignView properly displays
    if (doc != null) {
        val startFrame = interactionState.rootNode(rootNodeQuery, doc, isRoot, customizations)
        if (startFrame != null) {
            LaunchedEffect(docId) { designComposeCallbacks?.docReadyCallback?.invoke(docId) }
            CompositionLocalProvider(LocalDesignIsRootContext provides DesignIsRoot(false)) {
                // Whenever the root view changes, call deferComputations() so that we defer layout
                // calculation
                // until all views have been added
                if (isRoot) {
                    DisposableEffect(startFrame) { onDispose {} }
                    val density = LocalDensity.current.density
                    DisposableEffect(density) {
                        LayoutManager.setDensity(density)
                        onDispose {}
                    }
                }

                beginSection(DCTraces.DESIGNVIEW)
                val designViewParentLayout =
                    if (isRoot) {
                        rootParentLayoutInfo
                    } else {
                        LocalParentLayoutInfo.current
                    }
                DesignParentLayout(designViewParentLayout) {
                    if (startFrame.component_info.isPresent) {
                        // If this view is a variant but variantParentName was not set, set it here.
                        // This could happen if a node is replaced via component replacement with a
                        // variant of a component set.
                        val compInfo = startFrame.component_info.get()
                        if (variantParentName.isEmpty() && compInfo.component_set_name.isNotEmpty())
                            variantParentName = compInfo.component_set_name
                    }
                    DesignView(
                        modifier.semantics { sDocRenderStatus = docRenderStatus },
                        startFrame,
                        variantParentName,
                        docId,
                        doc,
                        customizations,
                        interactionState,
                        interactionScope,
                        parentComponents,
                    )
                }
                endSection()
                docRenderStatus = DocRenderStatus.Rendered
                // If we're the root, then also paint overlays
                if (isRoot || designSwitcherPolicy == DesignSwitcherPolicy.IS_DESIGN_SWITCHER) {
                    for (overlay in interactionState.rootOverlays(doc)) {
                        DesignOverlay(overlay.frame_extras.get(), interactionState) {
                            DesignParentLayout(rootParentLayoutInfo) {
                                DesignView(
                                    modifier = Modifier,
                                    overlay,
                                    "",
                                    docId,
                                    doc,
                                    customizations,
                                    interactionState,
                                    interactionScope,
                                    listOf(),
                                )
                            }
                        }
                    }
                }
                designSwitcher()

                // For root views, tell the layout manager that it is done loading after all
                // child composables have been called so that it can trigger a layout compute.
                if (isRoot) DisposableEffect(startFrame) { onDispose {} }
            }

            return
        }
        // We have a document, we have a starting frame name, but couldn't find the frame.
        if (rootFrameName.isNotEmpty()) {
            noFrameErrorMessage = "Node \"$rootFrameName\" not found in $docId"
            noFrameBgColor = Color(0xFFFFFFFF)
            Log.e(TAG, "Node not found: rootNodeQuery $rootNodeQuery rootFrameName $rootFrameName")
            docRenderStatus = DocRenderStatus.NodeNotFound
        }
    } else {
        // The doc is null, so either we're fetching it, or it's missing.

        if (!DesignSettings.liveUpdatesEnabled) {
            noFrameErrorMessage = "Document $docId not available"
            docRenderStatus = DocRenderStatus.NotAvailable
        } else {
            noFrameErrorMessage = "Fetching $docId..."
            docRenderStatus = DocRenderStatus.Fetching
        }
    }
    CompositionLocalProvider(LocalDesignIsRootContext provides DesignIsRoot(false)) {
        Box(
            Modifier.fillMaxSize().background(noFrameBgColor).padding(20.dp).semantics {
                sDocRenderStatus = docRenderStatus
            }
        ) {
            BasicText(text = noFrameErrorMessage, style = TextStyle(fontSize = 24.sp))
        }
        designSwitcher()
    }
}
