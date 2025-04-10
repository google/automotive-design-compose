/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.designcompose.testapp.clusterdemo

import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import com.google.android.filament.*
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.*
import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.GestureDetector
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.rotation
import com.google.android.filament.utils.scale
import com.google.android.filament.utils.translation
import com.google.android.filament.utils.transpose
import java.nio.Buffer
import kotlinx.coroutines.*

private const val kNearPlane = 0.01f // 1 cm
private const val kFarPlane = 10000.0f // 10 km
private const val kAperture = 16f
private const val kShutterSpeed = 1f / 125f
private const val kSensitivity = 100f

/** Derived from Filament's ModelViewer, but supporting multiple models in a scene. */
class CarVizViewer(val engine: Engine, private val uiHelper: UiHelper) :
    android.view.View.OnTouchListener {
    var egoCarAsset: FilamentAsset? = null
        private set

    var animator: Animator? = null
        private set

    @Suppress("unused")
    val progress
        get() = resourceLoader.asyncGetLoadProgress()

    var normalizeSkinningWeights = true
    var cameraFocalLength = 45f
        set(value) {
            field = value
            updateCameraProjection()
        }

    var cameraNear = kNearPlane
        set(value) {
            field = value
            updateCameraProjection()
        }

    var cameraFar = kFarPlane
        set(value) {
            field = value
            updateCameraProjection()
        }

    val scene: Scene
    val view: View
    val camera: Camera
    val renderer: Renderer
    @Entity val light: Int
    private lateinit var displayHelper: DisplayHelper
    private lateinit var cameraManipulator: Manipulator
    private lateinit var gestureDetector: GestureDetector
    private var surfaceView: SurfaceView? = null
    private var textureView: TextureView? = null
    private var fetchResourcesJob: Job? = null
    private var swapChain: SwapChain? = null
    private var assetLoader: AssetLoader
    private var materialProvider: MaterialProvider
    private var resourceLoader: ResourceLoader
    private val readyRenderables = IntArray(128) // add up to 128 entities at a time
    private val otherAssets: ArrayList<FilamentAsset> = arrayListOf()
    private val eyePos = DoubleArray(3)
    private val target = DoubleArray(3)
    private val upward = DoubleArray(3)

    init {
        renderer = engine.createRenderer()
        scene = engine.createScene()
        camera =
            engine.createCamera(engine.entityManager.create()).apply {
                setExposure(kAperture, kShutterSpeed, kSensitivity)
            }
        view = engine.createView()
        view.scene = scene
        view.camera = camera
        materialProvider = UbershaderProvider(engine)
        assetLoader = AssetLoader(engine, materialProvider, EntityManager.get())
        resourceLoader = ResourceLoader(engine, normalizeSkinningWeights)
        // Always add a direct light source since it is required for shadowing.
        // We highly recommend adding an indirect light as well.
        light = EntityManager.get().create()
        val (r, g, b) = Colors.cct(6_500.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(r, g, b)
            .intensity(100_000.0f)
            .direction(0.0f, -1.0f, 0.0f)
            .castShadows(true)
            .build(engine, light)
        scene.addEntity(light)
        eyePos[0] = 0.0
        eyePos[1] = 5.0
        eyePos[2] = 2.5
        target[0] = 0.0
        target[1] = 0.0
        target[2] = 2.55
        upward[0] = 0.0
        upward[1] = 1.0
        upward[2] = 0.0
    }

    constructor(
        surfaceView: SurfaceView,
        engine: Engine = Engine.create(),
        uiHelper: UiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK),
        manipulator: Manipulator? = null,
    ) : this(engine, uiHelper) {
        cameraManipulator =
            manipulator
                ?: Manipulator.Builder()
                    .targetPosition(
                        kDefaultObjectPosition.x,
                        kDefaultObjectPosition.y,
                        kDefaultObjectPosition.z,
                    )
                    .viewport(surfaceView.width, surfaceView.height)
                    .build(Manipulator.Mode.ORBIT)
        this.surfaceView = surfaceView
        gestureDetector = GestureDetector(surfaceView, cameraManipulator)
        displayHelper = DisplayHelper(surfaceView.context)
        uiHelper.renderCallback = SurfaceCallback()
        uiHelper.attachTo(surfaceView)
        addDetachListener(surfaceView)
    }

    @Suppress("unused")
    constructor(
        textureView: TextureView,
        engine: Engine = Engine.create(),
        uiHelper: UiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK),
        manipulator: Manipulator? = null,
    ) : this(engine, uiHelper) {
        cameraManipulator =
            manipulator
                ?: Manipulator.Builder()
                    .targetPosition(
                        kDefaultObjectPosition.x,
                        kDefaultObjectPosition.y,
                        kDefaultObjectPosition.z,
                    )
                    .viewport(textureView.width, textureView.height)
                    .build(Manipulator.Mode.ORBIT)
        this.textureView = textureView
        gestureDetector = GestureDetector(textureView, cameraManipulator)
        displayHelper = DisplayHelper(textureView.context)
        uiHelper.renderCallback = SurfaceCallback()
        uiHelper.attachTo(textureView)
        addDetachListener(textureView)
    }

    /** Loads a monolithic binary glTF and populates the Filament scene. */
    fun loadEgoCarGlb(buffer: Buffer): FilamentAsset? {
        destroyModel()
        egoCarAsset = assetLoader.createAsset(buffer)
        egoCarAsset?.let { asset ->
            resourceLoader.asyncBeginLoad(asset)
            animator = asset.instance.animator
            asset.releaseSourceData()
        }
        return egoCarAsset
    }

    /** Load a "generic car" asset -- need to refactor this API into a generic thing. */
    fun loadGlb(buffer: Buffer): FilamentAsset? {
        val model = assetLoader.createAsset(buffer)
        model?.let { asset ->
            resourceLoader.asyncBeginLoad(asset)
            asset.releaseSourceData() // maybe avoid this if we want instanced assets
            otherAssets.add(asset)
        }
        return model
    }

    /** Scales the singular asset and positions it in the scene. */
    fun positionAsset(
        asset: FilamentAsset?,
        desiredWidth: Float,
        lateralOffset: Float,
        groundOffset: Float,
        longitudinalOffset: Float,
    ) {
        if (asset == null) return
        val tm = engine.transformManager
        val modelHalfExtent = asset.boundingBox.halfExtent
        val modelCenter = asset.boundingBox.center
        val modelWidth = modelHalfExtent[0] * 2.0f
        val centerX = modelCenter[0]
        val translation =
            Float3(
                centerX,
                -(modelCenter[1] + modelHalfExtent[1]),
                2.0f * (modelCenter[2] + modelHalfExtent[2]),
            )
        val scale = desiredWidth / modelWidth
        val transform =
            translation(Float3(lateralOffset, groundOffset, longitudinalOffset)) *
                scale(Float3(scale)) *
                translation(translation) *
                rotation(Float3(0.0f, 1.0f, 0.0f), 0.0f)
        tm.setTransform(tm.getInstance(asset.root), transpose(transform).toFloatArray())
    }

    fun lookAt(
        eyeX: Double,
        eyeY: Double,
        eyeZ: Double,
        targetX: Double,
        targetY: Double,
        targetZ: Double,
        upX: Double,
        upY: Double,
        upZ: Double,
    ) {
        eyePos[0] = eyeX
        eyePos[1] = eyeY
        eyePos[2] = eyeZ
        target[0] = targetX
        target[1] = targetY
        target[2] = targetZ
        upward[0] = upX
        upward[1] = upY
        upward[2] = upZ
    }

    /** Frees all entities associated with the most recently-loaded model. */
    fun destroyModel() {
        fetchResourcesJob?.cancel()
        resourceLoader.asyncCancelLoad()
        resourceLoader.evictResourceData()
        egoCarAsset?.let { asset ->
            this.scene.removeEntities(asset.entities)
            assetLoader.destroyAsset(asset)
            this.egoCarAsset = null
            this.animator = null
        }
        for (otherAsset in otherAssets) {
            this.scene.removeEntities(otherAsset.entities)
            assetLoader.destroyAsset(otherAsset)
        }
        otherAssets.clear()
    }

    /**
     * Renders the model and updates the Filament camera.
     *
     * @param frameTimeNanos time in nanoseconds when the frame started being rendered, typically
     *   comes from {@link android.view.Choreographer.FrameCallback}
     */
    fun render(frameTimeNanos: Long) {
        if (!uiHelper.isReadyToRender) {
            return
        }
        // Allow the resource loader to finalize textures that have become ready.
        resourceLoader.asyncUpdateLoad()
        // Add renderable entities to the scene as they become ready.
        egoCarAsset?.let { populateScene(it) }
        for (otherAsset in otherAssets) {
            populateScene(otherAsset)
        }
        camera.lookAt(
            // eyePosition
            eyePos[0],
            eyePos[1],
            eyePos[2],
            // eyeTarget
            target[0],
            target[1],
            target[2],
            // camera up
            upward[0],
            upward[1],
            upward[2],
        )
        // Render the scene, unless the renderer wants to skip the frame.
        if (renderer.beginFrame(swapChain!!, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    fun setOpacity(asset: FilamentAsset, opacity: Float) {
        for (entity in asset.renderableEntities) {
            val instance = engine.renderableManager.getInstance(entity)
            val primitiveCount = engine.renderableManager.getPrimitiveCount(instance)
            for (idx in 0 until primitiveCount) {
                val material = engine.renderableManager.getMaterialInstanceAt(instance, idx)
                material.setParameter("baseColorFactor", opacity, opacity, opacity, opacity)
            }
        }
    }

    private fun populateScene(asset: FilamentAsset) {
        val rcm = engine.renderableManager
        var count = 0
        val popRenderables = {
            count = asset.popRenderables(readyRenderables)
            count != 0
        }
        while (popRenderables()) {
            for (i in 0 until count) {
                val ri = rcm.getInstance(readyRenderables[i])
                rcm.setScreenSpaceContactShadows(ri, true)
            }
            scene.addEntities(readyRenderables.take(count).toIntArray())
        }
        scene.addEntities(asset.lightEntities)
    }

    private fun addDetachListener(view: android.view.View) {
        view.addOnAttachStateChangeListener(
            object : android.view.View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: android.view.View) {}

                override fun onViewDetachedFromWindow(v: android.view.View) {
                    uiHelper.detach()
                    destroyModel()
                    assetLoader.destroy()
                    materialProvider.destroyMaterials()
                    materialProvider.destroy()
                    resourceLoader.destroy()
                    engine.destroyEntity(light)
                    engine.destroyRenderer(renderer)
                    engine.destroyView(this@CarVizViewer.view)
                    engine.destroyScene(scene)
                    engine.destroyCameraComponent(camera.entity)
                    EntityManager.get().destroy(camera.entity)
                    EntityManager.get().destroy(light)
                    engine.destroy()
                }
            }
        )
    }

    /** Handles a [MotionEvent] to enable one-finger orbit, two-finger pan, and pinch-to-zoom. */
    fun onTouchEvent(event: MotionEvent) {
        gestureDetector.onTouchEvent(event)
    }

    @SuppressWarnings("ClickableViewAccessibility")
    override fun onTouch(view: android.view.View, event: MotionEvent): Boolean {
        onTouchEvent(event)
        return true
    }

    private fun updateCameraProjection() {
        val width = view.viewport.width
        val height = view.viewport.height
        val aspect = width.toDouble() / height.toDouble()
        camera.setLensProjection(
            cameraFocalLength.toDouble(),
            aspect,
            cameraNear.toDouble(),
            cameraFar.toDouble(),
        )
    }

    inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
            swapChain?.let { engine.destroySwapChain(it) }
            swapChain = engine.createSwapChain(surface)
            surfaceView?.let { displayHelper.attach(renderer, it.display) }
            textureView?.let { displayHelper.attach(renderer, it.display) }
        }

        override fun onDetachedFromSurface() {
            displayHelper.detach()
            swapChain?.let {
                engine.destroySwapChain(it)
                engine.flushAndWait()
                swapChain = null
            }
        }

        override fun onResized(width: Int, height: Int) {
            view.viewport = Viewport(0, 0, width, height)
            cameraManipulator.setViewport(width, height)
            updateCameraProjection()
            synchronizePendingFrames(engine)
        }
    }

    private fun synchronizePendingFrames(engine: Engine) {
        // Wait for all pending frames to be processed before returning. This is to
        // avoid a race between the surface being resized before pending frames are
        // rendered into it.
        val fence = engine.createFence()
        fence.wait(Fence.Mode.FLUSH, Fence.WAIT_FOR_EVER)
        engine.destroyFence(fence)
    }

    companion object {
        private val kDefaultObjectPosition = Float3(0.0f, 0.0f, -4.0f)
    }
}
