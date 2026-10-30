/*
 * Copyright 2025 Google LLC
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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LiveUpdateExceptionsTest {

    @Test
    fun testFetchException() {
        val defaultEx = FetchException()
        assertThat(defaultEx).isInstanceOf(RuntimeException::class.java)
        assertThat(defaultEx.message).isNull()

        val messageEx = FetchException("Bad request: 400")
        assertThat(messageEx.message).isEqualTo("Bad request: 400")
    }

    @Test
    fun testAccessDeniedException() {
        try {
            throw AccessDeniedException()
        } catch (e: AccessDeniedException) {
            assertThat(e).isInstanceOf(FetchException::class.java)
            assertThat(e.message).isNull()
        }

        val messageEx = AccessDeniedException("Invalid Authentication Token")
        assertThat(messageEx).isInstanceOf(FetchException::class.java)
        assertThat(messageEx.message).isEqualTo("Invalid Authentication Token")
    }

    @Test
    fun testFigmaFileNotFoundException() {
        try {
            throw FigmaFileNotFoundException()
        } catch (e: FigmaFileNotFoundException) {
            assertThat(e).isInstanceOf(FetchException::class.java)
            assertThat(e.message).isNull()
        }

        val messageEx = FigmaFileNotFoundException("doc_123")
        assertThat(messageEx).isInstanceOf(FetchException::class.java)
        assertThat(messageEx.message).isEqualTo("doc_123")
    }

    @Test
    fun testRateLimitedException() {
        try {
            throw RateLimitedException()
        } catch (e: RateLimitedException) {
            assertThat(e).isInstanceOf(FetchException::class.java)
            assertThat(e.message).isNull()
        }

        val messageEx = RateLimitedException("Figma Rate Limit Exceeded")
        assertThat(messageEx).isInstanceOf(FetchException::class.java)
        assertThat(messageEx.message).isEqualTo("Figma Rate Limit Exceeded")
    }

    @Test
    fun testInternalFigmaErrorException() {
        try {
            throw InternalFigmaErrorException()
        } catch (e: InternalFigmaErrorException) {
            assertThat(e).isInstanceOf(FetchException::class.java)
            assertThat(e.message).isNull()
        }

        val messageEx = InternalFigmaErrorException("Figma.com internal error")
        assertThat(messageEx).isInstanceOf(FetchException::class.java)
        assertThat(messageEx.message).isEqualTo("Figma.com internal error")
    }

    @Test
    fun testJniReflectiveConstructorLookup() {
        val exceptionClasses =
            listOf(
                FetchException::class.java,
                AccessDeniedException::class.java,
                FigmaFileNotFoundException::class.java,
                RateLimitedException::class.java,
                InternalFigmaErrorException::class.java,
            )

        for (clazz in exceptionClasses) {
            val noArgInstance = clazz.getDeclaredConstructor().newInstance()
            assertThat(noArgInstance).isInstanceOf(FetchException::class.java)
            assertThat(noArgInstance.message).isNull()

            val msgConstructor = clazz.getDeclaredConstructor(String::class.java)
            val msgInstance = msgConstructor.newInstance("jni error message")
            assertThat(msgInstance).isInstanceOf(FetchException::class.java)
            assertThat(msgInstance.message).isEqualTo("jni error message")
        }
    }
}
