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
    #[error("IO Error: {0}")]
    IoError(#[from] std::io::Error),
    #[error("HTTP Error: {0}")]
    NetworkError(#[from] reqwest::Error),
    #[error("Image Error: {0}")]
    ImageError(#[from] image::ImageError),
    #[error("Json Serialization Error: {0}")]
    JsonError(#[from] serde_json::Error),
    #[error("Json Path Serialization Error: {0}")]
    JsonPathError(#[from] serde_path_to_error::Error<serde_json::Error>),
    #[error("Layout Error: {0}")]
    LayoutError(#[from] taffy::TaffyError),
    #[error("String translation Error: {0}")]
    Utf8Error(#[from] std::string::FromUtf8Error),
    #[error("Figma Document Load Error: {0}")]
    DocumentLoadError(String),
    #[error("Error with DC Bundle: {0}")]
    DCBundleError(#[from] dc_bundle::Error),
}
