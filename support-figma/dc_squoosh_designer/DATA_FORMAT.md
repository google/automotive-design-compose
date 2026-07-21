# Data Format (Option A Transition Matrix Schema)

This document describes the data structures used by the DC Squoosh Designer plugin to store animation specifications within Figma.

## Storage Mechanism

The plugin stores data in the `sharedPluginData` of Figma components (variants).
*   **Namespace:** `designcompose`
*   **Key:** `squoosh`
*   **Value:** A stringified JSON object conforming to the Option A Transition Matrix Schema.

## JSON Structure (Option A Transition Matrix)

The root payload defines an explicit array of transition rules (`transitions`) and an optional default specification fallback (`default_spec`).

```json
{
  "default_spec": {
    "initial_delay": { "secs": 0, "nanos": 0 },
    "animation": {
      "Smooth": {
        "duration": { "secs": 0, "nanos": 300000000 },
        "repeat_type": "NoRepeat",
        "easing": "Linear"
      }
    },
    "interrupt_type": "None"
  },
  "transitions": [
    {
      "from": "VariantA",
      "to": "VariantB",
      "name": "Default",
      "spec": {
        "initial_delay": { "secs": 0, "nanos": 0 },
        "animation": {
          "Smooth": {
            "duration": { "secs": 0, "nanos": 500000000 },
            "repeat_type": "NoRepeat",
            "easing": "EaseInOut"
          }
        },
        "interrupt_type": "None"
      },
      "timelines": {
        "PRNDState-x": {
          "targetEasing": "Inherit",
          "keyframes": [
            { "fraction": 0.5, "value": { "Scalar": 100.0 }, "easing": "EaseIn" }
          ]
        }
      }
    },
    {
      "from": "*",
      "to": "VariantB",
      "name": "AlertPop",
      "spec": {
        "initial_delay": { "secs": 0, "nanos": 0 },
        "animation": {
          "Smooth": {
            "duration": { "secs": 0, "nanos": 300000000 },
            "repeat_type": "NoRepeat",
            "easing": "EaseOut"
          }
        },
        "interrupt_type": "None"
      },
      "timelines": {}
    }
  ]
}
```

### Transition Matrix Lookup Hierarchy

At runtime (in HAR and DriverUI), transition animation resolution follows a deterministic multi-stage lookup:
1. `"VariantA->VariantB:AlertPop"` (Exact custom animation name match)
2. `"VariantA->VariantB:Default"` (Pair default match)
3. `"*->VariantB:Default"` (Wildcard origin match)
4. `"VariantB"` (Base target variant fallback)

