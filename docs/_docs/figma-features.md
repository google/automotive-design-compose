---
Title: Figma Feature Support
nav_order: 21
layout: page
---

{% include toc.md %}

## Figma Supported Features {#Features}

- Primitives
  - Frames
  - Text
    - Layout Features: line height, letter spacing, horizontal alignment, vertical alignment, OpenType Flags
    - Styled Sub-Runs
    - ~~Text Case~~
    - Font Properties: weight, italic, ~~underline, strikethrough~~
  - Vectors: Rectangles, Paths, Arcs, Boolean Ops
- Layout
  - Constraints
  - Auto Layout: row, column, padding, etc.
- Fills
  - Solid Color
  - Gradients: Linear, Radial, Angular, ~~Diamond~~
  - Images
- Strokes 
  - Alignment: Inside, Outside, Center
  - Stroke Caps
  - ~~Individual stroke thicknesses~~
  - ~~Strokes on text~~
  - ~~Dashing~~
- Blend Modes
- Masking (one mask per parent, no unmasked siblings)
- Effects
  - Inner Shadow (not on Text)
  - Drop Shadow
  - ~~(Layer) Blur~~
  - ~~Background Blur~~
- ~~Image Filters~~
- Components & Variants
- Prototyping & Interactive Components
  - Actions: Navigate, Back, Close, Swap, Overlay, Change To
  - Triggers: Click, Press, KeyDown, After Timeout ~~Mouse support: Mouse Enter, Mouse Leave, Hover~~
  - ~~Animations~~

## DesignCompose Extensions {#Extensions}

- Live Update \
    DesignCompose checks for updates from Figma every 5 seconds and automatically re-renders the changes.

- Design Switcher \
    The Design Switcher shows up in the upper right corner of a DesignCompose app. It shows live update details and lets you change the current Figma document.

### Plugins
- Text Eliding & Max Line Count \
    Fine grained text control; hoping to retire this since Figma recently extended their native text capabilities.
- Lists & Grids \
    Flexible layout for complex (and very long) list data
- Dials & Gauges \
    Create dials & gauges of different kinds by applying values to arcs, rects, and performing translations.
