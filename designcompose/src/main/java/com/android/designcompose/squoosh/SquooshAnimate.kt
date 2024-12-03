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
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.geometry.Size
import com.android.designcompose.AnimatedAction
import com.android.designcompose.VariableState
import com.android.designcompose.asBuilder
import com.android.designcompose.decompose
import com.android.designcompose.fixedHeight
import com.android.designcompose.fixedWidth
import com.android.designcompose.proto.get
import com.android.designcompose.proto.ifContainerGetShape
import com.android.designcompose.proto.ifTextGetText
import com.android.designcompose.proto.nodeStyle
import com.android.designcompose.serdegen.Container
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.NodeStyle
import com.android.designcompose.serdegen.Shape
import com.android.designcompose.serdegen.VectorArc
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewDataType
import com.android.designcompose.serdegen.ViewShape
import com.android.designcompose.serdegen.ViewStyle
import com.android.designcompose.toLayoutTransform
import java.util.Optional
import kotlin.jvm.optionals.getOrElse

// Squoosh animation design
//
// We take two trees -- two SquooshResolvedNodes -- and then build a "transition tree" which
// combines
// them, and also generates a list of "animated properties" (which are functions that take a value
// 0..1 and updates things in the transition tree to be somewhere in between).
//
// Initially I'm just going to support Smart Animate, because it's the hardest, but I'll do the
// others once Smart Animate is in shape.

internal abstract class SquooshAnimatedItem(
    val target: SquooshResolvedNode,
    transition: AnimationTransition,
) {
    // The animation object that describes the easing and transition of this transition
    var animation: TargetBasedAnimation<Float, AnimationVector1D> =
        TargetBasedAnimation(
            animationSpec = transition.animationSpec(),
            typeConverter = Float.VectorConverter,
            initialValue = 0f,
            targetValue = 1f,
        )
    // Number of milliseconds to delay before starting this animation
    private val delayMillis: Int = transition.delayMillis()
    // The next animation frame value to pass to apply()
    var nextFrameValue: Float? = null

    // Given the current play time, calculate the next animation frame value to pass to apply()
    fun updateValuesFromNanos(playTimeNanos: Long) {
        val myPlayTimeNanos = myPlayTimeNanos(playTimeNanos)
        if (myPlayTimeNanos > 0) nextFrameValue = animation.getValueFromNanos(myPlayTimeNanos)
    }

    // Return true if this animated item has finished
    fun isFinishedFromNanos(playTimeNanos: Long): Boolean {
        val myPlayTimeNanos = myPlayTimeNanos(playTimeNanos)
        return animation.isFinishedFromNanos(myPlayTimeNanos)
    }

    // Return the current play time of this animation, taking into account the initial delay
    private fun myPlayTimeNanos(playTimeNanos: Long): Long {
        return playTimeNanos - (delayMillis.toLong() * 1000000L)
    }

    fun updateLayout(value: Float, width: Float, height: Float, from: Layout, to: Layout) {
        target.overrideLayoutSize = true
        target.computedLayout =
            Layout(
                0,
                width,
                height,
                to.left * value + from.left * (1f - value),
                to.top * value + from.top * (1f - value),
                to.content_width,
                to.content_height,
            )
    }

    abstract fun apply(value: Float)
}

internal class SquooshAnimatedFadeIn(target: SquooshResolvedNode, transition: AnimationTransition) :
    SquooshAnimatedItem(target, transition) {
    private val targetOpacity: Float = target.style.nodeStyle.opacity.getOrElse { 1f }

    override fun apply(value: Float) {
        target.style =
            target.style.withNodeStyle { s -> s.opacity = Optional.of(value * targetOpacity) }
    }
}

internal class SquooshAnimatedFadeOut(
    target: SquooshResolvedNode,
    transition: AnimationTransition,
) : SquooshAnimatedItem(target, transition) {
    private val targetOpacity: Float = target.style.nodeStyle.opacity.getOrElse { 1f }

    override fun apply(value: Float) {
        target.style =
            target.style.withNodeStyle { s ->
                s.opacity = Optional.of((1.0f - value) * targetOpacity)
            }
    }
}

