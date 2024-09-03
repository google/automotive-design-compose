/*
 * Copyright 2024 Google LLC
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

use crate::definition::element::Color;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

// Enum for fields that represent either a fixed number or a number variable
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum NumOrVar {
    Num(f32),
    Var { id: String, fallback: f32 },
}

// Enum for fields that represent either a fixed color or a color variable
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum ColorOrVar {
    Color(Color),
    Var { id: String, fallback: Color },
}

#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct VariableAlias {
    pub r#type: String,
    pub id: String,
}

// We redeclare VariableValue instead of using the one from figma_schema because
// the "untagged" attribute there prevents serde_reflection from being able to
// run properly.
#[derive(Deserialize, Serialize, Debug, Clone)]
pub enum VariableValue {
    Bool(bool),
    Number(f32),
    Text(String),
    Color(Color),
    Alias(VariableAlias),
}

// Each variable contains a map of possible values. This data structure helps
// keep track of that data and contains functions to retrieve the value of a
// variable given a mode.
#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct VariableValueMap {
    pub values_by_mode: HashMap<String, VariableValue>,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub enum VariableType {
    Bool,
    Number,
    Text,
    Color,
}

// Representation of a Figma variable. We convert a figma_schema::Variable into
// this format to make the fields a bit easier to access.
#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct Variable {
    pub id: String,
    pub name: String,
    pub remote: bool,
    pub key: String,
    pub variable_collection_id: String,
    pub var_type: VariableType,
    pub values_by_mode: VariableValueMap,
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

/// Stores variable mappings
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct VariableMap {
    pub collections: HashMap<String, Collection>, // ID -> Collection
    pub collection_name_map: HashMap<String, String>, // Name -> ID
    pub variables: HashMap<String, Variable>,     // ID -> Variable
    pub variable_name_map: HashMap<String, HashMap<String, String>>, // Collection ID -> [Name -> ID]
}
