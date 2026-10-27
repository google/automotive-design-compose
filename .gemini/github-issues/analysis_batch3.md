# GitHub Issues Analysis — Batch 3 (#1373 through #2456)

**Analysis Date:** 2026-10-26  
**Repository:** `google/automotive-design-compose`  
**Scope:** 28 Issues (#1373, #1419, #1446, #1450, #1544, #1555, #1568, #1690, #1721, #1792, #1798, #1814, #1994, #2113, #2138, #2145, #2181, #2183, #2188, #2189, #2190, #2208, #2245, #2261, #2330, #2332, #2401, #2456)

---

## 1. Executive Summary & Category Breakdown

Each issue is classified according to the 4 standard triage categories:
- **Category 1 (Stale / Awaiting Author Response / Answered):** Maintainer asked for info/response or answered a question 3+ months ago and got no follow-up.
- **Category 2 (Already Fixed in Commits / Releases):** Fixed by commits/PRs in the repository.
- **Category 3 (No Progress / Needs Author Relevance Check):** Old/backlog issue with no recent progress where we should ask the author if it is still relevant.
- **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix):** Needs a code fix or engineering design/response.

### Summary Table

| Issue # | Title | Author | Category | Status / Resolution Summary |
|---|---|---|---|---|
| **#1373** | Create a better separation between toolkit_schema and figma_schema | @timothyfroehlich | **Category 2** | Fixed in PR #1521 (commit `53b07ef2`) & PR #1886 (`ee08cce4`), released in v0.34.0 |
| **#1419** | This would be a candidate for writing an `Into` implementation in figma_schema | @timothyfroehlich | **Category 2** | Fixed in PR #1886 (commit `ee08cce4`), released in v0.34.0 (protobuf migration) |
| **#1446** | Animate between design token values | @rylin8 | **Category 3** | Backlog enhancement from 2024-07-29; no progress for over 2 years |
| **#1450** | Begin tightly vetting Rust dependencies | @timothyfroehlich | **Category 3** | Backlog process issue from 2024-07-29; no progress |
| **#1544** | Improve error handling: propagate an error from Rust and show it in DesignSwitcher | @yiqunw700 | **Category 2** | Fixed in PR #2374 (commit `b9e6756d`), released in v0.39.0 |
| **#1555** | More support for text stroke | @yiqunw700 | **Category 3** | Backlog P3 feature request from 2024-08-22; no progress |
| **#1568** | Determine the plan for managing Rust dependencies | @timothyfroehlich | **Category 3** | Empty backlog placeholder from 2024-08-27; no progress |
| **#1690** | Figma Collections, Modes, Variables not available for query from design compose | @Heneveld | **Category 4** | Actionable Customer Enhancement: `VariableManager` is internal; needs public query API |
| **#1721** | Dropdown list, Slider bar support | @BIN-KOUHINN | **Category 3** | Customer question from 2024-10-22 with no maintainer response for 2 years |
| **#1792** | Squoosh enabled cannot find variable for components imported from library | @Heneveld | **Category 2** | Fixed in PR #2206 (`3bc6997f`) & PR #2507 (`953bf979`) |
| **#1798** | Improvements for DesignSwitcher | @StephanSchuster | **Category 4** | Actionable Bug & Feature: `DebugNodeManager.DrawNodeNames()` offset bug with system bars |
| **#1814** | Better support for RRO | @rylin8 | **Category 4** | Actionable Bug: `DocServer.kt` caches `openRawResource` indefinitely without invalidation |
| **#1994** | Make component library with DC | @rylin8 | **Category 1** | Maintainer @anshu10sep asked for requirements on 2026-04-09; no author reply for >6 months |
| **#2113** | Component overrides next | @yiqunw700 | **Category 4** | Actionable Bug: `compute_component_overrides` name collision & nested component instances |
| **#2138** | RB HVAC investigation | @yiqunw700 | **Category 2** | Addressed via PR #2150 (commit `ba97a4db`), released in v0.35.0 |
| **#2145** | RTL support | @yiqunw700 | **Category 3** | Empty backlog placeholder from 2025-04-02; no progress |
| **#2181** | Customizations are not propagated to replacement component children | @rylin8 | **Category 4** | Actionable Bug: `SquooshTreeBuilder` skips children and omits parent customizations |
| **#2183** | Investigate improvements for testing for nodes | @rylin8 | **Category 3** | Backlog investigation ticket from 2025-04-30; no progress |
| **#2188** | Add support for insets in the Scalable UI plugin | @rylin8 | **Category 3** | Backlog plugin feature ticket from 2025-05-08; no progress |
| **#2189** | Add support for rounded corners in Scalable UI | @rylin8 | **Category 3** | Backlog exploration ticket from 2025-05-08; no progress |
| **#2190** | Allow progress bar and indicator preview simultaneously | @rylin8 | **Category 3** | Backlog plugin UI ticket from 2025-05-08; no progress |
| **#2208** | font family variables support | @farivar-google | **Category 3** | Backlog feature request from 2025-05-20; no progress |
| **#2245** | Scope Figma Draw features | @iamralpht | **Category 3** | Blocked on upstream Figma API; verified by author in 2025-07-05 |
| **#2261** | Same Figma node names do not execute smart animations | @Gurupreet | **Category 4** | Actionable Bug: `isTweenable()` in `SquooshAnimate.kt` aborts child recursion on style diffs |
| **#2330** | Layers with opacity start flickering when placed on another layer with opacity | @Elektrobit-HendrikCammann | **Category 2** | Fixed in PR #2400 (`93e5b254`), PR #2460 (`19682c01`), PR #2476 (`46ceece3`), v0.39.2+ |
| **#2332** | Component Instances are not render correctly until detached | @Elektrobit-HendrikCammann | **Category 1** | Maintainer @anshu10sep requested repro info on 2026-04-09 (>6 mo ago); also fixed in #2469/#2478 |
| **#2401** | Support Figma OAuth 2.0 Token Authentication in Addition to Personal Access Tokens | @ChristianDittrich | **Category 4** | Actionable Feature: `http_client.rs` hardcodes `X-Figma-Token` header; needs Bearer support |
| **#2456** | Cannot apply Android Modifier-based animations to child components | @kingartjr | **Category 4** | Actionable Bug: Child node modifiers have no Compose Layout node in Squoosh |

