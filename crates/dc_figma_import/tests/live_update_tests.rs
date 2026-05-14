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

//! End-to-end integration tests for the live update system.
//!
//! These tests validate all phases of the live update overhaul against
//! real Figma API responses. Set the FIGMA_ACCESS_TOKEN environment
//! variable to enable these tests.
//!
//! Run with: FIGMA_ACCESS_TOKEN=<token> cargo test -p dc_figma_import --test live_update_tests

use dc_figma_import::content_hash;
use dc_figma_import::http_client;
use dc_figma_import::node_tracker;
use dc_figma_import::remote_cache;
use std::collections::HashMap;
use std::env;

/// DesignSwitcherDoc — a real doc with 38 components
const DOC_DESIGN_SWITCHER: &str = "Ljph4e3sC0lHcynfXpoh9f";
/// VariablesTestDoc — a real doc with 111 variables (3 collections, 1 remote)
const DOC_VARIABLES_TEST: &str = "HhGxvL4aHhP8ALsLNz56TP";

fn get_api_key() -> Option<String> {
    env::var("FIGMA_ACCESS_TOKEN").ok()
}

// ═══════════════════════════════════════════════════════════════════════
// Phase 1: Correctness
// ═══════════════════════════════════════════════════════════════════════

mod phase1_image_session {
    use dc_figma_import::ImageContextSession;

    #[test]
    fn test_session_roundtrip_with_version() {
        // A freshly serialized session should roundtrip cleanly
        let empty_json = r#"{"version":2,"images":{},"vectors":[],"image_hash":{},"image_bounds":{},"client_images":[]}"#;
        let session: ImageContextSession = serde_json::from_str(empty_json).unwrap();
        let json = serde_json::to_vec(&session).unwrap();
        let restored: ImageContextSession = serde_json::from_slice(&json).unwrap();
        // Both should be the same type — this validates the serde roundtrip
        assert_eq!(
            serde_json::to_string(&session).unwrap(),
            serde_json::to_string(&restored).unwrap()
        );
    }

    #[test]
    fn test_old_session_deserializes_with_default_version() {
        // Simulate an old session JSON without the version field
        let old_json = r#"{"images":{},"vectors":[],"image_hash":{},"client_images":[]}"#;
        let result: Result<ImageContextSession, _> = serde_json::from_str(old_json);
        // Should succeed because version defaults via #[serde(default)]
        assert!(
            result.is_ok(),
            "Old session JSON without version should deserialize: {:?}",
            result.err()
        );
    }
}

mod phase1_remote_cache {
    use super::*;

