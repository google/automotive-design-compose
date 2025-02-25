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
use crate::definition::element::{
    background, Background, FontStretch, FontStyle, FontWeight, LineHeight, NumOrVar, Rectangle,
    Size, Stroke, TextDecoration, ViewShape,
};
use crate::definition::interaction::{PointerEvents, Reaction};
use crate::definition::layout::{
    FlexWrap, LayoutSizing, LayoutStyle, Overflow, OverflowDirection, ScrollInfo,
};
use crate::definition::modifier::{BlendMode, TextAlign, TextAlignVertical, TextOverflow};
use crate::definition::plugin::FrameExtras;
use crate::definition::view::view::RenderMethod;
use crate::definition::view::view_data::{Container, ViewDataType};
use std::collections::HashMap;
use std::sync::atomic::AtomicU16;

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
            hyperlink: None,
            shader_data: None,
        }
    }
}

impl ViewStyle {
    pub fn new_default() -> Self {
        Self {
            layout_style: Some(LayoutStyle::new_default()),
            node_style: Some(NodeStyle::new_default()),
        }
    }
    pub fn node_style(&self) -> &NodeStyle {
        self.node_style.as_ref().expect("NodeStyle is required.")
    }
    pub fn node_style_mut(&mut self) -> &mut NodeStyle {
        self.node_style.as_mut().expect("NodeStyle is required.")
    }
    pub fn layout_style(&self) -> &LayoutStyle {
        self.layout_style.as_ref().expect("LayoutStyle is required.")
    }
    pub fn layout_style_mut(&mut self) -> &mut LayoutStyle {
        self.layout_style.as_mut().expect("LayoutStyle is required.")
    }
}

