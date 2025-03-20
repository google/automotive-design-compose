/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use crate::design_compose_definition::{DesignComposeDefinition, DesignComposeDefinitionHeader};
use crate::Error;
use protobuf::{CodedInputStream, Message};

use std::fs::File;
use std::io::{Read, Write};
use std::path::Path;

pub fn encode_dcd_with_header(
    header: &DesignComposeDefinitionHeader,
    doc: &DesignComposeDefinition,
) -> Result<Vec<u8>, Error> {
    let mut encoded = header
        .write_length_delimited_to_bytes()
        .map_err(|e| Error::ProtobufWriteError(format!("Failed to write header: {}", e)))?;
    encoded.append(
        &mut doc
            .write_length_delimited_to_bytes()
            .map_err(|e| Error::ProtobufWriteError(format!("Failed to write definition: {}", e)))?,
    );
    Ok(encoded)
}

pub fn decode_dcd_with_header(
    data: &[u8],
) -> Result<(DesignComposeDefinitionHeader, DesignComposeDefinition), Error> {
    let mut cis = CodedInputStream::from_bytes(data);
    let header_len = cis.read_raw_varint32().map_err(|_| Error::DecodeError())?;
    let header_limit = cis.push_limit(header_len as u64).map_err(|_| Error::DecodeError())?;
    let header = DesignComposeDefinitionHeader::parse_from(&mut cis)
        .map_err(|e| Error::DCDLoadError(format!("Failed to parse header: {}", e)))?;
    cis.pop_limit(header_limit);

    // Ensure the version of the document matches this version of automotive design compose.
    if header.dc_version != DesignComposeDefinitionHeader::current_version() {
        return Err(Error::DCDLoadError(format!(
            "DesignComposeDefinition incorrect version. Expected {} Found: {}",
            DesignComposeDefinitionHeader::current_version(),
            header.dc_version
        )));
    }

    let dcd_length = cis.read_raw_varint32().map_err(|_| Error::DecodeError())?;
    let dcd_limit = cis.push_limit(dcd_length as u64).map_err(|_| Error::DecodeError())?;
    println!("DCD length = {:?}", dcd_length);
    let dcd = DesignComposeDefinition::parse_from(&mut cis)
        .map_err(|e| Error::DCDLoadError(format!("Failed to parse DCD: {}", e)))?;
    cis.pop_limit(dcd_limit);

    Ok((header, dcd))
}

/// A helper method to save serialized figma design docs.
pub fn save_design_def<P>(
    save_path: P,
    header: &DesignComposeDefinitionHeader,
    doc: &DesignComposeDefinition,
) -> Result<(), Error>
where
    P: AsRef<Path>,
{
    let mut output = File::create(save_path)?;
    output.write_all(encode_dcd_with_header(header, doc)?.as_slice())?;
    Ok(())
}

/// A helper method to load a DesignCompose Definition from a file.
pub fn load_design_def<P>(
    load_path: P,
) -> Result<(DesignComposeDefinitionHeader, DesignComposeDefinition), Error>
where
    P: AsRef<Path>,
{
    let mut buf: Vec<u8> = vec![];
    let mut document_file = File::open(&load_path)?;
    let _bytes = document_file.read_to_end(&mut buf)?;

    let (header, doc) = decode_dcd_with_header(&buf)?;

    Ok((header, doc))
}
