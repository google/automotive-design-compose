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

use crate::definition::element::color_or_var::ColorOrVar;
use serde::{Deserialize, Serialize};

/// Shadows can be applied to the border box, or the stroke box.
///
/// The border box is the box outside of the border for Outset shadows, and the box inside of the border for
/// inset shadows.
///
/// The stroke box is always the the outer edge of any stroke defined, or the edge of the view (ignoring the
/// border) if no strokes are present on the view.
#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize)]
pub enum ShadowBox {
    /// The shadow applies to the border box, either the inside of the border for inset shadows, or the outside
    /// of the border for outset shadows.
    BorderBox,
    /// The shadow applies to the stroke box. This is the outer edge of the stroke for both inset and ouset shadows.
    StrokeBox,
}

impl Default for ShadowBox {
    fn default() -> Self {
        Self::BorderBox
    }
}

/// BoxShadow defines a CSS-style box shadow, either outset or inset.
#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub enum BoxShadow {
    Outset {
        blur_radius: f32,
        spread_radius: f32,
        color: ColorOrVar,
        offset: (f32, f32),
        shadow_box: ShadowBox,
    },
    Inset {
        blur_radius: f32,
        spread_radius: f32,
        color: ColorOrVar,
        offset: (f32, f32),
        shadow_box: ShadowBox,
    },
}

impl BoxShadow {
    /// Create an outset box shadow.
    pub fn outset(
        blur_radius: f32,
        spread_radius: f32,
        color: ColorOrVar,
        offset: (f32, f32),
    ) -> BoxShadow {
        BoxShadow::Outset {
            blur_radius,
            spread_radius,
            color,
            offset,
            shadow_box: ShadowBox::BorderBox,
        }
    }
    /// Create an inset shadow.
    pub fn inset(
        blur_radius: f32,
        spread_radius: f32,
        color: ColorOrVar,
        offset: (f32, f32),
    ) -> BoxShadow {
        BoxShadow::Inset {
            blur_radius,
            spread_radius,
            color,
            offset,
            shadow_box: ShadowBox::BorderBox,
        }
    }
}

#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct TextShadow {
    pub blur_radius: f32,
    pub color: ColorOrVar,
    pub offset: (f32, f32),
}
