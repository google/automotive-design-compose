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

package com.android.designcompose.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.designcompose.DocContent
import com.android.designcompose.VariableState
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.common.GenericDocContent
import com.android.designcompose.common.VariantPropertyMap
import com.android.designcompose.definition.DesignComposeDefinition
import com.android.designcompose.definition.DesignComposeDefinitionHeader
import com.android.designcompose.definition.element.Background
import com.android.designcompose.definition.element.Color
import com.android.designcompose.definition.element.ColorOrVar
import com.android.designcompose.definition.view.View
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GradientOpacityTest {

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

    @Test
    fun testLinearGradientOpacity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val docContent = createDummyDocContent()
        val variableState = VariableState()

        val color = Color.newBuilder().setR(255).setG(0).setB(0).setA(255).build()
        val colorStop =
            Background.ColorStop.newBuilder()
                .setPosition(0f)
                .setColor(ColorOrVar.newBuilder().setColor(color).build())
                .build()

        val linearGradient =
            Background.LinearGradient.newBuilder()
                .setStartX(0f)
                .setStartY(0f)
                .setEndX(100f)
                .setEndY(100f)
                .addColorStops(colorStop)
                .addColorStops(colorStop)
                .build()

        val expectedOpacity = 0.5f
        val background =
            Background.newBuilder()
                .setLinearGradient(linearGradient)
                .setOpacity(expectedOpacity)
                .build()

        val result = background.asBrush(context, docContent, 1f, variableState)
        assertThat(result).isNotNull()
        val (_, opacity) = result!!

        assertThat(opacity).isEqualTo(expectedOpacity)
    }

    @Test
    fun testRadialGradientOpacity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val docContent = createDummyDocContent()
        val variableState = VariableState()

        val color = Color.newBuilder().setR(255).setG(0).setB(0).setA(255).build()
        val colorStop =
            Background.ColorStop.newBuilder()
                .setPosition(0f)
                .setColor(ColorOrVar.newBuilder().setColor(color).build())
                .build()

        val radialGradient =
            Background.RadialGradient.newBuilder()
                .setCenterX(50f)
                .setCenterY(50f)
                .setAngle(0f)
                .setRadiusX(50f)
                .setRadiusY(50f)
                .addColorStops(colorStop)
                .addColorStops(colorStop)
                .build()

        val expectedOpacity = 0.7f
        val background =
            Background.newBuilder()
                .setRadialGradient(radialGradient)
                .setOpacity(expectedOpacity)
                .build()

        val result = background.asBrush(context, docContent, 1f, variableState)
        assertThat(result).isNotNull()
        val (_, opacity) = result!!

        assertThat(opacity).isEqualTo(expectedOpacity)
    }
}
