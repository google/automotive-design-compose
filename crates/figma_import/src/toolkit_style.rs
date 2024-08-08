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

//! `toolkit_style` contains all of the style-related types that `toolkit_schema::View`
//! uses.

use crate::figma_schema;
use dc_bundle::definition::element::{FontFeature, FontStyle};
use dc_bundle::definition::layout::FlexWrap;
use dc_bundle::legacy_definition::element::background::Background;
use dc_bundle::legacy_definition::element::color::Color;
use dc_bundle::legacy_definition::element::font::{
    FontStretch, FontWeight, Hyperlink, TextDecoration,
};
use dc_bundle::legacy_definition::element::geometry::Size;
use dc_bundle::legacy_definition::element::path::{LineHeight, Stroke};
use dc_bundle::legacy_definition::element::variable::{ColorOrVar, NumOrVar};
use dc_bundle::legacy_definition::interaction::pointer::PointerEvents;
use dc_bundle::legacy_definition::layout::grid::{GridLayoutType, GridSpan};
use dc_bundle::legacy_definition::layout::layout_style::LayoutStyle;
use dc_bundle::legacy_definition::layout::positioning::{LayoutSizing, Overflow};
use dc_bundle::legacy_definition::modifier::blend::BlendMode;
use dc_bundle::legacy_definition::modifier::filter::FilterOp;
use dc_bundle::legacy_definition::modifier::shadow::{BoxShadow, TextShadow};
use dc_bundle::legacy_definition::modifier::text::{TextAlign, TextAlignVertical, TextOverflow};
use dc_bundle::legacy_definition::modifier::transform::LayoutTransform;
use dc_bundle::legacy_definition::plugin::meter_data::{
    ArcMeterData, MeterData, ProgressBarMeterData, ProgressMarkerMeterData,
    ProgressVectorMeterData, RotationMeterData,
};
use serde::{Deserialize, Serialize};

use dc_bundle::legacy_definition::view::node_style::{Display, Number};

// These are the style properties that apply to text, so we can use them on subsections of
// a longer string. We then assume that every style transition is a potential line break (and
// also run the linebreaking algorithm on the content of every style for the normal case where
// we need to break text that's all in one style).
#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct TextStyle {
    pub text_color: Background, // also text shadow?
    pub font_size: NumOrVar,
    pub font_family: Option<String>,
    pub font_weight: FontWeight,
    pub font_style: FontStyle,
    pub font_stretch: FontStretch,
    pub letter_spacing: f32,
    pub text_decoration: TextDecoration,
    pub line_height: LineHeight,
    pub font_features: Vec<FontFeature>,
    pub hyperlink: Option<Hyperlink>,
}
impl Default for TextStyle {
    fn default() -> Self {
        TextStyle {
            text_color: Background::Solid(ColorOrVar::Color(Color::BLACK)),
            font_size: NumOrVar::Num(18.0),
            font_family: None,
            font_weight: FontWeight::NORMAL,
            font_style: FontStyle::Normal,
            font_stretch: FontStretch::NORMAL,
            letter_spacing: 0.0,
            text_decoration: TextDecoration::None,
            line_height: LineHeight::Percent(1.0),
            font_features: Vec::new(),
            hyperlink: None,
        }
    }
}

