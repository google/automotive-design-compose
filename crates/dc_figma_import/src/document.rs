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

use crate::{
    animation_override::AnimationOverridePropagator,
    component_context::ComponentContext,
    error::Error,
    extended_layout_schema::ExtendedAutoLayout,
    figma_schema,
    image_context::{ImageContext, ImageContextSession},
    proxy_config::ProxyConfig,
    transform_flexbox::create_component_flexbox,
    variable_utils::create_variable,
};
use dc_bundle::definition::EncodedImageMap;
use dc_bundle::definition::NodeQuery;
use dc_bundle::figma_doc::FigmaDocInfo;
use dc_bundle::variable::variable_map::NameIdMap;
use dc_bundle::variable::{Collection, Mode, Variable, VariableMap};
use dc_bundle::view::view_data::{Container, View_data_type};
use dc_bundle::view::{ComponentInfo, ComponentOverrides, View, ViewData};
use dc_bundle::view_style::ViewStyle;
use log::{error, info, warn};
use protobuf::MessageField;
use std::{
    collections::{HashMap, HashSet},
    iter::FromIterator,
    sync::{LazyLock, Mutex},
};

const BASE_FILE_URL: &str = "https://api.figma.com/v1/files/";
const BASE_COMPONENT_URL: &str = "https://api.figma.com/v1/components/";
const BASE_PROJECT_URL: &str = "https://api.figma.com/v1/projects/";

/// Per-document cache for expensive data that doesn't change on text/color edits.
/// Populated on the initial full fetch and reused on subsequent update cycles.
#[derive(Clone)]
struct SmartCacheEntry {
    /// Cached /images API response (image hash → URL)
    image_refs: HashMap<String, Option<String>>,
    /// Cached /variables API response
    variables_responses: HashMap<String, figma_schema::VariablesResponse>,
    /// Cached remote component prefetch results (component key → JSON)
    prefetched_components: HashMap<String, String>,
    /// Cached variant nodes from remote libraries
    variant_nodes: HashMap<String, figma_schema::Node>,
    /// Component keys that previously failed (404, 429, etc.)
    failed_component_keys: HashSet<String>,
}

/// Process-wide smart cache, keyed by Figma document ID.
static SMART_CACHE: LazyLock<Mutex<HashMap<String, SmartCacheEntry>>> =
    LazyLock::new(|| Mutex::new(HashMap::new()));

pub use crate::HiddenNodePolicy;

/// Helper function to deserialize JSON strings while reporting errors mapped with the exact property path.
/// This aids troubleshooting by indicating precisely which JSON node caused failure.
fn deserialize_json_with_path<T: serde::de::DeserializeOwned>(json: &str) -> Result<T, Error> {
    let mut deserializer = serde_json::Deserializer::from_str(json);
    serde_path_to_error::deserialize(&mut deserializer).map_err(Into::into)
}

fn http_fetch(api_key: &str, url: String, proxy_config: &ProxyConfig) -> Result<String, Error> {
    crate::http_client::http_fetch(api_key, url, proxy_config)
}

/// Document update requests return this value to indicate if an update was
/// made or not.
#[derive(Copy, Clone, PartialEq, Eq, Debug)]
pub enum UpdateStatus {
    Updated,
    NotUpdated,
}

/// Branches alwasy return head of file, i.e. no version returned
fn get_branches(document_root: &figma_schema::FileResponse) -> Vec<FigmaDocInfo> {
    let mut branches = vec![];
    if let Some(doc_branches) = &document_root.branches {
        for hash in doc_branches {
            if let (Some(Some(id)), Some(Some(name))) = (hash.get("key"), hash.get("name")) {
                let figma_doc =
                    FigmaDocInfo { name: name.clone(), id: id.clone(), ..Default::default() };
                branches.push(figma_doc);
            }
        }
    }
    branches
}

fn load_image_hash_to_res_map(
    document_root: &figma_schema::FileResponse,
) -> HashMap<String, String> {
    let root_node = &document_root.document;
    let shared_plugin_data = &root_node.shared_plugin_data;
    if shared_plugin_data.contains_key("designcompose") {
        let plugin_data = shared_plugin_data.get("designcompose");
        if let Some(vsw_data) = plugin_data {
            if let Some(image_hash_to_res_data) = vsw_data.get("image_hash_to_res") {
                let image_hash_to_res_map: Option<HashMap<String, String>> =
                    serde_json::from_str(image_hash_to_res_data.as_str()).ok();
                if let Some(map) = image_hash_to_res_map {
                    return map;
                }
            }
        }
    }
    return HashMap::new();
}

/// Document is used to access and maintain an entire Figma document, including
/// components and image resources. It can be updated if the source document
/// has changed since this structure was created.
pub struct Document {
    api_key: String,
    document_id: String,
    version_id: String,
    proxy_config: ProxyConfig,
    document_root: figma_schema::FileResponse,
    variables_responses: HashMap<String, figma_schema::VariablesResponse>,
    image_context: ImageContext,
    variant_nodes: HashMap<String, figma_schema::Node>,
    component_sets: HashMap<String, String>,
    pub branches: Vec<FigmaDocInfo>,
    key_to_global_id_map: HashMap<String, String>,
    remote_node_responses: HashMap<(String, String), figma_schema::NodesResponse>,
    /// Tracks remote document IDs whose image refs have already been fetched
    /// to avoid redundant API calls within a single fetch cycle.
    fetched_remote_images: HashSet<String>,
    /// Cache of pre-fetched remote component key responses.
    /// Populated by `prefetch_remote_components()` before the main tree walk.
    /// Key: component key, Value: JSON response body.
    prefetched_components: HashMap<String, String>,
    /// Tracks top-level node structure for selective re-fetching.
    node_tracker: crate::node_tracker::NodeTracker,
    /// Content hashes of views from the last conversion, for incremental diffs.
    last_view_hashes: HashMap<String, String>,
    /// Cached mapping of query names (e.g., "#android-stage") to Figma node IDs
    /// (e.g., "12093:22798"). Populated on first node-scoped fetch and reused on
    /// subsequent update() calls to skip the expensive depth=2 structure fetch.
    cached_node_ids: HashMap<String, String>,
    /// Component keys that previously returned errors (404, 429, etc.).
    /// Skipped on subsequent prefetch rounds to avoid wasting API calls.
    failed_component_keys: HashSet<String>,
    /// Whether this Document was created as an update (vs initial load).
    /// On updates, remote component resolution is skipped since library
    /// components don't change on text/color edits.
    is_update: bool,
}

fn parse_document_id(document_id: &str) -> String {
    if document_id.starts_with("http://") || document_id.starts_with("https://") {
        if let Some(pos) = document_id.find("/file/") {
            let rest = &document_id[pos + 6..];
            if let Some(end_pos) = rest.find('/') {
                return rest[..end_pos].to_string();
            } else {
                return rest.to_string();
            }
        } else if let Some(pos) = document_id.find("/design/") {
            let rest = &document_id[pos + 8..];
            if let Some(end_pos) = rest.find('/') {
                return rest[..end_pos].to_string();
            } else {
                return rest.to_string();
            }
        }
    }
    document_id.to_string()
}

impl Document {
    pub fn root_node(&self) -> &figma_schema::Node {
        &self.document_root.document
    }

    /// Discover all top-level frame/component nodes from the document's pages.
    ///
    /// Iterates document → pages → children and returns the names of all
    /// visible top-level nodes. This enables dynamic root node discovery
    /// without requiring compile-time `@DesignComponent` annotations.
    pub fn discover_top_level_nodes(&self) -> Vec<String> {
        let mut names = Vec::new();
        // document.children = pages
        for page in &self.document_root.document.children {
            // page.children = top-level frames, components, etc.
            for node in &page.children {
                if node.visible && !node.name.is_empty() {
                    names.push(node.name.clone());
                }
            }
        }
        names
    }
    /// Fetch a document from Figma and return a Document instance that can be used
    /// to extract toolkit nodes.
    pub fn new(
        api_key: &str,
        document_id: String,
        version_id: String,
        proxy_config: &ProxyConfig,
        image_session: Option<ImageContextSession>,
    ) -> Result<Document, Error> {
        let document_id = parse_document_id(&document_id);

        // Fetch the document...
        let mut document_url = format!(
            "{}{}?plugin_data=shared&geometry=paths&branch_data=true",
            BASE_FILE_URL, document_id,
        );
        if !version_id.is_empty() {
            document_url.push_str("&version=");
            document_url.push_str(&version_id);
        }
        let document_root: figma_schema::FileResponse =
            deserialize_json_with_path(http_fetch(api_key, document_url, proxy_config)?.as_str())?;

        // ...and the mapping from imageRef to URL. It returns images from all versions.
        let image_ref_url = format!("{}{}/images", BASE_FILE_URL, document_id);
        let image_refs: figma_schema::ImageFillResponse =
            deserialize_json_with_path(http_fetch(api_key, image_ref_url, proxy_config)?.as_str())?;

        let image_hash_to_res_map = load_image_hash_to_res_map(&document_root);
        let mut image_context =
            ImageContext::new(image_refs.meta.images, image_hash_to_res_map, proxy_config);
        if let Some(session) = image_session {
            image_context.add_session_info(session);
        }

        let branches = get_branches(&document_root);
        let mut variables_responses = HashMap::new();
        match Self::fetch_variables(api_key, &document_id, proxy_config).map_err(Error::from) {
            Ok(mut it) => {
                let mut resolver = crate::library_resolver::LibraryResolver::new(
                    api_key.to_string(),
                    proxy_config.clone(),
                );
                resolver.discover_component_dependencies(&document_id, &document_root.components);
                resolver.discover_variable_dependencies(&document_id, &it);
                let remote_vars = resolver.fetch_all_library_variables(&document_id);
                for (_, remote_var_response) in remote_vars {
                    it.meta.variables.extend(remote_var_response.meta.variables);
                    it.meta
                        .variable_collections
                        .extend(remote_var_response.meta.variable_collections);
                }
                variables_responses.insert(document_id.clone(), it);
            }
            Err(err) => {
                error!("Failed to fetch variables for doc {} {:?}", document_id, err);
            }
        };

        Ok(Document {
            api_key: api_key.to_string(),
            document_id,
            version_id,
            proxy_config: proxy_config.clone(),
            document_root,
            variables_responses,
            image_context,
            variant_nodes: HashMap::new(),
            component_sets: HashMap::new(),
            branches,
            key_to_global_id_map: HashMap::new(),
            remote_node_responses: HashMap::new(),
            fetched_remote_images: HashSet::new(),
            prefetched_components: HashMap::new(),
            node_tracker: crate::node_tracker::NodeTracker::new(),
            last_view_hashes: HashMap::new(),
            cached_node_ids: HashMap::new(),
            failed_component_keys: HashSet::new(),
            is_update: false,
        })
    }

    // Fetch and store all the variables, collections, and modes from the Figma document.
    fn fetch_variables(
        api_key: &str,
        document_id: &String,
        proxy_config: &ProxyConfig,
    ) -> Result<figma_schema::VariablesResponse, Error> {
        let variables_url = format!("{}{}/variables/local", BASE_FILE_URL, document_id);
        let var_fetch = http_fetch(api_key, variables_url, proxy_config)?;
        let var_response: figma_schema::VariablesResponse =
            deserialize_json_with_path(var_fetch.as_str())?;
        Ok(var_response)
    }

    /// Fetch a document from Figma only if it has changed since the given last
    /// modified time.
    pub fn new_if_changed(
        api_key: &str,
        document_id: String,
        requested_version_id: String,
        proxy_config: &ProxyConfig,
        last_modified: String,
        last_version: String,
        image_session: Option<ImageContextSession>,
    ) -> Result<Option<Document>, Error> {
        let mut document_head_url = format!("{}{}?depth=1", BASE_FILE_URL, document_id);
        if !requested_version_id.is_empty() {
            document_head_url.push_str("&version=");
            document_head_url.push_str(&requested_version_id);
        }
        let document_head: figma_schema::FileHeadResponse = deserialize_json_with_path(
            http_fetch(api_key, document_head_url, proxy_config)?.as_str(),
        )?;

        if document_head.last_modified == last_modified && document_head.version == last_version {
            return Ok(None);
        }

        Document::new(api_key, document_id, requested_version_id, proxy_config, image_session)
            .map(Some)
    }

    /// Resolve query names (e.g., "#android-stage") to Figma node IDs
    /// by scanning the depth=2 document tree.
    fn resolve_query_node_ids(
        document: &figma_schema::Node,
        query_names: &[String],
    ) -> HashMap<String, String> {
        let mut result = HashMap::new();
        // Build a name → id map from the document tree (depth=2 has pages → top-level nodes)
        fn scan_for_names(node: &figma_schema::Node, name_to_id: &mut HashMap<String, String>) {
            if !node.name.is_empty() {
                name_to_id.insert(node.name.clone(), node.id.clone());
            }
            for child in &node.children {
                scan_for_names(child, name_to_id);
            }
        }

        let mut name_to_id = HashMap::new();
        scan_for_names(document, &mut name_to_id);

        for name in query_names {
            // Strip leading '#' if present (convention for DesignComponent node names)
            let lookup = if name.starts_with('#') { name.as_str() } else { name };
            if let Some(id) = name_to_id.get(lookup) {
                result.insert(name.clone(), id.clone());
            }
        }
        result
    }

    /// Inject fetched node subtrees into a shallow (depth=2) document tree.
    /// Replaces the stub children of matching top-level nodes with the
    /// fully-expanded subtree from the /nodes endpoint response.
    fn inject_fetched_nodes(
        document: &mut figma_schema::Node,
        fetched: &figma_schema::NodesResponse,
    ) -> usize {
        let mut injected_count = 0;
        for page in &mut document.children {
            for top_node in &mut page.children {
                if let Some(node_data) = fetched.nodes.get(&top_node.id) {
                    *top_node = node_data.document.clone();
                    injected_count += 1;
                }
            }
        }
        injected_count
    }

    /// Fetch a document from Figma using node-scoped fetching.
    /// Instead of downloading the entire file (which can be 1+ GB for large docs),
    /// this method:
    /// 1. Fetches depth=2 to get the document structure and top-level node IDs
    ///    (or uses pre_cached_node_ids to skip this step on updates)
    /// 2. Resolves query names to Figma node IDs
    /// 3. Fetches only the requested node subtrees via /nodes?ids=
    /// 4. Injects the fetched subtrees into the depth=2 document
    pub fn new_node_scoped(
        api_key: &str,
        document_id: String,
        version_id: String,
        proxy_config: &ProxyConfig,
        image_session: Option<ImageContextSession>,
        query_names: &[String],
        pre_cached_node_ids: Option<HashMap<String, String>>,
    ) -> Result<Document, Error> {
        let document_id = parse_document_id(&document_id);
        let is_update = pre_cached_node_ids.is_some();

        // Step 1: Get name→ID mapping AND base document tree.
        // depth=2 is needed for inject_fetched_nodes to find parent nodes.
        // On updates with cached IDs, skip discovery but still need depth=2 for injection.
        let (name_to_id, mut document_root) = if let Some(cached) =
            pre_cached_node_ids.filter(|c| !c.is_empty())
        {
            info!(
                "Node-scoped UPDATE: using {} pre-cached node IDs (depth=2 with ids= filter for fast response)",
                cached.len()
            );
            // Fast path: depth=2 but with ids= parameter to filter the response.
            // Figma returns ONLY the requested subtrees, dramatically reducing
            // the response payload and server processing time (~2-5s vs 22-47s).
            let ids_param: String =
                cached.values().map(|s| s.as_str()).collect::<Vec<_>>().join(",");
            let mut base_url = format!(
                "{}{}?depth=2&ids={}&branch_data=true&plugin_data=shared&geometry=paths",
                BASE_FILE_URL, document_id, ids_param,
            );
            if !version_id.is_empty() {
                base_url.push_str("&version=");
                base_url.push_str(&version_id);
            }
            let doc_root: figma_schema::FileResponse =
                deserialize_json_with_path(http_fetch(api_key, base_url, proxy_config)?.as_str())?;
            (cached, doc_root)
        } else {
            // Initial load OR update without cached IDs: fetch depth=2 for both
            // discovery AND as base document.
            let mut discovery_url =
                format!("{}{}?depth=2&branch_data=true", BASE_FILE_URL, document_id,);
            if !version_id.is_empty() {
                discovery_url.push_str("&version=");
                discovery_url.push_str(&version_id);
            }
            info!(
                "Node-scoped {}: depth=2 for name→ID discovery + base ({})",
                if is_update { "UPDATE" } else { "fetch" },
                document_id
            );
            let discovery_root: figma_schema::FileResponse = deserialize_json_with_path(
                http_fetch(api_key, discovery_url, proxy_config)?.as_str(),
            )?;
            let resolved = Self::resolve_query_node_ids(&discovery_root.document, query_names);
            info!(
                "Node-scoped fetch: resolved {}/{} query names to node IDs: {:?}",
                resolved.len(),
                query_names.len(),
                resolved
            );
            (resolved, discovery_root)
        };

        if !name_to_id.is_empty() && !is_update {
            // Step 3: Fetch the specific node subtrees (initial fetch only).
            // On updates, the /files?ids= response already contains the full subtrees.
            let node_ids: Vec<&str> = name_to_id.values().map(|s| s.as_str()).collect();
            let ids_param = node_ids.join(",");
            let nodes_url = format!(
                "{}{}/nodes?ids={}&plugin_data=shared&geometry=paths",
                BASE_FILE_URL, document_id, ids_param
            );
            info!(
                "Node-scoped fetch: fetching {} nodes via /nodes?ids={}",
                node_ids.len(),
                ids_param
            );
            let nodes_response: figma_schema::NodesResponse =
                deserialize_json_with_path(http_fetch(api_key, nodes_url, proxy_config)?.as_str())?;

            // Merge components from nodes response into document_root
            for (_node_id, node_data) in &nodes_response.nodes {
                for (comp_id, comp) in &node_data.components {
                    document_root.components.insert(comp_id.clone(), comp.clone());
                }
            }

            // Step 4: Inject fetched subtrees into the shallow document
            let injected = Self::inject_fetched_nodes(&mut document_root.document, &nodes_response);
            info!("Node-scoped fetch: injected {} node subtrees into document", injected);
        } else if is_update {
            info!(
                "Node-scoped UPDATE: skipping /nodes call (subtrees already in /files?ids= response)"
            );
        }

        // Smart cache: on updates, reuse cached /images and /variables data.
        // On initial load, fetch fresh and store in cache.
        let (image_context, variables_responses) = if is_update {
            let cached = SMART_CACHE.lock().ok().and_then(|c| c.get(&document_id).cloned());
            if let Some(entry) = cached {
                info!("Smart cache HIT: reusing cached /images ({} refs) and /variables ({} responses) for {}",
                    entry.image_refs.len(), entry.variables_responses.len(), document_id);
                let image_hash_to_res_map = load_image_hash_to_res_map(&document_root);
                let mut ctx =
                    ImageContext::new(entry.image_refs, image_hash_to_res_map, proxy_config);
                if let Some(session) = image_session {
                    ctx.add_session_info(session);
                }
                (ctx, entry.variables_responses)
            } else {
                info!("Smart cache MISS on update for {} — doing full fetch", document_id);
                // Fallback to full fetch
                let image_ref_url = format!("{}{}/images", BASE_FILE_URL, document_id);
                let image_refs: figma_schema::ImageFillResponse = deserialize_json_with_path(
                    http_fetch(api_key, image_ref_url, proxy_config)?.as_str(),
                )?;
                let image_hash_to_res_map = load_image_hash_to_res_map(&document_root);
                let mut ctx =
                    ImageContext::new(image_refs.meta.images, image_hash_to_res_map, proxy_config);
                if let Some(session) = image_session {
                    ctx.add_session_info(session);
                }
                let mut vars = HashMap::new();
                match Self::fetch_variables(api_key, &document_id, proxy_config)
                    .map_err(Error::from)
                {
                    Ok(mut it) => {
                        let mut resolver = crate::library_resolver::LibraryResolver::new(
                            api_key.to_string(),
                            proxy_config.clone(),
                        );
                        resolver.discover_component_dependencies(
                            &document_id,
                            &document_root.components,
                        );
                        resolver.discover_variable_dependencies(&document_id, &it);
                        let remote_vars = resolver.fetch_all_library_variables(&document_id);
                        for (_, remote_var_response) in remote_vars {
                            it.meta.variables.extend(remote_var_response.meta.variables);
                            it.meta
                                .variable_collections
                                .extend(remote_var_response.meta.variable_collections);
                        }
                        vars.insert(document_id.clone(), it);
                    }
                    Err(err) => {
                        error!("Failed to fetch variables for doc {} {:?}", document_id, err);
                    }
                };
                (ctx, vars)
            }
        } else {
            // Initial load: full fetch of images and variables
            let image_ref_url = format!("{}{}/images", BASE_FILE_URL, document_id);
            let image_refs: figma_schema::ImageFillResponse = deserialize_json_with_path(
                http_fetch(api_key, image_ref_url, proxy_config)?.as_str(),
            )?;

            let image_hash_to_res_map = load_image_hash_to_res_map(&document_root);
            let mut ctx = ImageContext::new(
                image_refs.meta.images.clone(),
                image_hash_to_res_map,
                proxy_config,
            );
            if let Some(session) = image_session {
                ctx.add_session_info(session);
            }

            let mut vars = HashMap::new();
            match Self::fetch_variables(api_key, &document_id, proxy_config).map_err(Error::from) {
                Ok(mut it) => {
                    let mut resolver = crate::library_resolver::LibraryResolver::new(
                        api_key.to_string(),
                        proxy_config.clone(),
                    );
                    resolver
                        .discover_component_dependencies(&document_id, &document_root.components);
                    resolver.discover_variable_dependencies(&document_id, &it);
                    let remote_vars = resolver.fetch_all_library_variables(&document_id);
                    for (_, remote_var_response) in remote_vars {
                        it.meta.variables.extend(remote_var_response.meta.variables);
                        it.meta
                            .variable_collections
                            .extend(remote_var_response.meta.variable_collections);
                    }
                    vars.insert(document_id.clone(), it);
                }
                Err(err) => {
                    error!("Failed to fetch variables for doc {} {:?}", document_id, err);
                }
            };

            // Store in smart cache for future updates
            if let Ok(mut cache) = SMART_CACHE.lock() {
                cache.insert(
                    document_id.clone(),
                    SmartCacheEntry {
                        image_refs: image_refs.meta.images,
                        variables_responses: vars.clone(),
                        prefetched_components: HashMap::new(), // populated later in nodes()
                        variant_nodes: HashMap::new(),         // populated later in nodes()
                        failed_component_keys: HashSet::new(), // populated later in nodes()
                    },
                );
                info!("Smart cache: stored initial /images and /variables for {}", document_id);
            }

            (ctx, vars)
        };

        let branches = get_branches(&document_root);

        Ok(Document {
            api_key: api_key.to_string(),
            document_id,
            version_id,
            proxy_config: proxy_config.clone(),
            document_root,
            variables_responses,
            image_context,
            variant_nodes: HashMap::new(),
            component_sets: HashMap::new(),
            branches,
            key_to_global_id_map: HashMap::new(),
            remote_node_responses: HashMap::new(),
            fetched_remote_images: HashSet::new(),
            prefetched_components: HashMap::new(),
            node_tracker: crate::node_tracker::NodeTracker::new(),
            last_view_hashes: HashMap::new(),
            cached_node_ids: name_to_id,
            failed_component_keys: HashSet::new(),
            is_update,
        })
    }

