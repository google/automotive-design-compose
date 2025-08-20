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

import com.android.designcompose.common.NodeQuery
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InteractionStateTest {

    @Test
    fun deferredActionNavigateBack() {
        val interactionState = InteractionState()
        interactionState.navigationHistory.add(NodeQuery.NodeId("node1"))
        val overlayMemory = arrayListOf("overlay1")
        interactionState.overlayMemory = overlayMemory

        val deferredAction = DeferredAction.NavigateBack(emptyList())
        deferredAction.apply(interactionState, null, null)

        assertEquals(0, interactionState.navigationHistory.size)
        assertEquals(0, interactionState.overlayMemory.size)
    }

    @Test
    fun deferredActionSwapNavigation() {
        val interactionState = InteractionState()
        interactionState.navigationHistory.add(NodeQuery.NodeId("node1"))
        val nodeQuery = NodeQuery.NodeId("node2")

        val deferredAction = DeferredAction.SwapNavigation(nodeQuery)
        deferredAction.apply(interactionState, null, null)

        assertEquals(1, interactionState.navigationHistory.size)
        assertEquals(nodeQuery, interactionState.navigationHistory[0])
    }
}
