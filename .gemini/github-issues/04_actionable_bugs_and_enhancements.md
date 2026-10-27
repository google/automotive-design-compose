# Category 4: Actionable Bugs & Enhancements — Requires Google Response or Code Fix

**Total Issues:** 26  
**Scope:** Confirmed bugs, architectural gaps, and high-value customer feature requests requiring code modifications, documentation additions, or engineering triage.

---

## Master Triage Table

| Issue | Title | Type | Priority | Affected Files | Dedicated Spec |
| :--- | :--- | :---: | :---: | :--- | :---: |
| [#120](https://github.com/google/automotive-design-compose/issues/120) | Re-make doc page for Custom Fonts | Documentation | **P3** | `docs/_docs/custom-fonts.md` | [Specs](./issues/issue-120.md) |
| [#121](https://github.com/google/automotive-design-compose/issues/121) | Re-make doc page on Figma Interactions | Documentation | **P3** | `docs/_docs/interactions.md` | [Specs](./issues/issue-121.md) |
| [#395](https://github.com/google/automotive-design-compose/issues/395) | Layout is broken on autolayout frames that are rotated | Bug | **P2** | `crates/dc_figma_import/src/transform_flexbox.rs` | [Specs](./issues/issue-395.md) |
| [#398](https://github.com/google/automotive-design-compose/issues/398) | Text alignment with multiple lines is incorrect | Bug | **P2** | `designcompose/src/main/java/com/android/designcompose/squoosh/SquooshText.kt` | [Specs](./issues/issue-398.md) |
| [#551](https://github.com/google/automotive-design-compose/issues/551) | DesignCompose does not download images larger than a certain size | Enhancement | **P2** | `crates/dc_figma_import/src/image_context.rs` | [Specs](./issues/issue-551.md) |
| [#573](https://github.com/google/automotive-design-compose/issues/573) | Make Brush function customization accept an `ImageContext` | Feature | **P2** | `designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt` | [Specs](./issues/issue-573.md) |
| [#635](https://github.com/google/automotive-design-compose/issues/635) | Allow `fetch` to get its nodes list from the JSON emitted by the KSP processor | Feature | **P2** | `crates/dc_figma_import/src/tools/fetch.rs` | [Specs](./issues/issue-635.md) |
| [#649](https://github.com/google/automotive-design-compose/issues/649) | Provide a means for users to access the Validation docs | Feature / Community | **P3** | `docs/_docs/working-with-source/figma-resources.md` | [Specs](./issues/issue-649.md) |
| [#661](https://github.com/google/automotive-design-compose/issues/661) | Improve documentation on finding and using the generated JSON file for Lists & Grids | Documentation | **P2** | `docs/_docs/modifiers/modifiers.md` | [Specs](./issues/issue-661.md) |
| [#673](https://github.com/google/automotive-design-compose/issues/673) | Support scale constraints with vectors | Bug | **P3** | `crates/dc_figma_import/src/transform_flexbox.rs` | [Specs](./issues/issue-673.md) |
| [#954](https://github.com/google/automotive-design-compose/issues/954) | Enable DesignModule usage across Gradle projects | Feature | **P2** | `codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt` | [Specs](./issues/issue-954.md) |
| [#1031](https://github.com/google/automotive-design-compose/issues/1031) | Vectors in Serialized Documents Don't Provide all Colors | Bug | **P2** | `crates/dc_figma_import/src/figma_schema.rs` | [Specs](./issues/issue-1031.md) |
| [#1117](https://github.com/google/automotive-design-compose/issues/1117) | ImageContext does not provide useful data when node constraints set to scale | Bug | **P2** | `designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt` | [Specs](./issues/issue-1117.md) |
| [#1138](https://github.com/google/automotive-design-compose/issues/1138) | Custom Design Modules must be in same directory as Generated Composable | Bug | **P2** | `codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt` | [Specs](./issues/issue-1138.md) |
| [#1169](https://github.com/google/automotive-design-compose/issues/1169) | Support duplicate variable names across multiple documents | Bug | **P2** | `designcompose/src/main/java/com/android/designcompose/VariableManager.kt` | [Specs](./issues/issue-1169.md) |
| [#1299](https://github.com/google/automotive-design-compose/issues/1299) | Control Figma UI prototype state in code | Feature | **P2** | `designcompose/src/main/java/com/android/designcompose/InteractionState.kt` | [Specs](./issues/issue-1299.md) |
| [#1300](https://github.com/google/automotive-design-compose/issues/1300) | Drag-Drop implementation | Feature | **P2** | `designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt` | [Specs](./issues/issue-1300.md) |
| [#1331](https://github.com/google/automotive-design-compose/issues/1331) | DesignVariant not working with figma component instances | Bug | **P1** | `designcompose/src/main/java/com/android/designcompose/InteractionState.kt` | [Specs](./issues/issue-1331.md) |
| [#1690](https://github.com/google/automotive-design-compose/issues/1690) | Figma Collections, Modes, Variables not available for query from design compose. | Feature | **P2** | `designcompose/src/main/java/com/android/designcompose/VariableManager.kt` | [Specs](./issues/issue-1690.md) |
| [#1798](https://github.com/google/automotive-design-compose/issues/1798) | Improvements for DesignSwitcher | Bug / Feature | **P2** | `designcompose/src/main/java/com/android/designcompose/DebugNodeManager.kt` | [Specs](./issues/issue-1798.md) |
| [#1814](https://github.com/google/automotive-design-compose/issues/1814) | Better support for RRO | Bug | **P1** | `designcompose/src/main/java/com/android/designcompose/DocServer.kt` | [Specs](./issues/issue-1814.md) |
| [#2113](https://github.com/google/automotive-design-compose/issues/2113) | Component overrides next | Bug | **P2** | `crates/dc_figma_import/src/transform_flexbox.rs` | [Specs](./issues/issue-2113.md) |
| [#2181](https://github.com/google/automotive-design-compose/issues/2181) | Customizations are not propagated to replacement component children | Bug | **P1** | `designcompose/src/main/java/com/android/designcompose/squoosh/SquooshTreeBuilder.kt` | [Specs](./issues/issue-2181.md) |
| [#2261](https://github.com/google/automotive-design-compose/issues/2261) | Same Figma node names do not execute smart animations. | Bug | **P2** | `designcompose/src/main/java/com/android/designcompose/squoosh/SquooshAnimate.kt` | [Specs](./issues/issue-2261.md) |
| [#2401](https://github.com/google/automotive-design-compose/issues/2401) | Support Figma OAuth 2.0 Token Authentication in Addition to Personal Access Tokens | Feature | **P2** | `crates/dc_figma_import/src/http_client.rs` | [Specs](./issues/issue-2401.md) |
| [#2456](https://github.com/google/automotive-design-compose/issues/2456) | Cannot apply Android Modifier-based animations to child components; works only via ComponentReplacementContext | Bug | **P2** | `designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt` | [Specs](./issues/issue-2456.md) |

---

## Documentation Gaps

### [#120: Re-make doc page for Custom Fonts](./issues/issue-120.md)
- **Priority:** `P3` | **Type:** `Documentation` | **Author:** @timothyfroehlich
- **Problem:** Missing dedicated documentation guide for Custom Fonts in DesignCompose.
- **Proposed Fix:** Author a dedicated `docs/_docs/custom-fonts.md` guide explaining font family matching, font asset directory placement, and fallback font configuration.
- **Detailed File:** [`./issues/issue-120.md`](./issues/issue-120.md)

### [#121: Re-make doc page on Figma Interactions](./issues/issue-121.md)
- **Priority:** `P3` | **Type:** `Documentation` | **Author:** @timothyfroehlich
- **Problem:** Missing comprehensive documentation page on Figma Interactions and Prototype Syncing.
- **Proposed Fix:** Author `docs/_docs/interactions.md` detailing Figma prototype triggers (On Tap, On Drag, Key events), supported reactions, variant animations, and plugin synchronization.
- **Detailed File:** [`./issues/issue-121.md`](./issues/issue-121.md)

### [#661: Improve documentation on finding and using the generated JSON file for Lists & Grids](./issues/issue-661.md)
- **Priority:** `P2` | **Type:** `Documentation` | **Author:** @iamralpht
- **Problem:** Improve documentation on finding and uploading generated JSON for Lists & Grids.
- **Proposed Fix:** Add a step-by-step section in `modifiers.md` specifying the exact Gradle build path (e.g., `build/generated/ksp/...`) and showing how to upload it to the Figma widget.
- **Detailed File:** [`./issues/issue-661.md`](./issues/issue-661.md)

---

## Community & Ecosystem

### [#649: Provide a means for users to access the Validation docs](./issues/issue-649.md)
- **Priority:** `P3` | **Type:** `Feature / Community` | **Author:** @timothyfroehlich
- **Problem:** Provide public read-only community access to the official Validation Figma documents.
- **Proposed Fix:** Publish read-only public links to the validation Figma files using the newly created Figma community team, and document the URLs in `figma-resources.md`.
- **Detailed File:** [`./issues/issue-649.md`](./issues/issue-649.md)

---

## Layout, Sizing & Constraint Bugs

### [#395: Layout is broken on autolayout frames that are rotated](./issues/issue-395.md)
- **Priority:** `P2` | **Type:** `Bug` | **Author:** @rylin8
- **Problem:** Layout is broken when an autolayout frame is rotated.
- **Proposed Fix:** Verify if PR #2439 (commit 797e29e7) and PR #2440 (commit 86e1a8b6) fully resolve the autolayout scenario from #395; add dedicated Roborazzi regression tests for rotated autolayout frames.
- **Detailed File:** [`./issues/issue-395.md`](./issues/issue-395.md)

### [#398: Text alignment with multiple lines is incorrect](./issues/issue-398.md)
- **Priority:** `P2` | **Type:** `Bug` | **Author:** @rylin8
- **Problem:** Text alignment with multiple lines is incorrect when auto-width is enabled.
- **Proposed Fix:** Update `SquooshText.kt` text measurement and layout bounds to properly align multiline paragraphs against container constraints; add golden comparison tests.
- **Detailed File:** [`./issues/issue-398.md`](./issues/issue-398.md)

### [#673: Support scale constraints with vectors](./issues/issue-673.md)
- **Priority:** `P3` | **Type:** `Bug` | **Author:** @rylin8
- **Problem:** Scale constraints do not scale arbitrary vector path geometry.
- **Proposed Fix:** Apply matrix scale transformations to vector path geometry during flexbox conversion when parent constraints are set to Scale, and add regression tests for vector icons.
- **Detailed File:** [`./issues/issue-673.md`](./issues/issue-673.md)

### [#1117: ImageContext does not provide useful data when node constraints set to scale](./issues/issue-1117.md)
- **Priority:** `P2` | **Type:** `Bug` | **Author:** @rylin8
- **Problem:** ImageContext does not provide useful dimensions when node constraints are set to scale.
- **Proposed Fix:** Pass measured runtime layout dimensions into `ImageContext` during the Squoosh render pass so shader and image customizations receive exact rendered pixel dimensions.
- **Detailed File:** [`./issues/issue-1117.md`](./issues/issue-1117.md)

---

## Component Overrides, Variants & Instances

### [#1138: Custom Design Modules must be in same directory as Generated Composable](./issues/issue-1138.md)
- **Priority:** `P2` | **Type:** `Bug` | **Author:** @Heneveld
- **Problem:** Custom Design Modules must be placed in the same directory/package as Generated Composables.
- **Proposed Fix:** Update KSP code generator in `BuildModuleClass.kt:185` to emit fully-qualified package imports or generate extensions in a unified public package.
- **Detailed File:** [`./issues/issue-1138.md`](./issues/issue-1138.md)

### [#1169: Support duplicate variable names across multiple documents](./issues/issue-1169.md)
- **Priority:** `P2` | **Type:** `Bug` | **Author:** @rylin8
- **Problem:** Support duplicate variable names across multiple documents.
- **Proposed Fix:** Scope variable collections by document ID (`docId: String`) in `VariableManager.kt` or support namespaced variable lookups.
- **Detailed File:** [`./issues/issue-1169.md`](./issues/issue-1169.md)

### [#1331: DesignVariant not working with figma component instances](./issues/issue-1331.md)
- **Priority:** `P1` | **Type:** `Bug` | **Author:** @miguel-galarza
- **Problem:** DesignVariant does not work when using multiple component instances.
- **Proposed Fix:** Include the unique instance hierarchy path in the `InteractionState` key so each component instance maintains independent variant state.
- **Detailed File:** [`./issues/issue-1331.md`](./issues/issue-1331.md)

### [#2113: Component overrides next](./issues/issue-2113.md)
- **Priority:** `P2` | **Type:** `Bug` | **Author:** @yiqunw700
- **Problem:** Component overrides collision and resolution in nested instances.
- **Proposed Fix:** Update `compute_component_overrides` to key overrides by canonical node ID hierarchy paths, and add regression tests for multi-level nested overrides.
- **Detailed File:** [`./issues/issue-2113.md`](./issues/issue-2113.md)

### [#2181: Customizations are not propagated to replacement component children](./issues/issue-2181.md)
- **Priority:** `P1` | **Type:** `Bug` | **Author:** @rylin8
- **Problem:** Customizations are not propagated to replacement component children.
- **Proposed Fix:** Pass the active `CustomizationContext` into `ComponentReplacementContext` so child composables can query and inherit parent customizations.
- **Detailed File:** [`./issues/issue-2181.md`](./issues/issue-2181.md)

---

## Animations & Gestures

### [#1299: Control Figma UI prototype state in code](./issues/issue-1299.md)
- **Priority:** `P2` | **Type:** `Feature` | **Author:** @miguel-galarza
- **Problem:** Control Figma UI prototype state programmatically in code.
- **Proposed Fix:** Expose a public `InteractionController` or `triggerReaction(nodeId: String)` API on `DesignView` to allow triggering Figma prototype interactions from application code.
- **Detailed File:** [`./issues/issue-1299.md`](./issues/issue-1299.md)

### [#1300: Drag-Drop implementation](./issues/issue-1300.md)
- **Priority:** `P2` | **Type:** `Feature` | **Author:** @miguel-galarza
- **Problem:** Drag-and-drop support between list containers.
- **Proposed Fix:** Design an extensible DragDropModifier or gesture interaction bridge integrating Jetpack Compose drag gestures with Squoosh list containers.
- **Detailed File:** [`./issues/issue-1300.md`](./issues/issue-1300.md)

### [#2261: Same Figma node names do not execute smart animations.](./issues/issue-2261.md)
- **Priority:** `P2` | **Type:** `Bug` | **Author:** @Gurupreet
- **Problem:** Same Figma node names do not execute smart animations.
- **Proposed Fix:** Refactor `findTweenNodes` in `SquooshAnimate.kt` to match nodes by name across trees even when parent container styling differs, enabling partial smart animations.
- **Detailed File:** [`./issues/issue-2261.md`](./issues/issue-2261.md)

### [#2456: Cannot apply Android Modifier-based animations to child components; works only via ComponentReplacementContext](./issues/issue-2456.md)
- **Priority:** `P2` | **Type:** `Bug` | **Author:** @kingartjr
- **Problem:** Cannot apply Android Modifier-based animations to child components.
- **Proposed Fix:** Support promoting animated modifier nodes into Squoosh replacement composables or provide animated modifier bridges for graphicsLayer transforms.
- **Detailed File:** [`./issues/issue-2456.md`](./issues/issue-2456.md)

---

## API & Developer Experience Enhancements

### [#551: DesignCompose does not download images larger than a certain size](./issues/issue-551.md)
- **Priority:** `P2` | **Type:** `Enhancement` | **Author:** @rylin8
- **Problem:** DesignCompose fails silently or errors without diagnostics on large Figma images.
- **Proposed Fix:** Add explicit file size and HTTP status code inspection in `http_fetch_image`, reporting actionable error messages (e.g. 'Image exceeds Figma CDN size limit') to the user and logcat.
- **Detailed File:** [`./issues/issue-551.md`](./issues/issue-551.md)

### [#573: Make Brush function customization accept an `ImageContext`](./issues/issue-573.md)
- **Priority:** `P2` | **Type:** `Feature` | **Author:** @iamralpht
- **Problem:** Make Brush function customization accept an ImageContext parameter.
- **Proposed Fix:** Add an overloaded `setBrushFunction(nodeName: String, brushFunction: (ImageContext) -> Brush)` in `CustomizationContext.kt`, passing the node's `ImageContext` during canvas render.
- **Detailed File:** [`./issues/issue-573.md`](./issues/issue-573.md)

### [#635: Allow `fetch` to get its nodes list from the JSON emitted by the KSP processor](./issues/issue-635.md)
- **Priority:** `P2` | **Type:** `Feature` | **Author:** @iamralpht
- **Problem:** Allow `fetch` tool to read node queries from JSON emitted by the KSP processor.
- **Proposed Fix:** Add `--nodes-file` / `--ksp-json` argument to `crates/dc_figma_import/src/tools/fetch.rs` to ingest node queries from KSP output automatically.
- **Detailed File:** [`./issues/issue-635.md`](./issues/issue-635.md)

### [#954: Enable DesignModule usage across Gradle projects](./issues/issue-954.md)
- **Priority:** `P2` | **Type:** `Feature` | **Author:** @timothyfroehlich
- **Problem:** Enable DesignModule usage across multi-project Gradle builds.
- **Proposed Fix:** Enhance KSP resolver in `BuildModuleClass.kt` to inspect compiled classpath metadata or generate standalone module provider descriptors on the compile classpath.
- **Detailed File:** [`./issues/issue-954.md`](./issues/issue-954.md)

### [#1690: Figma Collections, Modes, Variables not available for query from design compose.](./issues/issue-1690.md)
- **Priority:** `P2` | **Type:** `Feature` | **Author:** @Heneveld
- **Problem:** Expose public API to query Figma Collections, Modes, and Variables from code.
- **Proposed Fix:** Expose public inspection functions on `DesignSettings` / `VariableManager` (e.g. `getAvailableCollections()`, `getAvailableModes()`, `getCurrentMode()`).
- **Detailed File:** [`./issues/issue-1690.md`](./issues/issue-1690.md)

### [#1798: Improvements for DesignSwitcher](./issues/issue-1798.md)
- **Priority:** `P2` | **Type:** `Bug / Feature` | **Author:** @StephanSchuster
- **Problem:** DesignSwitcher drawNodeNames overlay has layout offset with system bars.
- **Proposed Fix:** Apply WindowInsets padding to `DebugNodeManager.DrawNodeNames()` and add a collapse toggle for the log viewer.
- **Detailed File:** [`./issues/issue-1798.md`](./issues/issue-1798.md)

### [#1814: Better support for RRO](./issues/issue-1814.md)
- **Priority:** `P1` | **Type:** `Bug` | **Author:** @rylin8
- **Problem:** Better support for Runtime Resource Overlays (RRO) in DocServer.
- **Proposed Fix:** Add `clearDocumentCache()` or `reloadRawResource()` to `DocServer.kt` to allow invalidating cached documents when Android configuration or RRO state changes.
- **Detailed File:** [`./issues/issue-1814.md`](./issues/issue-1814.md)

### [#2401: Support Figma OAuth 2.0 Token Authentication in Addition to Personal Access Tokens](./issues/issue-2401.md)
- **Priority:** `P2` | **Type:** `Feature` | **Author:** @ChristianDittrich
- **Problem:** Support Figma OAuth 2.0 Token Authentication in addition to Personal Access Tokens.
- **Proposed Fix:** Detect token format or add an `--oauth-token` flag, setting `Authorization: Bearer <token>` when OAuth tokens are provided.
- **Detailed File:** [`./issues/issue-2401.md`](./issues/issue-2401.md)

### [#1031: Vectors in Serialized Documents Don't Provide all Colors](./issues/issue-1031.md)
- **Priority:** `P2` | **Type:** `Bug` | **Author:** @johnoneil
- **Problem:** Vectors in serialized documents do not retain all colors from Figma vector networks.
- **Proposed Fix:** Add `fillOverrideTable` and `overrideID` to `figma_schema.rs` and update vector rasterization to map each path to its respective override fill.
- **Detailed File:** [`./issues/issue-1031.md`](./issues/issue-1031.md)

---
