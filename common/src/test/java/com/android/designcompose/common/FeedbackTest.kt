package com.android.designcompose.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FeedbackTest {
    @Test
    fun testSetStatus() {
        val feedback =
            object : FeedbackImpl() {
                override fun logMessage(str: String, level: FeedbackLevel) {}
            }
        feedback.setStatus("test message", FeedbackLevel.Info, DesignDocId("testDoc"))
        assertThat(feedback.getMessages()).hasSize(1)
        assertThat(feedback.getMessages().first().message).isEqualTo("test message")
    }

    @Test
    fun testAddIgnoredDocument() {
        val feedback =
            object : FeedbackImpl() {
                override fun logMessage(str: String, level: FeedbackLevel) {}
            }
        val docId = DesignDocId("testDoc")
        feedback.addIgnoredDocument(docId)
        assertThat(feedback.isDocumentIgnored(docId)).isTrue()
    }

    @Test
    fun testClearMessages() {
        val feedback =
            object : FeedbackImpl() {
                override fun logMessage(str: String, level: FeedbackLevel) {}
            }
        feedback.setStatus("test message", FeedbackLevel.Info, DesignDocId("testDoc"))
        feedback.clearMessages()
        assertThat(feedback.getMessages()).isEmpty()
    }

    @Test
    fun testShortDocId() {
        val feedback =
            object : FeedbackImpl() {
                override fun logMessage(str: String, level: FeedbackLevel) {}
            }
        val docId = DesignDocId("123456789", "abcdefg")
        assertThat(feedback.shortDocId(docId)).isEqualTo("1234567/defg")
    }

    @Test
    fun testDocumentMessages() {
        val feedback =
            object : FeedbackImpl() {
                override fun logMessage(str: String, level: FeedbackLevel) {}
            }
        feedback.setLevel(FeedbackLevel.Debug)
        val docId = DesignDocId("testDoc")
        feedback.diskLoadFail("testId", docId)
        assertThat(feedback.getMessages().first().message)
            .isEqualTo("Unable to open testId from disk; will try live and from assets")
        feedback.documentUnchanged(docId)
        assertThat(feedback.getMessages().first().message)
            .isEqualTo("Live update for testDoc unchanged...")
        feedback.documentUpdated(docId, 1)
        assertThat(feedback.getMessages().first().message)
            .isEqualTo("Live update for testDoc fetched and informed 1 subscribers")
        feedback.documentUpdateCode(docId, 404)
        assertThat(feedback.getMessages().first().message)
            .isEqualTo("Live update for testDoc unexpected server response: 404")
        feedback.documentUpdateWarnings(docId, "test warning")
        assertThat(feedback.getMessages().first().message)
            .isEqualTo("Live update for testDoc warning: test warning")
        feedback.documentUpdateError(docId, "test error")
        assertThat(feedback.getMessages().first().message)
            .isEqualTo("Live update for testDoc failed: test error")
        feedback.documentUpdateErrorRevert(docId, "test error")
        assertThat(feedback.getMessages().first().message)
            .isEqualTo("Live update for testDoc failed: test error, reverting to original doc ID")
        feedback.documentDecodeStart(docId)
        assertThat(feedback.getMessages().first().message)
            .isEqualTo("Starting to read doc testDoc...")
        feedback.documentDecodeReadBytes(100, docId)
        assertThat(feedback.getMessages().first().message)
            .isEqualTo("Read 100 bytes of doc testDoc")
        feedback.documentDecodeError(docId)
        assertThat(feedback.getMessages().first().message).isEqualTo("Error decoding doc testDoc")
        feedback.documentDecodeVersionMismatch(1, 2, docId)
        assertThat(feedback.getMessages().first().message)
            .isEqualTo("Wrong version in doc testDoc: Expected 1 but found 2")
        feedback.documentDecodeSuccess(1, "testName", "testDate", docId)
        assertThat(feedback.getMessages().first().message)
            .isEqualTo("Successfully deserialized V1 doc. Name: testName, last modified: testDate")
        feedback.documentSaveTo("testPath", docId)
        assertThat(feedback.getMessages().first().message)
            .isEqualTo("Saving doc testDoc to testPath")
        feedback.documentSaveSuccess(docId)
        assertThat(feedback.getMessages().first().message).isEqualTo("Save doc testDoc success")
        feedback.documentSaveError("test error", docId)
        assertThat(feedback.getMessages().first().message)
            .isEqualTo("Unable to save doc testDoc: test error")
        feedback.documentVariableMissingWarning(docId, "testVar")
        assertThat(feedback.getMessages().first().message)
            .isEqualTo("Failed to get variable value for testVar in doc testDoc")
    }
}
