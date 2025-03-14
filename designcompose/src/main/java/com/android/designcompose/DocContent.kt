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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.tracing.trace
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.common.FeedbackImpl
import com.android.designcompose.common.GenericDocContent
import com.android.designcompose.common.NodeQuery
import com.android.designcompose.common.decodeDiskBaseDoc
import com.android.designcompose.common.decodeServerBaseDoc
import com.android.designcompose.common.views
import com.android.designcompose.definition.element.Bounds
import com.android.designcompose.definition.element.KeyframeVariant
import com.android.designcompose.definition.element.ScalableUIComponentSet
import com.android.designcompose.definition.element.ScalableUiVariant
import com.android.designcompose.definition.element.bounds
import com.android.designcompose.definition.element.copy
import com.android.designcompose.definition.element.scalableDimension
import com.android.designcompose.definition.element.setOrNull
import com.android.designcompose.definition.element.variantOrNull
import com.android.designcompose.definition.view.nodeStyleOrNull
import com.android.designcompose.definition.view.scalableDataOrNull
import com.android.designcompose.definition.view.styleOrNull
import com.android.designcompose.live_update.ConvertResponse
import com.google.protobuf.kotlin.get
import java.io.File
import java.io.InputStream

// Essentially a wrapper class for the GenericDocContent. The initializer
// will decode the saved images into Bitmap objects (Android specific) for display

class DocContent(var c: GenericDocContent, previousDoc: DocContent?) {

    // Replace this record in the decoded doc.// Build upon the previously decoded images, because
    // if
    // we had previous content then we will
    // have told the server about the images we've already downloaded (and it will have skipped
    // sending them).
    //
    // We don't know if we should retire images from the previous run, so if we go for a long
    // time on a doc, then we could end up with a lot of unused images in memory.
    //
    // We also want to build a complete set of images for the doc we save to disk (if we're saving);
    // since we can't write to our just decoded doc, so any zero length byte arrays get updated to
    // point to the actual bytes we already have in heap.
    private var images: HashMap<String, Bitmap> = HashMap()

    init {
        for ((imageKey, bytes) in c.inMemoryImagesMap) {
            if (bytes.isEmpty) {
                if (
                    previousDoc != null &&
                        previousDoc.images[imageKey] != null &&
                        previousDoc.c.inMemoryImagesMap[imageKey] != null
                ) {
                    images[imageKey] = previousDoc.images[imageKey]!!
                    // Replace this record and write to disk as the images of the decoded doc.
                    c.inMemoryImagesMap[imageKey] = previousDoc.c.inMemoryImagesMap[imageKey]!!
                }
            } else {
                val byteArray = ByteArray(bytes.size()) { i -> bytes[i] }
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                images[imageKey] = bitmap
            }
        }
        Feedback.documentDecodeImages(c.document.imagesMap.size, c.header.name, c.docId)
    }

    /**
     * Look up an image resource from the document. The backend service pre-rasterizes complex
     * vector content at a variety of pixel densities. This function selects the appropriate
     * pre-rasterized image and also returns the density of the image.
     *
     * @return Pair<Image, Density>, or null if no match was found.
     */
    internal fun image(key: String, density: Float): Pair<Bitmap, Float>? {
        // We know that the service only encodes 1x (baseline), @2x and @3x.
        if (density < 1.2) {
            val img = this.images[key]
            if (img != null) return Pair(img, 1.0f)
        } else if (density < 2.2 && this.images.containsKey("$key@2x")) {
            val img = this.images["$key@2x"]
            if (img != null) return Pair(img, 2.0f)
        } else if (this.images.containsKey("$key@3x")) {
            val img = this.images["$key@3x"]
            if (img != null) return Pair(img, 3.0f)
        }
        val img = this.images[key]
        if (img != null) return Pair(img, 1.0f)
        return null
    }
}

fun decodeDiskDoc(
    docStream: InputStream,
    previousDoc: DocContent?,
    docId: DesignDocId,
    feedback: FeedbackImpl,
): DocContent? {
    var docContent: DocContent? = null
    trace(DCTraces.DECODEDISKDOC) {
        val baseDoc = decodeDiskBaseDoc(docStream, docId, feedback) ?: return@trace
        docContent = DocContent(baseDoc, previousDoc)
    }
    return docContent
}

