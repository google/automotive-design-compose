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

//! Persistent cache for remote node responses across poll cycles.
//!
//! When the live update system fetches component variants from remote documents,
//! those responses are stored in this cache along with the remote document's
//! `last_modified` timestamp. On subsequent poll cycles, the cache allows us
//! to skip re-fetching nodes from remote documents that haven't changed.
//!
//! The cache is serialized as JSON and passed between poll cycles via the
//! `ConvertRequest`/`ConvertResponse` pipeline, similar to `ImageContextSession`.

use serde::{Deserialize, Serialize};
use std::collections::HashMap;

/// Current cache format version. Bump when the serialization format changes.
const CACHE_VERSION: u32 = 1;

/// A cached entry for a remote document, keyed by (doc_id, node_id).
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct RemoteCacheEntry {
    /// The `last_modified` timestamp from the remote document at the time
    /// the nodes were fetched.
    pub last_modified: String,
    /// The serialized JSON of the NodesResponse for re-use.
    /// We store the raw JSON string rather than the deserialized struct
    /// to avoid coupling the cache format to the figma_schema types.
    pub response_json: String,
}

/// Persistent cache for remote node responses across poll cycles.
///
/// Keyed by `(document_id, parent_node_id)` to match the lookup pattern
/// used in `Document::fetch_component_variants`.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct RemoteNodeCache {
    /// Cache format version for forward-compatibility.
    version: u32,
    /// Cached remote node responses.
    /// Key: (remote_doc_id, parent_node_id)
    /// Value: cached entry with last_modified + response JSON
    entries: HashMap<String, RemoteCacheEntry>,
    /// Tracks the last_modified timestamp per remote document.
    /// Used for HEAD-check optimization: if the remote doc's last_modified
    /// hasn't changed, we can skip re-fetching all nodes from that doc.
    doc_timestamps: HashMap<String, String>,
}

impl RemoteNodeCache {
    /// Create a new empty cache.
    pub fn new() -> Self {
        RemoteNodeCache {
            version: CACHE_VERSION,
            entries: HashMap::new(),
            doc_timestamps: HashMap::new(),
        }
    }

    /// Create a cache key from document ID and node ID.
    fn make_key(doc_id: &str, node_id: &str) -> String {
        format!("{}:{}", doc_id, node_id)
    }

    /// Look up a cached response for the given remote document and node.
    /// Returns `Some(response_json)` if the cache entry exists and the
    /// remote document's `last_modified` matches the cached timestamp.
    pub fn get(&self, doc_id: &str, node_id: &str, current_last_modified: &str) -> Option<&str> {
        let key = Self::make_key(doc_id, node_id);
        if let Some(entry) = self.entries.get(&key) {
            if entry.last_modified == current_last_modified {
                return Some(&entry.response_json);
            }
        }
        None
    }

    /// Store a response in the cache.
    pub fn put(&mut self, doc_id: &str, node_id: &str, last_modified: &str, response_json: String) {
        let key = Self::make_key(doc_id, node_id);
        self.entries.insert(
            key,
            RemoteCacheEntry { last_modified: last_modified.to_string(), response_json },
        );
        self.doc_timestamps.insert(doc_id.to_string(), last_modified.to_string());
    }

    /// Get the cached last_modified timestamp for a remote document.
    /// Used to do a HEAD-check before deciding whether to re-fetch.
    pub fn doc_last_modified(&self, doc_id: &str) -> Option<&str> {
        self.doc_timestamps.get(doc_id).map(|s| s.as_str())
    }

    /// Update the last_modified timestamp for a remote document.
    pub fn set_doc_last_modified(&mut self, doc_id: &str, last_modified: &str) {
        self.doc_timestamps.insert(doc_id.to_string(), last_modified.to_string());
    }

    /// Check if this cache has a valid version.
    pub fn is_valid_version(&self) -> bool {
        self.version == CACHE_VERSION
    }

    /// Number of cached entries.
    pub fn len(&self) -> usize {
        self.entries.len()
    }

    /// Returns true if the cache is empty.
    pub fn is_empty(&self) -> bool {
        self.entries.is_empty()
    }
}

impl Default for RemoteNodeCache {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_cache_hit() {
        let mut cache = RemoteNodeCache::new();
        cache.put("doc1", "node1", "2024-01-01", "{\"test\": true}".to_string());

        assert_eq!(cache.get("doc1", "node1", "2024-01-01"), Some("{\"test\": true}"));
    }

    #[test]
    fn test_cache_miss_on_stale_timestamp() {
        let mut cache = RemoteNodeCache::new();
        cache.put("doc1", "node1", "2024-01-01", "{\"test\": true}".to_string());

        // Different last_modified → cache miss
        assert_eq!(cache.get("doc1", "node1", "2024-01-02"), None);
    }

    #[test]
    fn test_cache_miss_on_unknown_key() {
        let cache = RemoteNodeCache::new();
        assert_eq!(cache.get("doc1", "node1", "2024-01-01"), None);
    }

    #[test]
    fn test_doc_timestamps() {
        let mut cache = RemoteNodeCache::new();
        cache.set_doc_last_modified("doc1", "2024-01-01");

        assert_eq!(cache.doc_last_modified("doc1"), Some("2024-01-01"));
        assert_eq!(cache.doc_last_modified("doc2"), None);
    }

    #[test]
    fn test_version_check() {
        let cache = RemoteNodeCache::new();
        assert!(cache.is_valid_version());
    }

    #[test]
    fn test_serialization_roundtrip() {
        let mut cache = RemoteNodeCache::new();
        cache.put("doc1", "node1", "2024-01-01", "{\"nodes\": {}}".to_string());

        let json = serde_json::to_string(&cache).unwrap();
        let restored: RemoteNodeCache = serde_json::from_str(&json).unwrap();

        assert!(restored.is_valid_version());
        assert_eq!(restored.get("doc1", "node1", "2024-01-01"), Some("{\"nodes\": {}}"));
    }
}
