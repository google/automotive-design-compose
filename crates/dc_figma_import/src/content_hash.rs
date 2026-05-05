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

//! Content hashing for View diffing and incremental updates.
//!
//! Computes stable, deterministic hashes of View protobuf messages.
//! These hashes are used to detect which views have changed between
//! poll cycles, enabling the server to send only modified views
//! rather than the entire document definition.
//!
//! ## Stability Guarantees
//!
//! The hash is computed over the canonical protobuf wire format of
//! each View message. This means:
//! - Field ordering doesn't matter (protobuf defines canonical order)
//! - Default values are excluded (proto3 default value semantics)
//! - The hash changes if and only if the View's content changes
//!
//! ## Hash Algorithm
//!
//! We use FNV-1a 64-bit for speed over SHA-256. These hashes are
//! ephemeral (not stored long-term or used for security), so collision
//! resistance is not critical — we only need change detection.

use std::collections::HashMap;
use std::hash::{Hash, Hasher};

/// FNV-1a 64-bit hasher for fast, non-cryptographic hashing.
struct Fnv1aHasher {
    state: u64,
}

impl Fnv1aHasher {
    const FNV_OFFSET_BASIS: u64 = 0xcbf29ce484222325;
    const FNV_PRIME: u64 = 0x00000100000001B3;

    fn new() -> Self {
        Fnv1aHasher { state: Self::FNV_OFFSET_BASIS }
    }
}

impl Hasher for Fnv1aHasher {
    fn write(&mut self, bytes: &[u8]) {
        for &byte in bytes {
            self.state ^= byte as u64;
            self.state = self.state.wrapping_mul(Self::FNV_PRIME);
        }
    }

    fn finish(&self) -> u64 {
        self.state
    }
}

/// Compute a content hash for a serialized View.
///
/// Uses FNV-1a 64-bit hash over the protobuf wire bytes.
/// Returns the hash as a hex string.
pub fn compute_view_hash(view_bytes: &[u8]) -> String {
    let mut hasher = Fnv1aHasher::new();
    view_bytes.hash(&mut hasher);
    format!("{:016x}", hasher.finish())
}

/// Compute content hashes for all views in a definition.
///
/// Takes a map of (node_query_key → serialized_view_bytes) and returns
/// a map of (node_query_key → hash_string).
pub fn compute_all_view_hashes(views: &HashMap<String, Vec<u8>>) -> HashMap<String, String> {
    views.iter().map(|(key, bytes)| (key.clone(), compute_view_hash(bytes))).collect()
}

/// Diff two sets of view hashes to determine what changed.
///
/// Returns:
/// - `updated_keys`: Keys whose hash changed (view was modified)
/// - `removed_keys`: Keys present in `old` but not in `new` (view was deleted)
/// - `added_keys`: Keys present in `new` but not in `old` (view was added)
pub struct ViewDiff {
    /// View keys whose content hash changed.
    pub updated_keys: Vec<String>,
    /// View keys that were removed (present in old, absent in new).
    pub removed_keys: Vec<String>,
    /// View keys that were added (absent in old, present in new).
    pub added_keys: Vec<String>,
    /// Total number of views in the new set (for ratio calculation).
    pub total_new_views: usize,
}

impl ViewDiff {
    /// Returns true if the diff represents a small incremental change
    /// (less than 50% of views changed). If false, a full document
    /// response should be sent instead.
    pub fn is_incremental(&self) -> bool {
        self.is_incremental_with_threshold(0.5)
    }

    /// Returns true if the diff represents a small incremental change
    /// based on the given threshold (0.0 to 1.0). If the ratio of changed
    /// views exceeds the threshold, returns false (full document recommended).
    pub fn is_incremental_with_threshold(&self, threshold: f64) -> bool {
        if self.total_new_views == 0 {
            return false;
        }
        let changed_count =
            self.updated_keys.len() + self.removed_keys.len() + self.added_keys.len();
        (changed_count as f64 / self.total_new_views as f64) <= threshold
    }

    /// Returns the total number of changes.
    pub fn change_count(&self) -> usize {
        self.updated_keys.len() + self.removed_keys.len() + self.added_keys.len()
    }
}

