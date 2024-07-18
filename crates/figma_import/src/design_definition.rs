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

use crate::error::Error;
use std::collections::HashMap;
use std::fmt;
use std::fs::File;
use std::io::{Read, Write};
use std::path::Path;

use dc_bundle::legacy_definition::element::node::NodeQuery;
use serde::{Deserialize, Serialize};

use crate::{document::FigmaDocInfo, image_context::EncodedImageMap, toolkit_schema};

static CURRENT_VERSION: u32 = 20;

// This is our serialized document type.
#[derive(Serialize, Deserialize, Debug)]
pub struct DesignComposeDefinitionHeader {
    pub version: u32,
}
impl DesignComposeDefinitionHeader {
    pub fn current() -> DesignComposeDefinitionHeader {
        DesignComposeDefinitionHeader { version: CURRENT_VERSION }
    }
}

impl fmt::Display for DesignComposeDefinitionHeader {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        // NOTE: Using `write!` here instead of typical `format!`
        // to keep newlines.
        write!(f, "DC Version: {}\nVersion: {}", CURRENT_VERSION, &self.version)
    }
}

// This is our serialized document type.
#[derive(Serialize, Deserialize, Debug)]
pub struct DesignComposeDefinition {
    pub last_modified: String,
    pub views: HashMap<NodeQuery, toolkit_schema::View>,
    pub images: EncodedImageMap,
    pub name: String,
    pub component_sets: HashMap<String, String>,
    pub version: String,
    pub id: String,
    pub variable_map: toolkit_schema::VariableMap,
}

impl fmt::Display for DesignComposeDefinition {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        // NOTE: Using `write!` here instead of typical `format!`
        // to keep newlines.
        write!(
            f,
            "Doc ID: {}\nName: {}\nLast Modified: {}",
            &self.id, &self.name, &self.last_modified
        )
    }
}

// This is the struct we send over to the client. It contains the serialized document
// along with some extra data: document branches, project files, and errors
#[derive(Serialize, Deserialize, Debug)]
pub struct ServerFigmaDoc {
    pub figma_doc: DesignComposeDefinition,
    pub branches: Vec<FigmaDocInfo>,
    pub project_files: Vec<FigmaDocInfo>,
    pub errors: Vec<String>,
}

/// A helper method to load a DesignCompose Definition from figma.
pub fn load_design_def<P>(
    load_path: P,
) -> Result<(DesignComposeDefinitionHeader, DesignComposeDefinition), Error>
where
    P: AsRef<Path>,
{
    let mut document_file = File::open(&load_path)?;

    let mut buf: Vec<u8> = vec![];
    let _bytes = document_file.read_to_end(&mut buf)?;

    let header: DesignComposeDefinitionHeader = bincode::deserialize(buf.as_slice())?;

    // Ensure the version of the document matches this version of automotive design compose.
    if header.version != DesignComposeDefinitionHeader::current().version {
        return Err(Error::DocumentLoadError(format!(
            "Serialized Figma doc incorrect version. Expected {} Found: {}",
            DesignComposeDefinitionHeader::current().version,
            header.version
        )));
    }

    let header_size = bincode::serialized_size(&header)? as usize;

    let doc: DesignComposeDefinition = bincode::deserialize(&buf.as_slice()[header_size..])?;

    Ok((header, doc))
}

/// A helper method to save serialized figma design docs.
pub fn save_design_def<P>(save_path: P, doc: &DesignComposeDefinition) -> Result<(), Error>
where
    P: AsRef<Path>,
{
    let mut output = std::fs::File::create(save_path)?;
    let header = bincode::serialize(&DesignComposeDefinitionHeader::current())?;
    let doc = bincode::serialize(&doc)?;
    output.write_all(header.as_slice())?;
    output.write_all(doc.as_slice())?;
    Ok(())
}

#[cfg(test)]
mod serialized_document_tests {

    use super::*;
    use std::fs::File;
    use std::path::PathBuf;
    use testdir::testdir;

    #[test]
    fn load_save_load() {
        //Load a test doc.
        let mut doc_path = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
        doc_path.push("../../reference-apps/helloworld/helloworld-app/src/main/assets/figma/HelloWorldDoc_pxVlixodJqZL95zo2RzTHl.dcf");
        let (header, doc) = load_design_def(doc_path).expect("Failed to load design bundle.");

        // Dump some info
        println!("Deserialized header: {}", &header);
        println!("Deserialized doc: {}", &doc);

        // Re-save the test doc into a temporary file in a temporary directory.
        let tmp_dir = testdir!();
        let tmp_doc_path = PathBuf::from(&tmp_dir).join("tmp_pxVlixodJqZL95zo2RzTHl.dcf");
        save_design_def(&tmp_doc_path, &doc)
            .expect("Failed to save temporary DesignCompose Definition.");

        // Re-load the temporary file
        let (tmp_header, tmp_doc) =
            load_design_def(&tmp_doc_path).expect("Failed to load tmp DesignCompose Definition.");
        println!("Tmp deserialized header: {}", &tmp_header);
        println!("Tmp deserialized doc: {}", &tmp_doc);
    }

    #[test]
    #[should_panic]
    fn load_missing_doc() {
        // Try to load a doc which doesn't exist. This should fail with a clean error.
        let mut doc_path = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
        doc_path.push("this.doc.does.not.exist.dcf");
        let (_tmp_header, _tmp_doc) =
            load_design_def(&doc_path).expect("Failed to load tmp DesignCompose Definition.");
    }

    #[test]
    #[should_panic]
    fn load_bad_doc() {
        // Create a garbage binary doc in a temporary directory and load it, hopefully seeing a failure.
        let tmp_dir = testdir!();
        let garbage_doc_path = PathBuf::from(&tmp_dir).join("tmp.garbage.file.dcf");
        let mut file =
            File::create(&garbage_doc_path).expect("Failed to create new garbage binary doc file.");
        let data: Vec<u8> = (0..48).map(|v| v).collect();
        file.write_all(&data).expect("Failed to write garbage data to garbage file.");

        let (_tmp_header, _tmp_doc) = load_design_def(&garbage_doc_path)
            .expect("Failed to load garbage DesignCompose Definition.");
    }
}
