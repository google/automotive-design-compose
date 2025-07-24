// Copyright 2013 The Servo Project Developers. See the COPYRIGHT
// file at the same directory of this distribution.
//
// Licensed under the Apache License, Version 2.0 <LICENSE-APACHE or
// http://www.apache.org/licenses/LICENSE-2.0> or the MIT license
// <LICENSE-MIT or http://opensource.org/licenses/MIT>, at your
// option. This file may not be copied, modified, or distributed
// except according to those terms.

use crate::matrix_transform::AffineTransform;

/// Implementations are forked from euclid Transform2D.
impl AffineTransform {
    /// Create a transform specifying its matrix elements in row-major order.
    ///
    /// Beware: This library is written with the assumption that row vectors
    /// are being used. If your matrices use column vectors (i.e. transforming a vector
    /// is `T * v`), then please use `column_major`
    pub fn row_major(m11: f32, m12: f32, m21: f32, m22: f32, m31: f32, m32: f32) -> Self {
        AffineTransform { m11, m12, m21, m22, m31, m32, ..Default::default() }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_row_major() {
        let transform = AffineTransform::row_major(1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
        assert_eq!(transform.m11, 1.0);
        assert_eq!(transform.m12, 2.0);
        assert_eq!(transform.m21, 3.0);
        assert_eq!(transform.m22, 4.0);
        assert_eq!(transform.m31, 5.0);
        assert_eq!(transform.m32, 6.0);
    }
}
