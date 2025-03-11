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
mod component_context;
mod design_definition;
mod document;
mod error;
mod extended_layout_schema;
mod figma_schema;
mod image_context;
pub mod meter_schema;
mod proxy_config;
pub mod reaction_schema;
pub mod shader_schema;

pub mod tools;
mod transform_flexbox;
mod variable_utils;

// Exports for library users
pub use dc_bundle::design_compose_definition::DesignComposeDefinition;
pub use dc_bundle::design_compose_definition::DesignComposeDefinitionHeader;
pub use dc_bundle::geometry::Rectangle;
pub use document::Document;
pub use error::Error;
pub use image_context::ImageContextSession;
pub use proxy_config::ProxyConfig;

// Internal convenience
pub use dc_bundle::color::Color;
pub use dc_bundle::definition::NodeQuery;
pub use dc_bundle::definition_file::load_design_def;
pub use dc_bundle::definition_file::save_design_def;
pub use dc_bundle::view::{View, ViewData};
