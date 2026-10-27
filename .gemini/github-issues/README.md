# GitHub Issues Triage & Master Action Plan
**Repository:** `google/automotive-design-compose`  
**Current Date:** October 26, 2026  
**Total Open Issues Triaged:** 95 issues (100% of open backlog)

---

## Executive Summary

A comprehensive triage of all **95 open GitHub issues** in `google/automotive-design-compose` was conducted against the full Git commit history, pull requests, releases (up to `v0.39.6`), and current repository source code. Each issue was thoroughly evaluated and categorized into one of four actionable triage buckets:

| Category | Purpose | Count | % of Open | Document Link |
| :--- | :--- | :---: | :---: | :--- |
| **Category 1** | **Close Right Away: Stale / Awaiting Author / Answered** | **16** | 16.8% | [01_close_stale_no_response.md](./01_close_stale_no_response.md) |
| **Category 2** | **Close Right Away: Already Fixed in Releases / Commits** | **27** | 28.4% | [02_close_already_fixed.md](./02_close_already_fixed.md) |
| **Category 3** | **Ping Author: No Progress / Check Relevance** | **26** | 27.4% | [03_ping_author_relevance.md](./03_ping_author_relevance.md) |
| **Category 4** | **Actionable Bugs & Enhancements: Needs Google Action** | **26** | 27.4% | [04_actionable_bugs_and_enhancements.md](./04_actionable_bugs_and_enhancements.md) |
| **TOTAL** | **All Open Issues Accounted For** | **95** | 100.0% | |

### Triage Impact

- **Immediate Backlog Reduction:** **43 issues (45.2%)** can be closed immediately (16 stale + 27 already fixed).

- **Engagement & Verification:** **26 issues (27.4%)** can be verified by pinging authors with ready-to-post messages.

- **True Active Workload:** Only **26 issues (27.4%)** represent genuine active engineering bugs, enhancements, or documentation tasks.


---

## Category Summary & Issue Lists

### [Category 1: Close Right Away — Stale / Awaiting Author Response / Answered (16 Issues)](./01_close_stale_no_response.md)

Issues where maintainers already provided answers/workarounds >3 months ago, the author worked around the issue, or empty placeholders with no follow-up:

