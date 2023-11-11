package com.android.designcompose.squoosh

import android.util.Log
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewShape
import com.android.designcompose.serdegen.ViewStyle
import java.util.Optional

// Squoosh animation design
//
// We take two trees -- two SquooshResolvedNodes -- and then build a "transition tree" which combines
// them, and also generates a list of "animated properties" (which are functions that take a value
// 0..1 and updates things in the transition tree to be somewhere in between).
//
// Initially I'm just going to support Smart Animate, because it's the hardest, but I'll do the
// others once Smart Animate is in shape.

internal interface SquooshAnimatedItem {
    fun apply(value: Float) {}
}
internal class SquooshAnimatedFadeIn(private val target: SquooshResolvedNode) : SquooshAnimatedItem {
    override fun apply(value: Float) {
        target.style = target.style.with { s -> s.opacity = Optional.of(value) }
    }
}
internal class SquooshAnimatedFadeOut(private val target: SquooshResolvedNode) : SquooshAnimatedItem {
    override fun apply(value: Float) {
        target.style = target.style.with { s -> s.opacity = Optional.of(1.0f - value) }
    }
}
internal class SquooshAnimatedLayout(private val target: SquooshResolvedNode, private val from: Layout, private val to: Layout): SquooshAnimatedItem {
    override fun apply(value: Float) {
        val iv = 1.0f - value
        target.computedLayout = Layout(
            0,
            to.width * value + from.width * iv,
            to.height * value + from.height * iv,
            to.left * value + from.left * iv,
            to.top * value + from.top * iv
        )
    }
}
// XXX: No transform, coz no decomposition implemented yet.
//      No colors or strokes or corners.

internal class SquooshAnimate(
    val root: SquooshResolvedNode,
    private val items: List<SquooshAnimatedItem>
) {
    fun apply(value: Float) {
        for (item in items) {
            item.apply(value)
        }
    }
}

internal fun newSquooshAnimate(from: SquooshResolvedNode, fromNodeId: String, to: SquooshResolvedNode, toNodeId: String): SquooshAnimate? {
    val items = ArrayList<SquooshAnimatedItem>()
    val start = findNode(from, fromNodeId)
    val end = findNode(to, toNodeId)
    if (start == null) { Log.e("DC_SQUOOSH", "did not find node ${fromNodeId} in from tree"); dumpTree(from); Log.e("DC_SQUOOSH", "to..."); dumpTree(to)
    }
    if (end == null) { Log.e("DC_SQUOOSH", "did not find node ${toNodeId} in dest tree") }
    if (start != null && end != null) {
        mergeTreesWithAnimation(start, end, items)
        return SquooshAnimate(root = to, items = items)
    }
    return null
}

