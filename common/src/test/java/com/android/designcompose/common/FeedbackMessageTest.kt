/*
 * Copyright 2024 Google LLC
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

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FeedbackMessageTest {

    private lateinit var feedback: FeedbackImpl
    private val docId = DesignDocId("doc1")

    @Before
    fun setup() {
        feedback =
            object : FeedbackImpl() {
                override fun logMessage(str: String, level: FeedbackLevel) {
                    // No-op for testing
                }
            }
        feedback.setLevel(FeedbackLevel.Debug)
        feedback.clearMessages()
    }

    @Test
    fun testDiskLoadFail() {
        feedback.diskLoadFail("file1", docId)
        val message = feedback.getMessages().first()
        assertThat(message.message)
            .isEqualTo("Unable to open file1 from disk; will try live and from assets")
        assertThat(message.level).isEqualTo(FeedbackLevel.Debug)
    }

    @Test
    fun testDocumentUnchanged() {
        feedback.documentUnchanged(docId)
        val message = feedback.getMessages().first()
        assertThat(message.message)
            .isEqualTo("Live update for ${feedback.shortDocId(docId)} unchanged...")
        assertThat(message.level).isEqualTo(FeedbackLevel.Info)
    }

    @Test
    fun testDocumentUpdated() {
        feedback.documentUpdated(docId, 5)
        val message = feedback.getMessages().first()
        assertThat(message.message)
            .isEqualTo(
                "Live update for ${feedback.shortDocId(docId)} fetched and informed 5 subscribers"
            )
        assertThat(message.level).isEqualTo(FeedbackLevel.Info)
    }

    @Test
    fun testDocumentUpdateCode() {
        feedback.documentUpdateCode(docId, 404)
        val message = feedback.getMessages().first()
        assertThat(message.message)
            .isEqualTo(
                "Live update for ${feedback.shortDocId(docId)} unexpected server response: 404"
            )
        assertThat(message.level).isEqualTo(FeedbackLevel.Error)
    }

    @Test
    fun testDocumentUpdateWarnings() {
        feedback.documentUpdateWarnings(docId, "warning message")
        val message = feedback.getMessages().first()
        assertThat(message.message)
            .isEqualTo("Live update for ${feedback.shortDocId(docId)} warning: warning message")
        assertThat(message.level).isEqualTo(FeedbackLevel.Warn)
    }

    @Test
    fun testDocumentUpdateError() {
        feedback.documentUpdateError(docId, "error message")
        val message = feedback.getMessages().first()
        assertThat(message.message)
            .isEqualTo("Live update for ${feedback.shortDocId(docId)} failed: error message")
        assertThat(message.level).isEqualTo(FeedbackLevel.Error)
    }

    @Test
    fun testDocumentUpdateErrorRevert() {
        feedback.documentUpdateErrorRevert(docId, "error message")
        val message = feedback.getMessages().first()
        assertThat(message.message)
            .isEqualTo(
                "Live update for ${feedback.shortDocId(docId)} failed: error message, reverting to original doc ID"
            )
        assertThat(message.level).isEqualTo(FeedbackLevel.Error)
    }

    @Test
    fun testDocumentDecodeStart() {
        feedback.documentDecodeStart(docId)
        val message = feedback.getMessages().first()
        assertThat(message.message)
            .isEqualTo("Starting to read doc ${feedback.shortDocId(docId)}...")
        assertThat(message.level).isEqualTo(FeedbackLevel.Debug)
    }

    @Test
    fun testDocumentDecodeReadBytes() {
        feedback.documentDecodeReadBytes(1024, docId)
        val message = feedback.getMessages().first()
        assertThat(message.message)
            .isEqualTo("Read 1024 bytes of doc ${feedback.shortDocId(docId)}")
        assertThat(message.level).isEqualTo(FeedbackLevel.Info)
    }

    @Test
    fun testDocumentDecodeError() {
        feedback.documentDecodeError(docId)
        val message = feedback.getMessages().first()
        assertThat(message.message).isEqualTo("Error decoding doc ${feedback.shortDocId(docId)}")
        assertThat(message.level).isEqualTo(FeedbackLevel.Warn)
    }

    @Test
    fun testDocumentDecodeVersionMismatch() {
        feedback.documentDecodeVersionMismatch(1, 2, docId)
        val message = feedback.getMessages().first()
        assertThat(message.message)
            .isEqualTo("Wrong version in doc ${feedback.shortDocId(docId)}: Expected 1 but found 2")
        assertThat(message.level).isEqualTo(FeedbackLevel.Warn)
    }

    @Test
    fun testDocumentDecodeSuccess() {
        feedback.documentDecodeSuccess(1, "Test Doc", "2024-01-01", docId)
        val message = feedback.getMessages().first()
        assertThat(message.message)
            .isEqualTo(
                "Successfully deserialized V1 doc. Name: Test Doc, last modified: 2024-01-01"
            )
        assertThat(message.level).isEqualTo(FeedbackLevel.Info)
    }

    @Test
    fun testDocumentSaveTo() {
        feedback.documentSaveTo("/path/to/file", docId)
        val message = feedback.getMessages().first()
        assertThat(message.message)
            .isEqualTo("Saving doc ${feedback.shortDocId(docId)} to /path/to/file")
        assertThat(message.level).isEqualTo(FeedbackLevel.Info)
    }

    @Test
    fun testDocumentSaveSuccess() {
        feedback.documentSaveSuccess(docId)
        val message = feedback.getMessages().first()
        assertThat(message.message).isEqualTo("Save doc ${feedback.shortDocId(docId)} success")
        assertThat(message.level).isEqualTo(FeedbackLevel.Info)
    }

    @Test
    fun testDocumentSaveError() {
        feedback.documentSaveError("error message", docId)
        val message = feedback.getMessages().first()
        assertThat(message.message)
            .isEqualTo("Unable to save doc ${feedback.shortDocId(docId)}: error message")
        assertThat(message.level).isEqualTo(FeedbackLevel.Error)
    }

    @Test
    fun testDocumentVariableMissingWarning() {
        feedback.documentVariableMissingWarning(docId, "var1")
        val message = feedback.getMessages().first()
        assertThat(message.message)
            .isEqualTo("Failed to get variable value for var1 in doc ${feedback.shortDocId(docId)}")
        assertThat(message.level).isEqualTo(FeedbackLevel.Warn)
    }
}
