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
use std::fs;
use std::path::{Path, PathBuf};

fn main() -> Result<(), Box<dyn Error>> {
    let out_dir_str = std::env::var_os("OUT_DIR").unwrap();
    let out_dir = PathBuf::from(out_dir_str);
    let config = protobuf_codegen::Customize::default();

    // Define the directory where .proto files are located.
    let proto_path = Path::new(env!("CARGO_MANIFEST_DIR")).join("src").join("proto");

    // Collect the .proto files to compile.
    let proto_files = collect_proto_files(&proto_path)?;

    // Create input files as str
    let proto_files_str: Vec<String> =
        proto_files.iter().map(|p| p.to_str().unwrap().to_string()).collect();

    let mut codegen = protobuf_codegen::Codegen::new();

    codegen
        .customize(config)
        .out_dir(&out_dir)
        .include(&proto_path)
        .inputs(&proto_files_str)
        .cargo_out_dir("protos")
        .run()?;

    println!("cargo:rerun-if-changed={}", proto_path.to_str().unwrap());
    Ok(())
}

fn collect_proto_files(proto_path: &Path) -> Result<Vec<PathBuf>, Box<dyn Error>> {
    let mut proto_files = Vec::new();
    if proto_path.is_dir() {
        for entry in fs::read_dir(proto_path)? {
            let entry = entry?;
            let path = entry.path();
            if path.is_dir() {
                // Recursively search in subfolders.
                proto_files.extend(collect_proto_files(&path)?);
            } else if let Some(ext) = path.extension() {
                if ext == "proto" {
                    proto_files.push(path);
                }
            }
        }
    }
    Ok(proto_files)
}
