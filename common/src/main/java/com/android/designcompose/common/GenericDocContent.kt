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

import com.android.designcompose.serdegen.DesignComposeDefinition
import com.android.designcompose.serdegen.DesignComposeDefinitionHeader
import com.android.designcompose.serdegen.FigmaDocInfo
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.serdegen.ServerFigmaDoc
import com.android.designcompose.serdegen.View
import com.novi.bincode.BincodeDeserializer
import com.novi.bincode.BincodeSerializer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class GenericDocContent(
    var docId: DesignDocId,
    private val header: DesignComposeDefinitionHeader,
    val document: DesignComposeDefinition,
    val variantViewMap: HashMap<String, HashMap<String, View>>,
    val variantPropertyMap: VariantPropertyMap,
    private val imageSessionData: ByteArray,
    val imageSession: String?,
    val branches: List<FigmaDocInfo>? = null,
    val project_files: List<FigmaDocInfo>? = null,
) {
    fun save(filepath: File, feedback: FeedbackImpl) {
        feedback.documentSaveTo(filepath.absolutePath, docId)
        try {
            val bytes = toSerializedBytes(feedback)
            val output = FileOutputStream(filepath)
            output.write(bytes)
            output.close()
            feedback.documentSaveSuccess(docId)
        } catch (error: Throwable) {
            feedback.documentSaveError(error.toString(), docId)
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    fun toSerializedBytes(feedback: FeedbackImpl): ByteArray? {
        try {
            val serializer = BincodeSerializer()
            header.serialize(serializer)
            document.serialize(serializer)
            return serializer._bytes.toUByteArray().asByteArray() + imageSessionData
        } catch (error: Throwable) {
            feedback.documentSaveError(error.toString(), docId)
        }
        return null
    }
}

/// Read a serialized server document from the given stream. Deserialize it and save it to disk.
fun decodeServerBaseDoc(
    docBytes: ByteArray,
    docId: DesignDocId,
    feedback: FeedbackImpl
): GenericDocContent? {
    val deserializer = BincodeDeserializer(docBytes)
    val header = decodeHeader(deserializer, docId, feedback) ?: return null

    // Server sends content in the format of ServerFigmaDoc, which has additional data
    val serverDoc = ServerFigmaDoc.deserialize(deserializer)
    serverDoc.errors?.forEach { feedback.documentUpdateWarnings(docId, it) }
    val content = serverDoc.figma_doc
    val imageSessionData = decodeImageSession(docBytes, deserializer)
    feedback.documentDecodeSuccess(header.version, content.name, content.last_modified, docId)

    val variantViewMap = createVariantViewMap(content.views)
    val variantPropertyMap = createVariantPropertyMap(content.views)
    return GenericDocContent(
        docId,
        header,
        content,
        variantViewMap,
        variantPropertyMap,
        imageSessionData.imageSessionData,
        imageSessionData.imageSession,
        serverDoc.branches,
        serverDoc.project_files
    )
}

/// Read a serialized disk document from the given stream. Deserialize it and deserialize its images
fun decodeDiskBaseDoc(
    doc: InputStream,
    docId: DesignDocId,
    feedback: FeedbackImpl
): GenericDocContent? {
    val docBytes = readDocBytes(doc, docId, feedback)
    val deserializer = BincodeDeserializer(docBytes)

    val header = decodeHeader(deserializer, docId, feedback) ?: return null

    // Disk loads are in the format of DesignComposeDefinition
    val content = DesignComposeDefinition.deserialize(deserializer)
    val imageSessionData = decodeImageSession(docBytes, deserializer)
    val variantMap = createVariantViewMap(content.views)
    val variantPropertyMap = createVariantPropertyMap(content.views)

    feedback.documentDecodeSuccess(header.version, content.name, content.last_modified, docId)

    return GenericDocContent(
        docId,
        header,
        content,
        variantMap,
        variantPropertyMap,
        imageSessionData.imageSessionData,
        imageSessionData.imageSession
    )
}

// Given all the nodes, create a mapping of all components with variants. The HashMap maps the
// component set name to a second map of the component set's child node name, with the properties
// rearranged to be sorted, to their corresponding Views
private fun createVariantViewMap(
    nodes: Map<NodeQuery, View>?
): HashMap<String, HashMap<String, View>> {
    val variantMap: HashMap<String, HashMap<String, View>> = HashMap()
    nodes?.forEach {
        val nodeQuery = it.key
        val view = it.value
        if (nodeQuery is NodeQuery.NodeVariant) {
            val nodeName = nodeQuery.field0
            val parentNodeName = nodeQuery.field1

            val nodeNameToView = variantMap[parentNodeName] ?: HashMap()
            val sortedNodeName = createSortedVariantName(nodeName)
            nodeNameToView[sortedNodeName] = view
            variantMap[parentNodeName] = nodeNameToView
        }
    }
    return variantMap
}

private fun createVariantPropertyMap(nodes: Map<NodeQuery, View>?): VariantPropertyMap {
    // To support wildcard * variant nodes, we make a variant property map that lets us figure out
    // all the possible variant names for a given property name.
    // Then when matching node names, such as "#cluster/prnd=R, #cluster/charging=off", we first
    // parse out the property names. For each property name we try to match its variant name with
    // a node name that exists, but if it doesn't exist we use '*' if that exists. Finally we come
    // up with a node name that matches as many variant names as possible, using '*' for the rest.
    val propertyMap = VariantPropertyMap()
    nodes?.keys?.forEach {
        if (it is NodeQuery.NodeVariant) {
            val nodeName = it.field0!!
            val parentNodeName = it.field1!!
            val propertyValueList = nodeNameToPropertyValueList(nodeName)
            for (p in propertyValueList) propertyMap.addProperty(parentNodeName, p.first, p.second)
        }
    }
    return propertyMap
}

fun readDocBytes(doc: InputStream, docId: DesignDocId, feedback: FeedbackImpl): ByteArray {
    // Read the doc from assets or network...
    feedback.documentDecodeStart(docId)
    val buffer = ByteArrayOutputStream()
    var nRead: Int
    val tmp = ByteArray(128)

    do {
        nRead = doc.read(tmp, 0, tmp.size)
        if (nRead != -1) buffer.write(tmp, 0, nRead)
    } while (nRead != -1)
    buffer.flush()
    val docBytes = buffer.toByteArray()
    feedback.documentDecodeReadBytes(docBytes.size, docId)
    return docBytes
}

fun readErrorBytes(errorStream: InputStream?): String {
    if (errorStream == null) return ""

    val buffer = ByteArrayOutputStream()
    var nRead: Int
    val tmp = ByteArray(128)

    do {
        nRead = errorStream.read(tmp, 0, tmp.size)
        if (nRead != -1) buffer.write(tmp, 0, nRead)
    } while (nRead != -1)
    buffer.flush()
    val docBytes = buffer.toByteArray()
    return String(docBytes)
}

private fun decodeHeader(
    deserializer: BincodeDeserializer,
    docId: DesignDocId,
    feedback: FeedbackImpl
): DesignComposeDefinitionHeader? {
    // Now attempt to deserialize the doc)
    val header = DesignComposeDefinitionHeader.deserialize(deserializer)
    if (header.version != FSAAS_DOC_VERSION) {
        feedback.documentDecodeVersionMismatch(FSAAS_DOC_VERSION, header.version, docId)
        return null
    }
    return header
}

private data class ImageSession(val imageSessionData: ByteArray, var imageSession: String?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageSession

        if (!imageSessionData.contentEquals(other.imageSessionData)) return false
        if (imageSession != other.imageSession) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageSessionData.contentHashCode()
        result = 31 * result + (imageSession?.hashCode() ?: 0)
        return result
    }
}

private fun decodeImageSession(
    docBytes: ByteArray,
    deserializer: BincodeDeserializer,
): ImageSession {
    // The image session data is a JSON blob attached after the bincode document content.
    val imageSessionData = docBytes.copyOfRange(deserializer._buffer_offset, docBytes.size)
    val imageSession =
        if (imageSessionData.isNotEmpty()) {
            String(imageSessionData, Charsets.UTF_8)
        } else {
            null
        }
    return ImageSession(imageSessionData, imageSession)
}
