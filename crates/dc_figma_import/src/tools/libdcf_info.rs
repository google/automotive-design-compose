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

// Utility library to parse a .dcf file and return its contents.

//! Library for parsing DCF (Design Compose Definition) files.

use dc_bundle::definition_file::decode_dcd_with_header;
use protobuf::CodedInputStream;
use serde::{Deserialize, Serialize};
use std::fs::File;
use std::io::Read;
use std::path::Path;

/// Struct representing the information extracted from a DCF file.
#[derive(Debug, Serialize, Deserialize)]
pub struct DcfInfo {
    /// The Design Compose version.
    pub dc_version: u32,
    /// The unique document ID.
    pub document_id: String,
    /// The Figma version string.
    pub figma_version: String,
    /// The name of the document.
    pub name: String,
    /// The last modified timestamp.
    pub last_modified: String,
    /// The length of the DCD payload.
    pub dcd_length: u64,
}

/// Errors that can occur during DCF parsing.
#[derive(Debug)]
pub enum DcfError {
    /// IO error.
    Io(std::io::Error),
    /// Error from dc_bundle library.
    DcBundle(dc_bundle::Error),
    /// Custom parsing error.
    Parse(String),
}

impl From<std::io::Error> for DcfError {
    fn from(e: std::io::Error) -> Self {
        DcfError::Io(e)
    }
}

impl From<dc_bundle::Error> for DcfError {
    fn from(e: dc_bundle::Error) -> Self {
        DcfError::DcBundle(e)
    }
}

/// Parses a DCF file and returns its information.
///
/// # Arguments
///
/// * `file_path` - The path to the DCF file.
pub fn parse_dcf_info(file_path: &Path) -> Result<DcfInfo, DcfError> {
    let mut file = File::open(file_path)?;
    let mut buffer = Vec::new();
    file.read_to_end(&mut buffer)?;

    let decode_result = decode_dcd_with_header(&buffer);

    match decode_result {
        Ok((header, _doc)) => {
            // Manually parse length prefix to extract dcd_length
            let mut cis = CodedInputStream::from_bytes(&buffer);
            let header_len = cis.read_raw_varint32().map_err(|e| DcfError::Parse(e.to_string()))?;
            cis.skip_raw_bytes(header_len).map_err(|e| DcfError::Parse(e.to_string()))?;
            let dcd_length =
                cis.read_raw_varint32().map_err(|e| DcfError::Parse(e.to_string()))? as u64;

            Ok(DcfInfo {
                dc_version: header.dc_version,
                document_id: header.id,
                figma_version: header.response_version,
                name: header.name,
                last_modified: header.last_modified,
                dcd_length,
            })
        }
        Err(_) => {
            if buffer.len() >= 4 {
                let version_bytes: [u8; 4] = buffer[0..4].try_into().unwrap();
                let version = u32::from_le_bytes(version_bytes);
                if version < 27 {
                    return Err(DcfError::Parse(format!(
                        "DCF version {} < 27, not supported for detailed info",
                        version
                    )));
                }
            }
            Err(DcfError::Parse(format!("Failed to load file {:?}", file_path)))
        }
    }
}
