/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use dc_bundle::definition::element;
use dc_bundle::definition::element::ScalableUiVariant;
use log::error;
use serde::{Deserialize, Serialize};

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
enum DimType {
    #[serde(rename = "dp")]
    Dp,
    #[serde(rename = "percent")]
    Percent,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
struct Dimension {
    value: f32,
    dim: DimType,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
struct Bounds {
    left: Dimension,
    top: Dimension,
    right: Dimension,
    bottom: Dimension,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub(crate) struct VariantDataJson {
    #[serde(rename = "isDefault")]
    is_default: bool,
    bounds: Bounds,
}

impl Into<element::ScalableDimension> for Dimension {
    fn into(self) -> element::ScalableDimension {
        element::ScalableDimension {
            dimension: Some(match self.dim {
                DimType::Dp => element::scalable_dimension::Dimension::Points(self.value),
                DimType::Percent => element::scalable_dimension::Dimension::Percent(self.value),
            })
        }
    }
}

impl Into<dc_bundle::definition::element::Bounds> for Bounds {
    fn into(self) -> dc_bundle::definition::element::Bounds {
        dc_bundle::definition::element::Bounds {
            left: Some(self.left.into()),
            top: Some(self.top.into()),
            right: Some(self.right.into()),
            bottom: Some(self.bottom.into()),
        }
    }
}

impl Into<Option<ScalableUiVariant>> for VariantDataJson {
    fn into(self) -> Option<ScalableUiVariant> {
        Some(ScalableUiVariant {
            is_default: self.is_default,
            bounds: Some(self.bounds.into()),
        })
    }
}
