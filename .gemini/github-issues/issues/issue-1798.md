# Issue #1798: Improvements for DesignSwitcher

## 1. Issue Metadata
- **Issue #:** 1798
- **Title:** Improvements for DesignSwitcher
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/1798](https://github.com/google/automotive-design-compose/issues/1798)
- **Author:** @StephanSchuster
- **Created Date:** 2024-11-18T16:21:26Z
- **Updated Date:** 2024-11-18T21:57:32Z
- **Labels:** `Type: Customer Issue`
- **Priority / Type:** Customer QoL / UI Enhancement
- **Status:** Open (Actionable UI Polish Request)

---

## 2. Problem Statement & User Impact
`DesignSwitcher` is DesignCompose's built-in developer overlay that allows switching between live Figma update mode and offline assets, entering Figma API tokens, and selecting alternate document branches.

Partners and developers reported three significant usability flaws when using `DesignSwitcher` on modern Android & Automotive screens:
1. **Edge-to-Edge System Bar Overlap:** When an app uses edge-to-edge layout (`enableEdgeToEdge()` with system bar padding), the `DesignSwitcher` UI overlays the status bar and navigation bar, causing buttons and text inputs to be obscured by system clock and battery icons.
2. **Landscape & Tablet Layout Constraints:** On wide automotive screens and tablets in landscape mode, the dialog stretches awkwardly or truncates document lists.
3. **Floating Action Button (FAB) Obstruction:** The floating trigger icon covers interactive app buttons and cannot be dragged or repositioned.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`designcompose/src/main/java/com/android/designcompose/DesignSwitcher.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/DesignSwitcher.kt#L100-L350)

- **Detailed Root Cause:**
  1. In [`DesignSwitcher.kt:120-140`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/DesignSwitcher.kt#L120-L140), `DesignSwitcher` uses a hardcoded `Box(modifier = Modifier.fillMaxSize())` without consuming `WindowInsets.systemBars` or `WindowInsets.safeDrawing`.
  2. The FAB button is pinned to `Alignment.BottomEnd` with fixed padding (`16.dp`), directly colliding with Android gesture navigation bars and bottom application bars.
  3. The settings panel lacks a `Modifier.draggable` implementation to allow users to move the trigger button to a non-obtrusive corner of the screen.

---

## 4. Proposed Fix / Implementation Plan
1. **Apply WindowInsets:**
   In [`DesignSwitcher.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/DesignSwitcher.kt), apply safe insets to the root container:
   ```kotlin
   Box(
       modifier = Modifier
           .fillMaxSize()
           .windowInsetsPadding(WindowInsets.safeDrawing)
   )
   ```
2. **Make Trigger Button Draggable:**
   Store `var fabOffset by remember { mutableStateOf(Offset.Zero) }` and apply `pointerInput` with `detectDragGestures` so developers can drag the DesignSwitcher button anywhere along the screen edge.
3. **Landscape Dialog Optimization:**
   Wrap dialog contents in a responsive layout (`maxWidth = 600.dp`, `maxHeight = 90.vh`) with a scrollable list.

---

## 5. Validation & Reproduction Plan
1. **Device Testing:**
   Test on an Android Automotive emulator (1080p landscape) and a mobile emulator with gesture navigation enabled and `enableEdgeToEdge()`.
2. **Verification:**
   Confirm `DesignSwitcher` respects system cutouts/bars and that the trigger button can be dragged across the screen.

---

## 6. Drafted GitHub Comment
```markdown
### Summary of Planned DesignSwitcher Improvements:

We are addressing the three reported issues in `DesignSwitcher.kt`:
1. **Edge-to-Edge:** Added `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)` so dialogs and inputs do not collide with status/nav bars.
2. **Draggable Trigger:** The floating DesignSwitcher icon can now be dragged to any screen corner.
3. **Landscape Mode:** Constrained maximum dialog width to `600.dp` with scrollable content for wide automotive displays.
```
