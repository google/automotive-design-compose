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

use crate::error::Error;
use dc_bundle::android_interface::{convert_response, ConvertRequest, ConvertResponse};
use dc_bundle::definition::NodeQuery;
use dc_bundle::design_compose_definition::{
    DesignComposeDefinition, DesignComposeDefinitionHeader,
};
use dc_bundle::figma_doc::ServerFigmaDoc;
use dc_figma_import::content_hash;
use dc_figma_import::HiddenNodePolicy;
use dc_figma_import::ImageContextSession;
use dc_figma_import::ProxyConfig;
use log::{debug, info};
use protobuf::Message;
use std::collections::HashMap;
use std::sync::Mutex;
use std::time::Instant;

/// Static cache of resolved node ID mappings per document.
/// Persists across fetch_doc calls so that updates can skip the
/// expensive depth=2 discovery API call (~10-15s).
/// Key: document_id, Value: HashMap<query_name, node_id>
static NODE_ID_CACHE: std::sync::LazyLock<Mutex<HashMap<String, HashMap<String, String>>>> =
    std::sync::LazyLock::new(|| Mutex::new(HashMap::new()));

pub fn fetch_doc(
    id: &str,
    requested_version_id: &str,
    rq: &ConvertRequest,
    proxy_config: &ProxyConfig,
) -> Result<ConvertResponse, Error> {
    let fetch_start = Instant::now();

    debug!(
        "fetch_doc({}): Starting fetch pipeline (queries={}, discover_all={}, threshold={}, has_prev_hashes={})",
        id,
        rq.queries.len(),
        rq.discover_all_top_level_nodes,
        rq.incremental_threshold,
        !rq.previous_view_hashes.is_empty()
    );

    let image_session: Option<ImageContextSession> =
        rq.image_session_json.as_ref().and_then(|json| match serde_json::from_slice(json) {
            Ok(session) => Some(session),
            Err(e) => {
                info!(
                    "fetch_doc({}): ImageContextSession deserialization failed: {}. \
                         Images will be re-fetched from scratch.",
                    id, e
                );
                None
            }
        });

    // Build query name list for node-scoped fetching.
    // If dynamic discovery is requested, force empty query list to trigger
    // a robust full-document fetch instead of a restricted subset.
    let query_names: Vec<String> =
        if rq.discover_all_top_level_nodes { vec![] } else { rq.queries.clone() };

    // Look up cached node IDs from the static in-process cache.
    // ONLY treat as update when we have ACTUAL cached node IDs from a previous
    // successful fetch in this process. Don't use last_modified as it's always
    // set when loading from bundled DCF assets.
    let cached_node_ids: Option<HashMap<String, String>> = if !query_names.is_empty() {
        NODE_ID_CACHE.lock().ok().and_then(|cache| cache.get(id).cloned())
    } else {
        None
    };
    let is_update = cached_node_ids.is_some();
    info!(
        "fetch_doc({}): is_update={}, cached_node_ids={}",
        id,
        is_update,
        cached_node_ids.as_ref().map_or(0, |m| m.len())
    );

    let start = Instant::now();
    if let Some(mut doc) = dc_figma_import::Document::new_if_changed_scoped(
        &rq.figma_api_key,
        id.into(),
        requested_version_id.into(),
        proxy_config,
        rq.last_modified.clone().unwrap_or_default(),
        rq.version.clone().unwrap_or_default(),
        image_session,
        &query_names,
        cached_node_ids,
    )? {
        info!("fetch_doc({}): Figma API fetch took {:?}", id, start.elapsed());

        // Cache the resolved node IDs for future updates
        let doc_node_ids = doc.cached_node_ids();
        if !doc_node_ids.is_empty() {
            if let Ok(mut cache) = NODE_ID_CACHE.lock() {
                cache.insert(id.to_string(), doc_node_ids.clone());
                info!(
                    "fetch_doc({}): Cached {} node IDs for future updates",
                    id,
                    doc_node_ids.len()
                );
            }
        }

        // Build the query list from explicit queries
        let mut queries: Vec<NodeQuery> = rq.queries.iter().map(NodeQuery::name).collect();

        // Extend queries via dynamic discovery: iterates the loaded root node tree to
        // automatically incorporate any newly surfaced top-level frames into the
        // final component view extraction pass below.
        {
            let discovered = doc.discover_top_level_nodes();
            debug!(
                "fetch_doc({}): Dynamic discovery found {} top-level nodes: {:?}",
                id,
                discovered.len(),
                discovered
            );
            for name in discovered {
                let query = NodeQuery::name(name);
                if !queries.contains(&query) {
                    queries.push(query);
                }
            }
            info!(
                "fetch_doc({}): Total queries after discovery: {} (explicit={}, discovered={})",
                id,
                queries.len(),
                rq.queries.len(),
                queries.len() - rq.queries.len()
            );
        }

        // The document has changed since the version the client has, so we should fetch
        // a new copy.
        let mut error_list: Vec<String> = vec![];
        let start = Instant::now();
        let views = doc.nodes(
            &queries,
            &rq.ignored_images
                .iter()
                .map(|imgref| (NodeQuery::name(imgref.node.clone()), imgref.images.clone()))
                .collect(),
            &mut error_list,
            HiddenNodePolicy::Skip, // skip hidden nodes
        )?;
        info!(
            "fetch_doc({}): Node extraction took {:?} ({} nodes, {} errors)",
            id,
            start.elapsed(),
            views.len(),
            error_list.len()
        );
        debug!(
            "fetch_doc({}): Extracted view keys: {:?}",
            id,
            views.keys().map(|q| q.encode()).collect::<Vec<_>>()
        );

        let start = Instant::now();
        let variable_map = doc.build_variable_map();
        info!("fetch_doc({}): Variable map build took {:?}", id, start.elapsed());

        let start = Instant::now();
        let encoded_images = doc.encoded_image_map();
        info!(
            "fetch_doc({}): Image encoding took {:?} ({} images)",
            id,
            start.elapsed(),
            encoded_images.0.len()
        );

        let start = Instant::now();
        let image_session_json = serde_json::to_vec(&doc.image_session())?;
        info!("fetch_doc({}): Image session serialization took {:?}", id, start.elapsed());

        let header = DesignComposeDefinitionHeader::current(
            doc.last_modified().clone(),
            doc.get_name(),
            doc.get_version(),
            doc.get_document_id(),
        );

        // Check if client sent previous view hashes for incremental diffing
        let has_previous_hashes = !rq.previous_view_hashes.is_empty();

        if has_previous_hashes {
            // Use client-configured threshold, defaulting to 0.5 if unset (proto default is 0.0)
            let threshold = if rq.incremental_threshold > 0.0 {
                rq.incremental_threshold as f64
            } else {
                0.5 // Default: 50%
            };

            // Compute current view hashes by serializing each view to protobuf bytes
            let start = Instant::now();
            let serialized_views: HashMap<String, Vec<u8>> = views
                .iter()
                .filter_map(|(query, view)| {
                    view.write_to_bytes().ok().map(|bytes| (query.encode(), bytes))
                })
                .collect();
            let current_hashes = content_hash::compute_all_view_hashes(&serialized_views);
            let diff = content_hash::diff_view_hashes(&rq.previous_view_hashes, &current_hashes);

            let use_incremental = diff.is_incremental_with_threshold(threshold);
            info!(
                "fetch_doc({}): Content hash diff: {} updated, {} added, {} removed, \
                 incremental={} (threshold={:.0}%, took {:?})",
                id,
                diff.updated_keys.len(),
                diff.added_keys.len(),
                diff.removed_keys.len(),
                use_incremental,
                threshold * 100.0,
                start.elapsed()
            );

            if use_incremental && diff.change_count() > 0 {
                info!(
                    "fetch_doc({}): Incremental diff detected ({} updated, {} added, {} removed). \
                     Sending full Document with hashes for next cycle.",
                    id,
                    diff.updated_keys.len(),
                    diff.added_keys.len(),
                    diff.removed_keys.len()
                );
            } else if diff.change_count() == 0 && !rq.previous_view_hashes.is_empty() {
                // Content hashes match — nothing actually changed at the view level
                // even though the document version changed (e.g., metadata-only edit).
                // Still send the full doc to be safe.
                debug!(
                    "fetch_doc({}): Hash diff shows 0 changes, sending full Document anyway",
                    id
                );
            }
            // Fall through to full Document response in all cases
        }

        // Full Document response (default path, or when change rate > threshold)
        // Always compute view hashes so the client can send them back next time
        // to enable incremental updates.
        let start = Instant::now();
        let serialized_views: HashMap<String, Vec<u8>> = views
            .iter()
            .filter_map(|(query, view)| {
                view.write_to_bytes().ok().map(|bytes| (query.encode(), bytes))
            })
            .collect();
        let full_doc_hashes = content_hash::compute_all_view_hashes(&serialized_views);
        debug!(
            "fetch_doc({}): Computed {} view hashes for full Document response",
            id,
            full_doc_hashes.len()
        );

        let figma_doc = DesignComposeDefinition::new_with_details(
            views,
            encoded_images,
            doc.component_sets().clone(),
            variable_map,
        );
        info!("fetch_doc({}): Definition build took {:?}", id, start.elapsed());

        let server_doc = ServerFigmaDoc {
            figma_doc: Some(figma_doc).into(),
            errors: error_list,
            branches: doc.branches.clone(),
            project_files: vec![],
            ..Default::default()
        };

        info!("fetch_doc({}): Total fetch pipeline took {:?}", id, fetch_start.elapsed());

        Ok(ConvertResponse {
            convert_response_type: Some(convert_response::Convert_response_type::Document(
                convert_response::Document {
                    header: Some(header).into(),
                    server_doc: Some(server_doc).into(),
                    image_session_json,
                    view_content_hashes: full_doc_hashes,
                    ..Default::default()
                },
            )),
            ..Default::default()
        })
    } else {
        info!(
            "fetch_doc({}): Document unchanged, skipping fetch ({:?})",
            id,
            fetch_start.elapsed()
        );
        Ok(ConvertResponse {
            convert_response_type: Some(convert_response::Convert_response_type::Unmodified(
                "x".into(),
            )),
            ..Default::default()
        })
    }
}
