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

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.android.designcompose.serdegen.FontStyle
import com.android.designcompose.serdegen.LineHeight
import com.android.designcompose.serdegen.StyledTextRun
import com.android.designcompose.serdegen.TextAlign
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
    text: String? = null,
    runs: List<StyledTextRun>? = null,
    style: ViewStyle,
    document: DocContent,
    nodeName: String,
    viewLayoutInfo: SimplifiedLayoutInfo,
    customizations: CustomizationContext
) {
    if (!customizations.getVisible(nodeName)) return

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
            textAlign = customTextStyle?.textAlign
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

    val layoutModifier =
        when (viewLayoutInfo) {
            is LayoutInfoAbsolute -> viewLayoutInfo.marginModifier
            is LayoutInfoRow -> viewLayoutInfo.marginModifier
            is LayoutInfoColumn -> viewLayoutInfo.marginModifier
            is LayoutInfoGrid -> viewLayoutInfo.marginModifier
            else -> Modifier
        }

    DesignTextLayout(
        layoutModifier.textTransform(style).layoutStyle(annotatedText.text, style).clipToBounds(),
        style,
        TextLayoutData(annotatedText, textStyle, LocalFontLoader.current),
        annotatedText.text
    ) {
        val replacementComponent = customizations.getComponent(nodeName)
        if (replacementComponent != null) {
            replacementComponent(
                object : ComponentReplacementContext {
                    override val layoutModifier = modifier
                    override val appearanceModifier = Modifier
                    @Composable override fun Content() {}
                    override val textStyle = textStyle
                }
            )
        } else {
            BasicText(
                annotatedText,
                modifier = modifier,
                style = textStyle,
                overflow = overflow,
            )
        }
    }
}