- [#93: Add method to input Figma Design Token via Design Switcher](https://github.com/google/automotive-design-compose/issues/93) (@timothyfroehlich)
- [#97: Create method for setting the Figma Token once per device](https://github.com/google/automotive-design-compose/issues/97) (@timothyfroehlich)
- [#112: "Favorite docs" - Add method for setting alternate Figma Files to load in the Design Switcher](https://github.com/google/automotive-design-compose/issues/112) (@timothyfroehlich)
- [#132: Reorganize docs into a new set of categories](https://github.com/google/automotive-design-compose/issues/132) (@timothyfroehlich)
- [#136: Impossible to fetch node under intersection/union](https://github.com/google/automotive-design-compose/issues/136) (@elouannf)
- [#225: Live update stops working with frames containing large number of layers](https://github.com/google/automotive-design-compose/issues/225) (@FruitSnack1)
- [#250: Replace all references to Figma "documents" with "file"in the source code / API](https://github.com/google/automotive-design-compose/issues/250) (@timothyfroehlich)
- [#282: Consider making an allowlist for recursive doc fetches](https://github.com/google/automotive-design-compose/issues/282) (@iamralpht)
- [#628: Galaxy design crashes emulators on APIs greater than 33](https://github.com/google/automotive-design-compose/issues/628) (@timothyfroehlich)
- [#739: Design Component must be within the main document Figma interface](https://github.com/google/automotive-design-compose/issues/739) (@Heneveld)
- [#840: Variant prototype not working when the elements come from a library](https://github.com/google/automotive-design-compose/issues/840) (@MarcFerrerVWGS)
- [#1167: How to scale the Figma objects in the emulator based on the screen DPI](https://github.com/google/automotive-design-compose/issues/1167) (@spkdroid)
- [#1170: Scaling two separates instances of a component will always take the original size](https://github.com/google/automotive-design-compose/issues/1170) (@Heneveld)
- [#1187: Fill with image not showing on the emulator](https://github.com/google/automotive-design-compose/issues/1187) (@MarcFerrerVWGS)
- [#1994: Make component library with DC](https://github.com/google/automotive-design-compose/issues/1994) (@rylin8)
- [#2332: Component Instances are not render correctly until detached](https://github.com/google/automotive-design-compose/issues/2332) (@Elektrobit-HendrikCammann)

---

### [Category 2: Close Right Away — Already Fixed in Releases / Commits / Superseded (27 Issues)](./02_close_already_fixed.md)

Issues that have been fully resolved by merged PRs, commits, or major architectural transitions (e.g. Squoosh engine, Protocol Buffers `dc_bundle` migration, Kotlin 2.0):

- [#75: Create method for fetching the serialized Figma files for an app via Gradle task](https://github.com/google/automotive-design-compose/issues/75) (@timothyfroehlich)
- [#131: Add documentation covering Dials & Gauges for developers](https://github.com/google/automotive-design-compose/issues/131) (@timothyfroehlich)
- [#177: Implement lightweight theming using Variables](https://github.com/google/automotive-design-compose/issues/177) (@iamralpht)
- [#268: Add Fetcher support for figma token file](https://github.com/google/automotive-design-compose/issues/268) (@timothyfroehlich)
- [#269: Manifest support for Fetcher](https://github.com/google/automotive-design-compose/issues/269) (@timothyfroehlich)
- [#270: Convert existing Kotlin JSON generation in Common and DesignCompose to use kotlinx.serialization](https://github.com/google/automotive-design-compose/issues/270) (@timothyfroehlich)
- [#271: Update Kotlin and Rust to new Manifest schema](https://github.com/google/automotive-design-compose/issues/271) (@timothyfroehlich)
- [#272: Incremental update support for Fetcher](https://github.com/google/automotive-design-compose/issues/272) (@timothyfroehlich)
- [#273: Create Gradle figmaFileFetch task, including bundling the Fetcher tool with the plugin.](https://github.com/google/automotive-design-compose/issues/273) (@timothyfroehlich)
- [#274: Connect the figmaFileFetch task to the output of the Codegen task, add DSL, update documentation.](https://github.com/google/automotive-design-compose/issues/274) (@timothyfroehlich)
- [#275: Create figmaFileCommit and figmaFileFetch andCommit tasks, create developer script to update all serialized files in the repository.](https://github.com/google/automotive-design-compose/issues/275) (@timothyfroehlich)
- [#278: Document using DesignCompose to create a traditional Android View](https://github.com/google/automotive-design-compose/issues/278) (@iamralpht)
- [#290: Cleanup rectangle / bounding box setup](https://github.com/google/automotive-design-compose/issues/290) (@kportl)
- [#561: Performance optimization for Rust layout implementation](https://github.com/google/automotive-design-compose/issues/561) (@iamralpht)
- [#604: Add support for baseline alignment](https://github.com/google/automotive-design-compose/issues/604) (@iamralpht)
- [#606: Investigate transition to Kotlin 2.0 and KSP 2.0](https://github.com/google/automotive-design-compose/issues/606) (@timothyfroehlich)
- [#688: Nightly Experimental builds failing](https://github.com/google/automotive-design-compose/issues/688) (@timothyfroehlich)
- [#690: Improve handling of AAOS Unbundled Libraries in GitHub Actions](https://github.com/google/automotive-design-compose/issues/690) (@timothyfroehlich)
- [#890: Use version catalog for exoplayer test dependency](https://github.com/google/automotive-design-compose/issues/890) (@iamralpht)
- [#905: Masks inside of variants do not render correctly](https://github.com/google/automotive-design-compose/issues/905) (@HooHaa-code)
- [#1368: More flexibility possible?](https://github.com/google/automotive-design-compose/issues/1368) (@StephanSchuster)
- [#1373: Create a better separation between toolkit_schema and figma_schema](https://github.com/google/automotive-design-compose/issues/1373) (@timothyfroehlich)
- [#1419: This would be a candidate for writing an `Into` implementation in figma_schema](https://github.com/google/automotive-design-compose/issues/1419) (@timothyfroehlich)
- [#1544: Improve error handling: propagate an error from Rust and show it in DesignSwitcher](https://github.com/google/automotive-design-compose/issues/1544) (@yiqunw700)
- [#1792: Squoosh enabled cannot find variable for components imported from library](https://github.com/google/automotive-design-compose/issues/1792) (@Heneveld)
- [#2138: RB HVAC investigation](https://github.com/google/automotive-design-compose/issues/2138) (@yiqunw700)
- [#2330: Layers with opacity start flickering when placed on another layer with opacity](https://github.com/google/automotive-design-compose/issues/2330) (@Elektrobit-HendrikCammann)

---

### [Category 3: Ping Author — No Progress / Check Relevance (26 Issues)](./03_ping_author_relevance.md)

Older backlog ideas, quality-of-life proposals, or unanswered community questions where maintainers should check if the feature is still needed, with ready-to-post draft comments:

- [#95: Add emulator tests to CI](https://github.com/google/automotive-design-compose/issues/95) (@timothyfroehlich)
- [#101: Automatically stage Release Candidates to gMaven](https://github.com/google/automotive-design-compose/issues/101) (@timothyfroehlich)
- [#103: Allow for removal of Live Update code from production app](https://github.com/google/automotive-design-compose/issues/103) (@timothyfroehlich)
- [#469: Separate the JNI crate into Live Update and Layout](https://github.com/google/automotive-design-compose/issues/469) (@timothyfroehlich)
- [#684: Configure Firebase Test Lab, add performance testing](https://github.com/google/automotive-design-compose/issues/684) (@timothyfroehlich)
- [#708: Modify Tutorial instructions to run "setFigmaTokenDebug" from Android Studio instead of the command line.](https://github.com/google/automotive-design-compose/issues/708) (@timothyfroehlich)
- [#709: Enhance "SetFigmaToken" to target the actively running Android Device](https://github.com/google/automotive-design-compose/issues/709) (@timothyfroehlich)
- [#710: Feature idea: Automatically add Compose test tags to all DesignCompose customizations](https://github.com/google/automotive-design-compose/issues/710) (@timothyfroehlich)
- [#711: Test library - Create examples of various tests](https://github.com/google/automotive-design-compose/issues/711) (@timothyfroehlich)
- [#712: Documentation - Document compile-time optimizations](https://github.com/google/automotive-design-compose/issues/712) (@timothyfroehlich)
- [#713: Tutorial - consider alternative instructions for loading your Figma Doc ID](https://github.com/google/automotive-design-compose/issues/713) (@timothyfroehlich)
- [#883: Consolidate JNI Library artifacts into a single Gradle module](https://github.com/google/automotive-design-compose/issues/883) (@timothyfroehlich)
- [#1118: Provide additional test cases for external renderer comparison.](https://github.com/google/automotive-design-compose/issues/1118) (@johnoneil)
- [#1295: Localization plugin UI polish](https://github.com/google/automotive-design-compose/issues/1295) (@yiqunw700)
- [#1446: Animate between design token values](https://github.com/google/automotive-design-compose/issues/1446) (@rylin8)
- [#1450: Begin tightly vetting Rust dependencies](https://github.com/google/automotive-design-compose/issues/1450) (@timothyfroehlich)
- [#1555: More support for text stroke](https://github.com/google/automotive-design-compose/issues/1555) (@yiqunw700)
- [#1568: Determine the plan for managing Rust dependencies](https://github.com/google/automotive-design-compose/issues/1568) (@timothyfroehlich)
- [#1721: Dropdown list,  Slider bar support](https://github.com/google/automotive-design-compose/issues/1721) (@BIN-KOUHINN)
- [#2145: RTL support](https://github.com/google/automotive-design-compose/issues/2145) (@yiqunw700)
- [#2183: Investigate improvements for testing for nodes](https://github.com/google/automotive-design-compose/issues/2183) (@rylin8)
- [#2188: Add support for insets in the Scalable UI plugin](https://github.com/google/automotive-design-compose/issues/2188) (@rylin8)
- [#2189: Add support for rounded corners in Scalable UI](https://github.com/google/automotive-design-compose/issues/2189) (@rylin8)
- [#2190: Allow progress bar and indicator preview simultaneously](https://github.com/google/automotive-design-compose/issues/2190) (@rylin8)
- [#2208: font family variables support](https://github.com/google/automotive-design-compose/issues/2208) (@farivar-google)
- [#2245: Scope Figma Draw features](https://github.com/google/automotive-design-compose/issues/2245) (@iamralpht)

---

### [Category 4: Actionable Bugs & Enhancements — Requires Google Response or Code Fix (26 Issues)](./04_actionable_bugs_and_enhancements.md)

Confirmed bugs, missing architectural capabilities, documentation gaps, and high-value customer feature requests. Each issue has a dedicated engineering specification document:

- [#120: Re-make doc page for Custom Fonts](./issues/issue-120.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/120) | [Engineering Spec](./issues/issue-120.md)
- [#121: Re-make doc page on Figma Interactions](./issues/issue-121.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/121) | [Engineering Spec](./issues/issue-121.md)
- [#395: Layout is broken on autolayout frames that are rotated](./issues/issue-395.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/395) | [Engineering Spec](./issues/issue-395.md)
- [#398: Text alignment with multiple lines is incorrect](./issues/issue-398.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/398) | [Engineering Spec](./issues/issue-398.md)
- [#551: DesignCompose does not download images larger than a certain size](./issues/issue-551.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/551) | [Engineering Spec](./issues/issue-551.md)
- [#573: Make Brush function customization accept an `ImageContext`](./issues/issue-573.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/573) | [Engineering Spec](./issues/issue-573.md)
- [#635: Allow `fetch` to get its nodes list from the JSON emitted by the KSP processor](./issues/issue-635.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/635) | [Engineering Spec](./issues/issue-635.md)
- [#649: Provide a means for users to access the Validation docs](./issues/issue-649.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/649) | [Engineering Spec](./issues/issue-649.md)
- [#661: Improve documentation on finding and using the generated JSON file for Lists & Grids](./issues/issue-661.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/661) | [Engineering Spec](./issues/issue-661.md)
- [#673: Support scale constraints with vectors](./issues/issue-673.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/673) | [Engineering Spec](./issues/issue-673.md)
- [#954: Enable DesignModule usage across Gradle projects](./issues/issue-954.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/954) | [Engineering Spec](./issues/issue-954.md)
- [#1031: Vectors in Serialized Documents Don't Provide all Colors](./issues/issue-1031.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/1031) | [Engineering Spec](./issues/issue-1031.md)
- [#1117: ImageContext does not provide useful data when node constraints set to scale](./issues/issue-1117.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/1117) | [Engineering Spec](./issues/issue-1117.md)
- [#1138: Custom Design Modules must be in same directory as Generated Composable](./issues/issue-1138.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/1138) | [Engineering Spec](./issues/issue-1138.md)
- [#1169: Support duplicate variable names across multiple documents](./issues/issue-1169.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/1169) | [Engineering Spec](./issues/issue-1169.md)
- [#1299: Control Figma UI prototype state in code](./issues/issue-1299.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/1299) | [Engineering Spec](./issues/issue-1299.md)
- [#1300: Drag-Drop implementation](./issues/issue-1300.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/1300) | [Engineering Spec](./issues/issue-1300.md)
- [#1331: DesignVariant not working with figma component instances](./issues/issue-1331.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/1331) | [Engineering Spec](./issues/issue-1331.md)
- [#1690: Figma Collections, Modes, Variables not available for query from design compose.](./issues/issue-1690.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/1690) | [Engineering Spec](./issues/issue-1690.md)
- [#1798: Improvements for DesignSwitcher](./issues/issue-1798.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/1798) | [Engineering Spec](./issues/issue-1798.md)
- [#1814: Better support for RRO](./issues/issue-1814.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/1814) | [Engineering Spec](./issues/issue-1814.md)
- [#2113: Component overrides next](./issues/issue-2113.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/2113) | [Engineering Spec](./issues/issue-2113.md)
- [#2181: Customizations are not propagated to replacement component children](./issues/issue-2181.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/2181) | [Engineering Spec](./issues/issue-2181.md)
- [#2261: Same Figma node names do not execute smart animations.](./issues/issue-2261.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/2261) | [Engineering Spec](./issues/issue-2261.md)
- [#2401: Support Figma OAuth 2.0 Token Authentication in Addition to Personal Access Tokens](./issues/issue-2401.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/2401) | [Engineering Spec](./issues/issue-2401.md)
- [#2456: Cannot apply Android Modifier-based animations to child components; works only via ComponentReplacementContext](./issues/issue-2456.md) — [GitHub Issue](https://github.com/google/automotive-design-compose/issues/2456) | [Engineering Spec](./issues/issue-2456.md)

---

## Repository Artifacts Generated

- [`open_issues.json`](./open_issues.json): Complete raw JSON dump of all 95 open issues and comments.

- [`issues_digest.md`](./issues_digest.md): Full markdown digest of all 95 issues with complete bodies and comments.

- [`git_history.txt`](./git_history.txt): Git status, branch topology, release tags up to `v0.39.6`, and commit log.

- [`01_close_stale_no_response.md`](./01_close_stale_no_response.md): Category 1 issues with ready-to-post closing comments.

- [`02_close_already_fixed.md`](./02_close_already_fixed.md): Category 2 issues with commit hashes, PRs, tags, and closing comments.

- [`03_ping_author_relevance.md`](./03_ping_author_relevance.md): Category 3 issues with current context and ready-to-post ping comments.

- [`04_actionable_bugs_and_enhancements.md`](./04_actionable_bugs_and_enhancements.md): Category 4 master table and grouped technical summaries.

- [`issues/`](./issues/): Directory containing 26 individual technical issue specs (`issue-120.md` through `issue-2456.md`).
