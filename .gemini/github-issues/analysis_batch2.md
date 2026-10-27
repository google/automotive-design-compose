# GitHub Issues Analysis - Batch 2 (Issues #708 through #1368)

**Date of Analysis:** 2026-10-26  
**Scope:** 25 GitHub Issues (#708, #709, #710, #711, #712, #713, #739, #840, #883, #890, #905, #954, #1031, #1117, #1118, #1138, #1167, #1169, #1170, #1187, #1295, #1299, #1300, #1331, #1368)  
**Target Repository:** `/usr/local/google/home/priyanksingh/automotive-design-compose`

---

## Executive Summary & Category Breakdown

| Category | Description | Count | Issue Numbers |
| :--- | :--- | :---: | :--- |
| **Category 1** | **Stale / Awaiting Author Response / Answered**<br>Maintainer provided answer/workaround 3+ months ago without follow-up, or issue is stale after response. | **5** | #739, #840, #1167, #1170, #1187 |
| **Category 2** | **Already Fixed in Commits / Releases**<br>Resolved in repo via merged PRs and released versions. | **4** | #890, #905, #1368, and #739 (partially via DesignModules) |
| **Category 3** | **No Progress / Needs Author Relevance Check**<br>Old backlog or maintenance task with no activity/progress; verify if still relevant. | **8** | #708, #709, #710, #711, #712, #713, #883, #1118, #1295 |
| **Category 4** | **Actionable Bug / Enhancement Requiring Google Response or Code Fix**<br>Active bug or major missing feature in current codebase requiring code fixes or design. | **8** | #954, #1031, #1117, #1138, #1169, #1299, #1300, #1331 |

*(Note: #739 is primarily classified under Category 1 as answered/stale regarding inheritance, with its underlying code-reuse problem addressed in Category 2 via PR #906.)*

---

## Detailed Analysis of Issues

---

### Issue #708: Modify Tutorial instructions to run "setFigmaTokenDebug" from Android Studio instead of the command line

- **URL:** [https://github.com/google/automotive-design-compose/issues/708](https://github.com/google/automotive-design-compose/issues/708)
- **Author:** @timothyfroehlich
- **Created:** 2024-01-10T14:48:07Z
- **Updated:** 2024-01-10T14:52:28Z
- **Labels:** `top-qol`
- **Comments:** 0
- **Status in Codebase:** The tutorial documentation at [`docs/_docs/tutorial/index.md`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/tutorial/index.md#L77-L88) lines 77–88 still directs developers to run `./gradlew setFigmaTokenDebug` from the command line. No Gradle or Android Studio run configuration / instructions were added.
- **Classification:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Root Cause / Code Reference:**
  - File: [`docs/_docs/tutorial/index.md:77-88`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/tutorial/index.md#L77-L88)
- **Recommendation:** Ask author @timothyfroehlich if this quality-of-life tutorial simplification is still planned or can be closed in favor of current CLI docs.

---

### Issue #709: Enhance "SetFigmaToken" to target the actively running Android Device

- **URL:** [https://github.com/google/automotive-design-compose/issues/709](https://github.com/google/automotive-design-compose/issues/709)
- **Author:** @timothyfroehlich
- **Created:** 2024-01-10T14:49:49Z
- **Updated:** 2024-01-10T14:51:40Z
- **Labels:** `top-qol`
- **Comments:** 0
- **Status in Codebase:** In [`plugins/gradle-plugin/src/main/kotlin/com/android/designcompose/gradle/SetFigmaTokenTask.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/plugins/gradle-plugin/src/main/kotlin/com/android/designcompose/gradle/SetFigmaTokenTask.kt#L77-L88), the task still fails with `adb found multiple devices. Set '$ANDROID_SERIAL' environment variable to specify the target device` whenever more than one device/emulator is connected. It does not inspect Android Studio's active device injection properties (such as `-Pandroid.injected.invoked.from.ide=true` or device serial properties) or query active running emulators.
- **Classification:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Root Cause / Code Reference:**
  - File: [`plugins/gradle-plugin/src/main/kotlin/com/android/designcompose/gradle/SetFigmaTokenTask.kt:77-88`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/plugins/gradle-plugin/src/main/kotlin/com/android/designcompose/gradle/SetFigmaTokenTask.kt#L77-L88)
- **Recommendation:** Check if team still wants to enhance `SetFigmaTokenTask` with IDE active device detection.

---

### Issue #710: Feature idea: Automatically add Compose test tags to all DesignCompose customizations

- **URL:** [https://github.com/google/automotive-design-compose/issues/710](https://github.com/google/automotive-design-compose/issues/710)
- **Author:** @timothyfroehlich
- **Created:** 2024-01-10T16:04:37Z
- **Updated:** 2026-03-17T16:36:11Z
- **Labels:** `top-qol`
- **Comments:** 0
- **Status in Codebase:** In Squoosh, [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt#L766) line 766 only attaches `Modifier.testTag(child.node.view.name)` to composable replacement children. Plain rendered nodes with text, tap callbacks, meters, or visibility customizations are rendered via canvas drawing operations and do not produce individual Compose semantics nodes with test tags. As noted in [`docs/_docs/working-with-source/writing-tests.md:170`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/working-with-source/writing-tests.md#L170), developers must manually wrap nodes with replacement components to attach `Modifier.testTag()`.
- **Classification:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Root Cause / Code Reference:**
  - File: [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt:766`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt#L766)
  - File: [`docs/_docs/working-with-source/writing-tests.md:165-193`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/working-with-source/writing-tests.md#L165-L193)
- **Recommendation:** Ask author if automatic Compose test tag injection on all customizations is still desired or if current Squoosh replacement component testing is the official testing architecture.

---

### Issue #711: Test library - Create examples of various tests

- **URL:** [https://github.com/google/automotive-design-compose/issues/711](https://github.com/google/automotive-design-compose/issues/711)
- **Author:** @timothyfroehlich
- **Created:** 2024-01-10T16:35:15Z
- **Updated:** 2024-01-10T16:35:16Z
- **Labels:** *None*
- **Comments:** 0
- **Status in Codebase:** The repo has extensive internal validation examples in `integration-tests/validation`, but no standalone test library or user-facing sample module demonstrating testing for dials/gauges, variant assertions, or doc display was created for external consumers.
- **Classification:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Root Cause / Code Reference:**
  - File: [`docs/_docs/working-with-source/writing-tests.md`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/working-with-source/writing-tests.md)
- **Recommendation:** Check if a public test-recipe module is still desired on the roadmap or if existing validation tests suffice.

---

### Issue #712: Documentation - Document compile-time optimizations

- **URL:** [https://github.com/google/automotive-design-compose/issues/712](https://github.com/google/automotive-design-compose/issues/712)
- **Author:** @timothyfroehlich
- **Created:** 2024-01-10T16:45:19Z
- **Updated:** 2024-01-10T16:46:15Z
- **Labels:** `documentation`
- **Comments:** 0
- **Status in Codebase:** The documentation in `docs/` does not contain a dedicated guide explaining how to separate DesignDoc Interface declarations into their own files to optimize build times and KSP incremental compilation (building off #452).
- **Classification:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Root Cause / Code Reference:**
  - Path: [`docs/_docs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs)
- **Recommendation:** Check with author whether this documentation task is still planned or can be marked backlog/closed.

---

### Issue #713: Tutorial - consider alternative instructions for loading your Figma Doc ID

- **URL:** [https://github.com/google/automotive-design-compose/issues/713](https://github.com/google/automotive-design-compose/issues/713)
- **Author:** @timothyfroehlich
- **Created:** 2024-01-10T17:50:14Z
- **Updated:** 2024-01-10T17:50:30Z
- **Labels:** `documentation`, `tutorial`
- **Comments:** 0
- **Status in Codebase:** In [`reference-apps/tutorial/app/src/main/java/com/android/designcompose/tutorial/MainActivity.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/reference-apps/tutorial/app/src/main/java/com/android/designcompose/tutorial/MainActivity.kt#L83) line 83, the hardcoded document ID `3z4xExq0INrL9vxPhj9tl7` is still present, and [`docs/_docs/tutorial/index.md:89-103`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/tutorial/index.md#L89-L103) still tells users to use the in-app Design Switcher UI.
- **Classification:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Root Cause / Code Reference:**
  - File: [`reference-apps/tutorial/app/src/main/java/com/android/designcompose/tutorial/MainActivity.kt:83`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/reference-apps/tutorial/app/src/main/java/com/android/designcompose/tutorial/MainActivity.kt#L83)
  - File: [`docs/_docs/tutorial/index.md:89-103`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/tutorial/index.md#L89-L103)
- **Recommendation:** Check with author if changing `MainActivity.kt` directly remains preferred or if the Design Switcher instructions remain standard.

---

### Issue #739: Design Component must be within the main document Figma interface

- **URL:** [https://github.com/google/automotive-design-compose/issues/739](https://github.com/google/automotive-design-compose/issues/739)
- **Author:** @Heneveld
- **Created:** 2024-01-23T13:30:23Z
- **Updated:** 2024-06-07T17:48:18Z
- **Labels:** `Type: Customer Issue`
- **Assignee:** @rylin8
- **Comments:** 2
- **Status in Codebase:** The author requested interface inheritance for code reuse. Maintainer @rylin8 investigated on branch `wb/rylin/interface-inheritance` (commit `a6476307`), but concluded that interface inheritance was discouraged. Instead, DesignCompose implemented **DesignModules** (`@DesignModuleClass` and `@DesignModule`) in PR #906 (commit `c6635350`, release `v0.26.0`) and PR #1010 (commit `6fb43a97`), documented in [`docs/_docs/modules.md`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/modules.md). Interface inheritance remains explicitly disallowed with a compiler error in [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt:204-210`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt#L204-L210) (`"Extending a generated interface $overrideInterface not supported"`). In Comment #2 (2024-03-15), author Jonathan agreed that declaring separate interfaces/modules would satisfy their requirements. No comments or activity occurred since March 2024 (>2.5 years).
- **Classification:** **Category 1 (Stale / Awaiting Author Response / Answered)** (also resolved via DesignModules in **Category 2** PR #906)
- **Root Cause / Code Reference:**
  - File: [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt:204-210`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt#L204-L210)
  - Doc: [`docs/_docs/modules.md`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/modules.md)
- **Recommendation:** Close as resolved via DesignModules (`@DesignModuleClass` / `@DesignModule`), pointing author to `docs/_docs/modules.md`.

---

### Issue #840: Variant prototype not working when the elements come from a library

- **URL:** [https://github.com/google/automotive-design-compose/issues/840](https://github.com/google/automotive-design-compose/issues/840)
- **Author:** @MarcFerrerVWGS
- **Created:** 2024-03-04T08:26:33Z
- **Updated:** 2024-06-07T17:48:19Z
- **Labels:** `Type: Customer Issue`
- **Comments:** 12
- **Status in Codebase:** In Comment #11 (2024-03-20), maintainer @rylin8 explained the root cause: the library component defined 4 variant properties (`category`, `configuration`, `state`, and `emphasis_level`), but the code only declared `@DesignVariant` for `state`, making variant selection ambiguous. The author acknowledged in Comment #12 (2024-03-21) and reported network fetch issues on their emulator proxy. Furthermore, unreachable remote library nodes were addressed in commit `d2683b96` and remote variable alias mappings in PR #2507 (commit `953bf979`). The author never followed up regarding variant parameter definitions.
- **Classification:** **Category 1 (Stale / Awaiting Author Response / Answered)**
- **Root Cause / Code Reference:**
  - File: [`crates/dc_figma_import/src/document.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/document.rs) (remote library fetch improvements)
  - File: [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshVariantTransition.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshVariantTransition.kt)
- **Recommendation:** Mark as Stale / Answered; request verification from author if external library component fetching and variant mapping is still an issue on latest `0.39.x`.

---

### Issue #883: Consolidate JNI Library artifacts into a single Gradle module

- **URL:** [https://github.com/google/automotive-design-compose/issues/883](https://github.com/google/automotive-design-compose/issues/883)
- **Author:** @timothyfroehlich
- **Created:** 2024-03-19T18:40:40Z
- **Updated:** 2024-03-19T18:40:41Z
- **Labels:** *None*
- **Comments:** 0
- **Status in Codebase:** The JNI build artifacts remain split: [`designcompose/build.gradle.kts`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/build.gradle.kts#L100) builds Android targets, while [`test/test-native/build.gradle.kts`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/test/test-native/build.gradle.kts#L38-L50) builds host targets for Robolectric tests. They have not been consolidated into a single KMP module.
- **Classification:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Root Cause / Code Reference:**
  - File: [`designcompose/build.gradle.kts:100`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/build.gradle.kts#L100)
  - File: [`test/test-native/build.gradle.kts:38-50`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/test/test-native/build.gradle.kts#L38-L50)
- **Recommendation:** Check if team plans to proceed with a standalone KMP JNI module or maintain the current two-module structure.

---

### Issue #890: Use version catalog for exoplayer test dependency

- **URL:** [https://github.com/google/automotive-design-compose/issues/890](https://github.com/google/automotive-design-compose/issues/890)
- **Author:** @iamralpht
- **Created:** 2024-03-21T22:44:23Z
- **Updated:** 2024-03-22T15:29:02Z
- **Labels:** *None*
- **Comments:** 1
- **Status in Codebase:** **ALREADY FIXED!** In PR #1566 (commit `20cdf7a335ea8d245a38aa514bf2bc471e6a1984`) by Tim Froehlich, titled *"Upgrade androidx.media3 to 1.4.1 (#1566)"*, all hardcoded exoplayer/media3 dependencies in [`integration-tests/validation/build.gradle.kts`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/integration-tests/validation/build.gradle.kts#L87-L89) were replaced with version catalog entries `libs.androidx.media3.exoplayer`, `libs.androidx.media3.exoplayer.dash`, and `libs.androidx.media3.ui`, defined in [`gradle/libs.versions.toml`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/gradle/libs.versions.toml#L108-L110).
- **Commit / PR / Release Details:**
  - Commit: `20cdf7a335ea8d245a38aa514bf2bc471e6a1984`
  - PR: **#1566**
  - First Release Tag: **`v0.30.0`** (and `v0.30.0-rc01`)
- **Classification:** **Category 2 (Already Fixed in Commits / Releases)**
- **Recommendation:** Close issue as completed by PR #1566 in release v0.30.0.

---

### Issue #905: Masks inside of variants do not render correctly

- **URL:** [https://github.com/google/automotive-design-compose/issues/905](https://github.com/google/automotive-design-compose/issues/905)
- **Author:** @HooHaa-code
- **Created:** 2024-03-29T19:45:01Z
- **Updated:** 2024-06-07T17:48:20Z
- **Labels:** `Type: Customer Issue`
- **Comments:** 6
- **Status in Codebase:** **ALREADY FIXED!** The issue occurred in the legacy (non-Squoosh) rendering path (`DesignFrame.kt`). In Comment #6, maintainers verified that Squoosh rendered masks in variants correctly. In PR #1883 (commit `71d6aeec6a25b16386ad1db7daca4e26b12938d2`, titled *"Remove non-squoosh code path completely (#1883)"*), the entire non-Squoosh legacy rendering path was permanently removed, making Squoosh the sole rendering pipeline across DesignCompose.
- **Commit / PR / Release Details:**
  - Commit: `71d6aeec6a25b16386ad1db7daca4e26b12938d2`
  - PR: **#1883** (Fixes #1882)
  - First Release Tag: **`v0.33.0`** (and `v0.33.0-rc01`)
- **Classification:** **Category 2 (Already Fixed in Commits / Releases)**
- **Recommendation:** Close issue as resolved in v0.33.0 via PR #1883 where Squoosh became the sole renderer.

---

### Issue #954: Enable DesignModule usage across Gradle projects

- **URL:** [https://github.com/google/automotive-design-compose/issues/954](https://github.com/google/automotive-design-compose/issues/954)
- **Author:** @timothyfroehlich
- **Created:** 2024-04-03T16:19:39Z
- **Updated:** 2024-06-20T20:39:00Z
- **Labels:** `Type: New Feature`
- **Comments:** 0
- **Status in Codebase:** In [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt#L134-L149), `processDesignModulesClasses` relies on KSP `resolver.getSymbolsWithAnnotation` over the current module's source symbols. When `@DesignModuleClass` classes are declared in a separate Gradle project or library, their metadata and customization mappings are not exported into a shared manifest or KSP resource that downstream Gradle modules can read. Consequently, modules cannot be shared across Gradle project boundaries or published to Maven.
- **Classification:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)**
- **Root Cause / Code Reference:**
  - File: [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt:134-149`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt#L134-L149)
  - File: [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt:78-79`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt#L78-L79)
- **Recommendation:** Requires KSP metadata serialization (e.g. generating a JSON or class-level metadata annotation in `META-INF` of the library module) and reading it via `resolver.getDeclarationsFromPackage` or classloader resources.

---

### Issue #1031: Vectors in Serialized Documents Don't Provide all Colors

- **URL:** [https://github.com/google/automotive-design-compose/issues/1031](https://github.com/google/automotive-design-compose/issues/1031)
- **Author:** @johnoneil
- **Created:** 2024-04-23T04:27:36Z
- **Updated:** 2024-04-26T15:47:28Z
- **Labels:** *None*
- **Comments:** 2
- **Status in Codebase:** **STILL EXISTS.** In Figma, vector networks can contain multiple paths with mixed fills using `fillOverrideTable` on the node and `overrideID` on individual `fillGeometry` path objects. In [`crates/dc_figma_import/src/figma_schema.rs:305-310`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/figma_schema.rs#L305-L310), `Path` only deserializes `path` and `winding_rule`, omitting `override_id`. In [`crates/dc_figma_import/src/transform_flexbox.rs:1905-1910`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/transform_flexbox.rs#L1905-L1910), all paths in `fill_geometry` are converted into an unstyled list of `fill_paths`, and the entire node is assigned a single uniform background style from `node.fills`. When rendered, all sub-paths draw with only the node's primary color (e.g. white instead of white + green).
- **Classification:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)**
- **Root Cause / Code Reference:**
  - File: [`crates/dc_figma_import/src/figma_schema.rs:305-310`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/figma_schema.rs#L305-L310) (missing `override_id: Option<usize>` on `Path`, and missing `fill_override_table` on `Node`)
  - File: [`crates/dc_figma_import/src/transform_flexbox.rs:1904-1910`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/transform_flexbox.rs#L1904-L1910) (drops override IDs and applies uniform `view_style.background`)
  - File: [`crates/dc_bundle/src/definition/view.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_bundle/src/definition/view.rs) (requires per-path style representation or sub-node splitting)
- **Recommendation:** Implement `fillOverrideTable` and `overrideID` deserialization in `dc_figma_import` and support distinct path fills in `dc_bundle` and `FrameRender.kt`.

---

### Issue #1117: ImageContext does not provide useful data when node constraints set to scale

- **URL:** [https://github.com/google/automotive-design-compose/issues/1117](https://github.com/google/automotive-design-compose/issues/1117)
- **Author:** @rylin8
- **Created:** 2024-05-15T17:15:57Z
- **Updated:** 2024-05-15T17:16:14Z
- **Labels:** `:green_circle: P2`
- **Comments:** 0
- **Status in Codebase:** **STILL EXISTS.** In [`designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt:215-252`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt#L215-L252), `ImageContext` is instantiated with static Figma dimension definitions: `minWidth`, `maxWidth`, `width`, `minHeight`, `maxHeight`, `height`. When a node's constraints are set to scale or percent in Figma, `width.hasPoints()` and `height.hasPoints()` return `false`, causing `getPixelWidth()` and `getPixelHeight()` to return `null`. Meanwhile, in [`designcompose/src/main/java/com/android/designcompose/FrameRender.kt:643-656`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/FrameRender.kt#L643-L656), the actual measured/rendered layout dimensions (`size.width` and `size.height`, or `node.computedLayout`) are already known at draw time, but are not passed into `ImageContext`.
- **Classification:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)**
- **Root Cause / Code Reference:**
  - File: [`designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt:215-252`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt#L215-L252) (`ImageContext` lacks actual pixel dimensions `actualWidth: Float?`, `actualHeight: Float?`)
  - File: [`designcompose/src/main/java/com/android/designcompose/FrameRender.kt:644-654`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/FrameRender.kt#L644-L654) (passes only unresolved `node.style.layoutStyle.*` protos)
- **Recommendation:** Add `actualWidth: Float` and `actualHeight: Float` to `ImageContext` and populate them in `FrameRender.kt` using `size.width` / `size.height` (and in `SquooshRoot.kt`).

---

### Issue #1118: Provide additional test cases for external renderer comparison

- **URL:** [https://github.com/google/automotive-design-compose/issues/1118](https://github.com/google/automotive-design-compose/issues/1118)
- **Author:** @johnoneil
- **Created:** 2024-05-15T19:46:46Z
- **Updated:** 2024-05-15T19:46:46Z
- **Labels:** *None*
- **Comments:** 0
- **Status in Codebase:** The author created branch `origin/wb/johnoneil/rendering-parity-static-images` with commit `ec98de46` to generate additional static test cases (`StaticDocument.kt`), but the branch was abandoned and never merged into `main`.
- **Classification:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Root Cause / Code Reference:**
  - Branch: `origin/wb/johnoneil/rendering-parity-static-images` (commit `ec98de46`)
- **Recommendation:** Inquire with author if external renderer parity test images are still required or if this issue should be closed.

---

### Issue #1138: Custom Design Modules must be in same directory as Generated Composable

- **URL:** [https://github.com/google/automotive-design-compose/issues/1138](https://github.com/google/automotive-design-compose/issues/1138)
- **Author:** @Heneveld
- **Created:** 2024-05-21T14:00:27Z
- **Updated:** 2024-06-07T17:47:55Z
- **Labels:** `Type: Customer Issue`
- **Comments:** 1
- **Status in Codebase:** **STILL EXISTS.** In [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt:185`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt#L185), the generated extension function `fun $className.customizations(): CustomizationContext` is placed in the package of the `@DesignModuleClass`. In [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt:799`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt#L799), the generated doc composable invokes `customizations.mergeFrom($value.customizations())` without importing `$modulePackage.customizations`. When the module class resides in a different package from the `@DesignDoc` interface, the Kotlin compiler fails with `Unresolved reference: customizations`.
- **Classification:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)**
- **Root Cause / Code Reference:**
  - File: [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt:799`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt#L799)
  - File: [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt:166-185`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt#L166-L185)
- **Recommendation:** In `BuilderProcessor.kt`, collect all referenced module class package names and emit `import <modulePackage>.customizations` in the generated file header, or generate fully qualified extension calls.

---

### Issue #1167: How to scale the Figma objects in the emulator based on the screen DPI

- **URL:** [https://github.com/google/automotive-design-compose/issues/1167](https://github.com/google/automotive-design-compose/issues/1167)
- **Author:** @spkdroid
- **Created:** 2024-05-28T15:16:48Z
- **Updated:** 2024-06-07T17:47:56Z
- **Labels:** `Type: Customer Issue`
- **Comments:** 1
- **Status in Codebase:** In Comment #1 (2024-05-29), maintainer @iamralpht fully answered the question, explaining that DesignCompose already scales objects based on DPI automatically, and if extra programmatic scaling is desired, standard Compose `Modifier.scale()` can be applied to the composable. The author never replied or followed up for over 2 years.
- **Classification:** **Category 1 (Stale / Awaiting Author Response / Answered)**
- **Recommendation:** Close as Answered / Stale.

---

### Issue #1169: Support duplicate variable names across multiple documents

- **URL:** [https://github.com/google/automotive-design-compose/issues/1169](https://github.com/google/automotive-design-compose/issues/1169)
- **Author:** @rylin8
- **Created:** 2024-05-29T18:46:29Z
- **Updated:** 2024-05-29T18:46:29Z
- **Labels:** *None*
- **Comments:** 0
- **Status in Codebase:** **STILL EXISTS.** In [`designcompose/src/main/java/com/android/designcompose/VariableManager.kt:166-193`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/VariableManager.kt#L166-L193), `VariableManager` maintains a single global `varMap: VariableMap` across all loaded documents. Lines 166–167 contain the explicit comment: `// A global variable map containing entries from all documents. We currently don't support duplicate variable names across multiple documents.` When multiple documents contain variables or collections with the same name, `this.collectionIdsByName.putAll(...)` and `this.variablesById.putAll(...)` overwrite previous entries, leading to incorrect variable resolution when multiple Figma documents are active in the same application.
- **Classification:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)**
- **Root Cause / Code Reference:**
  - File: [`designcompose/src/main/java/com/android/designcompose/VariableManager.kt:163-195`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/VariableManager.kt#L163-L195)
- **Recommendation:** Scope variable resolution by `docId` in `VariableManager.kt` so each document resolves its own variable IDs and names from `docVarMap[docId.id]`.

---

### Issue #1170: Scaling two separates instances of a component will always take the original size

- **URL:** [https://github.com/google/automotive-design-compose/issues/1170](https://github.com/google/automotive-design-compose/issues/1170)
- **Author:** @Heneveld
- **Created:** 2024-05-29T19:22:22Z
- **Updated:** 2024-06-07T17:47:57Z
- **Labels:** `Type: Customer Issue`
- **Comments:** 1
- **Status in Codebase:** In Comment #1 (2024-06-06), maintainer @rylin8 replied that vector scaling is tracked in #673 and provided a concrete workaround (using frames with large rounded corners instead of vector ellipse shapes). The author never replied for over 2 years.
- **Classification:** **Category 1 (Stale / Awaiting Author Response / Answered)**
- **Recommendation:** Close as duplicate of #673 / answered with workaround.

---

### Issue #1187: Fill with image not showing on the emulator

- **URL:** [https://github.com/google/automotive-design-compose/issues/1187](https://github.com/google/automotive-design-compose/issues/1187)
- **Author:** @MarcFerrerVWGS
- **Created:** 2024-06-03T06:25:44Z
- **Updated:** 2024-06-07T17:47:57Z
- **Labels:** `Type: Customer Issue`
- **Comments:** 6
- **Status in Codebase:** In Comment #5 (2024-06-05), maintainer @rylin8 tested the user's uploaded Figma file and verified that the image loaded and rendered properly on the emulator, providing screenshots. In Comment #6 (2024-06-06), the author confirmed that their device logs showed `Decoded 1 images for test_images` and `Save doc success`, but suspected their emulator network proxy was causing issues. The author never followed up after Comment #6 (>2 years ago).
- **Classification:** **Category 1 (Stale / Awaiting Author Response / Answered)**
- **Recommendation:** Close as stale / resolved on maintainer's side.

---

### Issue #1295: Localization plugin UI polish

- **URL:** [https://github.com/google/automotive-design-compose/issues/1295](https://github.com/google/automotive-design-compose/issues/1295)
- **Author:** @yiqunw700
- **Created:** 2024-06-24T20:25:06Z
- **Updated:** 2024-07-15T18:53:59Z
- **Labels:** `plugin`, `i18n`
- **Comments:** 1
- **Status in Codebase:** A follow-up ticket to #1242 and PR #1320 regarding Figma plugin UI button labeling ("Submit" vs "Description"/"Char Limit") in [`support-figma/extended-layout-plugin/src/localization-module.ts`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/support-figma/extended-layout-plugin/src/localization-module.ts). No UX design or follow-up was provided since July 2024.
- **Classification:** **Category 3 (No Progress / Needs Author Relevance Check)**
- **Root Cause / Code Reference:**
  - File: [`support-figma/extended-layout-plugin/src/localization-module.ts`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/support-figma/extended-layout-plugin/src/localization-module.ts)
- **Recommendation:** Ask author @yiqunw700 if UX input was received or if this minor UI polish ticket is still needed.

---

### Issue #1299: Control Figma UI prototype state in code

- **URL:** [https://github.com/google/automotive-design-compose/issues/1299](https://github.com/google/automotive-design-compose/issues/1299)
- **Author:** @miguel-galarza
- **Created:** 2024-06-27T09:36:29Z
- **Updated:** 2024-10-22T08:18:25Z
- **Labels:** *None*
- **Comments:** 1 (+1 support from @BIN-KOUHINN)
- **Status in Codebase:** **STILL EXISTS.** In [`designcompose/src/main/java/com/android/designcompose/InteractionState.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/InteractionState.kt#L59-L100), prototype reactions (`ChangeTo`, `NavigateBack`, `OpenOverlay`, `SwapNavigation`) are managed via internal classes (`InteractionState`, `DeferredAction`, `InteractionStateManager`). There is NO public API for Android host code to:
  1. Observe the current visible screen / prototype navigation state.
  2. Programmatically navigate or set a prototype state (e.g. triggered by hardware steering wheel buttons, rotary knobs, or application deep links).
- **Classification:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)**
- **Root Cause / Code Reference:**
  - File: [`designcompose/src/main/java/com/android/designcompose/InteractionState.kt:59-150`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/InteractionState.kt#L59-L150)
  - File: [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshInteraction.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshInteraction.kt)
- **Recommendation:** Design a public API on `InteractionState` / `InteractionStateManager` (e.g. `currentDestination: State<String>`, `navigateTo(nodeQuery: NodeQuery)`) to expose and control prototype state from Kotlin.

---

### Issue #1300: Drag-Drop implementation

- **URL:** [https://github.com/google/automotive-design-compose/issues/1300](https://github.com/google/automotive-design-compose/issues/1300)
- **Author:** @miguel-galarza
- **Created:** 2024-06-27T09:59:54Z
- **Updated:** 2024-09-10T06:19:55Z
- **Labels:** *None*
- **Comments:** 2
- **Status in Codebase:** In Comment #1, @iamralpht asked for clarification on whether drag-and-drop was needed within a list or between lists; author replied in Comment #2 (2024-09-10) that it is needed *"between two lists located in the same screen"*. DesignCompose currently supports scroll containers and swipe meters, but has no API or gesture support for cross-list drag-and-drop in Squoosh list widgets.
- **Classification:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)** (needs design/implementation)
- **Root Cause / Code Reference:**
  - File: [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshListWidget.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshListWidget.kt)
- **Recommendation:** Review requirements for cross-list drag-and-drop or confirm if author still needs this feature.

---

### Issue #1331: DesignVariant not working with figma component instances

- **URL:** [https://github.com/google/automotive-design-compose/issues/1331](https://github.com/google/automotive-design-compose/issues/1331)
- **Author:** @miguel-galarza
- **Created:** 2024-07-08T08:16:33Z
- **Updated:** 2025-10-31T13:06:31Z
- **Labels:** *None*
- **Assignee:** @rylin8
- **Comments:** 11
- **Status in Codebase:** **STILL EXISTS.** In Figma, multiple instances of a component set share the same underlying component definition and target IDs. In [`designcompose/src/main/java/com/android/designcompose/InteractionState.kt:41-45`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/InteractionState.kt#L41-L45) (`getInstanceIdWithKey`), unless a manual `key` is supplied, interactions like `whilePressed` apply across all instances. Furthermore, when switching variants via `@DesignVariant`, node replacement drops node customizations (`TapCallback` and text customizations) because customizations bound to `#button1` are not rebound to the internal nodes of the newly active variant. Stephan Schuster confirmed on 2025-10-29 and 2025-10-31 that `TapCallback` still fails to trigger in `v0.37.2+`.
- **Classification:** **Category 4 (Actionable Bug / Enhancement Requiring Google Response or Code Fix)**
- **Root Cause / Code Reference:**
  - File: [`designcompose/src/main/java/com/android/designcompose/InteractionState.kt:41-45, 91-100`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/InteractionState.kt#L41-L45)
  - File: [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshTreeBuilder.kt:70-130`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshTreeBuilder.kt#L70-L130)
- **Recommendation:** Fix variant replacement in `SquooshTreeBuilder.kt` to preserve customizations across variant transitions, and auto-assign unique instance keys based on parent layout hierarchy.

---

### Issue #1368: More flexibility possible? (RRO raw resource loading & dynamic docId)

- **URL:** [https://github.com/google/automotive-design-compose/issues/1368](https://github.com/google/automotive-design-compose/issues/1368)
- **Author:** @StephanSchuster
- **Created:** 2024-07-12T15:14:49Z
- **Updated:** 2024-11-28T08:34:31Z
- **Labels:** *None*
- **Comments:** 12
- **Status in Codebase:** **ALREADY FIXED & RESOLVED!**
  1. **Loading offline docs from `res/raw` for RRO:** Added in PR #1778 (commit `635ca57b8c6d9495833a4fc28264bb0b0f5ef4fe`, release **v0.32.0**), adding `DesignSettings.setRawResourceId()` to [`designcompose/src/main/java/com/android/designcompose/DocServer.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/DocServer.kt#L160-L175). It was further expanded in PR #1807 (commit `e77b42bc821ca47fc1d9a3af47bb1e7ee893a5ce`, release **v0.33.0**) to associate raw resources with specific document IDs (`setRawResourceId(docId, resId)`).
  2. **Dynamic docId:** Addressed via `DesignDocOverride()`.
  3. **RRO Dynamic Cache Invalidation:** The maintainer explicitly filed issue **#1814** to track dynamic cache invalidation for runtime RRO switching.
- **Commit / PR / Release Details:**
  - Commit: `635ca57b8c6d9495833a4fc28264bb0b0f5ef4fe` (PR **#1778**, Release **`v0.32.0`**)
  - Commit: `e77b42bc821ca47fc1d9a3af47bb1e7ee893a5ce` (PR **#1807**, Release **`v0.33.0`**)
  - Follow-up ticket: **#1814**
- **Classification:** **Category 2 (Already Fixed in Commits / Releases)**
- **Recommendation:** Close issue as completed by PR #1778 and PR #1807, with further dynamic cache invalidation tracked in #1814.

---

## Batch 2 Summary Matrix

| Issue # | Title | Category | Current Status | Primary Files & Root Cause / Fix Details |
| :---: | :--- | :---: | :---: | :--- |
| **#708** | Modify Tutorial instructions for setFigmaTokenDebug | **Cat 3** | Backlog | `docs/_docs/tutorial/index.md:77-88` (still directs users to CLI) |
| **#709** | Enhance SetFigmaToken to target active device | **Cat 3** | Backlog | `SetFigmaTokenTask.kt:77-88` (requires manual `$ANDROID_SERIAL`) |
| **#710** | Auto-add Compose test tags to customizations | **Cat 3** | Backlog | `SquooshRoot.kt:766`, `writing-tests.md:170` (only replacement components get tags) |
| **#711** | Test library - Create examples of various tests | **Cat 3** | Backlog | `writing-tests.md` (no sample library for gauges/variants created) |
| **#712** | Document compile-time optimizations | **Cat 3** | Backlog | `docs/_docs` (docs on splitting interface files not written) |
| **#713** | Tutorial - alternative doc ID loading instructions | **Cat 3** | Backlog | `tutorial/MainActivity.kt:83`, `tutorial/index.md:89` (uses Design Switcher) |
| **#739** | Design Component within main Figma interface | **Cat 1** | Answered/Stale | Answered by rylin8; resolved via DesignModules (`@DesignModuleClass` in PR #906, v0.26.0) |
| **#840** | Variant prototype not working from library | **Cat 1** | Answered/Stale | Answered by rylin8 (unspecified variant properties); remote fetch improved in PR #2507 |
| **#883** | Consolidate JNI Library into single module | **Cat 3** | Backlog | `designcompose/build.gradle.kts` and `test/test-native/build.gradle.kts` still separate |
| **#890** | Version catalog for exoplayer test dependency | **Cat 2** | **Fixed** | Fixed by PR **#1566** (`20cdf7a3`), Release **`v0.30.0`** |
| **#905** | Masks inside of variants do not render correctly | **Cat 2** | **Fixed** | Fixed by PR **#1883** (`71d6aeec`), Release **`v0.33.0`** (Squoosh sole renderer) |
| **#954** | DesignModule usage across Gradle projects | **Cat 4** | **Active Bug** | `BuildModuleClass.kt:134-149` (KSP does not export/import module metadata across projects) |
| **#1031** | Vectors don't provide all colors (mixed fills) | **Cat 4** | **Active Bug** | `figma_schema.rs:305-310`, `transform_flexbox.rs:1905` (lacks `fillOverrideTable`/`overrideID`) |
| **#1117** | ImageContext lacks size on scale constraints | **Cat 4** | **Active Bug** | `CustomizationContext.kt:215-252`, `FrameRender.kt:644-654` (unresolved layout dimensions) |
| **#1118** | Additional test cases for renderer comparison | **Cat 3** | Abandoned | Branch `wb/johnoneil/rendering-parity-static-images` (`ec98de46`) never merged |
| **#1138** | Custom Modules must be in same directory | **Cat 4** | **Active Bug** | `BuilderProcessor.kt:799`, `BuildModuleClass.kt:185` (missing import of `customizations`) |
| **#1167** | Scale Figma objects based on screen DPI | **Cat 1** | Answered/Stale | Answered by iamralpht (DPI auto-handled; use `Modifier.scale()`) |
| **#1169** | Duplicate variable names across documents | **Cat 4** | **Active Bug** | `VariableManager.kt:166-193` (global `varMap` overwrites across doc IDs) |
| **#1170** | Scaling component instances takes original size | **Cat 1** | Answered/Stale | Answered by rylin8 (duplicate of #673; workaround provided) |
| **#1187** | Fill with image not showing on emulator | **Cat 1** | Answered/Stale | Answered by rylin8 (verified working; user reported local proxy) |
| **#1295** | Localization plugin UI polish | **Cat 3** | Backlog | `support-figma/extended-layout-plugin` (follow-up ticket awaiting UX input) |
| **#1299** | Control Figma UI prototype state in code | **Cat 4** | **Active Bug** | `InteractionState.kt:59-150` (all prototype state is internal; no public get/set API) |
| **#1300** | Drag-Drop implementation | **Cat 4** | **Active Bug** | `SquooshListWidget.kt` (no cross-list drag-and-drop gesture handling) |
| **#1331** | DesignVariant not working with instances | **Cat 4** | **Active Bug** | `InteractionState.kt:41-45`, `SquooshTreeBuilder.kt` (shared instanceId, lost callbacks) |
| **#1368** | More flexibility (RRO raw res & dynamic docId) | **Cat 2** | **Fixed** | Fixed by PR **#1778** (`635ca57b`, **`v0.32.0`**) and PR **#1807** (`e77b42bc`, **`v0.33.0`**) |
