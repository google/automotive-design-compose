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

import java.util.logging.Logger

const val TAG = "DesignCompose"

enum class FeedbackLevel {
    Debug,
    Info,
    Warn,
    Error,
}

class FeedbackMessage(
    val message: String,
    var count: Int,
    val timestamp: Long,
    val level: FeedbackLevel,
) {}

// Basic implementation of the Feedback class, used by docloader and Design Compose
abstract class FeedbackImpl {
    private val messages: ArrayDeque<FeedbackMessage> = ArrayDeque()
    private val ignoredDocuments: HashSet<DesignDocId> = HashSet()
    private var logLevel: FeedbackLevel = FeedbackLevel.Info
    private var maxMessages = 20
    var messagesListId = 0 // Change this every time the list changes so we can update subscribers

    // Implementation-specific functions
    abstract fun logMessage(str: String, level: FeedbackLevel)

    // Global public functions
    fun setLevel(lvl: FeedbackLevel) {
        logLevel = lvl
    }

    fun setMaxMessages(num: Int) {
        maxMessages = num
    }

    internal fun clearMessages() {
        messages.clear()
    }

    fun addIgnoredDocument(docId: DesignDocId): Boolean {
        ignoredDocuments.add(docId)
        return true
    }

    fun isDocumentIgnored(docId: DesignDocId): Boolean {
        return ignoredDocuments.contains(docId)
    }

    // Return the list of messages
    fun getMessages(): ArrayDeque<FeedbackMessage> {
        return messages
    }

    // Message functions

    fun diskLoadFail(id: String, docId: DesignDocId) {
        setStatus(
            "Unable to open $id from disk; will try live and from assets",
            FeedbackLevel.Debug,
            docId,
        )
    }

    fun documentUnchanged(docId: DesignDocId) {
        val truncatedId = shortDocId(docId)
        setStatus("Live update for $truncatedId unchanged...", FeedbackLevel.Info, docId)
    }

    fun documentUpdated(docId: DesignDocId, numSubscribers: Int) {
        val truncatedId = shortDocId(docId)
        setStatus(
            "Live update for $truncatedId fetched and informed $numSubscribers subscribers",
            FeedbackLevel.Info,
            docId,
        )
    }

    fun documentUpdateCode(docId: DesignDocId, code: Int) {
        val truncatedId = shortDocId(docId)
        setStatus(
            "Live update for $truncatedId unexpected server response: $code",
            FeedbackLevel.Error,
            docId,
        )
    }

    fun documentUpdateWarnings(docId: DesignDocId, msg: String) {
        val truncatedId = shortDocId(docId)
        setStatus("Live update for $truncatedId warning: $msg", FeedbackLevel.Warn, docId)
    }

    fun documentUpdateError(docId: DesignDocId, msg: String) {
        val truncatedId = shortDocId(docId)
        setStatus("Live update for $truncatedId failed: $msg", FeedbackLevel.Error, docId)
    }

    fun documentUpdateErrorRevert(docId: DesignDocId, msg: String) {
        val truncatedId = shortDocId(docId)
        setStatus(
            "Live update for $truncatedId failed: $msg, reverting to original doc ID",
            FeedbackLevel.Error,
            docId,
        )
    }

    fun documentDecodeStart(docId: DesignDocId) {
        val truncatedId = shortDocId(docId)
        setStatus("Starting to read doc $truncatedId...", FeedbackLevel.Debug, docId)
    }

    fun documentDecodeReadBytes(size: Int, docId: DesignDocId) {
        val truncatedId = shortDocId(docId)
        setStatus("Read $size bytes of doc $truncatedId", FeedbackLevel.Info, docId)
    }

    fun documentDecodeError(docId: DesignDocId) {
        val truncatedId = shortDocId(docId)
        setStatus("Error decoding doc $truncatedId", FeedbackLevel.Warn, docId)
    }

    fun documentDecodeVersionMismatch(expected: Int, actual: Int, docId: DesignDocId) {
        val truncatedId = shortDocId(docId)
        setStatus(
            "Wrong version in doc $truncatedId: Expected $expected but found $actual",
            FeedbackLevel.Warn,
            docId,
        )
    }

    fun documentDecodeSuccess(
        version: Int,
        name: String,
        lastModified: String,
        docId: DesignDocId,
    ) {
        setStatus(
            "Successfully deserialized V$version doc. Name: $name, last modified: $lastModified",
            FeedbackLevel.Info,
            docId,
        )
    }

    fun documentSaveTo(path: String, docId: DesignDocId) {
        val truncatedId = shortDocId(docId)
        setStatus("Saving doc $truncatedId to $path", FeedbackLevel.Info, docId)
    }

    fun documentSaveSuccess(docId: DesignDocId) {
        val truncatedId = shortDocId(docId)
        setStatus("Save doc $truncatedId success", FeedbackLevel.Info, docId)
    }

    fun documentSaveError(error: String, docId: DesignDocId) {
        val truncatedId = shortDocId(docId)
        setStatus("Unable to save doc $truncatedId: $error", FeedbackLevel.Error, docId)
    }

    fun documentVariableMissingWarning(docId: DesignDocId, varId: String) {
        val truncatedId = shortDocId(docId)
        setStatus(
            "Failed to get variable value for $varId in doc $truncatedId",
            FeedbackLevel.Warn,
            docId,
        )
    }

    open fun setStatus(str: String, level: FeedbackLevel, docId: DesignDocId) {
        // Ignore log levels we don't care about
        if (level < logLevel) return

        // Ignore if we don't care about this docId
        if (ignoredDocuments.contains(docId)) return

        logMessage(str, level)

        if (messages.isNotEmpty() && messages.first().message == str) {
            // Increment count if the message is the same
            ++messages.first().count
        } else {
            // Prepend new message and pop last if buffer is full
            val msg = FeedbackMessage(str, 1, System.currentTimeMillis(), level)
            if (messages.size == maxMessages) messages.removeLast()
            messages.addFirst(msg)
        }
        // Increase this ID to inform subscribers of a change
        ++messagesListId
    }

    protected fun shortDocId(docId: DesignDocId): String {
        val id = if (docId.id.length > 7) docId.id.substring(0, 7) else docId.id
        val versionId =
            if (docId.versionId.length > 4) docId.versionId.substring(docId.versionId.length - 4)
            else docId.versionId
        return if (versionId.isEmpty()) id else "${id}/${versionId}"
    }
}

// Instance of the Feedback class used in non-Android environments
object Feedback : FeedbackImpl() {
    private val javaLogger = Logger.getLogger(TAG)

    // Implementation-specific functions
    override fun logMessage(str: String, level: FeedbackLevel) {
        when (level) {
            FeedbackLevel.Debug -> javaLogger.config(str)
            FeedbackLevel.Info -> javaLogger.info(str)
            FeedbackLevel.Warn -> javaLogger.warning(str)
            FeedbackLevel.Error -> javaLogger.severe(str)
        }
    }
}
