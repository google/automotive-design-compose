/*
 * Copyright 2024 Google LLC
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
    fun testHypot() {
        assertThat(hypot(3f, 4f)).isEqualTo(5f)
        assertThat(hypot(0f, 0f)).isEqualTo(0f)
        assertThat(hypot(-3f, -4f)).isEqualTo(5f)
    }

    @Test
    fun testCoerceDiscrete() {
        assertThat(12.3f.coerceDiscrete(true, 0.5f)).isEqualTo(12.0f)
        assertThat(12.8f.coerceDiscrete(true, 0.5f)).isEqualTo(12.5f)
        assertThat(12.3f.coerceDiscrete(false, 0.5f)).isEqualTo(12.3f)
    }

    @Test
    fun testPointAtAngle() {
        val size = Size(200f, 100f)
        val radius = PointF(100f, 50f)

        // 0 degrees
        var point = 0f.pointAtAngle(size, radius)
        assertThat(point.x).isWithin(0.001f).of(200f)
        assertThat(point.y).isWithin(0.001f).of(50f)

        // 90 degrees
        point = 90f.pointAtAngle(size, radius)
        assertThat(point.x).isWithin(0.001f).of(100f)
        assertThat(point.y).isWithin(0.001f).of(100f)
    }

    @Test
    fun testUnitVectorFromAngle() {
        // 0 degrees
        var vector = 0f.unitVectorFromAngle()
        assertThat(vector.x).isWithin(0.001f).of(1f)
        assertThat(vector.y).isWithin(0.001f).of(0f)

        // 90 degrees
        vector = 90f.unitVectorFromAngle()
        assertThat(vector.x).isWithin(0.001f).of(0f)
        assertThat(vector.y).isWithin(0.001f).of(1f)
    }

    @Test
    fun testEllipseCircumferenceFromRadius() {
        val radius = PointF(10f, 5f)
        val circumference = radius.ellipseCircumferenceFromRadius()
        assertThat(circumference).isWithin(0.001f).of(48.442f)
    }

    @Test
    fun testDecomposeIdentity() {
        val matrix = Matrix()
        val decomposed = matrix.decompose()
        assertThat(decomposed.scaleX).isEqualTo(1f)
        assertThat(decomposed.scaleY).isEqualTo(1f)
        assertThat(decomposed.translateX).isEqualTo(0f)
        assertThat(decomposed.translateY).isEqualTo(0f)
        assertThat(decomposed.angle).isWithin(0.001f).of(0f)
    }

    @Test
    fun testDecomposeTranslation() {
        val matrix = Matrix()
        matrix.translate(10f, 20f, 0f)
        val decomposed = matrix.decompose()
        assertThat(decomposed.translateX).isEqualTo(10f)
        assertThat(decomposed.translateY).isEqualTo(20f)
    }

    @Test
    fun testDecomposeScale() {
        val matrix = Matrix()
        matrix.scale(2f, 3f, 1f)
        val decomposed = matrix.decompose()
        assertThat(decomposed.scaleX).isEqualTo(2f)
        assertThat(decomposed.scaleY).isEqualTo(3f)
    }

    @Test
    fun testDecomposeRotation() {
        val matrix = Matrix()
        matrix.rotateZ(45f)
        val decomposed = matrix.decompose()
        assertThat(decomposed.angle).isWithin(0.001f).of(-45f)
    }
}
