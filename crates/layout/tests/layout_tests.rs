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

//
// This file contains a set of unit tests to test the layout crate. It uses a
// saved serialized file that is retrieved from the Figma file at
// https://www.figma.com/file/OGUIhtwHL3z8wWZqnxYM9P. When modifying these
// tests, any changes to the Figma file require refetching and saving the file
// using the fetch binary from the designcompose root folder like this:
// cargo run --bin fetch --features=fetch -- --doc-id=OGUIhtwHL3z8wWZqnxYM9P --api-key=<API_KEY> --output=crates/layout/tests/layout-unit-tests.dcf --nodes='VerticalAutolayout'
// Note that every node used in these tests needs to be in the --nodes
// parameter list.
//

use figma_import::{
    toolkit_layout_style::{Dimension, LayoutSizing},
    toolkit_schema::View,
    NodeQuery, SerializedDesignDoc, ViewData,
};
use layout::{
    add_view, add_view_measure, clear_views, compute_node_layout, get_node_layout, print_layout,
    remove_view, set_node_size,
};
use std::collections::HashMap;
use std::fs::File;
use std::io::prelude::*;

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
        200.0
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

fn add_view_to_layout(
    view: &View,
    id: &mut i32,
    parent_layout_id: i32,
    child_index: i32,
    replacements: &HashMap<String, String>,
    views: &HashMap<NodeQuery, View>,
) {
    //println!("add_view_to_layout {}, {}, {}, {}", view.name, id, parent_layout_id, child_index);
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
            add_view_measure(my_id, parent_layout_id, child_index, view.clone(), measure_func);
        } else {
            let mut fixed_view = view.clone();
            fixed_view.style.width = Dimension::Points(view.style.bounding_box.width);
            fixed_view.style.height = Dimension::Points(view.style.bounding_box.height);
            add_view(my_id, parent_layout_id, child_index, fixed_view, None);
        }
    } else if let ViewData::Container { shape: _, children } = &view.data {
        add_view(my_id, parent_layout_id, child_index, view.clone(), None);
        let mut index = 0;
        for child in children {
            add_view_to_layout(child, id, my_id, index, replacements, views);
            index = index + 1;
        }
    }

    if parent_layout_id == -1 {
        compute_node_layout(my_id);
    }
}

fn load_doc() -> std::io::Result<SerializedDesignDoc> {
    let mut file = File::open("tests/layout-unit-tests.dcf")?;
    let mut buf: Vec<u8> = vec![];
    let bytes = file.read_to_end(&mut buf)?;
    println!("Read {} bytes", bytes);

    let figma_doc: SerializedDesignDoc = bincode::deserialize(buf.as_slice()).unwrap();
    Ok(figma_doc)
}

fn load_view(node_name: &str, doc: &SerializedDesignDoc) {
    let view_result = doc.views.get(&NodeQuery::NodeName(node_name.into()));
    assert!(view_result.is_some());
    let view = view_result.unwrap();
    let mut id = 0;
    add_view_to_layout(&view, &mut id, -1, -1, &HashMap::new(), &doc.views);
}

// Test for vertical autolayout frames with some fixed width children
#[test]
fn test_vertical_layout() {
    let figma_doc_result = load_doc();
    load_view("VerticalAutoLayout", &figma_doc_result.unwrap());

    let root_layout_result = get_node_layout(0);
    assert!(root_layout_result.is_some());
    let root_layout = root_layout_result.unwrap();
    assert!(root_layout.width == 100.0);
    assert!(root_layout.height == 110.0);

    let child1_layout_result = get_node_layout(1);
    assert!(child1_layout_result.is_some());
    let child1_layout = child1_layout_result.unwrap();
    assert!(child1_layout.width == 50.0);
    assert!(child1_layout.height == 50.0);

    let child2_layout_result = get_node_layout(2);
    assert!(child2_layout_result.is_some());
    let child2_layout = child2_layout_result.unwrap();
    assert!(child2_layout.width == 80.0);
    assert!(child2_layout.height == 30.0);

    clear_views();
}

// Test replacement nodes in auto layout
#[test]
fn test_replacement_autolayout() {
    let figma_doc_result = load_doc();
    load_view("ReplacementAutoLayout", &figma_doc_result.unwrap());

    let root_layout_result = get_node_layout(0);
    assert!(root_layout_result.is_some());
    let root_layout = root_layout_result.unwrap();

    assert!(root_layout.width == 140.0);
    assert!(root_layout.height == 70.0);

    let child1_layout_result = get_node_layout(1);
    assert!(child1_layout_result.is_some());
    let child1_layout = child1_layout_result.unwrap();
    assert!(child1_layout.width == 50.0);
    assert!(child1_layout.height == 50.0);
    assert!(child1_layout.left == 10.0);
    assert!(child1_layout.top == 10.0);

    let child2_layout_result = get_node_layout(2);
    assert!(child2_layout_result.is_some());
    let child2_layout = child2_layout_result.unwrap();
    assert!(child2_layout.width == 50.0);
    assert!(child2_layout.height == 50.0);
    assert!(child2_layout.left == 80.0);
    assert!(child2_layout.top == 10.0);

    clear_views();
}