### Counts by Category
- **Category 1 (Stale / Awaiting Author Response):** 2 issues (#1994, #2332)
- **Category 2 (Already Fixed in Commits / Releases):** 6 issues (#1373, #1419, #1544, #1792, #2138, #2330)
- **Category 3 (No Progress / Needs Author Relevance Check):** 12 issues (#1446, #1450, #1555, #1568, #1721, #2145, #2183, #2188, #2189, #2190, #2208, #2245)
- **Category 4 (Actionable Bug / Enhancement):** 8 issues (#1690, #1798, #1814, #2113, #2181, #2261, #2401, #2456)

---

## 2. Detailed Analysis of Each Issue

---

### Issue #1373: Create a better separation between toolkit_schema and figma_schema
- **URL:** https://github.com/google/automotive-design-compose/issues/1373
- **Author:** @timothyfroehlich
- **Created:** 2024-07-15T21:48:52Z | **Updated:** 2024-08-21T20:58:33Z
- **Category:** **Category 2 (Already Fixed in Commits / Releases)**
- **Analysis & Evidence:**
  - The issue noted that `toolkit_schema` was sharing data types (like `Rectangle`, `Vector`, `Color`) directly with `figma_schema`.
  - In commit `53b07ef25cc797b6c6837b5df03bc53cc08c2338` (PR #1521, 2024-08-19, "Migrate ViewShape/Style/Data and View to dc_bundle"), `toolkit_schema.rs` was completely deleted.
  - Subsequently, in PR #1886 (commit `ee08cce4b5e7370e237c7b2617921ceadda811a5`, "[PROTOCONV] Convert the Kotlin code to Proto", merged 2025-01-13), the entire bundle definition was cleanly separated into Protocol Buffers under `crates/dc_bundle/src/proto`, decoupling Figma's schema from DesignCompose's internal wire format.
  - Included in release: **v0.34.0** (and all subsequent releases).
- **Recommendation:** Close issue as completed, referencing PR #1521 and PR #1886.

---

### Issue #1419: This would be a candidate for writing an `Into` implementation in figma_schema
- **URL:** https://github.com/google/automotive-design-compose/issues/1419
- **Author:** @timothyfroehlich
- **Created:** 2024-07-23T20:10:33Z | **Updated:** 2024-07-23T20:10:33Z
- **Category:** **Category 2 (Already Fixed in Commits / Releases)**
- **Analysis & Evidence:**
  - Originated from a PR #1414 code review comment by @rylin8 on converting Figma background types into `dc_bundle`.
  - The architectural migration to Protocol Buffers in PR #1886 (commit `ee08cce4b5e7370e237c7b2617921ceadda811a5`, merged 2025-01-13, released in **v0.34.0**) replaced the manual schema struct conversions with direct mapping into generated Protobuf builders.
  - The legacy `figma_schema` `Into` candidates became obsolete once `crates/dc_figma_import/src/transform_flexbox.rs` directly constructs the protobuf messages.
- **Recommendation:** Close issue as resolved/obsolete via PR #1886.

---

### Issue #1446: Animate between design token values
- **URL:** https://github.com/google/automotive-design-compose/issues/1446
- **Author:** @rylin8
- **Created:** 2024-07-29T20:28:46Z | **Updated:** 2024-09-25T22:13:00Z
- **Category:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Analysis & Evidence:**
  - Feature request by @rylin8 to animate transitions when design tokens (Figma variables or Material themes) switch modes (e.g. Light <-> Dark theme).
  - Current implementation in `designcompose/src/main/java/com/android/designcompose/VariableManager.kt` evaluates variable lookups synchronously in Compose without animation transitions (`ColorOrVar.getValue` directly returns `Color(color)` or `VariableManager.getColor`).
  - No commits, PRs, or comments have been made on this issue since September 2024 (> 2 years ago).
- **Recommendation:** Post a comment asking the author (@rylin8) if token value transition animation is still on the roadmap or if it should be closed as backlog.

---

### Issue #1450: Begin tightly vetting Rust dependencies
- **URL:** https://github.com/google/automotive-design-compose/issues/1450
- **Author:** @timothyfroehlich
- **Created:** 2024-07-29T21:51:28Z | **Updated:** 2026-03-17T16:34:47Z
- **Category:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Analysis & Evidence:**
  - Tracks a process requirement for security vetting of Rust crate dependencies (e.g., using `cargo-vet` or similar Google internal vetting tooling).
  - While GitHub Actions workflows (`.github/workflows/scorecard.yml`, `codeql.yml`, `dependency-review.yml`) were added to the repository, no dedicated Rust crate vetting policy or tool integration has been documented or mandated.
  - The issue had zero comments and was only updated via automated label/touch in March 2026.
- **Recommendation:** Ask maintainers whether dependency vetting is handled via GitHub dependency review and Scorecard actions or if specific crate vetting tooling is still needed.

---

### Issue #1544: Improve error handling: propagate an error from Rust and show it in DesignSwitcher
- **URL:** https://github.com/google/automotive-design-compose/issues/1544
- **Author:** @yiqunw700
- **Created:** 2024-08-20T22:14:24Z | **Updated:** 2024-08-20T22:14:24Z
- **Category:** **Category 2 (Already Fixed in Commits / Releases)**
- **Analysis & Evidence:**
  - Originated from PR #1513 review by @rylin8 requesting that Rust Figma deserialization errors be captured and propagated across JNI to the Kotlin UI/DesignSwitcher.
  - **Fixed by PR #2374** (commit `b9e6756dcd8af2a4fdd4c2a9e4c74753ebaa1d5b`, "Propagate detailed JSON serialization errors from Figma Import", merged 2026-03-06):
    - Added `serde_path_to_error` dependency in `crates/dc_figma_import/Cargo.toml`.
    - Updated `crates/dc_figma_import/src/document.rs`, `error.rs`, and `crates/dc_jni/src/error.rs` to propagate exact JSON node paths and error contexts through JNI to Kotlin `Feedback.kt` and `DesignSwitcher`.
  - Included in release: **v0.39.0** (and v0.38.3 backport in commit `c46d9047`).
- **Recommendation:** Close issue as completed by PR #2374.

---

### Issue #1555: More support for text stroke
- **URL:** https://github.com/google/automotive-design-compose/issues/1555
- **Author:** @yiqunw700
- **Created:** 2024-08-22T20:15:46Z | **Updated:** 2024-08-26T17:13:50Z
- **Labels:** `Type: New Feature`, `P3`
- **Category:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Analysis & Evidence:**
  - Filed after initial text stroke support in PR #1379 (commit `ea7913f9`) to track future enhancements: dash stroke and inside/outside stroke alignment.
  - Inspection of `designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRender.kt` (lines 369-370, 429, 448) confirms that Squoosh only supports centered solid text stroke (`// Right now only centered stroke is supported`, `drawStyle = Stroke(width = strokeWidth)`).
  - No progress, comments, or PRs since August 2024 (> 2 years ago).
- **Recommendation:** Check with author (@yiqunw700) if dashed or inside/outside text strokes are still required by any automotive partner design.

---

### Issue #1568: Determine the plan for managing Rust dependencies
- **URL:** https://github.com/google/automotive-design-compose/issues/1568
- **Author:** @timothyfroehlich
- **Created:** 2024-08-27T17:54:17Z | **Updated:** 2026-03-17T16:34:26Z
- **Category:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Analysis & Evidence:**
  - Empty description issue filed in August 2024.
  - Automated Dependabot updates (`.github/dependabot.yml`) already manage Rust dependencies via Cargo updates and PR batching (`combine-prs.yml`).
  - No specific management plan has been proposed or discussed.
- **Recommendation:** Ask author (@timothyfroehlich) if this ticket can be closed since Dependabot and CI workflows currently govern dependency updates.

---

### Issue #1690: Figma Collections, Modes, Variables not available for query from design compose
- **URL:** https://github.com/google/automotive-design-compose/issues/1690
- **Author:** @Heneveld
- **Created:** 2024-10-02T16:31:08Z | **Updated:** 2024-11-18T21:58:41Z
- **Labels:** `Type: Customer Issue`
- **Category:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)**
- **Detailed Technical Findings:**
  - **Problem Statement:** Customers building dynamic theme or mode pickers in Android automotive apps cannot discover what collections or modes are available in the loaded `.dcf` file without hardcoding strings into their Kotlin code.
  - **Source Files Inspected:**
    - `designcompose/src/main/java/com/android/designcompose/VariableManager.kt` (lines 160-235)
    - `designcompose/src/main/java/com/android/designcompose/DocServer.kt` (lines 554, 778, 803, 837)
    - `crates/dc_bundle/src/proto/definition/element/variable.proto`
  - **Root Cause:**
    `VariableManager` is declared as `internal object VariableManager` (line 163), and its internal `varMap: VariableMap` is private. Neither `DesignDoc`, `DocServer`, nor any public API in `com.android.designcompose` provides methods to query:
    1. The list of variable collections (`varMap.collectionsByIdMap.values`).
    2. The list of modes available for a given collection (`collection.modeIdHashMap.values`).
    3. The list of variable names and their types.
  - **Proposed Code Fix:**
    1. Create a public data model and facade in `designcompose`:
       ```kotlin
       data class DesignCollectionInfo(val id: String, val name: String, val modes: List<DesignModeInfo>)
       data class DesignModeInfo(val id: String, val name: String)
       ```
    2. Add public query functions on `DesignSettings` or `DesignDoc`:
       ```kotlin
       object DesignVariables {
           fun getCollections(docId: DesignDocId): List<DesignCollectionInfo> {
               val map = VariableManager.getVariableMap(docId) ?: return emptyList()
               return map.collectionsByIdMap.values.map { c ->
                   DesignCollectionInfo(c.id, c.name, c.modeIdHashMap.values.map { DesignModeInfo(it.id, it.name) })
               }
           }
       }
       ```
- **Recommendation:** Implement the public query facade in `VariableManager.kt` and provide an example in the validation app.

---

### Issue #1721: Dropdown list, Slider bar support
- **URL:** https://github.com/google/automotive-design-compose/issues/1721
- **Author:** @BIN-KOUHINN
- **Created:** 2024-10-22T09:04:12Z | **Updated:** 2024-11-18T21:58:25Z
- **Labels:** `Type: Customer Issue`
- **Category:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Analysis & Evidence:**
  - Customer asked how to bind state to Figma prototype dropdowns and sliders from Android code.
  - Author added a follow-up comment on 2024-10-23 asking for feedback; no maintainer responded for 2 years.
  - In DesignCompose, dropdowns and sliders are implemented either via Component Replacement (`ComponentReplacementContext` with Compose `DropdownMenu` or `Slider`) or via component variants and `@Design(node = ...) tapCallback: TapCallback` / MeterState.
- **Recommendation:** Respond to the author clarifying the recommended pattern (Component Replacement vs. Variants + TapCallback) and ask if their team still needs assistance.

---

### Issue #1792: Squoosh enabled cannot find variable for components imported from library
- **URL:** https://github.com/google/automotive-design-compose/issues/1792
- **Author:** @Heneveld
- **Created:** 2024-11-14T20:02:33Z | **Updated:** 2025-05-22T17:51:52Z
- **Labels:** `Type: Customer Issue`
- **Category:** **Category 2 (Already Fixed in Commits / Releases)**
- **Analysis & Evidence:**
  - Reported that when a component is imported from an external Figma library and has variable bindings, DesignCompose Squoosh logged: `Failed to get variable value for VariableID:<hash>/... in doc ...` and rendered baked fallback colors.
  - **Fixed by:**
    1. **PR #2206** (commit `3bc6997f`, "Fix variable resolution when variable is alias", merged May 2025).
    2. **PR #2507** (commit `953bf979bbe38f6870d0bb7681fa54dfcf3154c1`, "fix(dc_figma_import): keep local variable id when a remote component aliases the same variable", merged 2026-10-20):
       - Registered each variable in `variables_by_id` under its local ID (`id` and `var.id`) as well as the global alias ID (`global_id`), ensuring both local and remote nodes resolve correctly.
       - Added unit test `test_build_variable_map_retains_local_and_global_alias_ids`.
- **Recommendation:** Close issue as completed, referencing PR #2206 and PR #2507.

---

### Issue #1798: Improvements for DesignSwitcher
- **URL:** https://github.com/google/automotive-design-compose/issues/1798
- **Author:** @StephanSchuster
- **Created:** 2024-11-18T16:21:26Z | **Updated:** 2024-11-18T21:57:32Z
- **Labels:** `Type: Customer Issue`
- **Category:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)**
- **Detailed Technical Findings:**
  - **Problem Statement:**
    1. When rendering edge-to-edge with system bars padding (e.g. `Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) { DesignSwitcher(...) }`), the "Show node names" overlay renders all node names with an offset equal to the status bar height.
    2. No ability to preconfigure options (such as launching with "Show mini messages" disabled).
    3. Visual pixel jump between "Status" and "Options" tabs.
  - **Source Files Inspected:**
    - `designcompose/src/main/java/com/android/designcompose/DebugNodeManager.kt` (lines 105-125)
    - `designcompose/src/main/java/com/android/designcompose/DesignSwitcher.kt`
  - **Root Cause (Offset Bug):**
    In `DebugNodeManager.kt:113-117`:
    ```kotlin
    Box(
        modifier =
            Modifier.absoluteOffset(it.position.x.dp, it.position.y.dp)
                .size(it.size.width.dp, it.size.height.dp)
    )
    ```
    `DrawNodeNames()` is placed as a child inside the `DesignSwitcher` layout. If the `DesignSwitcher` composable is positioned in a sub-box offset by `systemBarsPadding()`, its local coordinate space origin `(0, 0)` is shifted by the status bar height. However, `it.position` is recorded in window/screen coordinates. Consequently, the overlay boxes are double-offset by the parent insets.
  - **Proposed Code Fix:**
    1. In `DebugNodeManager.kt`, render `DrawNodeNames()` inside a `Popup(properties = PopupProperties(focusable = false))` or measure `DesignSwitcher` root position via `onGloballyPositioned` and subtract the parent offset from `it.position`.
    2. Add configuration flags to `DesignSwitcherPolicy` or `DesignSettings` (e.g., `showMiniMessagesDefault = false`).
- **Recommendation:** Apply coordinate compensation in `DebugNodeManager.kt` and expose preconfiguration options.

---

### Issue #1814: Better support for RRO
- **URL:** https://github.com/google/automotive-design-compose/issues/1814
- **Author:** @rylin8
- **Created:** 2024-11-27T19:29:47Z | **Updated:** 2025-04-21T17:24:42Z
- **Category:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)**
- **Detailed Technical Findings:**
  - **Problem Statement:**
    When using `setRawResourceId(designDocId, R.raw.the_dcf_file)` to load `.dcf` from Android raw resources, dynamically updating the resource via Runtime Resource Overlay (RRO) leaves the original `.dcf` file cached in memory. The app continues to render the old design until process restart.
  - **Source Files Inspected:**
    - `designcompose/src/main/java/com/android/designcompose/DocServer.kt` (lines 121, 233-239, 802-808, 831-841)
  - **Root Cause:**
    In `DocServer.kt`:
    ```kotlin
    if (preloadedDoc != null && preloadedDoc.c.docId == docId) {
        return preloadedDoc
    }
    ```
    Once loaded, the decoded document is cached in `documents[docId] = decodedDoc` (line 833). When an RRO is applied or updated on Android, a configuration change is triggered on the application, but `DocServer` has no `ComponentCallbacks` listener and no cache invalidation method to clear `documents[docId]`. As a result, line 802 immediately returns the stale cached `preloadedDoc` without re-reading `context.resources.openRawResource(rawResource)`.
  - **Proposed Code Fix:**
    1. In `DocServer.kt`, add an invalidation method:
       ```kotlin
       fun invalidateRawResource(designDocId: DesignDocId) {
           synchronized(documents) { documents.remove(designDocId) }
       }
       ```
    2. In `DesignSettings`, register a `ComponentCallbacks` listener on the application context:
       ```kotlin
       context.registerComponentCallbacks(object : ComponentCallbacks {
           override fun onConfigurationChanged(newConfig: Configuration) {
               DocServer.rawResourceId.keys.forEach { DocServer.invalidateRawResource(it) }
           }
           override fun onLowMemory() {}
       })
       ```
- **Recommendation:** Implement configuration change cache invalidation for raw resources in `DocServer.kt`.

---

### Issue #1994: Make component library with DC
- **URL:** https://github.com/google/automotive-design-compose/issues/1994
- **Author:** @rylin8
- **Created:** 2025-01-14T00:03:29Z | **Updated:** 2026-04-09T23:07:36Z
- **Category:** **Category 1 (Stale / Awaiting Author Response / Answered)**
- **Analysis & Evidence:**
  - Feature test task proposing to create a component library from Material 3 Figma Community kit.
  - On **2026-04-09**, maintainer @anshu10sep commented:
    > "What is the requirement for this feature? Can you provide some additional details"
  - As of **2026-10-26**, over 6 months (> 3 months) have elapsed without any response or clarification from the author.
- **Recommendation:** Close as stale due to lack of author response, or invite the author to reopen when concrete requirements are established.

---

### Issue #2113: Component overrides next
- **URL:** https://github.com/google/automotive-design-compose/issues/2113
- **Author:** @yiqunw700
- **Created:** 2025-03-11T22:19:36Z | **Updated:** 2025-04-17T18:56:37Z
- **Category:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)**
- **Detailed Technical Findings:**
  - **Problem Statement:**
    1. In `crates/dc_figma_import/src/document.rs`, `override_table` maps node names to override data. If multiple nodes share the same name, overrides collide.
    2. Only character overrides were being checked; styledText and text overrides needed proper table lookup.
    3. Nested component instance overrides do not work across variant changes.
    4. Rounded corners and shape overrides are not properly calculated.
  - **Source Files Inspected:**
    - `crates/dc_figma_import/src/document.rs` (lines 1195-1260, 1360-1425)
    - Branch `remotes/origin/yiqunw/hvac_ui` (commit `84d8a176e1c4eb768b5974e9de3e3a50bd40584c`, "Support shape overrides and nested component overrides")
  - **Root Cause:**
    `compute_component_overrides` uses `view.name` as `overrides_table_key`. In Figma files where nodes within component instances are unnamed or share default names (e.g. "Frame", "Text"), overrides overwrite one another. In addition, when instances are nested within instances, `for_each_component_instance` does not track relative path hierarchies. The author @yiqunw attempted a fix in branch `yiqunw/hvac_ui` in March 2025, but it was never completed, rebased, or merged into `main`.
  - **Proposed Code Fix:**
    1. Key the `overrides_table` by structural node path or unique component query path rather than raw `view.name`.
    2. Port the shape override calculations from commit `84d8a176` into `crates/dc_figma_import/src/document.rs` and `ViewStyleUtils.kt`.
- **Recommendation:** Rebase and land the nested component and shape override fixes from `origin/yiqunw/hvac_ui` with hierarchical path keys.

---

### Issue #2138: RB HVAC investigation
- **URL:** https://github.com/google/automotive-design-compose/issues/2138
- **Author:** @yiqunw700
- **Created:** 2025-03-24T19:13:32Z | **Updated:** 2025-04-02T18:25:25Z
- **Labels:** `SystemUI`
- **Category:** **Category 2 (Already Fixed in Commits / Releases)**
- **Analysis & Evidence:**
  - Investigation tracking issue for Red Bull / SystemUI HVAC validation.
  - **Fixed by PR #2150** (commit `ba97a4db9f8140a05545af2155d6a637ff3b4b90`, "Add a RB hvac example to validation (#2150) Towards: #2138 Fixes: #2148", merged 2025-04-09):
    - Added `HvacComponentsDoc_YUU5qZIHRlSwXEbPPk3INh.dcf` and `HvacComponents.kt` in `integration-tests/validation`.
    - Added meter schema support in `crates/figma_import/src/meter_schema.rs`.
  - Included in release: **v0.35.0**.
- **Recommendation:** Close issue as completed by PR #2150.

---

### Issue #2145: RTL support
- **URL:** https://github.com/google/automotive-design-compose/issues/2145
- **Author:** @yiqunw700
- **Created:** 2025-04-02T18:24:12Z | **Updated:** 2025-04-02T18:25:14Z
- **Labels:** `SystemUI`
- **Category:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Analysis & Evidence:**
  - Empty placeholder issue with no description or comments.
  - No RTL / `LayoutDirection` mirror support currently exists in `dc_layout` or `SquooshRoot.kt`.
  - Zero activity since April 2025.
- **Recommendation:** Ask author (@yiqunw700) if RTL support is actively needed for SystemUI projects or if requirements can be defined.

---

### Issue #2181: Customizations are not propagated to replacement component children
- **URL:** https://github.com/google/automotive-design-compose/issues/2181
- **Author:** @rylin8
- **Created:** 2025-04-28T18:39:00Z | **Updated:** 2025-04-28T18:39:11Z
- **Category:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)**
- **Detailed Technical Findings:**
  - **Problem Statement:**
    Customizations passed to a root-level function (e.g. `Stage(title = "...", button = { ... })`) do not propagate down to children if the child is a component replacement.
  - **Source Files Inspected:**
    - `designcompose/src/main/java/com/android/designcompose/squoosh/SquooshTreeBuilder.kt` (lines 408-420)
    - `designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt` (lines 1312-1326)
    - `codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt` (lines 639-641)
  - **Root Cause:**
    In `SquooshTreeBuilder.kt:408-420`:
    ```kotlin
    if (replacementComponent != null) {
        composableList?.addChild(SquooshChildComposable(component = replacementComponent, node = resolvedView, ...))
        resolvedView.needsChildRender = true
        skipChildren = true
    }
    ```
    When a component is replaced, `skipChildren = true` causes `SquooshTreeBuilder` to skip the entire child subtree in the parent document. When the replacement lambda is executed in `SquooshChildLayout`, if it composes another DesignCompose `@DesignComponent`, that child component constructs a new `CustomizationContext()` that only merges `LocalCustomizationContext.current`. However, customizations targeted at child node names inside the replacement are not transferred from the parent's `CustomizationContext`.
  - **Proposed Code Fix:**
    In `SquooshChildLayout`, provide `LocalCustomizationContext` containing the parent's `customizationContext`, or pass a child-scoped customization context through `ComponentReplacementContext` so child `@DesignComponent` calls inherit the parent's customization bindings.
