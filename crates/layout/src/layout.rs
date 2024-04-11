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

use log::{error, trace};
use std::collections::{HashMap, HashSet};
use taffy::prelude::{AvailableSpace, Size, Taffy};
use taffy::tree::LayoutTree;

use figma_import::{layout::LayoutChangedResponse, toolkit_schema, toolkit_style::LayoutStyle};

// Customizations that can applied to a node
struct Customizations {
    sizes: HashMap<i32, Size<u32>>,
}
impl Customizations {
    fn new() -> Self {
        Customizations { sizes: HashMap::new() }
    }

    fn add_size(&mut self, layout_id: i32, width: u32, height: u32) {
        self.sizes.insert(layout_id, Size { width, height });
    }

    fn get_size(&self, layout_id: i32) -> Option<&Size<u32>> {
        self.sizes.get(&layout_id)
    }

    fn remove(&mut self, layout_id: &i32) {
        self.sizes.remove(layout_id);
    }
}

pub struct LayoutManager {
    // taffy object that does all the layout computations
    taffy: Taffy,
    // node id -> Taffy layout
    layouts: HashMap<taffy::node::Node, toolkit_schema::Layout>,
    // A struct that keeps track of all customizations
    customizations: Customizations,
    // Incrementing ID used to keep track of layout changes. Incremented
    // every time relayout is done
    layout_state: i32,

    // layout id -> Taffy node used to calculate layout
    layout_id_to_taffy_node: HashMap<i32, taffy::node::Node>,
    // Taffy node -> layout id
    taffy_node_to_layout_id: HashMap<taffy::node::Node, i32>,
    // layout id -> name
    layout_id_to_name: HashMap<i32, String>,
    // A list of root level layout IDs used to recompute layout whenever
    // something has changed
    root_layout_ids: HashSet<i32>,
}
impl LayoutManager {
    pub fn new() -> Self {
        LayoutManager {
            taffy: Taffy::new(),
            layouts: HashMap::new(),
            customizations: Customizations::new(),
            layout_state: 0,

            layout_id_to_taffy_node: HashMap::new(),
            taffy_node_to_layout_id: HashMap::new(),
            layout_id_to_name: HashMap::new(),
            root_layout_ids: HashSet::new(),
        }
    }

    fn update_layout_internal(
        &mut self,
        layout_id: i32,
        parent_layout_id: i32,
        changed: &mut HashMap<i32, toolkit_schema::Layout>,
    ) {
        let node = self.layout_id_to_taffy_node.get(&layout_id);
        if let Some(node) = node {
            let layout = self.taffy.layout(*node);
            if let Ok(layout) = layout {
                let layout = toolkit_schema::Layout::from_taffy_layout(layout);
                let old_layout = self.layouts.get(node);
                let mut layout_changed = false;
                if let Some(old_layout) = old_layout {
                    if &layout != old_layout {
                        layout_changed = true;
                    }
                } else {
                    layout_changed = true;
                }

                // If a layout change is detected, add the node that changed and its parent.
                // The parent is needed because layout positioning for a child is done in the
                // parent's layout function.
                if layout_changed {
                    changed.insert(layout_id, layout.clone());
                    if parent_layout_id >= 0 {
                        if !changed.contains_key(&parent_layout_id) {
                            let parent_node = self.layout_id_to_taffy_node.get(&parent_layout_id);
                            if let Some(parent_node) = parent_node {
                                let parent_layout = self.layouts.get(parent_node);
                                if let Some(parent_layout) = parent_layout {
                                    changed.insert(parent_layout_id, parent_layout.clone());
                                }
                            }
                        }
                    }
                    self.layouts.insert(*node, layout);
                }
            }

            let children_result = self.taffy.children(*node);
            match children_result {
                Ok(children) => {
                    for child in children {
                        let child_layout_id = self.taffy_node_to_layout_id.get(&child);
                        if let Some(child_layout_id) = child_layout_id {
                            self.update_layout_internal(*child_layout_id, layout_id, changed);
                        }
                    }
                }
                Err(e) => {
                    error!("taffy children error: {}", e);
                }
            }
        }
    }

