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

package com.android.designcompose

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.common.DocumentServerParams
import com.android.designcompose.common.FeedbackLevel
import com.android.designcompose.common.NodeQuery
import java.time.Instant
import kotlin.collections.HashMap
import kotlinx.coroutines.delay

private interface DesignSwitcher {
    private fun queries(): ArrayList<String> {
        return arrayListOf(
            "#SettingsView",
            "#FigmaDoc",
            "#Message",
            "#MessageFailed",
            "#LoadingSpinner",
            "#Checkbox",
            "#NodeNamesCheckbox",
            "#MiniMessagesCheckbox",
            "#ShowRecompositionCheckbox",
            "#UseLocalResCheckbox",
            "#DesignViewMain",
            "#LiveMode",
            "#TopStatusBar",
        )
    }

    private fun nodeCustomizations(): Array<String> {
        return arrayOf(
            "#MiniViewMessage",
            "#CurrentlyLoaded",
            "#ChangingCurrent",
            "#CurrentFileName",
            "#ModDate",
            "#BranchFileCount",
            "#ProjectFileCount",
            "#BranchList",
            "#BranchSection",
            "#ProjectFileList",
            "#StatusMessageList",
            "#DocIdTextEdit",
            "#HelpText",
            "#GoButton",
            "#NodeNamesCheckbox",
            "#MiniMessagesCheckbox",
            "#ShowRecompositionCheckbox",
            "#UseLocalResCheckbox",
            "#Name",
            "#Id",
            "#Text",
            "#TimeStamp",
            "#LiveMode",
            "#TopStatusBar",
        )
    }

    private fun ignoredImages(): HashMap<String, Array<String>> {
        return hashMapOf(
            "#SettingsView" to
                arrayOf(
                    "#BranchList",
                    "#ProjectFileList",
                    "#StatusMessageList",
                    "#DocIdTextEdit",
                    "#Checkbox",
                    "#NodeNamesCheckbox",
                    "#MiniMessagesCheckbox",
                    "#ShowRecompositionCheckbox",
                    "#UseLocalResCheckbox",
                )
        )
    }

    enum class LiveMode {
        Live,
        Offline,
    }

    enum class TopStatusBar {
        Default,
        Loading,
        Loaded,
    }

    @Composable
    fun CustomComponent(
        modifier: Modifier,
        nodeName: String,
        rootNodeQuery: NodeQuery,
        parentComponents: List<ParentComponentInfo>,
        tapCallback: TapCallback?,
    ) {
        val customizations = remember { CustomizationContext() }
        if (tapCallback != null) customizations.setTapCallback(nodeName, tapCallback)
        customizations.mergeFrom(LocalCustomizationContext.current)
        val (docId, setDocId) = remember { mutableStateOf(designSwitcherDocId()) }
        val queries = queries()
        queries.add(nodeName)
        CompositionLocalProvider(LocalCustomizationContext provides customizations) {
            DesignDocInternal(
                designSwitcherDocName(),
                docId,
                rootNodeQuery = rootNodeQuery,
                customizations = customizations,
                modifier = modifier,
                serverParams = DocumentServerParams(queries, ignoredImages()),
                setDocId = setDocId,
                designSwitcherPolicy = DesignSwitcherPolicy.SHOW_IF_ROOT,
                parentComponents = parentComponents,
                liveUpdateMode = getLiveMode(),
            )
        }
    }

