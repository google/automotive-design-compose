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

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Density
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DesignKeyEvent
import com.android.designcompose.DocContent
import com.android.designcompose.InteractionState
import com.android.designcompose.KeyAction
import com.android.designcompose.KeyEventTracker
import com.android.designcompose.VariableState
import com.android.designcompose.asBuilder
import com.android.designcompose.defaultLayoutStyle
import com.android.designcompose.defaultNodeStyle
import com.android.designcompose.getComponent
import com.android.designcompose.getContent
import com.android.designcompose.getKey
import com.android.designcompose.getMatchingVariant
import com.android.designcompose.getTapCallback
import com.android.designcompose.getVisible
import com.android.designcompose.getVisibleState
import com.android.designcompose.mergeStyles
import com.android.designcompose.proto.OverlayBackgroundInteractionEnum
import com.android.designcompose.proto.OverlayPositionEnum
import com.android.designcompose.proto.layoutStyle
import com.android.designcompose.proto.newDimensionProtoPercent
import com.android.designcompose.proto.newViewShapeRect
import com.android.designcompose.proto.nodeStyle
import com.android.designcompose.proto.overlayBackgroundInteractionFromInt
import com.android.designcompose.proto.overlayPositionEnumFromInt
import com.android.designcompose.proto.toInt
import com.android.designcompose.proto.type
import com.android.designcompose.serdegen.Action
import com.android.designcompose.serdegen.ActionType
import com.android.designcompose.serdegen.AlignItems
import com.android.designcompose.serdegen.Background
import com.android.designcompose.serdegen.BackgroundType
import com.android.designcompose.serdegen.Color
import com.android.designcompose.serdegen.ColorOrVar
import com.android.designcompose.serdegen.ColorOrVarType
import com.android.designcompose.serdegen.ComponentInfo
import com.android.designcompose.serdegen.FlexDirection
import com.android.designcompose.serdegen.FrameExtras
import com.android.designcompose.serdegen.JustifyContent
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.serdegen.Overflow
import com.android.designcompose.serdegen.OverflowDirection
import com.android.designcompose.serdegen.PositionType
import com.android.designcompose.serdegen.Reaction
import com.android.designcompose.serdegen.RenderMethod
import com.android.designcompose.serdegen.ScrollInfo
import com.android.designcompose.serdegen.Trigger
import com.android.designcompose.serdegen.TriggerType
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewStyle
import com.android.designcompose.squooshNodeVariant
import com.android.designcompose.squooshRootNode
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

// Remember if there's a child composable for a given node, and also we return an ordered
// list of all the child composables we need to render, along with transforms etc.
internal class SquooshChildComposable(
    // If this child composable is to host an external composable (e.g.: for component replacement)
    // then this value will be non-null.
    val component: @Composable ((ComponentReplacementContext) -> Unit)?,

    // Used for node resolution for interactions
    val parentComponents: ParentComponentData?,

    // We use this to look up the transform and layout translation.
    val node: SquooshResolvedNode,
)

/// Record parent component info with a singly linked list; each child sees a straight path
/// up through the tree. This saves on allocating a new array for each node with a parent, and
/// allows us to do some optimizations like caching the hashcode to make hashmap indexing off of
/// ParentComponent O(1) instead of O(log n).
internal class ParentComponentData(
    val parent: ParentComponentData?,
    val instanceId: String,
    val componentInfo: ComponentInfo,
) {
    private val preComputedHashCode: Int =
        if (parent != null) {
            (parent.preComputedHashCode * 31 + instanceId.hashCode()) * 31 +
                componentInfo.id.hashCode()
        } else {
            instanceId.hashCode() * 31 + componentInfo.id.hashCode()
        }

    override fun hashCode(): Int {
        return preComputedHashCode
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParentComponentData

        if (preComputedHashCode != other.preComputedHashCode) return false
        if (parent != other.parent) return false
        if (instanceId != other.instanceId) return false
        if (componentInfo != other.componentInfo) return false

        return true
    }
}

