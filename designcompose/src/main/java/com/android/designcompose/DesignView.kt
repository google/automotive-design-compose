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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.designcompose.annotation.DesignMetaKey
import com.android.designcompose.common.DocumentServerParams
import com.android.designcompose.serdegen.Action
import com.android.designcompose.serdegen.ComponentInfo
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.serdegen.Overflow
import com.android.designcompose.serdegen.OverflowDirection
import com.android.designcompose.serdegen.Reaction
import com.android.designcompose.serdegen.Trigger
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewStyle
import java.util.Optional
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
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
                                min(1f, (numCompositionsSinceTimeout - 1).toFloat() / 100f)
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
                    style = style
                )
            }
        }
    }

data class ParentComponentInfo(val instanceId: String, val componentInfo: ComponentInfo)

// DebugNodeManager keeps track of the size and positions of all Figma nodes that we are rendering
// so that we can do a post render pass and draw the node names on top.
internal object DebugNodeManager {
    internal data class NodePosition(
        val id: String,
        val nodeName: String,
        val position: Offset,
        val size: IntSize,
        val color: Color,
    )
    private val showNodes: MutableLiveData<Boolean> = MutableLiveData(false)
    private val showRecomposition: MutableLiveData<Boolean> = MutableLiveData(false)
    private val nodes: SnapshotStateMap<Int, NodePosition> = mutableStateMapOf()
    private var nodeId: Int = 0
    internal fun getShowNodes(): LiveData<Boolean> {
        return showNodes
    }
    internal fun setShowNodes(show: Boolean) {
        if (!show) nodes.clear()
        showNodes.postValue(show)
    }
    internal fun getShowRecomposition(): LiveData<Boolean> {
        return showRecomposition
    }
    internal fun setShowRecomposition(show: Boolean) {
        showRecomposition.postValue(show)
    }
    internal fun addNode(docId: String, existingId: Int, node: NodePosition): Int {
        if (
            !showNodes.value!! ||
                !node.nodeName.startsWith("#") ||
                Feedback.isDocumentIgnored(docId)
        )
            return 0
        val oldNode = nodes[existingId]
        return if (oldNode != null) {
            nodes[existingId] = node
            existingId
        } else {
            ++nodeId
            nodes[nodeId] = node
            nodeId
        }
    }
    internal fun removeNode(id: Int) {
        nodes.remove(id)
    }
    @Composable
    internal fun DrawNodeNames() {
        val show: Boolean? by showNodes.observeAsState()
        if (show == null || !show!!) return

        // For each debug node, draw a box on top of the node, then text on a partially transparent
        // colored box at the top left of the node's box
        nodes.values.forEach {
            Box(
                modifier =
                    Modifier.absoluteOffset(it.position.x.dp, it.position.y.dp)
                        .size(it.size.width.dp, it.size.height.dp)
            ) {
                BasicText(
                    it.nodeName,
                    modifier = Modifier.then(Modifier.background(it.color)),
                    style = TextStyle(color = Color(1f, 1f, 1f, 1.0f)),
                )
            }
        }
    }
}

// Represents a key press event with optional meta keys. A DesignKeyEvent can be created with a
// single character representing the key and a list of meta keys. It can also be created from a
// list of javascript key codes, which is what Figma provides for an interaction with a key event
// type trigger
data class DesignKeyEvent(val key: Char, val metaKeys: List<DesignMetaKey>) {
    companion object {
        // Construct a DesignKeyEvent from a list of javascript key codes
        fun fromJsKeyCodes(jsKeyCodes: List<Byte>): DesignKeyEvent {
            var metaKeys: ArrayList<DesignMetaKey> = arrayListOf()
            var key: Char = '0'
            jsKeyCodes
                .map { it.toInt() }
                .forEach {
                    when (it) {
                        16 -> metaKeys.add(DesignMetaKey.MetaShift)
                        17 -> metaKeys.add(DesignMetaKey.MetaCtrl)
                        18 -> metaKeys.add(DesignMetaKey.MetaAlt)
                        91 -> metaKeys.add(DesignMetaKey.MetaMeta)
                        else -> key = it.toChar()
                    }
                }

            return DesignKeyEvent(key, metaKeys)
        }
    }
}

