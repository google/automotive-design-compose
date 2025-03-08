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

        tstyle.padding = (&self.padding.as_ref().cloned()).try_into_taffy()?;

        tstyle.flex_grow = self.flex_grow;
        tstyle.flex_shrink = self.flex_shrink;
        tstyle.flex_basis = (&self.flex_basis.as_ref().cloned()).try_into_taffy()?;
        tstyle.gap.width = (&self.item_spacing.as_ref().cloned()).try_into_taffy()?;
        tstyle.gap.height = (&self.item_spacing.as_ref().cloned()).try_into_taffy()?;

        tstyle.align_content = Some(self.align_content.enum_value_or_default().try_into_taffy()?);
        tstyle.justify_content =
            Some(self.justify_content.enum_value_or_default().try_into_taffy()?);
        tstyle.align_items = Some(self.align_items.enum_value_or_default().try_into_taffy()?);
        tstyle.flex_direction = self.flex_direction.enum_value_or_default().try_into_taffy()?;
        tstyle.align_self = self.align_self.enum_value_or_default().try_into_taffy()?;

        tstyle.size.width = (&self.width.as_ref().cloned()).try_into_taffy()?;
        tstyle.size.height = (&self.height.as_ref().cloned()).try_into_taffy()?;
        tstyle.min_size.width = (&self.min_width.as_ref().cloned()).try_into_taffy()?;
        tstyle.min_size.height = (&self.min_height.as_ref().cloned()).try_into_taffy()?;
        tstyle.max_size.width = (&self.max_width.as_ref().cloned()).try_into_taffy()?;
        tstyle.max_size.height = (&self.max_height.as_ref().cloned()).try_into_taffy()?;

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
        tstyle.inset.left = (&self.left.as_ref().cloned()).try_into_taffy()?;
        tstyle.inset.right = (&self.right.as_ref().cloned()).try_into_taffy()?;
        tstyle.inset.top = (&self.top.as_ref().cloned()).try_into_taffy()?;
        tstyle.inset.bottom = (&self.bottom.as_ref().cloned()).try_into_taffy()?;

        tstyle.margin = (&self.margin.as_ref().cloned()).try_into_taffy()?;

        tstyle.display = taffy::prelude::Display::Flex;

        tstyle.overflow.x = taffy::Overflow::Hidden;
        tstyle.overflow.y = taffy::Overflow::Hidden;

        Ok(tstyle)
    }
}
