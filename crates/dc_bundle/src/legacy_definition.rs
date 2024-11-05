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

// This module holds the original structures that made up the serialized design doc, and which will be replaced with the protobuf implementations of the Design Definition

use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::fmt;
use std::sync::Arc;
// To help keep the legacy definition files clear we alias `crate::definition`, which is the base
// module for the generated protobuf files to `proto`, so that all of the protobuf-generated types
// inside `legacy_definition` must be prepended with `proto::`
use crate::definition::element::ImageKey;
use crate::definition::element::VariableMap;
use crate::legacy_definition::element::node::NodeQuery;

pub mod element;
pub mod plugin;
pub mod view;

/// EncodedImageMap contains a mapping from ImageKey to network bytes. It can create an
/// ImageMap and is intended to be used when we want to use Figma-defined components but do
/// not want to communicate with the Figma service.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct EncodedImageMap(pub HashMap<ImageKey, Arc<serde_bytes::ByteBuf>>);

impl EncodedImageMap {
    pub fn map(&self) -> HashMap<ImageKey, Arc<serde_bytes::ByteBuf>> {
        self.0.clone()
    }
}

// LINT.IfChange
static CURRENT_VERSION: u32 = 22;
// Lint.ThenChange(common/src/main/java/com/android/designcompose/common/FsaasSession.kt)

// This is our serialized document type.
#[derive(Serialize, Deserialize, Debug)]
pub struct DesignComposeDefinitionHeader {
    pub version: u32,
}

impl DesignComposeDefinitionHeader {
    pub fn current() -> DesignComposeDefinitionHeader {
        DesignComposeDefinitionHeader { version: CURRENT_VERSION }
    }
}

impl fmt::Display for DesignComposeDefinitionHeader {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        // NOTE: Using `write!` here instead of typical `format!`
        // to keep newlines.
        write!(f, "DC Version: {}\nVersion: {}", CURRENT_VERSION, &self.version)
    }
}

// This is our serialized document type.
#[derive(Serialize, Deserialize, Debug)]
pub struct DesignComposeDefinition {
    pub last_modified: String,
    pub views: HashMap<NodeQuery, view::view::View>,
    pub images: EncodedImageMap,
    pub name: String,
    pub component_sets: HashMap<String, String>,
    pub version: String,
    pub id: String,
    pub variable_map: VariableMap,
}

impl fmt::Display for DesignComposeDefinition {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        // NOTE: Using `write!` here instead of typical `format!`
        // to keep newlines.
        write!(
            f,
            "Doc ID: {}\nName: {}\nLast Modified: {}",
            &self.id, &self.name, &self.last_modified
        )
    }
}
