# Category 2: Close Right Away — Already Fixed in Releases / Commits / Superseded

**Total Issues:** 27  
**Criteria:** Issue was already implemented, resolved, or rendered obsolete by merged commits, pull requests, and releases in the repository.

---

## Issue #75: Create method for fetching the serialized Figma files for an app via Gradle task

- **URL:** [https://github.com/google/automotive-design-compose/issues/75](https://github.com/google/automotive-design-compose/issues/75)
- **Author:** @timothyfroehlich
- **Commit(s):** `d5b554e1`, `1ac4a28b`, `38e26477`
- **PR(s):** PR #542, PR #1611, PR #2309
- **Release Tag(s):** `v0.25.0`, `v0.32.0`, `v0.38.0`
- **Technical Resolution:** Implemented in the internal Gradle plugin (plugins/gradle-plugin-internal/src/main/java/com/android/designcompose/gradle/internal/InternalGradlePlugin.kt) via fetchFigmaFiles and fetchAndUpdateFigmaFiles tasks, which fetch serialized .dcf files during test runs and copy them to src/main/assets/figma. Additionally, PR #2309 built and released the standalone cross-platform fetch CLI binary for fetching .dcf files outside Gradle.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. Gradle-based fetching for testing is provided by the fetchFigmaFiles and fetchAndUpdateFigmaFiles tasks in plugins/gradle-plugin-internal (PR #542 / #1611). Furthermore, the standalone fetch CLI tool is now packaged and distributed with official releases (PR #2309) for offline serialization workflows.
```

---

## Issue #131: Add documentation covering Dials & Gauges for developers

- **URL:** [https://github.com/google/automotive-design-compose/issues/131](https://github.com/google/automotive-design-compose/issues/131)
- **Author:** @timothyfroehlich
- **Commit(s):** `6355112d`, `10802204`
- **PR(s):** PR #2182, PR #1960
- **Release Tag(s):** `v0.35.0`, `v0.36.0`
- **Technical Resolution:** Extensive developer documentation covering Dials, Gauges, and Progress Bars was added to docs/_docs/modifiers/modifiers.md (lines 899-960), detailing Meter customizations, progress percentage values, and draggable progress bars with OnProgressChangedCallback (added in PR #2182).

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. Comprehensive documentation for developers integrating Dials, Gauges, and Progress Bars is available in the Modifiers & Customizations documentation (https://google.github.io/automotive-design-compose/docs/modifiers/modifiers#dials-gauges-and-progress-bars).
```

---

## Issue #177: Implement lightweight theming using Variables

- **URL:** [https://github.com/google/automotive-design-compose/issues/177](https://github.com/google/automotive-design-compose/issues/177)
- **Author:** @iamralpht
- **Commit(s):** `86a5fb87`, `a96dbc96`
- **PR(s):** PR #1055, PR #1165
- **Release Tag(s):** `v0.28.0`, `v0.29.0`
- **Technical Resolution:** Lightweight theming using Figma Variables and design tokens was implemented in PR #1055 and documented in PR #1165 (docs/_docs/variables.md). It supports Material Theme Builder collections, mode switching between Light and Dark via DesignVariableModeValues, and dynamic runtime collection swapping via DesignVariableCollection.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. Figma Variable theming support landed in PR #1055 and is fully documented in Variables & Theming (https://google.github.io/automotive-design-compose/docs/variables) with support for Material Theme Builder, DesignVariableCollection, and DesignVariableModeValues.
```

---

## Issue #268: Add Fetcher support for figma token file

- **URL:** [https://github.com/google/automotive-design-compose/issues/268](https://github.com/google/automotive-design-compose/issues/268)
- **Author:** @timothyfroehlich
- **Commit(s):** `41bba567`
- **PR(s):** PR #1594
- **Release Tag(s):** `v0.31.0`
- **Technical Resolution:** PR #1594 modified the fetch binary tool to automatically load the Figma access token from FIGMA_ACCESS_TOKEN environment variable or ~/.config/figma_access_token file (implemented in crates/dc_figma_import/src/tools/fetch.rs:89-100).

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. Implemented in PR #1594 (41bba567), which enables the fetch CLI tool to automatically resolve authentication tokens from command line arguments, the FIGMA_ACCESS_TOKEN environment variable, or ~/.config/figma_access_token.
```

---

## Issue #269: Manifest support for Fetcher

- **URL:** [https://github.com/google/automotive-design-compose/issues/269](https://github.com/google/automotive-design-compose/issues/269)
- **Author:** @timothyfroehlich
- **Commit(s):** `53b07ef2`, `ee08cce4`
- **PR(s):** PR #1521, PR #1886
- **Release Tag(s):** `v0.31.0`, `v0.34.0`
- **Technical Resolution:** The proposed JSON manifest format was superseded by the architectural migration to Protocol Buffers (dc_bundle). The fetch tool directly fetches and serializes Figma files and their dependencies into self-contained .dcf Protobuf bundles without needing an intermediate JSON manifest.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as superseded. The experimental JSON manifest approach was replaced by our migration to Protocol Buffers (dc_bundle) and .dcf files in PR #1521 and PR #1886, where the fetch tool packages documents and component dependencies directly.
```

---

## Issue #270: Convert existing Kotlin JSON generation in Common and DesignCompose to use kotlinx.serialization

- **URL:** [https://github.com/google/automotive-design-compose/issues/270](https://github.com/google/automotive-design-compose/issues/270)
- **Author:** @timothyfroehlich
- **Commit(s):** `53b07ef2`, `ee08cce4`
- **PR(s):** PR #1521, PR #1886
- **Release Tag(s):** `v0.31.0`, `v0.34.0`
- **Technical Resolution:** The migration of Kotlin JSON generation to kotlinx.serialization was superseded when all wire serialization between Kotlin and Rust was replaced by Protocol Buffers lite (protobuf-kotlin-lite and dc_bundle).

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as superseded. With the adoption of Protocol Buffers (dc_bundle) across both Kotlin and Rust (PR #1886), JSON generation via DocumentServerParams was deprecated and removed.
```

---

## Issue #271: Update Kotlin and Rust to new Manifest schema

- **URL:** [https://github.com/google/automotive-design-compose/issues/271](https://github.com/google/automotive-design-compose/issues/271)
- **Author:** @timothyfroehlich
- **Commit(s):** `ee08cce4`
- **PR(s):** PR #1886
- **Release Tag(s):** `v0.34.0`
- **Technical Resolution:** Superseded by the protobuf schema migration in PR #1886. The Kotlin and Rust layers communicate using generated Protocol Buffer messages rather than a custom JSON Manifest schema.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as superseded. The proposed JSON Manifest schema was superseded by the unified Protocol Buffer schema in dc_bundle (PR #1886).
```

---

## Issue #272: Incremental update support for Fetcher

- **URL:** [https://github.com/google/automotive-design-compose/issues/272](https://github.com/google/automotive-design-compose/issues/272)
- **Author:** @timothyfroehlich
- **Commit(s):** `5e466a04`, `1ac4a28b`
- **PR(s):** PR #1776, PR #1611
- **Release Tag(s):** `v0.32.0`, `v0.33.0`
- **Technical Resolution:** Incremental update support was built into dc_figma_import: document.rs checks version IDs and timestamps against existing .dcf files, and image_context.rs encodes and decodes previous image session maps so only changed/new assets are fetched.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. Incremental update support is implemented in dc_figma_import (document.rs and image_context.rs), which evaluates existing .dcf timestamps and preserves image sessions across fetches.
```

---

## Issue #273: Create Gradle figmaFileFetch task, including bundling the Fetcher tool with the plugin.

- **URL:** [https://github.com/google/automotive-design-compose/issues/273](https://github.com/google/automotive-design-compose/issues/273)
- **Author:** @timothyfroehlich
- **Commit(s):** `d5b554e1`, `38e26477`
- **PR(s):** PR #542, PR #2309
- **Release Tag(s):** `v0.25.0`, `v0.38.0`
- **Technical Resolution:** Covered by InternalGradlePlugin (fetchFigmaFiles) for test execution, and by the standalone fetch CLI binary which is cross-compiled for Linux, macOS, and Windows and distributed with GitHub releases (PR #2309).

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed / superseded. Automated test fetching is provided by fetchFigmaFiles in InternalGradlePlugin (PR #542), and the cross-platform fetch binary is now published directly with GitHub releases (PR #2309).
```

---

## Issue #274: Connect the figmaFileFetch task to the output of the Codegen task, add DSL, update documentation.

- **URL:** [https://github.com/google/automotive-design-compose/issues/274](https://github.com/google/automotive-design-compose/issues/274)
- **Author:** @timothyfroehlich
- **Commit(s):** `d5b554e1`, `1ac4a28b`
- **PR(s):** PR #542, PR #1611
- **Release Tag(s):** `v0.25.0`, `v0.32.0`
- **Technical Resolution:** Superseded by the @DesignDoc and Roborazzi Gradle plugin convention workflow, where @DesignDoc generates code and InternalGradlePlugin handles fetching directly.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as superseded. The workflow connects @DesignDoc annotations with automated fetching through our Roborazzi test convention plugin (InternalGradlePlugin).
```

---

## Issue #275: Create figmaFileCommit and figmaFileFetch andCommit tasks, create developer script to update all serialized files in the repository.

- **URL:** [https://github.com/google/automotive-design-compose/issues/275](https://github.com/google/automotive-design-compose/issues/275)
- **Author:** @timothyfroehlich
- **Commit(s):** `d5b554e1`, `1ac4a28b`
- **PR(s):** PR #542, PR #1611
- **Release Tag(s):** `v0.25.0`, `v0.32.0`
- **Technical Resolution:** Implemented by fetchAndUpdateFigmaFiles in InternalGradlePlugin.kt (lines 36-37, 73-76, 116-121), which automatically fetches and overwrites the .dcf files into src/main/assets/figma for git tracking.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. The fetchAndUpdateFigmaFiles Gradle task in InternalGradlePlugin fetches updated .dcf files and writes them directly to src/main/assets/figma so they can be committed to the repository.
```

---

## Issue #278: Document using DesignCompose to create a traditional Android View

- **URL:** [https://github.com/google/automotive-design-compose/issues/278](https://github.com/google/automotive-design-compose/issues/278)
- **Author:** @iamralpht
- **Commit(s):** `4a14a501`
- **PR(s):** PR #2254
- **Release Tag(s):** `v0.36.0`
- **Technical Resolution:** PR #2254 added documentation and code examples in docs/_docs/modifiers/modifiers.md under ComponentReplacementContext for Custom Android/Compose Views demonstrating how to wrap traditional Android views (like SurfaceView) inside an AndroidView composable replacing a design placeholder node.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. Documentation and code examples for embedding traditional Android views via AndroidView and ComponentReplacementContext were added in PR #2254 (see https://google.github.io/automotive-design-compose/docs/modifiers/modifiers#componentreplacementcontext-for-custom-androidcompose-views).
```

---

## Issue #290: Cleanup rectangle / bounding box setup

- **URL:** [https://github.com/google/automotive-design-compose/issues/290](https://github.com/google/automotive-design-compose/issues/290)
- **Author:** @kportl
- **Commit(s):** `53b07ef2`, `bc643047`, `f260fb8b`
- **PR(s):** PR #1521, PR #1519, PR #1459
- **Release Tag(s):** `v0.31.0`
- **Technical Resolution:** The legacy toolkit_schema.rs and its bounding box/rectangle fields were completely deleted when the serialization format was migrated to dc_bundle Protobuf messages.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. crates/figma_import/src/toolkit_schema.rs was removed in PR #1521 (53b07ef2) as part of the dc_bundle migration, eliminating the deprecated bounding box fields.
```

---

## Issue #561: Performance optimization for Rust layout implementation

- **URL:** [https://github.com/google/automotive-design-compose/issues/561](https://github.com/google/automotive-design-compose/issues/561)
- **Author:** @iamralpht
- **Commit(s):** `71d6aeec`, `1245586e`, `c631db26`
- **PR(s):** PR #1883, PR #2320, PR #2458
- **Release Tag(s):** `v0.34.0`, `v0.38.0`, `v0.39.2`
- **Technical Resolution:** Rust layout and list rendering performance was completely overhauled by transitioning to the Squoosh layout engine (designcompose/squoosh/), optimizing Taffy layout passes, adding list pagination, and optimizing native binary code generation.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed / superseded. Performance regressions from the early Rust layout branch were resolved by the introduction and stabilization of the Squoosh rendering architecture (PR #1883) and recent native optimizations in PR #2458.
```

---

## Issue #604: Add support for baseline alignment

- **URL:** [https://github.com/google/automotive-design-compose/issues/604](https://github.com/google/automotive-design-compose/issues/604)
- **Author:** @iamralpht
- **Commit(s):** `dd5caddb`, `fb0ed3d4`
- **PR(s):** PR #2455, PR #856
- **Release Tag(s):** `v0.39.2`
- **Technical Resolution:** PR #2455 fixed the mapping of LayoutAlignItems::Baseline in crates/dc_figma_import/src/transform_flexbox.rs, connecting it to AlignItems::ALIGN_ITEMS_BASELINE and Taffy baseline alignment with unit tests (test_layout_baseline). Released in v0.39.2 (commit b81b4bdf).

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. Implemented and verified in PR #2455 (dd5caddb), correctly mapping LayoutAlignItems::Baseline to Taffy baseline alignment, released in v0.39.2.
```

---

## Issue #606: Investigate transition to Kotlin 2.0 and KSP 2.0

- **URL:** [https://github.com/google/automotive-design-compose/issues/606](https://github.com/google/automotive-design-compose/issues/606)
- **Author:** @timothyfroehlich
- **Commit(s):** `0103147c`
- **PR(s):** PR #2232
- **Release Tag(s):** `v0.36.0`
- **Technical Resolution:** PR #2232 upgraded the project to Kotlin 2.0 (Kotlin 2.1.21) and JetBrains Compose Compiler 2.1.21, refactoring Gradle plugins for classloader isolation. (KSP 2 is tracked in issue #2231).

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. The upgrade to Kotlin 2.0 and Jetpack Compose Compiler 2.0 was merged in PR #2232 (0103147c). The project is currently running Kotlin 2.1.21.
```

---

## Issue #688: Nightly Experimental builds failing

- **URL:** [https://github.com/google/automotive-design-compose/issues/688](https://github.com/google/automotive-design-compose/issues/688)
- **Author:** @timothyfroehlich
- **Commit(s):** `3f723267`
- **PR(s):** PR #295
- **Release Tag(s):** `v0.22.0`
- **Technical Resolution:** The legacy experimental nightly workflow referenced in this issue was retired and removed during GitHub Actions consolidation, and CI builds run without disk space errors.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as obsolete. The experimental nightly build workflow was retired during workflow consolidation, and active CI pipelines run without runner disk space issues.
```

---

## Issue #690: Improve handling of AAOS Unbundled Libraries in GitHub Actions

- **URL:** [https://github.com/google/automotive-design-compose/issues/690](https://github.com/google/automotive-design-compose/issues/690)
- **Author:** @timothyfroehlich
- **Commit(s):** `380aa782`, `0fea4790`
- **PR(s):** PR #672, PR #2345
- **Release Tag(s):** `v0.25.0`, `v0.38.0`
- **Technical Resolution:** In .github/workflows/main.yml, AAOS unbundled libraries were split into an independent build step that uploads unbundled_m2repo, allowing dependent jobs to consume the prebuilts without recompilation.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. Unbundled AAOS library builds were isolated into a dedicated job in main.yml that uploads and caches unbundled_m2repo, eliminating redundant compilation across workflow jobs.
```

---

## Issue #890: Use version catalog for exoplayer test dependency

- **URL:** [https://github.com/google/automotive-design-compose/issues/890](https://github.com/google/automotive-design-compose/issues/890)
- **Author:** @iamralpht
- **Commit(s):** `20cdf7a3`
- **PR(s):** PR #1566
- **Release Tag(s):** `v0.31.0`
- **Technical Resolution:** PR #1566 migrated all hardcoded androidx.media3/exoplayer dependencies in integration-tests/validation/build.gradle.kts to the Gradle version catalog (libs.androidx.media3.exoplayer and libs.androidx.media3.ui).

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. Hardcoded exoplayer dependencies were migrated to the version catalog in PR #1566 (20cdf7a3).
```

---

## Issue #905: Masks inside of variants do not render correctly

- **URL:** [https://github.com/google/automotive-design-compose/issues/905](https://github.com/google/automotive-design-compose/issues/905)
- **Author:** @HooHaa-code
- **Commit(s):** `71d6aeec`
- **PR(s):** PR #1883
- **Release Tag(s):** `v0.34.0`
- **Technical Resolution:** Masks inside variants rendered incorrectly in the legacy non-Squoosh rendering path (DesignFrame.kt). In PR #1883, the entire legacy non-Squoosh code path was deleted, and Squoosh natively handles variant masks correctly.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. The variant mask rendering bug existed in the legacy rendering engine, which was completely replaced and removed in PR #1883 in favor of Squoosh.
```

---

## Issue #1368: More flexibility possible?

- **URL:** [https://github.com/google/automotive-design-compose/issues/1368](https://github.com/google/automotive-design-compose/issues/1368)
- **Author:** @StephanSchuster
- **Commit(s):** `635ca57b`, `8d067c29`
- **PR(s):** PR #1778, PR #2297
- **Release Tag(s):** `v0.32.0`, `v0.37.0`
- **Technical Resolution:** PR #1778 implemented DesignSettings.setRawResourceId() for loading offline DCF files from res/raw for RRO overlays. PR #2297 implemented runtime dynamic docId switching via DesignSettings.setDocId().

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. Support for loading RRO raw resources was added via DesignSettings.setRawResourceId() in PR #1778 (v0.32.0), and dynamic docId switching was added via DesignSettings.setDocId() in PR #2297 (v0.37.0).
```

---

## Issue #1373: Create a better separation between toolkit_schema and figma_schema

- **URL:** [https://github.com/google/automotive-design-compose/issues/1373](https://github.com/google/automotive-design-compose/issues/1373)
- **Author:** @timothyfroehlich
- **Commit(s):** `53b07ef2`, `ee08cce4`
- **PR(s):** PR #1521, PR #1886
- **Release Tag(s):** `v0.31.0`, `v0.34.0`
- **Technical Resolution:** PR #1521 deleted toolkit_schema.rs, and PR #1886 decoupled Figma REST schema from DesignCompose wire formats by migrating all toolkit definitions to Protocol Buffers in crates/dc_bundle/src/proto/.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. toolkit_schema.rs was removed in PR #1521 (53b07ef2), and the schema separation was finalized in PR #1886 with the migration to Protocol Buffers (dc_bundle).
```

---

## Issue #1419: This would be a candidate for writing an `Into` implementation in figma_schema

- **URL:** [https://github.com/google/automotive-design-compose/issues/1419](https://github.com/google/automotive-design-compose/issues/1419)
- **Author:** @timothyfroehlich
- **Commit(s):** `ee08cce4`
- **PR(s):** PR #1886
- **Release Tag(s):** `v0.34.0`
- **Technical Resolution:** With the migration to Protocol Buffers in PR #1886, manual Into conversion implementations in figma_schema became obsolete as transform_flexbox.rs directly constructs protobuf messages.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed / obsolete. Manual Into conversion implementations were rendered obsolete by the Protocol Buffer migration in PR #1886.
```

---

## Issue #1544: Improve error handling: propagate an error from Rust and show it in DesignSwitcher

- **URL:** [https://github.com/google/automotive-design-compose/issues/1544](https://github.com/google/automotive-design-compose/issues/1544)
- **Author:** @yiqunw700
- **Commit(s):** `b9e6756d`
- **PR(s):** PR #2374
- **Release Tag(s):** `v0.39.0`
- **Technical Resolution:** PR #2374 added serde_path_to_error to crates/dc_figma_import, allowing Rust deserialization errors with exact JSON paths to propagate across JNI to Kotlin and display in Design Switcher.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. Implemented in PR #2374 (b9e6756d), which propagates detailed JSON deserialization errors from Rust across JNI to the Design Switcher UI.
```

---

## Issue #1792: Squoosh enabled cannot find variable for components imported from library

- **URL:** [https://github.com/google/automotive-design-compose/issues/1792](https://github.com/google/automotive-design-compose/issues/1792)
- **Author:** @Heneveld
- **Commit(s):** `3bc6997f`, `953bf979`
- **PR(s):** PR #2206, PR #2507
- **Release Tag(s):** `v0.36.0`, `v0.39.6`
- **Technical Resolution:** Fixed in PR #2206 (Fix variable resolution when variable is alias) and PR #2507 (keep local variable id when a remote component aliases the same variable), ensuring variables in components imported from external libraries resolve properly in Squoosh.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. Fixed in PR #2206 (3bc6997f) and PR #2507 (953bf979), which correctly preserve and resolve variable aliases for components imported from external Figma libraries.
```

---

## Issue #2138: RB HVAC investigation

- **URL:** [https://github.com/google/automotive-design-compose/issues/2138](https://github.com/google/automotive-design-compose/issues/2138)
- **Author:** @yiqunw700
- **Commit(s):** `ba97a4db`
- **PR(s):** PR #2150
- **Release Tag(s):** `v0.35.0`
- **Technical Resolution:** Completed via PR #2150 (Add a RB hvac example to validation Towards: #2138 Fixes: #2148), adding HvacComponentsDoc in validation tests and meter schema support in dc_figma_import.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. The RB HVAC validation and meter integration were merged in PR #2150 (ba97a4db).
```

---

## Issue #2330: Layers with opacity start flickering when placed on another layer with opacity

- **URL:** [https://github.com/google/automotive-design-compose/issues/2330](https://github.com/google/automotive-design-compose/issues/2330)
- **Author:** @Elektrobit-HendrikCammann
- **Commit(s):** `93e5b254`, `19682c01`, `46ceece3`
- **PR(s):** PR #2400, PR #2460, PR #2476
- **Release Tag(s):** `v0.39.2`, `v0.39.3`
- **Technical Resolution:** Fixed across three PRs: PR #2400 fixed variable opacity rendering, PR #2460 separated raw layer opacity from variable opacity, and PR #2476 explicitly serialized 1.0/0.0 opacities to prevent shader alpha multiplication flickering.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as completed. Opacity flickering when layering transparent elements with variables was resolved across PR #2400 (93e5b254), PR #2460 (19682c01), and PR #2476 (46ceece3).
```

---
