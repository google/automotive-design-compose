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

// We use serde to serialize and deserialize our "toolkit style" views. Because we need
// serialization, we use our own replacement for ViewStyle which can be serialized and
// retain image references.
use serde::{Deserialize, Serialize};

use crate::figma_schema;
use crate::figma_schema::Rectangle;
use crate::reaction_schema::FrameExtras;
use crate::reaction_schema::Reaction;
use crate::toolkit_style::{StyledTextRun, ViewStyle};
use std::collections::HashMap;

pub use crate::figma_schema::{FigmaColor, OverflowDirection, StrokeCap, VariableAlias};

/// Shape of a view, either a rect or a path of some kind.
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum ViewShape {
    Rect {
        is_mask: bool,
    },
    RoundRect {
        corner_radius: [f32; 4],
        corner_smoothing: f32,
        is_mask: bool,
    },
    Path {
        path: Vec<crate::vector_schema::Path>,
        stroke: Vec<crate::vector_schema::Path>,
        is_mask: bool,
    },
    Arc {
        path: Vec<crate::vector_schema::Path>,
        stroke: Vec<crate::vector_schema::Path>,
        stroke_cap: StrokeCap,
        start_angle_degrees: f32,
        sweep_angle_degrees: f32,
        inner_radius: f32,
        corner_radius: f32,
        is_mask: bool,
    },
    VectorRect {
        path: Vec<crate::vector_schema::Path>,
        stroke: Vec<crate::vector_schema::Path>,
        corner_radius: [f32; 4],
        is_mask: bool,
    },
}

/// Details that are unique to each view type.
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum ViewData {
    Container { shape: ViewShape, children: Vec<View> },
    Text { content: String },
    StyledText { content: Vec<StyledTextRun> },
}

/// Figma component properties can be "overridden" in the UI. These overrides
/// are applied in the node tree we get back from Figma, so we don't have to
/// worry about them, unless we present a variant of a component as a result of
/// an interaction. In that case, we need to figure out what has been overridden
/// and apply it to the variant. This struct keeps track of those overrides that
/// need to be applied to variant instances.
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub struct ComponentOverrides {
    pub style: Option<ViewStyle>,
    pub data: ComponentContentOverride,
}

#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum ComponentContentOverride {
    None,
    Text(String),
    StyledText(Vec<StyledTextRun>),
}

/// Details on the Figma component that this view is an instance of.
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub struct ComponentInfo {
    pub id: String,
    pub name: String,
    pub component_set_name: String,
    // Currently we only figure out overrides for the root of a component, but soon
    // we will want to compute them for the whole tree.
    pub overrides: Option<ComponentOverrides>,
}

/// This struct contains information for scrolling on a frame. It combines the
/// scroll overflow direction, which comes from a frame, and the bool
/// paged_scrolling, which comes from the vsw-extended-layout plugin.
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub struct ScrollInfo {
    pub overflow: OverflowDirection,
    pub paged_scrolling: bool,
}
impl Default for ScrollInfo {
    fn default() -> Self {
        ScrollInfo { overflow: OverflowDirection::None, paged_scrolling: false }
    }
}

/// This enum may be used as a hint by the DesignCompose renderer implementation
/// to determine if it is important for the content to be rendered identically on different platforms.
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum RenderMethod {
    None,
    PixelPerfect,
}

