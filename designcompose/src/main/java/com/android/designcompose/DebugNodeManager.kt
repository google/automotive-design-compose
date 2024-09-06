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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.designcompose.common.DesignDocId

// DebugNodeManager keeps track of the size and positions of all Figma nodes that we are rendering
// so that we can do a post render pass and draw the node names on top.
internal object DebugNodeManager {
    internal data class NodePosition(
        val id: String,
        val nodeName: String,
        val position: Offset,
        val size: IntSize,
        val color: Color,
    )

    private val showNodes: MutableLiveData<Boolean> = MutableLiveData(false)
    private val showRecomposition: MutableLiveData<Boolean> = MutableLiveData(false)
    private val useLocalRes: MutableState<Boolean> = mutableStateOf(true)
    private val nodes: SnapshotStateMap<Int, NodePosition> = mutableStateMapOf()
    private var nodeId: Int = 0

    internal fun getShowNodes(): LiveData<Boolean> {
        return showNodes
    }

    internal fun setShowNodes(show: Boolean) {
        if (!show) nodes.clear()
        showNodes.postValue(show)
    }

    internal fun getShowRecomposition(): LiveData<Boolean> {
        return showRecomposition
    }

    internal fun setShowRecomposition(show: Boolean) {
        showRecomposition.postValue(show)
    }

    internal fun getUseLocalRes(): MutableState<Boolean> {
        return useLocalRes
    }

    internal fun setUseLocalRes(useLocal: Boolean) {
        useLocalRes.value = useLocal
    }

    internal fun addNode(docId: DesignDocId, existingId: Int, node: NodePosition): Int {
        if (
            !showNodes.value!! ||
                !node.nodeName.startsWith("#") ||
                Feedback.isDocumentIgnored(docId)
        )
            return 0
        val oldNode = nodes[existingId]
        return if (oldNode != null) {
            nodes[existingId] = node
            existingId
        } else {
            ++nodeId
            nodes[nodeId] = node
            nodeId
        }
    }

    internal fun removeNode(id: Int) {
        nodes.remove(id)
    }

    @Composable
    internal fun DrawNodeNames() {
        val show: Boolean? by showNodes.observeAsState()
        if (show == null || !show!!) return

        // For each debug node, draw a box on top of the node, then text on a partially transparent
        // colored box at the top left of the node's box
        nodes.values.forEach {
            Box(
                modifier =
                    Modifier.absoluteOffset(it.position.x.dp, it.position.y.dp)
                        .size(it.size.width.dp, it.size.height.dp)
            ) {
                BasicText(
                    it.nodeName,
                    modifier = Modifier.then(Modifier.background(it.color)),
                    style = TextStyle(color = Color(1f, 1f, 1f, 1.0f)),
                )
            }
        }
    }
}
