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

use std::env;
use std::io::{Error, ErrorKind};

use crate::{proxy_config::ProxyConfig, Document};
/// Utility program to fetch a doc and serialize it to file
use clap::Parser;
use dc_bundle::definition::NodeQuery;
use dc_bundle::definition_file::save_design_def;
use dc_bundle::design_compose_definition::DesignComposeDefinition;
use dc_bundle::design_compose_definition::DesignComposeDefinitionHeader;

#[derive(Debug)]
#[allow(dead_code)]
pub struct ConvertError(String);
impl From<crate::Error> for ConvertError {
    fn from(e: crate::Error) -> Self {
        eprintln!("Error during Figma conversion: {:?}", e);
        ConvertError(format!("Internal Server Error during Figma conversion: {:?}", e))
    }
}
impl From<dc_bundle::Error> for ConvertError {
    fn from(e: dc_bundle::Error) -> Self {
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
    pub doc_id: String,
    /// Figma Document Version ID to fetch and convert
    #[arg(short, long)]
    pub version_id: Option<String>,
    /// Figma API key to use for Figma requests
    #[arg(short, long, env("FIGMA_API_KEY"))]
    pub api_key: Option<String>,
    /// HTTP proxy server - <host>:<port>
    #[arg(long)]
    pub http_proxy: Option<String>,
    /// Root nodes to find in the doc and convert
    #[arg(short, long)]
    pub nodes: Vec<String>,
    /// Output file to write serialized doc into
    #[arg(short, long)]
    pub output: std::path::PathBuf,
}

//Loads a Figma access token from either the FIGMA_ACCESS_TOKEN environment variable or a file located at ~/.config/figma_access_token.
// Returns a Result<String, Error> where:
//  Ok(token) contains the loaded token as a String, with leading and trailing whitespace removed.
//  Err(Error) indicates an error occurred during the loading process. Possible errors include:
//      Failure to read the HOME environment variable.
//      Failure to read the token file (e.g., file not found, permission denied).
// This function prioritizes reading the token from the environment variable. If it's not set, it attempts to read the token from the specified file. The file path is constructed using the user's home directory.
pub fn load_figma_token() -> Result<String, Error> {
    match env::var("FIGMA_ACCESS_TOKEN") {
        Ok(token) => Ok(token),
        Err(_) => {
            let home_dir = match env::var("HOME") {
                Ok(val) => val,
                Err(_) => return Err(Error::new(ErrorKind::Other, "Could not read HOME from env")),
            };
            let config_path =
                std::path::Path::new(&home_dir).join(".config").join("figma_access_token");
            let token = match std::fs::read_to_string(config_path) {
                Ok(token) => token.trim().to_string(),
                Err(e) => {
                    return Err(Error::new(
                        ErrorKind::NotFound,
                        format!(
                            "Could not read Figma token from ~/.config/figma_access_token: {}",
                            e
                        ),
                    ))
                }
            };
            Ok(token)
        }
    }
}

pub fn build_definition(
    doc: &mut Document,
    nodes: &Vec<String>,
) -> Result<DesignComposeDefinition, ConvertError> {
    let mut error_list = Vec::new();
    // Convert the requested nodes from the Figma doc.
    let views = doc.nodes(
        &nodes.iter().map(|name| NodeQuery::name(name)).collect(),
        &Vec::new(),
        &mut error_list,
    )?;
    for error in error_list {
        eprintln!("Warning: {error}");
    }
    let variable_map = doc.build_variable_map();

    // Build the serializable doc structure
    Ok(DesignComposeDefinition::new_with_details(
        views,
        doc.encoded_image_map(),
        doc.component_sets().clone(),
        variable_map,
    ))
}

pub fn fetch(args: Args) -> Result<(), ConvertError> {
    let proxy_config: ProxyConfig = match args.http_proxy {
        Some(x) => ProxyConfig::HttpProxyConfig(x),
        None => ProxyConfig::None,
    };

    // If the API Key wasn't provided on the path or via env var, load it from env or ~/.config/figma_access_token
    let api_key = match args.api_key {
        Some(x) => x,
        None => load_figma_token()?,
    };

    let mut doc: Document = Document::new(
        api_key.as_str(),
        args.doc_id,
        args.version_id.unwrap_or(String::new()),
        &proxy_config,
        None,
    )?;

    let dc_definition = build_definition(&mut doc, &args.nodes)?;

    println!("Fetched document");
    println!("  DC Version: {}", DesignComposeDefinitionHeader::current_version());
    println!("  Doc ID: {}", doc.get_document_id());
    println!("  Version: {}", doc.get_version());
    println!("  Name: {}", doc.get_name());
    println!("  Last Modified: {}", doc.last_modified().clone());

    // We don't bother with serialization of image sessions with this tool.
    save_design_def(
        args.output,
        &DesignComposeDefinitionHeader::current(
            doc.last_modified().clone(),
            doc.get_name().clone(),
            doc.get_version().clone(),
            doc.get_document_id().clone(),
        ),
        &dc_definition,
    )?;
    Ok(())
}
