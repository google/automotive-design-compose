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

use crate::definition::layout as proto;
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

impl TryFrom<proto::AlignItems> for AlignItems {
    type Error = Error;
    fn try_from(proto: proto::AlignItems) -> Result<Self, Self::Error> {
        match proto {
            proto::AlignItems::FlexStart => Ok(AlignItems::FlexStart),
            proto::AlignItems::FlexEnd => Ok(AlignItems::FlexEnd),
            proto::AlignItems::Center => Ok(AlignItems::Center),
            proto::AlignItems::Baseline => Ok(AlignItems::Baseline),
            proto::AlignItems::Stretch => Ok(AlignItems::Stretch),
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

impl TryFrom<proto::AlignSelf> for AlignSelf {
    type Error = Error;

    fn try_from(proto: proto::AlignSelf) -> Result<Self, Self::Error> {
        match proto {
            proto::AlignSelf::Auto => Ok(AlignSelf::Auto),
            proto::AlignSelf::FlexStart => Ok(AlignSelf::FlexStart),
            proto::AlignSelf::FlexEnd => Ok(AlignSelf::FlexEnd),
            proto::AlignSelf::Center => Ok(AlignSelf::Center),
            proto::AlignSelf::Baseline => Ok(AlignSelf::Baseline),
            proto::AlignSelf::Stretch => Ok(AlignSelf::Stretch),
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

impl TryFrom<proto::AlignContent> for AlignContent {
    type Error = Error;

    fn try_from(proto: proto::AlignContent) -> Result<Self, Self::Error> {
        match proto {
            proto::AlignContent::Center => Ok(AlignContent::Center),
            proto::AlignContent::FlexStart => Ok(AlignContent::FlexStart),
            proto::AlignContent::FlexEnd => Ok(AlignContent::FlexEnd),
            proto::AlignContent::Stretch => Ok(AlignContent::Stretch),
            proto::AlignContent::SpaceBetween => Ok(AlignContent::SpaceBetween),
            proto::AlignContent::SpaceAround => Ok(AlignContent::SpaceAround),
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

impl TryFrom<proto::FlexDirection> for FlexDirection {
    type Error = Error;

    fn try_from(proto: proto::FlexDirection) -> Result<Self, Self::Error> {
        match proto {
            proto::FlexDirection::Row => Ok(FlexDirection::Row),
            proto::FlexDirection::Column => Ok(FlexDirection::Column),
            proto::FlexDirection::RowReverse => Ok(FlexDirection::RowReverse),
            proto::FlexDirection::ColumnReverse => Ok(FlexDirection::ColumnReverse),
            proto::FlexDirection::None => Ok(FlexDirection::None),
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

impl TryFrom<proto::JustifyContent> for JustifyContent {
    type Error = Error;

    fn try_from(proto: proto::JustifyContent) -> Result<Self, Self::Error> {
        match proto {
            proto::JustifyContent::FlexStart => Ok(JustifyContent::FlexStart),
            proto::JustifyContent::FlexEnd => Ok(JustifyContent::FlexEnd),
            proto::JustifyContent::Center => Ok(JustifyContent::Center),
            proto::JustifyContent::SpaceBetween => Ok(JustifyContent::SpaceBetween),
            proto::JustifyContent::SpaceAround => Ok(JustifyContent::SpaceAround),
            proto::JustifyContent::SpaceEvenly => Ok(JustifyContent::SpaceEvenly),
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

impl TryFrom<proto::PositionType> for PositionType {
    type Error = Error;

    fn try_from(proto: proto::PositionType) -> Result<Self, Self::Error> {
        match proto {
            proto::PositionType::Relative => Ok(PositionType::Relative),
            proto::PositionType::Absolute => Ok(PositionType::Absolute),
            _ => Err(Error::UnknownEnumVariant { enum_name: "PositionType".to_string() }),
        }
    }
}
