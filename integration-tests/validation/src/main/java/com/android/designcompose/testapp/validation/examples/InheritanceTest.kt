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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

@DesignDoc(id = "x9JgFP4Jfn406130rRc30Y")
interface Parent {
    @DesignComponent(node = "#stage", isRoot = true)
    fun mainFrame(
        @Design(node = "#replace-node")
        replaceIndicator: @Composable (ComponentReplacementContext) -> Unit,
    )
}

interface ParentNoDesignDoc {
    fun noDesignDocFunc() {}
}

@DesignDoc(id = "x9JgFP4Jfn406130rRc30Y")
interface Child : ParentGen, ParentNoDesignDoc {
    @DesignComponent(node = "#BlueReplacement") fun BlueNode()
}

@Composable
fun InheritanceTest() {
    ChildDoc.noDesignDocFunc()
    ChildDoc.mainFrame(
        modifier = Modifier.fillMaxWidth(),
        replaceIndicator = {
            ChildDoc.BlueNode(parentLayout = it.parentLayout, modifier = Modifier.width(300.dp))
        }
    )
}
