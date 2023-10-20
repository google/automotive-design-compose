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

//! `figma_import` fetches a document from Figma and converts nodes from the document
//! to toolkit_schema Views, which can then be further customized (changing text or style)
//! and presented in other components that implement logic.
//!
//! The goal of this crate is to perform the mapping from Figma to the toolkit; it does
//! not provide any kind of UI logic mapping.

use crate::toolkit_schema;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

// The layout response sent back to client which contains a layout state ID and
// a list of layout IDs that have changed.
#[derive(Serialize, Deserialize, Debug)]
pub struct LayoutChangedResponse {
    pub layout_state: i32,
    pub changed_layouts: HashMap<i32, toolkit_schema::Layout>,
}
impl LayoutChangedResponse {
    pub fn unchanged(layout_state: i32) -> Self {
        LayoutChangedResponse { layout_state, changed_layouts: HashMap::new() }
    }
}

// A representation of a Figma node to register for layout.
#[derive(Serialize, Deserialize, Debug)]
pub struct LayoutNode {
    pub layout_id: i32,
    pub parent_layout_id: i32,
    pub child_index: i32,
    pub view: toolkit_schema::View,
    pub base_view: Option<toolkit_schema::View>,
    pub use_measure_func: bool,
}

// A list of Figma nodes to register for layout
#[derive(Serialize, Deserialize, Debug)]
pub struct LayoutNodeList {
    pub layout_nodes: Vec<LayoutNode>,
}
