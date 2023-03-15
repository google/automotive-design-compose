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

package com.android.designcompose.reference.cluster

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.car.apps.common.imaging.ImageBinder
import com.android.car.media.common.MediaItemMetadata
import com.android.car.media.common.playback.PlaybackViewModel
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.DesignSettings
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant
import java.lang.Float.max
import java.lang.Float.min
import java.util.function.Consumer
import kotlin.math.roundToInt

val interFont =
    FontFamily(
        Font(R.font.inter_black, FontWeight.Black),
        Font(R.font.inter_blackitalic, FontWeight.Black, FontStyle.Italic),
        Font(R.font.inter_bold, FontWeight.Bold),
        Font(R.font.inter_bolditalic, FontWeight.Bold, FontStyle.Italic),
        Font(R.font.inter_extrabold, FontWeight.ExtraBold),
        Font(R.font.inter_extrabolditalic, FontWeight.ExtraBold, FontStyle.Italic),
        Font(R.font.inter_extralight, FontWeight.ExtraLight),
        Font(R.font.inter_extralightitalic, FontWeight.ExtraLight, FontStyle.Italic),
        Font(R.font.inter_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_mediumitalic, FontWeight.Medium, FontStyle.Italic),
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
        Font(R.font.inter_semibolditalic, FontWeight.SemiBold, FontStyle.Italic),
        Font(R.font.inter_thin, FontWeight.Thin),
        Font(R.font.inter_thinitalic, FontWeight.Thin, FontStyle.Italic),
    )

enum class PrndState {
    P,
    R,
    N,
    D,
}

enum class WeatherState {
    windy,
    snow,
    clear_sky_night,
    partly_cloudy_night,
    thunder,
    cloudy,
    rainy,
    clear_sky_day,
    partly_cloudy_day,
}

enum class BatteryState {
    full,
    low,
    verylow,
    unselected
}

enum class RegenState {
    on,
    off
}

enum class ChargingState {
    on,
    off
}

data class Controls(
    val shiftState: MutableState<PrndState>,
    val charging: MutableState<Boolean>,
    val batteryLevel: MutableState<Float>,
    val powerLevel: MutableState<Float>,
    val powerLevelMin: Float,
    val powerLevelMax: Float,
)

