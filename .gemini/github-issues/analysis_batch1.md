# GitHub Issues Triage Analysis: Batch 1 (#75 – #690)
**Repository:** `google/automotive-design-compose`  
**Current Date:** October 26, 2026  
**Scope:** 42 issues (#75 through #690) analyzed against full git history, PRs/releases, and current repository codebase.

---

## Executive Summary

| Category | Count | Issue Numbers |
| :--- | :---: | :--- |
| **Fully Fixed / Implemented / Resolved** | 10 | #177, #268, #272, #278, #290, #561, #604, #606, #688, #690 |
| **Obsolete / Superseded by Architecture Changes** | 7 | #75, #269, #270, #271, #273, #274, #275 |
| **Awaiting Author / Stale (>2–3 Years Inactive)** | 10 | #93, #97, #101, #112, #132, #136, #225, #250, #282, #628 |
| **Active / Actionable (Feature / Enhancement / Bug / Docs)** | 15 | #95, #103, #120, #121, #131, #395, #398, #469, #551, #573, #635, #649, #661, #673, #684 |

---

## Detailed Issue Analysis

### Issue #75: Create method for fetching the serialized Figma files for an app via Gradle task
- **Author:** @timothyfroehlich | **Created:** 2023-05-19 | **Updated:** 2026-03-17 | **Labels:** `Type: Customer Issue`, `engProd`, `P2` | **Comments:** 0
- **Status:** **Superseded / Partially Implemented**
- **Git & Codebase Findings:**
  - Timothy filed subtasks #268 through #275 to break this work down.
  - An internal Gradle task plugin was created in `plugins/gradle-plugin-internal/src/main/java/com/android/designcompose/gradle/internal/InternalGradlePlugin.kt` registering `fetchFigmaFiles` and `fetchAndUpdateFigmaFiles` (introduced via commit `d5b554e1` in PR #542, improved in PR #1611 `1ac4a28b`). These tasks fetch serialized `.dcf` files for Robolectric/Roborazzi tests and copy them to `src/main/assets/figma`.
  - In addition, a standalone cross-platform `fetch` CLI binary was released in PR #2309 (commit `38e26477`, `feat: Add Rust CLI tools to release process and update docs`) for offline asset fetching.
  - The original concept of bundling a standalone Gradle plugin task fetching directly during app compilation was superseded by the internal plugin + release of the `fetch` CLI.
- **Recommendation:** **Close as Completed / Superseded** with a reference to `InternalGradlePlugin` (`fetchFigmaFiles`) and the released `fetch` CLI binary.

---

### Issue #93: Add method to input Figma Design Token via Design Switcher
- **Author:** @timothyfroehlich | **Created:** 2023-05-31 | **Updated:** 2026-03-17 | **Labels:** `Type: New Feature`, `needs design`, `qol`, `P3`, `top-qol` | **Comments:** 1
- **Status:** **Stale / Needs Triage**
- **Git & Codebase Findings:**
  - Issue body was left empty. On 2024-06-20, Tim commented: *"Need a better description."* No further details or activity occurred in over 2 years.
  - Figma authentication currently functions via personal access tokens saved to `~/.config/figma_access_token` or broadcast via ADB / Gradle `setFigmaToken`.
  - Design Switcher provides file/branch switching and Live Update toggle/frequency controls (`docs/_docs/live-update/design-switcher.md`), but entering a secret token via in-app UI poses security considerations on shared vehicles/devices.
- **Recommendation:** **Close as Stale / Won't Fix** or ask if still desired given token file and ADB flows.

---

### Issue #95: Add emulator tests to CI
- **Author:** @timothyfroehlich | **Created:** 2023-05-31 | **Updated:** 2026-03-17 | **Labels:** `needs design`, `engProd`, `P1`, `Type: Task` | **Comments:** 4
- **Status:** **Superseded / Active Decision**
- **Git & Codebase Findings:**
  - Tim noted in June 2023 that emulator actions were added and reverted due to extreme flakiness.
  - Instead of emulators, the team adopted Roborazzi host screenshot testing (`roborazzi-record-screenshot.yml`, `roborazzi-compare-screenshot.yml`, recently migrated to Linux CI in commit `b66c4fa6` / PR #2479) and unit/integration tests running without an emulator.
  - Related to #684 (Firebase Test Lab).
- **Recommendation:** **Close as Superseded** by Roborazzi CI tests, or link to #684 if Cloud Emulator/FTL testing is still on the roadmap.

---

### Issue #97: Create method for setting the Figma Token once per device
- **Author:** @timothyfroehlich | **Created:** 2023-05-31 | **Updated:** 2026-03-17 | **Labels:** `Type: New Feature`, `qol`, `P3` | **Comments:** 1
- **Status:** **Stale / Won't Fix**
- **Git & Codebase Findings:**
  - Issue was empty. Tim commented on 2024-06-20: *"Needs better description."* No activity since.
  - Devices can receive the token via `./gradlew setFigmaToken` (which iterates installed apps and sends the broadcast via ADB) or setting system properties.
- **Recommendation:** **Close as Stale**.

---

### Issue #101: Automatically stage Release Candidates to gMaven
- **Author:** @timothyfroehlich | **Created:** 2023-05-31 | **Updated:** 2026-03-17 | **Labels:** `engProd`, `P2`, `Type: Task` | **Comments:** 1
- **Status:** **Active / Internal Google Infra**
- **Git & Codebase Findings:**
  - GitHub Actions `release.yml` automatically builds the Maven repo (`publishAllPublicationsToLocalDirRepository`) and uploads the `designcompose_m2repo` zip artifact.
  - Ralph commented on 2023-06-29: *"Pending internal review for Google backend components, moving out to 0.22."*
  - Staging to external gMaven / internal Kokoro requires Google internal pipeline credentials which cannot run directly in public GitHub Actions.
- **Recommendation:** **Keep Open (Google Internal KTLO)** or migrate tracking to internal Buganizer ticket.

---

### Issue #103: Allow for removal of Live Update code from production app
- **Author:** @timothyfroehlich | **Created:** 2023-06-01 | **Updated:** 2023-06-21 | **Labels:** `Type: New Feature`, `needs design`, `P3` | **Comments:** 0
- **Status:** **Active Architectural Backlog**
- **Git & Codebase Findings:**
  - Currently `designcompose` Android library includes Live Update classes (`LiveUpdateSettingsRepository`, `LiveUpdateExceptions`, etc.) bundled together.
  - JNI builds are consolidated via `BuildConfig` (commit `61b8ada7 Configure JNI library name via BuildConfig (#2500)`).
  - ProGuard/R8 minification strips unused code, and commit `6d309b4a Fix #435: Add assembleRelease to CI to catch minified build issues (#2425)` ensures release builds minify properly.
  - However, full modular separation into a separate `designcompose-liveupdate` artifact has not been done.
- **Recommendation:** **Keep Open** as a P3 enhancement. Combine tracking with #469.

---

### Issue #112: "Favorite docs" - Add method for setting alternate Figma Files to load in the Design Switcher
- **Author:** @timothyfroehlich | **Created:** 2023-06-06 | **Updated:** 2023-06-21 | **Labels:** `needs design`, `Type: Customer Issue`, `P3` | **Comments:** 1
- **Status:** **Closed / Resolved by Feature Guidance**
- **Git & Codebase Findings:**
  - On 2023-06-08, Ralph noted: *"Umesh notes that the existing feature to show branches allows easier navigation between docs in the Design Switcher, without needing to rebuild the app (so we can provide this guidance)."*
  - The Design Switcher now supports switching branches and custom Figma document IDs at runtime (`docs/_docs/live-update/design-switcher.md`).
- **Recommendation:** **Close as Resolved** (branches and custom file ID switching provided in Design Switcher).

---

### Issue #120: Re-make doc page for Custom Fonts
- **Author:** @timothyfroehlich | **Created:** 2023-06-13 | **Updated:** 2023-06-21 | **Labels:** `documentation`, `P3` | **Comments:** 0
- **Status:** **Active Documentation Task**
- **Git & Codebase Findings:**
  - Refers to internal bug `b/250922834`.
  - While `font-resources.png` exists in `docs/_docs/modifiers/`, there is no dedicated documentation page for Custom Fonts in `docs/`.
- **Recommendation:** **Keep Open** or author a dedicated `docs/_docs/custom-fonts.md` guide.

---

### Issue #121: Re-make doc page on Figma Interactions
- **Author:** @timothyfroehlich | **Created:** 2023-06-13 | **Updated:** 2023-06-21 | **Labels:** `documentation`, `P3` | **Comments:** 0
- **Status:** **Active Documentation Task**
- **Git & Codebase Findings:**
  - Refers to internal bug `b/250921446`.
  - Interactions are briefly touched on in `animations.md` and `modifiers.md` (Key Injection, TapCallback), but a standalone guide explaining Figma interaction mapping, variant change interactions, and sync plugin is missing.
- **Recommendation:** **Keep Open** (documentation backlog).

---

### Issue #131: Add documentation covering Dials & Gauges for developers
- **Author:** @timothyfroehlich | **Created:** 2023-06-22 | **Updated:** 2023-09-13 | **Labels:** `P2` | **Comments:** 2
- **Status:** **Fully Implemented / Documented**
- **Git & Codebase Findings:**
  - Comprehensive documentation exists in `docs/_docs/modifiers/modifiers.md` (lines 899–960):
    - Explains `Meter` customization and value binding (`progress: Float`).
    - Explains Draggable progress bars and `OnProgressChangedCallback` (added in PR #2182 `6355112d Support interactable progress bars and markers`).
- **Recommendation:** **Close as Completed**.

---

### Issue #132: Reorganize docs into a new set of categories
- **Author:** @timothyfroehlich | **Created:** 2023-06-22 | **Updated:** 2023-06-29 | **Labels:** `documentation`, `P2` | **Comments:** 2
- **Status:** **Stale / Obsolete**
- **Git & Codebase Findings:**
  - Assigned to @miriamvu in June 2023; no changes were made.
  - The documentation has since settled into its current Jekyll (`just-the-docs`) hierarchy: Tutorial, Live Update, Modifiers, Variables, Localization, Shaders, Working with Source.
- **Recommendation:** **Close as Stale / Not Planned**.

---

### Issue #136: Impossible to fetch node under intersection/union
- **Author:** @elouannf | **Created:** 2023-06-23 | **Updated:** 2024-05-08 | **Labels:** `needs design`, `P3` | **Comments:** 11
- **Status:** **Closed / Resolved (Workaround accepted by Author)**
- **Git & Codebase Findings:**
  - In Figma REST API, `BOOLEAN_OPERATION` nodes (Union, Intersection, Subtract) are rasterized/converted as single vector paths.
  - The author confirmed on 2023-08-14: *"I just found the problem for our masks. It's not a big deal because I was able to make it work in another way... it's not very important because I was able to make these changes anyway."*
  - No further activity has occurred in over 3 years.
- **Recommendation:** **Close as Resolved / Won't Fix (Figma API limitation; masks are supported workaround)**.

---

### Issue #177: Implement lightweight theming using Variables
- **Author:** @iamralpht | **Created:** 2023-07-12 | **Updated:** 2023-09-13 | **Labels:** `Type: New Feature`, `P1` | **Comments:** 0
- **Status:** **Fully Implemented & Documented**
- **Git & Codebase Findings:**
  - Fully implemented in PR #1055 (`86a5fb87 Closes #951: Initial support for variables and design tokens`).
  - Documented in PR #1165 (`a96dbc96 Closes #1145: Add documentation to variables support`).
  - Full guide available at `docs/_docs/variables.md` with Material Theme Builder integration, `DesignVariableCollection`, and `DesignVariableModeValues`.
- **Recommendation:** **Close as Completed** (PR #1055 & #1165).

---

### Issue #225: Live update stops working with frames containing large number of layers
- **Author:** @FruitSnack1 | **Created:** 2023-07-18 | **Updated:** 2024-05-29 | **Labels:** `Type: Customer Issue`, `P1` | **Comments:** 10
- **Status:** **Closed / Addressed by Optimizations**
- **Git & Codebase Findings:**
  - Author found the issue was caused by fetching hundreds of nested external library component instances.
  - Author confirmed on 2023-08-10: *"Without all the components and variants from different files, live update works normal and updates in matter of seconds... Just figured out a quick solution. In Figma you can detach all instances..."*
  - Addressed systemically in PR #2162 (`919c2016 Speed up fetch of components in other files`) and PR #2419 (`80df0009 Fix #281: Add performance logging for high-cost fetch operations`).
- **Recommendation:** **Close as Resolved / Completed**.

---

### Issue #250: Replace all references to Figma "documents" with "file" in the source code / API
- **Author:** @timothyfroehlich | **Created:** 2023-07-28 | **Updated:** 2023-08-09 | **Labels:** `P3` | **Comments:** 0
- **Status:** **Obsolete / Won't Fix**
- **Git & Codebase Findings:**
  - `@DesignDoc`, `DocServer`, `DocFetcher`, and `doc_id` are deeply ingrained public API and internal annotations across Kotlin and Rust.
  - Renaming this would cause massive breaking changes for all users and partners without functional benefit.
- **Recommendation:** **Close as Won't Fix (Not Planned)** to avoid breaking API churn.

---

### Issue #268: Add Fetcher support for figma token file
- **Author:** @timothyfroehlich | **Created:** 2023-08-01 | **Updated:** 2026-03-17 | **Labels:** `P1` | **Comments:** 0
- **Status:** **Fully Implemented**
- **Git & Codebase Findings:**
  - Implemented in PR #1594 (commit `41bba567 Modify the fetch tool to load the Figma token from command line, env var and .config file, in that order. (#1594)`).
  - `crates/dc_figma_import/src/tools/fetch.rs:89-100` (`load_figma_token()` loads token from `FIGMA_ACCESS_TOKEN` env var or `~/.config/figma_access_token`).
- **Recommendation:** **Close as Completed** (PR #1594).

---

### Issue #269: Manifest support for Fetcher
- **Author:** @timothyfroehlich | **Created:** 2023-08-01 | **Updated:** 2026-03-17 | **Labels:** `P1` | **Comments:** 0
- **Status:** **Superseded / Obsolete**
- **Git & Codebase Findings:**
  - Part of the abandoned August 2023 JSON manifest architecture before the project standardized on Protocol Buffers (`dc_bundle`).
  - The `fetch` tool handles document fetching directly into `.dcf` files.
- **Recommendation:** **Close as Not Planned / Superseded** by `dc_bundle` Protobuf architecture.

---

### Issue #270: Convert existing Kotlin JSON generation in Common and DesignCompose to use kotlinx.serialization
- **Author:** @timothyfroehlich | **Created:** 2023-08-01 | **Updated:** 2026-03-17 | **Labels:** `P1` | **Comments:** 0
- **Status:** **Obsolete**
- **Git & Codebase Findings:**
  - `DocumentServerParams` JSON generation was superseded when the serialization engine was migrated to Protocol Buffers (`dc_bundle` / `.dcf`).
  - No `kotlinx.serialization` manifest is needed or used.
- **Recommendation:** **Close as Obsolete / Not Planned**.

---

### Issue #271: Update Kotlin and Rust to new Manifest schema
- **Author:** @timothyfroehlich | **Created:** 2023-08-01 | **Updated:** 2026-04-09 | **Labels:** `P1` | **Comments:** 1
- **Status:** **Obsolete**
- **Git & Codebase Findings:**
  - On 2026-04-09, @anshu10sep commented: *"Tim, can you please add more details whats needed in here"* with no reply.
  - The entire manifest JSON schema initiative was replaced by Protobuf schema in `dc_bundle`.
- **Recommendation:** **Close as Obsolete / Not Planned**.

---

### Issue #272: Incremental update support for Fetcher
- **Author:** @timothyfroehlich | **Created:** 2023-08-01 | **Updated:** 2026-03-17 | **Labels:** `P2` | **Comments:** 0
- **Status:** **Fully Implemented**
- **Git & Codebase Findings:**
  - Implemented in `dc_figma_import`: `document.rs` checks version IDs and timestamps against existing `.dcf` files, and `image_context.rs` encodes/decodes image sessions to only fetch new/modified assets.
  - Confirmed in `crates/dc_figma_import/README.md`: *"It also supports incremental updates to avoid refetching assets the client already has."*
- **Recommendation:** **Close as Completed**.

---

### Issue #273: Create Gradle figmaFileFetch task, including bundling the Fetcher tool with the plugin
- **Author:** @timothyfroehlich | **Created:** 2023-08-01 | **Updated:** 2026-03-17 | **Labels:** `P1` | **Comments:** 0
- **Status:** **Superseded**
- **Git & Codebase Findings:**
  - Covered by `fetchFigmaFiles` in `InternalGradlePlugin` and the standalone `fetch` CLI released via GitHub releases (PR #2309).
- **Recommendation:** **Close as Completed / Superseded**.

---

### Issue #274: Connect the figmaFileFetch task to the output of the Codegen task, add DSL, update documentation
- **Author:** @timothyfroehlich | **Created:** 2023-08-01 | **Updated:** 2026-03-17 | **Labels:** `P1` | **Comments:** 0
- **Status:** **Obsolete / Superseded**
- **Git & Codebase Findings:**
  - Part of the old unexecuted Gradle DSL pipeline. Instead, `@DesignDoc` and Roborazzi test conventions fetch `.dcf` files directly.
- **Recommendation:** **Close as Not Planned**.

---

### Issue #275: Create figmaFileCommit and figmaFileFetch andCommit tasks, create developer script to update all serialized files in the repository
- **Author:** @timothyfroehlich | **Created:** 2023-08-01 | **Updated:** 2026-03-17 | **Labels:** `P1` | **Comments:** 0
- **Status:** **Fully Implemented**
- **Git & Codebase Findings:**
  - Implemented via `fetchAndUpdateFigmaFiles` task in `InternalGradlePlugin.kt` (lines 36-37, 73-76, 116-121) which fetches and copies `.dcf` files directly into `src/main/assets/figma`.
- **Recommendation:** **Close as Completed**.

---

### Issue #278: Document using DesignCompose to create a traditional Android View
- **Author:** @iamralpht | **Created:** 2023-08-02 | **Updated:** 2024-05-08 | **Labels:** `P2` | **Comments:** 0
- **Status:** **Fully Implemented & Documented**
- **Git & Codebase Findings:**
  - Implemented in PR #2254 (commit `4a14a501 Update SurfacView to use ComponentReplacementContext (#2254)`).
  - Full documentation and code example with `AndroidView` and `SurfaceView` added to `docs/_docs/modifiers/modifiers.md` under section: `## ComponentReplacementContext for Custom Android/Compose Views` (lines 417–457).
- **Recommendation:** **Close as Completed** (PR #2254).

---

### Issue #282: Consider making an allowlist for recursive doc fetches
- **Author:** @iamralpht | **Created:** 2023-08-09 | **Updated:** 2023-08-23 | **Labels:** `P2` | **Comments:** 0
- **Status:** **Closed / Superseded by Component Caching**
- **Git & Codebase Findings:**
  - Was an open discussion question from Ralph.
  - Resolved through caching: PR #2162 (`919c2016 Speed up fetch of components in other files`) caches already-retrieved components, and PR #2419 (`80df0009 Fix #281: Add performance logging for high-cost fetch operations`) adds performance warnings for expensive fetches.
- **Recommendation:** **Close as Resolved / Won't Fix (Allowlist unnecessary due to component caching)**.

---

### Issue #290: Cleanup rectangle / bounding box setup
- **Author:** @kportl | **Created:** 2023-08-15 | **Updated:** 2023-08-23 | **Labels:** `P2` | **Comments:** 0
- **Status:** **Fully Resolved (Code Removed)**
- **Git & Codebase Findings:**
  - Issue specifically asked to remove a legacy field in `crates/figma_import/src/toolkit_schema.rs#L126`.
  - `toolkit_schema.rs` has been completely deleted from the codebase.
  - The serialization types were fully migrated to Protobuf in `dc_bundle` (PRs #1459, #1519, #1521).
- **Recommendation:** **Close as Completed** (code refactored and removed in `dc_bundle` migration).

---

### Issue #395: Layout is broken on autolayout frames that are rotated
- **Author:** @rylin8 | **Created:** 2023-09-28 | **Updated:** 2023-11-01 | **Labels:** `P3`, `layout` | **Comments:** 0
- **Status:** **Partially Fixed / Verification Needed**
- **Git & Codebase Findings:**
  - Significant rotation fixes landed recently:
    - PR #2439 (`797e29e7 Fix #244: Rotated node margin calculation uses pre-rotation size (#2439)`)
    - PR #2440 (`86e1a8b6 Fix #1279: Squoosh rotated composable placement with placeRelativeWithLayer (#2440)`)
  - However, #395 specifically involves autolayout parent container frames with rotated children or rotated autolayout frames themselves.
- **Recommendation:** **Active / Needs Verification**: Check if PRs #2439 & #2440 resolved the test case in #395, and add a regression test.

---

### Issue #398: Text alignment with multiple lines is incorrect
- **Author:** @rylin8 | **Created:** 2023-09-28 | **Updated:** 2023-11-14 | **Labels:** `P2`, `layout` | **Comments:** 1
- **Status:** **Partially Addressed / Active**
- **Git & Codebase Findings:**
  - Ralph noted on 2023-11-14: *"This is fairly close (about as accurate as before) with the most recent round of text optimization changes. Moving to P2."*
  - Squoosh text rendering was rewritten in `SquooshText.kt`, and line height support was added in PR #2468 (`01e00454`).
- **Recommendation:** **Keep Open (P2)** pending a formal golden comparison for multiline auto-width alignment.

---

### Issue #469: Separate the JNI crate into Live Update and Layout
- **Author:** @timothyfroehlich | **Created:** 2023-10-16 | **Updated:** 2023-11-14 | **Labels:** `P2`, `layout` | **Comments:** 1
- **Status:** **Active Backlog**
- **Git & Codebase Findings:**
  - Layout is in `crates/dc_layout` while JNI bindings are in `crates/dc_jni` (which depends on `dc_layout` and `dc_figma_import`).
  - Complete decoupling of LiveUpdate from the final shared library `.so` has not been completed.
- **Recommendation:** **Keep Open (P2)** (track together with #103).

---

### Issue #551: DesignCompose does not download images larger than a certain size
- **Author:** @rylin8 | **Created:** 2023-11-13 | **Updated:** 2024-01-10 | **Labels:** `P2` | **Comments:** 1
- **Status:** **Active Enhancement**
- **Git & Codebase Findings:**
  - From internal bug `b/310338161`.
  - In `crates/dc_figma_import/src/image_context.rs`, images are fetched via `http_fetch_image`. If Figma fails or times out, a generic error is returned without explicitly diagnosing large image size limits.
  - Tim agreed: *"We should add an error."*
- **Recommendation:** **Active / Good First Issue**: Add explicit diagnostics or error logs when image size exceeds Figma limits.

---

### Issue #561: Performance optimization for Rust layout implementation
- **Author:** @iamralpht | **Created:** 2023-11-14 | **Updated:** 2024-01-02 | **Labels:** `P2`, `layout` | **Comments:** 0
- **Status:** **Completed / Superseded**
- **Git & Codebase Findings:**
  - Addressed by the massive rewrite to the Squoosh layout engine (`designcompose/src/main/java/com/android/designcompose/squoosh`), Taffy optimizations, list pagination benchmarks (PR #2320), and native binary size optimizations (PR #2458).
- **Recommendation:** **Close as Completed / Superseded** by Squoosh engine architecture.

---

### Issue #573: Make Brush function customization accept an `ImageContext`
- **Author:** @iamralpht | **Created:** 2023-11-21 | **Updated:** 2023-11-21 | **Labels:** `Type: New Feature`, `P2`, `java` | **Comments:** 0
- **Status:** **Active Feature Request**
- **Git & Codebase Findings:**
  - `ImageContext` exists in `CustomizationContext.kt:215`.
  - Currently `CustomizationContext.setBrushFunction` only accepts `() -> Brush` (`CustomizationContext.kt:273`).
  - Passing `(ImageContext) -> Brush` would allow dynamic shader / brush customizations to read designer colors and dimensions.
- **Recommendation:** **Active (P2 Feature Request)**: Straightforward enhancement to add `(ImageContext) -> Brush` overload.

---

### Issue #604: Add support for baseline alignment
- **Author:** @iamralpht | **Created:** 2023-12-11 | **Updated:** 2023-12-11 | **Labels:** `P2`, `layout` | **Comments:** 0
- **Status:** **Fully Fixed & Tested**
- **Git & Codebase Findings:**
  - Fixed in PR #2455 (commit `dd5caddb Fix LayoutAlignItems::Baseline mapping in Figma import (#2455)`).
  - Maps `LayoutAlignItems::Baseline` to `AlignItems::ALIGN_ITEMS_BASELINE` in `transform_flexbox.rs`, with unit test `test_layout_baseline`.
  - Released in `v0.39.2` (commit `b81b4bdf`).
- **Recommendation:** **Close as Completed** (PR #2455 / commit `dd5caddb`).

---

### Issue #606: Investigate transition to Kotlin 2.0 and KSP 2.0
- **Author:** @timothyfroehlich | **Created:** 2023-12-12 | **Updated:** 2026-03-17 | **Labels:** `P2` | **Comments:** 0
- **Status:** **Fully Completed**
- **Git & Codebase Findings:**
  - Upgraded to Kotlin 2.0 and Jetpack Compose Compiler in PR #2232 (commit `0103147c  feat: Upgrade to Kotlin 2.0 and Jetpack Compose Compiler 2.0 (#2232)`).
  - Repository is now running Kotlin `2.1.21` (`gradle/libs.versions.toml`).
  - KSP 2.0 transition is tracked separately in issue #2231 (`fix-issue-2231`).
- **Recommendation:** **Close as Completed** (PR #2232).

---

### Issue #628: Galaxy design crashes emulators on APIs greater than 33
- **Author:** @timothyfroehlich | **Created:** 2023-12-13 | **Updated:** 2026-03-17 | **Labels:** `P2` | **Comments:** 1
- **Status:** **Stale / Cannot Reproduce**
- **Git & Codebase Findings:**
  - On 2024-01-10, Tim commented: *"The Shiny design was most consistent in causing a crash. May have fixed, attempt to reproduce"*.
  - No reproduction steps or crashes have been reported since January 2024. All CI runs on API 34+ pass cleanly.
- **Recommendation:** **Close as Cannot Reproduce / Stale**.

---

### Issue #635: Allow `fetch` to get its nodes list from the JSON emitted by the KSP processor
- **Author:** @iamralpht | **Created:** 2023-12-14 | **Updated:** 2026-03-17 | **Labels:** `Type: New Feature`, `qol`, `engProd`, `P2` | **Comments:** 0
- **Status:** **Active Feature Request**
- **Git & Codebase Findings:**
  - `crates/dc_figma_import/src/tools/fetch.rs` currently only accepts manual node strings via `--nodes`.
  - The KSP processor in `codegen/` outputs component annotations and node queries.
  - Adding a `--nodes-file` / `--ksp-json` flag to `fetch` would significantly improve offline workflow for partners.
- **Recommendation:** **Active (P2 Feature Request)**.

---

### Issue #649: Provide a means for users to access the Validation docs
- **Author:** @timothyfroehlich | **Created:** 2023-12-19 | **Updated:** 2025-11-21 | **Labels:** `P3` | **Comments:** 3
- **Status:** **Active Community Request**
- **Git & Codebase Findings:**
  - External community members (@williamliuok and @superguoshiyu) actively commented in late 2025 asking for access to these reference designs.
  - Tim confirmed on 2024-06-03 that a public read-only Figma team exists for this.
- **Recommendation:** **Actionable for Maintainers**: Publish read-only Figma community links in documentation.

---

### Issue #661: Improve documentation on finding and using the generated JSON file for Lists & Grids
- **Author:** @iamralpht | **Created:** 2023-12-21 | **Updated:** 2024-01-03 | **Labels:** `Type: Customer Issue` | **Comments:** 0
- **Status:** **Active Documentation Task**
- **Git & Codebase Findings:**
  - `docs/_docs/modifiers/modifiers.md` (lines 621–750) describes the Auto Content Preview widget and annotations, but still lacks explicit instructions on where the generated JSON artifact is located in the build directory and how to upload it to Figma.
- **Recommendation:** **Active Docs Fix**: Add a short instructions callout in `modifiers.md`.

---

### Issue #673: Support scale constraints with vectors
- **Author:** @rylin8 | **Created:** 2023-12-28 | **Updated:** 2024-01-02 | **Labels:** `P3`, `layout` | **Comments:** 0
- **Status:** **Active Known Limitation**
- **Git & Codebase Findings:**
  - Scale constraints currently only scale bounding boxes/frames; vector path geometry remains unscaled for non-rectangular paths.
  - Noted by Robert Lin as having regressions when previously attempted.
- **Recommendation:** **Active (P3 Bug / Feature)**: Document as a known limitation or implement path matrix scaling in `transform_flexbox.rs`.

---

### Issue #684: Configure Firebase Test Lab, add performance testing
- **Author:** @timothyfroehlich | **Created:** 2024-01-02 | **Updated:** 2026-03-17 | **Labels:** `engProd`, `P2` | **Comments:** 0
- **Status:** **Active Backlog (Infra)**
- **Git & Codebase Findings:**
  - Post-submit performance benchmarking on FTL has not been configured in GitHub Actions workflows.
- **Recommendation:** **Keep Open (P2)** or migrate to internal Google tracking.

---

### Issue #688: Nightly Experimental builds failing
- **Author:** @timothyfroehlich | **Created:** 2024-01-03 | **Updated:** 2024-01-03 | **Labels:** None | **Comments:** 0
- **Status:** **Closed / Obsolete**
- **Git & Codebase Findings:**
  - The experimental nightly workflow referenced in this issue was removed/rebuilt during GitHub Actions overhauls (`.github/workflows/nightly.yml` was deleted).
  - Main workflow runs build, test, and release cleanly without disk exhaustion.
- **Recommendation:** **Close as Obsolete**.

---

### Issue #690: Improve handling of AAOS Unbundled Libraries in GitHub Actions
- **Author:** @timothyfroehlich | **Created:** 2024-01-03 | **Updated:** 2024-01-03 | **Labels:** None | **Comments:** 0
- **Status:** **Fully Implemented**
- **Git & Codebase Findings:**
  - In `.github/workflows/main.yml`, unbundled AAOS libraries were moved into their own build step ("Check out the Unbundled AAOS Repo" -> "Build the repo" -> "Upload maven repo: unbundled_m2repo").
  - Subsequent jobs ("Check MediaCompose") download the cached prebuilt `unbundled_m2repo` artifact rather than recompiling it each time.
- **Recommendation:** **Close as Completed**.

---
