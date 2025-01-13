// Copyright 2025 Google LLC
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

use crate::android_interface::convert_response;
use crate::android_interface::{ConvertRequest, ConvertResponse};
use crate::error::Error;
use dc_bundle::definition::{DesignComposeDefinition, DesignComposeDefinitionHeader, NodeQuery};
use figma_import::ProxyConfig;
use figma_import::{ImageContextSession, ServerFigmaDoc};

pub fn fetch_doc(
    id: &str,
    requested_version_id: &str,
    rq: &ConvertRequest,
    proxy_config: &ProxyConfig,
) -> Result<ConvertResponse, Error> {
    let image_session: Option<ImageContextSession> = {
        match &rq.image_session_json {
            Some(json) => match serde_json::from_slice(json) {
                Ok(session) => Some(session),
                Err(_) => None,
            },
            None => None,
        }
    };

    if let Some(mut doc) = figma_import::Document::new_if_changed(
        &rq.figma_api_key,
        id.into(),
        requested_version_id.into(),
        proxy_config,
        rq.last_modified.clone().unwrap_or(String::new()),
        rq.version.clone().unwrap_or(String::new()),
        image_session,
    )? {
        // The document has changed since the version the client has, so we should fetch
        // a new copy.
        let mut error_list: Vec<String> = vec![];
        let views = doc.nodes(
            &rq.queries.iter().map(NodeQuery::name).collect(),
            &rq.ignored_images
                .iter()
                .map(|imgref| (NodeQuery::name(imgref.node.clone()), imgref.images.clone()))
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

        let header = DesignComposeDefinitionHeader::current(
            doc.last_modified().clone(),
            doc.get_name(),
            doc.get_version(),
            doc.get_document_id(),
        );
        let server_doc = ServerFigmaDoc {
            figma_doc: Some(figma_doc),
            errors: error_list,
            branches: doc.branches.clone(),
            project_files: vec![],
        };

        Ok(ConvertResponse {
            convert_response_type: Some(convert_response::ConvertResponseType::Document(
                convert_response::Document {
                    header: Some(header),
                    server_doc: Some(server_doc),
                    // Return the image session as a JSON blob; we might want to encode this differently so we
                    // can be more robust if there's corruption.
                    image_session_json: serde_json::to_vec(&doc.image_session())?,
                },
            )),
        })
    } else {
        Ok(ConvertResponse {
            convert_response_type: Some(convert_response::ConvertResponseType::Unmodified(
                "x".into(),
            )),
        })
    }
}
