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

use std::io::Write;

use crate::{DesignComposeDefinition, DesignComposeDefinitionHeader, Document, ProxyConfig};
/// Utility program to fetch a doc and serialize it to file
use clap::Parser;
use dc_bundle::legacy_definition::element::node::NodeQuery;

#[derive(Debug)]
#[allow(dead_code)]
pub struct ConvertError(String);
impl From<crate::Error> for ConvertError {
    fn from(e: crate::Error) -> Self {
        eprintln!("Error during Figma conversion: {:?}", e);
        ConvertError(format!("Internal Server Error during Figma conversion: {:?}", e))
    }
}
impl From<bincode::Error> for ConvertError {
    fn from(e: bincode::Error) -> Self {
        eprintln!("Error during serialization: {:?}", e);
        ConvertError(format!("Error during serialization: {:?}", e))
    }
}
impl From<serde_json::Error> for ConvertError {
    fn from(e: serde_json::Error) -> Self {
        eprintln!("Error during image session serialization: {:?}", e);
        ConvertError(format!("Error during image session serialization: {:?}", e))
    }
}
impl From<std::io::Error> for ConvertError {
    fn from(e: std::io::Error) -> Self {
        eprintln!("Error creating output file: {:?}", e);
        ConvertError(format!("Error creating output file: {:?}", e))
    }
}
#[derive(Parser, Debug)]
pub struct Args {
    /// Figma Document ID to fetch and convert
    #[arg(short, long)]
    doc_id: String,
    /// Figma Document Version ID to fetch and convert
    #[arg(short, long)]
    version_id: Option<String>,
    /// Figma API key to use for Figma requests
    #[arg(short, long)]
    api_key: String,
    /// HTTP proxy server - <host>:<port>
    #[arg(long)]
    http_proxy: Option<String>,
    /// Root nodes to find in the doc and convert
    #[arg(short, long)]
    nodes: Vec<String>,
    /// Output file to write serialized doc into
    #[arg(short, long)]
    output: std::path::PathBuf,
}

pub fn fetch(args: Args) -> Result<(), ConvertError> {
    let proxy_config: ProxyConfig = match args.http_proxy {
        Some(x) => ProxyConfig::HttpProxyConfig(x),
        None => ProxyConfig::None,
    };
    let mut doc = Document::new(
        args.api_key.as_str(),
        args.doc_id,
        args.version_id.unwrap_or(String::new()),
        &proxy_config,
        None,
    )?;
    let mut error_list = Vec::new();
    // Convert the requested nodes from the Figma doc.
    let views = doc.nodes(
        &args.nodes.iter().map(|name| NodeQuery::name(name)).collect(),
        &Vec::new(),
        &mut error_list,
    )?;
    for error in error_list {
        eprintln!("Warning: {error}");
    }

    let variable_map = doc.build_variable_map();

    // Build the serializable doc structure
    let serializable_doc = DesignComposeDefinition {
        views,
        component_sets: doc.component_sets().clone(),
        images: doc.encoded_image_map(),
        last_modified: doc.last_modified().clone(),
        name: doc.get_name(),
        version: doc.get_version(),
        id: doc.get_document_id(),
        variable_map: variable_map,
    };
    println!("Fetched document");
    println!("  DC Version: {}", DesignComposeDefinitionHeader::current().version);
    println!("  Doc ID: {}", doc.get_document_id());
    println!("  Version: {}", doc.get_version());
    println!("  Name: {}", doc.get_name());
    println!("  Last Modified: {}", doc.last_modified().clone());
    // We don't bother with serialization of image sessions with this tool.
    let mut output = std::fs::File::create(args.output)?;
    let header = bincode::serialize(&DesignComposeDefinitionHeader::current())?;
    let doc = bincode::serialize(&serializable_doc)?;
    output.write_all(header.as_slice())?;
    output.write_all(doc.as_slice())?;
    Ok(())
}
