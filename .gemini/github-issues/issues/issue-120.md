# Issue #120: Re-make doc page for Custom Fonts

## 1. Issue Metadata
- **Issue #:** 120
- **Title:** Re-make doc page for Custom Fonts
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/120](https://github.com/google/automotive-design-compose/issues/120)
- **Author:** @timothyfroehlich
- **Created Date:** 2023-06-13T17:49:34Z
- **Updated Date:** 2023-06-21T21:09:34Z
- **Labels:** `documentation`, `:blue_square:  P3`
- **Priority / Type:** P3 / Documentation Task (Internal Ref: `b/250922834`)
- **Status:** Open (Actionable Documentation Enhancement)

---

## 2. Problem Statement & User Impact
When developers integrate Figma files with custom fonts into DesignCompose, text elements will silently fallback to system default fonts (Roboto) unless the custom font families are explicitly mapped and registered in Android. 

Prior to the documentation refactoring, a dedicated page guided developers through registering font families with `DesignSettings.addFontFamily()`. Currently, while an image asset [`docs/_docs/modifiers/font-resources.png`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/modifiers/font-resources.png) exists, there is no standalone documentation guide under [`docs/_docs/`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/) explaining:
1. How Figma font names correspond to Android Compose `FontFamily` definitions.
2. How and where in the Android application lifecycle to call `DesignSettings.addFontFamily()`.
3. How to package font files into `res/font/` or load Google Fonts asynchronously using Compose's downloadable fonts API.
4. How font weight and font style fallbacks behave when a specific Figma variant (e.g., SemiBold 600 Italic) is missing from the font family.

This causes developer confusion, visual mismatch between Figma and the Android app, and frequent inquiries on how custom typography should be configured.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`designcompose/src/main/java/com/android/designcompose/DocServer.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/DocServer.kt#L119-L265)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshText.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshText.kt#L70-L135)
  - [`docs/_docs/`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/) (Missing `custom-fonts.md`)

- **Implementation Mechanism:**
  In [`DocServer.kt:119`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/DocServer.kt#L119), `DesignSettings` maintains an internal dictionary:
  ```kotlin
  private var fontDb: HashMap<String, FontFamily> = HashMap()
  ```
  Fonts are registered via:
  ```kotlin
  fun addFontFamily(name: String, family: FontFamily) {
      fontDb[name] = family
  }
  ```
  During text rendering in [`SquooshText.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshText.kt), the node's font family name string serialized from Figma is looked up in `fontDb`. If not found, a warning is logged and the text falls back to `FontFamily.Default`:
  ```kotlin
  val family = fontFamily?.let { fontDb[fontFamily] }
  ```
  The lack of documentation leaves users unaware of `DesignSettings.addFontFamily()`, leading to broken typographic styling.

---

## 4. Proposed Fix / Implementation Plan
1. **Create Documentation Page:**
   Author a new guide [`docs/_docs/custom-fonts.md`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/custom-fonts.md) and link it in the documentation navigation under `DesignCompose Basics`.
2. **Key Contents to Include:**
   - **Step 1: Adding Fonts to Android:** Place `.ttf` or `.otf` font files in `app/src/main/res/font/` (e.g., `carter_one.ttf`, `inter_regular.ttf`, `inter_bold.ttf`).
   - **Step 2: Defining `FontFamily` in Compose:**
     ```kotlin
     val InterFontFamily = FontFamily(
         Font(R.font.inter_regular, FontWeight.Normal),
         Font(R.font.inter_bold, FontWeight.Bold)
     )
     ```
   - **Step 3: Registering with `DesignSettings`:**
     In the `Application` class or `MainActivity.onCreate()` before calling any `@DesignDoc` composables:
     ```kotlin
     DesignSettings.addFontFamily("Inter", InterFontFamily)
     ```
   - **Step 4: Naming Match Rules:** Explain that the string passed to `addFontFamily()` must exactly match the `fontFamily` name used in Figma (case-sensitive).
   - **Step 5: Google Downloadable Fonts:** Provide a sample using `GoogleFont.Provider` for downloadable web fonts.
3. **Update Sidebar Index:**
   Add `custom-fonts.md` to `docs/_data/navigation.yml` (or the Jekyll doc layout index).

---

## 5. Validation & Reproduction Plan
1. **Documentation Verification:**
   Run the Jekyll documentation site locally:
   ```bash
   cd docs
   bundle exec jekyll serve
   ```
   Verify that the "Custom Fonts" page renders cleanly, syntax highlighting is correct, and cross-references link to `DesignSettings`.
2. **Code Validation:**
   Verify against [`designcompose/src/test/kotlin/com/android/designcompose/DesignSettingsTest.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/test/kotlin/com/android/designcompose/DesignSettingsTest.kt) to confirm font registration unit tests pass.

---

## 6. Drafted GitHub Comment
```markdown
We have drafted a dedicated documentation page for Custom Fonts in DesignCompose. 

### Summary of How to Use Custom Fonts:
1. **Include Font Files:** Place your `.ttf`/`.otf` font files in `app/src/main/res/font/`.
2. **Build `FontFamily`:**
   ```kotlin
   val Poppins = FontFamily(
       Font(R.font.poppins_regular, FontWeight.Normal),
       Font(R.font.poppins_bold, FontWeight.Bold)
   )
   ```
3. **Register Font:**
   Register the family before loading any DesignCompose views (e.g. in your `Application.onCreate` or `MainActivity.onCreate`):
   ```kotlin
   DesignSettings.addFontFamily("Poppins", Poppins)
   ```
   *Note:* The name string must match the font family name defined in Figma.

A PR adding `docs/_docs/custom-fonts.md` will be submitted to add this guide to the official documentation site.
```
