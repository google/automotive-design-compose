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

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.MutableState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Paragraph
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DocContent
import com.android.designcompose.InteractionState
import com.android.designcompose.definition.interaction.Action
import com.android.designcompose.definition.modifier.TextOverflow
import com.android.designcompose.dispatch
import com.android.designcompose.getKey
import com.android.designcompose.getPressedReactionList
import com.android.designcompose.getPressedTapCallback
import com.android.designcompose.getTapCallback
import com.android.designcompose.removePressed
import com.android.designcompose.setPressed
import com.android.designcompose.undoDispatch
import kotlinx.coroutines.CoroutineScope

internal fun findTargetInstanceId(
    document: DocContent,
    parentComponents: ParentComponentData?,
    action: Action,
): String? {
    val destinationId: String? =
        if (action.actionTypeCase == Action.ActionTypeCase.NODE) action.node.destinationId else null

    val componentSetId = document.c.document.componentSetsMap[destinationId] ?: return null

    // Look up our list of parent components and try to find one that is a member of
    // this component set.
    var currentParent = parentComponents
    while (currentParent != null) {
        if (
            componentSetId == document.c.document.componentSetsMap[currentParent.componentInfo.id]
        ) {
            return currentParent.instanceId
        }

        currentParent = currentParent.parent
    }

    return null
}

internal fun Modifier.hyperlinkHandler(
    node: SquooshResolvedNode,
    uriHandler: UriHandler,
): Modifier {
    return this.then(
            Modifier.pointerInput(node.textInfo, node.computedLayout) {
                if (node.textInfo == null) return@pointerInput
                if (node.computedLayout == null) return@pointerInput
                val paragraph =
                    Paragraph(
                        paragraphIntrinsics = node.textInfo.paragraph,
                        width = node.computedLayout!!.width * density,
                        maxLines = node.textInfo.maxLines,
                        ellipsis =
                            node.view.style.nodeStyle.textOverflow ==
                                TextOverflow.TEXT_OVERFLOW_ELLIPSIS,
                    )
                detectTapGestures(
                    onPress = {
                        val offset = paragraph.getOffsetForPosition(Offset(it.x, it.y))
                        val sortedMap = node.textInfo.hyperlinkOffsetMap
                        val iterator = sortedMap.iterator()
                        var lastKey = iterator.next().key
                        var key: Int? = null
                        while (iterator.hasNext()) {
                            key = iterator.next().key
                            if (offset in lastKey..<key && sortedMap[lastKey] != null) {
                                val dispatchClickEvent = tryAwaitRelease()
                                if (dispatchClickEvent) {
                                    uriHandler.openUri(sortedMap[lastKey]!!)
                                    return@detectTapGestures
                                }
                            }
                            lastKey = key
                        }
                        if (key == null && sortedMap[lastKey] != null) {
                            val dispatchClickEvent = tryAwaitRelease()
                            if (dispatchClickEvent) {
                                uriHandler.openUri(sortedMap[lastKey]!!)
                            }
                        }
                    }
                )
            }
        )
        .semantics {
            this.contentDescription =
                if (node.view.data.hasText()) node.view.data.text.content
                else {
                    node.view.data.styledText.styledTextsList.joinToString(separator = "") {
                        it.text
                    }
                }
        }
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.squooshInteraction(
    document: DocContent,
    interactionState: InteractionState,
    interactionScope: CoroutineScope,
    customizations: CustomizationContext,
    childComposable: SquooshChildComposable,
    isPressed: MutableState<Boolean>,
): Modifier {
    val node = childComposable.node
    val reactions = node.view.reactionsList
    var tapCallback = customizations.getTapCallback(node.view)

    return this.then(
        Modifier.pointerInput(reactions, tapCallback) {
            awaitEachGesture {
                var tapPosition: Offset? = null
                if (!isPressed.value) {
                    // If not currently pressed, wait for the first press (down) event
                    val down = awaitFirstDown(true, PointerEventPass.Initial)
                    tapPosition = down.position
                }

                do {
                    val event = awaitPointerEvent()
                    if (event.changes.size != 1) {
                        // Too many changes, not a tap so abort
                        break
                    }

                    val change = event.changes[0]
                    if (change.pressed && !change.previousPressed) {
                        // Press event, dispatch any press events
                        reactions
                            ?.filter { r -> r.trigger.hasPress() }
                            ?.forEach {
                                interactionState.dispatch(
                                    it.action,
                                    findTargetInstanceId(
                                        document,
                                        childComposable.parentComponents,
                                        it.action,
                                    ),
                                    customizations.getKey(),
                                    node.unresolvedNodeId,
                                )
                            }
                        // Set the isPressed state so that if this node changed, the new variant
                        // starts out in the pressed state. Also save the original node's reactions
                        // into the interaction state so that the new variant can access them
                        isPressed.value = true
                        interactionState.setPressed(node.unresolvedNodeId, reactions, tapCallback)
                    } else if (change.pressed) {
                        // Drag event. If there's too much movement, abort the tap
                        if (
                            tapPosition != null &&
                                (change.position - tapPosition).getDistance() >
                                    viewConfiguration.touchSlop
                        ) {
                            isPressed.value = false
                            interactionState.removePressed(node.unresolvedNodeId)
                            break
                        }
                    } else if (change.previousPressed) {
                        // Touch release event.
                        // Get the list of reactions and if any are press events, undo them.
                        val unresolvedReactions =
                            interactionState.getPressedReactionList(node.unresolvedNodeId)
                        unresolvedReactions
                            .filter { r -> r.trigger.hasPress() }
                            .forEach {
                                interactionState.undoDispatch(
                                    it.action,
                                    findTargetInstanceId(
                                        document,
                                        childComposable.parentComponents,
                                        it.action,
                                    ),
                                    node.unresolvedNodeId,
                                    customizations.getKey(),
                                )
                            }

                        // Dispatch any click interactions
                        unresolvedReactions
                            .filter { r -> r.trigger.hasClick() }
                            .forEach {
                                interactionState.dispatch(
                                    it.action,
                                    findTargetInstanceId(
                                        document,
                                        childComposable.parentComponents,
                                        it.action,
                                    ),
                                    customizations.getKey(),
                                    null, // no undo
                                )
                            }

                        // Execute tap callback if one exists
                        if (tapCallback == null)
                            tapCallback =
                                interactionState.getPressedTapCallback(node.unresolvedNodeId)
                        tapCallback?.invoke()

                        // Reset the pressed state and consume the event
                        isPressed.value = false
                        interactionState.removePressed(node.unresolvedNodeId)
                        change.consume()
                    }
                } while (event.changes.any { it.pressed })
            }
        }
    )
}
