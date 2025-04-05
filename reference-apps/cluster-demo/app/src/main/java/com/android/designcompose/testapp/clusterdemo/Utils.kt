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

import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.Float4
import com.google.android.filament.utils.Mat4
import com.google.android.filament.utils.Quaternion
import com.google.android.filament.utils.length
import com.google.android.filament.utils.slerp
import kotlin.math.absoluteValue

/// Representation of a transformation that has been decomposed from a 4x4 into its
/// constituent components, which can be interpolated. Currently doesn't handle skew
/// or perspective.
data class DecomposedTransform(
    val scale: Float3,
    val position: Float3,
    val perspective: Float4,
    val rotation: Quaternion,
) {
    fun clone(): DecomposedTransform {
        return DecomposedTransform(
            scale = this.scale.xyz,
            position = this.position.xyz,
            perspective = this.perspective.xyzw,
            rotation = Quaternion(this.rotation),
        )
    }

    /// Interpolate between two DecomposedTransforms using a spherical interpolation
    /// for rotation.
    fun slerp(other: DecomposedTransform, delta: Float): DecomposedTransform {
        val iv = 1.0f - delta
        // XXX: Not handling perspective or skew.
        return DecomposedTransform(
            scale = this.scale * iv + other.scale * delta,
            position = this.position * iv + other.position * delta,
            perspective = this.perspective * iv + other.perspective * delta,
            rotation = slerp(this.rotation, other.rotation, delta),
        )
    }

    fun recompose(): Mat4 {
        val tx = position.x
        val ty = position.y
        val tz = position.z
        val qx = rotation[0]
        val qy = rotation[1]
        val qz = rotation[2]
        val qw = rotation[3]
        val sx = scale.x
        val sy = scale.y
        val sz = scale.z
        return Mat4(
            Float4(
                (1 - 2 * qy * qy - 2 * qz * qz) * sx,
                (2 * qx * qy + 2 * qz * qw) * sx,
                (2 * qx * qz - 2 * qy * qw) * sx,
                perspective[0],
            ),
            Float4(
                (2 * qx * qy - 2 * qz * qw) * sy,
                (1 - 2 * qx * qx - 2 * qz * qz) * sy,
                (2 * qy * qz + 2 * qx * qw) * sy,
                perspective[1],
            ),
            Float4(
                (2 * qx * qz + 2 * qy * qw) * sz,
                (2 * qy * qz - 2 * qx * qw) * sz,
                (1 - 2 * qx * qx - 2 * qy * qy) * sz,
                perspective[2],
            ),
            Float4(tx, ty, tz, perspective[3]),
        )
    }
}

fun Mat4.Companion.of(a: FloatArray): Mat4 {
    return of(
        a[0],
        a[1],
        a[2],
        a[3],
        a[4],
        a[5],
        a[6],
        a[7],
        a[8],
        a[9],
        a[10],
        a[11],
        a[12],
        a[13],
        a[14],
        a[15],
    )
}

fun Mat4.decompose(): DecomposedTransform {
    val m = Mat4(this) // This function does mutate the matrix
    // Extract translation
    val translation = m.translation

    // Extract perspective
    val perspMatrix = Mat4(m)
    perspMatrix[0][3] = 0.0f
    perspMatrix[1][3] = 0.0f
    perspMatrix[2][3] = 0.0f
    perspMatrix[3][3] = 1.0f

    if (perspMatrix.determinant() < 1e-8f) {
        // XXX: bail out
    }

    val perspective =
        if (m[0][3] != 0.0f || m[1][3] != 0.0f || m[2][3] != 0.0f) {
            val rhs = Float4(m[0][3], m[1][3], m[2][3], m[3][3])
            // XXX: This ought to be the right way to get the perspective point out.

            /*val transposeInversePerspMatrix = transpose(inverse(perspMatrix))

            // Clear out perspective for the rest of the decomposition (for skew etc).
            m[0][3] = 0.0f
            m[1][3] = 0.0f
            m[2][3] = 0.0f
            m[3][3] = 1.0f

            // Compute the perspective point
            transposeInversePerspMatrix * rhs
             */

            rhs
        } else {
            // No perspective
            Float4(0.0f, 0.0f, 0.0f, 1.0f)
        }

    // Extract upper-left for a determinant computation.
    val a = m[0][0]
    val b = m[0][1]
    val c = m[0][2]
    val d = m[1][0]
    val e = m[1][1]
    val f = m[1][2]
    val g = m[2][0]
    val h = m[2][1]
    val i = m[2][2]
    val A = e * i - f * h
    val B = f * g - d * i
    val C = d * h - e * g

    // Extract scale
    val det = a * A + b * B + c * C
    val scalex = length(Float3(a, b, c))
    val scaley = length(Float3(d, e, f))
    val scalez = length(Float3(g, h, i))
    var scale = Float3(scalex, scaley, scalez)
    if (det < 0.0) {
        scale = -scale
    }

    // Remote scale from the matrix if it is not close to zero
    val clone = Mat4(m)
    val rotation =
        if (det.absoluteValue > 1e-8f) { // SkMatrix epsilon const
            clone.x /= scale.x
            clone.y /= scale.y
            clone.z /= scale.z
            clone.toQuaternion()
        } else {
            Quaternion()
        }
    return DecomposedTransform(
        scale = scale,
        position = translation,
        perspective = perspective,
        rotation = rotation,
    )
}

