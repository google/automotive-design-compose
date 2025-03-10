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

use crate::geometry::{DimensionProto, DimensionRect, Size};
use crate::layout_style::LayoutStyle;
use crate::positioning::item_spacing::{self, ItemSpacingType};
use crate::positioning::{
    AlignContent, AlignItems, AlignSelf, FlexDirection, ItemSpacing, JustifyContent, PositionType,
};
use crate::Error;
use crate::Error::MissingFieldError;

impl ItemSpacing {
    pub fn new_default() -> Option<Self> {
        Some(Self { ItemSpacingType: Some(ItemSpacingType::Fixed(0)), ..Default::default() })
    }
}

impl LayoutStyle {
    pub fn bounding_box(&self) -> Result<&Size, Error> {
        self.bounding_box.as_ref().ok_or(MissingFieldError { field: "bounding_box".to_string() })
    }

    pub(crate) fn new_default() -> LayoutStyle {
        LayoutStyle {
            margin: Some(DimensionRect::new_with_default_value()).into(),
            padding: Some(DimensionRect::new_with_default_value()).into(),
            item_spacing: ItemSpacing::new_default().into(),
            top: DimensionProto::new_undefined(),
            left: DimensionProto::new_undefined(),
            bottom: DimensionProto::new_undefined(),
            right: DimensionProto::new_undefined(),
            width: DimensionProto::new_undefined(),
            height: DimensionProto::new_undefined(),
            min_width: DimensionProto::new_undefined(),
            max_width: DimensionProto::new_undefined(),
            min_height: DimensionProto::new_undefined(),
            max_height: DimensionProto::new_undefined(),
            bounding_box: Some(Size::default()).into(),
            flex_grow: 0.0,
            flex_shrink: 0.0,
            flex_basis: DimensionProto::new_undefined(),
            align_self: AlignSelf::ALIGN_SELF_AUTO.into(),
            align_content: AlignContent::ALIGN_CONTENT_STRETCH.into(),
            align_items: AlignItems::ALIGN_ITEMS_STRETCH.into(),
            flex_direction: FlexDirection::FLEX_DIRECTION_ROW.into(),
            justify_content: JustifyContent::JUSTIFY_CONTENT_FLEX_START.into(),
            position_type: PositionType::POSITION_TYPE_RELATIVE.into(),
            ..Default::default()
        }
    }
}

impl ItemSpacing {
    pub fn new_fixed(value: i32) -> Self {
        Self {
            ItemSpacingType: Some(item_spacing::ItemSpacingType::Fixed(value)),
            ..Default::default()
        }
    }
    pub fn new_auto(width: i32, height: i32) -> Self {
        Self {
            ItemSpacingType: Some(item_spacing::ItemSpacingType::Auto(item_spacing::Auto {
                width,
                height,
                ..Default::default()
            })),
            ..Default::default()
        }
    }
}
