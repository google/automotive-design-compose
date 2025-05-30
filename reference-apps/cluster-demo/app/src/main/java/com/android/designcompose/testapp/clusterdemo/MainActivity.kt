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

package com.android.designcompose.testapp.clusterdemo

import android.content.Context
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.DesignComposeCallbacks
import com.android.designcompose.DesignDocOverride
import com.android.designcompose.DesignDocSettings
import com.android.designcompose.DesignSettings
import com.android.designcompose.DesignVariableCollection
import com.android.designcompose.DesignVariableModeValues
import com.android.designcompose.LocalDesignDocSettings
import com.android.designcompose.Meter
import com.android.designcompose.MeterState
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.squoosh.SmartAnimateTransition
import java.io.IOException
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

enum class SpeedoDisplayState {
    park,
    speedo,
}

enum class ShiftState {
    P,
    R,
    N,
    D,
}

enum class CarView {
    parked,
    driving,
}

enum class ViewMode {
    normal,
    sport,
    charging,
    reverse,
}

enum class Notification {
    none,
    callanswer,
    callincoming,
    warning,
    emergency,
    autopilot,
    wiper,
}

enum class DriveDemoState {
    Parked,
    Accelerating,
    Driving1,
    Driving2,
}

enum class ChargeDemoState {
    NotCharging,
    Charging,
    ChargingDone,
}

enum class AndroidState {
    on,
    off,
}

enum class HarState {
    on,
    off,
}

enum class PhoneCallState {
    incall,
    incoming,
}

enum class Panel1 {
    welcome,
    speedo,
    nav,
}

enum class Panel2 {
    startroute,
    phone,
    media,
    power,
    nav,
}

enum class Panel3 {
    compass,
    speedo,
    nav,
    none,
}

enum class LightDarkMode {
    Default,
    Light,
    Dark,
}

enum class Theme(val themeName: String) {
    Main("material-theme"),
    Custom("custom-theme"),
}

private var mCamera: Camera? = null

const val TAG = "DriverUIDemo"
val DISPLAY_HEIGHT: Dp = 720.dp

const val rangeMax = 300F
const val batteryTempMax = 60F
const val powerMin = -200F
const val powerMax = 1000F
const val tempDegreesMin = 0F
const val tempDegreesMax = 120F
const val rpmMax = 10F
const val speedMax = 120F

const val layoutWidthMin = 300F
const val layoutWidthMax = 1500F

