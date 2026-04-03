# DC Squoosh Designer

This document provides an overview of the DC Squoosh Designer Figma plugin, including its goals, techniques, and technical details.

## Overview

DC Squoosh Designer is a Figma plugin designed to facilitate the creation and management of animations between component variants. It provides a user interface for defining animation specifications and previewing them in real-time within the Figma canvas. The plugin is intended to be used with the DesignCompose toolkit, allowing designers to create animations that can be directly used in Jetpack Compose applications.

## Goals

The primary goals of the DC Squoosh Designer plugin are:

- **Streamline Animation Design:** To provide a seamless workflow for designers to create animations without leaving the Figma environment.
- **Bridge Design and Development:** To generate animation specifications that can be directly consumed by developers, ensuring a high-fidelity translation from design to implementation.
- **Real-time Preview:** To offer designers an immediate visual feedback loop by allowing them to preview animations directly in Figma.
- **Integration with DesignCompose:** To work in conjunction with the DesignCompose library, enabling animations to be easily integrated into Android applications.

## Core Concepts

The plugin's functionality is centered around a few core concepts:

### Component Sets and Variants

The plugin operates on Figma's component sets. When a component set or one of its instances is selected, the plugin's UI displays a list of all the variants within that set. This allows designers to define animations that transition between these different states.

### Animation Data

Animation specifications are saved directly onto the component variants as shared plugin data. This ensures that the animation data is intrinsically linked to the components and is versioned along with the Figma file.

### Preview Frame

To provide a live preview of the animations, the plugin uses a dedicated "Preview Frame". When an animation is previewed, the plugin:

1.  Clones the selected component variant.
2.  Places the cloned nodes into the Preview Frame.
3.  Applies the animation transformations to the nodes within the Preview Frame.

This approach allows for a non-destructive preview of the animation without modifying the original components.

### Debugging and Comparison

The plugin includes a debugging feature that allows designers to compare the state of the nodes in the Preview Frame with the corresponding nodes in a target variant. This helps in identifying and resolving any discrepancies between the animation's final state and the intended design.

## Building

To build the plugin, you need to have Node.js and npm installed.

1.  **Install Dependencies:**

    ```bash
    npm install
    ```

2.  **Build the Plugin:**
    ```bash
    npm run build
    ```

This will compile the TypeScript code and create the necessary files in the `dist` directory.

3. **Package for Distribution:**
    ```bash
    npm run dist
    ```
This will build the project and create a zip file in `dist-package` containing all necessary files to load the plugin in Figma.

4. **Run Tests:**
    ```bash
    npm test
    ```
This executes the Jest unit tests for the plugin logic.

## Formatting

This project uses Prettier for code formatting. To format the code, run the following command:

```bash
npm run format
```

## Technical Details

### Implementation

The plugin is written in **TypeScript** and interacts with the Figma Plugin API to access and manipulate the Figma document. The user interface is built with HTML and communicates with the main plugin code using the `figma.ui.postMessage` and `figma.ui.onmessage` APIs.

### Data Storage

Animation data is stored as a JSON string within the shared plugin data of each component variant. The plugin uses the key `designcompose/animations` to store this data.

### Node Serialization

To manage the state of the components and their animations, the plugin includes logic to serialize Figma nodes into a JSON format. This serialized data includes properties such as position, size, rotation, colors, and more.

### Architecture

The plugin is divided into two main parts: the main thread (`code.ts`) and the UI thread (`ui.html`).

**Main Thread (`code.ts`)**

The main thread is responsible for all interactions with the Figma document and the Figma Plugin API. Its key responsibilities include:

- **Selection Management:** It listens for selection changes in the Figma document. When the user selects a component set or an instance of a variant, it triggers the process to display the animation controls.
- **Node Serialization:** It walks the layer hierarchy of the selected component's variants and serializes them into a JSON structure. This data includes properties like position, size, rotation, and colors, and is sent to the UI thread.
- **Preview Frame Management:** It manages a dedicated "Preview Frame" on the Figma canvas. To show an animation, it clones the contents of the starting variant into this frame.
- **Applying Animation Properties:** As the UI thread plays the animation, it sends messages with the interpolated properties for each node at each frame. The main thread receives these messages and applies the transformations to the corresponding nodes within the Preview Frame.
- **Data Persistence:** When the user saves animation properties, the main thread receives the data from the UI and saves it as a JSON string in the shared plugin data of the corresponding component variant.
- **Sandbox Testing:** The main thread executes integration tests that interact with the real Figma API to verify functionality like node creation and plugin data storage.

**UI Thread (`ui.html`)**

The UI thread is responsible for the user interface and all the client-side animation logic. It runs in a browser environment and does not have direct access to the Figma document. Its key responsibilities include:

- **User Interface:** It renders the animation timeline, property editors, and playback controls.
- **Timeline Visualization:** It renders an interactive timeline that visualizes the keyframes (variants) and the animated properties of each node. This is a custom implementation using standard web technologies, allowing for a high degree of control and performance.
- **Animation Logic:** When the user plays an animation, the UI thread calculates the intermediate states of all animated nodes between the start and end keyframes. It takes into account the duration, easing curves, and other animation parameters.
    - **Matrix-based Transforms:** Uses robust matrix multiplication and inversion to calculate relative transforms (`x`, `y`, `rotation`) correctly, even when parent nodes are rotated or scaled.
    - **Center-based Rotation:** Implements specific interpolation logic to rotate nodes around their visual center during transitions, preventing the "wobble" effect associated with top-left pivot rotation.
- **Communication:** It communicates with the main thread by posting messages. For example, it sends messages to update the nodes in the Preview Frame during playback, and to save the animation data when the user modifies it.
- **State Management:** It keeps track of the current state of the animation, such as the current time, whether it's playing or paused, and the selected keyframe.
- **In-App Testing:** The UI includes a built-in test runner that can trigger sandbox tests and verify UI logic directly within the plugin window.

### Development Tools

- **Ping Main Thread:** A button in the UI triggers a round-trip message to the main thread. This also logs a raw JSON dump of the animation data for the selected component to the Figma console, which is useful for debugging and generating test data.
- **Run Tests:** A button in the UI executes the embedded test suite, verifying both the UI components and the Figma sandbox interactions.
- **Rust Parser:** The `rs/` directory contains strongly-typed Rust data structures (`CustomTimeline`, `VariantAnimation`, etc.) perfectly matching the plugin's robust JSON format, yielding superior parsing performance and strict type safety for the DesignCompose native engine.