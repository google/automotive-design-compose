/*
 * Copyright 2026 Google LLC
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

package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate

// Regression coverage for nested-component Modifier propagation (PR #2466).
//
// This reuses the ComponentTapCallback Figma document (and its committed .dcf asset), whose
// "#Main" frame renders a nested "CompTest" component instance. (The generated doc lists
// CompTest under "#Main" in ignoredImages, so CompTest is rendered live as a nested component
// rather than as a flattened image.)
//
// `ComponentTapCallbackDoc.mainFrameWithNestedModifier` registers a Modifier on the nested
// "CompTest" node via CustomizationContext.setModifier("CompTest", ...). Before the fix a
// Modifier registered on a nested component had nowhere to apply -- the nested node's pixels
// are drawn by the parent's draw block, not inside a Compose Layout for that node -- so the
// Modifier was silently dropped. The fix renders the modifier-wrapped subtree via a
// sub-SquooshRoot so transforms like rotate/alpha/graphicsLayer actually affect the pixels.
//
// A visually-obvious 45-degree rotation is applied: with the fix the rendered nested component
// is rotated; reverting the sub-SquooshRoot modifier-wrap renders it unrotated, changing the
// captured snapshot.
@Composable
fun NestedComponentModifierTest() {
    ComponentTapCallbackDoc.mainFrameWithNestedModifier(nestedModifier = Modifier.rotate(45f))
}