/// Represents a toolkit View (like a Composable).
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub struct View {
    // id doesn't exist in vsw toolkit, but is useful for tracing from Figma node to output
    pub id: String,
    // name doesn't exist in vsw toolkit, but is also useful for tracing.
    pub name: String,
    // information on the component that this view is an instance of (if any).
    pub component_info: Option<ComponentInfo>,
    // interactivity
    pub reactions: Option<Vec<Reaction>>,
    // interactivity related frame properties
    pub frame_extras: Option<FrameExtras>,
    // overflow (scrolling) data
    pub scroll_info: ScrollInfo,
    // common style
    pub style: ViewStyle,
    pub data: ViewData,
    pub design_absolute_bounding_box: Option<Rectangle>,
    pub render_method: RenderMethod,
}
impl View {
    pub(crate) fn new_rect(
        id: &String,
        name: &String,
        shape: ViewShape,
        style: ViewStyle,
        component_info: Option<ComponentInfo>,
        reactions: Option<Vec<Reaction>>,
        scroll_info: ScrollInfo,
        frame_extras: Option<FrameExtras>,
        design_absolute_bounding_box: Option<Rectangle>,
        render_method: RenderMethod,
    ) -> View {
        View {
            id: id.clone(),
            name: name.clone(),
            component_info,
            reactions,
            style,
            frame_extras,
            scroll_info,
            data: ViewData::Container { shape, children: vec![] },
            design_absolute_bounding_box,
            render_method,
        }
    }
    pub(crate) fn new_text(
        id: &String,
        name: &String,
        style: ViewStyle,
        component_info: Option<ComponentInfo>,
        reactions: Option<Vec<Reaction>>,
        text: &str,
        design_absolute_bounding_box: Option<Rectangle>,
        render_method: RenderMethod,
    ) -> View {
        View {
            id: id.clone(),
            name: name.clone(),
            component_info,
            reactions,
            style,
            frame_extras: None,
            scroll_info: ScrollInfo::default(),
            data: ViewData::Text { content: text.into() },
            design_absolute_bounding_box,
            render_method,
        }
    }
    pub(crate) fn new_styled_text(
        id: &String,
        name: &String,
        style: ViewStyle,
        component_info: Option<ComponentInfo>,
        reactions: Option<Vec<Reaction>>,
        text: Vec<StyledTextRun>,
        design_absolute_bounding_box: Option<Rectangle>,
        render_method: RenderMethod,
    ) -> View {
        View {
            id: id.clone(),
            name: name.clone(),
            style,
            component_info,
            reactions,
            frame_extras: None,
            scroll_info: ScrollInfo::default(),
            data: ViewData::StyledText { content: text },
            design_absolute_bounding_box,
            render_method,
        }
    }
    pub(crate) fn add_child(&mut self, child: View) {
        if let ViewData::Container { children, .. } = &mut self.data {
            children.push(child);
        }
    }
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct Mode {
    pub id: String,
    pub name: String,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct Collection {
    pub id: String,
    pub name: String,
    pub default_mode_id: String,
    pub modes: Vec<Mode>,
}

// We redeclare VariableValue instead of using the one from figma_schema because
// the "untagged" attribute there prevents serde_reflection from being able to
// run properly.
#[derive(Deserialize, Serialize, Debug, Clone)]
pub enum VariableValue {
    Bool(bool),
    Number(f32),
    Text(String),
    Color(FigmaColor),
    Alias(VariableAlias),
}
impl VariableValue {
    fn from_figma_value(v: &figma_schema::VariableValue) -> VariableValue {
        match v {
            figma_schema::VariableValue::Boolean(b) => VariableValue::Bool(b.clone()),
            figma_schema::VariableValue::Float(f) => VariableValue::Number(f.clone()),
            figma_schema::VariableValue::String(s) => VariableValue::Text(s.clone()),
            figma_schema::VariableValue::Color(c) => VariableValue::Color(c.clone()),
            figma_schema::VariableValue::Alias(a) => VariableValue::Alias(a.clone()),
        }
    }
}

// Each variable contains a map of possible values. This data structure helps
// keep track of that data and contains functions to retrieve the value of a
// variable given a mode.
#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct VariableValueMap {
    pub values_by_mode: HashMap<String, VariableValue>,
}
impl VariableValueMap {
    fn from_figma_map(map: &HashMap<String, figma_schema::VariableValue>) -> VariableValueMap {
        let mut values_by_mode: HashMap<String, VariableValue> = HashMap::new();
        for (mode_id, value) in map.iter() {
            values_by_mode.insert(mode_id.clone(), VariableValue::from_figma_value(value));
        }
        VariableValueMap { values_by_mode }
    }
    fn get_bool(&self, mode_id: &String, var_map: &VariableMap) -> Option<bool> {
        // TODO return result?
        if let Some(value) = self.values_by_mode.get(mode_id) {
            if let VariableValue::Bool(b) = value {
                return Some(*b);
            } else if let VariableValue::Alias(a) = value {
                let maybe_var = var_map.get_variable(&a.id);
                if let Some(alias_var) = maybe_var {
                    return alias_var.get_bool(mode_id, var_map);
                } else {
                    // TODO can't find alias error
                }
            } else {
                // TODO invalid variable value error
            }
        } else {
            // TODO invalid variable for mode error
        }
        None
    }
    fn get_number(&self, mode_id: &String, var_map: &VariableMap) -> Option<f32> {
        if let Some(value) = self.values_by_mode.get(mode_id) {
            if let VariableValue::Number(f) = value {
                return Some(*f);
            } else if let VariableValue::Alias(a) = value {
                let maybe_var = var_map.get_variable(&a.id);
                if let Some(alias_var) = maybe_var {
                    return alias_var.get_number(mode_id, var_map);
                } else {
                    // TODO can't find alias error
                }
            } else {
                // TODO invalid variable value error
            }
        } else {
            // TODO invalid variable for mode error
        }
        None
    }
    fn get_string(&self, mode_id: &String, var_map: &VariableMap) -> Option<String> {
        if let Some(value) = self.values_by_mode.get(mode_id) {
            if let VariableValue::Text(s) = value {
                return Some(s.clone());
            } else if let VariableValue::Alias(a) = value {
                let maybe_var = var_map.get_variable(&a.id);
                if let Some(alias_var) = maybe_var {
                    return alias_var.get_string(mode_id, var_map);
                } else {
                    // TODO can't find alias error
                }
            } else {
                // TODO invalid variable value error
            }
        } else {
            // TODO invalid variable for mode error
        }
        None
    }
    fn get_color(&self, mode_id: &String, var_map: &VariableMap) -> Option<FigmaColor> {
        if let Some(value) = self.values_by_mode.get(mode_id) {
            if let VariableValue::Color(c) = value {
                return Some(*c);
            } else if let VariableValue::Alias(a) = value {
                let maybe_var = var_map.get_variable(&a.id);
                if let Some(alias_var) = maybe_var {
                    return alias_var.get_color(mode_id, var_map);
                } else {
                    // TODO can't find alias error
                }
            } else {
                // TODO invalid variable value error
            }
        } else {
            // TODO invalid variable for mode error
        }
        None
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub enum VariableType {
    Bool,
    Number,
    Text,
    Color,
}

// Representation of a Figma variable. We convert a figma_schema::Variable into
// this format to make the fields a bit easier to access.
#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct Variable {
    pub id: String,
    pub name: String,
    pub remote: bool,
    pub key: String,
    pub variable_collection_id: String,
    pub var_type: VariableType,
    pub values_by_mode: VariableValueMap,
}
impl Variable {
    pub fn from_figma_var(v: &figma_schema::Variable) -> Variable {
        match v {
            figma_schema::Variable::Boolean { common, values_by_mode } => Variable {
                id: common.id.clone(),
                name: common.name.clone(),
                remote: common.remote,
                key: common.key.clone(),
                variable_collection_id: common.variable_collection_id.clone(),
                var_type: VariableType::Bool,
                values_by_mode: VariableValueMap::from_figma_map(values_by_mode),
                //values_by_mode2,
            },
            figma_schema::Variable::Float { common, values_by_mode } => Variable {
                id: common.id.clone(),
                name: common.name.clone(),
                remote: common.remote,
                key: common.key.clone(),
                variable_collection_id: common.variable_collection_id.clone(),
                var_type: VariableType::Number,
                values_by_mode: VariableValueMap::from_figma_map(values_by_mode),
            },
            figma_schema::Variable::String { common, values_by_mode } => Variable {
                id: common.id.clone(),
                name: common.name.clone(),
                remote: common.remote,
                key: common.key.clone(),
                variable_collection_id: common.variable_collection_id.clone(),
                var_type: VariableType::Text,
                values_by_mode: VariableValueMap::from_figma_map(values_by_mode),
            },
            figma_schema::Variable::Color { common, values_by_mode } => Variable {
                id: common.id.clone(),
                name: common.name.clone(),
                remote: common.remote,
                key: common.key.clone(),
                variable_collection_id: common.variable_collection_id.clone(),
                var_type: VariableType::Color,
                values_by_mode: VariableValueMap::from_figma_map(values_by_mode),
            },
        }
    }

