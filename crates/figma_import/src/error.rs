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

use std::fmt::Display;

/// Combined error type for all errors that can occur working with Figma documents.
#[derive(Debug)]
pub enum Error {
    IoError(std::io::Error),
    HttpError(ureq::Error),
    ImageError(image::ImageError),
    JsonError(serde_json::Error),
    SvgParseError(usvg::Error),
    BincodeError(bincode::Error),
}
impl From<std::io::Error> for Error {
    fn from(io_error: std::io::Error) -> Self {
        Error::IoError(io_error)
    }
}
impl From<ureq::Error> for Error {
    fn from(ureq_error: ureq::Error) -> Self {
        Error::HttpError(ureq_error)
    }
}
impl From<image::ImageError> for Error {
    fn from(image_error: image::ImageError) -> Self {
        Error::ImageError(image_error)
    }
}
impl From<serde_json::Error> for Error {
    fn from(serde_error: serde_json::Error) -> Self {
        Error::JsonError(serde_error)
    }
}
impl From<usvg::Error> for Error {
    fn from(usvg_error: usvg::Error) -> Self {
        Error::SvgParseError(usvg_error)
    }
}
impl From<bincode::Error> for Error {
    fn from(bincode_error: bincode::Error) -> Self {
        Error::BincodeError(bincode_error)
    }
}
impl std::error::Error for Error {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Error::IoError(e) => Some(e),
            Error::HttpError(e) => Some(e),
            Error::ImageError(e) => Some(e),
            Error::JsonError(e) => Some(e),
            Error::SvgParseError(e) => Some(e),
            Error::BincodeError(e) => Some(e),
        }
    }
}
impl Display for Error {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{:?}", self)
    }
}
