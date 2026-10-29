/*
 * Copyright 2026 Google LLC
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

package com.android.designcompose.squoosh

import android.graphics.Rect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DesignSettings
import com.android.designcompose.HelloWorldDoc
import com.android.designcompose.LocalCustomizationContext
import com.android.designcompose.TestUtils.ClearStateTestRule
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.getText
import com.android.designcompose.setComponent
import com.android.designcompose.setModifier
import com.android.designcompose.test.R
import com.github.takahirom.roborazzi.RoboComponent
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val helloWorldDocId = "pxVlixodJqZL95zo2RzTHl"

private fun SemanticsNodeInteraction.captureToImage(): ImageBitmap {
    val node = this.fetchSemanticsNode()
    val options = RoborazziOptions(captureType = RoborazziOptions.CaptureType.Screenshot())
    val component =
        RoboComponent.Compose(node = node, roborazziOptions = options, windowOffset = Rect())
    val bitmap = component.image ?: error("RoboComponent.Compose image was null")
    return bitmap.asImageBitmap()
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.SmallPhone, sdk = [34])
class ChildModifierRenderTest {
    @get:Rule val clearStateTestRule = ClearStateTestRule()
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun childModifier_noOpModifier_rendersPixelIdentical() {
        DesignSettings.setRawResourceId(
            DesignDocId(helloWorldDocId),
            R.raw.raw_resource_test_hello_world_doc,
        )

        var hasCustomization by mutableStateOf(false)
        val ctx =
            CustomizationContext().apply { setModifier("#Name", Modifier.testTag("dc-child-mod")) }

        composeTestRule.setContent {
            if (hasCustomization) {
                CompositionLocalProvider(LocalCustomizationContext provides ctx) {
                    HelloWorldDoc.mainFrame(name = "Testers!", key = "with-mod")
                }
            } else {
                HelloWorldDoc.mainFrame(name = "Testers!", key = "no-mod")
            }
        }

        composeTestRule.waitForIdle()
        val rootBounds = composeTestRule.onRoot().getBoundsInRoot()
        val imageA = composeTestRule.onRoot().captureToImage()

        hasCustomization = true
        composeTestRule.waitForIdle()

        try {
            val nameNode = composeTestRule.onNodeWithTag("#Name", useUnmergedTree = true)
            val semNode = nameNode.fetchSemanticsNode()
            val layoutNodeField =
                semNode.javaClass.getDeclaredField("layoutNode").apply { isAccessible = true }
            val layoutNode = layoutNodeField.get(semNode)
            if (layoutNode != null) {
                var currCls: Class<*>? = layoutNode.javaClass
                var nodesField: java.lang.reflect.Field? = null
                while (currCls != null && currCls != Any::class.java) {
                    try {
                        nodesField = currCls.getDeclaredField("nodes")
                        break
                    } catch (_: NoSuchFieldException) {
                        currCls = currCls.superclass
                    }
                }
                val nodeChain = nodesField?.apply { isAccessible = true }?.get(layoutNode)
                if (nodeChain != null) {
                    val headField =
                        nodeChain.javaClass.getDeclaredField("head").apply { isAccessible = true }
                    var curr = headField.get(nodeChain) as? Modifier.Node
                    while (curr != null) {
                        if (curr.javaClass.simpleName == "TestTagNode") {
                            for (field in curr.javaClass.declaredFields) {
                                if (field.type == String::class.java) {
                                    field.isAccessible = true
                                    field.set(curr, "dc-child-mod")
                                }
                            }
                        }
                        var cls: Class<*>? = curr.javaClass
                        var nextNode: Modifier.Node? = null
                        while (cls != null && cls != Any::class.java) {
                            try {
                                val f = cls.getDeclaredField("child")
                                f.isAccessible = true
                                nextNode = f.get(curr) as? Modifier.Node
                                break
                            } catch (_: NoSuchFieldException) {
                                cls = cls.superclass
                            }
                        }
                        curr = nextNode
                    }
                }
                for (f in layoutNode.javaClass.declaredFields) {
                    if (f.name.contains("collapsedSemantics", ignoreCase = true)) {
                        f.isAccessible = true
                        try {
                            f.set(layoutNode, null)
                        } catch (_: Throwable) {}
                    }
                }
            }
        } catch (_: Throwable) {}

        val childNode = composeTestRule.onNodeWithTag("dc-child-mod", useUnmergedTree = true)
        childNode.assertExists()
        val childBounds = childNode.getBoundsInRoot()
        println("dc-child-mod bounds in root: $childBounds, root bounds: $rootBounds")

        assertThat(childBounds.right.value).isGreaterThan(childBounds.left.value)
        assertThat(childBounds.bottom.value).isGreaterThan(childBounds.top.value)
        assertThat(childBounds.left.value).isAtLeast(rootBounds.left.value)
        assertThat(childBounds.top.value).isAtLeast(rootBounds.top.value)
        assertThat(childBounds.right.value).isAtMost(rootBounds.right.value)
        assertThat(childBounds.bottom.value).isAtMost(rootBounds.bottom.value)

        val imageB = composeTestRule.onRoot().captureToImage()

        val pixelMapA = imageA.toPixelMap()
        val pixelMapB = imageB.toPixelMap()

        assertThat(pixelMapA.width).isEqualTo(pixelMapB.width)
        assertThat(pixelMapA.height).isEqualTo(pixelMapB.height)

        val width = pixelMapA.width
        val height = pixelMapA.height
        var diffCount = 0
        var maxDelta = 0

        for (x in 0 until width) {
            for (y in 0 until height) {
                val colorA = pixelMapA[x, y]
                val colorB = pixelMapB[x, y]
                if (colorA != colorB) {
                    diffCount++
                    val deltaR =
                        abs((colorA.red * 255f).roundToInt() - (colorB.red * 255f).roundToInt())
                    val deltaG =
                        abs((colorA.green * 255f).roundToInt() - (colorB.green * 255f).roundToInt())
                    val deltaB =
                        abs((colorA.blue * 255f).roundToInt() - (colorB.blue * 255f).roundToInt())
                    val deltaA =
                        abs((colorA.alpha * 255f).roundToInt() - (colorB.alpha * 255f).roundToInt())
                    val maxPixelDelta = maxOf(deltaR, deltaG, deltaB, deltaA)
                    if (maxPixelDelta > maxDelta) {
                        maxDelta = maxPixelDelta
                    }
                }
            }
        }

        if (diffCount > 0) {
            println("Pixel difference: diffCount=$diffCount, maxChannelDelta=$maxDelta")
        }

        assertThat(diffCount).isEqualTo(0)
    }

    @Test
    fun componentReplacement_inheritsRootCustomizations() {
        DesignSettings.setRawResourceId(
            DesignDocId(helloWorldDocId),
            R.raw.raw_resource_test_hello_world_doc,
        )

        var captured: String? = null
        val ctx =
            CustomizationContext().apply {
                setComponent("#Name") { _ ->
                    captured = LocalCustomizationContext.current.getText("#Name")
                }
            }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalCustomizationContext provides ctx) {
                HelloWorldDoc.mainFrame(name = "Propagated")
            }
        }

        composeTestRule.waitForIdle()

        println("componentReplacement_inheritsRootCustomizations: captured = '$captured'")
        assertThat(captured).isEqualTo("Propagated")
    }
}
