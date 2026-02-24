# Animation Timeline Editor - Design Document (Iteration 2)

## 1. Overview

The animation timeline editor is a TypeScript-based UI widget for visualizing and manipulating animation keyframes. It's designed to be a reusable component that can be easily integrated into other applications.

The widget will display a hierarchical view of nodes and their animatable properties. Each property will have a timeline where keyframes can be added, selected, and moved. The timeline's duration is normalized to a range of `[0.0, 1.0]`.

This document outlines the component's architecture, data model, API, and implementation details.

## 2. Data Model

The core data model is defined by the following TypeScript interfaces. This model is designed to be flexible and easily serializable.

```typescript
// Represents a single keyframe on a timeline.
export interface Keyframe {
  id: string;
  position: number; // Timeline position (0.0 to 1.0)
  value: any; // The actual animated value (e.g., a number, a string for color, etc.)
  locked?: boolean; // If true, cannot be moved or deleted (e.g., variant keyframes)
  easing?: string; // Easing function name (e.g., "Linear", "EaseIn")
  isMissing?: boolean; // If true, indicates the node is missing in this variant
}

// Represents a timeline for a single animatable property.
export interface Timeline {
  id: string;
  property: string; // Name of the animated property (e.g., "position.x")
  keyframes: Keyframe[];
  isCustom?: boolean; // If true, this timeline was added by the user
}

// Represents a node in the hierarchy.
export interface Node {
  id: string;
  name: string;
  timelines: Timeline[];
  children: Node[];
}

// Represents the entire animation data.
export interface AnimationData {
  nodes: Node[];
}
```

## 3. Architecture

The editor will be built with a component-based architecture, promoting separation of concerns and reusability.

- **`TimelineEditor` (Main Component):**
  - The root component that orchestrates all other components.
  - Manages the application state.
  - Handles user input and dispatches events.
  - Renders the main layout, including the node tree and the timeline grid.

- **`NodeTreeView`:**
  - Renders the hierarchical list of nodes.
  - Handles expanding and collapsing of nodes.
  - Emits events when a node is selected.

- **`TimelineGridView`:**
  - Renders the timelines, keyframes, and the grid.
  - Handles direct manipulation of keyframes (selection, dragging).
  - Visualizes the selected ranges.

- **`State`:**
  - A simple state management object that holds the current state of the editor, including the animation data, selection, etc. This will be a plain object, and the `TimelineEditor` will be responsible for updating it.

## 4. API

The `TimelineEditor` will expose a clean and simple API for interacting with the component.

### Constructor

- `new TimelineEditor(container: HTMLElement, options?: EditorOptions)`: Creates a new instance of the timeline editor within the given container element.

### Methods

- `setData(data: AnimationData): void`: Sets the animation data to be displayed and rendered.
- `getData(): AnimationData`: Retrieves the current animation data.
- `on(eventName: string, callback: Function): void`: Subscribes to events.
- `off(eventName: string, callback: Function): void`: Unsubscribes from events.
- `destroy(): void`: Cleans up the component and removes it from the DOM.

### Events

The component will emit the following events:

- `'keyframe:select'`: When a keyframe is selected.
  - Payload: `{ keyframe: Keyframe, timeline: Timeline, node: Node }`
- `'keyframe:move'`: When a keyframe is moved.
  - Payload: `{ keyframe: Keyframe, newPosition: number }`
- `'range:select'`: When a time range is selected.
  - Payload: `{ start: number, end: number, timeline: Timeline }`

## 5. HTML Structure

The component will have a clear and semantic HTML structure.

```html
<div class="timeline-editor">
  <div class="node-tree">
    <!-- NodeTreeView will render nodes here -->
  </div>
  <div class="timeline-grid">
    <!-- TimelineGridView will render timelines and keyframes here -->
  </div>
</div>
```

## 6. Implementation Details

- **Rendering:** The component will be rendered using the standard DOM API. No external libraries or frameworks are required.
- **Styling:** CSS with BEM (Block, Element, Modifier) naming convention for clear and maintainable styles.
- **Dependencies:** No runtime dependencies.
- **Tests:** Unit tests will be written using a simple testing framework (or just plain assertions) to ensure the component's logic is correct.
- **`test.html`:** A simple HTML file will be created to host and test the component with sample data.

This refined design provides a more solid foundation for implementation. It clarifies the data model, component responsibilities, and the external API. The state management is explicitly mentioned, and the HTML structure is defined.