internal class SquooshAnimatedScale(
    target: SquooshResolvedNode,
    private val from: SquooshResolvedNode,
    private val to: SquooshResolvedNode,
    private val scaleFrom: Boolean,
    transition: AnimationTransition,
) : SquooshAnimatedItem(target, transition) {
    private val fromDecomposed = from.style.nodeStyle.transform.decompose(1F)

    override fun apply(value: Float) {
        if (from.computedLayout == null || to.computedLayout == null) return
        val fromLayout = from.computedLayout!!
        val toLayout = to.computedLayout!!
        val fromSize = Size(fromLayout.width, fromLayout.height)
        val toSize = Size(toLayout.width, toLayout.height)

        // Calculate the scale factor, taking into account whether we are scaling the from or to
        // node.
        var scaleX = 1f + value * (toSize.width - fromSize.width) / fromSize.width
        if (!scaleFrom) scaleX /= (toSize.width / fromSize.width)
        var scaleY = 1f + value * (toSize.height - fromSize.height) / fromSize.height
        if (!scaleFrom) scaleY /= (toSize.height / fromSize.height)

        val transform = fromDecomposed.copy()
        transform.scaleX = scaleX
        transform.scaleY = scaleY
        target.style =
            target.style.withNodeStyle { s ->
                s.transform = Optional.of(transform.toMatrix().toLayoutTransform())
            }

        updateLayout(
            value,
            maxOf(toLayout.width, fromLayout.width),
            maxOf(toLayout.height, fromLayout.height),
            fromLayout,
            toLayout,
        )
    }
}

internal class SquooshAnimatedLayout(
    target: SquooshResolvedNode,
    private val from: SquooshResolvedNode,
    private val to: SquooshResolvedNode,
    transition: AnimationTransition,
) : SquooshAnimatedItem(target, transition) {
    private val fromDecomposed = from.style.nodeStyle.transform.decompose(1F)
    private val toDecomposed = to.style.nodeStyle.transform.decompose(1F)
    private val tweenTypes = computeTweenTypes()

    companion object {
        const val TWEEN_TRANSFORM = 0x0001
        const val TWEEN_OPACITY = 0x0002
    }

    private fun needsTransformTween(from: SquooshResolvedNode, to: SquooshResolvedNode): Boolean {
        return from.view.style.get().nodeStyle.transform != to.view.style.get().nodeStyle.transform
    }

    private fun needsOpacityTween(from: SquooshResolvedNode, to: SquooshResolvedNode): Boolean {
        return from.view.style.get().nodeStyle.opacity.getOrElse { 1F } !=
            to.view.style.get().nodeStyle.opacity.getOrElse { 1F }
    }

    private fun computeTweenTypes(): Int {
        var types = 0
        if (needsTransformTween(from, to)) types = types or TWEEN_TRANSFORM
        if (needsOpacityTween(from, to)) types = types or TWEEN_OPACITY
        return types
    }

    private fun doOpacityTween(value: Float, iv: Float) {
        if (tweenTypes and TWEEN_OPACITY != 0) {
            this.target.style =
                this.target.style.withNodeStyle { s ->
                    val fromOpacity = from.style.nodeStyle.opacity.getOrElse { 1F }
                    val toOpacity = to.style.nodeStyle.opacity.getOrElse { 1F }
                    s.opacity = Optional.of(toOpacity * value + fromOpacity * iv)
                }
        }
    }

    private fun doTransformTween(value: Float, iv: Float) {
        if (tweenTypes and TWEEN_TRANSFORM != 0) {
            // Interpolate the decomposed matrix values, then construct a new matrix
            val targetDecomposed = fromDecomposed.interpolateTo(toDecomposed, value)
            target.style =
                target.style.withNodeStyle { s ->
                    s.transform = Optional.of(targetDecomposed.toMatrix().toLayoutTransform())
                }

            val fromLayout = from.computedLayout!!
            val toLayout = to.computedLayout!!
            // Pass in 1 for the density because at this stage, we just want the raw size from the
            // design. The render code will take into account density.
            val fromWidth = from.view.style.get().fixedWidth(1F)
            val fromHeight = from.view.style.get().fixedHeight(1F)
            val toWidth = to.view.style.get().fixedWidth(1F)
            val toHeight = to.view.style.get().fixedHeight(1F)
            updateLayout(
                value,
                toWidth * value + fromWidth * iv,
                toHeight * value + fromHeight * iv,
                fromLayout,
                toLayout,
            )
        }
    }

    private fun doLayoutTween(value: Float, iv: Float) {
        // Don't tween again if something already tweened layout
        if (tweenTypes and TWEEN_TRANSFORM == 0) {
            // When doing a layout animation, set overrideLayoutSize to true so that the rendering
            // code
            // uses this computed size as opposed to the size of the node in ViewStyle. This ensures
            // that size change animations and rotation animations render at the correct size.
            val fromLayout = from.computedLayout!!
            val toLayout = to.computedLayout!!
            updateLayout(
                value,
                toLayout.width * value + fromLayout.width * iv,
                toLayout.height * value + fromLayout.height * iv,
                fromLayout,
                toLayout,
            )
        }
    }

    override fun apply(value: Float) {
        val iv = 1.0f - value
        if (from.computedLayout == null || to.computedLayout == null) {
            Log.e(
                TAG,
                "Unable to compute animated layout for ${target.view.name} because from ${from.computedLayout} to ${to.computedLayout}  layout is uncomputed.",
            )
            return
        }

        doOpacityTween(value, iv)
        doTransformTween(value, iv)
        doLayoutTween(value, iv)
    }
}

