// Copyright 2026 Google LLC
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

//! Tracks top-level node structure for selective re-fetching.
//!
//! When a Figma document changes, instead of fetching the entire document tree,
//! the `NodeTracker` can determine which top-level components/frames changed
//! by comparing their metadata (child count, name) against the previous state.
//! Only the changed subtrees are then fetched via `/v1/files/{id}/nodes?ids=`.
//!
//! ## Figma API Considerations
//!
//! - The `/v1/files/{id}/nodes` API has a limit of ~50 node IDs per request
//! - For documents with > 50 top-level nodes, batching is required
//! - A shallow fetch (`depth=2`) gives us enough metadata to detect changes
//!   without downloading the full node tree

use serde::{Deserialize, Serialize};
use std::collections::HashMap;

/// Maximum number of node IDs per `/v1/files/{id}/nodes?ids=` request.
/// Figma doesn't document an exact limit, but ~50 is safe.
pub const MAX_NODE_IDS_PER_REQUEST: usize = 50;

/// Metadata tracked for each top-level node.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct TrackedNode {
    /// Figma node ID.
    pub node_id: String,
    /// Node name for debugging.
    pub name: String,
    /// Number of direct children (used as a quick change indicator).
    pub child_count: usize,
    /// A lightweight structure fingerprint computed from child names/IDs.
    /// Changes when children are added, removed, or reordered.
    pub structure_hash: String,
}

/// Tracks which top-level nodes exist in a document and their structure.
/// Used to determine which nodes need re-fetching after a document update.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct NodeTracker {
    /// node_id -> TrackedNode metadata
    tracked_nodes: HashMap<String, TrackedNode>,
}

impl NodeTracker {
    pub fn new() -> Self {
        NodeTracker { tracked_nodes: HashMap::new() }
    }

    /// Record a top-level node's current state.
    pub fn track(&mut self, node_id: String, name: String, children: &[(String, String)]) {
        let structure_hash = compute_structure_hash(children);
        self.tracked_nodes.insert(
            node_id.clone(),
            TrackedNode { node_id, name, child_count: children.len(), structure_hash },
        );
    }

    /// Given new node metadata, determine which nodes changed.
    ///
    /// Returns a list of node IDs that need re-fetching because:
    /// - They are new (not previously tracked)
    /// - Their structure hash changed (children added/removed/reordered)
    ///
    /// Also returns a list of node IDs that were removed.
    pub fn diff(&self, new_nodes: &[(String, String, Vec<(String, String)>)]) -> NodeDiff {
        let mut changed = Vec::new();
        let mut added = Vec::new();
        let mut removed = Vec::new();

        // Build a set of new node IDs for removal detection
        let new_node_ids: std::collections::HashSet<_> =
            new_nodes.iter().map(|(id, _, _)| id.clone()).collect();

        // Find changed and added nodes
        for (node_id, name, children) in new_nodes {
            let new_hash = compute_structure_hash(children);
            match self.tracked_nodes.get(node_id) {
                Some(tracked) if tracked.structure_hash != new_hash => {
                    changed.push(node_id.clone());
                }
                None => {
                    added.push(node_id.clone());
                }
                _ => {} // unchanged
            }
            let _ = name; // used only for tracking, not comparison
        }

        // Find removed nodes
        for node_id in self.tracked_nodes.keys() {
            if !new_node_ids.contains(node_id) {
                removed.push(node_id.clone());
            }
        }

        NodeDiff { changed, added, removed }
    }

    /// Batch node IDs into groups of MAX_NODE_IDS_PER_REQUEST for API calls.
    pub fn batch_node_ids(node_ids: &[String]) -> Vec<Vec<String>> {
        node_ids.chunks(MAX_NODE_IDS_PER_REQUEST).map(|chunk| chunk.to_vec()).collect()
    }

    /// Number of tracked nodes.
    pub fn len(&self) -> usize {
        self.tracked_nodes.len()
    }

    /// Returns true if no nodes are tracked.
    pub fn is_empty(&self) -> bool {
        self.tracked_nodes.is_empty()
    }
}

