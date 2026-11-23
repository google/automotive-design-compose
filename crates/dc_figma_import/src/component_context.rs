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

use crate::figma_schema;
use dc_bundle::{
    definition::NodeQuery,
    reaction::{action::Action_type, Reaction},
};
use std::collections::{BTreeSet, HashMap, HashSet};

/// ComponentContext is created by Document, and is used to ensure that all dependent nodes and
/// components are converted from the Figma source JSON. Reactions (interactive links) in a node
/// can refer to another node, which is the primary way we have a tree in a document that refers
/// to out-of-tree nodes.
pub struct ComponentContext {
    /// List of all of the nodes that we have converted as top-level items.
    converted_nodes: HashSet<String>,
    /// List of all of the nodes that we have found reactions or other links to, and still need
    /// to convert (stored in a `BTreeSet` so multi-hop conversions run in deterministic ID order).
    referenced_nodes: BTreeSet<String>,
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
            referenced_nodes: BTreeSet::new(),
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
            if let Some(Action_type::Node(node)) =
                reaction.action.as_ref().and_then(|a| a.action_type.as_ref())
            {
                if let Some(destination_node_id) = &node.destination_id {
                    if !self.converted_nodes.contains(destination_node_id) {
                        self.referenced_nodes.insert(destination_node_id.clone());
                    }
                }
            }
        }
    }

    /// Add a component that we found an instance of.
    pub fn add_component_info(&mut self, component_id: &String) {
        if self.converted_nodes.contains(component_id) {
            return;
        }
        self.referenced_nodes.insert(component_id.clone());
    }

    /// Mark a node ID as scheduled for conversion so subsequent `add_component_info` or
    /// `add_destination_nodes` calls do not re-enqueue it as a duplicate `NodeQuery::NodeId`.
    pub(crate) fn mark_converted(&mut self, node_id: &str) {
        self.referenced_nodes.remove(node_id);
        self.converted_nodes.insert(node_id.to_string());
    }

    /// Get the list of nodes to convert next, and reset the list of referenced nodes.
    pub fn referenced_list<'a>(
        &mut self,
        id_index: &HashMap<String, &'a figma_schema::Node>,
    ) -> Vec<(NodeQuery, &'a figma_schema::Node)> {
        let mut list = Vec::new();

        // Create a NodeId query and try to find the node for each referenced node we know about
        // in sorted node-ID order so multi-hop conversions stay deterministic.
        for node_id in std::mem::take(&mut self.referenced_nodes) {
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

#[cfg(test)]
mod tests {
    use super::*;
    use crate::animation_override::test_support::test_frame;
    use dc_bundle::reaction::{
        action::{self, Action_type},
        Action,
    };

    fn node_reaction(destination_id: Option<&str>) -> Reaction {
        Reaction {
            action: Some(Action {
                action_type: Some(Action_type::Node(action::Node {
                    destination_id: destination_id.map(str::to_string),
                    ..Default::default()
                })),
                ..Default::default()
            })
            .into(),
            ..Default::default()
        }
    }

    fn back_reaction() -> Reaction {
        Reaction {
            action: Some(Action {
                action_type: Some(Action_type::Back(
                    ::protobuf::well_known_types::empty::Empty::new(),
                )),
                ..Default::default()
            })
            .into(),
            ..Default::default()
        }
    }

    #[test]
    fn referenced_list_returns_queued_nodes_in_deterministic_sorted_order() {
        let initial_node = test_frame("05:0", "#Start", vec![]);
        let n_02_9 = test_frame("02:9", "#FrameA", vec![]);
        let n_10_1 = test_frame("10:1", "#CompB", vec![]);
        let n_15_0 = test_frame("15:0", "#PreMarked", vec![]);
        let n_20_5 = test_frame("20:5", "#DestC", vec![]);
        let n_30_2 = test_frame("30:2", "#CompD", vec![]);

        let mut id_index = HashMap::new();
        for node in [&initial_node, &n_02_9, &n_10_1, &n_15_0, &n_20_5, &n_30_2] {
            id_index.insert(node.id.clone(), node);
        }

        let initial_queries = vec![(NodeQuery::name("#Start"), &initial_node)];
        let mut ctx = ComponentContext::new(&initial_queries);

        // Queue out-of-order component IDs, reaction destinations, duplicates, initial-converted
        // IDs, pre-marked IDs, Back/unlinked reactions, and a missing node ID.
        ctx.add_component_info(&"30:2".to_string());
        ctx.add_component_info(&"10:1".to_string());
        ctx.add_component_info(&"05:0".to_string());
        ctx.add_component_info(&"15:0".to_string());
        ctx.add_component_info(&"99:9".to_string());
        ctx.add_destination_nodes(&vec![
            node_reaction(Some("20:5")),
            node_reaction(Some("02:9")),
            node_reaction(Some("10:1")),
            node_reaction(Some("05:0")),
            node_reaction(None),
            back_reaction(),
        ]);

        // Pre-marking "15:0" removes it from the pending worklist and prevents re-queuing.
        ctx.mark_converted("15:0");
        ctx.add_component_info(&"15:0".to_string());

        let batch = ctx.referenced_list(&id_index);
        let batch_ids: Vec<(NodeQuery, &str)> =
            batch.iter().map(|(q, n)| (q.clone(), n.id.as_str())).collect();
        assert_eq!(
            batch_ids,
            vec![
                (NodeQuery::id("02:9"), "02:9"),
                (NodeQuery::id("10:1"), "10:1"),
                (NodeQuery::id("20:5"), "20:5"),
                (NodeQuery::id("30:2"), "30:2"),
            ],
            "referenced_list must drain queued nodes in deterministic sorted node-ID order"
        );
        assert_eq!(ctx.missing_nodes(), &HashSet::from(["99:9".to_string()]));

        // Drained worklist is empty, and already-converted IDs are not re-queued on later hops.
        assert!(ctx.referenced_list(&id_index).is_empty());
        ctx.add_component_info(&"10:1".to_string());
        ctx.add_destination_nodes(&vec![node_reaction(Some("20:5"))]);
        assert!(ctx.referenced_list(&id_index).is_empty());
    }
}
