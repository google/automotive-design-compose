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

use lazy_static::lazy_static;
use log::{error, info};
use std::collections::{HashMap, HashSet};
use std::sync::{Mutex, MutexGuard};
use taffy::prelude::{AvailableSpace, Size, Taffy};
use taffy::tree::LayoutTree;

use figma_import::{layout::LayoutChangedResponse, toolkit_schema};

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

struct LayoutManager {
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
    // layout id -> View
    layout_id_to_view: HashMap<i32, toolkit_schema::View>,
    // A list of root level layout IDs used to recompute layout whenever
    // something has changed
    root_layout_ids: HashSet<i32>,
}
impl LayoutManager {
    fn new() -> Self {
        LayoutManager {
            layouts: HashMap::new(),
            customizations: Customizations::new(),
            layout_state: 0,

            layout_id_to_taffy_node: HashMap::new(),
            taffy_node_to_layout_id: HashMap::new(),
            layout_id_to_view: HashMap::new(),
            root_layout_ids: HashSet::new(),
        }
    }

    fn recompute_layouts(&mut self, taffy: &mut Taffy) -> LayoutChangedResponse {
        info!("recompute_layouts {}", self.root_layout_ids.len());
        for layout_id in &self.root_layout_ids {
            let node = self.layout_id_to_taffy_node.get(layout_id);
            if let Some(node) = node {
                let result = taffy.compute_layout(
                    *node,
                    Size {
                        // TODO get this size from somewhere
                        height: AvailableSpace::Definite(500.0),
                        width: AvailableSpace::Definite(500.0),
                    },
                );
                if let Some(e) = result.err() {
                    error!("recompute_layout: compute_layoute error: {}", e);
                }
            }
        }

        let changed_layouts = self.update_layouts(taffy);
        self.layout_state = self.layout_state + 1;

        LayoutChangedResponse { layout_state: self.layout_state, changed_layouts: changed_layouts }
    }

    // Update the hash of layouts and return a list of node IDs whose layouts
    // changed
    fn update_layouts(&mut self, taffy: &Taffy) -> HashMap<i32, toolkit_schema::Layout> {
        fn update_layout(
            layout_id: i32,
            parent_layout_id: i32,
            manager: &mut LayoutManager,
            taffy: &Taffy,
            changed: &mut HashMap<i32, toolkit_schema::Layout>,
        ) {
            let node = manager.layout_id_to_taffy_node.get(&layout_id);
            if let Some(node) = node {
                let layout = taffy.layout(*node);
                if let Ok(layout) = layout {
                    let layout = toolkit_schema::Layout::from_taffy_layout(layout);
                    let old_layout = manager.layouts.get(node);
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
                                let parent_node =
                                    manager.layout_id_to_taffy_node.get(&parent_layout_id);
                                if let Some(parent_node) = parent_node {
                                    let parent_layout = manager.layouts.get(parent_node);
                                    if let Some(parent_layout) = parent_layout {
                                        changed.insert(parent_layout_id, parent_layout.clone());
                                    }
                                }
                            }
                        }
                        manager.layouts.insert(*node, layout);
                    }
                }