@DesignDoc(id = "GLJJrR1JI4HVEjL1qB40zq")
interface Cluster {
    @DesignComponent(node = "#Stage")
    fun ClusterMain(
        // Media
        @Design(node = "#cluster/media/title") mediaTitle: String,
        @Design(node = "#cluster/media/artist") mediaArtist: String,
        // @Design(node = "#cluster/media/source-icon") mediaSourceIcon: Bitmap?,
        @Design(node = "#cluster/media/album-art") mediaAlbumArt: Bitmap?,

        // Date time
        @Design(node = "#cluster/date") date: String,
        @Design(node = "#cluster/time") time: String,
        @Design(node = "#cluster/time-ampm") ampm: String,

        // Weather
        @Design(node = "#cluster/weather/temp") temp: String,

        // Battery
        @Design(node = "#cluster/battery")
        battery: @Composable (ComponentReplacementContext) -> Unit,

        // Power Level
        @Design(node = "#cluster/power-level")
        powerLevel: @Composable (ComponentReplacementContext) -> Unit,

        // Range
        @Design(node = "#cluster/range") range: String,
        @Design(node = "#cluster/range-units") rangeUnits: String,

        // Speedo
        @Design(node = "#cluster/speedo") speed: String,
        @Design(node = "#cluster/speedo-units") speedUnits: String,

        // Max speed?
        @Design(node = "#cluster/max-speed") showMaxSpeed: Boolean,
        @Design(node = "#cluster/max-speed/speed") maxSpeed: String,

        // Speed limit
        @Design(node = "#cluster/speed-limit") showSpeedLimit: Boolean,
        @Design(node = "#cluster/speed-limit/speed") speedLimit: String,

        // Alert
        @Design(node = "#cluster/alert") showAlert: Boolean,

        // Blinkers
        @Design(node = "#cluster/left-blinker") leftBlinker: Boolean,
        @Design(node = "#cluster/right-blinker") rightBlinker: Boolean,

        // Telltales
        @Design(node = "#cluster/telltale/no-seatbelt") tellTaleSeatbelt: Boolean,
        @Design(node = "#cluster/telltale/low-tire-pressure") tellTaleTirePressure: Boolean,
        @Design(node = "#cluster/telltale/airbag") tellTaleAirbag: Boolean,
        @Design(node = "#cluster/telltale/abs") tellTaleAbs: Boolean,
        @Design(node = "#cluster/telltale/brake") tellTaleBrake: Boolean,
        @Design(node = "#cluster/telltale/traction") tellTaleTracktion: Boolean,
        @Design(node = "#cluster/telltale/fog-lights") tellTaleFogLights: Boolean,
        @Design(node = "#cluster/telltale/park-lights") tellTaleParkLights: Boolean,
        @Design(node = "#cluster/telltale/hibeam") tellTaleHibeam: Boolean,
        @Design(node = "#cluster/telltale/lowbeam") tellTaleLowbeam: Boolean,

        // Various @DesignVariant properties to represent car states
        @DesignVariant(property = "#cluster/prnd") shiftState: PrndState,
        @DesignVariant(property = "#cluster/charging") chargingState: ChargingState,
        @DesignVariant(property = "#cluster/weather") weatherState: WeatherState,
        @DesignVariant(property = "#cluster/regen") regenState: RegenState,
        @DesignVariant(property = "#cluster/battery") batteryState: BatteryState,
    )
    @DesignComponent(node = "#cluster/battery")
    fun Battery(
        @DesignVariant(property = "#cluster/battery") state: BatteryState,
        @Design(node = "#charge_level") level: @Composable (ComponentReplacementContext) -> Unit,
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignSettings.enableLiveUpdates(this)
        DesignSettings.addFontFamily("Inter", interFont)

        setContent {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) { MainFrame() }
        }
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

    @Composable
    private fun MainFrame() {
        val shiftState = remember { mutableStateOf(PrndState.P) }
        val charging = remember { mutableStateOf(false) }
        val batteryLevel = remember { mutableStateOf(50.0f) }
        val powerLevel = remember { mutableStateOf(20.0f) }
        val regenState = if (powerLevel.value < 0f) RegenState.on else RegenState.off
        val controls = remember {
            Controls(shiftState, charging, batteryLevel, powerLevel, -30f, 100f)
        }

        val media = getMedia(application, applicationContext)
        val batteryPercent = batteryLevel.value / 100.0f
        val batteryState =
            when {
                batteryLevel.value <= 15 -> BatteryState.verylow
                powerLevel.value < 0f -> BatteryState.unselected
                batteryLevel.value <= 30 -> BatteryState.low
                else -> BatteryState.full
            }

        ClusterDoc.ClusterMain(
            modifier = Modifier.fillMaxWidth().height(720.dp),
            mediaTitle = media.title,
            mediaArtist = media.artist,
            mediaAlbumArt = media.albumArt,
            date = "DEC 16",
            time = "9:58",
            ampm = "AM",
            temp = "58°",
            battery = { context ->
                ClusterDoc.Battery(
                    modifier = context.layoutModifier.then(context.appearanceModifier),
                    state = batteryState
                ) { context ->
                    Column(Modifier.fillMaxWidth().fillMaxHeight(batteryPercent)) {
                        context.Content()
                    }
                }
            },
            powerLevel = { context ->
                PowerMeter(
                    context.layoutModifier,
                    controls.powerLevel.value,
                    controls.powerLevelMin,
                    controls.powerLevelMax
                )
            },
            range = "150",
            rangeUnits = "MI",
            speed = "70",
            speedUnits = "MPH",
            showMaxSpeed = true,
            maxSpeed = "65",
            showSpeedLimit = true,
            speedLimit = "65",
            showAlert = false,
            leftBlinker = true,
            rightBlinker = true,
            tellTaleSeatbelt = true,
            tellTaleTirePressure = false,
            tellTaleAirbag = true,
            tellTaleAbs = false,
            tellTaleBrake = true,
            tellTaleTracktion = false,
            tellTaleFogLights = true,
            tellTaleParkLights = false,
            tellTaleHibeam = true,
            tellTaleLowbeam = false,
            shiftState = shiftState.value,
            chargingState = if (controls.charging.value) ChargingState.on else ChargingState.off,
            weatherState = WeatherState.rainy,
            regenState = regenState,
            batteryState = batteryState,
        )
        Controls(controls)
    }

