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

use jni::JNIEnv;
use log::error;
use thiserror::Error;
#[derive(Error, Debug)]
pub enum Error {
    #[error("Error: {0}")]
    GenericError(String),
    #[error("Protobuf Decode Error: {0:#?}")]
    ProtobufDecodeError(#[from] prost::DecodeError),
    #[error("Protobuf Encode Error")]
    ProtobufEncodeError(#[from] prost::EncodeError),
    #[error("Protobuf ConversionError: {0}")]
    MissingFieldError(#[from] layout::proto::Error),
    #[error("Json Serialization Error")]
    JsonError(#[from] serde_json::Error),
    #[error("Serialization Error")]
    BincodeError(#[from] bincode::Error),
    #[error("Figma_import Error")]
    FigmaImportError(#[from] figma_import::Error),
    #[error("JNI Error")]
    JNIError(#[from] jni::errors::Error),
}

pub fn throw_basic_exception(env: &mut JNIEnv, err: &dyn std::error::Error) {
    // An error occurring while trying to throw an exception shouldn't happen,
    // but let's at least panic with a decent error message
    env.throw(err.to_string()).expect("Error while trying to throw Exception");
}
