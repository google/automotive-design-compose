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

use serde::{Deserialize, Serialize};

pub trait Dimensionable {
    fn dimension(self) -> Dimension;
}
impl Dimensionable for Dimension {
    fn dimension(self) -> Dimension {
        self
    }
}
impl Dimensionable for f32 {
    fn dimension(self) -> Dimension {
        Dimension::Points(self)
    }
}
impl Dimensionable for i32 {
    fn dimension(self) -> Dimension {
        Dimension::Points(self as f32)
    }
}

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum Dimension {
    Undefined,
    Auto,
    Points(f32),
    Percent(f32),
}
impl Default for Dimension {
    fn default() -> Self {
        Self::Undefined
    }
}
impl Dimension {
    pub fn is_points(&self) -> bool {
        match self {
            Dimension::Points(_) => true,
            _ => false,
        }
    }
    pub fn points(&self) -> f32 {
        match self {
            Dimension::Points(p) => *p,
            _ => 0.0,
        }
    }
}

impl Into<taffy::prelude::LengthPercentage> for &Dimension {
    fn into(self) -> taffy::prelude::LengthPercentage {
        match self {
            Dimension::Percent(p) => taffy::prelude::LengthPercentage::Percent(*p),
            Dimension::Points(p) => taffy::prelude::LengthPercentage::Points(*p),
            _ => taffy::prelude::LengthPercentage::Points(0.0),
        }
    }
}

impl Into<taffy::prelude::LengthPercentageAuto> for &Dimension {
    fn into(self) -> taffy::prelude::LengthPercentageAuto {
        match self {
            Dimension::Percent(p) => taffy::prelude::LengthPercentageAuto::Percent(*p),
            Dimension::Points(p) => taffy::prelude::LengthPercentageAuto::Points(*p),
            Dimension::Auto => taffy::prelude::LengthPercentageAuto::Auto,
            _ => taffy::prelude::LengthPercentageAuto::Points(0.0),
        }
    }
}

impl Into<taffy::prelude::Dimension> for &Dimension {
    fn into(self) -> taffy::prelude::Dimension {
        match self {
            Dimension::Percent(p) => taffy::prelude::Dimension::Percent(*p),
            Dimension::Points(p) => taffy::prelude::Dimension::Points(*p),
            Dimension::Auto => taffy::prelude::Dimension::Auto,
            _ => taffy::prelude::Dimension::Auto,
        }
    }
}

#[derive(Debug, Copy, Clone, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Rect<T> {
    pub start: T,
    pub end: T,
    pub top: T,
    pub bottom: T,
}

impl Default for Rect<Dimension> {
    fn default() -> Self {
        Self {
            start: Default::default(),
            end: Default::default(),
            top: Default::default(),
            bottom: Default::default(),
        }
    }
}

#[derive(Debug, Copy, Clone, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Size<T> {
    pub width: T,
    pub height: T,
}

impl Default for Size<f32> {
    fn default() -> Self {
        Size { width: 0.0, height: 0.0 }
    }
}

impl Default for Size<Dimension> {
    fn default() -> Self {
        Self { width: Dimension::Auto, height: Dimension::Auto }
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
