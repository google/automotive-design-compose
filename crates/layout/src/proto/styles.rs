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

use dc_proto::design::layout::{
    item_spacing, AlignContent, AlignItems, AlignSelf, FlexDirection, ItemSpacing, JustifyContent,
    PositionType,
};

use crate::styles;

impl TryFrom<AlignSelf> for styles::AlignSelf {
    type Error = dc_proto::Error;

    fn try_from(proto: AlignSelf) -> Result<Self, Self::Error> {
        match proto {
            AlignSelf::Auto => Ok(styles::AlignSelf::Auto),
            AlignSelf::FlexStart => Ok(styles::AlignSelf::FlexStart),
            AlignSelf::FlexEnd => Ok(styles::AlignSelf::FlexEnd),
            AlignSelf::Center => Ok(styles::AlignSelf::Center),
            AlignSelf::Baseline => Ok(styles::AlignSelf::Baseline),
            AlignSelf::Stretch => Ok(styles::AlignSelf::Stretch),
            _ => Err(dc_proto::Error::UnknownEnumVariant { enum_name: "AlignSelf".to_string() }),
        }
    }
}

impl TryFrom<AlignItems> for styles::AlignItems {
    type Error = dc_proto::Error;
    fn try_from(proto: AlignItems) -> Result<Self, Self::Error> {
        match proto {
            AlignItems::FlexStart => Ok(styles::AlignItems::FlexStart),
            AlignItems::FlexEnd => Ok(styles::AlignItems::FlexEnd),
            AlignItems::Center => Ok(styles::AlignItems::Center),
            AlignItems::Baseline => Ok(styles::AlignItems::Baseline),
            AlignItems::Stretch => Ok(styles::AlignItems::Stretch),
            _ => Err(dc_proto::Error::UnknownEnumVariant { enum_name: "AlignItems".to_string() }),
        }
    }
}

impl TryFrom<AlignContent> for styles::AlignContent {
    type Error = dc_proto::Error;

    fn try_from(proto: AlignContent) -> Result<Self, Self::Error> {
        match proto {
            AlignContent::Center => Ok(styles::AlignContent::Center),
            AlignContent::FlexStart => Ok(styles::AlignContent::FlexStart),
            AlignContent::FlexEnd => Ok(styles::AlignContent::FlexEnd),
            AlignContent::Stretch => Ok(styles::AlignContent::Stretch),
            AlignContent::SpaceBetween => Ok(styles::AlignContent::SpaceBetween),
            AlignContent::SpaceAround => Ok(styles::AlignContent::SpaceAround),
            _ => Err(dc_proto::Error::UnknownEnumVariant { enum_name: "AlignContent".to_string() }),
        }
    }
}

impl TryFrom<FlexDirection> for styles::FlexDirection {
    type Error = dc_proto::Error;

    fn try_from(proto: FlexDirection) -> Result<Self, Self::Error> {
        match proto {
            FlexDirection::Row => Ok(styles::FlexDirection::Row),
            FlexDirection::Column => Ok(styles::FlexDirection::Column),
            FlexDirection::RowReverse => Ok(styles::FlexDirection::RowReverse),
            FlexDirection::ColumnReverse => Ok(styles::FlexDirection::ColumnReverse),
            FlexDirection::None => Ok(styles::FlexDirection::None),
            _ => {
                Err(dc_proto::Error::UnknownEnumVariant { enum_name: "FlexDirection".to_string() })
            }
        }
    }
}

impl TryFrom<JustifyContent> for styles::JustifyContent {
    type Error = dc_proto::Error;

    fn try_from(proto: JustifyContent) -> Result<Self, Self::Error> {
        match proto {
            JustifyContent::FlexStart => Ok(styles::JustifyContent::FlexStart),
            JustifyContent::FlexEnd => Ok(styles::JustifyContent::FlexEnd),
            JustifyContent::Center => Ok(styles::JustifyContent::Center),
            JustifyContent::SpaceBetween => Ok(styles::JustifyContent::SpaceBetween),
            JustifyContent::SpaceAround => Ok(styles::JustifyContent::SpaceAround),
            JustifyContent::SpaceEvenly => Ok(styles::JustifyContent::SpaceEvenly),
            _ => {
                Err(dc_proto::Error::UnknownEnumVariant { enum_name: "JustifyContent".to_string() })
            }
        }
    }
}

impl TryFrom<PositionType> for styles::PositionType {
    type Error = dc_proto::Error;

    fn try_from(proto: PositionType) -> Result<Self, Self::Error> {
        match proto {
            PositionType::Relative => Ok(styles::PositionType::Relative),
            PositionType::Absolute => Ok(styles::PositionType::Absolute),
            _ => Err(dc_proto::Error::UnknownEnumVariant { enum_name: "PositionType".to_string() }),
        }
    }
}

impl TryFrom<ItemSpacing> for styles::ItemSpacing {
    type Error = dc_proto::Error;

    fn try_from(proto: ItemSpacing) -> Result<Self, Self::Error> {
        match proto
            .r#type
            .as_ref()
            .ok_or(dc_proto::Error::MissingFieldError { field: "ItemSpacing".to_string() })?
        {
            item_spacing::Type::Fixed(s) => Ok(styles::ItemSpacing::Fixed(*s)),
            item_spacing::Type::Auto(s) => Ok(styles::ItemSpacing::Auto(s.width, s.height)),
        }
    }
}
