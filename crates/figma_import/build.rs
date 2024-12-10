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

use ::vergen::EmitBuilder;
use std::error::Error;
use std::path::Path;

fn main() -> Result<(), Box<dyn Error>> {
    let mut prost_config = prost_build::Config::new();
    prost_config.type_attribute(".", "#[derive(serde::Serialize, serde::Deserialize)]");

    // The DesignComposeDefinition is built by the dc_bundle crate. This line configures the compiler
    // to use the message from there, rather than compiling it's own copy of the message
    prost_config.extern_path(
        ".designcompose.definition.DesignComposeDefinition",
        "::dc_bundle::definition::DesignComposeDefinition",
    );

    let proto_path = Path::new(env!("CARGO_MANIFEST_DIR"))
        .parent()
        .and_then(Path::parent)
        .unwrap()
        .join("proto");

    prost_config
        .compile_protos(&[proto_path.join("live_update/figma/figma_doc.proto")], &[&proto_path])?;

    println!("cargo:rerun-if-changed={}", proto_path.to_str().unwrap());
    // Generate the default 'cargo:' instruction output
    EmitBuilder::builder().emit()?;
    Ok(())
}
