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

import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import androidx.tracing.trace

// HTTP Proxy configuration.
internal data class HttpProxyConfig(val proxySpec: String)

// Proxy configuration.
//
// Only HTTP proxy supported.
internal class ProxyConfig {
    var httpProxyConfig: HttpProxyConfig? = null
}

@Keep
internal class TextSize(
    var width: Float = 0F,
    var height: Float = 0F,
)

@Keep
internal object Jni {

    fun tracedJnifetchdoc(docId: String, requestJson: String, proxyConfig: ProxyConfig): ByteArray {
        lateinit var result: ByteArray
        trace(DCTraces.JNIFETCHDOC) { result = jniFetchDoc(docId, requestJson, proxyConfig) }
        return result
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    external fun jniFetchDoc(
        docId: String,
        requestJson: String,
        proxyConfig: ProxyConfig
    ): ByteArray

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    external fun jniCreateLayoutManager(): Int

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    external fun jniSetNodeSize(
        managerId: Int,
        layoutId: Int,
        rootLayoutId: Int,
        width: Int,
        height: Int
    ): ByteArray?

    fun tracedJniAddNodes(
        managerId: Int,
        rootLayoutId: Int,
        serializedNodes: ByteArray
    ): ByteArray? {
        var result: ByteArray? = null
        trace(DCTraces.JNIADDNODES) {
            result = jniAddNodes(managerId, rootLayoutId, serializedNodes)
        }
        return result
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    external fun jniAddNodes(
        managerId: Int,
        rootLayoutId: Int,
        serializedNodes: ByteArray,
    ): ByteArray?

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    external fun jniRemoveNode(
        managerId: Int,
        layoutId: Int,
        rootLayoutId: Int,
        computeLayout: Boolean
    ): ByteArray?

    init {
        System.loadLibrary("jni")
    }
}
