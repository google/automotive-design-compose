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

/// Utility program to fetch a doc and serialize it to file
use clap::Parser;

use figma_import::tools::fetch_layout::fetch_layout;
use figma_import::tools::fetch_layout::Args;

fn main() {
    let args = Args::parse();
    if let Err(e) = fetch_layout(args) {
        eprintln!("Fetch failed: {:?}", e);
        std::process::exit(1);
    }
}
