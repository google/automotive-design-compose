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

package com.android.designcompose.common

import com.android.designcompose.definition.DesignComposeDefinition
import com.android.designcompose.definition.DesignComposeDefinitionHeader
import com.android.designcompose.definition.view.View
import com.android.designcompose.live_update.ConvertResponse
import com.android.designcompose.live_update.figma.ServerFigmaDoc
import com.google.protobuf.ByteString
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GenericDocContentExtraTest {
    private lateinit var feedback: FeedbackImpl
    private lateinit var tempFile: File

    @Before
    fun setup() {
        feedback =
            object : FeedbackImpl() {
                override fun logMessage(str: String, level: FeedbackLevel) {
                    // No-op for testing
                }
            }
        tempFile = File.createTempFile("test", ".dcf")
    }

    @After
    fun cleanup() {
        tempFile.delete()
    }

    @Test
    fun testSave() {
        val docId = DesignDocId("doc1")
        val header =
            DesignComposeDefinitionHeader.newBuilder().setDcVersion(FSAAS_DOC_VERSION).build()
        val definition = DesignComposeDefinition.getDefaultInstance()
        val content =
            GenericDocContent(
                docId,
                header,
                definition,
                HashMap(),
                VariantPropertyMap(),
                HashMap(),
                ByteString.EMPTY,
            )

        content.save(tempFile, feedback)
        assertTrue(tempFile.exists())
        assertTrue(tempFile.length() > 0)
    }

    @Test
    fun testDecodeServerBaseDoc() {
        val docId = DesignDocId("doc1")
        val header =
            DesignComposeDefinitionHeader.newBuilder()
                .setDcVersion(FSAAS_DOC_VERSION)
                .setName("Test Doc")
                .setLastModified("2023-01-01")
                .build()
        val serverDoc =
            ServerFigmaDoc.newBuilder()
                .setFigmaDoc(DesignComposeDefinition.getDefaultInstance())
                .build()
        val docResponse =
            ConvertResponse.Document.newBuilder()
                .setHeader(header)
                .setServerDoc(serverDoc)
                .setImageSessionJson(ByteString.EMPTY)
                .build()

        val content = decodeServerBaseDoc(docResponse, docId, feedback)
        assertTrue(content != null)
        assertEquals(docId, content!!.docId)
        assertEquals(header, content.header)
    }

    @Test
    fun testCreateVariantViewMap() {
        val view = View.getDefaultInstance()
        val nodes =
            mapOf(
                NodeQuery.NodeVariant("property=value", "component") to view,
                NodeQuery.NodeVariant("property=value2", "component") to view,
                NodeQuery.NodeName("not-a-variant") to view,
            )
        val variantViewMap = createVariantViewMap(nodes)
        assertEquals(1, variantViewMap.size)
        assertTrue(variantViewMap.containsKey("component"))
        assertEquals(2, variantViewMap["component"]!!.size)
        assertTrue(variantViewMap["component"]!!.containsKey("property=value"))
        assertTrue(variantViewMap["component"]!!.containsKey("property=value2"))
    }

    @Test
    fun testCreateVariantPropertyMap() {
        val view = View.getDefaultInstance()
        val nodes =
            mapOf(
                NodeQuery.NodeVariant("property=value", "component") to view,
                NodeQuery.NodeVariant("property=value2", "component") to view,
                NodeQuery.NodeVariant("property2=value3", "component") to view,
                NodeQuery.NodeName("not-a-variant") to view,
            )
        val variantPropertyMap = createVariantPropertyMap(nodes)
        val componentProperties = variantPropertyMap.propertyMap["component"]
        assertTrue(componentProperties != null)
        assertEquals(2, componentProperties.size)
        assertTrue(componentProperties.containsKey("property"))
        assertTrue(componentProperties.containsKey("property2"))
        assertEquals(2, componentProperties["property"]!!.size)
        assertEquals(1, componentProperties["property2"]!!.size)
        assertTrue(componentProperties["property"]!!.contains("value"))
        assertTrue(componentProperties["property"]!!.contains("value2"))
        assertTrue(componentProperties["property2"]!!.contains("value3"))
    }

    @Test
    fun testCreateNodeIdMap() {
        val childView = View.newBuilder().setId("child").build()
        val containerView =
            View.newBuilder()
                .setId("container")
                .setData(
                    com.android.designcompose.definition.view.ViewData.newBuilder()
                        .setContainer(
                            com.android.designcompose.definition.view.ViewData.Container
                                .newBuilder()
                                .addChildren(childView)
                        )
                )
                .build()
        val nodes: Map<NodeQuery, View> = mapOf(NodeQuery.NodeName("container") to containerView)
        val nodeIdMap = createNodeIdMap(nodes)
        assertEquals(2, nodeIdMap.size)
        assertTrue(nodeIdMap.containsKey("container"))
        assertTrue(nodeIdMap.containsKey("child"))
    }
}
