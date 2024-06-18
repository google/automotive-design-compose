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

use layout::types::Size;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use dc_design_package::toolkit_layout_style::{Display, FlexWrap, LayoutSizing, Number, Overflow};
use dc_design_package::toolkit_schema::{ColorOrVar, NumOrVar};
use dc_design_package::toolkit_style::{Background, BlendMode, BoxShadow, FilterOp, GridLayoutType, LineHeight, MeterData, PointerEvents, StrokeAlign, StrokeWeight, TextAlign, TextAlignVertical, TextOverflow};

use crate::{
    color::Color,
    toolkit_font_style::{FontStretch, FontStyle, FontWeight},
};

/// This structure represents an OpenType feature, for example "tnum" controls tabular numbers
/// in fonts that support the feature.
#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct FontFeature {
    pub tag: [u8; 4],
    pub enabled: bool,
}
impl FontFeature {
    pub fn new(tag: &[u8; 4]) -> Self {
        FontFeature { tag: *tag, enabled: true }
    }
}

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
    pub line_height: LineHeight,
    pub font_features: Vec<FontFeature>,
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
            letter_spacing: 1.0,
            line_height: LineHeight::Percent(1.0),
            font_features: Vec::new(),
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
}

#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize)]
pub struct TextShadow {
    pub blur_radius: f32,
    pub color: Color,
    pub offset: (f32, f32),
}

/// A stroke is similar to a border, except that it does not change layout (a border insets
/// the children by the border size), it may be inset, centered or outset from the view bounds
/// and there can be multiple strokes on a view.
#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct Stroke {
    /// The alignment of strokes on this view.
    pub stroke_align: StrokeAlign,
    /// The thickness of strokes on this view (in pixels).
    pub stroke_weight: StrokeWeight,
    /// The stroke colors/fills
    pub strokes: Vec<Background>,
}
impl Default for Stroke {
    fn default() -> Self {
        Stroke {
            stroke_align: StrokeAlign::Center,
            stroke_weight: StrokeWeight::Uniform(0.0),
            strokes: Vec::new(),
        }
    }
}

#[derive(Clone, Debug, Deserialize)]
pub struct LayoutPixel;

pub type LayoutTransform = euclid::Transform3D<f32, LayoutPixel, LayoutPixel>;
pub type AffineTransform = euclid::Transform2D<f32, LayoutPixel, LayoutPixel>;

#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct GridSpan {
    pub node_name: String,
    pub node_variant: HashMap<String, String>,
    pub span: u32,
    pub max_span: bool,
}

#[derive(Serialize, Deserialize, Clone, Debug, Default, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct ProgressMarkerMeterData {
    pub enabled: bool,
    pub discrete: bool,
    pub discrete_value: f32,
    #[serde(default)]
    pub vertical: bool,
    #[serde(default)]
    pub start_x: f32,
    #[serde(default)]
    pub end_x: f32,
    #[serde(default)]
    pub start_y: f32,
    #[serde(default)]
    pub end_y: f32,
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
            text_color: Background::Solid(ColorOrVar::Color(Color::from_u8s(0, 0, 0, 255))),
            font_size: NumOrVar::Num(18.0),
            font_family: None,
            font_weight: FontWeight::NORMAL,
            font_style: FontStyle::Normal,
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
            display_type: Display::default(),
            flex_wrap: FlexWrap::default(),
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
