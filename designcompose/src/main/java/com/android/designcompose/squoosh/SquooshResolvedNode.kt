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

import android.graphics.PointF
import com.android.designcompose.TextMeasureData
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewStyle

/// A SquooshResolvedNode represents a design element from the DesignCompose tree after variants
/// and other customizations have been applied. The SquooshResolvedNode tree is handed to layout
/// verbatim.
///
/// Once layout has been completed, a SquooshResolvedNode tree can be rendered, or used to generate
/// animations (resulting in a new SquooshResolvedNode tree) and then rendered.
internal class SquooshResolvedNode(
    var view: View, // updated by animations
    var style: ViewStyle,
    val layoutId: Int,
    val textInfo: TextMeasureData?,
    val unresolvedNodeId: String, // The node id before we resolved variants; used for interactions
    var firstChild: SquooshResolvedNode? = null,
    var nextSibling: SquooshResolvedNode? = null,
    var parent: SquooshResolvedNode? = null,
    var computedLayout: Layout? = null,
    var needsChildRender: Boolean = false,
) {
    fun offsetFromAncestor(ancestor: SquooshResolvedNode? = null): PointF
    {
        var n: SquooshResolvedNode? = this
        var x = 0f
        var y = 0f
        while (n != ancestor && n != null) {
            val layout = n.computedLayout
            if (layout != null) {
                x += layout.left
                y += layout.top
            }
            n = n.parent
        }
        return PointF(x, y)
    }
}