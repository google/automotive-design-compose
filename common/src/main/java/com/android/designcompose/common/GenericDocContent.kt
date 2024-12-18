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
import com.android.designcompose.live_update.figma.FigmaDocInfo
import com.android.designcompose.live_update.figma.ServerFigmaDoc
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class GenericDocContent(
    var docId: DesignDocId,
    val header: DesignComposeDefinitionHeader,
    val document: DesignComposeDefinition,
    val variantViewMap: HashMap<String, HashMap<String, View>>,
    val variantPropertyMap: VariantPropertyMap,
    val nodeIdMap: HashMap<String, View>,
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
            val outputStream = ByteArrayOutputStream()
            header.writeDelimitedTo(outputStream)
            document.writeDelimitedTo(outputStream)
            outputStream.write(imageSessionData)
            return outputStream.toByteArray()
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
    feedback: FeedbackImpl,
): GenericDocContent? {
    val docStream = docBytes.inputStream()
    val header = decodeHeader(docStream, docId, feedback) ?: return null

    // Server sends content in the format of ServerFigmaDoc, which has additional data
    val serverDoc = ServerFigmaDoc.parseDelimitedFrom(docStream)
    serverDoc.errorsList?.forEach { feedback.documentUpdateWarnings(docId, it) }
    val content = serverDoc.figmaDoc
    val imageSessionData = decodeImageSession(docStream)
    feedback.documentDecodeSuccess(header.dcVersion, header.name, header.lastModified, docId)

    val viewMap = content.views()
    val variantViewMap = createVariantViewMap(viewMap)
    val variantPropertyMap = createVariantPropertyMap(viewMap)
    val nodeIdMap = createNodeIdMap(viewMap)
    return GenericDocContent(
        docId,
        header,
        content,
        variantViewMap,
        variantPropertyMap,
        nodeIdMap,
        imageSessionData.imageSessionData,
        imageSessionData.imageSession,
        serverDoc.branchesList,
        serverDoc.projectFilesList,
    )
}

/// Read a serialized disk document from the given stream. Deserialize it and deserialize its images
fun decodeDiskBaseDoc(
    docStream: InputStream,
    docId: DesignDocId,
    feedback: FeedbackImpl,
): GenericDocContent? {
    feedback.documentDecodeStart(docId)

    val header = decodeHeader(docStream, docId, feedback) ?: return null
    val content = DesignComposeDefinition.parseDelimitedFrom(docStream)

    val imageSessionData = decodeImageSession(docStream)
    val viewMap = content.views()
    val variantMap = createVariantViewMap(viewMap)
    val variantPropertyMap = createVariantPropertyMap(viewMap)
    val nodeIdMap = createNodeIdMap(viewMap)

    feedback.documentDecodeSuccess(header.dcVersion, header.name, header.lastModified, docId)

    return GenericDocContent(
        docId,
        header,
        content,
        variantMap,
        variantPropertyMap,
        nodeIdMap,
        imageSessionData.imageSessionData,
        imageSessionData.imageSession,
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
            val nodeName = nodeQuery.name
            val parentNodeName = nodeQuery.parent

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
            val nodeName = it.name
            val parentNodeName = it.parent
            val propertyValueList = nodeNameToPropertyValueList(nodeName)
            for (p in propertyValueList) propertyMap.addProperty(parentNodeName, p.first, p.second)
        }
    }
    return propertyMap
}

// Recursively add all views to a map indexed by the node id so that we can look up any view that
// we already have in our hash of views.
private fun createNodeIdMap(nodes: Map<NodeQuery, View>?): HashMap<String, View> {
    val nodeIdMap = HashMap<String, View>()
    fun addViewToMap(view: View) {
        nodeIdMap[view.id] = view
        if (view.data.hasContainer()) {
            view.data.container.childrenList.forEach { addViewToMap(it) }
        }
    }
    nodes?.values?.forEach { addViewToMap(it) }
    return nodeIdMap
}

private fun decodeHeader(
    docStream: InputStream,
    docId: DesignDocId,
    feedback: FeedbackImpl,
): DesignComposeDefinitionHeader? {
    // Now attempt to deserialize the doc)
    val header = DesignComposeDefinitionHeader.parseDelimitedFrom(docStream)
    if (header.dcVersion != FSAAS_DOC_VERSION) {
        feedback.documentDecodeVersionMismatch(FSAAS_DOC_VERSION, header.dcVersion, docId)
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

private fun decodeImageSession(docStream: InputStream): ImageSession {
    // The image session data is a JSON blob attached after the proto document content.
    val imageSessionData = docStream.readBytes()
    val imageSession =
        if (imageSessionData.isNotEmpty()) {
            String(imageSessionData, Charsets.UTF_8)
        } else {
            null
        }
    return ImageSession(imageSessionData, imageSession)
}
