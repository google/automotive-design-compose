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
use ::taffy::style_helpers::TaffyZero;
use dc_bundle::{
    positioning::{
        item_spacing, AlignContent, AlignItems, AlignSelf, FlexDirection, ItemSpacing,
        JustifyContent, PositionType,
    },
    Error,
};
use protobuf::MessageField;
use taffy::prelude as taffy;

impl TryIntoTaffy<taffy::AlignItems> for AlignItems {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::AlignItems, Self::Error> {
        match self {
            AlignItems::ALIGN_ITEMS_CENTER => Ok(taffy::AlignItems::Center),
            AlignItems::ALIGN_ITEMS_FLEX_START => Ok(taffy::AlignItems::FlexStart),
            AlignItems::ALIGN_ITEMS_FLEX_END => Ok(taffy::AlignItems::FlexEnd),
            AlignItems::ALIGN_ITEMS_BASELINE => Ok(taffy::AlignItems::Baseline),
            AlignItems::ALIGN_ITEMS_STRETCH => Ok(taffy::AlignItems::Stretch),
            AlignItems::ALIGN_ITEMS_UNSPECIFIED => {
                Err(Error::UnknownEnumVariant { enum_name: "AlignItems".to_string() })
            }
        }
    }
}

impl TryIntoTaffy<Option<taffy::AlignItems>> for AlignSelf {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<Option<taffy::AlignItems>, Self::Error> {
        match self {
            AlignSelf::ALIGN_SELF_AUTO => Ok(None),
            AlignSelf::ALIGN_SELF_FLEX_START => Ok(Some(taffy::AlignItems::FlexStart)),
            AlignSelf::ALIGN_SELF_FLEX_END => Ok(Some(taffy::AlignItems::FlexEnd)),
            AlignSelf::ALIGN_SELF_CENTER => Ok(Some(taffy::AlignItems::Center)),
            AlignSelf::ALIGN_SELF_BASELINE => Ok(Some(taffy::AlignItems::Baseline)),
            AlignSelf::ALIGN_SELF_STRETCH => Ok(Some(taffy::AlignItems::Stretch)),
            AlignSelf::ALIGN_SELF_UNSPECIFIED => {
                Err(Error::UnknownEnumVariant { enum_name: "AlignSelf".to_string() })
            }
        }
    }
}

impl TryIntoTaffy<taffy::AlignContent> for AlignContent {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::AlignContent, Self::Error> {
        match self {
            AlignContent::ALIGN_CONTENT_CENTER => Ok(taffy::AlignContent::Center),
            AlignContent::ALIGN_CONTENT_FLEX_START => Ok(taffy::AlignContent::FlexStart),
            AlignContent::ALIGN_CONTENT_FLEX_END => Ok(taffy::AlignContent::FlexEnd),
            AlignContent::ALIGN_CONTENT_SPACE_AROUND => Ok(taffy::AlignContent::SpaceAround),
            AlignContent::ALIGN_CONTENT_SPACE_BETWEEN => Ok(taffy::AlignContent::SpaceBetween),
            AlignContent::ALIGN_CONTENT_STRETCH => Ok(taffy::AlignContent::Stretch),
            AlignContent::ALIGN_CONTENT_UNSPECIFIED => {
                Err(Error::UnknownEnumVariant { enum_name: "AlignContent".to_string() })
            }
        }
    }
}

impl TryIntoTaffy<taffy::FlexDirection> for FlexDirection {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::FlexDirection, Self::Error> {
        match self {
            FlexDirection::FLEX_DIRECTION_ROW => Ok(taffy::FlexDirection::Row),
            FlexDirection::FLEX_DIRECTION_COLUMN => Ok(taffy::FlexDirection::Column),
            FlexDirection::FLEX_DIRECTION_ROW_REVERSE => Ok(taffy::FlexDirection::RowReverse),
            FlexDirection::FLEX_DIRECTION_COLUMN_REVERSE => Ok(taffy::FlexDirection::ColumnReverse),
            FlexDirection::FLEX_DIRECTION_NONE => Ok(taffy::FlexDirection::Row),
            FlexDirection::FLEX_DIRECTION_UNSPECIFIED => {
                Err(Error::UnknownEnumVariant { enum_name: "FlexDirection".to_string() })
            }
        }
    }
}

