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

use serde::{Deserialize, Serialize};

// The layout response sent back to client which contains a layout state ID and
// a list of layout IDs that have changed.
#[derive(Serialize, Deserialize, Debug)]
pub struct LayoutChangedResponse {
    pub layout_state: i32,
    pub changed_layout_ids: Vec<i32>,
}
impl LayoutChangedResponse {
    pub fn unchanged(layout_state: i32) -> Self {
        LayoutChangedResponse { layout_state, changed_layout_ids: vec![] }
    }
}
