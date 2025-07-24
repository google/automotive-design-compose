use crate::variable::VariableMap;
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
use crate::design_compose_definition::{DesignComposeDefinition, DesignComposeDefinitionHeader};
use crate::view::View;
use crate::Error;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::fmt;
use std::hash::Hash;
use std::sync::Arc;

pub mod element;
pub mod layout;
pub mod modifier;
pub mod view;

// LINT.IfChange
pub static CURRENT_VERSION: u32 = 28;
// Lint.ThenChange(common/src/main/java/com/android/designcompose/common/DCDVersion.kt)

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
            ..Default::default()
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
            &self.dc_version, &self.id, &self.name, &self.last_modified, &self.response_version
        )
    }
}

impl DesignComposeDefinition {
    pub fn new_with_details(
        views: HashMap<NodeQuery, View>,
        images: EncodedImageMap,
        component_sets: HashMap<String, String>,
        variable_map: VariableMap,
    ) -> DesignComposeDefinition {
        DesignComposeDefinition {
            views: views.iter().map(|(k, v)| (k.encode(), v.to_owned())).collect(),
            images: images.into(),
            component_sets,
            variable_map: Some(variable_map).into(),
            ..Default::default()
        }
    }
    pub fn views(&self) -> Result<HashMap<NodeQuery, View>, Error> {
        self.views
            .iter()
            .map(|(k, v)| NodeQuery::decode(k).map(|query| (query, v.clone())))
            .collect::<Result<HashMap<NodeQuery, View>, Error>>()
    }
}

impl fmt::Display for DesignComposeDefinition {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        // NOTE: Using `write!` here instead of typical `format!`
        // to keep newlines.
        write!(
            f,
            "Views: {}\nComponent Sets: {}",
            self.views.keys().count(),
            self.component_sets.keys().count()
        )
    }
}

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
    fn test_header_current() {
        let header = DesignComposeDefinitionHeader::current(
            "2024-01-01".to_string(),
            "Test".to_string(),
            "v1".to_string(),
            "doc1".to_string(),
        );
        assert_eq!(header.dc_version, CURRENT_VERSION);
        assert_eq!(header.last_modified, "2024-01-01");
        assert_eq!(header.name, "Test");
        assert_eq!(header.response_version, "v1");
        assert_eq!(header.id, "doc1");
    }

    #[test]
    fn test_definition_new() {
        let mut views = HashMap::new();
        views
            .insert(NodeQuery::id("view1"), View { id: "view1".to_string(), ..Default::default() });
        let images = EncodedImageMap(HashMap::new());
        let component_sets = HashMap::new();
        let variable_map = VariableMap::new();
        let def =
            DesignComposeDefinition::new_with_details(views, images, component_sets, variable_map);
        assert_eq!(def.views.len(), 1);
    }

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

    #[test]
    fn test_definition_views() {
        let mut views_map = HashMap::new();
        let query = NodeQuery::id("view1");
        let view = View { id: "view1".to_string(), ..Default::default() };
        views_map.insert(query.clone(), view.clone());

        let images = EncodedImageMap(HashMap::new());
        let component_sets = HashMap::new();
        let variable_map = VariableMap::new();
        let def = DesignComposeDefinition::new_with_details(
            views_map,
            images,
            component_sets,
            variable_map,
        );

        let decoded_views = def.views().unwrap();
        assert_eq!(decoded_views.len(), 1);
        assert_eq!(decoded_views.get(&query), Some(&view));
    }

    #[test]
    fn test_encoded_image_map_map() {
        let mut image_data = HashMap::new();
        let image_bytes = serde_bytes::ByteBuf::from(vec![1, 2, 3]);
        image_data.insert("image1".to_string(), Arc::new(image_bytes));

        let encoded_map = EncodedImageMap(image_data.clone());
        let mapped_data = encoded_map.map();

        assert_eq!(image_data, mapped_data);
    }
}

/// EncodedImageMap contains a mapping from ImageKey to network bytes. It can create an
/// ImageMap and is intended to be used when we want to use Figma-defined components but do
/// not want to communicate with the Figma service.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct EncodedImageMap(pub HashMap<String, Arc<serde_bytes::ByteBuf>>);

impl EncodedImageMap {
    pub fn map(&self) -> HashMap<String, Arc<serde_bytes::ByteBuf>> {
        self.0.clone()
    }
}

impl Into<HashMap<String, Vec<u8>>> for EncodedImageMap {
    fn into(self) -> HashMap<String, Vec<u8>> {
        self.0.iter().map(|(k, v)| (k.clone(), v.to_vec())).collect()
    }
}