- **Recommendation:** Provide the parent `CustomizationContext` through `LocalCustomizationContext` in `SquooshChildLayout`.

---

### Issue #2183: Investigate improvements for testing for nodes
- **URL:** https://github.com/google/automotive-design-compose/issues/2183
- **Author:** @rylin8
- **Created:** 2025-04-30T18:47:49Z | **Updated:** 2025-04-30T18:47:49Z
- **Category:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Analysis & Evidence:**
  - Investigation ticket noting that `onNodeWithTag()` only works for nodes with click/tap interactions or explicit test tags, making node validation in unit tests difficult without hurting performance.
  - No proposals, comments, or PRs since April 2025.
- **Recommendation:** Check with author (@rylin8) if this investigation is still active.

---

### Issue #2188: Add support for insets in the Scalable UI plugin
- **URL:** https://github.com/google/automotive-design-compose/issues/2188
- **Author:** @rylin8
- **Assignee:** @farivar-google
- **Created:** 2025-05-08T17:17:27Z | **Updated:** 2025-05-08T17:22:29Z
- **Category:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Analysis & Evidence:**
  - Feature ticket proposing inset definitions in the Scalable UI Figma plugin (`support-figma/`) and parsing support in DCF.
  - No plugin or repository changes have been made for insets since May 2025.
