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

//! Library dependency discovery and resolution.
//!
//! Discovers which external Figma libraries a document depends on by analyzing:
//! - Remote components (`component.remote == true` in the components hash)
//! - Remote variables (`variable.remote == true`)
//! - Variable aliases pointing to external collections
//!
//! Once dependencies are discovered, this module can fetch variables from all
//! library documents to enable complete cross-library alias resolution.
//!
//! ## Limitations
//!
//! - Figma's Team Library API requires different permissions than the Files API
//! - We extract library document IDs from component/variable metadata rather
//!   than using the Team Library endpoint directly
//! - This is a best-effort approach: libraries not discoverable through
//!   components or variables will be missed

use crate::error::Error;
use crate::figma_schema;
use crate::http_client;
use crate::proxy_config::ProxyConfig;
use log::{info, warn};
use std::collections::{HashMap, HashSet};

const BASE_FILE_URL: &str = "https://api.figma.com/v1/files/";

/// Discovers and tracks library dependencies for a Figma document.
#[derive(Clone, Debug)]
pub struct LibraryResolver {
    api_key: String,
    proxy_config: ProxyConfig,
    /// doc_id -> set of library doc IDs it depends on
    dependency_graph: HashMap<String, HashSet<String>>,
    /// library_doc_id -> last_modified timestamp
    library_versions: HashMap<String, String>,
}

impl LibraryResolver {
    pub fn new(api_key: String, proxy_config: ProxyConfig) -> Self {
        LibraryResolver {
            api_key,
            proxy_config,
            dependency_graph: HashMap::new(),
            library_versions: HashMap::new(),
        }
    }

    /// Analyze a document's components hash to discover library dependencies.
    ///
    /// Looks at components that have a `key` field — the key format for
    /// remote components includes the source document ID. We extract unique
    /// document IDs from the component responses that point to external files.
    pub fn discover_component_dependencies(
        &mut self,
        primary_doc_id: &str,
        components: &HashMap<String, figma_schema::Component>,
    ) -> HashSet<String> {
        let mut library_doc_ids: HashSet<String> = HashSet::new();

        for (_id, component) in components {
            if component.key.is_empty() {
                continue;
            }
            if component.key.contains('/') {
                if let Some(doc_id) = component.key.split('/').next() {
                    if doc_id != primary_doc_id {
                        library_doc_ids.insert(doc_id.to_string());
                    }
                }
            }
        }

        if !library_doc_ids.is_empty() {
            info!(
                "Discovered {} library dependencies for doc {}",
                library_doc_ids.len(),
                primary_doc_id
            );
            self.dependency_graph
                .entry(primary_doc_id.to_string())
                .or_default()
                .extend(library_doc_ids.iter().cloned());
        }

        library_doc_ids
    }

    /// Analyze a document's variables response to discover variable library dependencies.
    ///
    /// Remote variables have `remote: true` in their metadata. The library
    /// document ID can be extracted from the variable's `key` field.
    pub fn discover_variable_dependencies(
        &mut self,
        primary_doc_id: &str,
        variables_response: &figma_schema::VariablesResponse,
    ) -> HashSet<String> {
        let mut library_doc_ids: HashSet<String> = HashSet::new();

        for (_id, collection) in &variables_response.meta.variable_collections {
            if collection.remote {
                if collection.key.contains('/') {
                    if let Some(doc_id) = collection.key.split('/').next() {
                        if doc_id != primary_doc_id {
                            library_doc_ids.insert(doc_id.to_string());
                        }
                    }
                }
            }
        }

        for (_id, variable) in &variables_response.meta.variables {
            let common = match variable {
                figma_schema::Variable::Boolean { common, .. }
                | figma_schema::Variable::Float { common, .. }
                | figma_schema::Variable::String { common, .. }
                | figma_schema::Variable::Color { common, .. } => common,
            };

            if common.remote {
                if common.key.contains('/') {
                    if let Some(doc_id) = common.key.split('/').next() {
                        if doc_id != primary_doc_id {
                            library_doc_ids.insert(doc_id.to_string());
                        }
                    }
                }
            }
        }

        if !library_doc_ids.is_empty() {
            self.dependency_graph
                .entry(primary_doc_id.to_string())
                .or_default()
                .extend(library_doc_ids.iter().cloned());
        }

        library_doc_ids
    }

    /// Fetch variables from a specific library document.
    pub fn fetch_library_variables(
        &self,
        library_doc_id: &str,
    ) -> Result<figma_schema::VariablesResponse, Error> {
        let url = format!("{}{}/variables/local", BASE_FILE_URL, library_doc_id);
        let body = http_client::http_fetch(&self.api_key, url, &self.proxy_config)?;
        let response: figma_schema::VariablesResponse = serde_json::from_str(&body)?;
        Ok(response)
    }

    /// Fetch variables from all known library dependencies concurrently.
    ///
    /// Returns a map of (library_doc_id -> VariablesResponse).
    pub fn fetch_all_library_variables(
        &self,
        primary_doc_id: &str,
    ) -> HashMap<String, figma_schema::VariablesResponse> {
        let mut results = HashMap::new();

        let library_ids = match self.dependency_graph.get(primary_doc_id) {
            Some(ids) => ids.clone(),
            None => return results,
        };

        if library_ids.is_empty() {
            return results;
        }

        info!("Fetching variables from {} library dependencies", library_ids.len());

        // Build batch requests for concurrent fetch
        let requests: Vec<http_client::BatchRequest> = library_ids
            .iter()
            .map(|doc_id| http_client::BatchRequest {
                id: doc_id.clone(),
                url: format!("{}{}/variables/local", BASE_FILE_URL, doc_id),
            })
            .collect();

        let responses = http_client::http_fetch_batch(&self.api_key, requests, &self.proxy_config);

        for response in responses {
            match response.result {
                Ok(body) => match serde_json::from_str::<figma_schema::VariablesResponse>(&body) {
                    Ok(vars) => {
                        results.insert(response.id, vars);
                    }
                    Err(e) => {
                        warn!("Failed to parse variables for library doc {}: {:?}", response.id, e);
                    }
                },
                Err(e) => {
                    warn!("Failed to fetch variables for library doc {}: {:?}", response.id, e);
                }
            }
        }

        info!(
            "Successfully fetched variables from {}/{} library dependencies",
            results.len(),
            library_ids.len()
        );

        results
    }

    /// Get the dependency graph.
    pub fn dependencies(&self) -> &HashMap<String, HashSet<String>> {
        &self.dependency_graph
    }

    /// Get all library doc IDs for a primary document.
    pub fn library_ids(&self, primary_doc_id: &str) -> HashSet<String> {
        self.dependency_graph.get(primary_doc_id).cloned().unwrap_or_default()
    }

    /// Register a known library dependency (discovered externally).
    pub fn add_dependency(&mut self, primary_doc_id: &str, library_doc_id: &str) {
        self.dependency_graph
            .entry(primary_doc_id.to_string())
            .or_default()
            .insert(library_doc_id.to_string());
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_add_dependency() {
        let mut resolver = LibraryResolver::new("key".to_string(), ProxyConfig::None);
        resolver.add_dependency("doc1", "lib1");
        resolver.add_dependency("doc1", "lib2");

        let libs = resolver.library_ids("doc1");
        assert_eq!(libs.len(), 2);
        assert!(libs.contains("lib1"));
        assert!(libs.contains("lib2"));
    }

    #[test]
    fn test_no_dependencies() {
        let resolver = LibraryResolver::new("key".to_string(), ProxyConfig::None);
        assert!(resolver.library_ids("doc1").is_empty());
    }
}