private data class KeyAction(
    val interactionState: InteractionState,
    val action: Action,
    val targetInstanceId: String?,
    val key: String?,
    val undoInstanceId: String?
)

// Manager to handle key event injects and listeners of key events
private object KeyInjectManager {
    private val keyListenerMap: HashMap<DesignKeyEvent, HashSet<KeyAction>> = HashMap()

    // Inject a key event and dispatch any interactions on listeners of the key event
    fun injectKey(key: Char, metaKeys: List<DesignMetaKey>) {
        val keyEvent = DesignKeyEvent(key, metaKeys)
        val listeners = keyListenerMap[keyEvent]
        listeners?.forEach {
            it.interactionState.dispatch(it.action, it.targetInstanceId, it.key, it.undoInstanceId)
        }
    }

    // Register a listener for a specific key event. This happens when a view with a key event
    // trigger is composed.
    fun addListener(keyEvent: DesignKeyEvent, keyAction: KeyAction) {
        if (keyListenerMap[keyEvent].isNullOrEmpty()) keyListenerMap[keyEvent] = HashSet()
        keyListenerMap[keyEvent]?.add(keyAction)
    }

    // Remove a listener for a specific key event. This happens when a view with a key event trigger
    // is removed from composition.
    fun removeListener(keyEvent: DesignKeyEvent, keyAction: KeyAction) {
        val listeners = keyListenerMap[keyEvent]
        listeners?.remove(keyAction)
    }
}

