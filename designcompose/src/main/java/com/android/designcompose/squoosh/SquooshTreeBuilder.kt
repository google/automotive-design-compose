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
import com.android.designcompose.DocContent
import com.android.designcompose.InteractionState
import com.android.designcompose.ReplacementContent
import com.android.designcompose.VariableState
import com.android.designcompose.asBuilder
import com.android.designcompose.getComponent
import com.android.designcompose.getContent
import com.android.designcompose.getKey
import com.android.designcompose.getMatchingVariant
import com.android.designcompose.getVisible
import com.android.designcompose.getVisibleState
import com.android.designcompose.mergeStyles
import com.android.designcompose.serdegen.Action
import com.android.designcompose.serdegen.AlignItems
import com.android.designcompose.serdegen.Background
import com.android.designcompose.serdegen.Color
import com.android.designcompose.serdegen.ColorOrVar
import com.android.designcompose.serdegen.ComponentInfo
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.FlexDirection
import com.android.designcompose.serdegen.FrameExtras
import com.android.designcompose.serdegen.JustifyContent
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.serdegen.Overflow
import com.android.designcompose.serdegen.OverflowDirection
import com.android.designcompose.serdegen.OverlayBackgroundInteraction
import com.android.designcompose.serdegen.OverlayPositionType
import com.android.designcompose.serdegen.PositionType
import com.android.designcompose.serdegen.Reaction
import com.android.designcompose.serdegen.RenderMethod
import com.android.designcompose.serdegen.ScrollInfo
import com.android.designcompose.serdegen.Trigger
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewShape
import com.android.designcompose.serdegen.ViewStyle
import com.android.designcompose.squooshNodeVariant
import com.android.designcompose.squooshRootNode
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

// Remember if there's a child composable for a given node, and also we return an ordered
// list of all the child composables we need to render, along with transforms etc.
internal class SquooshChildComposable(
    // One of these will be populated if this is for a child composable. If they're both
    // null, then this child composable exists just for event handling.
    val component: @Composable ((ComponentReplacementContext) -> Unit)?,
    val content: ReplacementContent?,

    // Used for node resolution for interactions
    val parentComponents: ParentComponentData?,

    // We use this to look up the transform and layout translation.
    val node: SquooshResolvedNode
)

/// Record parent component info with a singly linked list; each child sees a straight path
/// up through the tree. This saves on allocating a new array for each node with a parent, and
/// allows us to do some optimizations like caching the hashcode to make hashmap indexing off of
/// ParentComponent O(1) instead of O(log n).
internal class ParentComponentData(
    val parent: ParentComponentData?,
    val instanceId: String,
    val componentInfo: ComponentInfo
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
    rootLayoutId: Int,
    document: DocContent,
    customizations: CustomizationContext,
    variantTransition: SquooshVariantTransition,
    interactionState: InteractionState,
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
    overlays: List<View>? = null,
    appContext: Context,
    useLocalStringRes: Boolean?,
): SquooshResolvedNode? {
    if (!customizations.getVisible(v.name)) return null
    customizations.getVisibleState(v.name)?.let { if (!it.value) return null }
    var componentLayoutId = rootLayoutId
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
        componentLayoutId = rootLayoutId + layoutIdAllocator.componentLayoutId(parentComps) * 100000

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
                    customizations.getMatchingVariant(view.component_info) ?: componentInfo.name
                )
            if (variantNodeName != null && variantNodeName != componentInfo.name) {
                // Find the view associated with the variant name
                val variantNodeQuery =
                    NodeQuery.NodeVariant(variantNodeName, variantParentName.ifEmpty { view.name })
                val variantView =
                    interactionState.squooshRootNode(variantNodeQuery, document, isRoot)
                if (variantView != null) {
                    view = variantView
                }
            }
            variantTransition.selectedVariant(v.id, view.id)
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
    val layoutId = componentLayoutId + v.unique_id
    layoutIdAllocator.visitLayoutId(layoutId)

    // XXX-PERF: computeTextInfo is *super* slow. It needs to use a cache between frames.
    val textInfo =
        squooshComputeTextInfo(
            view,
            density,
            document,
            customizations,
            fontResourceLoader,
            variableState,
            appContext = appContext,
            useLocalStringRes = useLocalStringRes,
        )
    val resolvedView = SquooshResolvedNode(view, style, layoutId, textInfo, v.id, layoutNode = null)

    if (view.data is ViewData.Container) {
        val viewData = view.data as ViewData.Container
        var previousChild: SquooshResolvedNode? = null

        for (child in viewData.children) {
            val childResolvedNode =
                resolveVariantsRecursively(
                    child,
                    componentLayoutId,
                    document,
                    customizations,
                    variantTransition,
                    interactionState,
                    parentComps,
                    density,
                    fontResourceLoader,
                    composableList,
                    layoutIdAllocator,
                    "",
                    false,
                    variableState,
                    appContext = appContext,
                    useLocalStringRes = useLocalStringRes,
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
                    r.trigger is Trigger.OnClick ||
                    r.trigger is Trigger.OnPress
        }
    }

    // If this node has a content customization, then we make a special record of it so that we can
    // zip through all of them after layout and render them in the right location.
    val replacementContent = customizations.getContent(view.name)
    val replacementComponent = customizations.getComponent(view.name)
    if (replacementContent != null || replacementComponent != null) {
        composableList.add(
            SquooshChildComposable(
                component = replacementComponent,
                content = replacementContent,
                node = resolvedView,
                parentComponents = parentComps
            )
        )
        // Make sure that the renderer knows that it needs to do an external render for this
        // node.
        resolvedView.needsChildRender = true
    } else if (hasSupportedInteraction) {
        // Add a SquooshChildComposable to handle the interaction.
        composableList.add(
            SquooshChildComposable(
                component = null,
                content = null,
                node = resolvedView,
                parentComponents = parentComps
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
                    rootLayoutId,
                    document,
                    customizations,
                    variantTransition,
                    interactionState,
                    null,
                    density,
                    fontResourceLoader,
                    composableList,
                    layoutIdAllocator,
                    "",
                    false,
                    variableState,
                    appContext = appContext,
                    useLocalStringRes = useLocalStringRes,
                ) ?: continue

            // Make a synthetic parent for the overlay.
            val overlayContainer =
                generateOverlayNode(overlayExtras, overlayContent, rootLayoutId, layoutIdAllocator)
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
                    content = null,
                    node = overlayContainer,
                    parentComponents = parentComps
                )
            )
        }
    }

    return resolvedView
}

