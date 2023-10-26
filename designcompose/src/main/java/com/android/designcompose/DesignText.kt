/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.designcompose

import android.util.Log
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.isIdentity
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.FontStyle
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.LineHeight
import com.android.designcompose.serdegen.StyledTextRun
import com.android.designcompose.serdegen.TextAlign
import com.android.designcompose.serdegen.TextAlignVertical
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewStyle
import java.util.Optional
import kotlin.math.roundToInt

internal fun Modifier.textTransform(style: ViewStyle) =
    this.then(
        Modifier.drawWithContent {
            val transform = style.transform.asComposeTransform(density)
            if (transform != null && !transform.isIdentity()) {
                drawContext.transform.transform(transform)
            }
            val blendMode = style.blend_mode.asComposeBlendMode()
            val useBlendModeLayer = style.blend_mode.useLayer()
            if (useBlendModeLayer) {
                val paint = Paint()
                paint.blendMode = blendMode
                drawContext.canvas.saveLayer(Rect(Offset.Zero, drawContext.size), paint)
            }
            drawContent()
            if (useBlendModeLayer) {
                drawContext.canvas.restore()
            }
        }
    )

@Composable
internal fun DesignText(
    modifier: Modifier = Modifier,
    view: View,
    text: String? = null,
    runs: List<StyledTextRun>? = null,
    style: ViewStyle,
    document: DocContent,
    nodeName: String,
    customizations: CustomizationContext,
    parentLayout: ParentLayoutInfo?,
    layoutId: Int,
): Boolean {
    if (!customizations.getVisible(nodeName)) return false

    // Replace newline characters with the line separator that the system uses. This prevents
    // newline
    // characters that are not supported by a font to be printed out with the not-defined (tofu)
    // character
    val newlineChars =
        arrayOf(
            Char(0xD).toString() + Char(0xA).toString(), // CRLF
            Char(0xD).toString(), // CR
            Char(0xA).toString(), // LF
            Char(0x85).toString(), // NEL
            Char(0xB).toString(), // VT
            Char(0xC).toString(), // FF
            Char(0x2028).toString(), // LS
            Char(0x2029).toString(),
        ) // PS
    var newlineFixedText = text
    val ls = System.getProperty("line.separator")
    ls?.let { newlineChars.forEach { newlineFixedText = newlineFixedText?.replace(it, ls) } }

    val density = LocalDensity.current

    // Apply custom text
    var useText = customizations.getText(nodeName)
    if (useText == null)
        useText = (customizations.getTextFunction(nodeName) ?: { newlineFixedText })()

    val fontFamily = DesignSettings.fontFamily(style.font_family)
    val customTextStyle = customizations.getTextStyle(nodeName)
    val textBuilder = AnnotatedString.Builder()

    if (useText != null) {
        textBuilder.append(useText)
    } else if (runs != null) {
        for (run in runs) {
            val textBrushAndOpacity = run.style.text_color.asBrush(document, density.density)
            textBuilder.pushStyle(
                @OptIn(ExperimentalTextApi::class)
                SpanStyle(
                    brush = textBrushAndOpacity?.first,
                    alpha = textBrushAndOpacity?.second ?: 1.0f,
                    fontSize = run.style.font_size.sp,
                    fontWeight =
                        androidx.compose.ui.text.font.FontWeight(
                            run.style.font_weight.value.roundToInt()
                        ),
                    fontStyle =
                        when (run.style.font_style) {
                            is FontStyle.Italic -> androidx.compose.ui.text.font.FontStyle.Italic
                            else -> androidx.compose.ui.text.font.FontStyle.Normal
                        },
                    fontFamily = DesignSettings.fontFamily(run.style.font_family, fontFamily),
                    fontFeatureSettings =
                        run.style.font_features.joinToString(", ") { feature ->
                            String(feature.tag.toByteArray())
                        }
                )
            )
            textBuilder.append(run.text)
            textBuilder.pop()
        }
    }
    val annotatedText = textBuilder.toAnnotatedString()

    val lineHeight =
        customTextStyle?.lineHeight
            ?: when (style.line_height) {
                is LineHeight.Pixels ->
                    if (runs != null) {
                        ((style.line_height as LineHeight.Pixels).value / style.font_size).em
                    } else {
                        (style.line_height as LineHeight.Pixels).value.sp
                    }
                else -> TextUnit.Unspecified
            }
    val fontWeight =
        customTextStyle?.fontWeight
            ?: androidx.compose.ui.text.font.FontWeight(style.font_weight.value.roundToInt())
    val fontStyle =
        customTextStyle?.fontStyle
            ?: when (style.font_style) {
                is FontStyle.Italic -> androidx.compose.ui.text.font.FontStyle.Italic
                else -> androidx.compose.ui.text.font.FontStyle.Normal
            }
    // Compose only supports a single outset shadow on text; we must use a canvas and perform
    // manual text layout (and editing, and accessibility) to do fancier text.
    val shadow =
        style.text_shadow.flatMap { textShadow ->
            Optional.of(
                Shadow(
                    // Ensure that blur radius is never zero, because Compose interprets that as no
                    // shadow (rather than as a hard-edged shadow).
                    blurRadius = textShadow.blur_radius * density.density * blurFudgeFactor + 0.1f,
                    offset =
                        Offset(
                            textShadow.offset[0] * density.density,
                            textShadow.offset[1] * density.density
                        ),
                    color = convertColor(textShadow.color)
                )
            )
        }
    val textBrushAndOpacity = style.text_color.asBrush(document, density.density)
    val textStyle =
        @OptIn(ExperimentalTextApi::class)
        TextStyle(
            brush = textBrushAndOpacity?.first,
            alpha = textBrushAndOpacity?.second ?: 1.0f,
            fontSize = customTextStyle?.fontSize ?: style.font_size.sp,
            fontFamily = fontFamily,
            fontFeatureSettings =
                style.font_features.joinToString(", ") { feature ->
                    String(feature.tag.toByteArray())
                },
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textAlign =
                customTextStyle?.textAlign
                    ?: when (style.text_align) {
                        is TextAlign.Center -> androidx.compose.ui.text.style.TextAlign.Center
                        is TextAlign.Right -> androidx.compose.ui.text.style.TextAlign.Right
                        else -> androidx.compose.ui.text.style.TextAlign.Left
                    },
            shadow = shadow.orElse(null),
        )
    val overflow =
        if (style.text_overflow is com.android.designcompose.serdegen.TextOverflow.Clip)
            TextOverflow.Clip
        else TextOverflow.Ellipsis

    val textLayoutData =
        TextLayoutData(annotatedText, textStyle, LocalFontLoader.current, style.text_size)
    val maxLines = if (style.line_count.isPresent) style.line_count.get().toInt() else Int.MAX_VALUE
    val textMeasureData =
        TextMeasureData(
            textLayoutData,
            density,
            maxLines,
            style.min_width.pointsAsDp(density.density).value
        )

    // Get the layout for this view that describes its size and position.
    val (layout, setLayout) = remember { mutableStateOf<Layout?>(null) }
    // Keep track of the layout state, which changes whenever this view's layout changes
    val (layoutState, setLayoutState) = remember { mutableStateOf(0) }
    // The height and top offset of the text might be slightly different than the height and top
    // that is used in layout. This is because we want to honor the position from Figma used for
    // layout, but when rendering we sometimes need to adjust the position because Compose text is
    // slightly different than Figma text.
    val (renderHeight, setRenderHeight) = remember { mutableStateOf<Int?>(null) }
    val (renderTop, setRenderTop) = remember { mutableStateOf<Int?>(null) }
    // Measure the text and subscribe for layout changes whenever the text data changes.
    DisposableEffect(textMeasureData, style) {
        val parentLayoutId = parentLayout?.parentLayoutId ?: -1
        val rootLayoutId = parentLayout?.rootLayoutId ?: layoutId
        val childIndex = parentLayout?.childIndex ?: -1
        Log.d(
            TAG,
            "Subscribe TEXT $nodeName  layoutId $layoutId parent $parentLayoutId index $childIndex"
        )

        // Only measure the text and subscribe with the resulting size if isAutoHeightFillWidth() is
        // false, because otherwise the measureFunc is used
        if (!isAutoHeightFillWidth(style)) {
            val textBounds = measureTextBounds(style, textLayoutData, density)
            Log.d(
                TAG,
                "Text measure $nodeName: textBounds ${textBounds.width} ${textBounds.layoutHeight} vertOffset ${textBounds.verticalOffset} renderHeight ${textBounds.renderHeight}"
            )
            setRenderHeight(textBounds.renderHeight)
            setRenderTop(textBounds.verticalOffset)

            LayoutManager.subscribeText(
                layoutId,
                setLayoutState,
                parentLayoutId,
                rootLayoutId,
                childIndex,
                style,
                view.name,
                textBounds.width,
                textBounds.layoutHeight
            )
        } else {
            // Use default layout for render height and 0 for top
            setRenderHeight(null)
            setRenderTop(0)

            LayoutManager.subscribeWithMeasure(
                layoutId,
                setLayoutState,
                parentLayoutId,
                rootLayoutId,
                childIndex,
                style,
                view.name,
                textMeasureData
            )
        }

        onDispose {}
    }
    // Unsubscribe to layout changes when the composable is no longer in view.
    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "Unsubscribe TEXT $nodeName layoutId $layoutId")
            LayoutManager.unsubscribe(layoutId)
        }
    }
    LaunchedEffect(layoutState) {
        val newLayout = LayoutManager.getLayout(layoutId)
        setLayout(newLayout)
    }

    val content =
        @Composable {
            val replacementComponent = customizations.getComponent(nodeName)
            if (replacementComponent != null) {
                replacementComponent(
                    object : ComponentReplacementContext {
                        override val layoutModifier = modifier
                        override val appearanceModifier = Modifier

                        @Composable override fun Content() {}

                        override val textStyle = textStyle
                        override val parentLayout = parentLayout
                    }
                )
            } else {
                // Text needs to use a modifier that sets the size so that it wraps properly
                val height = renderHeight ?: layout?.height() ?: 0
                val textModifier = modifier.sizeToModifier(layout?.width() ?: 0, height)
                BasicText(
                    annotatedText,
                    modifier = textModifier,
                    style = textStyle,
                    overflow = overflow,
                )
            }
        }

    val name = view.name
    val layoutModifier =
        modifier
            .wrapContentSize(align = Alignment.TopStart, unbounded = true)
            .textTransform(style)
            .layoutStyle(name, layoutId)
    val layoutWithDensity = layout?.withDensity(density.density)
    DesignTextLayout(
        layoutModifier,
        name,
        layoutWithDensity,
        layoutState,
        renderHeight,
        renderTop,
        content
    )
    return true
}

