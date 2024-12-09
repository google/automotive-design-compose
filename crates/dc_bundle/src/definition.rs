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
use crate::Error;
use serde::{Deserialize, Serialize};
use std::hash::Hash;

pub mod element;
pub mod interaction;
pub mod layout;
pub mod modifier;
pub mod plugin;
pub mod view;

include!(concat!(env!("OUT_DIR"), "/designcompose.definition.rs"));

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

impl NodeQuery {
    /// Construct a NodeQuery::NodeId from the given ID string.
    pub fn id(id: impl ToString) -> NodeQuery {
        NodeQuery::NodeId(id.to_string())
    }

    /// Construct a NodeQuery::NodeName from the given node name
    pub fn name(name: impl ToString) -> NodeQuery {
        NodeQuery::NodeName(name.to_string())
    }

    /// Construct a NodeQuery::NodeVariant from the given variant name and parent name
    pub fn variant(name: impl ToString, parent: impl ToString) -> NodeQuery {
        NodeQuery::NodeVariant(name.to_string(), parent.to_string())
    }

    /// Construct a NodeQuery::NodeComponentSet from the given node component set name
    pub fn component_set(name: impl ToString) -> NodeQuery {
        NodeQuery::NodeComponentSet(name.to_string())
    }

    pub fn encode(&self) -> String {
        match self {
            NodeQuery::NodeId(id) => format!("id:{}", id),
            NodeQuery::NodeName(name) => format!("name:{}", name),
            NodeQuery::NodeVariant(name, parent) => {
                assert!(!name.contains('\x1f'));
                format!("variant:{}\x1f{}", name, parent)
            }
            NodeQuery::NodeComponentSet(name) => format!("component_set:{}", name),
        }
    }

    pub fn decode(s: &str) -> Result<NodeQuery, Error> {
        let (query_type, query_value) =
            s.split_once(':').ok_or_else(|| Error::InvalidNodeQuery { query: s.to_string() })?;

        match query_type {
            "id" => Ok(NodeQuery::NodeId(query_value.to_string())),
            "name" => Ok(NodeQuery::NodeName(query_value.to_string())),
            "variant" => {
                let variant_parts: Vec<&str> = query_value.split('\x1f').collect();
                if variant_parts.len() != 2 {
                    return Err(Error::InvalidNodeQuery { query: s.to_string() });
                }
                Ok(NodeQuery::NodeVariant(
                    variant_parts[0].to_string(),
                    variant_parts[1].to_string(),
                ))
            }
            "component_set" => Ok(NodeQuery::NodeComponentSet(query_value.to_string())),
            _ => Err(Error::InvalidNodeQuery { query: s.to_string() }),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_node_query_id() {
        let query = NodeQuery::id("test_id");
        assert_eq!(query, NodeQuery::NodeId("test_id".to_string()));
        assert_eq!(query.encode(), "id:test_id");
        assert_eq!(NodeQuery::decode("id:test_id").unwrap(), query);
    }

    #[test]
    fn test_node_query_name() {
        let query = NodeQuery::name("test_name");
        assert_eq!(query, NodeQuery::NodeName("test_name".to_string()));
        assert_eq!(query.encode(), "name:test_name");
        assert_eq!(NodeQuery::decode("name:test_name").unwrap(), query);
    }

    #[test]
    fn test_node_query_variant() {
        let query = NodeQuery::variant("variant_name", "parent_name");
        assert_eq!(
            query,
            NodeQuery::NodeVariant("variant_name".to_string(), "parent_name".to_string())
        );
        assert_eq!(query.encode(), "variant:variant_name\x1fparent_name");
        assert_eq!(NodeQuery::decode("variant:variant_name\x1fparent_name").unwrap(), query);
    }

    #[test]
    fn test_node_query_name_uses_name() {
        let query = NodeQuery::name("name:deadbeef");
        assert_eq!(query, NodeQuery::NodeName("name:deadbeef".to_string()));
        assert_eq!(query.encode(), "name:name:deadbeef");
        assert_eq!(NodeQuery::decode("name:name:deadbeef").unwrap(), query);
    }

    #[test]
    fn test_node_query_component_set() {
        let query = NodeQuery::component_set("component_set_name");
        assert_eq!(query, NodeQuery::NodeComponentSet("component_set_name".to_string()));
        assert_eq!(query.encode(), "component_set:component_set_name");
        assert_eq!(NodeQuery::decode("component_set:component_set_name").unwrap(), query);
    }

    #[test]
    fn test_node_query_from_string_invalid() {
        assert!(NodeQuery::decode("invalid_query").is_err());
        assert!(NodeQuery::decode("id").is_err());
        assert!(NodeQuery::decode("variant:name").is_err()); // Missing parent
        assert!(NodeQuery::decode("variant:name\x1fparent\x1fextra").is_err()); // Extra part
        assert!(NodeQuery::decode("unknown:value").is_err()); // Unknown type
    }

    #[test]
    #[should_panic]
    fn test_node_query_variant_with_unit_separator() {
        let name_with_separator = "name\x1fwith\x1fseparator";
        let parent = "parent";
        let query = NodeQuery::variant(name_with_separator, parent);
        query.encode();
    }
}
