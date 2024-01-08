# SQUOOSH DESIGN
November 23, 2023. Last updated: November 23, 2023.

Squoosh is a new design for the DesignCompose renderer that uses fewer Composables. We've always
created multiple Composable nodes per design node, which is excessive as DesignCompose moved to
perform vector rendering natively (meaning every vector element became several Composables, rather
than just having whole groups flattened to an image). We also integrated an external layout engine
so that we could share code between the Kotlin and native DesignCompose renderers. Supporting an
external layout engine made our existing Composable implementations more complex.

Squoosh builds its own tree of design elements, performs layout on it, and renders it. It embeds
child Composables for nested children and to handle input events. This design has the following
advantages:

 - it's easier to port to native, where we have another DesignCompose renderer in development.
 - the book-keeping for layout is centralized, so it is a bit easier to understand.
 - it's easier to profile and optimize, because there is less framework code in the hot paths.
 - it's easier to implement features like transitions and animation, because we can generate as many
   trees as we want and compare across them.
 - it looks like it will be a smaller codebase, but every codebase looks that way to start with ;).

## Status
- most rendering is OK, no divergence from regular DesignCompose.
- some customizations aren't implemented; custom images and custom content notably.
- scrolling isn't implemented.
- Lists and Grids is entirely unimplemented.

## Modules
Squoosh has a few key modules and functions:

 - `SquooshResolvedNode` is a design node in a tree that can be sent to layout. It's "resolved"
   because variants and some customizations have been applied.
 - `resolveVariantsRecursively` generates a `SquooshResolvedNode` tree from a DesignCompose `View`
   and all of the context (customizations, interaction state, etc). This function is currently
   slow and needs some optimization.
 - `updateLayoutTree` generates a `LayoutNodeList` from a `SquooshResolvedNode` and any previous
   set of layout values. This is typically pretty fast.
 - `SquooshLayout.doLayout` calls the JNI layout implementation to perform a layout iteration.
 - `populateComputedLayout` iterates a `SquooshResolvedNode` tree and copies values from the JNI
   layout iteration into the relevant `SquooshResolvedNode`.
 - `mergeTreesAndCreateSquooshAnimationControl` inspects two `SquooshResolvedNode` trees and updates
   the destination tree to have all of the nodes needed to show a transition from the "from" tree
   into the "to" tree.
 - `Modifier.squooshRender` knows how to render an entire `SquooshResolvedNode`--typically
   corresponding to the entire Composable.

## Typical Execution
Whenever squoosh needs to render, it will:

1. Build a new `SquooshResolvedNode` tree with `resolveVariantsRecursively`
2. Create a request to JNI layout using `updateLayoutTree`
3. Submit the request to JNI layout using `SquooshLayout.doLayout`
4. Put the results of the layout into the `SquooshResolvedNode` tree so that rendering is easier
   using `populateComputedLayout`
5. If there are transitions, then generate a new `InteractionState` (and soon `CustomizationContext`)
   with the transition values applied (these state objects start out without the action of the
   transition applied, then we ask them to make a copy with all active transitions applied -- so it
   is the state of the world once all transitions are complete). We then run through steps 1-4 again
   with the updated context.
    * This gives us a `SquooshResolvedNode` tree of the world after all transitions have ended.
    * We use `newSquooshAnimate` to create `SquooshAnimationControl` objects that control progress
      for each transition. `newSquooshAnimate` identifies similar nodes and different nodes and
      creates objects that will animate the appropriate property for each. Some nodes from the source
      tree get moved to the destination tree so that they can be faded out.
6. If there are transitions, we launch an effect to run an animation clock and update the state of
   each transition's animation. This is how we can seemingly run many different animations at once
   -- they all just show different positions between the two trees, and can't be nested.
7. We emit a `Layout` Composable that occupies the appropriate space and has a `squooshRender`
   Modifier attached.

## Nested Composables and input events
Squoosh still uses Compose extensively! Child Composables (including `AndroidView`) can be added,
and this is supported by having `resolveVariantsRecursively` generate a list of child Composables
that should be emitted (including externally supplied child Composables, and anything in the
DesignCompose tree that needs input).

Inside of the `Layout` Composable in `SquooshRoot`, we nest any Child Composables from our list.

Painting just one child Composable is tricky. Compose only lets us paint *all* child Composables in
one go. So we have a horrible hack where a boxed object is shared between the child Composables and
the `squooshRender` Modifier. When `squooshRender` wants to paint a particular child Composable it
updates the boxed object (a `SquooshRenderSelector`) to have the id of the node that should be
rendered. All of the child Composables have a Modifier that customizes drawing to completely skip
drawing unless the id in the `SquooshRenderSelector` matches. Yuck! But good enough...

## Animated transitions and enum-driven variant changes
Squoosh implements animated transitions, and can also animate variant changes caused by enum values
changing.

This is described above in the "typical flow". When there are animations to be run, they are always
between states of some kind. We generate two trees, merge parts of them that need to be animated
between, and then do 