    #[test]
    fn test_cache_lifecycle() {
        let mut cache = remote_cache::RemoteNodeCache::new();
        assert!(cache.is_empty());

        // Put a response
        cache.put("doc1", "node1", "2024-01-01T00:00:00Z", r#"{"nodes":{}}"#.to_string());
        assert_eq!(cache.len(), 1);

        // Hit with matching timestamp
        let hit = cache.get("doc1", "node1", "2024-01-01T00:00:00Z");
        assert!(hit.is_some());
        assert!(hit.unwrap().contains("nodes"));

        // Miss with different timestamp
        let miss = cache.get("doc1", "node1", "2024-01-02T00:00:00Z");
        assert!(miss.is_none());

        // Document timestamp tracking
        assert_eq!(cache.doc_last_modified("doc1"), Some("2024-01-01T00:00:00Z"));
    }

    #[test]
    fn test_cache_serialization() {
        let mut cache = remote_cache::RemoteNodeCache::new();
        cache.put("doc1", "node1", "ts1", "resp1".to_string());
        cache.put("doc1", "node2", "ts1", "resp2".to_string());

        let json = serde_json::to_string(&cache).unwrap();
        let restored: remote_cache::RemoteNodeCache = serde_json::from_str(&json).unwrap();

        assert!(restored.is_valid_version());
        assert_eq!(restored.len(), 2);
        assert_eq!(restored.get("doc1", "node1", "ts1"), Some("resp1"));
        assert_eq!(restored.get("doc1", "node2", "ts1"), Some("resp2"));
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Phase 2: Performance
// ═══════════════════════════════════════════════════════════════════════

mod phase2_http_client {
    use super::*;

    #[test]
    fn test_batch_fetch_empty() {
        let results =
            http_client::http_fetch_batch("fake_key", vec![], &dc_figma_import::ProxyConfig::None);
        assert!(results.is_empty());
    }

    #[test]
    fn test_batch_fetch_invalid_urls_dont_panic() {
        let requests = vec![http_client::BatchRequest {
            id: "r1".to_string(),
            url: "http://localhost:1/nonexistent".to_string(),
        }];
        let results = http_client::http_fetch_batch(
            "fake_key",
            requests,
            &dc_figma_import::ProxyConfig::None,
        );
        assert_eq!(results.len(), 1);
        assert!(results[0].result.is_err());
    }

    #[test]
    fn test_live_single_fetch() {
        let api_key = match get_api_key() {
            Some(k) => k,
            None => {
                eprintln!("SKIP: FIGMA_ACCESS_TOKEN not set");
                return;
            }
        };

        // Fetch the DesignSwitcher doc metadata
        let url = format!("https://api.figma.com/v1/files/{}?depth=1", DOC_DESIGN_SWITCHER);
        let result = http_client::http_fetch(&api_key, url, &dc_figma_import::ProxyConfig::None);
        assert!(result.is_ok(), "Single fetch failed: {:?}", result.err());

        let body = result.unwrap();
        assert!(body.contains("Design Switcher"), "Response should contain doc name");
        assert!(body.contains("lastModified"), "Response should contain lastModified");
    }

    #[test]
    fn test_live_batch_fetch_concurrent() {
        let api_key = match get_api_key() {
            Some(k) => k,
            None => {
                eprintln!("SKIP: FIGMA_ACCESS_TOKEN not set");
                return;
            }
        };

        // Fetch two docs concurrently
        let requests = vec![
            http_client::BatchRequest {
                id: "design_switcher".to_string(),
                url: format!("https://api.figma.com/v1/files/{}?depth=1", DOC_DESIGN_SWITCHER),
            },
            http_client::BatchRequest {
                id: "variables_test".to_string(),
                url: format!("https://api.figma.com/v1/files/{}?depth=1", DOC_VARIABLES_TEST),
            },
        ];

        let results =
            http_client::http_fetch_batch(&api_key, requests, &dc_figma_import::ProxyConfig::None);

        assert_eq!(results.len(), 2, "Should get 2 responses");

        let ids: std::collections::HashSet<_> = results.iter().map(|r| r.id.clone()).collect();
        assert!(ids.contains("design_switcher"));
        assert!(ids.contains("variables_test"));

        for response in &results {
            assert!(
                response.result.is_ok(),
                "Batch fetch for '{}' failed: {:?}",
                response.id,
                response.result.as_ref().err()
            );
            let body = response.result.as_ref().unwrap();
            assert!(
                body.contains("lastModified"),
                "Response for '{}' should contain lastModified",
                response.id
            );
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Phase 3: Incremental Updates
// ═══════════════════════════════════════════════════════════════════════

mod phase3_content_hashing {
    use super::*;

    #[test]
    fn test_deterministic_hashing() {
        let data = b"test view protobuf bytes";
        let h1 = content_hash::compute_view_hash(data);
        let h2 = content_hash::compute_view_hash(data);
        assert_eq!(h1, h2, "Same input must produce same hash");
    }

    #[test]
    fn test_different_views_different_hashes() {
        let h1 = content_hash::compute_view_hash(b"view_a");
        let h2 = content_hash::compute_view_hash(b"view_b");
        assert_ne!(h1, h2, "Different views must produce different hashes");
    }

    #[test]
    fn test_diff_detects_updates() {
        let mut old = HashMap::new();
        old.insert("view1".to_string(), "hash_old_1".to_string());
        old.insert("view2".to_string(), "hash_old_2".to_string());
        old.insert("view3".to_string(), "hash_old_3".to_string());

        let mut new = HashMap::new();
        new.insert("view1".to_string(), "hash_new_1".to_string()); // changed
        new.insert("view2".to_string(), "hash_old_2".to_string()); // unchanged
        new.insert("view4".to_string(), "hash_new_4".to_string()); // added

        let diff = content_hash::diff_view_hashes(&old, &new);
        assert_eq!(diff.updated_keys, vec!["view1"]);
        assert_eq!(diff.removed_keys, vec!["view3"]);
        assert_eq!(diff.added_keys, vec!["view4"]);
        assert_eq!(diff.change_count(), 3);
        assert!(!diff.is_incremental()); // 3/3 = 100% → NOT incremental
    }

    #[test]
    fn test_incremental_threshold() {
        // 10 views, 2 changed → 20% → incremental
        let old: HashMap<String, String> =
            (0..10).map(|i| (format!("v{}", i), format!("h{}", i))).collect();
        let mut new = old.clone();
        new.insert("v0".to_string(), "changed".to_string());
        new.insert("v1".to_string(), "changed".to_string());

        let diff = content_hash::diff_view_hashes(&old, &new);
        assert!(diff.is_incremental(), "20% change rate should be incremental");

        // 10 views, 6 changed → 60% → NOT incremental
        for i in 0..6 {
            new.insert(format!("v{}", i), format!("changed_{}", i));
        }
        let diff2 = content_hash::diff_view_hashes(&old, &new);
        assert!(!diff2.is_incremental(), "60% change rate should NOT be incremental");
    }

    #[test]
    fn test_bulk_hash_computation() {
        let mut views = HashMap::new();
        for i in 0..100 {
            views.insert(format!("view_{}", i), format!("data_{}", i).into_bytes());
        }

        let hashes = content_hash::compute_all_view_hashes(&views);
        assert_eq!(hashes.len(), 100);

        // All hashes should be unique
        let unique: std::collections::HashSet<_> = hashes.values().collect();
        assert_eq!(unique.len(), 100, "All hashes should be unique");
    }
}

mod phase3_node_tracker {
    use super::*;

    #[test]
    fn test_tracker_lifecycle() {
        let mut tracker = node_tracker::NodeTracker::new();
        assert!(tracker.is_empty());

        // Track some nodes
        let children = vec![("c1".to_string(), "Child1".to_string())];
        tracker.track("1:1".to_string(), "Frame1".to_string(), &children);
        tracker.track("1:2".to_string(), "Frame2".to_string(), &[]);
        assert_eq!(tracker.len(), 2);

        // No changes
        let new_nodes = vec![
            ("1:1".to_string(), "Frame1".to_string(), children.clone()),
            ("1:2".to_string(), "Frame2".to_string(), vec![]),
        ];
        let diff = tracker.diff(&new_nodes);
        assert!(!diff.has_changes());

        // Modify a child
        let new_children = vec![
            ("c1".to_string(), "Child1".to_string()),
            ("c2".to_string(), "Child2".to_string()),
        ];
        let modified_nodes = vec![
            ("1:1".to_string(), "Frame1".to_string(), new_children),
            ("1:2".to_string(), "Frame2".to_string(), vec![]),
        ];
        let diff2 = tracker.diff(&modified_nodes);
        assert!(diff2.has_changes());
        assert_eq!(diff2.changed.len(), 1);
        assert_eq!(diff2.changed[0], "1:1");
    }

    #[test]
    fn test_batch_respects_limit() {
        let ids: Vec<String> = (0..75).map(|i| format!("node_{}", i)).collect();
        let batches = node_tracker::NodeTracker::batch_node_ids(&ids);
        assert_eq!(batches.len(), 2);
        assert_eq!(batches[0].len(), 50);
        assert_eq!(batches[1].len(), 25);
    }

    #[test]
    fn test_live_node_structure_from_figma() {
        let api_key = match get_api_key() {
            Some(k) => k,
            None => {
                eprintln!("SKIP: FIGMA_ACCESS_TOKEN not set");
                return;
            }
        };

        // Fetch shallow doc to get page structure
        let url = format!("https://api.figma.com/v1/files/{}?depth=2", DOC_DESIGN_SWITCHER);
        let body = http_client::http_fetch(&api_key, url, &dc_figma_import::ProxyConfig::None)
            .expect("Failed to fetch doc");

        let doc: serde_json::Value = serde_json::from_str(&body).unwrap();
        let pages = doc["document"]["children"].as_array().unwrap();

        let mut tracker = node_tracker::NodeTracker::new();
        for page in pages {
            let page_id = page["id"].as_str().unwrap().to_string();
            let page_name = page["name"].as_str().unwrap().to_string();
            let children: Vec<(String, String)> = page["children"]
                .as_array()
                .unwrap_or(&vec![])
                .iter()
                .map(|c| {
                    (
                        c["id"].as_str().unwrap_or("").to_string(),
                        c["name"].as_str().unwrap_or("").to_string(),
                    )
                })
                .collect();
            tracker.track(page_id, page_name, &children);
        }

        assert!(tracker.len() > 0, "Should have tracked at least one page");

        // Self-diff should show no changes
        let same_nodes: Vec<_> = pages
            .iter()
            .map(|page| {
                let id = page["id"].as_str().unwrap().to_string();
                let name = page["name"].as_str().unwrap().to_string();
                let children: Vec<(String, String)> = page["children"]
                    .as_array()
                    .unwrap_or(&vec![])
                    .iter()
                    .map(|c| {
                        (
                            c["id"].as_str().unwrap_or("").to_string(),
                            c["name"].as_str().unwrap_or("").to_string(),
                        )
                    })
                    .collect();
                (id, name, children)
            })
            .collect();

        let diff = tracker.diff(&same_nodes);
        assert!(!diff.has_changes(), "Self-diff should show no changes");
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Phase 4: Library Intelligence
// ═══════════════════════════════════════════════════════════════════════

mod phase4_library_resolver {
    use super::*;

    #[test]
    fn test_dependency_graph() {
        let mut resolver = dc_figma_import::library_resolver::LibraryResolver::new(
            "fake_key".to_string(),
            dc_figma_import::ProxyConfig::None,
        );

        resolver.add_dependency("doc1", "lib_a");
        resolver.add_dependency("doc1", "lib_b");
        resolver.add_dependency("doc2", "lib_a");

        let doc1_libs = resolver.library_ids("doc1");
        assert_eq!(doc1_libs.len(), 2);
        assert!(doc1_libs.contains("lib_a"));
        assert!(doc1_libs.contains("lib_b"));

        let doc2_libs = resolver.library_ids("doc2");
        assert_eq!(doc2_libs.len(), 1);
    }

    #[test]
    fn test_live_variable_fetch() {
        let api_key = match get_api_key() {
            Some(k) => k,
            None => {
                eprintln!("SKIP: FIGMA_ACCESS_TOKEN not set");
                return;
            }
        };

        // Fetch variables from the VariablesTest doc
        let url = format!("https://api.figma.com/v1/files/{}/variables/local", DOC_VARIABLES_TEST);
        let body = http_client::http_fetch(&api_key, url, &dc_figma_import::ProxyConfig::None)
            .expect("Failed to fetch variables");

        let vars: serde_json::Value = serde_json::from_str(&body).unwrap();
        let collections = vars["meta"]["variableCollections"].as_object().unwrap();
        let variables = vars["meta"]["variables"].as_object().unwrap();

        assert!(collections.len() >= 2, "Should have multiple variable collections");
        assert!(variables.len() >= 50, "Should have many variables");

        // Check for remote collection
        let has_remote = collections.values().any(|c| c["remote"].as_bool().unwrap_or(false));
        assert!(has_remote, "Should have at least one remote collection");

        // Check for alias variables
        let has_alias = variables.values().any(|v| {
            v["valuesByMode"].as_object().map_or(false, |modes| {
                modes.values().any(|val| {
                    val.is_object()
                        && val.get("type").and_then(|t| t.as_str()) == Some("VARIABLE_ALIAS")
                })
            })
        });
        println!("Has alias variables: {}", has_alias);
    }
}

// ═══════════════════════════════════════════════════════════════════════
// End-to-End: Full Pipeline Test
// ═══════════════════════════════════════════════════════════════════════

mod e2e_pipeline {
    use super::*;

    #[test]
    fn test_full_live_update_pipeline() {
        let api_key = match get_api_key() {
            Some(k) => k,
            None => {
                eprintln!("SKIP: FIGMA_ACCESS_TOKEN not set");
                return;
            }
        };

        println!("=== E2E: Full Live Update Pipeline ===");

        // Step 1: Fetch document metadata (simulates initial poll)
        println!("[1] Fetching initial document metadata...");
        let url = format!("https://api.figma.com/v1/files/{}?depth=2", DOC_DESIGN_SWITCHER);
        let body = http_client::http_fetch(&api_key, url, &dc_figma_import::ProxyConfig::None)
            .expect("Failed to fetch doc");

        let doc: serde_json::Value = serde_json::from_str(&body).unwrap();
        let last_modified = doc["lastModified"].as_str().unwrap();
        let version = doc["version"].as_str().unwrap();
        println!("  Doc: {} (v{})", doc["name"].as_str().unwrap(), version);
        println!("  Last modified: {}", last_modified);

        // Step 2: Track page structure
        println!("[2] Building node tracker...");
        let mut tracker = node_tracker::NodeTracker::new();
        let pages = doc["document"]["children"].as_array().unwrap();
        for page in pages {
            let id = page["id"].as_str().unwrap().to_string();
            let name = page["name"].as_str().unwrap().to_string();
            let children: Vec<(String, String)> = page["children"]
                .as_array()
                .unwrap_or(&vec![])
                .iter()
                .map(|c| {
                    (
                        c["id"].as_str().unwrap_or("").to_string(),
                        c["name"].as_str().unwrap_or("").to_string(),
                    )
                })
                .collect();
            tracker.track(id, name, &children);
        }
        println!("  Tracked {} pages", tracker.len());

        // Step 3: Build remote node cache
        println!("[3] Building remote node cache...");
        let mut cache = remote_cache::RemoteNodeCache::new();
        cache.set_doc_last_modified(DOC_DESIGN_SWITCHER, last_modified);
        assert_eq!(cache.doc_last_modified(DOC_DESIGN_SWITCHER), Some(last_modified));

        // Step 4: Simulate content hashing
        println!("[4] Computing content hashes...");
        let mut fake_views = HashMap::new();
        let components = doc["components"].as_object().unwrap();
        for (id, comp) in components {
            let name = comp["name"].as_str().unwrap_or("unknown");
            let view_data = format!("{}-{}-{}", id, name, last_modified);
            fake_views.insert(id.clone(), view_data.into_bytes());
        }
        let hashes = content_hash::compute_all_view_hashes(&fake_views);
        println!("  Computed {} view hashes", hashes.len());

        // Step 5: Simulate second poll — no changes
        println!("[5] Simulating second poll (no changes)...");
        let same_hashes = content_hash::compute_all_view_hashes(&fake_views);
        let diff = content_hash::diff_view_hashes(&hashes, &same_hashes);
        assert_eq!(diff.change_count(), 0, "No changes expected on same data");
        println!("  No changes detected ✓");

        // Step 6: Simulate incremental change
        println!("[6] Simulating incremental update...");
        let mut modified_views = fake_views.clone();
        if let Some((key, _)) = modified_views.iter().next() {
            let key = key.clone();
            modified_views.insert(key.clone(), b"modified_view_data".to_vec());
        }
        let modified_hashes = content_hash::compute_all_view_hashes(&modified_views);
        let diff = content_hash::diff_view_hashes(&hashes, &modified_hashes);
        assert_eq!(diff.updated_keys.len(), 1, "Should detect 1 updated view");
        println!("  Detected {} updated view(s) ✓", diff.updated_keys.len());
        println!("  Is incremental: {} ✓", diff.is_incremental());

        // Step 7: Fetch variables
        println!("[7] Fetching variables...");
        let var_url =
            format!("https://api.figma.com/v1/files/{}/variables/local", DOC_VARIABLES_TEST);
        let var_result =
            http_client::http_fetch(&api_key, var_url, &dc_figma_import::ProxyConfig::None);
        assert!(var_result.is_ok(), "Variable fetch should succeed");
        let var_body = var_result.unwrap();
        let vars: serde_json::Value = serde_json::from_str(&var_body).unwrap();
        let var_count = vars["meta"]["variables"].as_object().map(|v| v.len()).unwrap_or(0);
        let collection_count =
            vars["meta"]["variableCollections"].as_object().map(|c| c.len()).unwrap_or(0);
        println!("  Fetched {} variables in {} collections ✓", var_count, collection_count);

        // Step 8: Batch fetch images
        println!("[8] Batch-fetching image refs...");
        let requests = vec![
            http_client::BatchRequest {
                id: "design_switcher_images".to_string(),
                url: format!("https://api.figma.com/v1/files/{}/images", DOC_DESIGN_SWITCHER),
            },
            http_client::BatchRequest {
                id: "variables_test_images".to_string(),
                url: format!("https://api.figma.com/v1/files/{}/images", DOC_VARIABLES_TEST),
            },
        ];
        let img_results =
            http_client::http_fetch_batch(&api_key, requests, &dc_figma_import::ProxyConfig::None);
        assert_eq!(img_results.len(), 2);
        for r in &img_results {
            assert!(
                r.result.is_ok(),
                "Image fetch for {} failed: {:?}",
                r.id,
                r.result.as_ref().err()
            );
        }
        println!("  Fetched image refs for 2 docs concurrently ✓");

        // Step 9: Cache serialization roundtrip
        println!("[9] Testing cache serialization...");
        let cache_json = serde_json::to_string(&cache).unwrap();
        let restored: remote_cache::RemoteNodeCache = serde_json::from_str(&cache_json).unwrap();
        assert!(restored.is_valid_version());
        assert_eq!(restored.doc_last_modified(DOC_DESIGN_SWITCHER), Some(last_modified));
        println!("  Cache roundtrip validated ✓");

        println!("=== E2E: All steps completed successfully ===");
    }

    #[test]
    fn test_concurrent_multi_doc_fetch_performance() {
        let api_key = match get_api_key() {
            Some(k) => k,
            None => {
                eprintln!("SKIP: FIGMA_ACCESS_TOKEN not set");
                return;
            }
        };

        // Time sequential vs concurrent fetches
        let urls: Vec<String> = vec![DOC_DESIGN_SWITCHER, DOC_VARIABLES_TEST]
            .iter()
            .map(|id| format!("https://api.figma.com/v1/files/{}?depth=1", id))
            .collect();

        // Sequential
        let seq_start = std::time::Instant::now();
        for url in &urls {
            let _ =
                http_client::http_fetch(&api_key, url.clone(), &dc_figma_import::ProxyConfig::None)
                    .unwrap();
        }
        let seq_duration = seq_start.elapsed();

        // Concurrent
        let requests: Vec<http_client::BatchRequest> = urls
            .iter()
            .enumerate()
            .map(|(i, url)| http_client::BatchRequest {
                id: format!("req_{}", i),
                url: url.clone(),
            })
            .collect();

        let conc_start = std::time::Instant::now();
        let results =
            http_client::http_fetch_batch(&api_key, requests, &dc_figma_import::ProxyConfig::None);
        let conc_duration = conc_start.elapsed();

        for r in &results {
            assert!(r.result.is_ok());
        }

        println!(
            "Sequential: {:?}, Concurrent: {:?}, Speedup: {:.2}x",
            seq_duration,
            conc_duration,
            seq_duration.as_millis() as f64 / conc_duration.as_millis().max(1) as f64
        );

        // Concurrent should be at least somewhat faster (or equal for 2 requests)
        // Don't assert on timing as network latency varies
    }
}
