# Issue #1690: Figma Collections, Modes, Variables not available for query from design compose

## 1. Issue Metadata
- **Issue #:** 1690
- **Title:** Figma Collections, Modes, Variables not available for query from design compose
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/1690](https://github.com/google/automotive-design-compose/issues/1690)
- **Author:** @Heneveld
- **Created Date:** 2024-10-02T16:31:08Z
- **Updated Date:** 2024-11-18T21:58:41Z
- **Labels:** `Type: Customer Issue`
- **Priority / Type:** Customer Feature Request / Variables & Theming API
- **Status:** Open (Actionable Variable Query API Enhancement)

---

## 2. Problem Statement & User Impact
Figma Variables allow designers to organize design tokens into Collections (e.g. `BrandColors`, `Spacing`, `Typography`) and Modes (e.g. `Light`, `Dark`, `HighContrast`).

While DesignCompose parses variables from Figma files and resolves them during node styling, it currently provides **no public runtime API** for Android applications to query this metadata:
- Developers cannot discover which variable Collections and Modes exist in a document.
- Developers cannot programmatically query the current value of a variable (e.g., getting the primary brand color to tint native Android system UI bars or Compose dialogs).
- Developers cannot switch Modes programmatically (e.g., toggling between Day and Night themes) without internal hacking.
- Teams building dynamic automotive themes cannot synchronize native Android Jetpack Compose Material themes with Figma design tokens.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`designcompose/src/main/java/com/android/designcompose/VariableManager.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/VariableManager.kt#L160-L220)
  - [`designcompose/src/main/java/com/android/designcompose/DocServer.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/DocServer.kt#L119-L160)
  - [`designcompose/src/main/proto/designcompose_definition.proto`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/proto/designcompose_definition.proto)

- **Detailed Root Cause:**
  1. In `designcompose_definition.proto`, `VariableCollection` and `Variable` message definitions are serialized from Figma and loaded into memory by `DocServer`.
  2. In [`VariableManager.kt:160-220`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/VariableManager.kt#L160-L220), `VariableManager` maintains `varMap`, but all query methods are marked `internal` or package-private.
  3. No public interface or `CompositionLocal` (e.g. `LocalDesignVariables` or `DesignVariables.query()`) exists to expose:
     - `availableCollections(docId: String): List<VariableCollection>`
     - `availableModes(collectionId: String): List<String>`
     - `getVariableColor(name: String, mode: String?): Color?`
     - `setMode(collectionName: String, modeName: String)`

---

## 4. Proposed Fix / Implementation Plan
1. **Expose Public Query & Control API on `DesignSettings` / `VariableManager`:**
   In [`VariableManager.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/VariableManager.kt):
   ```kotlin
   object DesignVariables {
       fun getCollections(docId: String): List<String>
       fun getModes(docId: String, collectionName: String): List<String>
       fun getColor(docId: String, variableName: String, mode: String? = null): Color?
       fun getFloat(docId: String, variableName: String, mode: String? = null): Float?
       fun setMode(docId: String, collectionName: String, modeName: String)
   }
   ```
2. **Provide Compose State Binding:**
   Add a `@Composable fun rememberDesignVariableMode(collection: String): MutableState<String>` that triggers recomposition when the active Figma variable mode is switched.

---

## 5. Validation & Reproduction Plan
1. **Unit Test:**
   In `VariableManagerTest.kt`, load a serialized `.dcf` file containing `Light` and `Dark` modes. Verify `DesignVariables.getColor("surface", "Dark")` returns the dark hex color.
2. **Interactive Demo App:**
   In `reference-apps/tutorial`, build a settings screen with a radio group allowing the user to switch between `Light` and `Dark` Figma modes dynamically.

---

## 6. Drafted GitHub Comment
```markdown
### Proposed Feature Architecture for Issue #1690:

To enable querying and switching Figma Variables, Collections, and Modes at runtime, we propose adding a public `DesignVariables` API:

```kotlin
// Query available modes:
val modes = DesignVariables.getModes(docId, "Theme") // ["Light", "Dark"]

// Get design token color:
val primaryColor = DesignVariables.getColor(docId, "Color/Primary")

// Switch active mode:
DesignVariables.setMode(docId, "Theme", "Dark")
```
This allows native Jetpack Compose components and status bars to stay synchronized with DesignCompose tokens.
```