/// Iterate over a given view tree recursively, applying customizations that will select
/// variants or affect layout. The output of this function is a `SquooshResolvedNode` which
/// has links to siblings and children (but no layout value).
///
/// Subsequent passes of this generated tree can set up a native layout tree, and then populate
/// the computed layout values, and finally render the view tree.
internal fun resolveVariantsRecursively(
    v: View,
    document: DocContent,
    customizations: CustomizationContext,
    variantTransition: SquooshVariantTransition,
    interactionState: InteractionState,
    keyTracker: KeyEventTracker,
    parentComponents: ParentComponentData?,
    density: Density,
    fontResourceLoader: Font.ResourceLoader,
    // XXX: This probably won't show up in any profile, but I used linked lists everywhere
    //      else to reduce the number of objects we make (especially since we run this code
    //      every recompose.
    composableList: ArrayList<SquooshChildComposable>,
    layoutIdAllocator: SquooshLayoutIdAllocator,
    variantParentName: String = "",
    isRoot: Boolean,
    variableState: VariableState,
    appContext: Context,
    customVariantTransition: CustomVariantTransition?,
    textMeasureCache: TextMeasureCache,
    componentLayoutId: Int = 0,
    overlays: List<View>? = null,
): SquooshResolvedNode? {
    if (!customizations.getVisible(v.name)) return null
    customizations.getVisibleState(v.name)?.let { if (!it.value) return null }
    var componentLayoutId = componentLayoutId
    var parentComps = parentComponents
    var overrideStyle: ViewStyle? = null
    var view = v

    // If we have a component then we might need to get an override style, and we definitely
    // need to get a different layout id.
    if (v.component_info.isPresent) {
        val componentInfo = v.component_info.get()
        parentComps = ParentComponentData(parentComponents, v.id, componentInfo)

        // Ensure that the children of this component get unique layout ids, even though there
        // may be multiple instances of the same component in one tree.
        componentLayoutId =
            computeComponentLayoutId(
                componentLayoutId,
                layoutIdAllocator.componentLayoutId(parentComps),
            )

        // Do we have an override style? This is style data which we should apply to the final style
        // even if we're swapping out our view definition for a variant.
        if (componentInfo.overrides.isPresent) {
            val overrides = componentInfo.overrides.get()
            if (overrides.style.isPresent) {
                overrideStyle = overrides.style.get()
            }
        }

        // See if we have a variant replacement; this only happens for component instances (for both
        // interaction-driven and customization-driven variant changes).

        // First ask the interaction state if there's a variant we should render instead.
        view = interactionState.squooshNodeVariant(v.id, customizations.getKey(), document) ?: v

        // If we didn't replace the component because of an interaction, we might want to replace it
        // because of a variant customization.
        if (view.name == v.name) {
            // If an interaction has not changed the current variant, then check to see if this node
            // is part of a component set with variants and if any @DesignVariant annotations
            // set variant properties that match. If so, variantNodeName will be set to the
            // name of the node with all the variants set to the @DesignVariant parameters
            //
            // We also give the variant transition system an opportunity to change the component
            // that we look up. It uses this to ensure that we have one tree with the "from"
            // variant rendered.

            val variantNodeName =
                variantTransition.selectVariant(
                    v.id,
                    customizations.getMatchingVariant(view.component_info) ?: componentInfo.name,
                )
            if (variantNodeName != null && variantNodeName != componentInfo.name) {
                // Find the view associated with the variant name
                val variantNodeQuery =
                    NodeQuery.NodeVariant(
                        variantNodeName,
                        variantParentName.ifEmpty { view.component_info.get().component_set_name },
                    )
                val variantView =
                    interactionState.squooshRootNode(
                        variantNodeQuery,
                        document,
                        isRoot,
                        customizations,
                    )
                if (variantView != null) {
                    view = variantView
                }
            }
            variantTransition.selectedVariant(v, view, customVariantTransition)
        }
    }

    // Calculate the style we're going to use. If we have an override style then we have to apply
    // that on top of the view (or variant) style.
    val style =
        if (overrideStyle == null) {
            view.style
        } else {
            // XXX-PERF: This is not needed by Battleship, and takes over 50% of the runtime of
            //           resolveVariants (not including computeTextInfo).
            mergeStyles(view.style, overrideStyle)
        }

    // Now we know the view we want to render, the style we want to use, etc. We can create
    // a record of it. After this, another pass can be done to build a layout tree. Finally,
    // layout can be performed and rendering done.
    val layoutId = computeLayoutId(componentLayoutId, v.unique_id)
    layoutIdAllocator.visitLayoutId(layoutId)

    // XXX-PERF: computeTextInfo is *super* slow. It needs to use a cache between frames.
    val textInfo =
        squooshComputeTextInfo(
            view,
            layoutId,
            density,
            document,
            customizations,
            fontResourceLoader,
            variableState,
            appContext = appContext,
            textMeasureCache = textMeasureCache,
        )
    val resolvedView = SquooshResolvedNode(view, style, layoutId, textInfo, v.id, layoutNode = null)

    if (view.data is ViewData.Container) {
        val viewData = view.data as ViewData.Container
        var previousChild: SquooshResolvedNode? = null

        for (child in viewData.children) {
            val childResolvedNode =
                resolveVariantsRecursively(
                    child,
                    document,
                    customizations,
                    variantTransition,
                    interactionState,
                    keyTracker,
                    parentComps,
                    density,
                    fontResourceLoader,
                    composableList,
                    layoutIdAllocator,
                    "",
                    false,
                    variableState,
                    appContext = appContext,
                    textMeasureCache = textMeasureCache,
                    customVariantTransition = customVariantTransition,
                    componentLayoutId = componentLayoutId,
                ) ?: continue

            childResolvedNode.parent = resolvedView

            if (resolvedView.firstChild == null) resolvedView.firstChild = childResolvedNode
            if (previousChild != null) previousChild.nextSibling = childResolvedNode

            previousChild = childResolvedNode
        }
    }

    // Find out if we have some supported interactions; currently that's just on press and on click.
    // We'll add timeouts and the others later...
    var hasSupportedInteraction = false
    view.reactions.ifPresent { reactions ->
        reactions.forEach { r ->
            hasSupportedInteraction =
                hasSupportedInteraction ||
                    r.trigger.type is TriggerType.Click ||
                    r.trigger.type is TriggerType.Press
            if (r.trigger.type is TriggerType.KeyDown) {
                // Register to be a listener for key reactions on this node
                val keyTrigger = r.trigger.type as TriggerType.KeyDown
                val keyEvent = DesignKeyEvent.fromJsKeyCodes(keyTrigger.value.key_codes)
                val keyAction =
                    KeyAction(
                        interactionState,
                        r.action.get(),
                        findTargetInstanceId(document, parentComps, r.action.get()),
                        customizations.getKey(),
                        null,
                    )
                keyTracker.addListener(keyEvent, keyAction)
            }
        }
    }
    val tapCallback = customizations.getTapCallback(view)
    if (tapCallback != null) hasSupportedInteraction = true

    // If this node has a content customization, then we make a special record of it so that we can
    // zip through all of them after layout and render them in the right location.
    val replacementContent = customizations.getContent(view.name)
    val replacementComponent = customizations.getComponent(view.name)
    if (replacementComponent != null) {
        composableList.add(
            SquooshChildComposable(
                component = replacementComponent,
                node = resolvedView,
                parentComponents = parentComps,
            )
        )
        // Make sure that the renderer knows that it needs to do an external render for this
        // node.
        resolvedView.needsChildRender = true
    } else if (replacementContent != null) {
        // Replacement Content represents a (short, non-virtualized) list of child composables.
        // We want these child composables to be laid out inside of this container using the
        // layout properties set up in the DesignCompose document (so if a designer set the list
        // to wrap and center align its children, then the Composables coming in here should be
        // presented in that way.
        //
        // To do this, we must synthesize a ResolvedSquooshNode for each child, and also consult
        // its intrinsic size before we perform layout.
        var previousReplacementChild: SquooshResolvedNode? = null
        for (idx in 0..<replacementContent.count) {
            val childComponent = replacementContent.content(idx)
            val replacementChild =
                generateReplacementListChildNode(resolvedView, idx, layoutIdAllocator)
            if (previousReplacementChild != null)
                previousReplacementChild.nextSibling = replacementChild
            else resolvedView.firstChild = replacementChild
            previousReplacementChild = replacementChild

            composableList.add(
                SquooshChildComposable(
                    component = @Composable { childComponent() },
                    node = replacementChild,
                    parentComponents = parentComps,
                )
            )
        }
    } else if (hasSupportedInteraction) {
        // Add a SquooshChildComposable to handle the interaction.
        composableList.add(
            SquooshChildComposable(
                component = null,
                node = resolvedView,
                parentComponents = parentComps,
            )
        )
    }

    // Roots also get overlays
    if (overlays != null) {
        for (overlay in overlays) {
            val overlayExtras = overlay.frame_extras.getOrNull() ?: continue

            // We want to ensure that the background close interaction appears after the regular
            // content but before the child content. We can't construct a `SquooshChildComposable`
            // without the background node, which we use the style of the overlay itself to
            // construct (although perhaps we should construct the overlay background from entirely
            // whole cloth).
            val interactionInsertionPoint = composableList.size

            // Resolve the tree for the overlay content.
            val overlayContent =
                resolveVariantsRecursively(
                    overlay,
                    document,
                    customizations,
                    variantTransition,
                    interactionState,
                    keyTracker,
                    null,
                    density,
                    fontResourceLoader,
                    composableList,
                    layoutIdAllocator,
                    "",
                    false,
                    variableState,
                    appContext = appContext,
                    textMeasureCache = textMeasureCache,
                    customVariantTransition = customVariantTransition,
                    componentLayoutId = componentLayoutId,
                ) ?: continue

            // Make a synthetic parent for the overlay.
            val overlayContainer =
                generateOverlayNode(overlayExtras, overlayContent, layoutIdAllocator)
            // Append to the root
            var lastSibling = resolvedView.firstChild
            while (lastSibling?.nextSibling != null) lastSibling = lastSibling.nextSibling
            if (lastSibling != null) lastSibling.nextSibling = overlayContainer
            else resolvedView.firstChild = overlayContainer

            // Add a SquooshChildComposable to handle the click. An overlay either takes the
            // click and closes, or takes the click and does nothing, so either way this is
            // the correct thing to do.
            composableList.add(
                interactionInsertionPoint,
                SquooshChildComposable(
                    component = null,
                    node = overlayContainer,
                    parentComponents = parentComps,
                ),
            )
        }
    }

    return resolvedView
}

