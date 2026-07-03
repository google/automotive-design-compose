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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.DesignVariableModeValues
import com.android.designcompose.DocRenderStatus
import com.android.designcompose.Meter
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant
import com.android.designcompose.sDocClass
import com.android.designcompose.sDocRenderStatus

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

    enum class WrapperVariant {
        Full,
        Thin,
    }
}

@DesignDoc(id = "CuF1b1eAIukB6YszX6B5OZ")
interface ClusterHARUI {
    @DesignComponent(node = "#HAR-stage")
    fun harMain(
        @Design(node = "#HAR-UI") harUi: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#HAR-UI-Wrapper")
        harUiWrapper: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#driving/speed") speed: String,
        @Design(node = "#driving/speedo-gauge") speedGauge: Meter,
        @Design(node = "#driving/needle") speedNeedle: Meter,
        @Design(node = "#driving/rpm") rpm: String,
        @Design(node = "#driving/speedo-gauge/rpm") rpmGauge: Meter,
        @Design(node = "#driving/rpm-needle") rpmNeedle: Meter,
        @Design(node = "#driving/speed-limit") speedLimit: String,
        @Design(node = "#driving/speed-limit-container") speedLimitDisplayed: Boolean,
        @Design(node = "#weather/temp") temp: String,
        @Design(node = "#driving/date") date: String,
        @Design(node = "#range-remaining") remainingRange: String,
        @Design(node = "#driving/battery-gauge") batteryLevel: String,
        @Design(node = "#telltale/no-seatbelt") seatbelt: Boolean,
        @Design(node = "#telltale/low-tire-pressure") lowTirePressure: Boolean,
        @Design(node = "#telltale/airbag") airbag: Boolean,
        @Design(node = "#telltale/abs") abs: Boolean,
        @Design(node = "#telltale/brake") brake: Boolean,
        @Design(node = "#telltale/traction") traction: Boolean,
        @Design(node = "#telltale/fog-lights") fogLights: Boolean,
        @Design(node = "#telltale/park-lights") parkLights: Boolean,
        @Design(node = "#telltale/hi-beam") hibeam: Boolean,
        @Design(node = "#telltale/low-beam") lowbeam: Boolean,
        @Design(node = "#telltale/left-blinker") turnSignalLeft: Boolean,
        @Design(node = "#telltale/right-blinker") turnSignalRight: Boolean,
        @Design(node = "#telltale/open-doors") openDoors: Boolean,
        @Design(node = "#telltale/open-trunk") openTrunk: Boolean,
    )

    @DesignComponent(node = "#HAR-UI")
    fun harUi(@DesignVariant(property = "#har-variant") harVariant: ClusterHARVariant.HarVariant)

    @DesignComponent(node = "#HAR-UI-Wrapper")
    fun harUiWrapper(
        @DesignVariant(property = "#wrapper-variant")
        wrapperVariant: ClusterHARVariant.WrapperVariant
    )
}

@Composable
fun RenderHarStage(
    modifier: Modifier,
    harVariant: ClusterHARVariant.HarVariant,
    wrapperVariant: ClusterHARVariant.WrapperVariant,
    speed: String,
    speedGauge: Float,
    speedNeedle: Float,
    rpm: String,
    rpmGauge: Float,
    rpmNeedle: Float,
    speedLimit: String,
    speedLimitDisplayed: Boolean,
    temp: String,
    date: String,
    remainingRange: String,
    batteryLevel: String,
    seatbelt: Boolean,
    lowTirePressure: Boolean,
    airbag: Boolean,
    abs: Boolean,
    brake: Boolean,
    traction: Boolean,
    fogLights: Boolean,
    parkLights: Boolean,
    hibeam: Boolean,
    lowbeam: Boolean,
    turnSignalLeft: Boolean,
    turnSignalRight: Boolean,
    openDoors: Boolean,
    openTrunk: Boolean,
) {
    ClusterHARUIDoc.harMain(
        modifier = modifier,
        harUi = { ctx ->
            ClusterHARUIDoc.harUi(modifier = ctx.layoutModifier, harVariant = harVariant)
        },
        harUiWrapper = { ctx ->
            ClusterHARUIDoc.harUiWrapper(
                modifier = ctx.layoutModifier,
                wrapperVariant = wrapperVariant,
            )
        },
        speed = speed,
        speedGauge = speedGauge,
        speedNeedle = speedNeedle,
        rpm = rpm,
        rpmGauge = rpmGauge,
        rpmNeedle = rpmNeedle,
        speedLimit = speedLimit,
        speedLimitDisplayed = speedLimitDisplayed,
        temp = temp,
        date = date,
        remainingRange = remainingRange,
        batteryLevel = batteryLevel,
        seatbelt = seatbelt,
        lowTirePressure = lowTirePressure,
        airbag = airbag,
        abs = abs,
        brake = brake,
        traction = traction,
        fogLights = fogLights,
        parkLights = parkLights,
        hibeam = hibeam,
        lowbeam = lowbeam,
        turnSignalLeft = turnSignalLeft,
        turnSignalRight = turnSignalRight,
        openDoors = openDoors,
        openTrunk = openTrunk,
    )
}

