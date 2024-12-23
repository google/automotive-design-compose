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
use std::fmt;
// To help keep the legacy definition files clear we alias `crate::definition`, which is the base
// module for the generated protobuf files to `proto`, so that all of the protobuf-generated types
// inside `legacy_definition` must be prepended with `proto::`

// LINT.IfChange
static CURRENT_VERSION: u32 = 24;
// Lint.ThenChange(common/src/main/java/com/android/designcompose/common/FsaasSession.kt)

// This is our serialized document type.
#[derive(Serialize, Deserialize, Debug)]
pub struct DesignComposeDefinitionHeaderV0 {
    pub version: u32,
}
impl DesignComposeDefinitionHeaderV0 {
    // Return the max serialized document version that used this old V0 header
    pub fn max_version() -> u32 {
        22
    }
}

// This is our serialized document type.
#[derive(Serialize, Deserialize, Debug)]
pub struct DesignComposeDefinitionHeader {
    pub dc_version: u32,
    pub last_modified: String,
    pub name: String,
    pub response_version: String,
    pub id: String,
}

impl DesignComposeDefinitionHeader {
    pub fn current(
        last_modified: String,
        name: String,
        response_version: String,
        id: String,
    ) -> DesignComposeDefinitionHeader {
        DesignComposeDefinitionHeader {
            dc_version: CURRENT_VERSION,
            last_modified,
            name,
            response_version,
            id,
        }
    }
    pub fn current_version() -> u32 {
        CURRENT_VERSION
    }
}

impl fmt::Display for DesignComposeDefinitionHeader {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        // NOTE: Using `write!` here instead of typical `format!`
        // to keep newlines.
        write!(
            f,
            "DC Version: {}\nDoc ID: {}\nName: {}\nLast Modified: {}\nResponse Version: {}",
            CURRENT_VERSION, &self.id, &self.name, &self.last_modified, &self.response_version
        )
    }
}
