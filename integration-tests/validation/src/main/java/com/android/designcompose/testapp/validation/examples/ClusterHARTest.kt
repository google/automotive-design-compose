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

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant

object ClusterHARVariant {
    enum class HarVariant {
        Modern,
        ModernCamera,
        Retro,
        RetroCamera,
        Compact,
        CompactCamera,
        Thin,
        ThinCamera,
        Medium,
        MediumCamera,
        Maps,
        MapsCamera,
    }
}

@DesignDoc(id = "CuF1b1eAIukB6YszX6B5OZ")
interface ClusterHARUI {
    @DesignComponent(node = "#HAR-stage")
    fun harMain(@DesignVariant(property = "#har-variant") harVariant: ClusterHARVariant.HarVariant)
}

@Composable
fun ClusterHARTest() {
    val harVariant = remember { mutableStateOf(ClusterHARVariant.HarVariant.Modern) }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val scale = maxWidth.value / 1920f
            Box(
                modifier = Modifier.requiredSize(maxWidth, 720.dp * scale),
                contentAlignment = Alignment.Center,
            ) {
                ClusterHARUIDoc.harMain(
                    modifier =
                        Modifier.requiredSize(1920.dp, 720.dp).graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        },
                    harVariant = harVariant.value,
                )
            }
        }
        Column(
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.8f))
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Theme:")
                Button(onClick = { harVariant.value = ClusterHARVariant.HarVariant.Modern }) {
                    Text("Modern")
                }
                Button(onClick = { harVariant.value = ClusterHARVariant.HarVariant.ModernCamera }) {
                    Text("Modern Cam")
                }
                Button(onClick = { harVariant.value = ClusterHARVariant.HarVariant.Retro }) {
                    Text("Retro")
                }
                Button(onClick = { harVariant.value = ClusterHARVariant.HarVariant.RetroCamera }) {
                    Text("Retro Cam")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Size:")
                Button(onClick = { harVariant.value = ClusterHARVariant.HarVariant.Compact }) {
                    Text("Compact")
                }
                Button(
                    onClick = { harVariant.value = ClusterHARVariant.HarVariant.CompactCamera }
                ) {
                    Text("Compact Cam")
                }
                Button(onClick = { harVariant.value = ClusterHARVariant.HarVariant.Thin }) {
                    Text("Thin")
                }
                Button(onClick = { harVariant.value = ClusterHARVariant.HarVariant.ThinCamera }) {
                    Text("Thin Cam")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Other:")
                Button(onClick = { harVariant.value = ClusterHARVariant.HarVariant.Medium }) {
                    Text("Medium")
                }
                Button(onClick = { harVariant.value = ClusterHARVariant.HarVariant.MediumCamera }) {
                    Text("Medium Cam")
                }
                Button(onClick = { harVariant.value = ClusterHARVariant.HarVariant.Maps }) {
                    Text("Maps")
                }
                Button(onClick = { harVariant.value = ClusterHARVariant.HarVariant.MapsCamera }) {
                    Text("Maps Cam")
                }
            }
        }
    }
}
