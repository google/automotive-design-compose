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

/// We can fetch nodes either by ID or by their name.
#[derive(Clone, PartialEq, Eq, PartialOrd, Ord, Debug, Hash, Serialize, Deserialize)]
pub enum NodeQuery {
    /// Find the node by ID
    NodeId(String),
    /// Find the node by name
    NodeName(String),
    /// Node by name that is a variant, so the name may have multiple properties
    /// The first string is the node name and the second is its parent's name
    NodeVariant(String, String),
    NodeComponentSet(String),
}

// Helper methods for NodeQuery construction.
impl NodeQuery {
    /// Construct a NodeQuery::NodeId from the given ID string.
    pub fn id(id: impl ToString) -> NodeQuery {
        NodeQuery::NodeId(id.to_string())
    }

    /// Construct a NodeQuery::NodeName from the given ID string.
    pub fn name(name: impl ToString) -> NodeQuery {
        NodeQuery::NodeName(name.to_string())
    }

    /// Construct a NodeQuery::NodeVariant from the given node name
    pub fn variant(name: impl ToString, parent: impl ToString) -> NodeQuery {
        NodeQuery::NodeVariant(name.to_string(), parent.to_string())
    }

    pub fn component_set(name: impl ToString) -> NodeQuery {
        NodeQuery::NodeComponentSet(name.to_string())
    }
}
