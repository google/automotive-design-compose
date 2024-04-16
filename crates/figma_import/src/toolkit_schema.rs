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

use std::sync::atomic::AtomicU16;

// We use serde to serialize and deserialize our "toolkit style" views. Because we need
// serialization, we use our own replacement for ViewStyle which can be serialized and
// retain image references.
use serde::{Deserialize, Serialize};

use crate::figma_schema::Rectangle;
use crate::reaction_schema::FrameExtras;
use crate::reaction_schema::Reaction;
use crate::toolkit_style::{StyledTextRun, ViewStyle};

pub use crate::figma_schema::OverflowDirection;
pub use crate::figma_schema::StrokeCap;

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
}
impl View {
    fn next_unique_id() -> u16 {
        static COUNTER: AtomicU16 = AtomicU16::new(0);
        COUNTER.fetch_add(1, std::sync::atomic::Ordering::Relaxed)
    }
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
        }
    }
    pub(crate) fn add_child(&mut self, child: View) {
        if let ViewData::Container { children, .. } = &mut self.data {
            children.push(child);
        }
    }
}
