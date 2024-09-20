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
use crate::legacy_definition::layout::positioning::{
    AlignContent, AlignItems, AlignSelf, FlexDirection, ItemSpacing, JustifyContent, PositionType,
};
use crate::legacy_definition::proto;
use crate::Error;
use crate::Error::MissingFieldError;
use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct LayoutStyle {
    pub margin: Option<DimensionRect>,
    pub padding: Option<DimensionRect>,
    pub item_spacing: ItemSpacing,
    pub top: Option<DimensionProto>,
    pub left: Option<DimensionProto>,
    pub bottom: Option<DimensionProto>,
    pub right: Option<DimensionProto>,
    pub width: Option<DimensionProto>,
    pub height: Option<DimensionProto>,
    pub min_width: Option<DimensionProto>,
    pub max_width: Option<DimensionProto>,
    pub min_height: Option<DimensionProto>,
    pub max_height: Option<DimensionProto>,
    pub bounding_box: Size,
    pub flex_grow: f32,
    pub flex_shrink: f32,
    pub flex_basis: Option<DimensionProto>,
    pub align_self: AlignSelf,
    pub align_content: AlignContent,
    pub align_items: AlignItems,
    pub flex_direction: FlexDirection,
    pub justify_content: JustifyContent,
    pub position_type: PositionType,
}

impl Default for LayoutStyle {
    fn default() -> LayoutStyle {
        LayoutStyle {
            margin: (DimensionRect::new()),
            padding: (DimensionRect::new()),
            item_spacing: ItemSpacing::default(),
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
            bounding_box: Size::default(),
            flex_grow: 0.0,
            flex_shrink: 0.0,
            flex_basis: DimensionProto::new_undefined(),
            align_self: AlignSelf::Auto,
            align_content: AlignContent::Stretch,
            align_items: AlignItems::Stretch,
            flex_direction: FlexDirection::Row,
            justify_content: JustifyContent::FlexStart,
            position_type: PositionType::Relative,
        }
    }
}

impl TryFrom<proto::layout::LayoutStyle> for LayoutStyle {
    type Error = Error;

    fn try_from(proto: proto::layout::LayoutStyle) -> Result<Self, Self::Error> {
        let layout_style = LayoutStyle {
            margin: proto.margin.clone(),
            padding: proto.padding.clone(),
            item_spacing: proto
                .item_spacing
                .clone()
                .ok_or(MissingFieldError { field: "item_spacing".to_string() })?
                .try_into()?,
            top: proto.top,
            left: proto.left,
            right: proto.right,
            bottom: proto.bottom,
            width: proto.width,
            height: proto.height,
            min_width: proto.min_width,
            min_height: proto.min_height,
            max_width: proto.max_width,
            max_height: proto.max_height,
            bounding_box: proto
                .bounding_box
                .clone()
                .ok_or(MissingFieldError { field: "bounding_box".to_string() })?
                .into(),
            flex_grow: proto.flex_grow,
            flex_shrink: proto.flex_shrink,
            flex_basis: proto.flex_basis,
            align_self: proto.align_self().try_into()?,
            align_content: proto.align_content().try_into()?,
            align_items: proto.align_items().try_into()?,
            flex_direction: proto.flex_direction().try_into()?,
            justify_content: proto.justify_content().try_into()?,
            position_type: proto.position_type().try_into()?,
        };
        Ok(layout_style)
    }
}