    /// Fetch a document from Figma only if it has changed since the given last
    /// modified time. Uses node-scoped fetching when query_names are provided.
    /// `cached_node_ids` enables the update fast-path: skips depth=2 discovery,
    /// /images, /variables, and remote component resolution.
    pub fn new_if_changed_scoped(
        api_key: &str,
        document_id: String,
        requested_version_id: String,
        proxy_config: &ProxyConfig,
        last_modified: String,
        last_version: String,
        image_session: Option<ImageContextSession>,
        query_names: &[String],
        cached_node_ids: Option<HashMap<String, String>>,
    ) -> Result<Option<Document>, Error> {
        let parsed_id = parse_document_id(&document_id);
        let mut document_head_url = format!("{}{}?depth=1", BASE_FILE_URL, parsed_id);
        if !requested_version_id.is_empty() {
            document_head_url.push_str("&version=");
            document_head_url.push_str(&requested_version_id);
        }
        let document_head: figma_schema::FileHeadResponse = deserialize_json_with_path(
            http_fetch(api_key, document_head_url, proxy_config)?.as_str(),
        )?;

        if document_head.last_modified == last_modified && document_head.version == last_version {
            return Ok(None);
        }

        if query_names.is_empty() {
            // No queries — fall back to full doc fetch
            Document::new(api_key, document_id, requested_version_id, proxy_config, image_session)
                .map(Some)
        } else {
            // Use node-scoped fetching for efficiency
            Document::new_node_scoped(
                api_key,
                document_id,
                requested_version_id,
                proxy_config,
                image_session,
                query_names,
                cached_node_ids,
            )
            .map(Some)
        }
    }

    /// Ask Figma if an updated document is available, and then fetch the updated document
    /// if so. Uses node-scoped fetching when query_names are provided.
    pub fn update(
        &mut self,
        proxy_config: &ProxyConfig,
        query_names: Option<&[String]>,
    ) -> Result<UpdateStatus, Error> {
        self.proxy_config = proxy_config.clone();

        // Fetch just the top level of the document. (depth=0 causes an internal server error).
        let mut document_head_url = format!("{}{}?depth=1", BASE_FILE_URL, self.document_id);
        if !self.version_id.is_empty() {
            document_head_url.push_str("&version=");
            document_head_url.push_str(&self.version_id);
        }
        let document_head: figma_schema::FileHeadResponse = deserialize_json_with_path(
            http_fetch(self.api_key.as_str(), document_head_url, &self.proxy_config)?.as_str(),
        )?;

        // Now compare the version and modification times and bail out if they're the same.
        if document_head.last_modified == self.document_root.last_modified
            && document_head.version == self.document_root.version
        {
            return Ok(UpdateStatus::NotUpdated);
        }

        // Decide between node-scoped and full-doc fetch
        if let Some(names) = query_names {
            if !names.is_empty() {
                // Try to use cached node IDs from the initial fetch to skip depth=2
                let name_to_id = if !self.cached_node_ids.is_empty() {
                    info!(
                        "Node-scoped update: using {} cached node IDs (skipping depth=2)",
                        self.cached_node_ids.len()
                    );
                    self.cached_node_ids.clone()
                } else {
                    // First update or cache miss — do lightweight depth=2
                    let mut discovery_url =
                        format!("{}{}?depth=2", BASE_FILE_URL, self.document_id,);
                    if !self.version_id.is_empty() {
                        discovery_url.push_str("&version=");
                        discovery_url.push_str(&self.version_id);
                    }
                    info!("Node-scoped update: lightweight depth=2 for name→ID discovery");
                    let discovery_root: figma_schema::FileResponse = deserialize_json_with_path(
                        http_fetch(self.api_key.as_str(), discovery_url, &self.proxy_config)?
                            .as_str(),
                    )?;
                    let resolved = Self::resolve_query_node_ids(&discovery_root.document, names);
                    self.cached_node_ids = resolved.clone();
                    resolved
                };

                info!(
                    "Node-scoped update: using {}/{} query→ID mappings",
                    name_to_id.len(),
                    names.len()
                );

                // Fetch a lightweight depth=1 structure as the base document_root
                // (we need version/lastModified metadata but not the full tree)
                let mut base_url =
                    format!("{}{}?depth=1&branch_data=true", BASE_FILE_URL, self.document_id,);
                if !self.version_id.is_empty() {
                    base_url.push_str("&version=");
                    base_url.push_str(&self.version_id);
                }
                let mut document_root: figma_schema::FileResponse = deserialize_json_with_path(
                    http_fetch(self.api_key.as_str(), base_url, &self.proxy_config)?.as_str(),
                )?;

                if !name_to_id.is_empty() {
                    let ids_param: String =
                        name_to_id.values().map(|s| s.as_str()).collect::<Vec<_>>().join(",");
                    let nodes_url = format!(
                        "{}{}/nodes?ids={}&plugin_data=shared&geometry=paths",
                        BASE_FILE_URL, self.document_id, ids_param
                    );
                    info!("Node-scoped update: fetching /nodes?ids={}", ids_param);
                    let nodes_response: figma_schema::NodesResponse = deserialize_json_with_path(
                        http_fetch(self.api_key.as_str(), nodes_url, &self.proxy_config)?.as_str(),
                    )?;

                    // Merge components
                    for (_node_id, node_data) in &nodes_response.nodes {
                        for (comp_id, comp) in &node_data.components {
                            document_root.components.insert(comp_id.clone(), comp.clone());
                        }
                    }

                    Self::inject_fetched_nodes(&mut document_root.document, &nodes_response);
                }

                // Fetch images
                let image_ref_url = format!("{}{}/images", BASE_FILE_URL, self.document_id);
                let image_refs: figma_schema::ImageFillResponse = deserialize_json_with_path(
                    http_fetch(self.api_key.as_str(), image_ref_url, &self.proxy_config)?.as_str(),
                )?;

                self.branches = get_branches(&document_root);
                self.document_root = document_root;
                self.image_context.update_images(image_refs.meta.images);

                return Ok(UpdateStatus::Updated);
            }
        }

        // Fall back to full document fetch
        let mut document_url = format!(
            "{}{}?plugin_data=shared&geometry=paths&branch_data=true",
            BASE_FILE_URL, self.document_id,
        );
        if !self.version_id.is_empty() {
            document_url.push_str("&version=");
            document_url.push_str(&self.version_id);
        }
        let document_root: figma_schema::FileResponse = deserialize_json_with_path(
            http_fetch(self.api_key.as_str(), document_url, &self.proxy_config)?.as_str(),
        )?;

        // ...and the mapping from imageRef to URL. It returns images from all versions.
        let image_ref_url = format!("{}{}/images", BASE_FILE_URL, self.document_id);
        let image_refs: figma_schema::ImageFillResponse = deserialize_json_with_path(
            http_fetch(self.api_key.as_str(), image_ref_url, &self.proxy_config)?.as_str(),
        )?;

        self.branches = get_branches(&document_root);
        self.document_root = document_root;
        self.image_context.update_images(image_refs.meta.images);

        Ok(UpdateStatus::Updated)
    }

    /// Return the last modified time of this document. This seems to update whenever the doc
    /// is changed (but the version number does not).
    pub fn last_modified(&self) -> &String {
        &self.document_root.last_modified
    }

    /// Return the cached node ID mapping (query name → Figma node ID).
    /// Used by the JNI layer to persist across fetch cycles.
    pub fn cached_node_ids(&self) -> &HashMap<String, String> {
        &self.cached_node_ids
    }

    /// Pre-fetch all remote component metadata concurrently.
    ///
    /// Walks the entire node tree to discover component instances whose keys
    /// refer to remote documents. Collects all unique component keys and
    /// batch-fetches them in parallel using `http_fetch_batch`. The results
    /// are stored in a HashMap that `fetch_component_variants` can consult
    /// before making individual HTTP calls.
    ///
    /// This reduces remote component fetch time from O(N) sequential calls
    /// to O(1) concurrent batches.
    fn prefetch_remote_components(
        &self,
        root: &figma_schema::Node,
        component_hash: &HashMap<String, figma_schema::Component>,
        id_index: &HashMap<String, &figma_schema::Node>,
    ) -> HashMap<String, String> {
        use crate::http_client::{http_fetch_batch, BatchRequest};

        // Phase 1: Discover all unique component keys that need fetching
        let mut keys_to_fetch: HashSet<String> = HashSet::new();
        Self::collect_remote_component_keys(root, component_hash, id_index, &mut keys_to_fetch);

        // Phase 1.5: Skip keys we already have cached or that previously failed
        let already_cached =
            keys_to_fetch.iter().filter(|k| self.prefetched_components.contains_key(*k)).count();
        let already_failed =
            keys_to_fetch.iter().filter(|k| self.failed_component_keys.contains(*k)).count();
        keys_to_fetch.retain(|k| {
            !self.prefetched_components.contains_key(k) && !self.failed_component_keys.contains(k)
        });

        if keys_to_fetch.is_empty() {
            if already_cached > 0 || already_failed > 0 {
                log::info!(
                    "prefetch_remote_components: all keys resolved from cache ({} cached, {} skipped-failed)",
                    already_cached,
                    already_failed
                );
            }
            return HashMap::new();
        }

        log::info!(
            "prefetch_remote_components: fetching {} new keys ({} cached, {} skipped-failed)",
            keys_to_fetch.len(),
            already_cached,
            already_failed
        );

        // Phase 2: Build batch requests
        let requests: Vec<BatchRequest> = keys_to_fetch
            .iter()
            .map(|key| BatchRequest {
                id: key.clone(),
                url: format!("{}{}", BASE_COMPONENT_URL, key),
            })
            .collect();

        // Phase 3: Fetch all concurrently
        let responses = http_fetch_batch(&self.api_key, requests, &self.proxy_config);

        // Phase 4: Collect results into a cache
        let mut cache: HashMap<String, String> = HashMap::new();
        for response in responses {
            match response.result {
                Ok(body) => {
                    cache.insert(response.id, body);
                }
                Err(e) => {
                    log::warn!(
                        "prefetch_remote_components: failed to fetch component key {}: {:?}",
                        response.id,
                        e
                    );
                    // Don't insert into cache — will be added to failed_component_keys by caller
                }
            }
        }

        log::info!(
            "prefetch_remote_components: successfully pre-fetched {}/{} component keys",
            cache.len(),
            keys_to_fetch.len()
        );

        cache
    }

    /// Recursively collect all unique component keys from Instance nodes
    /// that are not present in the local id_index (i.e., remote components).
    fn collect_remote_component_keys(
        node: &figma_schema::Node,
        component_hash: &HashMap<String, figma_schema::Component>,
        id_index: &HashMap<String, &figma_schema::Node>,
        keys: &mut HashSet<String>,
    ) {
        if let figma_schema::NodeData::Instance { frame: _, component_id } = &node.data {
            if !id_index.contains_key(component_id) {
                if let Some(component) = component_hash.get(component_id) {
                    keys.insert(component.key.clone());
                }
            }
        }

        for child in &node.children {
            Self::collect_remote_component_keys(child, component_hash, id_index, keys);
        }
    }

    /// Find all nodes whose data is of type NodeData::Instance, which means could be a
    /// component with variants. Find its parent, and if it is of type NodeData::ComponentSet,
    /// then add all of its children to self.variant_nodes. Also fill out node_doc_hash,
    /// which hashes node ids to the document id they come from.
    fn fetch_component_variants(
        &mut self,
        node: &figma_schema::Node,
        node_doc_hash: &mut HashMap<String, String>,
        variant_nodes: &mut HashMap<String, figma_schema::Node>,
        id_index: &HashMap<String, &figma_schema::Node>,
        component_hash: &HashMap<String, figma_schema::Component>,
        parent_tree: &mut Vec<String>,
        error_list: &mut Vec<String>,
        error_hash: &mut HashSet<String>,
        completed_hash: &mut HashSet<String>,
        hidden_node_policy: HiddenNodePolicy,
    ) -> Result<(), Error> {
        // Ignore hidden nodes
        if !node.visible && hidden_node_policy == HiddenNodePolicy::Skip {
            return Ok(());
        }
        fn add_node_doc_hash(
            node: &figma_schema::Node,
            node_doc_hash: &mut HashMap<String, String>,
            doc_id: &String,
        ) {
            // Add the node id, doc id to the hash and recurse on all children
            node_doc_hash.insert(node.id.clone(), doc_id.clone());
            for child in &node.children {
                add_node_doc_hash(child, node_doc_hash, doc_id);
            }
        }

        if let figma_schema::NodeData::Instance { frame: _, component_id } = &node.data {
            // If the component_id is in id_index, we know it's in this document so we don't
            // need to do anything. If it isn't, it's in a different doc, so proceed to
            // download data for it
            if !id_index.contains_key(component_id) {
                // Find the component info for the component_id
                let component = component_hash.get(component_id);
                if let Some(component) = component {
                    // Fetch the component from the figma api given its key
                    let file_key = component.key.clone();
                    // If we already retrieved this component instance but got an error, don't try again
                    if error_hash.contains(&file_key) {
                        return Ok(());
                    }
                    // If we already completed this node, skip
                    if completed_hash.contains(&file_key) {
                        return Ok(());
                    }
                    let component_url = format!("{}{}", BASE_COMPONENT_URL, file_key);
                    // Check the prefetch cache first (populated by prefetch_remote_components)
                    let component_http_response = if let Some(cached) =
                        self.prefetched_components.get(&file_key)
                    {
                        completed_hash.insert(file_key);
                        cached.clone()
                    } else {
                        // Cache miss — fetch individually (handles components discovered
                        // during recursive traversal of remote nodes)
                        match http_fetch(
                            self.api_key.as_str(),
                            component_url.clone(),
                            &self.proxy_config,
                        ) {
                            Ok(str) => {
                                completed_hash.insert(file_key);
                                str
                            }
                            Err(e) => {
                                let fetch_error = if let Error::NetworkError(reqwest_error) = &e {
                                    if let Some(code) = reqwest_error.status() {
                                        format!("HTTP {} at {}", code, component_url)
                                    } else {
                                        reqwest_error.to_string()
                                    }
                                } else {
                                    e.to_string()
                                };
                                let error_string = format!(
                                    "Fetch component error {}: {} -> {}",
                                    fetch_error,
                                    parent_tree.join(" -> "),
                                    node.name
                                );
                                error_hash.insert(file_key);
                                error_list.push(error_string);
                                return Ok(());
                            }
                        }
                    };

                    // Deserialize into a ComponentKeyResponse
                    let component_key_response: figma_schema::ComponentKeyResponse =
                        serde_json::from_str(component_http_response.as_str())?;
                    // If this variant points to a file_key different than this document, fetch it
                    let maybe_parent_node_id = component_key_response.parent_id();
                    if let Some(parent_node_id) = maybe_parent_node_id {
                        let variant_document_id = component_key_response.meta.file_key;
                        if variant_document_id != self.document_id {
                            if let Err(e) = Self::fetch_variables(
                                &self.api_key,
                                &variant_document_id,
                                &self.proxy_config,
                            ) {
                                error!(
                                    "Failed to fetch variables for remote document {}: {:?}",
                                    variant_document_id, e
                                );
                            }
                            if !self.variables_responses.contains_key(&variant_document_id) {
                                match Self::fetch_variables(
                                    self.api_key.as_str(),
                                    &variant_document_id,
                                    &self.proxy_config,
                                ) {
                                    Ok(vars) => {
                                        self.variables_responses
                                            .insert(variant_document_id.clone(), vars);
                                    }
                                    Err(e) => {
                                        error!(
                                            "Failed to fetch variables for doc {}: {:?}",
                                            variant_document_id, e
                                        );
                                    }
                                }
                            }

                            let nodes_response = if let Some(response) = self
                                .remote_node_responses
                                .get(&(variant_document_id.clone(), parent_node_id.clone()))
                            {
                                response.clone()
                            } else {
                                let nodes_url = format!(
                                    "{}{}/nodes?ids={}",
                                    BASE_FILE_URL, variant_document_id, parent_node_id
                                );
                                let http_str = http_fetch(
                                    self.api_key.as_str(),
                                    nodes_url,
                                    &self.proxy_config,
                                )?;
                                let response: figma_schema::NodesResponse =
                                    serde_json::from_str(http_str.as_str())?;
                                self.remote_node_responses.insert(
                                    (variant_document_id.clone(), parent_node_id.clone()),
                                    response.clone(),
                                );
                                response
                            };

                            // Fetch image refs from the remote document so that
                            // image fills in remote components can be resolved.
                            if !self.fetched_remote_images.contains(&variant_document_id) {
                                let remote_image_url =
                                    format!("{}{}/images", BASE_FILE_URL, variant_document_id);
                                match http_fetch(
                                    self.api_key.as_str(),
                                    remote_image_url,
                                    &self.proxy_config,
                                ) {
                                    Ok(body) => {
                                        match serde_json::from_str::<figma_schema::ImageFillResponse>(
                                            body.as_str(),
                                        ) {
                                            Ok(remote_image_refs) => {
                                                self.image_context.merge_remote_images(
                                                    remote_image_refs.meta.images,
                                                );
                                            }
                                            Err(e) => {
                                                warn!(
                                                    "Failed to parse image refs for remote doc {}: {:?}",
                                                    variant_document_id, e
                                                );
                                            }
                                        }
                                    }
                                    Err(e) => {
                                        warn!(
                                            "Failed to fetch images for remote doc {}: {:?}",
                                            variant_document_id, e
                                        );
                                    }
                                }
                                self.fetched_remote_images.insert(variant_document_id.clone());
                            }

                            // The response is a list of nodes, but we only requested one so this loop
                            // should only go through one time
                            for (node_id, node_response_data) in nodes_response.nodes {
                                if node_id != parent_node_id {
                                    continue; // We only care about parent_node_id
                                }
                                // If the parent is a COMPONENT_SET, then we want to get the parent's children
                                // and add them to our list of nodes
                                if let figma_schema::NodeData::ComponentSet { frame: _ } =
                                    node_response_data.document.data
                                {
                                    for node in node_response_data.document.children {
                                        add_node_doc_hash(
                                            &node,
                                            node_doc_hash,
                                            &variant_document_id,
                                        );
                                        // Recurse on all children
                                        for child in &node.children {
                                            parent_tree.push(node.name.clone());
                                            self.fetch_component_variants(
                                                child,
                                                node_doc_hash,
                                                variant_nodes,
                                                id_index,
                                                &node_response_data.components,
                                                parent_tree,
                                                error_list,
                                                error_hash,
                                                completed_hash,
                                                hidden_node_policy,
                                            )?;
                                            parent_tree.pop();
                                        }
                                        variant_nodes.insert(node.id.clone(), node);
                                    }
                                }
                            }
                        }
                    } else {
                        let error_string = format!(
                            "Fetch component unable to find component parent for: {} -> {}",
                            parent_tree.join(" -> "),
                            node.name
                        );
                        error_list.push(error_string);
                        return Ok(());
                    }
                }
            }
        }
        // Recurse on all children
        for child in &node.children {
            parent_tree.push(node.name.clone());
            self.fetch_component_variants(
                child,
                node_doc_hash,
                variant_nodes,
                id_index,
                component_hash,
                parent_tree,
                error_list,
                error_hash,
                completed_hash,
                hidden_node_policy,
            )?;
            parent_tree.pop();
        }
        Ok(())
    }

