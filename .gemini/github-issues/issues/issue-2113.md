# Issue #2113: Component overrides next

## 1. Issue Metadata
- **Issue #:** 2113
- **Title:** Component overrides next
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/2113](https://github.com/google/automotive-design-compose/issues/2113)
- **Author:** @yiqunw700
- **Created Date:** 2025-03-11T22:19:36Z
- **Updated Date:** 2025-04-17T18:56:37Z
- **Labels:** *None*
- **Priority / Type:** Internal Architecture / Figma Component Overrides
- **Status:** Open (Actionable Importer & Squoosh Enhancement)

---

## 2. Problem Statement & User Impact
In Figma, when a designer embeds a component instance inside another component, they can customize/override properties of internal layers (text content, fills, visibility, variant states).

DesignCompose processes component overrides during import. However, two critical issues exist:
1. **Name Collision in Override Table:** In `crates/dc_figma_import/src/document.rs`, `override_table` maps node **names** to override data. If multiple sub-layers within an instance share the same name (e.g. two layers named `"Title"` or `"Icon"`), overrides intended for one layer corrupt or clobber the other.
2. **Text Replacement Overrides in Nested Components:** Overriding text inside deeply nested component instances fails to render properly because text layout properties are applied before instance swap resolution completes.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`crates/dc_figma_import/src/document.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/document.rs)
  - [`crates/dc_figma_import/src/transform_flexbox.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/transform_flexbox.rs)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshTreeBuilder.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshTreeBuilder.kt)

- **Detailed Root Cause:**
  1. Figma's internal schema identifies overriden sub-nodes using `overriddenSymbolID` or path-based node ID indexing (`"1:2;3:4"`).
  2. In `document.rs`, DesignCompose attempts to resolve overrides by node name (`node.name`). When designers use default layer names or standard design system names, multiple child nodes collide in the lookup map.
  3. Path-based hierarchical identifiers must be preserved in the serialized override table to disambiguate identical layer names.

---

## 4. Proposed Fix / Implementation Plan
1. **Switch Override Table to Path-Indexed Keys:**
   In [`document.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/document.rs):
   Index overrides by the hierarchical node ID path:
   ```rust
   pub struct ComponentOverrideKey {
       pub instance_id: String,
       pub target_node_id: String,
   }
   ```
2. **Fallback to Name Matching Only on Unambiguous Nodes:**
   If path resolution is unavailable, use name matching only if exactly one child in the component set matches that name.
3. **Preserve Overrides Across Nested Instances:**
   Update `SquooshTreeBuilder.kt` to apply text and style overrides hierarchically from outer instances to inner instances.

---

## 5. Validation & Reproduction Plan
1. **Test Document:**
   Create a component instance containing two distinct child layers named `"Label"` with different text overrides (`"Start"` and `"Finish"`).
2. **Verify Rendering:**
   Confirm both labels display their distinct text strings without clobbering each other.

---

## 6. Drafted GitHub Comment
```markdown
### Analysis & Fix for Component Overrides (#2113):

### Root Cause:
`crates/dc_figma_import/src/document.rs` indexes component instance overrides by simple node name. When a component contains multiple layers sharing the same name, overrides collide.

### Fix:
Index overrides using Figma's hierarchical node ID paths (`"${instanceId}:${subNodeId}"`) to ensure deterministic override resolution regardless of layer naming collisions.
```