- **Recommendation:** Check with author and assignee if Scalable UI inset support is still planned.

---

### Issue #2189: Add support for rounded corners in Scalable UI
- **URL:** https://github.com/google/automotive-design-compose/issues/2189
- **Author:** @rylin8
- **Assignee:** @farivar-google
- **Created:** 2025-05-08T17:22:04Z | **Updated:** 2025-05-08T17:22:33Z
- **Category:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Analysis & Evidence:**
  - Tracking ticket to explore reading corner radius on the main node of Scalable UI variants.
  - No changes or comments since May 2025.
- **Recommendation:** Check with author and assignee if this exploration is still needed.

---

### Issue #2190: Allow progress bar and indicator preview simultaneously
- **URL:** https://github.com/google/automotive-design-compose/issues/2190
- **Author:** @rylin8
- **Assignee:** @farivar-google
- **Created:** 2025-05-08T18:45:50Z | **Updated:** 2025-05-08T18:46:07Z
- **Category:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Analysis & Evidence:**
  - Feature request for the Dials & Gauges Figma plugin to preview progress bar and indicator with a single slider.
  - No changes in `support-figma/` since May 2025.
- **Recommendation:** Check with assignee (@farivar-google) if plugin updates are planned.

---

### Issue #2208: font family variables support
- **URL:** https://github.com/google/automotive-design-compose/issues/2208
- **Author:** @farivar-google
- **Created:** 2025-05-20T18:20:55Z | **Updated:** 2025-05-20T18:21:31Z
- **Labels:** `Type: New Feature`
- **Category:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Analysis & Evidence:**
  - Proposes binding Figma font family to variable modes.
  - In `crates/dc_bundle/src/proto/definition/view/node_style.proto`, `font_family` is `optional string font_family = 3;` (not variable-bound).
  - No comments or activity since May 2025.
