# Issue #1169: Support duplicate variable names across multiple documents

## 1. Issue Metadata
- **Issue #:** 1169
- **Title:** Support duplicate variable names across multiple documents
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/1169](https://github.com/google/automotive-design-compose/issues/1169)
- **Author:** @rylin8
- **Created Date:** 2024-06-25T17:46:17Z
- **Updated Date:** 2024-06-25T17:46:17Z
- **Labels:** `bug`
- **Priority / Type:** Bug / Variable & Theming System
- **Status:** Open (Actionable Theming Collision Bug)

---

## 2. Problem Statement & User Impact
Figma Variables (design tokens) allow teams to define colors, numbers, and strings organized into Collections and Modes (e.g. `Light`, `Dark`).

When an application loads multiple Figma documents (e.g. `ClusterDoc` and `MediaDoc`, or a primary document and an external UI library document):
- If two documents define a variable with the same name (such as `Color/Primary`, `Spacing/MarginSmall`, or `Theme/Surface`), DesignCompose's variable manager stores them in a single flat map.
- The document loaded last overwrites the variable definition of the first document.
- As a result, components in the first document render with incorrect colors, dimensions, or theme values from the second document.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`designcompose/src/main/java/com/android/designcompose/VariableManager.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/VariableManager.kt#L160-L195)
  - [`designcompose/src/main/java/com/android/designcompose/DocServer.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/DocServer.kt)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt)

- **Detailed Root Cause:**
  In [`VariableManager.kt:166-193`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/VariableManager.kt#L166-L193):
  ```kotlin
  object VariableManager {
      private var varMap: VariableMap = HashMap()
      
      fun init(doc: DesignDoc) {
          // Merges into global map unconditionally:
          varMap.putAll(doc.variables)
      }
  }
  ```
  `VariableManager` is a global singleton that maintains a single `varMap`. It has no concept of namespacing by `docId`. Calling `VariableManager.init()` for Document B clobbers any colliding variable entries registered by Document A.

---

## 4. Proposed Fix / Implementation Plan
1. **Namespace Variables by Document ID:**
   In [`VariableManager.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/VariableManager.kt):
   Refactor `varMap` to store document-scoped collections:
   ```kotlin
   private val docVarMaps: HashMap<String, VariableMap> = HashMap()
   
   fun init(docId: String, variables: VariableMap) {
       docVarMaps[docId] = variables
   }
   
   fun getVariable(docId: String, varName: String): VariableValue? {
       return docVarMaps[docId]?.get(varName) ?: globalFallbackMap[varName]
   }
   ```
2. **Pass `docId` During Variable Resolution:**
   In `SquooshRoot.kt` and `ViewStyleUtils.kt`, pass the active `documentId` into `VariableManager.getValue(docId, variableId)`.

---

## 5. Validation & Reproduction Plan
1. **Unit Test in `VariableManagerTest.kt`:**
   Register Doc A with `Color/Background = #FF0000` (Red) and Doc B with `Color/Background = #0000FF` (Blue). Verify that querying Doc A returns Red and Doc B returns Blue.
2. **Integration Test:**
   Load two components from different Figma files simultaneously on screen and verify neither overrides the other's color tokens.

---

## 6. Drafted GitHub Comment
```markdown
### Technical Findings & Proposed Architecture for Issue #1169:

### Root Cause:
`VariableManager` stores all variables in a single flat `varMap: VariableMap` singleton without document scoping. Loading a second document overwrites colliding variable names via `putAll`.

### Planned Fix:
1. Update `VariableManager` to maintain a two-level map: `HashMap<DocId, VariableMap>`.
2. Pass `docId` from `SquooshRoot` when resolving variable tokens during render and mode switching.
```
