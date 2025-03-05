// Copyright 2013 The Servo Project Developers. See the COPYRIGHT
// file at the same directory of this distribution.
//
// Licensed under the Apache License, Version 2.0 <LICENSE-APACHE or
// http://www.apache.org/licenses/LICENSE-2.0> or the MIT license
// <LICENSE-MIT or http://opensource.org/licenses/MIT>, at your
// option. This file may not be copied, modified, or distributed
// except according to those terms.

use crate::matrix_transform::{AffineTransform, LayoutTransform};

/// Implementations are forked from euclid Transform3D.
impl LayoutTransform {
    /// Create a transform specifying its components in row-major order.
    ///
    /// For example, the translation terms m41, m42, m43 on the last row with the
    /// row-major convention) are the 13rd, 14th and 15th parameters.
    ///
    /// Beware: This library is written with the assumption that row vectors
    /// are being used. If your matrices use column vectors (i.e. transforming a vector
    /// is `T * v`), then please use `column_major`
    pub fn row_major(
        m11: f32,
        m12: f32,
        m13: f32,
        m14: f32,
        m21: f32,
        m22: f32,
        m23: f32,
        m24: f32,
        m31: f32,
        m32: f32,
        m33: f32,
        m34: f32,
        m41: f32,
        m42: f32,
        m43: f32,
        m44: f32,
    ) -> Self {
        LayoutTransform {
            m11,
            m12,
            m13,
            m14,
            m21,
            m22,
            m23,
            m24,
            m31,
            m32,
            m33,
            m34,
            m41,
            m42,
            m43,
            m44,
            ..Default::default()
        }
    }

    /// Create a 4 by 4 transform representing a 2d transformation, specifying its components
    /// in row-major order:
    ///
    /// ```text
    /// m11  m12   0   0
    /// m21  m22   0   0
    ///   0    0   1   0
    /// m41  m42   0   1
    /// ```
    #[inline]
    pub fn row_major_2d(m11: f32, m12: f32, m21: f32, m22: f32, m41: f32, m42: f32) -> Self {
        Self::row_major(
            m11, m12, 0f32, 0f32, // row1
            m21, m22, 0f32, 0f32, // row2
            0f32, 0f32, 1f32, 0f32, //row3
            m41, m42, 0f32, 1f32, //row4
        )
    }

    /// Create a 3d translation transform:
    ///
    /// ```text
    /// 1 0 0 0
    /// 0 1 0 0
    /// 0 0 1 0
    /// x y z 1
    /// ```
    #[inline]
    pub fn create_translation(x: f32, y: f32, z: f32) -> Self {
        Self::row_major(
            1f32, 0f32, 0f32, 0f32, // row1
            0f32, 1f32, 0f32, 0f32, // row2
            0f32, 0f32, 1f32, 0f32, // row3
            x, y, z, 1f32, // row4
        )
    }

    /// Creates an identity matrix:
    ///
    /// ```text
    /// 1 0 0 0
    /// 0 1 0 0
    /// 0 0 1 0
    /// 0 0 0 1
    /// ```
    #[inline]
    pub fn identity() -> Self {
        Self::create_translation(0f32, 0f32, 0f32)
    }

    /// Create a 3d rotation transform from an angle / axis.
    /// The supplied axis must be normalized.
    pub fn create_rotation(x: f32, y: f32, z: f32, theta: f32) -> Self {
        let xx = x * x;
        let yy = y * y;
        let zz = z * z;

        let half_theta = theta / 2f32;
        let sc = half_theta.sin() * half_theta.cos();
        let sq = half_theta.sin() * half_theta.sin();

        Self::row_major(
            1f32 - 2f32 * (yy + zz) * sq,
            2f32 * (x * y * sq - z * sc),
            2f32 * (x * z * sq + y * sc),
            0f32,
            2f32 * (x * y * sq + z * sc),
            1f32 - 2f32 * (xx + zz) * sq,
            2f32 * (y * z * sq - x * sc),
            0f32,
            2f32 * (x * z * sq - y * sc),
            2f32 * (y * z * sq + x * sc),
            1f32 - 2f32 * (xx + yy) * sq,
            0f32,
            0f32,
            0f32,
            0f32,
            1f32,
        )
    }