    @Composable
    private fun PowerMeter(
        layoutModifier: Modifier,
        powerLevel: Float,
        powerLevelMin: Float,
        powerLevelMax: Float
    ) {
        val sweepAngle =
            if (powerLevel > 0) 180f * powerLevel / powerLevelMax
            else 180f * powerLevel / -powerLevelMin
        val color = if (powerLevel > 0) Color(93, 240, 197, 156) else Color(236, 249, 133, 200)
        Canvas(
            modifier = layoutModifier,
            onDraw = {
                drawArc(
                    color = color,
                    startAngle = 90f,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
                drawCircle(
                    Color.White,
                    size.width / 2 - 40,
                    Offset(size.width / 2, size.height / 2)
                )
            }
        )
    }

    @Composable
    private fun Button(name: String, selected: Boolean, select: () -> Unit) {
        val textColor = if (selected) Color.White else Color.Gray
        val borderColor = if (selected) Color.White else Color.Black
        var modifier =
            Modifier.padding(10.dp)
                .clickable { select() }
                .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
                .absolutePadding(10.dp, 2.dp, 10.dp, 2.dp)

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier,
        ) {
            Text(name, fontSize = 30.sp, color = textColor)
        }
    }

    @Composable
    private fun Slider(value: MutableState<Float>, min: Float, max: Float) {
        val sliderMax = 400f
        val v = remember { mutableStateOf(sliderMax * (value.value - min) / (max - min)) }
        Box(
            modifier =
                Modifier.width(440.dp)
                    .height(40.dp)
                    .border(width = 2.dp, color = Color.Gray, shape = RoundedCornerShape(8.dp))
        ) {
            Box(
                modifier =
                    Modifier.offset { IntOffset(v.value.roundToInt() + 5, 5) }
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state =
                                rememberDraggableState { delta ->
                                    v.value = max(min(v.value + delta, sliderMax), 0f)
                                    value.value = min + (max - min) * v.value / sliderMax
                                }
                        )
                        .size(30.dp)
                        .border(
                            width = 25.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(5.dp)
                        )
            )
        }
    }

    @Composable
    private fun Controls(controls: Controls) {
        Box(Modifier.absoluteOffset(x = 10.dp, y = 730.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Shift State:   ", fontSize = 30.sp, color = Color.White)
                    Button("P", controls.shiftState.value == PrndState.P) {
                        controls.shiftState.value = PrndState.P
                    }
                    Button("R", controls.shiftState.value == PrndState.R) {
                        controls.shiftState.value = PrndState.R
                    }
                    Button("N", controls.shiftState.value == PrndState.N) {
                        controls.shiftState.value = PrndState.N
                    }
                    Button("D", controls.shiftState.value == PrndState.D) {
                        controls.shiftState.value = PrndState.D
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Charging:   ", fontSize = 30.sp, color = Color.White)
                    Button("On", controls.charging.value) { controls.charging.value = true }
                    Button("Off", !controls.charging.value) { controls.charging.value = false }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Battery:  ", fontSize = 30.sp, color = Color.White)
                    Box(modifier = Modifier.width(110.dp)) {
                        Text(
                            String.format("%.2f", controls.batteryLevel.value),
                            fontSize = 30.sp,
                            color = Color.White
                        )
                    }
                    Slider(controls.batteryLevel, 0f, 100f)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Power:  ", fontSize = 30.sp, color = Color.White)
                    Box(modifier = Modifier.width(110.dp)) {
                        Text(
                            String.format("%.2f", controls.powerLevel.value),
                            fontSize = 30.sp,
                            color = Color.White
                        )
                    }
                    Slider(controls.powerLevel, controls.powerLevelMin, controls.powerLevelMax)
                }
            }
        }
    }
}