internal class SquooshAnimatedArc(
    target: SquooshResolvedNode,
    private val from: Shape.Arc,
    private val to: Shape.Arc,
    transition: AnimationTransition,
) : SquooshAnimatedItem(target, transition) {
    override fun apply(value: Float) {
        val iv = 1.0f - value
        val arc =
            Shape.Arc(
                VectorArc(
                    listOf(),
                    listOf(),
                    to.value.stroke_cap,
                    from.value.start_angle_degrees * iv + to.value.start_angle_degrees * value,
                    from.value.sweep_angle_degrees * iv + to.value.sweep_angle_degrees * value,
                    from.value.inner_radius * iv + to.value.inner_radius * value,
                    from.value.corner_radius * iv + to.value.corner_radius * value,
                    to.value.is_mask,
                )
            )
        // Unfortunately, the ViewData and View objects are also immutable.
        val viewDataValue =
            Container.Builder()
                .apply {
                    shape = Optional.of(ViewShape(Optional.of(arc)))
                    children =
                        (target.view.data.get().view_data_type.get() as ViewDataType.Container)
                            .value
                            .children
                }
                .build()

        val viewBuilder = View.Builder()
        viewBuilder.data = Optional.of(ViewData(Optional.of(ViewDataType.Container(viewDataValue))))
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
    private val items: List<SquooshAnimatedItem>,
    var animation: TargetBasedAnimation<Float, AnimationVector1D>? = null,
    private var nextFrameValue: Float = 0F,
) {
    // Given the current play time, calculate the next animation frame value to pass to apply()
    // for all animation items
    fun updateValuesFromNanos(playTimeNanos: Long) {
        nextFrameValue = animation?.getValueFromNanos(playTimeNanos) ?: 0F
        for (item in items) {
            item.updateValuesFromNanos(playTimeNanos)
        }
    }

    // Return true if all animated items have finished
    fun isFinishedFromNanos(playTimeNanos: Long): Boolean {
        for (item in items) {
            if (!item.isFinishedFromNanos(playTimeNanos)) return false
        }
        return true
    }

    fun apply(value: Float = nextFrameValue) {
        for (item in items) {
            val nextValue = item.nextFrameValue ?: value
            item.apply(nextValue)
        }
    }
}

internal class SquooshAnimationRequest(
    val toNodeId: String,
    val animationId: Int,
    val interruptedId: Int?,
    val transition: AnimationTransition,
    val action: AnimatedAction?,
    val variant: VariantAnimationInfo?,
    var animationControl: SquooshAnimationControl? = null,
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
    customVariantTransition: CustomVariantTransition?,
    variableState: VariableState,
    parent: SquooshResolvedNode? = null,
    alreadyMatchedSet: HashSet<Int> = HashSet(),
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
                    mergeRecursive(
                        from,
                        toNode,
                        parent,
                        animations,
                        alreadyMatchedSet,
                        customVariantTransition,
                        variableState,
                        requestedAnim.transition,
                    )
                if (animations.isNotEmpty()) {
                    requestedAnim.animationControl = SquooshAnimationControl(animations)
                }

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
            createMergedAnimationTree(
                nextFrom,
                to,
                requestedAnimations,
                customVariantTransition,
                variableState,
                parent,
                alreadyMatchedSet,
            )
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
                    customVariantTransition,
                    variableState,
                    cloned,
                    alreadyMatchedSet,
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
        needsChildRender = this.needsChildRender,
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
    customVariantTransition: CustomVariantTransition?,
    variableState: VariableState,
    parentTransition: AnimationTransition = DEFAULT_TRANSITION,
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

    // If there is a custom variant transition between the FROM and TO nodes, use it. Otherwise
    // use the parent transition if not null. Lastly use the default transition.
    val transition =
        customVariantTransition?.invoke(VariantTransitionContext(from.view, to.view))
            ?: parentTransition

    if (isTweenable(from.view, to.view)) {
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
                val c =
                    mergeRecursive(
                        matchingFromChild,
                        child,
                        n,
                        anims,
                        alreadyMatchedSet,
                        customVariantTransition,
                        variableState,
                        transition,
                    )
                if (previousChild != null) previousChild.nextSibling = c else n.firstChild = c
                previousChild = c
            } else {
                // No match; this is an incoming child, so clone it including children and give it
                // a fade in animation.
                val c = child.cloneSelfAndChildren(n)
                if (previousChild != null) previousChild.nextSibling = c else n.firstChild = c
                previousChild = c

                anims.add(SquooshAnimatedFadeIn(c, transition))
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

                anims.add(SquooshAnimatedFadeOut(c, transition))
            }
            child = child.nextSibling
        }

        // Now see what kind of animations we can make; starting with a layout animation.
        anims.add(SquooshAnimatedLayout(n, from, to, transition))

        // If they're both arcs, then they might need an arc animation.
        // XXX: Refactor this so we don't inspect every type right here.

        val fromArc: Shape.Arc? = from.view.data.ifContainerGetShape()?.let { it as? Shape.Arc }
        val toArc: Shape.Arc? = from.view.data.ifContainerGetShape()?.let { it as? Shape.Arc }
        if (fromArc != null && toArc != null && fromArc != toArc) {
            anims.add(SquooshAnimatedArc(n, fromArc, toArc, transition))
        }

        return n
    } else {
        // Ok, we need to clone both the "from" and "to" and make a crossfade animation between
        // them.
        val fromClone = from.cloneSelfAndChildren(parent)
        val toClone = to.cloneSelfAndChildren(parent)
        fromClone.nextSibling = toClone

        anims.add(SquooshAnimatedFadeOut(fromClone, transition))
        anims.add(SquooshAnimatedFadeIn(toClone, transition))
        if (shouldScale(from, to)) {
            anims.add(SquooshAnimatedScale(fromClone, from, to, true, transition))
            anims.add(SquooshAnimatedScale(toClone, from, to, false, transition))
        } else {
            anims.add(SquooshAnimatedLayout(fromClone, from, to, transition))
            anims.add(SquooshAnimatedLayout(toClone, from, to, transition))
        }

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
    alreadyMatchedSet: HashSet<Int>,
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
    if (needsStyleTween(a.style.get(), b.style.get())) return false

    val aShape = a.data.ifContainerGetShape()
    val bShape = b.data.ifContainerGetShape()
    return (aShape is Shape.Rect && bShape is Shape.Rect) ||
        (aShape is Shape.RoundRect && aShape is Shape.RoundRect) ||
        (aShape is Shape.VectorRect && bShape is Shape.VectorRect) ||
        (aShape is Shape.Arc && bShape is Shape.Arc)
}

