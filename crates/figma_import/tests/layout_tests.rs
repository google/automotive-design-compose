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
// cargo run --bin fetch --features=fetch -- --doc-id=OGUIhtwHL3z8wWZqnxYM9P --api-key=<API_KEY> --output=crates/figma_import/tests/layout-unit-tests.dcf --nodes='VerticalAutolayout'
// Note that every node used in these tests needs to be in the --nodes
// parameter list.
//

use dc_bundle::definition::element::dimension_proto::Dimension;
use dc_bundle::definition::element::DimensionProto;
use dc_bundle::definition::layout::LayoutSizing;
use dc_bundle::legacy_definition::element::node::NodeQuery;
use dc_bundle::legacy_definition::view::view::{View, ViewData};
use dc_bundle::legacy_definition::DesignComposeDefinition;
use figma_import::load_design_def;
use layout::LayoutManager;
use std::collections::HashMap;

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
    manager: &mut LayoutManager,
    id: &mut i32,
    parent_layout_id: i32,
    child_index: i32,
    replacements: &HashMap<String, String>,
    views: &HashMap<NodeQuery, View>,
) {
    //println!("add_view_to_layout {}, {}, {}, {}", view.name, id, parent_layout_id, child_index);
    let my_id: i32 = id.clone();
    *id = *id + 1;
    if let ViewData::Text { content: _, res_name: _ } = &view.data {
        let mut use_measure_func = false;
        if let Dimension::Auto(()) = view.style.layout_style().width.unwrap().dimension.unwrap() {
            if let Dimension::Auto(()) =
                view.style.layout_style().height.unwrap().dimension.unwrap()
            {
                if view.style.node_style().horizontal_sizing == i32::from(LayoutSizing::Fill) {
                    use_measure_func = true;
                }
            }
        }
        if use_measure_func {
            manager
                .add_style(
                    my_id,
                    parent_layout_id,
                    child_index,
                    view.style.layout_style().clone(),
                    view.name.clone(),
                    use_measure_func,
                    None,
                    None,
                )
                .unwrap();
        } else {
            let mut fixed_view = view.clone();
            fixed_view.style.layout_style_mut().width =
                DimensionProto::new_points(view.style.layout_style().bounding_box().unwrap().width);
            fixed_view.style.layout_style_mut().height = DimensionProto::new_points(
                view.style.layout_style().bounding_box().unwrap().height,
            );
            manager
                .add_style(
                    my_id,
                    parent_layout_id,
                    child_index,
                    fixed_view.style.layout_style().clone(),
                    fixed_view.name.clone(),
                    false,
                    None,
                    None,
                )
                .unwrap();
        }
    } else if let ViewData::Container { shape: _, children } = &view.data {
        manager
            .add_style(
                my_id,
                parent_layout_id,
                child_index,
                view.style.layout_style().clone(),
                view.name.clone(),
                false,
                None,
                None,
            )
            .unwrap();
        let mut index = 0;
        for child in children {
            add_view_to_layout(child, manager, id, my_id, index, replacements, views);
            index = index + 1;
        }
    }

    if parent_layout_id == -1 {
        manager.compute_node_layout(my_id);
    }
}

fn load_doc() -> Result<DesignComposeDefinition, figma_import::Error> {
    let (_header, figma_doc) = load_design_def("tests/layout-unit-tests.dcf")?;
    Ok(figma_doc)
}

fn load_view(node_name: &str, doc: &DesignComposeDefinition) -> LayoutManager {
    let view_result = doc.views.get(&NodeQuery::NodeName(node_name.into()));
    assert!(view_result.is_some());
    let view = view_result.unwrap();
    let mut id = 0;
    let mut manager = LayoutManager::new(measure_func);
    add_view_to_layout(&view, &mut manager, &mut id, -1, -1, &HashMap::new(), &doc.views);
    manager
}

// Test for vertical autolayout frames with some fixed width children
#[test]
fn test_vertical_layout() {
    let figma_doc_result = load_doc();
    let manager = load_view("VerticalAutoLayout", &figma_doc_result.unwrap());

    let root_layout_result = manager.get_node_layout(0);
    assert!(root_layout_result.is_some());
    let root_layout = root_layout_result.unwrap();
    assert_eq!(root_layout.width, 100.0);
    assert_eq!(root_layout.height, 110.0);

    let child1_layout_result = manager.get_node_layout(1);
    assert!(child1_layout_result.is_some());
    let child1_layout = child1_layout_result.unwrap();
    assert_eq!(child1_layout.width, 50.0);
    assert_eq!(child1_layout.height, 50.0);

    let child2_layout_result = manager.get_node_layout(2);
    assert!(child2_layout_result.is_some());
    let child2_layout = child2_layout_result.unwrap();
    assert_eq!(child2_layout.width, 80.0);
    assert_eq!(child2_layout.height, 30.0);
}

