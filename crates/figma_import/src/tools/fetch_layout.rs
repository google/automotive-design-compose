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

use crate::{toolkit_schema::View, DesignComposeDefinition, Document, ProxyConfig, ViewData};
/// Utility program to fetch a doc and serialize it to file
use clap::Parser;
use dc_bundle::legacy_definition::element::geometry::Dimension;
use dc_bundle::legacy_definition::element::node::NodeQuery;
use dc_bundle::legacy_definition::layout::grid::LayoutSizing;
use layout::LayoutManager;
use std::collections::HashMap;
use std::io;
use std::io::prelude::*;
use taffy::error::TaffyError;

fn _pause() {
    let mut stdin = io::stdin();
    let mut stdout = io::stdout();

    // We want the cursor to stay at the end of the line, so we print without a newline and flush manually.
    write!(stdout, "Update Figma doc then press any key to continue...").unwrap();
    stdout.flush().unwrap();

    // Read a single byte and discard
    let _ = stdin.read(&mut [0u8]).unwrap();
}

#[derive(Debug)]
#[allow(dead_code)]
pub struct ConvertError(String);
impl From<crate::Error> for ConvertError {
    fn from(e: crate::Error) -> Self {
        eprintln!("Error during Figma conversion: {:?}", e);
        ConvertError(format!("Internal Server Error during Figma conversion: {:?}", e))
    }
}
impl From<bincode::Error> for ConvertError {
    fn from(e: bincode::Error) -> Self {
        eprintln!("Error during serialization: {:?}", e);
        ConvertError(format!("Error during serialization: {:?}", e))
    }
}
impl From<serde_json::Error> for ConvertError {
    fn from(e: serde_json::Error) -> Self {
        eprintln!("Error during image session serialization: {:?}", e);
        ConvertError(format!("Error during image session serialization: {:?}", e))
    }
}
impl From<std::io::Error> for ConvertError {
    fn from(e: std::io::Error) -> Self {
        eprintln!("Error creating output file: {:?}", e);
        ConvertError(format!("Error creating output file: {:?}", e))
    }
}
impl From<TaffyError> for ConvertError {
    fn from(e: TaffyError) -> Self {
        eprintln!("Error with taffy layout: {:?}", e);
        ConvertError(format!("Error with taffy layout: {:?}", e))
    }
}
#[derive(Parser, Debug)]
pub struct Args {
    /// Figma Document ID to fetch and convert
    #[arg(short, long)]
    doc_id: String,
    /// Figma Document Version ID to fetch and convert
    #[arg(short, long)]
    version_id: Option<String>,
    /// Figma API key to use for Figma requests
    #[arg(short, long)]
    api_key: String,
    /// HTTP proxy server - <host>:<port>
    #[arg(long)]
    http_proxy: Option<String>,
    /// Root nodes to find in the doc and convert
    #[arg(short, long)]
    nodes: Vec<String>,
    /// Output file to write serialized doc into
    #[arg(short, long)]
    output: std::path::PathBuf,
}
fn measure_func(
    layout_id: i32,
    width: f32,
    height: f32,
    avail_width: f32,
    avail_height: f32,
) -> (f32, f32) {
    let mut result_height = if height > 0.0 { height } else { 30.0 };
    let result_width = if width > 0.0 {
        width
    } else if avail_width > 0.0 {
        //avail_width
        212.0
        //300.0
    } else {
        result_height = 60.0;
        100.0
    };

    println!(
        "measure_func layout_id {} width {} height {} available_width {} available_height {} -> {}, {}",
        layout_id, width, height, avail_width, avail_height, result_width, result_height
    );

    (result_width, result_height)
}
fn test_layout(
    layout_manager: &mut LayoutManager,
    view: &View,
    id: &mut i32,
    parent_layout_id: i32,
    child_index: i32,
    views: &HashMap<NodeQuery, View>,
) {
    println!("test_layout {}, {}, {}, {}", view.name, id, parent_layout_id, child_index);
    let my_id: i32 = id.clone();
    *id = *id + 1;
    if let ViewData::Text { content: _, res_name: _ } = &view.data {
        let mut use_measure_func = false;
        if let Dimension::Auto = view.style.layout_style.width {
            if let Dimension::Auto = view.style.layout_style.height {
                if view.style.node_style.horizontal_sizing == LayoutSizing::Fill {
                    use_measure_func = true;
                }
            }
        }
        if use_measure_func {
            layout_manager.add_style_measure(
                my_id,
                parent_layout_id,
                child_index,
                view.style.layout_style.clone(),
                view.name.clone(),
                measure_func,
            );
        } else {
            let mut fixed_view = view.clone();
            fixed_view.style.layout_style.width =
                Dimension::Points(view.style.layout_style.bounding_box.width);
            fixed_view.style.layout_style.height =
                Dimension::Points(view.style.layout_style.bounding_box.height);
            layout_manager.add_style(
                my_id,
                parent_layout_id,
                child_index,
                fixed_view.style.layout_style.clone(),
                fixed_view.name.clone(),
                None,
                Some(view.style.layout_style.bounding_box.width as i32),
                Some(view.style.layout_style.bounding_box.height as i32),
            );
        }
    } else if let ViewData::Container { shape: _, children } = &view.data {
        if view.name.starts_with("#Replacement") {
            let square = views.get(&NodeQuery::NodeName("#BlueSquare".to_string()));
            if let Some(square) = square {
                layout_manager.add_style(
                    my_id,
                    parent_layout_id,
                    child_index,
                    square.style.layout_style.clone(),
                    square.name.clone(),
                    None,
                    None,
                    None,
                );
            }
        } else {
            layout_manager.add_style(
                my_id,
                parent_layout_id,
                child_index,
                view.style.layout_style.clone(),
                view.name.clone(),
                None,
                None,
                None,
            );
        }
        let mut index = 0;
        for child in children {
            test_layout(layout_manager, child, id, my_id, index, views);
            index = index + 1;
        }
    }

    if parent_layout_id == -1 {
        layout_manager.set_node_size(0, 0, 1200, 800);
        layout_manager.compute_node_layout(my_id);
    }
}
pub fn fetch_layout(args: Args) -> Result<(), ConvertError> {
    let proxy_config: ProxyConfig = match args.http_proxy {
        Some(x) => ProxyConfig::HttpProxyConfig(x),
        None => ProxyConfig::None,
    };
    let mut doc: Document = Document::new(
        args.api_key.as_str(),
        args.doc_id.clone(),
        args.version_id.unwrap_or(String::new()),
        &proxy_config,
        None,
    )?;
    let mut error_list = Vec::new();
    // Convert the requested nodes from the Figma doc.
    let views = doc.nodes(
        &args.nodes.iter().map(|name| NodeQuery::name(name)).collect(),
        &Vec::new(),
        &mut error_list,
    )?;
    for error in error_list {
        eprintln!("Warning: {error}");
    }

    let stage = views.get(&NodeQuery::NodeName("#stage".to_string()));
    if let Some(stage) = stage {
        let mut id = 0;
        let mut layout_manager = LayoutManager::new();
        test_layout(&mut layout_manager, stage, &mut id, -1, -1, &views);
        layout_manager.print_layout(0, |msg| println!("{}", msg));
    }

    /*
    pause();

    println!("");
    println!("Fetching Again...");
    doc = Document::new(args.api_key.as_str(), args.doc_id, &proxy_config, None)?;
    error_list = Vec::new();
    let views = doc.nodes(
        &args.nodes.iter().map(|name| NodeQuery::name(name)).collect(),
        &Vec::new(),
        &mut error_list,
    )?;
    for error in error_list {
        eprintln!("Warning: {error}");
    }
    let init_response = init_layout(&views);
    println!("Init result: {:?}", init_response);
    for view in views.values() {
        print_layout(view);
    }
    */

    let variable_map = doc.build_variable_map();

    // Build the serializable doc structure
    let serializable_doc = DesignComposeDefinition {
        views,
        component_sets: doc.component_sets().clone(),
        images: doc.encoded_image_map(),
        last_modified: doc.last_modified().clone(),
        name: doc.get_name(),
        version: doc.get_version(),
        id: doc.get_document_id(),
        variable_map: variable_map,
    };

    // We don't bother with serialization headers or image sessions with
    // this tool.
    let output = std::fs::File::create(args.output)?;
    bincode::serialize_into(output, &serializable_doc)?;
    Ok(())
}
