# DC Squoosh Parser

A Rust library for parsing and interpolating custom animation data exported from the DC Squoosh Designer Figma plugin.

## Usage

Add this to your `Cargo.toml`:
```toml
[dependencies]
dc_squoosh_parser = { path = "path/to/rs" }
serde_json = "1.0"
```

## Core Types

### `Variant`
The root structure representing a Figma Component Variant. It contains the animation specification.

### `PropertyLookup`
A helper structure to efficiently retrieve animation timelines for specific nodes and properties.

```rust
let lookup = PropertyLookup::new(&variant);
if let Some(timeline) = lookup.get("NodeName", "property.path") {
    // ...
}
```

### `KeyframeValue`
An enum representing the value of a property at a specific point in time. Supports interpolation.
*   `Scalar(f32)`: For `x`, `y`, `width`, `height`, `opacity`, `rotation`.
*   `CornerRadii([f32; 4])`: For `topLeftRadius`, etc. (normalized to 4-element array).
*   `Color(Rgba)`: For solid fills/strokes.
*   `Gradient(Vec<GradientStop>)`: For gradient fills.
*   `Arc(ArcData)`: For ellipse arcs.

## Interpolation

The library provides optimized, low-allocation interpolation suitable for render loops.

```rust
// 1. Get the timeline
let timeline = lookup.get("Button", "x").unwrap();

// 2. Define Start/End values (from your scene graph)
let start = KeyframeValue::Scalar(0.0);
let end = KeyframeValue::Scalar(100.0);

// 3. Interpolate at time t (0.0 to 1.0)
// This automatically handles custom keyframes and easing defined in the timeline.
let current_value = timeline.interpolate(&start, &end, 0.5);
```

## Easing Functions
Supports standard easing functions:
*   Linear
*   EaseIn, EaseOut, EaseInOut (Quad)
*   EaseInCubic, EaseOutCubic, EaseInOutCubic
*   Instant
