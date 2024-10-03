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

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.DesignDocSettings
import com.android.designcompose.LocalDesignDocSettings
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant
import com.android.designcompose.squoosh.SmartAnimateTransition
import kotlin.math.roundToInt

enum class AnimType {
    Normal, // Everything animates immediately over 1 second
    OneByOne, // Each square has a different delay so they animate one by one
    DifferentDurations, // Each square has a different duration so they end at different times
}

enum class ChildAnimType {
    Normal, // Child animates with parent
    Early, // Child animates before parent, if parent has delay
    Late, // Child animates after parent
}

enum class SceneState {
    CenterAll,
    BeforeWelcome,
    Open,
    Closed,
}

@DesignDoc(id = "vJRf4zxY4QX4zzSSUd1nJ5")
interface VariantAnimationTimelineTest {
    @DesignComponent(node = "root/display_1")
    fun Main(@DesignVariant(property = "SceneState") sceneState: SceneState)
}

@Composable
fun VariantAnimationTimelineTest() {
    val animType = remember { mutableStateOf(AnimType.Normal) }
    val childAnimType = remember { mutableStateOf(ChildAnimType.Normal) }
    val sceneState = remember { mutableStateOf(SceneState.Closed) }
    val tNumRegex = Regex("T([1-8])")

    CompositionLocalProvider(
        LocalDesignDocSettings provides
            DesignDocSettings(
                useSquoosh = true,
                customVariantTransition = { context ->
                    val defaultEasing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
                    val regexMatch = tNumRegex.find(context.from?.name ?: "")
                    val tNum = regexMatch?.let { it.groupValues[1].toInt() }
                    if (
                        context.from?.name == "T3Rect" &&
                            childAnimType.value != ChildAnimType.Normal
                    ) {
                        val delayMillis =
                            if (childAnimType.value == ChildAnimType.Early) 0 else 1500
                        SmartAnimateTransition(
                            tween(durationMillis = 500, easing = defaultEasing),
                            delayMillis,
                        )
                    } else if (tNum != null) {
                        val durationMillis =
                            when (animType.value) {
                                AnimType.Normal -> 1000
                                AnimType.OneByOne -> 500
                                AnimType.DifferentDurations -> (0.25f * 1000f * tNum).roundToInt()
                            }
                        val delayMillis =
                            when (animType.value) {
                                AnimType.Normal -> 0
                                AnimType.OneByOne -> (0.25f * 1000f * tNum).roundToInt()
                                AnimType.DifferentDurations -> 0
                            }
                        SmartAnimateTransition(
                            tween(durationMillis = durationMillis, easing = defaultEasing),
                            delayMillis,
                        )
                    } else {
                        SmartAnimateTransition(
                            tween(
                                durationMillis = (1f * 1000.0).roundToInt(),
                                easing = defaultEasing,
                            ),
                            0,
                        )
                    }
                },
            )
    ) {
        VariantAnimationTimelineTestDoc.Main(sceneState = sceneState.value)
    }

    Column(Modifier.absoluteOffset(y = 750.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Animation Type:", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.TestButton(
                name = "Normal",
                tag = "Normal",
                selected = animType.value == AnimType.Normal,
            ) {
                animType.value = AnimType.Normal
            }
            com.android.designcompose.testapp.validation.TestButton(
                name = "OneByOne",
                tag = "OneByOne",
                selected = animType.value == AnimType.OneByOne,
            ) {
                animType.value = AnimType.OneByOne
            }
            com.android.designcompose.testapp.validation.TestButton(
                name = "DifferentDurations",
                tag = "DifferentDurations",
                selected = animType.value == AnimType.DifferentDurations,
            ) {
                animType.value = AnimType.DifferentDurations
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Child Animation Type:", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.TestButton(
                name = "Normal",
                tag = "ChildNormal",
                selected = childAnimType.value == ChildAnimType.Normal,
            ) {
                childAnimType.value = ChildAnimType.Normal
            }
            com.android.designcompose.testapp.validation.TestButton(
                name = "Early",
                tag = "ChildEarly",
                selected = childAnimType.value == ChildAnimType.Early,
            ) {
                childAnimType.value = ChildAnimType.Early
            }
            com.android.designcompose.testapp.validation.TestButton(
                name = "Late",
                tag = "ChildLate",
                selected = childAnimType.value == ChildAnimType.Late,
            ) {
                childAnimType.value = ChildAnimType.Late
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Variant:", fontSize = 30.sp, color = Color.Black)
            com.android.designcompose.testapp.validation.TestButton(
                name = "Columns",
                tag = "Columns",
                selected = sceneState.value == SceneState.Closed,
            ) {
                sceneState.value = SceneState.Closed
            }
            com.android.designcompose.testapp.validation.TestButton(
                name = "Spread",
                tag = "Spread",
                selected = sceneState.value == SceneState.BeforeWelcome,
            ) {
                sceneState.value = SceneState.BeforeWelcome
            }
            com.android.designcompose.testapp.validation.TestButton(
                name = "Center",
                tag = "Center",
                selected = sceneState.value == SceneState.CenterAll,
            ) {
                sceneState.value = SceneState.CenterAll
            }
            com.android.designcompose.testapp.validation.TestButton(
                name = "Cluster",
                tag = "Cluster",
                selected = sceneState.value == SceneState.Open,
            ) {
                sceneState.value = SceneState.Open
            }
        }
    }
}
