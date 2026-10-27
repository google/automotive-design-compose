/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.designcompose

import androidx.compose.ui.text.font.FontFamily
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.common.GenericDocContent
import com.android.designcompose.common.VariantPropertyMap
import com.android.designcompose.definition.DesignComposeDefinition
import com.android.designcompose.definition.DesignComposeDefinitionHeader
import com.android.designcompose.definition.element.Color
import com.android.designcompose.definition.element.ColorOrVar
import com.android.designcompose.definition.element.Mode
import com.android.designcompose.definition.element.Variable
import com.android.designcompose.definition.element.VariableMap
import com.android.designcompose.definition.element.VariableValue
import com.android.designcompose.definition.view.View
import com.android.designcompose.utils.toColor
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DesignSettingsTest {

    @After
    fun tearDown() {
        DesignSettings.clearRawResources()
        VariableManager.testOnlyReset()
    }

    @Test
    fun testSetRawResourceId() {
        val docId = DesignDocId("test")
        DesignSettings.setRawResourceId(docId, 123)
        assertThat(DesignSettings.rawResourceId).containsKey(docId)
        assertThat(DesignSettings.rawResourceId[docId]).isEqualTo(123)
    }

    @Test
    fun testClearRawResources() {
        val docId = DesignDocId("test")
        DesignSettings.setRawResourceId(docId, 123)
        DesignSettings.clearRawResources()
        assertThat(DesignSettings.rawResourceId).isEmpty()
    }

    @Test
    fun testAddFontFamily() {
        val fontFamily = FontFamily.Default
        DesignSettings.addFontFamily("testFont", fontFamily)
        assertThat(DesignSettings.fontFamily("testFont")).isEqualTo(fontFamily)
    }

    @Test
    fun testSetRawResourceIdIdempotent() {
        val docId = DesignDocId("testDoc")
        val initialGen = DocServer.rawResourceGeneration.value

        // First registration does not change DocServer.rawResourceGeneration
        DesignSettings.setRawResourceId(docId, 100)
        assertThat(DocServer.rawResourceGeneration.value).isEqualTo(initialGen)

        // Same id again does not change DocServer.rawResourceGeneration
        DesignSettings.setRawResourceId(docId, 100)
        assertThat(DocServer.rawResourceGeneration.value).isEqualTo(initialGen)

        // A different id bumps it by exactly 1
        DesignSettings.setRawResourceId(docId, 200)
        assertThat(DocServer.rawResourceGeneration.value).isEqualTo(initialGen + 1)
    }

    @Test
    fun testClearRawResourcesEvictsCachedDocs() {
        val docId = DesignDocId("testEvictDoc")
        DesignSettings.setRawResourceId(docId, 101)

        val docContent = createDummyDocContent(docId)
        synchronized(DocServer.documents) { DocServer.documents[docId] = docContent }
        assertThat(DocServer.documents).containsKey(docId)

        val genBefore = DocServer.rawResourceGeneration.value
        DesignSettings.clearRawResources()

        assertThat(DocServer.documents).doesNotContainKey(docId)
        assertThat(DocServer.rawResourceGeneration.value).isEqualTo(genBefore + 1)
        assertThat(DesignSettings.rawResourceId).isEmpty()
    }

    @Test
    fun testVariableQueryApi() {
        val docId = DesignDocId("testDocVar")
        val collection =
            com.android.designcompose.definition.element.Collection.newBuilder()
                .setId("col1")
                .setName("ThemeCollection")
                .setDefaultModeId("mode1")
                .putModeNameHash("Light", "mode1")
                .putModeIdHash("mode1", Mode.newBuilder().setId("mode1").setName("Light").build())
                .putModeNameHash("Dark", "mode2")
                .putModeIdHash("mode2", Mode.newBuilder().setId("mode2").setName("Dark").build())
                .build()

        val varMap =
            VariableMap.newBuilder()
                .putCollectionsById("col1", collection)
                .putCollectionIdsByName("ThemeCollection", "col1")
                .putVariableNameIdMapsByCid(
                    "col1",
                    VariableMap.NameIdMap.newBuilder()
                        .putM("primaryColor", "var1")
                        .putM("secondaryColor", "var2")
                        .build(),
                )
                .build()

        VariableManager.init(docId, varMap)

        assertThat(DesignSettings.getVariableCollections(docId)).containsExactly("ThemeCollection")
        assertThat(DesignSettings.getVariableModes(docId, "ThemeCollection"))
            .containsExactly("Light", "Dark")
        assertThat(DesignSettings.getVariableNames(docId, "ThemeCollection"))
            .containsExactly("primaryColor", "secondaryColor")

        val unloadedDocId = DesignDocId("unloadedDoc")
        assertThat(DesignSettings.getVariableCollections(unloadedDocId)).isEmpty()
        assertThat(DesignSettings.getVariableModes(unloadedDocId, "ThemeCollection")).isEmpty()
        assertThat(DesignSettings.getVariableNames(unloadedDocId, "ThemeCollection")).isEmpty()
    }

    @Test
    fun testDuplicateCollectionAndVariableNamesAcrossDocs() {
        val doc1 = DesignDocId("doc1")
        val doc2 = DesignDocId("doc2")

        val color1 = createColor(255, 0, 0, 255)
        val color2 = createColor(0, 0, 255, 255)

        val col1 = createCollection(collectionId = "c1", collectionName = "Theme", modeId = "m1")
        val var1 =
            createColorVariable(
                varId = "v1",
                varName = "primary",
                collectionId = "c1",
                modeId = "m1",
                color = color1,
            )
        val varMap1 =
            VariableMap.newBuilder()
                .putCollectionsById("c1", col1)
                .putCollectionIdsByName("Theme", "c1")
                .putVariablesById("v1", var1)
                .putVariableNameIdMapsByCid(
                    "c1",
                    VariableMap.NameIdMap.newBuilder().putM("primary", "v1").build(),
                )
                .build()

        val col2 = createCollection(collectionId = "c2", collectionName = "Theme", modeId = "m2")
        val var2 =
            createColorVariable(
                varId = "v2",
                varName = "primary",
                collectionId = "c2",
                modeId = "m2",
                color = color2,
            )
        val varMap2 =
            VariableMap.newBuilder()
                .putCollectionsById("c2", col2)
                .putCollectionIdsByName("Theme", "c2")
                .putVariablesById("v2", var2)
                .putVariableNameIdMapsByCid(
                    "c2",
                    VariableMap.NameIdMap.newBuilder().putM("primary", "v2").build(),
                )
                .build()

        VariableManager.init(doc1, varMap1)
        VariableManager.init(doc2, varMap2)

        val colorVar1 = createColorOrVar("v1")
        val colorVar2 = createColorOrVar("v2")

        val state1 = VariableState(varCollection = "Theme", docId = "doc1")
        val state2 = VariableState(varCollection = "Theme", docId = "doc2")

        val resolvedColor1 = colorVar1.getValue(state1)
        val resolvedColor2 = colorVar2.getValue(state2)

        assertThat(resolvedColor1).isEqualTo(color1.toColor())
        assertThat(resolvedColor2).isEqualTo(color2.toColor())
        assertThat(resolvedColor1).isNotEqualTo(resolvedColor2)
    }

    @Test
    fun testDuplicateVariableIdsAcrossDocs() {
        val doc1 = DesignDocId("doc1")
        val doc2 = DesignDocId("doc2")

        val color1 = createColor(255, 128, 0, 255)
        val color2 = createColor(0, 255, 128, 255)

        val col1 = createCollection(collectionId = "c1", collectionName = "Theme1", modeId = "m1")
        val var1 =
            createColorVariable(
                varId = "v1",
                varName = "accent",
                collectionId = "c1",
                modeId = "m1",
                color = color1,
            )
        val varMap1 =
            VariableMap.newBuilder()
                .putCollectionsById("c1", col1)
                .putCollectionIdsByName("Theme1", "c1")
                .putVariablesById("v1", var1)
                .putVariableNameIdMapsByCid(
                    "c1",
                    VariableMap.NameIdMap.newBuilder().putM("accent", "v1").build(),
                )
                .build()

        val col2 = createCollection(collectionId = "c2", collectionName = "Theme2", modeId = "m2")
        val var2 =
            createColorVariable(
                varId = "v1",
                varName = "accent",
                collectionId = "c2",
                modeId = "m2",
                color = color2,
            )
        val varMap2 =
            VariableMap.newBuilder()
                .putCollectionsById("c2", col2)
                .putCollectionIdsByName("Theme2", "c2")
                .putVariablesById("v1", var2)
                .putVariableNameIdMapsByCid(
                    "c2",
                    VariableMap.NameIdMap.newBuilder().putM("accent", "v1").build(),
                )
                .build()

        VariableManager.init(doc1, varMap1)
        VariableManager.init(doc2, varMap2)

        val colorVar = createColorOrVar("v1")
        val stateDoc1 = VariableState(docId = "doc1")
        val stateDoc2 = VariableState(docId = "doc2")

        val resolvedColor1 = colorVar.getValue(stateDoc1)
        val resolvedColor2 = colorVar.getValue(stateDoc2)

        assertThat(resolvedColor1).isEqualTo(color1.toColor())
        assertThat(resolvedColor2).isEqualTo(color2.toColor())
        assertThat(resolvedColor1).isNotEqualTo(resolvedColor2)
    }

    private fun createDummyDocContent(docId: DesignDocId): DocContent {
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

    private fun createColor(r: Int, g: Int, b: Int, a: Int = 255): Color =
        Color.newBuilder().setR(r).setG(g).setB(b).setA(a).build()

    private fun createColorVariable(
        varId: String,
        varName: String,
        collectionId: String,
        modeId: String,
        color: Color,
    ): Variable {
        val varValue = VariableValue.newBuilder().setColor(color).build()
        val valueMap =
            Variable.VariableValueMap.newBuilder().putValuesByMode(modeId, varValue).build()
        return Variable.newBuilder()
            .setId(varId)
            .setName(varName)
            .setVariableCollectionId(collectionId)
            .setVarType(Variable.VariableType.VARIABLE_TYPE_COLOR)
            .setValuesByMode(valueMap)
            .build()
    }

    private fun createCollection(
        collectionId: String,
        collectionName: String,
        modeId: String = "mode1",
        modeName: String = "Default",
    ): com.android.designcompose.definition.element.Collection {
        val mode = Mode.newBuilder().setId(modeId).setName(modeName).build()
        return com.android.designcompose.definition.element.Collection.newBuilder()
            .setId(collectionId)
            .setName(collectionName)
            .setDefaultModeId(modeId)
            .putModeNameHash(modeName, modeId)
            .putModeIdHash(modeId, mode)
            .build()
    }

    private fun createColorOrVar(varId: String): ColorOrVar {
        val fallback = createColor(0, 0, 0, 0)
        return ColorOrVar.newBuilder()
            .setVar(ColorOrVar.ColorVar.newBuilder().setId(varId).setFallback(fallback).build())
            .build()
    }
}