- **Recommendation:** Check with author (@farivar-google) if font family variable binding is still needed.

---

### Issue #2245: Scope Figma Draw features
- **URL:** https://github.com/google/automotive-design-compose/issues/2245
- **Author:** @iamralpht
- **Created:** 2025-07-01T11:51:07Z | **Updated:** 2025-07-05T05:30:09Z
- **Category:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Analysis & Evidence:**
  - Investigated text on a path support from Figma Draw for instrument clusters.
  - On **2025-07-05**, @iamralpht attempted text path support in commit `62da8cb2da4c29af40ca49289f18e7d546827d54` and reported in Comment #1 that it was **blocked by Figma's REST API implementation**.
  - Blocked upstream; no activity since July 2025.
- **Recommendation:** Close as blocked on upstream Figma API, or verify whether Figma's REST API has added text path support.

---

### Issue #2261: Same Figma node names do not execute smart animations
- **URL:** https://github.com/google/automotive-design-compose/issues/2261
- **Author:** @Gurupreet
- **Created:** 2025-07-21T13:15:34Z | **Updated:** 2025-07-21T13:15:37Z
- **Category:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)**
- **Detailed Technical Findings:**
  - **Problem Statement:**
    When two nodes share the same name across variant states, Figma executes smart animation between them, but DesignCompose does not animate them, switching visibility instantly instead.
  - **Source Files Inspected:**
    - `designcompose/src/main/java/com/android/designcompose/squoosh/SquooshAnimate.kt` (lines 669-779, 814-841)
  - **Root Cause:**
    In `SquooshAnimate.kt:669`, `mergeRecursive()` checks:
    ```kotlin
    if (isTweenable(from.view, to.view)) {
        // Recurse over children and match by name
    } else {
        // Clone both from and to and crossfade
    }
    ```
    `isTweenable()` (lines 814-835) calls `needsStyleTween(a.style, b.style)`.
    If background fills or strokes differ:
    ```kotlin
    private fun needsStyleTween(a: ViewStyle, b: ViewStyle): Boolean {
        if (a.nodeStyle.backgroundsList != b.nodeStyle.backgroundsList) return true
        if (a.nodeStyle.stroke != b.nodeStyle.stroke) return true
        return false
    }
    ```
    If `needsStyleTween` is `true`, `isTweenable()` immediately returns `false`!
    When `isTweenable()` returns `false`, `mergeRecursive()` completely skips matching children! Instead, it clones the entire `from` and `to` subtrees and performs a crossfade / instantaneous visibility change (`SquooshAnimatedFadeOut` / `SquooshAnimatedFadeIn`). Even if child nodes have identical names, their layout and transforms are never animated.
  - **Proposed Code Fix:**
    In `SquooshAnimate.kt`, decouple child recursion from container tweenability:
    Even when `from.view` and `to.view` have different background styles, `mergeRecursive()` should still recurse into child nodes to match and animate children with identical names.
