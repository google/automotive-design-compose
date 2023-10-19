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
use figma_import::{
    toolkit_layout_style::{Dimension, LayoutSizing},
    toolkit_schema::View,
    Document, NodeQuery, ProxyConfig, SerializedDesignDoc, ViewData,
};
use layout::{
    add_view, add_view_measure, compute_layout, print_layout, remove_view, set_node_size,
};
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
struct ConvertError(String);
impl From<figma_import::Error> for ConvertError {
    fn from(e: figma_import::Error) -> Self {
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
struct Args {
    /// Figma Document ID to fetch and convert
    #[arg(short, long)]
    doc_id: String,
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
    view: &View,
    id: &mut i32,
    parent_layout_id: i32,
    child_index: i32,
    views: &HashMap<NodeQuery, View>,
) {
    println!("test_layout {}, {}, {}, {}", view.name, id, parent_layout_id, child_index);
    let my_id: i32 = id.clone();
    *id = *id + 1;
    if let ViewData::Text { content: _ } = &view.data {
        let mut use_measure_func = false;
        if let Dimension::Auto = view.style.width {
            if let Dimension::Auto = view.style.height {
                if view.style.horizontal_sizing == LayoutSizing::Fill {
                    use_measure_func = true;
                }
            }
        }
        if use_measure_func {
            add_view_measure(
                my_id,
                parent_layout_id,
                child_index,
                view.clone(),
                measure_func,
                false,
            );
        } else {
            let mut fixed_view = view.clone();
            fixed_view.style.width = Dimension::Points(view.style.bounding_box.width);
            fixed_view.style.height = Dimension::Points(view.style.bounding_box.height);
            add_view(my_id, parent_layout_id, child_index, fixed_view, None, false);
        }
    } else if let ViewData::Container { shape: _, children } = &view.data {
        if view.name.starts_with("#Replacement") {
            let square = views.get(&NodeQuery::NodeName("#BlueSquare".to_string()));
            if let Some(square) = square {
                add_view(
                    my_id,
                    parent_layout_id,
                    child_index,
                    square.clone(),
                    Some(view.clone()),
                    false,
                );
            }
        } else {
            add_view(my_id, parent_layout_id, child_index, view.clone(), None, false);
        }
        let mut index = 0;
        for child in children {
            test_layout(child, id, my_id, index, views);
            index = index + 1;
        }
    }

    if parent_layout_id == -1 {
        compute_layout();
    }
}
fn fetch_impl(args: Args) -> Result<(), ConvertError> {
    let proxy_config: ProxyConfig = match args.http_proxy {
        Some(x) => ProxyConfig::HttpProxyConfig(x),
        None => ProxyConfig::None,
    };
    let mut doc: Document =
        Document::new(args.api_key.as_str(), args.doc_id.clone(), &proxy_config, None)?;
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
        test_layout(stage, &mut id, -1, -1, &views);
        print_layout(0);
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

    // Build the serializable doc structure
    let serializable_doc = SerializedDesignDoc {
        views,
        component_sets: doc.component_sets().clone(),
        images: doc.encoded_image_map(),
        last_modified: doc.last_modified().clone(),
        name: doc.get_name(),
        version: doc.get_version(),
        id: doc.get_document_id(),
    };

    // We don't bother with serialization headers or image sessions with
    // this tool.
    let output = std::fs::File::create(args.output)?;
    bincode::serialize_into(output, &serializable_doc)?;
    Ok(())
}
fn main() {
    let args = Args::parse();
    if let Err(e) = fetch_impl(args) {
        eprintln!("Fetch failed: {:?}", e);
        std::process::exit(1);
    }
}
