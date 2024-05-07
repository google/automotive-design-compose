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

package com.android.designcompose.testapp.validation.examples

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant

// Icon2 modified the icon and it has #iconContainer missing
const val DOC_ID = "kB8BnWbUm7ZeqqRwyt2nFH"

enum class IconButtonState {
    Enabled,
    Selected
}

enum class Icons {
    HEAD_SIDE_THINKING,
    HEADSET,
    HEADPHONES,
    HAT_BIRTHDAY,
}

@DesignDoc(id = DOC_ID)
interface ModifiedComponentReplace {
    @DesignComponent(node = "#mainFrame", isRoot = true)
    fun mainFrame(
        @Design(node = "#Icon1") icon1: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#Icon2") icon2: @Composable (ComponentReplacementContext) -> Unit,
    )

    @DesignComponent(node = "IconButton")
    fun iconButton(
        @DesignVariant(property = "state") state: IconButtonState,
        @Design(node = "#iconContainer") icon: @Composable (ComponentReplacementContext) -> Unit,
    )

    @DesignComponent(node = "fi-rr-head-side-thinking") fun headSideThinkingIcon()

    @DesignComponent(node = "fi-rr-headphones") fun headphoneIcon()

    @DesignComponent(node = "fi-rr-headset") fun headsetIcon()

    @DesignComponent(node = "fi-rr-hat-birthday") fun hatBirthdayIcon()
}

@Composable
fun ModifiedComponentReplaceTest() {
    val icon1State = remember { mutableStateOf(IconButtonState.Enabled) }
    val icon2State = remember { mutableStateOf(IconButtonState.Enabled) }

    Box(modifier = Modifier.fillMaxSize()) {
        ModifiedComponentReplaceDoc.mainFrame(
            Modifier.fillMaxSize(),
            icon1 = {
                iconButton(Icons.HEAD_SIDE_THINKING, icon1State.value) {
                    icon1State.value =
                        if (icon1State.value == IconButtonState.Enabled) IconButtonState.Selected
                        else IconButtonState.Enabled
                }
            },
            icon2 = { iconButton(Icons.HAT_BIRTHDAY, icon2State.value) {} },
        )
        Row(Modifier.fillMaxWidth().padding(16.dp).align(Alignment.BottomStart)) {
            Button(
                onClick = {
                    icon1State.value =
                        if (icon1State.value == IconButtonState.Enabled) IconButtonState.Selected
                        else IconButtonState.Enabled
                }
            ) {
                Text(text = "Toggle Icon1 State")
            }
            Button(
                modifier = Modifier.padding(16.dp, 0.dp, 0.dp, 0.dp),
                onClick = {
                    icon2State.value =
                        if (icon2State.value == IconButtonState.Enabled) IconButtonState.Selected
                        else IconButtonState.Enabled
                }
            ) {
                Text(text = "Toggle Icon2 State")
            }
        }
    }
}

@Composable
fun iconButton(icon: Icons, state: IconButtonState, tapCallback: TapCallback) {
    ModifiedComponentReplaceDoc.iconButton(
        state = state,
        icon = {
            when (icon) {
                Icons.HEAD_SIDE_THINKING -> ModifiedComponentReplaceDoc.headSideThinkingIcon()
                Icons.HEADPHONES -> ModifiedComponentReplaceDoc.headphoneIcon()
                Icons.HEADSET -> ModifiedComponentReplaceDoc.headsetIcon()
                Icons.HAT_BIRTHDAY -> ModifiedComponentReplaceDoc.hatBirthdayIcon()
            }
        },
    )
}
