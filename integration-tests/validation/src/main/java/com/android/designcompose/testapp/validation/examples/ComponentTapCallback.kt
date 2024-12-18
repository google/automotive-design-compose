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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant

enum class DrivingState {
    P,
    R,
    D,
}

@DesignDoc(id = "1jeKYynjk1nqYblZ66QDDK")
interface ComponentTapCallback {
    @DesignComponent(node = "#Main", isRoot = true)
    fun mainFrame(
        @Design(node = "CompTest")
        drivingStateIndicator: @Composable (ComponentReplacementContext) -> Unit
    )

    @DesignComponent(node = "CompTest")
    fun drivingStateIndicator(
        @DesignVariant(property = "prnd") state: DrivingState,
        @Design(node = "CompTest") onPress: TapCallback,
    )
}

@Composable
fun ComponentTapCallbackTest() {
    val drivingState = remember { mutableStateOf(DrivingState.P) }
    ComponentTapCallbackDoc.mainFrame(
        drivingStateIndicator = {
            val testTag =
                when (drivingState.value) {
                    DrivingState.P -> "Parked"
                    DrivingState.R -> "Reverse"
                    DrivingState.D -> "Drive"
                }
            ComponentTapCallbackDoc.drivingStateIndicator(
                modifier = Modifier.testTag(testTag),
                state = drivingState.value,
                onPress = {
                    drivingState.value =
                        when (drivingState.value) {
                            DrivingState.P -> DrivingState.R
                            DrivingState.R -> DrivingState.D
                            DrivingState.D -> DrivingState.P
                        }
                },
            )
        }
    )
}