- **Recommendation:** Refactor `mergeRecursive()` in `SquooshAnimate.kt` so that container style differences do not abort child smart-animate matching.

---

### Issue #2330: Layers with opacity start flickering when placed on another layer with opacity
- **URL:** https://github.com/google/automotive-design-compose/issues/2330
- **Author:** @Elektrobit-HendrikCammann
- **Created:** 2025-11-13T18:17:52Z | **Updated:** 2025-11-13T18:17:52Z
- **Category:** **Category 2 (Already Fixed in Commits / Releases)**
- **Analysis & Evidence:**
  - Reported layer opacity flickering when layers with opacity are stacked on top of variable-bound gradients/images in v0.38.0.
  - **Fixed by:**
    1. **PR #2400** (commit `93e5b254`, "Fix Variables with included Opacity rendering (Issue #2328)").
    2. **PR #2460** (commit `19682c01`, "Fix gradient and fill opacity rendering with variables", merged 2026-05-13): Separated raw layer opacity from variable resolution in `crates/dc_figma_import/src/transform_flexbox.rs` and `GraphicUtils.kt`.
    3. **PR #2476** (commit `46ceece3`, "Fix DesignCompose exporter to explicitly serialize 1.0/0.0 opacity", merged 2026-06-15): Resolved missing vs. explicit `0.0` / `1.0` opacity ambiguities in protobuf serialization.
  - Included in release: **v0.39.2** and **v0.39.3**.