    @Composable
    fun SettingsView(
        modifier: Modifier,
        mini_view_message: String,
        current_doc_name: String,
        last_mod_time: String,
        branch_file_count: String,
        project_file_count: String,
        branch_list: ReplacementContent,
        show_branch_section: Boolean,
        project_file_list: ReplacementContent,
        status_message_list: ReplacementContent,
        doc_text_edit: @Composable ((ComponentReplacementContext) -> Unit)?,
        show_help_text: Boolean,
        on_tap_go: TapCallback,
        node_names_checkbox: ReplacementContent,
        mini_messages_checkbox: ReplacementContent,
        show_recomposition_checkbox: ReplacementContent,
        useLocalResCheckbox: ReplacementContent,
        live_mode: LiveMode,
        top_status_bar: TopStatusBar,
    ) {
        val customizations = CustomizationContext()
        customizations.setText("#MiniViewMessage", mini_view_message)
        customizations.setText("#CurrentFileName", current_doc_name)
        customizations.setText("#ModDate", last_mod_time)
        customizations.setText("#BranchFileCount", branch_file_count)
        customizations.setText("#ProjectFileCount", project_file_count)
        customizations.setContent("#BranchList", branch_list)
        customizations.setVisible("#BranchSection", show_branch_section)
        customizations.setContent("#ProjectFileList", project_file_list)
        customizations.setContent("#StatusMessageList", status_message_list)
        customizations.setComponent("#DocIdTextEdit", doc_text_edit)
        customizations.setVisible("#HelpText", show_help_text)
        customizations.setTapCallback("#GoButton", on_tap_go)
        customizations.setContent("#NodeNamesCheckbox", node_names_checkbox)
        customizations.setContent("#MiniMessagesCheckbox", mini_messages_checkbox)
        customizations.setContent("#ShowRecompositionCheckbox", show_recomposition_checkbox)
        customizations.setContent("#UseLocalResCheckbox", useLocalResCheckbox)

        val variantProperties = HashMap<String, String>()
        variantProperties["#LiveMode"] = live_mode.name
        variantProperties["#TopStatusBar"] = top_status_bar.name
        customizations.setVariantProperties(variantProperties)
        customizations.setCustomComposable {
            mod,
            nodeName,
            rootNodeQuery,
            parentComponents,
            tapCallback ->
            CustomComponent(mod, nodeName, rootNodeQuery, parentComponents, tapCallback)
        }

        CompositionLocalProvider(LocalCustomizationContext provides customizations) {
            DesignDocInternal(
                designSwitcherDocName(),
                designSwitcherDocId(),
                rootNodeQuery = NodeQuery.NodeName("#SettingsView"),
                customizations = customizations,
                modifier = modifier.semantics { sDocClass = DesignSwitcherDoc.javaClass.name },
                serverParams = DocumentServerParams(queries(), ignoredImages()),
                designSwitcherPolicy = DesignSwitcherPolicy.IS_DESIGN_SWITCHER,
                liveUpdateMode = getLiveMode(),
            )
        }
    }

    @Composable
    fun FigmaDoc(name: String, docId: String, tapCallback: TapCallback) {
        val customizations = CustomizationContext()
        customizations.setText("#Name", name)
        customizations.setText("#Id", docId)
        customizations.setTapCallback("#GoButton", tapCallback)

        CompositionLocalProvider(LocalCustomizationContext provides customizations) {
            DesignDocInternal(
                designSwitcherDocName(),
                designSwitcherDocId(),
                rootNodeQuery = NodeQuery.NodeName("#FigmaDoc"),
                customizations = customizations,
                serverParams = DocumentServerParams(queries(), ignoredImages()),
                liveUpdateMode = getLiveMode(),
            )
        }
    }

    @Composable
    fun Message(text: String, timestamp: String) {
        val customizations = CustomizationContext()
        customizations.setText("#Text", text)
        customizations.setText("#TimeStamp", timestamp)

        CompositionLocalProvider(LocalCustomizationContext provides customizations) {
            DesignDocInternal(
                designSwitcherDocName(),
                designSwitcherDocId(),
                rootNodeQuery = NodeQuery.NodeName("#Message"),
                customizations = customizations,
                serverParams = DocumentServerParams(queries(), ignoredImages()),
                liveUpdateMode = getLiveMode(),
            )
        }
    }

    @Composable
    fun MessageFailed(text: String, timestamp: String) {
        val customizations = CustomizationContext()
        customizations.setText("#Text", text)
        customizations.setText("#TimeStamp", timestamp)

        CompositionLocalProvider(LocalCustomizationContext provides customizations) {
            DesignDocInternal(
                designSwitcherDocName(),
                designSwitcherDocId(),
                rootNodeQuery = NodeQuery.NodeName("#MessageFailed"),
                customizations = customizations,
                serverParams = DocumentServerParams(queries(), ignoredImages()),
                liveUpdateMode = getLiveMode(),
            )
        }
    }

