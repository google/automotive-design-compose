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

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Density
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DocContent
import com.android.designcompose.InteractionState
import com.android.designcompose.ReplacementContent
import com.android.designcompose.getComponent
import com.android.designcompose.getContent
import com.android.designcompose.getKey
import com.android.designcompose.getMatchingVariant
import com.android.designcompose.mergeStyles
import com.android.designcompose.serdegen.ComponentInfo
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.serdegen.Trigger
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewStyle
import com.android.designcompose.squooshNodeVariant
import com.android.designcompose.squooshRootNode

// Remember if there's a child composable for a given node, and also we return an ordered
// list of all the child composables we need to render, along with transforms etc.
internal class SquooshChildComposable(
    // One of these should be populated...
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
    private val preComputedHashCode: Int = if (parent != null) {
        (parent.preComputedHashCode * 31 + instanceId.hashCode()) * 31 + componentInfo.id.hashCode()
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
    interactionState: InteractionState,
    parentComponents: ParentComponentData?,
    density: Density,
    fontResourceLoader: Font.ResourceLoader,
    // XXX: This probably won't show up in any profile, but I used linked lists everywhere
    //      else to reduce the number of objects we make (especially since we run this code
    //      every recompose.
    composableList: ArrayList<SquooshChildComposable>,
    layoutIdAllocator: SquooshLayoutIdAllocator,
    variantParentName: String = ""): SquooshResolvedNode
{
    var componentLayoutId = rootLayoutId
    var parentComps = parentComponents
    var overrideStyle: ViewStyle? = null

    // If we have a component then we might need to get an override style, and we definitely
    // need to get a different layout id.
    if (v.component_info.isPresent) {
        val componentInfo = v.component_info.get()
        parentComps = ParentComponentData(parentComponents, v.id, componentInfo)

        // Ensure that the children of this component get unique layout ids, even though there
        // may be multiple instances of the same component in one tree.
        componentLayoutId = layoutIdAllocator.componentLayoutId(parentComps) * 1000000

        // Do we have an override style? This is style data which we should apply to the final style
        // even if we're swapping out our view definition for a variant.
        if (componentInfo.overrides.isPresent) {
            val overrides = componentInfo.overrides.get()
            if (overrides.style.isPresent) {
                overrideStyle = overrides.style.get()
            }

            // XXX: override data?
        }
    }

    // See if we've got a replacement node from an interaction
    var view = interactionState.squooshNodeVariant(v.id, customizations.getKey(), document) ?: v
    val hasVariantReplacement = view.name != v.name

    if (!hasVariantReplacement) {
        // If an interaction has not changed the current variant, then check to see if this node
        // is part of a component set with variants and if any @DesignVariant annotations
        // set variant properties that match. If so, variantNodeName will be set to the
        // name of the node with all the variants set to the @DesignVariant parameters
        val variantNodeName = customizations.getMatchingVariant(view.component_info)
        if (variantNodeName != null) {
            // Find the view associated with the variant name
            val variantNodeQuery =
                NodeQuery.NodeVariant(variantNodeName, variantParentName.ifEmpty { view.name })
            val isRoot = true // XXX-SQUOOSH: Need to support non-root components.
            val variantView = interactionState.squooshRootNode(variantNodeQuery, document, isRoot)
            if (variantView != null) {
                view = variantView
            }
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
    val textInfo = squooshComputeTextInfo(view, density, document, customizations, fontResourceLoader)
    val resolvedView = SquooshResolvedNode(view, style, layoutId, textInfo, v.id)

    if (view.data is ViewData.Container) {
        val viewData = view.data as ViewData.Container
        var previousChild: SquooshResolvedNode? = null

        for (child in viewData.children) {
            val childResolvedNode = resolveVariantsRecursively(
                child,
                componentLayoutId,
                document,
                customizations,
                interactionState,
                parentComps,
                density,
                fontResourceLoader,
                composableList,
                layoutIdAllocator,
            )

            childResolvedNode.parent = resolvedView

            if (resolvedView.firstChild == null)
                resolvedView.firstChild = childResolvedNode
            if (previousChild != null)
                previousChild.nextSibling = childResolvedNode

            previousChild = childResolvedNode
        }
    }

    // Find out if we have some supported interactions; currently that's just on press and on click.
    // We'll add timeouts and the others later...
    var hasSupportedInteraction = false
    view.reactions.ifPresent { reactions ->
        reactions.forEach { r ->
            hasSupportedInteraction = hasSupportedInteraction || r.trigger is Trigger.OnClick || r.trigger is Trigger.OnPress
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

    return resolvedView
}