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

import android.os.Trace.beginSection
import android.os.Trace.endSection
import androidx.annotation.Discouraged
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.dp
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.common.DocumentServerParams
import com.android.designcompose.common.NodeQuery
import com.android.designcompose.serdegen.ComponentInfo
import com.android.designcompose.squoosh.SquooshRoot
import kotlin.math.min
import kotlinx.coroutines.delay

// This debugging modifier draws a border around elements that are recomposing. The border increases
// in size and interpolates from red to green as more recompositions occur before a timeout. This
// code is borrowed from the Play Store
@Stable fun Modifier.recomposeHighlighter(): Modifier = this.then(recomposeModifier)

// Use a single instance + @Stable to ensure that recompositions can enable skipping optimizations
// Modifier.composed will still remember unique data per call site. If the FinskyPref is updated,
// the process is restarted from debug options.
private val recomposeModifier =
    Modifier.composed(inspectorInfo = debugInspectorInfo { name = "recomposeHighlighter" }) {
        // The total number of compositions that have occurred. We're not using a State<> here be
        // able
        // to read/write the value without invalidating (which would cause infinite recomposition).
        val totalCompositions = remember { arrayOf(0L) }
        totalCompositions[0]++

        // The value of totalCompositions at the last timeout.
        val totalCompositionsAtLastTimeout = remember { mutableStateOf(0L) }

        // Start the timeout, and reset everytime there's a recomposition. (Using totalCompositions
        // as
        // the key is really just to cause the timer to restart every composition).
        LaunchedEffect(totalCompositions[0]) {
            delay(3000)
            totalCompositionsAtLastTimeout.value = totalCompositions[0]
        }

        Modifier.drawWithCache {
            onDrawWithContent {
                // Draw actual content.
                drawContent()

                // Below is to draw the highlight, if necessary. A lot of the logic is copied from
                // Modifier.border
                val numCompositionsSinceTimeout =
                    totalCompositions[0] - totalCompositionsAtLastTimeout.value

                val hasValidBorderParams = size.minDimension > 0f
                if (!hasValidBorderParams || numCompositionsSinceTimeout <= 0) {
                    return@onDrawWithContent
                }

                val (color, strokeWidthPx) =
                    when (numCompositionsSinceTimeout) {
                        // We need at least one composition to draw, so draw the smallest border
                        // color in
                        // blue.
                        1L -> Color.Blue to 1f
                        // 2 compositions is _probably_ okay.
                        2L -> Color.Green to 2.dp.toPx()
                        // 3 or more compositions before timeout may indicate an issue. lerp the
                        // color from
                        // yellow to red, and continually increase the border size.
                        else -> {
                            lerp(
                                Color.Yellow.copy(alpha = 0.8f),
                                Color.Red.copy(alpha = 0.5f),
                                min(1f, (numCompositionsSinceTimeout - 1).toFloat() / 100f),
                            ) to numCompositionsSinceTimeout.toInt().dp.toPx()
                        }
                    }

                val halfStroke = strokeWidthPx / 2
                val topLeft = Offset(halfStroke, halfStroke)
                val borderSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

                val fillArea = (strokeWidthPx * 2) > size.minDimension
                val rectTopLeft = if (fillArea) Offset.Zero else topLeft
                val size = if (fillArea) size else borderSize
                val style = if (fillArea) Fill else Stroke(strokeWidthPx)

                drawRect(
                    brush = SolidColor(color),
                    topLeft = rectTopLeft,
                    size = size,
                    style = style,
                )
            }
        }
    }

data class ParentComponentInfo(val instanceId: String, val componentInfo: ComponentInfo)

// We want to know if we're the "root" component. For now, we'll do that using a local composition
// struct that indicates we're a "root" by default, and then gets propagated as "not a root" for
// children.
//
// This should work well if everything is a compose component, but might get confused (with multiple
// roots) when mixing Android Views and Compose views (where multiple Android Views have Compose
// children). In that case, we may need to extend the annotations to include the concept of a root.
internal data class DesignIsRoot(val isRoot: Boolean)

internal val LocalDesignIsRootContext = compositionLocalOf { DesignIsRoot(true) }

// Current customization context that contains all customizations passed through from any ancestor
val LocalCustomizationContext = compositionLocalOf { CustomizationContext() }

// Current document override ID that can be used to override the document ID specified from the
// @DesignDoc annotation
internal val LocalDocOverrideContext = compositionLocalOf { DesignDocId("") }

// Public function to set the document override ID
@Composable
@Discouraged(
    message =
        "Use of this function will override all document IDs in the tree. If more" +
            " than one root document is used, all will instead use this document ID. Use this function only" +
            " when there is no other way to set the document ID."
)
fun DesignDocOverride(docId: DesignDocId, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDocOverrideContext provides docId) { content() }
}

