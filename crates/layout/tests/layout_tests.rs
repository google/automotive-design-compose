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
    add_view, add_view_measure, clear_views, compute_layout, get_node_layout, print_layout,
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
        add_view(my_id, parent_layout_id, child_index, view.clone(), None, false);
        let mut index = 0;
        for child in children {
            add_view_to_layout(child, id, my_id, index, replacements, views);
            index = index + 1;
        }
    }

    if parent_layout_id == -1 {
        compute_layout();
    }
}

fn setup() -> std::io::Result<SerializedDesignDoc> {
    let mut file = File::open("tests/layout-unit-tests.dcf")?;
    let mut buf: Vec<u8> = vec![];
    let bytes = file.read_to_end(&mut buf)?;
    println!("Read {} bytes", bytes);

    let figma_doc: SerializedDesignDoc = bincode::deserialize(buf.as_slice()).unwrap();
    Ok(figma_doc)
}

// Test for vertical autolayout frames with some fixed width children
#[test]
fn test_vertical_layout() {
    let figma_doc_result = setup();
    assert!(figma_doc_result.is_ok());

    let figma_doc = figma_doc_result.unwrap();
    let view_result = figma_doc.views.get(&NodeQuery::NodeName("VerticalAutoLayout".into()));
    assert!(view_result.is_some());

    let view = view_result.unwrap();
    let mut id = 0;
    add_view_to_layout(&view, &mut id, -1, -1, &HashMap::new(), &figma_doc.views);

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

// Test replacement nodes in autolayout
#[test]
fn test_replacement_autolayout() {
    let figma_doc_result = setup();
    assert!(figma_doc_result.is_ok());

    let figma_doc = figma_doc_result.unwrap();
    let view_result = figma_doc.views.get(&NodeQuery::NodeName("ReplacementAutoLayout".into()));
    assert!(view_result.is_some());

    let view = view_result.unwrap();
    let mut id = 0;
    add_view_to_layout(&view, &mut id, -1, -1, &HashMap::new(), &figma_doc.views);

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

// Test replacement nodes in autolayout
#[test]
fn test_replacement_fixedlayout() {
    let figma_doc_result = setup();
    assert!(figma_doc_result.is_ok());

    let figma_doc = figma_doc_result.unwrap();
    let view_result = figma_doc.views.get(&NodeQuery::NodeName("ReplacementFixedLayout".into()));
    assert!(view_result.is_some());

    let view = view_result.unwrap();
    let mut id = 0;
    add_view_to_layout(&view, &mut id, -1, -1, &HashMap::new(), &figma_doc.views);

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

// Add tests:
//
// 2. Nodes in horizontal layout
// 3. Nodes in absolute layout
// 4. Remove node
// 5. Remove node, add node
// 6. Remove text node, add node with same ID
// 7. Change node variant
