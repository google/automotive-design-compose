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

use dc_bundle::definition::layout::LayoutStyle;
use serde::{Deserialize, Serialize};

// A representation of a Figma node to register for layout.
#[derive(Serialize, Deserialize, Debug)]
pub struct LayoutNode {
    pub layout_id: i32,
    pub parent_layout_id: i32,
    pub child_index: i32,
    pub style: LayoutStyle,
    pub name: String,
    pub use_measure_func: bool,
    pub fixed_width: Option<i32>,
    pub fixed_height: Option<i32>,
}

// A parent node id and a list of child ids
#[derive(Serialize, Deserialize, Debug)]
pub struct LayoutParentChildren {
    pub parent_layout_id: i32,
    pub child_layout_ids: Vec<i32>,
}

// A list of Figma nodes to register for layout
#[derive(Serialize, Deserialize, Debug)]
pub struct LayoutNodeList {
    pub layout_nodes: Vec<LayoutNode>,
    pub parent_children: Vec<LayoutParentChildren>,
}
