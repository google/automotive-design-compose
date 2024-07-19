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

use dc_bundle::legacy_definition::element::node::NodeQuery;
use std::collections::{HashMap, HashSet};

use crate::{
    figma_schema,
    reaction_schema::{Action, Reaction},
};

/// ComponentContext is created by Document, and is used to ensure that all dependent nodes and
/// components are converted from the Figma source JSON. Reactions (interactive links) in a node
/// can refer to another node, which is the primary way we have a tree in a document that refers
/// to out-of-tree nodes.
pub struct ComponentContext {
    /// List of all of the nodes that we have converted as top-level items.
    converted_nodes: HashSet<String>,
    /// List of all of the nodes that we have found reactions or other links to, and still need
    /// to convert.
    referenced_nodes: HashSet<String>,
    /// List of nodes that were referenced and couldn't be found. These are errors, since we won't
    /// be able to perform the requested action that referenced these.
    missing_nodes: HashSet<String>,
}

impl ComponentContext {
    /// Create a new ComponentContext which knows that the given nodes will be converted.
    pub fn new(nodes: &Vec<(NodeQuery, &figma_schema::Node)>) -> ComponentContext {
        let mut converted_nodes = HashSet::new();
        for (_, node) in nodes {
            converted_nodes.insert(node.id.clone());
        }
        ComponentContext {
            converted_nodes,
            referenced_nodes: HashSet::new(),
            missing_nodes: HashSet::new(),
        }
    }

    /// Tell the component context about some reactions that have been parsed. It will add
    /// any destination nodes to the list of nodes to be converted so that they're available
    /// at runtime.
    pub fn add_destination_nodes(&mut self, reactions: &Vec<Reaction>) {
        for reaction in reactions {
            // Some actions (like Back and Close) don't have a destination ID, so we don't do
            // anything with those.
            let destination_node_id = match &reaction.action {
                Action::Node { destination_id: Some(id), .. } => id,
                _ => continue,
            };

            // If we've already converted this node then don't add it again.
            if self.converted_nodes.contains(destination_node_id) {
                continue;
            }

            // Add it to the list to convert.
            self.referenced_nodes.insert(destination_node_id.clone());
        }
    }

    /// Add a component that we found an instance of.
    pub fn add_component_info(&mut self, component_id: &String) {
        if self.converted_nodes.contains(component_id) {
            return;
        }
        self.referenced_nodes.insert(component_id.clone());
    }

    /// Get the list of nodes to convert next, and reset the list of referenced nodes.
    pub fn referenced_list<'a>(
        &mut self,
        id_index: &HashMap<String, &'a figma_schema::Node>,
    ) -> Vec<(NodeQuery, &'a figma_schema::Node)> {
        let mut list = Vec::new();

        // Create a NodeId query and try to find the node for each referenced node we know about.
        for node_id in self.referenced_nodes.drain() {
            if let Some(node) = id_index.get(&node_id) {
                list.push((NodeQuery::NodeId(node_id.clone()), *node));
                self.converted_nodes.insert(node_id);
            } else {
                self.missing_nodes.insert(node_id);
            }
        }

        list
    }

    /// Return the missing nodes.
    pub fn missing_nodes(&self) -> &HashSet<String> {
        &self.missing_nodes
    }
}
