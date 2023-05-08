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
import java.util.Optional
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class DecomposedMatrix2D(
    var scaleX: Float = 1F,
    var scaleY: Float = 1F,
    var translateX: Float = 0F,
    var translateY: Float = 0F,
    var angle: Float = 0F,
    var m11: Float = 1F,
    var m12: Float = 0F,
    var m21: Float = 0F,
    var m22: Float = 1F,
)

// Decompose a matrix in list form into its translation, angle, and scale parts
internal fun Optional<List<Float>>.decompose(density: Float): DecomposedMatrix2D {
    val matrix = this.asComposeTransform(density)
    return matrix?.decompose() ?: DecomposedMatrix2D()
}

// Decompose a matrix into its translation, angle, and scale parts
internal fun Matrix.decompose(): DecomposedMatrix2D {
    val result = DecomposedMatrix2D()
    var row0x = this[0, 0]
    var row0y = this[0, 1]
    var row1x = this[1, 0]
    var row1y = this[1, 1]
    result.translateX = this[3, 0]
    result.translateY = this[3, 1]

    // Compute scaling factors.
    result.scaleX = hypot(row0x, row0y)
    result.scaleY = hypot(row1x, row1y)

    // If determinant is negative, one axis was flipped.
    val determinant = row0x * row1y - row0y * row1x
    if (determinant < 0) {
        // Flip axis with minimum unit vector dot product.
        if (row0x < row1y) result.scaleX = -result.scaleX else result.scaleY = -result.scaleY
    }

    // Renormalize matrix to remove scale.
    if (result.scaleX != 1F) {
        row0x *= 1 / result.scaleX
        row0y *= 1 / result.scaleX
    }
    if (result.scaleY != 1F) {
        row1x *= 1 / result.scaleY
        row1y *= 1 / result.scaleY
    }

    // Compute rotation and renormalize matrix.
    result.angle = -atan2(row0y, row0x)

    if (result.angle != 0F) {
        // Rotate(-angle) = [cos(angle), sin(angle), -sin(angle), cos(angle)]
        //                = [row0x, -row0y, row0y, row0x]
        // Thanks to the normalization above.
        val sn = -row0y
        val cs = row0x
        val m11 = row0x
        val m12 = row0y
        val m21 = row1x
        val m22 = row1y

        row0x = cs * m11 + sn * m21
        row0y = cs * m12 + sn * m22
        row1x = -sn * m11 + cs * m21
        row1y = -sn * m12 + cs * m22
    }

    result.m11 = row0x
    result.m12 = row0y
    result.m21 = row1x
    result.m22 = row1y

    // Convert into degrees because our rotation functions expect it.
    result.angle = Math.toDegrees(result.angle.toDouble()).toFloat()

    return result
}

internal fun Matrix.setTranslation(x: Float, y: Float) {
    this[3, 0] = x
    this[3, 1] = y
}

// Hypotenuse of right triangle with sides x and y
internal fun hypot(x: Float, y: Float): Float {
    return sqrt(x * x + y * y)
}

// Clamp to a value that is a multiple of discreteValue
internal fun Float.coerceDiscrete(discrete: Boolean, discreteValue: Float): Float {
    if (discrete) return this - (this % discreteValue)
    return this
}

// Multiple a PointF with a scalar
internal inline operator fun PointF.times(scalar: Float): PointF {
    return PointF(x * scalar, y * scalar)
}

// Given a bounding frame and an ellipse, return the point along the ellipse at this angle
internal fun Float.pointAtAngle(frameSize: Size, radius: PointF): PointF {
    return PointF(
        frameSize.width / 2.0F + cos(Math.toRadians(this.toDouble())).toFloat() * radius.x,
        frameSize.height / 2.0F + sin(Math.toRadians(this.toDouble())).toFloat() * radius.y,
    )
}

// Convert an angle into a unit vector
internal fun Float.unitVectorFromAngle(): PointF {
    return PointF(
        cos(Math.toRadians(this.toDouble())).toFloat(),
        sin(Math.toRadians(this.toDouble())).toFloat(),
    )
}

// Approximate the circumference of an ellipse with this radius
internal fun PointF.ellipseCircumferenceFromRadius(): Float {
    // Ramanujan approximation of circumference of an ellipse
    return Math.PI.toFloat() *
        (3 * (this.x + this.y) - sqrt((3 * this.x + this.y) * (this.x + 3 * this.y)))
}