// Find nodes where we want to have animation; this is typically from a component CHANGE_TO
internal fun findNode(n: SquooshResolvedNode, id: String): SquooshResolvedNode? {
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

internal fun dumpTree(n: SquooshResolvedNode, indent: String = "") {
    Log.d("DC_SQUOOSH", "$indent ${n.view.name} (${n.view.id} unresolved: ${n.unresolvedNodeId} component info: ${n.view.component_info})")
    var child = n.firstChild
    while (child != null) {
        dumpTree(child, "$indent ")
        child = child.nextSibling
    }
}

// The algorithm for generating the transition tree from the source and dest trees is as follows:
//  1. Look at the children of a destination tree node
//  2. For each child:
//     - Look at the children of the corresponding source tree node
//     - Find the first matching node, if any. A matching node has the same name.
//     - If there's a matching node, create the appropriate transitions. Mark the source node as consumed, or remove it from the source tree. Maybe recurse?!?
//     - If there is no matching node, create a fade in transition.
//  3. Create fade out transitions for all of the other nodes in the source tree.
//
// Do we need to look backwards? Or is it OK to look forwards?
//
// We end up moving all of the nodes from the "from" tree to the "to" tree, and the "to" tree is the
// transition tree.
//
// Another model for this is that the rendering part of each node is done in isolation. So none of
// the nodes that render are parents after this.
internal fun mergeTreesWithAnimation(from: SquooshResolvedNode, to: SquooshResolvedNode, anims: ArrayList<SquooshAnimatedItem>) {
    var toChild = to.firstChild
    var prevChild: SquooshResolvedNode? = null

    while (toChild != null) {
        val matchInFromTree = extractChildNamed(from, toChild.view.name)
        // If we have a match, then actually ditch the `matchInFromTree` node, but use its layout
        // data, and try to match its kids into the `toChild` node.
        //
        // If both objects are the same kind of shape, then we can recurse into the children.
        // If not then we just do a crossfade and layout animation on the parents.
        if (matchInFromTree != null) {
            if (isTweenable(toChild.view, matchInFromTree.view)) {
                // We want to add all of `matchInFromTree`'s children to `toChild`, or match them.
                mergeTreesWithAnimation(matchInFromTree, toChild, anims)
                // Animate the node we're keeping from the old place to its current place.
                anims.add(SquooshAnimatedLayout(toChild, matchInFromTree.computedLayout!!, toChild.computedLayout!!))
            } else {
                // We're doing a layout crossfade.
                anims.add(SquooshAnimatedFadeIn(toChild))
                anims.add(SquooshAnimatedFadeOut(matchInFromTree))
                anims.add(SquooshAnimatedLayout(toChild, matchInFromTree.computedLayout!!, toChild.computedLayout!!))
                anims.add(SquooshAnimatedLayout(matchInFromTree, matchInFromTree.computedLayout!!, toChild.computedLayout!!))

                // Add the "from" node to our transition tree. Put it before the matching toChild
                // so that it draws behind.
                if (prevChild == null) {
                    to.firstChild = matchInFromTree
                } else {
                    prevChild.nextSibling = matchInFromTree
                }
                matchInFromTree.nextSibling = toChild
            }
        } else {
            // We didn't find a match. In this case we need to fade this node in.
            anims.add(SquooshAnimatedFadeIn(toChild))
        }
        prevChild = toChild
        toChild = toChild.nextSibling
    }

    // If there are any children remaining in `from` then we need to add them to the start of `to`
    // and create fade out animations for them.
    val fromFirstChild = from.firstChild
    var fromLastChild = from.firstChild
    while (fromLastChild?.nextSibling != null) {
        fromLastChild = fromLastChild.nextSibling
    }
    // Add fade-outs for all of them.
    var child = from.firstChild
    while (child != null) {
        anims.add(SquooshAnimatedFadeOut(child))
        child = child.nextSibling
    }
    // Move the from remainders into the start of the to list.
    if (fromLastChild != null) {
        fromLastChild.nextSibling = to.firstChild
        to.firstChild = fromFirstChild
    }
    from.firstChild = null
}

internal fun extractChildNamed(parent: SquooshResolvedNode, name: String): SquooshResolvedNode? {
    var child = parent.firstChild
    var previousChild: SquooshResolvedNode? = null
    while (child != null) {
        if (child.view.name == name) {
            // Remove child from the list.
            if (previousChild != null) {
                previousChild.nextSibling = child.nextSibling
            } else {
                parent.firstChild = child.nextSibling
            }
            child.nextSibling = null
            return child
        }
        previousChild = child
        child = child.nextSibling
    }
    return null
}

// We can tween between two views (and thus don't need one of them in the tree) if:
//  - They are both Containers
//  - They both have a Rect or RoundRect shape (for now we need the shapes to be the same).
internal fun isTweenable(a: View, b: View): Boolean {
    val aData = a.data
    val bData = b.data
    if (aData is ViewData.Container && bData is ViewData.Container) {
        return (aData.shape is ViewShape.Rect && bData.shape is ViewShape.Rect) || (aData.shape is ViewShape.RoundRect && bData.shape is ViewShape.RoundRect)
    }
    return false
}

// XXX: Horrible code to deal with our terrible generated types. Maybe if style moves to proto then
//      we'll get some more egonomic generated classes.
internal fun ViewStyle.asBuilder(): ViewStyle.Builder {
    val builder = ViewStyle.Builder()
    builder.text_color = text_color
    builder.font_size = font_size
    builder.font_family = font_family
    builder.font_weight = font_weight
    builder.font_style = font_style
    builder.font_stretch = font_stretch
    builder.background = background
    builder.box_shadow = box_shadow
    builder.stroke = stroke
    builder.opacity = opacity
    builder.transform = transform
    builder.relative_transform = relative_transform
    builder.text_align = text_align
    builder.text_align_vertical = text_align_vertical
    builder.text_overflow = text_overflow
    builder.text_shadow = text_shadow
    builder.text_size = text_size
    builder.line_height = line_height
    builder.line_count = line_count
    builder.font_features = font_features
    builder.filter = filter
    builder.backdrop_filter = backdrop_filter
    builder.blend_mode = blend_mode
    // We don't need to copy any of the layout properties, but do it anyway to
    // avoid confusion if this function later gets used more generally; we should
    // refactor ViewStyle to separate the fields used for rendering from those used
    // for layout.
    builder.display_type = display_type
    builder.position_type = position_type
    builder.flex_direction = flex_direction
    builder.flex_wrap = flex_wrap
    builder.grid_layout = grid_layout
    builder.grid_columns_rows = grid_columns_rows
    builder.grid_adaptive_min_size = grid_adaptive_min_size
    builder.grid_span_content = grid_span_content
    builder.overflow = overflow
    builder.max_children = max_children
    builder.overflow_node_id = overflow_node_id
    builder.overflow_node_name = overflow_node_name
    builder.align_items = align_items
    builder.align_self = align_self
    builder.align_content = align_content
    builder.justify_content = justify_content
    builder.top = top
    builder.left = left
    builder.bottom = bottom
    builder.right = right
    builder.margin = margin
    builder.padding = padding
    builder.item_spacing = item_spacing
    builder.cross_axis_item_spacing = cross_axis_item_spacing
    builder.flex_grow = flex_grow
    builder.flex_shrink = flex_shrink
    builder.flex_basis = flex_basis
    builder.bounding_box = bounding_box
    builder.horizontal_sizing = horizontal_sizing
    builder.vertical_sizing = vertical_sizing
    builder.width = width
    builder.height = height
    builder.min_width = min_width
    builder.min_height = min_height
    builder.max_width = max_width
    builder.max_height = max_height
    builder.aspect_ratio = aspect_ratio
    builder.pointer_events = pointer_events
    builder.meter_data = meter_data
    return builder
}

internal fun ViewStyle.with(delta: (ViewStyle.Builder) -> Unit): ViewStyle {
    val builder = asBuilder()
    delta(builder)
    return builder.build()
}