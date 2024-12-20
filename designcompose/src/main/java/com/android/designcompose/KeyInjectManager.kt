/*
 * Copyright 2024 Google LLC
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

import com.android.designcompose.annotation.DesignMetaKey
import com.android.designcompose.definition.interaction.Action

// Represents a key press event with optional meta keys. A DesignKeyEvent can be created with a
// single character representing the key and a list of meta keys. It can also be created from a
// list of javascript key codes, which is what Figma provides for an interaction with a key event
// type trigger
data class DesignKeyEvent(val key: Char, val metaKeys: List<DesignMetaKey>) {
    companion object {
        // Construct a DesignKeyEvent from a list of javascript key codes
        fun fromJsKeyCodes(jsKeyCodes: List<Byte>): DesignKeyEvent {
            var metaKeys: ArrayList<DesignMetaKey> = arrayListOf()
            var key: Char = '0'
            jsKeyCodes
                .map { it.toInt() }
                .forEach {
                    when (it) {
                        16 -> metaKeys.add(DesignMetaKey.MetaShift)
                        17 -> metaKeys.add(DesignMetaKey.MetaCtrl)
                        18 -> metaKeys.add(DesignMetaKey.MetaAlt)
                        91 -> metaKeys.add(DesignMetaKey.MetaMeta)
                        else -> key = it.toChar()
                    }
                }

            return DesignKeyEvent(key, metaKeys)
        }
    }
}

internal data class KeyAction(
    val interactionState: InteractionState,
    val action: Action,
    val targetInstanceId: String?,
    val key: String?,
    val undoInstanceId: String?,
)

// Manager to handle key event injects and listeners of key events
internal object KeyInjectManager {
    private val keyListenerMap: HashMap<DesignKeyEvent, HashSet<KeyAction>> = HashMap()
    private val keyEventTrackers: MutableList<KeyEventTracker> = mutableListOf()

    // Inject a key event and dispatch any interactions on listeners of the key event
    fun injectKey(key: Char, metaKeys: List<DesignMetaKey>) {
        val keyEvent = DesignKeyEvent(key, metaKeys)
        val listeners = keyListenerMap[keyEvent]
        listeners?.forEach {
            it.interactionState.dispatch(it.action, it.targetInstanceId, it.key, it.undoInstanceId)
        }
        keyEventTrackers.forEach { it.injectKey(key, metaKeys) }
    }

    // Add a KeyEventTracker to our global list. This should happen when a root squoosh view is
    // added.
    fun addTracker(tracker: KeyEventTracker) {
        keyEventTrackers.add(tracker)
    }

    // Remove a KeyEventTracker from our global list. This should happen when a root squoosh view
    // is removed.
    fun removeTracker(tracker: KeyEventTracker) {
        keyEventTrackers.remove(tracker)
    }

    // Register a listener for a specific key event. This happens when a view with a key event
    // trigger is composed.
    fun addListener(keyEvent: DesignKeyEvent, keyAction: KeyAction) {
        if (keyListenerMap[keyEvent].isNullOrEmpty()) keyListenerMap[keyEvent] = HashSet()
        keyListenerMap[keyEvent]?.add(keyAction)
    }

    // Remove a listener for a specific key event. This happens when a view with a key event trigger
    // is removed from composition.
    fun removeListener(keyEvent: DesignKeyEvent, keyAction: KeyAction) {
        val listeners = keyListenerMap[keyEvent]
        listeners?.remove(keyAction)
    }
}

// An object similar to KeyInjectManager, but instantiated for each squoosh root node. This keeps
// track of any node under the root that has key event interactions
internal class KeyEventTracker {
    private val keyListenerMap: HashMap<DesignKeyEvent, HashSet<KeyAction>> = HashMap()

    // Inject a key event and dispatch any interactions on listeners of the key event
    fun injectKey(key: Char, metaKeys: List<DesignMetaKey>) {
        val keyEvent = DesignKeyEvent(key, metaKeys)
        val listeners = keyListenerMap[keyEvent]
        listeners?.forEach {
            it.interactionState.dispatch(it.action, it.targetInstanceId, it.key, it.undoInstanceId)
        }
    }

    // Register a listener for a specific key event. This happens when a view with a key event
    // trigger is composed.
    fun addListener(keyEvent: DesignKeyEvent, keyAction: KeyAction) {
        if (keyListenerMap[keyEvent].isNullOrEmpty()) keyListenerMap[keyEvent] = HashSet()
        keyListenerMap[keyEvent]?.add(keyAction)
    }

    fun clearListeners() {
        keyListenerMap.clear()
    }
}

// Public function to inject a key event
fun DesignInjectKey(key: Char, metaKeys: List<DesignMetaKey>) {
    KeyInjectManager.injectKey(key, metaKeys)
}
