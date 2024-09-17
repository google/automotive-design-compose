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

use crate::into_taffy::{IntoTaffy, TryIntoTaffy};
use dc_bundle::definition::element::DimensionExt;
use dc_bundle::legacy_definition::layout::layout_style::LayoutStyle;

impl TryIntoTaffy<taffy::prelude::Style> for &LayoutStyle {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::prelude::Style, Self::Error> {
        let mut tstyle = taffy::prelude::Style::default();

        tstyle.padding = (&self.padding).try_into_taffy()?;

        tstyle.flex_grow = self.flex_grow;
        tstyle.flex_shrink = self.flex_shrink;
        tstyle.flex_basis = (&self.flex_basis).try_into_taffy()?;
        tstyle.gap.width = (&self.item_spacing).into_taffy();
        tstyle.gap.height = (&self.item_spacing).into_taffy();

        tstyle.align_content = Some((&self.align_content).into_taffy());
        tstyle.justify_content = Some((&self.justify_content).into_taffy());
        tstyle.align_items = Some((&self.align_items).into_taffy());
        tstyle.flex_direction = (&self.flex_direction).into_taffy();
        tstyle.align_self = (&self.align_self).into_taffy();

        tstyle.size.width = (&self.width).try_into_taffy()?;
        tstyle.size.height = (&self.height).try_into_taffy()?;
        tstyle.min_size.width = (&self.min_width).try_into_taffy()?;
        tstyle.min_size.height = (&self.min_height).try_into_taffy()?;
        tstyle.max_size.width = (&self.max_width).try_into_taffy()?;
        tstyle.max_size.height = (&self.max_height).try_into_taffy()?;

        // If we have a fixed size, use the bounding box since that takes into
        // account scale and rotation, and disregard min/max sizes.
        // TODO support this with non-fixed sizes also!
        if self.width.is_points()? {
            tstyle.size.width = taffy::prelude::Dimension::Points(self.bounding_box.width);
            tstyle.min_size.width = taffy::prelude::Dimension::Auto;
            tstyle.max_size.width = taffy::prelude::Dimension::Auto;
        }
        if self.height.is_points()? {
            tstyle.size.height = taffy::prelude::Dimension::Points(self.bounding_box.height);
            tstyle.min_size.height = taffy::prelude::Dimension::Auto;
            tstyle.max_size.height = taffy::prelude::Dimension::Auto;
        }

        tstyle.position = (&self.position_type).into_taffy();
        tstyle.inset.left = (&self.left).try_into_taffy()?;
        tstyle.inset.right = (&self.right).try_into_taffy()?;
        tstyle.inset.top = (&self.top).try_into_taffy()?;
        tstyle.inset.bottom = (&self.bottom).try_into_taffy()?;

        tstyle.margin = (&self.margin).try_into_taffy()?;

        tstyle.display = taffy::prelude::Display::Flex; // TODO set to None to hide
        Ok(tstyle)
    }
}
