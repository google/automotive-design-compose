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
    "TIMELINE_ID": "{\"targetEasing\":\"Inherit\",\"keyframes\":[{\"fraction\":0.5,\"value\":{\"Scalar\":1},\"easing\":\"Linear\"}]}"
    // This field stores stringified JSON data for custom (user-added) property timelines.
    // Figma's API limits plugin data to strings, so the inner object is JSON-serialized again.
    // System-generated timelines (e.g., for 'x', 'y' changes between variants)
    // do not store their keyframe data here, but derive it from variant properties.
  }
}
```

To easily and safely persist complex keyframe sequences, `customKeyframeData` stores serialized property timelines as stringified JSON objects.

### Structured Timeline Format (Decoded)

When decoded, the JSON string mapped to `TIMELINE_ID` has the following structure, aligning with the Protobuf definitions:

```json
{
  "targetEasing": "Inherit",
  "keyframes": [
    {
      "fraction": 0.2,
      "value": { "Scalar": 0.4 },
      "easing": "Linear"
    },
    {
      "fraction": 0.5,
      "value": { "Scalar": 0.8 },
      "easing": "EaseIn"
    }
  ]
}
```

*Note: All custom timelines enforce this JSON structure within the stringified payload, yielding superior parsing performance and explicit typing for multi-scalar values (like color or gradient arrays).*
