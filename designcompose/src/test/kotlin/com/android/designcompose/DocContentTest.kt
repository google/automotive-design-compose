/*
 * Copyright 2025 Google LLC
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

import com.android.designcompose.common.DesignDocId
import com.android.designcompose.common.FeedbackImpl
import com.android.designcompose.common.FeedbackLevel
import com.android.designcompose.common.GenericDocContent
import com.android.designcompose.common.VariantPropertyMap
import com.android.designcompose.definition.DesignComposeDefinition
import com.android.designcompose.definition.DesignComposeDefinitionHeader
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import java.io.ByteArrayInputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DocContentTest {
    @Test
    fun testImage() {
        val c =
            GenericDocContent(
                docId = DesignDocId(""),
                header = DesignComposeDefinitionHeader.newBuilder().build(),
                document = DesignComposeDefinition.newBuilder().build(),
                variantViewMap = HashMap(),
                variantPropertyMap = VariantPropertyMap(),
                nodeIdMap = HashMap(),
                imageSession = ByteString.EMPTY,
            )
        val docContent = DocContent(c, null)
        assertThat(docContent.image("key", 1.0f)).isNull()
    }

    @Test
    fun testDecodeDiskDoc() {
        val docId = DesignDocId("TestDoc")
        val feedback =
            object : FeedbackImpl() {
                override fun logMessage(str: String, level: FeedbackLevel) {}
            }
        val docStream = ByteArrayInputStream(ByteArray(0))
        val docContent = decodeDiskDoc(docStream, null, docId, feedback)
        assertThat(docContent).isNull()
    }
}
