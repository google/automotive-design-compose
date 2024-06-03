// Copyright 2024 Google LLC
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

use std::error::Error;
use std::path::Path;

fn main() -> Result<(), Box<dyn Error>> {
    let mut prost_config = prost_build::Config::new();
    // Derive Copy for Dimension. Must derive it for both the enum and
    // for the proto message that holds the enum.
    prost_config.message_attribute("DimensionProto", "#[derive(Copy)]");
    prost_config.message_attribute("DimensionProto.Auto", "#[derive(Copy)]");
    prost_config.message_attribute("DimensionProto.Undefined", "#[derive(Copy)]");
    prost_config.enum_attribute("Dimension", "#[derive(Copy)]");

    let proto_path = Path::new(env!("CARGO_MANIFEST_DIR"))
        .parent()
        .and_then(Path::parent)
        .unwrap()
        .join("proto");

    prost_config.include_file("protos.rs");
    prost_config
        .compile_protos(&[proto_path.join("android_interface/jni_layout.proto")], &[&proto_path])?;

    println!("cargo:rerun-if-changed={}", proto_path.to_str().unwrap());
    Ok(())
}
