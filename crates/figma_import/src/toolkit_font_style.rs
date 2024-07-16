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
use crate::utils::f32_eq;
use dc_bundle::legacy_definition::element::variable::NumOrVar;
use serde::{Deserialize, Serialize};
use std::{
    fmt::{self, Debug, Display, Formatter},
    hash::{Hash, Hasher},
};
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
        match (&self.0, &other.0) {
            (NumOrVar::Num(a), NumOrVar::Num(b)) => a == b,
            (
                NumOrVar::Var { id: id1, fallback: fb1 },
                NumOrVar::Var { id: id2, fallback: fb2 },
            ) => id1 == id2 && fb1 == fb2,
            _ => false,
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

/// Allows strikethrough or underline text decoration to be selected.
#[derive(Clone, Copy, PartialEq, Debug, Hash, Deserialize, Serialize, Default)]
pub enum TextDecoration {
    #[default]
    None,
    Strikethrough,
    Underline,
}

impl Display for TextDecoration {
    fn fmt(&self, f: &mut Formatter) -> fmt::Result {
        Debug::fmt(self, f)
    }
}