@Composable
fun ClusterHARTest() {
    val darkModes =
        hashMapOf(
            "material-theme" to "Dark",
            "custom-theme" to "Dark",
            "local-material-theme" to "Dark",
            "local-custom-theme" to "Dark",
        )

    DesignVariableModeValues(darkModes) {
        Column(
            modifier =
                Modifier.fillMaxSize().background(Color.Black).padding(10.dp).semantics {
                    sDocClass = ClusterHARUIDoc.javaClass.name
                    sDocRenderStatus = DocRenderStatus.Rendered
                },
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val scale = 0.6f

            // 1. Retro Variant
            Box(
                modifier = Modifier.requiredSize(1152.dp, 432.dp),
                contentAlignment = Alignment.Center,
            ) {
                RenderHarStage(
                    modifier =
                        Modifier.requiredSize(1920.dp, 720.dp).graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        },
                    harVariant = ClusterHARVariant.HarVariant.Retro,
                    wrapperVariant = ClusterHARVariant.WrapperVariant.Full,
                    speed = "60",
                    speedGauge = 60f,
                    speedNeedle = 60f,
                    rpm = "31",
                    rpmGauge = 31f,
                    rpmNeedle = 31f,
                    speedLimit = "65",
                    speedLimitDisplayed = true,
                    temp = "64°F",
                    date = "Thursday, Feb 27",
                    remainingRange = "89 MI",
                    batteryLevel = "85",
                    // telltales:
                    seatbelt = true,
                    lowTirePressure = true,
                    airbag = false,
                    abs = true,
                    brake = true,
                    traction = false,
                    fogLights = false,
                    parkLights = false,
                    hibeam = false,
                    lowbeam = false,
                    turnSignalLeft = true,
                    turnSignalRight = true,
                    openDoors = false,
                    openTrunk = false,
                )
            }

            // 2. Modern Camera Variant
            Box(
                modifier = Modifier.requiredSize(1152.dp, 432.dp),
                contentAlignment = Alignment.Center,
            ) {
                RenderHarStage(
                    modifier =
                        Modifier.requiredSize(1920.dp, 720.dp).graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        },
                    harVariant = ClusterHARVariant.HarVariant.ModernCamera,
                    wrapperVariant = ClusterHARVariant.WrapperVariant.Full,
                    speed = "84",
                    speedGauge = 84f,
                    speedNeedle = 84f,
                    rpm = "84",
                    rpmGauge = 84f,
                    rpmNeedle = 84f,
                    speedLimit = "55",
                    speedLimitDisplayed = true,
                    temp = "72°F",
                    date = "Friday, Feb 28",
                    remainingRange = "120 MI",
                    batteryLevel = "62",
                    // telltales:
                    seatbelt = true,
                    lowTirePressure = false,
                    airbag = true,
                    abs = false,
                    brake = true,
                    traction = false,
                    fogLights = true,
                    parkLights = false,
                    hibeam = true,
                    lowbeam = false,
                    turnSignalLeft = false,
                    turnSignalRight = true,
                    openDoors = false,
                    openTrunk = false,
                )
            }

            // 3. Compact Variant
            Box(
                modifier = Modifier.requiredSize(1152.dp, 432.dp),
                contentAlignment = Alignment.Center,
            ) {
                RenderHarStage(
                    modifier =
                        Modifier.requiredSize(1920.dp, 720.dp).graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        },
                    harVariant = ClusterHARVariant.HarVariant.Compact,
                    wrapperVariant = ClusterHARVariant.WrapperVariant.Full,
                    speed = "64",
                    speedGauge = 64f,
                    speedNeedle = 64f,
                    rpm = "0",
                    rpmGauge = 0f,
                    rpmNeedle = 0f,
                    speedLimit = "65",
                    speedLimitDisplayed = false,
                    temp = "68°F",
                    date = "Wednesday, Feb 26",
                    remainingRange = "250 MI",
                    batteryLevel = "90",
                    // telltales:
                    seatbelt = true,
                    lowTirePressure = true,
                    airbag = false,
                    abs = false,
                    brake = false,
                    traction = true,
                    fogLights = false,
                    parkLights = true,
                    hibeam = false,
                    lowbeam = true,
                    turnSignalLeft = true,
                    turnSignalRight = false,
                    openDoors = true,
                    openTrunk = false,
                )
            }
        }
    }
}
