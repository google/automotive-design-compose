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

/// Utility program to fetch a doc and serialize it to file
use clap::Parser;
use figma_import::{Document, NodeQuery, SerializedDesignDoc};
#[derive(Debug)]
struct ConvertError(String);
impl From<figma_import::Error> for ConvertError {
    fn from(e: figma_import::Error) -> Self {
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
struct Args {
    /// Figma Document ID to fetch and convert
    #[arg(short, long)]
    doc_id: String,
    /// Figma API key to use for Figma requests
    #[arg(short, long)]
    api_key: String,
    /// Root nodes to find in the doc and convert
    #[arg(short, long)]
    nodes: Vec<String>,
    /// Output file to write serialized doc into
    #[arg(short, long)]
    output: std::path::PathBuf,
}
fn fetch_impl(args: Args) -> Result<(), ConvertError> {
    let mut doc = Document::new(args.api_key.as_str(), args.doc_id, None)?;
    let mut error_list = Vec::new();
    // Convert the requested nodes from the Figma doc.
    let nodes = doc.nodes(
        &args.nodes.iter().map(|name| NodeQuery::name(name)).collect(),
        &Vec::new(),
        &mut error_list,
    )?;
    for error in error_list {
        eprintln!("Warning: {error}");
    }
    // Build the serializable doc structure
    let serializable_doc = SerializedDesignDoc {
        nodes,
        component_sets: doc.component_sets().clone(),
        images: doc.encoded_image_map(),
        last_modified: doc.last_modified().clone(),
        name: doc.get_name(),
        version: doc.get_version(),
        id: doc.get_document_id(),
    };
    // We don't bother with serialization headers or image sessions with
    // this tool.
    let output = std::fs::File::create(args.output)?;
    bincode::serialize_into(output, &serializable_doc)?;
    Ok(())
}
fn main() {
    let args = Args::parse();
    if let Err(e) = fetch_impl(args) {
        eprintln!("Fetch failed: {:?}", e);
        std::process::exit(1);
    }
}
