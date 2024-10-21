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

package com.android.designcompose.testapp.validation.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// Tests that component replacement is laid out and rendered properly.
// When replacing within an autolayout frame, the autolayout frame should make space for the new
// component. When replacing in a non-autolayout frame, the new component should be placed in the
// same place as the original node.
@DesignDoc(id = "bQVVy2GSZJ8veYaJUrG6Ni")
interface ComponentReplace {
    @DesignComponent(node = "#stage")
    fun Main(
        @Design(node = "#replace-node-horizontal")
        replaceHorizontal: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#replace-node-vertical")
        replaceVertical: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#replace-node-absolute")
        replaceAbsolute: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#replace-node-box")
        replaceBox: @Composable (ComponentReplacementContext) -> Unit,
    )

    @DesignComponent(node = "#BlueReplacement") fun BlueNode()

    @DesignComponent(node = "#RedReplacement") fun RedNode()
}

@Composable
fun ComponentReplaceTest() {
    ComponentReplaceDoc.Main(
        modifier = Modifier.fillMaxWidth(),
        replaceHorizontal = { ComponentReplaceDoc.BlueNode() },
        replaceVertical = { ComponentReplaceDoc.RedNode() },
        replaceAbsolute = { ComponentReplaceDoc.BlueNode() },
        replaceBox = { Box(Modifier.background(Color.Green).then(it.layoutModifier)) },
    )
}
