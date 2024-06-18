/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use serde::{Deserialize, Serialize};
use figma_import::color::Color;
use figma_import::figma_schema;
use figma_import::figma_schema::{FigmaColor, Rectangle, StrokeCap, VariableAlias, VariableCommon};
use figma_import::reaction_schema::{FrameExtras, Reaction};
use figma_import::toolkit_schema::{Collection, ScrollInfo, VariableType, VariableValueMap};
use figma_import::toolkit_style::StyledTextRun;
use std::collections::HashMap;
use std::sync::atomic::AtomicU16;
use crate::toolkit_style::ViewStyle;

// Enum for fields that represent either a fixed number or a number variable
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum NumOrVar {
    Num(f32),
    Var(String),
}

impl NumOrVar {
    pub fn from_var(
        bound_variables: &figma_schema::BoundVariables,
        var_name: &str,
        num: f32,
    ) -> NumOrVar {
        let var = bound_variables.get_variable(var_name);
        if let Some(var) = var {
            NumOrVar::Var(var)
        } else {
            NumOrVar::Num(num)
        }
    }
}

#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum ColorOrVar {
    Color(Color),
    Var(String),
}

impl ColorOrVar {
    pub fn from_var(
        bound_variables: &figma_schema::BoundVariables,
        var_name: &str,
        color: &FigmaColor,
    ) -> ColorOrVar {
        let var = bound_variables.get_variable(var_name);
        if let Some(var) = var {
            ColorOrVar::Var(var)
        } else {
            ColorOrVar::Color(color.into())
        }
    }
}

/// Shape of a view, either a rect or a path of some kind.
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum ViewShape {
    Rect {
        is_mask: bool,
    },
    RoundRect {
        corner_radius: [NumOrVar; 4],
        corner_smoothing: f32,
        is_mask: bool,
    },
    Path {
        path: Vec<figma_import::vector_schema::Path>,
        stroke: Vec<figma_import::vector_schema::Path>,
        is_mask: bool,
    },
    Arc {
        path: Vec<figma_import::vector_schema::Path>,
        stroke: Vec<figma_import::vector_schema::Path>,
        stroke_cap: StrokeCap,
        start_angle_degrees: f32,
        sweep_angle_degrees: f32,
        inner_radius: f32,
        corner_radius: f32,
        is_mask: bool,
    },
    VectorRect {
        path: Vec<figma_import::vector_schema::Path>,
        stroke: Vec<figma_import::vector_schema::Path>,
        corner_radius: [NumOrVar; 4],
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
    // unique numeric id (not replicated within one DesignCompose file), which is used by the
    // renderer when building a layout tree, or when mapping from an integer to a View.
    pub unique_id: u16,
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
    // a hash of variable collection id -> mode id if variable modes are set on this view
    pub explicit_variable_modes: Option<HashMap<String, String>>,
}

impl View {
    fn next_unique_id() -> u16 {
        static COUNTER: AtomicU16 = AtomicU16::new(0);
        COUNTER.fetch_add(1, std::sync::atomic::Ordering::Relaxed)
    }
    pub fn new_rect(
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
        explicit_variable_modes: Option<HashMap<String, String>>,
    ) -> View {
        View {
            unique_id: View::next_unique_id(),
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
            explicit_variable_modes,
        }
    }
    pub fn new_text(
        id: &String,
        name: &String,
        style: ViewStyle,
        component_info: Option<ComponentInfo>,
        reactions: Option<Vec<Reaction>>,
        text: &str,
        design_absolute_bounding_box: Option<Rectangle>,
        render_method: RenderMethod,
        explicit_variable_modes: Option<HashMap<String, String>>,
    ) -> View {
        View {
            unique_id: View::next_unique_id(),
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
            explicit_variable_modes,
        }
    }
    pub fn new_styled_text(
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
            unique_id: View::next_unique_id(),
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
            explicit_variable_modes: None,
        }
    }
    pub fn add_child(&mut self, child: View) {
        if let ViewData::Container { children, .. } = &mut self.data {
            children.push(child);
        }
    }
}

// We redeclare VariableValue instead of using the one from figma_schema because
// the "untagged" attribute there prevents serde_reflection from being able to
// run properly.
#[derive(Deserialize, Serialize, Debug, Clone)]
pub enum VariableValue {
    Bool(bool),
    Number(f32),
    Text(String),
    Color(Color),
    Alias(VariableAlias),
}

impl VariableValue {
    fn from_figma_value(v: &figma_schema::VariableValue) -> VariableValue {
        match v {
            figma_schema::VariableValue::Boolean(b) => VariableValue::Bool(b.clone()),
            figma_schema::VariableValue::Float(f) => VariableValue::Number(f.clone()),
            figma_schema::VariableValue::String(s) => VariableValue::Text(s.clone()),
            figma_schema::VariableValue::Color(c) => VariableValue::Color(c.into()),
            figma_schema::VariableValue::Alias(a) => VariableValue::Alias(a.clone()),
        }
    }
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
    fn new(
        var_type: VariableType,
        common: &VariableCommon,
        values_by_mode: &HashMap<String, figma_schema::VariableValue>,
    ) -> Self {
        Variable {
            id: common.id.clone(),
            name: common.name.clone(),
            remote: common.remote,
            key: common.key.clone(),
            variable_collection_id: common.variable_collection_id.clone(),
            var_type,
            values_by_mode: VariableValueMap::from_figma_map(values_by_mode),
        }
    }
    pub fn from_figma_var(v: &figma_schema::Variable) -> Variable {
        match v {
            figma_schema::Variable::Boolean { common, values_by_mode } => {
                Variable::new(VariableType::Bool, common, values_by_mode)
            }
            figma_schema::Variable::Float { common, values_by_mode } => {
                Variable::new(VariableType::Number, common, values_by_mode)
            }
            figma_schema::Variable::String { common, values_by_mode } => {
                Variable::new(VariableType::Bool, common, values_by_mode)
            }
            figma_schema::Variable::Color { common, values_by_mode } => {
                Variable::new(VariableType::Text, common, values_by_mode)
            }
        }
    }
}

/// Stores variable mappings
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct VariableMap {
    pub collections: HashMap<String, Collection>, // ID -> Collection
    pub collection_name_map: HashMap<String, String>, // Name -> ID
    pub variables: HashMap<String, Variable>,     // ID -> Variable
    pub variable_name_map: HashMap<String, HashMap<String, String>>, // Collection ID -> [Name -> ID]
}
