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

// Utility program to parse a .dcf file and print out the header and version info for the file.

use bincode::Options;
use clap::Parser;
use figma_import::{SerializedDesignDoc, SerializedDesignDocHeader};

#[derive(Debug)]
struct ParseError(String);
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

#[derive(Parser, Debug)]
struct Args {
    // Path to the .dcf file to deserialize
    dcf_file: std::path::PathBuf,
}

fn dcf_info(args: Args) -> Result<(), ParseError> {
    let file_contents = std::fs::read(args.dcf_file)?;
    let bytes = file_contents.as_slice();

    // Deserialize the header
    let header: SerializedDesignDocHeader = bincode::deserialize(bytes)?;
    let header_size = bincode::serialized_size(&header)? as usize;

    println!("Deserialized header");
    println!("  DC Version: {}", header.version);

    let current_version = SerializedDesignDocHeader::current().version;
    if header.version != current_version {
        println!(
            "Header version {} does not match current version {}, aborting",
            header.version, current_version
        );
        return Err(ParseError("Version mismatch".to_string()));
    }

    // Deserialize the document
    let doc: SerializedDesignDoc = bincode::Options::deserialize(
        bincode::Options::with_limit(
            bincode::config::DefaultOptions::new().with_fixint_encoding().allow_trailing_bytes(),
            100 * 1024 * 1024, // Max 100MB
        ),
        &bytes[header_size..],
    )?;

    println!("Deserialized file");
    println!("  Doc ID: {}", doc.id);
    println!("  Figma Version: {}", doc.version);
    println!("  Name: {}", doc.name);
    println!("  Last Modified: {}", doc.last_modified);
    Ok(())
}

fn main() {
    let args = Args::parse();
    if let Err(e) = dcf_info(args) {
        eprintln!("dcf_info failed: {:?}", e);
        std::process::exit(1);
    }
}
