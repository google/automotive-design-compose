# Data Format

This document describes the data structures used by the DC Squoosh Designer plugin to store animation specifications within Figma.

## Storage Mechanism

The plugin stores data in the `sharedPluginData` of Figma components (variants).
*   **Namespace:** `designcompose`
*   **Key:** `animations`
*   **Value:** A stringified JSON object.

## JSON Structure

The root object contains the animation specification for transitioning *to* the variant where the data is stored.

```json
{
  "spec": {
    "initial_delay": { "secs": 0, "nanos": 0 },
    "animation": {
      "Smooth": {
        "duration": { "secs": 0, "nanos": 300000000 },
        "easing": "Linear"
      }
    },
    "interrupt_type": "None"
  },
  "customKeyframeData": {
    "TIMELINE_ID": "SERIALIZED_KEYFRAMES_STRING",
    // This field stores serialized data for custom (user-added) property timelines.
    // System-generated timelines (e.g., for 'x', 'y' changes between variants)
    // do not store their keyframe data here, but derive it from variant properties.
    ...
  }
}
```

## Custom Keyframe Serialization

To efficiently store complex keyframe sequences within the JSON structure, `customKeyframeData` uses a compact formatted string.

**Format:**
`TargetEasing|Keyframe1;Keyframe2;...`

*   **`TargetEasing`**: (String) The easing curve defined for the transition *from* the last custom keyframe to the next variant state (e.g., "Inherit", "Linear", "EaseIn"). "Inherit" means it uses the global animation easing.
*   **`KeyframeN`**: A serialized string representing a single custom keyframe.

### Keyframe String Format

Each keyframe entry is separated by a semicolon `;` and follows this internal structure:

`Fraction,Value,Easing`

1.  **`Fraction`**: (Number) A value between 0 and 1 representing the time position of the keyframe within the transition.
2.  **`Value`**: The value of the property at this keyframe. Stored as a **Base64 encoded string** (primitives are stringified, complex types like objects or arrays are JSON-stringified before encoding).
3.  **`Easing`**: (String) The easing curve for the segment *following* this keyframe. (e.g., "Linear", "EaseIn", "Instant").

### Examples

**Simple Primitives (e.g., Opacity):**
A transition that stays at 0.5 opacity halfway through, then eases out to the variant state.
- TargetEasing: `EaseOut`
- Keyframe 1: Fraction `0.5`, Value `0.5`, Easing `Linear`. (Base64 of `"0.5"` is `MC41`)

Serialized string: `EaseOut|0.5,MC41,Linear`

**Complex Data Types (e.g., Position and Object Types):**
A transition involving an object like `{ "x": 10, "y": 20 }`.
- TargetEasing: `EaseInOut`
- Keyframe 1: Fraction `0`, Value `100`, Easing `Linear`. (Base64 of `"100"` is `MTAw`)
- Keyframe 2: Fraction `0.5`, Value `{"x":10,"y":20}`, Easing `EaseIn`. (Base64 of `{"x":10,"y":20}` is `eyJ4IjoxMCwieSI6MjB9`)
- Keyframe 3: Fraction `1`, Value `"test"`, Easing `EaseOut`. (Base64 of `"test"` is `dGVzdA==`)

Serialized string: `EaseInOut|0,MTAw,Linear;0.5,eyJ4IjoxMCwieSI6MjB9,EaseIn;1,dGVzdA==,EaseOut`

*Note: Properties like `id`, `locked`, `isMissing`, and internal `data` are part of the in-memory `Keyframe` interface used by the plugin and are not directly stored in this serialized string format. The `isCustom` flag for a `Timeline` is inferred by its presence in `customKeyframeData` during deserialization.*
