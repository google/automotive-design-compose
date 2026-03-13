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
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DocumentServerParamsTest {

    @Test
    fun testDefaultValues() {
        val params = DocumentServerParams()
        assertNull(params.nodeQueries)
        assertNull(params.ignoredImages)
    }

    @Test
    fun testWithValues() {
        val nodeQueries = arrayListOf("query1", "query2")
        val ignoredImages = hashMapOf("image1" to arrayOf("frame1", "frame2"))
        val params = DocumentServerParams(nodeQueries, ignoredImages)
        assertEquals(nodeQueries, params.nodeQueries)
        assertEquals(ignoredImages, params.ignoredImages)
    }
}
