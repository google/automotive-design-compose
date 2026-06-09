/*
 * Copyright 2026 Google LLC
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DesignSettings
import com.android.designcompose.DocContent
import com.android.designcompose.VariableState
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.common.GenericDocContent
import com.android.designcompose.common.VariantPropertyMap
import com.android.designcompose.definition.DesignComposeDefinition
import com.android.designcompose.definition.DesignComposeDefinitionHeader
import com.android.designcompose.definition.element.fontWeight
import com.android.designcompose.definition.element.lineHeight
import com.android.designcompose.definition.element.numOrVar
import com.android.designcompose.definition.view.View
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.android.designcompose.definition.view.ViewDataKt.text
import com.android.designcompose.definition.view.nodeStyle
import com.android.designcompose.definition.view.view
import com.android.designcompose.definition.view.viewData
import com.android.designcompose.definition.view.viewStyle
import com.android.designcompose.setTextStyle
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SquooshTextTest {

    private lateinit var context: Context
    private lateinit var docContent: DocContent
    private lateinit var fontResolver: FontFamily.Resolver
    private lateinit var textMeasureCache: TextMeasureCache
    private lateinit var textHash: HashSet<String>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        docContent = createDummyDocContent()
        fontResolver = createFontFamilyResolver(context)
        textMeasureCache = TextMeasureCache()
        textHash = HashSet()
        DesignSettings.clearRawResources()
    }

    private fun createDummyDocContent(): DocContent {
        val docId = DesignDocId("dummy")
        val header = DesignComposeDefinitionHeader.getDefaultInstance()
        val document = DesignComposeDefinition.getDefaultInstance()
        val variantViewMap = HashMap<String, HashMap<String, View>>()
        val variantPropertyMap = VariantPropertyMap()
        val nodeIdMap = HashMap<String, View>()
        val imageSession = ByteString.EMPTY

        val genericDocContent =
            GenericDocContent(
                docId,
                header,
                document,
                variantViewMap,
                variantPropertyMap,
                nodeIdMap,
                imageSession,
            )
        return DocContent(genericDocContent, null)
    }

    private fun createTextView(fontFamilyName: String): View {
        return view {
            name = "text-node"
            style = viewStyle {
                nodeStyle = nodeStyle {
                    fontFamily = fontFamilyName
                    fontSize = numOrVar { num = 12f }
                    fontWeight = fontWeight { weight = numOrVar { num = 400f } }
                }
            }
            data = viewData { text = text { content = "Hello" } }
        }
    }

    @Test
    fun testDefaultFontFamily() {
        val view = createTextView("FigmaFont")
        val customizations = CustomizationContext()
        val variableState = VariableState()

        // Register FigmaFont in DesignSettings
        DesignSettings.addFontFamily("FigmaFont", FontFamily.SansSerif)

        val result =
            squooshComputeTextInfo(
                v = view,
                overrideViewData = null,
                layoutId = 1,
                density = Density(1f),
                document = docContent,
                customizations = customizations,
                fontResolver = fontResolver,
                variableState = variableState,
                appContext = context,
                textMeasureCache = textMeasureCache,
                textHash = textHash,
            )

        assertThat(result).isNotNull()
        val (_, textStyle) = result!!
        assertThat(textStyle.fontFamily).isEqualTo(FontFamily.SansSerif)
    }

    @Test
    fun testCustomFontFamily() {
        val view = createTextView("FigmaFont")
        val customizations = CustomizationContext()
        val variableState = VariableState()

        // Register FigmaFont in DesignSettings
        DesignSettings.addFontFamily("FigmaFont", FontFamily.SansSerif)

        // Customize TextStyle with a different fontFamily
        val customStyle = TextStyle(fontFamily = FontFamily.Serif)
        customizations.setTextStyle("text-node", customStyle)

        val result =
            squooshComputeTextInfo(
                v = view,
                overrideViewData = null,
                layoutId = 1,
                density = Density(1f),
                document = docContent,
                customizations = customizations,
                fontResolver = fontResolver,
                variableState = variableState,
                appContext = context,
                textMeasureCache = textMeasureCache,
                textHash = textHash,
            )

        assertThat(result).isNotNull()
        val (_, textStyle) = result!!
        assertThat(textStyle.fontFamily).isEqualTo(FontFamily.Serif)
    }

    private fun createTextViewWithLineHeight(percentValue: Float?, pixelsValue: Float?): View {
        return view {
            name = "text-node"
            style = viewStyle {
                nodeStyle = nodeStyle {
                    fontFamily = "FigmaFont"
                    fontSize = numOrVar { num = 12f }
                    fontWeight = fontWeight {
                        weight = numOrVar { num = 400f }
                    }
                    if (percentValue != null) {
                        lineHeight = lineHeight { percent = percentValue }
                    } else if (pixelsValue != null) {
                        lineHeight = lineHeight { pixels = pixelsValue }
                    }
                }
            }
            data = viewData {
                text = text {
                    content = "Hello"
                }
            }
        }
    }

    @Test
    fun testPercentLineHeight() {
        val view = createTextViewWithLineHeight(percentValue = 1.2f, pixelsValue = null)
        val customizations = CustomizationContext()
        val variableState = VariableState()

        val result = squooshComputeTextInfo(
            v = view,
            overrideViewData = null,
            layoutId = 1,
            density = Density(1f),
            document = docContent,
            customizations = customizations,
            fontResolver = fontResolver,
            variableState = variableState,
            appContext = context,
            textMeasureCache = textMeasureCache,
            textHash = textHash
        )

        assertThat(result).isNotNull()
        val (_, textStyle) = result!!
        assertThat(textStyle.lineHeight).isEqualTo(1.2.em)
    }

    @Test
    fun testPixelLineHeight() {
        val view = createTextViewWithLineHeight(percentValue = null, pixelsValue = 16f)
        val customizations = CustomizationContext()
        val variableState = VariableState()

        val result = squooshComputeTextInfo(
            v = view,
            overrideViewData = null,
            layoutId = 1,
            density = Density(1f),
            document = docContent,
            customizations = customizations,
            fontResolver = fontResolver,
            variableState = variableState,
            appContext = context,
            textMeasureCache = textMeasureCache,
            textHash = textHash
        )

        assertThat(result).isNotNull()
        val (_, textStyle) = result!!
        assertThat(textStyle.lineHeight).isEqualTo(16.sp)
    }
}