- **Recommendation:** Close issue as completed, referencing PR #2400, PR #2460, and PR #2476.

---

### Issue #2332: Component Instances are not render correctly until detached
- **URL:** https://github.com/google/automotive-design-compose/issues/2332
- **Author:** @Elektrobit-HendrikCammann
- **Created:** 2025-11-13T18:29:53Z | **Updated:** 2026-04-09T22:59:36Z
- **Category:** **Category 1 (Stale / Awaiting Author Response / Answered)**
- **Analysis & Evidence:**
  - Reported rare mispositioning of component instances on screen until detached in v0.38.0.
  - On **2026-04-09**, maintainer @anshu10sep commented:
    > "Can you please provide additional information and maybe a .fig file or a test app to repro this with step-by-step instructions to validate it"
  - As of **2026-10-26**, more than 6 months have passed with no response or reproduction sample provided by the author.
  - *(Note: Component instance positioning and variant merging fixes were also landed in PR #2469, PR #2470, and PR #2478 / commit `977a3ca5` in v0.39.3/v0.39.4).*
- **Recommendation:** Close as awaiting author response / stale, noting that variant style and positioning resolution improvements in v0.39.3+ likely addressed the issue.

---

### Issue #2401: Support Figma OAuth 2.0 Token Authentication in Addition to Personal Access Tokens
- **URL:** https://github.com/google/automotive-design-compose/issues/2401
- **Author:** @ChristianDittrich
- **Created:** 2026-03-16T13:51:38Z | **Updated:** 2026-03-16T13:51:38Z
- **Category:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)**
- **Detailed Technical Findings:**
  - **Problem Statement:**
    Figma Personal Access Tokens (PATs) are long-lived and user-bound, creating security and fragility issues for enterprise teams. Figma recommends OAuth 2.0 bearer tokens for integrations acting on behalf of users. Currently, DesignCompose only supports PATs.
  - **Source Files Inspected:**
    - `crates/dc_figma_import/src/http_client.rs` (lines 36, 187, 288)
    - `designcompose/src/main/java/com/android/designcompose/DocServer.kt`
  - **Root Cause:**
    In `crates/dc_figma_import/src/http_client.rs`:
    ```rust
    const FIGMA_TOKEN_HEADER: &str = "X-Figma-Token";
    // Attached unconditionally:
    let body = client
        .get(url.as_str())
        .header(FIGMA_TOKEN_HEADER, api_key)
    ```
    The HTTP client unconditionally attaches the PAT-specific header `X-Figma-Token: <token>`. Figma's OAuth 2.0 specification requires `Authorization: Bearer <token>`. Any OAuth token passed to DesignCompose is rejected by Figma with HTTP 403 Forbidden.
  - **Proposed Code Fix:**
    1. In `http_client.rs`:
       ```rust
       fn apply_figma_auth(builder: reqwest::RequestBuilder, token: &str) -> reqwest::RequestBuilder {
           if token.starts_with("Bearer ") {
               builder.header(reqwest::header::AUTHORIZATION, token)
           } else if token.contains('.') || token.starts_with("fig_oauth_") {
               builder.header(reqwest::header::AUTHORIZATION, format!("Bearer {}", token))
           } else {
               builder.header("X-Figma-Token", token)
           }
       }
       ```
    2. Update `async_http_fetch` and `http_fetch_batch` to use `apply_figma_auth`.
    3. Update `DocServer.kt` to allow setting an OAuth token or token provider.
