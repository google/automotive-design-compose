/*
 * Copyright 2025 Google LLC
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

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.DesignVariableCollection
import com.android.designcompose.DesignVariableModeValues
import com.android.designcompose.Meter
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant
import kotlin.math.roundToInt

object HvacVariant {
    const val MATERIAL_THEME_NAME = "material-theme"

    enum class DayNightMode {
        Light,
        Dark,
    }

    enum class BooleanState {
        True,
        False,
    }

    enum class Icon {
        ac_unit,
        power,
        seat_heat_left,
        seat_heat_right,
        fan,
        recirculate,
        windshield_defrost_front,
        windshield_defrost_rear,
        fan_defrost_low,
        fan_low,
        fan_mid_low,
        fan_mid,
        dot,
        sync_alt,
    }

    enum class Level {
        off,
        low,
        high,
    }
}

@DesignDoc(id = "YUU5qZIHRlSwXEbPPk3INh")
interface HvacComponents {
    @DesignComponent(node = "#stage")
    fun hvacPanel(
        @Design(node = "#hvac_on_off")
        hvacOnOff: @Composable ((ComponentReplacementContext) -> Unit),
        @Design(node = "#auto_temperature_on_off")
        autoOnOff: @Composable ((ComponentReplacementContext) -> Unit),
        @Design(node = "#cooling_on_off")
        coolingOnOff: @Composable ((ComponentReplacementContext) -> Unit),
        @Design(node = "#direction_face")
        directionFace: @Composable ((ComponentReplacementContext) -> Unit),
        @Design(node = "#direction_floor")
        directionFloor: @Composable ((ComponentReplacementContext) -> Unit),
        @Design(node = "#direction_defrost_front_and_floor")
        directionDefrostFrontFloor: @Composable ((ComponentReplacementContext) -> Unit),
        @Design(node = "#recycle_air_on_off")
        recycleAir: @Composable ((ComponentReplacementContext) -> Unit),
        @Design(node = "#direction_defrost_front")
        defrostFront: @Composable ((ComponentReplacementContext) -> Unit),
        @Design(node = "#direction_defrost_rear")
        defrostRear: @Composable ((ComponentReplacementContext) -> Unit),
        @Design(node = "#seat_heater_driver_on_off")
        seatHeaterDriver: @Composable ((ComponentReplacementContext) -> Unit),
        @Design(node = "#seat_heater_passenger_on_off")
        seatHeaterPassenger: @Composable ((ComponentReplacementContext) -> Unit),
        @Design(node = "#hvac_driver_passenger_sync")
        driverPassengerSync: @Composable ((ComponentReplacementContext) -> Unit),
        @Design(node = "#fan_speed_control")
        fanSpeedControl: @Composable ((ComponentReplacementContext) -> Unit),
    )

    @DesignComponent(node = "#ToggleButton")
    fun toggleButton(
        @DesignVariant(property = "#icon-on") iconOn: HvacVariant.BooleanState,
        @DesignVariant(property = "#icon-enabled") iconEnabled: HvacVariant.BooleanState,
        @DesignVariant(property = "#icon") icon: HvacVariant.Icon,
        @DesignVariant(property = "#toggle-button-on") buttonOn: HvacVariant.BooleanState,
        @DesignVariant(property = "#toggle-button-enabled") buttonEnabled: HvacVariant.BooleanState,
        @DesignVariant(property = "#toggle-button-pressed") buttonPressed: HvacVariant.BooleanState,
        @Design(node = "#ToggleButton") tapCallback: TapCallback,
        @Design(node = "#text") text: String?,
        @Design(node = "#text") textVisible: Boolean,
        @Design(node = "#ToggleButtonIcon") iconVisible: Boolean,
    )

    @DesignComponent(node = "#LevelButton")
    fun levelButton(
        @DesignVariant(property = "#level") level: HvacVariant.Level,
        @DesignVariant(property = "#level-button-enabled") buttonEnabled: HvacVariant.BooleanState,
        @DesignVariant(property = "#level-button-pressed") buttonPressed: HvacVariant.BooleanState,
        @Design(node = "#LevelButtonIcon")
        icon: @Composable ((ComponentReplacementContext) -> Unit),
        @Design(node = "#LevelButton") tapCallback: TapCallback,
    )

    @DesignComponent(node = "#IconWithState")
    fun iconWithState(
        @DesignVariant(property = "#icon-on") iconOn: HvacVariant.BooleanState,
        @DesignVariant(property = "#icon-enabled") iconEnabled: HvacVariant.BooleanState,
        @DesignVariant(property = "#icon") icon: HvacVariant.Icon,
    )

    @DesignComponent(node = "#FanSpeedControl")
    fun fanSpeedControl(
        @DesignVariant(property = "#fan-speed-control-enabled") enabled: HvacVariant.BooleanState,
        @Design(node = "#speed") speed: String,
        @Design(node = "#progress") progress: Meter,
        @Design(node = "#indicator") progressIndicator: Meter,
    )
}

fun isSystemInDarkMode(context: Context): Boolean {
    val currentNightMode =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES
}

@Composable
fun HvacPanel() {
    val hvacOnState = remember { mutableStateOf(false) }
    val autoOnState = remember { mutableStateOf(false) }
    val driverPassengerSyncState = remember { mutableStateOf(false) }
    val coolingOnState = remember { mutableStateOf(false) }
    val fanDirection = remember { mutableIntStateOf(0) }
    val recycleAirState = remember { mutableStateOf(false) }
    val defrostFrontState = remember { mutableStateOf(false) }
    val defrostRearState = remember { mutableStateOf(false) }
    val seatHeaterDriverState = remember { mutableIntStateOf(0) }
    val seatHeaterPassengerState = remember { mutableIntStateOf(0) }
    val fanSpeedState = remember { mutableFloatStateOf(100f) }

    val isSystemInDarkMode = isSystemInDarkMode(LocalContext.current)
    val dayNightMode =
        if (isSystemInDarkMode) HvacVariant.DayNightMode.Dark else HvacVariant.DayNightMode.Light

    val modeValues = hashMapOf(Pair(HvacVariant.MATERIAL_THEME_NAME, dayNightMode.name))
    DesignVariableCollection(HvacVariant.MATERIAL_THEME_NAME) {
        DesignVariableModeValues(modeValues) {
            HvacComponentsDoc.hvacPanel(
                hvacOnOff = {
                    val on =
                        if (hvacOnState.value) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    val enabled = HvacVariant.BooleanState.True
                    HvacComponentsDoc.toggleButton(
                        iconOn = on,
                        iconEnabled = enabled,
                        buttonOn = on,
                        buttonEnabled = enabled,
                        buttonPressed = HvacVariant.BooleanState.False,
                        text = null,
                        textVisible = false,
                        iconVisible = true,
                        icon = HvacVariant.Icon.power,
                        tapCallback = { hvacOnState.value = !hvacOnState.value },
                        key = "#hvac_on_off",
                    )
                },
                autoOnOff = {
                    val on =
                        if (autoOnState.value) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    val enabled =
                        if (hvacOnState.value) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    HvacComponentsDoc.toggleButton(
                        iconOn = on,
                        iconEnabled = enabled,
                        buttonOn = on,
                        buttonEnabled = enabled,
                        buttonPressed = HvacVariant.BooleanState.False,
                        text = "AUTO",
                        textVisible = true,
                        iconVisible = false,
                        icon = HvacVariant.Icon.ac_unit, // Could be anything
                        tapCallback = {
                            if (enabled == HvacVariant.BooleanState.True)
                                autoOnState.value = !autoOnState.value
                        },
                        key = "#auto_on_off",
                    )
                },
                coolingOnOff = {
                    val on =
                        if (coolingOnState.value) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    val enabled =
                        if (hvacOnState.value && !autoOnState.value) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    HvacComponentsDoc.toggleButton(
                        iconOn = on,
                        iconEnabled = enabled,
                        buttonOn = on,
                        buttonEnabled = enabled,
                        buttonPressed = HvacVariant.BooleanState.False,
                        text = null,
                        textVisible = false,
                        iconVisible = true,
                        icon = HvacVariant.Icon.ac_unit,
                        tapCallback = {
                            if (enabled == HvacVariant.BooleanState.True)
                                coolingOnState.value = !coolingOnState.value
                        },
                        key = "#cooling_on_off",
                    )
                },
                directionFace = {
                    val on =
                        if (fanDirection.intValue == 0) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    val enabled =
                        if (hvacOnState.value && !autoOnState.value) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    HvacComponentsDoc.toggleButton(
                        iconOn = on,
                        iconEnabled = enabled,
                        buttonOn = on,
                        buttonEnabled = enabled,
                        buttonPressed = HvacVariant.BooleanState.False,
                        text = null,
                        textVisible = false,
                        iconVisible = true,
                        icon = HvacVariant.Icon.fan_mid,
                        tapCallback = {
                            if (enabled == HvacVariant.BooleanState.True) fanDirection.intValue = 0
                        },
                        key = "#direction_face",
                    )
                },
                directionFloor = {
                    val on =
                        if (fanDirection.intValue == 1) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    val enabled =
                        if (hvacOnState.value && !autoOnState.value) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    HvacComponentsDoc.toggleButton(
                        iconOn = on,
                        iconEnabled = enabled,
                        buttonOn = on,
                        buttonEnabled = enabled,
                        buttonPressed = HvacVariant.BooleanState.False,
                        text = null,
                        textVisible = false,
                        iconVisible = true,
                        icon = HvacVariant.Icon.fan_low,
                        tapCallback = {
                            if (enabled == HvacVariant.BooleanState.True) fanDirection.intValue = 1
                        },
                        key = "#direction_floor",
                    )
                },
                directionDefrostFrontFloor = {
                    val on =
                        if (fanDirection.intValue == 2) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    val enabled =
                        if (hvacOnState.value && !autoOnState.value) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    HvacComponentsDoc.toggleButton(
                        iconOn = on,
                        iconEnabled = enabled,
                        buttonOn = on,
                        buttonEnabled = enabled,
                        buttonPressed = HvacVariant.BooleanState.False,
                        text = null,
                        textVisible = false,
                        iconVisible = true,
                        icon = HvacVariant.Icon.fan_defrost_low,
                        tapCallback = {
                            if (enabled == HvacVariant.BooleanState.True) fanDirection.intValue = 2
                        },
                        key = "#direction_defrost_front_and_floor",
                    )
                },
                recycleAir = {
                    val on =
                        if (recycleAirState.value) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    val enabled =
                        if (hvacOnState.value && !autoOnState.value) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    HvacComponentsDoc.toggleButton(
                        iconOn = on,
                        iconEnabled = enabled,
                        buttonOn = on,
                        buttonEnabled = enabled,
                        buttonPressed = HvacVariant.BooleanState.False,
                        text = null,
                        textVisible = false,
                        iconVisible = true,
                        icon = HvacVariant.Icon.recirculate,
                        tapCallback = {
                            if (enabled == HvacVariant.BooleanState.True)
                                recycleAirState.value = !recycleAirState.value
                        },
                        key = "#recycle_air_on_off",
                    )
                },
                defrostFront = {
                    val on =
                        if (defrostFrontState.value) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    val enabled = HvacVariant.BooleanState.True
                    HvacComponentsDoc.toggleButton(
                        iconOn = on,
                        iconEnabled = enabled,
                        buttonOn = on,
                        buttonEnabled = enabled,
                        buttonPressed = HvacVariant.BooleanState.False,
                        text = null,
                        textVisible = false,
                        iconVisible = true,
                        icon = HvacVariant.Icon.windshield_defrost_front,
                        tapCallback = { defrostFrontState.value = !defrostFrontState.value },
                        key = "#direction_defrost_front",
                    )
                },
                defrostRear = {
                    val on =
                        if (defrostRearState.value) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    val enabled = HvacVariant.BooleanState.True
                    HvacComponentsDoc.toggleButton(
                        iconOn = on,
                        iconEnabled = enabled,
                        buttonOn = on,
                        buttonEnabled = enabled,
                        buttonPressed = HvacVariant.BooleanState.False,
                        text = null,
                        textVisible = false,
                        iconVisible = true,
                        icon = HvacVariant.Icon.windshield_defrost_rear,
                        tapCallback = { defrostRearState.value = !defrostRearState.value },
                        key = "#direction_defrost_rear",
                    )
                },
                seatHeaterDriver = {
                    val enabled = HvacVariant.BooleanState.True
                    HvacComponentsDoc.levelButton(
                        level = HvacVariant.Level.entries[seatHeaterDriverState.intValue % 3],
                        buttonEnabled = enabled,
                        buttonPressed = HvacVariant.BooleanState.False,
                        icon = {
                            HvacComponentsDoc.iconWithState(
                                iconOn =
                                    if (seatHeaterDriverState.intValue == 0)
                                        HvacVariant.BooleanState.False
                                    else HvacVariant.BooleanState.True,
                                iconEnabled = enabled,
                                icon = HvacVariant.Icon.seat_heat_left,
                            )
                        },
                        tapCallback = {
                            seatHeaterDriverState.intValue =
                                (seatHeaterDriverState.intValue + 1) % 3
                        },
                        key = "#seat_heater_driver_on_off",
                    )
                },
                seatHeaterPassenger = {
                    val enabled = HvacVariant.BooleanState.True
                    HvacComponentsDoc.levelButton(
                        level = HvacVariant.Level.entries[seatHeaterPassengerState.intValue % 3],
                        buttonEnabled = enabled,
                        buttonPressed = HvacVariant.BooleanState.False,
                        icon = {
                            HvacComponentsDoc.iconWithState(
                                iconOn =
                                    if (seatHeaterPassengerState.intValue == 0)
                                        HvacVariant.BooleanState.False
                                    else HvacVariant.BooleanState.True,
                                iconEnabled = enabled,
                                icon = HvacVariant.Icon.seat_heat_right,
                            )
                        },
                        tapCallback = {
                            seatHeaterPassengerState.intValue =
                                (seatHeaterPassengerState.intValue + 1) % 3
                        },
                        key = "#seat_heater_passenger_on_off",
                    )
                },
                driverPassengerSync = {
                    val on =
                        if (driverPassengerSyncState.value) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    val enabled =
                        if (hvacOnState.value) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    HvacComponentsDoc.toggleButton(
                        iconOn = on,
                        iconEnabled = enabled,
                        buttonOn = on,
                        buttonEnabled = enabled,
                        buttonPressed = HvacVariant.BooleanState.False,
                        text = null,
                        textVisible = false,
                        iconVisible = true,
                        icon = HvacVariant.Icon.sync_alt,
                        tapCallback = {
                            if (enabled == HvacVariant.BooleanState.True)
                                driverPassengerSyncState.value = !driverPassengerSyncState.value
                        },
                        key = "#hvac_driver_passenger_sync",
                    )
                },
                fanSpeedControl = {
                    val enabled =
                        if (hvacOnState.value && !autoOnState.value) HvacVariant.BooleanState.True
                        else HvacVariant.BooleanState.False
                    val step = 100f / 6f
                    val speed = (fanSpeedState.floatValue / step).roundToInt()
                    val progress = speed * step
                    HvacComponentsDoc.fanSpeedControl(
                        enabled = enabled,
                        speed = (speed + 1).toString(),
                        progress = progress,
                        progressIndicator = progress,
                        key = "#fan_speed_control",
                    )
                },
            )
        }
    }

    // TODO: support interaction
    Row(
        Modifier.absoluteOffset(0.dp, 400.dp).height(50.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Slider(fanSpeedState, 0f, 100f, "progress-bar")
    }
}
