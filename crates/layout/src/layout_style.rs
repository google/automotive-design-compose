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

use dc_design_package::layout_style::LayoutStyle;

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
