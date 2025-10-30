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

import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
import com.android.designcompose.getOnProgressChangedCallback
import com.android.designcompose.getPressedReactionList
import com.android.designcompose.getPressedTapCallback
import com.android.designcompose.getTapCallback
import com.android.designcompose.removePressed
import com.android.designcompose.setMeterState
import com.android.designcompose.setPressed
import com.android.designcompose.undoDispatch
import com.android.designcompose.utils.getProgressBarData
import com.android.designcompose.utils.getProgressMarkerData
import kotlinx.coroutines.CoroutineScope
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

internal fun Modifier.progressBarSlider(
    node: SquooshResolvedNode,
    interactionScope: CoroutineScope,
    interactionSource: MutableInteractionSource,
    customizations: CustomizationContext,
    meterState: MutableState<Float?>,
): Modifier {
    return this.pointerInput(interactionSource) {
        fun onOffsetChange(offset: Offset) {
            val pbNode = node.findProgressBarChild()
            val pbMeterData = pbNode?.style?.getProgressBarData()
            pbMeterData?.let {
                val progressChanged = customizations.getOnProgressChangedCallback(pbNode.view.name)
                val progress: Float
                if (it.vertical) {
                    val startY = it.startY * density
                    progress =
                        ((size.height - offset.y + startY / 2).coerceAtLeast(0f) /
                            (size.height - startY) * 100f)
                } else {
                    val startX = it.startX * density
                    progress =
                        ((offset.x - startX / 2).coerceAtLeast(0f) / (size.width - startX) * 100f)
                            .coerceAtMost(100f)
                }
                progressChanged?.onProgressChanged(progress)
                meterState.value = progress
                customizations.setMeterState(pbNode.view.name, meterState)
            }

            val pmNode = node.findProgressMarkerDescendant()
            val pmMeterData = pmNode?.style?.getProgressMarkerData()
            pmMeterData?.let {
                val progressChanged = customizations.getOnProgressChangedCallback(pmNode.view.name)
                val progress: Float
                if (it.vertical) {
                    val startY = it.startY * density
                    progress =
                        ((size.height - offset.y + startY / 2).coerceAtLeast(0f) /
                            (size.height - startY) * 100f)
                } else {
                    val startX = it.startX * density
                    progress =
                        ((offset.x - startX / 2).coerceAtLeast(0f) / (size.width - startX) * 100f)
                            .coerceAtMost(100f)
                }
                progressChanged?.onProgressChanged(progress)
                meterState.value = progress
                customizations.setMeterState(pmNode.view.name, meterState)
            }
        }

        var offset: Offset
        var interaction: Interaction? = null
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = true)
            offset = down.position
            interactionScope.launch {
                interaction = PressInteraction.Press(down.position)
                interactionSource.emit(interaction as PressInteraction.Press)
            }
            onOffsetChange(offset)
            var change =
                awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
                    val original = offset
                    val summed = original + overSlop
                    val newValue =
                        Offset(
                            x = summed.x.coerceIn(0f, size.width.toFloat()),
                            y = summed.y.coerceIn(0f, size.height.toFloat()),
                        )
                    change.consume()
                    offset = Offset(newValue.x, newValue.y)
                    onOffsetChange(offset)

                    interactionScope.launch {
                        interaction = DragInteraction.Start()
                        interactionSource.emit(interaction as DragInteraction.Start)
                    }
                }
            while (change != null && change.pressed) {
                change = awaitDragOrCancellation(change.id)
                if (change != null && change.pressed) {
                    val original = offset
                    val summed = original + change.positionChange()
                    val newValue =
                        Offset(
                            x = summed.x.coerceIn(0f, size.width.toFloat()),
                            y = summed.y.coerceIn(0f, size.height.toFloat()),
                        )
                    change.consume()
                    offset = Offset(newValue.x, newValue.y)
                    onOffsetChange(offset)
                }
            }

            if (change == null) {
                if (interaction is PressInteraction.Press) {
                    val pressInteraction = interaction as PressInteraction.Press
                    interactionScope.launch {
                        interactionSource.emit(PressInteraction.Cancel(pressInteraction))
                    }
                } else if (interaction is DragInteraction.Start) {
                    val dragStart = interaction as DragInteraction.Start
                    interactionScope.launch {
                        interactionSource.emit(DragInteraction.Cancel(dragStart))
                    }
                }
            } else {
                if (interaction is PressInteraction.Press) {
                    val pressInteraction = interaction as PressInteraction.Press
                    interactionScope.launch {
                        interactionSource.emit(PressInteraction.Release(pressInteraction))
                    }
                } else if (interaction is DragInteraction.Start) {
                    val dragStart = interaction as DragInteraction.Start
                    interactionScope.launch {
                        interactionSource.emit(DragInteraction.Stop(dragStart))
                    }
                }
            }
        }
    }
}

