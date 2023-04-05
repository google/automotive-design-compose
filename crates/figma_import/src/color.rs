// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use crate::utils::f32_eq;
use serde::{Deserialize, Serialize};

#[derive(Debug, Copy, Clone, PartialEq, Eq, Hash, Deserialize, Serialize)]
pub struct Color {
    pub color: (u8, u8, u8, u8),
}

impl Color {
    pub fn from_u8s(r: u8, g: u8, b: u8, a: u8) -> Color {
        Color { color: (r, g, b, a) }
    }
    pub fn from_u8_tuple(color: (u8, u8, u8, u8)) -> Color {
        Color { color }
    }
    pub fn from_f32s(r: f32, g: f32, b: f32, a: f32) -> Color {
        let tou8 = |c| (c * 255.0) as u8;
        Color { color: (tou8(r), tou8(g), tou8(b), tou8(a)) }
    }
    /// 0xAARRGGBB
    pub fn from_u32(c: u32) -> Color {
        Color {
            color: (
                ((c & 0x00FF_0000u32) >> 16) as u8,
                ((c & 0x0000_FF00u32) >> 8) as u8,
                (c & 0x0000_00FFu32) as u8,
                ((c & 0xFF00_0000u32) >> 24) as u8,
            ),
        }
    }
    pub fn from_f32_tuple(color: (f32, f32, f32, f32)) -> Color {
        let tou8 = |c| (c * 255.0) as u8;
        Color { color: (tou8(color.0), tou8(color.1), tou8(color.2), tou8(color.3)) }
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

    /// Returns the H,S,V (Hue, Saturation, Value) representation
    /// of color.
    ///
    pub fn as_hsv(&self) -> (f32, f32, f32) {
        Color::hsv_from_u8((self.color.0, self.color.1, self.color.2))
    }

    pub fn as_u8_tuple(&self) -> (u8, u8, u8, u8) {
        self.color
    }
    pub fn as_f32_tuple(&self) -> (f32, f32, f32, f32) {
        let tof32 = |c| c as f32 / 255.0;
        (tof32(self.color.0), tof32(self.color.1), tof32(self.color.2), tof32(self.color.3))
    }
    pub fn as_f32_array(&self) -> [f32; 4] {
        let c = self.as_f32_tuple();
        [c.0, c.1, c.2, c.3]
    }
    pub fn as_u32(&self) -> u32 {
        let c = self.color;
        ((c.0 as u32) << 24) | ((c.1 as u32) << 16) | ((c.2 as u32) << 8) | (c.3 as u32)
    }

    pub fn set_red(&mut self, r: u8) {
        self.color.0 = r;
    }
    pub fn set_green(&mut self, g: u8) {
        self.color.1 = g;
    }
    pub fn set_blue(&mut self, b: u8) {
        self.color.2 = b;
    }
    pub fn set_alpha(&mut self, a: u8) {
        self.color.3 = a;
    }

    pub fn r(&self) -> u8 {
        self.color.0
    }
    pub fn g(&self) -> u8 {
        self.color.1
    }
    pub fn b(&self) -> u8 {
        self.color.2
    }
    pub fn a(&self) -> u8 {
        self.color.3
    }

    pub const WHITE: Color = Color { color: (255, 255, 255, 255) };
    pub const BLACK: Color = Color { color: (0, 0, 0, 255) };
    pub const RED: Color = Color { color: (255, 0, 0, 255) };
    pub const GREEN: Color = Color { color: (0, 255, 0, 255) };
    pub const BLUE: Color = Color { color: (0, 0, 255, 255) };
    pub const YELLOW: Color = Color { color: (255, 255, 0, 255) };
    pub const MAGENTA: Color = Color { color: (255, 0, 255, 255) };
    pub const CYAN: Color = Color { color: (0, 255, 255, 255) };
    pub const GRAY: Color = Color { color: (128, 128, 128, 255) };
    pub const HOT_PINK: Color = Color { color: (255, 105, 180, 255) };
}

#[test]
fn test_rgb_to_hsv() {
    fn case(r: u8, g: u8, b: u8) -> (f32, f32, f32) {
        Color::hsv_from_u8((r, g, b))
    }

    fn feq(a: f32, b: f32, epsillon: f32) -> bool {
        a <= b + epsillon && a >= b - epsillon
    }

    fn deg_2_rad(d: f32) -> f32 {
        (std::f32::consts::PI / 180.0) * d
    }

    fn hsv_eq(a: (f32, f32, f32), b: (f32, f32, f32)) -> bool {
        const F_EPSILLON: f32 = 0.01;
        const A_EPSILLON: f32 = std::f32::consts::PI * 2.0 / (511.0);
        feq(a.0, b.0, A_EPSILLON) && feq(a.1, b.1, F_EPSILLON) && feq(a.2, b.2, F_EPSILLON)
    }

    // Test values via: https://www.rapidtables.com/convert/color/rgb-to-hsv.html
    assert!(hsv_eq(case(255, 255, 255), (deg_2_rad(0.0), 0.0, 1.0))); // White
    assert!(hsv_eq(case(0, 0, 0), (deg_2_rad(0.0), 0.0, 0.0))); // Black
    assert!(hsv_eq(case(255, 0, 0), (deg_2_rad(0.0), 1.0, 1.0))); // Red
    assert!(hsv_eq(case(0, 255, 0), (deg_2_rad(120.0), 1.0, 1.0))); // Green or "Lime"
    assert!(hsv_eq(case(0, 0, 255), (deg_2_rad(240.0), 1.0, 1.0))); // Blue
    assert!(hsv_eq(case(255, 255, 0), (deg_2_rad(60.0), 1.0, 1.0))); // Yellow
    assert!(hsv_eq(case(255, 0, 255), (deg_2_rad(300.0), 1.0, 1.0))); // Magenta h = -60.0
    assert!(hsv_eq(case(0, 255, 255), (deg_2_rad(180.0), 1.0, 1.0))); // Cyan

    assert!(hsv_eq(case(191, 191, 191), (deg_2_rad(0.0), 0.0, 0.75))); // Silver
    assert!(hsv_eq(case(128, 128, 128), (deg_2_rad(0.0), 0.0, 0.5))); // Gray
    assert!(hsv_eq(case(128, 0, 0), (deg_2_rad(0.0), 1.0, 0.5))); // Maroon
    assert!(hsv_eq(case(128, 128, 0), (deg_2_rad(60.0), 1.0, 0.5))); // Olive
    assert!(hsv_eq(case(0, 128, 0), (deg_2_rad(120.0), 1.0, 0.5))); // Green
    assert!(hsv_eq(case(128, 0, 128), (deg_2_rad(300.0), 1.0, 0.5))); // Purple h: -60.0
    assert!(hsv_eq(case(0, 128, 128), (deg_2_rad(180.0), 1.0, 0.5))); // Teal
    assert!(hsv_eq(case(0, 0, 128), (deg_2_rad(240.0), 1.0, 0.5))); //  Navy
    assert!(hsv_eq(case(255, 105, 180), (deg_2_rad(330.0), 0.588, 1.0))); //  Hot pink h: -30.0
}
