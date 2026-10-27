# Issue #121: Re-make doc page on Figma Interactions

## 1. Issue Metadata
- **Issue #:** 121
- **Title:** Re-make doc page on Figma Interactions
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/121](https://github.com/google/automotive-design-compose/issues/121)
- **Author:** @timothyfroehlich
- **Created Date:** 2023-06-13T17:50:05Z
- **Updated Date:** 2023-06-21T21:09:30Z
- **Labels:** `documentation`, `:blue_square:  P3`
- **Priority / Type:** P3 / Documentation Task (Internal Ref: `b/250921446`)
- **Status:** Open (Actionable Documentation Enhancement)

---

## 2. Problem Statement & User Impact
Figma allows designers to define rich prototype interactions (On click, While hovering, While pressing, Key/gamepad triggers) with actions like "Change to" (variant switching), "Navigate to" (screen navigation), and "Open overlay".

DesignCompose translates many of these interactions directly into Compose state changes and animations. However, there is no comprehensive documentation page detailing:
1. Which Figma interaction triggers and actions are supported vs. unsupported in DesignCompose.
2. How to wire Kotlin tap callbacks ([`TapCallback`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt)) alongside Figma interactions.
3. How to inject key/gamepad events into DesignCompose via [`KeyInjectRegistry`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/InteractionState.kt).
4. How variant transitions execute within component sets versus screen-level navigation.

Developers building automotive UI (such as HVAC controls, media playback buttons, and steer-wheel physical key handlers) lack documentation on how to trigger or listen to these interactions.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`designcompose/src/main/java/com/android/designcompose/InteractionState.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/InteractionState.kt#L38-L150)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt#L1310-L1350)
  - [`docs/_docs/animations.md`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/animations.md)
  - [`docs/_docs/modifiers/modifiers.md`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/modifiers/modifiers.md)

- **Implementation Details:**
  Interaction processing in DesignCompose is handled by `InteractionState`:
  - `InteractionState.dispatch()` executes actions parsed from Figma's `Reaction` protos (`Action.Node`, `Action.SetVariant`, etc.).
  - `KeyInjectRegistry` maps physical Android hardware key codes (`KeyEvent.KEYCODE_*`) to Figma key triggers.
  - While [`animations.md`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/animations.md) covers smart-animate interpolation curves and timing, the interaction trigger setup and Kotlin integration details are missing.

---

## 4. Proposed Fix / Implementation Plan
1. **Create Documentation Page:**
   Author [`docs/_docs/figma-interactions.md`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/figma-interactions.md) under the Interaction & Animation section.
2. **Sections to Document:**
   - **Supported Triggers:**
     - `On Click / On Tap` -> Handled automatically; can be combined with Kotlin `@Design(node = "#Button") tap: TapCallback = { /* handle click */ }`.
     - `While Pressing / While Hovering` -> Maps to Compose interaction state (press/hover) and variant change.
     - `Key / Gamepad` -> Explain how to register key event dispatchers using `KeyInjectRegistry.dispatch(keyEvent)`.
   - **Supported Actions:**
     - `Change To` (Component Variant switching) -> Automatically updates the rendered variant without Composable recomposition overhead.
     - `Navigate To` -> Switches top-level frame destination in prototype mode.
     - `Back` -> Reverts to previous destination.
   - **Overriding and Combining with Compose Logic:**
     Explain whether a Figma interaction prevents or coexists with a Kotlin `tap` callback.
   - **Known Limitations:**
     Document that "Open overlay", "Scroll to", and external URL open actions require custom Kotlin handling.

---

## 5. Validation & Reproduction Plan
1. **Doc Verification:**
   Build Jekyll site locally and verify formatting and links.
2. **Example App Testing:**
   Reference the Tutorial app and Media Center demo interaction implementations:
   [`reference-apps/tutorial/app/src/main/java/com/android/designcompose/tutorial/MainActivity.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/reference-apps/tutorial/app/src/main/java/com/android/designcompose/tutorial/MainActivity.kt).

---

## 6. Drafted GitHub Comment
```markdown
We have drafted a comprehensive guide on Figma Interactions in DesignCompose (`docs/_docs/figma-interactions.md`).

### Guide Overview:
- **Triggers Supported:** On Click, While Pressing, While Hovering, Key / Gamepad presses.
- **Actions Supported:** "Change to" (variant transitions) and "Navigate to" (screen navigation).
- **Combining with Code:** How `@Design(node = "#Node") onClick: TapCallback` executes in conjunction with Figma prototype reactions.
- **Hardware Key Support:** Using `KeyInjectRegistry` to route steering wheel or rotary dial `KeyEvent`s directly to Figma key triggers.

We will open a PR adding this page to the official documentation.
```