// Test replacement nodes in auto layout
#[test]
fn test_replacement_autolayout() {
    let figma_doc_result = load_doc();
    let manager = load_view("ReplacementAutoLayout", &figma_doc_result.unwrap());

    let root_layout_result = manager.get_node_layout(0);
    assert!(root_layout_result.is_some());
    let root_layout = root_layout_result.unwrap();

    assert_eq!(root_layout.width, 140.0);
    assert_eq!(root_layout.height, 70.0);

    let child1_layout_result = manager.get_node_layout(1);
    assert!(child1_layout_result.is_some());
    let child1_layout = child1_layout_result.unwrap();
    assert_eq!(child1_layout.width, 50.0);
    assert_eq!(child1_layout.height, 50.0);
    assert_eq!(child1_layout.left, 10.0);
    assert_eq!(child1_layout.top, 10.0);

    let child2_layout_result = manager.get_node_layout(2);
    assert!(child2_layout_result.is_some());
    let child2_layout = child2_layout_result.unwrap();
    assert_eq!(child2_layout.width, 50.0);
    assert_eq!(child2_layout.height, 50.0);
    assert_eq!(child2_layout.left, 80.0);
    assert_eq!(child2_layout.top, 10.0);
}

// Test replacement nodes in fixed layout
#[test]
fn test_replacement_fixedlayout() {
    let figma_doc_result = load_doc();
    let manager = load_view("ReplacementFixedLayout", &figma_doc_result.unwrap());

    let root_layout_result = manager.get_node_layout(0);
    assert!(root_layout_result.is_some());
    let root_layout = root_layout_result.unwrap();

    assert_eq!(root_layout.width, 140.0);
    assert_eq!(root_layout.height, 70.0);

    let child1_layout_result = manager.get_node_layout(1);
    assert!(child1_layout_result.is_some());
    let child1_layout = child1_layout_result.unwrap();
    assert_eq!(child1_layout.width, 50.0);
    assert_eq!(child1_layout.height, 50.0);
    assert_eq!(child1_layout.left, 10.0);
    assert_eq!(child1_layout.top, 10.0);

    let child2_layout_result = manager.get_node_layout(2);
    assert!(child2_layout_result.is_some());
    let child2_layout = child2_layout_result.unwrap();
    assert_eq!(child2_layout.width, 50.0);
    assert_eq!(child2_layout.height, 50.0);
    assert_eq!(child2_layout.left, 80.0);
    assert_eq!(child2_layout.top, 10.0);
}

#[test]
fn test_vertical_fill() {
    let figma_doc_result = load_doc();
    let manager = load_view("VerticalFill", &figma_doc_result.unwrap());

    let root_layout_result = manager.get_node_layout(0);
    assert!(root_layout_result.is_some());
    let root_layout = root_layout_result.unwrap();

    assert_eq!(root_layout.width, 150.0);
    assert_eq!(root_layout.height, 130.0);

    // Right node should fill to fit root
    let right_layout_result = manager.get_node_layout(2);
    assert!(right_layout_result.is_some());
    let right_layout = right_layout_result.unwrap();
    assert_eq!(right_layout.width, 70.0);
    assert_eq!(right_layout.height, 110.0);
    assert_eq!(right_layout.left, 70.0);
    assert_eq!(right_layout.top, 10.0);

    // Auto fill height node should fill to fit Right
    let auto_fill_height_result = manager.get_node_layout(3);
    assert!(auto_fill_height_result.is_some());
    let auto_fill_height = auto_fill_height_result.unwrap();
    assert_eq!(auto_fill_height.width, 70.0);
    assert_eq!(auto_fill_height.height, 30.0);
}

#[test]
fn test_vertical_fill_resize() {
    let figma_doc_result = load_doc();
    let mut manager = load_view("VerticalFill", &figma_doc_result.unwrap());

    // Increase fixed left node height by 30 pixels
    let result = manager.set_node_size(1, 0, 50, 140);
    assert!(result.changed_layouts.contains_key(&2));
    assert!(result.changed_layouts.contains_key(&3));

    // Right node should be taller by 30 pixels
    let right_layout_result = manager.get_node_layout(2);
    assert!(right_layout_result.is_some());
    let right_layout = right_layout_result.unwrap();
    assert_eq!(right_layout.width, 70.0);
    assert_eq!(right_layout.height, 140.0);
    assert_eq!(right_layout.left, 70.0);
    assert_eq!(right_layout.top, 10.0);

    // Auto fill height node should be taller by 30 pixels
    let auto_fill_height_result = manager.get_node_layout(3);
    assert!(auto_fill_height_result.is_some());
    let auto_fill_height = auto_fill_height_result.unwrap();
    assert_eq!(auto_fill_height.width, 70.0);
    assert_eq!(auto_fill_height.height, 60.0);
}

