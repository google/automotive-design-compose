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

use dc_bundle::design::element::{dimension_proto::Dimension, DimensionProto, DimensionRect, Size};

use crate::types;

impl TryFrom<Option<DimensionProto>> for types::Dimension {
    type Error = dc_bundle::Error;

    fn try_from(proto: Option<DimensionProto>) -> Result<Self, Self::Error> {
        Ok(
            match proto
                .ok_or(dc_bundle::Error::MissingFieldError { field: "DimensionProto".to_string() })?
                .dimension
                .ok_or(dc_bundle::Error::MissingFieldError { field: "Dimension".to_string() })?
            {
                Dimension::Auto(_) => types::Dimension::Auto,
                Dimension::Points(p) => types::Dimension::Points(p),
                Dimension::Percent(p) => types::Dimension::Percent(p),
                Dimension::Undefined(_) => types::Dimension::Undefined,
            },
        )
    }
}

impl TryFrom<DimensionRect> for types::Rect<types::Dimension> {
    type Error = dc_bundle::Error;
    fn try_from(proto: DimensionRect) -> Result<Self, Self::Error> {
        let rect = types::Rect {
            start: proto.start.try_into()?,
            end: proto.end.try_into()?,
            top: proto.top.try_into()?,
            bottom: proto.bottom.try_into()?,
        };
        Ok(rect)
    }
}

impl From<Size> for types::Size<f32> {
    fn from(proto: Size) -> Self {
        Self { width: proto.width, height: proto.height }
    }
}
