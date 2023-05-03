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

package com.android.designcompose.tutorial

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.android.designcompose.DesignSettings
import com.android.designcompose.ImageReplacementContext
import com.android.designcompose.ListContent
import com.android.designcompose.ListContentData
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignContentTypes
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignPreviewContent
import com.android.designcompose.annotation.DesignVariant
import com.android.designcompose.annotation.PreviewNode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

val googleSansFont =
    FontFamily(
        Font(R.font.googlesans_bold, FontWeight.Bold, FontStyle.Normal),
        Font(R.font.googlesans_bolditalic, FontWeight.Bold, FontStyle.Italic),
        Font(R.font.googlesans_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.googlesans_medium, FontWeight.Medium, FontStyle.Normal),
        Font(R.font.googlesans_mediumitalic, FontWeight.Medium, FontStyle.Italic),
        Font(R.font.googlesans_regular, FontWeight.Normal, FontStyle.Normal),
    )

enum class PlayState {
    Play,
    Pause
}

enum class GridItemType {
    Square,
    Rect,
}

enum class ButtonState {
    normal,
    pressed,
}

@DesignDoc(id = "3z4xExq0INrL9vxPhj9tl7", version = "0.8")
interface Tutorial {
    @DesignComponent(node = "#stage", isRoot = true)
    fun Stage(
        @Design(node = "#live-time") liveTime: String,
        @Design(node = "#title") title: String,
        @Design(node = "#subtitle") subtitle: String,
        @Design(node = "#long-title") longTitle: String,
        @Design(node = "#photo") photo: @Composable (ImageReplacementContext) -> Bitmap?,
        @Design(node = "#icon") icon: @Composable (ImageReplacementContext) -> Bitmap?,
        @Design(node = "#timer") timer: String,
        @Design(node = "#buttontext") buttonText: String,
        @Design(node = "#boldtitle") boldTitle: String,
        @DesignVariant(property = "#media/now-playing/play-state-button") playState: PlayState,
        @Design(node = "#media/now-playing/play-state-button") onPlayPauseTap: TapCallback,
        @DesignContentTypes(nodes = ["#GridItem"])
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#GridItem=Rect"),
                    PreviewNode(3, "#GridItem=Square"),
                    PreviewNode(1, "#GridItem=Rect"),
                    PreviewNode(6, "#GridItem=Square"),
                    PreviewNode(1, "#GridItem=Rect"),
                    PreviewNode(6, "#GridItem=Square")
                ]
        )
        @Design(node = "#browse/grid/auto-content")
        gridContent: ListContent,
        @DesignContentTypes(nodes = ["#Track"])
        @DesignPreviewContent(
            name = "Track List",
            nodes =
                [
                    PreviewNode(8, "#Track"),
                ]
        )
        @Design(node = "#browse/tracklist/auto-content")
        trackContent: ListContent,
        @DesignContentTypes(nodes = ["#GridItem2"])
        @DesignPreviewContent(
            name = "Browse Outlined",
            nodes =
                [
                    PreviewNode(1, "#GridItem2=Rect"),
                    PreviewNode(3, "#GridItem2=Square"),
                    PreviewNode(1, "#GridItem2=Rect"),
                    PreviewNode(6, "#GridItem2=Square"),
                    PreviewNode(1, "#GridItem2=Rect"),
                    PreviewNode(6, "#GridItem2=Square")
                ]
        )
        @Design(node = "#browse/grid-outlined/auto-content")
        gridOutlinedContent: ListContent,
    )
    @DesignComponent(node = "#Track")
    fun Track(
        @Design(node = "#title") title: String,
    )
    @DesignComponent(node = "#GridItem")
    fun GridItem(@DesignVariant(property = "#GridItem") itemType: GridItemType)
    @DesignComponent(node = "#GridItem2")
    fun GridItem2(@DesignVariant(property = "#GridItem2") itemType: GridItemType)

    // Unused functions for document checker tutorial slide
    @DesignComponent(node = "#bluebutton") fun ButtonDuplicate()
    @DesignComponent(node = "#purplebutton") fun ButtonMissing()
    @DesignComponent(node = "#greenbutton")
    fun CustomButtonMissing(@Design(node = "#greenbuttontext") text: String)
    @DesignComponent(node = "#orangesubmitbutton")
    fun BadComponentSet(@DesignVariant(property = "#orangesubmitbutton") buttonState: ButtonState)
    @DesignComponent(node = "#redbutton")
    fun CustomButtonMismatch(@Design(node = "#redbuttontext") text: String)
}

