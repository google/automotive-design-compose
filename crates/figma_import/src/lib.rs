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

//! `figma_import` fetches a document from Figma and converts nodes from the document
//! to toolkit_schema Views, which can then be further customized (changing text or style)
//! and presented in other components that implement logic.
//!
//! The goal of this crate is to perform the mapping from Figma to the toolkit; it does
//! not provide any kind of UI logic mapping.
mod color;
mod component_context;
mod document;
mod error;
mod extended_layout_schema;
mod fetch;
mod figma_schema;
mod image_context;
pub mod reaction_schema;
mod serialized_document;
mod svg;
pub mod toolkit_font_style;
pub mod toolkit_layout_style;
pub mod toolkit_schema;
pub mod toolkit_style;
mod transform_flexbox;
mod utils;
// Exports for library users
pub use document::{Document, NodeQuery};
pub use error::Error;
pub use fetch::{fetch_doc, ConvertRequest, ConvertResponse};
pub use figma_schema::Rectangle;
pub use image_context::{ImageContextSession, ImageKey};
pub use serialized_document::{SerializedFigmaDoc, SerializedFigmaDocHeader, ServerFigmaDoc};
pub use toolkit_schema::{View, ViewData}; // ugly hack
                                          // Internal convenience
pub use color::Color;
#[cfg(feature = "http_mock")]
mod figma_v1_document_mocks;
/// Functionality related to reflection for deserializing our bincode archives in other
/// languages
#[cfg(feature = "reflection")]
pub mod reflection;

#[cfg(feature = "android")]
mod jni;

#[cfg(debug_assertions)]
#[cfg(feature = "android")]
mod test_jni;