    /// Find all of the Component Instance views and see which style and text properties are
    /// overridden in the instance compared to the reference component. If we then render a
    /// different variant of the component (due to an interaction) we can apply these delta
    /// styles to get the correct output.
    fn compute_component_overrides(nodes: &mut HashMap<NodeQuery, View>) {
        // XXX: Would be nice to avoid cloning here. Do we need to? We need to mutate the
        //      instance views in place. And we can't hold a ref and a mutable ref to nodes
        //      at the same time.
        let reference_components = nodes.clone();

        // This function finds all of the Component Instances (views with a populated
        // component_info field) in the given view tree, and looks up which component
        // they are an instance of. If the component is found, then the "action" function
        // is run with a mutable reference to the Component Instance view and a reference
        // to the component.
        //
        // These two pieces of information (the instance and the component) can then be
        // used to figure out which properties of the component have been customized in
        // the instance. Then we can be sure to apply those customized properties to other
        // variants (where Figma just gives us the variant definition, but not filled out
        // instance with overrides applied).
        fn for_each_component_instance(
            reference_components: &HashMap<NodeQuery, View>,
            view: &mut View,
            parent_component_info: Option<&mut ComponentInfo>,
            parent_reference_component: Option<&View>,
            action: &impl Fn(
                MessageField<ViewStyle>,
                MessageField<ViewData>,
                String,
                String,
                &mut ComponentInfo,
                Option<&View>,
                bool,
            ),
        ) {
            match (view.component_info.as_mut(), parent_component_info) {
                (Some(parent_info), Some(_)) => {
                    // This matches a component instance within a component instance.
                    // The style and data overrides are written to a hash map keyed by the view name
                    // in the component info of the instance.
                    action(
                        view.style.clone(),
                        view.data.clone(),
                        view.id.clone(),
                        view.name.clone(),
                        parent_info,
                        parent_reference_component,
                        false,
                    );
                    if let Some(data) = view.data.as_mut() {
                        if let Some(View_data_type::Container { 0: Container { children, .. } }) =
                            data.view_data_type.as_mut()
                        {
                            for child in children {
                                for_each_component_instance(
                                    reference_components,
                                    child,
                                    Some(parent_info),
                                    parent_reference_component,
                                    action,
                                );
                            }
                        }
                    }
                }
                (Some(info), _) => {
                    // This is the root node of a component instance.
                    // Compute its style and data overrides and write to its component info whose
                    // key is the component_set_name.

                    // See if we can find the target component. If not then don't look up
                    // references. Try searching by id, name, and variant
                    let queries = [
                        NodeQuery::NodeId(info.id.clone()),
                        NodeQuery::NodeName(info.name.clone()),
                        NodeQuery::NodeVariant(info.name.clone(), info.component_set_name.clone()),
                    ];

                    let reference_component_option =
                        queries.iter().find_map(|query| reference_components.get(query));

                    if reference_component_option.is_some() {
                        action(
                            view.style.clone(),
                            view.data.clone(),
                            view.id.clone(),
                            info.name.clone(),
                            info,
                            reference_component_option,
                            true,
                        );
                    }

                    if let Some(data) = view.data.as_mut() {
                        if let Some(View_data_type::Container { 0: Container { children, .. } }) =
                            data.view_data_type.as_mut()
                        {
                            for child in children {
                                for_each_component_instance(
                                    reference_components,
                                    child,
                                    Some(info),
                                    reference_component_option,
                                    action,
                                );
                            }
                        }
                    }
                }
                (None, Some(parent_info)) => {
                    // This matches a descendent view of a component instance.
                    // The style and data overrides are written to hash map keyed by the view name
                    // in the component info of the instance.
                    action(
                        view.style.clone(),
                        view.data.clone(),
                        view.id.clone(),
                        view.name.clone(),
                        parent_info,
                        parent_reference_component,
                        false,
                    );
                    if let Some(data) = view.data.as_mut() {
                        if let Some(View_data_type::Container { 0: Container { children, .. } }) =
                            data.view_data_type.as_mut()
                        {
                            for child in children {
                                for_each_component_instance(
                                    reference_components,
                                    child,
                                    Some(parent_info),
                                    parent_reference_component,
                                    action,
                                );
                            }
                        }
                    }
                }

                (None, None) => {
                    // This matches the nodes from the root node of the view tree until it
                    // meets a component instance.
                    if let Some(data) = view.data.as_mut() {
                        if let Some(View_data_type::Container { 0: Container { children, .. } }) =
                            data.view_data_type.as_mut()
                        {
                            for child in children {
                                for_each_component_instance(
                                    reference_components,
                                    child,
                                    None,
                                    None,
                                    action,
                                );
                            }
                        }
                    }
                }
            }
        }

        for view in nodes.values_mut() {
            for_each_component_instance(
                &reference_components,
                view,
                None,
                None,
                &|view_style,
                  view_data,
                  view_id,
                  // overrides_table_key will either be the component_set_name or view_name.
                  // This only works if the view name is identical.
                  overrides_table_key,
                  component_info,
                  component,
                  is_component_root| {
                    if let Some(reference_component) = component {
                        let template_view_option = if is_component_root {
                            Some(reference_component)
                        } else {
                            reference_component.find_view_by_id(&view_id)
                        };
                        if let Some(template_view) = template_view_option {
                            let mut override_view_style = if view_style == template_view.style {
                                MessageField::none()
                            } else if let Some(view_style_ref) = view_style.as_ref() {
                                let diff: Option<ViewStyle> =
                                    Some(template_view.style().difference(view_style_ref));
                                diff.into()
                            } else {
                                error!("ViewStyle is required.");
                                MessageField::none()
                            };

                            let anim_over = view_style.as_ref().and_then(|v| {
                                v.node_style().animation_override.clone().into_option()
                            });

                            let template_anim_over = template_view.style.as_ref().and_then(|s| {
                                s.node_style().animation_override.clone().into_option()
                            });

                            if let Some(anim_over) = anim_over {
                                if Some(&anim_over) != template_anim_over.as_ref() {
                                    if override_view_style.is_none() {
                                        let mut new_style = ViewStyle::new_default();
                                        new_style.node_style_mut().animation_override =
                                            Some(anim_over).into();
                                        override_view_style = Some(new_style).into();
                                    } else {
                                        override_view_style
                                            .mut_or_insert_default()
                                            .node_style_mut()
                                            .animation_override = Some(anim_over).into();
                                    }
                                }
                            }

                            let override_view_data =
                                if let Some(reference_view_data) = template_view.data.as_ref() {
                                    if let Some(data) = view_data.as_ref() {
                                        reference_view_data.difference(data).into()
                                    } else {
                                        MessageField::none()
                                    }
                                } else {
                                    MessageField::none()
                                };

                            if override_view_style.is_some() || override_view_data.is_some() {
                                component_info.overrides_table.insert(
                                    overrides_table_key,
                                    ComponentOverrides {
                                        style: override_view_style,
                                        view_data: override_view_data,
                                        ..Default::default()
                                    },
                                );
                            }
                        }
                    }
                },
            );
        }
    }

    /// Convert the nodes with the given names to a structure that's closer to a toolkit
    /// View. This method doesn't use the toolkit itself.
    pub fn nodes(
        &mut self,
        node_names: &Vec<NodeQuery>,
        ignored_images: &Vec<(NodeQuery, Vec<String>)>,
        error_list: &mut Vec<String>,
        hidden_node_policy: HiddenNodePolicy,
    ) -> Result<HashMap<NodeQuery, View>, Error> {
        // First we gather all of nodes that we're going to convert and find all of the
        // child nodes that can't be rendered. Then we ask Figma to do a batch render on
        // them. Finally we convert and return the set of toolkit nodes.
        let mut name_index = HashMap::new();
        let mut id_index = HashMap::new();
        let mut variant_index = HashMap::new();
        let mut component_set_name_index = HashMap::new();
        // If a component belongs to a component set (there are "variants" in the Figma UI)
        // then we want to know which component set it belongs to so that if the component
        // set has bindings then we can use them for the instance.
        //
        // (For example, we might encounter an instance of a PRNDL component; the component
        // itself has no bindings, but the component set binds to the "vehicle gear" signal
        // and selects a different variant/component based on the gear; in the Figma JSON we
        // will find a component instance, like "Gear=P", and then need to find the component
        // set "PRNDL", and then look at its bindings. If we find the component set and find
        // the bindings then we know we should replace the instance with special node that
        // knows to subscribe to the Gear signal).
        //
        // This map goes from "component id" -> "component set node", so there will be one
        // entry for each component in a component set.
        let mut component_id_index = HashMap::new();
        let root_node = self.document_root.document.clone();
        index_node(
            &root_node,
            None,
            &mut name_index,
            &mut id_index,
            &mut variant_index,
            &mut component_set_name_index,
            &mut component_id_index,
            hidden_node_policy,
        );

        // Fetch component variant nodes
        // doc_nodes is a hash that that hashes document_id -> list of node IDs from that
        // document that we need, so that we can retrieve images from the correct document
        let mut node_doc_hash: HashMap<String, String> = HashMap::new();
        let mut variant_nodes: HashMap<String, figma_schema::Node> = HashMap::new();
        let mut parent_tree: Vec<String> = vec![];
        let mut error_hash: HashSet<String> = HashSet::new();
        let mut completed_hash: HashSet<String> = HashSet::new();
        let components = self.document_root.components.clone();

        // Smart cache: on updates, inject cached remote component data instead of re-fetching.
        // On initial load, do the full prefetch + variant resolution and store results.
        if !self.is_update {
            // INITIAL LOAD: Full remote component resolution
            let new_components =
                self.prefetch_remote_components(&root_node, &components, &id_index);

            let mut all_discovered_keys: HashSet<String> = HashSet::new();
            Self::collect_remote_component_keys(
                &root_node,
                &components,
                &id_index,
                &mut all_discovered_keys,
            );
            for key in &all_discovered_keys {
                if !new_components.contains_key(key)
                    && !self.prefetched_components.contains_key(key)
                {
                    self.failed_component_keys.insert(key.clone());
                }
            }
            for (k, v) in new_components {
                self.prefetched_components.insert(k, v);
            }

            // Full variant resolution
            self.fetch_component_variants(
                &root_node,
                &mut node_doc_hash,
                &mut variant_nodes,
                &id_index,
                &components,
                &mut parent_tree,
                error_list,
                &mut error_hash,
                &mut completed_hash,
                hidden_node_policy,
            )?;
            self.variant_nodes = variant_nodes;

            // Index variant nodes
            for (_, node) in &self.variant_nodes {
                index_node(
                    node,
                    None,
                    &mut name_index,
                    &mut id_index,
                    &mut variant_index,
                    &mut component_set_name_index,
                    &mut component_id_index,
                    hidden_node_policy,
                );
            }

            // Store in smart cache for future updates
            if let Ok(mut cache) = SMART_CACHE.lock() {
                if let Some(entry) = cache.get_mut(&self.document_id) {
                    entry.prefetched_components = self.prefetched_components.clone();
                    entry.variant_nodes = self.variant_nodes.clone();
                    entry.failed_component_keys = self.failed_component_keys.clone();
                    info!(
                        "Smart cache: stored {} remote components and {} variant nodes for {}",
                        entry.prefetched_components.len(),
                        entry.variant_nodes.len(),
                        self.document_id
                    );
                }
            }
        } else {
            // UPDATE: Inject cached remote component data
            let cached = SMART_CACHE.lock().ok().and_then(|c| c.get(&self.document_id).cloned());
            if let Some(entry) = cached {
                info!(
                    "Smart cache HIT: injecting {} remote components and {} variant nodes for {}",
                    entry.prefetched_components.len(),
                    entry.variant_nodes.len(),
                    self.document_id
                );
                self.prefetched_components = entry.prefetched_components;
                self.failed_component_keys = entry.failed_component_keys;
                self.variant_nodes = entry.variant_nodes;

                // Re-index the cached variant nodes
                for (_, node) in &self.variant_nodes {
                    index_node(
                        node,
                        None,
                        &mut name_index,
                        &mut id_index,
                        &mut variant_index,
                        &mut component_set_name_index,
                        &mut component_id_index,
                        hidden_node_policy,
                    );
                }
            } else {
                info!(
                    "Smart cache MISS on update for {} — doing full remote resolution",
                    self.document_id
                );
                // Fallback: full resolution
                let new_components =
                    self.prefetch_remote_components(&root_node, &components, &id_index);
                let mut all_discovered_keys: HashSet<String> = HashSet::new();
                Self::collect_remote_component_keys(
                    &root_node,
                    &components,
                    &id_index,
                    &mut all_discovered_keys,
                );
                for key in &all_discovered_keys {
                    if !new_components.contains_key(key)
                        && !self.prefetched_components.contains_key(key)
                    {
                        self.failed_component_keys.insert(key.clone());
                    }
                }
                for (k, v) in new_components {
                    self.prefetched_components.insert(k, v);
                }
                self.fetch_component_variants(
                    &root_node,
                    &mut node_doc_hash,
                    &mut variant_nodes,
                    &id_index,
                    &components,
                    &mut parent_tree,
                    error_list,
                    &mut error_hash,
                    &mut completed_hash,
                    hidden_node_policy,
                )?;
                self.variant_nodes = variant_nodes;
                for (_, node) in &self.variant_nodes {
                    index_node(
                        node,
                        None,
                        &mut name_index,
                        &mut id_index,
                        &mut variant_index,
                        &mut component_set_name_index,
                        &mut component_id_index,
                        hidden_node_policy,
                    );
                }
            }
        }

        let resolve_query_node = |query: &NodeQuery| -> Option<&figma_schema::Node> {
            match query {
                NodeQuery::NodeId(id) => id_index.get(id).copied(),
                NodeQuery::NodeName(node_name) => name_index.get(node_name).copied(),
                NodeQuery::NodeComponentSet(node_name) => {
                    component_set_name_index.get(node_name).copied()
                }
                NodeQuery::NodeVariant(node_name, parent_name) => {
                    variant_index.get(&(node_name.clone(), parent_name.clone())).copied()
                }
            }
        };

        // Convert explicit node_names into a HashSet, expand any directly-queried component sets
        // or overflow nodes first, and then expand component-set instances by exact component ID
        // so same-named component sets (e.g., two "ProgressBar" sets) don't cross-trigger each
        // other via `NodeQuery::NodeComponentSet(name)` or `component_set_name_index` collisions.
        let mut node_name_hash: HashSet<NodeQuery> = HashSet::from_iter(node_names.iter().cloned());
        let mut component_set_hash: HashMap<String, Vec<String>> = HashMap::new();
        add_variant_node_names(
            &self.document_root.document,
            &mut node_name_hash,
            &mut component_set_hash,
        );

        // Some nodes may have additional plugin data that specifies an overflow node. Look for these
        // and add them to node_name_hash before component-set instance expansion.
        add_overflow_nodes(&self.document_root.document, &mut node_name_hash);

        let mut initial_nodes_by_query: HashMap<NodeQuery, &figma_schema::Node> = node_name_hash
            .into_iter()
            .filter_map(|query| resolve_query_node(&query).map(|node| (query, node)))
            .collect();
        let mut initial_sorted: Vec<(&NodeQuery, &&figma_schema::Node)> =
            initial_nodes_by_query.iter().collect();
        initial_sorted
            .sort_by(|(a, _), (b, _)| node_query_sort_key(a).cmp(&node_query_sort_key(b)));
        let query_nodes: Vec<&figma_schema::Node> =
            initial_sorted.into_iter().map(|(_, node)| *node).collect();
        let mut visited_component_sets: HashSet<String> = HashSet::new();
        for (query, node) in expand_component_set_queries(
            &query_nodes,
            &component_id_index,
            hidden_node_policy,
            &mut visited_component_sets,
        ) {
            initial_nodes_by_query.entry(query).or_insert(node);
        }

        // Build a deterministically ordered list of initial nodes to convert.
        let mut nodes: Vec<(NodeQuery, &figma_schema::Node)> =
            initial_nodes_by_query.into_iter().collect();
        nodes.sort_by(|(a, _), (b, _)| node_query_sort_key(a).cmp(&node_query_sort_key(b)));

        // We use ComponentContext to track all of the nodes that need to be converted to
        // satisfy reactions.
        let mut component_context = ComponentContext::new(&nodes);
        let mut views = HashMap::new();
        let mut converted_views_by_id: HashMap<String, View> = HashMap::new();
        let ignored_image_hash = build_ignored_image_hash(ignored_images, &component_set_hash);
        let mut anim_propagator =
            AnimationOverridePropagator::new(&id_index, &component_id_index, hidden_node_policy);

        loop {
            // Transform each queued query from the Figma schema to the toolkit schema, reusing
            // already-converted views when multiple query types resolve to the same Figma node ID.
            for (query, node) in nodes {
                let view = if let Some(existing_view) = converted_views_by_id.get(&node.id) {
                    existing_view.clone()
                } else {
                    // Set the set of images to ignore for this node
                    let ignored_image_set =
                        ignored_image_hash.get(&NodeQuery::name(node.name.clone()));
                    self.image_context.set_ignored_images(ignored_image_set);

                    let mut converted = create_component_flexbox(
                        node,
                        &self.document_root.components,
                        &self.document_root.component_sets,
                        &mut component_context,
                        &mut self.image_context,
                        hidden_node_policy,
                        &mut self.key_to_global_id_map,
                    )?;
                    anim_propagator.propagate(&mut converted);
                    converted_views_by_id.insert(node.id.clone(), converted.clone());
                    converted
                };
                views.insert(query, view);
            }

            // Drain newly referenced out-of-tree nodes; exit once the worklist is exhausted.
            nodes = component_context.referenced_list(&id_index);
            if nodes.is_empty() {
                break;
            }

            let mut queued_queries: HashSet<NodeQuery> =
                nodes.iter().map(|(query, _)| query.clone()).collect();
            let ref_nodes: Vec<&figma_schema::Node> = nodes.iter().map(|(_, node)| *node).collect();
            for (query, node) in expand_component_set_queries(
                &ref_nodes,
                &component_id_index,
                hidden_node_policy,
                &mut visited_component_sets,
            ) {
                if views.contains_key(&query) || !queued_queries.insert(query.clone()) {
                    continue;
                }
                component_context.mark_converted(&node.id);
                nodes.push((query, node));
            }
        }

        // Populate the override data for components.
        Self::compute_component_overrides(&mut views);

        // Update our mapping from instance to component set.
        self.component_sets = component_id_index
            .iter()
            .map(|(component_id, component_set_node)| {
                (component_id.clone(), component_set_node.id.clone())
            })
            .collect();

        // If there are nodes that were referenced that we weren't able to convert then we can report them
        // here. Ideally we'd log this so that a customer could see it and figure out what's going on and
        // why an action isn't working.
        let missing_nodes = component_context.missing_nodes();
        if !missing_nodes.is_empty() {
            warn!("Figma: Unable to find nodes referenced by interactions: {:?}", missing_nodes);
            warn!("       These interactions won't work.");
        }
        Ok(views)
    }