    @Composable
    fun Checkbox(modifier: Modifier, checked: Boolean) {
        val nodeName = "#Checkbox=" + (if (checked) "On" else "Off")
        val queries = queries()
        queries.add(nodeName)
        DesignDocInternal(
            designSwitcherDocName(),
            designSwitcherDocId(),
            rootNodeQuery = NodeQuery.NodeVariant(nodeName, "#Checkbox"),
            customizations = CustomizationContext(),
            modifier = modifier,
            serverParams = DocumentServerParams(queries, ignoredImages()),
            liveUpdateMode = getLiveMode(),
        )
    }
}

internal object DesignSwitcherDoc : DesignSwitcher {}

internal fun designSwitcherDocId() = DesignDocId("Ljph4e3sC0lHcynfXpoh9f")

internal fun designSwitcherDocName() = "DesignSwitcherDoc"

/**
 * Disable Live Mode for the Design Switcher
 *
 * Controls whether the Design Switcher will fetch updates to it's DesignDoc from Figma.
 *
 * This should only be changed temporarily. Do not commit a change to `true`.
 */
private const val ENABLE_LIVE_MODE = false

private var enableLiveModeForTesting = false

internal fun enableLiveModeForTesting(enabled: Boolean) {
    if (Build.FINGERPRINT == "robolectric") {
        enableLiveModeForTesting = enabled
    } else {
        throw IllegalAccessException("Not designed for production...")
    }
}

private fun getLiveMode(): LiveUpdateMode {
    return if (ENABLE_LIVE_MODE || enableLiveModeForTesting) {
        LiveUpdateMode.LIVE
    } else {
        LiveUpdateMode.OFFLINE
    }
}

private fun elapsedTimeString(elapsedSeconds: Long): String {
    val elapsedSecondsNonNeg: Long = maxOf(elapsedSeconds, 0)
    when {
        elapsedSecondsNonNeg > 60 * 60 * 24 * 30 -> {
            // more than a month
            val months = elapsedSecondsNonNeg / 60 / 60 / 24 / 30
            return "${months}mth"
        }
        elapsedSecondsNonNeg > 60 * 60 * 24 -> {
            // more than a day
            val days = elapsedSecondsNonNeg / 60 / 60 / 24
            return "${days}d"
        }
        elapsedSecondsNonNeg > 60 * 60 -> {
            // more than an hour
            val hours = elapsedSecondsNonNeg / 60 / 60
            return "${hours}hr"
        }
        elapsedSecondsNonNeg > 60 -> {
            // more than a minute}
            val minutes = elapsedSecondsNonNeg / 60
            return "${minutes}m"
        }
        else -> {
            return "${elapsedSecondsNonNeg}s"
        }
    }
}

private fun GetBranches(
    branchHash: HashMap<String, String>?,
    setDocId: (DesignDocId) -> Unit,
    interactionState: InteractionState,
): ReplacementContent {
    val branchList = branchHash?.toList() ?: listOf()
    return ReplacementContent(
        count = branchList.size,
        content = { index ->
            {
                DesignSwitcherDoc.FigmaDoc(
                    branchList[index].second,
                    branchList[index].first,
                    {
                        interactionState.close(null)
                        setDocId(DesignDocId(branchList[index].first))
                    },
                )
            }
        },
    )
}

private fun GetProjectFileCount(doc: DocContent?): String {
    val count = doc?.c?.projectFiles?.size ?: 0
    return count.toString()
}

private fun GetProjectList(
    doc: DocContent?,
    setDocId: (DesignDocId) -> Unit,
    interactionState: InteractionState,
): ReplacementContent {
    return ReplacementContent(
        count = doc?.c?.projectFiles?.size ?: 0,
        content = { index ->
            {
                val docId = doc?.c?.projectFiles?.get(index)?.id ?: ""
                DesignSwitcherDoc.FigmaDoc(
                    doc?.c?.projectFiles?.get(index)?.name ?: "",
                    docId,
                    {
                        interactionState.close(null)
                        setDocId(DesignDocId(docId))
                    },
                )
            }
        },
    )
}

