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
import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.utils.*
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
    var otherCar: FilamentAsset? = null
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
        val egoAsset = modelViewer.loadEgoCarGlb(egoModel)
        modelViewer.positionAsset(egoAsset, 2.2f, 0.0f, 0.0f, 0.0f)
        otherCar = modelViewer.loadGlb(context.assets.readToByteBuffer("carviz/generic-car.glb"))
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

private val Float3VectorConverter: TwoWayConverter<Float3, AnimationVector3D> =
    TwoWayConverter(
        convertToVector = { AnimationVector3D(it.x, it.y, it.z) },
        convertFromVector = { Float3(it.v1, it.v2, it.v3) },
    )

// Need drive rail state, too, not just gear. Parked with drive rail off is different from
// parked with drive rail on.
@Composable
fun CarVizPlaceholder(shiftState: ShiftState) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val transitionShiftState =
        updateTransition(targetState = shiftState, label = "CarViz Shift State")
    // Need to get this stuff into a plugin.
    val cameraEye by
        transitionShiftState.animateValue(
            typeConverter = Float3VectorConverter,
            label = "CarViz Eye Position",
            transitionSpec = { tween(durationMillis = 4000, easing = EaseInOutSine) },
        ) { targetShiftState ->
            when (targetShiftState) {
                ShiftState.P -> Float3(1.52f, 0.34f, 4.87f)
                ShiftState.R -> Float3(0.0f, 5.0f, 0.0f)
                ShiftState.N -> Float3(0.0f, 3.0f, 2.36f)
                ShiftState.D -> Float3(0.0f, 0.5f, 0.0f)
            }
        }
    val cameraTarget by
        transitionShiftState.animateValue(
            typeConverter = Float3VectorConverter,
            label = "CarViz Camera Target",
            transitionSpec = { tween(durationMillis = 4000, easing = EaseInOutSine) },
        ) { targetShiftState ->
            when (targetShiftState) {
                ShiftState.P -> Float3(-2.25f, -0.94f, 0.0f)
                ShiftState.R -> Float3(0.0f, -1.56f, 3.0f)
                ShiftState.N -> Float3(0.0f, -1.56f, 3.0f)
                ShiftState.D -> Float3(0.0f, -0.5f, 5.0f)
            }
        }
    AndroidView(
        factory = { context ->
            val filamentModelView = FilamentModelView(context)
            lifecycleOwner.lifecycle.addObserver(filamentModelView)
            filamentModelView
        },
        onRelease = { filamentModelView ->
            lifecycleOwner.lifecycle.removeObserver(filamentModelView)
        },
        update = { filamentModelView ->
            filamentModelView.modelViewer.lookAt(
                cameraEye.x.toDouble(),
                cameraEye.y.toDouble(),
                cameraEye.z.toDouble(),
                cameraTarget.x.toDouble(),
                cameraTarget.y.toDouble(),
                cameraTarget.z.toDouble(),
                0.0,
                1.0,
                0.0,
            )
        },
    )
}