impl ViewStyle {
    /// Compute the difference between this style and the given style, returning a style
    /// that can be applied to this style to make it equal the given style using apply_non_default.
    pub fn difference(&self, other: &ViewStyle) -> ViewStyle {
        let mut delta = ViewStyle::new_default();
        if self.node_style().text_color != other.node_style().text_color {
            delta.node_style_mut().text_color = other.node_style().text_color.clone();
        }
        if self.node_style().font_size != other.node_style().font_size {
            delta.node_style_mut().font_size = other.node_style().font_size.clone();
        }
        if self.node_style().font_family != other.node_style().font_family {
            delta.node_style_mut().font_family = other.node_style().font_family.clone();
        }
        if self.node_style().font_weight != other.node_style().font_weight {
            delta.node_style_mut().font_weight = other.node_style().font_weight.clone();
        }
        if self.node_style().font_style != other.node_style().font_style {
            delta.node_style_mut().font_style = other.node_style().font_style;
        }
        if self.node_style().text_decoration != other.node_style().text_decoration {
            delta.node_style_mut().text_decoration = other.node_style().text_decoration;
        }
        if self.node_style().letter_spacing != other.node_style().letter_spacing {
            delta.node_style_mut().letter_spacing = other.node_style().letter_spacing;
        }
        if self.node_style().font_stretch != other.node_style().font_stretch {
            delta.node_style_mut().font_stretch = other.node_style().font_stretch.clone();
        }
        if self.node_style().backgrounds != other.node_style().backgrounds {
            delta.node_style_mut().backgrounds = other.node_style().backgrounds.clone();
        }
        if self.node_style().box_shadows != other.node_style().box_shadows {
            delta.node_style_mut().box_shadows = other.node_style().box_shadows.clone();
        }
        if self.node_style().stroke != other.node_style().stroke {
            delta.node_style_mut().stroke = other.node_style().stroke.clone();
        }
        if self.node_style().opacity != other.node_style().opacity {
            delta.node_style_mut().opacity = other.node_style().opacity;
        }
        if self.node_style().transform != other.node_style().transform {
            delta.node_style_mut().transform = other.node_style().transform;
        }
        if self.node_style().relative_transform != other.node_style().relative_transform {
            delta.node_style_mut().relative_transform = other.node_style().relative_transform;
        }
        if self.node_style().text_align != other.node_style().text_align {
            delta.node_style_mut().text_align = other.node_style().text_align;
        }
        if self.node_style().text_align_vertical != other.node_style().text_align_vertical {
            delta.node_style_mut().text_align_vertical = other.node_style().text_align_vertical;
        }
        if self.node_style().text_overflow != other.node_style().text_overflow {
            delta.node_style_mut().text_overflow = other.node_style().text_overflow;
        }
        if self.node_style().text_shadow != other.node_style().text_shadow {
            delta.node_style_mut().text_shadow = other.node_style().text_shadow.clone();
        }
        if self.node_style().node_size != other.node_style().node_size {
            delta.node_style_mut().node_size = other.node_style().node_size.clone();
        }
        if self.node_style().line_height != other.node_style().line_height {
            delta.node_style_mut().line_height = other.node_style().line_height.clone();
        }
        if self.node_style().line_count != other.node_style().line_count {
            delta.node_style_mut().line_count = other.node_style().line_count;
        }
        if self.node_style().font_features != other.node_style().font_features {
            delta.node_style_mut().font_features = other.node_style().font_features.clone();
        }
        if self.node_style().filters != other.node_style().filters {
            delta.node_style_mut().filters = other.node_style().filters.clone();
        }
        if self.node_style().backdrop_filters != other.node_style().backdrop_filters {
            delta.node_style_mut().backdrop_filters = other.node_style().backdrop_filters.clone();
        }
        if self.node_style().blend_mode != other.node_style().blend_mode {
            delta.node_style_mut().blend_mode = other.node_style().blend_mode;
        }
        if self.node_style().hyperlink != other.node_style().hyperlink {
            delta.node_style_mut().hyperlink = other.node_style().hyperlink.clone();
        }
        if self.node_style().display_type != other.node_style().display_type {
            delta.node_style_mut().display_type = other.node_style().display_type;
        }
        if self.layout_style().position_type != other.layout_style().position_type {
            delta.layout_style_mut().position_type = other.layout_style().position_type;
        }
        if self.layout_style().flex_direction != other.layout_style().flex_direction {
            delta.layout_style_mut().flex_direction = other.layout_style().flex_direction;
        }
        if self.node_style().flex_wrap != other.node_style().flex_wrap {
            delta.node_style_mut().flex_wrap = other.node_style().flex_wrap;
        }
        if self.node_style().grid_layout_type != other.node_style().grid_layout_type {
            delta.node_style_mut().grid_layout_type = other.node_style().grid_layout_type;
        }
        if self.node_style().grid_columns_rows != other.node_style().grid_columns_rows {
            delta.node_style_mut().grid_columns_rows = other.node_style().grid_columns_rows;
        }
        if self.node_style().grid_adaptive_min_size != other.node_style().grid_adaptive_min_size {
            delta.node_style_mut().grid_adaptive_min_size =
                other.node_style().grid_adaptive_min_size;
        }
        if self.node_style().grid_span_contents != other.node_style().grid_span_contents {
            delta.node_style_mut().grid_span_contents =
                other.node_style().grid_span_contents.clone();
        }
        if self.node_style().overflow != other.node_style().overflow {
            delta.node_style_mut().overflow = other.node_style().overflow;
        }
        if self.node_style().max_children != other.node_style().max_children {
            delta.node_style_mut().max_children = other.node_style().max_children;
        }
        if self.node_style().overflow_node_id != other.node_style().overflow_node_id {
            delta.node_style_mut().overflow_node_id = other.node_style().overflow_node_id.clone();
        }
        if self.node_style().overflow_node_name != other.node_style().overflow_node_name {
            delta.node_style_mut().overflow_node_name =
                other.node_style().overflow_node_name.clone();
        }
        if self.node_style().shader_data != other.node_style().shader_data {
            delta.node_style_mut().shader_data = other.node_style().shader_data.clone();
        }
        if self.layout_style().align_items != other.layout_style().align_items {
            delta.layout_style_mut().align_items = other.layout_style().align_items;
        }
        if self.layout_style().align_content != other.layout_style().align_content {
            delta.layout_style_mut().align_content = other.layout_style().align_content;
        }
        if self.layout_style().justify_content != other.layout_style().justify_content {
            delta.layout_style_mut().justify_content = other.layout_style().justify_content;
        }
        if self.layout_style().top != other.layout_style().top {
            delta.layout_style_mut().top = other.layout_style().top;
        }
        if self.layout_style().left != other.layout_style().left {
            delta.layout_style_mut().left = other.layout_style().left;
        }
        if self.layout_style().bottom != other.layout_style().bottom {
            delta.layout_style_mut().bottom = other.layout_style().bottom;
        }
        if self.layout_style().right != other.layout_style().right {
            delta.layout_style_mut().right = other.layout_style().right;
        }
        if self.layout_style().margin != other.layout_style().margin {
            delta.layout_style_mut().margin = other.layout_style().margin.clone();
        }
        if self.layout_style().padding != other.layout_style().padding {
            delta.layout_style_mut().padding = other.layout_style().padding.clone();
        }
        if self.layout_style().item_spacing != other.layout_style().item_spacing {
            delta.layout_style_mut().item_spacing = other.layout_style().item_spacing.clone();
        }
        if self.node_style().cross_axis_item_spacing != other.node_style().cross_axis_item_spacing {
            delta.node_style_mut().cross_axis_item_spacing =
                other.node_style().cross_axis_item_spacing;
        }
        if self.layout_style().flex_grow != other.layout_style().flex_grow {
            delta.layout_style_mut().flex_grow = other.layout_style().flex_grow;
        }
        if self.layout_style().flex_shrink != other.layout_style().flex_shrink {
            delta.layout_style_mut().flex_shrink = other.layout_style().flex_shrink;
        }
        if self.layout_style().flex_basis != other.layout_style().flex_basis {
            delta.layout_style_mut().flex_basis = other.layout_style().flex_basis;
        }
        if self.layout_style().width != other.layout_style().width {
            delta.layout_style_mut().width = other.layout_style().width;
        }
        if self.layout_style().height != other.layout_style().height {
            delta.layout_style_mut().height = other.layout_style().height;
        }
        if self.layout_style().max_width != other.layout_style().max_width {
            delta.layout_style_mut().max_width = other.layout_style().max_width;
        }
        if self.layout_style().max_height != other.layout_style().max_height {
            delta.layout_style_mut().max_height = other.layout_style().max_height;
        }
        if self.layout_style().min_width != other.layout_style().min_width {
            delta.layout_style_mut().min_width = other.layout_style().min_width;
        }
        if self.layout_style().min_height != other.layout_style().min_height {
            delta.layout_style_mut().min_height = other.layout_style().min_height;
        }
        if self.node_style().aspect_ratio != other.node_style().aspect_ratio {
            delta.node_style_mut().aspect_ratio = other.node_style().aspect_ratio;
        }
        if self.node_style().pointer_events != other.node_style().pointer_events {
            delta.node_style_mut().pointer_events = other.node_style().pointer_events;
        }
        if self.node_style().meter_data != other.node_style().meter_data {
            delta.node_style_mut().meter_data = other.node_style().meter_data.clone();
        }
        delta
    }
}

