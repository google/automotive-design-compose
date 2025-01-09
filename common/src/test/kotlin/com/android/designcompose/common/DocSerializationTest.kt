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
import kotlin.io.path.inputStream
import kotlin.io.path.toPath
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

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
}

@RunWith(Parameterized::class)
class LoadSaveLoadAllDocsTest(private val fileName: String, private val docName: String) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {1}")
        fun data(): Collection<Array<String>> {
            // Get the URL of the "figma" directory within the resources
            val resourcesUrl =
                LoadSaveLoadAllDocsTest::class.java.classLoader!!.getResource("figma")
            // Convert the URL to a File object
            val resourcesFile = resourcesUrl?.toURI()?.toPath()?.toFile()

            return resourcesFile!!
                .walk()
                .filter { it.name.endsWith(".dcf") }
                // Map to only keep the file name
                .map { it.name }
                .toList()
                // For each file name, create an array containing the full file name and the
                // document name
                // (obtained by removing "Doc" from the file name).  This array is then used as
                // input
                // for the parameterized test, with the document name used for naming the test.
                .map { arrayOf(it, it.substringBefore("Doc")) }
        }
    }

    @Test
    fun loadSaveLoad() {
        // Get an input stream for the .dcf file from the resources directory. The fileName variable
        // comes from the parameterized test data, and is the name of the .dcf file to load.
        val inputStream: InputStream =
            LoadSaveLoadAllDocsTest::class
                .java
                .classLoader!!
                .getResourceAsStream("figma/$fileName")!!
        // Decode the .dcf file from the input stream into a DesignDoc object.  Feedback is an
        // object
        // that can be used to collect feedback during decoding, which is not used here.
        val doc = decodeDiskBaseDoc(inputStream, DesignDocId(fileName), Feedback)
        assertNotNull(doc)
        // Create a temporary file to save the document to.
        val savedDoc = createTempFile()
        // Save the DesignDoc object to the temporary file.
        doc.save(savedDoc.toFile(), Feedback)
        // Load the saved document back from the temporary file into a new DesignDoc object.
        val loadedDoc =
            decodeDiskBaseDoc(savedDoc.toFile().inputStream(), DesignDocId(fileName), Feedback)
        assertNotNull(loadedDoc)
    }
}
