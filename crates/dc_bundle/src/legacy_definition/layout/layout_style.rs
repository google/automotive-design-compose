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

use crate::Error;
use serde::{Deserialize, Serialize};

use crate::legacy_definition::element::geometry::{Dimension, Rect, Size};
use crate::legacy_definition::layout::positioning::{
    AlignContent, AlignItems, AlignSelf, FlexDirection, ItemSpacing, JustifyContent, PositionType,
};
use crate::legacy_definition::proto;

#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct LayoutStyle {
    pub margin: Rect<Dimension>,
    pub padding: Rect<Dimension>,
    pub item_spacing: ItemSpacing,
    pub top: Dimension,
    pub left: Dimension,
    pub bottom: Dimension,
    pub right: Dimension,
    pub width: Dimension,
    pub height: Dimension,
    pub min_width: Dimension,
    pub max_width: Dimension,
    pub min_height: Dimension,
    pub max_height: Dimension,
    pub bounding_box: Size<f32>,
    pub flex_grow: f32,
    pub flex_shrink: f32,
    pub flex_basis: Dimension,
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
            margin: Rect::<Dimension>::default(),
            padding: Rect::<Dimension>::default(),
            item_spacing: ItemSpacing::default(),
            top: Dimension::default(),
            left: Dimension::default(),
            bottom: Dimension::default(),
            right: Dimension::default(),
            width: Dimension::default(),
            height: Dimension::default(),
            min_width: Dimension::default(),
            max_width: Dimension::default(),
            min_height: Dimension::default(),
            max_height: Dimension::default(),
            bounding_box: Size::default(),
            flex_grow: 0.0,
            flex_shrink: 0.0,
            flex_basis: Dimension::default(),
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
            margin: proto
                .margin
                .clone()
                .ok_or(Error::MissingFieldError { field: "margin".to_string() })?
                .try_into()?,
            padding: proto
                .padding
                .clone()
                .ok_or(Error::MissingFieldError { field: "padding".to_string() })?
                .try_into()?,
            item_spacing: proto
                .item_spacing
                .clone()
                .ok_or(Error::MissingFieldError { field: "item_spacing".to_string() })?
                .try_into()?,
            top: proto.top.try_into()?,
            left: proto.left.try_into()?,
            right: proto.right.try_into()?,
            bottom: proto.bottom.try_into()?,
            width: proto.width.try_into()?,
            height: proto.height.try_into()?,
            min_width: proto.min_width.try_into()?,
            min_height: proto.min_height.try_into()?,
            max_width: proto.max_width.try_into()?,
            max_height: proto.max_height.try_into()?,
            bounding_box: proto
                .bounding_box
                .clone()
                .ok_or(Error::MissingFieldError { field: "bounding_box".to_string() })?
                .into(),
            flex_grow: proto.flex_grow,
            flex_shrink: proto.flex_shrink,
            flex_basis: proto.flex_basis.try_into()?,
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
