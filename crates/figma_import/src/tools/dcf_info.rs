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

use crate::load_design_def;
use clap::Parser;

#[derive(Debug)]
#[allow(dead_code)]
pub struct ParseError(String);
impl From<bincode::Error> for ParseError {
    fn from(e: bincode::Error) -> Self {
        eprintln!("Error during deserialization: {:?}", e);
        ParseError(format!("Error during deserialization: {:?}", e))
    }
}
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

#[derive(Parser, Debug)]
pub struct Args {
    // Path to the .dcf file to deserialize
    pub dcf_file: std::path::PathBuf,
    // Optional string argument to dump file structure from a given node root.
    #[clap(long, short)]
    pub node: Option<String>,
}

pub fn dcf_info(args: Args) -> Result<(), ParseError> {
    let file_path = &args.dcf_file;
    let node = args.node;

    let (header, doc) = load_design_def(file_path)?;

    println!("Deserialized file");
    println!("  Header Version: {}", header.version);
    println!("  Doc ID: {}", doc.id);
    println!("  Figma Version: {}", doc.version);
    println!("  Name: {}", doc.name);
    println!("  Last Modified: {}", doc.last_modified);

    if let Some(node) = node {
        println!("Dumping file from node: {}:", node);
        if let Some(view) = doc.views.get(&crate::NodeQuery::name(&node)) {
            // NOTE: uses format and Debug implementation to pretty print the node and all children.
            // See: https://doc.rust-lang.org/std/fmt/#usage
            println!("{:#?}", view);
        } else {
            return Err(ParseError(format!("Node: {} not found in document.", node)));
        }
    }

    Ok(())
}
