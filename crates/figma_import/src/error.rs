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

use thiserror::Error;

/// Combined error type for all errors that can occur working with Figma documents.
#[derive(Error, Debug)]
pub enum Error {
    #[error("IO Error")]
    IoError(#[from] std::io::Error),
    #[error("HTTP Error")]
    NetworkError(#[from] ureq::Error),
    #[error("Image Error")]
    ImageError(#[from] image::ImageError),
    #[error("Json Serialization Error")]
    JsonError(#[from] serde_json::Error),
    #[error("Serialization Error")]
    BincodeError(#[from] bincode::Error),
    #[error("Layout Error")]
    LayoutError(#[from] taffy::error::TaffyError),
    #[error("String translation Error")]
    Utf8Error(#[from] std::string::FromUtf8Error),
    #[error("Figma Document Load Error")]
    DocumentLoadError(String),
    #[error("Error with DC Bundle")]
    DCBundleError(#[from] dc_bundle::Error),
}
