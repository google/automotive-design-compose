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

use crate::definition::element::num_or_var::NumOrVar;
use crate::definition::element::Background;
use crate::definition::element::Stroke;
use crate::definition::element::{
    background, FontFeature, FontStyle, Hyperlink, Size, TextDecoration,
};
use crate::definition::layout::FlexWrap;
use crate::definition::modifier::FilterOp;
use crate::definition::modifier::LayoutTransform;
use crate::legacy_definition::element::font::{FontStretch, FontWeight};
use crate::legacy_definition::element::path::LineHeight;
use crate::legacy_definition::interaction::pointer::PointerEvents;
use crate::legacy_definition::layout::grid::{GridLayoutType, GridSpan};
use crate::legacy_definition::layout::positioning::{LayoutSizing, Overflow};
use crate::legacy_definition::modifier::blend::BlendMode;
use crate::legacy_definition::modifier::shadow::{BoxShadow, TextShadow};
use crate::legacy_definition::modifier::text::{TextAlign, TextAlignVertical, TextOverflow};
use crate::legacy_definition::plugin::meter_data::MeterData;
use serde::{Deserialize, Serialize};

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum Number {
    Defined(f32),
    Undefined,
}

impl Default for Number {
    fn default() -> Self {
        Self::Undefined
    }
}

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum Display {
    #[serde(rename = "flex")]
    Flex,
    #[serde(rename = "none")]
    None,
}

impl Default for Display {
    fn default() -> Self {
        Self::Flex
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
    pub node_size: Size,
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
            text_color: Background::new(background::BackgroundType::None(())),
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