/// Create a SquooshResolvedNode for an overlay, with the appropriate layout style set up.
private fun generateOverlayNode(
    overlay: FrameExtras,
    node: SquooshResolvedNode,
    rootLayoutId: Int,
    layoutIdAllocator: SquooshLayoutIdAllocator
): SquooshResolvedNode {
    // Make a view based on the child node which uses a synthesized style.
    val overlayStyle = node.style.asBuilder()
    val layoutStyle = node.style.layout_style.asBuilder()
    val nodeStyle = node.style.node_style.asBuilder()
    nodeStyle.overflow = Overflow.Visible()
    layoutStyle.position_type = PositionType.Absolute()
    layoutStyle.top = Dimension.Percent(0.0f)
    layoutStyle.left = Dimension.Percent(0.0f)
    layoutStyle.right = Dimension.Percent(0.0f)
    layoutStyle.bottom = Dimension.Percent(0.0f)
    layoutStyle.width = Dimension.Percent(1.0f)
    layoutStyle.height = Dimension.Percent(1.0f)
    layoutStyle.flex_direction = FlexDirection.Column()
    when (overlay.overlayPositionType) {
        is OverlayPositionType.TOP_LEFT -> {
            layoutStyle.justify_content = JustifyContent.FlexStart() // Y
            layoutStyle.align_items = AlignItems.FlexStart() // X
        }
        is OverlayPositionType.TOP_CENTER -> {
            layoutStyle.justify_content = JustifyContent.FlexStart() // Y
            layoutStyle.align_items = AlignItems.Center() // X
        }
        is OverlayPositionType.TOP_RIGHT -> {
            layoutStyle.justify_content = JustifyContent.FlexStart() // Y
            layoutStyle.align_items = AlignItems.FlexEnd() // X
        }
        is OverlayPositionType.BOTTOM_LEFT -> {
            layoutStyle.justify_content = JustifyContent.FlexEnd() // Y
            layoutStyle.align_items = AlignItems.FlexStart() // X
        }
        is OverlayPositionType.BOTTOM_CENTER -> {
            layoutStyle.justify_content = JustifyContent.FlexEnd() // Y
            layoutStyle.align_items = AlignItems.Center() // X
        }
        is OverlayPositionType.BOTTOM_RIGHT -> {
            layoutStyle.justify_content = JustifyContent.FlexEnd() // Y
            layoutStyle.align_items = AlignItems.FlexEnd() // X
        }
        // Center and Manual both are centered; not clear how to implement manual positioning
        // without making a layout-dependent query.
        else -> {
            layoutStyle.justify_content = JustifyContent.Center()
            layoutStyle.align_items = AlignItems.Center()
        }
    }
    nodeStyle.background = listOf()

    overlay.overlayBackground.color.ifPresent { color ->
        val c =
            listOf(
                (color.r * 255.0).toInt().toByte(),
                (color.g * 255.0).toInt().toByte(),
                (color.b * 255.0).toInt().toByte(),
                (color.a * 255.0).toInt().toByte(),
            )
        val colorBuilder = Color.Builder()
        colorBuilder.color = c
        nodeStyle.background = listOf(Background.Solid(ColorOrVar.Color(colorBuilder.build())))
    }
    overlayStyle.layout_style = layoutStyle.build()
    overlayStyle.node_style = nodeStyle.build()
    val style = overlayStyle.build()

    // Now synthesize a view.
    val overlayViewData = ViewData.Container.Builder()
    overlayViewData.shape = ViewShape.Rect(false)
    overlayViewData.children = listOf(node.view)

    val overlayScrollInfo = ScrollInfo.Builder()
    overlayScrollInfo.paged_scrolling = false
    overlayScrollInfo.overflow = OverflowDirection.NONE()

    val overlayView = View.Builder()
    overlayView.unique_id = (node.view.unique_id + 0x2000).toShort()
    overlayView.id = "overlay-${node.view.id}"
    overlayView.name = "Overlay ${node.view.name}"
    overlayView.component_info = Optional.empty()
    overlayView.reactions =
        if (
            overlay.overlayBackgroundInteraction
                is OverlayBackgroundInteraction.CLOSE_ON_CLICK_OUTSIDE
        ) {
            // Synthesize a reaction that will close the overlay.
            val r = Reaction.Builder()
            r.trigger = Trigger.OnClick()
            r.action = Action.Close()
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

    val layoutId = rootLayoutId + node.layoutId + 0x20000000
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
