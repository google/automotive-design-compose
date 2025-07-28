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
use std::collections::HashMap;
use std::sync::atomic::AtomicU16;

use crate::background::{background, Background};
use crate::blend::BlendMode;
use crate::font::{FontStretch, FontStyle, FontWeight, TextDecoration};
use crate::frame_extras::FrameExtras;
use crate::geometry::{Rectangle, Size};
use crate::layout_style::LayoutStyle;
use crate::node_style::{Display, NodeStyle};
use crate::path::line_height::Line_height_type;
use crate::path::{LineHeight, Stroke};
use crate::pointer::PointerEvents;
use crate::positioning::{FlexWrap, LayoutSizing, Overflow, OverflowDirection, ScrollInfo};
use crate::reaction::Reaction;
use crate::text::{TextAlign, TextAlignVertical, TextOverflow};
use crate::text_style::StyledTextRun;
use crate::variable::NumOrVar;
use crate::view::view::RenderMethod;
use crate::view::view_data::{Container, StyledTextRuns, Text, View_data_type};
use crate::view::{ComponentInfo, View, ViewData};
use crate::view_shape::ViewShape;
use crate::view_style::ViewStyle;

impl NodeStyle {
    pub(crate) fn new_default() -> NodeStyle {
        NodeStyle {
            font_color: Some(Background::new_with_background(background::Background_type::None(
                ().into(),
            )))
            .into(),
            font_size: Some(NumOrVar::from_num(18.0)).into(),
            font_family: None,
            font_weight: Some(FontWeight::normal()).into(),
            font_style: FontStyle::FONT_STYLE_NORMAL.into(),
            text_decoration: TextDecoration::TEXT_DECORATION_NONE.into(),
            letter_spacing: None,
            font_stretch: Some(FontStretch::normal()).into(),
            backgrounds: Vec::new(),
            box_shadows: Vec::new(),
            stroke: Some(Stroke::default()).into(),
            opacity: None,
            transform: None.into(),
            relative_transform: None.into(),
            text_align: TextAlign::TEXT_ALIGN_LEFT.into(),
            text_align_vertical: TextAlignVertical::TEXT_ALIGN_VERTICAL_TOP.into(),
            text_overflow: TextOverflow::TEXT_OVERFLOW_CLIP.into(),
            text_shadow: None.into(),
            node_size: Some(Size { width: 0.0, height: 0.0, ..Default::default() }).into(),
            line_height: Some(LineHeight {
                line_height_type: Some(Line_height_type::Percent(1.0)),
                ..Default::default()
            })
            .into(),
            line_count: None,
            font_features: Vec::new(),
            filters: Vec::new(),
            backdrop_filters: Vec::new(),
            blend_mode: BlendMode::BLEND_MODE_PASS_THROUGH.into(),
            display_type: Display::DISPLAY_FLEX.into(),
            flex_wrap: FlexWrap::FLEX_WRAP_NO_WRAP.into(),
            grid_layout_type: None,
            grid_columns_rows: 0,
            grid_adaptive_min_size: 1,
            grid_span_contents: vec![],
            overflow: Overflow::OVERFLOW_VISIBLE.into(),
            max_children: None,
            overflow_node_id: None,
            overflow_node_name: None,
            cross_axis_item_spacing: 0.0,
            horizontal_sizing: LayoutSizing::LAYOUT_SIZING_FIXED.into(),
            vertical_sizing: LayoutSizing::LAYOUT_SIZING_FIXED.into(),
            aspect_ratio: None,
            pointer_events: PointerEvents::POINTER_EVENTS_INHERIT.into(),
            meter_data: None.into(),
            hyperlink: None.into(),
            shader_data: None.into(),
            scalable_data: None.into(),
            ..Default::default()
        }
    }
}

