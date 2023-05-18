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

import com.android.designcompose.common.DocumentServerParams
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertFailsWith

/**
 * Jni fetch tests
 *
 * These tests use the JNI Library and will reach out to Figma.com itself.
 */
class JniFetchTests {

    /**
     * Invalid key test
     *
     * Tests that a fetch request using an invalid Figma API Key returns the proper failure
     */
    private val requestJson = constructPostJson("NOT_A_FIG_KEY", null, DocumentServerParams())
    @Test
    fun invalidKey() {
       assertFailsWith(AccessDeniedException::class){
           LiveUpdateJni.jniFetchDoc("DummyDocId", requestJson)
       }
    }
}
