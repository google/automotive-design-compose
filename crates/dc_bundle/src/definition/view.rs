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
use crate::definition::element::line_height::LineHeightType;
use crate::definition::element::num_or_var::NumOrVarType;
use crate::definition::element::{
    background, Background, FontStretch, FontStyle, FontWeight, LineHeight, NumOrVar, Size, Stroke,
    TextDecoration,
};
use crate::definition::interaction::PointerEvents;
use crate::definition::layout::{FlexWrap, LayoutSizing, Overflow};
use crate::definition::modifier::{BlendMode, TextAlign, TextAlignVertical, TextOverflow};

include!(concat!(env!("OUT_DIR"), "/designcompose.definition.view.rs"));

impl NodeStyle {
    pub(crate) fn new_default() -> NodeStyle {
        NodeStyle {
            text_color: Some(Background::new(background::BackgroundType::None(()))),
            font_size: Some(NumOrVar::from_num(18.0)),
            font_family: None,
            font_weight: FontWeight::NORMAL.into(),
            font_style: FontStyle::Normal.into(),
            text_decoration: TextDecoration::None.into(),
            letter_spacing: None,
            font_stretch: Some(FontStretch::NORMAL),
            backgrounds: Vec::new(),
            box_shadows: Vec::new(),
            stroke: Some(Stroke::default()),
            opacity: None,
            transform: None,
            relative_transform: None,
            text_align: TextAlign::Left.into(),
            text_align_vertical: TextAlignVertical::Top.into(),
            text_overflow: TextOverflow::Clip.into(),
            text_shadow: None,
            node_size: Some(Size { width: 0.0, height: 0.0 }),
            line_height: Some(LineHeight { line_height_type: Some(LineHeightType::Percent(1.0)) }),
            line_count: None,
            font_features: Vec::new(),
            filters: Vec::new(),
            backdrop_filters: Vec::new(),
            blend_mode: BlendMode::PassThrough.into(),
            display_type: Display::Flex.into(),
            flex_wrap: FlexWrap::NoWrap.into(),
            grid_layout_type: None,
            grid_columns_rows: 0,
            grid_adaptive_min_size: 1,
            grid_span_contents: vec![],
            overflow: Overflow::Visible.into(),
            max_children: None,
            overflow_node_id: None,
            overflow_node_name: None,
            cross_axis_item_spacing: 0.0,
            horizontal_sizing: LayoutSizing::Fixed.into(),
            vertical_sizing: LayoutSizing::Fixed.into(),
            aspect_ratio: None,
            pointer_events: PointerEvents::Inherit.into(),
            meter_data: None,
            hyperlinks: None,
        }
    }
}
