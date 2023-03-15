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

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.designcompose.common.FeedbackImpl
import com.android.designcompose.common.FeedbackLevel

object Feedback : FeedbackImpl() {
    private val lastMessage: MutableLiveData<String> = MutableLiveData("")
    val subscribers: HashMap<String, (Int) -> Unit> = HashMap()

    // Implementation-specific functions
    override fun logMessage(str: String, level: FeedbackLevel) {
        when (level) {
            FeedbackLevel.Debug -> Log.d(TAG, str)
            FeedbackLevel.Info -> Log.i(TAG, str)
            FeedbackLevel.Warn -> Log.w(TAG, str)
            FeedbackLevel.Error -> Log.e(TAG, str)
        }
    }

    // Return the latest message
    internal fun getLatestMessage(): LiveData<String> {
        return lastMessage
    }

    // Register and unregister a listener of feedback messages
    fun register(id: String, setMessagesId: (Int) -> Unit) {
        subscribers[id] = setMessagesId
    }

    fun unregister(id: String) {
        subscribers.remove(id)
    }

    // Message functions
    fun addSubscriber(docId: String) {
        val truncatedId = shortDocId(docId)
        setStatus("Add subscriber for $truncatedId", FeedbackLevel.Debug, docId)
    }

    fun removeSubscriber(docId: String) {
        val truncatedId = shortDocId(docId)
        setStatus("Remove subscriber for $truncatedId", FeedbackLevel.Debug, docId)
    }

    internal fun assetLoadFail(id: String, docId: String) {
        setStatus("Unable to open $id from assets", FeedbackLevel.Debug, docId)
    }
    internal fun startLiveUpdate(docId: String) {
        val truncatedId = shortDocId(docId)
        setStatus("Live update fetching $truncatedId", FeedbackLevel.Debug, docId)
    }

    fun documentDecodeImages(numImages: Int, name: String, docId: String) {
        setStatus("Decoded $numImages images for $name", FeedbackLevel.Info, docId)
    }

    override fun setStatus(str: String, level: FeedbackLevel, docId: String) {
        super.setStatus(str, level, docId)

        lastMessage.postValue(str)

        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(
            kotlinx.coroutines.Runnable {
                // Notify all subscribers on the main thread
                subscribers.forEach { (_, setMessagesId) -> setMessagesId(messagesListId) }
            }
        )
    }
}
