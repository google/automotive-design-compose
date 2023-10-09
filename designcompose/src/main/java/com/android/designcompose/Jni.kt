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

internal object Jni {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    external fun jniFetchDoc(
        docId: String,
        requestJson: String,
        proxyConfig: ProxyConfig
    ): ByteArray

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    external fun jniGetLayout(layoutId: Int): ByteArray?

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    external fun jniSetNodeSize(layoutId: Int, width: Int, height: Int): ByteArray?

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    external fun jniAddNode(
        layoutId: Int,
        parentLayoutId: Int,
        childIndex: Int,
        serializedView: ByteArray,
        serializedVariantView: ByteArray,
        computeLayout: Boolean
    ): ByteArray?

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    external fun jniAddTextNode(
        layoutId: Int,
        parentLayoutId: Int,
        childIndex: Int,
        serializedView: ByteArray,
        computeLayout: Boolean
    ): ByteArray?

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    external fun jniRemoveNode(layoutId: Int, computeLayout: Boolean): ByteArray?

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    external fun jniComputeLayout(): ByteArray?

    init {
        System.loadLibrary("jni")
    }
}
