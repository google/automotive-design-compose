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
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.Paragraph
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DocContent
import com.android.designcompose.InteractionState
import com.android.designcompose.definition.interaction.Action
import com.android.designcompose.definition.modifier.TextOverflow
import com.android.designcompose.dispatch
import com.android.designcompose.getKey
import com.android.designcompose.getTapCallback
import com.android.designcompose.setPressed
import com.android.designcompose.undoDispatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

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
    val tapCallback = customizations.getTapCallback(node.view)

    return this.then(
        Modifier.pointerInput(reactions, tapCallback, isPressed.value) {
            // Use the interaction scope so that we don't have our event handler removed when our
            // Modifier node is removed from the tree (allowing interactions like "close the overlay
            // while pressed" applied to an overlay to actually receive the touch release event and
            // dispatch the "undo" action).
            interactionScope.launch(start = CoroutineStart.UNDISPATCHED) {
                detectTapGestures(
                    onPress = {
                        // Set the "pressed" state.
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
                        isPressed.value = true
                        interactionState.setPressed(node.view.id, true)
                        val dispatchClickEvent = tryAwaitRelease()

                        // Clear the "pressed" state.
                        reactions
                            ?.filter { r -> r.trigger.hasPress() }
                            ?.forEach {
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

                        // If the tap wasn't cancelled (turned into a drag, a window opened on top
                        // of us, etc) then we can run the action.
                        if (dispatchClickEvent) {
                            reactions
                                ?.filter { r -> r.trigger.hasClick() }
                                ?.forEach {
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
                            tapCallback?.invoke()
                        }
                        isPressed.value = false
                        interactionState.setPressed(node.view.id, false)
                    }
                )
            }
        }
    )
}