private fun needsStyleTween(a: ViewStyle, b: ViewStyle): Boolean {
    // Compare some style things and decide if we need to tween the styles.
    if (a.nodeStyle.backgrounds != b.nodeStyle.backgrounds) return true
    if (a.nodeStyle.stroke != b.nodeStyle.stroke) return true
    return false
}

// Return true if we should do a scale animation instead of a layout size animation
private fun shouldScale(from: SquooshResolvedNode, to: SquooshResolvedNode): Boolean {
    val fromData = from.view.data
    val toData = to.view.data

    if (
        fromData.ifContainerGetShape() is Shape.Path && toData.ifContainerGetShape() is Shape.Path
    ) {
        return true
    } else {
        val fromText = fromData.ifTextGetText() ?: return false
        val toText = toData.ifTextGetText() ?: return false
        return (fromText.content == toText.content &&
            fromText.res_name == toText.res_name &&
            from.style.nodeStyle.font_family == to.style.nodeStyle.font_family)
    }
}

private fun ViewStyle.withNodeStyle(delta: (NodeStyle.Builder) -> Unit): ViewStyle {
    val builder = asBuilder()
    val nodeStyleBuilder = nodeStyle.asBuilder()
    delta(nodeStyleBuilder)
    builder.node_style = Optional.of(nodeStyleBuilder.build())
    return builder.build()
}
