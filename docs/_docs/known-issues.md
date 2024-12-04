---
Title: Known Issues
nav_order: 20
layout: page
---

{% include toc.md %}

# Known Issues

## Layout {#Layout}

### Auto Layout containers don't shrink {#AutoLayoutContainers}

Auto Layout containers with Hug Contents set don't shrink to appear smaller
than they appear in the design. This can be an issue if a design contains a list
(auto-content) with more placeholder items in the design than the app provides
at runtime.

### Layout properties are not supported on Figma Group elements {#LayoutPropertiesNotSupported}

Layout properties applied to Figma Group elements are not supported. A
workaround is to use a Frame element instead of a Group element.

### Layout constraints on top-level components {#LayoutConstraints}

Layout constraints applied to top-level components are not honored when the
component is placed in a Composable customization. As a workaround, use Auto
Layout properties to achieve the same result. (Layout constraints are supported
in general).

## Render {#Render}

### Layer Blur and Background Blur effects {#LayerBlur}

DesignCompose does not support Layer Blur and Background Blur effects.

### Blend modes {#BlendModes}

Only Normal and Pass Through blend modes are supported. All other blend modes
are interpreted as Normal.

### Gradients {#Gradients}

All gradients types are supported except for the diamond gradient.

### Stroke properties {#StrokeProperties}

Dashed strokes and distinct top-, left-, bottom-, and right-stroke thicknesses
are ignored.

### Text {#Text}

Centered strokes on text are supported. Inside and outside strokes on text are rendered as centered strokes. Letter case is ignored.

## Interaction {#Interaction}

### Rotary input {#RotaryInput}

DesignCompose does not support rotary input. For news on possible future
support, ask your Google point of contact.

### Animation {#Animation}

DesignCompose does not support the importing of animations from Figma. For news
on possible future support, ask your Google point of contact.

## Runtime {#Runtime}

### Live updates on shadows on vectors {#LiveUpdatesonShadows}

Live updates don't immediately reflect updates to the Shadow Effect property
when applied to vectors.
