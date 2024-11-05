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

use crate::into_taffy::TryIntoTaffy;
use dc_bundle::definition::layout::{
    item_spacing, AlignContent, AlignItems, AlignSelf, FlexDirection, ItemSpacing, JustifyContent,
    PositionType,
};
use dc_bundle::Error;
use taffy::prelude as taffy;

impl TryIntoTaffy<taffy::AlignItems> for AlignItems {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::AlignItems, Self::Error> {
        match self {
            AlignItems::Center => Ok(taffy::AlignItems::Center),
            AlignItems::FlexStart => Ok(taffy::AlignItems::FlexStart),
            AlignItems::FlexEnd => Ok(taffy::AlignItems::FlexEnd),
            AlignItems::Baseline => Ok(taffy::AlignItems::Baseline),
            AlignItems::Stretch => Ok(taffy::AlignItems::Stretch),
            AlignItems::Unspecified => {
                Err(Error::UnknownEnumVariant { enum_name: "AlignItems".to_string() })
            }
        }
    }
}

impl TryIntoTaffy<Option<taffy::AlignItems>> for AlignSelf {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<Option<taffy::AlignItems>, Self::Error> {
        match self {
            AlignSelf::Auto => Ok(None),
            AlignSelf::FlexStart => Ok(Some(taffy::AlignItems::FlexStart)),
            AlignSelf::FlexEnd => Ok(Some(taffy::AlignItems::FlexEnd)),
            AlignSelf::Center => Ok(Some(taffy::AlignItems::Center)),
            AlignSelf::Baseline => Ok(Some(taffy::AlignItems::Baseline)),
            AlignSelf::Stretch => Ok(Some(taffy::AlignItems::Stretch)),
            AlignSelf::Unspecified => {
                Err(Error::UnknownEnumVariant { enum_name: "AlignSelf".to_string() })
            }
        }
    }
}

impl TryIntoTaffy<taffy::AlignContent> for AlignContent {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::AlignContent, Self::Error> {
        match self {
            AlignContent::Center => Ok(taffy::AlignContent::Center),
            AlignContent::FlexStart => Ok(taffy::AlignContent::FlexStart),
            AlignContent::FlexEnd => Ok(taffy::AlignContent::FlexEnd),
            AlignContent::SpaceAround => Ok(taffy::AlignContent::SpaceAround),
            AlignContent::SpaceBetween => Ok(taffy::AlignContent::SpaceBetween),
            AlignContent::Stretch => Ok(taffy::AlignContent::Stretch),
            AlignContent::Unspecified => {
                Err(Error::UnknownEnumVariant { enum_name: "AlignContent".to_string() })
            }
        }
    }
}

impl TryIntoTaffy<taffy::FlexDirection> for FlexDirection {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::FlexDirection, Self::Error> {
        match self {
            FlexDirection::Row => Ok(taffy::FlexDirection::Row),
            FlexDirection::Column => Ok(taffy::FlexDirection::Column),
            FlexDirection::RowReverse => Ok(taffy::FlexDirection::ColumnReverse),
            FlexDirection::ColumnReverse => Ok(taffy::FlexDirection::ColumnReverse),
            FlexDirection::None => Ok(taffy::FlexDirection::Row),
            FlexDirection::Unspecified => {
                Err(Error::UnknownEnumVariant { enum_name: "FlexDirection".to_string() })
            }
        }
    }
}

impl TryIntoTaffy<taffy::JustifyContent> for JustifyContent {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::JustifyContent, Self::Error> {
        match self {
            JustifyContent::Center => Ok(taffy::JustifyContent::Center),
            JustifyContent::FlexStart => Ok(taffy::JustifyContent::FlexStart),
            JustifyContent::FlexEnd => Ok(taffy::JustifyContent::FlexEnd),
            JustifyContent::SpaceAround => Ok(taffy::JustifyContent::SpaceAround),
            JustifyContent::SpaceBetween => Ok(taffy::JustifyContent::SpaceBetween),
            JustifyContent::SpaceEvenly => Ok(taffy::JustifyContent::SpaceEvenly),
            JustifyContent::Unspecified => {
                Err(Error::UnknownEnumVariant { enum_name: "JustifyContent".to_string() })
            }
        }
    }
}

impl TryIntoTaffy<taffy::Position> for PositionType {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::Position, Self::Error> {
        match self {
            PositionType::Absolute => Ok(taffy::Position::Absolute),
            PositionType::Relative => Ok(taffy::Position::Relative),
            PositionType::Unspecified => {
                Err(Error::UnknownEnumVariant { enum_name: "PositionType".to_string() })
            }
        }
    }
}
impl TryIntoTaffy<taffy::LengthPercentage> for &Option<ItemSpacing> {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::LengthPercentage, Self::Error> {
        match self {
            Some(spacing) => match spacing.item_spacing_type {
                Some(item_spacing::ItemSpacingType::Fixed(s)) => {
                    Ok(taffy::LengthPercentage::Points(s as f32))
                }
                Some(item_spacing::ItemSpacingType::Auto(..)) => {
                    Ok(taffy::LengthPercentage::Points(0.0))
                }
                None => Err(dc_bundle::Error::UnknownEnumVariant {
                    enum_name: "ItemSpacing".to_string(),
                }),
            },
            None => Err(Error::MissingFieldError { field: "ItemSpacing".to_string() }),
        }
    }
}