impl TryIntoTaffy<taffy::JustifyContent> for JustifyContent {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::JustifyContent, Self::Error> {
        match self {
            JustifyContent::JUSTIFY_CONTENT_CENTER => Ok(taffy::JustifyContent::Center),
            JustifyContent::JUSTIFY_CONTENT_FLEX_START => Ok(taffy::JustifyContent::FlexStart),
            JustifyContent::JUSTIFY_CONTENT_FLEX_END => Ok(taffy::JustifyContent::FlexEnd),
            JustifyContent::JUSTIFY_CONTENT_SPACE_AROUND => Ok(taffy::JustifyContent::SpaceAround),
            JustifyContent::JUSTIFY_CONTENT_SPACE_BETWEEN => {
                Ok(taffy::JustifyContent::SpaceBetween)
            }
            JustifyContent::JUSTIFY_CONTENT_SPACE_EVENLY => Ok(taffy::JustifyContent::SpaceEvenly),
            JustifyContent::JUSTIFY_CONTENT_UNSPECIFIED => {
                Err(Error::UnknownEnumVariant { enum_name: "JustifyContent".to_string() })
            }
        }
    }
}

impl TryIntoTaffy<taffy::Position> for PositionType {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::Position, Self::Error> {
        match self {
            PositionType::POSITION_TYPE_ABSOLUTE => Ok(taffy::Position::Absolute),
            PositionType::POSITION_TYPE_RELATIVE => Ok(taffy::Position::Relative),
            PositionType::POSITION_TYPE_UNSPECIFIED => {
                Err(Error::UnknownEnumVariant { enum_name: "PositionType".to_string() })
            }
        }
    }
}
impl TryIntoTaffy<taffy::LengthPercentage> for &MessageField<ItemSpacing> {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::LengthPercentage, Self::Error> {
        match self.as_ref() {
            Some(spacing) => match spacing.ItemSpacingType {
                Some(item_spacing::ItemSpacingType::Fixed(s)) => {
                    Ok(taffy::LengthPercentage::Length(s as f32))
                }
                Some(item_spacing::ItemSpacingType::Auto(..)) => Ok(taffy::LengthPercentage::ZERO),
                Some(_) => Err(dc_bundle::Error::UnknownEnumVariant {
                    enum_name: "ItemSpacing".to_string(),
                }),
                None => Err(dc_bundle::Error::UnknownEnumVariant {
                    enum_name: "ItemSpacing".to_string(),
                }),
            },
            None => Err(Error::MissingFieldError { field: "ItemSpacing".to_string() }),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_align_items_try_into_taffy() {
        assert_eq!(
            AlignItems::ALIGN_ITEMS_FLEX_START.try_into_taffy().unwrap(),
            taffy::AlignItems::FlexStart
        );
        assert_eq!(
            AlignItems::ALIGN_ITEMS_FLEX_END.try_into_taffy().unwrap(),
            taffy::AlignItems::FlexEnd
        );
        assert_eq!(
            AlignItems::ALIGN_ITEMS_CENTER.try_into_taffy().unwrap(),
            taffy::AlignItems::Center
        );
        assert_eq!(
            AlignItems::ALIGN_ITEMS_BASELINE.try_into_taffy().unwrap(),
            taffy::AlignItems::Baseline
        );
        assert_eq!(
            AlignItems::ALIGN_ITEMS_STRETCH.try_into_taffy().unwrap(),
            taffy::AlignItems::Stretch
        );
        assert!(AlignItems::ALIGN_ITEMS_UNSPECIFIED.try_into_taffy().is_err());
    }

    #[test]
    fn test_align_self_try_into_taffy() {
        assert_eq!(AlignSelf::ALIGN_SELF_AUTO.try_into_taffy().unwrap(), None);
        assert_eq!(
            AlignSelf::ALIGN_SELF_FLEX_START.try_into_taffy().unwrap(),
            Some(taffy::AlignItems::FlexStart)
        );
        assert_eq!(
            AlignSelf::ALIGN_SELF_FLEX_END.try_into_taffy().unwrap(),
            Some(taffy::AlignItems::FlexEnd)
        );
        assert_eq!(
            AlignSelf::ALIGN_SELF_CENTER.try_into_taffy().unwrap(),
            Some(taffy::AlignItems::Center)
        );
        assert_eq!(
            AlignSelf::ALIGN_SELF_BASELINE.try_into_taffy().unwrap(),
            Some(taffy::AlignItems::Baseline)
        );
        assert_eq!(
            AlignSelf::ALIGN_SELF_STRETCH.try_into_taffy().unwrap(),
            Some(taffy::AlignItems::Stretch)
        );
        assert!(AlignSelf::ALIGN_SELF_UNSPECIFIED.try_into_taffy().is_err());
    }

    #[test]
    fn test_align_content_try_into_taffy() {
        assert_eq!(
            AlignContent::ALIGN_CONTENT_FLEX_START.try_into_taffy().unwrap(),
            taffy::AlignContent::FlexStart
        );
        assert_eq!(
            AlignContent::ALIGN_CONTENT_FLEX_END.try_into_taffy().unwrap(),
            taffy::AlignContent::FlexEnd
        );
        assert_eq!(
            AlignContent::ALIGN_CONTENT_CENTER.try_into_taffy().unwrap(),
            taffy::AlignContent::Center
        );
        assert_eq!(
            AlignContent::ALIGN_CONTENT_STRETCH.try_into_taffy().unwrap(),
            taffy::AlignContent::Stretch
        );
        assert_eq!(
            AlignContent::ALIGN_CONTENT_SPACE_BETWEEN.try_into_taffy().unwrap(),
            taffy::AlignContent::SpaceBetween
        );
        assert_eq!(
            AlignContent::ALIGN_CONTENT_SPACE_AROUND.try_into_taffy().unwrap(),
            taffy::AlignContent::SpaceAround
        );
        assert!(AlignContent::ALIGN_CONTENT_UNSPECIFIED.try_into_taffy().is_err());
    }

    #[test]
    fn test_flex_direction_try_into_taffy() {
        assert_eq!(
            FlexDirection::FLEX_DIRECTION_ROW.try_into_taffy().unwrap(),
            taffy::FlexDirection::Row
        );
        assert_eq!(
            FlexDirection::FLEX_DIRECTION_COLUMN.try_into_taffy().unwrap(),
            taffy::FlexDirection::Column
        );
        assert_eq!(
            FlexDirection::FLEX_DIRECTION_ROW_REVERSE.try_into_taffy().unwrap(),
            taffy::FlexDirection::RowReverse
        );
        assert_eq!(
            FlexDirection::FLEX_DIRECTION_COLUMN_REVERSE.try_into_taffy().unwrap(),
            taffy::FlexDirection::ColumnReverse
        );
        assert!(FlexDirection::FLEX_DIRECTION_UNSPECIFIED.try_into_taffy().is_err());
    }

    #[test]
    fn test_justify_content_try_into_taffy() {
        assert_eq!(
            JustifyContent::JUSTIFY_CONTENT_FLEX_START.try_into_taffy().unwrap(),
            taffy::JustifyContent::FlexStart
        );
        assert_eq!(
            JustifyContent::JUSTIFY_CONTENT_FLEX_END.try_into_taffy().unwrap(),
            taffy::JustifyContent::FlexEnd
        );
        assert_eq!(
            JustifyContent::JUSTIFY_CONTENT_CENTER.try_into_taffy().unwrap(),
            taffy::JustifyContent::Center
        );
        assert_eq!(
            JustifyContent::JUSTIFY_CONTENT_SPACE_BETWEEN.try_into_taffy().unwrap(),
            taffy::JustifyContent::SpaceBetween
        );
        assert_eq!(
            JustifyContent::JUSTIFY_CONTENT_SPACE_AROUND.try_into_taffy().unwrap(),
            taffy::JustifyContent::SpaceAround
        );
        assert_eq!(
            JustifyContent::JUSTIFY_CONTENT_SPACE_EVENLY.try_into_taffy().unwrap(),
            taffy::JustifyContent::SpaceEvenly
        );
        assert!(JustifyContent::JUSTIFY_CONTENT_UNSPECIFIED.try_into_taffy().is_err());
    }

    #[test]
    fn test_position_type_try_into_taffy() {
        assert_eq!(
            PositionType::POSITION_TYPE_RELATIVE.try_into_taffy().unwrap(),
            taffy::Position::Relative
        );
        assert_eq!(
            PositionType::POSITION_TYPE_ABSOLUTE.try_into_taffy().unwrap(),
            taffy::Position::Absolute
        );
        assert!(PositionType::POSITION_TYPE_UNSPECIFIED.try_into_taffy().is_err());
    }

    #[test]
    fn test_item_spacing_try_into_taffy() {
        let fixed_spacing = ItemSpacing {
            ItemSpacingType: Some(item_spacing::ItemSpacingType::Fixed(10)),
            ..Default::default()
        };
        let fixed_spacing_field: MessageField<ItemSpacing> = Some(fixed_spacing).into();
        assert_eq!(
            (&fixed_spacing_field).try_into_taffy().unwrap(),
            taffy::LengthPercentage::Length(10.0)
        );

        let auto_spacing = ItemSpacing {
            ItemSpacingType: Some(item_spacing::ItemSpacingType::Auto(item_spacing::Auto::new())),
            ..Default::default()
        };
        let auto_spacing_field: MessageField<ItemSpacing> = Some(auto_spacing).into();
        assert_eq!((&auto_spacing_field).try_into_taffy().unwrap(), taffy::LengthPercentage::ZERO);

        let none_spacing: MessageField<ItemSpacing> = None.into();
        assert!((&none_spacing).try_into_taffy().is_err());
    }
}
