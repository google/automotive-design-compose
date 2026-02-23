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
import java.io.File
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
        val docSubscription =
            LiveDocSubscription(docId.id, docId, DocumentServerParams(), null, {}, null)
        DocServer.subscribe(docSubscription)
        assertThat(DocServer.subscriptions).containsKey(docId)
        assertThat(DocServer.subscriptions[docId]?.subscribers).contains(docSubscription)
    }

    @Test
    fun testUnsubscribe() {
        val docId = DesignDocId("doc1")
        val docSubscription =
            LiveDocSubscription(docId.id, docId, DocumentServerParams(), null, {}, null)
        DocServer.subscribe(docSubscription)
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

    @Test
    fun testMergeSubscriptionParams() {
        val docId = DesignDocId("doc1")
        val queries1 = arrayListOf("query1")
        val queries2 = arrayListOf("query2")
        val ignoredImages1 = hashMapOf("node1" to arrayOf("img1"))
        val ignoredImages2 = hashMapOf("node2" to arrayOf("img2"))

        val param1 = DocumentServerParams(queries1, ignoredImages1)
        val file1 = File("file1")
        val sub1 = LiveDocSubscription(docId.id, docId, param1, file1, {}, null)

        val param2 = DocumentServerParams(queries2, ignoredImages2)
        val file2 = File("file2")
        val sub2 = LiveDocSubscription(docId.id, docId, param2, file2, {}, null)

        val subscriptions = listOf(sub1, sub2)
        val mergedParams = DocServer.mergeSubscriptionParams(subscriptions)

        assertThat(mergedParams.nodeQueries).containsExactly("query1", "query2")
        assertThat(mergedParams.ignoredImages?.keys).containsExactly("node1", "node2")
    }
}
