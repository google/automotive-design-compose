# Issue #1031: Vectors in Serialized Documents Don't Provide all Colors

## 1. Issue Metadata
- **Issue #:** 1031
- **Title:** Vectors in Serialized Documents Don't Provide all Colors
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/1031](https://github.com/google/automotive-design-compose/issues/1031)
- **Author:** @rylin8
- **Created Date:** 2024-05-15T21:49:57Z
- **Updated Date:** 2024-05-15T21:49:57Z
- **Labels:** `bug`
- **Priority / Type:** Bug / Serialization & Vector Rendering
- **Status:** Open (Actionable Importer Bug)

---

## 2. Problem Statement & User Impact
In Figma, vector nodes (such as compound shapes, complex multi-color SVG icons, or illustrations) can have distinct fills and strokes assigned to individual paths within the node (via vector networks or `fillOverrideTable`).

When DesignCompose serializes these vector nodes into `.dcf` format:
- All paths within the vector node are assigned a single, uniform color (typically the first fill or the parent node's background fill).
- Secondary colors, multi-color accents, and individual path fill overrides are lost.
- Icons such as Google Maps pins (red pin with white center), battery indicators (grey outline with green charge bar), or multi-color automotive status icons render as flat monochrome silhouettes.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`crates/dc_figma_import/src/figma_schema.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/figma_schema.rs#L305-L310)
  - [`crates/dc_figma_import/src/transform_flexbox.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/transform_flexbox.rs#L1900-L1940)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRender.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRender.kt)

- **Detailed Root Cause:**
  1. In Figma's REST API schema, multi-colored vector nodes store fill overrides in `fillOverrideTable: HashMap<u32, Paint>` and individual path geometries reference these overrides via `overrideID: Option<u32>` on each `Path` item.
  2. In [`figma_schema.rs:305-310`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/figma_schema.rs#L305-L310), `Path` deserialization does not include `overrideID`:
     ```rust
     #[derive(Deserialize, Debug, Clone)]
     pub struct Path {
         pub path: String,
         pub winding_rule: Option<WindingRule>,
         // Missing: pub override_id: Option<u32>
     }
     ```
  3. In [`transform_flexbox.rs:1905`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/transform_flexbox.rs#L1905), the transformer takes a single fill collection from `node.fills` and assigns it across all vector paths in `View.vector_paths`:
     ```rust
     let fills = node.fills.clone();
     // Fills are assigned to the entire node style rather than per-path
     ```
  4. In `SquooshRender.kt`, the renderer draws each path in `vectorPaths` using the parent view's brush/paint.

---

## 4. Proposed Fix / Implementation Plan
1. **Update Rust Figma Schema:**
   In [`crates/dc_figma_import/src/figma_schema.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/figma_schema.rs):
   - Add `override_id: Option<u32>` to `Path`.
   - Add `fill_override_table: Option<HashMap<u32, Vec<Paint>>>` to `Node`.
2. **Associate Paints with Paths in `transform_flexbox.rs`:**
   Update `VectorPath` proto definition to include optional `fills: Vec<Paint>` per path:
   ```rust
   let path_fills = if let Some(override_id) = path.override_id {
       node.fill_override_table.as_ref()
           .and_then(|table| table.get(&override_id))
           .cloned()
           .unwrap_or_else(|| node.fills.clone())
   } else {
       node.fills.clone()
   };
   ```
3. **Update Squoosh Rendering in Kotlin:**
   In `SquooshRender.kt`, when iterating over `view.vectorPaths`, use `vectorPath.fills` if present instead of `view.style.fills`.

---

## 5. Validation & Reproduction Plan
1. **Figma Test Fixture:**
   Create a test vector in Figma composed of 3 subpaths with Red, Green, and Blue fills.
2. **Fetch and Verify Serialization:**
   Run `fetch` and inspect the generated `.dcf` file or JSON output to verify individual paths retain their specific color paints.
3. **Scuba Golden Test:**
   Add a test case in `integration-tests/validation` verifying multi-color rendering parity.

---

## 6. Drafted GitHub Comment
```markdown
### Technical Findings & Fix for Issue #1031:

### Root Cause:
1. `crates/dc_figma_import/src/figma_schema.rs` omits `overrideID` on `Path` and `fillOverrideTable` on `Node`.
2. `transform_flexbox.rs` assigns the parent node's global `fills` list to all paths uniformly.

### Planned Fix:
- Deserialize `overrideID` and `fillOverrideTable` from Figma's API.
- Attach resolved fill paints to each `VectorPath` in the serialized proto.
- Update `SquooshRender.kt` to paint each path with its specific fill brush.
```
