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

//! Flexbox definitions derived from `stretch` 0.3.2 licensed MIT.
//! https://github.com/vislyhq/stretch

use serde::{Deserialize, Serialize};

use crate::figma_schema;

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum Number {
    Defined(f32),
    Undefined,
}

impl Default for Number {
    fn default() -> Self {
        Self::Undefined
    }
}

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum Direction {
    Inherit,
    #[serde(rename = "ltr")]
    LTR,
    #[serde(rename = "rtl")]
    RTL,
}

impl Default for Direction {
    fn default() -> Self {
        Self::Inherit
    }
}

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum Display {
    #[serde(rename = "flex")]
    Flex,
    #[serde(rename = "none")]
    None,
}

impl Default for Display {
    fn default() -> Self {
        Self::Flex
    }
}

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum Overflow {
    Visible,
    Hidden,
    Scroll,
}

impl Default for Overflow {
    fn default() -> Self {
        Self::Visible
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq, Default, Copy)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum LayoutSizing {
    #[default]
    Fixed,
    Hug,
    Fill,
}

impl From<figma_schema::LayoutSizing> for LayoutSizing {
    fn from(layout_sizing: figma_schema::LayoutSizing) -> Self {
        match layout_sizing {
            figma_schema::LayoutSizing::Fill => LayoutSizing::Fill,
            figma_schema::LayoutSizing::Fixed => LayoutSizing::Fixed,
            figma_schema::LayoutSizing::Hug => LayoutSizing::Hug,
        }
    }
}
