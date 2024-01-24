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

import android.util.Log
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.android.designcompose.common.DocumentServerParams
import com.android.designcompose.common.FeedbackLevel
import com.android.designcompose.serdegen.NodeQuery
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
            "#Name",
            "#Id",
            "#Text",
            "#TimeStamp",
            "#Logout",
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
                ),
        )
    }

    enum class LiveMode {
        Live,
        Offline
    }

    enum class TopStatusBar {
        Default,
        Loading,
        Loaded,
    }

    @Composable
    fun CustomComponent(
        modifier: Modifier = Modifier,
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
                rootNodeQuery,
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
        modifier: Modifier = Modifier,
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
        on_tap_go: Modifier,
        node_names_checkbox: ReplacementContent,
        mini_messages_checkbox: ReplacementContent,
        show_recomposition_checkbox: ReplacementContent,
        on_tap_logout: Modifier,
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
        customizations.setModifier("#GoButton", on_tap_go)
        customizations.setContent("#NodeNamesCheckbox", node_names_checkbox)
        customizations.setContent("#MiniMessagesCheckbox", mini_messages_checkbox)
        customizations.setContent("#ShowRecompositionCheckbox", show_recomposition_checkbox)
        customizations.setModifier("#LogoutButton", on_tap_logout)

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
                NodeQuery.NodeName("#SettingsView"),
                customizations = customizations,
                modifier = modifier.semantics { sDocClass = DesignSwitcherDoc.javaClass.name },
                serverParams = DocumentServerParams(queries(), ignoredImages()),
                designSwitcherPolicy = DesignSwitcherPolicy.IS_DESIGN_SWITCHER,
                liveUpdateMode = getLiveMode(),
            )
        }
    }

    @Composable
    fun FigmaDoc(
        name: String,
        id: String,
        modifier: Modifier,
        parentLayout: ParentLayoutInfo,
    ) {
        val customizations = CustomizationContext()
        customizations.setText("#Name", name)
        customizations.setText("#Id", id)
        customizations.setModifier("#GoButton", modifier)

        CompositionLocalProvider(LocalCustomizationContext provides customizations) {
            DesignDocInternal(
                designSwitcherDocName(),
                designSwitcherDocId(),
                NodeQuery.NodeName("#FigmaDoc"),
                customizations = customizations,
                serverParams = DocumentServerParams(queries(), ignoredImages()),
                liveUpdateMode = getLiveMode(),
                parentLayout = parentLayout,
            )
        }
    }

    @Composable
    fun Message(
        text: String,
        timestamp: String,
        parentLayout: ParentLayoutInfo,
    ) {
        val customizations = CustomizationContext()
        customizations.setText("#Text", text)
        customizations.setText("#TimeStamp", timestamp)

        CompositionLocalProvider(LocalCustomizationContext provides customizations) {
            DesignDocInternal(
                designSwitcherDocName(),
                designSwitcherDocId(),
                NodeQuery.NodeName("#Message"),
                customizations = customizations,
                serverParams = DocumentServerParams(queries(), ignoredImages()),
                liveUpdateMode = getLiveMode(),
                parentLayout = parentLayout,
            )
        }
    }

    @Composable
    fun MessageFailed(
        text: String,
        timestamp: String,
        parentLayout: ParentLayoutInfo,
    ) {
        val customizations = CustomizationContext()
        customizations.setText("#Text", text)
        customizations.setText("#TimeStamp", timestamp)

        CompositionLocalProvider(LocalCustomizationContext provides customizations) {
            DesignDocInternal(
                designSwitcherDocName(),
                designSwitcherDocId(),
                NodeQuery.NodeName("#MessageFailed"),
                customizations = customizations,
                serverParams = DocumentServerParams(queries(), ignoredImages()),
                liveUpdateMode = getLiveMode(),
                parentLayout = parentLayout,
            )
        }
    }

    @Composable
    fun Checkbox(
        modifier: Modifier,
        checked: Boolean,
        parentLayout: ParentLayoutInfo,
    ) {
        val nodeName = "#Checkbox=" + (if (checked) "On" else "Off")
        val queries = queries()
        queries.add(nodeName)
        DesignDocInternal(
            designSwitcherDocName(),
            designSwitcherDocId(),
            NodeQuery.NodeVariant(nodeName, "#Checkbox"),
            customizations = CustomizationContext(),
            modifier = modifier,
            serverParams = DocumentServerParams(queries, ignoredImages()),
            liveUpdateMode = getLiveMode(),
            parentLayout = parentLayout,
        )
    }
}

