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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.designcompose.ReplacementContent
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

@DesignDoc(id = "33jtmtH0zokPgbKpf0b9Xb")
interface LayoutReplacementSurfaceViewTest {
    @DesignComponent(node = "#stage")
    fun MainFrame(
        @Design(node = "#surfaceview_place_holder")
        surfaceViewPlaceHolder: ReplacementContent,
        @Design(node = "#surfaceview_place_holder_with_size")
        surfaceViewPlaceHolderWithSize: ReplacementContent,
    )
}

@Composable
fun LayoutReplacementSurfaceViewTest() {
    Row {
        // SurfaceView Outside the DesignCompose tree
        CircleDrawingWithSurfaceView(modifier = Modifier.fillMaxSize(0.2f))

        LayoutReplacementSurfaceViewTestDoc.MainFrame(
            modifier = Modifier.fillMaxSize(0.8f),
            // SurfaceView Inside the DesignCompose tree with ReplacementContent.
            surfaceViewPlaceHolder = ReplacementContent(
                count = 1,
                // SurfaceView should take the full size of replacing node.
                // This is not happening in DC 0.33 onwards, SurfaceView is not visible.
                content = { { CircleDrawingWithSurfaceView(modifier = Modifier.fillMaxSize()) } }
            ),
            surfaceViewPlaceHolderWithSize = ReplacementContent(
                count = 1,
                // Defining specific size(eg 100.dp) is showing the SurfaceView.
                content = { { CircleDrawingWithSurfaceView(modifier = Modifier.size(100.dp)) } }
            ),
        )
    }
}

@Composable
fun CircleDrawingWithSurfaceView(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                // Add a callback to know when the surface is ready
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        // It's now safe to draw
                        val canvas: Canvas? = holder.lockCanvas()
                        canvas?.let {
                            // Clear canvas
                            it.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

                            // Draw a red circle
                            val paint = Paint().apply {
                                color = Color.RED
                                isAntiAlias = true
                            }
                            val centerX = it.width / 2f
                            val centerY = it.height / 2f
                            val radius = it.width.coerceAtMost(it.height) / 4f
                            it.drawCircle(centerX, centerY, radius, paint)

                            holder.unlockCanvasAndPost(it)
                        }
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                    override fun surfaceDestroyed(holder: SurfaceHolder) {}
                })
            }
        },
        modifier = modifier
    )
}

