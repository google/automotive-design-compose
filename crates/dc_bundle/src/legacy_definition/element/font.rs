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
use crate::utils::f32_eq;
use serde::{Deserialize, Serialize};
use std::fmt;
use std::fmt::{Debug, Display, Formatter};
use std::hash::{Hash, Hasher};

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

/// This structure represents an OpenType feature, for example "tnum" controls tabular numbers
/// in fonts that support the feature.
#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct FontFeature {
    pub tag: [u8; 4],
    pub enabled: bool,
}

impl FontFeature {
    pub fn new(tag: &[u8; 4]) -> Self {
        FontFeature { tag: *tag, enabled: true }
    }
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
