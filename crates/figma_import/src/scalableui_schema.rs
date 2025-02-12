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

use dc_bundle::definition::element::{self, ScalableUiComponentSet};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

//
// Schema data for component sets
//

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
struct Event {
    event_name: String,
    variant_id: String,
    variant_name: String,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ComponentSetDataJson {
    id: String,
    name: String,
    role: String,
    event_list: Vec<Event>,
}

impl Into<element::Event> for &Event {
    fn into(self) -> element::Event {
        element::Event {
            event_name: self.event_name.clone(),
            variant_id: self.variant_id.clone(),
            variant_name: self.variant_name.clone(),
        }
    }
}

impl Into<ScalableUiComponentSet> for ComponentSetDataJson {
    fn into(self) -> ScalableUiComponentSet {
        let mut event_map: HashMap<String, element::Event> = HashMap::new();
        for e in &self.event_list {
            event_map.insert(e.event_name.clone(), e.into());
        }
        ScalableUiComponentSet {
            id: self.id,
            name: self.name,
            role: self.role,
            //event_list: self.event_list.iter().map(|i| i.into()).collect(),
            event_map: event_map,
        }
    }
}

//
// Schema data for variants
//

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
            width: None,
            height: None,
        }
    }
}

impl Into<element::ScalableUiVariant> for VariantDataJson {
    fn into(self) -> element::ScalableUiVariant {
        element::ScalableUiVariant {
            is_default: self.is_default,
            is_visible: true,
            bounds: Some(self.bounds.into()),
        }
    }
}

//
// ScalableUiDataJson represents the schema for any node that has scalable ui data
//
#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(untagged)]
pub(crate) enum ScalableUiDataJson {
    Set(ComponentSetDataJson),
    Variant(VariantDataJson),
}

impl Into<Option<element::ScalableUiData>> for ScalableUiDataJson {
    fn into(self) -> Option<element::ScalableUiData> {
        Some(element::ScalableUiData {
            data: Some(match self {
                ScalableUiDataJson::Set(set) => element::scalable_ui_data::Data::Set(set.into()),
                ScalableUiDataJson::Variant(var) => element::scalable_ui_data::Data::Variant(var.into()),
            })
        })
    }
}