internal fun Mat4.determinant(): Float {
    val m = this
    val result = Mat4()

    var pair0 = m.z.z * m.w.w
    var pair1 = m.w.z * m.z.w
    var pair2 = m.y.z * m.w.w
    var pair3 = m.w.z * m.y.w
    var pair4 = m.y.z * m.z.w
    var pair5 = m.z.z * m.y.w
    var pair6 = m.x.z * m.w.w
    var pair7 = m.w.z * m.x.w
    var pair8 = m.x.z * m.z.w
    var pair9 = m.z.z * m.x.w
    var pair10 = m.x.z * m.y.w
    var pair11 = m.y.z * m.x.w

    result.x.x = pair0 * m.y.y + pair3 * m.z.y + pair4 * m.w.y
    result.x.x -= pair1 * m.y.y + pair2 * m.z.y + pair5 * m.w.y
    result.x.y = pair1 * m.x.y + pair6 * m.z.y + pair9 * m.w.y
    result.x.y -= pair0 * m.x.y + pair7 * m.z.y + pair8 * m.w.y
    result.x.z = pair2 * m.x.y + pair7 * m.y.y + pair10 * m.w.y
    result.x.z -= pair3 * m.x.y + pair6 * m.y.y + pair11 * m.w.y
    result.x.w = pair5 * m.x.y + pair8 * m.y.y + pair11 * m.z.y
    result.x.w -= pair4 * m.x.y + pair9 * m.y.y + pair10 * m.z.y
    result.y.x = pair1 * m.y.x + pair2 * m.z.x + pair5 * m.w.x
    result.y.x -= pair0 * m.y.x + pair3 * m.z.x + pair4 * m.w.x
    result.y.y = pair0 * m.x.x + pair7 * m.z.x + pair8 * m.w.x
    result.y.y -= pair1 * m.x.x + pair6 * m.z.x + pair9 * m.w.x
    result.y.z = pair3 * m.x.x + pair6 * m.y.x + pair11 * m.w.x
    result.y.z -= pair2 * m.x.x + pair7 * m.y.x + pair10 * m.w.x
    result.y.w = pair4 * m.x.x + pair9 * m.y.x + pair10 * m.z.x
    result.y.w -= pair5 * m.x.x + pair8 * m.y.x + pair11 * m.z.x

    pair0 = m.z.x * m.w.y
    pair1 = m.w.x * m.z.y
    pair2 = m.y.x * m.w.y
    pair3 = m.w.x * m.y.y
    pair4 = m.y.x * m.z.y
    pair5 = m.z.x * m.y.y
    pair6 = m.x.x * m.w.y
    pair7 = m.w.x * m.x.y
    pair8 = m.x.x * m.z.y
    pair9 = m.z.x * m.x.y
    pair10 = m.x.x * m.y.y
    pair11 = m.y.x * m.x.y

    result.z.x = pair0 * m.y.w + pair3 * m.z.w + pair4 * m.w.w
    result.z.x -= pair1 * m.y.w + pair2 * m.z.w + pair5 * m.w.w
    result.z.y = pair1 * m.x.w + pair6 * m.z.w + pair9 * m.w.w
    result.z.y -= pair0 * m.x.w + pair7 * m.z.w + pair8 * m.w.w
    result.z.z = pair2 * m.x.w + pair7 * m.y.w + pair10 * m.w.w
    result.z.z -= pair3 * m.x.w + pair6 * m.y.w + pair11 * m.w.w
    result.z.w = pair5 * m.x.w + pair8 * m.y.w + pair11 * m.z.w
    result.z.w -= pair4 * m.x.w + pair9 * m.y.w + pair10 * m.z.w
    result.w.x = pair2 * m.z.z + pair5 * m.w.z + pair1 * m.y.z
    result.w.x -= pair4 * m.w.z + pair0 * m.y.z + pair3 * m.z.z
    result.w.y = pair8 * m.w.z + pair0 * m.x.z + pair7 * m.z.z
    result.w.y -= pair6 * m.z.z + pair9 * m.w.z + pair1 * m.x.z
    result.w.z = pair6 * m.y.z + pair11 * m.w.z + pair3 * m.x.z
    result.w.z -= pair10 * m.w.z + pair2 * m.x.z + pair7 * m.y.z
    result.w.w = pair10 * m.z.z + pair4 * m.x.z + pair9 * m.y.z
    result.w.w -= pair8 * m.y.z + pair11 * m.z.z + pair5 * m.x.z

    val determinant =
        m.x.x * result.x.x + m.y.x * result.x.y + m.z.x * result.x.z + m.w.x * result.x.w

    return determinant
}