    // Parse through all the variables collected and store them into hash tables for easy access
    pub fn build_variable_map(&self) -> VariableMap {
        let mut collections_by_id: HashMap<String, Collection> = HashMap::new();
        let mut collection_ids_by_name: HashMap<String, String> = HashMap::new();
        let mut variables_by_id: HashMap<String, Variable> = HashMap::new();
        let mut variable_name_id_maps_by_cid: HashMap<String, NameIdMap> = HashMap::new();

        for variables_response in self.variables_responses.values() {
            for (_, c) in variables_response.meta.variable_collections.iter() {
                let mut mode_name_hash: HashMap<String, String> = HashMap::new();
                let mut mode_id_hash: HashMap<String, Mode> = HashMap::new();
                for m in &c.modes {
                    let mode =
                        Mode { id: m.mode_id.clone(), name: m.name.clone(), ..Default::default() };
                    mode_name_hash.insert(mode.name.clone(), mode.id.clone());
                    mode_id_hash.insert(mode.id.clone(), mode);
                }
                let collection = Collection {
                    id: c.id.clone(),
                    name: c.name.clone(),
                    default_mode_id: c.default_mode_id.clone(),
                    mode_name_hash,
                    mode_id_hash,
                    ..Default::default()
                };
                collections_by_id.insert(collection.id.clone(), collection);
                collection_ids_by_name.insert(c.name.clone(), c.id.clone());
            }

            for (id, v) in variables_response.meta.variables.iter() {
                let var = create_variable(v);

                let maybe_name_map =
                    variable_name_id_maps_by_cid.get_mut(&var.variable_collection_id);
                if let Some(name_map) = maybe_name_map {
                    name_map.m.insert(var.name.clone(), id.clone());
                } else {
                    let mut name_to_id = HashMap::new();
                    name_to_id.insert(var.name.clone(), id.clone());
                    variable_name_id_maps_by_cid.insert(
                        var.variable_collection_id.clone(),
                        NameIdMap { m: name_to_id, ..Default::default() },
                    );
                }

                // Register under the local id, var.id, and global id so both local and remote nodes resolve.
                // Fixes "Failed to get variable value for VariableID:..." warnings and wrong fallback colors
                // when remote components alias the variable under library-hashed IDs.
                if let Some(global_id) = self.key_to_global_id_map.get(&var.key) {
                    if global_id != id && global_id != &var.id {
                        variables_by_id.insert(global_id.clone(), var.clone());
                    }
                }
                if &var.id != id {
                    variables_by_id.insert(var.id.clone(), var.clone());
                }
                variables_by_id.insert(id.clone(), var);
            }
        }

        let var_map = VariableMap {
            collections_by_id,
            collection_ids_by_name,
            variables_by_id,
            variable_name_id_maps_by_cid,
            ..Default::default()
        };

        // Post-process: detect unresolved variable aliases.
        // An alias is unresolved when its target variable ID is not present in the
        // merged variable map. This typically happens when the alias target lives
        // in a library doc that wasn't fetched through component dependencies.
        let mut unresolved_count = 0;
        for (var_id, var) in &var_map.variables_by_id {
            if let Some(ref values_by_mode) = var.values_by_mode.as_ref() {
                for (mode_id, value) in &values_by_mode.values_by_mode {
                    if let Some(dc_bundle::variable::variable_value::Value::Alias(alias_id)) =
                        &value.Value
                    {
                        if !var_map.variables_by_id.contains_key(alias_id) {
                            warn!(
                                "Unresolved variable alias: variable '{}' (id={}) in mode '{}' \
                                 aliases '{}' which is not in the variable map. \
                                 The aliased variable's library may not have been fetched.",
                                var.name, var_id, mode_id, alias_id
                            );
                            unresolved_count += 1;
                        }
                    }
                }
            }
        }
        if unresolved_count > 0 {
            warn!(
                "build_variable_map: {} unresolved variable alias(es) detected. \
                 These variables will fall back to default values at runtime.",
                unresolved_count
            );
        }

        var_map
    }

    /// Return the EncodedImageMap containing the mapping from ImageKey references in Nodes returned by this document
    /// to encoded image bytes. EncodedImageMap can be serialized and deserialized, and transformed into an ImageMap.
    pub fn encoded_image_map(&self) -> EncodedImageMap {
        self.image_context.encoded_image_map()
    }

    /// Return the ImageContextSession so that subsequent requests can avoid fetching the same images. We also return
    /// the list of all images referenced by this document, so that a client knows which images to copy from the old
    /// document (if it requested an incremental update).
    pub fn image_session(&self) -> ImageContextSession {
        self.image_context.as_session()
    }

    /// Return the mapping from Component node ID to Component Set node ID.
    pub fn component_sets(&self) -> &HashMap<String, String> {
        &self.component_sets
    }

    /// Get all the Figma documents in the given project ID
    pub fn get_projects(&self, project_id: &str) -> Result<Vec<FigmaDocInfo>, Error> {
        let url = format!("{}{}/files", BASE_PROJECT_URL, project_id);
        let project_files: figma_schema::ProjectFilesResponse = serde_json::from_str(
            http_fetch(self.api_key.as_str(), url, &self.proxy_config)?.as_str(),
        )?;

        let mut figma_docs: Vec<FigmaDocInfo> = vec![];
        for file_hash in &project_files.files {
            if let Some(doc_id) = file_hash.get("key") {
                if let Some(name) = file_hash.get("name") {
                    let figma_doc = FigmaDocInfo {
                        name: name.clone(),
                        id: doc_id.clone(),
                        ..Default::default()
                    };
                    // Getting project files return head version of the files.
                    figma_docs.push(figma_doc);
                }
            }
        }

        Ok(figma_docs)
    }

    /// Get the name of the document
    pub fn get_name(&self) -> String {
        self.document_root.name.clone()
    }

    /// Get the document ID of the document
    pub fn get_document_id(&self) -> String {
        self.document_id.clone()
    }

    /// Get the version string in the document's file response
    pub fn get_version(&self) -> String {
        self.document_root.version.clone()
    }

    pub fn cache(&self) -> HashMap<String, String> {
        self.image_context.cache()
    }
}

fn index_node<'a>(
    node: &'a figma_schema::Node,
    parent_node: Option<&'a figma_schema::Node>,
    name_index: &mut HashMap<String, &'a figma_schema::Node>,
    id_index: &mut HashMap<String, &'a figma_schema::Node>,
    variant_index: &mut HashMap<(String, String), &'a figma_schema::Node>,
    component_set_name_index: &mut HashMap<String, &'a figma_schema::Node>,
    component_id_index: &mut HashMap<String, &'a figma_schema::Node>,
    hidden_node_policy: HiddenNodePolicy,
) {
    // Ignore hidden nodes
    if !node.visible && hidden_node_policy == HiddenNodePolicy::Skip {
        return;
    }

    // If we have a parent that is a component set, add to the variant index
    let mut is_variant = false;
    if let Some(parent) = parent_node {
        if let figma_schema::NodeData::ComponentSet { .. } = parent.data {
            is_variant = true;
            variant_index.insert((node.name.clone(), parent.name.clone()), node);
        }
    }

    if !is_variant {
        // If there's already a node with the same name, then only replace it if
        // we're a component.
        if name_index.get(&node.name).is_some() {
            if let figma_schema::NodeData::Component { .. } = node.data {
                name_index.insert(node.name.clone(), node);
            }
        } else {
            name_index.insert(node.name.clone(), node);
        }
    }
    id_index.insert(node.id.clone(), node);
    for child in &node.children {
        index_node(
            child,
            Some(node),
            name_index,
            id_index,
            variant_index,
            component_set_name_index,
            component_id_index,
            hidden_node_policy,
        );
    }
    if let figma_schema::NodeData::ComponentSet { .. } = node.data {
        component_set_name_index.insert(node.name.clone(), node);
        for child in &node.children {
            if matches!(child.data, figma_schema::NodeData::Component { .. })
                && (hidden_node_policy == HiddenNodePolicy::Keep || child.visible)
            {
                component_id_index.insert(child.id.clone(), node);
            }
        }
    }
}

// For each node or visible descendant component instance, emit the exact component set and
// variant nodes associated with that instance (resolved by component ID so same-named
// ComponentSets do not collide by name, while honoring `HiddenNodePolicy`).
fn find_component_sets<'a>(
    node: &'a figma_schema::Node,
    component_set_index: &HashMap<String, &'a figma_schema::Node>,
    hidden_node_policy: HiddenNodePolicy,
    expanded_nodes: &mut HashMap<NodeQuery, &'a figma_schema::Node>,
    visited_sets: &mut HashSet<String>,
) {
    if hidden_node_policy == HiddenNodePolicy::Skip && !node.visible {
        return;
    }
    if let figma_schema::NodeData::Instance { frame: _, component_id } = &node.data {
        if let Some(&cs) = component_set_index.get(component_id) {
            if hidden_node_policy == HiddenNodePolicy::Keep || cs.visible {
                expanded_nodes.entry(NodeQuery::NodeComponentSet(cs.name.clone())).or_insert(cs);

                // Recurse on the children of each component set at most once so cyclic
                // component-set references terminate cleanly.
                if visited_sets.insert(cs.id.clone()) {
                    for child in &cs.children {
                        if !matches!(child.data, figma_schema::NodeData::Component { .. })
                            || (hidden_node_policy == HiddenNodePolicy::Skip && !child.visible)
                        {
                            continue;
                        }
                        expanded_nodes
                            .entry(NodeQuery::NodeVariant(child.name.clone(), cs.name.clone()))
                            .or_insert(child);
                        find_component_sets(
                            child,
                            component_set_index,
                            hidden_node_policy,
                            expanded_nodes,
                            visited_sets,
                        );
                    }
                }
            }
        }
    }

    for child in &node.children {
        find_component_sets(
            child,
            component_set_index,
            hidden_node_policy,
            expanded_nodes,
            visited_sets,
        );
    }
}

// For each node that is a component set, add all of its children (which are variants)
// into the node name hash so that we retrieve those nodes. Also add them to the
// component_set_hash, which hashes a component set node name to all of its children.
fn add_variant_node_names(
    node: &figma_schema::Node,
    node_name_hash: &mut HashSet<NodeQuery>,
    component_set_hash: &mut HashMap<String, Vec<String>>,
) {
    if let figma_schema::NodeData::ComponentSet { .. } = &node.data {
        // Add the component set's children to node_name_has if either the component set itself
        // is in the node_name_hash, or one of the child variants is in node_name_hash
        let mut add_children = node_name_hash.contains(&NodeQuery::NodeName(node.name.clone()))
            || node_name_hash.contains(&NodeQuery::NodeComponentSet(node.name.clone()));
        if !add_children {
            'outer: for child in &node.children {
                let child_name_parts = child.name.split(',');
                for variant_name in child_name_parts {
                    let variant_name = variant_name.to_string();
                    let variant_parts: Vec<&str> = variant_name.split('=').collect();
                    if variant_parts.len() == 2 {
                        // Format is property_name=value, so check if the property_name is in node_name_hash
                        if node_name_hash
                            .contains(&NodeQuery::NodeName(variant_parts[0].to_string()))
                        {
                            add_children = true;
                            break 'outer;
                        }
                    }
                }
            }
        }
        if add_children {
            for child in &node.children {
                let child_node_query =
                    NodeQuery::NodeVariant(child.name.clone(), node.name.clone());
                if !node_name_hash.contains(&child_node_query) {
                    node_name_hash.insert(child_node_query);
                }
            }
        }
        let variants = component_set_hash.entry(node.name.clone()).or_default();
        for child in &node.children {
            if !variants.iter().any(|existing| existing == &child.name) {
                variants.push(child.name.clone());
            }
        }
    }
    // Recurse on all children
    for child in &node.children {
        add_variant_node_names(child, node_name_hash, component_set_hash);
    }
}

// Search the node hierarchy for nodes that have plugin data that specifies an overflow
// node. Add these nodes to node_name_hash so that we fetch them.
fn add_overflow_nodes(node: &figma_schema::Node, node_name_hash: &mut HashSet<NodeQuery>) {
    if node.shared_plugin_data.contains_key("designcompose") {
        let plugin_data = node.shared_plugin_data.get("designcompose");
        if let Some(vsw_data) = plugin_data {
            if let Some(plugin_layout) = vsw_data.get("vsw-extended-auto-layout") {
                let extended_auto_layout: Option<ExtendedAutoLayout> =
                    serde_json::from_str(plugin_layout.as_str()).ok();
                if let Some(extended_auto_layout) = extended_auto_layout {
                    if extended_auto_layout.limit_content {
                        let lcd = extended_auto_layout.limit_content_data;
                        node_name_hash.insert(NodeQuery::NodeId(lcd.overflow_node_id));
                    }
                }
            }
        }
    }

    // Recurse on all children
    for child in &node.children {
        add_overflow_nodes(child, node_name_hash);
    }
}

fn node_query_sort_key(query: &NodeQuery) -> (u8, &str, &str) {
    match query {
        NodeQuery::NodeId(id) => (0, id.as_str(), ""),
        NodeQuery::NodeName(name) => (1, name.as_str(), ""),
        NodeQuery::NodeComponentSet(name) => (2, name.as_str(), ""),
        NodeQuery::NodeVariant(name, parent) => (3, name.as_str(), parent.as_str()),
    }
}

fn build_ignored_image_hash(
    ignored_images: &[(NodeQuery, Vec<String>)],
    component_set_hash: &HashMap<String, Vec<String>>,
) -> HashMap<NodeQuery, HashSet<String>> {
    let mut ignored_image_hash: HashMap<NodeQuery, HashSet<String>> = HashMap::new();
    for (node_name, node_images) in ignored_images {
        let img_set: HashSet<String> = node_images.iter().cloned().collect();
        ignored_image_hash.insert(node_name.clone(), img_set);

        // If node_name is a component set, all its variant children use the same set of ignored images
        if let NodeQuery::NodeName(name) = node_name {
            if let Some(variant_list) = component_set_hash.get(name) {
                for variant_name in variant_list {
                    let variant_img_set: HashSet<String> = node_images.iter().cloned().collect();
                    ignored_image_hash.insert(NodeQuery::name(variant_name), variant_img_set);
                }
            }
        }
    }
    ignored_image_hash
}