internal object DesignSwitcherDoc : DesignSwitcher {}

internal fun designSwitcherDocId() = "Ljph4e3sC0lHcynfXpoh9f"

internal fun designSwitcherDocName() = "DesignSwitcherDoc"

/**
 * Disable Live Mode for the Design Switcher
 *
 * Controls whether the Design Switcher will fetch updates to it's DesignDoc from Figma.
 *
 * This should only be changed temporarily. Do not commit a change to `false`.
 */
private const val DISABLE_LIVE_MODE = true

private fun getLiveMode(): LiveUpdateMode {
    return if (DISABLE_LIVE_MODE) {
        LiveUpdateMode.OFFLINE
    } else {
        LiveUpdateMode.LIVE
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
    setDocId: (String) -> Unit,
    interactionState: InteractionState
): ReplacementContent {
    val branchList = branchHash?.toList() ?: listOf()
    return ReplacementContent(
        count = branchList.size,
        content = { index ->
            { rc ->
                DesignSwitcherDoc.FigmaDoc(
                    branchList[index].second,
                    branchList[index].first,
                    Modifier.clickable {
                        interactionState.close(null)
                        setDocId(branchList[index].first)
                    },
                    ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId),
                )
            }
        }
    )
}

private fun GetProjectFileCount(doc: DocContent?): String {
    var count = doc?.c?.project_files?.size ?: 0
    return count.toString()
}

private fun GetProjectList(
    doc: DocContent?,
    setDocId: (String) -> Unit,
    interactionState: InteractionState
): ReplacementContent {
    return ReplacementContent(
        count = doc?.c?.project_files?.size ?: 0,
        content = { index ->
            { rc ->
                DesignSwitcherDoc.FigmaDoc(
                    doc?.c?.project_files?.get(index)?.name ?: "",
                    doc?.c?.project_files?.get(index)?.id ?: "",
                    Modifier.clickable {
                        interactionState.close(null)
                        setDocId(doc?.c?.project_files?.get(index)?.id ?: "")
                    },
                    ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId),
                )
            }
        }
    )
}

@Composable
private fun GetMessages(docId: String): ReplacementContent {
    val (_, setMessagesId) = remember { mutableStateOf(0) }
    DisposableEffect(docId) {
        Feedback.register(docId, setMessagesId)
        onDispose { Feedback.unregister(docId) }
    }

    val messages = Feedback.getMessages()
    return ReplacementContent(
        count = messages.size,
        content = { index ->
            { rc ->
                val it = messages[index]
                val message = if (it.count > 1) it.message + "(${it.count})" else it.message
                val secondsAgo = (System.currentTimeMillis() - it.timestamp) / 1000
                if (it.level == FeedbackLevel.Error || it.level == FeedbackLevel.Warn)
                    DesignSwitcherDoc.MessageFailed(
                        message,
                        elapsedTimeString(secondsAgo),
                        parentLayout = ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId)
                    )
                else
                    DesignSwitcherDoc.Message(
                        message,
                        elapsedTimeString(secondsAgo),
                        parentLayout = ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId)
                    )
            }
        }
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
            { rc ->
                if (state)
                    DesignSwitcherDoc.Checkbox(
                        modifier = clickModifier,
                        true,
                        parentLayout = ParentLayoutInfo(rc.parentLayoutId, 0, rc.rootLayoutId)
                    )
                else
                    DesignSwitcherDoc.Checkbox(
                        modifier = clickModifier,
                        false,
                        parentLayout = ParentLayoutInfo(rc.parentLayoutId, 0, rc.rootLayoutId)
                    )
            }
        }
    )
}

