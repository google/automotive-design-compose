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
use crate::definition::element::{DimensionProto, DimensionRect, Size};
use crate::definition::layout::item_spacing::ItemSpacingType;
use crate::Error;
use crate::Error::MissingFieldError;

include!(concat!(env!("OUT_DIR"), "/designcompose.definition.layout.rs"));

impl ItemSpacing {
    pub fn new_default() -> Option<Self> {
        Some(Self { item_spacing_type: Some(ItemSpacingType::Fixed(0)) })
    }
}

impl LayoutStyle {
    pub fn bounding_box(&self) -> Result<&Size, Error> {
        self.bounding_box.as_ref().ok_or(MissingFieldError { field: "bounding_box".to_string() })
    }

    pub(crate) fn new_default() -> LayoutStyle {
        LayoutStyle {
            margin: DimensionRect::new(),
            padding: DimensionRect::new(),
            item_spacing: ItemSpacing::new_default(),
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
            bounding_box: Some(Size::default()),
            flex_grow: 0.0,
            flex_shrink: 0.0,
            flex_basis: DimensionProto::new_undefined(),
            align_self: AlignSelf::Auto.into(),
            align_content: AlignContent::Stretch.into(),
            align_items: AlignItems::Stretch.into(),
            flex_direction: FlexDirection::Row.into(),
            justify_content: JustifyContent::FlexStart.into(),
            position_type: PositionType::Relative.into(),
        }
    }
}

impl ItemSpacing {
    pub fn fixed(value: i32) -> Self {
        Self { item_spacing_type: Some(item_spacing::ItemSpacingType::Fixed(value)) }
    }
    pub fn auto(width: i32, height: i32) -> Self {
        Self {
            item_spacing_type: Some(item_spacing::ItemSpacingType::Auto(item_spacing::Auto {
                width,
                height,
            })),
        }
    }
}
