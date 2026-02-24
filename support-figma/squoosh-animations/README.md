# Figma Animation Plugin

This document provides a detailed overview of the Figma animation plugin, its architecture, how it works, and its future goals.

## Project Overview

This plugin is a powerful tool for creating and previewing animations directly within the Figma environment. It allows designers and developers to define complex animations by manipulating the properties of elements across different variants of a component set. The plugin provides a timeline view, a detailed property inspector, and a real-time, interpolated animation preview.

## Architecture

The plugin is built on the standard Figma plugin architecture, which consists of two main parts:

*   **`code.js` (Main Thread):** This script runs in Figma's main thread and has access to the Figma document and the full Figma Plugin API. It acts as the "backend" of the plugin, responsible for:
    *   Reading the properties of the selected component set and its variants.
    *   Comparing the variants to detect property changes.
    *   Handling updates to the Figma document when properties are edited in the plugin's UI.
    *   Sending all necessary data to the UI for rendering.

*   **`ui.html` (UI Thread):** This is a self-contained web application that runs in a sandboxed iframe. It has no direct access to the Figma document. It acts as the "frontend" of the plugin, responsible for:
    *   Displaying the timeline, property inspector, and animation preview.
    *   Handling all user interaction, such as clicking the "Play" button or editing properties in the table.
    *   Running the animation loop and performing all the interpolation calculations.
    *   Communicating with `code.js` to send updates back to the Figma document.

## How It Works

The plugin's workflow can be broken down into a few key steps:

1.  **Selection and Data Collection:** When a user selects a component set in Figma, the `selectionchange` event is fired in `code.js`. The script then traverses the entire structure of each variant in the component set.

2.  **Property Change Detection:** For each element, the script records the values of all animatable properties (position, size, color, etc.) in each variant. It then compares these values to identify which properties have changed across the variants.

3.  **Data Serialization:** The collected data, including the list of variants, the detected property changes, and the raw SVG for each variant, is packaged into a plain JavaScript object. This is necessary because complex objects like `Map` are not preserved when sent to the UI.

4.  **UI Rendering:** The data is sent to `ui.html`, which then renders the timeline and the property inspector table. The timeline is drawn on a `<canvas>` element, and the property table is dynamically generated as an HTML `<table>`.

5.  **Animation Preview:** When the "Play" button is clicked:
    *   The SVG of the first keyframe is cloned and inserted into the preview panel.
    *   A `requestAnimationFrame` loop is started to ensure a smooth, high-framerate animation.
    *   On each frame, the animation logic calculates the current progress and identifies the "from" and "to" keyframes for that point in time.
    *   The `transform` matrices of the elements in the "from" and "to" keyframes are decomposed into their constituent parts (translate, scale, rotate, shear).
    *   Each of these components is interpolated individually, and then recomposed into a new `transform` matrix.
    *   This new matrix, along with any other interpolated properties, is applied directly to the live SVG elements in the preview panel.

## Tools and Technologies

*   **Figma Plugin API:** The core of the plugin's interaction with the Figma document.
*   **HTML, CSS, JavaScript:** The standard web technologies used to build the plugin's user interface.
*   **`DOMParser`:** A built-in browser API used to parse the SVG strings from Figma into a DOM structure that can be manipulated for the animation.
*   **`DOMMatrix`:** A built-in browser API used for the decomposition and recomposition of SVG transform matrices, which is the key to the accurate interpolation logic.

## Known Goals and Future Work

This plugin is a powerful tool, but there are many ways it could be improved:

*   **Support for More Properties:** The animation engine could be expanded to support more animatable properties, such as gradients, shadows, and other effects.
*   **Advanced Easing:** The interpolation logic could be enhanced to support a variety of easing functions (e.g., ease-in, ease-out, bounce) for more expressive animations.
*   **Interactive Timeline:** The timeline could be made more interactive, with support for dragging and reordering keyframes.
*   **Code Export:** The plugin could be extended to export the animation data as CSS or JavaScript code, allowing it to be used directly in web projects.
