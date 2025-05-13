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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import com.android.designcompose.definition.view.View
import com.android.designcompose.definition.view.ViewData
import com.android.designcompose.definition.view.fontSizeOrNull
import com.android.designcompose.definition.view.fontWeightOrNull
import com.android.designcompose.definition.view.styleOrNull
import com.android.designcompose.definition.view.styledTextOrNull
import com.android.designcompose.definition.view.textColorOrNull
import com.android.designcompose.definition.view.textOrNull
import com.android.designcompose.definition.view.textShadowOrNull
import com.android.designcompose.getBrush
import com.android.designcompose.getText
import com.android.designcompose.getTextState
import com.android.designcompose.getTextStyle
import com.android.designcompose.getValue
import com.android.designcompose.utils.asBrush
import com.android.designcompose.utils.blurFudgeFactor
import com.android.designcompose.utils.getTextContent
import com.android.designcompose.utils.isAutoWidthText
import com.android.designcompose.utils.protoVersionsFontColor
import kotlin.math.roundToInt

val newlineRegex = Regex("\\R+")
val lineSeparator: String? = System.getProperty("line.separator")

private fun normalizeNewlines(text: String): String {
    if (lineSeparator != null) return text.replace(newlineRegex, lineSeparator)
    return text
}

/// A simple generational cache for TextMeasureData, because it is expensive to compute.
internal class TextMeasureCache {
    internal class Entry(
        val data: TextMeasureData,
        val text: String?,
        val customStyle: TextStyle?,
        val annotatedText: String,
        val textStyle: TextStyle,
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
    overrideViewData: ViewData?,
    layoutId: Int,
    density: Density,
    document: DocContent,
    customizations: CustomizationContext,
    fontResolver: FontFamily.Resolver,
    variableState: VariableState,
    appContext: Context,
    textMeasureCache: TextMeasureCache,
    textHash: HashSet<String>,
): Pair<TextMeasureData?, TextStyle>? {
    val customizedText =
        customizations.getText(v.name) ?: customizations.getTextState(v.name)?.value
    val customTextStyle = customizations.getTextStyle(v.name)
    val fontFamily =
        DesignSettings.fontFamily(v.style.nodeStyle.takeIf { it.hasFontFamily() }?.fontFamily)

    val cachedText = textMeasureCache.get(layoutId)
    if (
        cachedText != null &&
            cachedText.text == customizedText &&
            cachedText.customStyle == customTextStyle
    ) {
        textHash.add(cachedText.annotatedText)
        textMeasureCache.put(layoutId, cachedText)
        return Pair(cachedText.data, cachedText.textStyle)
    }

    val hyperlinkOffsetMap = LinkedHashMap<Int, String?>()

    val annotatedText =
        if (customizedText != null) {
            val builder = AnnotatedString.Builder()
            builder.append(normalizeNewlines(customizedText))
            builder.toAnnotatedString()
        } else {
            val viewText = overrideViewData?.textOrNull ?: v.data.textOrNull
            val viewStyledText = overrideViewData?.styledTextOrNull ?: v.data.styledTextOrNull
            if (viewText != null) {
                val builder = AnnotatedString.Builder()
                val text = normalizeNewlines(getTextContent(appContext, viewText))
                builder.append(text)
                if (v.style.nodeStyle.hasHyperlink()) {
                    val link = v.style.nodeStyle.hyperlink.value
                    builder.addLink(LinkAnnotation.Url(link), 0, text.length)
                    hyperlinkOffsetMap[0] = link
                }
                builder.toAnnotatedString()
            } else if (viewStyledText != null) {
                val builder = AnnotatedString.Builder()
                var startIndex = 0
                for (run in getTextContent(appContext, viewStyledText)) {
                    val style = run.styleOrNull!!
                    val textBrushAndOpacity =
                        style.textColorOrNull!!.asBrush(
                            appContext,
                            document,
                            density.density,
                            variableState,
                        )
                    val fontWeight = style.fontWeightOrNull!!.weight.getValue(variableState)
                    builder.pushStyle(
                        (SpanStyle(
                            brush = textBrushAndOpacity?.first,
                            alpha = textBrushAndOpacity?.second ?: 1.0f,
                            fontSize = style.fontSizeOrNull!!.getValue(variableState).sp,
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
                    if (run.style.hasHyperlink()) {
                        val link = run.style.hyperlink.value
                        builder.addLink(
                            LinkAnnotation.Url(link),
                            startIndex,
                            startIndex + run.text.length,
                        )
                        hyperlinkOffsetMap[startIndex] = link
                        hyperlinkOffsetMap[startIndex + run.text.length] = null
                    }
                    startIndex += run.text.length
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
            ?: FontWeight(v.style.nodeStyle.fontWeight.weight.getValue(variableState).roundToInt())
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
    val customBrush = customizations.getBrush(v.name)
    // Call this helper to get the font color since the proto field has changed over time
    val fontColor = protoVersionsFontColor(v.style)
    val textBrushAndOpacity =
        fontColor?.asBrush(appContext, document, density.density, variableState)
    // The brush and opacity is stored here for use with ComponentReplacementContext. They are
    // retrieved again later at rendering time since we need to pass the brush, opacity and draw
    // style explicitly.
    val textStyle =
        (TextStyle(
            brush = customBrush ?: textBrushAndOpacity?.first ?: SolidColor(Color.Transparent),
            fontSize =
                customTextStyle?.fontSize ?: v.style.nodeStyle.fontSize.getValue(variableState).sp,
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
            annotatedText.text,
            textStyle,
            annotatedText.spanStyles,
            listOf(),
            density,
            fontResolver,
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
            hyperlinkOffsetMap,
        )

    textMeasureCache.put(
        layoutId,
        TextMeasureCache.Entry(
            textMeasureData,
            customizedText,
            customTextStyle,
            annotatedText.text,
            textStyle,
        ),
    )

    textHash.add(annotatedText.text)

    return Pair(textMeasureData, textStyle)
}
