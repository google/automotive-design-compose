# Issue #2181: Customizations are not propagated to replacement component children

## 1. Issue Metadata
- **Issue #:** 2181
- **Title:** Customizations are not propagated to replacement component children
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/2181](https://github.com/google/automotive-design-compose/issues/2181)
- **Author:** @rylin8
- **Created Date:** 2025-04-28T18:39:00Z
- **Updated Date:** 2025-04-28T18:39:11Z
- **Labels:** *None*
- **Priority / Type:** Bug / Customization Context Propagation
- **Status:** Open (Actionable Customization Bug)

---

## 2. Problem Statement & User Impact
In DesignCompose, developers often wrap complex screen sections using component replacement (`ComponentReplacementContext`) to insert dynamic Compose content, animations, or video players.

However, when a parent component replaces a container node (e.g. `Stage`), any customizations defined for the child nodes inside that subtree (such as text bindings, custom tap callbacks, or image replacements) are **completely dropped**:
```kotlin
Stage(
    title = "Now Playing", // Child customization
    stageReplacement = { context ->
        // Custom wrapper
        Box {
            context.Content() // title customization is lost!
        }
    }
)
```
The child content rendered inside `context.Content()` reverts to the raw default Figma design state, ignoring all `@Design` parameters passed to the parent composable.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt#L100-L150)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt#L750-L780)

- **Detailed Root Cause:**
  1. In [`SquooshRoot.kt:765`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt#L765), when creating the `ComponentReplacementContext`:
     ```kotlin
     val replacementContext = ComponentReplacementContext(
         layout = childLayout,
         content = {
             // Renders child subtree
             SquooshSubtree(...)
         }
     )
     ```
  2. A new, empty `CustomizationContext` is instantiated for the sub-tree render, rather than forwarding the parent `customizationContext`.
  3. Consequently, all registered `textCustomizations`, `tapCallbacks`, and `imageCustomizations` present in the outer context are inaccessible to the child composable.

---

## 4. Proposed Fix / Implementation Plan
1. **Forward Parent Customization Context:**
   In [`SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt):
   Ensure `context.Content()` passes the parent `customizationContext` to the child `SquooshRoot` invocation.
2. **Support Merging & Selective Overrides:**
   Allow replacement contexts to either inherit parent customizations or optionally override specific child nodes.

---

## 5. Validation & Reproduction Plan
1. **Unit Test in `SquooshRootTest.kt`:**
   Define a container with child `#Text` and set `title = "Custom String"`. Replace the container using `ComponentReplacementContext` and verify that `context.Content()` renders `"Custom String"` instead of the Figma default.

---

## 6. Drafted GitHub Comment
```markdown
### Root Cause & Fix for Issue #2181:

### Root Cause:
`ComponentReplacementContext.Content()` executes a sub-tree render pass using a new `CustomizationContext` rather than forwarding the parent composable's customization map.

### Fix:
In `SquooshRoot.kt`, pass `customizationContext = this.customizationContext` into the child composable builder inside `ComponentReplacementContext`.
```