    pub fn get_value_string(&self, mode_id: &String, var_map: &VariableMap) -> String {
        let value = self.values_by_mode.values_by_mode.get(mode_id);
        if let Some(value) = value {
            match value {
                VariableValue::Bool(b) => b.to_string(),
                VariableValue::Number(f) => f.to_string(),
                VariableValue::Text(s) => s.clone(),
                VariableValue::Color(c) => format!("{} {} {} {}", c.r, c.g, c.b, c.a),
                VariableValue::Alias(a) => {
                    let alias_var = var_map.get_variable(&a.id);
                    if let Some(var) = alias_var {
                        format!("Alias -> {}", var.get_value_string(mode_id, var_map))
                    } else {
                        format!("Invalid alias id {}", a.id)
                    }
                }
            }
        } else {
            format!("No value for mode {}", mode_id)
        }
    }

    pub fn is_bool(&self) -> bool {
        self.var_type == VariableType::Bool
    }

    pub fn is_number(&self) -> bool {
        self.var_type == VariableType::Number
    }

    pub fn is_string(&self) -> bool {
        self.var_type == VariableType::Text
    }

    pub fn is_color(&self) -> bool {
        self.var_type == VariableType::Color
    }

    pub fn get_bool(&self, mode_id: &String, var_map: &VariableMap) -> Option<bool> {
        self.values_by_mode.get_bool(mode_id, var_map)
    }

