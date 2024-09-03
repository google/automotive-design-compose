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

use std::fmt;
use std::fmt::{Debug, Display, Formatter};
include!(concat!(env!("OUT_DIR"), "/designcompose.definition.element.rs"));

impl Display for FontStyle {
    fn fmt(&self, f: &mut Formatter) -> fmt::Result {
        Debug::fmt(self, f)
    }
}
impl FontFeature {
    pub fn new(tag: String) -> Self {
        FontFeature { tag, enabled: true }
    }
}

impl Display for TextDecoration {
    fn fmt(&self, f: &mut Formatter) -> fmt::Result {
        Debug::fmt(self, f)
    }
}

/// Compare two finite f32 values
/// This is not meant for situations dealing with NAN and INFINITY
#[inline]
fn f32_eq(a: &f32, b: &f32) -> bool {
    assert!(a.is_finite());
    assert!(b.is_finite());
    (a - b).abs() < f32::EPSILON
}

impl Color {
    pub fn from_u8s(r: u8, g: u8, b: u8, a: u8) -> Color {
        Color { r: r as u32, g: g as u32, b: b as u32, a: a as u32 }
    }
    pub fn from_u8_tuple(color: (u8, u8, u8, u8)) -> Color {
        Color { r: color.0 as u32, g: color.1 as u32, b: color.2 as u32, a: color.3 as u32 }
    }
    pub fn from_f32s(r: f32, g: f32, b: f32, a: f32) -> Color {
        let tou32 = |c| (c * 255.0) as u32;
        Color { r: tou32(r), g: tou32(g), b: tou32(b), a: tou32(a) }
    }
    /// 0xAARRGGBB
    pub fn from_u32(c: u32) -> Color {
        Color {
            r: ((c & 0x00FF_0000u32) >> 16) as u32,
            g: ((c & 0x0000_FF00u32) >> 8) as u32,
            b: (c & 0x0000_00FFu32) as u32,
            a: ((c & 0xFF00_0000u32) >> 24) as u32,
        }
    }
    pub fn from_f32_tuple(color: (f32, f32, f32, f32)) -> Color {
        Color::from_f32s(color.0, color.1, color.2, color.3)
    }

    /// Returns the H,S,V (Hue, Saturation, Value) representation
    /// of the (Red, Green, Blue) color argument.
    ///
    /// # Arguments
    ///
    /// * `color` - a tuple of r,g,b values between 0.0 and 1.0
    ///
    pub fn hsv_from_u8(color: (u8, u8, u8)) -> (f32, f32, f32) {
        // After: https://math.stackexchange.com/questions/556341/rgb-to-hsv-color-conversion-algorithm
        let r = color.0;
        let g = color.1;
        let b = color.2;

        let r = r as f32 / 255.0f32;
        let g = g as f32 / 255.0f32;
        let b = b as f32 / 255.0f32;

        let maxc = f32::max(f32::max(r, g), b);
        let minc = f32::min(f32::min(r, g), b);
        let v = maxc;
        if f32_eq(&minc, &maxc) {
            return (0.0, 0.0, v);
        }
        let s = (maxc - minc) / maxc;
        let rc = (maxc - r) / (maxc - minc);
        let gc = (maxc - g) / (maxc - minc);
        let bc = (maxc - b) / (maxc - minc);
        let h = if f32_eq(&r, &maxc) {
            bc - gc
        } else if f32_eq(&g, &maxc) {
            2.0 + rc - bc
        } else {
            4.0 + gc - rc
        };
        let h = (h / 6.0) % 1.0;
        let mut h = h * 2.0 * std::f32::consts::PI;
        // make sure h is positive and within [0:2_PI)
        if h < 0.0 {
            h += 2.0 * std::f32::consts::PI;
        } else if h >= 2.0 * std::f32::consts::PI {
            h -= 2.0 * std::f32::consts::PI;
        }
        (h, s, v)
    }

    pub fn hsv_from_u32(color: (u32, u32, u32)) -> (f32, f32, f32) {
        Color::hsv_from_u8((color.0 as u8, color.1 as u8, color.2 as u8))
    }

    /// Returns the H,S,V (Hue, Saturation, Value) representation
    /// of color.
    ///
    pub fn as_hsv(&self) -> (f32, f32, f32) {
        Color::hsv_from_u32((self.r, self.g, self.b))
    }

    pub fn as_u8_tuple(&self) -> (u8, u8, u8, u8) {
        (self.r as u8, self.g as u8, self.b as u8, self.a as u8)
    }
    pub fn as_f32_tuple(&self) -> (f32, f32, f32, f32) {
        let tof32 = |c| c as f32 / 255.0;
        (tof32(self.r), tof32(self.g), tof32(self.b), tof32(self.a))
    }
    pub fn as_f32_array(&self) -> [f32; 4] {
        let c = self.as_f32_tuple();
        [c.0, c.1, c.2, c.3]
    }
    pub fn as_u32(&self) -> u32 {
        (self.r << 24) | (self.g << 16) | (self.b << 8) | self.a
    }

    pub fn set_red(&mut self, r: u8) {
        self.r = r as u32;
    }
    pub fn set_green(&mut self, g: u8) {
        self.g = g as u32;
    }
    pub fn set_blue(&mut self, b: u8) {
        self.b = b as u32;
    }
    pub fn set_alpha(&mut self, a: u8) {
        self.a = a as u32;
    }

    pub fn r(&self) -> u8 {
        self.r as u8
    }
    pub fn g(&self) -> u8 {
        self.g as u8
    }
    pub fn b(&self) -> u8 {
        self.b as u8
    }
    pub fn a(&self) -> u8 {
        self.a as u8
    }

    pub const WHITE: Color = Color { r: 255, g: 255, b: 255, a: 255 };
    pub const BLACK: Color = Color { r: 0, g: 0, b: 0, a: 255 };
    pub const RED: Color = Color { r: 255, g: 0, b: 0, a: 255 };
    pub const GREEN: Color = Color { r: 0, g: 255, b: 0, a: 255 };
    pub const BLUE: Color = Color { r: 0, g: 0, b: 255, a: 255 };
    pub const YELLOW: Color = Color { r: 255, g: 255, b: 0, a: 255 };
    pub const MAGENTA: Color = Color { r: 255, g: 0, b: 255, a: 255 };
    pub const CYAN: Color = Color { r: 0, g: 255, b: 255, a: 255 };
    pub const GRAY: Color = Color { r: 128, g: 128, b: 128, a: 255 };
    pub const HOT_PINK: Color = Color { r: 255, g: 105, b: 180, a: 255 };
}

impl Into<Color> for &FloatColor {
    fn into(self) -> Color {
        Color::from_f32s(self.r, self.g, self.b, self.a)
    }
}