/// Compute the diff between two sets of view content hashes.
pub fn diff_view_hashes(
    old_hashes: &HashMap<String, String>,
    new_hashes: &HashMap<String, String>,
) -> ViewDiff {
    let mut updated_keys = Vec::new();
    let mut removed_keys = Vec::new();
    let mut added_keys = Vec::new();

    // Find updated and removed
    for (key, old_hash) in old_hashes {
        match new_hashes.get(key) {
            Some(new_hash) if new_hash != old_hash => {
                updated_keys.push(key.clone());
            }
            None => {
                removed_keys.push(key.clone());
            }
            _ => {} // unchanged
        }
    }

    // Find added
    for key in new_hashes.keys() {
        if !old_hashes.contains_key(key) {
            added_keys.push(key.clone());
        }
    }

    ViewDiff { updated_keys, removed_keys, added_keys, total_new_views: new_hashes.len() }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_same_bytes_same_hash() {
        let bytes = b"hello world";
        assert_eq!(compute_view_hash(bytes), compute_view_hash(bytes));
    }

    #[test]
    fn test_different_bytes_different_hash() {
        let a = compute_view_hash(b"hello");
        let b = compute_view_hash(b"world");
        assert_ne!(a, b);
    }

    #[test]
    fn test_hash_format() {
        let hash = compute_view_hash(b"test");
        assert_eq!(hash.len(), 16); // 64-bit = 16 hex chars
        assert!(hash.chars().all(|c| c.is_ascii_hexdigit()));
    }

    #[test]
    fn test_diff_no_changes() {
        let mut old = HashMap::new();
        old.insert("a".to_string(), "hash1".to_string());
        old.insert("b".to_string(), "hash2".to_string());

        let diff = diff_view_hashes(&old, &old);
        assert_eq!(diff.change_count(), 0);
        assert!(diff.is_incremental());
    }

    #[test]
    fn test_diff_one_updated() {
        let mut old = HashMap::new();
        old.insert("a".to_string(), "hash1".to_string());
        old.insert("b".to_string(), "hash2".to_string());

        let mut new = HashMap::new();
        new.insert("a".to_string(), "hash1_changed".to_string());
        new.insert("b".to_string(), "hash2".to_string());

        let diff = diff_view_hashes(&old, &new);
        assert_eq!(diff.updated_keys, vec!["a"]);
        assert!(diff.removed_keys.is_empty());
        assert!(diff.added_keys.is_empty());
        assert!(diff.is_incremental());
    }

    #[test]
    fn test_diff_added_and_removed() {
        let mut old = HashMap::new();
        old.insert("a".to_string(), "hash1".to_string());

        let mut new = HashMap::new();
        new.insert("b".to_string(), "hash2".to_string());

        let diff = diff_view_hashes(&old, &new);
        assert_eq!(diff.removed_keys, vec!["a"]);
        assert_eq!(diff.added_keys, vec!["b"]);
        assert!(!diff.is_incremental()); // 100% change rate → not incremental
    }

    #[test]
    fn test_is_incremental_threshold() {
        // 3 views, 1 changed → 33% → incremental
        let mut old = HashMap::new();
        old.insert("a".to_string(), "hash1".to_string());
        old.insert("b".to_string(), "hash2".to_string());
        old.insert("c".to_string(), "hash3".to_string());

        let mut new = old.clone();
        new.insert("a".to_string(), "hash1_changed".to_string());

        let diff = diff_view_hashes(&old, &new);
        assert!(diff.is_incremental());

        // 3 views, 2 changed → 66% → NOT incremental
        new.insert("b".to_string(), "hash2_changed".to_string());
        let diff2 = diff_view_hashes(&old, &new);
        assert!(!diff2.is_incremental());
    }

    #[test]
    fn test_is_incremental_with_custom_threshold() {
        // 4 views, 3 changed → 75% change rate
        let mut old = HashMap::new();
        old.insert("a".to_string(), "hash1".to_string());
        old.insert("b".to_string(), "hash2".to_string());
        old.insert("c".to_string(), "hash3".to_string());
        old.insert("d".to_string(), "hash4".to_string());

        let mut new = old.clone();
        new.insert("a".to_string(), "changed1".to_string());
        new.insert("b".to_string(), "changed2".to_string());
        new.insert("c".to_string(), "changed3".to_string());

        let diff = diff_view_hashes(&old, &new);
        assert_eq!(diff.change_count(), 3);

        // With default 0.5 threshold → 75% > 50% → NOT incremental
        assert!(!diff.is_incremental());
        assert!(!diff.is_incremental_with_threshold(0.5));

        // With 0.8 threshold → 75% <= 80% → IS incremental
        assert!(diff.is_incremental_with_threshold(0.8));

        // With 0.7 threshold → 75% > 70% → NOT incremental
        assert!(!diff.is_incremental_with_threshold(0.7));

        // With 0.75 threshold → 75% <= 75% → IS incremental (boundary)
        assert!(diff.is_incremental_with_threshold(0.75));
    }

    #[test]
    fn test_threshold_zero_always_full() {
        let mut old = HashMap::new();
        old.insert("a".to_string(), "hash1".to_string());
        old.insert("b".to_string(), "hash2".to_string());

        let new = old.clone(); // No changes at all
        let diff = diff_view_hashes(&old, &new);

        // 0 changes with threshold 0.0 → 0% <= 0% → incremental
        assert!(diff.is_incremental_with_threshold(0.0));

        // 1 change with threshold 0.0 → any change > 0% → NOT incremental
        let mut new_changed = old.clone();
        new_changed.insert("a".to_string(), "changed".to_string());
        let diff2 = diff_view_hashes(&old, &new_changed);
        assert!(!diff2.is_incremental_with_threshold(0.0));
    }

    #[test]
    fn test_threshold_one_always_incremental() {
        let mut old = HashMap::new();
        old.insert("a".to_string(), "hash1".to_string());

        // 100% change rate with threshold 1.0 → 100% <= 100% → incremental
        let mut new = HashMap::new();
        new.insert("a".to_string(), "changed".to_string());
        let diff = diff_view_hashes(&old, &new);
        assert!(diff.is_incremental_with_threshold(1.0));

        // Even with added + removed (200% effective), ratio is capped by total_new_views
        let mut old2 = HashMap::new();
        old2.insert("a".to_string(), "hash1".to_string());
        let mut new2 = HashMap::new();
        new2.insert("b".to_string(), "hash2".to_string());
        let diff2 = diff_view_hashes(&old2, &new2);
        // 1 removed + 1 added = 2, total_new = 1 → 200% > 100% → NOT incremental
        assert!(!diff2.is_incremental_with_threshold(1.0));
    }

    #[test]
    fn test_empty_views_not_incremental() {
        let old = HashMap::new();
        let new = HashMap::new();
        let diff = diff_view_hashes(&old, &new);
        assert!(!diff.is_incremental()); // total_new_views == 0
        assert!(!diff.is_incremental_with_threshold(1.0));
    }
}
