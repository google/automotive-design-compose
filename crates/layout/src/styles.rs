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
use taffy::prelude as taffy;

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum AlignItems {
    FlexStart,
    FlexEnd,
    Center,
    Baseline,
    Stretch,
}

impl Default for AlignItems {
    fn default() -> Self {
        Self::Stretch
    }
}

impl Into<taffy::AlignItems> for &AlignItems {
    fn into(self) -> taffy::AlignItems {
        match self {
            AlignItems::Center => taffy::AlignItems::Center,
            AlignItems::FlexStart => taffy::AlignItems::FlexStart,
            AlignItems::FlexEnd => taffy::AlignItems::FlexEnd,
            AlignItems::Baseline => taffy::AlignItems::Baseline,
            AlignItems::Stretch => taffy::AlignItems::Stretch,
        }
    }
}

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum AlignSelf {
    Auto,
    FlexStart,
    FlexEnd,
    Center,
    Baseline,
    Stretch,
}

impl Default for AlignSelf {
    fn default() -> Self {
        Self::Auto
    }
}

impl Into<Option<taffy::AlignItems>> for &AlignSelf {
    fn into(self) -> Option<taffy::AlignItems> {
        match self {
            AlignSelf::Auto => None,
            AlignSelf::FlexStart => Some(taffy::AlignItems::FlexStart),
            AlignSelf::FlexEnd => Some(taffy::AlignItems::FlexEnd),
            AlignSelf::Center => Some(taffy::AlignItems::Center),
            AlignSelf::Baseline => Some(taffy::AlignItems::Baseline),
            AlignSelf::Stretch => Some(taffy::AlignItems::Stretch),
        }
    }
}

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum AlignContent {
    FlexStart,
    FlexEnd,
    Center,
    Stretch,
    SpaceBetween,
    SpaceAround,
}

impl Default for AlignContent {
    fn default() -> Self {
        Self::Stretch
    }
}

impl Into<taffy::AlignContent> for &AlignContent {
    fn into(self) -> taffy::AlignContent {
        match self {
            AlignContent::Center => taffy::AlignContent::Center,
            AlignContent::FlexStart => taffy::AlignContent::FlexStart,
            AlignContent::FlexEnd => taffy::AlignContent::FlexEnd,
            AlignContent::SpaceAround => taffy::AlignContent::SpaceAround,
            AlignContent::SpaceBetween => taffy::AlignContent::SpaceBetween,
            AlignContent::Stretch => taffy::AlignContent::Stretch,
        }
    }
}

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum FlexDirection {
    Row,
    Column,
    RowReverse,
    ColumnReverse,
    None,
}

impl Default for FlexDirection {
    fn default() -> Self {
        Self::Row
    }
}

impl Into<taffy::FlexDirection> for &FlexDirection {
    fn into(self) -> taffy::FlexDirection {
        match self {
            FlexDirection::Row => taffy::FlexDirection::Row,
            FlexDirection::Column => taffy::FlexDirection::Column,
            FlexDirection::RowReverse => taffy::FlexDirection::RowReverse,
            FlexDirection::ColumnReverse => taffy::FlexDirection::ColumnReverse,
            FlexDirection::None => taffy::FlexDirection::Row,
        }
    }
}

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum JustifyContent {
    FlexStart,
    FlexEnd,
    Center,
    SpaceBetween,
    SpaceAround,
    SpaceEvenly,
}

impl Default for JustifyContent {
    fn default() -> Self {
        Self::FlexStart
    }
}

impl Into<taffy::JustifyContent> for &JustifyContent {
    fn into(self) -> taffy::JustifyContent {
        match self {
            JustifyContent::Center => taffy::JustifyContent::Center,
            JustifyContent::FlexStart => taffy::JustifyContent::FlexStart,
            JustifyContent::FlexEnd => taffy::JustifyContent::FlexEnd,
            JustifyContent::SpaceAround => taffy::JustifyContent::SpaceAround,
            JustifyContent::SpaceBetween => taffy::JustifyContent::SpaceBetween,
            JustifyContent::SpaceEvenly => taffy::JustifyContent::SpaceEvenly,
        }
    }
}

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum PositionType {
    Relative,
    Absolute,
}

impl Default for PositionType {
    fn default() -> Self {
        Self::Relative
    }
}

impl Into<taffy::Position> for &PositionType {
    fn into(self) -> taffy::Position {
        match self {
            PositionType::Absolute => taffy::Position::Absolute,
            PositionType::Relative => taffy::Position::Relative,
        }
    }
}

#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub enum ItemSpacing {
    Fixed(i32),     // Fixed space between columns/rows
    Auto(i32, i32), // Min space between columns/rows, item width/height
}

impl Default for ItemSpacing {
    fn default() -> Self {
        ItemSpacing::Fixed(0)
    }
}

impl Into<taffy::LengthPercentage> for &ItemSpacing {
    fn into(self) -> taffy::LengthPercentage {
        match self {
            ItemSpacing::Fixed(s) => taffy::LengthPercentage::Points(*s as f32),
            ItemSpacing::Auto(..) => taffy::LengthPercentage::Points(0.0),
        }
    }
}
