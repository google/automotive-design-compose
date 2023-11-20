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
import com.android.designcompose.common.FeedbackImpl
import com.android.designcompose.common.GenericDocContent
import com.android.designcompose.common.decodeDiskBaseDoc
import com.android.designcompose.common.decodeServerBaseDoc
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
        for ((imageKey, bytes) in c.document.images.value) {
            if (bytes.content().isEmpty()) {
                if (previousDoc != null && previousDoc.images[imageKey.value] != null) {
                    images[imageKey.value] = previousDoc.images[imageKey.value]!!
                    // Replace this record in the decoded doc.
                    c.document.images.value[imageKey] =
                        previousDoc.c.document.images.value[imageKey]
                }
            } else {
                val bitmap = BitmapFactory.decodeByteArray(bytes.content(), 0, bytes.content().size)
                images[imageKey.value] = bitmap
            }
        }
        Feedback.documentDecodeImages(c.document.images.value.size, c.document.name, c.docId)
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
    docId: String,
    feedback: FeedbackImpl
): DocContent? {
    var docContent: DocContent? = null
    trace(DCTraces.DECODEDISKDOC) {
        val baseDoc = decodeDiskBaseDoc(docStream, docId, feedback) ?: return@trace
        docContent = DocContent(baseDoc, previousDoc)
    }
    return docContent
}

fun decodeServerDoc(
    docBytes: ByteArray,
    previousDoc: DocContent?,
    docId: String,
    save: File?,
    feedback: FeedbackImpl
): DocContent? {
    // We must initialize the fully-decoded DocContent, which decodes images before
    // saving it to disk
    val baseDoc = decodeServerBaseDoc(docBytes, docId, feedback) ?: return null
    val fullDoc = DocContent(baseDoc, previousDoc)
    save?.let { fullDoc.c.save(save, Feedback) }
    return fullDoc
}
