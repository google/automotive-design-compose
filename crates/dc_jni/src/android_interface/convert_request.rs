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

use crate::error::Error;
use dc_bundle::android_interface::{convert_response, ConvertRequest, ConvertResponse};
use dc_bundle::definition::NodeQuery;
use dc_bundle::design_compose_definition::{
    DesignComposeDefinition, DesignComposeDefinitionHeader,
};
use dc_bundle::figma_doc::ServerFigmaDoc;
use dc_figma_import::HiddenNodePolicy;
use dc_figma_import::ImageContextSession;
use dc_figma_import::ProxyConfig;
use log::info;
use std::time::Instant;

pub fn fetch_doc(
    id: &str,
    requested_version_id: &str,
    rq: &ConvertRequest,
    proxy_config: &ProxyConfig,
) -> Result<ConvertResponse, Error> {
    let fetch_start = Instant::now();

    let image_session: Option<ImageContextSession> =
        rq.image_session_json.as_ref().and_then(|json| serde_json::from_slice(json).ok());

    let start = Instant::now();
    if let Some(mut doc) = dc_figma_import::Document::new_if_changed(
        &rq.figma_api_key,
        id.into(),
        requested_version_id.into(),
        proxy_config,
        rq.last_modified.clone().unwrap_or_default(),
        rq.version.clone().unwrap_or_default(),
        image_session,
    )? {
        info!("fetch_doc({}): Figma API fetch took {:?}", id, start.elapsed());

        // The document has changed since the version the client has, so we should fetch
        // a new copy.
        let mut error_list: Vec<String> = vec![];
        let start = Instant::now();
        let views = doc.nodes(
            &rq.queries.iter().map(NodeQuery::name).collect(),
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
        let figma_doc = DesignComposeDefinition::new_with_details(
            views,
            encoded_images,
            doc.component_sets().clone(),
            variable_map,
        );
        info!("fetch_doc({}): Definition build took {:?}", id, start.elapsed());

        let header = DesignComposeDefinitionHeader::current(
            doc.last_modified().clone(),
            doc.get_name(),
            doc.get_version(),
            doc.get_document_id(),
        );
        let server_doc = ServerFigmaDoc {
            figma_doc: Some(figma_doc).into(),
            errors: error_list,
            branches: doc.branches.clone(),
            project_files: vec![],
            ..Default::default()
        };

        let start = Instant::now();
        let image_session_json = serde_json::to_vec(&doc.image_session())?;
        info!("fetch_doc({}): Image session serialization took {:?}", id, start.elapsed());

        info!("fetch_doc({}): Total fetch pipeline took {:?}", id, fetch_start.elapsed());

        Ok(ConvertResponse {
            convert_response_type: Some(convert_response::Convert_response_type::Document(
                convert_response::Document {
                    header: Some(header).into(),
                    server_doc: Some(server_doc).into(),
                    // Return the image session as a JSON blob; we might want to encode this differently so we
                    // can be more robust if there's corruption.
                    image_session_json,
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