/// Result of comparing current node metadata against tracked state.
#[derive(Clone, Debug)]
pub struct NodeDiff {
    /// Node IDs whose structure changed (need re-fetching).
    pub changed: Vec<String>,
    /// Node IDs that are new (need initial fetch).
    pub added: Vec<String>,
    /// Node IDs that were removed.
    pub removed: Vec<String>,
}

impl NodeDiff {
    /// All node IDs that need fetching (changed + added).
    pub fn fetch_ids(&self) -> Vec<String> {
        let mut ids = self.changed.clone();
        ids.extend(self.added.iter().cloned());
        ids
    }

    /// Returns true if there are any changes.
    pub fn has_changes(&self) -> bool {
        !self.changed.is_empty() || !self.added.is_empty() || !self.removed.is_empty()
    }
}

/// Compute a lightweight structure hash from a list of (child_id, child_name) pairs.
/// This changes when children are added, removed, or reordered.
fn compute_structure_hash(children: &[(String, String)]) -> String {
    use std::hash::{Hash, Hasher};

    struct Fnv1a(u64);
    impl Fnv1a {
        fn new() -> Self {
            Fnv1a(0xcbf29ce484222325)
        }
    }
    impl Hasher for Fnv1a {
        fn write(&mut self, bytes: &[u8]) {
            for &b in bytes {
                self.0 ^= b as u64;
                self.0 = self.0.wrapping_mul(0x00000100000001B3);
            }
        }
        fn finish(&self) -> u64 {
            self.0
        }
    }

    let mut hasher = Fnv1a::new();
    for (id, name) in children {
        id.hash(&mut hasher);
        name.hash(&mut hasher);
    }
    format!("{:016x}", hasher.finish())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_empty_tracker_all_added() {
        let tracker = NodeTracker::new();
        let new_nodes = vec![
            ("1:1".to_string(), "Frame1".to_string(), vec![]),
            ("1:2".to_string(), "Frame2".to_string(), vec![]),
        ];

        let diff = tracker.diff(&new_nodes);
        assert_eq!(diff.added.len(), 2);
        assert!(diff.changed.is_empty());
        assert!(diff.removed.is_empty());
    }

    #[test]
    fn test_no_changes() {
        let children = vec![("c1".to_string(), "Child1".to_string())];
        let mut tracker = NodeTracker::new();
        tracker.track("1:1".to_string(), "Frame1".to_string(), &children);

        let new_nodes = vec![("1:1".to_string(), "Frame1".to_string(), children)];
        let diff = tracker.diff(&new_nodes);
        assert!(!diff.has_changes());
    }

    #[test]
    fn test_child_added() {
        let old_children = vec![("c1".to_string(), "Child1".to_string())];
        let mut tracker = NodeTracker::new();
        tracker.track("1:1".to_string(), "Frame1".to_string(), &old_children);

        let new_children = vec![
            ("c1".to_string(), "Child1".to_string()),
            ("c2".to_string(), "Child2".to_string()),
        ];
        let new_nodes = vec![("1:1".to_string(), "Frame1".to_string(), new_children)];
        let diff = tracker.diff(&new_nodes);
        assert_eq!(diff.changed, vec!["1:1"]);
    }

    #[test]
    fn test_node_removed() {
        let mut tracker = NodeTracker::new();
        tracker.track("1:1".to_string(), "Frame1".to_string(), &[]);
        tracker.track("1:2".to_string(), "Frame2".to_string(), &[]);

        let new_nodes = vec![("1:1".to_string(), "Frame1".to_string(), vec![])];
        let diff = tracker.diff(&new_nodes);
        assert_eq!(diff.removed, vec!["1:2"]);
    }

    #[test]
    fn test_batch_node_ids() {
        let ids: Vec<String> = (0..120).map(|i| format!("node_{}", i)).collect();
        let batches = NodeTracker::batch_node_ids(&ids);
        assert_eq!(batches.len(), 3); // 50 + 50 + 20
        assert_eq!(batches[0].len(), 50);
        assert_eq!(batches[1].len(), 50);
        assert_eq!(batches[2].len(), 20);
    }
}
