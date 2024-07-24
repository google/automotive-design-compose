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

use clap::Parser;

use figma_import::tools::dcf_info::dcf_info;
use figma_import::tools::dcf_info::Args;

fn main() {
    let args = Args::parse();
    if let Err(e) = dcf_info(args) {
        eprintln!("dcf_info failed: {:?}", e);
        std::process::exit(1);
    }
}
