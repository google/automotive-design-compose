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
use crate::definition::{DesignComposeDefinition, DesignComposeDefinitionHeader};
use crate::Error;
use prost::bytes::{Buf, Bytes};
use prost::Message;
use std::fs::File;
use std::io::{Read, Write};
use std::path::Path;

pub fn encode_dcd_with_header(
    header: &DesignComposeDefinitionHeader,
    doc: &DesignComposeDefinition,
) -> Vec<u8> {
    let mut encoded = header.encode_length_delimited_to_vec();
    encoded.append(&mut doc.encode_length_delimited_to_vec());
    encoded
}

pub fn decode_dcd_with_header<B>(
    mut msg: B,
) -> Result<(DesignComposeDefinitionHeader, DesignComposeDefinition), Error>
where
    B: Buf,
{
    let header = DesignComposeDefinitionHeader::decode_length_delimited(&mut msg)?;

    // Ensure the version of the document matches this version of automotive design compose.
    if header.dc_version != DesignComposeDefinitionHeader::current_version() {
        return Err(Error::DCDLoadError(format!(
            "DesignComposeDefinition incorrect version. Expected {} Found: {}",
            DesignComposeDefinitionHeader::current_version(),
            header.dc_version
        )));
    }

    let dcd = DesignComposeDefinition::decode_length_delimited(msg)?;
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
    output.write_all(encode_dcd_with_header(header, doc).as_slice())?;
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

    let (header, doc) = decode_dcd_with_header(Bytes::from(buf))?;

    Ok((header, doc))
}