    /// Returns a transform with a rotation applied before self's transformation.
    #[must_use]
    pub fn pre_rotate(&self, x: f32, y: f32, z: f32, theta: f32) -> Self {
        self.pre_transform(&Self::create_rotation(x, y, z, theta))
    }

    /// Returns the multiplication of the two matrices such that mat's transformation
    /// applies before self's transformation.
    ///
    /// Assuming row vectors, this is equivalent to mat * self
    #[inline]
    #[must_use]
    pub fn pre_transform(&self, mat: &LayoutTransform) -> LayoutTransform {
        mat.post_transform(self)
    }

    /// Returns the multiplication of the two matrices such that mat's transformation
    /// applies after self's transformation.
    ///
    /// Assuming row vectors, this is equivalent to self * mat
    #[must_use]
    pub fn post_transform(&self, mat: &LayoutTransform) -> Self {
        Self::row_major(
            self.m11 * mat.m11 + self.m12 * mat.m21 + self.m13 * mat.m31 + self.m14 * mat.m41,
            self.m11 * mat.m12 + self.m12 * mat.m22 + self.m13 * mat.m32 + self.m14 * mat.m42,
            self.m11 * mat.m13 + self.m12 * mat.m23 + self.m13 * mat.m33 + self.m14 * mat.m43,
            self.m11 * mat.m14 + self.m12 * mat.m24 + self.m13 * mat.m34 + self.m14 * mat.m44,
            self.m21 * mat.m11 + self.m22 * mat.m21 + self.m23 * mat.m31 + self.m24 * mat.m41,
            self.m21 * mat.m12 + self.m22 * mat.m22 + self.m23 * mat.m32 + self.m24 * mat.m42,
            self.m21 * mat.m13 + self.m22 * mat.m23 + self.m23 * mat.m33 + self.m24 * mat.m43,
            self.m21 * mat.m14 + self.m22 * mat.m24 + self.m23 * mat.m34 + self.m24 * mat.m44,
            self.m31 * mat.m11 + self.m32 * mat.m21 + self.m33 * mat.m31 + self.m34 * mat.m41,
            self.m31 * mat.m12 + self.m32 * mat.m22 + self.m33 * mat.m32 + self.m34 * mat.m42,
            self.m31 * mat.m13 + self.m32 * mat.m23 + self.m33 * mat.m33 + self.m34 * mat.m43,
            self.m31 * mat.m14 + self.m32 * mat.m24 + self.m33 * mat.m34 + self.m34 * mat.m44,
            self.m41 * mat.m11 + self.m42 * mat.m21 + self.m43 * mat.m31 + self.m44 * mat.m41,
            self.m41 * mat.m12 + self.m42 * mat.m22 + self.m43 * mat.m32 + self.m44 * mat.m42,
            self.m41 * mat.m13 + self.m42 * mat.m23 + self.m43 * mat.m33 + self.m44 * mat.m43,
            self.m41 * mat.m14 + self.m42 * mat.m24 + self.m43 * mat.m34 + self.m44 * mat.m44,
        )
    }

    /// Returns a transform with a translation applied after self's transformation.
    #[must_use]
    pub fn post_translate(&self, x: f32, y: f32, z: f32) -> Self {
        self.post_transform(&Self::create_translation(x, y, z))
    }

