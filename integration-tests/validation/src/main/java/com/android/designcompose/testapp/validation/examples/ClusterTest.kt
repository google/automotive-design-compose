/*
 * Copyright 2024 The Android Open Source Project
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

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant

object ClusterVariant {
    enum class ComponentType {
        Navigation,
        NavigationSmall,
        NavigationThin,
        Call,
        InCall,
        CallThin,
        InCallThin,
        Media,
        NoMedia,
        MediaThin,
        NoMediaThin,
        Bootup,
        Alert,
        Gauge,
        ADAS,
        None,
        Weather,
        MediaNavigation,
    }

    enum class ViewMode {
        normal,
        sport,
        charging,
        reverse,
    }

    enum class PhoneCallState {
        incall,
        incoming,
        nocall,
    }

    enum class MediaPlayerState {
        on,
        loading,
        nomedia,
    }
}

@DesignDoc(id = "CuF1b1eAIukB6YszX6B5OZ")
interface Cluster {
    @DesignComponent(node = "#android-stage")
    fun androidMain(
        @DesignVariant(property = "#panel1") panel1Component: ClusterVariant.ComponentType,
        @DesignVariant(property = "#panel2") panel2Component: ClusterVariant.ComponentType,
        @DesignVariant(property = "#panel3") panel3Component: ClusterVariant.ComponentType,
        @DesignVariant(property = "#media") mediaPlayerState: ClusterVariant.MediaPlayerState,
        @DesignVariant(property = "#phone/state") phoneState: ClusterVariant.PhoneCallState,
        @DesignVariant(property = "#view-mode") viewMode: ClusterVariant.ViewMode,
    )
}

@Composable
fun PanelButtons(
    name: String,
    state: MutableState<ClusterVariant.ComponentType>,
    mediaState: MutableState<ClusterVariant.MediaPlayerState>,
    phoneState: MutableState<ClusterVariant.PhoneCallState>,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name)
        Button(
            onClick = {
                Log.d("ClusterTest", "$name setting to Media")
                state.value = ClusterVariant.ComponentType.Media
                mediaState.value = ClusterVariant.MediaPlayerState.on
            }
        ) {
            Text("Media")
        }
        Button(
            onClick = {
                Log.d("ClusterTest", "$name setting to NoMedia")
                state.value = ClusterVariant.ComponentType.NoMedia
                mediaState.value = ClusterVariant.MediaPlayerState.nomedia
            }
        ) {
            Text("NoMedia")
        }
        Button(
            onClick = {
                Log.d("ClusterTest", "$name setting to Telephony (Call)")
                state.value = ClusterVariant.ComponentType.Call
                phoneState.value = ClusterVariant.PhoneCallState.incall
            }
        ) {
            Text("Telephony")
        }
        Button(
            onClick = {
                Log.d("ClusterTest", "$name setting to Maps (Navigation)")
                state.value = ClusterVariant.ComponentType.Navigation
            }
        ) {
            Text("Maps")
        }
        Button(
            onClick = {
                Log.d("ClusterTest", "$name setting to None")
                state.value = ClusterVariant.ComponentType.None
            }
        ) {
            Text("None")
        }
    }
}

@Composable
fun ClusterTest() {
    val panel1Component = remember { mutableStateOf(ClusterVariant.ComponentType.Media) }
    val panel2Component = remember { mutableStateOf(ClusterVariant.ComponentType.NoMedia) }
    val panel3Component = remember { mutableStateOf(ClusterVariant.ComponentType.Navigation) }
    val mediaPlayerState = remember { mutableStateOf(ClusterVariant.MediaPlayerState.loading) }
    val phoneState = remember { mutableStateOf(ClusterVariant.PhoneCallState.nocall) }

    Log.d(
        "ClusterTest",
        "Recomposing ClusterTest: panel1=${panel1Component.value}, panel2=${panel2Component.value}, panel3=${panel3Component.value}, media=${mediaPlayerState.value}, phone=${phoneState.value}",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 90.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            val scale = maxWidth.value / 1920f
            Box(
                modifier = Modifier.requiredSize(maxWidth, 720.dp * scale),
                contentAlignment = Alignment.TopCenter,
            ) {
                ClusterDoc.androidMain(
                    modifier =
                        Modifier.requiredSize(1920.dp, 720.dp).graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        },
                    panel1Component = panel1Component.value,
                    panel2Component = panel2Component.value,
                    panel3Component = panel3Component.value,
                    mediaPlayerState = mediaPlayerState.value,
                    phoneState = phoneState.value,
                    viewMode = ClusterVariant.ViewMode.normal,
                )
            }
        }
        Column(modifier = Modifier.padding(16.dp)) {
            PanelButtons("Panel 1", panel1Component, mediaPlayerState, phoneState)
            PanelButtons("Panel 2", panel2Component, mediaPlayerState, phoneState)
            PanelButtons("Panel 3", panel3Component, mediaPlayerState, phoneState)
        }
    }
}