    // Update the layout for the specified layout_id and its children, and
    // return a hash of layouts that changed.
    fn update_layout(&mut self, layout_id: i32) -> HashMap<i32, toolkit_schema::Layout> {
        let mut changed: HashMap<i32, toolkit_schema::Layout> = HashMap::new();
        self.update_layout_internal(layout_id, -1, &mut changed);
        changed
    }

    // Get the computed layout for the given node
    pub fn get_node_layout(&self, layout_id: i32) -> Option<toolkit_schema::Layout> {
        let node = self.layout_id_to_taffy_node.get(&layout_id);
        if let Some(node) = node {
            let layout = self.taffy.layout(*node);
            if let Ok(layout) = layout {
                return Some(toolkit_schema::Layout::from_taffy_layout(layout));
            }
        }
        None
    }

    pub fn update_children(&mut self, parent_layout_id: i32, children: &Vec<i32>) {
        if let Some(parent_node) = self.layout_id_to_taffy_node.get(&parent_layout_id) {
            let child_nodes: Vec<_> = children
                .iter()
                .filter_map(|child_id| self.layout_id_to_taffy_node.get(child_id).copied())
                .collect();
            if let Err(e) = self.taffy.set_children(*parent_node, child_nodes.as_slice()) {
                error!("error setting children! {:?}", e);
            }
        }
    }

    pub fn add_style_measure(
        &mut self,
        layout_id: i32,
        parent_layout_id: i32,
        child_index: i32,
        style: LayoutStyle,
        name: String,
        measure_func: impl Send + Sync + 'static + Fn(i32, f32, f32, f32, f32) -> (f32, f32),
    ) {
        trace!("add_view_measure layoutId {}", layout_id);
        let layout_measure_func = move |size: Size<Option<f32>>,
                                        available_size: Size<AvailableSpace>|
              -> Size<f32> {
            let width = if let Some(w) = size.width { w } else { 0.0 };
            let height = if let Some(h) = size.height { h } else { 0.0 };
            let available_width = match available_size.width {
                AvailableSpace::Definite(w) => w,
                AvailableSpace::MaxContent => f32::MAX,
                AvailableSpace::MinContent => 0.0,
            };
            let available_height = match available_size.height {
                AvailableSpace::Definite(h) => h,
                AvailableSpace::MaxContent => f32::MAX,
                AvailableSpace::MinContent => 0.0,
            };
            let result = measure_func(layout_id, width, height, available_width, available_height);
            Size { width: result.0, height: result.1 }
        };

        self.add_style(
            layout_id,
            parent_layout_id,
            child_index,
            style,
            name,
            Some(taffy::node::MeasureFunc::Boxed(Box::new(layout_measure_func))),
            None,
            None,
        )
    }