@Composable
private fun GetMessages(docId: DesignDocId): ReplacementContent {
    val (_, setMessagesId) = remember { mutableStateOf(0) }
    DisposableEffect(docId) {
        Feedback.register(docId, setMessagesId)
        onDispose { Feedback.unregister(docId) }
    }

    val messages = Feedback.getMessages()
    return ReplacementContent(
        count = messages.size,
        content = { index ->
            {
                val it = messages[index]
                val message = if (it.count > 1) it.message + "(${it.count})" else it.message
                val secondsAgo = (System.currentTimeMillis() - it.timestamp) / 1000
                if (it.level == FeedbackLevel.Error || it.level == FeedbackLevel.Warn)
                    DesignSwitcherDoc.MessageFailed(message, elapsedTimeString(secondsAgo))
                else DesignSwitcherDoc.Message(message, elapsedTimeString(secondsAgo))
            }
        },
    )
}

@Composable
private fun getMiniMessage(): String {
    val lastMessage: String? by Feedback.getLatestMessage().observeAsState()
    val (miniMessage, setMiniMessage) = remember { mutableStateOf(lastMessage) }

    // Clear out the mini view message after 5 seconds if it has not changed
    LaunchedEffect(lastMessage) {
        setMiniMessage(lastMessage)
        if (!lastMessage.isNullOrEmpty()) {
            delay(5000)
            setMiniMessage("")
        }
    }

    return miniMessage ?: ""
}

@Composable
private fun GetNodeNamesCheckbox(state: Boolean, setState: (Boolean) -> Unit): ReplacementContent {
    val clickModifier =
        Modifier.clickable {
            setState(!state)
            DebugNodeManager.setShowNodes(!state)
        }
    return ReplacementContent(
        count = 1,
        content = {
            {
                if (state) DesignSwitcherDoc.Checkbox(modifier = clickModifier, true)
                else DesignSwitcherDoc.Checkbox(modifier = clickModifier, false)
            }
        },
    )
}

@Composable
private fun GetMiniMessagesCheckbox(
    state: Boolean,
    setState: (Boolean) -> Unit,
): ReplacementContent {
    val clickModifier = Modifier.clickable { setState(!state) }
    return ReplacementContent(
        count = 1,
        content = {
            {
                if (state) DesignSwitcherDoc.Checkbox(modifier = clickModifier, true)
                else DesignSwitcherDoc.Checkbox(modifier = clickModifier, false)
            }
        },
    )
}

@Composable
private fun GetShowRecompositionCheckbox(
    state: Boolean,
    setState: (Boolean) -> Unit,
): ReplacementContent {
    val clickModifier =
        Modifier.clickable {
            setState(!state)
            DebugNodeManager.setShowRecomposition(!state)
        }
    return ReplacementContent(
        count = 1,
        content = {
            {
                if (state) DesignSwitcherDoc.Checkbox(modifier = clickModifier, true)
                else DesignSwitcherDoc.Checkbox(modifier = clickModifier, false)
            }
        },
    )
}

@Composable
private fun GetUseLocalResCheckbox(
    state: Boolean,
    setState: (Boolean) -> Unit,
): ReplacementContent {
    val clickModifier =
        Modifier.clickable {
            setState(!state)
            DebugNodeManager.setUseLocalRes(!state)
        }
    return ReplacementContent(
        count = 1,
        content = {
            {
                if (state) DesignSwitcherDoc.Checkbox(modifier = clickModifier, true)
                else DesignSwitcherDoc.Checkbox(modifier = clickModifier, false)
            }
        },
    )
}

