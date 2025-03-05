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

use crate::into_taffy::TryIntoTaffy;

use dc_bundle::geometry::DimensionRect;
use taffy::prelude::LengthPercentage;
use taffy::prelude::{Dimension as TaffyDimension, LengthPercentageAuto};

impl TryIntoTaffy<taffy::prelude::Rect<TaffyDimension>> for &Option<DimensionRect> {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::prelude::Rect<TaffyDimension>, Self::Error> {
        let rect = self
            .as_ref()
            .ok_or(dc_bundle::Error::MissingFieldError { field: "DimensionRect".to_string() })?;
        Ok(taffy::prelude::Rect {
            left: (&rect.start.as_ref().cloned()).try_into_taffy()?,
            right: (&rect.end.as_ref().cloned()).try_into_taffy()?,
            top: (&rect.top.as_ref().cloned()).try_into_taffy()?,
            bottom: (&rect.bottom.as_ref().cloned()).try_into_taffy()?,
        })
    }
}

impl TryIntoTaffy<taffy::prelude::Rect<LengthPercentage>> for &Option<DimensionRect> {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::prelude::Rect<LengthPercentage>, Self::Error> {
        let rect = self
            .as_ref()
            .ok_or(dc_bundle::Error::MissingFieldError { field: "DimensionRect".to_string() })?;

        Ok(taffy::prelude::Rect {
            left: (&rect.start.as_ref().cloned()).try_into_taffy()?,
            right: (&rect.end.as_ref().cloned()).try_into_taffy()?,
            top: (&rect.top.as_ref().cloned()).try_into_taffy()?,
            bottom: (&rect.bottom.as_ref().cloned()).try_into_taffy()?,
        })
    }
}

impl TryIntoTaffy<taffy::prelude::Rect<LengthPercentageAuto>> for &Option<DimensionRect> {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::prelude::Rect<LengthPercentageAuto>, Self::Error> {
        let rect = self
            .as_ref()
            .ok_or(dc_bundle::Error::MissingFieldError { field: "DimensionRect".to_string() })?;

        Ok(taffy::prelude::Rect {
            left: (&rect.start.as_ref().cloned()).try_into_taffy()?,
            right: (&rect.end.as_ref().cloned()).try_into_taffy()?,
            top: (&rect.top.as_ref().cloned()).try_into_taffy()?,
            bottom: (&rect.bottom.as_ref().cloned()).try_into_taffy()?,
        })
    }
}
