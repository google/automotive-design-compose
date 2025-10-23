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

package com.android.designcompose

import android.graphics.PointF
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MathUtilsTest {

    @Test
    fun testDecomposeMatrix() {
        val matrix = Matrix()
        matrix.translate(10f, 20f, 0f)
        matrix.rotateZ(45f)
        matrix.scale(2f, 3f, 1f)
        val decomposed = matrix.decompose()

        assertThat(decomposed.translateX).isWithin(0.001f).of(10f)
        assertThat(decomposed.translateY).isWithin(0.001f).of(20f)
        assertThat(decomposed.angle).isWithin(0.001f).of(-45f)
        assertThat(decomposed.scaleX).isWithin(0.001f).of(2f)
        assertThat(decomposed.scaleY).isWithin(0.001f).of(3f)
    }

    @Test
    fun testToMatrix() {
        val decomposed =
            DecomposedMatrix2D(
                translateX = 10f,
                translateY = 20f,
                angle = 45f,
                scaleX = 2f,
                scaleY = 3f,
            )
        val matrix = decomposed.toMatrix()
        val decomposed2 = matrix.decompose()

        assertThat(decomposed2.translateX).isWithin(0.001f).of(10f)
        assertThat(decomposed2.translateY).isWithin(0.001f).of(20f)
        assertThat(decomposed2.angle).isWithin(0.001f).of(45f)
        assertThat(decomposed2.scaleX).isWithin(0.001f).of(2f)
        assertThat(decomposed2.scaleY).isWithin(0.001f).of(3f)
    }

    @Test
    fun testInterpolateTo() {
        val decomposed1 =
            DecomposedMatrix2D(
                translateX = 10f,
                translateY = 20f,
                angle = 45f,
                scaleX = 2f,
                scaleY = 3f,
            )
        val decomposed2 =
            DecomposedMatrix2D(
                translateX = 20f,
                translateY = 40f,
                angle = 90f,
                scaleX = 4f,
                scaleY = 6f,
            )
        val interpolated = decomposed1.interpolateTo(decomposed2, 0.5f)

        assertThat(interpolated.translateX).isWithin(0.001f).of(15f)
        assertThat(interpolated.translateY).isWithin(0.001f).of(30f)
        assertThat(interpolated.angle).isWithin(0.001f).of(67.5f)
        assertThat(interpolated.scaleX).isWithin(0.001f).of(3f)
        assertThat(interpolated.scaleY).isWithin(0.001f).of(4.5f)
    }

    @Test
    fun testCoerceDiscrete() {
        assertThat(12.3f.coerceDiscrete(true, 5f)).isEqualTo(10f)
        assertThat(12.3f.coerceDiscrete(false, 5f)).isEqualTo(12.3f)
    }

    @Test
    fun testPointAtAngle() {
        val point = 90f.pointAtAngle(Size(100f, 100f), PointF(50f, 50f))
        assertThat(point.x).isWithin(0.001f).of(50f)
        assertThat(point.y).isWithin(0.001f).of(100f)
    }

    @Test
    fun testUnitVectorFromAngle() {
        val point = 90f.unitVectorFromAngle()
        assertThat(point.x).isWithin(0.001f).of(0f)
        assertThat(point.y).isWithin(0.001f).of(1f)
    }

    @Test
    fun testEllipseCircumferenceFromRadius() {
        val circumference = PointF(10f, 20f).ellipseCircumferenceFromRadius()
        assertThat(circumference).isWithin(0.001f).of(96.884f)
    }
}