// Measure text height given a width. Called from Rust as a measure function for text that has auto
// height and variable width. Layout computes the width, then calls this function to get the
// corresponding text height.
fun measureTextBoundsFunc(
    layoutId: Int,
    width: Float,
    height: Float,
    availableWidth: Float,
    availableHeight: Float
): Pair<Float, Float> {
    val density = LayoutManager.getDensity()
    val width = width * density
    val availableWidth = availableWidth * density
    val availableHeight = availableHeight * density

    val textMeasureData = LayoutManager.getTextMeasureData(layoutId)
    if (textMeasureData == null) {
        Log.d(TAG, "measureTextBoundsFunc() error: no textMeasureData for layoutId $layoutId")
        return Pair(0F, 0F)
    }

    // textMeasureData.textLayout.
    val inWidth =
        if (width > 0F) width.toInt()
        else if (textMeasureData.styleWidth > 0F && textMeasureData.styleWidth <= availableWidth)
            textMeasureData.styleWidth.toInt()
        // else if (availableWidth > 0F) availableWidth.toInt()
        else 0 // Int.MAX_VALUE
    val (rectBounds, _) =
        textMeasureData.textLayout.boundsForWidth(
            inWidth,
            textMeasureData.maxLines,
            textMeasureData.density
        )
    val outHeight =
        if (availableHeight > 0f && rectBounds.height().toFloat() > availableHeight) availableHeight
        else rectBounds.height().toFloat()

    return Pair(rectBounds.width().toFloat() / density, outHeight / density)
}

