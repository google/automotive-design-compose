/*
 * Copyright 2026 Google LLC
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
import com.android.designcompose.definition.interaction.Action
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InteractionStateManagerTest {

    @Before
    fun setUp() {
        InteractionStateManager.states.clear()
        // Reset the active roots using manually clearing it for tests
        InteractionStateManager.activeRoots.clear()
    }

    @Test
    fun testRegisterAndUnregisterRoot() {
        val docId = DesignDocId("test_doc")

        // Request state before registering to ensure it's lazily created
        val state = InteractionStateManager.stateForDoc(docId)
        assertThat(InteractionStateManager.states).containsKey(docId)

        // Register 2 roots
        InteractionStateManager.registerRoot(docId)
        InteractionStateManager.registerRoot(docId)

        // Unregister 1 root
        InteractionStateManager.unregisterRoot(docId)

        // State should still be present since there is 1 active root left
        assertThat(InteractionStateManager.states).containsKey(docId)

        // Unregister the last root
        InteractionStateManager.unregisterRoot(docId)

        // State should be cleared
        assertThat(InteractionStateManager.states).doesNotContainKey(docId)
    }

    @Test
    fun testUnregisterBelowZeroDoesNotCrash() {
        val docId = DesignDocId("test_doc")

        // Unregister without registering
        InteractionStateManager.unregisterRoot(docId)
        assertThat(InteractionStateManager.states).doesNotContainKey(docId)
    }

    @Test
    fun testMultipleChangeToUndo() {
        val state = InteractionState()
        val undoInstanceId = "pressed_node"
        val key: String? = null

        // Target A ChangeTo
        val actionA =
            Action.newBuilder()
                .setNode(
                    Action.Node.newBuilder()
                        .setDestinationId("variant_a_new")
                        .setNavigation(Action.Node.Navigation.NAVIGATION_CHANGE_TO)
                        .build()
                )
                .build()

        // Target B ChangeTo
        val actionB =
            Action.newBuilder()
                .setNode(
                    Action.Node.newBuilder()
                        .setDestinationId("variant_b_new")
                        .setNavigation(Action.Node.Navigation.NAVIGATION_CHANGE_TO)
                        .build()
                )
                .build()

        // Set initial states
        state.variantMemory["target_a"] = "variant_a_old"
        state.variantMemory["target_b"] = "variant_b_old"

        // Dispatch A
        state.dispatch(actionA, "target_a", key, undoInstanceId)
        // Dispatch B
        state.dispatch(actionB, "target_b", key, undoInstanceId)

        // Verify states changed
        assertThat(state.variantMemory["target_a"]).isEqualTo("variant_a_new")
        assertThat(state.variantMemory["target_b"]).isEqualTo("variant_b_new")

        // Undo B
        state.undoDispatch(actionB, "target_b", undoInstanceId, key)
        // Undo A
        state.undoDispatch(actionA, "target_a", undoInstanceId, key)

        // Verify states restored
        assertThat(state.variantMemory["target_a"]).isEqualTo("variant_a_old")
        assertThat(state.variantMemory["target_b"]).isEqualTo("variant_b_old")
    }
}