fun decodeServerDoc(
    docResponse: ConvertResponse.Document,
    previousDoc: DocContent?,
    docId: DesignDocId,
    save: File?,
    feedback: FeedbackImpl,
): DocContent? {
    // We must initialize the fully-decoded DocContent, which decodes images before
    // saving it to disk
    val baseDoc = decodeServerBaseDoc(docResponse, docId, feedback) ?: return null
    val fullDoc = DocContent(baseDoc, previousDoc)
    save?.let { fullDoc.c.save(save, Feedback) }
    return fullDoc
}

class ScalableUiDoc(doc: DocContent) {
    // variant name -> scalable ui data
    val variantMap: HashMap<String, ScalableUiVariant> = HashMap()
    // variant id -> scalable ui data
    val variantIdMap: HashMap<String, ScalableUiVariant> = HashMap()
    // component set name -> { event name -> variant name }
    val componentSetMap: HashMap<String, ScalableUIComponentSet> =
        HashMap() // HashMap<String, String>> = HashMap()

    init {
        val allViews = doc.c.document.views()
        doc.c.variantViewMap.forEach { setMap ->
            val componentSetQuery = NodeQuery.NodeComponentSet(setMap.key)
            val componentSetView = allViews[componentSetQuery]
            componentSetView?.styleOrNull?.nodeStyleOrNull?.scalableDataOrNull?.setOrNull?.let {
                setData ->
                val setName = setData.name
                componentSetMap[setName] = setData
            }

            setMap.value.forEach { variantMap ->
                val variant = variantMap.value
                if (variant.data.hasContainer()) {
                    if (variant.data.container.childrenCount > 0) {
                        val child = variant.data.container.getChildren(0)
                        if (child.name == "main") {
                            val layout = child.style.layoutStyle
                            val scalableUiVariant =
                                variant.styleOrNull
                                    ?.nodeStyleOrNull
                                    ?.scalableDataOrNull
                                    ?.variantOrNull
                                    ?.copy {
                                        isVisible =
                                            when (child.style.nodeStyle.displayType) {
                                                com.android.designcompose.definition.view.Display
                                                    .DISPLAY_NONE -> false
                                                else -> true
                                            }
                                        alpha =
                                            if (child.style.nodeStyle.hasOpacity())
                                                child.style.nodeStyle.opacity
                                            else 1f
                                        bounds = bounds {
                                            left = scalableDimension {
                                                points = layout.margin.start.points
                                            }
                                            top = scalableDimension {
                                                points = layout.margin.top.points
                                            }
                                            right = scalableDimension {
                                                points = layout.margin.end.points
                                            }
                                            bottom = scalableDimension {
                                                points = layout.margin.bottom.points
                                            }
                                            width = scalableDimension {
                                                points = layout.width.points
                                            }
                                            height = scalableDimension {
                                                points = layout.height.points
                                            }
                                        }
                                    }
                            scalableUiVariant?.let {
                                this.variantMap[variantMap.key] = it
                                this.variantIdMap[variant.id] = it
                            }
                        }
                    }
                }
            }

            componentSetView?.styleOrNull?.nodeStyleOrNull?.scalableDataOrNull?.setOrNull?.let {
                setData ->
                val setName = setData.name
                componentSetMap[setName] = setData
                println("### Set ${setData.name}, ${setData.id}")
                setData.variantIdsList.forEach {
                    println(
                        "  ### Variant $it: Default ${variantIdMap[it]?.isDefault} Visible ${variantIdMap[it]?.isVisible}"
                    )
                }
            }
        }
    }

    fun hasVariant(variantName: String): Boolean {
        return variantMap.containsKey(variantName)
    }

    fun getVisible(variantName: String): Boolean {
        return variantMap[variantName]?.isVisible == true
    }

    fun getBounds(variantName: String): Bounds? {
        return variantMap[variantName]?.bounds
    }

    fun getTransitions(componentSetName: String, result: HashMap<String, String>) {
        val eventHash = componentSetMap[componentSetName]?.eventMapMap
        eventHash?.forEach { result[it.key] = it.value.variantName }
    }

    fun getKeyframeVariantList(componentSetName: String): List<KeyframeVariant>? {
        return componentSetMap[componentSetName]?.keyframeVariantsList
    }

    fun getPanels(): List<ScalableUIComponentSet> {
        return componentSetMap.values.toList()
    }

    fun getVariantById(id: String): ScalableUiVariant? {
        return variantIdMap[id]
    }
}
