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

fn main() -> Result<(), Box<dyn Error>> {
    let mut prost_config = prost_build::Config::new();
    // Derive Copy for Dimension. Must derive it for both the enum and
    // for the proto message that holds the enum.
    prost_config.message_attribute("DimensionProto", "#[derive(Copy)]");
    prost_config.enum_attribute("Dimension", "#[derive(Copy)]");

    prost_config.compile_protos(
        &["proto/layout/layout_style.proto", "proto/layout/layout_manager.proto"],
        &["proto/layout"],
    )?;

    println!("cargo:rerun-if-changed=proto/layout");
    Ok(())
}