    pub fn add_style(
        &mut self,
        layout_id: i32,
        parent_layout_id: i32,
        child_index: i32,
        style: LayoutStyle,
        name: String,
        measure_func: Option<taffy::node::MeasureFunc>,
        fixed_width: Option<i32>,
        fixed_height: Option<i32>,
    ) {
        let mut node_style: taffy::style::Style = (&style).into();

        self.apply_customizations(layout_id, &mut node_style);
        self.apply_fixed_size(&mut node_style, fixed_width, fixed_height);

        let node = self.layout_id_to_taffy_node.get(&layout_id);
        if let Some(node) = node {
            // We already have this view in our tree. Update it's style
            let result = self.taffy.set_style(*node, node_style);
            if let Some(e) = result.err() {
                error!("taffy set_style error: {}", e);
            }
        } else {
            // This is a new view to add to our tree. Create a new node and add it
            let result = if let Some(measure_func) = measure_func {
                self.taffy.new_leaf_with_measure(node_style, measure_func)
            } else {
                self.taffy.new_leaf(node_style)
            };
            match result {
                Ok(node) => {
                    let parent_node = self.layout_id_to_taffy_node.get(&parent_layout_id);
                    if let Some(parent_node) = parent_node {
                        if child_index < 0 {
                            // Don't bother inserting into the parent's child list. The caller will invoke set_children
                            // manually instead.
                            self.taffy_node_to_layout_id.insert(node, layout_id);
                            self.layout_id_to_taffy_node.insert(layout_id, node);
                            self.layout_id_to_name.insert(layout_id, name.clone());
                        } else {
                            // This has a parent node, so add it as a child
                            let children_result = self.taffy.children(*parent_node);
                            match children_result {
                                Ok(mut children) => {
                                    if child_index as usize > children.len() {
                                        error!("omg! child index {} > children.len {} for node {} going into node {:?}", child_index, children.len(), name, self.layout_id_to_name.get(&parent_layout_id));
                                    }
                                    children.insert(child_index as usize, node);
                                    let set_children_result =
                                        self.taffy.set_children(*parent_node, children.as_ref());
                                    if let Some(e) = set_children_result.err() {
                                        error!("taffy set_children error: {}", e);
                                    } else {
                                        self.taffy_node_to_layout_id.insert(node, layout_id);
                                        self.layout_id_to_taffy_node.insert(layout_id, node);
                                        self.layout_id_to_name.insert(layout_id, name.clone());
                                    }
                                }
                                Err(e) => {
                                    error!("taffy_children error: {}", e);
                                }
                            }
                        }
                    } else {
                        // No parent; this is a root node
                        self.taffy_node_to_layout_id.insert(node, layout_id);
                        self.layout_id_to_taffy_node.insert(layout_id, node);
                        self.layout_id_to_name.insert(layout_id, name.clone());
                        self.root_layout_ids.insert(layout_id);
                    }
                }
                Err(e) => {
                    error!("taffy_new_leaf error: {}", e);
                }
            }
        }
    }

    pub fn remove_view(
        &mut self,
        layout_id: i32,
        root_layout_id: i32,
        compute_layout: bool,
    ) -> LayoutChangedResponse {
        let taffy_node = self.layout_id_to_taffy_node.get(&layout_id);
        if let Some(taffy_node) = taffy_node {
            let parent = self.taffy.parent(*taffy_node);
            if let Some(parent) = parent {
                // We need to mark the parent as dirty, otherwise layout doesn't get recomputed
                // on the parent, so if this node is in middle of a list, there will be a gap.
                // This may be a bug with taffy.
                let dirty_result = self.taffy.mark_dirty(parent);
                if let Some(e) = dirty_result.err() {
                    error!("taffy dirty error: {}", e);
                }
            }
            let remove_result = self.taffy.remove(*taffy_node);
            match remove_result {
                Ok(removed_node) => {
                    // Remove from all our hashes
                    self.taffy_node_to_layout_id.remove(&removed_node);
                    self.layout_id_to_taffy_node.remove(&layout_id);
                    self.layout_id_to_name.remove(&layout_id);
                    self.root_layout_ids.remove(&layout_id);
                    self.customizations.remove(&layout_id);
                }
                Err(e) => {
                    error!("taffy remove error: {}", e);
                }
            }
        } else {
            error!("no taffy node for layout_id {}", layout_id);
        }

        if compute_layout {
            self.compute_node_layout(root_layout_id)
        } else {
            LayoutChangedResponse::unchanged(self.layout_state)
        }
    }

