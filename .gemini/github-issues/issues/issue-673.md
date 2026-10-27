# Issue #673: Support scale constraints with vectors

## 1. Issue Metadata
- **Issue #:** 673
- **Title:** Support scale constraints with vectors
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/673](https://github.com/google/automotive-design-compose/issues/673)
- **Author:** @rylin8
- **Created Date:** 2023-12-28T23:18:17Z
- **Updated Date:** 2024-01-02T21:43:04Z
- **Labels:** `:blue_square:  P3`, `layout`
- **Priority / Type:** P3 / Layout & Rendering Bug
- **Status:** Open (Active Vector Scaling Limitation)

---

## 2. Problem Statement & User Impact
In Figma, vector nodes (e.g. custom shapes, icons, telltale warning lights, complex curves) can be assigned horizontal and vertical constraints set to `SCALE`. When their parent frame is resized, Figma automatically scales the vector path geometry proportionally.

In DesignCompose:
- Scale constraints only resize the bounding box / frame container of the vector node.
- The internal vector path data (SVG path commands: `M`, `C`, `L`, `Z`) remains fixed at the original design dimensions.
- As a result, the vector does not scale to fill the frame, appearing clipped, uncentered, or tiny inside an expanded container.
- When an earlier attempt was made to fix this, it caused regressions across telltales and vehicle status graphics.

This affects responsive UI layouts where icons and vector graphics must scale across different automotive screen resolutions and display densities.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`crates/dc_figma_import/src/transform_flexbox.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/transform_flexbox.rs#L1880-L1930)
  - [`crates/dc_figma_import/src/figma_schema.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/figma_schema.rs#L300-L330)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRender.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRender.kt)

- **Detailed Root Cause:**
  1. In [`transform_flexbox.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/transform_flexbox.rs), vector paths are parsed and stored as pre-rasterized/pre-computed path segments relative to the original design width and height (`node.size.x`, `node.size.y`).
  2. When constraints are `ConstraintType::Scale`, the flexbox layout solver computes a new `layout.width` and `layout.height`.
  3. However, during rendering in Squoosh (`SquooshRender.kt`), vector drawing directly executes the Skia path commands without applying a 2D scale transform matrix (`Matrix().apply { setScale(newWidth / originalWidth, newHeight / originalHeight) }`).
  4. The previous regression noted by @rylin8 occurred because scaling was applied uniformly to all vector nodes, inadvertently distorting vectors that had fixed constraints inside auto-layout wrappers.

---

## 4. Proposed Fix / Implementation Plan
1. **Record Original Design Size in `View`:**
   Ensure `View` stores `design_size: (f32, f32)` alongside the vector paths.
2. **Apply Scaling Transform in `SquooshRender.kt`:**
   In [`SquooshRender.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRender.kt), when rendering a node containing `vectorPaths` where horizontal or vertical constraints are `Scale`:
   ```kotlin
   val scaleX = if (horizontalConstraint == Scale) layoutWidth / designWidth else 1.0f
   val scaleY = if (verticalConstraint == Scale) layoutHeight / designHeight else 1.0f
   
   if (scaleX != 1.0f || scaleY != 1.0f) {
       canvas.save()
       canvas.scale(scaleX, scaleY)
       // Draw vector path
       canvas.restore()
   } else {
       // Draw vector path normal
   }
   ```
3. **Respect Aspect Ratio / Non-Uniform Scaling:**
   Support both uniform and non-uniform scaling depending on Figma's constraint configuration.

---

## 5. Validation & Reproduction Plan
1. **Reproduction Unit Test:**
   Create a test vector in `integration-tests/validation` with constraint `SCALE` placed inside a frame that resizes from 100x100 to 200x200.
2. **Regression Check:**
   Verify that existing telltale icons and non-scaling vectors maintain pixel-perfect alignment against existing Scuba goldens.

---

## 6. Drafted GitHub Comment
```markdown
### Analysis & Fix Strategy for Vector Scale Constraints:

### Root Cause:
Currently, scale constraints only resize the layout bounds allocated by the flex solver; the Skia vector path commands are drawn at their 1:1 original design size without a scale transformation matrix.

### Proposed Fix:
In `SquooshRender.kt`, apply a canvas matrix scale transformation when drawing vector paths if the node has `Constraint.Scale`:
```kotlin
val scaleX = node.computedLayout.width / node.designWidth
val scaleY = node.computedLayout.height / node.designHeight
canvas.withSave {
    canvas.scale(scaleX, scaleY)
    drawVectorPath(node.vectorPaths)
}
```
This isolates the scaling to vector rendering without modifying flexbox bounds or regressing fixed-size telltales.
```
