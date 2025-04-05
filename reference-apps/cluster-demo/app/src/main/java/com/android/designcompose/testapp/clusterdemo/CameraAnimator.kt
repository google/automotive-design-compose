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

import android.util.Log
import android.view.Choreographer
import androidx.compose.animation.core.EaseInOutCubic
import com.google.android.filament.Camera
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.utils.Mat4

data class ReferenceCamera(val camera: Camera) {
    val modelView: DecomposedTransform = Mat4.of(camera.getModelMatrix(FloatArray(16))).decompose()
}

class CameraAnimator(scene: FilamentAsset, mainCamera: Camera) {
    val referenceCameras = HashMap<ShiftState, ReferenceCamera>()
    var defaultCamera: ReferenceCamera
    private var currentModelView: DecomposedTransform

    // - pull out all of the cameras and decompose them into ReferenceCameras
    // - decide the default camera and set that to the scene
    // - add animation of the camera between states; just simple lerp, no momentum on position.
    //   - really the camera should be a physical object that models momentum.
    init {
        defaultCamera = ReferenceCamera(mainCamera)
        currentModelView = defaultCamera.modelView
        // First extract all of the cameras from the scene, and figure out which ones relate
        // to different shift states.
        for (cameraEntity in scene.cameraEntities) {
            val name = scene.getName(cameraEntity)

            // Only consider shift state cameras.
            if (!name.startsWith("#ShiftState=")) {
                Log.d(TAG, "Ignoring camera: $name")
                continue
            }
            val camera = scene.engine.getCameraComponent(cameraEntity) ?: continue
            val referenceCam = ReferenceCamera(camera)
            when (name) {
                "#ShiftState=P" -> {
                    referenceCameras[ShiftState.P] = referenceCam
                    defaultCamera = referenceCam
                    currentModelView = referenceCam.modelView
                }
                "#ShiftState=R" -> referenceCameras[ShiftState.R] = referenceCam
                "#ShiftState=N" -> referenceCameras[ShiftState.N] = referenceCam
                "#ShiftState=D" -> referenceCameras[ShiftState.D] = referenceCam
            }
            Log.d(TAG, "Extracted camera: $name")
        }
        if (referenceCameras.size == 0) {
            Log.e(TAG, "No cameras found in gltf")
        }
    }

    var currentAnimStart: Long = 0
    var currentAnimSource: DecomposedTransform? = null
    var currentAnimTarget: DecomposedTransform? = null

    fun setShiftState(shiftState: ShiftState) {
        // Start an animation to the new position from the current one.
        Choreographer.getInstance().postVsyncCallback { frameData ->
            // If we're already targeting or at this camera position, then do nothing.
            // XXX: We never update this, and I think it's probably wrong.
            // if (this.shiftState == shiftState) return@postVsyncCallback

            // If we can't get a camera for the target, then use the default.
            val target = referenceCameras[shiftState] ?: defaultCamera

            currentAnimStart = frameData.frameTimeNanos
            currentAnimSource = currentModelView
            currentAnimTarget = target.modelView.clone()
        }
    }

    private val ANIM_DURATION_NS = 3 * 1e9 // 1sec

    fun getModelView(frameTimeNanos: Long): Mat4 {
        val animSource = currentAnimSource
        val animTarget = currentAnimTarget
        val animDeltaTime = frameTimeNanos - currentAnimStart

        if (animDeltaTime < 0 || currentAnimStart == 0L || animSource == null || animTarget == null)
            return currentModelView.recompose()
        val animDelta =
            EaseInOutCubic.transform(
                (animDeltaTime / ANIM_DURATION_NS.toFloat()).coerceIn(0.0f, 1.0f)
            )
        val modelView = animSource.slerp(animTarget, animDelta)
        currentModelView = modelView
        return modelView.recompose()
    }
}
