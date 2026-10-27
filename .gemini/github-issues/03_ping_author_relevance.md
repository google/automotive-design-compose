# Category 3: Ping Author — No Progress / Check Relevance

**Total Issues:** 26  
**Criteria:** Older backlog tickets, investigation placeholders, or unanswered community questions with no progress in recent months; ping author for status or provide guidance.

---

## Issue #95: Add emulator tests to CI

- **URL:** [https://github.com/google/automotive-design-compose/issues/95](https://github.com/google/automotive-design-compose/issues/95)
- **Author:** @timothyfroehlich
- **Context & Current Codebase State:** Emulator tests in CI were reverted in 2023 due to extreme runner flakiness. CI now runs host Roborazzi screenshot verification on Linux (PR #2479). No emulator tests run in GitHub Actions.

### Ready-to-Post GitHub Comment

```markdown
Hi @timothyfroehlich, following up on CI emulator testing. Since we now have hermetic Roborazzi screenshot comparisons running on Linux CI without emulators, is dedicated emulator testing still desired or can this issue be closed in favor of our Roborazzi suite?
```

---

## Issue #101: Automatically stage Release Candidates to gMaven

- **URL:** [https://github.com/google/automotive-design-compose/issues/101](https://github.com/google/automotive-design-compose/issues/101)
- **Author:** @timothyfroehlich
- **Context & Current Codebase State:** GitHub Actions automatically builds and uploads designcompose_m2repo artifacts, but gMaven staging requires Google-internal Kokoro authentication which cannot run on public GitHub.

### Ready-to-Post GitHub Comment

```markdown
Hi @timothyfroehlich, following up on automated gMaven staging. Since internal Kokoro pipelines handle Google-internal publishing and GitHub Actions builds the m2repo artifacts, is there any remaining public action item here or should this be tracked internally?
```

---

## Issue #103: Allow for removal of Live Update code from production app

- **URL:** [https://github.com/google/automotive-design-compose/issues/103](https://github.com/google/automotive-design-compose/issues/103)
- **Author:** @timothyfroehlich
- **Context & Current Codebase State:** ProGuard/R8 minification strips unused code, but designcompose has not been physically split into separate core and live-update libraries. Relates to #469.

### Ready-to-Post GitHub Comment

```markdown
Hi @timothyfroehlich, checking in on the modularization of LiveUpdate. Given that R8/ProGuard strips unreferenced code in release builds, do we still plan to physically split the library into separate `designcompose-core` and `designcompose-liveupdate` artifacts?
```

---

## Issue #469: Separate the JNI crate into Live Update and Layout

- **URL:** [https://github.com/google/automotive-design-compose/issues/469](https://github.com/google/automotive-design-compose/issues/469)
- **Author:** @timothyfroehlich
- **Context & Current Codebase State:** The Rust crates were split into `dc_layout`, `dc_figma_import`, and `dc_bundle`, but `dc_jni` still bundles layout and Figma import into a single shared library. Relates to #103.

### Ready-to-Post GitHub Comment

```markdown
Hi @timothyfroehlich, following up on splitting the JNI crate. With the crates now structured as `dc_layout`, `dc_figma_import`, and `dc_bundle`, is splitting the final JNI cdylib into distinct live-update and layout shared libraries still on our roadmap?
```

---

## Issue #684: Configure Firebase Test Lab, add performance testing

- **URL:** [https://github.com/google/automotive-design-compose/issues/684](https://github.com/google/automotive-design-compose/issues/684)
- **Author:** @timothyfroehlich
- **Context & Current Codebase State:** Post-submit performance testing via Firebase Test Lab has not been configured in GitHub Actions workflows. Benchmarks currently run on local devices.

### Ready-to-Post GitHub Comment

```markdown
Hi @timothyfroehlich, checking in on Firebase Test Lab performance testing. Is automated FTL benchmarking still planned for our CI matrix, or should we continue relying on local benchmark runs?
```

---

## Issue #708: Modify Tutorial instructions to run "setFigmaTokenDebug" from Android Studio instead of the command line.

- **URL:** [https://github.com/google/automotive-design-compose/issues/708](https://github.com/google/automotive-design-compose/issues/708)
- **Author:** @timothyfroehlich
- **Context & Current Codebase State:** Tutorial docs still instruct running `./gradlew setFigmaTokenDebug` from the terminal. No Android Studio run configuration instructions have been authored.

### Ready-to-Post GitHub Comment

```markdown
Hi @timothyfroehlich, checking if modifying the tutorial documentation to run `setFigmaTokenDebug` via Android Studio instead of the command line is still planned, or if current CLI instructions are preferred.
```

---

## Issue #709: Enhance "SetFigmaToken" to target the actively running Android Device

- **URL:** [https://github.com/google/automotive-design-compose/issues/709](https://github.com/google/automotive-design-compose/issues/709)
- **Author:** @timothyfroehlich
- **Context & Current Codebase State:** `SetFigmaTokenTask.kt` still errors when multiple devices are detected (`adb found multiple devices`). IDE active device detection has not been implemented.

### Ready-to-Post GitHub Comment

```markdown
Hi @timothyfroehlich, checking if automatic target device detection in `SetFigmaTokenTask` when multiple emulators/devices are attached is still an active quality-of-life priority.
```

---

## Issue #710: Feature idea: Automatically add Compose test tags to all DesignCompose customizations

- **URL:** [https://github.com/google/automotive-design-compose/issues/710](https://github.com/google/automotive-design-compose/issues/710)
- **Author:** @timothyfroehlich
- **Context & Current Codebase State:** Compose test tags are only automatically added to replacement components in Squoosh (`SquooshRoot.kt:766`). Canvas-rendered elements do not generate test tags.

### Ready-to-Post GitHub Comment

```markdown
Hi @timothyfroehlich, following up on automatic test tag injection. With Squoosh rendering nodes onto canvas surfaces, test tags are currently attached via component replacements. Is broader automatic semantic tag generation still planned?
```

---

## Issue #711: Test library - Create examples of various tests

- **URL:** [https://github.com/google/automotive-design-compose/issues/711](https://github.com/google/automotive-design-compose/issues/711)
- **Author:** @timothyfroehlich
- **Context & Current Codebase State:** The repository contains extensive internal test suites in `integration-tests/validation`, but no standalone external sample test library was published.

### Ready-to-Post GitHub Comment

```markdown
Hi @timothyfroehlich, checking if creating a dedicated standalone sample testing library is still desired or if the comprehensive examples in `integration-tests/validation` and `docs/working-with-source/writing-tests.md` fulfill this requirement.
```

---

## Issue #712: Documentation - Document compile-time optimizations

- **URL:** [https://github.com/google/automotive-design-compose/issues/712](https://github.com/google/automotive-design-compose/issues/712)
- **Author:** @timothyfroehlich
- **Context & Current Codebase State:** Documentation does not include a dedicated guide on file separation for KSP incremental compile-time optimization.

### Ready-to-Post GitHub Comment

```markdown
Hi @timothyfroehlich, checking if you would still like a dedicated documentation page on compile-time optimization and interface file separation, or if this can be closed.
```

---

## Issue #713: Tutorial - consider alternative instructions for loading your Figma Doc ID

- **URL:** [https://github.com/google/automotive-design-compose/issues/713](https://github.com/google/automotive-design-compose/issues/713)
- **Author:** @timothyfroehlich
- **Context & Current Codebase State:** Tutorial instructions still use the standard Figma Doc ID workflow.

### Ready-to-Post GitHub Comment

```markdown
Hi @timothyfroehlich, checking if alternative instructions for loading Figma Doc IDs in the tutorial are still needed or if current tutorial instructions remain the standard flow.
```

---

## Issue #883: Consolidate JNI Library artifacts into a single Gradle module

- **URL:** [https://github.com/google/automotive-design-compose/issues/883](https://github.com/google/automotive-design-compose/issues/883)
- **Author:** @timothyfroehlich
- **Context & Current Codebase State:** The Cargo build outputs `libdesigncompose_jni.so` and `dc_jni`. A single consolidated Gradle module for all JNI artifacts was proposed.

### Ready-to-Post GitHub Comment

```markdown
Hi @timothyfroehlich, checking in on consolidating JNI library artifacts into a single Gradle module. Given recent JNI naming and build updates via BuildConfig (PR #2500), is further Gradle module consolidation still planned?
```

---

## Issue #1118: Provide additional test cases for external renderer comparison.

- **URL:** [https://github.com/google/automotive-design-compose/issues/1118](https://github.com/google/automotive-design-compose/issues/1118)
- **Author:** @johnoneil
- **Context & Current Codebase State:** Proposal from May 2024 to make `liveUpdate` a formal Gradle plugin extension block rather than relying on system properties. No changes made in over 2 years.

### Ready-to-Post GitHub Comment

```markdown
Hi @timothyfroehlich, following up on adding a dedicated `liveUpdate` DSL extension block to the DesignCompose Gradle plugin. Is this still planned for our Gradle plugin API?
```

---

## Issue #1295: Localization plugin UI polish

- **URL:** [https://github.com/google/automotive-design-compose/issues/1295](https://github.com/google/automotive-design-compose/issues/1295)
- **Author:** @yiqunw700
- **Context & Current Codebase State:** Feature request from June 2024 to support border radius on dashed strokes. No commits or progress.

### Ready-to-Post GitHub Comment

```markdown
Hi @rylin8, checking if border radius support on dashed strokes is still on the roadmap or if it should be closed as a backlog item.
```

---

## Issue #1446: Animate between design token values

- **URL:** [https://github.com/google/automotive-design-compose/issues/1446](https://github.com/google/automotive-design-compose/issues/1446)
- **Author:** @rylin8
- **Context & Current Codebase State:** Feature request from July 2024 to animate transitions when design tokens or Figma variables switch modes. No progress for over 2 years.

### Ready-to-Post GitHub Comment

```markdown
Hi @rylin8, checking if animated transitions between design token / variable mode values (e.g. Light/Dark theme transitions) is still on the roadmap or should be archived for now.
```

---

## Issue #1450: Begin tightly vetting Rust dependencies

- **URL:** [https://github.com/google/automotive-design-compose/issues/1450](https://github.com/google/automotive-design-compose/issues/1450)
- **Author:** @timothyfroehlich
- **Context & Current Codebase State:** Process task from July 2024 proposing a policy for vetting Rust dependencies. `cargo-deny` runs in CI via `deny.toml`.

### Ready-to-Post GitHub Comment

```markdown
Hi @timothyfroehlich, checking if our current `cargo-deny` configuration and CI vulnerability checks in `deny.toml` satisfy this requirement or if additional dependency review processes are needed.
```

---

## Issue #1555: More support for text stroke

- **URL:** [https://github.com/google/automotive-design-compose/issues/1555](https://github.com/google/automotive-design-compose/issues/1555)
- **Author:** @yiqunw700
- **Context & Current Codebase State:** P3 feature request from August 2024 proposing extended support for text stroke outlines. No progress in 2 years.

### Ready-to-Post GitHub Comment

```markdown
Hi @yiqunw700, following up on extended text stroke support. Is this feature still needed for upcoming designs or can this issue be closed?
```

---

## Issue #1568: Determine the plan for managing Rust dependencies

- **URL:** [https://github.com/google/automotive-design-compose/issues/1568](https://github.com/google/automotive-design-compose/issues/1568)
- **Author:** @timothyfroehlich
- **Context & Current Codebase State:** Empty placeholder issue from August 2024 regarding managing Rust dependencies.

### Ready-to-Post GitHub Comment

```markdown
Hi @timothyfroehlich, checking if this issue can be closed since Rust dependencies are continuously audited via Dependabot and `cargo-deny`.
```

---

## Issue #1721: Dropdown list,  Slider bar support

- **URL:** [https://github.com/google/automotive-design-compose/issues/1721](https://github.com/google/automotive-design-compose/issues/1721)
- **Author:** @BIN-KOUHINN
- **Context & Current Codebase State:** Customer asked on 2024-10-22 how to implement Dropdown lists and Slider bars in DesignCompose. No maintainer response was given.

### Ready-to-Post GitHub Comment

```markdown
Hi @BIN-KOUHINN, apologies for the long delay in responding!

In DesignCompose, interactive Dropdown menus and Sliders can be implemented in two ways depending on your design setup:

1. **Using Component Replacement (Recommended for full interactive behavior):**
   In Figma, designate a placeholder frame (e.g., `#dropdown_placeholder` or `#slider_placeholder`). In your Composable interface, annotate it with `@Design(node = "#dropdown_placeholder") customDropdown: @Composable (ComponentReplacementContext) -> Unit`. You can then render Jetpack Compose's native `DropdownMenu` or `Slider` with full state handling while DesignCompose manages its positioning and layout.

2. **Using Dials & Gauges / Draggable Progress Bars (For visual sliders):**
   If you want the visual styling from Figma, you can use the Dials & Gauges plugin to mark a node as a draggable progress bar. In Kotlin, bind it to an `OnProgressChangedCallback` via `@Design(node = "#slider-tap") onSliderChanged: OnProgressChangedCallback` (see [Dials & Gauges Documentation](https://google.github.io/automotive-design-compose/docs/modifiers/modifiers#draggable-progress-bars)).

3. **Using Component Variants with TapCallback (For simple dropdowns):**
   Design a component set in Figma with `State=Closed` and `State=Open` variants, and bind `@Design(node = "#trigger") onToggle: TapCallback` to switch between variant states.

Please let us know if you still need assistance with this!
```

---

## Issue #2145: RTL support

- **URL:** [https://github.com/google/automotive-design-compose/issues/2145](https://github.com/google/automotive-design-compose/issues/2145)
- **Author:** @yiqunw700
- **Context & Current Codebase State:** Empty backlog placeholder from April 2025 titled 'RTL support'. No progress.

### Ready-to-Post GitHub Comment

```markdown
Hi @yiqunw700, checking in on Right-to-Left (RTL) text and layout support. Are there specific requirements or designs driving this, or should we archive this placeholder for now?
```

---

## Issue #2183: Investigate improvements for testing for nodes

- **URL:** [https://github.com/google/automotive-design-compose/issues/2183](https://github.com/google/automotive-design-compose/issues/2183)
- **Author:** @rylin8
- **Context & Current Codebase State:** Backlog investigation ticket from April 2025 on testing improvements for nodes. No progress.

### Ready-to-Post GitHub Comment

```markdown
Hi @rylin8, following up on node testing improvements. Have existing Roborazzi comparisons and `ComponentReplacementContext` testing satisfied our needs, or are there further testing APIs planned?
```

---

## Issue #2188: Add support for insets in the Scalable UI plugin

- **URL:** [https://github.com/google/automotive-design-compose/issues/2188](https://github.com/google/automotive-design-compose/issues/2188)
- **Author:** @rylin8
- **Context & Current Codebase State:** Plugin feature request from May 2025 to add support for insets in the Scalable UI plugin. No progress.

### Ready-to-Post GitHub Comment

```markdown
Hi @rylin8, checking if inset support in the Scalable UI plugin is still on the roadmap or if it should be deprioritized.
```

---

## Issue #2189: Add support for rounded corners in Scalable UI

- **URL:** [https://github.com/google/automotive-design-compose/issues/2189](https://github.com/google/automotive-design-compose/issues/2189)
- **Author:** @rylin8
- **Context & Current Codebase State:** Plugin exploration ticket from May 2025 regarding rounded corners in Scalable UI. No progress.

### Ready-to-Post GitHub Comment

```markdown
Hi @rylin8, checking if rounded corner support in the Scalable UI plugin is still planned or if it can be closed.
```

---

## Issue #2190: Allow progress bar and indicator preview simultaneously

- **URL:** [https://github.com/google/automotive-design-compose/issues/2190](https://github.com/google/automotive-design-compose/issues/2190)
- **Author:** @rylin8
- **Context & Current Codebase State:** Plugin UI improvement from May 2025 to allow simultaneous progress bar and indicator preview in the plugin. No progress.

### Ready-to-Post GitHub Comment

```markdown
Hi @rylin8, checking if simultaneous progress bar and indicator preview in the plugin is still planned.
```

---

## Issue #2208: font family variables support

- **URL:** [https://github.com/google/automotive-design-compose/issues/2208](https://github.com/google/automotive-design-compose/issues/2208)
- **Author:** @farivar-google
- **Context & Current Codebase State:** Feature request from May 2025 for font family variables support. Currently font weights and sizes support variables, but font families do not.

### Ready-to-Post GitHub Comment

```markdown
Hi @farivar-google, checking if font family variable binding support is currently needed for any active partner themes or designs.
```

---

## Issue #2245: Scope Figma Draw features

- **URL:** [https://github.com/google/automotive-design-compose/issues/2245](https://github.com/google/automotive-design-compose/issues/2245)
- **Author:** @iamralpht
- **Context & Current Codebase State:** Figma Draw feature scoping from July 2025. Blocked because Figma does not yet expose Figma Draw data in the REST API.

### Ready-to-Post GitHub Comment

```markdown
Hi @iamralpht, checking in on Figma Draw support. Since Figma does not currently expose Draw data via the REST API, should we keep this on hold or close until upstream API support is introduced?
```

---
