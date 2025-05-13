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
import com.android.designcompose.definition.copy
import com.android.designcompose.definition.view.View
import com.android.designcompose.live_update.ConvertResponse
import com.android.designcompose.live_update.figma.FigmaDocInfo
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteString
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
    val imageSession: ByteString,
    val branches: List<FigmaDocInfo>? = null,
    val projectFiles: List<FigmaDocInfo>? = null,
) {
    val inMemoryImagesMap: MutableMap<String, ByteString> = document.imagesMap.toMutableMap()

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
            // Write cached images map to disk
            if (inMemoryImagesMap == document.imagesMap) {
                document.writeDelimitedTo(outputStream)
            } else {
                document
                    .copy {
                        images.clear()
                        images.putAll(inMemoryImagesMap)
                    }
                    .writeDelimitedTo(outputStream)
            }
            outputStream.write(imageSession.toByteArray())
            return outputStream.toByteArray()
        } catch (error: Throwable) {
            feedback.documentSaveError(error.toString(), docId)
        }
        return null
    }
}

/// Read a serialized server document from the given stream. Deserialize it and save it to disk.
fun decodeServerBaseDoc(
    docResponse: ConvertResponse.Document,
    docId: DesignDocId,
    feedback: FeedbackImpl,
): GenericDocContent? {

    val header = docResponse.header
    feedback.documentDecodeSuccess(header.dcVersion, header.name, header.lastModified, docId)

    // Server sends content in the format of ServerFigmaDoc, which has additional data
    //    val serverDoc = ServerFigmaDoc.parseDelimitedFrom(docStream)
    val serverDoc = docResponse.serverDoc
    serverDoc.errorsList?.forEach { feedback.documentUpdateWarnings(docId, it) }
    val content = serverDoc.figmaDoc

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
        docResponse.imageSessionJson,
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

    // Proto bytes are parsed to their immutable ByteString representation. It's just a ByteArray
    // that's immutable, basically.
    val imageSession = docStream.readBytes().toByteString()
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
        imageSession,
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
    // Warn for a version mismatch, but don't fail since we support backward compatibility with old
    // protobuf versions
    if (header.dcVersion != FSAAS_DOC_VERSION)
        feedback.documentDecodeVersionMismatch(FSAAS_DOC_VERSION, header.dcVersion, docId)
    return header
}

fun headerVersion(docStream: InputStream): Int {
    val header = DesignComposeDefinitionHeader.parseDelimitedFrom(docStream)
    return header.dcVersion
}
