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

use crate::{Document, ImageContextSession, ServerFigmaDoc};
use dc_bundle::definition::{DesignComposeDefinition, NodeQuery};
use dc_bundle::legacy_definition::DesignComposeDefinitionHeader;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
struct IgnoredImage<'r> {
    node: &'r str,
    images: Vec<String>,
}

fn serde_ok_or_none<'de, D, T>(deserializer: D) -> Result<Option<T>, D::Error>
where
    D: serde::Deserializer<'de>,
    T: Deserialize<'de>,
{
    let v = serde_json::Value::deserialize(deserializer)?;
    Ok(T::deserialize(v).ok())
}

// Proxy configuration.
#[derive(Debug, Clone)]
pub enum ProxyConfig {
    // HTTP Proxy Config in <host>:<port> format
    HttpProxyConfig(String),
    None,
}

#[derive(Serialize, Deserialize)]
pub struct ConvertRequest<'r> {
    figma_api_key: &'r str,
    // Node names
    pub queries: Vec<&'r str>,
    // Ignored images
    ignored_images: Vec<IgnoredImage<'r>>,

    // Last modified comes from the previously fetched document and is
    // used to avoid fetching the same doc version over and over.
    last_modified: Option<String>,

    // The version also comes from the previously fetched document. When certain
    // properties of a Figma doc like a new branch changes, the version gets
    // updated but the last_modified field does not.
    version: Option<String>,

    // Image session also comes from the previously fetched document. We allow this
    // to fail decoding, in case we're receiving a request from a client that has an
    // image session generated by an old version.
    #[serde(deserialize_with = "serde_ok_or_none")]
    image_session: Option<ImageContextSession>,
}

impl<'r> ConvertRequest<'r> {
    pub fn new(figma_api_key: &'r str, queries: Vec<&'r str>, version: Option<String>) -> Self {
        Self {
            figma_api_key,
            queries,
            ignored_images: Vec::new(),
            last_modified: None,
            version,
            image_session: None,
        }
    }
}

#[derive(Serialize, Deserialize)]
pub enum ConvertResponse {
    Document(Vec<u8>),
    Unmodified(String),
}

pub fn fetch_doc(
    id: &str,
    requested_version_id: &str,
    rq: &ConvertRequest,
    proxy_config: &ProxyConfig,
) -> Result<ConvertResponse, crate::Error> {
    if let Some(mut doc) = Document::new_if_changed(
        rq.figma_api_key,
        id.into(),
        requested_version_id.into(),
        proxy_config,
        rq.last_modified.clone().unwrap_or(String::new()),
        rq.version.clone().unwrap_or(String::new()),
        rq.image_session.clone(),
    )? {
        // The document has changed since the version the client has, so we should fetch
        // a new copy.
        let mut error_list: Vec<String> = vec![];
        let views = doc.nodes(
            &rq.queries.iter().map(NodeQuery::name).collect(),
            &rq.ignored_images
                .iter()
                .map(|imgref| (NodeQuery::name(imgref.node), imgref.images.clone()))
                .collect(),
            &mut error_list,
        )?;

        let variable_map = doc.build_variable_map();

        let figma_doc = DesignComposeDefinition::new(
            views,
            doc.encoded_image_map(),
            doc.component_sets().clone(),
            variable_map,
        );
        let mut response = bincode::serialize(&DesignComposeDefinitionHeader::current(
            doc.last_modified().clone(),
            doc.get_name(),
            doc.get_version(),
            doc.get_document_id(),
        ))?;
        response.append(&mut bincode::serialize(&ServerFigmaDoc {
            figma_doc: Some(figma_doc),
            errors: error_list,
            branches: doc.branches.clone(),
            project_files: vec![],
        })?);

        // Return the image session as a JSON blob; we might want to encode this differently so we
        // can be more robust if there's corruption.
        response.append(&mut serde_json::to_vec(&doc.image_session())?);

        Ok(ConvertResponse::Document(response))
    } else {
        Ok(ConvertResponse::Unmodified("x".into()))
    }
}
