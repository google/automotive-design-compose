# DC Squoosh Designer Manual

This manual explains how to use the DC Squoosh Designer Figma plugin to create and edit advanced animations for Design Compose.

## Installation

To install the DC Squoosh Designer Figma plugin locally:

1.  **Download the Desktop Figma App:** Ensure you have the Figma desktop application installed.
2.  **Import from Manifest:** In Figma, go to `Plugins` -> `Development` -> `Import plugin from manifest...`.
3.  **Select manifest.json:** Browse to the `dist` directory of this project and select `manifest.json` (or the root `manifest.json` if running directly from source).
4.  **Open the Plugin:** The plugin will appear in your `Development` plugins list.

## Core Concepts

Understanding the difference between the two types of keyframes is crucial for using this tool effectively.

### Variant Keyframes
These are the "anchor" points of your animation. They are automatically generated based on the variants in your selected Component Set.
*   **Source:** They come directly from your Figma design.
*   **Locked:** You cannot move or delete them directly in the timeline. Their timing is determined by the animation duration settings in the Control Panel.
*   **Visual:** Represented as **Gray** circles.
*   **Function:** They define the start and end states of the main transitions.

### Added (Custom) Keyframes
These are keyframes you manually add to the timeline to create more complex motion paths.
*   **Flexible:** You can add, remove, and move them freely between the locked variant keyframes.
*   **Visual:** Represented as **Dark Grey** circles (turning **Blue** when selected).
*   **Function:** Use these to add intermediate steps to create custom motion profiles between the start and end states.

### Missing Keyframes
These appear when a node exists in one variant but not in another.
*   **Visual:** Represented as **White circles with an Orange border**.
*   **Function:** They indicate a "gap" in the node's existence. The plugin handles these by holding the last known value or fading out, preventing interpolation errors (like flying to 0,0).

## User Interface Guide

### 1. Selecting and Previewing
*   **Select a Component Set:** To start, select a component instance on your canvas that belongs to a Component Set with multiple variants.
*   **Select Preview Frame:** Click the **"Select Preview Frame"** button in the Control Panel, then select a Frame on your canvas. The plugin will clone your component into this frame to show a live preview of the animation.
*   **Throttle:** Check the **"Throttle"** box to limit preview updates to 60fps. This makes dragging the playhead smoother by dropping intermediate frames if Figma is processing slowly.

### 2. Timeline Navigation
*   **Zoom:** Hover your mouse over the **Timeline Header** (the ruler area with numbers) and **scroll your mouse wheel** up or down.
*   **Pan:** Click and drag anywhere on the **Timeline Header** to pan the view horizontally.
*   **Scrub:** Click or drag on the **Timeline Header** to move the playhead and scrub through the animation.

### 3. Managing Keyframes
*   **Create Keyframe:** **Double-click** on any timeline track (the horizontal line for a property) to add a new custom keyframe at that position.
*   **Move Keyframe:** Click and drag any **unlocked** (custom) keyframe to change its timing. *Note: You cannot drag the Gray Variant Keyframes.*
*   **Select Keyframes:**
    *   **Single Click:** Selects a single keyframe.
    *   **Drag Selection:** Click and drag on the empty background of the timeline grid to draw a selection box around multiple keyframes.
*   **Remove Keyframe:** Select a keyframe and click the **"Remove Keyframe"** button in the Properties Panel, or **Double-click** an existing unlocked keyframe.

### 4. Managing Timelines
*   **Add Timeline:** Hover over a node in the Node Tree (left panel) and click the **"+"** button. Select a property to animate (e.g., `x`, `opacity`, `fills.0.solid`).
*   **Delete Timeline:** For custom timelines you've added, a **"-"** button appears next to the property name in the Node Tree. Click it to remove the timeline and all its custom keyframes. *Note: You cannot delete system-generated timelines derived from variants.*

### 5. Editing Properties
The **Properties Panel** (right/bottom side) changes based on what you have selected.

#### Animation Settings (Global)
When no specific keyframe is selected, you see the settings for the current Variant state:
*   **Duration:** How long the transition *into* this state takes.
*   **Initial Delay:** Delay before the transition starts.
*   **Easing:** The default easing curve (e.g., Linear, EaseIn, EaseOut) for the transition.

#### Keyframe Properties
When a keyframe is selected:
*   **Value:** You can manually edit the property value (e.g., X position, Opacity) for precise control.
*   **Time/Fraction:** Fine-tune the exact timing of the keyframe.

#### Animation Section Properties (Easing Overrides)
Click on the line *between* two keyframes to select a **Section**.
*   **Easing:** You can override the easing for just this specific segment.
    *   **Inherit:** Uses the global easing defined for the variant transition.
    *   **Linear/EaseIn/etc.:** Forces a specific curve for this part of the motion.
*   **Gradient Preview:** If animating colors or gradients, a visual preview of the interpolation is displayed here.

## Supported Animation Properties

You can animate a wide range of Figma properties. When you add a new timeline to a node (using the `+` button next to the layer name), you specify the property string:

*   **Basic:** `x`, `y`, `width`, `height`, `rotation`, `opacity`, `visible`
*   **Corners:** `topLeftRadius`, `topRightRadius`, `bottomLeftRadius`, `bottomRightRadius` (The plugin normalizes all corner radius changes to individual corners to support transitions between uniform and mixed radii).
*   **Stroke:** `strokeWeight`
*   **Fills & Strokes (Advanced):**
    *   Solid Color: `fills.0.solid` (targets the first fill layer)
    *   Gradient Stops: `fills.0.gradient.stops`
    *   Gradient Handles: `fills.0.gradient.positions`
    *   Opacity: `fills.0.opacity`
    *   *(Same logic applies to `strokes`)*
*   **Arcs:** `arcData` (for pie charts/donuts)

## Troubleshooting
*   **"Nothing happens when I play":** Ensure you have selected a valid Component Set instance and that your variants have different property values to animate between.
*   **"UI is lagging":** Enable the **"Throttle"** checkbox in the Control Panel.
*   **"Cannot move keyframe":** Check if it is a **Variant Keyframe** (indicated by a lock icon in the panel). These are fixed points defined by your design variants.
*   **"Run Tests":** If you suspect a bug, click the "Run Tests" button in the top control bar. This runs a suite of checks to verify that the plugin's logic and connection to Figma are working correctly.
