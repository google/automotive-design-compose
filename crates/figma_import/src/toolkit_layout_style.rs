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
use taffy::prelude as taffy;

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

#[derive(Debug, Copy, Clone, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Rect<T> {
    pub start: T,
    pub end: T,
    pub top: T,
    pub bottom: T,
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

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum PositionType {
    Relative,
    Absolute,
}

impl Into<taffy::Position> for &PositionType {
    fn into(self) -> taffy::Position {
        match self {
            PositionType::Absolute => taffy::Position::Absolute,
            PositionType::Relative => taffy::Position::Relative,
        }
    }
}

impl Default for PositionType {
    fn default() -> Self {
        Self::Relative
    }
}

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum FlexWrap {
    NoWrap,
    Wrap,
    WrapReverse,
}

impl Default for FlexWrap {
    fn default() -> Self {
        Self::NoWrap
    }
}

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum Dimension {
    Undefined,
    Auto,
    Points(f32),
    Percent(f32),
}

impl Into<taffy::LengthPercentage> for &Dimension {
    fn into(self) -> taffy::LengthPercentage {
        match self {
            Dimension::Percent(p) => taffy::LengthPercentage::Percent(*p),
            Dimension::Points(p) => taffy::LengthPercentage::Points(*p),
            _ => taffy::LengthPercentage::Points(0.0),
        }
    }
}

impl Into<taffy::LengthPercentageAuto> for &Dimension {
    fn into(self) -> taffy::LengthPercentageAuto {
        match self {
            Dimension::Percent(p) => taffy::LengthPercentageAuto::Percent(*p),
            Dimension::Points(p) => taffy::LengthPercentageAuto::Points(*p),
            Dimension::Auto => taffy::LengthPercentageAuto::Auto,
            _ => taffy::LengthPercentageAuto::Points(0.0),
        }
    }
}

impl Into<taffy::Dimension> for &Dimension {
    fn into(self) -> taffy::Dimension {
        match self {
            Dimension::Percent(p) => taffy::Dimension::Percent(*p),
            Dimension::Points(p) => taffy::Dimension::Points(*p),
            Dimension::Auto => taffy::Dimension::Auto,
            _ => taffy::Dimension::Auto,
        }
    }
}

impl Default for Dimension {
    fn default() -> Self {
        Self::Undefined
    }
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

impl Default for Size<Dimension> {
    fn default() -> Self {
        Self { width: Dimension::Auto, height: Dimension::Auto }
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
