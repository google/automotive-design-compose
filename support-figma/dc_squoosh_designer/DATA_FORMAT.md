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

To easily and safely persist complex keyframe sequences, `customKeyframeData` stores serialized property timelines strictly as robust, typed JSON objects mirroring the protobuf schema.

### Standard Format (Typed JSON Schema)

Each custom timeline property entry contains a strongly-typed JSON object array representing all keyframes, perfectly aligning with the Rust engine (`CustomTimeline`) and the Protobuf definitions.

```json
{
  "customKeyframeData": {
    "opacity": {
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
  }
}
```

*Note: All custom timelines enforce this JSON structure natively, yielding superior parsing performance, explicit typing for multi-scalar values (like color or gradient arrays), and complete immunity to legacy string-delimiter collisions.*
