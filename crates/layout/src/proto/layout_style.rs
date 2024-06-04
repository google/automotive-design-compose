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

use dc_proto::design::layout::LayoutStyle;

use crate::layout_style;

impl TryFrom<LayoutStyle> for layout_style::LayoutStyle {
    type Error = dc_proto::Error;

    fn try_from(proto: LayoutStyle) -> Result<Self, Self::Error> {
        let layout_style = layout_style::LayoutStyle {
            margin: proto
                .margin
                .clone()
                .ok_or(dc_proto::Error::MissingFieldError { field: "margin".to_string() })?
                .try_into()?,
            padding: proto
                .padding
                .clone()
                .ok_or(dc_proto::Error::MissingFieldError { field: "padding".to_string() })?
                .try_into()?,
            item_spacing: proto
                .item_spacing
                .clone()
                .ok_or(dc_proto::Error::MissingFieldError { field: "item_spacing".to_string() })?
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
                .ok_or(dc_proto::Error::MissingFieldError { field: "bounding_box".to_string() })?
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