// Text can be either a string, or a list of styled runs.
#[derive(Clone, PartialEq, Debug, Deserialize, Serialize)]
pub struct StyledTextRun {
    pub text: String,
    pub style: TextStyle,
}
impl StyledTextRun {
    pub fn new(label: impl ToString) -> StyledTextRun {
        StyledTextRun { text: label.to_string(), style: Default::default() }
    }
    pub fn bold(self) -> Self {
        StyledTextRun {
            style: TextStyle { font_weight: FontWeight::BOLD, ..self.style },
            text: self.text,
        }
    }
    pub fn italic(self) -> Self {
        StyledTextRun {
            style: TextStyle { font_style: FontStyle::Italic, ..self.style },
            text: self.text,
        }
    }
    pub fn underline(self) -> Self {
        StyledTextRun {
            style: TextStyle { text_decoration: TextDecoration::Underline, ..self.style },
            text: self.text,
        }
    }
    pub fn strikethrough(self) -> Self {
        StyledTextRun {
            style: TextStyle { text_decoration: TextDecoration::Strikethrough, ..self.style },
            text: self.text,
        }
    }
    pub fn size(self, size: f32) -> Self {
        StyledTextRun {
            style: TextStyle { font_size: NumOrVar::Num(size), ..self.style },
            text: self.text,
        }
    }
    pub fn fill(self, text_color: Background) -> Self {
        StyledTextRun { style: TextStyle { text_color, ..self.style }, text: self.text }
    }
    pub fn family(self, family_name: impl ToString) -> Self {
        StyledTextRun {
            style: TextStyle { font_family: Some(family_name.to_string()), ..self.style },
            text: self.text,
        }
    }
    pub fn feature(self, feature: FontFeature) -> Self {
        let mut font_features = self.style.font_features;
        font_features.push(feature);
        StyledTextRun { style: TextStyle { font_features, ..self.style }, text: self.text }
    }
    pub fn hyperlink(self, hyperlink: Hyperlink) -> Self {
        StyledTextRun {
            style: TextStyle { hyperlink: Some(hyperlink), ..self.style },
            text: self.text,
        }
    }
}

// Schema for progress vector data that we read from Figma plugin data
#[derive(Serialize, Deserialize, Clone, Debug, Default, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct ProgressVectorMeterDataSchema {
    pub enabled: bool,
    pub discrete: bool,
    pub discrete_value: f32,
    pub paths: Vec<figma_schema::Path>,
}

// Schema for dials & gauges Figma plugin data
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub enum MeterDataSchema {
    ArcData(ArcMeterData),
    RotationData(RotationMeterData),
    ProgressBarData(ProgressBarMeterData),
    ProgressMarkerData(ProgressMarkerMeterData),
    ProgressVectorData(ProgressVectorMeterDataSchema),
}

impl Into<MeterData> for MeterDataSchema {
    fn into(self) -> MeterData {
        match self {
            MeterDataSchema::ArcData(data) => MeterData::ArcData(data),
            MeterDataSchema::RotationData(data) => MeterData::RotationData(data),
            MeterDataSchema::ProgressBarData(data) => MeterData::ProgressBarData(data),
            MeterDataSchema::ProgressMarkerData(data) => MeterData::ProgressMarkerData(data),
            MeterDataSchema::ProgressVectorData(data) => {
                MeterData::ProgressVectorData(ProgressVectorMeterData {
                    enabled: data.enabled,
                    discrete: data.discrete,
                    discrete_value: data.discrete_value,
                })
            }
        }
    }
}

