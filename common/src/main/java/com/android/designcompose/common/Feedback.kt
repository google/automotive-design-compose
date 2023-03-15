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

import java.lang.Exception

const val TAG = "DesignCompose"

enum class FeedbackLevel {
    Debug,
    Info,
    Warn,
    Error
}

class FeedbackMessage(
    val message: String,
    var count: Int,
    val timestamp: Long,
    val level: FeedbackLevel
) {}

// Basic implementation of the Feedback class, used by docloader and Design Compose
abstract class FeedbackImpl {
    private val messages: ArrayDeque<FeedbackMessage> = ArrayDeque()
    private val ignoredDocuments: HashSet<String> = HashSet()
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

    fun addIgnoredDocument(docId: String): Boolean {
        ignoredDocuments.add(docId)
        return true
    }

    fun isDocumentIgnored(docId: String): Boolean {
        return ignoredDocuments.contains(docId)
    }

    // Return the list of messages
    fun getMessages(): ArrayDeque<FeedbackMessage> {
        return messages
    }

    // Message functions
    //    fun newDocServer(url: String){
    //    }

    fun diskLoadFail(id: String, docId: String) {
        setStatus(
            "Unable to open $id from disk; will try live and from assets",
            FeedbackLevel.Debug,
            docId
        )
    }

    fun documentUnchanged(docId: String) {
        val truncatedId = shortDocId(docId)
        setStatus("Live update for $truncatedId unchanged...", FeedbackLevel.Info, docId)
    }

    fun documentUpdated(docId: String, numSubscribers: Int) {
        val truncatedId = shortDocId(docId)
        setStatus(
            "Live update for $truncatedId fetched and informed $numSubscribers subscribers",
            FeedbackLevel.Info,
            docId
        )
    }

    fun documentUpdateCode(docId: String, code: Int) {
        val truncatedId = shortDocId(docId)
        setStatus(
            "Live update for $truncatedId unexpected server response: $code",
            FeedbackLevel.Error,
            docId
        )
    }

    fun documentUpdateWarnings(docId: String, msg: String) {
        val truncatedId = shortDocId(docId)
        setStatus("Live update for $truncatedId warning: $msg", FeedbackLevel.Warn, docId)
    }

    fun documentUpdateError(docId: String, exception: Exception) {
        val truncatedId = shortDocId(docId)
        setStatus("Live update for $truncatedId failed with $exception", FeedbackLevel.Error, docId)
    }

    fun documentUpdateError(docId: String, url: String, exception: Exception) {
        val truncatedId = shortDocId(docId)
        setStatus(
            "Live update for $truncatedId failed with $exception for url $url",
            FeedbackLevel.Error,
            docId
        )
    }

    fun documentUpdateError(docId: String, code: Int, errorMessage: String?) {
        val truncatedId = shortDocId(docId)
        setStatus(
            "Live update for $truncatedId failed with server code $code, error: $errorMessage",
            FeedbackLevel.Error,
            docId
        )
    }

    fun documentUpdateErrorRevert(docId: String, exception: Exception) {
        val truncatedId = shortDocId(docId)
        setStatus(
            "Live update for $truncatedId failed with error: $exception, reverting to original doc ID",
            FeedbackLevel.Error,
            docId
        )
    }

    fun documentDecodeStart(docId: String) {
        val truncatedId = shortDocId(docId)
        setStatus("Starting to read doc $truncatedId...", FeedbackLevel.Debug, docId)
    }

    fun documentDecodeReadBytes(size: Int, docId: String) {
        val truncatedId = shortDocId(docId)
        setStatus("Read $size bytes of doc $truncatedId", FeedbackLevel.Info, docId)
    }

    fun documentDecodeError(docId: String) {
        val truncatedId = shortDocId(docId)
        setStatus("Error decoding doc $truncatedId", FeedbackLevel.Warn, docId)
    }

    fun documentDecodeVersionMismatch(expected: Int, actual: Int, docId: String) {
        val truncatedId = shortDocId(docId)
        setStatus(
            "Wrong version in doc $truncatedId: Expected $expected but found $actual",
            FeedbackLevel.Warn,
            docId
        )
    }

    fun documentDecodeSuccess(version: Int, name: String, lastModified: String, docId: String) {
        setStatus(
            "Successfully deserialized V$version doc. Name: $name, last modified: $lastModified",
            FeedbackLevel.Info,
            docId
        )
    }

    fun documentSaveTo(path: String, docId: String) {
        val truncatedId = shortDocId(docId)
        setStatus("Saving doc $truncatedId to $path", FeedbackLevel.Info, docId)
    }

    fun documentSaveSuccess(docId: String) {
        val truncatedId = shortDocId(docId)
        setStatus("Save doc $truncatedId success", FeedbackLevel.Info, docId)
    }

    fun documentSaveError(error: String, docId: String) {
        val truncatedId = shortDocId(docId)
        setStatus("Unable to save doc $truncatedId: $error", FeedbackLevel.Error, docId)
    }

    open fun setStatus(str: String, level: FeedbackLevel, docId: String) {
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

    protected fun shortDocId(docId: String): String {
        return if (docId.length > 7) docId.substring(0, 7) else docId
    }
}

// This has been commented out since it was only used by the gradle preview plugin which currently
// has been disabled. If we bring the plugin back we may need to uncomment this code so that the
// plugin can log feedback messages.
/*
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
*/
