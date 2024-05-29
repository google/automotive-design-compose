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

// This implementation is derived from font-kit 0.6.0, which is dual licensed under
// MIT and Apache 2.0. We are using it under the Apache 2.0 terms.
use crate::toolkit_schema::NumOrVar;
use crate::utils::f32_eq;
use serde::{Deserialize, Serialize};
use std::{
    fmt::{self, Debug, Display, Formatter},
    hash::{Hash, Hasher},
};

#[derive(Debug, Copy, Clone)]
pub struct FontMetrics {
    pub units_per_em: u32,
    pub ascent: f32,
    pub descent: f32,
    pub height: f32,
    pub underline_position: f32,
    pub underline_thickness: f32,
}
impl PartialEq for FontMetrics {
    fn eq(&self, other: &Self) -> bool {
        self.units_per_em == other.units_per_em
            && f32_eq(&self.ascent, &other.ascent)
            && f32_eq(&self.descent, &other.descent)
            && f32_eq(&self.height, &other.height)
            && f32_eq(&self.underline_position, &other.underline_position)
            && f32_eq(&self.underline_thickness, &other.underline_thickness)
    }
}
impl Hash for FontMetrics {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.units_per_em.hash(state);
        ((self.ascent * 64.0) as i32).hash(state);
        ((self.descent * 64.0) as i32).hash(state);
        ((self.height * 64.0) as i32).hash(state);
        ((self.underline_position * 64.0) as i32).hash(state);
        ((self.underline_thickness * 64.0) as i32).hash(state);
    }
}
/// Allows italic or oblique faces to be selected.
#[derive(Clone, Copy, PartialEq, Debug, Hash, Deserialize, Serialize, Default)]
pub enum FontStyle {
    /// A face that is neither italic not obliqued.
    #[default]
    Normal,
    /// A form that is generally cursive in nature.
    Italic,
    /// A typically-sloped version of the regular face.
    Oblique,
}

impl Display for FontStyle {
    fn fmt(&self, f: &mut Formatter) -> fmt::Result {
        Debug::fmt(self, f)
    }
}
/// The degree of blackness or stroke thickness of a font. This value ranges from 100.0 to 900.0,
/// with 400.0 as normal.
#[derive(Clone, Debug, Deserialize, Serialize)]
pub struct FontWeight(pub NumOrVar);
impl Default for FontWeight {
    #[inline]
    fn default() -> FontWeight {
        FontWeight::NORMAL
    }
}
impl PartialEq for FontWeight {
    fn eq(&self, other: &Self) -> bool {
        match &self.0 {
            NumOrVar::Num(n) => match &other.0 {
                NumOrVar::Num(o) => f32_eq(n, o),
                _ => false,
            },
            NumOrVar::Var(v) => match &other.0 {
                NumOrVar::Var(o) => v == o,
                _ => false,
            },
        }
    }
}
impl FontWeight {
    /// Thin weight (100), the thinnest value.
    pub const THIN: FontWeight = FontWeight(NumOrVar::Num(100.0));
    /// Extra light weight (200).
    pub const EXTRA_LIGHT: FontWeight = FontWeight(NumOrVar::Num(200.0));
    /// Light weight (300).
    pub const LIGHT: FontWeight = FontWeight(NumOrVar::Num(300.0));
    /// Normal (400).
    pub const NORMAL: FontWeight = FontWeight(NumOrVar::Num(400.0));
    /// Medium weight (500, higher than normal).
    pub const MEDIUM: FontWeight = FontWeight(NumOrVar::Num(500.0));
    /// Semibold weight (600).
    pub const SEMIBOLD: FontWeight = FontWeight(NumOrVar::Num(600.0));
    /// Bold weight (700).
    pub const BOLD: FontWeight = FontWeight(NumOrVar::Num(700.0));
    /// Extra-bold weight (800).
    pub const EXTRA_BOLD: FontWeight = FontWeight(NumOrVar::Num(800.0));
    /// Black weight (900), the thickest value.
    pub const BLACK: FontWeight = FontWeight(NumOrVar::Num(900.0));
}
/// The width of a font as an approximate fraction of the normal width.
///
/// Widths range from 0.5 to 2.0 inclusive, with 1.0 as the normal width.
#[derive(Clone, Copy, Debug, PartialOrd, Deserialize, Serialize)]
pub struct FontStretch(pub f32);
impl PartialEq for FontStretch {
    fn eq(&self, other: &Self) -> bool {
        f32_eq(&self.0, &other.0)
    }
}
impl Default for FontStretch {
    #[inline]
    fn default() -> FontStretch {
        FontStretch::NORMAL
    }
}
impl FontStretch {
    /// Ultra-condensed width (50%), the narrowest possible.
    pub const ULTRA_CONDENSED: FontStretch = FontStretch(0.5);
    /// Extra-condensed width (62.5%).
    pub const EXTRA_CONDENSED: FontStretch = FontStretch(0.625);
    /// Condensed width (75%).
    pub const CONDENSED: FontStretch = FontStretch(0.75);
    /// Semi-condensed width (87.5%).
    pub const SEMI_CONDENSED: FontStretch = FontStretch(0.875);
    /// Normal width (100%).
    pub const NORMAL: FontStretch = FontStretch(1.0);
    /// Semi-expanded width (112.5%).
    pub const SEMI_EXPANDED: FontStretch = FontStretch(1.125);
    /// Expanded width (125%).
    pub const EXPANDED: FontStretch = FontStretch(1.25);
    /// Extra-expanded width (150%).
    pub const EXTRA_EXPANDED: FontStretch = FontStretch(1.5);
    /// Ultra-expanded width (200%), the widest possible.
    pub const ULTRA_EXPANDED: FontStretch = FontStretch(2.0);
}
impl Hash for FontStretch {
    fn hash<H: Hasher>(&self, state: &mut H) {
        let x = (self.0 * 100.0) as i32;
        x.hash(state);
    }
}
