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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
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
import com.android.designcompose.asBrush
import com.android.designcompose.blurFudgeFactor
import com.android.designcompose.convertColor
import com.android.designcompose.getText
import com.android.designcompose.getTextStyle
import com.android.designcompose.isAutoWidthText
import com.android.designcompose.serdegen.LineHeight
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import java.util.Optional
import kotlin.math.roundToInt

val newlineRegex = Regex("\\R+")
val lineSeparator: String? = System.getProperty("line.separator")

private fun normalizeNewlines(text: String): String {
    if (lineSeparator != null) return text.replace(newlineRegex, lineSeparator)
    return text
}

/// Take DesignCompose text information, and generate the information required to measure
/// and render text using Compose APIs.
///
/// Internally, this function creates a Compose ParagraphIntrinsics, which does some work
/// on the text in order to quickly answer layout queries. Creating a ParagraphIntrinsics
/// is heavyweight, so we should avoid doing it frequently.
internal fun squooshComputeTextInfo(
    v: View,
    density: Density,
    document: DocContent,
    customizations: CustomizationContext,
    fontResourceLoader: Font.ResourceLoader
): TextMeasureData? {
    val customizedText = customizations.getText(v.name)
    val customTextStyle = customizations.getTextStyle(v.name)
    val fontFamily = DesignSettings.fontFamily(v.style.font_family)

    val annotatedText =
        if (customizedText != null) {
            val builder = AnnotatedString.Builder()
            builder.append(normalizeNewlines(customizedText))
            builder.toAnnotatedString()
        } else
            when (v.data) {
                is ViewData.Text -> {
                    val builder = AnnotatedString.Builder()
                    builder.append(normalizeNewlines((v.data as ViewData.Text).content))
                    builder.toAnnotatedString()
                }
                is ViewData.StyledText -> {
                    val builder = AnnotatedString.Builder()
                    for (run in (v.data as ViewData.StyledText).content) {
                        val textBrushAndOpacity =
                            run.style.text_color.asBrush(document, density.density)
                        builder.pushStyle(
                            @OptIn(ExperimentalTextApi::class)
                            (SpanStyle(
                                brush = textBrushAndOpacity?.first,
                                alpha = textBrushAndOpacity?.second ?: 1.0f,
                                fontSize = run.style.font_size.sp,
                                fontWeight = FontWeight(run.style.font_weight.value.roundToInt()),
                                fontStyle =
                                    when (run.style.font_style) {
                                        is com.android.designcompose.serdegen.FontStyle.Italic ->
                                            FontStyle.Italic
                                        else -> FontStyle.Normal
                                    },
                                fontFamily =
                                    DesignSettings.fontFamily(run.style.font_family, fontFamily),
                                fontFeatureSettings =
                                    run.style.font_features.joinToString(", ") { feature ->
                                        String(feature.tag.toByteArray())
                                    },
                                // platformStyle = PlatformSpanStyle(includeFontPadding = false),
                            ))
                        )
                        builder.append(run.text)
                        builder.pop()
                    }
                    builder.toAnnotatedString()
                }
                else -> return null
            }

    val lineHeight =
        customTextStyle?.lineHeight
            ?: when (v.style.line_height) {
                is LineHeight.Pixels -> (v.style.line_height as LineHeight.Pixels).value.sp
                else -> TextUnit.Unspecified
            }
    val fontWeight =
        customTextStyle?.fontWeight ?: FontWeight(v.style.font_weight.value.roundToInt())
    val fontStyle =
        customTextStyle?.fontStyle
            ?: when (v.style.font_style) {
                is com.android.designcompose.serdegen.FontStyle.Italic -> FontStyle.Italic
                else -> FontStyle.Normal
            }
    // Compose only supports a single outset shadow on text; we must use a canvas and perform
    // manual text layout (and editing, and accessibility) to do fancier text.
    val shadow =
        v.style.text_shadow.flatMap { textShadow ->
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
    val textBrushAndOpacity = v.style.text_color.asBrush(document, density.density)
    val textStyle =
        @OptIn(ExperimentalTextApi::class)
        (TextStyle(
            brush = textBrushAndOpacity?.first,
            alpha = textBrushAndOpacity?.second ?: 1.0f,
            fontSize = customTextStyle?.fontSize ?: v.style.font_size.sp,
            fontFamily = fontFamily,
            fontFeatureSettings =
                v.style.font_features.joinToString(", ") { feature ->
                    String(feature.tag.toByteArray())
                },
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textAlign =
                customTextStyle?.textAlign
                    ?: when (v.style.text_align) {
                        is com.android.designcompose.serdegen.TextAlign.Center -> TextAlign.Center
                        is com.android.designcompose.serdegen.TextAlign.Right -> TextAlign.Right
                        else -> TextAlign.Left
                    },
            shadow = shadow.orElse(null),
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
        ))

    val paragraph =
        ParagraphIntrinsics(
            text = annotatedText.text,
            style = textStyle,
            spanStyles = annotatedText.spanStyles,
            density = density,
            resourceLoader = fontResourceLoader
        )

    val maxLines =
        if (v.style.line_count.isPresent) v.style.line_count.get().toInt() else Int.MAX_VALUE

    return TextMeasureData(
        annotatedText.text.hashCode(),
        paragraph,
        density,
        maxLines,
        v.style.isAutoWidthText()
    )
}
