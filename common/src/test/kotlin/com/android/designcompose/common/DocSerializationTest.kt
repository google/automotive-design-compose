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
import kotlin.io.path.createTempFile
import kotlin.io.path.toPath
import kotlin.test.assertNotNull
import org.junit.Test

class DocSerializationTest {

    // Load the DesignSwitcherDoc from the Designcompose module
    @Test
    fun decodeDesignSwitcher() {
        val inputStream: InputStream =
            DocSerializationTest::class
                .java
                .classLoader!!
                .getResourceAsStream("figma/DesignSwitcherDoc_Ljph4e3sC0lHcynfXpoh9f.dcf")!!
        assertNotNull(inputStream)

        assertNotNull(decodeDiskBaseDoc(inputStream, DesignDocId("test_doc"), Feedback))
    }

    // Load the HelloWorldDoc from HelloWorld
    @Test
    fun decodeHelloWorld() {
        val inputStream: InputStream =
            DocSerializationTest::class
                .java
                .classLoader!!
                .getResourceAsStream("figma/HelloWorldDoc_pxVlixodJqZL95zo2RzTHl.dcf")!!
        assertNotNull(inputStream)

        assertNotNull(decodeDiskBaseDoc(inputStream, DesignDocId("test_doc"), Feedback))
    }

    @Test
    fun loadSaveLoadHelloWorld() {
        val inputStream: InputStream =
            DocSerializationTest::class
                .java
                .classLoader!!
                .getResourceAsStream("figma/HelloWorldDoc_pxVlixodJqZL95zo2RzTHl.dcf")!!
        assertNotNull(inputStream)
        val doc = decodeDiskBaseDoc(inputStream, DesignDocId("loadSaveLoad"), Feedback)
        assertNotNull(doc)
        val savedDoc = createTempFile()
        doc.save(savedDoc.toFile(), Feedback)
        val loadedDoc =
            decodeDiskBaseDoc(
                savedDoc.toFile().inputStream(),
                DesignDocId("loadSaveLoad"),
                Feedback,
            )
        assertNotNull(loadedDoc)
    }

    // Iterate through all dcf files in the figma/ directory and test them
    @Test
    fun loadSaveLoadAllDocs() {
        val resourcesUrl = DocSerializationTest::class.java.classLoader!!.getResource("figma")
        val resourcesFile = resourcesUrl?.toURI()?.toPath()?.toFile()

        resourcesFile!!
            .walkTopDown()
            .filter { it.name.endsWith(".dcf") }
            .forEach { file ->
                println("Testing ${file.name}")
                val doc = decodeDiskBaseDoc(file.inputStream(), DesignDocId(file.name), Feedback)
                assertNotNull(doc)
                val savedDoc = createTempFile()
                doc.save(savedDoc.toFile(), Feedback)
                val loadedDoc =
                    decodeDiskBaseDoc(
                        savedDoc.toFile().inputStream(),
                        DesignDocId(file.name),
                        Feedback,
                    )
                assertNotNull(loadedDoc)
            }
    }
}