var ALBUM_ART: Bitmap? = null
var VECTOR_DRAWABLE: Drawable? = null

@Composable
fun TutorialMain() {
    val (playState, setPlayState) = remember { mutableStateOf(PlayState.Play) }
    val (currentTime, setCurrentTime) = remember { mutableStateOf("") }
    val (timer, setTimer) = remember { mutableStateOf(0) }
    val timerStr = "%02d:%02d".format(timer / 60, timer % 60)

    LaunchedEffect(0) {
        while (true) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
            setCurrentTime(sdf.format(Date()))
            delay(1000)
        }
    }

    LaunchedEffect(playState) {
        var t = timer
        if (playState == PlayState.Pause) {
            while (true) {
                t = (t + 1) % 180
                setTimer(t)
                delay(1000)
            }
        }
    }

    TutorialDoc.Stage(
        modifier = Modifier.fillMaxSize(),
        liveTime = currentTime,
        title = "Reflection",
        subtitle = "Maximus Max",
        longTitle = "This long title can wrap many many lines or it can be cut off or elide",
        photo = { context ->
            var image = ALBUM_ART
            if (image != null) {
                val width = context.imageContext.getPixelWidth() ?: 300
                val height = context.imageContext.getPixelHeight() ?: 300
                image = Bitmap.createScaledBitmap(image, width, height, false)
            }
            image
        },
        icon = { context ->
            var vectorImage: Bitmap? = null
            if (VECTOR_DRAWABLE != null) {
                val drawable = VECTOR_DRAWABLE!!
                val color = context.imageContext.getBackgroundColor()
                val width = context.imageContext.getPixelWidth() ?: 300
                val height = context.imageContext.getPixelHeight() ?: 300
                vectorImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val c = Canvas(vectorImage)
                drawable!!.setBounds(0, 0, width, height)
                if (color != null) {
                    when (drawable) {
                        is ColorDrawable -> drawable.color = color
                        is VectorDrawable -> drawable.setTint(color)
                        is VectorDrawableCompat -> drawable.setTint(color)
                    }
                }
                drawable.draw(c)
            }
            vectorImage
        },
        timer = timerStr,
        buttonText = "Hooray!",
        boldTitle = "Congrats, you hooked up these components correctly",
        playState = playState,
        onPlayPauseTap = {
            setPlayState(if (playState == PlayState.Play) PlayState.Pause else PlayState.Play)
        },
        gridContent = { spanFunc ->
            val getItemType: (Int) -> GridItemType = { index ->
                if (index % 4 == 0) GridItemType.Rect else GridItemType.Square
            }
            ListContentData(
                count = 30,
                span = { index ->
                    spanFunc { TutorialDoc.GridItemDesignNodeData(getItemType(index)) }
                },
                key = { index -> index },
            ) { index ->
                TutorialDoc.GridItem(itemType = getItemType(index))
            }
        },
        trackContent = {
            ListContentData(count = 8) { index ->
                when (index) {
                    0 -> TutorialDoc.Track(title = "Reflection")
                    1 -> TutorialDoc.Track(title = "Selfies Everyday")
                    2 -> TutorialDoc.Track(title = "Who Can Look Away?")
                    3 -> TutorialDoc.Track(title = "Hey Girl")
                    4 -> TutorialDoc.Track(title = "Bye Girl")
                    5 -> TutorialDoc.Track(title = "I Don't Need a Filter")
                    6 -> TutorialDoc.Track(title = "I'm an Empath")
                    7 -> TutorialDoc.Track(title = "Don't Tell Me What To Do")
                }
            }
        },
        gridOutlinedContent = { spanFunc ->
            val getItemType: (Int) -> GridItemType = { index ->
                if (index % 4 == 0) GridItemType.Rect else GridItemType.Square
            }
            ListContentData(
                count = 30,
                span = { index ->
                    spanFunc { TutorialDoc.GridItem2DesignNodeData(getItemType(index)) }
                },
                key = { index -> index },
            ) { index ->
                TutorialDoc.GridItem2(itemType = getItemType(index))
            }
        },
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ALBUM_ART = BitmapFactory.decodeResource(resources, R.drawable.album_cover)
        VECTOR_DRAWABLE =
            VectorDrawableCompat.create(resources, R.drawable.ic_launcher_foreground, null)

        DesignSettings.enableLiveUpdates(this)
        DesignSettings.addFontFamily("GoogleSans", googleSansFont)

        setContent { TutorialMain() }
    }
}