impl ViewStyle {
    pub fn new_default() -> Self {
        Self {
            layout_style: Some(LayoutStyle::new_default()).into(),
            node_style: Some(NodeStyle::new_default()).into(),
            ..Default::default()
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
        if self.node_style().font_color != other.node_style().font_color {
            delta.node_style_mut().font_color = other.node_style().font_color.clone();
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
            delta.node_style_mut().transform = other.node_style().transform.clone();
        }
        if self.node_style().relative_transform != other.node_style().relative_transform {
            delta.node_style_mut().relative_transform =
                other.node_style().relative_transform.clone();
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
            delta.layout_style_mut().top = other.layout_style().top.clone();
        }
        if self.layout_style().left != other.layout_style().left {
            delta.layout_style_mut().left = other.layout_style().left.clone();
        }
        if self.layout_style().bottom != other.layout_style().bottom {
            delta.layout_style_mut().bottom = other.layout_style().bottom.clone();
        }
        if self.layout_style().right != other.layout_style().right {
            delta.layout_style_mut().right = other.layout_style().right.clone();
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
            delta.layout_style_mut().flex_basis = other.layout_style().flex_basis.clone();
        }
        if self.layout_style().width != other.layout_style().width {
            delta.layout_style_mut().width = other.layout_style().width.clone();
        }
        if self.layout_style().height != other.layout_style().height {
            delta.layout_style_mut().height = other.layout_style().height.clone();
        }
        if self.layout_style().max_width != other.layout_style().max_width {
            delta.layout_style_mut().max_width = other.layout_style().max_width.clone();
        }
        if self.layout_style().max_height != other.layout_style().max_height {
            delta.layout_style_mut().max_height = other.layout_style().max_height.clone();
        }
        if self.layout_style().min_width != other.layout_style().min_width {
            delta.layout_style_mut().min_width = other.layout_style().min_width.clone();
        }
        if self.layout_style().min_height != other.layout_style().min_height {
            delta.layout_style_mut().min_height = other.layout_style().min_height.clone();
        }
        if self.layout_style().bounding_box != other.layout_style().bounding_box {
            delta.layout_style_mut().bounding_box = other.layout_style().bounding_box.clone();
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

impl ViewData {
    /// Compute the difference between this view data and the given view data.
    /// Right now only computes the text overrides.
    pub fn difference(&self, other: &ViewData) -> Option<ViewData> {
        if let Some(View_data_type::Text { .. }) = self.view_data_type {
            if self != other {
                return Some(other.clone());
            }
        }
        if let Some(View_data_type::StyledText { .. }) = self.view_data_type {
            if self != other {
                return Some(other.clone());
            }
        }
        return None;
    }
}

impl ScrollInfo {
    pub fn new_default() -> Self {
        ScrollInfo {
            overflow: OverflowDirection::OVERFLOW_DIRECTION_NONE.into(),
            paged_scrolling: false,
            ..Default::default()
        }
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
            component_info: component_info.into(),
            reactions: reactions.unwrap_or_default(),
            style: Some(style).into(),
            frame_extras: frame_extras.into(),
            scroll_info: Some(scroll_info).into(),
            data: Some(ViewData {
                view_data_type: Some(View_data_type::Container {
                    0: Container {
                        shape: Some(shape).into(),
                        children: vec![],
                        ..Default::default()
                    },
                }),
                ..Default::default()
            })
            .into(),
            design_absolute_bounding_box: design_absolute_bounding_box.into(),
            render_method: render_method.into(),
            explicit_variable_modes,
            ..Default::default()
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
            component_info: component_info.into(),
            reactions: reactions.unwrap_or_default(),
            style: Some(style).into(),
            frame_extras: None.into(),
            scroll_info: Some(ScrollInfo::new_default()).into(),
            data: Some(ViewData {
                view_data_type: Some(View_data_type::Text {
                    0: Text { content: text.into(), res_name: text_res_name, ..Default::default() },
                }),
                ..Default::default()
            })
            .into(),
            design_absolute_bounding_box: design_absolute_bounding_box.into(),
            render_method: render_method.into(),
            explicit_variable_modes,
            ..Default::default()
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
            style: Some(style).into(),
            component_info: component_info.into(),
            reactions: reactions.unwrap_or_default(),
            frame_extras: None.into(),
            scroll_info: Some(ScrollInfo::new_default()).into(),
            data: Some(ViewData {
                view_data_type: Some(View_data_type::StyledText {
                    0: StyledTextRuns {
                        styled_texts: text,
                        res_name: text_res_name,
                        ..Default::default()
                    },
                }),
                ..Default::default()
            })
            .into(),
            design_absolute_bounding_box: design_absolute_bounding_box.into(),
            render_method: render_method.into(),
            explicit_variable_modes: HashMap::new(),
            ..Default::default()
        }
    }
    pub fn add_child(&mut self, child: View) {
        if let Some(data) = self.data.as_mut() {
            if let Some(View_data_type::Container { 0: Container { children, .. } }) =
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

    /** This function is now only called by a view that is a COMPONENT. */
    pub fn find_view_by_id(&self, view_id: &String) -> Option<&View> {
        if view_id.as_str() == self.id {
            return Some(&self);
        } else if let Some(id) = view_id.split(";").last() {
            // If this is a descendent node of an instance, the last section is the node id
            // of the view in the component. Example: I70:17;29:15
            if self.id == id.to_string() {
                return Some(&self);
            }
        }
        if let Some(data) = &self.data.as_ref() {
            if let Some(View_data_type::Container { 0: Container { children, .. } }) =
                &data.view_data_type
            {
                for child in children {
                    let result = child.find_view_by_id(&view_id);
                    if result.is_some() {
                        return result;
                    }
                }
            }
        }
        return None;
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::path::stroke_weight;
    use crate::path::StrokeWeight;
    use crate::variable::num_or_var::NumOrVarType;

    #[test]
    fn test_node_style_new_default() {
        let style = NodeStyle::new_default();
        assert!(style.font_color.is_some());
        assert_eq!(style.font_size.unwrap().NumOrVarType, Some(NumOrVarType::Num(18.0)));
        assert_eq!(style.font_weight.unwrap(), FontWeight::normal());
        assert_eq!(style.font_style.enum_value().unwrap(), FontStyle::FONT_STYLE_NORMAL);
    }

    #[test]
    fn test_view_style_new_default() {
        let style = ViewStyle::new_default();
        assert!(style.layout_style.is_some());
        assert!(style.node_style.is_some());
    }

    #[test]
    fn test_view_style_difference() {
        let mut style1 = ViewStyle::new_default();
        let mut style2 = ViewStyle::new_default();

        // Test a few properties
        style2.node_style_mut().opacity = Some(0.5);
        style2.node_style_mut().letter_spacing = Some(1.2);
        style2.layout_style_mut().flex_grow = 1.0;

        let diff = style1.difference(&style2);
        assert_eq!(diff.node_style().opacity, Some(0.5));
        assert_eq!(diff.node_style().letter_spacing, Some(1.2));
        assert_eq!(diff.layout_style().flex_grow, 1.0);

        // Test no difference
        style1.node_style_mut().opacity = Some(0.5);
        style1.node_style_mut().letter_spacing = Some(1.2);
        style1.layout_style_mut().flex_grow = 1.0;
        let diff2 = style1.difference(&style2);
        assert_eq!(diff2.node_style().opacity, None);
        assert_eq!(diff2.node_style().letter_spacing, None);
        assert_eq!(diff2.layout_style().flex_grow, 0.0);

        // Test all properties
        let mut style3 = ViewStyle::new_default();
        let mut style4 = ViewStyle::new_default();
        style4.node_style_mut().font_color =
            Some(Background::new_with_background(background::Background_type::Solid(
                crate::variable::ColorOrVar::new_color(crate::color::Color::red()),
            )))
            .into();
        style4.node_style_mut().font_size = Some(NumOrVar::from_num(24.0)).into();
        style4.node_style_mut().font_family = Some("Roboto".to_string());
        style4.node_style_mut().font_weight = Some(FontWeight::bold()).into();
        style4.node_style_mut().font_style = FontStyle::FONT_STYLE_ITALIC.into();
        style4.node_style_mut().text_decoration = TextDecoration::TEXT_DECORATION_UNDERLINE.into();
        style4.node_style_mut().letter_spacing = Some(2.0);
        style4.node_style_mut().font_stretch = Some(FontStretch::expanded()).into();
        style4.node_style_mut().backgrounds.push(Background::new_with_background(
            background::Background_type::Solid(crate::variable::ColorOrVar::new_color(
                crate::color::Color::blue(),
            )),
        ));
        style4.node_style_mut().stroke = Some(Stroke {
            stroke_weight: Some(StrokeWeight {
                stroke_weight_type: Some(stroke_weight::Stroke_weight_type::Uniform(1.0)),
                ..Default::default()
            })
            .into(),
            ..Default::default()
        })
        .into();
        style4.layout_style_mut().flex_direction =
            crate::positioning::FlexDirection::FLEX_DIRECTION_COLUMN.into();
        style4.layout_style_mut().align_items =
            crate::positioning::AlignItems::ALIGN_ITEMS_CENTER.into();
        style4.layout_style_mut().margin = Some(crate::geometry::DimensionRect {
            start: crate::geometry::DimensionProto::new_points(10.0),
            ..Default::default()
        })
        .into();

        let diff3 = style3.difference(&style4);
        assert_eq!(diff3.node_style().font_color, style4.node_style().font_color.clone());
        assert_eq!(diff3.node_style().font_size, style4.node_style().font_size.clone());
        assert_eq!(diff3.node_style().font_family, style4.node_style().font_family.clone());
        assert_eq!(diff3.node_style().font_weight, style4.node_style().font_weight.clone());
        assert_eq!(diff3.node_style().font_style, style4.node_style().font_style);
        assert_eq!(diff3.node_style().text_decoration, style4.node_style().text_decoration);
        assert_eq!(diff3.node_style().letter_spacing, style4.node_style().letter_spacing);
        assert_eq!(diff3.node_style().font_stretch, style4.node_style().font_stretch.clone());
        assert_eq!(diff3.node_style().backgrounds, style4.node_style().backgrounds.clone());
        assert_eq!(diff3.node_style().stroke, style4.node_style().stroke.clone());
        assert_eq!(diff3.layout_style().flex_direction, style4.layout_style().flex_direction);
        assert_eq!(diff3.layout_style().align_items, style4.layout_style().align_items);
        assert_eq!(diff3.layout_style().margin, style4.layout_style().margin.clone());

        // Test no difference with all properties set
        style3 = style4.clone();
        let diff4 = style3.difference(&style4);
        assert_eq!(diff4.node_style().font_color, ViewStyle::new_default().node_style().font_color);
        assert_eq!(diff4.node_style().font_size, ViewStyle::new_default().node_style().font_size);
        assert!(diff4.node_style().font_family.is_none());
        assert_eq!(
            diff4.node_style().font_weight,
            ViewStyle::new_default().node_style().font_weight
        );
        assert_eq!(
            diff4.node_style().font_style.enum_value().unwrap(),
            FontStyle::FONT_STYLE_NORMAL
        );
        assert_eq!(
            diff4.node_style().text_decoration.enum_value().unwrap(),
            TextDecoration::TEXT_DECORATION_NONE
        );
        assert!(diff4.node_style().letter_spacing.is_none());
        assert_eq!(
            diff4.node_style().font_stretch,
            ViewStyle::new_default().node_style().font_stretch
        );
        assert!(diff4.node_style().backgrounds.is_empty());
        assert_eq!(diff4.node_style().stroke, ViewStyle::new_default().node_style().stroke);
        assert_eq!(
            diff4.layout_style().flex_direction.enum_value().unwrap(),
            crate::positioning::FlexDirection::FLEX_DIRECTION_ROW
        );
        assert_eq!(
            diff4.layout_style().align_items.enum_value().unwrap(),
            crate::positioning::AlignItems::ALIGN_ITEMS_STRETCH
        );
        assert_eq!(diff4.layout_style().margin, ViewStyle::new_default().layout_style().margin);
    }

    #[test]
    fn test_view_new_rect() {
        let view = View::new_rect(
            &"rect1".to_string(),
            &"Rect View".to_string(),
            ViewShape::default(),
            ViewStyle::new_default(),
            None,
            None,
            ScrollInfo::new_default(),
            None,
            None,
            RenderMethod::RENDER_METHOD_NONE,
            HashMap::new(),
        );
        assert_eq!(view.id, "rect1");
        assert_eq!(view.name, "Rect View");
        assert!(matches!(
            view.data.unwrap().view_data_type,
            Some(View_data_type::Container { .. })
        ));
    }

    #[test]
    fn test_view_new_text() {
        let view = View::new_text(
            &"text1".to_string(),
            &"Text View".to_string(),
            ViewStyle::new_default(),
            None,
            None,
            "Hello",
            None,
            None,
            RenderMethod::RENDER_METHOD_NONE,
            HashMap::new(),
        );
        assert_eq!(view.id, "text1");
        assert_eq!(view.name, "Text View");
        assert!(matches!(view.data.unwrap().view_data_type, Some(View_data_type::Text { .. })));
    }

    #[test]
    fn test_view_add_child() {
        let mut parent = View::new_rect(
            &"parent".to_string(),
            &"Parent".to_string(),
            ViewShape::default(),
            ViewStyle::new_default(),
            None,
            None,
            ScrollInfo::new_default(),
            None,
            None,
            RenderMethod::RENDER_METHOD_NONE,
            HashMap::new(),
        );
        let child = View::new_rect(
            &"child".to_string(),
            &"Child".to_string(),
            ViewShape::default(),
            ViewStyle::new_default(),
            None,
            None,
            ScrollInfo::new_default(),
            None,
            None,
            RenderMethod::RENDER_METHOD_NONE,
            HashMap::new(),
        );
        parent.add_child(child);
        if let Some(View_data_type::Container { 0: Container { children, .. } }) =
            parent.data.unwrap().view_data_type
        {
            assert_eq!(children.len(), 1);
            assert_eq!(children[0].id, "child");
        } else {
            panic!("Wrong data type");
        }
    }

    #[test]
    fn test_find_view_by_id() {
        let child = View::new_rect(
            &"child".to_string(),
            &"Child".to_string(),
            ViewShape::default(),
            ViewStyle::new_default(),
            None,
            None,
            ScrollInfo::new_default(),
            None,
            None,
            RenderMethod::RENDER_METHOD_NONE,
            HashMap::new(),
        );
        let mut parent = View::new_rect(
            &"parent".to_string(),
            &"Parent".to_string(),
            ViewShape::default(),
            ViewStyle::new_default(),
            None,
            None,
            ScrollInfo::new_default(),
            None,
            None,
            RenderMethod::RENDER_METHOD_NONE,
            HashMap::new(),
        );
        parent.add_child(child);
        assert!(parent.find_view_by_id(&"child".to_string()).is_some());
        assert!(parent.find_view_by_id(&"parent".to_string()).is_some());
        assert!(parent.find_view_by_id(&"I1;child".to_string()).is_some());
        assert!(parent.find_view_by_id(&"nonexistent".to_string()).is_none());
    }

    #[test]
    fn test_view_data_difference() {
        // Test different text
        let view_data1 = ViewData {
            view_data_type: Some(View_data_type::Text(Text {
                content: "hello".to_string(),
                ..Default::default()
            })),
            ..Default::default()
        };
        let view_data2 = ViewData {
            view_data_type: Some(View_data_type::Text(Text {
                content: "world".to_string(),
                ..Default::default()
            })),
            ..Default::default()
        };
        assert_eq!(view_data1.difference(&view_data2), Some(view_data2.clone()));

        // Test same text
        let view_data3 = ViewData {
            view_data_type: Some(View_data_type::Text(Text {
                content: "hello".to_string(),
                ..Default::default()
            })),
            ..Default::default()
        };
        assert_eq!(view_data1.difference(&view_data3), None);

        // Test different styled text
        let view_data4 = ViewData {
            view_data_type: Some(View_data_type::StyledText(StyledTextRuns {
                styled_texts: vec![],
                ..Default::default()
            })),
            ..Default::default()
        };
        let view_data5 = ViewData {
            view_data_type: Some(View_data_type::StyledText(StyledTextRuns {
                styled_texts: vec![StyledTextRun::default()],
                ..Default::default()
            })),
            ..Default::default()
        };
        assert_eq!(view_data4.difference(&view_data5), Some(view_data5.clone()));

        // Test same styled text
        let view_data6 = ViewData {
            view_data_type: Some(View_data_type::StyledText(StyledTextRuns {
                styled_texts: vec![],
                ..Default::default()
            })),
            ..Default::default()
        };
        assert_eq!(view_data4.difference(&view_data6), None);

        // Test container type
        let view_data7 = ViewData {
            view_data_type: Some(View_data_type::Container(Container::default())),
            ..Default::default()
        };
        let view_data8 = ViewData {
            view_data_type: Some(View_data_type::Container(Container {
                children: vec![View::default()],
                ..Default::default()
            })),
            ..Default::default()
        };
        assert_eq!(view_data7.difference(&view_data8), None);
    }
}