/// ToolkitStyle contains all of the styleable parameters accepted by the Rect and Text components.
///
#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct NodeStyle {
    pub text_color: Background,
    pub font_size: NumOrVar,
    pub font_family: Option<String>,
    pub font_weight: FontWeight,
    pub font_style: FontStyle,
    pub text_decoration: TextDecoration,
    pub letter_spacing: Option<f32>,
    pub font_stretch: FontStretch,
    pub background: Vec<Background>,
    pub box_shadow: Vec<BoxShadow>,
    pub stroke: Stroke,
    pub opacity: Option<f32>,
    pub transform: Option<LayoutTransform>,
    pub relative_transform: Option<LayoutTransform>,
    pub text_align: TextAlign,
    pub text_align_vertical: TextAlignVertical,
    pub text_overflow: TextOverflow,
    pub text_shadow: Option<TextShadow>,
    pub node_size: Size<f32>,
    pub line_height: LineHeight,
    pub line_count: Option<usize>, // None means no limit on # lines.
    pub font_features: Vec<FontFeature>,
    pub filter: Vec<FilterOp>,
    pub backdrop_filter: Vec<FilterOp>,
    pub blend_mode: BlendMode,
    pub hyperlink: Option<Hyperlink>,

    pub display_type: Display,
    pub flex_wrap: FlexWrap,
    pub grid_layout: Option<GridLayoutType>,
    pub grid_columns_rows: u32,
    pub grid_adaptive_min_size: u32,
    pub grid_span_content: Vec<GridSpan>,
    pub overflow: Overflow,
    pub max_children: Option<u32>,
    pub overflow_node_id: Option<String>,
    pub overflow_node_name: Option<String>,
    pub cross_axis_item_spacing: f32,
    pub horizontal_sizing: LayoutSizing,
    pub vertical_sizing: LayoutSizing,
    pub aspect_ratio: Number,
    pub pointer_events: PointerEvents,
    pub meter_data: Option<MeterData>,
}
impl Default for NodeStyle {
    fn default() -> NodeStyle {
        NodeStyle {
            text_color: Background::None,
            font_size: NumOrVar::Num(18.0),
            font_family: None,
            font_weight: FontWeight::NORMAL,
            font_style: FontStyle::Normal,
            text_decoration: TextDecoration::None,
            letter_spacing: None,
            font_stretch: FontStretch::NORMAL,
            background: Vec::new(),
            box_shadow: Vec::new(),
            stroke: Stroke::default(),
            opacity: None,
            transform: None,
            relative_transform: None,
            text_align: TextAlign::Left,
            text_align_vertical: TextAlignVertical::Top,
            text_overflow: TextOverflow::Clip,
            text_shadow: None,
            node_size: Size::default(),
            line_height: LineHeight::Percent(1.0),
            line_count: None,
            font_features: Vec::new(),
            filter: Vec::new(),
            backdrop_filter: Vec::new(),
            blend_mode: BlendMode::default(),
            hyperlink: None,
            display_type: Display::default(),
            flex_wrap: FlexWrap::NoWrap,
            grid_layout: None,
            grid_columns_rows: 0,
            grid_adaptive_min_size: 1,
            grid_span_content: vec![],
            overflow: Overflow::default(),
            max_children: None,
            overflow_node_id: None,
            overflow_node_name: None,
            cross_axis_item_spacing: 0.0,
            horizontal_sizing: LayoutSizing::default(),
            vertical_sizing: LayoutSizing::default(),
            aspect_ratio: Number::default(),
            pointer_events: PointerEvents::default(),
            meter_data: None,
        }
    }
}

