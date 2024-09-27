// Copyright 2013 The Servo Project Developers. See the COPYRIGHT
// file at the same directory of this distribution.
//
// Licensed under the Apache License, Version 2.0 <LICENSE-APACHE or
// http://www.apache.org/licenses/LICENSE-2.0> or the MIT license
// <LICENSE-MIT or http://opensource.org/licenses/MIT>, at your
// option. This file may not be copied, modified, or distributed
// except according to those terms.

use crate::definition::modifier::AffineTransform;

/// Implementations are forked from euclid Transform2D.
impl AffineTransform {
    /// Create a transform specifying its matrix elements in row-major order.
    ///
    /// Beware: This library is written with the assumption that row vectors
    /// are being used. If your matrices use column vectors (i.e. transforming a vector
    /// is `T * v`), then please use `column_major`
    pub const fn row_major(m11: f32, m12: f32, m21: f32, m22: f32, m31: f32, m32: f32) -> Self {
        AffineTransform { m11, m12, m21, m22, m31, m32 }
    }
}
