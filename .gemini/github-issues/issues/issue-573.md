# Issue #573: Make Brush function customization accept an `ImageContext`

## 1. Issue Metadata
- **Issue #:** 573
- **Title:** Make Brush function customization accept an `ImageContext`
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/573](https://github.com/google/automotive-design-compose/issues/573)
- **Author:** @iamralpht
- **Created Date:** 2023-11-21T21:40:48Z
- **Updated Date:** 2023-11-21T21:40:48Z
- **Labels:** `Type: New Feature`, `:green_circle:  P2`, `java`
- **Priority / Type:** P2 / API Enhancement Request
- **Status:** Open (Actionable Feature Request)

---

## 2. Problem Statement & User Impact
DesignCompose allows developers to customize node backgrounds and strokes using custom Compose `Brush`es (such as gradients, animated shaders, or AGSL runtime shaders) via `@Design(node = "#Node") customBrush: BrushFunction`.

Currently, `BrushFunction` is defined as a parameterless lambda:
```kotlin
typealias BrushFunction = () -> Brush?
```
Because it receives no parameters, the custom brush function cannot inspect:
- The node's rendered layout size (width and height in pixels).
- The original design colors defined by the Figma designer.
- The node's density or local coordinate space.

To create responsive gradients (e.g. `Brush.linearGradient(colors, start = Offset.Zero, end = Offset(width, height))`) or AGSL runtime shaders that need pixel coordinates and colors, developers are forced to hardcode dimensions or wrap the entire component in a `ComponentReplacementContext`.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt#L215-L280)
  - [`designcompose/src/main/java/com/android/designcompose/FrameRender.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/FrameRender.kt#L470-L495)
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt)
  - [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt)

- **Detailed Root Cause:**
  1. `ImageContext` already exists in [`CustomizationContext.kt:215`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt#L215) and supplies `width`, `height`, and node metadata for custom image customizations.
  2. In [`CustomizationContext.kt:273`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt#L273), the brush customization is stored as:
     ```kotlin
     fun setBrushFunction(nodeName: String, brushFunction: () -> Brush?)
     ```
  3. During rendering in Squoosh or `FrameRender.kt`, the function is invoked without arguments: `val brush = brushFunction()`. Passing an `ImageContext` (or `BrushContext`) with the node's dimensions and designer colors is directly feasible but was never exposed in the API.

---

## 4. Proposed Fix / Implementation Plan
1. **Add Overloaded Function Type in `CustomizationContext.kt`:**
   ```kotlin
   typealias BrushFunction = (ImageContext) -> Brush?
   
   fun setBrushFunction(nodeName: String, brushFunction: (ImageContext) -> Brush?) {
       brushFunctions[nodeName] = brushFunction
   }
   
   // Backward-compatible overload:
   @JvmName("setBrushFunctionNoArgs")
   fun setBrushFunction(nodeName: String, brushFunction: () -> Brush?) {
       setBrushFunction(nodeName) { _ -> brushFunction() }
   }
   ```
2. **Update Codegen in `BuilderProcessor.kt`:**
   Allow `@Design(node = "#Node") brush: (ImageContext) -> Brush?` in generated composable signatures in addition to `() -> Brush?`.
3. **Pass `ImageContext` at Call-sites in Squoosh / FrameRender:**
   Construct `ImageContext` with the node's rendered size and original color palette when evaluating custom brushes.

---

## 5. Validation & Reproduction Plan
1. **Unit Test:**
   Add a test in `CustomizationContextTest.kt` verifying that `setBrushFunction` receives a non-null `ImageContext` containing the node width and height.
2. **Robolectric Shader Test:**
   Create a test composable with a linear gradient brush that sizes itself dynamically from `imageContext.width` and `imageContext.height`.

---

## 6. Drafted GitHub Comment
```markdown
### Proposed API Enhancement for Brush Customization:

To allow dynamic gradients and shaders to adapt to node dimensions and Figma colors, we propose updating `BrushFunction` to accept `ImageContext`:

```kotlin
// In CustomizationContext.kt
typealias BrushFunction = (ImageContext) -> Brush?

// Usage in Kotlin:
@Composable
fun MyComponent() {
    MyFigmaView(
        backgroundBrush = { context ->
            Brush.linearGradient(
                colors = listOf(Color.Blue, Color.Cyan),
                start = Offset.Zero,
                end = Offset(context.width ?: 100f, context.height ?: 100f)
            )
        }
    )
}
```

A backward-compatible overload accepting `() -> Brush?` will be preserved so existing code continues to compile without changes.
```
