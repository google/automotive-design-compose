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
import androidx.compose.ui.text.TextStyle
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
import com.android.designcompose.common.NodeQuery
import com.android.designcompose.definition.element.FontStyle
import com.android.designcompose.definition.element.StrokeAlign
import com.android.designcompose.definition.element.TextDecoration
import com.android.designcompose.definition.element.ViewShapeKt.box
import com.android.designcompose.definition.element.background
import com.android.designcompose.definition.element.dimensionProto
import com.android.designcompose.definition.element.dimensionRect
import com.android.designcompose.definition.element.fontStretch
import com.android.designcompose.definition.element.fontWeight
import com.android.designcompose.definition.element.lineHeight
import com.android.designcompose.definition.element.numOrVar
import com.android.designcompose.definition.element.size
import com.android.designcompose.definition.element.stroke
import com.android.designcompose.definition.element.strokeWeight
import com.android.designcompose.definition.element.viewShape
import com.android.designcompose.definition.interaction.PointerEvents
import com.android.designcompose.definition.layout.AlignContent
import com.android.designcompose.definition.layout.AlignItems
import com.android.designcompose.definition.layout.AlignSelf
import com.android.designcompose.definition.layout.FlexDirection
import com.android.designcompose.definition.layout.FlexWrap
import com.android.designcompose.definition.layout.ItemSpacingKt.auto
import com.android.designcompose.definition.layout.JustifyContent
import com.android.designcompose.definition.layout.LayoutSizing
import com.android.designcompose.definition.layout.Overflow
import com.android.designcompose.definition.layout.OverflowDirection
import com.android.designcompose.definition.layout.PositionType
import com.android.designcompose.definition.layout.itemSpacing
import com.android.designcompose.definition.layout.layoutStyle
import com.android.designcompose.definition.layout.scrollInfo
import com.android.designcompose.definition.modifier.BlendMode
import com.android.designcompose.definition.modifier.TextAlign
import com.android.designcompose.definition.modifier.TextAlignVertical
import com.android.designcompose.definition.modifier.TextOverflow
import com.android.designcompose.definition.plugin.FrameExtras
import com.android.designcompose.definition.view.ComponentInfo
import com.android.designcompose.definition.view.Display
import com.android.designcompose.definition.view.View
import com.android.designcompose.definition.view.ViewData
import com.android.designcompose.definition.view.ViewDataKt.container
import com.android.designcompose.definition.view.ViewStyle
import com.android.designcompose.definition.view.containerOrNull
import com.android.designcompose.definition.view.frameExtrasOrNull
import com.android.designcompose.definition.view.nodeStyle
import com.android.designcompose.definition.view.styleOrNull
import com.android.designcompose.definition.view.view
import com.android.designcompose.definition.view.viewData
import com.android.designcompose.definition.view.viewDataOrNull
import com.android.designcompose.definition.view.viewStyle
import com.android.designcompose.getComponent
import com.android.designcompose.getContent
import com.android.designcompose.getKey
import com.android.designcompose.getListContent
import com.android.designcompose.getMatchingVariant
import com.android.designcompose.getTapCallback
import com.android.designcompose.getVisible
import com.android.designcompose.getVisibleState
import com.android.designcompose.isPressed
import com.android.designcompose.squooshNodeVariant
import com.android.designcompose.squooshRootNode
import com.android.designcompose.utils.hasScrolling
import com.android.designcompose.utils.isSupportedInteraction
import com.android.designcompose.utils.mergeStyles

// Helper class to hold the list of child composables and overlays that need to be composed
// separately.
internal class ComposableList(
    val childComposables: ArrayList<SquooshChildComposable> = arrayListOf(),
    val overlayNodes: ArrayList<SquooshOverlayComposable> = arrayListOf(),
) {
    fun addChild(child: SquooshChildComposable) {
        childComposables.add(child)
    }

    fun clearChildren() {
        childComposables.clear()
    }

    fun addOverlay(nodeId: String, extras: FrameExtras) {
        overlayNodes.add(SquooshOverlayComposable(nodeId, extras))
    }
}

// Remember if there's a child composable for a given node, and also we return an ordered
// list of all the child composables we need to render, along with transforms etc.
internal class SquooshChildComposable(
    // If this child composable is to host an external composable (e.g.: for component replacement)
    // then this value will be non-null.
    var component: @Composable ((ComponentReplacementContext) -> Unit)? = null,

    // If this view is scrollable, set the view here and SquooshRoot() will compose another
    // SquooshRoot() of this view so that scrolling can be handled on it.
    val scrollView: View? = null,

    // Used for node resolution for interactions
    val parentComponents: ParentComponentData?,

    // We use this to look up the transform and layout translation.
    val node: SquooshResolvedNode,

    // TextStyle to be used in ComponentReplacementContext if replacing a text node
    val textStyle: TextStyle? = null,
)

