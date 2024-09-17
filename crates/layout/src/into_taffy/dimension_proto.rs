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
use dc_bundle::definition::element::dimension_proto::Dimension;
use dc_bundle::definition::element::DimensionProto;
use dc_bundle::Error;
use taffy::prelude::Dimension as TaffyDimension;

impl TryIntoTaffy<TaffyDimension> for &Option<DimensionProto> {
    type Error = Error;
    fn try_into_taffy(self) -> Result<TaffyDimension, Self::Error> {
        match self {
            Some(DimensionProto { dimension: Some(Dimension::Percent(p)) }) => {
                Ok(TaffyDimension::Percent(*p))
            }
            Some(DimensionProto { dimension: Some(Dimension::Points(p)) }) => {
                Ok(TaffyDimension::Points(*p))
            }
            Some(DimensionProto { dimension: Some(Dimension::Auto(_)) }) => {
                Ok(TaffyDimension::Auto)
            }
            Some(_) => Ok(TaffyDimension::Auto),
            None => Err(Error::MissingFieldError { field: "DimensionProto".to_string() }),
        }
    }
}

impl TryIntoTaffy<taffy::prelude::LengthPercentage> for &Option<DimensionProto> {
    type Error = Error;
    fn try_into_taffy(self) -> Result<taffy::prelude::LengthPercentage, Self::Error> {
        match self {
            Some(DimensionProto { dimension: Some(Dimension::Percent(p)) }) => {
                Ok(taffy::prelude::LengthPercentage::Percent(*p))
            }
            Some(DimensionProto { dimension: Some(Dimension::Points(p)) }) => {
                Ok(taffy::prelude::LengthPercentage::Points(*p))
            }
            Some(_) => Ok(taffy::prelude::LengthPercentage::Points(0.0)),

            None => Err(Error::MissingFieldError { field: "DimensionProto".to_string() }),
        }
    }
}

impl TryIntoTaffy<taffy::prelude::LengthPercentageAuto> for &Option<DimensionProto> {
    type Error = Error;
    fn try_into_taffy(self) -> Result<taffy::prelude::LengthPercentageAuto, Self::Error> {
        match self {
            Some(DimensionProto { dimension: Some(Dimension::Percent(p)) }) => {
                Ok(taffy::prelude::LengthPercentageAuto::Percent(*p))
            }
            Some(DimensionProto { dimension: Some(Dimension::Points(p)) }) => {
                Ok(taffy::prelude::LengthPercentageAuto::Points(*p))
            }
            Some(DimensionProto { dimension: Some(Dimension::Auto(_)) }) => {
                Ok(taffy::prelude::LengthPercentageAuto::Auto)
            }
            Some(_) => Ok(taffy::prelude::LengthPercentageAuto::Points(0.0)),
            None => Err(Error::MissingFieldError { field: "DimensionProto".to_string() }),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use dc_bundle::definition::element::dimension_proto::Dimension;
    use dc_bundle::definition::element::DimensionProto;

    #[test]
    fn test_try_into_taffy_dimension() {
        let dimension_proto = Some(DimensionProto { dimension: Some(Dimension::Points(10.0)) });
        let result: Result<TaffyDimension, dc_bundle::Error> = (&dimension_proto).try_into_taffy();
        assert!(result.is_ok());
    }

    #[test]
    fn test_try_into_taffy_dimension_none() {
        let dimension_proto: Option<DimensionProto> = None;
        let result: Result<TaffyDimension, dc_bundle::Error> = (&dimension_proto).try_into_taffy();
        assert!(result.is_err());
    }

    #[test]
    fn test_try_into_taffy_length_percentage() {
        let dimension_proto = Some(DimensionProto { dimension: Some(Dimension::Percent(10.0)) });
        let result: Result<taffy::prelude::LengthPercentage, dc_bundle::Error> =
            (&dimension_proto).try_into_taffy();
        assert!(result.is_ok());
    }

    #[test]
    fn test_try_into_taffy_length_percentage_none() {
        let dimension_proto: Option<DimensionProto> = None;
        let result: Result<taffy::prelude::LengthPercentage, dc_bundle::Error> =
            (&dimension_proto).try_into_taffy();
        assert!(result.is_err());
    }

    #[test]
    fn test_try_into_taffy_length_percentage_auto() {
        let dimension_proto = Some(DimensionProto { dimension: Some(Dimension::Auto(())) });
        let result: Result<taffy::prelude::LengthPercentageAuto, dc_bundle::Error> =
            (&dimension_proto).try_into_taffy();
        assert!(result.is_ok());
    }

    #[test]
    fn test_try_into_taffy_length_percentage_auto_none() {
        let dimension_proto: Option<DimensionProto> = None;
        let result: Result<taffy::prelude::LengthPercentageAuto, dc_bundle::Error> =
            (&dimension_proto).try_into_taffy();
        assert!(result.is_err());
    }
}
