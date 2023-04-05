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

use std::collections::HashMap;

use serde::{Deserialize, Serialize};

use crate::{document::FigmaDocInfo, image_context::EncodedImageMap, toolkit_schema, NodeQuery};

static CURRENT_VERSION: u32 = 11;

// This is our serialized document type.
#[derive(Serialize, Deserialize, Debug)]
pub struct SerializedFigmaDocHeader {
    pub version: u32,
}
impl SerializedFigmaDocHeader {
    pub fn current() -> SerializedFigmaDocHeader {
        SerializedFigmaDocHeader { version: CURRENT_VERSION }
    }
}

// This is our serialized document type.
#[derive(Serialize, Deserialize, Debug)]
pub struct SerializedFigmaDoc {
    pub last_modified: String,
    pub nodes: HashMap<NodeQuery, toolkit_schema::View>,
    pub images: EncodedImageMap,
    pub name: String,
    pub component_sets: HashMap<String, String>,
    pub version: String,
}

// This is the struct we send over to the client. It contains the serialized document
// along with some extra data: document branches, project files, and errors
#[derive(Serialize, Deserialize, Debug)]
pub struct ServerFigmaDoc {
    pub figma_doc: SerializedFigmaDoc,
    pub branches: Vec<FigmaDocInfo>,
    pub project_files: Vec<FigmaDocInfo>,
    pub errors: Vec<String>,
}