@Composable
private fun GetMiniMessagesCheckbox(
    state: Boolean,
    setState: (Boolean) -> Unit
): ReplacementContent {
    val clickModifier = Modifier.clickable { setState(!state) }
    return ReplacementContent(
        count = 1,
        content = {
            { rc ->
                if (state)
                    DesignSwitcherDoc.Checkbox(
                        modifier = clickModifier,
                        true,
                        parentLayout = ParentLayoutInfo(rc.parentLayoutId, 0, rc.rootLayoutId)
                    )
                else
                    DesignSwitcherDoc.Checkbox(
                        modifier = clickModifier,
                        false,
                        parentLayout = ParentLayoutInfo(rc.parentLayoutId, 0, rc.rootLayoutId)
                    )
            }
        }
    )
}

@Composable
private fun GetShowRecompositionCheckbox(
    state: Boolean,
    setState: (Boolean) -> Unit
): ReplacementContent {
    val clickModifier =
        Modifier.clickable {
            setState(!state)
            DebugNodeManager.setShowRecomposition(!state)
        }
    return ReplacementContent(
        count = 1,
        content = {
            { rc ->
                if (state)
                    DesignSwitcherDoc.Checkbox(
                        modifier = clickModifier,
                        true,
                        parentLayout = ParentLayoutInfo(rc.parentLayoutId, 0, rc.rootLayoutId)
                    )
                else
                    DesignSwitcherDoc.Checkbox(
                        modifier = clickModifier,
                        false,
                        parentLayout = ParentLayoutInfo(rc.parentLayoutId, 0, rc.rootLayoutId)
                    )
            }
        }
    )
}

@Composable
internal fun DesignSwitcher(
    doc: DocContent?,
    currentDocId: String,
    branchHash: HashMap<String, String>?,
    setDocId: (String) -> Unit
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
            val lastModifiedInstant = Instant.parse(doc.c.document.last_modified)
            val lastModifiedSeconds =
                System.currentTimeMillis() / 1000 - lastModifiedInstant.epochSecond
            val elapsed = elapsedTimeString(lastModifiedSeconds)
            "Modified $elapsed ago"
        } else ""

    val interactionState = InteractionStateManager.stateForDoc(designSwitcherDocId())

    val (nodeNamesChecked, setNodeNamesChecked) = remember { mutableStateOf(false) }
    val (miniMessagesChecked, setMiniMessagesChecked) = remember { mutableStateOf(true) }
    val (showRecompositionChecked, setShowRecompositionChecked) = remember { mutableStateOf(false) }
    val miniMessage = if (miniMessagesChecked) getMiniMessage() else ""

    CompositionLocalProvider(LocalDocOverrideContext provides designSwitcherDocId()) {
        DesignSwitcherDoc.SettingsView(
            modifier = Modifier,
            mini_view_message = miniMessage,
            current_doc_name = doc?.c?.document?.name ?: "",
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
                    textStyle = context.textStyle ?: TextStyle.Default,
                    modifier =
                        Modifier.onKeyEvent {
                            if (it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                                interactionState.close(null)
                                setDocId(docIdText.trim())
                                true
                            } else {
                                false
                            }
                        }
                )
            },
            show_help_text = docIdText.isEmpty(),
            on_tap_go =
                Modifier.clickable {
                    if (docIdText.isNotEmpty()) {
                        interactionState.close(null)
                        setDocId(docIdText)
                    }
                },
            node_names_checkbox = GetNodeNamesCheckbox(nodeNamesChecked, setNodeNamesChecked),
            mini_messages_checkbox =
                GetMiniMessagesCheckbox(miniMessagesChecked, setMiniMessagesChecked),
            show_recomposition_checkbox =
                GetShowRecompositionCheckbox(showRecompositionChecked, setShowRecompositionChecked),
            on_tap_logout = Modifier.clickable { Log.i(TAG, "TODO: Re-implement Logging out") },
            live_mode =
                if (DesignSettings.isDocumentLive.value) DesignSwitcher.LiveMode.Live
                else DesignSwitcher.LiveMode.Offline,
            top_status_bar = topStatusBar,
        )
    }
}