#[derive(Clone, Debug, PartialEq, Deserialize, Serialize, Default)]
pub struct ViewStyle {
    pub layout_style: LayoutStyle,
    pub node_style: NodeStyle,
}
impl ViewStyle {
    /// Compute the difference between this style and the given style, returning a style
    /// that can be applied to this style to make it equal the given style using apply_non_default.
    pub fn difference(&self, other: &ViewStyle) -> ViewStyle {
        let mut delta = ViewStyle::default();
        if self.node_style.text_color != other.node_style.text_color {
            delta.node_style.text_color = other.node_style.text_color.clone();
        }
        if self.node_style.font_size != other.node_style.font_size {
            delta.node_style.font_size = other.node_style.font_size.clone();
        }
        if self.node_style.font_family != other.node_style.font_family {
            delta.node_style.font_family = other.node_style.font_family.clone();
        }
        if self.node_style.font_weight != other.node_style.font_weight {
            delta.node_style.font_weight = other.node_style.font_weight.clone();
        }
        if self.node_style.font_style != other.node_style.font_style {
            delta.node_style.font_style = other.node_style.font_style;
        }
        if self.node_style.text_decoration != other.node_style.text_decoration {
            delta.node_style.text_decoration = other.node_style.text_decoration;
        }
        if self.node_style.letter_spacing != other.node_style.letter_spacing {
            delta.node_style.letter_spacing = other.node_style.letter_spacing;
        }
        if self.node_style.font_stretch != other.node_style.font_stretch {
            delta.node_style.font_stretch = other.node_style.font_stretch;
        }
        if self.node_style.background != other.node_style.background {
            delta.node_style.background = other.node_style.background.clone();
        }
        if self.node_style.box_shadow != other.node_style.box_shadow {
            delta.node_style.box_shadow = other.node_style.box_shadow.clone();
        }
        if self.node_style.stroke != other.node_style.stroke {
            delta.node_style.stroke = other.node_style.stroke.clone();
        }
        if self.node_style.opacity != other.node_style.opacity {
            delta.node_style.opacity = other.node_style.opacity;
        }
        if self.node_style.transform != other.node_style.transform {
            delta.node_style.transform = other.node_style.transform;
        }
        if self.node_style.relative_transform != other.node_style.relative_transform {
            delta.node_style.relative_transform = other.node_style.relative_transform;
        }
        if self.node_style.text_align != other.node_style.text_align {
            delta.node_style.text_align = other.node_style.text_align;
        }
        if self.node_style.text_align_vertical != other.node_style.text_align_vertical {
            delta.node_style.text_align_vertical = other.node_style.text_align_vertical;
        }
        if self.node_style.text_overflow != other.node_style.text_overflow {
            delta.node_style.text_overflow = other.node_style.text_overflow;
        }
        if self.node_style.text_shadow != other.node_style.text_shadow {
            delta.node_style.text_shadow = other.node_style.text_shadow;
        }
        if self.node_style.node_size != other.node_style.node_size {
            delta.node_style.node_size = other.node_style.node_size;
        }
        if self.node_style.line_height != other.node_style.line_height {
            delta.node_style.line_height = other.node_style.line_height;
        }
        if self.node_style.line_count != other.node_style.line_count {
            delta.node_style.line_count = other.node_style.line_count;
        }
        if self.node_style.font_features != other.node_style.font_features {
            delta.node_style.font_features = other.node_style.font_features.clone();
        }
        if self.node_style.filter != other.node_style.filter {
            delta.node_style.filter = other.node_style.filter.clone();
        }
        if self.node_style.backdrop_filter != other.node_style.backdrop_filter {
            delta.node_style.backdrop_filter = other.node_style.backdrop_filter.clone();
        }
        if self.node_style.blend_mode != other.node_style.blend_mode {
            delta.node_style.blend_mode = other.node_style.blend_mode;
        }
        if self.node_style.hyperlink != other.node_style.hyperlink {
            delta.node_style.hyperlink = other.node_style.hyperlink.clone();
        }
        if self.node_style.display_type != other.node_style.display_type {
            delta.node_style.display_type = other.node_style.display_type;
        }
        if self.layout_style.position_type != other.layout_style.position_type {
            delta.layout_style.position_type = other.layout_style.position_type;
        }
        if self.layout_style.flex_direction != other.layout_style.flex_direction {
            delta.layout_style.flex_direction = other.layout_style.flex_direction;
        }
        if self.node_style.flex_wrap != other.node_style.flex_wrap {
            delta.node_style.flex_wrap = other.node_style.flex_wrap;
        }
        if self.node_style.grid_layout != other.node_style.grid_layout {
            delta.node_style.grid_layout = other.node_style.grid_layout;
        }
        if self.node_style.grid_columns_rows != other.node_style.grid_columns_rows {
            delta.node_style.grid_columns_rows = other.node_style.grid_columns_rows;
        }
        if self.node_style.grid_adaptive_min_size != other.node_style.grid_adaptive_min_size {
            delta.node_style.grid_adaptive_min_size = other.node_style.grid_adaptive_min_size;
        }
        if self.node_style.grid_span_content != other.node_style.grid_span_content {
            delta.node_style.grid_span_content = other.node_style.grid_span_content.clone();
        }
        if self.node_style.overflow != other.node_style.overflow {
            delta.node_style.overflow = other.node_style.overflow;
        }
        if self.node_style.max_children != other.node_style.max_children {
            delta.node_style.max_children = other.node_style.max_children;
        }
        if self.node_style.overflow_node_id != other.node_style.overflow_node_id {
            delta.node_style.overflow_node_id = other.node_style.overflow_node_id.clone();
        }
        if self.node_style.overflow_node_name != other.node_style.overflow_node_name {
            delta.node_style.overflow_node_name = other.node_style.overflow_node_name.clone();
        }
        if self.layout_style.align_items != other.layout_style.align_items {
            delta.layout_style.align_items = other.layout_style.align_items;
        }
        if self.layout_style.align_content != other.layout_style.align_content {
            delta.layout_style.align_content = other.layout_style.align_content;
        }
        if self.layout_style.justify_content != other.layout_style.justify_content {
            delta.layout_style.justify_content = other.layout_style.justify_content;
        }
        if self.layout_style.top != other.layout_style.top {
            delta.layout_style.top = other.layout_style.top;
        }
        if self.layout_style.left != other.layout_style.left {
            delta.layout_style.left = other.layout_style.left;
        }
        if self.layout_style.bottom != other.layout_style.bottom {
            delta.layout_style.bottom = other.layout_style.bottom;
        }
        if self.layout_style.right != other.layout_style.right {
            delta.layout_style.right = other.layout_style.right;
        }
        if self.layout_style.margin != other.layout_style.margin {
            delta.layout_style.margin = other.layout_style.margin;
        }
        if self.layout_style.padding != other.layout_style.padding {
            delta.layout_style.padding = other.layout_style.padding;
        }
        if self.layout_style.item_spacing != other.layout_style.item_spacing {
            delta.layout_style.item_spacing = other.layout_style.item_spacing.clone();
        }
        if self.node_style.cross_axis_item_spacing != other.node_style.cross_axis_item_spacing {
            delta.node_style.cross_axis_item_spacing = other.node_style.cross_axis_item_spacing;
        }
        if self.layout_style.flex_grow != other.layout_style.flex_grow {
            delta.layout_style.flex_grow = other.layout_style.flex_grow;
        }
        if self.layout_style.flex_shrink != other.layout_style.flex_shrink {
            delta.layout_style.flex_shrink = other.layout_style.flex_shrink;
        }
        if self.layout_style.flex_basis != other.layout_style.flex_basis {
            delta.layout_style.flex_basis = other.layout_style.flex_basis;
        }
        if self.layout_style.width != other.layout_style.width {
            delta.layout_style.width = other.layout_style.width;
        }
        if self.layout_style.height != other.layout_style.height {
            delta.layout_style.height = other.layout_style.height;
        }
        if self.layout_style.max_width != other.layout_style.max_width {
            delta.layout_style.max_width = other.layout_style.max_width;
        }
        if self.layout_style.max_height != other.layout_style.max_height {
            delta.layout_style.max_height = other.layout_style.max_height;
        }
        if self.layout_style.min_width != other.layout_style.min_width {
            delta.layout_style.min_width = other.layout_style.min_width;
        }
        if self.layout_style.min_height != other.layout_style.min_height {
            delta.layout_style.min_height = other.layout_style.min_height;
        }
        if self.node_style.aspect_ratio != other.node_style.aspect_ratio {
            delta.node_style.aspect_ratio = other.node_style.aspect_ratio;
        }
        if self.node_style.pointer_events != other.node_style.pointer_events {
            delta.node_style.pointer_events = other.node_style.pointer_events;
        }
        if self.node_style.meter_data != other.node_style.meter_data {
            delta.node_style.meter_data = other.node_style.meter_data.clone();
        }
        delta
    }
}
