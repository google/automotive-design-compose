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

use serde::{Deserialize, Serialize};

/// We can fetch figma documents in a project or from branches of another document.
/// We store each as a name and ID
#[derive(Clone, PartialEq, Eq, Debug, Hash, Serialize, Deserialize)]
pub struct FigmaDocInfo {
    pub name: String,
    pub id: String,
    pub version_id: String,
}

impl FigmaDocInfo {
    pub fn new(name: String, id: String, version_id: String) -> FigmaDocInfo {
        FigmaDocInfo { name, id, version_id }
    }
}
