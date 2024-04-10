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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tracing.trace
import com.android.designcompose.serdegen.FontStyle
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.LineHeight
import com.android.designcompose.serdegen.StyledTextRun
import com.android.designcompose.serdegen.TextAlign
import com.android.designcompose.serdegen.TextAlignVertical
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewStyle
import java.util.Optional
import kotlin.math.ceil
import kotlin.math.roundToInt

internal fun Modifier.textTransform(style: ViewStyle) =
    this.then(
        Modifier.drawWithContent {
            val transform = style.node_style.transform.asComposeTransform(density)
            if (transform != null && !transform.isIdentity()) {
                drawContext.transform.transform(transform)
            }
            val blendMode = style.node_style.blend_mode.asComposeBlendMode()
            val useBlendModeLayer = style.node_style.blend_mode.useLayer()
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

// Data class that holds all the data that, if changed, should trigger a layout resubscription so
// that the text can be remeasured.
internal data class TextData(
    val annotatedText: AnnotatedString,
    val textStyle: TextStyle,
    val style: ViewStyle,
    val density: Density,
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
            Char(0x2029).toString(), // PS
        )
    var newlineFixedText = text
    val ls = System.getProperty("line.separator")
    ls?.let { newlineChars.forEach { newlineFixedText = newlineFixedText?.replace(it, ls) } }

    val density = LocalDensity.current

    // Apply custom text
    var useText = customizations.getText(nodeName)
    if (useText == null)
        useText = (customizations.getTextFunction(nodeName) ?: { newlineFixedText })()

    val fontFamily = DesignSettings.fontFamily(style.node_style.font_family)
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
            ?: when (style.node_style.line_height) {
                is LineHeight.Pixels ->
                    if (runs != null) {
                        ((style.node_style.line_height as LineHeight.Pixels).value /
                                style.node_style.font_size)
                            .em
                    } else {
                        (style.node_style.line_height as LineHeight.Pixels).value.sp
                    }
                else -> TextUnit.Unspecified
            }
    val fontWeight =
        customTextStyle?.fontWeight
            ?: androidx.compose.ui.text.font.FontWeight(
                style.node_style.font_weight.value.roundToInt()
            )
    val fontStyle =
        customTextStyle?.fontStyle
            ?: when (style.node_style.font_style) {
                is FontStyle.Italic -> androidx.compose.ui.text.font.FontStyle.Italic
                else -> androidx.compose.ui.text.font.FontStyle.Normal
            }
    // Compose only supports a single outset shadow on text; we must use a canvas and perform
    // manual text layout (and editing, and accessibility) to do fancier text.
    val shadow =
        style.node_style.text_shadow.flatMap { textShadow ->
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
    val customBrushFunction = customizations.getBrushFunction(nodeName)
    val customBrush =
        if (customBrushFunction != null) {
            customBrushFunction()
        } else {
            customizations.getBrush(nodeName)
        }

    val textBrushAndOpacity = style.node_style.text_color.asBrush(document, density.density)
    val textStyle =
        @OptIn(ExperimentalTextApi::class)
        TextStyle(
            brush = customBrush ?: textBrushAndOpacity?.first,
            alpha = textBrushAndOpacity?.second ?: 1.0f,
            fontSize = customTextStyle?.fontSize ?: style.node_style.font_size.sp,
            fontFamily = fontFamily,
            fontFeatureSettings =
                style.node_style.font_features.joinToString(", ") { feature ->
                    String(feature.tag.toByteArray())
                },
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textAlign =
                customTextStyle?.textAlign
                    ?: when (style.node_style.text_align) {
                        is TextAlign.Center -> androidx.compose.ui.text.style.TextAlign.Center
                        is TextAlign.Right -> androidx.compose.ui.text.style.TextAlign.Right
                        else -> androidx.compose.ui.text.style.TextAlign.Left
                    },
            shadow = shadow.orElse(null),
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
        )
    val overflow =
        if (style.node_style.text_overflow is com.android.designcompose.serdegen.TextOverflow.Clip)
            TextOverflow.Clip
        else TextOverflow.Ellipsis

    val paragraph =
        ParagraphIntrinsics(
            text = annotatedText.text,
            style = textStyle,
            spanStyles = annotatedText.spanStyles,
            density = density,
            resourceLoader = LocalFontLoader.current
        )

    val maxLines =
        if (style.node_style.line_count.isPresent) style.node_style.line_count.get().toInt()
        else Int.MAX_VALUE
    val textMeasureData =
        TextMeasureData(
            annotatedText.text.hashCode(),
            paragraph,
            density,
            maxLines,
            style.isAutoWidthText(),
        )

    // Get the layout for this view that describes its size and position.
    val (layout, setLayout) = remember { mutableStateOf<Layout?>(null) }
    // Keep track of the layout state, which changes whenever this view's layout changes
    val (layoutState, setLayoutState) = remember { mutableStateOf(0) }
    val parentLayout = LocalParentLayoutInfo.current
    val rootLayoutId = parentLayout?.rootLayoutId ?: layoutId
    // Subscribe for layout changes whenever the text data changes, and use a measure function to
    // measure the text width and height
    val textData = TextData(annotatedText, textStyle, style, density)
    DisposableEffect(textData) {
        trace(DCTraces.DESIGNTEXT_DE_SUBSCRIBE) {
            val parentLayoutId = parentLayout?.parentLayoutId ?: -1
            val childIndex = parentLayout?.childIndex ?: -1
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
            LayoutManager.unsubscribe(
                layoutId,
                rootLayoutId,
                parentLayout?.isWidgetAncestor == true
            )
        }
    }
    LaunchedEffect(layoutState) {
        val newLayout = LayoutManager.getLayout(layoutId)
        setLayout(newLayout)
    }

    val content =
        @Composable {
            // Text needs to use a modifier that sets the size so that it wraps properly
            val height = layout?.height() ?: 0
            val textModifier = modifier.sizeToModifier(layout?.width() ?: 0, height)
            val replacementComponent = customizations.getComponent(nodeName)
            if (replacementComponent != null) {
                replacementComponent(
                    object : ComponentReplacementContext {
                        override val layoutModifier = textModifier
                        override val textStyle = textStyle
                    }
                )
            } else {
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

    // Measure and compre to layout to apply vertical centering
    val verticalOffset =
        if (layoutWithDensity != null) {
            val paragraph =
                Paragraph(
                    paragraphIntrinsics = textMeasureData.paragraph,
                    width = layoutWithDensity.width,
                    maxLines = textMeasureData.maxLines,
                    ellipsis =
                        style.node_style.text_overflow
                            is com.android.designcompose.serdegen.TextOverflow.Ellipsis
                )

            when (style.node_style.text_align_vertical) {
                is TextAlignVertical.Center -> (layoutWithDensity.height - paragraph.height) / 2F
                is TextAlignVertical.Bottom -> layoutWithDensity.height - paragraph.height
                else -> 0F
            }
        } else {
            0F
        }
    DesignTextLayout(
        layoutModifier,
        layoutWithDensity,
        layoutState,
        verticalOffset.roundToInt(),
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
    @Suppress("unused") height: Float,
    availableWidth: Float,
    @Suppress("unused") availableHeight: Float
): Pair<Float, Float> {
    // We currently don't support vertical text, only horizontal, so this function just performs
    // height-for-width queries on text, and ignores the `height` and `availableHeight` args.

    // Look up the measure data -- this map is created/updated when building the layout tree.
    val textMeasureData = LayoutManager.getTextMeasureData(layoutId)
    if (textMeasureData == null) {
        Log.d(TAG, "measureTextBoundsFunc() error: no textMeasureData for layoutId $layoutId")
        return Pair(0F, 0F)
    }
    val density = textMeasureData.density.density

    // Some distinct values are being collapsed, so we can't tell the difference between no
    // available space, and a request to report the minimum space.
    val layoutWidth =
        if (textMeasureData.autoWidth) {
            textMeasureData.paragraph.maxIntrinsicWidth
        } else if (width > 0.0f) {
            width * density
        } else if (availableWidth <= 0.0f) {
            textMeasureData.paragraph.minIntrinsicWidth
        } else if (availableWidth >= Float.MAX_VALUE) {
            textMeasureData.paragraph.maxIntrinsicWidth
        } else {
            availableWidth * density
        }

    // Perform a layout using the given width.
    val textLayout =
        Paragraph(
            paragraphIntrinsics = textMeasureData.paragraph,
            width = layoutWidth,
            maxLines = textMeasureData.maxLines
        )

    // The `textLayout.width` field doesn't give the tightest bounds.
    var maxLineWidth = 0.0f
    for (i in 0 until textLayout.lineCount) {
        maxLineWidth = textLayout.getLineWidth(i).coerceAtLeast(maxLineWidth)
    }

    return Pair(ceil(maxLineWidth / density), ceil(textLayout.height / density))
}