internal fun Modifier.hyperlinkHandler(
    node: SquooshResolvedNode,
    uriHandler: UriHandler,
): Modifier {
    return this.pointerInput(node.textInfo, node.computedLayout) {
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
    interactionSource: MutableInteractionSource,
): Modifier {
    // We wrap the logic in Modifier.composed. This gives us a @Composable context.
    return this.then(
        Modifier.composed {
            val node = childComposable.node
            val viewName = node.view.name

            // Because we are now in a @Composable context, these calls are valid.
            val latestReactions by rememberUpdatedState(node.view.reactionsList)
            val latestTapCallback by rememberUpdatedState(customizations.getTapCallback(node.view))
            val latestParentComponents by rememberUpdatedState(childComposable.parentComponents)

            // The pointerInput modifier is now created inside this composable scope.
            // We pass Unit as the key to ensure it doesn't restart on recomposition.
            Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        var pressInteraction: PressInteraction.Press? = null

                        try {
                            // 1. Show the "Pressed" state immediately.
                            pressInteraction = PressInteraction.Press(it)
                            interactionScope.launch { interactionSource.emit(pressInteraction!!) }
                            isPressed.value = true
                            interactionState.setPressed(
                                node.unresolvedNodeId,
                                latestReactions,
                                latestTapCallback,
                            )

                            // Dispatch any "on press" reactions...
                            latestReactions
                                ?.filter { r -> r.trigger.hasPress() }
                                ?.forEach { reaction ->
                                    interactionState.dispatch(
                                        reaction.action,
                                        findTargetInstanceId(
                                            document,
                                            latestParentComponents,
                                            reaction.action,
                                        ),
                                        customizations.getKey(),
                                        node.unresolvedNodeId,
                                    )
                                }

                            // 2. Wait for the press to end.
                            val success = tryAwaitRelease()

                            if (success) {
                                // This was a tap. Do the tap actions.
                                val currentReactions = latestReactions
                                val clickReactions =
                                    currentReactions?.filter { r -> r.trigger.hasClick() }
                                        ?: emptyList()

                                clickReactions.forEach { reaction ->
                                    interactionState.dispatch(
                                        reaction.action,
                                        findTargetInstanceId(
                                            document,
                                            latestParentComponents,
                                            reaction.action,
                                        ),
                                        customizations.getKey(),
                                        null, // no undo
                                    )
                                }

                                val finalTapCallback =
                                    latestTapCallback
                                        ?: interactionState.getPressedTapCallback(
                                            node.unresolvedNodeId
                                        )
                                finalTapCallback?.invoke()
                            }

                            // 3. Clean up the "Pressed" visual state.
                            interactionScope.launch {
                                val endInteraction =
                                    if (success) {
                                        PressInteraction.Release(pressInteraction!!)
                                    } else {
                                        PressInteraction.Cancel(pressInteraction!!)
                                    }
                                interactionSource.emit(endInteraction)
                            }

                            // Undo the "on press" reactions.
                            interactionState
                                .getPressedReactionList(node.unresolvedNodeId)
                                .filter { r -> r.trigger.hasPress() }
                                .forEach { reaction ->
                                    interactionState.undoDispatch(
                                        reaction.action,
                                        findTargetInstanceId(
                                            document,
                                            latestParentComponents,
                                            reaction.action,
                                        ),
                                        node.unresolvedNodeId,
                                        customizations.getKey(),
                                    )
                                }
                        } finally {
                            // 4. Ensure isPressed boolean is always reset.
                            isPressed.value = false
                            interactionState.removePressed(node.unresolvedNodeId)
                        }
                    }
                )
            }
        }
    )
}
