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

To easily and safely persist complex keyframe sequences, `customKeyframeData` stores serialized property timelines primarily as standard escaped JSON arrays.

### Primary Format (JSON Array)

Each custom timeline property entry points to a JSON string array containing all keyframes. 

```json
{
  "customKeyframeData": {
    "opacity": "[{\"fraction\":0.2,\"value\":0.4,\"easing\":\"Linear\"},{\"fraction\":0.5,\"value\":0.8,\"easing\":\"EaseIn\"}]"
  }
}
```

### Legacy Format (Pipe-Separated Strings)

For backward compatibility, the engine still supports the older, compact pipe-separated payload format. It separates global target easing, Base64-encoded values, and segment easings utilizing delimiters like `|`, `;`, and `,` to avoid escape sequence overhead.

**Legacy Payload Layout:**
`TargetEasing|Fraction,Base64Value,Easing;...`

*Note: New and updated custom keyframes will automatically upgrade and save using pure JSON to prevent string delimiter collision.*