    /// Compute the determinant of the transform.
    pub fn determinant(&self) -> f32 {
        self.m14 * self.m23 * self.m32 * self.m41
            - self.m13 * self.m24 * self.m32 * self.m41
            - self.m14 * self.m22 * self.m33 * self.m41
            + self.m12 * self.m24 * self.m33 * self.m41
            + self.m13 * self.m22 * self.m34 * self.m41
            - self.m12 * self.m23 * self.m34 * self.m41
            - self.m14 * self.m23 * self.m31 * self.m42
            + self.m13 * self.m24 * self.m31 * self.m42
            + self.m14 * self.m21 * self.m33 * self.m42
            - self.m11 * self.m24 * self.m33 * self.m42
            - self.m13 * self.m21 * self.m34 * self.m42
            + self.m11 * self.m23 * self.m34 * self.m42
            + self.m14 * self.m22 * self.m31 * self.m43
            - self.m12 * self.m24 * self.m31 * self.m43
            - self.m14 * self.m21 * self.m32 * self.m43
            + self.m11 * self.m24 * self.m32 * self.m43
            + self.m12 * self.m21 * self.m34 * self.m43
            - self.m11 * self.m22 * self.m34 * self.m43
            - self.m13 * self.m22 * self.m31 * self.m44
            + self.m12 * self.m23 * self.m31 * self.m44
            + self.m13 * self.m21 * self.m32 * self.m44
            - self.m11 * self.m23 * self.m32 * self.m44
            - self.m12 * self.m21 * self.m33 * self.m44
            + self.m11 * self.m22 * self.m33 * self.m44
    }

    /// Multiplies all of the transform's component by a scalar and returns the result.
    #[must_use]
    pub fn mul_s(&self, x: f32) -> Self {
        Self::row_major(
            self.m11 * x,
            self.m12 * x,
            self.m13 * x,
            self.m14 * x,
            self.m21 * x,
            self.m22 * x,
            self.m23 * x,
            self.m24 * x,
            self.m31 * x,
            self.m32 * x,
            self.m33 * x,
            self.m34 * x,
            self.m41 * x,
            self.m42 * x,
            self.m43 * x,
            self.m44 * x,
        )
    }

