/*
 * Copyright 2024 Google LLC
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

import java.io.InputStream
import kotlin.test.assertNotNull
import org.junit.Test

class GenericDocContentKtTest {

    @Test
    fun decodeDesignSwitcher() {

        // Load the DesignSwitcherDoc from the Designcompose module
        val inputStream: InputStream =
            GenericDocContentKtTest::class
                .java
                .classLoader!!
                .getResourceAsStream("figma/DesignSwitcherDoc_Ljph4e3sC0lHcynfXpoh9f.dcf")!!
        assertNotNull(inputStream)

        assertNotNull(decodeDiskBaseDoc(inputStream, DesignDocId("test_doc"), Feedback))
    }

    @Test
    fun decodeHelloWorld() {

        // Load the HelloWorldDoc from HelloWorld
        val inputStream: InputStream =
            GenericDocContentKtTest::class
                .java
                .classLoader!!
                .getResourceAsStream("figma/HelloWorldDoc_pxVlixodJqZL95zo2RzTHl.dcf")!!
        assertNotNull(inputStream)

        assertNotNull(decodeDiskBaseDoc(inputStream, DesignDocId("test_doc"), Feedback))
    }

    @Test fun loadSaveLoad() {
        val inputStream: InputStream =
            GenericDocContentKtTest::class
                .java
                .classLoader!!
                .getResourceAsStream("figma/HelloWorldDoc_pxVlixodJqZL95zo2RzTHl.dcf")!!
        assertNotNull(inputStream)
        val doc = decodeDiskBaseDoc(inputStream, DesignDocId("loadSaveLoad"), Feedback)
        assertNotNull(doc)
        val savedDoc = kotlin.io.path.createTempFile()
        doc.save(savedDoc.toFile(), Feedback)
        val loadedDoc = decodeDiskBaseDoc(savedDoc.toFile().inputStream(), DesignDocId("loadSaveLoad"), Feedback)
        assertNotNull(loadedDoc)
    }
}
