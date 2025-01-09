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

package com.android.designcompose.figmaIntegrationTests

import androidx.test.platform.app.InstrumentationRegistry
import com.android.designcompose.AccessDeniedException
import com.android.designcompose.Feedback
import com.android.designcompose.FigmaFileNotFoundException
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.common.DocumentServerParams
import com.android.designcompose.decodeServerDoc
import com.android.designcompose.fetchDocument
import com.android.designcompose.live_update.documentOrNull
import io.mockk.mockkObject
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Test

const val smallDocID = "pxVlixodJqZL95zo2RzTHl" // HelloWorld Doc
const val largeDocID = "RfGl9SWnBEvdg8T1Ex6ZAR" // Battleship Doc
const val veryLargeDocID = "f5zC8J6uGPzsWLUeE4AW4D" // Cluster Doc

/**
 * Jni fetch tests
 *
 * These tests use the JNI Library and will reach out to Figma.com itself.
 *
 * These tests can be excluded by running Gradle with:
 * -Pandroid.testInstrumentationRunnerArguments.notPackage=com.android.designcompose.figmaIntegrationTests
 */
class JniFetchTests {

    private val actualFigmaToken: String? =
        InstrumentationRegistry.getArguments().getString("FIGMA_ACCESS_TOKEN")

    @Before
    fun setup() {
        assertNotNull(actualFigmaToken, "Cannot run this test without Figma Access Token")
        mockkObject(Feedback)
    }

    private fun testFetch(docID: String) {
        val response =
            fetchDocument(actualFigmaToken!!, DocumentServerParams(), null, DesignDocId(docID))

        assertNotNull(response)
        val decodedDoc =
            decodeServerDoc(response.documentOrNull!!, null, DesignDocId(docID), null, Feedback)
        assertNotNull(decodedDoc)
        assertEquals(decodedDoc.c.docId.id, docID)
    }

    @Test
    fun smallFetch() {
        testFetch(smallDocID)
    }

    @Test
    fun largeFetch() {
        testFetch(largeDocID)
    }

    //    @Test
    //    fun veryLargeFetch() {
    //        testFetch(veryLargeDocID)
    //    }

    @Test
    fun invalidToken() {
        assertFailsWith(AccessDeniedException::class) {
            fetchDocument(
                "NOT_A_FIGMA_TOKEN",
                DocumentServerParams(),
                null,
                DesignDocId(smallDocID),
            )
        }
    }

    @Test
    fun invalidDocId() {
        assertFailsWith<FigmaFileNotFoundException> {
            fetchDocument(
                actualFigmaToken!!,
                DocumentServerParams(),
                null,
                DesignDocId("NOT_A_DOC"),
            )
        }
    }
}