    /// Returns the inverse transform if possible.
    pub fn inverse(&self) -> Option<LayoutTransform> {
        let det = self.determinant();

        if det == 0f32 {
            return None;
        }

        let m = Self::row_major(
            self.m23 * self.m34 * self.m42 - self.m24 * self.m33 * self.m42
                + self.m24 * self.m32 * self.m43
                - self.m22 * self.m34 * self.m43
                - self.m23 * self.m32 * self.m44
                + self.m22 * self.m33 * self.m44,
            self.m14 * self.m33 * self.m42
                - self.m13 * self.m34 * self.m42
                - self.m14 * self.m32 * self.m43
                + self.m12 * self.m34 * self.m43
                + self.m13 * self.m32 * self.m44
                - self.m12 * self.m33 * self.m44,
            self.m13 * self.m24 * self.m42 - self.m14 * self.m23 * self.m42
                + self.m14 * self.m22 * self.m43
                - self.m12 * self.m24 * self.m43
                - self.m13 * self.m22 * self.m44
                + self.m12 * self.m23 * self.m44,
            self.m14 * self.m23 * self.m32
                - self.m13 * self.m24 * self.m32
                - self.m14 * self.m22 * self.m33
                + self.m12 * self.m24 * self.m33
                + self.m13 * self.m22 * self.m34
                - self.m12 * self.m23 * self.m34,
            self.m24 * self.m33 * self.m41
                - self.m23 * self.m34 * self.m41
                - self.m24 * self.m31 * self.m43
                + self.m21 * self.m34 * self.m43
                + self.m23 * self.m31 * self.m44
                - self.m21 * self.m33 * self.m44,
            self.m13 * self.m34 * self.m41 - self.m14 * self.m33 * self.m41
                + self.m14 * self.m31 * self.m43
                - self.m11 * self.m34 * self.m43
                - self.m13 * self.m31 * self.m44
                + self.m11 * self.m33 * self.m44,
            self.m14 * self.m23 * self.m41
                - self.m13 * self.m24 * self.m41
                - self.m14 * self.m21 * self.m43
                + self.m11 * self.m24 * self.m43
                + self.m13 * self.m21 * self.m44
                - self.m11 * self.m23 * self.m44,
            self.m13 * self.m24 * self.m31 - self.m14 * self.m23 * self.m31
                + self.m14 * self.m21 * self.m33
                - self.m11 * self.m24 * self.m33
                - self.m13 * self.m21 * self.m34
                + self.m11 * self.m23 * self.m34,
            self.m22 * self.m34 * self.m41 - self.m24 * self.m32 * self.m41
                + self.m24 * self.m31 * self.m42
                - self.m21 * self.m34 * self.m42
                - self.m22 * self.m31 * self.m44
                + self.m21 * self.m32 * self.m44,
            self.m14 * self.m32 * self.m41
                - self.m12 * self.m34 * self.m41
                - self.m14 * self.m31 * self.m42
                + self.m11 * self.m34 * self.m42
                + self.m12 * self.m31 * self.m44
                - self.m11 * self.m32 * self.m44,
            self.m12 * self.m24 * self.m41 - self.m14 * self.m22 * self.m41
                + self.m14 * self.m21 * self.m42
                - self.m11 * self.m24 * self.m42
                - self.m12 * self.m21 * self.m44
                + self.m11 * self.m22 * self.m44,
            self.m14 * self.m22 * self.m31
                - self.m12 * self.m24 * self.m31
                - self.m14 * self.m21 * self.m32
                + self.m11 * self.m24 * self.m32
                + self.m12 * self.m21 * self.m34
                - self.m11 * self.m22 * self.m34,
            self.m23 * self.m32 * self.m41
                - self.m22 * self.m33 * self.m41
                - self.m23 * self.m31 * self.m42
                + self.m21 * self.m33 * self.m42
                + self.m22 * self.m31 * self.m43
                - self.m21 * self.m32 * self.m43,
            self.m12 * self.m33 * self.m41 - self.m13 * self.m32 * self.m41
                + self.m13 * self.m31 * self.m42
                - self.m11 * self.m33 * self.m42
                - self.m12 * self.m31 * self.m43
                + self.m11 * self.m32 * self.m43,
            self.m13 * self.m22 * self.m41
                - self.m12 * self.m23 * self.m41
                - self.m13 * self.m21 * self.m42
                + self.m11 * self.m23 * self.m42
                + self.m12 * self.m21 * self.m43
                - self.m11 * self.m22 * self.m43,
            self.m12 * self.m23 * self.m31 - self.m13 * self.m22 * self.m31
                + self.m13 * self.m21 * self.m32
                - self.m11 * self.m23 * self.m32
                - self.m12 * self.m21 * self.m33
                + self.m11 * self.m22 * self.m33,
        );

        Some(m.mul_s(1f32 / det))
    }

    /// Create a 3d scale transform:
    ///
    /// ```text
    /// x 0 0 0
    /// 0 y 0 0
    /// 0 0 z 0
    /// 0 0 0 1
    /// ```
    #[inline]
    pub fn create_scale(x: f32, y: f32, z: f32) -> Self {
        Self::row_major(
            x, 0f32, 0f32, 0f32, // row1
            0f32, y, 0f32, 0f32, // row2
            0f32, 0f32, z, 0f32, //row3
            0f32, 0f32, 0f32, 1f32, //row4
        )
    }

    /// Returns a transform with a scale applied after self's transformation.
    #[must_use]
    pub fn post_scale(&self, x: f32, y: f32, z: f32) -> Self {
        self.post_transform(&Self::create_scale(x, y, z))
    }

    /// Create a 2D transform picking the relevant terms from this transform.
    ///
    /// This method assumes that self represents a 2d transformation, callers
    /// should check that [`self.is_2d()`] returns `true` beforehand.
    ///
    /// [`self.is_2d()`]: #method.is_2d
    pub fn to_2d(&self) -> AffineTransform {
        AffineTransform::row_major(self.m11, self.m12, self.m21, self.m22, self.m41, self.m42)
    }
}
