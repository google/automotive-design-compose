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

// Utility program to parse a .dcf file and print its contents.
// By default prints the header and file info.
// Provide the optional `--node` or `-n` switch to dump figma document using
// that node as root.
// Example:
// `cargo run --bin dcf_info --features="dcf_info" -- tests/layout-unit-tests.dcf`
// or
// `cargo run --bin dcf_info --features="dcf_info" -- tests/layout-unit-tests.dcf -n HorizontalFill`

use clap::Parser;
use dc_bundle::definition_file::load_design_def;
use std::fs::File;
use std::io::Read;
use std::mem;

#[derive(Debug)]
#[allow(dead_code)]
pub struct ParseError(String);

impl From<std::io::Error> for ParseError {
    fn from(e: std::io::Error) -> Self {
        eprintln!("Error opening file: {:?}", e);
        ParseError(format!("Error opening file: {:?}", e))
    }
}
impl From<crate::Error> for ParseError {
    fn from(e: crate::Error) -> Self {
        ParseError(format!("Figma Import Error: {:?}", e))
    }
}
impl From<dc_bundle::Error> for ParseError {
    fn from(e: dc_bundle::Error) -> Self {
        eprintln!("Error during deserialization: {:?}", e);
        ParseError(format!("Error during deserialization: {:?}", e))
    }
}

#[derive(Parser, Debug)]
pub struct Args {
    // Path to the .dcf file to deserialize
    pub dcf_file: std::path::PathBuf,
    // Optional string argument to dump file structure from a given node root.
    #[clap(long, short)]
    pub node: Option<String>,
    #[clap(long)]
    pub varinfo: bool,
}

pub fn dcf_info(args: Args) -> Result<(), ParseError> {
    let file_path = &args.dcf_file;
    let node = args.node;

    let load_result = load_design_def(file_path);
    if let Ok((header, doc)) = load_result {
        println!("Deserialized file");
        println!("  DC Version: {}", header.dc_version);
        println!("  Doc ID: {}", header.id);
        println!("  Figma Version: {}", header.response_version);
        println!("  Name: {}", header.name);
        println!("  Last Modified: {}", header.last_modified);

        if args.varinfo {
            if let Some(variable_map) = doc.variable_map.as_ref() {
                println!("Variables: {:#?}", variable_map);
            }
        }

        if let Some(node) = node {
            println!("Dumping file from node: {}:", node);
            if let Some(view) = doc.views.get(&crate::NodeQuery::name(&node).encode()) {
                // NOTE: uses format and Debug implementation to pretty print the node and all children.
                // See: https://doc.rust-lang.org/std/fmt/#usage
                println!("{:#?}", view);
            } else {
                return Err(ParseError(format!("Node: {} not found in document.", node)));
            }
        }
    } else {
        // If loading failed, try to read just the first byte to determine the DC version
        let mut document_file = File::open(&file_path)?;
        let mut buffer = [0; mem::size_of::<u32>()]; // Create a byte buffer to fit an integer
        document_file.read_exact(&mut buffer)?; // Read exactly the number of bytes for an integer into the buffer

        let version = u32::from_le_bytes(buffer);
        if version < 27 {
            println!("DC Version: {}", version);
            println!("DCF files version < 27 do not have additional information to parse.");
        } else {
            println!("Failed to load file {:?}", file_path);
        }
    }

    Ok(())
}
