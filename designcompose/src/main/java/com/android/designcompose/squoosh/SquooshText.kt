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

package com.android.designcompose.squoosh

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DesignSettings
import com.android.designcompose.DocContent
import com.android.designcompose.TextMeasureData
import com.android.designcompose.VariableState
import com.android.designcompose.definition.element.LineHeight.LineHeightTypeCase
import com.android.designcompose.definition.element.TextDecoration
import com.android.designcompose.definition.element.weightOrNull
import com.android.designcompose.definition.view.View
import com.android.designcompose.definition.view.fontSizeOrNull
import com.android.designcompose.definition.view.fontWeightOrNull
import com.android.designcompose.definition.view.styleOrNull
import com.android.designcompose.definition.view.textColorOrNull
import com.android.designcompose.definition.view.textShadowOrNull
import com.android.designcompose.getText
import com.android.designcompose.getTextState
import com.android.designcompose.getTextStyle
import com.android.designcompose.getValue
import com.android.designcompose.proto.getOrThrow
import com.android.designcompose.utils.asBrush
import com.android.designcompose.utils.blurFudgeFactor
import com.android.designcompose.utils.getTextContent
import com.android.designcompose.utils.isAutoWidthText
import kotlin.math.roundToInt

val newlineRegex = Regex("\\R+")
val lineSeparator: String? = System.getProperty("line.separator")
const val defaultFontWeight = 400
const val defaultFontSize = 18f

private fun normalizeNewlines(text: String): String {
    if (lineSeparator != null) return text.replace(newlineRegex, lineSeparator)
    return text
}

/// A simple generational cache for TextMeasureData, because it is expensive to compute.
internal class TextMeasureCache {
    internal class Entry(
        val data: TextMeasureData,
        val text: String?,
        val style: TextStyle?,
        val annotatedText: String,
        // XXX: Do we need to use the annotated string? This impl might break localization.
    )

    private var cache: HashMap<Int, Entry> = HashMap()
    private var nextGeneration: HashMap<Int, Entry> = HashMap()

    fun get(layoutId: Int): Entry? {
        return cache[layoutId]
    }

    fun put(layoutId: Int, data: Entry) {
        // Store in the cache as well as nextGeneration because the same TextMeasureCache
        // gets used for the base tree and transition tree without a call to collect between.
        cache[layoutId] = data
        nextGeneration[layoutId] = data
    }

    /// Perform a garbage collection by releasing all of the cache entries that were not
    /// used since the last time `collect` was called. `SquooshRoot` will generate the
    /// base tree and the transition tree between calls to `collect()`.
    fun collect() {
        cache.clear()
        cache.putAll(nextGeneration)
        nextGeneration.clear()
    }
}

