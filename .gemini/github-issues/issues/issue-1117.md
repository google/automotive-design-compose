# Issue #1117: ImageContext does not provide useful data when node constraints set to scale

## 1. Issue Metadata
- **Issue #:** 1117
- **Title:** ImageContext does not provide useful data when node constraints set to scale
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/1117](https://github.com/google/automotive-design-compose/issues/1117)
- **Author:** @rylin8
- **Created Date:** 2024-06-03T18:18:16Z
- **Updated Date:** 2024-06-03T18:18:16Z
- **Labels:** `bug`
- **Priority / Type:** Bug / Customization API
- **Status:** Open (Actionable Customization Bug)

---

## 2. Problem Statement & User Impact
DesignCompose allows developers to dynamically supply images using custom callbacks via `@Design(node = "#ImageNode") image: (ImageContext) -> Bitmap?`.
`ImageContext` provides metadata such as `width: Float?` and `height: Float?` so that developers or image loading libraries (Glide, Coil) can fetch/decode a bitmap at the exact target dimensions.

However, when the target node in Figma has its constraints set to **`Scale`** (horizontal or vertical), `imageContext.width` and `imageContext.height` evaluate to **`null`**:
- Image loading libraries cannot determine target dimensions and fall back to full unscaled bitmaps, causing massive memory spikes and slow decode times.
- Layouts using custom image loaders fail to render or render distorted images.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt#L215-L252)
  - [`designcompose/src/main/java/com/android/designcompose/FrameRender.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/FrameRender.kt#L643-L668)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt)

- **Detailed Root Cause:**
  1. In [`CustomizationContext.kt:215-252`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt#L215-L252), `ImageContext` is constructed by reading the uncomputed static design style properties:
     ```kotlin
     val width = if (node.style.layoutStyle.width.hasPoints()) node.style.layoutStyle.width.points else null
     val height = if (node.style.layoutStyle.height.hasPoints()) node.style.layoutStyle.height.points else null
     ```
  2. In Figma, when a node has constraints set to `Scale` (or in AutoLayout when set to `Fill container`), `layoutStyle.width` is represented as a percentage or scale factor (`Dimension.Percent` or `Dimension.Undefined`), so `hasPoints()` returns `false`.
  3. Consequently, `width` and `height` in `ImageContext` become `null`, even though the layout solver has already computed the exact pixel dimensions in `computedLayout.width` and `computedLayout.height`.

---

## 4. Proposed Fix / Implementation Plan
1. **Pass Computed Layout Dimensions:**
   In [`FrameRender.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/FrameRender.kt) and [`SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt):
   Instead of populating `ImageContext` from static design styles (`node.style.layoutStyle`), pass the actual solved layout dimensions:
   ```kotlin
   val resolvedWidth = computedLayout?.width ?: if (node.style.layoutStyle.width.hasPoints()) node.style.layoutStyle.width.points else null
   val resolvedHeight = computedLayout?.height ?: if (node.style.layoutStyle.height.hasPoints()) node.style.layoutStyle.height.points else null
   
   val imageContext = ImageContext(
       width = resolvedWidth,
       height = resolvedHeight,
       minWidth = null,
       maxWidth = null
   )
   ```
2. **Handle Density Scaling:**
   Ensure `resolvedWidth` and `resolvedHeight` account for device screen density (`LocalDensity.current`).

---

## 5. Validation & Reproduction Plan
1. **Unit Test in `CustomizationContextTest.kt`:**
   Create a node whose `width` is `Dimension.Percent(1.0f)` and verify that passing `computedLayout = Layout(width = 400f, height = 300f)` populates `ImageContext(width = 400f, height = 300f)`.
2. **Robolectric Integration Test:**
   Render a scaling image container and verify Coil/Glide receives `Target(400, 300)` instead of `Target.UNDEFINED`.

---

## 6. Drafted GitHub Comment
```markdown
### Analysis & Resolution for Issue #1117:

### Root Cause:
`ImageContext` checks `node.style.layoutStyle.width.hasPoints()`. When a node's constraint is set to `Scale` or `Fill container`, dimensions are stored as percentages or undefined points, causing `hasPoints()` to return `false` and yielding `null` dimensions.

### Fix:
Construct `ImageContext` using the solved layout bounds (`computedLayout.width` and `computedLayout.height`) rather than uncomputed layout styles. This ensures `ImageContext` always supplies the rendered pixel dimensions regardless of whether constraints are fixed, scale, or auto-layout.
```
