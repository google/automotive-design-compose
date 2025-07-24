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

use dc_bundle::layout_style::LayoutStyle;

use crate::into_taffy::TryIntoTaffy;

impl TryIntoTaffy<taffy::prelude::Style> for &LayoutStyle {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::prelude::Style, Self::Error> {
        let mut tstyle = taffy::prelude::Style::default();

        tstyle.padding = self.padding.try_into_taffy()?;

        tstyle.flex_grow = self.flex_grow;
        tstyle.flex_shrink = self.flex_shrink;
        tstyle.flex_basis = self.flex_basis.try_into_taffy()?;
        tstyle.gap.width = self.item_spacing.try_into_taffy()?;
        tstyle.gap.height = self.item_spacing.try_into_taffy()?;

        tstyle.align_content = Some(self.align_content.enum_value_or_default().try_into_taffy()?);
        tstyle.justify_content =
            Some(self.justify_content.enum_value_or_default().try_into_taffy()?);
        tstyle.align_items = Some(self.align_items.enum_value_or_default().try_into_taffy()?);
        tstyle.flex_direction = self.flex_direction.enum_value_or_default().try_into_taffy()?;
        tstyle.align_self = self.align_self.enum_value_or_default().try_into_taffy()?;

        tstyle.size.width = self.width.try_into_taffy()?;
        tstyle.size.height = self.height.try_into_taffy()?;
        tstyle.min_size.width = self.min_width.try_into_taffy()?;
        tstyle.min_size.height = self.min_height.try_into_taffy()?;
        tstyle.max_size.width = self.max_width.try_into_taffy()?;
        tstyle.max_size.height = self.max_height.try_into_taffy()?;

        // If we have a fixed size, use the bounding box since that takes into
        // account scale and rotation, and disregard min/max sizes.
        // TODO support this with non-fixed sizes also!
        if self.width.has_points() {
            tstyle.size.width = taffy::prelude::Dimension::Length(self.bounding_box()?.width);
            tstyle.min_size.width = taffy::prelude::Dimension::Auto;
            tstyle.max_size.width = taffy::prelude::Dimension::Auto;
        }
        if self.height.has_points() {
            tstyle.size.height = taffy::prelude::Dimension::Length(self.bounding_box()?.height);
            tstyle.min_size.height = taffy::prelude::Dimension::Auto;
            tstyle.max_size.height = taffy::prelude::Dimension::Auto;
        }

        tstyle.position = self.position_type.enum_value_or_default().try_into_taffy()?;
        tstyle.inset.left = self.left.try_into_taffy()?;
        tstyle.inset.right = self.right.try_into_taffy()?;
        tstyle.inset.top = self.top.try_into_taffy()?;
        tstyle.inset.bottom = self.bottom.try_into_taffy()?;

        tstyle.margin = self.margin.try_into_taffy()?;

        tstyle.display = taffy::prelude::Display::Flex;

        tstyle.overflow.x = taffy::Overflow::Hidden;
        tstyle.overflow.y = taffy::Overflow::Hidden;

        Ok(tstyle)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use dc_bundle::geometry::{dimension_proto, DimensionProto, DimensionRect, Size};
    use dc_bundle::positioning::{
        AlignContent, AlignItems, AlignSelf, FlexDirection, ItemSpacing, JustifyContent,
        PositionType,
    };

    #[test]
    fn test_layout_style_try_into_taffy() {
        let mut layout_style = LayoutStyle {
            padding: Some(DimensionRect {
                start: DimensionProto::new_points(0.0),
                end: DimensionProto::new_points(0.0),
                top: DimensionProto::new_points(0.0),
                bottom: DimensionProto::new_points(0.0),
                ..Default::default()
            })
            .into(),
            margin: Some(DimensionRect {
                start: DimensionProto::new_points(0.0),
                end: DimensionProto::new_points(0.0),
                top: DimensionProto::new_points(0.0),
                bottom: DimensionProto::new_points(0.0),
                ..Default::default()
            })
            .into(),
            align_content: AlignContent::ALIGN_CONTENT_FLEX_START.into(),
            justify_content: JustifyContent::JUSTIFY_CONTENT_FLEX_START.into(),
            align_items: AlignItems::ALIGN_ITEMS_FLEX_START.into(),
            align_self: AlignSelf::ALIGN_SELF_AUTO.into(),
            position_type: PositionType::POSITION_TYPE_RELATIVE.into(),
            bounding_box: Some(Size { width: 100.0, height: 200.0, ..Default::default() }).into(),
            ..Default::default()
        };
        layout_style.flex_grow = 2.0;
        layout_style.flex_shrink = 0.5;
        layout_style.flex_direction = FlexDirection::FLEX_DIRECTION_COLUMN.into();
        layout_style.align_items = AlignItems::ALIGN_ITEMS_CENTER.into();
        if let Some(padding) = layout_style.padding.as_mut() {
            padding.set_start(dimension_proto::Dimension::Points(10.0));
        }
        layout_style.width = DimensionProto::new_points(100.0);
        layout_style.height = DimensionProto::new_points(200.0);
        layout_style.min_width = DimensionProto::new_points(50.0);
        layout_style.min_height = DimensionProto::new_points(100.0);
        layout_style.max_width = DimensionProto::new_points(200.0);
        layout_style.max_height = DimensionProto::new_points(400.0);
        layout_style.left = DimensionProto::new_points(10.0);
        layout_style.right = DimensionProto::new_points(10.0);
        layout_style.top = DimensionProto::new_points(10.0);
        layout_style.bottom = DimensionProto::new_points(10.0);
        layout_style.flex_basis = DimensionProto::new_points(100.0);
        layout_style.item_spacing = Some(ItemSpacing::new_fixed(10)).into();

        let taffy_style: taffy::prelude::Style = (&layout_style).try_into_taffy().unwrap();

        assert_eq!(taffy_style.flex_grow, 2.0);
        assert_eq!(taffy_style.flex_shrink, 0.5);
        assert_eq!(taffy_style.flex_direction, taffy::prelude::FlexDirection::Column);
        assert_eq!(taffy_style.align_items, Some(taffy::prelude::AlignItems::Center));
        assert_eq!(taffy_style.padding.left, taffy::prelude::LengthPercentage::Length(10.0));
        assert_eq!(taffy_style.size.width, taffy::prelude::Dimension::Length(100.0));
        assert_eq!(taffy_style.size.height, taffy::prelude::Dimension::Length(200.0));
    }
}