    pub fn get_number(&self, mode_id: &String, var_map: &VariableMap) -> Option<f32> {
        self.values_by_mode.get_number(mode_id, var_map)
    }

    pub fn get_string(&self, mode_id: &String, var_map: &VariableMap) -> Option<String> {
        self.values_by_mode.get_string(mode_id, var_map)
    }

    pub fn get_color(&self, mode_id: &String, var_map: &VariableMap) -> Option<FigmaColor> {
        self.values_by_mode.get_color(mode_id, var_map)
    }
}

/// Stores variable mappings
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct VariableMap {
    pub collections: HashMap<String, Collection>,
    pub modes: HashMap<String, Mode>,
    pub variables: HashMap<String, Variable>,
}
impl VariableMap {
    fn get_variable(&self, var_id: &String) -> Option<&Variable> {
        self.variables.get(var_id)
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct BoundVariables {
    pub variables: HashMap<String, VariableAlias>,
}
impl BoundVariables {

}

/// The final result of a layout algorithm for a single taffy Node
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub struct Layout {
    // Relative ordering of the node. Render nodes with a higher order on top
    // of nodes with lower order
    pub order: u32,
    pub width: f32,
    pub height: f32,
    /// The top-left corner of the node
    pub left: f32,
    pub top: f32,
}
impl Layout {
    pub fn from_taffy_layout(l: &taffy::prelude::Layout) -> Layout {
        Layout {
            order: l.order,
            width: l.size.width,
            height: l.size.height,
            left: l.location.x,
            top: l.location.y,
        }
    }
}
