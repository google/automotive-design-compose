// Copyright 2023 Google LLC
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

// We use serde to serialize and deserialize our "toolkit style" views. Because we need
// serialization, we use our own replacement for ViewStyle which can be serialized and
// retain image references.
use serde::{Deserialize, Serialize};

use crate::figma_schema;
use std::collections::HashMap;
use dc_design_package::toolkit_schema::VariableValue;
pub use crate::figma_schema::{FigmaColor, OverflowDirection, Rectangle, StrokeCap, VariableAlias};

/// This struct contains information for scrolling on a frame. It combines the
/// scroll overflow direction, which comes from a frame, and the bool
/// paged_scrolling, which comes from the vsw-extended-layout plugin.
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub struct ScrollInfo {
    pub overflow: OverflowDirection,
    pub paged_scrolling: bool,
}
impl Default for ScrollInfo {
    fn default() -> Self {
        ScrollInfo { overflow: OverflowDirection::None, paged_scrolling: false }
    }
}

// Representation of a variable mode. Variables can have fixed values for each available mode
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct Mode {
    pub id: String,
    pub name: String,
}

// Representation of a variable collection. Every variable belongs to a collection, and a
// collection contains one or more modes.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct Collection {
    pub id: String,
    pub name: String,
    pub default_mode_id: String,
    pub mode_name_hash: HashMap<String, String>, // name -> id
    pub mode_id_hash: HashMap<String, Mode>,     // id -> Mode
}

// Each variable contains a map of possible values. This data structure helps
// keep track of that data and contains functions to retrieve the value of a
// variable given a mode.
#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct VariableValueMap {
    pub values_by_mode: HashMap<String, VariableValue>,
}
impl VariableValueMap {
    fn from_figma_map(map: &HashMap<String, figma_schema::VariableValue>) -> VariableValueMap {
        let mut values_by_mode: HashMap<String, VariableValue> = HashMap::new();
        for (mode_id, value) in map.iter() {
            values_by_mode.insert(mode_id.clone(), VariableValue::from_figma_value(value));
        }
        VariableValueMap { values_by_mode }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub enum VariableType {
    Bool,
    Number,
    Text,
    Color,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct BoundVariables {
    pub variables: HashMap<String, VariableAlias>,
}