impl ScrollInfo {
    pub fn new_default() -> Self {
        ScrollInfo { overflow: i32::from(OverflowDirection::None), paged_scrolling: false }
    }
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
        explicit_variable_modes: HashMap<String, String>,
    ) -> View {
        View {
            unique_id: View::next_unique_id() as u32,
            id: id.clone(),
            name: name.clone(),
            component_info,
            reactions: reactions.unwrap_or_default(),
            style: Some(style),
            frame_extras,
            scroll_info: Some(scroll_info),
            data: Some(ViewData {
                view_data_type: Some(ViewDataType::Container {
                    0: Container { shape: Some(shape), children: vec![] },
                }),
            }),
            design_absolute_bounding_box,
            render_method: i32::from(render_method),
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
        text_res_name: Option<String>,
        design_absolute_bounding_box: Option<Rectangle>,
        render_method: RenderMethod,
        explicit_variable_modes: HashMap<String, String>,
    ) -> View {
        View {
            unique_id: View::next_unique_id() as u32,
            id: id.clone(),
            name: name.clone(),
            component_info,
            reactions: reactions.unwrap_or_default(),
            style: Some(style),
            frame_extras: None,
            scroll_info: Some(ScrollInfo::new_default()),
            data: Some(ViewData {
                view_data_type: Some(ViewDataType::Text {
                    0: view_data::Text { content: text.into(), res_name: text_res_name },
                }),
            }),
            design_absolute_bounding_box,
            render_method: i32::from(render_method),
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
        text_res_name: Option<String>,
        design_absolute_bounding_box: Option<Rectangle>,
        render_method: RenderMethod,
    ) -> View {
        View {
            unique_id: View::next_unique_id() as u32,
            id: id.clone(),
            name: name.clone(),
            style: Some(style),
            component_info,
            reactions: reactions.unwrap_or_default(),
            frame_extras: None,
            scroll_info: Some(ScrollInfo::new_default()),
            data: Some(ViewData {
                view_data_type: Some(ViewDataType::StyledText {
                    0: view_data::StyledTextRuns { styled_texts: text, res_name: text_res_name },
                }),
            }),
            design_absolute_bounding_box,
            render_method: i32::from(render_method),
            explicit_variable_modes: HashMap::new(),
        }
    }
    pub fn add_child(&mut self, child: View) {
        if let Some(data) = self.data.as_mut() {
            if let Some(ViewDataType::Container { 0: Container { children, .. } }) =
                data.view_data_type.as_mut()
            {
                children.push(child);
            }
        }
    }

    pub fn style(&self) -> &ViewStyle {
        self.style.as_ref().expect("ViewStyle is required.")
    }
    pub fn style_mut(&mut self) -> &mut ViewStyle {
        self.style.as_mut().expect("ViewStyle is required.")
    }
}