/// Take DesignCompose text information, and generate the information required to measure
/// and render text using Compose APIs.
///
/// Internally, this function creates a Compose ParagraphIntrinsics, which does some work
/// on the text in order to quickly answer layout queries. Creating a ParagraphIntrinsics
/// is heavyweight, so we should avoid doing it frequently.
internal fun squooshComputeTextInfo(
    v: View,
    layoutId: Int,
    density: Density,
    document: DocContent,
    customizations: CustomizationContext,
    fontResourceLoader: Font.ResourceLoader,
    variableState: VariableState,
    appContext: Context,
    textMeasureCache: TextMeasureCache,
    textHash: HashSet<String>,
): TextMeasureData? {
    val customizedText =
        customizations.getText(v.name) ?: customizations.getTextState(v.name)?.value
    val customTextStyle = customizations.getTextStyle(v.name)
    val fontFamily =
        DesignSettings.fontFamily(v.style.nodeStyle.takeIf { it.hasFontFamily() }?.fontFamily)

    val cachedText = textMeasureCache.get(layoutId)
    if (
        cachedText != null &&
            cachedText.text == customizedText &&
            cachedText.style == customTextStyle
    ) {
        textHash.add(cachedText.annotatedText)
        textMeasureCache.put(layoutId, cachedText)
        return cachedText.data
    }

    val annotatedText =
        if (customizedText != null) {
            val builder = AnnotatedString.Builder()
            builder.append(normalizeNewlines(customizedText))
            builder.toAnnotatedString()
        } else {
            if (v.data.hasText()) {
                val builder = AnnotatedString.Builder()
                builder.append(normalizeNewlines(getTextContent(appContext, v.data.text)))
                builder.toAnnotatedString()
            } else if (v.data.hasStyledText()) {
                val builder = AnnotatedString.Builder()
                for (run in getTextContent(appContext, v.data.styledText)) {
                    val style = run.styleOrNull.getOrThrow("StyledTextRun")
                    val textBrushAndOpacity =
                        style.textColorOrNull
                            .getOrThrow("textColor")
                            .asBrush(appContext, document, density.density, variableState)
                    val fontWeight =
                        style.fontWeightOrNull
                            .getOrThrow("fontWeight")
                            .weight
                            .getValue(variableState)
                    builder.pushStyle(
                        (SpanStyle(
                            brush = textBrushAndOpacity?.first,
                            alpha = textBrushAndOpacity?.second ?: 1.0f,
                            fontSize =
                                style.fontSizeOrNull
                                    .getOrThrow("fontSize")
                                    .getValue(variableState)
                                    .sp,
                            fontWeight = FontWeight(fontWeight.roundToInt()),
                            fontStyle =
                                when (style.fontStyle) {
                                    com.android.designcompose.definition.element.FontStyle
                                        .FONT_STYLE_ITALIC -> FontStyle.Italic
                                    else -> FontStyle.Normal
                                },
                            fontFamily =
                                DesignSettings.fontFamily(
                                    style.takeIf { it.hasFontFamily() }?.fontFamily,
                                    fontFamily,
                                ),
                            fontFeatureSettings =
                                style.fontFeaturesList.joinToString(", ") { feature ->
                                    String(feature.tag.toByteArray())
                                },
                            letterSpacing = style.letterSpacing.sp,
                            textDecoration =
                                when (style.textDecoration) {
                                    TextDecoration.TEXT_DECORATION_UNDERLINE ->
                                        androidx.compose.ui.text.style.TextDecoration.Underline
                                    TextDecoration.TEXT_DECORATION_STRIKETHROUGH ->
                                        androidx.compose.ui.text.style.TextDecoration.LineThrough
                                    else -> androidx.compose.ui.text.style.TextDecoration.None
                                },
                            // platformStyle = PlatformSpanStyle(includeFontPadding = false),
                        ))
                    )
                    builder.append(run.text)
                    builder.pop()
                }
                builder.toAnnotatedString()
            } else return null
        }

    val lineHeight =
        customTextStyle?.lineHeight
            ?: when (v.style.nodeStyle.lineHeight.lineHeightTypeCase) {
                LineHeightTypeCase.PIXELS -> v.style.nodeStyle.lineHeight.pixels.sp
                else -> TextUnit.Unspecified
            }
    val fontWeight =
        customTextStyle?.fontWeight
            ?: FontWeight(
                v.style.nodeStyle.fontWeightOrNull
                    ?.weightOrNull
                    ?.getValue(variableState)
                    ?.roundToInt() ?: defaultFontWeight
            )
    val fontStyle =
        customTextStyle?.fontStyle
            ?: when (v.style.nodeStyle.fontStyle) {
                com.android.designcompose.definition.element.FontStyle.FONT_STYLE_ITALIC ->
                    FontStyle.Italic
                else -> FontStyle.Normal
            }
    val textDecoration =
        customTextStyle?.textDecoration
            ?: when (v.style.nodeStyle.textDecoration) {
                TextDecoration.TEXT_DECORATION_UNDERLINE ->
                    androidx.compose.ui.text.style.TextDecoration.Underline
                TextDecoration.TEXT_DECORATION_STRIKETHROUGH ->
                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                else -> androidx.compose.ui.text.style.TextDecoration.None
            }
    val letterSpacing =
        customTextStyle?.letterSpacing
            ?: (v.style.nodeStyle.takeIf { it.hasLetterSpacing() }?.letterSpacing ?: 0f).sp
    // Compose only supports a single outset shadow on text; we must use a canvas and perform
    // manual text layout (and editing, and accessibility) to do fancier text.
    val shadow =
        v.style.nodeStyle.textShadowOrNull?.let { textShadow ->
            Shadow(
                // Ensure that blur radius is never zero, because Compose interprets that as no
                // shadow (rather than as a hard-edged shadow).
                blurRadius = textShadow.blurRadius * density.density * blurFudgeFactor + 0.1f,
                offset =
                    Offset(
                        textShadow.offsetX * density.density,
                        textShadow.offsetY * density.density,
                    ),
                color = textShadow.color.getValue(variableState) ?: Color.Transparent,
            )
        }
    // The brush and opacity is computed later at rendering time.
    val textStyle =
        (TextStyle(
            fontSize =
                customTextStyle?.fontSize
                    ?: (v.style.nodeStyle.fontSize?.getValue(variableState) ?: defaultFontSize).sp,
            fontFamily = fontFamily,
            fontFeatureSettings =
                v.style.nodeStyle.fontFeaturesList.joinToString(", ") { feature ->
                    String(feature.tag.toByteArray())
                },
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            textDecoration = textDecoration,
            letterSpacing = letterSpacing,
            fontStyle = fontStyle,
            textAlign =
                customTextStyle?.textAlign
                    ?: when (v.style.nodeStyle.textAlign) {
                        com.android.designcompose.definition.modifier.TextAlign.TEXT_ALIGN_CENTER ->
                            TextAlign.Center
                        com.android.designcompose.definition.modifier.TextAlign.TEXT_ALIGN_RIGHT ->
                            TextAlign.Right
                        else -> TextAlign.Left
                    },
            shadow = shadow,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
        ))

    val paragraph =
        ParagraphIntrinsics(
            text = annotatedText.text,
            style = textStyle,
            spanStyles = annotatedText.spanStyles,
            density = density,
            resourceLoader = fontResourceLoader,
        )

    val maxLines =
        if (v.style.nodeStyle.hasLineCount()) v.style.nodeStyle.lineCount else Int.MAX_VALUE

    val textMeasureData =
        TextMeasureData(
            annotatedText.text.hashCode(),
            paragraph,
            density,
            maxLines,
            v.style.isAutoWidthText(),
        )

    textMeasureCache.put(
        layoutId,
        TextMeasureCache.Entry(textMeasureData, customizedText, customTextStyle, annotatedText.text),
    )

    textHash.add(annotatedText.text)

    return textMeasureData
}
