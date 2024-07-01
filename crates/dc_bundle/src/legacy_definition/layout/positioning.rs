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

use serde::{Deserialize, Serialize};

use crate::legacy_definition::proto;
use crate::Error;

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

impl TryFrom<proto::layout::AlignItems> for AlignItems {
    type Error = Error;
    fn try_from(proto: proto::layout::AlignItems) -> Result<Self, Self::Error> {
        match proto {
            proto::layout::AlignItems::FlexStart => Ok(AlignItems::FlexStart),
            proto::layout::AlignItems::FlexEnd => Ok(AlignItems::FlexEnd),
            proto::layout::AlignItems::Center => Ok(AlignItems::Center),
            proto::layout::AlignItems::Baseline => Ok(AlignItems::Baseline),
            proto::layout::AlignItems::Stretch => Ok(AlignItems::Stretch),
            _ => Err(Error::UnknownEnumVariant { enum_name: "AlignItems".to_string() }),
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

impl TryFrom<proto::layout::AlignSelf> for AlignSelf {
    type Error = Error;

    fn try_from(proto: proto::layout::AlignSelf) -> Result<Self, Self::Error> {
        match proto {
            proto::layout::AlignSelf::Auto => Ok(AlignSelf::Auto),
            proto::layout::AlignSelf::FlexStart => Ok(AlignSelf::FlexStart),
            proto::layout::AlignSelf::FlexEnd => Ok(AlignSelf::FlexEnd),
            proto::layout::AlignSelf::Center => Ok(AlignSelf::Center),
            proto::layout::AlignSelf::Baseline => Ok(AlignSelf::Baseline),
            proto::layout::AlignSelf::Stretch => Ok(AlignSelf::Stretch),
            _ => Err(Error::UnknownEnumVariant { enum_name: "AlignSelf".to_string() }),
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

impl TryFrom<proto::layout::AlignContent> for AlignContent {
    type Error = Error;

    fn try_from(proto: proto::layout::AlignContent) -> Result<Self, Self::Error> {
        match proto {
            proto::layout::AlignContent::Center => Ok(AlignContent::Center),
            proto::layout::AlignContent::FlexStart => Ok(AlignContent::FlexStart),
            proto::layout::AlignContent::FlexEnd => Ok(AlignContent::FlexEnd),
            proto::layout::AlignContent::Stretch => Ok(AlignContent::Stretch),
            proto::layout::AlignContent::SpaceBetween => Ok(AlignContent::SpaceBetween),
            proto::layout::AlignContent::SpaceAround => Ok(AlignContent::SpaceAround),
            _ => Err(Error::UnknownEnumVariant { enum_name: "AlignContent".to_string() }),
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

impl TryFrom<proto::layout::FlexDirection> for FlexDirection {
    type Error = Error;

    fn try_from(proto: proto::layout::FlexDirection) -> Result<Self, Self::Error> {
        match proto {
            proto::layout::FlexDirection::Row => Ok(FlexDirection::Row),
            proto::layout::FlexDirection::Column => Ok(FlexDirection::Column),
            proto::layout::FlexDirection::RowReverse => Ok(FlexDirection::RowReverse),
            proto::layout::FlexDirection::ColumnReverse => Ok(FlexDirection::ColumnReverse),
            proto::layout::FlexDirection::None => Ok(FlexDirection::None),
            _ => Err(Error::UnknownEnumVariant { enum_name: "FlexDirection".to_string() }),
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

impl TryFrom<proto::layout::JustifyContent> for JustifyContent {
    type Error = Error;

    fn try_from(proto: proto::layout::JustifyContent) -> Result<Self, Self::Error> {
        match proto {
            proto::layout::JustifyContent::FlexStart => Ok(JustifyContent::FlexStart),
            proto::layout::JustifyContent::FlexEnd => Ok(JustifyContent::FlexEnd),
            proto::layout::JustifyContent::Center => Ok(JustifyContent::Center),
            proto::layout::JustifyContent::SpaceBetween => Ok(JustifyContent::SpaceBetween),
            proto::layout::JustifyContent::SpaceAround => Ok(JustifyContent::SpaceAround),
            proto::layout::JustifyContent::SpaceEvenly => Ok(JustifyContent::SpaceEvenly),
            _ => Err(Error::UnknownEnumVariant { enum_name: "JustifyContent".to_string() }),
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

impl TryFrom<proto::layout::PositionType> for PositionType {
    type Error = Error;

    fn try_from(proto: proto::layout::PositionType) -> Result<Self, Self::Error> {
        match proto {
            proto::layout::PositionType::Relative => Ok(PositionType::Relative),
            proto::layout::PositionType::Absolute => Ok(PositionType::Absolute),
            _ => Err(Error::UnknownEnumVariant { enum_name: "PositionType".to_string() }),
        }
    }
}