// Test replacement nodes in fixed layout
#[test]
fn test_replacement_fixedlayout() {
    let figma_doc_result = load_doc();
    load_view("ReplacementFixedLayout", &figma_doc_result.unwrap());

    let root_layout_result = get_node_layout(0);
    assert!(root_layout_result.is_some());
    let root_layout = root_layout_result.unwrap();

    assert!(root_layout.width == 140.0);
    assert!(root_layout.height == 70.0);

    let child1_layout_result = get_node_layout(1);
    assert!(child1_layout_result.is_some());
    let child1_layout = child1_layout_result.unwrap();
    assert!(child1_layout.width == 50.0);
    assert!(child1_layout.height == 50.0);
    assert!(child1_layout.left == 10.0);
    assert!(child1_layout.top == 10.0);

    let child2_layout_result = get_node_layout(2);
    assert!(child2_layout_result.is_some());
    let child2_layout = child2_layout_result.unwrap();
    assert!(child2_layout.width == 50.0);
    assert!(child2_layout.height == 50.0);
    assert!(child2_layout.left == 80.0);
    assert!(child2_layout.top == 10.0);

    clear_views();
}

#[test]
fn test_vertical_fill() {
    let figma_doc_result = load_doc();
    load_view("VerticalFill", &figma_doc_result.unwrap());

    let root_layout_result = get_node_layout(0);
    assert!(root_layout_result.is_some());
    let root_layout = root_layout_result.unwrap();

    assert!(root_layout.width == 150.0);
    assert!(root_layout.height == 130.0);

    // Right node should fill to fit root
    let right_layout_result = get_node_layout(2);
    assert!(right_layout_result.is_some());
    let right_layout = right_layout_result.unwrap();
    assert!(right_layout.width == 70.0);
    assert!(right_layout.height == 110.0);
    assert!(right_layout.left == 70.0);
    assert!(right_layout.top == 10.0);

    // Auto fill height node should fill to fit Right
    let auto_fill_height_result = get_node_layout(3);
    assert!(auto_fill_height_result.is_some());
    let auto_fill_height = auto_fill_height_result.unwrap();
    assert!(auto_fill_height.width == 70.0);
    assert!(auto_fill_height.height == 30.0);

    clear_views();
}

#[test]
fn test_vertical_fill_resize() {
    let figma_doc_result = load_doc();
    load_view("VerticalFill", &figma_doc_result.unwrap());

    // Increase fixed left node height by 30 pixels
    let result = set_node_size(1, 0, 50, 140);
    assert!(result.changed_layouts.contains_key(&2));
    assert!(result.changed_layouts.contains_key(&3));

    // Right node should be taller by 30 pixels
    let right_layout_result = get_node_layout(2);
    assert!(right_layout_result.is_some());
    let right_layout = right_layout_result.unwrap();
    assert!(right_layout.width == 70.0);
    assert!(right_layout.height == 140.0);
    assert!(right_layout.left == 70.0);
    assert!(right_layout.top == 10.0);

    // Auto fill height node should be taller by 30 pixels
    let auto_fill_height_result = get_node_layout(3);
    assert!(auto_fill_height_result.is_some());
    let auto_fill_height = auto_fill_height_result.unwrap();
    assert!(auto_fill_height.width == 70.0);
    assert!(auto_fill_height.height == 60.0);

    clear_views();
}

#[test]
fn test_horizontal_fill() {
    let figma_doc_result = load_doc();
    load_view("HorizontalFill", &figma_doc_result.unwrap());

    let root_layout_result = get_node_layout(0);
    assert!(root_layout_result.is_some());
    let root_layout = root_layout_result.unwrap();

    assert!(root_layout.width == 130.0);
    assert!(root_layout.height == 150.0);

    // Bottom node should fill to fit root
    let bottom_layout_result = get_node_layout(2);
    assert!(bottom_layout_result.is_some());
    let bottom_layout = bottom_layout_result.unwrap();
    assert!(bottom_layout.width == 110.0);
    assert!(bottom_layout.height == 70.0);
    assert!(bottom_layout.left == 10.0);
    assert!(bottom_layout.top == 70.0);

    // Auto fill width node should fill to fit Bottom
    let auto_fill_width_result = get_node_layout(3);
    assert!(auto_fill_width_result.is_some());
    let auto_fill_width = auto_fill_width_result.unwrap();
    assert!(auto_fill_width.width == 30.0);
    assert!(auto_fill_width.height == 70.0);

    clear_views();
}

#[test]
fn test_horizontal_fill_resize() {
    let figma_doc_result = load_doc();
    load_view("HorizontalFill", &figma_doc_result.unwrap());

    // Increase fixed top node width by 30 pixels
    let result = set_node_size(1, 0, 140, 50);
    assert!(result.changed_layouts.contains_key(&2));
    assert!(result.changed_layouts.contains_key(&3));

    // Bottom node should be wider by 30 pixels
    let bottom_layout_result = get_node_layout(2);
    assert!(bottom_layout_result.is_some());
    let bottom_layout = bottom_layout_result.unwrap();
    assert!(bottom_layout.width == 140.0);
    assert!(bottom_layout.height == 70.0);
    assert!(bottom_layout.left == 10.0);
    assert!(bottom_layout.top == 70.0);

    // Auto fill width node should be wider by 30 pixels
    let auto_fill_width_result = get_node_layout(3);
    assert!(auto_fill_width_result.is_some());
    let auto_fill_width = auto_fill_width_result.unwrap();
    assert!(auto_fill_width.width == 60.0);
    assert!(auto_fill_width.height == 70.0);

    clear_views();
}

// Add tests:
//
// 2. Nodes in horizontal layout
// 3. Nodes in absolute layout
// 4. Remove node
// 5. Remove node, add node
// 6. Remove text node, add node with same ID
// 7. Change node variant