// Public function to inject a key event
fun DesignInjectKey(key: Char, metaKeys: List<DesignMetaKey>) {
    KeyInjectManager.injectKey(key, metaKeys)
}

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
    docId: String,
    document: DocContent,
    customizations: CustomizationContext,
    interactionState: InteractionState,
    interactionScope: CoroutineScope,
    parentComponents: List<ParentComponentInfo>,
    parentLayoutInfo: ParentLayoutInfo,
    maskInfo: MaskInfo? = null,
) {
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
    val view = interactionState.nodeVariant(v.id, customizations.getKey(), document) ?: v
    var m = Modifier as Modifier

    // Use the recompose highlighter to show what is being recomposed, if the design switcher
    // checkbox
    // is checked
    val showRecomposition: Boolean? by DebugNodeManager.getShowRecomposition().observeAsState()
    if (showRecomposition == true) m = m.recomposeHighlighter()

    // Look up the appropriate target instance by looking at the destination id
    // and finding a parent that is a member of a component set that contains that
    // id.
    //
    // This ensures that we change the appropriate component if the design has embedded
    // components within components, and has actions set on some instance of an inner
    // component that should change the outer component.
    val findTargetInstanceId: (Action) -> String? = { action ->
        val destinationId =
            when (action) {
                is Action.Node -> action.destination_id.orElse(null)
                else -> null
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

    var currentTimeout = Float.MAX_VALUE
    var onTimeout: Reaction? = null

    // Extend our modifier with any reactions
    val onClickReactions: MutableList<Reaction> = ArrayList()
    val onPressReactions: MutableList<Reaction> = ArrayList()
    val onDragReactions: MutableList<Reaction> = ArrayList()
    val onKeyReactions: MutableList<Reaction> = ArrayList()
    view.reactions.ifPresent { reactions ->
        for (reaction in reactions) {
            when (reaction.trigger) {
                is Trigger.OnClick -> onClickReactions.add(reaction)
                is Trigger.OnPress -> onPressReactions.add(reaction)
                is Trigger.OnDrag -> onDragReactions.add(reaction)
                is Trigger.AfterTimeout -> {
                    if ((reaction.trigger as Trigger.AfterTimeout).timeout < currentTimeout) {
                        onTimeout = reaction
                        currentTimeout = (reaction.trigger as Trigger.AfterTimeout).timeout
                    }
                }
                is Trigger.OnKeyDown -> onKeyReactions.add(reaction)
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
                    interactionScope.launch {
                        detectTapGestures(
                            onPress = {
                                for (onPressReaction in onPressReactions) {
                                    interactionState.dispatch(
                                        onPressReaction.action,
                                        findTargetInstanceId(onPressReaction.action),
                                        customizations.getKey(),
                                        v.id
                                    )
                                }
                                setIsPressed(true)
                                val dispatchClickEvent = tryAwaitRelease()
                                for (onPressReaction in onPressReactions) {
                                    interactionState.undoDispatch(
                                        findTargetInstanceId(onPressReaction.action),
                                        v.id,
                                        customizations.getKey()
                                    )
                                }
                                if (dispatchClickEvent) {
                                    for (onClickReaction in onClickReactions) {
                                        interactionState.dispatch(
                                            onClickReaction.action,
                                            findTargetInstanceId(onClickReaction.action),
                                            customizations.getKey(),
                                            null
                                        )
                                    }
                                    // Execute tap callback if one exists
                                    if (tapCallback != null) tapCallback()
                                }
                                setIsPressed(false)
                            }
                        )
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
                            for (onDragReaction in onDragReactions) {
                                interactionState.dispatch(
                                    onDragReaction.action,
                                    findTargetInstanceId(onDragReaction.action),
                                    customizations.getKey(),
                                    null
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
        val keyTrigger = keyReaction.trigger as Trigger.OnKeyDown
        val keyEvent = DesignKeyEvent.fromJsKeyCodes(keyTrigger.key_codes)
        DisposableEffect(keyEvent) {
            val keyAction =
                KeyAction(
                    interactionState,
                    keyReaction.action,
                    findTargetInstanceId(keyReaction.action),
                    customizations.getKey(),
                    null
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
                timeout.action,
                findTargetInstanceId(timeout.action),
                customizations.getKey(),
                null
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

    val viewLayoutInfo = calcLayoutInfo(modifier, view, style, parentLayoutInfo)

    // Add various scroll modifiers depending on the overflow flag.
    // Only add scroll modifiers if not a grid layout because grid layout adds its own scrolling
    if (viewLayoutInfo !is LayoutInfoGrid) {
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

    when (view.data) {
        is ViewData.Text ->
            DesignText(
                modifier = positionModifierFunc(Color(0f, 0.6f, 0f, 0.7f)),
                text = (view.data as ViewData.Text).content,
                style = style,
                document = document,
                nodeName = view.name,
                viewLayoutInfo = viewLayoutInfo,
                customizations = customizations
            )
        is ViewData.StyledText ->
            DesignText(
                modifier = positionModifierFunc(Color(0f, 0.6f, 0f, 0.7f)),
                runs = (view.data as ViewData.StyledText).content,
                style = style,
                document = document,
                nodeName = view.name,
                viewLayoutInfo = viewLayoutInfo,
                customizations = customizations
            )
        is ViewData.Container -> {
            // Check to see whether an interaction has changed the current variant. If it did, then
            // we
            // ignore any variant properties set from @DesignVariant annotations by passing
            // Optional.empty
            // into DesignFrame's componentInfo parameter.
            val hasVariantReplacement = view.name != v.name

            // Get the mask info from parameters unless we have a child that is a mask, in which
            // case we know the mask view type is MaskParent and we create a new parent size
            // mutable state.
            var maskViewType = maskInfo?.type
            var parentSize = maskInfo?.parentSize
            if (view.hasChildMask()) {
                maskViewType = MaskViewType.MaskParent
                parentSize = remember { mutableStateOf(Size(0F, 0F)) }
            }

            DesignFrame(
                m,
                style,
                (view.data as ViewData.Container).shape,
                view.name,
                variantParentName,
                viewLayoutInfo,
                document,
                customizations,
                if (hasVariantReplacement) Optional.empty() else view.component_info,
                parentComponents,
                MaskInfo(parentSize, maskViewType),
            ) { parentLayoutInfoForChildren ->
                val customContent = customizations.getContent(view.name)
                if (customContent != null) {
                    customContent()
                } else {
                    if ((view.data as ViewData.Container).children.isNotEmpty()) {
                        // Create  a list of views to render. If the view is a mask, the second item
                        // in the pair is a list of views that they mask. This lets us iterate
                        // through all the views that a mask affects first, render them to a layer,
                        // and then render the mask itself on top with appropriate alpha blending.
                        // Note that we currently only support one mask under a parent, and we
                        // don't support unmasked nodes under a parent when there exists a mask.
                        val viewList: ArrayList<Pair<View, ArrayList<View>>> = ArrayList()
                        var currentMask: View? = null
                        (view.data as ViewData.Container).children.forEach { child ->
                            val shouldClip = child.style.overflow is Overflow.Hidden
                            if (child.isMask()) {
                                // Add the mask to the list and set the current mask
                                viewList.add(Pair(child, ArrayList()))
                                currentMask = child
                            } else if (shouldClip) {
                                // A node with clip contents ends the reach of the last mask, so
                                // add this view to the list and clear the current mask
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
                        viewList.forEach {
                            val childView = it.first
                            val maskedChildren = it.second
                            var maskViewType = MaskViewType.None
                            if (maskedChildren.isNotEmpty()) {
                                maskedChildren.forEach { maskedChild ->
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
                                        parentLayoutInfoForChildren,
                                        MaskInfo(parentSize, maskViewType),
                                    )
                                }
                                maskViewType = MaskViewType.MaskNode
                            }
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
                                parentLayoutInfoForChildren,
                                MaskInfo(parentSize, maskViewType),
                            )
                        }
                    }
                }
            }
        }
    }
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

// A global object that keeps track of the current document ID we are subscribed to.
// When switching document IDs, we notify all subscribers of the change to trigger
// a recomposition.
internal object DocumentSwitcher {
    private val subscribers: HashMap<String, ArrayList<(String) -> Unit>> = HashMap()
    private val documentSwitchHash: HashMap<String, String> = HashMap()
    private val documentSwitchReverseHash: HashMap<String, String> = HashMap()
    internal fun subscribe(originalDocId: String, setDocId: (String) -> Unit) {
        val list = subscribers[originalDocId] ?: ArrayList()
        list.add(setDocId)
        subscribers[originalDocId] = list
    }
    internal fun switch(originalDocId: String, newDocId: String) {
        if (newDocId.isEmpty()) return
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
    internal fun revertToOriginal(docId: String) {
        val originalDocId = documentSwitchReverseHash[docId]
        if (originalDocId != null) {
            switch(originalDocId, originalDocId)
            documentSwitchReverseHash.remove(docId)
        }
    }
    internal fun isNotOriginalDocId(docId: String): Boolean {
        val originalDocId = documentSwitchReverseHash[docId]
        return originalDocId != null
    }
    internal fun getSwitchedDocId(docId: String): String {
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
    OFFLINE // Live updates off (load from serialized file)
}

@Composable
fun DesignDoc(
    docName: String,
    docId: String,
    rootNodeQuery: NodeQuery,
    modifier: Modifier = Modifier,
    placeholder: (@Composable () -> Unit)? = null,
    customizations: CustomizationContext = CustomizationContext(),
    serverParams: DocumentServerParams = DocumentServerParams(),
    setDocId: (String) -> Unit = {},
    designSwitcherPolicy: DesignSwitcherPolicy = DesignSwitcherPolicy.SHOW_IF_ROOT,
    designDocReadyCallback: ((String) -> Unit)? = null,
    parentComponents: List<ParentComponentInfo> = listOf(),
) =
    DesignDocInternal(
        docName,
        docId,
        rootNodeQuery,
        modifier = modifier,
        placeholder = placeholder,
        customizations = customizations,
        serverParams = serverParams,
        setDocId = setDocId,
        designSwitcherPolicy = designSwitcherPolicy,
        designDocReadyCallback = designDocReadyCallback,
        parentComponents = parentComponents,
    )

@Composable
internal fun DesignDocInternal(
    docName: String,
    incomingDocId: String,
    rootNodeQuery: NodeQuery,
    modifier: Modifier = Modifier,
    placeholder: (@Composable () -> Unit)? = null,
    customizations: CustomizationContext = CustomizationContext(),
    serverParams: DocumentServerParams = DocumentServerParams(),
    setDocId: (String) -> Unit = {},
    designSwitcherPolicy: DesignSwitcherPolicy = DesignSwitcherPolicy.SHOW_IF_ROOT,
    liveUpdateMode: LiveUpdateMode = LiveUpdateMode.LIVE,
    designDocReadyCallback: ((String) -> Unit)? = null,
    parentComponents: List<ParentComponentInfo> = listOf(),
) {
    val docId = DocumentSwitcher.getSwitchedDocId(incomingDocId)
    val doc = DocServer.doc(docName, docId, serverParams, liveUpdateMode == LiveUpdateMode.OFFLINE)
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

    val variantParentName =
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
    val switchDocId: (String) -> Unit = { newDocId: String ->
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

    if (doc != null) {
        val startFrame = interactionState.rootNode(rootNodeQuery, doc, isRoot)
        if (startFrame != null) {
            LaunchedEffect(docId) {
                if (designDocReadyCallback != null) designDocReadyCallback(docId)
            }
            CompositionLocalProvider(LocalDesignIsRootContext provides DesignIsRoot(false)) {
                DesignView(
                    modifier,
                    startFrame,
                    variantParentName,
                    docId,
                    doc,
                    customizations,
                    interactionState,
                    interactionScope,
                    parentComponents,
                    if (isRoot) {
                        rootParentLayoutInfo
                    } else {
                        absoluteParentLayoutInfo
                    }
                )
                // If we're the root, then also paint overlays
                if (isRoot || designSwitcherPolicy == DesignSwitcherPolicy.IS_DESIGN_SWITCHER) {
                    for (overlay in interactionState.rootOverlays(doc)) {
                        DesignOverlay(overlay.frame_extras.get(), interactionState) {
                            DesignView(
                                // Consume clicks inside the overlay so that it doesn't close the
                                // overlay
                                Modifier.clickable {},
                                overlay,
                                "",
                                docId,
                                doc,
                                customizations,
                                interactionState,
                                interactionScope,
                                listOf(),
                                rootParentLayoutInfo
                            )
                        }
                    }
                }
                designSwitcher()
            }

            return
        }
        // We have a document, we have a starting frame name, but couldn't find the frame.
        if (rootFrameName.isNotEmpty()) {
            noFrameErrorMessage = "Node \"$rootFrameName\" not found in $docId"
            noFrameBgColor = Color(0xFFFFFFFF)
            println("Node not found: rootNodeQuery $rootNodeQuery rootFrameName $rootFrameName")
        }
    } else {
        // The doc is null, so either we're fetching it, or it's missing.
        noFrameErrorMessage =
            if (!DesignSettings.liveUpdatesEnabled) {
                "Document $docId not available"
            } else {
                "Fetching $docId..."
            }
    }

    // If we have a placeholder then present it.
    if (placeholder != null) {
        placeholder()
        designSwitcher()
    } else {
        CompositionLocalProvider(LocalDesignIsRootContext provides DesignIsRoot(false)) {
            Box(Modifier.fillMaxSize().background(noFrameBgColor).padding(20.dp)) {
                BasicText(text = noFrameErrorMessage, style = TextStyle(fontSize = 24.sp))
            }
            designSwitcher()
        }
    }
}