/// Create a SquooshResolvedNode for a content replacement list child. The minimum width and
/// height will come later (at layout time) by asking the Composable child for its intrinsic
/// size.
private fun generateReplacementListChildNode(
    node: SquooshResolvedNode,
    childIdx: Int,
    layoutIdAllocator: SquooshLayoutIdAllocator,
): SquooshResolvedNode {
    val itemStyle = ViewStyle.Builder()
    val layoutStyle = defaultLayoutStyle()
    val nodeStyle = defaultNodeStyle()

    itemStyle.layout_style = Optional.of(layoutStyle.build())
    itemStyle.node_style = Optional.of(nodeStyle.build())

    val listChildViewData = ViewData.Container.Builder()
    listChildViewData.shape = newViewShapeRect(false)

    listChildViewData.children = emptyList()

    val listChildScrollInfo = ScrollInfo.Builder()
    listChildScrollInfo.paged_scrolling = false
    listChildScrollInfo.overflow = OverflowDirection.None()

    val listChildView = View.Builder()
    listChildView.unique_id = 0 // This is unused.
    listChildView.id = "replacement-${node.view.id}-${childIdx}"
    listChildView.name = "Replacement List ${node.view.name} / $childIdx"
    listChildView.component_info = Optional.empty()
    listChildView.reactions = Optional.empty()
    listChildView.frame_extras = Optional.empty()
    listChildView.scroll_info = listChildScrollInfo.build()
    listChildView.style = itemStyle.build()
    listChildView.data = listChildViewData.build()
    listChildView.design_absolute_bounding_box = Optional.empty()
    listChildView.render_method = RenderMethod.None()
    listChildView.explicit_variable_modes = Optional.empty()

    val listLayoutId = layoutIdAllocator.listLayoutId(node.layoutId)
    val layoutId = computeSyntheticListItemLayoutId(listLayoutId, childIdx)
    layoutIdAllocator.visitLayoutId(layoutId)

    val listChildNode =
        SquooshResolvedNode(
            view = listChildView.build(),
            style = listChildView.style,
            layoutId = layoutId,
            textInfo = null,
            unresolvedNodeId = "list-child-${node.unresolvedNodeId}-${childIdx}",
            firstChild = null,
            nextSibling = null,
            parent = node,
            computedLayout = null,
            needsChildRender = true,
            needsChildLayout = true, // Important
        )

    return listChildNode
}

