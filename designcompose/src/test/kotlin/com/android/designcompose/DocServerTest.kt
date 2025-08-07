/*
 * Copyright 2025 Google LLC
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
import com.android.designcompose.common.DocumentServerParams
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DocServerTest {

    @Before
    fun setUp() {
        DocServer.testOnlyClearDocuments()
        DocServer.subscriptions.clear()
    }

    @After
    fun tearDown() {
        DocServer.stopLiveUpdates()
    }

    @Test
    fun testSubscribe() {
        val docId = DesignDocId("doc1")
        val docSubscription = LiveDocSubscription(docId.id, docId, {}, null)
        DocServer.subscribe(docSubscription, DocumentServerParams(), null)
        assertThat(DocServer.subscriptions).containsKey(docId)
        assertThat(DocServer.subscriptions[docId]?.subscribers).contains(docSubscription)
    }

    @Test
    fun testUnsubscribe() {
        val docId = DesignDocId("doc1")
        val docSubscription = LiveDocSubscription(docId.id, docId, {}, null)
        DocServer.subscribe(docSubscription, DocumentServerParams(), null)
        DocServer.unsubscribe(docSubscription)
        assertThat(DocServer.subscriptions).doesNotContainKey(docId)
    }

    @Test
    fun testGetProxyConfig() {
        System.setProperty("http.proxyHost", "proxy.example.com")
        System.setProperty("http.proxyPort", "8080")
        val proxyConfig = DocServer.getProxyConfig()
        assertThat(proxyConfig.httpProxyConfig?.proxySpec).isEqualTo("proxy.example.com:8080")
        System.clearProperty("http.proxyHost")
        System.clearProperty("http.proxyPort")
    }
}