- **Recommendation:** Implement OAuth Bearer token header detection in `http_client.rs` and update documentation.

---

### Issue #2456: Cannot apply Android Modifier-based animations to child components; works only via ComponentReplacementContext
- **URL:** https://github.com/google/automotive-design-compose/issues/2456
- **Author:** @kingartjr
- **Created:** 2026-05-05T22:22:04Z | **Updated:** 2026-05-19T15:22:23Z
- **Category:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)**
- **Detailed Technical Findings:**
  - **Problem Statement:**
    Applying Compose `Modifier` animations (such as `.graphicsLayer { rotationY = animatedRotation }`) via `@Design(node = "#Child") modifier: Modifier` works at the root screen level (`#Home`), but has no effect when applied to child/nested components (e.g. tiles in a dashboard). Developers are forced to replace the entire component using `ComponentReplacementContext`.
  - **Source Files Inspected:**
    - `designcompose/src/main/java/com/android/designcompose/squoosh/SquooshTreeBuilder.kt` (lines 405-485)
    - `designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt` (lines 753-774, 1312-1326)
    - `designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRender.kt` (lines 70-350)
  - **Root Cause:**
    Squoosh flattens and renders child nodes as raw Skia/Canvas drawing commands in the parent root `drawWithContent` block.
    1. In `SquooshTreeBuilder.kt`, a node with a registered modifier is not recognized as requiring an external composable unless it has a replacement or interaction.
    2. Even when a `composableChildModifier` is constructed in `SquooshRoot.kt` (line 772: `composableChildModifier.then(it)`), the actual pixels for that node are not rendered by the child layout—they were already drawn on the root canvas during the parent's render pass. Therefore, `Modifier.graphicsLayer` applied to `composableChildModifier` has nothing to transform.
  - **Proposed Code Fix:**
    - Community member @hbowden already verified a fix on branch `https://github.com/hbowden/automotive-design-compose/tree/nested-component-modifier-fix`:
    - When a nested component has a registered modifier in `CustomizationContext`, mark `resolvedView.needsChildRender = true` in `SquooshTreeBuilder.kt` and create a child composable sub-`SquooshRoot` with `renderSubtree = true`.
    - This isolates the component's draw commands into the child composable layout node, allowing `Modifier.graphicsLayer`, `animateFloatAsState`, and alpha animations to correctly transform the rendered pixels.
- **Recommendation:** Adopt and land the sub-SquooshRoot modifier fix from @hbowden's branch and add screenshot/animation unit tests.

---
