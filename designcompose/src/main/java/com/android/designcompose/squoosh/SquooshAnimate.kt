/*
 * Copyright 2024 Google LLC
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
import com.android.designcompose.AnimatedAction
import com.android.designcompose.asBuilder
import com.android.designcompose.decompose
import com.android.designcompose.fixedHeight
import com.android.designcompose.fixedWidth
import com.android.designcompose.hasTransformChange
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.NodeStyle
import com.android.designcompose.serdegen.Transition
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewShape
import com.android.designcompose.serdegen.ViewStyle
import com.android.designcompose.toFloatList
import java.util.Optional

// Squoosh animation design
//
// We take two trees -- two SquooshResolvedNodes -- and then build a "transition tree" which
// combines
// them, and also generates a list of "animated properties" (which are functions that take a value
// 0..1 and updates things in the transition tree to be somewhere in between).
//
// Initially I'm just going to support Smart Animate, because it's the hardest, but I'll do the
// others once Smart Animate is in shape.

internal interface SquooshAnimatedItem {
    fun apply(value: Float) {}
}

internal class SquooshAnimatedFadeIn(private val target: SquooshResolvedNode) :
    SquooshAnimatedItem {
    override fun apply(value: Float) {
        target.style = target.style.withNodeStyle { s -> s.opacity = Optional.of(value) }
    }
}

internal class SquooshAnimatedFadeOut(private val target: SquooshResolvedNode) :
    SquooshAnimatedItem {
    override fun apply(value: Float) {
        target.style = target.style.withNodeStyle { s -> s.opacity = Optional.of(1.0f - value) }
    }
}

internal class SquooshAnimatedLayout(
    private val target: SquooshResolvedNode,
    private val from: SquooshResolvedNode,
    private val to: SquooshResolvedNode,
) : SquooshAnimatedItem {
    private val transformedChanged = hasTransformChange(from.view.style, to.view.style)
    private val fromDecomposed = from.style.node_style.transform.decompose(1F)
    private val toDecomposed = to.style.node_style.transform.decompose(1F)

    override fun apply(value: Float) {
        val iv = 1.0f - value
        val fromLayout = from.computedLayout
        val toLayout = to.computedLayout

        if (fromLayout == null || toLayout == null) {
            Log.e(
                TAG,
                "Unable to compute animated layout for ${target.view.name} because from $fromLayout /to $toLayout  layout is uncomputed."
            )
            return
        }

        // Pass in 1 for the density because at this stage, we just want the raw size from the
        // design. The render code will take into account density.
        val fromWidth = if (transformedChanged) from.view.style.fixedWidth(1F) else fromLayout.width
        val fromHeight =
            if (transformedChanged) from.view.style.fixedHeight(1F) else fromLayout.height
        val toWidth = if (transformedChanged) to.view.style.fixedWidth(1F) else toLayout.width
        val toHeight = if (transformedChanged) to.view.style.fixedHeight(1F) else toLayout.height

        if (transformedChanged) {
            // Interpolate the decomposed matrix values, then construct a new matrix
            val target = fromDecomposed.interpolateTo(toDecomposed, value)
            this.target.style =
                this.target.style.withNodeStyle { s ->
                    s.transform = Optional.of(target.toMatrix().toFloatList())
                }
        }

        // When doing a layout animation, set overrideLayoutSize to true so that the rendering code
        // uses this computed size as opposed to the size of the node in ViewStyle. This ensures
        // that size change animations and rotation animations render at the correct size.
        target.overrideLayoutSize = true
        target.computedLayout =
            Layout(
                0,
                toWidth * value + fromWidth * iv,
                toHeight * value + fromHeight * iv,
                toLayout.left * value + fromLayout.left * iv,
                toLayout.top * value + fromLayout.top * iv
            )
    }
}

internal class SquooshAnimatedArc(
    private val target: SquooshResolvedNode,
    private val from: ViewShape.Arc,
    private val to: ViewShape.Arc
) : SquooshAnimatedItem {
    override fun apply(value: Float) {
        val iv = 1.0f - value
        val arcBuilder = ViewShape.Arc.Builder()

        arcBuilder.path = listOf()
        arcBuilder.stroke = listOf()
        arcBuilder.corner_radius = from.corner_radius * iv + to.corner_radius * value
        arcBuilder.inner_radius = from.inner_radius * iv + to.inner_radius * value
        arcBuilder.is_mask = to.is_mask
        arcBuilder.start_angle_degrees =
            from.start_angle_degrees * iv + to.start_angle_degrees * value
        arcBuilder.stroke_cap = to.stroke_cap
        arcBuilder.sweep_angle_degrees =
            from.sweep_angle_degrees * iv + to.sweep_angle_degrees * value
        val arc = arcBuilder.build()

        // Unfortunately, the ViewData and View objects are also immutable.
        val viewDataBuilder = ViewData.Container.Builder()
        viewDataBuilder.shape = arc
        viewDataBuilder.children = (target.view.data as ViewData.Container).children
        val viewData = viewDataBuilder.build()

        val viewBuilder = View.Builder()
        viewBuilder.data = viewData
        viewBuilder.id = target.view.id
        viewBuilder.name = target.view.name
        viewBuilder.style = target.view.style
        viewBuilder.component_info = target.view.component_info
        viewBuilder.design_absolute_bounding_box =
            target.view.design_absolute_bounding_box // didn't we delete this?
        viewBuilder.frame_extras = target.view.frame_extras
        viewBuilder.reactions = target.view.reactions
        viewBuilder.render_method = target.view.render_method
        viewBuilder.explicit_variable_modes = target.view.explicit_variable_modes
        viewBuilder.scroll_info = target.view.scroll_info
        viewBuilder.unique_id = target.view.unique_id
        val view = viewBuilder.build()

        target.view = view
    }
}

// XXX: No transform, coz no decomposition-recomposition implemented yet.
//      No colors or strokes or corners.

internal class SquooshAnimationControl(
    val root: SquooshResolvedNode,
    private val items: List<SquooshAnimatedItem>
) {
    fun apply(value: Float) {
        for (item in items) {
            item.apply(value)
        }
    }
}

internal class SquooshAnimationRequest(
    val toNodeId: String,
    val animationId: Int,
    val interruptedId: Int?,
    val transition: Transition,
    val action: AnimatedAction?,
    val variant: VariantAnimationInfo?,
    var animationControl: SquooshAnimationControl? = null
)

/// Create a new tree, based on "from", which additional nodes from "to" where an animation has
/// been requested. If `requestedAnimationControls` is empty or has no matching ids then the output
/// tree is the same as `from`.
///
/// The tree that's returned from this function should only be used for rendering and event
/// dispatching. Other algorithms like layout won't work on this tree, because it's combining
/// elements of two other trees.
internal fun createMergedAnimationTree(
    from: SquooshResolvedNode,
    to: SquooshResolvedNode,
    requestedAnimations: HashMap<String, SquooshAnimationRequest>,
    parent: SquooshResolvedNode? = null,
    alreadyMatchedSet: HashSet<Int> = HashSet()
): SquooshResolvedNode {
    if (requestedAnimations.isEmpty()) return from.cloneSelfAndChildren(parent)

    // Does this match a thing that we need to animate?
    var requestedAnim = requestedAnimations[from.view.id]
    if (requestedAnim == null) requestedAnim = requestedAnimations[from.unresolvedNodeId]
    val (cloned: SquooshResolvedNode, alreadyBuiltChildren) =
        if (requestedAnim != null && requestedAnim.animationControl == null) {
            // Can we find it in the "to" tree?
            val toNode = findNode(to, requestedAnim.toNodeId)
            if (toNode != null) {
                val animations: ArrayList<SquooshAnimatedItem> = ArrayList()
                val mergedClonedNode =
                    mergeRecursive(from, toNode, parent, animations, alreadyMatchedSet)
                if (animations.isNotEmpty())
                    requestedAnim.animationControl =
                        SquooshAnimationControl(mergedClonedNode, animations)

                Pair(mergedClonedNode, true)
            } else {
                // An animation was requested, but no destination node could be found. We'll end up
                // leaving the animationControl field blank, which will result in an error log later
                // on.
                Pair(from.cloneSelf(parent), false)
            }
        } else {
            // Clone the target node, then look at its siblings, then at its child.
            Pair(from.cloneSelf(parent), false)
        }

    // First recurse along siblings
    val nextFrom = from.nextSibling
    if (nextFrom != null) {
        val nextCloned =
            createMergedAnimationTree(nextFrom, to, requestedAnimations, parent, alreadyMatchedSet)
        // Insert at the end; does this reorder? Hope not
        var c: SquooshResolvedNode? = cloned
        while (c?.nextSibling != null) c = c.nextSibling
        c!!.nextSibling = nextCloned
    }

    // Then recurse along children, if we didn't already consider them by finding a match.
    if (!alreadyBuiltChildren) {
        val childFrom = from.firstChild
        if (childFrom != null) {
            val child =
                createMergedAnimationTree(
                    childFrom,
                    to,
                    requestedAnimations,
                    cloned,
                    alreadyMatchedSet
                )
            cloned.firstChild = child
        }
    }

    return cloned
}

private fun SquooshResolvedNode.cloneSelf(parent: SquooshResolvedNode?): SquooshResolvedNode =
    SquooshResolvedNode(
        view = this.view,
        style = this.style,
        layoutId = this.layoutId,
        textInfo = this.textInfo,
        unresolvedNodeId = this.unresolvedNodeId,
        layoutNode = this,
        parent = parent,
        computedLayout = this.computedLayout,
        needsChildRender = this.needsChildRender
    )

private fun SquooshResolvedNode.cloneSelfAndChildren(
    parent: SquooshResolvedNode?
): SquooshResolvedNode {
    val n = this.cloneSelf(parent)
    var child = this.firstChild
    var prevChild: SquooshResolvedNode? = null
    while (child != null) {
        val c = child.cloneSelfAndChildren(n)
        if (n.firstChild == null) n.firstChild = c
        if (prevChild != null) prevChild.nextSibling = c
        prevChild = c
        child = child.nextSibling
    }
    return n
}

private fun mergeRecursive(
    from: SquooshResolvedNode,
    to: SquooshResolvedNode,
    parent: SquooshResolvedNode?,
    anims: ArrayList<SquooshAnimatedItem>,
    alreadyMatchedSet: HashSet<Int>,
): SquooshResolvedNode {
    // We have an exact match on `from` and `to`, so we can construct various animation controls to
    // go between them in a third node. Then we need to inspect the children and match them on name.
    //
    // We either got called based on a name match (i.e.: this function recursing over children and
    // looking up children in the "to" tree using the names of children in the "from" tree) or an id
    // match (i.e.: the ids of two nodes corresponding to the same component instance).
    //
    // We have a few cases to consider here:
    //  1. The nodes match and don't need to be morphed into each other. In this case we can do just
    //     a layout / transform animation on one of them (since that's all that has changed) and
    //     then inspect the children to find matches.
    //  2. The nodes don't match. In this case, we do a crossfade and layout animation on both trees
    //     where the "from" is outgoing (it will fade away) and the "to" is incoming (it will fade
    //     in).
    //  3. When looking at children in (1), we have three cases!
    //      - we find a matching child and it goes to case (1) or (2) above.
    //      - we don't find a matching child in "from" for a child in "to", so this gets an outgoing
    //        animation (it fades out)
    //      - we don't find a matching child in "to" for a child in "from", so this gets an incoming
    //        animation (it fades in)

    if (isTweenable(from.view, to.view) && !needsStyleTween(from.style, to.style)) {
        // Clone the "to" node.
        val n = to.cloneSelf(parent)
        var previousChild: SquooshResolvedNode? = null
        // In this case, we need to recurse over all of the children and see if we can merge those,
        // too.
        val matchedSet = HashSet<String>()
        var child = to.firstChild
        while (child != null) {
            val matchingFromChild = findChildNamed(from, child.view.name, alreadyMatchedSet)
            if (matchingFromChild != null) {
                // We have a match. Remember it so that we can more quickly test all of the "from"
                // children later.
                matchedSet.add(child.view.name)
                // Also remember that we visited this node in the whole tree, so we won't match
                // against it again, in the case that there are many children with the same name
                alreadyMatchedSet.add(matchingFromChild.layoutId)

                // Create the animation for this one.
                val c = mergeRecursive(matchingFromChild, child, n, anims, alreadyMatchedSet)
                if (previousChild != null) previousChild.nextSibling = c else n.firstChild = c
                previousChild = c
            } else {
                // No match; this is an incoming child, so clone it including children and give it
                // a fade in animation.
                val c = child.cloneSelfAndChildren(n)
                if (previousChild != null) previousChild.nextSibling = c else n.firstChild = c
                previousChild = c

                anims.add(SquooshAnimatedFadeIn(c))
            }

            // Sometimes when we do a merge, we actually end up with multiple nodes (because we
            // chose to do a crossfade). In this case, `previousChild` actually isn't the last
            // child and we need to catch it up.
            while (previousChild?.nextSibling != null) previousChild = previousChild.nextSibling

            child = child.nextSibling
        }

        // Now look at all of the "from" children which didn't match anything in "to" and create
        // a fade out animation for them.
        child = from.firstChild
        while (child != null) {
            if (!matchedSet.contains(child.view.name)) {
                val c = child.cloneSelfAndChildren(n)
                if (previousChild != null) previousChild.nextSibling = c else n.firstChild = c
                previousChild = c

                anims.add(SquooshAnimatedFadeOut(c))
            }
            child = child.nextSibling
        }

        // Now see what kind of animations we can make; starting with a layout animation.
        anims.add(SquooshAnimatedLayout(n, from, to))

        // If they're both arcs, then they might need an arc animation.
        // XXX: Refactor this so we don't inspect every type right here.
        if (
            from.view.data is ViewData.Container &&
                (from.view.data as ViewData.Container).shape is ViewShape.Arc &&
                to.view.data is ViewData.Container &&
                (to.view.data as ViewData.Container).shape is ViewShape.Arc
        ) {
            val fromArc: ViewShape.Arc =
                (from.view.data as ViewData.Container).shape as ViewShape.Arc
            val toArc: ViewShape.Arc = (to.view.data as ViewData.Container).shape as ViewShape.Arc

            if (fromArc != toArc) {
                anims.add(SquooshAnimatedArc(n, fromArc, toArc))
            }
        }

        return n
    } else {
        // Ok, we need to clone both the "from" and "to" and make a crossfade animation between
        // them.
        val fromClone = from.cloneSelfAndChildren(parent)
        val toClone = to.cloneSelfAndChildren(parent)
        fromClone.nextSibling = toClone

        anims.add(SquooshAnimatedFadeOut(fromClone))
        anims.add(SquooshAnimatedFadeIn(toClone))
        anims.add(SquooshAnimatedLayout(fromClone, from, to))
        anims.add(SquooshAnimatedLayout(toClone, from, to))

        return fromClone
    }
}

// Find nodes where we want to have animation; this is typically from a component CHANGE_TO
private fun findNode(n: SquooshResolvedNode, id: String): SquooshResolvedNode? {
    if (n.view.id == id || n.unresolvedNodeId == id) return n
    var child = n.firstChild
    while (child != null) {
        if (child.view.id == id || child.unresolvedNodeId == id) return child
        val found = findNode(child, id)
        if (found != null) return found
        child = child.nextSibling
    }
    return null
}

private fun findChildNamed(
    parent: SquooshResolvedNode,
    name: String,
    alreadyMatchedSet: HashSet<Int>
): SquooshResolvedNode? {
    var child = parent.firstChild
    while (child != null) {
        if (child.view.name == name && !alreadyMatchedSet.contains(child.layoutId)) return child
        child = child.nextSibling
    }
    return null
}

// We can tween between two views (and thus don't need one of them in the tree) if:
//  - They are both Containers
//  - They both have a Rect or RoundRect shape (for now we need the shapes to be the same).
private fun isTweenable(a: View, b: View): Boolean {
    val aData = a.data
    val bData = b.data
    if (aData is ViewData.Container && bData is ViewData.Container) {
        // Rects and RoundRects can be tweened.
        if (
            (aData.shape is ViewShape.Rect && bData.shape is ViewShape.Rect) ||
                (aData.shape is ViewShape.RoundRect && bData.shape is ViewShape.RoundRect)
        )
            return true

        if ((aData.shape is ViewShape.VectorRect && bData.shape is ViewShape.VectorRect))
            return true

        // Arcs can be tweened.
        if (aData.shape is ViewShape.Arc && bData.shape is ViewShape.Arc) return true
    }
    return false
}

private fun needsStyleTween(a: ViewStyle, b: ViewStyle): Boolean {
    // Compare some style things and decide if we need to tween the styles.
    if (a.node_style.background != b.node_style.background) return true
    if (a.node_style.stroke != b.node_style.stroke) return true
    return false
}

private fun ViewStyle.withNodeStyle(delta: (NodeStyle.Builder) -> Unit): ViewStyle {
    val builder = asBuilder()
    val nodeStyleBuilder = node_style.asBuilder()
    delta(nodeStyleBuilder)
    builder.node_style = nodeStyleBuilder.build()
    return builder.build()
}
