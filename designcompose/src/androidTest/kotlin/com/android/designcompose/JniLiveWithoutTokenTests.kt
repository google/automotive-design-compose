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
import kotlin.test.assertFailsWith
import org.junit.Test

val dummyFigmaTokenJson = constructPostJson("NOT_A_FIGMA_TOKEN", null, DocumentServerParams())

class JniLiveWithoutTokenTests {
    /**
     * Invalid key test
     *
     * Tests that a fetch request using an invalid Figma API Key returns the proper failure
     */
    @Test
    fun invalidKey() {
        assertFailsWith(AccessDeniedException::class) {
            LiveUpdateJni.jniFetchDoc("DummyDocId", dummyFigmaTokenJson)
        }
    }
}