#[test]
fn test_horizontal_fill() {
    let figma_doc_result = load_doc();
    let manager = load_view("HorizontalFill", &figma_doc_result.unwrap());

    let root_layout_result = manager.get_node_layout(0);
    assert!(root_layout_result.is_some());
    let root_layout = root_layout_result.unwrap();

    assert_eq!(root_layout.width, 130.0);
    assert_eq!(root_layout.height, 150.0);

    // Bottom node should fill to fit root
    let bottom_layout_result = manager.get_node_layout(2);
    assert!(bottom_layout_result.is_some());
    let bottom_layout = bottom_layout_result.unwrap();
    assert_eq!(bottom_layout.width, 110.0);
    assert_eq!(bottom_layout.height, 70.0);
    assert_eq!(bottom_layout.left, 10.0);
    assert_eq!(bottom_layout.top, 70.0);

    // Auto fill width node should fill to fit Bottom
    let auto_fill_width_result = manager.get_node_layout(3);
    assert!(auto_fill_width_result.is_some());
    let auto_fill_width = auto_fill_width_result.unwrap();
    assert_eq!(auto_fill_width.width, 30.0);
    assert_eq!(auto_fill_width.height, 70.0);
}

#[test]
fn test_horizontal_fill_resize() {
    let figma_doc_result = load_doc();
    let mut manager = load_view("HorizontalFill", &figma_doc_result.unwrap());

    // Increase fixed top node width by 30 pixels
    let result = manager.set_node_size(1, 0, 140, 50);
    assert!(result.changed_layouts.contains_key(&2));
    assert!(result.changed_layouts.contains_key(&3));

    // Bottom node should be wider by 30 pixels
    let bottom_layout_result = manager.get_node_layout(2);
    assert!(bottom_layout_result.is_some());
    let bottom_layout = bottom_layout_result.unwrap();
    assert_eq!(bottom_layout.width, 140.0);
    assert_eq!(bottom_layout.height, 70.0);
    assert_eq!(bottom_layout.left, 10.0);
    assert_eq!(bottom_layout.top, 70.0);

    // Auto fill width node should be wider by 30 pixels
    let auto_fill_width_result = manager.get_node_layout(3);
    assert!(auto_fill_width_result.is_some());
    let auto_fill_width = auto_fill_width_result.unwrap();
    assert_eq!(auto_fill_width.width, 60.0);
    assert_eq!(auto_fill_width.height, 70.0);
}

#[test]
fn test_constraints_left_right() {
    let figma_doc_result = load_doc();
    let mut manager = load_view("ConstraintsLayoutLR", &figma_doc_result.unwrap());

    // Change root node size and check that child stretches correctly
    let result = manager.set_node_size(0, 0, 200, 200);
    assert!(result.changed_layouts.contains_key(&1));

    let child_layout_result = manager.get_node_layout(1);
    assert!(child_layout_result.is_some());
    let child_layout = child_layout_result.unwrap();
    assert_eq!(child_layout.width, 150.0);
    assert_eq!(child_layout.height, 50.0);
}

#[test]
fn test_constraints_top_bottom() {
    let figma_doc_result = load_doc();
    let mut manager = load_view("ConstraintsLayoutTB", &figma_doc_result.unwrap());

    // Change root node size and check that child stretches correctly
    let result = manager.set_node_size(0, 0, 200, 200);
    assert!(result.changed_layouts.contains_key(&1));

    let child_layout_result = manager.get_node_layout(1);
    assert!(child_layout_result.is_some());
    let child_layout = child_layout_result.unwrap();
    assert_eq!(child_layout.width, 50.0);
    assert_eq!(child_layout.height, 150.0);
}

#[test]
fn test_constraints_left_right_top_bottom() {
    let figma_doc_result = load_doc();
    let mut manager = load_view("ConstraintsLayoutLRTB", &figma_doc_result.unwrap());

    // Change root node size and check that child stretches correctly
    let result = manager.set_node_size(0, 0, 200, 200);
    assert!(result.changed_layouts.contains_key(&1));

    let child_layout_result = manager.get_node_layout(1);
    assert!(child_layout_result.is_some());
    let child_layout = child_layout_result.unwrap();
    assert_eq!(child_layout.width, 150.0);
    assert_eq!(child_layout.height, 150.0);
}

