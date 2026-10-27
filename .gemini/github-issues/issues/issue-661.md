# Issue #661: Improve documentation on finding and using the generated JSON file for Lists & Grids

## 1. Issue Metadata
- **Issue #:** 661
- **Title:** Improve documentation on finding and using the generated JSON file for Lists & Grids
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/661](https://github.com/google/automotive-design-compose/issues/661)
- **Author:** @iamralpht
- **Created Date:** 2023-12-21T22:35:52Z
- **Updated Date:** 2024-01-03T21:05:35Z
- **Labels:** `Type: Customer Issue`
- **Priority / Type:** Documentation / Customer QoL
- **Status:** Open (Actionable Documentation Enhancement)

---

## 2. Problem Statement & User Impact
DesignCompose supports dynamic lists and grids via the `@DesignListContent` and `@DesignGridContent` annotations. To enable Figma designers to preview how dynamic lists will look inside Figma, DesignCompose provides an "Auto Content Preview" Figma plugin.

The plugin requires a JSON file generated during the Android compilation process. In [`docs/_docs/modifiers/modifiers.md`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/modifiers/modifiers.md#L621-L750), the documentation mentions this JSON file but lacks crucial details:
- What is the exact path and filename of the generated JSON file in the Android Gradle `build/` directory?
- Which Gradle task generates it (e.g. `kspDebugKotlin`)?
- Step-by-step instructions for loading the JSON file into the Figma plugin.
- Troubleshooting tips if the JSON file is missing or fails to generate.

Users are unable to use the Auto Content Preview plugin without searching through Gradle build artifacts manually.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`docs/_docs/modifiers/modifiers.md`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/modifiers/modifiers.md#L621-L750)
  - [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt)

- **Detailed Root Cause:**
  In `BuilderProcessor.kt`, the KSP processor generates the list schema JSON:
  `build/generated/ksp/<variant>/resources/com/android/designcompose/codegen/` or in the build assets directory.
  Because the documentation in `modifiers.md` only states *"upload the generated JSON file"* without providing the build path, developers frequently get stuck trying to find it.

---

## 4. Proposed Fix / Implementation Plan
1. **Update `docs/_docs/modifiers/modifiers.md`:**
   Add a clear, highlighted callout box under the **Creating Lists** section:
   ```markdown
   > [!TIP]
   > ### Locating the Auto Content Preview JSON File
   > After building your project with `./gradlew compileDebugKotlin` or running your app:
   > 1. Locate the generated JSON file at:
   >    `<module>/build/generated/ksp/<flavor>Debug/resources/com/android/designcompose/codegen/<DocumentName>_content.json`
   > 2. Open Figma and run the **DesignCompose Auto Content Preview** plugin.
   > 3. Click **Upload JSON** and select the file above.
   > 4. Select your List component in Figma to preview live populated content directly on the canvas!
   ```
2. **Add Troubleshooting Step:**
   Explain that if the file is not found, verify that `@DesignListContent` is applied and that KSP has run successfully.

---

## 5. Validation & Reproduction Plan
1. **Build Verification:**
   Run `./gradlew :reference-apps:tutorial:kspDebugKotlin` and verify the exact output path of the generated JSON file.
2. **Docs Verification:**
   Verify formatting of the updated `modifiers.md` in Jekyll.

---

## 6. Drafted GitHub Comment
```markdown
### Documentation Update for Lists & Grids JSON:

We have added explicit instructions to `docs/_docs/modifiers/modifiers.md` detailing how to find and upload the generated JSON file:

1. **Build your project:** Run `./gradlew compileDebugKotlin` (or `:app:kspDebugKotlin`).
2. **File Location:** The JSON is emitted to:
   `app/build/generated/ksp/debug/resources/com/android/designcompose/codegen/<DocName>_content.json`
3. **Upload to Figma:** Open the DesignCompose Auto Content Preview plugin in Figma, click **Upload JSON**, and select the file.

A PR updating the documentation has been prepared.
```
