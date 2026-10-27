# Category 1: Close Right Away — Stale / Awaiting Author Response / Answered

**Total Issues:** 16  
**Criteria:** Maintainer provided an answer/workaround 3+ months ago without follow-up, author confirmed resolution/workaround, or empty/stale backlog ticket with no progress.

---

## Issue #93: Add method to input Figma Design Token via Design Switcher

- **URL:** [https://github.com/google/automotive-design-compose/issues/93](https://github.com/google/automotive-design-compose/issues/93)
- **Author:** @timothyfroehlich
- **Created:** `2023-05-31T21:27:20Z` | **Last Updated:** `2026-03-17T16:38:39Z`
- **Why Close Right Away:** Empty feature idea placeholder filed by @timothyfroehlich in May 2023 with no description. On 2024-06-20 the author commented 'Need a better description' and no further details were provided for over 2 years. Entering Figma design tokens via on-device UI is also a security concern on shared automotive targets, and token files/ADB broadcasts are the standard workflow.

### Ready-to-Post GitHub Closing Comment

```markdown
Hi @timothyfroehlich, closing this issue as stale. This placeholder has had no updates since the June 2024 note requesting a description. Figma tokens are securely configured via `~/.config/figma_access_token` and ADB broadcast (`./gradlew setFigmaToken`). Please feel free to reopen if this in-app UI capability is still needed with a specific design.
```

---

## Issue #97: Create method for setting the Figma Token once per device

- **URL:** [https://github.com/google/automotive-design-compose/issues/97](https://github.com/google/automotive-design-compose/issues/97)
- **Author:** @timothyfroehlich
- **Created:** `2023-05-31T21:30:59Z` | **Last Updated:** `2026-03-17T16:38:30Z`
- **Why Close Right Away:** Empty placeholder issue filed in May 2023. On 2024-06-20 the author commented 'Needs better description' and no activity has occurred since. Device-wide token configuration is already achievable via `./gradlew setFigmaToken` or setting Android system properties across all installed apps.

### Ready-to-Post GitHub Closing Comment

```markdown
Hi @timothyfroehlich, closing this issue as stale since there have been no updates or requirements added since June 2024. Device-wide token propagation is currently handled via `./gradlew setFigmaToken` or system properties. Please reopen if you wish to pursue a different mechanism.
```

---

## Issue #112: "Favorite docs" - Add method for setting alternate Figma Files to load in the Design Switcher

- **URL:** [https://github.com/google/automotive-design-compose/issues/112](https://github.com/google/automotive-design-compose/issues/112)
- **Author:** @timothyfroehlich
- **Created:** `2023-06-06T21:22:09Z` | **Last Updated:** `2023-06-21T21:09:07Z`
- **Why Close Right Away:** Filed in June 2023 requesting a way to switch alternate Figma files in Design Switcher. Maintainer Ralph noted in June 2023 that Figma file branches allow switching between alternate files/versions without rebuilds. Design Switcher also now provides a direct text field to enter alternate Figma file IDs at runtime.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as resolved. The Design Switcher now supports switching between Figma file branches and entering arbitrary alternate Figma File IDs dynamically at runtime (see [Design Switcher documentation](https://google.github.io/automotive-design-compose/docs/live-update/design-switcher)). Please feel free to open a new issue if additional favorite bookmarking features are needed.
```

---

## Issue #132: Reorganize docs into a new set of categories

- **URL:** [https://github.com/google/automotive-design-compose/issues/132](https://github.com/google/automotive-design-compose/issues/132)
- **Author:** @timothyfroehlich
- **Created:** `2023-06-22T17:23:22Z` | **Last Updated:** `2023-06-29T18:48:09Z`
- **Why Close Right Away:** Filed in June 2023 proposing a reorganization of documentation categories. Assigned to Miriam Vu with no activity since June 2023 (>3 years). The documentation hierarchy has long since stabilized on `just-the-docs` structure (Tutorial, Live Update, Modifiers, Variables, Localization, Shaders, Working with Source).

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as stale. The documentation navigation has settled into our current Jekyll structure (Tutorial, Live Update, Modifiers, Variables, Shaders, etc.). If you have specific structural changes or category suggestions in the future, please open a fresh issue or PR.
```

---

## Issue #136: Impossible to fetch node under intersection/union

- **URL:** [https://github.com/google/automotive-design-compose/issues/136](https://github.com/google/automotive-design-compose/issues/136)
- **Author:** @elouannf
- **Created:** `2023-06-23T13:52:30Z` | **Last Updated:** `2024-05-08T20:48:43Z`
- **Why Close Right Away:** Reported inability to fetch/customize text nodes nested under boolean union/intersection operations. Maintainer Ralph explained that Figma's REST API flattens boolean operation children into vector paths, and suggested mask layers as a workaround. On 2023-08-14, author @elouannf confirmed the workaround worked: 'I was able to make it work in another way... it's not very important because I was able to make these changes anyway.' No activity in over 3 years.

### Ready-to-Post GitHub Closing Comment

```markdown
Hi @elouannf, closing this issue as resolved. As noted, Figma's REST API flattens boolean operation hierarchies into vector geometry, but mask layers and component replacements provide the supported workaround which you confirmed met your requirements. Thanks for using DesignCompose!
```

---

## Issue #225: Live update stops working with frames containing large number of layers

- **URL:** [https://github.com/google/automotive-design-compose/issues/225](https://github.com/google/automotive-design-compose/issues/225)
- **Author:** @FruitSnack1
- **Created:** `2023-07-18T06:45:04Z` | **Last Updated:** `2024-05-29T20:48:49Z`
- **Why Close Right Away:** Reported live update taking minutes when pasting a frame with 389 layers. The author @FruitSnack1 investigated and found it was caused by fetching hundreds of nested variant components from external shared libraries. On 2023-08-10, the author reported a working solution: 'Without all the components and variants from different files, live update works normal and updates in matter of seconds... Just figured out a quick solution. In Figma you can detach all instances.' Furthermore, performance was optimized in PR #2162 (caching remote components) and PR #2419 (logging expensive fetches). No activity since August 2023.

### Ready-to-Post GitHub Closing Comment

```markdown
Hi @FruitSnack1, closing as resolved. As you identified, the delay was due to recursive fetches of nested external library components, which resolved after detaching or scoping library dependencies. We have also merged component caching optimizations in PR #2162 and high-cost fetch telemetry in PR #2419. Thank you for reporting!
```

---

## Issue #250: Replace all references to Figma "documents" with "file"in the source code / API

- **URL:** [https://github.com/google/automotive-design-compose/issues/250](https://github.com/google/automotive-design-compose/issues/250)
- **Author:** @timothyfroehlich
- **Created:** `2023-07-28T14:53:42Z` | **Last Updated:** `2023-08-09T20:18:24Z`
- **Why Close Right Away:** P3 suggestion from July 2023 to replace the word 'document' with 'file' throughout the codebase. `@DesignDoc`, `DocServer`, `DocFetcher`, and `doc_id` are deeply rooted in public APIs, annotations, Gradle plugins, and Rust crates. Making this rename would cause massive breaking changes for all users and partners without functional benefit.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as won't fix (not planned). `@DesignDoc` and related `Doc*` terminology are core parts of the public API and serialization contracts. Renaming them to 'file' would introduce breaking changes across existing partner applications and libraries without functional benefit. Thank you for the proposal!
```

---

## Issue #282: Consider making an allowlist for recursive doc fetches

- **URL:** [https://github.com/google/automotive-design-compose/issues/282](https://github.com/google/automotive-design-compose/issues/282)
- **Author:** @iamralpht
- **Created:** `2023-08-09T20:58:20Z` | **Last Updated:** `2023-08-23T20:11:10Z`
- **Why Close Right Away:** Discussion ticket from August 2023 asking whether an allowlist should be created for recursive document fetching. Instead of an allowlist, the team implemented component deduplication and caching in PR #2162 ('Speed up fetch of components in other files') and diagnostic logging in PR #2419 ('Add performance logging for high-cost fetch operations'), eliminating redundant downloads without requiring manual allowlist configuration.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as superseded. Rather than requiring users to maintain a manual document allowlist, we implemented component caching and deduplication in PR #2162 and high-cost fetch logging in PR #2419 to prevent redundant recursive downloads automatically.
```

---

## Issue #628: Galaxy design crashes emulators on APIs greater than 33

- **URL:** [https://github.com/google/automotive-design-compose/issues/628](https://github.com/google/automotive-design-compose/issues/628)
- **Author:** @timothyfroehlich
- **Created:** `2023-12-13T21:52:28Z` | **Last Updated:** `2026-03-17T16:36:35Z`
- **Why Close Right Away:** Filed in Dec 2023 regarding emulator crashes on API > 33 with Galaxy/Shiny designs. On 2024-01-10, @timothyfroehlich commented: 'The Shiny design was most consistent in causing a crash. May have fixed, attempt to reproduce.' No crash or reproduction has been reported since (nearly 3 years). All modern CI builds run on API 34/35 without emulator GPU crashes.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as cannot reproduce / resolved. Following the emulator and shader fixes in early 2024, no further crashes have been observed on Android API 34+ (current CI runs on API 35 cleanly). Please reopen if you encounter this on a specific emulator image.
```

---

## Issue #739: Design Component must be within the main document Figma interface

- **URL:** [https://github.com/google/automotive-design-compose/issues/739](https://github.com/google/automotive-design-compose/issues/739)
- **Author:** @Heneveld
- **Created:** `2024-01-23T13:30:23Z` | **Last Updated:** `2024-06-07T17:48:18Z`
- **Why Close Right Away:** User requested interface inheritance to share components across DesignDoc interfaces. Maintainer @rylin8 investigated on branch `wb/rylin/interface-inheritance` (commit `a6476307`) but determined interface inheritance was brittle. Instead, the team designed and implemented DesignModules (`@DesignModuleClass` and `@DesignModule`) in PR #906 (commit `c6635350`, release v0.26.0) and PR #1010, providing the official modular composition mechanism.

### Ready-to-Post GitHub Closing Comment

```markdown
Hi @Heneveld, closing as resolved. While direct interface inheritance was explored on branch `wb/rylin/interface-inheritance`, the team concluded that composition via DesignModules was the cleaner architectural pattern. DesignModules was shipped in PR #906 (`@DesignModuleClass` and `@DesignModule`) in release v0.26.0 and is the recommended way to share and reuse component definitions across interfaces.
```

---

## Issue #840: Variant prototype not working when the elements come from a library

- **URL:** [https://github.com/google/automotive-design-compose/issues/840](https://github.com/google/automotive-design-compose/issues/840)
- **Author:** @MarcFerrerVWGS
- **Created:** `2024-03-04T08:26:33Z` | **Last Updated:** `2024-06-07T17:48:19Z`
- **Why Close Right Away:** User reported variant prototype interaction not switching states when components came from a library. In Comment #11 (2024-03-20), maintainer @rylin8 analyzed the user's Figma setup and explained the issue: the library component defined 4 variant properties (`category`, `configuration`, `state`, `emphasis_level`), but the Kotlin code only declared `@DesignVariant` for `state`, leaving variant resolution ambiguous. In Comment #12 (2024-03-21), author @MarcFerrerVWGS acknowledged the explanation and shifted to discussing proxy connection issues. No further questions have been asked in over 2.5 years.

### Ready-to-Post GitHub Closing Comment

```markdown
Hi @MarcFerrerVWGS, closing this issue as answered. As explained in the thread, when a Figma component set uses multiple variant properties, all variant axes must be declared or defaulted so DesignCompose can resolve the exact variant node. If you have any ongoing questions, please feel free to open a new issue.
```

---

## Issue #1167: How to scale the Figma objects in the emulator based on the screen DPI

- **URL:** [https://github.com/google/automotive-design-compose/issues/1167](https://github.com/google/automotive-design-compose/issues/1167)
- **Author:** @spkdroid
- **Created:** `2024-05-28T15:16:48Z` | **Last Updated:** `2024-06-07T17:47:56Z`
- **Why Close Right Away:** User asked how to scale Figma objects on emulators based on screen DPI. On 2024-05-29, maintainer @iamralpht provided a comprehensive answer: DesignCompose automatically scales coordinates to density pixels (dp) based on the device DPI, and if additional custom scaling is desired, standard Jetpack Compose `Modifier.scale()` can be applied to the composable. The author never replied or asked follow-up questions for over 2.5 years.

### Ready-to-Post GitHub Closing Comment

```markdown
Hi @spkdroid, closing as answered. As noted by Ralph, DesignCompose automatically renders Figma designs in density-independent pixels (dp) according to the display DPI. Additional manual scaling can be achieved with standard Compose `Modifier.scale()`. Please feel free to open a new issue if you need further help!
```

---

## Issue #1170: Scaling two separates instances of a component will always take the original size

- **URL:** [https://github.com/google/automotive-design-compose/issues/1170](https://github.com/google/automotive-design-compose/issues/1170)
- **Author:** @Heneveld
- **Created:** `2024-05-29T19:22:22Z` | **Last Updated:** `2024-06-07T17:47:57Z`
- **Why Close Right Away:** User reported that scaling two separate instances of a component with vector ellipse shapes resulted in the original size. On 2024-06-06, maintainer @rylin8 explained that vector scale constraints are tracked in #673 and provided an immediate working design workaround: using a frame with large corner radius instead of vector paths, which scales properly under all constraints. The author never responded in over 2.5 years.

### Ready-to-Post GitHub Closing Comment

```markdown
Hi @Heneveld, closing this issue as answered. The underlying vector path scaling constraint is tracked in #673, and the recommended solution (using frames with 50% / large corner radius instead of path vectors) allows full responsive scaling across arbitrary component instances. Thank you!
```

---

## Issue #1187: Fill with image not showing on the emulator

- **URL:** [https://github.com/google/automotive-design-compose/issues/1187](https://github.com/google/automotive-design-compose/issues/1187)
- **Author:** @MarcFerrerVWGS
- **Created:** `2024-06-03T06:25:44Z` | **Last Updated:** `2024-06-07T17:47:57Z`
- **Why Close Right Away:** User reported image fills not appearing in their emulator. In Comment #5 (2024-06-05), maintainer @rylin8 downloaded and tested the user's Figma file, verifying that images rendered correctly and attaching screenshot proof. In Comment #6 (2024-06-06), the author confirmed device logs showed `Decoded 1 images for test_images` and `Save doc success`, indicating the issue was caused by their local emulator proxy/network environment. No follow-up in over 2.5 years.

### Ready-to-Post GitHub Closing Comment

```markdown
Hi @MarcFerrerVWGS, closing as resolved. As verified during testing, image fills in the sample document render correctly, and your logs confirmed successful image decode and save once local network/proxy access to Figma CDN was established. Please reopen if you observe this again.
```

---

## Issue #1994: Make component library with DC

- **URL:** [https://github.com/google/automotive-design-compose/issues/1994](https://github.com/google/automotive-design-compose/issues/1994)
- **Author:** @rylin8
- **Created:** `2025-01-14T00:03:29Z` | **Last Updated:** `2026-04-09T23:07:36Z`
- **Why Close Right Away:** Backlog test task proposing to create a component library from Material 3 Figma kit. On 2026-04-09, maintainer @anshu10sep commented: 'What is the requirement for this feature? Can you provide some additional details'. Over 6 months (>3 months) have passed without any reply or clarification from the author.

### Ready-to-Post GitHub Closing Comment

```markdown
Closing as stale due to lack of response. We requested clarification on the requirements in April 2026 without follow-up. Please feel free to reopen with specific use cases if this is still desired.
```

---

## Issue #2332: Component Instances are not render correctly until detached

- **URL:** [https://github.com/google/automotive-design-compose/issues/2332](https://github.com/google/automotive-design-compose/issues/2332)
- **Author:** @Elektrobit-HendrikCammann
- **Created:** `2025-11-13T18:29:53Z` | **Last Updated:** `2026-04-09T22:59:36Z`
- **Why Close Right Away:** Reported that component instances rendered incorrectly until detached in v0.38.0. On 2026-04-09, maintainer @anshu10sep requested reproduction info: 'Can you please provide additional information and maybe a .fig file or a test app to repro this with step-by-step instructions to validate it'. More than 6 months have passed without reply or sample provided. Additionally, multiple component instance variant resolution bugs were fixed in PR #2469, PR #2478, and PR #2507.

### Ready-to-Post GitHub Closing Comment

```markdown
Hi @Elektrobit-HendrikCammann, closing as stale. We requested a reproduction sample in April 2026 without follow-up. In the meantime, several fixes for component instance variant and style resolution have landed in v0.39.0+ (PR #2469, PR #2478, and PR #2507). If you still encounter any rendering discrepancy on the latest release, please feel free to provide a sample and reopen!
```

---
