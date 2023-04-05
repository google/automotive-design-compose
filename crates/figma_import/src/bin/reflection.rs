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

use clap::Parser;
use serde_generate::SourceInstaller;
/// This program uses the serde-reflection and serde-generate crates to emit a Java
/// implementation of a deserializer for our serialized view trees. We have to trace
/// all of the types used in our schema manually.
///
/// We can also emit a yaml representation of the schema, which could be used to diff
/// our schema in CI.

#[derive(Parser)]
struct Cli {
    #[arg(short, long, default_value = "out")]
    out_dir: std::path::PathBuf,
}

#[cfg(feature = "reflection")]
fn main() {
    let args = Cli::parse();

    let registry = figma_import::reflection::registry().expect("no tracer registry");

    let config =
        serde_generate::CodeGeneratorConfig::new("com.android.designcompose.serdegen".into())
            .with_encodings(vec![serde_generate::Encoding::Bincode]);

    let installer = serde_generate::java::Installer::new(args.out_dir);
    installer.install_module(&config, &registry).expect("couldn't write java sources");
    installer.install_serde_runtime().expect("couldn't write runtime");
    installer.install_bincode_runtime().expect("couldn't write runtime");
}

#[cfg(not(feature = "reflection"))]
fn main() {
    panic!("Reflection feature not enabled")
}
