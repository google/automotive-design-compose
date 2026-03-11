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

//! Utility program to parse a .dcf file and print its contents.
//! By default prints the header and file info.
//! Provide the optional `--node` or `-n` switch to dump figma document using
//! that node as root.
//! Example:
//! dcf_info -- tests/layout-unit-tests.dcf`
//! or
//! dcf_info -- tests/layout-unit-tests.dcf -n HorizontalFill`

use crate::tools::libdcf_info::parse_dcf_info;
use crate::tools::libdcf_info::DcfError;
use clap::Parser;
use dc_bundle::definition_file::load_design_def;
use serde::Serialize;
use std::fs::File;
use std::io::{self, Read, Write};
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
impl From<dc_bundle::Error> for ParseError {
    fn from(e: dc_bundle::Error) -> Self {
        eprintln!("Error during deserialization: {:?}", e);
        ParseError(format!("Error during deserialization: {:?}", e))
    }
}
impl From<DcfError> for ParseError {
    fn from(e: DcfError) -> Self {
        match e {
            DcfError::Io(e) => e.into(),
            DcfError::DcBundle(e) => e.into(),
            DcfError::Parse(s) => ParseError(s),
        }
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
    // Output format as JSON
    #[clap(long)]
    pub json: bool,
    // Output file path
    #[clap(long, short)]
    pub output: Option<std::path::PathBuf>,
}

pub fn dcf_info(args: Args) -> Result<(), ParseError> {
    let file_path = &args.dcf_file;
    let node = args.node;

    let mut writer: Box<dyn Write> = match args.output {
        Some(path) => Box::new(File::create(path)?),
        None => Box::new(io::stdout()),
    };

    if args.json {
        match parse_dcf_info(file_path) {
            Ok(info) => match serde_json::to_string(&info) {
                Ok(json_string) => {
                    writeln!(writer, "{}", json_string)?;
                }
                Err(e) => {
                    #[derive(Serialize)]
                    struct ErrorWrapper {
                        error: String,
                    }
                    let err_json = serde_json::to_string(&ErrorWrapper {
                        error: format!("Error serializing: {:?}", e),
                    })
                    .unwrap();
                    writeln!(writer, "{}", err_json)?;
                }
            },
            Err(e) => {
                #[derive(Serialize)]
                struct ErrorDetail {
                    message: String,
                }
                #[derive(Serialize)]
                struct ErrorWrapper {
                    error: ErrorDetail,
                }

                let err_json = serde_json::to_string(&ErrorWrapper {
                    error: ErrorDetail { message: format!("{:?}", e) },
                })
                .unwrap();
                writeln!(writer, "{}", err_json)?;
            }
        }
        return Ok(());
    }

    let load_result = load_design_def(file_path);
    if let Ok((header, doc)) = load_result {
        writeln!(writer, "Deserialized file")?;
        writeln!(writer, "  DC Version: {}", header.dc_version)?;
        writeln!(writer, "  Doc ID: {}", header.id)?;
        writeln!(writer, "  Figma Version: {}", header.response_version)?;
        writeln!(writer, "  Name: {}", header.name)?;
        writeln!(writer, "  Last Modified: {}", header.last_modified)?;

        if args.varinfo {
            if let Some(variable_map) = doc.variable_map.as_ref() {
                writeln!(writer, "Variables: {:#?}", variable_map)?;
            }
        }

        writeln!(writer, "--- DUMPING VIEWS ---")?;
        for (id, view) in &doc.views {
            if let Some(style) = view.style.as_ref() {
                if let Some(node) = style.node_style.as_ref() {
                    if let Some(anim) = node.animation_override.as_ref() {
                        use dc_bundle::animationspec::animation_override::Animation_override as DcAnimOverride;
                        if let Some(DcAnimOverride::Custom(spec)) = &anim.animation_override {
                            writeln!(writer, "View ID: {} Name: {}", id, view.name)?;
                            writeln!(
                                writer,
                                "  Custom Timeline Keys: {:?}",
                                spec.custom_keyframe_data.keys()
                            )?;
                            for (k, v) in &spec.custom_keyframe_data {
                                writeln!(writer, "    {}: {}", k, v)?;
                            }
                        }
                    }
                }
            }
        }

        if let Some(node) = node {
            writeln!(writer, "Dumping file from node: {}:", node)?;
            if let Some(view) =
                doc.views.get(&dc_bundle::definition::NodeQuery::name(&node).encode())
            {
                // NOTE: uses format and Debug implementation to pretty print the node and all children.
                // See: https://doc.rust-lang.org/std/fmt/#usage
                writeln!(writer, "{:#?}", view)?;
            } else {
                return Err(ParseError(format!("Node: {} not found in document.", node)));
            }
        }
    } else {
        // If loading failed, try to read just the first byte to determine the DC version
        let mut document_file = File::open(file_path)?;
        let mut buffer = [0; mem::size_of::<u32>()]; // Create a byte buffer to fit an integer
        document_file.read_exact(&mut buffer)?; // Read exactly the number of bytes for an integer into the buffer

        let version = u32::from_le_bytes(buffer);
        if version < 27 {
            writeln!(writer, "DC Version: {}", version)?;
            writeln!(
                writer,
                "DCF files version < 27 do not have additional information to parse."
            )?;
        } else {
            eprintln!("Failed to load file {:?}", file_path);
            return Err(ParseError(format!("Failed to load file {:?}", file_path)));
        }
    }

    Ok(())
}
