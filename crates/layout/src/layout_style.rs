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
use log::error;

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
        error!("Into taffy align_content {:?}", &self.align_content);
        tstyle.justify_content = Some((&self.justify_content).into_taffy());
        error!("Into taffy justify_content {:?}", &self.justify_content);
        tstyle.align_items = Some((&self.align_items).into_taffy());
        error!("Into taffy align_items {:?}", &self.align_items);
        tstyle.flex_direction = (&self.flex_direction).into_taffy();
        error!("Into taffy flex_direction {:?}", &self.flex_direction);
        tstyle.align_self = (&self.align_self).into_taffy();
        error!("Into taffy align_self {:?}", &self.align_self);

        tstyle.size.width = (&self.width).try_into_taffy()?;
        error!("Into taffy width {:?}", &self.width);
        tstyle.size.height = (&self.height).try_into_taffy()?;
        error!("Into taffy height {:?}", &self.height);
        tstyle.min_size.width = (&self.min_width).try_into_taffy()?;
        error!("Into taffy min_width {:?}", &self.min_width);
        tstyle.min_size.height = (&self.min_height).try_into_taffy()?;
        error!("Into taffy min_height {:?}", &self.min_height);
        tstyle.max_size.width = (&self.max_width).try_into_taffy()?;
        error!("Into taffy max_width {:?}", &self.max_width);
        tstyle.max_size.height = (&self.max_height).try_into_taffy()?;
        error!("Into taffy max_height {:?}", &self.max_height);

        // If we have a fixed size, use the bounding box since that takes into
        // account scale and rotation, and disregard min/max sizes.
        // TODO support this with non-fixed sizes also!
        if self.width.is_points()? {
            tstyle.size.width = taffy::prelude::Dimension::Points(self.bounding_box.width);
            tstyle.min_size.width = taffy::prelude::Dimension::Auto;
            tstyle.max_size.width = taffy::prelude::Dimension::Auto;
            error!("Into taffy override max/min width to auto");
        }
        if self.height.is_points()? {
            tstyle.size.height = taffy::prelude::Dimension::Points(self.bounding_box.height);
            tstyle.min_size.height = taffy::prelude::Dimension::Auto;
            tstyle.max_size.height = taffy::prelude::Dimension::Auto;
            error!("Into taffy override max/min height to auto");
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
