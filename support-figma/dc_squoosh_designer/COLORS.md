# Timeline Color Scheme

This document outlines the visual style and color coding for keyframes and timeline elements in the Animation Designer. The goal is to provide a consistent, high-contrast interface where color denotes state (Editable vs. Locked vs. Missing) and structure denotes selection.

## Keyframe States

### 1. Standard Keyframes (Editable)
Represents a user-created or standard keyframe that can be modified.

*   **Concept:** Active, Editable data.
*   **Normal Style:**
    *   Fill: Dark Grey (`#333333`)
    *   Border: None or thin neutral border
*   **Selected Style:**
    *   Fill: Bright Blue (`#2196F3`)
    *   Border: White (`#ffffff`) (2px)
    *   Shadow: Thin Black (`#000000`) outer ring (box-shadow)
    *   *Reasoning:* High visibility "pop" indicating selection.

### 2. Locked Keyframes (Read-only)
Represents a keyframe derived directly from a Figma variant. These are fixed and cannot be moved or deleted.

*   **Concept:** Fixed, Immutable data.
*   **Normal Style:**
    *   Fill: Medium Gray (`#9E9E9E`)
*   **Selected Style:**
    *   Fill: Dark Gray (`#616161`)
    *   Border: White (`#ffffff`) (2px)
    *   Shadow: Thin Black (`#000000`) outer ring
    *   *Reasoning:* Gray implies "disabled" while the selection style remains consistent.

### 3. Missing Keyframes
Represents a point in time where the target node does not exist in the corresponding variant.

*   **Concept:** Void, Gap, Attention.
*   **Normal Style:**
    *   Fill: White (`#ffffff`)
    *   Border: Orange (`#FF9800`) (2px solid)
    *   *Reasoning:* A "hollow" look suggests absence. Orange draws attention to the gap.
*   **Selected Style:**
    *   Fill: Solid Orange (`#E65100`)
    *   Border: White (`#ffffff`) (2px)
    *   Shadow: Thin Black (`#000000`) outer ring
    *   *Reasoning:* Solid fill on selection makes it clear which specific missing point is targeted.

## Timeline Sections

### Range Selection
Visualizes a selected span of time on the timeline track.

*   **Normal:**
    *   Background: Pale Blue (`rgba(33, 150, 243, 0.15)`)
    *   Border: Blue (`#2196F3`)
*   **Dragging:**
    *   Background: Medium Blue (`rgba(33, 150, 243, 0.4)`)
    *   *Reasoning:* Keeps the "active/manipulation" theme consistently blue, distinct from the orange used for missing items.

## Color Logic Summary
*   **Blue:** Active / Standard / Selection.
*   **Gray:** Locked / Fixed / Inactive.
*   **Orange:** Missing / Alert.
