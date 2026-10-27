# Issue #1331: DesignVariant not working with figma component instances

## 1. Issue Metadata
- **Issue #:** 1331
- **Title:** DesignVariant not working with figma component instances
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/1331](https://github.com/google/automotive-design-compose/issues/1331)
- **Author:** @yiqunw700
- **Created Date:** 2024-08-08T18:18:23Z
- **Updated Date:** 2024-08-08T18:18:23Z
- **Labels:** `bug`
- **Priority / Type:** Bug / Variants & Component Instances
- **Status:** Open (Actionable Variant State Bug)

---

## 2. Problem Statement & User Impact
In Figma, developers define component sets with properties and variants (e.g., `Button` with `type=Primary|Secondary` and `state=Default|Pressed`).
When an instance of that component is embedded inside another parent frame or screen:
- Attempting to control the instance's variant using `@DesignVariant` in Kotlin fails to switch the variant.
- In other cases, switching a variant on one instance causes all instances of the same component set across the screen to switch simultaneously.
- Furthermore, when the variant switches, child customizations (e.g. button text or icon replacement) are wiped out or revert to design defaults.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`designcompose/src/main/java/com/android/designcompose/InteractionState.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/InteractionState.kt#L40-L60)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt#L750-L775)
  - [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt)

- **Detailed Root Cause:**
  1. In [`InteractionState.kt:41-45`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/InteractionState.kt#L41-L45):
     Variant states are keyed by `nodeId` of the component set rather than the unique `instanceId` of the embedded instance. As a result, all instances share identical state keys.
  2. In `SquooshRoot.kt`, when a variant changes, Squoosh swaps the underlying `View` tree to the target variant's view. However, instance customizations (such as custom text or replacement callbacks attached to the original instance node ID) are not mapped to the corresponding child nodes in the new variant's subtree.

---

## 4. Proposed Fix / Implementation Plan
1. **Scope Variant Keys by Instance Path:**
   In `InteractionState.kt`, key variant state maps using a compound key:
   `"${instancePath}/${componentSetId}"` rather than `componentSetId` alone.
2. **Re-bind Customizations Across Variant Swaps:**
   In `SquooshTreeBuilder.kt`, when resolving an instance with a variant override, transfer customizations from the instance declaration to matching child nodes in the variant view tree by node name match.

---

## 5. Validation & Reproduction Plan
1. **Unit Test:**
   Create a screen with two instances of `IconButton`. Set instance 1 to `state = Active` and instance 2 to `state = Inactive`. Verify both render their distinct variant states independently.
2. **Scuba Parity Test:**
   Verify that custom text passed to an instance is preserved when switching variants.

---

## 6. Drafted GitHub Comment
```markdown
### Technical Findings on `@DesignVariant` with Component Instances:

### Root Cause:
1. `InteractionState` keys variant state by component set ID rather than the instance path, causing instance state collisions.
2. When switching variant trees, child customizations attached to the original instance are not reparented to the new variant's children.

### Planned Fix:
- Key variant state using the unique instance path in `InteractionState`.
- Map instance customization keys to variant children by node name match during tree resolution in `SquooshTreeBuilder`.
```
