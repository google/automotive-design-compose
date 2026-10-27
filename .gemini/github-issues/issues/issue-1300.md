# Issue #1300: Drag-Drop implementation

## 1. Issue Metadata
- **Issue #:** 1300
- **Title:** Drag-Drop implementation
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/1300](https://github.com/google/automotive-design-compose/issues/1300)
- **Author:** @yiqunw700
- **Created Date:** 2024-07-29T15:23:45Z
- **Updated Date:** 2024-07-29T15:23:45Z
- **Labels:** `enhancement`
- **Priority / Type:** Feature Request / UI Interactions
- **Status:** Open (Actionable Feature Request)

---

## 2. Problem Statement & User Impact
Automotive infotainment systems frequently require drag-and-drop interactions:
- Reordering favorites / shortcuts in an app drawer or dock.
- Dragging audio sources / media tracks into playlists.
- Dragging widgets or navigation tiles between split-screen panes.

DesignCompose currently provides no built-in drag-and-drop support:
- Lists and grids rendered via Squoosh only support scrolling and click interactions.
- Squoosh flattens nodes into direct canvas draw commands, preventing standard Compose `Modifier.draggable` or `Modifier.dragAndDropSource` from targeting individual items.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt)
  - [`designcompose/src/main/java/com/android/designcompose/ListWidget.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/ListWidget.kt)

- **Detailed Root Cause:**
  Because Squoosh optimizes rendering by drawing list rows directly to Canvas without separate LayoutNodes, the individual items do not exist as independent Compose LayoutNodes. Consequently, Android Compose's `dragAndDropSource` and pointer gesture recognizers cannot attach to individual rendered list rows without explicit custom hit-testing and dragging state tracking in Squoosh.

---

## 4. Proposed Fix / Implementation Plan
1. **Expose Item-level Component Replacement for Dragging:**
   For lists requiring reordering, allow developers to supply custom item composables using `ComponentReplacementContext` with `Modifier.draggable`.
2. **Native Squoosh Drag State Support:**
   Add pointer input detection (`pointerInput { detectDragGesturesAfterLongPress { ... } }`) in `SquooshRoot.kt` that tracks hit-tested item bounds, offsets the active dragged item during drawing, and fires `onItemReorder(fromIndex, toIndex)` callbacks.

---

## 5. Validation & Reproduction Plan
1. **Robolectric Drag Test:**
   Simulate drag gesture on a list item and verify `onItemReorder` callback receives correct indices.
2. **Integration Sample:**
   Create an interactive reorderable favorites grid in the media center demo app.

---

## 6. Drafted GitHub Comment
```markdown
### Drag-and-Drop Implementation Path:

Because Squoosh optimizes rendering by drawing list rows directly to Canvas without separate LayoutNodes, standard Compose `Modifier.draggable` cannot directly attach to internal items.

### Recommended Approaches:
1. **Workaround Today:** Use `ComponentReplacementContext` for the list items to wrap them in native Compose `Box(Modifier.draggable)`.
2. **Proposed Native Feature:** Implement gesture hit-testing in `SquooshRoot` to track dragging offsets and emit reorder events.
```
