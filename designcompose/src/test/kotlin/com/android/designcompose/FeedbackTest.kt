/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.designcompose

import android.os.Looper
import android.util.Log
import com.android.designcompose.common.DesignDocId
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FeedbackTest {
    private val docId = DesignDocId("test")

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        Feedback.clear()
    }

    @After
    fun tearDown() {
        // Unregister the subscriber to ensure tests are isolated
        Feedback.unregister(docId)
    }

    @Test
    fun testFeedback() {
        var messageId = -1 // Use a sentinel value to be sure the callback ran
        Feedback.register(docId) { id -> messageId = id }

        Feedback.addSubscriber(docId)
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()

        // Change the assertion to check FOR 0, not against it.
        assertThat(messageId).isEqualTo(0)
    }
}