class CameraPreview(context: Context, private val camera: Camera) :
    SurfaceView(context), SurfaceHolder.Callback {
    companion object {
        const val TAG = "DriverUICamera"
    }

    private val mHolder: SurfaceHolder =
        holder.apply {
            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            addCallback(this@CameraPreview)
            // deprecated setting, but required on Android versions prior to 3.0
            setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        camera.apply {
            try {
                setPreviewDisplay(holder)
                startPreview()
            } catch (e: IOException) {
                Log.d(TAG, "Error setting camera preview: ${e.message}")
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        camera.stopPreview()
        camera.release()
        println("Camera Surface Destroyed")
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        if (mHolder.surface == null) {
            // preview surface does not exist
            return
        }

        // stop preview before making changes
        try {
            camera.stopPreview()
        } catch (e: Exception) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        camera.apply {
            try {
                setPreviewDisplay(mHolder)
                startPreview()
            } catch (e: Exception) {
                Log.d(TAG, "Error starting camera preview: ${e.message}")
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    fun getCameraInstance(): Camera? {
        return try {
            Camera.open() // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            println("Error: ${e.localizedMessage}")
            null // returns null if camera is unavailable
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mCamera = getCameraInstance()
        val cameraView = mCamera?.let { CameraPreview(this, it) }

        DesignSettings.enableLiveUpdates(this)
        DesignSettings.addFontFamily("Roboto", robotoFontFamily)
        DesignSettings.addFontFamily("Noto Sans", notoSansFontFamily)
        DesignSettings.addFontFamily("PT Sans Narrow", ptSansNarrowFontFamily)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent { MainFrame(cameraView) }
    }

    override fun onDestroy() {
        mCamera?.release()
        mCamera = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        window.setDecorFitsSystemWindows(false)
        val controller = window.insetsController
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

// Future Demo Branch od2Xm016aH1SvThcqFp0hE
// Galaxy V2 E82Od5Wfu2xuqVLyOjQV7Z
// Shiny V2 cJKbU0Kl8tBuU1bkIemt2I
// Circular 4C3C3GlbQfJk1acq8YjoJe
// Responsive Ac6itRStVMTTnJBuNsiDv7
@DesignDoc(id = "od2Xm016aH1SvThcqFp0hE", designFeatures = ["shader"])
interface Cluster {
    @DesignComponent(node = "#HAR-stage")
    fun HarMain(
        // Whether Android is on or off.
        @DesignVariant(property = "#heartbeat") androidState: AndroidState,

        // The current view mode
        @DesignVariant(property = "#view-mode") viewMode: ViewMode,

        // UI panels
        @DesignVariant(property = "#panel-1") panel1: Panel1,
        @DesignVariant(property = "#panel-2") panel2: Panel2,
        @DesignVariant(property = "#panel-3") panel3: Panel3,
        @DesignVariant(property = "#car-view") carView: CarView,

        // High priority notifications
        @DesignVariant(property = "#driving/notification") notification: Notification,

        // Shift state
        @DesignVariant(property = "#driving/shift-state") shiftState: ShiftState,

        // Phone call state
        @DesignVariant(property = "#phone/state") phoneState: PhoneCallState,

        // Weather
        @Design(node = "#weather/temp") tempValue: State<String>,

        // New design variants
        @DesignVariant(property = "#driving/speedo-state") speedoDisplayState: SpeedoDisplayState,

        // New car values
        @Design(node = "#driving/speed") speedMph: State<String>,
        @Design(node = "#driving/range") rangeMiles: State<String>,
        @Design(node = "#driving/power") powerWhMi: State<String>,
        @Design(node = "#driving/battery-percent") batteryPercent: State<String>,
        @Design(node = "#driving/battery-temp") batteryTemp: State<String>,
        @Design(node = "#driving/icon-power-minus") showPowerIcon: Boolean,
        @Design(node = "#driving/icon-power-plus") showRegenIcon: Boolean,

        // New dials gauges
        @Design(node = "#driving/speedo-gauge") speedoGauge: Meter,
        @Design(node = "#driving/battery-gauge") batteryGauge: Meter,
        @Design(node = "#driving/battery-temp-gauge") batteryTempGauge: Meter,
        @Design(node = "#driving/regen-gauge") regenGauge: Meter,
        @Design(node = "#driving/power-gauge") powerGauge: Meter,
        @Design(node = "#driving/rpm-gauge") rpmGauge: Meter,
        @Design(node = "#driving/rpm") rpm: State<String>,

        // Camera
        @Design(node = "#driving/camera") camera: @Composable (ComponentReplacementContext) -> Unit,

        // Telltales
        @Design(node = "#telltale/no-seatbelt") telltaleNoSeatbelt: Boolean,
        @Design(node = "#telltale/low-tire-pressure") telltaleLowTirePressure: Boolean,
        @Design(node = "#telltale/airbag") telltaleAirbag: Boolean,
        @Design(node = "#telltale/abs") telltaleAbs: Boolean,
        @Design(node = "#telltale/brake") telltaleBrake: Boolean,
        @Design(node = "#telltale/traction") telltaleTraction: Boolean,
        @Design(node = "#telltale/fog-lights") telltaleFogLights: Boolean,
        @Design(node = "#telltale/park-lights") telltaleParkLights: Boolean,
        @Design(node = "#telltale/hi-beam") telltaleHibeam: Boolean,
        @Design(node = "#telltale/low-beam") telltaleLowbeam: Boolean,
        @Design(node = "#telltale/park") parkBrake: Boolean,
        @Design(node = "#telltale/left-blinker") telltaleLeftBlinker: Boolean,
        @Design(node = "#telltale/right-blinker") telltaleRightBlinker: Boolean,
    )

    @DesignComponent(node = "#android-stage")
    fun AndroidMain(
        // The current view mode
        @DesignVariant(property = "#view-mode") viewMode: ViewMode,

        // UI panels
        @DesignVariant(property = "#panel-1") panel1: Panel1,
        @DesignVariant(property = "#panel-2") panel2: Panel2,
        @DesignVariant(property = "#panel-3") panel3: Panel3,
        @DesignVariant(property = "#car-view") carView: CarView,

        // High priority notifications
        @DesignVariant(property = "#driving/notification") notification: Notification,

        // Shift state
        @DesignVariant(property = "#driving/shift-state") shiftState: ShiftState,

        // Phone call state
        @DesignVariant(property = "#phone/state") phoneState: PhoneCallState,

        // Media progress
        @Design(node = "#media/progress") mediaProgress: MeterState,

        // Weather
        @Design(node = "#weather/temp") tempValue: State<String>,

        // New design variants
        @DesignVariant(property = "#driving/speedo-state") speedoDisplayState: SpeedoDisplayState,

        // New car values
        @Design(node = "#driving/speed") speedMph: State<String>,
        @Design(node = "#driving/range") rangeMiles: State<String>,
        @Design(node = "#driving/power") powerWhMi: State<String>,
        // @Design(node = "#driving/battery-percent") batteryPercent: String,
        @Design(node = "#driving/battery-temp") batteryTemp: State<String>,
        @Design(node = "#driving/icon-power-minus") showPowerIcon: Boolean,
        @Design(node = "#driving/icon-power-plus") showRegenIcon: Boolean,

        // New dials gauges
        @Design(node = "#driving/speedo-gauge") speedoGauge: Meter,
        @Design(node = "#driving/battery-gauge") batteryGauge: Meter,
        @Design(node = "#driving/battery-temp-gauge") batteryTempGauge: Meter,
        @Design(node = "#driving/regen-gauge") regenGauge: Meter,
        @Design(node = "#driving/power-gauge") powerGauge: Meter,
        @Design(node = "#driving/rpm-gauge") rpmGauge: Meter,
        @Design(node = "#driving/rpm") rpm: State<String>,

        // Maps
        @Design(node = "#driving/map-view") maps: @Composable (ComponentReplacementContext) -> Unit,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainFrame(cameraPreview: CameraPreview?) {
    // Telltales
    var allTelltales by remember { mutableStateOf(true) }
    var tpmsWarning by remember { mutableStateOf(true) }
    var stabilityFailure by remember { mutableStateOf(true) }
    var lightsSideLights by remember { mutableStateOf(true) }
    var brakesParking by remember { mutableStateOf(true) }
    var brakesABS by remember { mutableStateOf(true) }
    var brakesFailure by remember { mutableStateOf(true) }
    var lightsHighBeam by remember { mutableStateOf(true) }
    var lightsLowBeam by remember { mutableStateOf(true) }
    var lightsFog by remember { mutableStateOf(true) }
    var seatbelt by remember { mutableStateOf(true) }
    var driverAirbag by remember { mutableStateOf(true) }
    var parkBrake by remember { mutableStateOf(true) }

    // Car states
    val androidState = remember { mutableStateOf(AndroidState.on) }
    val harState = remember { mutableStateOf(HarState.on) }
    val panel1 = remember { mutableStateOf(Panel1.welcome) }
    val panel2 = remember { mutableStateOf(Panel2.startroute) }
    val panel3 = remember { mutableStateOf(Panel3.compass) }
    val phoneState = remember { mutableStateOf(PhoneCallState.incall) }

    val speedoDisplayState = remember { mutableStateOf(SpeedoDisplayState.park) }
    val shiftState = remember { mutableStateOf(ShiftState.P) }
    val carView = remember { mutableStateOf(CarView.parked) }
    val viewMode = remember { mutableStateOf(ViewMode.normal) }
    val notification = remember { mutableStateOf(Notification.none) }
    val speedo = remember { mutableStateOf(0F) }
    val rangeMiles = remember { mutableStateOf(rangeMax / 2F) }
    val batteryTemp = remember { mutableStateOf(batteryTempMax / 2F) }
    val batteryTempStringState = remember {
        derivedStateOf { batteryTemp.value.roundToInt().toString() }
    }
    val batteryTempGauge = batteryTemp.value / batteryTempMax * 100F
    val power = remember { mutableStateOf(0F) }
    val tempDegrees = remember { mutableFloatStateOf(71.0f) }
    val tempDegreesStringState = remember { derivedStateOf { "${tempDegrees.value.toInt()}F" } }
    val rpm = remember { mutableStateOf(5F) }
    val rpmState = remember { derivedStateOf { rpm.value.roundToInt().toString() } }
    val mediaProgress = remember { mutableFloatStateOf(100F) }

    // Demo animations
    var driveDemoState by remember { mutableStateOf(DriveDemoState.Parked) }
    val transitionDrive = updateTransition(driveDemoState, label = "Drive Demo Status")

    // Drive speed animation goes 0 to 65, then back and forth between 60 and 70
    val driveSpeedDemo by
        transitionDrive.animateFloat(
            transitionSpec = {
                when {
                    DriveDemoState.Parked isTransitioningTo DriveDemoState.Accelerating -> {
                        tween(durationMillis = 8000, easing = EaseInOutSine)
                    }
                    DriveDemoState.Accelerating isTransitioningTo DriveDemoState.Driving1 -> {
                        tween(durationMillis = 5000, easing = EaseInOutSine)
                    }
                    DriveDemoState.Driving1 isTransitioningTo DriveDemoState.Driving2 -> {
                        tween(durationMillis = 5000, easing = EaseInOutSine)
                    }
                    DriveDemoState.Driving2 isTransitioningTo DriveDemoState.Driving1 -> {
                        tween(durationMillis = 5000, easing = EaseInOutSine)
                    }
                    else -> {
                        snap()
                    }
                }
            },
            label = "",
        ) { state ->
            when (state) {
                DriveDemoState.Parked -> 0F
                DriveDemoState.Accelerating -> 65F
                DriveDemoState.Driving1 -> 60F
                DriveDemoState.Driving2 -> 70F
            }
        }
    val speedValue = if (driveDemoState == DriveDemoState.Parked) speedo.value else driveSpeedDemo

    val speedGauge = (speedValue / speedMax * 100F)

    val speedValueState = remember(speedValue) { mutableStateOf("${speedValue.toInt()}") }

    // Power animation goes from 0 to 600, then back and forth between 300 and -100
    val powerDemo by
        transitionDrive.animateFloat(
            transitionSpec = {
                when {
                    DriveDemoState.Parked isTransitioningTo DriveDemoState.Accelerating -> {
                        tween(durationMillis = 1000, easing = EaseInOutSine)
                    }
                    DriveDemoState.Accelerating isTransitioningTo DriveDemoState.Driving1 -> {
                        tween(durationMillis = 5000, easing = EaseInOutSine)
                    }
                    DriveDemoState.Driving1 isTransitioningTo DriveDemoState.Driving2 -> {
                        tween(durationMillis = 5000, easing = EaseInOutSine)
                    }
                    DriveDemoState.Driving2 isTransitioningTo DriveDemoState.Driving1 -> {
                        tween(durationMillis = 5000, easing = EaseInOutSine)
                    }
                    else -> {
                        snap()
                    }
                }
            },
            label = "",
        ) { state ->
            when (state) {
                DriveDemoState.Parked -> 0F
                DriveDemoState.Accelerating -> 600F
                DriveDemoState.Driving1 -> -100F
                DriveDemoState.Driving2 -> 300F
            }
        }
    val powerValue = if (driveDemoState == DriveDemoState.Parked) power.value else powerDemo
    val powerWhMi = remember { mutableStateOf(0) }
    val powerWhMiStringState = remember { derivedStateOf { powerWhMi.value.toString() } }
    val powerUpdateTime = remember { mutableStateOf(0) }
    LaunchedEffect(powerUpdateTime.value) {
        delay(1000)
        powerWhMi.value = powerValue.roundToInt().absoluteValue
        powerUpdateTime.value += 1
    }
    val powerGauge = if (powerValue < 0) 0F else powerValue / powerMax * 100F
    val regenGauge = if (powerValue > 0) 0F else powerValue / powerMin * 100F
    val showPowerIcon = powerValue > 0
    val showRegenIcon = powerValue < 0

    if (transitionDrive.targetState == transitionDrive.currentState)
        when (transitionDrive.currentState) {
            DriveDemoState.Accelerating -> driveDemoState = DriveDemoState.Driving1
            DriveDemoState.Driving1 -> driveDemoState = DriveDemoState.Driving2
            DriveDemoState.Driving2 -> driveDemoState = DriveDemoState.Driving1
            DriveDemoState.Parked -> {}
        }

    // Charge animation goes from 20 to 90% of max range
    var chargeDemoState by remember { mutableStateOf(ChargeDemoState.NotCharging) }
    val transitionCharge = updateTransition(chargeDemoState, label = "Charge Demo Status")
    val rangeChargingDemo by
        transitionCharge.animateFloat(
            transitionSpec = {
                when {
                    ChargeDemoState.Charging isTransitioningTo ChargeDemoState.ChargingDone -> {
                        tween(durationMillis = 20000, easing = EaseInOut)
                    }
                    else -> {
                        snap()
                    }
                }
            },
            label = "",
        ) { state ->
            when (state) {
                ChargeDemoState.NotCharging -> 20F
                ChargeDemoState.Charging -> 20F
                ChargeDemoState.ChargingDone -> rangeMax * 0.88F
            }
        }
    if (transitionCharge.targetState == transitionCharge.currentState)
        when (transitionCharge.currentState) {
            ChargeDemoState.Charging -> {
                chargeDemoState = ChargeDemoState.ChargingDone
            }
            else -> {}
        }
    val rangeValue =
        if (chargeDemoState == ChargeDemoState.NotCharging) rangeMiles.value else rangeChargingDemo
    val rangeValueState = remember(rangeValue) { mutableStateOf("${rangeValue.toInt()}") }
    val batteryGauge = rangeValue / rangeMax * 100F
    val batteryGaugeStringState =
        remember(batteryGauge) { mutableStateOf("${batteryGauge.toInt()}") }

    val rpmGauge = rpm.value / rpmMax * 100F

    val layoutWidth = remember { mutableStateOf(layoutWidthMin) }

    val context = LocalContext.current
    val player = remember {
        val exoPlayer = ExoPlayer.Builder(context).build()
        val mediaItem = MediaItem.fromUri("file:///android_asset/maps/maps.mp4")
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
        exoPlayer.prepare()
        exoPlayer.play()

        exoPlayer
    }

    val theme = remember { mutableStateOf(Theme.Main) }
    val mode = remember { mutableStateOf(LightDarkMode.Default) }
    val themeName = theme.value?.themeName
    val modeValues =
        if (themeName != null && mode.value != LightDarkMode.Default)
            hashMapOf(themeName to mode.value.name)
        else null

    val docId = remember { mutableStateOf("od2Xm016aH1SvThcqFp0hE") }
    CompositionLocalProvider(
        LocalDesignDocSettings provides
            DesignDocSettings(
                customVariantTransition = { context ->
                    SmartAnimateTransition(
                        tween(
                            durationMillis = (1f * 1000.0).roundToInt(),
                            easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f),
                        )
                    )
                }
            )
    ) {
        DesignDocOverride(docId = DesignDocId(docId.value, "")) {
            DesignVariableCollection(themeName) {
                DesignVariableModeValues(modeValues) {
                    if (androidState.value == AndroidState.on)
                        ClusterDoc.AndroidMain(
                            // backgroundBrush = backgroundBrush,
                            viewMode = viewMode.value,
                            panel1 = panel1.value,
                            panel2 = panel2.value,
                            panel3 = panel3.value,
                            carView = carView.value,
                            notification = notification.value,
                            shiftState = shiftState.value,
                            phoneState = phoneState.value,
                            mediaProgress = mediaProgress,
                            tempValue = tempDegreesStringState,
                            speedoDisplayState = speedoDisplayState.value,
                            speedMph = speedValueState,
                            rangeMiles = rangeValueState,
                            powerWhMi = powerWhMiStringState,
                            // batteryPercent = batteryGauge.roundToInt().toString(),
                            batteryTemp = batteryTempStringState,
                            showPowerIcon = showPowerIcon,
                            showRegenIcon = showRegenIcon,
                            speedoGauge = speedGauge,
                            batteryGauge = batteryGauge,
                            batteryTempGauge = batteryTempGauge,
                            regenGauge = regenGauge,
                            powerGauge = powerGauge,
                            rpmGauge = rpmGauge,
                            rpm = rpmState,
                            maps = {
                                AndroidView(
                                    factory = { cx ->
                                        val playerView = PlayerView(cx)
                                        playerView.player = player
                                        playerView
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            },
                            designComposeCallbacks =
                                DesignComposeCallbacks(
                                    docReadyCallback = { docId ->
                                        Log.i(TAG, "MainClusterActivity Ready!")
                                    },
                                    newDocDataCallback = { docId, content ->
                                        if (content != null) {
                                            Log.i(TAG, "$docId updated. size: ${content.size}")
                                        }
                                    },
                                ),
                        )
                    val mod =
                        if (docId.value == "Ac6itRStVMTTnJBuNsiDv7")
                            Modifier.width(layoutWidth.value.dp)
                        else Modifier
                    if (harState.value == HarState.on)
                        ClusterDoc.HarMain(
                            modifier = mod,
                            androidState = androidState.value,
                            viewMode = viewMode.value,
                            panel1 = panel1.value,
                            panel2 = panel2.value,
                            panel3 = panel3.value,
                            carView = carView.value,
                            notification = notification.value,
                            shiftState = shiftState.value,
                            phoneState = phoneState.value,
                            tempValue = tempDegreesStringState,
                            speedoDisplayState = speedoDisplayState.value,
                            speedMph = speedValueState,
                            rangeMiles = rangeValueState,
                            batteryPercent = batteryGaugeStringState,
                            powerWhMi = powerWhMiStringState,
                            batteryTemp = batteryTempStringState,
                            showPowerIcon = showPowerIcon,
                            showRegenIcon = showRegenIcon,
                            speedoGauge = speedGauge,
                            batteryGauge = batteryGauge,
                            batteryTempGauge = batteryTempGauge,
                            regenGauge = regenGauge,
                            powerGauge = powerGauge,
                            rpmGauge = rpmGauge,
                            rpm = rpmState,
                            camera = {
                                if (cameraPreview != null) {
                                    AndroidView(modifier = Modifier, factory = { cameraPreview })
                                }
                            },
                            // Telltales
                            telltaleNoSeatbelt = seatbelt,
                            telltaleLowTirePressure = tpmsWarning,
                            telltaleAirbag = driverAirbag,
                            telltaleAbs = brakesABS,
                            telltaleBrake = brakesFailure,
                            telltaleTraction = stabilityFailure,
                            telltaleFogLights = lightsFog,
                            telltaleParkLights = brakesParking,
                            telltaleHibeam = lightsHighBeam,
                            telltaleLowbeam = lightsLowBeam,
                            parkBrake = parkBrake,
                            telltaleLeftBlinker = lightsSideLights,
                            telltaleRightBlinker = lightsSideLights,
                            designComposeCallbacks =
                                DesignComposeCallbacks(
                                    docReadyCallback = { docId ->
                                        Log.i(TAG, "MainClusterActivity Ready!")
                                    },
                                    newDocDataCallback = { docId, content ->
                                        if (content != null) {
                                            Log.i(TAG, "$docId updated. size: ${content.size}")
                                        }
                                    },
                                ),
                        )
                }
            }
        }
    }

    /* Manual Controls */
    Column(
        modifier = Modifier.offset(10.dp, 800.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    driveDemoState = DriveDemoState.Parked
                    viewMode.value = ViewMode.normal
                    panel1.value = Panel1.welcome
                    panel2.value = Panel2.startroute
                    panel3.value = Panel3.compass
                    speedoDisplayState.value = SpeedoDisplayState.park
                    shiftState.value = ShiftState.P
                    carView.value = CarView.parked
                }
            ) {
                Text("Welcome")
            }
            Button(
                onClick = {
                    driveDemoState = DriveDemoState.Accelerating
                    if (viewMode.value == ViewMode.charging || viewMode.value == ViewMode.reverse)
                        viewMode.value = ViewMode.normal
                    panel1.value = Panel1.speedo
                    panel2.value = Panel2.power
                    panel3.value = Panel3.compass
                    speedoDisplayState.value = SpeedoDisplayState.speedo
                    shiftState.value = ShiftState.D
                    carView.value = CarView.driving
                }
            ) {
                Text("Start Drive")
            }
            Button(
                onClick = {
                    driveDemoState = DriveDemoState.Parked
                    speedoDisplayState.value = SpeedoDisplayState.park
                    shiftState.value = ShiftState.P
                    carView.value = CarView.parked
                    panel3.value = Panel3.none
                }
            ) {
                Text("Cancel Drive")
            }
            Button(
                onClick = {
                    viewMode.value = ViewMode.normal
                    panel2.value = Panel2.media
                }
            ) {
                Text("Media")
            }
            Button(
                onClick = {
                    viewMode.value = ViewMode.normal
                    panel2.value = Panel2.phone
                    phoneState.value = PhoneCallState.incoming
                }
            ) {
                Text("Incoming Phone")
            }
            Button(
                onClick = {
                    viewMode.value = ViewMode.normal
                    panel2.value = Panel2.phone
                    phoneState.value = PhoneCallState.incall
                }
            ) {
                Text("Answer Phone")
            }
            Button(onClick = { viewMode.value = ViewMode.sport }) { Text("Sport Mode") }
            Button(onClick = { viewMode.value = ViewMode.normal }) { Text("Normal Mode") }
            Button(onClick = { notification.value = Notification.warning }) { Text("Warning") }
            Button(onClick = { notification.value = Notification.none }) { Text("Warning Off") }
            Button(
                onClick = {
                    driveDemoState = DriveDemoState.Parked
                    chargeDemoState = ChargeDemoState.Charging
                    viewMode.value = ViewMode.charging
                    speedoDisplayState.value = SpeedoDisplayState.park
                    shiftState.value = ShiftState.P
                    carView.value = CarView.parked
                }
            ) {
                Text("Start Charge")
            }
            Button(onClick = { chargeDemoState = ChargeDemoState.NotCharging }) {
                Text("Stop Charge")
            }
            Button(
                onClick = {
                    driveDemoState = DriveDemoState.Parked
                    viewMode.value = ViewMode.normal
                    panel1.value = Panel1.welcome
                    panel2.value = Panel2.startroute
                    panel3.value = Panel3.compass
                    speedoDisplayState.value = SpeedoDisplayState.park
                    shiftState.value = ShiftState.P
                    carView.value = CarView.parked
                    // TODO update text to show updated deliveries, times, etc
                }
            ) {
                Text("Welcome 2")
            }
            Button(
                onClick = {
                    driveDemoState = DriveDemoState.Accelerating
                    if (viewMode.value == ViewMode.charging || viewMode.value == ViewMode.reverse)
                        viewMode.value = ViewMode.normal
                    panel3.value = Panel3.compass
                    speedoDisplayState.value = SpeedoDisplayState.speedo
                    shiftState.value = ShiftState.D
                    carView.value = CarView.driving
                    panel1.value = Panel1.nav
                    panel2.value = Panel2.nav
                    panel3.value = Panel3.speedo
                }
            ) {
                Text("Drive Nav")
            }
            Button(
                onClick = {
                    viewMode.value = ViewMode.reverse
                    driveDemoState = DriveDemoState.Parked
                    panel1.value = Panel1.speedo
                    speedoDisplayState.value = SpeedoDisplayState.speedo
                    shiftState.value = ShiftState.R
                    carView.value = CarView.driving
                    panel3.value = Panel3.none
                }
            ) {
                Text("Reverse")
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabelledToggle("Android On", androidState.value == AndroidState.on) {
                androidState.value = if (it) AndroidState.on else AndroidState.off
            }
            LabelledToggle("Har On", harState.value == HarState.on) {
                harState.value = if (it) HarState.on else HarState.off
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabelledDropDown("ViewMode:", viewMode.value, onStateChange = { viewMode.value = it })
            LabelledDropDown("Panel 1:", panel1.value, onStateChange = { panel1.value = it })
            LabelledDropDown("Panel 2:", panel2.value, onStateChange = { panel2.value = it })
            LabelledDropDown("Panel 3:", panel3.value, onStateChange = { panel3.value = it })
            LabelledDropDown(
                "Speedo State:",
                speedoDisplayState.value,
                onStateChange = { speedoDisplayState.value = it },
            )
            LabelledDropDown(
                "Shift State:",
                shiftState.value,
                onStateChange = { shiftState.value = it },
            )
            LabelledDropDown("CarView:", carView.value, onStateChange = { carView.value = it })
            LabelledDropDown(
                "Notification:",
                notification.value,
                onStateChange = { notification.value = it },
            )
            LabelledDropDown("Phone:", phoneState.value, onStateChange = { phoneState.value = it })
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SliderControl("Speed: ", speedo.value, onValueChange = { speedo.value = it }, speedMax)
            SliderControl(
                "Battery: ",
                rangeMiles.value,
                onValueChange = { rangeMiles.value = it },
                rangeMax,
            )
            SliderControl(
                "BatteryTemp: ",
                batteryTemp.value,
                onValueChange = { batteryTemp.value = it },
                batteryTempMax,
            )
            SliderControl(
                "Power:",
                power.value,
                onValueChange = { power.value = it },
                powerMax,
                powerMin,
            )
            SliderControl("RPM:", rpm.value, onValueChange = { rpm.value = it }, rpmMax)
            SliderControl(
                "Temp:",
                tempDegrees.value,
                onValueChange = { tempDegrees.value = it },
                tempDegreesMax,
                tempDegreesMin,
            )
            SliderControl(
                "Media:",
                mediaProgress.value!!,
                onValueChange = { mediaProgress.value = it },
                100F,
                0F,
            )
            SliderControl(
                "Width:",
                layoutWidth.value,
                onValueChange = { layoutWidth.value = it },
                layoutWidthMax,
                layoutWidthMin,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.background(Color.LightGray)) {
                LabelledToggle("All Telltales", allTelltales) {
                    allTelltales = it
                    tpmsWarning = it
                    stabilityFailure = it
                    brakesParking = it
                    brakesABS = it
                    brakesFailure = it
                    seatbelt = it
                    lightsHighBeam = it
                    lightsLowBeam = it
                    lightsSideLights = it
                    lightsFog = it
                    driverAirbag = it
                    parkBrake = it
                }
            }
            Spacer(modifier = Modifier.width(30.dp))
            LabelledToggle("TPMS", tpmsWarning) { tpmsWarning = it }
            LabelledToggle("Stability", stabilityFailure) { stabilityFailure = it }
            LabelledToggle("Parking Brake", brakesParking) { brakesParking = it }
            LabelledToggle("ABS", brakesABS) { brakesABS = it }
            LabelledToggle("Brake Failure", brakesFailure) { brakesFailure = it }
            LabelledToggle("Seatbelt", seatbelt) { seatbelt = it }
            LabelledToggle("High Beams", lightsHighBeam) { lightsHighBeam = it }
            LabelledToggle("Low Beams", lightsLowBeam) { lightsLowBeam = it }
            LabelledToggle("Side Lights", lightsSideLights) { lightsSideLights = it }
            LabelledToggle("Fog Lights", lightsFog) { lightsFog = it }
            LabelledToggle("Driver Airbag", driverAirbag) { driverAirbag = it }
            LabelledToggle("Park Brake", parkBrake) { parkBrake = it }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MODES")
            Button(onClick = { mode.value = LightDarkMode.Light }) { Text("Light") }
            Button(onClick = { mode.value = LightDarkMode.Dark }) { Text("Dark") }
            Button(onClick = { theme.value = Theme.Main }) { Text("Main") }
            Button(onClick = { theme.value = Theme.Custom }) { Text("Custom") }

            Text("DESIGNS")
            Button(onClick = { docId.value = "od2Xm016aH1SvThcqFp0hE" }) { Text("Future") }
            Button(onClick = { docId.value = "E82Od5Wfu2xuqVLyOjQV7Z" }) { Text("Galaxy") }
            Button(onClick = { docId.value = "cJKbU0Kl8tBuU1bkIemt2I" }) { Text("Shiny") }
            Button(onClick = { docId.value = "4C3C3GlbQfJk1acq8YjoJe" }) { Text("Circular") }
            Button(onClick = { docId.value = "Ac6itRStVMTTnJBuNsiDv7" }) { Text("Responsive") }
        }
    }
}
