// Copyright 2024 Google LLC
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

use crate::into_taffy::IntoTaffy;
use dc_bundle::definition::element::dimension_proto::Dimension;
use serde::{Deserialize, Serialize};
use taffy::style_helpers::TaffyZero;

impl IntoTaffy<taffy::prelude::LengthPercentage> for &Dimension {
    fn into_taffy(self) -> taffy::prelude::LengthPercentage {
        match self {
            Dimension::Percent(p) => taffy::prelude::LengthPercentage::Percent(*p),
            Dimension::Points(p) => taffy::prelude::LengthPercentage::Length(*p),
            _ => taffy::prelude::LengthPercentage::ZERO,
        }
    }
}

impl IntoTaffy<taffy::prelude::LengthPercentageAuto> for &Dimension {
    fn into_taffy(self) -> taffy::prelude::LengthPercentageAuto {
        match self {
            Dimension::Percent(p) => taffy::prelude::LengthPercentageAuto::Percent(*p),
            Dimension::Points(p) => taffy::prelude::LengthPercentageAuto::Length(*p),
            Dimension::Auto => taffy::prelude::LengthPercentageAuto::Auto,
            _ => taffy::prelude::LengthPercentageAuto::ZERO,
        }
    }
}

impl IntoTaffy<taffy::prelude::Dimension> for &Dimension {
    fn into_taffy(self) -> taffy::prelude::Dimension {
        match self {
            Dimension::Percent(p) => taffy::prelude::Dimension::Percent(*p),
            Dimension::Points(p) => taffy::prelude::Dimension::Length(*p),
            Dimension::Auto => taffy::prelude::Dimension::Auto,
            _ => taffy::prelude::Dimension::Auto,
        }
    }
}

/// The final result of a layout algorithm for a single taffy Node
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub struct Layout {
    // Relative ordering of the node. Render nodes with a higher order on top
    // of nodes with lower order
    pub order: u32,
    pub width: f32,
    pub height: f32,
    /// The top-left corner of the node
    pub left: f32,
    pub top: f32,
}

impl Layout {
    pub fn from_taffy_layout(l: &taffy::prelude::Layout) -> Layout {
        Layout {
            order: l.order,
            width: l.size.width,
            height: l.size.height,
            left: l.location.x,
            top: l.location.y,
        }
    }
}