// A global object that keeps track of the current document ID we are subscribed to.
// When switching document IDs, we notify all subscribers of the change to trigger
// a recomposition.
internal object DocumentSwitcher {
    private val subscribers: HashMap<DesignDocId, ArrayList<(DesignDocId) -> Unit>> = HashMap()
    private val documentSwitchHash: HashMap<DesignDocId, DesignDocId> = HashMap()
    private val documentSwitchReverseHash: HashMap<DesignDocId, DesignDocId> = HashMap()

    internal fun subscribe(originalDocId: DesignDocId, setDocId: (DesignDocId) -> Unit) {
        val list = subscribers[originalDocId] ?: ArrayList()
        list.add(setDocId)
        subscribers[originalDocId] = list
    }

    internal fun switch(originalDocId: DesignDocId, newDocId: DesignDocId) {
        if (!newDocId.isValid()) return
        if (originalDocId != newDocId) {
            documentSwitchHash[originalDocId] = newDocId
            documentSwitchReverseHash[newDocId] = originalDocId
        } else {
            documentSwitchHash.remove(originalDocId)
            documentSwitchReverseHash.remove(originalDocId)
        }
        val list = subscribers[originalDocId]
        list?.forEach { it(newDocId) }
    }

    internal fun revertToOriginal(docId: DesignDocId) {
        val originalDocId = documentSwitchReverseHash[docId]
        if (originalDocId != null) {
            switch(originalDocId, originalDocId)
            documentSwitchReverseHash.remove(docId)
        }
    }

    internal fun isNotOriginalDocId(docId: DesignDocId): Boolean {
        val originalDocId = documentSwitchReverseHash[docId]
        return originalDocId != null
    }

    internal fun getSwitchedDocId(docId: DesignDocId): DesignDocId {
        return documentSwitchHash[docId] ?: docId
    }
}

enum class DesignSwitcherPolicy {
    SHOW_IF_ROOT, // Show the design switcher on root nodes
    HIDE, // Hide the design switcher
    IS_DESIGN_SWITCHER, // This is the design switcher, so don't show embed another one
}

enum class LiveUpdateMode {
    LIVE, // Live updates on
    OFFLINE, // Live updates off (load from serialized file)
}

class DesignComposeCallbacks(
    val docReadyCallback: ((DesignDocId) -> Unit)? = null,
    val newDocDataCallback: ((DesignDocId, ByteArray?) -> Unit)? = null,
)

@Composable
fun DesignDoc(
    docName: String,
    docId: DesignDocId,
    rootNodeQuery: NodeQuery,
    modifier: Modifier = Modifier,
    customizations: CustomizationContext = CustomizationContext(),
    serverParams: DocumentServerParams = DocumentServerParams(),
    setDocId: (DesignDocId) -> Unit = {},
    designSwitcherPolicy: DesignSwitcherPolicy = DesignSwitcherPolicy.SHOW_IF_ROOT,
    designComposeCallbacks: DesignComposeCallbacks? = null,
    parentComponents: List<ParentComponentInfo> = listOf(),
) {
    beginSection(DCTraces.DESIGNDOCINTERNAL)
    DesignDocInternal(
        docName,
        docId,
        rootNodeQuery,
        modifier = modifier,
        customizations = customizations,
        serverParams = serverParams,
        setDocId = setDocId,
        designSwitcherPolicy = designSwitcherPolicy,
        designComposeCallbacks = designComposeCallbacks,
        parentComponents = parentComponents,
    )
    endSection()
}

@Composable
internal fun DesignDocInternal(
    docName: String,
    incomingDocId: DesignDocId,
    rootNodeQuery: NodeQuery,
    modifier: Modifier = Modifier,
    customizations: CustomizationContext = CustomizationContext(),
    serverParams: DocumentServerParams = DocumentServerParams(),
    setDocId: (DesignDocId) -> Unit = {},
    designSwitcherPolicy: DesignSwitcherPolicy = DesignSwitcherPolicy.SHOW_IF_ROOT,
    liveUpdateMode: LiveUpdateMode = LiveUpdateMode.LIVE,
    designComposeCallbacks: DesignComposeCallbacks? = null,
    parentComponents: List<ParentComponentInfo> = listOf(),
) {
    val overrideDocId = LocalDocOverrideContext.current
    // Use the override document ID if it is not empty
    val currentDocId = if (overrideDocId.isValid()) overrideDocId else incomingDocId
    SquooshRoot(
        docName = docName,
        incomingDocId = currentDocId,
        rootNodeQuery = rootNodeQuery,
        modifier = modifier,
        customizationContext = customizations,
        serverParams = serverParams,
        setDocId = setDocId,
        designSwitcherPolicy = designSwitcherPolicy,
        liveUpdateMode = liveUpdateMode,
        designComposeCallbacks = designComposeCallbacks,
    )
}
