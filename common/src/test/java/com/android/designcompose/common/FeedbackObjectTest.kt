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

import com.google.common.truth.Truth.assertThat
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FeedbackObjectTest {
    private val logger = Logger.getLogger(TAG)
    private val handler = TestHandler()
    private var originalLevel: Level? = null

    @Before
    fun setUp() {
        originalLevel = logger.level
        logger.level = Level.ALL
        logger.addHandler(handler)
        handler.records.clear()
    }

    @After
    fun tearDown() {
        logger.removeHandler(handler)
        logger.level = originalLevel
    }

    @Test
    fun testLogMessage() {
        Feedback.logMessage("Debug message", FeedbackLevel.Debug)
        assertThat(handler.records).isNotEmpty()
        assertThat(handler.records.last().level).isEqualTo(Level.CONFIG)
        assertThat(handler.records.last().message).isEqualTo("Debug message")

        Feedback.logMessage("Info message", FeedbackLevel.Info)
        assertThat(handler.records.last().level).isEqualTo(Level.INFO)
        assertThat(handler.records.last().message).isEqualTo("Info message")

        Feedback.logMessage("Warn message", FeedbackLevel.Warn)
        assertThat(handler.records.last().level).isEqualTo(Level.WARNING)
        assertThat(handler.records.last().message).isEqualTo("Warn message")

        Feedback.logMessage("Error message", FeedbackLevel.Error)
        assertThat(handler.records.last().level).isEqualTo(Level.SEVERE)
        assertThat(handler.records.last().message).isEqualTo("Error message")
    }

    private class TestHandler : Handler() {
        val records = mutableListOf<LogRecord>()

        override fun publish(record: LogRecord) {
            records.add(record)
        }

        override fun flush() {}

        override fun close() {}
    }
}