/// Create a SquooshResolvedNode for an overlay, with the appropriate layout style set up.
private fun generateOverlayNode(
    overlay: FrameExtras,
    node: SquooshResolvedNode,
    layoutIdAllocator: SquooshLayoutIdAllocator,
): SquooshResolvedNode {
    // Make a view based on the child node which uses a synthesized style.
    val overlayStyle = node.style.asBuilder()
    val layoutStyle = node.style.layoutStyle.asBuilder()
    val nodeStyle = node.style.nodeStyle.asBuilder()
    nodeStyle.overflow = Overflow.Visible().toInt()
    layoutStyle.position_type = PositionType.Absolute().toInt()
    layoutStyle.top = newDimensionProtoPercent(0.0f)
    layoutStyle.left = newDimensionProtoPercent(0.0f)
    layoutStyle.right = newDimensionProtoPercent(0.0f)
    layoutStyle.bottom = newDimensionProtoPercent(0.0f)
    layoutStyle.width = newDimensionProtoPercent(1.0f)
    layoutStyle.height = newDimensionProtoPercent(1.0f)
    layoutStyle.flex_direction = FlexDirection.Column().toInt()
    when (overlayPositionEnumFromInt(overlay.overlay_position_type)) {
        OverlayPositionEnum.TOP_LEFT -> {
            layoutStyle.justify_content = JustifyContent.FlexStart().toInt() // Y
            layoutStyle.align_items = AlignItems.FlexStart().toInt() // X
        }
        OverlayPositionEnum.TOP_CENTER -> {
            layoutStyle.justify_content = JustifyContent.FlexStart().toInt() // Y
            layoutStyle.align_items = AlignItems.Center().toInt() // X
        }
        OverlayPositionEnum.TOP_RIGHT -> {
            layoutStyle.justify_content = JustifyContent.FlexStart().toInt() // Y
            layoutStyle.align_items = AlignItems.FlexEnd().toInt() // X
        }
        OverlayPositionEnum.BOTTOM_LEFT -> {
            layoutStyle.justify_content = JustifyContent.FlexEnd().toInt() // Y
            layoutStyle.align_items = AlignItems.FlexStart().toInt() // X
        }
        OverlayPositionEnum.BOTTOM_CENTER -> {
            layoutStyle.justify_content = JustifyContent.FlexEnd().toInt() // Y
            layoutStyle.align_items = AlignItems.Center().toInt() // X
        }
        OverlayPositionEnum.BOTTOM_RIGHT -> {
            layoutStyle.justify_content = JustifyContent.FlexEnd().toInt() // Y
            layoutStyle.align_items = AlignItems.FlexEnd().toInt() // X
        }
        // Center and Manual both are centered; not clear how to implement manual positioning
        // without making a layout-dependent query.
        else -> {
            layoutStyle.justify_content = JustifyContent.Center().toInt()
            layoutStyle.align_items = AlignItems.Center().toInt()
        }
    }
    nodeStyle.backgrounds = listOf()

    overlay.overlay_background.getOrNull()?.color?.getOrNull()?.let { color ->
        val colorBuilder = Color.Builder()
        colorBuilder.r = (color.r * 255.0).toInt()
        colorBuilder.g = (color.g * 255.0).toInt()
        colorBuilder.b = (color.b * 255.0).toInt()
        colorBuilder.a = (color.a * 255.0).toInt()
        nodeStyle.backgrounds =
            listOf(
                Background(
                    Optional.of(
                        BackgroundType.Solid(
                            ColorOrVar(Optional.of(ColorOrVarType.Color(colorBuilder.build())))
                        )
                    )
                )
            )
    }
    overlayStyle.layout_style = Optional.of(layoutStyle.build())
    overlayStyle.node_style = Optional.of(nodeStyle.build())
    val style = overlayStyle.build()

    // Now synthesize a view.
    val overlayViewData = ViewData.Container.Builder()
    overlayViewData.shape = newViewShapeRect(false)
    overlayViewData.children = listOf(node.view)

    val overlayScrollInfo = ScrollInfo.Builder()
    overlayScrollInfo.paged_scrolling = false
    overlayScrollInfo.overflow = OverflowDirection.None()

    val overlayView = View.Builder()
    overlayView.unique_id = (node.view.unique_id + 0x2000).toShort()
    overlayView.id = "overlay-${node.view.id}"
    overlayView.name = "Overlay ${node.view.name}"
    overlayView.component_info = Optional.empty()
    overlayView.reactions =
        if (
            overlayBackgroundInteractionFromInt(overlay.overlay_background_interaction) ==
                OverlayBackgroundInteractionEnum.CLOSE_ON_CLICK_OUTSIDE
        ) {
            // Synthesize a reaction that will close the overlay.
            val r = Reaction.Builder()
            r.trigger = Optional.of(Trigger(Optional.of(TriggerType.Click(com.novi.serde.Unit()))))
            r.action = Optional.of(Action(Optional.of(ActionType.Close(com.novi.serde.Unit()))))
            Optional.of(listOf(r.build()))
        } else {
            Optional.empty()
        }
    overlayView.frame_extras = Optional.empty()
    overlayView.scroll_info = overlayScrollInfo.build()
    overlayView.style = style
    overlayView.data = overlayViewData.build()
    overlayView.design_absolute_bounding_box = Optional.empty()
    overlayView.render_method = RenderMethod.None()
    overlayView.explicit_variable_modes = Optional.empty()

    val overlayLayoutId = layoutIdAllocator.listLayoutId(node.layoutId)
    val layoutId =
        computeSyntheticOverlayLayoutId(
            overlayLayoutId,
            0,
        ) // XXX: What about multiple overlays of the same content?
    layoutIdAllocator.visitLayoutId(layoutId)

    val overlayNode =
        SquooshResolvedNode(
            view = overlayView.build(),
            style = style,
            layoutId = layoutId,
            textInfo = null,
            unresolvedNodeId = "overlay-${node.unresolvedNodeId}",
            firstChild = node,
            nextSibling = null,
            parent = node.parent,
            computedLayout = null,
            needsChildRender = false,
        )
    node.parent = overlayNode

    return overlayNode
}
