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

use crate::styles::ItemSpacing;
use crate::styles::{
    AlignContent, AlignItems, AlignSelf, FlexDirection, JustifyContent, PositionType,
};
use crate::types::{Dimension, Rect, Size};
use serde::{Deserialize, Serialize};

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
            align_self: AlignSelf::default(),
            align_content: AlignContent::default(),
            align_items: AlignItems::default(),
            flex_direction: FlexDirection::default(),
            justify_content: JustifyContent::default(),
            position_type: PositionType::default(),
        }
    }
}

impl Into<taffy::prelude::Style> for &LayoutStyle {
    fn into(self) -> taffy::prelude::Style {
        let mut tstyle = taffy::prelude::Style::default();

        tstyle.padding.left = (&self.padding.start).into();
        tstyle.padding.right = (&self.padding.end).into();
        tstyle.padding.top = (&self.padding.top).into();
        tstyle.padding.bottom = (&self.padding.bottom).into();

        tstyle.flex_grow = self.flex_grow;
        tstyle.flex_shrink = self.flex_shrink;
        tstyle.flex_basis = (&self.flex_basis).into();
        tstyle.gap.width = (&self.item_spacing).into();
        tstyle.gap.height = (&self.item_spacing).into();

        tstyle.align_content = Some((&self.align_content).into());
        tstyle.justify_content = Some((&self.justify_content).into());
        tstyle.align_items = Some((&self.align_items).into());
        tstyle.flex_direction = (&self.flex_direction).into();
        tstyle.align_self = (&self.align_self).into();

        tstyle.size.width = (&self.width).into();
        tstyle.size.height = (&self.height).into();
        tstyle.min_size.width = (&self.min_width).into();
        tstyle.min_size.height = (&self.min_height).into();
        tstyle.max_size.width = (&self.max_width).into();
        tstyle.max_size.height = (&self.max_height).into();

        // If we have a fixed size, use the bounding box since that takes into
        // account scale and rotation, and disregard min/max sizes.
        // TODO support this with non-fixed sizes also!
        if self.width.is_points() {
            tstyle.size.width = taffy::prelude::Dimension::Points(self.bounding_box.width);
            tstyle.min_size.width = taffy::prelude::Dimension::Auto;
            tstyle.max_size.width = taffy::prelude::Dimension::Auto;
        }
        if self.height.is_points() {
            tstyle.size.height = taffy::prelude::Dimension::Points(self.bounding_box.height);
            tstyle.min_size.height = taffy::prelude::Dimension::Auto;
            tstyle.max_size.height = taffy::prelude::Dimension::Auto;
        }

        tstyle.position = (&self.position_type).into();
        tstyle.inset.left = (&self.left).into();
        tstyle.inset.right = (&self.right).into();
        tstyle.inset.top = (&self.top).into();
        tstyle.inset.bottom = (&self.bottom).into();

        tstyle.margin.left = (&self.margin.start).into();
        tstyle.margin.right = (&self.margin.end).into();
        tstyle.margin.top = (&self.margin.top).into();
        tstyle.margin.bottom = (&self.margin.bottom).into();

        tstyle.display = taffy::prelude::Display::Flex; // TODO set to None to hide
        tstyle
    }
}
