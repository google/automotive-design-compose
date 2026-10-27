# Issue #398: Text alignment with multiple lines is incorrect

## 1. Issue Metadata
- **Issue #:** 398
- **Title:** Text alignment with multiple lines is incorrect
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/398](https://github.com/google/automotive-design-compose/issues/398)
- **Author:** @rylin8
- **Created Date:** 2023-09-28T22:09:56Z
- **Updated Date:** 2023-11-14T21:47:34Z
- **Labels:** `:green_circle:  P2`, `layout`
- **Priority / Type:** P2 / Text Layout Bug
- **Status:** Open (Actionable Text Layout Alignment Enhancement)

---

## 2. Problem Statement & User Impact
In Figma, text nodes can specify horizontal alignment (`Left`, `Center`, `Right`, `Justified`) and vertical alignment (`Top`, `Center`, `Bottom`) combined with resizing modes (`Auto width`, `Auto height`, `Fixed size`).

When multi-line text is rendered in DesignCompose:
1. Multi-line text inside an `Auto width` or `Auto height` frame often misaligns when set to `Center` or `Right` alignment.
2. In multi-line text with varying line lengths, the second and subsequent lines may not align correctly with respect to the node's bounding frame.
3. Vertical centering of multi-line text within fixed-height frames exhibits vertical drift due to discrepancies between Figma's baseline/line-height metrics and Android FontMetrics.

This affects dashboards, message dialogs, settings labels, and alert notifications where text wraps across multiple lines.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`designcompose/src/main/java/com/android/designcompose/squoosh/SquooshText.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshText.kt#L70-L150)
  - [`crates/dc_figma_import/src/transform_flexbox.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/transform_flexbox.rs#L710-L760)

- **Detailed Root Cause:**
  1. **Line Measure vs. Block Measure:**
     In [`SquooshText.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshText.kt), text measurement creates an Android `TextLayout` (`StaticLayout` or Compose `TextMeasurer`). In `Auto width` mode with multiple lines, `TextMeasurer` uses the maximum line width for the overall layout bounds. However, when individual lines are drawn, if `textAlign` is not mapped directly to `StaticLayout.Alignment`, the lines align to the left of the measured block instead of the parent box.
  2. **Line Height Handling:**
     Figma specifies line height as a fixed point size or percentage of font size. If a font's internal ascender and descender metrics differ from Figma's line-height calculation, multi-line vertical offsets accumulate cumulative errors.

---

## 4. Proposed Fix / Implementation Plan
1. **Ensure `TextAlign` is Propagated to `TextMeasurer` and `Paragraph`:**
   In [`SquooshText.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/squoosh/SquooshText.kt):
   ```kotlin
   val composeTextAlign = when (node.style.textAlign) {
       ViewStyle.TextAlign.Center -> TextAlign.Center
       ViewStyle.TextAlign.Right -> TextAlign.End
       ViewStyle.TextAlign.Justified -> TextAlign.Justify
       else -> TextAlign.Start
   }
   ```
2. **Explicit Width Constraints for Multi-line Center/Right Alignment:**
   When rendering multi-line text with `Center` or `Right` alignment, ensure the text layout width is constrained to the full available width of the text container (`computedLayout.width`), rather than the intrinsic width of the longest text line.
3. **Line Height Truncation/Padding Fix:**
   Set `PlatformTextStyle(includeFontPadding = false)` to prevent Android's default font padding from introducing asymmetric vertical offsets.

---

## 5. Validation & Reproduction Plan
1. **Unit Test in `SquooshTextTest.kt`:**
   Add test cases in [`designcompose/src/test/kotlin/com/android/designcompose/squoosh/SquooshTextTest.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/test/kotlin/com/android/designcompose/squoosh/SquooshTextTest.kt):
   - 3-line text with `TextAlign.Center` and `Auto width`.
   - 3-line text with `TextAlign.Right` inside a fixed-width container.
   - Verify line X-coordinates match expected center/right bounds.
2. **Visual Parity Test:**
   Inspect the Alignment Validation document in the integration test suite.

---

## 6. Drafted GitHub Comment
```markdown
### Technical Update on Multi-line Text Alignment:

Multi-line text alignment issues occur when the `TextLayout` is measured using the intrinsic width of the longest line rather than the parent container width, causing centered or right-aligned lines to be offset.

### Proposed Code Fix:
In `SquooshText.kt`:
1. Ensure `TextAlign.Center` / `TextAlign.End` is explicitly passed to the `ParagraphIntrinsics` / `TextMeasurer`.
2. When creating the paragraph layout, set the layout width to `constraints.maxWidth` (or the computed node width) so lines align relative to the full node box.
3. Apply `PlatformTextStyle(includeFontPadding = false)` to avoid vertical misalignment.
```
