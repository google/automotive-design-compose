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

package com.android.designcompose.testapp.clusterdemo

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Choreographer
import android.view.SurfaceView
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import kotlin.math.sin

private fun AssetManager.readToByteBuffer(name: String): ByteBuffer {
    val stream = open(name)
    val bytes = ByteArray(stream.available())
    stream.read(bytes)
    return ByteBuffer.wrap(bytes)
}

internal class ColorView(context: Context) : View(context) {
    init {
        setBackgroundColor(Color.GREEN)
    }
}

internal class FilamentModelView(context: Context) : SurfaceView(context), LifecycleEventObserver {
    companion object {
        init {
            Utils.init()
        }
    }

    var firstFrameNanos: Long = -1
    val choreographer = Choreographer.getInstance()
    val modelViewer = CarVizViewer(this)
    val cameraAnimator: CameraAnimator
    var otherCar: FilamentAsset? = null
    var egoCar: FilamentAsset? = null

    private val frameCallback: Choreographer.FrameCallback =
        object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (firstFrameNanos == -1L) {
                    firstFrameNanos = frameTimeNanos
                }
                val deltaNanos = frameTimeNanos - firstFrameNanos
                val deltaSeconds = deltaNanos / 1000000000f

                val otherCar = otherCar
                if (otherCar != null) {
                    modelViewer.positionAsset(
                        otherCar,
                        2.2f,
                        0.0f,
                        -0.15f,
                        (sin(deltaSeconds) + 1.0f) * 1.5f + 3.0f,
                    )
                }

                val egoCar = egoCar
                if (egoCar != null) {
                    val animCount = egoCar.instance.animator.animationCount
                    for (i in 0..<animCount) {

                        val duration = egoCar.instance.animator.getAnimationDuration(i)
                        val progress = deltaSeconds % (duration * 2.0f)
                        egoCar.instance.animator.applyAnimation(
                            i,
                            if (progress < duration) {
                                progress
                            } else {
                                duration - (progress - duration)
                            },
                        )
                    }
                }

                choreographer.postFrameCallback(this)
                modelViewer.render(frameTimeNanos)
            }
        }

    init {
        setOnTouchListener(modelViewer)

        // This is wrong -- I want the SurfaceView underneath, but I can't figure out how to
        // make it clear to white if I make it opaque. The other alternative is to use a
        // TextureView, but then I see some new performance issues.
        setZOrderOnTop(true)
        setBackgroundColor(Color.TRANSPARENT)
        holder.setFormat(PixelFormat.TRANSLUCENT)

        modelViewer.view.blendMode = com.google.android.filament.View.BlendMode.TRANSLUCENT
        modelViewer.scene.skybox = null

        val clearOptions = modelViewer.renderer.clearOptions
        clearOptions.clear = true
        modelViewer.renderer.clearOptions = clearOptions

        val egoModel = context.assets.readToByteBuffer("carviz/google_car.glb")
        egoCar = modelViewer.loadEgoCarGlb(egoModel)

        val egoCar = egoCar
        if (egoCar != null && false) {
            val duration = egoCar.instance.animator.getAnimationDuration(2)
            val progress = 2.0f % duration
            egoCar.instance.animator.applyAnimation(2, progress)
        }

        modelViewer.positionAsset(egoCar, 2.2f, 0.0f, 0.0f, 0.0f)

        modelViewer.lookAt(
            // eyePosition
            0.0,
            4.0,
            2.5,
            // eyeTarget
            0.0,
            0.0,
            2.55,
            // camera up
            0.0,
            1.0,
            0.0,
        )

        cameraAnimator = CameraAnimator(egoCar!!, modelViewer.camera)
        modelViewer.cameraAnimator = cameraAnimator

        otherCar = modelViewer.loadGlb(context.assets.readToByteBuffer("carviz/generic-car.glb"))

        val lighting = context.assets.readToByteBuffer("carviz/default_env_ibl.ktx")
        KTX1Loader.createIndirectLight(modelViewer.engine, lighting).apply {
            intensity = 50_000f
            modelViewer.scene.indirectLight = this
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_PAUSE -> choreographer.removeFrameCallback(frameCallback)
            Lifecycle.Event.ON_DESTROY -> choreographer.removeFrameCallback(frameCallback)
            Lifecycle.Event.ON_RESUME -> choreographer.postFrameCallback(frameCallback)
            else -> {}
        }
        firstFrameNanos = -1
    }
}

// Need drive rail state, too, not just gear. Parked with drive rail off is different from
// parked with drive rail on.
@Composable
fun CarVizPlaceholder(modifier: Modifier, shiftState: ShiftState, widthScale: Float) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier.onGloballyPositioned {},
        factory = { context ->
            val filamentModelView = FilamentModelView(context)
            lifecycleOwner.lifecycle.addObserver(filamentModelView)
            filamentModelView
        },
        onRelease = { filamentModelView ->
            lifecycleOwner.lifecycle.removeObserver(filamentModelView)
        },
        update = { filamentModelView -> filamentModelView.cameraAnimator.setShiftState(shiftState) },
    )
}
