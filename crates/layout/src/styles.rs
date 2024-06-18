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

use crate::IntoTaffy;
use dc_bundle::legacy_definition::layout::grid::ItemSpacing;
use dc_bundle::legacy_definition::layout::positioning::{
    AlignContent, AlignItems, AlignSelf, FlexDirection, JustifyContent, PositionType,
};
use taffy::prelude as taffy;

impl IntoTaffy<taffy::AlignItems> for &AlignItems {
    fn into_taffy(self) -> taffy::AlignItems {
        match self {
            AlignItems::Center => taffy::AlignItems::Center,
            AlignItems::FlexStart => taffy::AlignItems::FlexStart,
            AlignItems::FlexEnd => taffy::AlignItems::FlexEnd,
            AlignItems::Baseline => taffy::AlignItems::Baseline,
            AlignItems::Stretch => taffy::AlignItems::Stretch,
        }
    }
}

impl IntoTaffy<Option<taffy::AlignItems>> for &AlignSelf {
    fn into_taffy(self) -> Option<taffy::AlignItems> {
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

impl IntoTaffy<taffy::AlignContent> for &AlignContent {
    fn into_taffy(self) -> taffy::AlignContent {
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

impl IntoTaffy<taffy::FlexDirection> for &FlexDirection {
    fn into_taffy(self) -> taffy::FlexDirection {
        match self {
            FlexDirection::Row => taffy::FlexDirection::Row,
            FlexDirection::Column => taffy::FlexDirection::Column,
            FlexDirection::RowReverse => taffy::FlexDirection::RowReverse,
            FlexDirection::ColumnReverse => taffy::FlexDirection::ColumnReverse,
            FlexDirection::None => taffy::FlexDirection::Row,
        }
    }
}

impl IntoTaffy<taffy::JustifyContent> for &JustifyContent {
    fn into_taffy(self) -> taffy::JustifyContent {
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

impl IntoTaffy<taffy::Position> for &PositionType {
    fn into_taffy(self) -> taffy::Position {
        match self {
            PositionType::Absolute => taffy::Position::Absolute,
            PositionType::Relative => taffy::Position::Relative,
        }
    }
}

impl IntoTaffy<taffy::LengthPercentage> for &ItemSpacing {
    fn into_taffy(self) -> taffy::LengthPercentage {
        match self {
            ItemSpacing::Fixed(s) => taffy::LengthPercentage::Points(*s as f32),
            ItemSpacing::Auto(..) => taffy::LengthPercentage::Points(0.0),
        }
    }
}
