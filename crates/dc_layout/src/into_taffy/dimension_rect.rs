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
use protobuf::MessageField;
use taffy::prelude::LengthPercentage;
use taffy::prelude::{Dimension as TaffyDimension, LengthPercentageAuto};

impl TryIntoTaffy<taffy::prelude::Rect<TaffyDimension>> for &MessageField<DimensionRect> {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::prelude::Rect<TaffyDimension>, Self::Error> {
        let rect = self
            .as_ref()
            .ok_or(dc_bundle::Error::MissingFieldError { field: "DimensionRect".to_string() })?;
        Ok(taffy::prelude::Rect {
            left: rect.start.try_into_taffy()?,
            right: rect.end.try_into_taffy()?,
            top: rect.top.try_into_taffy()?,
            bottom: rect.bottom.try_into_taffy()?,
        })
    }
}

impl TryIntoTaffy<taffy::prelude::Rect<LengthPercentage>> for &MessageField<DimensionRect> {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::prelude::Rect<LengthPercentage>, Self::Error> {
        let rect = self
            .as_ref()
            .ok_or(dc_bundle::Error::MissingFieldError { field: "DimensionRect".to_string() })?;

        Ok(taffy::prelude::Rect {
            left: rect.start.try_into_taffy()?,
            right: rect.end.try_into_taffy()?,
            top: rect.top.try_into_taffy()?,
            bottom: rect.bottom.try_into_taffy()?,
        })
    }
}

impl TryIntoTaffy<taffy::prelude::Rect<LengthPercentageAuto>> for &MessageField<DimensionRect> {
    type Error = dc_bundle::Error;
    fn try_into_taffy(self) -> Result<taffy::prelude::Rect<LengthPercentageAuto>, Self::Error> {
        let rect = self
            .as_ref()
            .ok_or(dc_bundle::Error::MissingFieldError { field: "DimensionRect".to_string() })?;

        Ok(taffy::prelude::Rect {
            left: rect.start.try_into_taffy()?,
            right: rect.end.try_into_taffy()?,
            top: rect.top.try_into_taffy()?,
            bottom: rect.bottom.try_into_taffy()?,
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use dc_bundle::geometry::DimensionProto;

    fn create_test_dimension_rect() -> DimensionRect {
        let mut rect = DimensionRect::default();
        rect.start = DimensionProto::new_points(10.0);
        rect.end = DimensionProto::new_points(20.0);
        rect.top = DimensionProto::new_points(30.0);
        rect.bottom = DimensionProto::new_points(40.0);
        rect
    }

    #[test]
    fn test_try_into_taffy_dimension_rect() {
        let rect: MessageField<DimensionRect> = Some(create_test_dimension_rect()).into();
        let result: Result<taffy::prelude::Rect<TaffyDimension>, dc_bundle::Error> =
            (&rect).try_into_taffy();
        assert!(result.is_ok());
        let taffy_rect = result.unwrap();
        assert_eq!(taffy_rect.left, TaffyDimension::Length(10.0));
        assert_eq!(taffy_rect.right, TaffyDimension::Length(20.0));
        assert_eq!(taffy_rect.top, TaffyDimension::Length(30.0));
        assert_eq!(taffy_rect.bottom, TaffyDimension::Length(40.0));
    }

    #[test]
    fn test_try_into_taffy_dimension_rect_none() {
        let rect: MessageField<DimensionRect> = None.into();
        let result: Result<taffy::prelude::Rect<TaffyDimension>, dc_bundle::Error> =
            (&rect).try_into_taffy();
        assert!(result.is_err());
    }

    #[test]
    fn test_try_into_taffy_length_percentage_rect() {
        let rect: MessageField<DimensionRect> = Some(create_test_dimension_rect()).into();
        let result: Result<taffy::prelude::Rect<LengthPercentage>, dc_bundle::Error> =
            (&rect).try_into_taffy();
        assert!(result.is_ok());
        let taffy_rect = result.unwrap();
        assert_eq!(taffy_rect.left, LengthPercentage::Length(10.0));
        assert_eq!(taffy_rect.right, LengthPercentage::Length(20.0));
        assert_eq!(taffy_rect.top, LengthPercentage::Length(30.0));
        assert_eq!(taffy_rect.bottom, LengthPercentage::Length(40.0));
    }

    #[test]
    fn test_try_into_taffy_length_percentage_rect_none() {
        let rect: MessageField<DimensionRect> = None.into();
        let result: Result<taffy::prelude::Rect<LengthPercentage>, dc_bundle::Error> =
            (&rect).try_into_taffy();
        assert!(result.is_err());
    }

    #[test]
    fn test_try_into_taffy_length_percentage_auto_rect() {
        let rect: MessageField<DimensionRect> = Some(create_test_dimension_rect()).into();
        let result: Result<taffy::prelude::Rect<LengthPercentageAuto>, dc_bundle::Error> =
            (&rect).try_into_taffy();
        assert!(result.is_ok());
        let taffy_rect = result.unwrap();
        assert_eq!(taffy_rect.left, LengthPercentageAuto::Length(10.0));
        assert_eq!(taffy_rect.right, LengthPercentageAuto::Length(20.0));
        assert_eq!(taffy_rect.top, LengthPercentageAuto::Length(30.0));
        assert_eq!(taffy_rect.bottom, LengthPercentageAuto::Length(40.0));
    }

    #[test]
    fn test_try_into_taffy_length_percentage_auto_rect_none() {
        let rect: MessageField<DimensionRect> = None.into();
        let result: Result<taffy::prelude::Rect<LengthPercentageAuto>, dc_bundle::Error> =
            (&rect).try_into_taffy();
        assert!(result.is_err());
    }
}
