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

import com.android.designcompose.definition.element.Background
import com.android.designcompose.definition.element.Color
import com.android.designcompose.definition.element.ColorOrVar
import com.android.designcompose.definition.element.ShaderData
import com.android.designcompose.definition.element.Stroke
import com.android.designcompose.definition.element.StrokeAlign
import com.android.designcompose.definition.element.StrokeWeight
import com.android.designcompose.definition.view.nodeStyle
import com.android.designcompose.definition.view.viewStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SquooshAnimateStyleTest {

    private fun solidBackground(r: Int, g: Int, b: Int, a: Int, opacity: Float = 1.0f): Background {
        val color = Color.newBuilder().setR(r).setG(g).setB(b).setA(a).build()
        val solid = ColorOrVar.newBuilder().setColor(color).build()
        return Background.newBuilder().setSolid(solid).setOpacity(opacity).build()
    }

    private fun varBoundBackground(varId: String): Background {
        val colorVar = ColorOrVar.ColorVar.newBuilder().setId(varId).build()
        val solid = ColorOrVar.newBuilder().setVar(colorVar).build()
        return Background.newBuilder().setSolid(solid).setOpacity(1.0f).build()
    }

    private fun linearGradientBackground(): Background {
        val linear = Background.LinearGradient.newBuilder().build()
        return Background.newBuilder().setLinearGradient(linear).setOpacity(1.0f).build()
    }

    private fun imageBackground(): Background {
        val image = Background.Image.newBuilder().setKey("image-key").build()
        return Background.newBuilder().setImage(image).setOpacity(1.0f).build()
    }

    private fun stroke(
        weight: Float,
        strokes: List<Background>,
        align: StrokeAlign = StrokeAlign.STROKE_ALIGN_INSIDE,
        shader: ShaderData? = null,
    ): Stroke {
        val builder =
            Stroke.newBuilder()
                .setStrokeAlign(align)
                .setStrokeWeight(StrokeWeight.newBuilder().setUniform(weight).build())
                .addAllStrokes(strokes)
        if (shader != null) {
            builder.setShaderData(shader)
        }
        return builder.build()
    }

    @Test
    fun testCanInterpolatePaints() {
        val solidRed = solidBackground(255, 0, 0, 255)
        val solidGreen = solidBackground(0, 255, 0, 255)
        val solidBlue = solidBackground(0, 0, 255, 255)

        // True for solid <-> solid same count
        assertThat(canInterpolatePaints(listOf(solidRed), listOf(solidGreen))).isTrue()
        assertThat(
                canInterpolatePaints(listOf(solidRed, solidGreen), listOf(solidGreen, solidBlue))
            )
            .isTrue()

        // False for count mismatch
        assertThat(canInterpolatePaints(listOf(solidRed), listOf(solidRed, solidGreen))).isFalse()
        assertThat(canInterpolatePaints(listOf(solidRed, solidGreen), listOf(solidRed))).isFalse()

        // False for gradient paints
        val gradient = linearGradientBackground()
        assertThat(canInterpolatePaints(listOf(solidRed), listOf(gradient))).isFalse()
        assertThat(canInterpolatePaints(listOf(gradient), listOf(solidRed))).isFalse()

        // False for image paints
        val image = imageBackground()
        assertThat(canInterpolatePaints(listOf(solidRed), listOf(image))).isFalse()
        assertThat(canInterpolatePaints(listOf(image), listOf(solidRed))).isFalse()

        // False for var-bound colors (solid.hasVar())
        val varBound = varBoundBackground("color-var-1")
        assertThat(canInterpolatePaints(listOf(solidRed), listOf(varBound))).isFalse()
        assertThat(canInterpolatePaints(listOf(varBound), listOf(solidRed))).isFalse()
    }

    @Test
    fun testCanInterpolateStyle() {
        val redPaint = solidBackground(255, 0, 0, 255)
        val greenPaint = solidBackground(0, 255, 0, 255)

        val stroke1 = stroke(2.0f, listOf(redPaint))
        val stroke2DifferentWeight = stroke(4.0f, listOf(redPaint))
        val stroke2DifferentColor = stroke(2.0f, listOf(greenPaint))

        val styleWithStroke1 = viewStyle {
            nodeStyle = nodeStyle {
                stroke = stroke1
                backgrounds.add(redPaint)
            }
        }

        // True when only solid colors differ (backgrounds and stroke colors)
        val styleWithDifferentColors = viewStyle {
            nodeStyle = nodeStyle {
                stroke = stroke2DifferentColor
                backgrounds.add(greenPaint)
            }
        }
        assertThat(canInterpolateStyle(styleWithStroke1, styleWithDifferentColors)).isTrue()

        // False when stroke weight differs
        val styleWithDifferentWeight = viewStyle {
            nodeStyle = nodeStyle {
                stroke = stroke2DifferentWeight
                backgrounds.add(redPaint)
            }
        }
        assertThat(canInterpolateStyle(styleWithStroke1, styleWithDifferentWeight)).isFalse()

        // False when only one side has strokes
        val styleWithoutStroke = viewStyle { nodeStyle = nodeStyle { backgrounds.add(redPaint) } }
        assertThat(canInterpolateStyle(styleWithStroke1, styleWithoutStroke)).isFalse()
        assertThat(canInterpolateStyle(styleWithoutStroke, styleWithStroke1)).isFalse()
    }

    @Test
    fun testLerpSolidPaints() {
        val fromPaint = solidBackground(100, 50, 0, 200, opacity = 0.2f)
        val toPaint = solidBackground(200, 150, 100, 100, opacity = 0.8f)

        val fromList = listOf(fromPaint)
        val toList = listOf(toPaint)

        // At t = 0, equals from colors and opacity
        val at0 = lerpSolidPaints(fromList, toList, 0.0f)
        assertThat(at0).hasSize(1)
        assertThat(at0[0].solid.color.r).isEqualTo(100)
        assertThat(at0[0].solid.color.g).isEqualTo(50)
        assertThat(at0[0].solid.color.b).isEqualTo(0)
        assertThat(at0[0].solid.color.a).isEqualTo(200)
        assertThat(at0[0].opacity).isEqualTo(0.2f)

        // At t = 1, equals to colors and opacity
        val at1 = lerpSolidPaints(fromList, toList, 1.0f)
        assertThat(at1).hasSize(1)
        assertThat(at1[0].solid.color.r).isEqualTo(200)
        assertThat(at1[0].solid.color.g).isEqualTo(150)
        assertThat(at1[0].solid.color.b).isEqualTo(100)
        assertThat(at1[0].solid.color.a).isEqualTo(100)
        assertThat(at1[0].opacity).isEqualTo(0.8f)

        // At t = 0.5, is the rounded midpoint
        val atMid = lerpSolidPaints(fromList, toList, 0.5f)
        assertThat(atMid).hasSize(1)
        assertThat(atMid[0].solid.color.r).isEqualTo(150) // (100 + 200) / 2
        assertThat(atMid[0].solid.color.g).isEqualTo(100) // (50 + 150) / 2
        assertThat(atMid[0].solid.color.b).isEqualTo(50) // (0 + 100) / 2
        assertThat(atMid[0].solid.color.a).isEqualTo(150) // (200 + 100) / 2
        assertThat(atMid[0].opacity).isEqualTo(0.5f)

        // At t = 1.2 (overshoot), channels keep within [0, 255] and opacity within [0, 1]
        val extremeFrom = solidBackground(0, 200, 0, 0, opacity = 0.0f)
        val extremeTo = solidBackground(250, 255, 250, 255, opacity = 1.0f)
        val atOvershoot = lerpSolidPaints(listOf(extremeFrom), listOf(extremeTo), 1.2f)
        assertThat(atOvershoot).hasSize(1)
        // 0 + 1.2 * 250 = 300 -> clamped to 255
        assertThat(atOvershoot[0].solid.color.r).isEqualTo(255)
        assertThat(atOvershoot[0].solid.color.g).isEqualTo(255)
        assertThat(atOvershoot[0].solid.color.b).isEqualTo(255)
        assertThat(atOvershoot[0].solid.color.a).isEqualTo(255)
        // 0.0 + 1.2 * 1.0 = 1.2 -> clamped to 1.0f
        assertThat(atOvershoot[0].opacity).isEqualTo(1.0f)

        // Negative overshoot at t = -0.5
        val atUndershoot = lerpSolidPaints(listOf(extremeFrom), listOf(extremeTo), -0.5f)
        assertThat(atUndershoot[0].solid.color.r).isEqualTo(0)
        assertThat(atUndershoot[0].solid.color.a).isEqualTo(0)
        assertThat(atUndershoot[0].opacity).isEqualTo(0.0f)
    }
}