#[test]
fn test_constraints_center() {
    let figma_doc_result = load_doc();
    let mut manager = load_view("ConstraintsLayoutCenter", &figma_doc_result.unwrap());

    // Change root node size and check that child stretches correctly
    let result = manager.set_node_size(0, 0, 200, 200);
    assert!(result.changed_layouts.contains_key(&1));

    let child_layout_result = manager.get_node_layout(1);
    assert!(child_layout_result.is_some());
    let child_layout = child_layout_result.unwrap();
    assert_eq!(child_layout.width, 50.0);
    assert_eq!(child_layout.height, 50.0);
    assert_eq!(child_layout.left, 75.0);
    assert_eq!(child_layout.top, 75.0);
}

#[test]
fn test_constraints_widget() {
    let figma_doc_result = load_doc();
    let mut manager = load_view("ConstraintsLayoutWidget", &figma_doc_result.unwrap());

    // Change root node size and check that the widget stretches correctly
    let result = manager.set_node_size(0, 0, 200, 200);
    assert!(result.changed_layouts.contains_key(&1));
    assert!(result.changed_layouts.contains_key(&2));
    assert!(result.changed_layouts.contains_key(&3));

    // Widget parent has constraints set, so it should stretch
    let widget_parent_layout_result = manager.get_node_layout(1);
    assert!(widget_parent_layout_result.is_some());
    let widget_parent_layout = widget_parent_layout_result.unwrap();
    assert_eq!(widget_parent_layout.width, 150.0);
    assert_eq!(widget_parent_layout.height, 150.0);

    // Widget itself should fit to size of parent
    let widget_layout_result = manager.get_node_layout(2);
    assert!(widget_layout_result.is_some());
    let widget_layout = widget_layout_result.unwrap();
    assert_eq!(widget_layout.width, 150.0);
    assert_eq!(widget_layout.height, 150.0);

    // Widget child contains the actual data and should also be the same size
    let widget_child_layout_result = manager.get_node_layout(3);
    assert!(widget_child_layout_result.is_some());
    let widget_child_layout = widget_child_layout_result.unwrap();
    assert_eq!(widget_child_layout.width, 150.0);
    assert_eq!(widget_child_layout.height, 150.0);
}

#[test]
fn test_zero_width_height() {
    let figma_doc_result = load_doc();
    let manager = load_view("VectorScale", &figma_doc_result.unwrap());

    // A vector with height 0 and constraints set to scale should result in a height of 0
    let bar_layout_result = manager.get_node_layout(2);
    assert!(bar_layout_result.is_some());
    let bar_layout = bar_layout_result.unwrap();
    assert_eq!(bar_layout.width, 80.0);
    assert_eq!(bar_layout.height, 0.0);

    let bg_layout_result = manager.get_node_layout(3);
    assert!(bg_layout_result.is_some());
    let bg_layout = bg_layout_result.unwrap();
    assert_eq!(bg_layout.width, 20.0);
    assert_eq!(bg_layout.height, 0.0);

    // A vector with width 0 and constraints set to scale should result in a width of 0
    let vert_bar_layout_result = manager.get_node_layout(5);
    assert!(vert_bar_layout_result.is_some());
    let vert_bar_layout = vert_bar_layout_result.unwrap();
    assert_eq!(vert_bar_layout.width, 0.0);
    assert_eq!(vert_bar_layout.height, 20.0);
}

#[test]
fn test_vertical_scroll_contents() {
    let figma_doc_result = load_doc();
    let manager = load_view("VerticalScrolling", &figma_doc_result.unwrap());

    // Test that the layout manager calculates the correct size and content height for a
    // frame that scrolls vertically
    let scroll_frame_layout_result = manager.get_node_layout(1);
    assert!(scroll_frame_layout_result.is_some());
    let scroll_layout = scroll_frame_layout_result.unwrap();
    assert_eq!(scroll_layout.width, 80.0);
    assert_eq!(scroll_layout.height, 100.0);
    assert_eq!(scroll_layout.content_height, 210.0);
}

#[test]
fn test_horizontal_scroll_contents() {
    let figma_doc_result = load_doc();
    let manager = load_view("HorizontalScrolling", &figma_doc_result.unwrap());

    // Test that the layout manager calculates the correct size and content width for a
    // frame that scrolls horizontally
    let scroll_frame_layout_result = manager.get_node_layout(1);
    assert!(scroll_frame_layout_result.is_some());
    let scroll_layout = scroll_frame_layout_result.unwrap();
    assert_eq!(scroll_layout.width, 100.0);
    assert_eq!(scroll_layout.height, 80.0);
    assert_eq!(scroll_layout.content_width, 210.0);
}

// Add tests:
//
// 2. Nodes in horizontal layout
// 3. Nodes in absolute layout
// 4. Remove node
// 5. Remove node, add node
// 6. Remove text node, add node with same ID
// 7. Change node variant