internal class SquooshOverlayComposable(val nodeId: String, val extras: FrameExtras)

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
    viewFromTree: View,
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
    composableList: ComposableList?,
    layoutIdAllocator: SquooshLayoutIdAllocator,
    variantParentName: String = "",
    isRoot: Boolean,
    variableState: VariableState,
    appContext: Context,
    customVariantTransition: CustomVariantTransition?,
    textMeasureCache: TextMeasureCache,
    textHash: HashSet<String>,
    overlays: List<View>? = null,
    isScrollComponent: Boolean = false,
    componentLayoutId: Int = 0,
): SquooshResolvedNode? {
    if (!customizations.getVisible(viewFromTree.name)) return null
    customizations.getVisibleState(viewFromTree.name)?.let { if (!it.value) return null }
    var thisLayoutId = componentLayoutId
    var parentComps = parentComponents
    var overrideStyle: ViewStyle? = null
    var overrideViewData: ViewData? = null
    var view = viewFromTree

    // If we have a component then we might need to get an override style, and we definitely
    // need to get a different layout id.
    if (viewFromTree.hasComponentInfo()) {
        parentComps =
            ParentComponentData(parentComponents, viewFromTree.id, viewFromTree.componentInfo)

        overrideStyle =
            viewFromTree.componentInfo.overridesTableMap[
                    viewFromTree.componentInfo.componentSetName]
                ?.styleOrNull
        overrideViewData =
            viewFromTree.componentInfo.overridesTableMap[
                    viewFromTree.componentInfo.componentSetName]
                ?.viewDataOrNull

        // Ensure that the children of this component get unique layout ids, even though there
        // may be multiple instances of the same component in one tree.
        thisLayoutId =
            computeComponentLayoutId(thisLayoutId, layoutIdAllocator.componentLayoutId(parentComps))

        // See if we have a variant replacement; this only happens for component instances (for both
        // interaction-driven and customization-driven variant changes).

        // First ask the interaction state if there's a variant we should render instead.
        interactionState
            .squooshNodeVariant(viewFromTree.id, customizations.getKey(), document)
            ?.let { view = it }

        // If we didn't replace the component because of an interaction, we might want to replace it
        // because of a variant customization.
        if (
            view.name == viewFromTree.name
        ) { // TODO: why we don't check if view == viewFromTree????
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
                    viewFromTree.id,
                    customizations.getMatchingVariant(view.componentInfo)
                        ?: viewFromTree.componentInfo.name,
                )
            if (variantNodeName != null && variantNodeName != viewFromTree.componentInfo.name) {
                // Find the view associated with the variant name
                val variantNodeQuery =
                    NodeQuery.NodeVariant(
                        variantNodeName,
                        variantParentName.ifEmpty { view.componentInfo.componentSetName },
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
            variantTransition.selectedVariant(viewFromTree, view, customVariantTransition)
        }
    } else {
        overrideViewData =
            parentComps?.componentInfo?.overridesTableMap?.get(viewFromTree.name)?.viewDataOrNull
        overrideStyle =
            parentComps?.componentInfo?.overridesTableMap?.get(viewFromTree.name)?.styleOrNull
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
    val layoutId = computeLayoutId(thisLayoutId, viewFromTree.uniqueId)
    layoutIdAllocator.visitLayoutId(layoutId)

    // XXX-PERF: computeTextInfo is *super* slow. It needs to use a cache between frames.
    val textData =
        squooshComputeTextInfo(
            view,
            overrideViewData,
            layoutId,
            density,
            document,
            customizations,
            fontResourceLoader,
            variableState,
            appContext,
            textMeasureCache,
            textHash,
        )
    val textInfo = textData?.first
    val textStyle = textData?.second

    // Find out if we have some supported interactions. These currently include press, click,
    // timeout, and key press.
    var hasSupportedInteraction = false
    view.reactionsList.forEach { r ->
        hasSupportedInteraction = hasSupportedInteraction || r.trigger.isSupportedInteraction()
        if (r.trigger.hasKeyDown()) {
            // Register to be a listener for key reactions on this node
            val keyTrigger = r.trigger.keyDown
            val keyEvent = DesignKeyEvent.fromJsKeyCodes(keyTrigger.keyCodes.toList())
            val keyAction =
                KeyAction(
                    interactionState,
                    r.action,
                    findTargetInstanceId(document, parentComps, r.action),
                    customizations.getKey(),
                    null,
                )
            keyTracker.addListener(keyEvent, keyAction)
        }
    }
    val tapCallback = customizations.getTapCallback(view)
    if (tapCallback != null) hasSupportedInteraction = true
    if (textInfo?.hyperlinkOffsetMap?.isNotEmpty() == true) hasSupportedInteraction = true

    if (interactionState.isPressed(viewFromTree.id)) hasSupportedInteraction = true

    // If this node has a content customization, then we make a special record of it so that we can
    // zip through all of them after layout and render them in the right location.
    val replacementContent = customizations.getContent(view.name)
    val replacementComponent = customizations.getComponent(view.name)
    val listWidgetContent = customizations.getListContent(view.name)

    // If this is a text node being replaced, don't store textInfo into resolvedView
    val resolvedTextInfo = if (replacementComponent != null) null else textInfo
    val resolvedView = SquooshResolvedNode(view, style, layoutId, resolvedTextInfo, viewFromTree.id)

    var skipChildren = false // Set to true for customizations that replace children
    var skipComposableList =
        false // Set to true for scrolling view because it has its own SquooshRoot()
    if (replacementComponent != null) {
        composableList?.addChild(
            SquooshChildComposable(
                component = replacementComponent,
                node = resolvedView,
                parentComponents = parentComps,
                textStyle = textStyle,
            )
        )
        // Make sure that the renderer knows that it needs to do an external render for this
        // node.
        resolvedView.needsChildRender = true
        skipChildren = true
    } else if (view.hasScrolling() && resolvedView.layoutId != 0 && !isScrollComponent) {
        // If the view has scrolling, is not the root, and isScrollComponent is false (to prevent
        // infinite recursion), add the view to the list so it can be composed separately in order
        // to support scrolling.
        composableList?.addChild(
            SquooshChildComposable(
                scrollView = view,
                node = resolvedView,
                parentComponents = parentComps,
            )
        )
        resolvedView.needsChildRender = true
        skipComposableList = true
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

            composableList?.addChild(
                SquooshChildComposable(
                    component = @Composable { childComponent() },
                    node = replacementChild,
                    parentComponents = parentComps,
                )
            )
        }
        skipChildren = true
    } else if (listWidgetContent != null) {
        addListWidget(
            listWidgetContent,
            resolvedView,
            style,
            customizations,
            layoutIdAllocator,
            parentComps,
            composableList,
        )
        skipChildren = true
    } else if (hasSupportedInteraction) {
        // Add a SquooshChildComposable to handle the interaction.
        composableList?.addChild(
            SquooshChildComposable(
                component = null,
                node = resolvedView,
                parentComponents = parentComps,
            )
        )
    }

    if (!skipChildren) {
        view.data.containerOrNull?.let { viewData ->
            var previousChild: SquooshResolvedNode? = null

            for (child in view.data.container.childrenList) {
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
                        if (skipComposableList) null else composableList,
                        layoutIdAllocator,
                        "",
                        false,
                        variableState,
                        appContext,
                        customVariantTransition,
                        textMeasureCache,
                        textHash,
                        componentLayoutId = thisLayoutId,
                    ) ?: continue

                childResolvedNode.parent = resolvedView

                if (resolvedView.firstChild == null) resolvedView.firstChild = childResolvedNode
                if (previousChild != null) previousChild.nextSibling = childResolvedNode

                previousChild = childResolvedNode
            }
        }
    }

    // Roots also get overlays
    if (overlays != null) {
        for (overlay in overlays) {
            val overlayExtras = overlay.frameExtrasOrNull ?: continue
            composableList?.addOverlay(overlay.id, overlayExtras)
        }
    }

    return resolvedView
}