private class TextBounds(
    // Width of the measured text
    val width: Int,
    // Height of the text for layout purposes. This is usually the height of the text in Figma,
    // which is used so that text is laid out in a way to match Figma.
    val layoutHeight: Int,
    // Height of the text for rendering purposes. Since text in Compose is a bit different than in
    // Figma, this is usually taller than layoutHeight and is used to render text so that it does
    // not get cut off at the bottom.
    val renderHeight: Int,
    // Vertical offset to render text, calculated from the text vertical alignment
    val verticalOffset: Int,
)

private fun measureTextBounds(
    style: ViewStyle,
    textLayoutData: TextLayoutData,
    density: Density
): TextBounds {
    var textWidth: Int
    // renderHeight tracks the height used to render the text. This can be slightly different than
    // layoutHeight below because Compose measures text differently than Figma.
    var renderHeight: Int
    // layoutHeight tracks the height used for calculating layout. This is typically the same height
    // defined in Figma, but when the text is set to auto height then this gets set to the height
    // calculated by boundsForWidth().
    var rectBounds: android.graphics.Rect
    var layoutHeight: Int
    when (val width = style.width) {
        is Dimension.Points -> {
            // Fixed width
            textWidth = width.pointsAsDp(density.density).value.roundToInt()
            when (style.height) {
                is Dimension.Points -> {
                    // Fixed height. Get actual height so we can calculate vertical alignment
                    rectBounds = textLayoutData.boundsForWidth(Int.MAX_VALUE, 1, density).first
                    renderHeight = style.bounding_box.height.roundToInt()
                    layoutHeight = style.height.pointsAsDp(density.density).value.roundToInt()
                }
                else -> {
                    // Auto height
                    val maxLines =
                        if (style.line_count.isPresent) style.line_count.get().toInt()
                        else Int.MAX_VALUE
                    rectBounds = textLayoutData.boundsForWidth(textWidth, maxLines, density).first
                    renderHeight = (rectBounds.height().toFloat() / density.density).roundToInt()
                    layoutHeight = rectBounds.height()
                }
            }
        }
        else -> {
            // Auto width, meaning everything is in one line
            // TODO auto width can also span multiple lines; support this
            rectBounds = textLayoutData.boundsForWidth(Int.MAX_VALUE, 1, density).first
            textWidth = rectBounds.width()
            renderHeight = (rectBounds.height().toFloat() / density.density).roundToInt()
            layoutHeight = (textLayoutData.textBoxSize.height * density.density).roundToInt()
        }
    }

    var verticalAlignmentOffset = rectBounds.top
    if (layoutHeight > rectBounds.height()) {
        when (style.text_align_vertical) {
            is TextAlignVertical.Center ->
                verticalAlignmentOffset = (layoutHeight - rectBounds.height()) / 2
            is TextAlignVertical.Bottom ->
                verticalAlignmentOffset = (layoutHeight - rectBounds.height())
        }
    }

    return TextBounds(textWidth, layoutHeight, renderHeight, verticalAlignmentOffset)
}