    // Set the given node's size to a fixed value, recompute layout, and return
    // the changed nodes
    pub fn set_node_size(
        &mut self,
        layout_id: i32,
        root_layout_id: i32,
        width: u32,
        height: u32,
    ) -> LayoutChangedResponse {
        let node = self.layout_id_to_taffy_node.get(&layout_id);
        if let Some(node) = node {
            let result = self.taffy.style(*node);
            match result {
                Ok(style) => {
                    let mut new_style = style.clone();
                    new_style.min_size.width = taffy::prelude::Dimension::Points(width as f32);
                    new_style.min_size.height = taffy::prelude::Dimension::Points(height as f32);
                    new_style.size.width = taffy::prelude::Dimension::Points(width as f32);
                    new_style.size.height = taffy::prelude::Dimension::Points(height as f32);
                    new_style.max_size.width = taffy::prelude::Dimension::Points(width as f32);
                    new_style.max_size.height = taffy::prelude::Dimension::Points(height as f32);

                    let result = self.taffy.set_style(*node, new_style);
                    if let Some(e) = result.err() {
                        error!("taffy set_style error: {}", e);
                    } else {
                        self.customizations.add_size(layout_id, width, height);
                    }
                }
                Err(e) => {
                    error!("taffy style error: {}", e);
                }
            }
        }

        self.compute_node_layout(root_layout_id)
    }

    // Apply any customizations that have been saved for this node
    fn apply_customizations(&self, layout_id: i32, style: &mut taffy::style::Style) {
        let size = self.customizations.get_size(layout_id);
        if let Some(size) = size {
            style.min_size.width = taffy::prelude::Dimension::Points(size.width as f32);
            style.min_size.height = taffy::prelude::Dimension::Points(size.height as f32);
            style.size.width = taffy::prelude::Dimension::Points(size.width as f32);
            style.size.height = taffy::prelude::Dimension::Points(size.height as f32);
            style.max_size.width = taffy::prelude::Dimension::Points(size.width as f32);
            style.max_size.height = taffy::prelude::Dimension::Points(size.height as f32);
        }
    }

    fn apply_fixed_size(
        &self,
        style: &mut taffy::style::Style,
        fixed_width: Option<i32>,
        fixed_height: Option<i32>,
    ) {
        if let Some(fixed_width) = fixed_width {
            style.min_size.width = taffy::prelude::Dimension::Points(fixed_width as f32);
        }
        if let Some(fixed_height) = fixed_height {
            style.min_size.height = taffy::prelude::Dimension::Points(fixed_height as f32);
        }
    }

    // Compute the layout on the node with the given layout_id. This should always be
    // a root level node.
    pub fn compute_node_layout(&mut self, layout_id: i32) -> LayoutChangedResponse {
        trace!("compute_node_layout {}", layout_id);
        let node = self.layout_id_to_taffy_node.get(&layout_id);
        if let Some(node) = node {
            let result = self.taffy.compute_layout(
                *node,
                Size {
                    // TODO get this size from somewhere
                    height: AvailableSpace::Definite(500.0),
                    width: AvailableSpace::Definite(500.0),
                },
            );
            if let Some(e) = result.err() {
                error!("compute_node_layout_internal: compute_layoute error: {}", e);
            }
        }

        let changed_layouts = self.update_layout(layout_id);
        self.layout_state = self.layout_state + 1;

        LayoutChangedResponse { layout_state: self.layout_state, changed_layouts: changed_layouts }
    }

    pub fn print_layout(self, layout_id: i32, print_func: fn(String) -> ()) {
        self.print_layout_recurse(layout_id, "".to_string(), print_func);
    }

    fn print_layout_recurse(&self, layout_id: i32, space: String, print_func: fn(String) -> ()) {
        let layout_node = self.layout_id_to_taffy_node.get(&layout_id);
        if let Some(layout_node) = layout_node {
            let layout = self.taffy.layout(*layout_node);
            if let Ok(layout) = layout {
                let name_result = self.layout_id_to_name.get(&layout_id);
                if let Some(name) = name_result {
                    print_func(format!("{}Node {} {}:", space, name, layout_id));
                    print_func(format!("  {}{:?}", space, layout));
                }

                let children_result = self.taffy.children(*layout_node);
                if let Ok(children) = children_result {
                    for child in children {
                        let layout_id = self.taffy_node_to_layout_id.get(&child);
                        if let Some(child_layout_id) = layout_id {
                            self.print_layout_recurse(
                                *child_layout_id,
                                format!("{}  ", space),
                                print_func,
                            );
                        }
                    }
                }
            }
        }
    }
}
