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
