# Issue #1299: Control Figma UI prototype state in code

## 1. Issue Metadata
- **Issue #:** 1299
- **Title:** Control Figma UI prototype state in code
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/1299](https://github.com/google/automotive-design-compose/issues/1299)
- **Author:** @Heneveld
- **Created Date:** 2024-07-29T14:48:47Z
- **Updated Date:** 2024-07-29T14:48:47Z
- **Labels:** `enhancement`
- **Priority / Type:** Feature Request / State Management
- **Status:** Open (Actionable API Feature Request)

---

## 2. Problem Statement & User Impact
Figma prototypes support interactive flows with multiple screens and states linked via "Navigate to", "Change to", and conditional reactions.
DesignCompose executes these prototype interactions locally.

However, developers currently have **no programmatic control** over this prototype state from Kotlin:
- Android code cannot read the current screen/destination node ID or active variant state.
- Android code cannot trigger a programmatic transition (e.g. navigate back to home on timeout, switch screens when the car shifts into Reverse, or reset prototype state on user logout).
- There is no callback or flow (`StateFlow<String>`) emitting state change events.

Developers are forced to bypass Figma prototype interactions entirely and implement manual Compose state machines.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`designcompose/src/main/java/com/android/designcompose/InteractionState.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/InteractionState.kt#L59-L150)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt#L1310-L1350)

- **Detailed Root Cause:**
  In [`InteractionState.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/InteractionState.kt):
  - `nodeVariant`, `variations`, and destination history are stored in internal Compose `mutableStateOf` fields.
  - The dispatching method `dispatch()` is `internal` and only invoked from low-level tap callbacks in `SquooshRoot.kt`.
  - No public controller class (e.g. `DesignPrototypeController`) is exposed to developers or passed via `CompositionLocal`.

---

## 4. Proposed Fix / Implementation Plan
1. **Introduce `DesignPrototypeController`:**
   ```kotlin
   class DesignPrototypeController(
       internal val interactionState: InteractionState
   ) {
       val currentDestination: State<String?> = interactionState.currentDestinationState
       
       fun navigateTo(nodeNameOrId: String) {
           interactionState.dispatch(Action.Node(nodeNameOrId))
       }
       
       fun setVariant(componentSetNodeId: String, variantName: String) {
           interactionState.dispatch(Action.SetVariant(componentSetNodeId, variantName))
       }
       
       fun navigateBack(): Boolean {
           return interactionState.goBack()
       }
   }
   ```
2. **Expose Controller in `@DesignComponent` Composable:**
   Allow passing `controller: DesignPrototypeController = rememberDesignPrototypeController()` into generated composables, providing two-way binding.

---

## 5. Validation & Reproduction Plan
1. **Unit Test:**
   Create an `InteractionStateTest` verifying that calling `controller.navigateTo("#Screen2")` updates `currentDestination` and triggers a destination transition.
2. **Sample App Validation:**
   In `reference-apps/tutorial`, add a button outside the DesignCompose view that calls `controller.navigateTo("#Home")` and verify screen transition.

---

## 6. Drafted GitHub Comment
```markdown
### Feature Proposal: `DesignPrototypeController`

To allow controlling Figma prototype navigation and variant states programmatically from Kotlin, we propose adding a public controller:

```kotlin
val controller = rememberDesignPrototypeController()

// Read current screen:
val currentScreen by controller.currentDestination

// Programmatically navigate:
Button(onClick = { controller.navigateTo("#HomeScreen") }) {
    Text("Go Home")
}

// Pass to DesignCompose:
MyFigmaScreen(prototypeController = controller)
```
```
