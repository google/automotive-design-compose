# Issue #1814: Better support for RRO

## 1. Issue Metadata
- **Issue #:** 1814
- **Title:** Better support for RRO
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/1814](https://github.com/google/automotive-design-compose/issues/1814)
- **Author:** @rylin8
- **Created Date:** 2024-11-27T19:29:47Z
- **Updated Date:** 2025-04-21T17:24:42Z
- **Labels:** *None*
- **Priority / Type:** Android Automotive OEM / Theming & Architecture
- **Status:** Open (Actionable RRO Dynamic Invalidation Enhancement)

---

## 2. Problem Statement & User Impact
Android Automotive OS (AAOS) heavily relies on **Runtime Resource Overlays (RRO)** to customize system apps (SystemUI, HVAC, Media) for different vehicle trims, OEM brands, and screen configurations without recompiling app binaries.

In PR #1778 and #1807, DesignCompose added `DesignSettings.setRawResourceId(docId, resId)` so apps can load `.dcf` serialized documents directly from Android `R.raw.*` resources.
However, when an RRO overlay is enabled or updated dynamically at runtime (e.g., via `adb shell cmd overlay enable <overlay_package>` or vehicle mode change):
- `DocServer` permanently caches the previously loaded document in its in-memory cache.
- The app continues rendering the old design until the entire Android application process is killed.
- Furthermore, raw resource lookup does not trigger Compose recomposition when configuration changes occur (`onConfigurationChanged`).

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`designcompose/src/main/java/com/android/designcompose/DocServer.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/DocServer.kt#L166-L195)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt)

- **Detailed Root Cause:**
  In [`DocServer.kt:166-177`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/DocServer.kt#L166-L177):
  ```kotlin
  fun setRawResourceId(docId: String, resId: Int) {
      rawResourceIdMap[docId] = resId
  }
  ```
  Once the resource is opened via `context.resources.openRawResource(resId)` and parsed into a `DesignDoc`, it is cached in `docCache: HashMap<String, DesignDoc>`.
  There is no API to invalidate this cache, nor does `DocServer` register an Android `ComponentCallbacks2` or `BroadcastReceiver` listening for overlay or configuration change events.

---

## 4. Proposed Fix / Implementation Plan
1. **Expose Invalidation API:**
   Add `DesignSettings.invalidateDocument(docId: String)` and `DesignSettings.invalidateAll()` to clear `docCache` and increment a document generation counter (`docVersion: MutableState<Int>`).
2. **Listen for Configuration & Overlay Changes:**
   In the Android `Application` setup, provide a helper:
   ```kotlin
   fun registerRroChangeListener(context: Context) {
       context.registerComponentCallbacks(object : ComponentCallbacks2 {
           override fun onConfigurationChanged(newConfig: Configuration) {
               DocServer.invalidateRawResourceDocs()
           }
           override fun onLowMemory() {}
           override fun onTrimMemory(level: Int) {}
       })
   }
   ```
3. **Recomposition on Version Increment:**
   Ensure `SquooshRoot` reads `DocServer.getDocumentState(docId)` so that clearing the cache automatically causes all active `@DesignDoc` composables to reload the updated `.dcf` overlay from `resources.openRawResource()`.

---

## 5. Validation & Reproduction Plan
1. **Automotive Emulator Test:**
   - Install base app with default raw resource `.dcf`.
   - Install RRO overlay APK overriding `R.raw.my_design`.
   - Enable overlay via `cmd overlay enable` and call `DesignSettings.invalidateAll()`.
   - Confirm UI immediately reloads with new OEM theme without process restart.

---

## 6. Drafted GitHub Comment
```markdown
### Architecture for Dynamic RRO Support:

While PRs #1778 and #1807 introduced `DesignSettings.setRawResourceId()`, full dynamic RRO support requires cache invalidation.

### Implementation Steps:
1. Add `DesignSettings.invalidateDocument(docId)` to clear in-memory document caches in `DocServer.kt`.
2. Connect document loading to a Compose `docVersion` state so clearing the cache automatically triggers recomposition.
3. Provide an automatic `ComponentCallbacks2` helper that invalidates raw resource caches whenever Android triggers `onConfigurationChanged()`.
```
