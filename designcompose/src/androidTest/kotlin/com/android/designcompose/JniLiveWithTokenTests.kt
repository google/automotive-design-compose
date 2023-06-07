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

import androidx.test.platform.app.InstrumentationRegistry
import com.android.designcompose.common.DocumentServerParams
import io.mockk.mockkObject
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Test

const val smallDocID = "pxVlixodJqZL95zo2RzTHl" // HelloWorld Doc
const val largeDocID = "RfGl9SWnBEvdg8T1Ex6ZAR" // Battleship Doc
const val veryLargeDocID = "KKvsSWtfwRYxYibnaVKBQK" // Cluster Doc
/**
 * Jni fetch tests
 *
 * These tests use the JNI Library and will reach out to Figma.com itself.
 *
 * These tests can be excluded by running Gradle with:
 * -Pandroid.testInstrumentationRunnerArguments.notClass=com.android.designcompose.JniLiveWithTokenTests
 */
class JniLiveWithTokenTests {

    private val actualFigmaToken: String? =
        InstrumentationRegistry.getArguments().getString("FIGMA_ACCESS_TOKEN")
    private lateinit var firstFetchJson: String

    @Before
    fun setup() {
        assertNotNull(actualFigmaToken, "Cannot run this test without Figma Access Token")
        firstFetchJson = constructPostJson(actualFigmaToken, null, DocumentServerParams())

        mockkObject(Feedback)
    }

    @Test
    fun invalidDocId() {
        assertFailsWith<FigmaFileNotFoundException> {
            LiveUpdateJni.jniFetchDoc("InvalidDocID", firstFetchJson, ProxyConfig())
        }
    }

    private fun testFetch(docID: String) {
        with(LiveUpdateJni.fetchDocBytes(docID, firstFetchJson, ProxyConfig() = null)) {
            assertNotNull(this)
            val decodedDoc = decodeServerDoc(this, null, docID, null, Feedback)
            assertNotNull(decodedDoc)
            assertEquals(decodedDoc.c.docId, docID)
        }
    }

    @Test
    fun smallFetch() {
        testFetch(smallDocID)
    }
    @Test
    fun largeFetch() {
        testFetch(largeDocID)
    }
    // Currently failing due to #98
    @Test(expected = RuntimeException::class)
    fun veryLargeFetch() {
        testFetch(veryLargeDocID)
    }
}