/// Create a SquooshResolvedNode for a content replacement list child. The minimum width and
/// height will come later (at layout time) by asking the Composable child for its intrinsic
/// size.
internal fun generateReplacementListChildNode(
    node: SquooshResolvedNode,
    childIdx: Int,
    layoutIdAllocator: SquooshLayoutIdAllocator,
): SquooshResolvedNode {
    val listChildViewData = container { shape = viewShape { rect = box { isMask = false } } }

    val listChildScrollInfo = scrollInfo {
        pagedScrolling = false
        overflow = OverflowDirection.OVERFLOW_DIRECTION_NONE
    }

    val listChildView = view {
        uniqueId = 0 // This is unused.
        id = "replacement-${node.view.id}-${childIdx}"
        name = "Replacement List ${node.view.name} / $childIdx"
        scrollInfo = listChildScrollInfo
        data = viewData { container = listChildViewData }
        renderMethod = View.RenderMethod.RENDER_METHOD_NONE

        // The Rust code currently fails if any of these fields are unset so we initialize
        // a default viewStyle. (the nodeStyle isn't needed right now)
        style = viewStyle {
            layoutStyle = layoutStyle {
                positionType = PositionType.POSITION_TYPE_RELATIVE
                flexDirection = FlexDirection.FLEX_DIRECTION_ROW
                alignItems = AlignItems.ALIGN_ITEMS_FLEX_START
                alignSelf = AlignSelf.ALIGN_SELF_AUTO
                alignContent = AlignContent.ALIGN_CONTENT_FLEX_START
                justifyContent = JustifyContent.JUSTIFY_CONTENT_FLEX_START
                left = dimensionProto { undefined }
                top = dimensionProto { undefined }
                right = dimensionProto { undefined }
                bottom = dimensionProto { undefined }
                margin = dimensionRect {
                    start = dimensionProto { undefined }
                    end = dimensionProto { undefined }
                    top = dimensionProto { undefined }
                    bottom = dimensionProto { undefined }
                }
                padding = dimensionRect {
                    start = dimensionProto { undefined }
                    end = dimensionProto { undefined }
                    top = dimensionProto { undefined }
                    bottom = dimensionProto { undefined }
                }
                itemSpacing = itemSpacing { auto = auto {} }
                flexGrow = 0f
                flexShrink = 0f
                flexBasis = dimensionProto { undefined }
                boundingBox = size {
                    width = 0f
                    height = 0f
                }
                width = dimensionProto { undefined }
                height = dimensionProto { undefined }
                minWidth = dimensionProto { undefined }
                minHeight = dimensionProto { undefined }
                maxWidth = dimensionProto { undefined }
                maxHeight = dimensionProto { undefined }
            }
            nodeStyle = nodeStyle {
                textColor = background { none }
                fontSize = numOrVar { num = 0f }
                fontFamily = ""
                fontWeight = fontWeight { weight = numOrVar { 0f } }
                fontStyle = FontStyle.FONT_STYLE_NORMAL
                fontStretch = fontStretch { value = 0f }
                backgrounds.clear()
                boxShadows.clear()
                stroke = stroke {
                    strokeAlign = StrokeAlign.STROKE_ALIGN_CENTER
                    strokeWeight = strokeWeight { uniform = 0f }
                    strokes.clear()
                    clearShaderData()
                }
                clearOpacity()
                clearTransform()
                clearRelativeTransform()

                textDecoration = TextDecoration.TEXT_DECORATION_NONE
                textAlign = TextAlign.TEXT_ALIGN_LEFT
                textAlignVertical = TextAlignVertical.TEXT_ALIGN_VERTICAL_TOP
                textOverflow = TextOverflow.TEXT_OVERFLOW_CLIP

                clearTextShadow()
                nodeSize = size {
                    width = 0f
                    height = 0f
                }
                lineHeight = lineHeight { percent = 0f }
                clearLineCount()
                clearLetterSpacing()
                fontFeatures.clear()
                filters.clear()
                backdropFilters.clear()
                blendMode = BlendMode.BLEND_MODE_PASS_THROUGH
                displayType = Display.DISPLAY_FLEX
                flexWrap = FlexWrap.FLEX_WRAP_NO_WRAP
                clearGridLayoutType()
                gridColumnsRows = 0
                gridAdaptiveMinSize = 0
                gridSpanContents.clear()
                overflow = Overflow.OVERFLOW_VISIBLE
                clearMaxChildren()
                clearOverflowNodeId()
                clearOverflowNodeName()
                crossAxisItemSpacing = 0f
                horizontalSizing = LayoutSizing.LAYOUT_SIZING_HUG
                verticalSizing = LayoutSizing.LAYOUT_SIZING_HUG
                clearAspectRatio()
                pointerEvents = PointerEvents.POINTER_EVENTS_AUTO
                clearMeterData()
                clearHyperlink()
                clearShaderData()
            }
        }
    }

    val listLayoutId = layoutIdAllocator.listLayoutId(node.layoutId)
    val layoutId = computeSyntheticListItemLayoutId(listLayoutId, childIdx)
    layoutIdAllocator.visitLayoutId(layoutId)

    val listChildNode =
        SquooshResolvedNode(
            view = listChildView,
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
            // Since layout replacement nodes are added to the composable child list,
            // SquooshLayout() will be called on both them and their parent. Set skipLayoutScroll
            // to true so that both the parent and this node don't both offset their positions by
            // the current scroll value.
            skipLayoutScroll = true,
        )

    return listChildNode
}
