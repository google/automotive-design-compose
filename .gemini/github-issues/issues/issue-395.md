# Issue #395: Layout is broken on autolayout frames that are rotated

## 1. Issue Metadata
- **Issue #:** 395
- **Title:** Layout is broken on autolayout frames that are rotated
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/395](https://github.com/google/automotive-design-compose/issues/395)
- **Author:** @rylin8
- **Created Date:** 2023-09-28T22:00:33Z
- **Updated Date:** 2023-11-01T20:22:51Z
- **Labels:** `:blue_square:  P3`, `layout`
- **Priority / Type:** P3 / Layout Bug
- **Status:** Open (Active / Verification Needed following recent rotation fixes)

---

## 2. Problem Statement & User Impact
When an AutoLayout container in Figma has rotated child elements, or when the AutoLayout frame itself is rotated (e.g. 45 degrees, 90 degrees, or 180 degrees), the layout calculations fail to produce correct positions and sizes in Android:
- Child elements overlap or are clipped unexpectedly.
- Parent AutoLayout frames compute bounding dimensions based on the unrotated element dimensions rather than the axis-aligned bounding box (AABB) of the rotated items.
- In nested AutoLayout scenarios, margins and spacing (`item_spacing`) are applied along the unrotated local axis rather than the parent layout flow direction.

This breaks speedometer dials, rotated indicators, vertical text strips, and chevron/arrow icons rotated via AutoLayout.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`crates/dc_figma_import/src/transform_flexbox.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/transform_flexbox.rs#L180-L260)
  - [`crates/layout/src/lib.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/layout/src/lib.rs)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt#L890-L930)

- **Detailed Root Cause:**
  1. **Figma vs Flexbox Rotation Semantics:**
     Figma allows any node in an AutoLayout frame to have an arbitrary rotation transform (`rotation: f32` or transform matrix). In Figma's layout engine, layout bounds for rotated children in AutoLayout are computed using their projected bounding box on the parent layout axes.
  2. **Flexbox Layout Engine Limitation:**
     In [`transform_flexbox.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/transform_flexbox.rs), DesignCompose converts Figma AutoLayout into Flexbox style rules (`Dimension::Points(width)`, `Dimension::Points(height)`). However, Flexbox does not natively understand 2D rotation transforms.
  3. **Recent Commits:**
     - PR #2439 (`797e29e7 Fix #244: Rotated node margin calculation uses pre-rotation size (#2439)`) fixed margin calculation for rotated nodes.
     - PR #2440 (`86e1a8b6 Fix #1279: Squoosh rotated composable placement (#2440)`) fixed composable placement for rotated child views.
     However, when an AutoLayout frame *itself* has rotation combined with `wrap` or non-standard alignment (`Center`, `SpaceBetween`), the layout solver in Rust still calculates Flexbox cross-axis lengths using pre-rotated sizes.

---

## 4. Proposed Fix / Implementation Plan
1. **Compute Projected Bounding Box in Rust Importer:**
   In [`crates/dc_figma_import/src/transform_flexbox.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/transform_flexbox.rs), when an AutoLayout child has a non-zero rotation:
   ```rust
   let rad = node.rotation.to_radians();
   let cos_a = rad.cos().abs();
   let sin_a = rad.sin().abs();
   let effective_w = unrotated_w * cos_a + unrotated_h * sin_a;
   let effective_h = unrotated_w * sin_a + unrotated_h * cos_a;
   ```
   Pass the effective width and height to the flex layout node, while retaining the original rotation transform matrix in `ViewStyle.transform` for rendering.
2. **Squoosh Tree Render Adjustment:**
   In [`SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt), ensure the pivot point for rotation is applied at the center of the pre-rotation bounds within the allocated layout box.

---

## 5. Validation & Reproduction Plan
1. **Robolectric & Scuba Golden Test:**
   Add a test case in [`integration-tests/validation/`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/integration-tests/validation/) containing:
   - Horizontal AutoLayout frame with children rotated at 45, 90, and 180 degrees.
   - Vertical AutoLayout frame with rotated children.
   - Rotated AutoLayout frame containing non-rotated children.
2. **Golden Image Comparison:**
   Compare the rendered Scuba golden image against Figma's desktop rendering.

---

## 6. Drafted GitHub Comment
```markdown
### Status Update on Rotated AutoLayout Frames:

Following the recent landing of PR #2439 (Fix #244: Rotated node margin calculation) and PR #2440 (Fix #1279: Squoosh rotated composable placement), significant improvements were made to rotated node positioning in Squoosh.

However, complex AutoLayout cases (where the parent AutoLayout frame itself is rotated or when rotated children use `SpaceBetween` / `Wrap` alignment) still require projecting child bounding boxes onto the parent flex axes prior to flexbox layout computation.

We are preparing an automated test case in `integration-tests/validation` to verify the remaining edge cases against Figma rendering.
```
