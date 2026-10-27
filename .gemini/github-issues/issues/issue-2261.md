# Issue #2261: Same Figma node names do not execute smart animations

## 1. Issue Metadata
- **Issue #:** 2261
- **Title:** Same Figma node names do not execute smart animations
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/2261](https://github.com/google/automotive-design-compose/issues/2261)
- **Author:** @Gurupreet
- **Created Date:** 2025-07-21T13:15:34Z
- **Updated Date:** 2025-07-21T13:15:37Z
- **Labels:** *None*
- **Priority / Type:** Bug / Smart Animation Engine
- **Status:** Open (Actionable Squoosh Animation Bug)

---

## 2. Problem Statement & User Impact
In Figma, Smart Animate matches layers with identical names across different frames/variants and smoothly interpolates their position, size, rotation, opacity, and corner radius.

In DesignCompose:
When switching between two variant states where child nodes share identical names, Smart Animate fails to interpolate the child nodes if their parent container has different background styles (such as changing from transparent to a solid fill, or changing background colors):
- Instead of smoothly gliding or resizing, child nodes jump abruptly or crossfade.
- Car dashboards, media play/pause state transitions, and expandable menus lose their fluid motion.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshAnimate.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshAnimate.kt#L120-L220)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt#L1310-L1345)

- **Detailed Root Cause:**
  In [`SquooshAnimate.kt:145-160`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshAnimate.kt#L145-L160):
  ```kotlin
  fun isTweenable(from: SquooshResolvedNode, to: SquooshResolvedNode): Boolean {
      if (from.view.name != to.view.name) return false
      if (from.view.data.node_type != to.view.data.node_type) return false
      if (needsStyleTween(from.style, to.style)) {
          // Aborts matching if style tween is unsupported on container!
          return false
      }
      return true
  }
  ```
  When `isTweenable()` returns `false`, `mergeRecursive()` aborts matching the children of that container! It falls back to treating the entire container and its children as disappearing/appearing nodes, performing a full crossfade (`SquooshAnimatedFadeOut` / `SquooshAnimatedFadeIn`) rather than recursing down to match child nodes with identical names.

---

## 4. Proposed Fix / Implementation Plan
1. **Decouple Child Matching from Container Style Tweening:**
   In [`SquooshAnimate.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshAnimate.kt):
   Refactor `mergeRecursive()` so that even if `from.view` and `to.view` have incompatible background styles (requiring a container crossfade), the function continues recursing into child nodes to identify matching layer names and generate child tween animations.
2. **Independent Child Transform Tracking:**
   Render the matched child nodes using their interpolated world layout coordinates rather than being locked to the fading parent container.

---

## 5. Validation & Reproduction Plan
1. **Reproduction Unit Test:**
   Create two variant frames:
   - Variant A: Container with Red background containing child `"Icon"` at (0, 0).
   - Variant B: Container with Blue background containing child `"Icon"` at (100, 100).
   Verify that `"Icon"`'s offset interpolates from (0, 0) to (100, 100) across `animationProgress = 0.5f`.

---

## 6. Drafted GitHub Comment
```markdown
### Technical Findings & Fix for Issue #2261:

### Root Cause:
In `SquooshAnimate.kt`, `isTweenable()` returns `false` if the container node's style differences cannot be smoothly tweened. When `isTweenable()` is false, `mergeRecursive()` aborts recursing into children, causing all child nodes to crossfade instead of animating.

### Fix:
Separate container style tweenability from child tree traversal in `mergeRecursive()`. Even when a container performs a crossfade, continue matching and smart-animating child nodes that share identical names.
```
