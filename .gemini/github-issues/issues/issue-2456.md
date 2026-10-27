# Issue #2456: Cannot apply Android Modifier-based animations to child components; works only via ComponentReplacementContext

## 1. Issue Metadata
- **Issue #:** 2456
- **Title:** Cannot apply Android Modifier-based animations to child components; works only via ComponentReplacementContext
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/2456](https://github.com/google/automotive-design-compose/issues/2456)
- **Author:** @kingartjr
- **Created Date:** 2026-05-05T22:22:04Z
- **Updated Date:** 2026-05-19T15:22:23Z
- **Labels:** *None*
- **Priority / Type:** Bug / Jetpack Compose Modifier Integration
- **Status:** Open (Actionable Squoosh Modifier Architecture Bug)

---

## 2. Problem Statement & User Impact
Developers building Android animations often use standard Compose Modifiers, such as:
- `Modifier.graphicsLayer { rotationY = animatedRotation; alpha = animatedAlpha }`
- `Modifier.scale(scale)`
- `Modifier.offset { IntOffset(x, y) }`

When targeting the root screen composable (`@Design(node = "#Home") modifier: Modifier`), Compose modifiers work as expected.
However, when developers apply a `Modifier` to a **child component or nested tile** (e.g. `@Design(node = "#Tile") tileModifier: Modifier`):
- The modifier has no visual effect on the child component.
- The child node remains static at its initial position and rotation.
- To animate the child tile, developers are forced to use `ComponentReplacementContext` to replace the entire component with a native Compose composable.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshTreeBuilder.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshTreeBuilder.kt#L405-L485)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt#L753-L774)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRender.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRender.kt)

- **Detailed Root Cause:**
  1. Squoosh is an ultra-fast renderer that flattens the Figma node hierarchy into direct Skia/Canvas drawing commands in a single root Compose `drawWithContent` block.
  2. Because child nodes are drawn directly by the parent root canvas pass, they do **not** have their own Compose `LayoutNode` or individual canvas layer.
  3. When `SquooshRoot.kt` encounters a child modifier:
     ```kotlin
     composableChildModifier.then(it)
     ```
     the modifier is attached to an empty overlay layout. The actual pixels for the child node were already drawn during the parent's canvas render pass! Therefore, `graphicsLayer` transforms applied to the child modifier transform an empty box while the rendered pixels remain unchanged on the root canvas.

---

## 4. Proposed Fix / Implementation Plan
1. **Mark Modified Nodes for Subtree Rendering:**
   Community member @hbowden demonstrated a validated solution:
   In [`SquooshTreeBuilder.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshTreeBuilder.kt), when a child node has a registered custom `Modifier` in `CustomizationContext`, mark `node.needsChildRender = true` and exclude its subtree from the parent's flat draw pass.
2. **Render Sub-SquooshRoot in Child LayoutNode:**
   In [`SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt):
   Instantiate a child `SquooshRoot` composable wrapped with the child's `Modifier`.
   Because the child draws its own content within its own Compose layout node, Compose `Modifier.graphicsLayer` transforms (rotation, scale, alpha) smoothly animate the child's rendered pixels.

---

## 5. Validation & Reproduction Plan
1. **Robolectric & Animation Test:**
   Create a test composable with a child tile animated via `rememberInfiniteTransition` and `graphicsLayer { rotationZ = angle }`.
   Verify that pixel dumps at different timestamps reflect the rotating tile.

---

## 6. Drafted GitHub Comment
```markdown
### Analysis & Fix for Issue #2456:

### Root Cause:
Squoosh flattens child nodes and draws them directly onto the parent root Canvas. Because child nodes do not possess independent Compose `LayoutNode`s, `Modifier.graphicsLayer` attached to child nodes has no visual effect on the pixels drawn on the root canvas.

### Proposed Fix:
Adopt the sub-SquooshRoot approach:
When a child node has an attached `Modifier`:
1. Exclude the child from the parent's flat draw pass in `SquooshTreeBuilder.kt`.
2. Spawn a child `SquooshRoot` wrapped in the child's custom `Modifier`.
This grants the child its own layout node and canvas layer, allowing Compose `graphicsLayer` animations (rotation, scale, alpha) to work seamlessly.
```