                let children_result = taffy.children(*node);
                match children_result {
                    Ok(children) => {
                        for child in children {
                            let child_layout_id = manager.taffy_node_to_layout_id.get(&child);
                            if let Some(child_layout_id) = child_layout_id {
                                update_layout(*child_layout_id, layout_id, manager, taffy, changed);
                            }
                        }
                    }
                    Err(e) => {
                        error!("taffy children error: {}", e);
                    }
                }
            }
        }

        let mut changed: HashMap<i32, toolkit_schema::Layout> = HashMap::new();
        for layout_id in &self.root_layout_ids.clone() {
            update_layout(*layout_id, -1, self, &taffy, &mut changed);
        }
        changed
    }

    // Get the computed layout for the given node
    fn get_node_layout(&self, layout_id: i32) -> Option<toolkit_schema::Layout> {
        let taffy = taffy();

        let node = self.layout_id_to_taffy_node.get(&layout_id);
        if let Some(node) = node {
            let layout = taffy.layout(*node);
            if let Ok(layout) = layout {
                return Some(toolkit_schema::Layout::from_taffy_layout(layout));
            }
        }
        None
    }

    // If a base view exists, copy the base's margin to the style. This is necessary to
    // preserve the position of an instance of a component with variants where the variant
    // changes due to an interaction, or if a component has been replaced with another
    // composable. Without this, the new variant or replaced component displayed would have
    // its position reset to 0, 0.
    fn apply_base_style(
        &self,
        style: &mut taffy::style::Style,
        base_view: &Option<toolkit_schema::View>,
    ) {
        if let Some(view) = base_view {
            let base_style: taffy::style::Style = (&view.style).into();
            style.margin = base_style.margin;
            style.position = base_style.position;
        }
    }

    fn add_view(
        &mut self,
        layout_id: i32,
        parent_layout_id: i32,
        child_index: i32,
        view: toolkit_schema::View,
        base_view: Option<toolkit_schema::View>,
        measure_func: Option<taffy::node::MeasureFunc>,
    ) {
        let mut taffy = taffy();

        let mut node_style: taffy::style::Style = (&view.style).into();
        if view.name.starts_with("FrameTop") {
            println!("{} VIEWSTYLE:", view.name);
            println!("{:#?}", view.style);
        }

        self.apply_base_style(&mut node_style, &base_view);
        self.apply_customizations(layout_id, &mut node_style);

        let node = self.layout_id_to_taffy_node.get(&layout_id);
        if let Some(node) = node {
            // We already have this view in our tree. Update it's style
            let result = taffy.set_style(*node, node_style);
            if let Some(e) = result.err() {
                error!("taffy set_style error: {}", e);
            }
        } else {
            // This is a new view to add to our tree. Create a new node and add it
            let result = if let Some(measure_func) = measure_func {
                taffy.new_leaf_with_measure(node_style, measure_func)
            } else {
                taffy.new_leaf(node_style)
            };
            match result {
                Ok(node) => {
                    let parent_node = self.layout_id_to_taffy_node.get(&parent_layout_id);
                    if let Some(parent_node) = parent_node {
                        // This has a parent node, so add it as a child
                        let children_result = taffy.children(*parent_node);
                        match children_result {
                            Ok(mut children) => {
                                children.insert(child_index as usize, node);
                                let set_children_result =
                                    taffy.set_children(*parent_node, children.as_ref());
                                if let Some(e) = set_children_result.err() {
                                    error!("taffy set_children error: {}", e);
                                } else {
                                    self.taffy_node_to_layout_id.insert(node, layout_id);
                                    self.layout_id_to_taffy_node.insert(layout_id, node);
                                    self.layout_id_to_view.insert(layout_id, view.clone());
                                }
                            }
                            Err(e) => {
                                error!("taffy_children error: {}", e);
                            }
                        }
                    } else {
                        // No parent; this is a root node
                        self.taffy_node_to_layout_id.insert(node, layout_id);
                        self.layout_id_to_taffy_node.insert(layout_id, node);
                        self.layout_id_to_view.insert(layout_id, view.clone());
                        self.root_layout_ids.insert(layout_id);
                    }
                }
                Err(e) => {
                    error!("taffy_new_leaf error: {}", e);
                }
            }
        }
    }

    fn remove_view(&mut self, layout_id: i32, compute_layout: bool) -> LayoutChangedResponse {
        let mut taffy = taffy();
        self.remove_view_internal(layout_id, compute_layout, &mut taffy)
    }

    fn remove_view_internal(
        &mut self,
        layout_id: i32,
        compute_layout: bool,
        taffy: &mut Taffy,
    ) -> LayoutChangedResponse {
        let taffy_node = self.layout_id_to_taffy_node.get(&layout_id);
        if let Some(taffy_node) = taffy_node {
            let parent = taffy.parent(*taffy_node);
            if let Some(parent) = parent {
                // We need to mark the parent as dirty, otherwise layout doesn't get recomputed
                // on the parent, so if this node is in middle of a list, there will be a gap.
                // This may be a bug with taffy.
                let dirty_result = taffy.mark_dirty(parent);
                if let Some(e) = dirty_result.err() {
                    error!("taffy dirty error: {}", e);
                }
            }
            let remove_result = taffy.remove(*taffy_node);
            match remove_result {
                Ok(removed_node) => {
                    // Remove from all our hashes
                    self.taffy_node_to_layout_id.remove(&removed_node);
                    self.layout_id_to_taffy_node.remove(&layout_id);
                    self.layout_id_to_view.remove(&layout_id);
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
            self.recompute_layouts(taffy)
        } else {
            LayoutChangedResponse::unchanged(self.layout_state)
        }
    }

    // Recursively remove a node's children and then the node itself
    fn remove_recursive(&mut self, layout_id: i32, taffy: &mut Taffy) {
        let node = self.layout_id_to_taffy_node.get(&layout_id);
        if let Some(node) = node {
            let children_result = taffy.children(*node);
            if let Ok(children) = children_result {
                for child in children.iter() {
                    let child_layout_id = self.taffy_node_to_layout_id.get(child);
                    if let Some(child_layout_id) = child_layout_id {
                        self.remove_recursive(*child_layout_id, taffy);
                    }
                }
            }
        }
        self.remove_view_internal(layout_id, false, taffy);
    }

    // Remove all nodes
    fn clear(&mut self) {
        let mut taffy = taffy();
        for layout_id in &self.root_layout_ids.clone() {
            self.remove_recursive(*layout_id, &mut taffy);
        }
    }

    // Set the given node's size to a fixed value, recompute layout, and return
    // the changed nodes
    fn set_node_size(
        &mut self,
        layout_id: i32,
        root_layout_id: i32,
        width: u32,
        height: u32,
    ) -> LayoutChangedResponse {
        let mut taffy = taffy();

        let node = self.layout_id_to_taffy_node.get(&layout_id);
        if let Some(node) = node {
            let result = taffy.style(*node);
            match result {
                Ok(style) => {
                    let mut new_style = style.clone();
                    new_style.min_size.width = taffy::prelude::Dimension::Points(width as f32);
                    new_style.min_size.height = taffy::prelude::Dimension::Points(height as f32);
                    new_style.size.width = taffy::prelude::Dimension::Points(width as f32);
                    new_style.size.height = taffy::prelude::Dimension::Points(height as f32);
                    new_style.max_size.width = taffy::prelude::Dimension::Points(width as f32);
                    new_style.max_size.height = taffy::prelude::Dimension::Points(height as f32);

                    let result = taffy.set_style(*node, new_style);
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

        self.compute_node_layout_internal(root_layout_id, &mut taffy)
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

    // Compute the layout on the node with the given layout_id. This should always be
    // a root level node.
    fn compute_node_layout(&mut self, layout_id: i32) -> LayoutChangedResponse {
        let mut taffy = taffy();
        self.compute_node_layout_internal(layout_id, &mut taffy)
    }

    fn compute_node_layout_internal(
        &mut self,
        layout_id: i32,
        taffy: &mut Taffy,
    ) -> LayoutChangedResponse {
        info!("compute_node_layout {}", layout_id);
        let node = self.layout_id_to_taffy_node.get(&layout_id);
        if let Some(node) = node {
            let result = taffy.compute_layout(
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

        let changed_layouts = self.update_layouts(&taffy);
        self.layout_state = self.layout_state + 1;

        LayoutChangedResponse { layout_state: self.layout_state, changed_layouts: changed_layouts }
    }
}

lazy_static! {
    static ref LAYOUT_MANAGER: Mutex<LayoutManager> = Mutex::new(LayoutManager::new());
    static ref TAFFY: Mutex<Taffy> = Mutex::new(Taffy::new());
}

fn manager() -> MutexGuard<'static, LayoutManager> {
    LAYOUT_MANAGER.lock().unwrap()
}

fn taffy() -> MutexGuard<'static, Taffy> {
    TAFFY.lock().unwrap()
}

pub fn unchanged_response() -> LayoutChangedResponse {
    LayoutChangedResponse::unchanged(manager().layout_state)
}

pub fn get_node_layout(layout_id: i32) -> Option<toolkit_schema::Layout> {
    manager().get_node_layout(layout_id)
}

pub fn add_view(
    layout_id: i32,
    parent_layout_id: i32,
    child_index: i32,
    view: toolkit_schema::View,
    base_view: Option<toolkit_schema::View>,
) {
    manager().add_view(layout_id, parent_layout_id, child_index, view, base_view, None)
}

pub fn add_view_measure(
    layout_id: i32,
    parent_layout_id: i32,
    child_index: i32,
    view: toolkit_schema::View,
    measure_func: impl Send + Sync + 'static + Fn(i32, f32, f32, f32, f32) -> (f32, f32),
) {
    info!("add_view_measure layoutId {}", layout_id);
    let layout_measure_func =
        move |size: Size<Option<f32>>, available_size: Size<AvailableSpace>| -> Size<f32> {
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

    manager().add_view(
        layout_id,
        parent_layout_id,
        child_index,
        view,
        None,
        Some(taffy::node::MeasureFunc::Boxed(Box::new(layout_measure_func))),
    )
}

pub fn remove_view(layout_id: i32, compute_layout: bool) -> LayoutChangedResponse {
    manager().remove_view(layout_id, compute_layout)
}

pub fn clear_views() {
    manager().clear()
}

pub fn set_node_size(
    layout_id: i32,
    root_layout_id: i32,
    width: u32,
    height: u32,
) -> LayoutChangedResponse {
    manager().set_node_size(layout_id, root_layout_id, width, height)
}

pub fn compute_node_layout(layout_id: i32) -> LayoutChangedResponse {
    manager().compute_node_layout(layout_id)
}

pub fn print_layout(layout_id: i32) {
    let manager = &mut LAYOUT_MANAGER.lock().unwrap();
    let taffy = &mut TAFFY.lock().unwrap();
    print_layout_recurse(layout_id, manager, taffy, "".to_string());
}

fn print_layout_recurse(layout_id: i32, manager: &LayoutManager, taffy: &Taffy, space: String) {
    let layout_node = manager.layout_id_to_taffy_node.get(&layout_id);
    if let Some(layout_node) = layout_node {
        let layout = taffy.layout(*layout_node);
        if let Ok(layout) = layout {
            let view_result = manager.layout_id_to_view.get(&layout_id);
            if let Some(view) = view_result {
                println!("{}Node {} {}:", space, view.name, layout_id);
                println!("  {}{:?}", space, layout);
            }

            let children_result = taffy.children(*layout_node);
            if let Ok(children) = children_result {
                for child in children {
                    let layout_id = manager.taffy_node_to_layout_id.get(&child);
                    if let Some(child_layout_id) = layout_id {
                        print_layout_recurse(
                            *child_layout_id,
                            manager,
                            taffy,
                            format!("{}  ", space),
                        );
                    }
                }
            }
        }
    }
}
