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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

@DesignDoc(id = "9g0jn7KXNloRmOZIGze2Rr")
interface CompositingViewsTest {
    @DesignComponent(node = "#MainFrame")
    fun MainFrame(
        @Design(node = "#Child1") child1: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#Child2") child2: @Composable (ComponentReplacementContext) -> Unit,
    )
}

@Composable
fun CompositingViewsTest() {
    val context = LocalContext.current
    val player = remember {
        val exoPlayer = ExoPlayer.Builder(context).build()
        val mediaItem =
            MediaItem.fromUri(
                "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            )
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()

        exoPlayer
    }

    var (counter, setCounter) = remember { mutableStateOf(0) }
    val color =
        when (counter % 5) {
            0 -> Color.Black
            1 -> Color.Blue
            2 -> Color.Red
            3 -> Color.Green
            4 -> Color.Cyan
            5 -> Color.Magenta
            else -> Color.Yellow
        }

    CompositingViewsTestDoc.MainFrame(
        child1 = {
            Box(Modifier.fillMaxSize().background(color).clickable { setCounter(counter + 1) })
        },
        child2 = {
            AndroidView(
                factory = { cx ->
                    val playerView = PlayerView(cx)
                    playerView.player = player
                    playerView
                },
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}
