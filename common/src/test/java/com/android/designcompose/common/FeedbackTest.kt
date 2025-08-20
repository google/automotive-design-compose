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

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

class MockFeedback : FeedbackImpl() {
    var lastLoggedMessage = ""
    var lastLogLevel = FeedbackLevel.Debug

    override fun logMessage(str: String, level: FeedbackLevel) {
        lastLoggedMessage = str
        lastLogLevel = level
    }
}

@RunWith(JUnit4::class)
class FeedbackTest {
    @Test
    fun testSetLevel() {
        val feedback = MockFeedback()
        feedback.setLevel(FeedbackLevel.Error)
        feedback.setStatus("Test Message", FeedbackLevel.Warn, DesignDocId("test"))
        assertEquals("", feedback.lastLoggedMessage)
    }

    @Test
    fun testMaxMessages() {
        val feedback = MockFeedback()
        feedback.setMaxMessages(2)
        feedback.setStatus("Message 1", FeedbackLevel.Info, DesignDocId("test"))
        feedback.setStatus("Message 2", FeedbackLevel.Info, DesignDocId("test"))
        feedback.setStatus("Message 3", FeedbackLevel.Info, DesignDocId("test"))
        assertEquals(2, feedback.getMessages().size)
        assertEquals("Message 3", feedback.getMessages().first().message)
    }

    @Test
    fun testIgnoredDocument() {
        val feedback = MockFeedback()
        val docId = DesignDocId("ignored")
        feedback.addIgnoredDocument(docId)
        assertTrue(feedback.isDocumentIgnored(docId))
        feedback.setStatus("Ignored Message", FeedbackLevel.Info, docId)
        assertEquals("", feedback.lastLoggedMessage)
    }

    @Test
    fun testShortDocId() {
        val feedback = MockFeedback()
        val longDocId = DesignDocId("1234567890", "abcdefgh")
        assertEquals("1234567/efgh", feedback.shortDocId(longDocId))
        val shortDocId = DesignDocId("123", "abc")
        assertEquals("123/abc", feedback.shortDocId(shortDocId))
    }
}
