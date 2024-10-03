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

import com.android.designcompose.common.DesignDocId
import com.android.designcompose.serdegen.ConvertResponse
import com.novi.bincode.BincodeDeserializer

internal object LiveUpdate {
    fun fetchDocBytes(
        docId: DesignDocId,
        requestJson: String,
        proxyConfig: ProxyConfig,
    ): ByteArray? {
        val serializedResponse: ByteArray =
            Jni.tracedJnifetchdoc(docId.id, docId.versionId, requestJson, proxyConfig)
        val deserializer = BincodeDeserializer(serializedResponse)
        val convResp = ConvertResponse.deserialize(deserializer)
        if (convResp is ConvertResponse.Document) return convResp.value.toByteArray()
        return null
    }
}
