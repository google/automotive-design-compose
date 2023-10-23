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

package com.android.designcompose.squoosh

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DocContent
import com.android.designcompose.InteractionState
import com.android.designcompose.dispatch
import com.android.designcompose.getKey
import com.android.designcompose.serdegen.Action
import com.android.designcompose.serdegen.Trigger
import com.android.designcompose.undoDispatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

private fun findTargetInstanceId(
    document: DocContent,
    parentComponents: ParentComponentData?,
    action: Action
): String? {
    val destinationId =
        when (action) {
            is Action.Node -> action.destination_id.orElse(null)
            else -> null
        } ?: return null

    val componentSetId = document.c.document.component_sets[destinationId] ?: return null

    // Look up our list of parent components and try to find one that is a member of
    // this component set.
    var currentParent = parentComponents
    while (currentParent != null) {
        if (componentSetId == document.c.document.component_sets[currentParent.componentInfo.id]) {
            return currentParent.instanceId
        }

        currentParent = currentParent.parent
    }

    return null
}

internal fun Modifier.squooshInteraction(
    document: DocContent,
    interactionState: InteractionState,
    interactionScope: CoroutineScope,
    customizations: CustomizationContext,
    childComposable: SquooshChildComposable
): Modifier {
    val node = childComposable.node
    val maybeReactions = node.view.reactions
    if (!maybeReactions.isPresent) return this
    val reactions = maybeReactions.get()

    return this.then(
        Modifier.pointerInput(reactions) {
            // Use the interaction scope so that we don't have our event handler removed when our
            // Modifier node is removed from the tree (allowing interactions like "close the overlay
            // while pressed" applied to an overlay to actually receive the touch release event and
            // dispatch the "undo" action).
            interactionScope.launch(start = CoroutineStart.UNDISPATCHED) {
                detectTapGestures(
                    onPress = {
                        // Set the "pressed" state.
                        for (r in reactions.filter { r -> r.trigger is Trigger.OnPress }) {
                            interactionState.dispatch(
                                r.action,
                                findTargetInstanceId(
                                    document,
                                    childComposable.parentComponents,
                                    r.action
                                ),
                                customizations.getKey(),
                                node.unresolvedNodeId
                            )
                        }
                        val dispatchClickEvent = tryAwaitRelease()

                        // Clear the "pressed" state.
                        for (r in reactions.filter { r -> r.trigger is Trigger.OnPress }) {
                            interactionState.undoDispatch(
                                findTargetInstanceId(
                                    document,
                                    childComposable.parentComponents,
                                    r.action
                                ),
                                node.unresolvedNodeId,
                                customizations.getKey()
                            )
                        }

                        // If the tap wasn't cancelled (turned into a drag, a window opened on top
                        // of
                        // us, etc) then we can run the action.
                        if (dispatchClickEvent) {
                            for (r in reactions.filter { r -> r.trigger is Trigger.OnClick }) {
                                interactionState.dispatch(
                                    r.action,
                                    findTargetInstanceId(
                                        document,
                                        childComposable.parentComponents,
                                        r.action
                                    ),
                                    customizations.getKey(),
                                    null // no undo
                                )
                            }
                        }
                    }
                )
            }
        }
    )
}
