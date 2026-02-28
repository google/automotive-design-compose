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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.android.designcompose.common.DesignDocId
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DebugNodeManagerTest {

    // This rule swaps the background executor used by Architecture Components
    // with a different one which executes each task synchronously.
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        // This setup also benefits from the rule, ensuring setShowNodes completes instantly.
        DebugNodeManager.setShowNodes(false)
        val field = DebugNodeManager::class.java.getDeclaredField("nodeId")
        field.isAccessible = true
        field.set(DebugNodeManager, 0)
    }

    @Test
    fun testSetShowNodes() {
        DebugNodeManager.setShowNodes(true)
        assertThat(DebugNodeManager.getShowNodes().value).isTrue()
        DebugNodeManager.setShowNodes(false)
        assertThat(DebugNodeManager.getShowNodes().value).isFalse()
    }

    @Test
    fun testSetShowRecomposition() {
        DebugNodeManager.setShowRecomposition(true)
        assertThat(DebugNodeManager.getShowRecomposition().value).isTrue()
        DebugNodeManager.setShowRecomposition(false)
        assertThat(DebugNodeManager.getShowRecomposition().value).isFalse()
    }

    @Test
    fun testSetUseLocalRes() {
        DebugNodeManager.setUseLocalRes(true)
        assertThat(DebugNodeManager.getUseLocalRes().value).isTrue()
        DebugNodeManager.setUseLocalRes(false)
        assertThat(DebugNodeManager.getUseLocalRes().value).isFalse()
    }

    @Test
    fun testAddAndRemoveNode() {
        DebugNodeManager.setShowNodes(true)
        val docId = DesignDocId("testDoc")
        val node =
            DebugNodeManager.NodePosition(
                "node1",
                "#nodeName",
                Offset(1f, 1f),
                IntSize(10, 10),
                Color.Red,
            )
        val nodeId = DebugNodeManager.addNode(docId, 0, node)
        assertThat(nodeId).isNotEqualTo(0)

        DebugNodeManager.removeNode(nodeId)
        // After removing, adding the same node should result in a new ID
        val newNodeId = DebugNodeManager.addNode(docId, 0, node)
        assertThat(newNodeId).isNotEqualTo(nodeId)
        assertThat(newNodeId).isGreaterThan(0)
    }

    @Test
    fun testAddNodeWhenShowNodesIsFalse() {
        DebugNodeManager.setShowNodes(false)
        val docId = DesignDocId("testDoc")
        val node =
            DebugNodeManager.NodePosition(
                "node1",
                "#nodeName",
                Offset(1f, 1f),
                IntSize(10, 10),
                Color.Red,
            )
        val nodeId = DebugNodeManager.addNode(docId, 0, node)
        assertThat(nodeId).isEqualTo(0)
    }
}