internal class MediaData {
    var title: String = ""
    var artist: String = ""
    var albumArt: Bitmap? = null
}

const val MEDIA_SOURCE_MODE_PLAYBACK = 0

// Helper class that loads art for MediaItemMetadata. Create this with a callback
// that takes the loaded bitmap, then provide the artwork key by calling setImage().
private class MediaArtworkBinder(width: Int, height: Int, setBitmap: (b: Bitmap?) -> Unit) :
    ImageBinder<MediaItemMetadata.ArtworkRef?>(
        PlaceholderType.BACKGROUND,
        android.util.Size(width, height),
        Consumer { drawable: Drawable? ->
            if (drawable == null) {
                setBitmap(null)
            } else {
                val buffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val c = android.graphics.Canvas(buffer)
                drawable.setBounds(0, 0, width, height)
                drawable.draw(c)
                setBitmap(buffer)
            }
        }
    ) {}

@Composable
private fun getMedia(application: Application, context: Context): MediaData {
    val playbackViewModel: PlaybackViewModel = remember {
        PlaybackViewModel.get(application, MEDIA_SOURCE_MODE_PLAYBACK)
    }

    val media = MediaData()
    val metadata: MediaItemMetadata? by playbackViewModel!!.metadata.observeAsState()
    media.title = metadata?.title?.toString() ?: ""
    media.artist = metadata?.artist?.toString() ?: ""

    val (albumBitmap, setAlbumBitmap) = remember { mutableStateOf<Bitmap?>(null) }
    val (artworkKey, setArtworkKey) = remember { mutableStateOf(metadata?.artworkKey) }
    val newArtworkKey = metadata?.artworkKey
    if (artworkKey != newArtworkKey) {
        val albumArt = MediaArtworkBinder(350, 350) { b: Bitmap? -> setAlbumBitmap(b) }
        albumArt.setImage(context, newArtworkKey)
        setArtworkKey(newArtworkKey)
    }

    media.albumArt = albumBitmap
    return media
}

@Preview(widthDp = 1920)
@Composable
fun DefaultPreview() {
    val media = MediaData()
    ClusterDoc.ClusterMain(
        modifier = Modifier.fillMaxWidth().height(720.dp),
        mediaTitle = media.title,
        mediaArtist = media.artist,
        mediaAlbumArt = media.albumArt,
        date = "DEC 16",
        time = "9:58",
        ampm = "AM",
        temp = "58°",
        battery = {},
        powerLevel = {},
        range = "150",
        rangeUnits = "MI",
        speed = "70",
        speedUnits = "MPH",
        showMaxSpeed = true,
        maxSpeed = "65",
        showSpeedLimit = true,
        speedLimit = "65",
        showAlert = false,
        leftBlinker = true,
        rightBlinker = true,
        tellTaleSeatbelt = true,
        tellTaleTirePressure = false,
        tellTaleAirbag = true,
        tellTaleAbs = false,
        tellTaleBrake = true,
        tellTaleTracktion = false,
        tellTaleFogLights = true,
        tellTaleParkLights = false,
        tellTaleHibeam = true,
        tellTaleLowbeam = false,
        shiftState = PrndState.P,
        chargingState = ChargingState.off,
        weatherState = WeatherState.rainy,
        regenState = RegenState.off,
        batteryState = BatteryState.full,
    )
}