@Composable
internal fun DesignSwitcher(
    doc: DocContent?,
    currentDocId: DesignDocId,
    branchHash: HashMap<String, String>?,
    setDocId: (DesignDocId) -> Unit,
) {
    remember { Feedback.addIgnoredDocument(designSwitcherDocId()) }
    val (docIdText, setDocIdText) = remember { mutableStateOf("") }
    val (topStatusBar, setTopStatusBar) =
        remember { mutableStateOf(DesignSwitcher.TopStatusBar.Default) }
    val loaded = (doc != null)

    LaunchedEffect(loaded) {
        if (!loaded) {
            setTopStatusBar(DesignSwitcher.TopStatusBar.Loading)
        } else {
            // Show "Loaded" variant for 2.5 seconds
            setTopStatusBar(DesignSwitcher.TopStatusBar.Loaded)
            delay(2500)
            setTopStatusBar(DesignSwitcher.TopStatusBar.Default)
        }
    }

    val lastModifiedString =
        if (doc != null) {
            val lastModifiedInstant = Instant.parse(doc.c.header.lastModified)
            val lastModifiedSeconds =
                System.currentTimeMillis() / 1000 - lastModifiedInstant.epochSecond
            val elapsed = elapsedTimeString(lastModifiedSeconds)
            "Modified $elapsed ago"
        } else ""

    val interactionState = InteractionStateManager.stateForDoc(designSwitcherDocId())

    val (nodeNamesChecked, setNodeNamesChecked) =
        remember { mutableStateOf(DebugNodeManager.getShowNodes().value ?: false) }
    val (miniMessagesChecked, setMiniMessagesChecked) = remember { mutableStateOf(true) }
    val (showRecompositionChecked, setShowRecompositionChecked) =
        remember { mutableStateOf(DebugNodeManager.getShowRecomposition().value ?: false) }
    val (useLocalResChecked, setUseLocalResChecked) = remember { DebugNodeManager.getUseLocalRes() }
    val miniMessage = if (miniMessagesChecked) getMiniMessage() else ""

    CompositionLocalProvider(LocalDocOverrideContext provides designSwitcherDocId()) {
        DesignSwitcherDoc.SettingsView(
            modifier = Modifier,
            mini_view_message = miniMessage,
            current_doc_name = doc?.c?.header?.name ?: "",
            last_mod_time = lastModifiedString,
            branch_file_count = branchHash?.size.toString(),
            project_file_count = GetProjectFileCount(doc),
            branch_list = GetBranches(branchHash, setDocId, interactionState),
            show_branch_section = !branchHash.isNullOrEmpty(),
            project_file_list = GetProjectList(doc, setDocId, interactionState),
            status_message_list = GetMessages(currentDocId),
            doc_text_edit = { context ->
                BasicTextField(
                    value = docIdText,
                    onValueChange = setDocIdText,
                    textStyle = context.textStyle ?: TextStyle.Default.copy(color = Color.White),
                    cursorBrush = SolidColor(context.textStyle?.color ?: (Color.White)),
                    modifier =
                        Modifier.onKeyEvent {
                            if (it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                                interactionState.close(null)
                                setDocId(DesignDocId(docIdText.trim()))
                                true
                            } else {
                                false
                            }
                        },
                )
            },
            show_help_text = docIdText.isEmpty(),
            on_tap_go = {
                if (docIdText.isNotEmpty()) {
                    interactionState.close(null)
                    setDocId(DesignDocId(docIdText))
                }
            },
            node_names_checkbox = GetNodeNamesCheckbox(nodeNamesChecked, setNodeNamesChecked),
            mini_messages_checkbox =
                GetMiniMessagesCheckbox(miniMessagesChecked, setMiniMessagesChecked),
            show_recomposition_checkbox =
                GetShowRecompositionCheckbox(showRecompositionChecked, setShowRecompositionChecked),
            useLocalResCheckbox = GetUseLocalResCheckbox(useLocalResChecked, setUseLocalResChecked),
            live_mode =
                if (DesignSettings.isDocumentLive.value) DesignSwitcher.LiveMode.Live
                else DesignSwitcher.LiveMode.Offline,
            top_status_bar = topStatusBar,
        )
    }
}
