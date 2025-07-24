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

use dc_bundle::geometry::dimension_proto::Dimension;
use dc_bundle::geometry::DimensionProto;
use dc_bundle::Error;
use protobuf::MessageField;
use taffy::prelude::Dimension as TaffyDimension;
use taffy::style_helpers::TaffyZero;

impl TryIntoTaffy<TaffyDimension> for &MessageField<DimensionProto> {
    type Error = Error;
    fn try_into_taffy(self) -> Result<TaffyDimension, Self::Error> {
        match self.as_ref() {
            Some(DimensionProto { Dimension: Some(Dimension::Percent(p)), .. }) => {
                Ok(TaffyDimension::Percent(*p))
            }
            Some(DimensionProto { Dimension: Some(Dimension::Points(p)), .. }) => {
                Ok(TaffyDimension::Length(*p))
            }
            Some(DimensionProto { Dimension: Some(Dimension::Auto(_)), .. }) => {
                Ok(TaffyDimension::Auto)
            }
            Some(_) => Ok(TaffyDimension::Auto),
            None => Err(Error::MissingFieldError { field: "DimensionProto".to_string() }),
        }
    }
}

impl TryIntoTaffy<taffy::prelude::LengthPercentage> for &MessageField<DimensionProto> {
    type Error = Error;
    fn try_into_taffy(self) -> Result<taffy::prelude::LengthPercentage, Self::Error> {
        match self.as_ref() {
            Some(DimensionProto { Dimension: Some(Dimension::Percent(p)), .. }) => {
                Ok(taffy::prelude::LengthPercentage::Percent(*p))
            }
            Some(DimensionProto { Dimension: Some(Dimension::Points(p)), .. }) => {
                Ok(taffy::prelude::LengthPercentage::Length(*p))
            }
            Some(_) => Ok(taffy::prelude::LengthPercentage::ZERO),

            None => Err(Error::MissingFieldError { field: "DimensionProto".to_string() }),
        }
    }
}

impl TryIntoTaffy<taffy::prelude::LengthPercentageAuto> for &MessageField<DimensionProto> {
    type Error = Error;
    fn try_into_taffy(self) -> Result<taffy::prelude::LengthPercentageAuto, Self::Error> {
        match self.as_ref() {
            Some(DimensionProto { Dimension: Some(Dimension::Percent(p)), .. }) => {
                Ok(taffy::prelude::LengthPercentageAuto::Percent(*p))
            }
            Some(DimensionProto { Dimension: Some(Dimension::Points(p)), .. }) => {
                Ok(taffy::prelude::LengthPercentageAuto::Length(*p))
            }
            Some(DimensionProto { Dimension: Some(Dimension::Auto(_)), .. }) => {
                Ok(taffy::prelude::LengthPercentageAuto::Auto)
            }
            Some(_) => Ok(taffy::prelude::LengthPercentageAuto::ZERO),
            None => Err(Error::MissingFieldError { field: "DimensionProto".to_string() }),
        }
    }
}

#[cfg(test)]
mod tests {
    use dc_bundle::geometry::dimension_proto::Dimension;
    use protobuf::well_known_types::empty::Empty;

    use super::*;

    #[test]
    fn test_try_into_taffy_dimension() {
        let points: MessageField<DimensionProto> =
            Some(DimensionProto { Dimension: Some(Dimension::Points(10.0)), ..Default::default() })
                .into();
        assert_eq!(
            TryIntoTaffy::<TaffyDimension>::try_into_taffy(&points).unwrap(),
            TaffyDimension::Length(10.0)
        );

        let percent: MessageField<DimensionProto> =
            Some(DimensionProto { Dimension: Some(Dimension::Percent(0.5)), ..Default::default() })
                .into();
        assert_eq!(
            TryIntoTaffy::<TaffyDimension>::try_into_taffy(&percent).unwrap(),
            TaffyDimension::Percent(0.5)
        );

        let auto: MessageField<DimensionProto> = Some(DimensionProto {
            Dimension: Some(Dimension::Auto(Empty::new())),
            ..Default::default()
        })
        .into();
        assert_eq!(
            TryIntoTaffy::<TaffyDimension>::try_into_taffy(&auto).unwrap(),
            TaffyDimension::Auto
        );
    }

    #[test]
    fn test_try_into_taffy_dimension_none() {
        let dimension_proto: MessageField<DimensionProto> = None.into();
        let result: Result<TaffyDimension, dc_bundle::Error> =
            TryIntoTaffy::<TaffyDimension>::try_into_taffy(&dimension_proto);
        assert!(result.is_err());
    }

    #[test]
    fn test_try_into_taffy_length_percentage() {
        let points: MessageField<DimensionProto> =
            Some(DimensionProto { Dimension: Some(Dimension::Points(10.0)), ..Default::default() })
                .into();
        assert_eq!(
            TryIntoTaffy::<taffy::prelude::LengthPercentage>::try_into_taffy(&points).unwrap(),
            taffy::prelude::LengthPercentage::Length(10.0)
        );

        let percent: MessageField<DimensionProto> =
            Some(DimensionProto { Dimension: Some(Dimension::Percent(0.5)), ..Default::default() })
                .into();
        assert_eq!(
            TryIntoTaffy::<taffy::prelude::LengthPercentage>::try_into_taffy(&percent).unwrap(),
            taffy::prelude::LengthPercentage::Percent(0.5)
        );
    }

    #[test]
    fn test_try_into_taffy_length_percentage_none() {
        let dimension_proto: MessageField<DimensionProto> = None.into();
        let result: Result<taffy::prelude::LengthPercentage, dc_bundle::Error> =
            TryIntoTaffy::<taffy::prelude::LengthPercentage>::try_into_taffy(&dimension_proto);
        assert!(result.is_err());
    }

    #[test]
    fn test_try_into_taffy_length_percentage_auto() {
        let points: MessageField<DimensionProto> =
            Some(DimensionProto { Dimension: Some(Dimension::Points(10.0)), ..Default::default() })
                .into();
        assert_eq!(
            TryIntoTaffy::<taffy::prelude::LengthPercentageAuto>::try_into_taffy(&points).unwrap(),
            taffy::prelude::LengthPercentageAuto::Length(10.0)
        );

        let percent: MessageField<DimensionProto> =
            Some(DimensionProto { Dimension: Some(Dimension::Percent(0.5)), ..Default::default() })
                .into();
        assert_eq!(
            TryIntoTaffy::<taffy::prelude::LengthPercentageAuto>::try_into_taffy(&percent).unwrap(),
            taffy::prelude::LengthPercentageAuto::Percent(0.5)
        );

        let auto: MessageField<DimensionProto> = Some(DimensionProto {
            Dimension: Some(Dimension::Auto(Empty::new())),
            ..Default::default()
        })
        .into();
        assert_eq!(
            TryIntoTaffy::<taffy::prelude::LengthPercentageAuto>::try_into_taffy(&auto).unwrap(),
            taffy::prelude::LengthPercentageAuto::Auto
        );
    }

    #[test]
    fn test_try_into_taffy_length_percentage_auto_none() {
        let dimension_proto: MessageField<DimensionProto> = None.into();
        let result: Result<taffy::prelude::LengthPercentageAuto, dc_bundle::Error> =
            TryIntoTaffy::<taffy::prelude::LengthPercentageAuto>::try_into_taffy(&dimension_proto);
        assert!(result.is_err());
    }
}