fn expand_component_set_queries<'a>(
    seed_nodes: &[&'a figma_schema::Node],
    component_set_index: &HashMap<String, &'a figma_schema::Node>,
    hidden_node_policy: HiddenNodePolicy,
    visited_sets: &mut HashSet<String>,
) -> Vec<(NodeQuery, &'a figma_schema::Node)> {
    let mut expanded: HashMap<NodeQuery, &'a figma_schema::Node> = HashMap::new();
    for &seed in seed_nodes {
        find_component_sets(
            seed,
            component_set_index,
            hidden_node_policy,
            &mut expanded,
            visited_sets,
        );
    }
    let mut sorted: Vec<(NodeQuery, &'a figma_schema::Node)> = expanded.into_iter().collect();
    sorted.sort_by(|(a, _), (b, _)| node_query_sort_key(a).cmp(&node_query_sort_key(b)));
    sorted
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::animation_override::test_support::*;

    #[test]
    fn test_parse_document_id_formats() {
        assert_eq!(parse_document_id("ABC"), "ABC");
        assert_eq!(parse_document_id("https://www.figma.com/file/XYZ"), "XYZ");
        assert_eq!(parse_document_id("https://www.figma.com/file/XYZ/node-id"), "XYZ");
        assert_eq!(parse_document_id("https://www.figma.com/design/123"), "123");
        assert_eq!(parse_document_id("https://www.figma.com/design/123/node-id"), "123");
    }

    #[derive(serde::Deserialize, Debug)]
    struct DummyStruct {
        field: String,
    }

    #[derive(serde::Deserialize, Debug)]
    struct DummyParent {
        child: DummyStruct,
    }

    #[test]
    fn test_deserialize_json_with_path_errors() {
        let bad_json = r#"{"child": {"field": 123}}"#;
        let result: Result<DummyParent, Error> = deserialize_json_with_path(bad_json);
        assert!(result.is_err());
        let err_msg = result.unwrap_err().to_string();
        assert!(err_msg.contains("Json Path Serialization Error"));
        assert!(err_msg.contains("child.field"));
    }

    #[test]
    fn test_deserialize_json_with_path_success() {
        let good_json = r#"{"child": {"field": "value"}}"#;
        let result: Result<DummyParent, Error> = deserialize_json_with_path(good_json);
        assert!(result.is_ok());
        assert_eq!(result.unwrap().child.field, "value");
    }

    #[test]
    fn test_compute_component_overrides_coverage() {
        use dc_bundle::positioning::ScrollInfo;
        use dc_bundle::view::view::RenderMethod;
        use dc_bundle::view_shape::ViewShape;
        use std::collections::HashMap;

        let mut views = HashMap::new();

        // 1. Reference Component
        let ref_id = "ref_id".to_string();
        let ref_view = View::new_rect(
            &ref_id,
            &"RefComponent".to_string(),
            ViewShape::default(),
            ViewStyle::new_default(),
            None,
            None,
            ScrollInfo::new_default(),
            None,
            None,
            RenderMethod::RENDER_METHOD_NONE,
            HashMap::new(),
        );
        views.insert(NodeQuery::NodeId(ref_id.clone()), ref_view.clone());
        views.insert(NodeQuery::NodeVariant("RefComponent".to_string(), "".to_string()), ref_view);

        // 2. Instance 1 (Root of instance)
        let mut comp_info1 = ComponentInfo::default();
        comp_info1.id = ref_id.clone();
        comp_info1.name = "RefComponent".to_string();

        let inst1_id = "inst1_id".to_string();
        let mut inst1 = View::new_rect(
            &inst1_id,
            &"Instance1".to_string(),
            ViewShape::default(),
            ViewStyle::new_default(),
            Some(comp_info1),
            None,
            ScrollInfo::new_default(),
            None,
            None,
            RenderMethod::RENDER_METHOD_NONE,
            HashMap::new(),
        );

        // 3. Child 1 (Descendant of instance 1)
        let child1_id = "child1_id".to_string();
        let child1 = View::new_rect(
            &child1_id,
            &"Child1".to_string(),
            ViewShape::default(),
            ViewStyle::new_default(),
            None,
            None,
            ScrollInfo::new_default(),
            None,
            None,
            RenderMethod::RENDER_METHOD_NONE,
            HashMap::new(),
        );

        // 4. Instance 2 (Nested Instance)
        let mut comp_info2 = ComponentInfo::default();
        comp_info2.id = ref_id.clone();
        comp_info2.name = "RefComponent".to_string();

        let inst2_id = "inst2_id".to_string();
        let inst2 = View::new_rect(
            &inst2_id,
            &"Instance2".to_string(),
            ViewShape::default(),
            ViewStyle::new_default(),
            Some(comp_info2),
            None,
            ScrollInfo::new_default(),
            None,
            None,
            RenderMethod::RENDER_METHOD_NONE,
            HashMap::new(),
        );

        // Assemble the tree: inst1 -> [child1, inst2]
        inst1.add_child(child1);
        inst1.add_child(inst2);

        views.insert(NodeQuery::NodeId(inst1_id), inst1);

        // Call the method
        Document::compute_component_overrides(&mut views);

        // Assert that we completed execution without panic
        assert!(true);
    }

    #[test]
    fn test_build_variable_map_retains_local_and_global_alias_ids() {
        let json = serde_json::json!({
            "error": false,
            "status": 200,
            "meta": {
                "variableCollections": {
                    "VariableCollectionId:1:1": {
                        "id": "VariableCollectionId:1:1",
                        "name": "Collection 1",
                        "modes": [
                            {"modeId": "1:0", "name": "Mode 1"}
                        ],
                        "defaultModeId": "1:0",
                        "remote": false,
                        "hiddenFromPublishing": false,
                        "variableIds": ["VariableID:12286:6095"],
                        "key": "col_key"
                    }
                },
                "variables": {
                    "VariableID:12286:6095": {
                        "id": "VariableID:12286:6095",
                        "name": "colors/primary",
                        "key": "var_key_123",
                        "variableCollectionId": "VariableCollectionId:1:1",
                        "resolvedType": "COLOR",
                        "valuesByMode": {},
                        "remote": false
                    }
                }
            }
        });
        let variables_response: figma_schema::VariablesResponse =
            serde_json::from_value(json).expect("Failed to deserialize mock variables response");

        let mut key_to_global_id_map = HashMap::new();
        let alias_id = "VariableID:remote_hash/999".to_string();
        key_to_global_id_map.insert("var_key_123".to_string(), alias_id.clone());

        let mut variables_responses = HashMap::new();
        variables_responses.insert("doc_test".to_string(), variables_response);

        let mut doc = make_test_document(test_file_response("Test Doc", vec![], &[], &[]));
        doc.variables_responses = variables_responses;
        doc.key_to_global_id_map = key_to_global_id_map;

        let variable_map = doc.build_variable_map();

        // Assert variables_by_id contains BOTH the local id and the alias id
        assert!(
            variable_map.variables_by_id.contains_key("VariableID:12286:6095"),
            "variables_by_id must contain local id"
        );
        assert!(
            variable_map.variables_by_id.contains_key(&alias_id),
            "variables_by_id must contain global alias id"
        );

        // Assert name map points to the local id
        let name_map = variable_map
            .variable_name_id_maps_by_cid
            .get("VariableCollectionId:1:1")
            .expect("Collection name map must exist");
        assert_eq!(
            name_map.m.get("colors/primary"),
            Some(&"VariableID:12286:6095".to_string()),
            "Name map must point to the local id"
        );
    }

    fn make_test_document(document_root: figma_schema::FileResponse) -> Document {
        Document {
            api_key: String::new(),
            document_id: "doc_test".to_string(),
            version_id: String::new(),
            proxy_config: ProxyConfig::None,
            document_root,
            variables_responses: HashMap::new(),
            image_context: ImageContext::new(HashMap::new(), HashMap::new(), &ProxyConfig::None),
            variant_nodes: HashMap::new(),
            component_sets: HashMap::new(),
            branches: vec![],
            key_to_global_id_map: HashMap::new(),
            remote_node_responses: HashMap::new(),
            fetched_remote_images: HashSet::new(),
            prefetched_components: HashMap::new(),
            node_tracker: crate::node_tracker::NodeTracker::new(),
            last_view_hashes: HashMap::new(),
            cached_node_ids: HashMap::new(),
            failed_component_keys: HashSet::new(),
            is_update: false,
        }
    }

    #[test]
    fn test_document_nodes_propagates_matrix_to_component_set_variants_and_instance() {
        use crate::reaction_schema::{BezierJson, EasingJson as ReactionEasingJson, TriggerJson};

        let custom_bezier = ReactionEasingJson::CustomCubicBezier {
            bezier: BezierJson { x1: 0.2, y1: 0.0, x2: 0.0, y2: 1.0 },
        };
        let page_children = vec![
            test_frame("10:0", "#Main", vec![test_instance("10:1", "#Pill", "20:1", vec![])]),
            test_component_set(
                "20:0",
                "PillSet",
                vec![
                    with_interactions(
                        test_component("20:1", "State=Idle", vec![]),
                        vec![smart_animate_change_to_with_easing(
                            TriggerJson::OnHover,
                            "20:2",
                            custom_bezier,
                            0.2,
                        )],
                    ),
                    test_component("20:2", "State=Active", vec![]),
                ],
            ),
        ];

        let mut doc = make_test_document(test_file_response(
            "ComponentSet Propagation Doc",
            page_children,
            &[],
            &[],
        ));
        let mut errors = Vec::new();
        let views = doc
            .nodes(&vec![NodeQuery::name("#Main")], &vec![], &mut errors, HiddenNodePolicy::Skip)
            .unwrap();
        assert!(errors.is_empty());

        let cs_view = views.get(&NodeQuery::NodeComponentSet("PillSet".to_string())).unwrap();
        let idle_view = views.get(&NodeQuery::variant("State=Idle", "PillSet")).unwrap();
        let active_view = views.get(&NodeQuery::variant("State=Active", "PillSet")).unwrap();
        let main_view = views.get(&NodeQuery::name("#Main")).unwrap();
        let pill_inst = main_view.find_view_by_id("10:1").expect("#Pill instance must exist");

        for view in [cs_view, idle_view, active_view, pill_inst] {
            let Some(dc_bundle::animationspec::animation_override::Animation_override::Matrix(
                matrix,
            )) = view_animation_override(view)
            else {
                panic!("Expected Matrix on {}", view.name);
            };
            assert_eq!(matrix.transitions.len(), 2);
            let fwd = matrix
                .transitions
                .iter()
                .find(|t| t.source_variant == "State=Idle" && t.target_variant == "State=Active")
                .expect("Forward State=Idle -> State=Active transition must exist");
            let Some(dc_bundle::animationspec::animations::Type::Smooth(smooth)) =
                fwd.spec.as_ref().unwrap().animation.as_ref().unwrap().type_.as_ref()
            else {
                panic!("Expected Smooth animation");
            };
            assert_eq!(smooth.duration.as_ref().unwrap().nanos, 200_000_000);
            match smooth.easing.as_ref().unwrap().type_.as_ref().unwrap() {
                dc_bundle::animationspec::easing::Type::Bezier(b) => {
                    assert!((b.p0 - 0.2).abs() < 1e-6);
                    assert!((b.p3 - 1.0).abs() < 1e-6);
                }
                other => panic!("Expected Bezier easing, got {:?}", other),
            }
            assert!(matrix
                .transitions
                .iter()
                .any(|t| t.source_variant == "State=Active" && t.target_variant == "State=Idle"));
        }

        assert!(
            pill_inst.component_info.as_ref().unwrap().overrides_table.is_empty(),
            "Unmodified #Pill instance must not accrue spurious overrides_table entries"
        );
    }

    #[test]
    fn test_document_nodes_nested_instances_isolated_without_spurious_overrides() {
        use crate::reaction_schema::TriggerJson;

        let page_children = vec![
            test_frame(
                "10:0",
                "#Main",
                vec![test_instance(
                    "10:1",
                    "#OuterCard",
                    "20:1",
                    vec![test_instance("I10:1;20:11", "#InnerPill", "30:1", vec![])],
                )],
            ),
            test_component_set(
                "20:0",
                "CardSet",
                vec![
                    with_interactions(
                        test_component(
                            "20:1",
                            "Card=One",
                            vec![test_instance("20:11", "#InnerPill", "30:1", vec![])],
                        ),
                        vec![smart_animate_change_to(TriggerJson::OnClick, "20:2", 0.4)],
                    ),
                    test_component("20:2", "Card=Two", vec![]),
                ],
            ),
            test_component_set(
                "30:0",
                "PillSet",
                vec![
                    with_interactions(
                        test_component("30:1", "Pill=Idle", vec![]),
                        vec![smart_animate_change_to(TriggerJson::OnHover, "30:2", 0.15)],
                    ),
                    test_component("30:2", "Pill=Active", vec![]),
                ],
            ),
        ];

        let mut doc =
            make_test_document(test_file_response("TA2 Nested Doc", page_children, &[], &[]));
        let mut errors = Vec::new();
        let views = doc
            .nodes(&vec![NodeQuery::name("#Main")], &vec![], &mut errors, HiddenNodePolicy::Skip)
            .unwrap();
        assert!(errors.is_empty());

        let main_view = views.get(&NodeQuery::name("#Main")).unwrap();
        let outer_card = main_view.find_view_by_id("10:1").expect("#OuterCard must exist");
        let inner_pill = outer_card.find_view_by_id("I10:1;20:11").expect("#InnerPill must exist");

        let Some(dc_bundle::animationspec::animation_override::Animation_override::Matrix(
            outer_matrix,
        )) = view_animation_override(outer_card)
        else {
            panic!("Expected CardSet Matrix on #OuterCard");
        };
        assert_eq!(outer_matrix.transitions.len(), 1);
        assert_eq!(outer_matrix.transitions[0].source_variant, "Card=One");
        assert_eq!(outer_matrix.transitions[0].target_variant, "Card=Two");

        let Some(dc_bundle::animationspec::animation_override::Animation_override::Matrix(
            inner_matrix,
        )) = view_animation_override(inner_pill)
        else {
            panic!("Expected PillSet Matrix on nested #InnerPill");
        };
        assert_eq!(inner_matrix.transitions.len(), 2);
        assert!(inner_matrix.transitions.iter().all(|t| t.source_variant.starts_with("Pill=")));

        assert!(
            outer_card.component_info.as_ref().unwrap().overrides_table.is_empty(),
            "Nested instances matching their templates must not bloat overrides_table"
        );
        assert!(inner_pill.component_info.as_ref().unwrap().overrides_table.is_empty());
    }

    #[test]
    fn test_document_nodes_four_tier_squoosh_precedence_and_overrides_table() {
        use crate::reaction_schema::TriggerJson;
        use dc_bundle::animationspec::animation_override::Animation_override;

        // Config (b): ComponentSet has DisableAnimations on SetDisabled, Variant V=A of
        // SetCustomVariant has custom 900ms Matrix, unmodified Instances inherit both cleanly
        // without overrides_table clobbering them.
        // Config (c): Instance #InstanceOverride of SetRestOnly has explicit DisableAnimations
        // overriding SetRestOnly's REST Matrix.
        let page_children = vec![
            test_frame(
                "10:0",
                "#Main",
                vec![
                    test_instance("10:1", "#DisabledInst", "20:1", vec![]),
                    test_frame(
                        "10:20",
                        "Row2",
                        vec![test_instance("10:2", "#Custom900Inst", "30:1", vec![])],
                    ),
                    test_frame(
                        "10:30",
                        "Row3",
                        vec![with_squoosh(
                            test_instance("10:3", "#InstanceOverride", "40:1", vec![]),
                            SQUOOSH_DISABLE,
                        )],
                    ),
                ],
            ),
            with_squoosh(
                test_component_set(
                    "20:0",
                    "SetDisabled",
                    vec![
                        with_interactions(
                            test_component("20:1", "S=A", vec![]),
                            vec![smart_animate_change_to(TriggerJson::OnClick, "20:2", 0.2)],
                        ),
                        test_component("20:2", "S=B", vec![]),
                    ],
                ),
                SQUOOSH_DISABLE,
            ),
            with_squoosh(
                test_component_set(
                    "30:0",
                    "SetCustomVariant",
                    vec![
                        with_interactions(
                            with_squoosh(
                                test_component("30:1", "V=A", vec![]),
                                &squoosh_matrix_json(None, &[("V=A", "V=B", 900)]),
                            ),
                            vec![smart_animate_change_to(TriggerJson::OnHover, "30:2", 0.2)],
                        ),
                        test_component("30:2", "V=B", vec![]),
                    ],
                ),
                SQUOOSH_DISABLE,
            ),
            test_component_set(
                "40:0",
                "SetRestOnly",
                vec![
                    with_interactions(
                        test_component("40:1", "R=A", vec![]),
                        vec![smart_animate_change_to(TriggerJson::OnClick, "40:2", 0.2)],
                    ),
                    test_component("40:2", "R=B", vec![]),
                ],
            ),
        ];

        let mut doc =
            make_test_document(test_file_response("IT2 Precedence Doc", page_children, &[], &[]));
        let mut errors = Vec::new();
        let views = doc
            .nodes(&vec![NodeQuery::name("#Main")], &vec![], &mut errors, HiddenNodePolicy::Skip)
            .unwrap();
        assert!(errors.is_empty());

        let main_view = views.get(&NodeQuery::name("#Main")).unwrap();

        // (b1) ComponentSet DisableAnimations propagates to both NodeVariant and unmodified Instance.
        let disabled_variant = views.get(&NodeQuery::variant("S=A", "SetDisabled")).unwrap();
        let disabled_inst = main_view.find_view_by_id("10:1").unwrap();
        for view in [disabled_variant, disabled_inst] {
            assert!(matches!(
                view_animation_override(view),
                Some(Animation_override::DisableAnimations(_))
            ));
        }
        assert!(disabled_inst.component_info.as_ref().unwrap().overrides_table.is_empty());

        // (b2) Variant Component 900ms Matrix wins over 200ms REST for V=A->V=B, adds reversible
        // V=B->V=A at 200ms, and matches cleanly with unmodified Instance (empty overrides_table).
        let custom_variant = views.get(&NodeQuery::variant("V=A", "SetCustomVariant")).unwrap();
        let custom_inst = main_view.find_view_by_id("10:2").unwrap();
        for view in [custom_variant, custom_inst] {
            let Some(Animation_override::Matrix(m)) = view_animation_override(view) else {
                panic!("Expected Matrix on {}", view.name);
            };
            assert_eq!(m.transitions.len(), 2);
            assert_eq!(matrix_pair_nanos(m, "V=A", "V=B"), (0, 900_000_000));
        }
        assert!(custom_inst.component_info.as_ref().unwrap().overrides_table.is_empty());
        // Variant V=B without its own squoosh still inherits SetCustomVariant's DisableAnimations.
        let vb_variant = views.get(&NodeQuery::variant("V=B", "SetCustomVariant")).unwrap();
        assert!(matches!(
            view_animation_override(vb_variant),
            Some(Animation_override::DisableAnimations(_))
        ));

        // (c) Explicit per-instance DisableAnimations wins on Instance while NodeVariant retains
        // synthesized REST Matrix, and overrides_table records the instance override.
        let rest_variant = views.get(&NodeQuery::variant("R=A", "SetRestOnly")).unwrap();
        assert!(matches!(
            view_animation_override(rest_variant),
            Some(Animation_override::Matrix(_))
        ));
        let inst_override = main_view.find_view_by_id("10:3").unwrap();
        assert!(matches!(
            view_animation_override(inst_override),
            Some(Animation_override::DisableAnimations(_))
        ));
        assert!(matches!(
            overrides_table_anim_override(inst_override, "R=A"),
            Some(Animation_override::DisableAnimations(_))
        ));
    }

    #[test]
    fn test_document_nodes_out_of_tree_navigate_discovers_component_set_and_propagates_matrix() {
        use crate::reaction_schema::{BezierJson, EasingJson as ReactionEasingJson, TriggerJson};
        use dc_bundle::animationspec::animation_override::Animation_override;
        use dc_bundle::reaction::action::{node::Navigation, Action_type};

        let custom_bezier = ReactionEasingJson::CustomCubicBezier {
            bezier: BezierJson { x1: 0.2, y1: 0.0, x2: 0.0, y2: 1.0 },
        };
        let page_children = vec![
            with_interactions(
                test_frame("10:0", "#Start", vec![]),
                vec![navigate_interaction(TriggerJson::OnClick, "11:0")],
            ),
            test_frame("11:0", "#Dest", vec![test_instance("11:1", "#DestPill", "20:1", vec![])]),
            test_component_set(
                "20:0",
                "DestPillSet",
                vec![
                    with_interactions(
                        test_component("20:1", "State=Idle", vec![]),
                        vec![smart_animate_change_to_with_easing(
                            TriggerJson::OnHover,
                            "20:2",
                            custom_bezier,
                            0.25,
                        )],
                    ),
                    test_component("20:2", "State=Active", vec![]),
                ],
            ),
        ];

        let mut doc = make_test_document(test_file_response(
            "OutOfTree Navigate Doc",
            page_children,
            &[],
            &[],
        ));
        let mut errors = Vec::new();
        let views = doc
            .nodes(&vec![NodeQuery::name("#Start")], &vec![], &mut errors, HiddenNodePolicy::Skip)
            .expect("nodes() should succeed");
        assert!(errors.is_empty());

        let start_view = views.get(&NodeQuery::name("#Start")).expect("#Start view must exist");
        assert!(start_view.reactions.iter().any(|r| {
            matches!(
                r.action.as_ref().and_then(|a| a.action_type.as_ref()),
                Some(Action_type::Node(n))
                    if n.destination_id.as_deref() == Some("11:0")
                        && n.navigation.enum_value_or_default() == Navigation::NAVIGATION_NAVIGATE
            )
        }));

        let dest_view =
            views.get(&NodeQuery::id("11:0")).expect("Out-of-tree #Dest view must be emitted");
        let dest_pill =
            dest_view.find_view_by_id("11:1").expect("Placed #DestPill instance must exist");
        let cs_view = views
            .get(&NodeQuery::component_set("DestPillSet"))
            .expect("DestPillSet NodeComponentSet view must be discovered and emitted");
        let idle_view = views
            .get(&NodeQuery::variant("State=Idle", "DestPillSet"))
            .expect("State=Idle NodeVariant view must be discovered and emitted");
        let active_view = views
            .get(&NodeQuery::variant("State=Active", "DestPillSet"))
            .expect("State=Active NodeVariant view must be discovered and emitted");

        for view in [cs_view, idle_view, active_view, dest_pill] {
            let Some(Animation_override::Matrix(matrix)) = view_animation_override(view) else {
                panic!("Expected Matrix AnimationOverride on {}", view.name);
            };
            assert_eq!(matrix.transitions.len(), 2);
            assert_eq!(matrix_pair_nanos(matrix, "State=Idle", "State=Active"), (0, 250_000_000));
            assert!(matrix
                .transitions
                .iter()
                .any(|t| t.source_variant == "State=Active" && t.target_variant == "State=Idle"));
        }

        assert!(
            dest_pill.component_info.as_ref().unwrap().overrides_table.is_empty(),
            "Unmodified #DestPill instance inside out-of-tree #Dest must have empty overrides_table"
        );
        assert!(
            !views.contains_key(&NodeQuery::id("20:1"))
                && !views.contains_key(&NodeQuery::id("20:2")),
            "Extra discovered ComponentSet variants should not be redundantly emitted as NodeId views: keys={:?}",
            views.keys().collect::<Vec<_>>()
        );
    }

    #[test]
    fn test_document_nodes_out_of_tree_multi_hop_and_squoosh_precedence() {
        use crate::reaction_schema::TriggerJson;
        use dc_bundle::animationspec::animation_override::Animation_override;

        // Multi-hop out-of-tree navigation:
        // #Start (queried) -> NAVIGATE -> #Dest -> variant State=Active -> NAVIGATE -> #SubModal -> #ModalPill (Instance of ModalSet with DisableAnimations).
        let page_children = vec![
            test_frame(
                "10:0",
                "#Start",
                vec![with_interactions(
                    test_frame("10:1", "#GoBtn", vec![]),
                    vec![navigate_interaction(TriggerJson::OnClick, "11:0")],
                )],
            ),
            test_frame("11:0", "#Dest", vec![test_instance("11:1", "#DestPill", "20:1", vec![])]),
            test_frame(
                "12:0",
                "#SubModal",
                vec![test_instance("12:1", "#ModalPill", "30:1", vec![])],
            ),
            test_component_set(
                "20:0",
                "DestPillSet",
                vec![
                    with_interactions(
                        test_component("20:1", "State=Idle", vec![]),
                        vec![smart_animate_change_to(TriggerJson::OnClick, "20:2", 0.2)],
                    ),
                    with_interactions(
                        test_component("20:2", "State=Active", vec![]),
                        vec![navigate_interaction(TriggerJson::OnClick, "12:0")],
                    ),
                ],
            ),
            with_squoosh(
                test_component_set(
                    "30:0",
                    "ModalSet",
                    vec![
                        with_interactions(
                            test_component("30:1", "M=A", vec![]),
                            vec![smart_animate_change_to(TriggerJson::OnHover, "30:2", 0.3)],
                        ),
                        test_component("30:2", "M=B", vec![]),
                    ],
                ),
                SQUOOSH_DISABLE,
            ),
        ];

        let mut doc = make_test_document(test_file_response(
            "MultiHop OutOfTree Doc",
            page_children,
            &[],
            &[],
        ));
        let mut errors = Vec::new();
        let views = doc
            .nodes(&vec![NodeQuery::name("#Start")], &vec![], &mut errors, HiddenNodePolicy::Skip)
            .expect("nodes() should succeed");
        assert!(errors.is_empty());

        assert!(views.contains_key(&NodeQuery::component_set("DestPillSet")));
        assert!(views.contains_key(&NodeQuery::variant("State=Idle", "DestPillSet")));
        assert!(views.contains_key(&NodeQuery::variant("State=Active", "DestPillSet")));
        assert!(views.contains_key(&NodeQuery::component_set("ModalSet")));
        let modal_variant_a = views
            .get(&NodeQuery::variant("M=A", "ModalSet"))
            .expect("Second-hop ModalSet NodeVariant M=A must be discovered");
        let sub_modal =
            views.get(&NodeQuery::id("12:0")).expect("Second-hop #SubModal must be emitted");
        let modal_pill =
            sub_modal.find_view_by_id("12:1").expect("Placed #ModalPill instance must exist");

        for view in [modal_variant_a, modal_pill] {
            assert!(matches!(
                view_animation_override(view),
                Some(Animation_override::DisableAnimations(_))
            ));
        }
        assert!(modal_pill.component_info.as_ref().unwrap().overrides_table.is_empty());
        assert!(!views.contains_key(&NodeQuery::id("20:1")));
        assert!(!views.contains_key(&NodeQuery::id("20:2")));
        assert!(!views.contains_key(&NodeQuery::id("30:1")));
        assert!(!views.contains_key(&NodeQuery::id("30:2")));
    }

    #[test]
    fn test_document_nodes_preserves_nested_template_child_squoosh_and_overrides_table() {
        use crate::reaction_schema::TriggerJson;
        use dc_bundle::animationspec::animation_override::Animation_override;

        let custom_squoosh = squoosh_custom_ms(400);
        let page_children = vec![
            test_frame(
                "10:0",
                "#Main",
                vec![
                    test_instance(
                        "10:1",
                        "#OuterCard",
                        "20:1",
                        vec![
                            test_instance("I10:1;20:11", "#InnerPillDisable", "30:1", vec![]),
                            test_instance("I10:1;20:12", "#InnerPillCustom", "30:1", vec![]),
                        ],
                    ),
                    test_instance(
                        "10:2",
                        "#OuterCardDefaultOverride",
                        "20:1",
                        vec![
                            with_squoosh(
                                test_instance("I10:2;20:11", "#InnerPillDisable", "30:1", vec![]),
                                SQUOOSH_DEFAULT,
                            ),
                            test_instance("I10:2;20:12", "#InnerPillCustom", "30:1", vec![]),
                        ],
                    ),
                    test_instance(
                        "10:3",
                        "#DeepOuter",
                        "25:1",
                        vec![test_instance(
                            "I10:3;25:11",
                            "#MidCard",
                            "20:1",
                            vec![
                                test_instance(
                                    "I10:3;25:11;20:11",
                                    "#InnerPillDisable",
                                    "30:1",
                                    vec![],
                                ),
                                test_instance(
                                    "I10:3;25:11;20:12",
                                    "#InnerPillCustom",
                                    "30:1",
                                    vec![],
                                ),
                            ],
                        )],
                    ),
                ],
            ),
            test_component_set(
                "20:0",
                "CardSet",
                vec![test_component(
                    "20:1",
                    "Card=One",
                    vec![
                        with_squoosh(
                            test_instance("20:11", "#InnerPillDisable", "30:1", vec![]),
                            SQUOOSH_DISABLE,
                        ),
                        with_squoosh(
                            test_instance("20:12", "#InnerPillCustom", "30:1", vec![]),
                            &custom_squoosh,
                        ),
                    ],
                )],
            ),
            test_component_set(
                "25:0",
                "DeepCardSet",
                vec![test_component(
                    "25:1",
                    "Deep=One",
                    vec![test_instance(
                        "25:11",
                        "#MidCard",
                        "20:1",
                        vec![
                            test_instance("I25:11;20:11", "#InnerPillDisable", "30:1", vec![]),
                            with_squoosh(
                                test_instance("I25:11;20:12", "#InnerPillCustom", "30:1", vec![]),
                                SQUOOSH_DEFAULT,
                            ),
                        ],
                    )],
                )],
            ),
            test_component_set(
                "30:0",
                "PillSet",
                vec![
                    with_interactions(
                        test_component("30:1", "Pill=Idle", vec![]),
                        vec![smart_animate_change_to(TriggerJson::OnHover, "30:2", 0.15)],
                    ),
                    test_component("30:2", "Pill=Active", vec![]),
                ],
            ),
        ];

        let mut doc = make_test_document(test_file_response(
            "Nested Template Squoosh Doc",
            page_children,
            &[],
            &[],
        ));
        let mut errors = Vec::new();
        let views = doc
            .nodes(&vec![NodeQuery::name("#Main")], &vec![], &mut errors, HiddenNodePolicy::Skip)
            .unwrap();
        assert!(errors.is_empty());

        let main_view = views.get(&NodeQuery::name("#Main")).unwrap();

        // 1. Unmodified #OuterCard (10:1) preserves template children's DisableAnimations and Custom
        //    when REST omits sharedPluginData on I10:1;20:11 and I10:1;20:12, keeping overrides_table empty.
        let outer_card = main_view.find_view_by_id("10:1").expect("#OuterCard must exist");
        let inner_disable =
            outer_card.find_view_by_id("I10:1;20:11").expect("#InnerPillDisable must exist");
        let inner_custom =
            outer_card.find_view_by_id("I10:1;20:12").expect("#InnerPillCustom must exist");
        assert!(matches!(
            view_animation_override(inner_disable),
            Some(Animation_override::DisableAnimations(_))
        ));
        assert!(matches!(
            view_animation_override(inner_custom),
            Some(Animation_override::Custom(_))
        ));
        assert!(
            outer_card.component_info.as_ref().unwrap().overrides_table.is_empty(),
            "Unmodified #OuterCard must have empty overrides_table when nested template squoosh is preserved"
        );
        assert!(
            inner_disable.component_info.as_ref().unwrap().overrides_table.is_empty(),
            "Unmodified #InnerPillDisable must have empty overrides_table"
        );
        assert!(
            inner_custom.component_info.as_ref().unwrap().overrides_table.is_empty(),
            "Unmodified #InnerPillCustom must have empty overrides_table"
        );

        // 2. Explicit "Default" override on instance child I10:2;20:11 overrides template DisableAnimations
        //    and adopts PillSet's synthesized REST Matrix, recording the override vs template 20:11.
        let outer_default_override =
            main_view.find_view_by_id("10:2").expect("#OuterCardDefaultOverride must exist");
        let child_default_override =
            outer_default_override.find_view_by_id("I10:2;20:11").expect("I10:2;20:11 must exist");
        assert!(matches!(
            view_animation_override(child_default_override),
            Some(Animation_override::Matrix(_))
        ));
        assert!(matches!(
            overrides_table_anim_override(child_default_override, "#InnerPillDisable"),
            Some(Animation_override::Matrix(_))
        ));
        // Sibling I10:2;20:12 without sharedPluginData still preserves template Custom.
        let sibling_custom = outer_default_override.find_view_by_id("I10:2;20:12").unwrap();
        assert!(matches!(
            view_animation_override(sibling_custom),
            Some(Animation_override::Custom(_))
        ));
        assert!(sibling_custom.component_info.as_ref().unwrap().overrides_table.is_empty());

        // 3. Multi-level ';' template lookup:
        //    - I10:3;25:11;20:11 skips unannotated I25:11;20:11 and finds DisableAnimations on 20:11.
        //    - I10:3;25:11;20:12 hits intermediate explicit "Default" on I25:11;20:12, stopping before
        //      20:12's Custom and resolving to PillSet's REST Matrix.
        let deep_outer = main_view.find_view_by_id("10:3").expect("#DeepOuter must exist");
        let deep_disable =
            deep_outer.find_view_by_id("I10:3;25:11;20:11").expect("I10:3;25:11;20:11 must exist");
        let deep_intermediate_default =
            deep_outer.find_view_by_id("I10:3;25:11;20:12").expect("I10:3;25:11;20:12 must exist");
        assert!(matches!(
            view_animation_override(deep_disable),
            Some(Animation_override::DisableAnimations(_))
        ));
        assert!(matches!(
            view_animation_override(deep_intermediate_default),
            Some(Animation_override::Matrix(_))
        ));
    }

    #[test]
    fn test_document_nodes_multi_tier_matrix_composition_on_press_and_remote_isolation() {
        use crate::reaction_schema::TriggerJson;
        use dc_bundle::animationspec::animation_override::Animation_override;

        let inst_matrix_squoosh =
            squoosh_matrix_json(None, &[("S=A", "S=B", 300), ("S=C", "S=A", 350)]);
        let inst_custom_squoosh = squoosh_custom_ms(400);

        let page_children = vec![
            test_frame(
                "10:0",
                "#Main",
                vec![
                    with_squoosh(
                        test_instance("10:1", "#InstMatrix", "20:1", vec![]),
                        &inst_matrix_squoosh,
                    ),
                    with_squoosh(
                        test_instance("10:2", "#InstCustom", "20:1", vec![]),
                        &inst_custom_squoosh,
                    ),
                    test_frame(
                        "10:30",
                        "Row3",
                        vec![with_squoosh(
                            test_instance("10:3", "#InstNoOverride", "20:1", vec![]),
                            SQUOOSH_DEFAULT,
                        )],
                    ),
                    test_instance("10:4", "#RemoteButton", "999:1", vec![]),
                ],
            ),
            build_multi_tier_button_cs("20:0", "SetMulti", TriggerJson::OnPress),
            with_squoosh(
                test_component_set(
                    "30:0",
                    "Button",
                    vec![
                        with_interactions(
                            test_component("30:1", "Btn=A", vec![]),
                            vec![smart_animate_change_to(TriggerJson::OnClick, "30:2", 0.2)],
                        ),
                        test_component("30:2", "Btn=B", vec![]),
                    ],
                ),
                SQUOOSH_DISABLE,
            ),
        ];

        let mut doc = make_test_document(test_file_response(
            "MultiTier And Remote Isolation Doc",
            page_children,
            &[("999:1", "remote_btn_k", "Btn=Remote", "999:0")],
            &[("999:0", "cs_remote_b", "Button")],
        ));
        doc.prefetched_components.insert(
            "remote_btn_k".to_string(),
            serde_json::json!({
                "error": false,
                "status": 200,
                "meta": {
                    "key": "remote_btn_k",
                    "file_key": "doc_test",
                    "node_id": "999:1",
                    "thumbnail_url": "",
                    "name": "Btn=Remote",
                    "description": "",
                    "created_at": "",
                    "updated_at": "",
                    "containing_frame": {
                        "name": null,
                        "nodeId": null,
                        "pageId": "0:1",
                        "pageName": "Page 1",
                        "containingStateGroup": {
                            "name": "Button",
                            "nodeId": "999:0"
                        }
                    },
                    "user": { "id": "u1", "handle": "h1", "img_url": "" }
                }
            })
            .to_string(),
        );

        let mut errors = Vec::new();
        let views = doc
            .nodes(&vec![NodeQuery::name("#Main")], &vec![], &mut errors, HiddenNodePolicy::Skip)
            .unwrap();
        assert!(errors.is_empty());

        let main_view = views.get(&NodeQuery::name("#Main")).unwrap();

        // #InstMatrix: Instance Matrix (S=A->S=B 300ms, S=C->S=A 350ms) + Variant Matrix +
        // ComponentSet Matrix (S=B->S=A 600ms, default_spec 500ms) + REST ON_PRESS (S=B->S=C 250ms
        // and reversible S=C->S=B 250ms).
        let inst_matrix = main_view.find_view_by_id("10:1").unwrap();
        let Some(Animation_override::Matrix(m)) = view_animation_override(inst_matrix) else {
            panic!("Expected Matrix on #InstMatrix");
        };
        assert_eq!(smooth_nanos(m.default_spec.as_ref().unwrap()), 500_000_000);
        assert_eq!(m.transitions.len(), 5);
        assert_eq!(matrix_pair_nanos(m, "S=A", "S=B"), (0, 300_000_000));
        assert_eq!(matrix_pair_nanos(m, "S=C", "S=A"), (0, 350_000_000));
        assert_eq!(matrix_pair_nanos(m, "S=B", "S=A"), (0, 600_000_000));
        assert_eq!(matrix_pair_nanos(m, "S=B", "S=C"), (0, 250_000_000));
        assert_eq!(matrix_pair_nanos(m, "S=C", "S=B"), (0, 250_000_000));
        assert!(
            matches!(
                overrides_table_anim_override(inst_matrix, "S=A"),
                Some(Animation_override::Matrix(_))
            ),
            "#InstMatrix instance override must be recorded in overrides_table"
        );

        // #InstCustom: Instance Custom replaces lower-tier Matrices outright.
        let inst_custom = main_view.find_view_by_id("10:2").unwrap();
        assert!(matches!(
            view_animation_override(inst_custom),
            Some(Animation_override::Custom(_))
        ));
        assert!(
            matches!(
                overrides_table_anim_override(inst_custom, "S=A"),
                Some(Animation_override::Custom(_))
            ),
            "#InstCustom instance override must be recorded in overrides_table"
        );

        // #InstNoOverride ("Default"): inherits Variant S=A composed Matrix (900ms S=A->S=B,
        // 600ms S=B->S=A, 500ms default_spec, 250ms ON_PRESS forward + reverse), empty overrides_table.
        let inst_no_ov = main_view.find_view_by_id("10:3").unwrap();
        let Some(Animation_override::Matrix(no_ov_m)) = view_animation_override(inst_no_ov) else {
            panic!("Expected Matrix on #InstNoOverride");
        };
        assert_eq!(smooth_nanos(no_ov_m.default_spec.as_ref().unwrap()), 500_000_000);
        assert_eq!(no_ov_m.transitions.len(), 4);
        assert_eq!(matrix_pair_nanos(no_ov_m, "S=A", "S=B"), (0, 900_000_000));
        assert!(
            inst_no_ov.component_info.as_ref().unwrap().overrides_table.is_empty(),
            "#InstNoOverride matching composed Variant S=A must have empty overrides_table"
        );

        // #RemoteButton: unresolved remote componentId ("999:1") whose component_set_name matches
        // local "Button" ComponentSet (DisableAnimations + REST) must NOT inherit local override.
        let remote_btn = main_view.find_view_by_id("10:4").unwrap();
        assert_eq!(remote_btn.component_info.as_ref().unwrap().component_set_name, "Button");
        assert_eq!(remote_btn.component_info.as_ref().unwrap().id, "999:1");
        assert!(
            view_animation_override(remote_btn).is_none(),
            "Unresolved remote instance must not inherit local Button ComponentSet DisableAnimations"
        );
    }

    #[test]
    fn test_document_nodes_terminates_on_cyclic_component_sets_and_cyclic_navigate() {
        use crate::reaction_schema::TriggerJson;
        use dc_bundle::animationspec::animation_override::Animation_override;

        // Mutual ComponentSet cycle (SetA <-> SetB), self-referential ComponentSet (SetSelf -> SetSelf),
        // and mutual + self-cyclic NAVIGATE paths (#Start <-> #PageB -> #PageB).
        let page_children = vec![
            with_interactions(
                test_frame(
                    "10:0",
                    "#Start",
                    vec![
                        test_instance("10:1", "#InstA", "20:1", vec![]),
                        test_instance("10:2", "#InstSelf", "40:1", vec![]),
                    ],
                ),
                vec![navigate_interaction(TriggerJson::OnClick, "11:0")],
            ),
            with_interactions(
                test_frame("11:0", "#PageB", vec![]),
                vec![
                    navigate_interaction(TriggerJson::OnClick, "10:0"),
                    navigate_interaction(TriggerJson::OnHover, "11:0"),
                ],
            ),
            test_component_set(
                "20:0",
                "SetA",
                vec![
                    with_interactions(
                        test_component(
                            "20:1",
                            "A=One",
                            vec![test_instance("20:11", "#InstB", "30:1", vec![])],
                        ),
                        vec![smart_animate_change_to(TriggerJson::OnClick, "20:2", 0.2)],
                    ),
                    test_component("20:2", "A=Two", vec![]),
                ],
            ),
            test_component_set(
                "30:0",
                "SetB",
                vec![test_component(
                    "30:1",
                    "B=One",
                    vec![test_instance("30:11", "#BackToA", "20:2", vec![])],
                )],
            ),
            test_component_set(
                "40:0",
                "SetSelf",
                vec![test_component(
                    "40:1",
                    "Self=One",
                    vec![test_instance("40:11", "#SelfChild", "40:1", vec![])],
                )],
            ),
        ];

        let mut doc = make_test_document(test_file_response(
            "Cyclic Sets And Navigate Doc",
            page_children,
            &[],
            &[],
        ));
        let mut errors = Vec::new();
        let views = doc
            .nodes(&vec![NodeQuery::name("#Start")], &vec![], &mut errors, HiddenNodePolicy::Skip)
            .expect("Cyclic ComponentSets and NAVIGATE reactions must terminate without overflow");
        assert!(errors.is_empty());

        assert!(views.contains_key(&NodeQuery::name("#Start")));
        assert!(views.contains_key(&NodeQuery::id("11:0")));
        assert!(views.contains_key(&NodeQuery::component_set("SetA")));
        assert!(views.contains_key(&NodeQuery::component_set("SetB")));
        assert!(views.contains_key(&NodeQuery::component_set("SetSelf")));
        assert!(views.contains_key(&NodeQuery::variant("B=One", "SetB")));
        assert!(views.contains_key(&NodeQuery::variant("Self=One", "SetSelf")));

        let a_one = views.get(&NodeQuery::variant("A=One", "SetA")).unwrap();
        let Some(Animation_override::Matrix(m)) = view_animation_override(a_one) else {
            panic!("Expected SetA Matrix on A=One");
        };
        assert_eq!(matrix_pair_nanos(m, "A=One", "A=Two"), (0, 200_000_000));
    }

    #[test]
    fn test_document_nodes_dual_node_id_and_node_variant_queries_convert_once() {
        use crate::reaction_schema::TriggerJson;
        use dc_bundle::animationspec::animation_override::Animation_override;

        let make_dual_doc = |start: figma_schema::Node, extras: Vec<figma_schema::Node>| {
            let mut page_children = vec![start];
            page_children.extend(extras);
            page_children.push(test_component_set(
                "20:0",
                "SetDual",
                vec![
                    with_interactions(
                        test_component("20:1", "D=One", vec![]),
                        vec![
                            smart_animate_change_to(TriggerJson::OnHover, "20:2", 0.2),
                            navigate_interaction(TriggerJson::OnClick, "12:0"),
                        ],
                    ),
                    test_component("20:2", "D=Two", vec![]),
                ],
            ));
            page_children.push(test_frame("12:0", "#Leaf", vec![]));
            make_test_document(test_file_response("Dual Query Doc", page_children, &[], &[]))
        };

        // (a) Direct dual queries NodeId("20:1") + NodeVariant("D=One", "SetDual") convert once.
        let mut doc_direct = make_dual_doc(test_frame("10:0", "#Unused", vec![]), vec![]);
        let mut errors_a = Vec::new();
        let views_a = doc_direct
            .nodes(
                &vec![NodeQuery::id("20:1"), NodeQuery::variant("D=One", "SetDual")],
                &vec![],
                &mut errors_a,
                HiddenNodePolicy::Skip,
            )
            .unwrap();
        assert!(errors_a.is_empty());
        let direct_id = views_a.get(&NodeQuery::id("20:1")).unwrap();
        let direct_var = views_a.get(&NodeQuery::variant("D=One", "SetDual")).unwrap();
        assert_eq!(
            direct_id.unique_id, direct_var.unique_id,
            "Dual NodeId + NodeVariant queries must reuse the same converted View (single unique_id)"
        );
        assert_eq!(direct_id, direct_var);
        assert!(matches!(view_animation_override(direct_id), Some(Animation_override::Matrix(_))));

        // (b) Out-of-tree discovery hitting both NAVIGATE->20:1 and #OtherWithInst(Instance 20:1)
        //     converts 20:1 once, expands #Leaf ("12:0"), and keeps #DualInst overrides_table empty.
        let mut doc_discover = make_dual_doc(
            with_interactions(
                test_frame("10:0", "#Start", vec![]),
                vec![
                    navigate_interaction(TriggerJson::OnClick, "11:0"),
                    navigate_interaction(TriggerJson::OnHover, "20:1"),
                ],
            ),
            vec![test_frame(
                "11:0",
                "#OtherWithInst",
                vec![test_instance("11:1", "#DualInst", "20:1", vec![])],
            )],
        );
        let mut errors_b = Vec::new();
        let views_b = doc_discover
            .nodes(&vec![NodeQuery::name("#Start")], &vec![], &mut errors_b, HiddenNodePolicy::Skip)
            .unwrap();
        assert!(errors_b.is_empty());
        let disc_id = views_b.get(&NodeQuery::id("20:1")).unwrap();
        let disc_var = views_b.get(&NodeQuery::variant("D=One", "SetDual")).unwrap();
        assert_eq!(
            disc_id.unique_id, disc_var.unique_id,
            "Out-of-tree dual NodeId + NodeVariant discovery must convert 20:1 only once"
        );
        assert_eq!(disc_id, disc_var);
        assert!(views_b.contains_key(&NodeQuery::id("12:0")));
        let other_view = views_b.get(&NodeQuery::id("11:0")).unwrap();
        let dual_inst = other_view.find_view_by_id("11:1").unwrap();
        assert!(dual_inst.component_info.as_ref().unwrap().overrides_table.is_empty());

        // (c) When NodeVariant("D=One", "SetDual") was converted in hop 1, a later-hop NAVIGATE to
        //     "20:1" must not re-enqueue a duplicate NodeId("20:1") conversion.
        let mut doc_prior = make_dual_doc(
            with_interactions(
                test_frame("10:0", "#Start", vec![test_instance("10:1", "#Inst", "20:1", vec![])]),
                vec![navigate_interaction(TriggerJson::OnClick, "11:0")],
            ),
            vec![with_interactions(
                test_frame("11:0", "#LaterNav", vec![]),
                vec![navigate_interaction(TriggerJson::OnClick, "20:1")],
            )],
        );
        let mut errors_c = Vec::new();
        let views_c = doc_prior
            .nodes(&vec![NodeQuery::name("#Start")], &vec![], &mut errors_c, HiddenNodePolicy::Skip)
            .unwrap();
        assert!(errors_c.is_empty());
        assert!(views_c.contains_key(&NodeQuery::variant("D=One", "SetDual")));
        assert!(!views_c.contains_key(&NodeQuery::id("20:1")));
    }

    #[test]
    fn test_document_nodes_hidden_node_policy_skip_suppresses_hidden_rest_destination() {
        use crate::reaction_schema::TriggerJson;

        let build_doc = || {
            let page_children = vec![
                test_frame(
                    "10:0",
                    "#Main",
                    vec![
                        with_interactions(
                            test_frame("10:1", "#VisibleBtn", vec![]),
                            vec![navigate_interaction(TriggerJson::OnClick, "11:0")],
                        ),
                        with_visible(
                            with_interactions(
                                test_frame("10:2", "#HiddenBtn", vec![]),
                                vec![navigate_interaction(TriggerJson::OnClick, "12:0")],
                            ),
                            false,
                        ),
                    ],
                ),
                test_frame("11:0", "#VisibleDest", vec![]),
                test_frame("12:0", "#HiddenOnlyDest", vec![]),
            ];
            make_test_document(test_file_response("Hidden Policy Nav Doc", page_children, &[], &[]))
        };

        let mut errors_skip = Vec::new();
        let views_skip = build_doc()
            .nodes(
                &vec![NodeQuery::name("#Main")],
                &vec![],
                &mut errors_skip,
                HiddenNodePolicy::Skip,
            )
            .unwrap();
        assert!(errors_skip.is_empty());
        let main_skip = views_skip.get(&NodeQuery::name("#Main")).unwrap();
        assert!(main_skip.find_view_by_id("10:1").is_some());
        assert!(main_skip.find_view_by_id("10:2").is_none());
        assert!(views_skip.contains_key(&NodeQuery::id("11:0")));
        assert!(
            !views_skip.contains_key(&NodeQuery::id("12:0")),
            "HiddenNodePolicy::Skip must suppress REST NAVIGATE destination expansion from hidden children"
        );

        let mut errors_keep = Vec::new();
        let views_keep = build_doc()
            .nodes(
                &vec![NodeQuery::name("#Main")],
                &vec![],
                &mut errors_keep,
                HiddenNodePolicy::Keep,
            )
            .unwrap();
        assert!(errors_keep.is_empty());
        let main_keep = views_keep.get(&NodeQuery::name("#Main")).unwrap();
        assert!(main_keep.find_view_by_id("10:2").is_some());
        assert!(views_keep.contains_key(&NodeQuery::id("11:0")));
        assert!(views_keep.contains_key(&NodeQuery::id("12:0")));
    }

    fn with_image_fill(mut node: figma_schema::Node, image_ref: &str) -> figma_schema::Node {
        node.fills = vec![figma_schema::Paint {
            visible: true,
            opacity: 1.0,
            data: figma_schema::PaintData::Image {
                scale_mode: figma_schema::ScaleMode::Fill,
                image_transform: None,
                scaling_factor: None,
                rotation: 0.0,
                image_ref: Some(image_ref.to_string()),
                gif_ref: None,
                filters: None,
            },
        }];
        node
    }

    fn seed_hermetic_images(doc: &mut Document, entries: &[(&str, &str)]) {
        #[derive(serde::Serialize)]
        struct SessionSeed {
            version: u32,
            images: HashMap<String, Option<String>>,
            vectors: HashSet<String>,
            image_hash: HashMap<String, crate::image_context::VectorImageId>,
            client_images: HashSet<String>,
        }

        let images: HashMap<String, Option<String>> =
            entries.iter().map(|(r, url)| ((*r).to_string(), Some((*url).to_string()))).collect();
        let client_images: HashSet<String> =
            entries.iter().map(|(_, url)| (*url).to_string()).collect();
        let seed = SessionSeed {
            version: 2,
            images,
            vectors: HashSet::new(),
            image_hash: HashMap::new(),
            client_images,
        };
        let session: ImageContextSession =
            serde_json::from_value(serde_json::to_value(seed).unwrap())
                .expect("SessionSeed must deserialize into ImageContextSession");
        doc.image_context.add_session_info(session);
    }

    fn view_first_image_key(view: &View) -> Option<&str> {
        use dc_bundle::background::background::Background_type;

        match view
            .style
            .as_ref()?
            .node_style
            .as_ref()?
            .backgrounds
            .first()?
            .background_type
            .as_ref()?
        {
            Background_type::Image(img) => Some(img.key.as_str()),
            _ => None,
        }
    }

    fn build_same_named_progress_bar_sets() -> (figma_schema::Node, figma_schema::Node) {
        use crate::reaction_schema::TriggerJson;

        // First ComponentSet named "ProgressBar" (20:0): HBar=Small <-> HBar=Big (200ms).
        let hbar_set = test_component_set(
            "20:0",
            "ProgressBar",
            vec![
                with_interactions(
                    test_component(
                        "20:1",
                        "HBar=Small",
                        vec![
                            with_image_fill(
                                test_frame("20:11", "#SkipBarImg", vec![]),
                                "ref_h_skip",
                            ),
                            with_image_fill(
                                test_frame("20:12", "#KeepBarImg", vec![]),
                                "ref_h_keep",
                            ),
                        ],
                    ),
                    vec![smart_animate_change_to(TriggerJson::OnHover, "20:2", 0.2)],
                ),
                test_component("20:2", "HBar=Big", vec![]),
            ],
        );

        // Second ComponentSet ALSO named "ProgressBar" (30:0): VBar=Small <-> VBar=Big (350ms).
        // Indexed after 20:0 so `component_set_name_index["ProgressBar"]` points to 30:0.
        let vbar_set = test_component_set(
            "30:0",
            "ProgressBar",
            vec![
                with_interactions(
                    test_component(
                        "30:1",
                        "VBar=Small",
                        vec![
                            with_image_fill(
                                test_frame("30:11", "#SkipBarImg", vec![]),
                                "ref_v_skip",
                            ),
                            with_image_fill(
                                test_frame("30:12", "#KeepBarImg", vec![]),
                                "ref_v_keep",
                            ),
                        ],
                    ),
                    vec![smart_animate_change_to(TriggerJson::OnHover, "30:2", 0.35)],
                ),
                test_component("30:2", "VBar=Big", vec![]),
            ],
        );

        (hbar_set, vbar_set)
    }

    #[test]
    fn test_document_nodes_same_named_progress_bar_initial_query_preserves_hbar_variants() {
        use dc_bundle::animationspec::animation_override::Animation_override;

        let (hbar_set, vbar_set) = build_same_named_progress_bar_sets();
        let page_children = vec![
            test_frame("10:0", "#HBarStage", vec![test_instance("10:1", "#HBar", "20:1", vec![])]),
            test_frame(
                "11:0",
                "#UnreferencedVBarStage",
                vec![test_instance("11:1", "#VBar", "30:1", vec![])],
            ),
            hbar_set,
            vbar_set,
        ];

        let mut doc = make_test_document(test_file_response(
            "Same-Named ProgressBar Initial Doc",
            page_children,
            &[],
            &[],
        ));
        seed_hermetic_images(
            &mut doc,
            &[
                ("ref_h_skip", "mem://h_skip"),
                ("ref_h_keep", "mem://h_keep"),
                ("ref_v_skip", "mem://v_skip"),
                ("ref_v_keep", "mem://v_keep"),
            ],
        );

        let mut errors = Vec::new();
        let views = doc
            .nodes(
                &vec![NodeQuery::name("#HBarStage")],
                &vec![],
                &mut errors,
                HiddenNodePolicy::Skip,
            )
            .expect("nodes() should succeed for initial #HBarStage query");
        assert!(errors.is_empty());

        let hbar_small = views
            .get(&NodeQuery::variant("HBar=Small", "ProgressBar"))
            .expect("NodeVariant(HBar=Small, ProgressBar) must be preserved and converted");
        let hbar_big = views
            .get(&NodeQuery::variant("HBar=Big", "ProgressBar"))
            .expect("NodeVariant(HBar=Big, ProgressBar) must be preserved and converted");
        assert_eq!(hbar_small.id, "20:1");
        assert_eq!(hbar_big.id, "20:2");

        let stage_view = views.get(&NodeQuery::name("#HBarStage")).unwrap();
        let hbar_inst = stage_view.find_view_by_id("10:1").expect("#HBar instance must exist");

        for view in [hbar_small, hbar_big, hbar_inst] {
            let Some(Animation_override::Matrix(matrix)) = view_animation_override(view) else {
                panic!("Expected HBar AnimationMatrix on {}", view.name);
            };
            assert_eq!(matrix.transitions.len(), 2);
            assert_eq!(matrix_pair_nanos(matrix, "HBar=Small", "HBar=Big"), (0, 200_000_000));
            assert_eq!(matrix_pair_nanos(matrix, "HBar=Big", "HBar=Small"), (0, 200_000_000));
        }
        assert!(
            hbar_inst.component_info.as_ref().unwrap().overrides_table.is_empty(),
            "Unmodified #HBar instance must have empty overrides_table"
        );

        // Unreferenced VBar variants and unreferenced stage must not cross-leak on initial query,
        // NodeComponentSet("ProgressBar") must point to the referenced 20:0 set (not 30:0), and
        // variants must not be redundantly emitted under NodeId keys.
        assert_eq!(views.get(&NodeQuery::component_set("ProgressBar")).unwrap().id, "20:0");
        assert!(!views.contains_key(&NodeQuery::variant("VBar=Small", "ProgressBar")));
        assert!(!views.contains_key(&NodeQuery::variant("VBar=Big", "ProgressBar")));
        assert!(!views.contains_key(&NodeQuery::id("11:0")));
        for raw_id in ["20:1", "20:2", "30:1", "30:2"] {
            assert!(!views.contains_key(&NodeQuery::id(raw_id)));
        }
        assert!(!doc.encoded_image_map().0.contains_key("mem://v_keep"));
    }

    #[test]
    fn test_document_nodes_same_named_progress_bar_out_of_tree_navigate_and_ignored_images() {
        use crate::reaction_schema::TriggerJson;
        use dc_bundle::animationspec::animation_override::Animation_override;

        let image_entries = [
            ("ref_h_skip", "mem://h_skip"),
            ("ref_h_keep", "mem://h_keep"),
            ("ref_v_skip", "mem://v_skip"),
            ("ref_v_keep", "mem://v_keep"),
        ];
        let ignored_images =
            vec![(NodeQuery::name("ProgressBar"), vec!["#SkipBarImg".to_string()])];

        // (a) Initial #HBarStage holds HBar=Small and navigates out-of-tree to #VBarStage holding
        //     VBar=Small: both same-named "ProgressBar" sets expand deterministically without
        //     visited_component_sets or component_set_hash collision data loss.
        let (hbar_set, vbar_set) = build_same_named_progress_bar_sets();
        let page_children = vec![
            with_interactions(
                test_frame(
                    "10:0",
                    "#HBarStage",
                    vec![test_instance("10:1", "#HBar", "20:1", vec![])],
                ),
                vec![navigate_interaction(TriggerJson::OnClick, "11:0")],
            ),
            test_frame("11:0", "#VBarStage", vec![test_instance("11:1", "#VBar", "30:1", vec![])]),
            hbar_set,
            vbar_set,
        ];
        let mut doc_both = make_test_document(test_file_response(
            "Same-Named ProgressBar OutOfTree Navigate Doc",
            page_children,
            &[],
            &[],
        ));
        seed_hermetic_images(&mut doc_both, &image_entries);

        let mut errors_both = Vec::new();
        let views_both = doc_both
            .nodes(
                &vec![NodeQuery::name("#HBarStage")],
                &ignored_images,
                &mut errors_both,
                HiddenNodePolicy::Skip,
            )
            .expect("nodes() should succeed across out-of-tree NAVIGATE to second ProgressBar set");
        assert!(errors_both.is_empty());

        let hbar_small = views_both
            .get(&NodeQuery::variant("HBar=Small", "ProgressBar"))
            .expect("HBar=Small variant must remain converted");
        let hbar_big = views_both
            .get(&NodeQuery::variant("HBar=Big", "ProgressBar"))
            .expect("HBar=Big variant must remain converted");
        let vbar_small = views_both
            .get(&NodeQuery::variant("VBar=Small", "ProgressBar"))
            .expect("Out-of-tree VBar=Small variant must be expanded and converted");
        let vbar_big = views_both
            .get(&NodeQuery::variant("VBar=Big", "ProgressBar"))
            .expect("Out-of-tree VBar=Big variant must be expanded and converted");
        assert_eq!(hbar_small.id, "20:1");
        assert_eq!(hbar_big.id, "20:2");
        assert_eq!(vbar_small.id, "30:1");
        assert_eq!(vbar_big.id, "30:2");

        let hbar_stage = views_both.get(&NodeQuery::name("#HBarStage")).unwrap();
        let hbar_inst = hbar_stage.find_view_by_id("10:1").unwrap();
        let vbar_stage = views_both
            .get(&NodeQuery::id("11:0"))
            .expect("Out-of-tree #VBarStage destination frame must be converted");
        let vbar_inst = vbar_stage.find_view_by_id("11:1").unwrap();

        for view in [hbar_small, hbar_big, hbar_inst] {
            let Some(Animation_override::Matrix(m)) = view_animation_override(view) else {
                panic!("Expected HBar Matrix on {}", view.name);
            };
            assert_eq!(m.transitions.len(), 2);
            assert_eq!(matrix_pair_nanos(m, "HBar=Small", "HBar=Big"), (0, 200_000_000));
        }
        for view in [vbar_small, vbar_big, vbar_inst] {
            let Some(Animation_override::Matrix(m)) = view_animation_override(view) else {
                panic!("Expected VBar Matrix on {}", view.name);
            };
            assert_eq!(m.transitions.len(), 2);
            assert_eq!(matrix_pair_nanos(m, "VBar=Small", "VBar=Big"), (0, 350_000_000));
        }
        assert!(hbar_inst.component_info.as_ref().unwrap().overrides_table.is_empty());
        assert!(vbar_inst.component_info.as_ref().unwrap().overrides_table.is_empty());
        for raw_id in ["20:1", "20:2", "30:1", "30:2"] {
            assert!(
                !views_both.contains_key(&NodeQuery::id(raw_id)),
                "Variant {raw_id} must not be redundantly emitted as NodeId"
            );
        }

        // Verify ignored_images for NodeQuery::name("ProgressBar") applied to BOTH ProgressBar sets
        // (HBar=Small in set 20:0 was not overwritten by VBar=* in set 30:0).
        assert_eq!(view_first_image_key(hbar_small.find_view_by_id("20:11").unwrap()), None);
        assert_eq!(
            view_first_image_key(hbar_small.find_view_by_id("20:12").unwrap()),
            Some("mem://h_keep")
        );
        assert_eq!(view_first_image_key(vbar_small.find_view_by_id("30:11").unwrap()), None);
        assert_eq!(
            view_first_image_key(vbar_small.find_view_by_id("30:12").unwrap()),
            Some("mem://v_keep")
        );
        let encoded_both = doc_both.encoded_image_map();
        assert!(encoded_both.0.contains_key("mem://h_keep"));
        assert!(encoded_both.0.contains_key("mem://v_keep"));
        assert!(!encoded_both.0.contains_key("mem://h_skip"));
        assert!(!encoded_both.0.contains_key("mem://v_skip"));

        // (b) Pure out-of-tree NAVIGATE (#Start -> #VBarStage) with no ProgressBar in initial query
        //     expands only the referenced VBar ProgressBar set while preserving ignored_images.
        let (hbar_set_b, vbar_set_b) = build_same_named_progress_bar_sets();
        let mut doc_vonly = make_test_document(test_file_response(
            "Same-Named ProgressBar VBar-Only Navigate Doc",
            vec![
                with_interactions(
                    test_frame("09:0", "#Start", vec![]),
                    vec![navigate_interaction(TriggerJson::OnClick, "11:0")],
                ),
                test_frame(
                    "11:0",
                    "#VBarStage",
                    vec![test_instance("11:1", "#VBar", "30:1", vec![])],
                ),
                hbar_set_b,
                vbar_set_b,
            ],
            &[],
            &[],
        ));
        seed_hermetic_images(&mut doc_vonly, &image_entries);

        let mut errors_vonly = Vec::new();
        let views_vonly = doc_vonly
            .nodes(
                &vec![NodeQuery::name("#Start")],
                &ignored_images,
                &mut errors_vonly,
                HiddenNodePolicy::Skip,
            )
            .expect("nodes() should succeed on #Start -> #VBarStage out-of-tree NAVIGATE");
        assert!(errors_vonly.is_empty());
        assert!(views_vonly.contains_key(&NodeQuery::id("11:0")));
        assert!(views_vonly.contains_key(&NodeQuery::variant("VBar=Small", "ProgressBar")));
        assert!(views_vonly.contains_key(&NodeQuery::variant("VBar=Big", "ProgressBar")));
        assert!(!views_vonly.contains_key(&NodeQuery::variant("HBar=Small", "ProgressBar")));
        assert!(!views_vonly.contains_key(&NodeQuery::variant("HBar=Big", "ProgressBar")));
        let vbar_small_only =
            views_vonly.get(&NodeQuery::variant("VBar=Small", "ProgressBar")).unwrap();
        assert_eq!(view_first_image_key(vbar_small_only.find_view_by_id("30:11").unwrap()), None);
        assert_eq!(
            view_first_image_key(vbar_small_only.find_view_by_id("30:12").unwrap()),
            Some("mem://v_keep")
        );
    }

    #[test]
    fn test_document_nodes_hidden_node_policy_component_sets_and_hidden_variant_navigate() {
        use crate::reaction_schema::TriggerJson;
        use dc_bundle::animationspec::animation_override::Animation_override;

        let build_doc = || {
            let page_children = vec![
                test_frame(
                    "10:0",
                    "#Main",
                    vec![
                        // (a) Visible Instance of VisibleRefSet (which contains a hidden variant
                        //     Component with a NAVIGATE interaction).
                        test_instance("10:1", "#VisibleInst", "20:1", vec![]),
                        // (b) Hidden child Instance referencing an otherwise-unreferenced ComponentSet.
                        with_visible(test_instance("10:2", "#HiddenInst", "30:1", vec![]), false),
                    ],
                ),
                test_component_set(
                    "20:0",
                    "VisibleRefSet",
                    vec![
                        with_interactions(
                            test_component("20:1", "State=Visible", vec![]),
                            vec![
                                smart_animate_change_to(TriggerJson::OnClick, "20:3", 0.2),
                                smart_animate_change_to(TriggerJson::OnHover, "20:2", 0.35),
                            ],
                        ),
                        with_visible(
                            with_interactions(
                                test_component("20:2", "State=HiddenDraft", vec![]),
                                vec![navigate_interaction(TriggerJson::OnClick, "13:0")],
                            ),
                            false,
                        ),
                        test_component("20:3", "State=Active", vec![]),
                    ],
                ),
                test_component_set(
                    "30:0",
                    "HiddenInstOnlySet",
                    vec![
                        with_interactions(
                            test_component("30:1", "H=One", vec![]),
                            vec![navigate_interaction(TriggerJson::OnClick, "14:0")],
                        ),
                        test_component("30:2", "H=Two", vec![]),
                    ],
                ),
                test_frame("13:0", "#HiddenVariantDest", vec![]),
                test_frame("14:0", "#HiddenInstSetDest", vec![]),
            ];
            make_test_document(test_file_response(
                "Hidden Policy ComponentSets And Variant Navigate Doc",
                page_children,
                &[],
                &[],
            ))
        };

        // 1. HiddenNodePolicy::Skip omits the hidden variant, the hidden variant's NAVIGATE
        //    destination, and the hidden child instance's otherwise-unreferenced ComponentSet.
        let mut doc_skip = build_doc();
        let mut errors_skip = Vec::new();
        let views_skip = doc_skip
            .nodes(
                &vec![NodeQuery::name("#Main")],
                &vec![],
                &mut errors_skip,
                HiddenNodePolicy::Skip,
            )
            .unwrap();
        assert!(errors_skip.is_empty());

        let main_skip = views_skip.get(&NodeQuery::name("#Main")).unwrap();
        let visible_inst_skip =
            main_skip.find_view_by_id("10:1").expect("#VisibleInst must exist under Skip");
        assert!(
            main_skip.find_view_by_id("10:2").is_none(),
            "#HiddenInst must be omitted from #Main under HiddenNodePolicy::Skip"
        );

        assert!(views_skip.contains_key(&NodeQuery::component_set("VisibleRefSet")));
        let vis_var_skip = views_skip
            .get(&NodeQuery::variant("State=Visible", "VisibleRefSet"))
            .expect("Visible variant State=Visible must be converted under Skip");
        assert!(views_skip.contains_key(&NodeQuery::variant("State=Active", "VisibleRefSet")));
        assert!(
            !views_skip.contains_key(&NodeQuery::variant("State=HiddenDraft", "VisibleRefSet")),
            "Hidden variant State=HiddenDraft must be omitted under HiddenNodePolicy::Skip"
        );
        assert!(!views_skip.contains_key(&NodeQuery::id("20:2")));
        assert!(
            !doc_skip.component_sets.contains_key("20:2"),
            "index_node must omit hidden variant 20:2 from component_sets under Skip"
        );
        assert!(
            !views_skip.contains_key(&NodeQuery::id("13:0")),
            "Hidden variant NAVIGATE destination 13:0 must not expand under HiddenNodePolicy::Skip"
        );

        assert!(
            !views_skip.contains_key(&NodeQuery::component_set("HiddenInstOnlySet")),
            "ComponentSet referenced only by hidden child Instance must be omitted under Skip"
        );
        assert!(!views_skip.contains_key(&NodeQuery::variant("H=One", "HiddenInstOnlySet")));
        assert!(!views_skip.contains_key(&NodeQuery::variant("H=Two", "HiddenInstOnlySet")));
        assert!(!views_skip.contains_key(&NodeQuery::id("30:1")));
        assert!(!views_skip.contains_key(&NodeQuery::id("14:0")));

        let Some(Animation_override::Matrix(skip_matrix)) = view_animation_override(vis_var_skip)
        else {
            panic!("Expected Matrix on State=Visible under Skip");
        };
        assert_eq!(skip_matrix.transitions.len(), 1);
        assert_eq!(
            matrix_pair_nanos(skip_matrix, "State=Visible", "State=Active"),
            (0, 200_000_000)
        );
        assert!(visible_inst_skip.component_info.as_ref().unwrap().overrides_table.is_empty());

        // 2. HiddenNodePolicy::Keep includes the hidden variant, its NAVIGATE destination, and the
        //    hidden child instance's ComponentSet (plus its variants and destinations).
        let mut doc_keep = build_doc();
        let mut errors_keep = Vec::new();
        let views_keep = doc_keep
            .nodes(
                &vec![NodeQuery::name("#Main")],
                &vec![],
                &mut errors_keep,
                HiddenNodePolicy::Keep,
            )
            .unwrap();
        assert!(errors_keep.is_empty());

        let main_keep = views_keep.get(&NodeQuery::name("#Main")).unwrap();
        assert!(main_keep.find_view_by_id("10:1").is_some());
        assert!(
            main_keep.find_view_by_id("10:2").is_some(),
            "#HiddenInst must be retained in #Main under HiddenNodePolicy::Keep"
        );

        assert!(views_keep.contains_key(&NodeQuery::component_set("VisibleRefSet")));
        let vis_var_keep =
            views_keep.get(&NodeQuery::variant("State=Visible", "VisibleRefSet")).unwrap();
        assert!(
            views_keep.contains_key(&NodeQuery::variant("State=HiddenDraft", "VisibleRefSet")),
            "Hidden variant State=HiddenDraft must be included under HiddenNodePolicy::Keep"
        );
        assert_eq!(doc_keep.component_sets.get("20:2").map(String::as_str), Some("20:0"));
        assert!(
            views_keep.contains_key(&NodeQuery::id("13:0")),
            "Hidden variant NAVIGATE destination 13:0 must be included under HiddenNodePolicy::Keep"
        );

        assert!(
            views_keep.contains_key(&NodeQuery::component_set("HiddenInstOnlySet")),
            "Hidden child instance's ComponentSet must be included under HiddenNodePolicy::Keep"
        );
        assert!(views_keep.contains_key(&NodeQuery::variant("H=One", "HiddenInstOnlySet")));
        assert!(views_keep.contains_key(&NodeQuery::variant("H=Two", "HiddenInstOnlySet")));
        assert!(views_keep.contains_key(&NodeQuery::id("14:0")));

        let Some(Animation_override::Matrix(keep_matrix)) = view_animation_override(vis_var_keep)
        else {
            panic!("Expected Matrix on State=Visible under Keep");
        };
        assert_eq!(keep_matrix.transitions.len(), 3);
        assert_eq!(
            matrix_pair_nanos(keep_matrix, "State=Visible", "State=HiddenDraft"),
            (0, 350_000_000)
        );
    }

    #[test]
    fn test_document_nodes_default_no_override_canonicalization_and_template_reset_overrides() {
        use dc_bundle::animationspec::animation_override::Animation_override;

        // StaticSet (20:0) and StaticPillSet (30:0) have NO REST SMART_ANIMATE transitions.
        let page_children = vec![
            test_frame(
                "10:0",
                "#Main",
                vec![
                    // Unmodified top-level instance of a variant that carries {"override":"Default"}.
                    test_instance("10:1", "#InstOfDefaultVariant", "20:1", vec![]),
                    // Top-level instance carrying {"override":"Default"} of a plain variant.
                    with_squoosh(
                        test_instance("10:2", "#DefaultOnTopInstance", "20:2", vec![]),
                        SQUOOSH_DEFAULT,
                    ),
                    // Outer instance whose ';'-delimited child I10:3;40:11 resets template 40:11's
                    // DisableAnimations to "Default" without any synthesized REST matrix, while
                    // sibling I10:3;40:12 has "Default" over plain template 40:12.
                    test_instance(
                        "10:3",
                        "#OuterCardReset",
                        "40:1",
                        vec![
                            with_squoosh(
                                test_instance("I10:3;40:11", "#InnerPillReset", "30:1", vec![]),
                                SQUOOSH_DEFAULT,
                            ),
                            with_squoosh(
                                test_instance("I10:3;40:12", "#InnerPillPlain", "30:1", vec![]),
                                SQUOOSH_DEFAULT,
                            ),
                        ],
                    ),
                    // Deep outer instance whose unmodified descendants inherit intermediate template
                    // I50:11;40:11's reset (Some(NoOverride)) and I50:11;40:12's canonicalized None.
                    test_instance(
                        "10:4",
                        "#DeepOuter",
                        "50:1",
                        vec![test_instance(
                            "I10:4;50:11",
                            "#MidCard",
                            "40:1",
                            vec![
                                test_instance(
                                    "I10:4;50:11;40:11",
                                    "#InnerPillReset",
                                    "30:1",
                                    vec![],
                                ),
                                test_instance(
                                    "I10:4;50:11;40:12",
                                    "#InnerPillPlain",
                                    "30:1",
                                    vec![],
                                ),
                            ],
                        )],
                    ),
                ],
            ),
            with_squoosh(
                test_component_set(
                    "20:0",
                    "StaticSet",
                    vec![
                        with_squoosh(
                            test_component("20:1", "S=DefaultVar", vec![]),
                            SQUOOSH_DEFAULT,
                        ),
                        test_component("20:2", "S=Plain", vec![]),
                    ],
                ),
                SQUOOSH_DEFAULT,
            ),
            test_component_set(
                "30:0",
                "StaticPillSet",
                vec![
                    test_component("30:1", "Pill=Idle", vec![]),
                    test_component("30:2", "Pill=Active", vec![]),
                ],
            ),
            test_component_set(
                "40:0",
                "CardSet",
                vec![test_component(
                    "40:1",
                    "Card=One",
                    vec![
                        with_squoosh(
                            test_instance("40:11", "#InnerPillReset", "30:1", vec![]),
                            SQUOOSH_DISABLE,
                        ),
                        test_instance("40:12", "#InnerPillPlain", "30:1", vec![]),
                    ],
                )],
            ),
            test_component_set(
                "50:0",
                "DeepCardSet",
                vec![test_component(
                    "50:1",
                    "Deep=One",
                    vec![test_instance(
                        "50:11",
                        "#MidCard",
                        "40:1",
                        vec![
                            with_squoosh(
                                test_instance("I50:11;40:11", "#InnerPillReset", "30:1", vec![]),
                                SQUOOSH_DEFAULT,
                            ),
                            with_squoosh(
                                test_instance("I50:11;40:12", "#InnerPillPlain", "30:1", vec![]),
                                SQUOOSH_DEFAULT,
                            ),
                        ],
                    )],
                )],
            ),
        ];

        let mut doc = make_test_document(test_file_response(
            "Default NoOverride Canonicalization And Template Reset Doc",
            page_children,
            &[],
            &[],
        ));
        let mut errors = Vec::new();
        let views = doc
            .nodes(&vec![NodeQuery::name("#Main")], &vec![], &mut errors, HiddenNodePolicy::Skip)
            .unwrap();
        assert!(errors.is_empty());

        // 1. Standalone variants, ComponentSet, and top-level instances with "Default" and no REST
        //    transitions canonicalize animation_override to None without spurious overrides_table entries.
        let static_cs = views.get(&NodeQuery::component_set("StaticSet")).unwrap();
        let var_default = views.get(&NodeQuery::variant("S=DefaultVar", "StaticSet")).unwrap();
        let var_plain = views.get(&NodeQuery::variant("S=Plain", "StaticSet")).unwrap();
        assert!(view_animation_override(static_cs).is_none());
        assert!(view_animation_override(var_default).is_none());
        assert!(view_animation_override(var_plain).is_none());

        let main_view = views.get(&NodeQuery::name("#Main")).unwrap();
        let inst_of_default_var = main_view.find_view_by_id("10:1").unwrap();
        let default_on_top_inst = main_view.find_view_by_id("10:2").unwrap();
        assert!(view_animation_override(inst_of_default_var).is_none());
        assert!(view_animation_override(default_on_top_inst).is_none());
        assert!(
            inst_of_default_var.component_info.as_ref().unwrap().overrides_table.is_empty(),
            "Unmodified instance of 'Default'-annotated variant must have empty overrides_table"
        );
        assert!(
            default_on_top_inst.component_info.as_ref().unwrap().overrides_table.is_empty(),
            "Top-level instance with 'Default' and no REST transitions must have empty overrides_table"
        );

        // 2. ';'-delimited instance child resetting active template DisableAnimations without a REST
        //    matrix retains Some(NoOverride) and records the reset in overrides_table, while sibling
        //    with "Default" over plain template canonicalizes to None with empty overrides_table.
        let card_one = views.get(&NodeQuery::variant("Card=One", "CardSet")).unwrap();
        assert!(matches!(
            view_animation_override(card_one.find_view_by_id("40:11").unwrap()),
            Some(Animation_override::DisableAnimations(_))
        ));
        assert!(view_animation_override(card_one.find_view_by_id("40:12").unwrap()).is_none());

        let outer_reset = main_view.find_view_by_id("10:3").unwrap();
        let child_reset = outer_reset.find_view_by_id("I10:3;40:11").unwrap();
        let child_plain_default = outer_reset.find_view_by_id("I10:3;40:12").unwrap();
        assert!(
            matches!(view_animation_override(child_reset), Some(Animation_override::NoOverride(_))),
            "';'-delimited child resetting DisableAnimations without REST matrix must retain Some(NoOverride)"
        );
        assert!(
            matches!(
                overrides_table_anim_override(child_reset, "#InnerPillReset"),
                Some(Animation_override::NoOverride(_))
            ),
            "Resetting active template DisableAnimations must record Some(NoOverride) in overrides_table"
        );
        assert!(view_animation_override(child_plain_default).is_none());
        assert!(
            child_plain_default.component_info.as_ref().unwrap().overrides_table.is_empty(),
            "';'-delimited child with 'Default' over plain template must not bloat overrides_table"
        );

        // 3. Intermediate template I50:11;40:11 inside Deep=One retains Some(NoOverride), and
        //    unmodified deep nested descendant I10:4;50:11;40:11 agrees with its intermediate
        //    template without spurious overrides_table entries.
        let deep_one = views.get(&NodeQuery::variant("Deep=One", "DeepCardSet")).unwrap();
        let mid_template_reset = deep_one.find_view_by_id("I50:11;40:11").unwrap();
        let mid_template_plain = deep_one.find_view_by_id("I50:11;40:12").unwrap();
        assert!(matches!(
            view_animation_override(mid_template_reset),
            Some(Animation_override::NoOverride(_))
        ));
        assert!(mid_template_reset.component_info.as_ref().unwrap().overrides_table.is_empty());
        assert!(view_animation_override(mid_template_plain).is_none());
        assert!(mid_template_plain.component_info.as_ref().unwrap().overrides_table.is_empty());

        let deep_outer = main_view.find_view_by_id("10:4").unwrap();
        let deep_mid = deep_outer.find_view_by_id("I10:4;50:11").unwrap();
        let deep_descendant_reset = deep_outer.find_view_by_id("I10:4;50:11;40:11").unwrap();
        let deep_descendant_plain = deep_outer.find_view_by_id("I10:4;50:11;40:12").unwrap();
        assert!(matches!(
            view_animation_override(deep_descendant_reset),
            Some(Animation_override::NoOverride(_))
        ));
        assert_eq!(
            deep_descendant_reset.style().node_style().animation_override,
            mid_template_reset.style().node_style().animation_override,
            "Unmodified deep nested reset descendant must agree with intermediate template I50:11;40:11"
        );
        assert_eq!(
            deep_descendant_plain.style().node_style().animation_override,
            mid_template_plain.style().node_style().animation_override
        );
        for unmodified in [deep_outer, deep_mid, deep_descendant_reset, deep_descendant_plain] {
            assert!(
                unmodified.component_info.as_ref().unwrap().overrides_table.is_empty(),
                "Unmodified deep nested view {} must have empty overrides_table",
                unmodified.id
            );
        }
    }
}
