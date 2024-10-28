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

//! Flexbox definitions derived from `stretch` 0.3.2 licensed MIT.
//! https://github.com/vislyhq/stretch

use std::collections::HashMap;
use std::f32::consts::PI;

use crate::toolkit_style::MeterDataSchema;

use crate::{
    component_context::ComponentContext,
    extended_layout_schema::{ExtendedAutoLayout, LayoutType, SizePolicy},
    image_context::ImageContext,
    variable_utils::{bound_variables_color, FromFigmaVar},
};
use crate::{figma_schema, Error};
use dc_bundle::definition::element::{
    view_shape, DimensionProto, DimensionRect, DimensionRectExt, FontFeature, FontStyle,
    FontWeight, ViewShape,
};

use crate::figma_schema::LayoutPositioning;
use crate::reaction_schema::{FrameExtrasJson, ReactionJson};
use dc_bundle::definition;
use dc_bundle::definition::element::dimension_proto::Dimension;
use dc_bundle::definition::element::line_height::LineHeight;
use dc_bundle::definition::element::num_or_var::NumOrVarType;
use dc_bundle::definition::element::view_shape::RoundRect;
use dc_bundle::definition::element::Path;
use dc_bundle::definition::element::{
    background, stroke_weight, Background, StrokeAlign, StrokeWeight,
};
use dc_bundle::definition::interaction::Reaction;
use dc_bundle::definition::layout::FlexWrap;
use dc_bundle::definition::modifier::{filter_op, BoxShadow, FilterOp, TextShadow};
use dc_bundle::definition::modifier::{BlendMode, LayoutTransform};
use dc_bundle::definition::plugin::FrameExtras;
use dc_bundle::legacy_definition::layout::grid::{GridLayoutType, GridSpan};
use dc_bundle::legacy_definition::layout::positioning::{
    AlignContent, AlignItems, AlignSelf, FlexDirection, ItemSpacing, JustifyContent, LayoutSizing,
    Overflow, OverflowDirection, PositionType,
};
use dc_bundle::legacy_definition::modifier::text::{TextAlign, TextAlignVertical, TextOverflow};
use dc_bundle::legacy_definition::view::component::ComponentInfo;
use dc_bundle::legacy_definition::view::text_style::{StyledTextRun, TextStyle};
use dc_bundle::legacy_definition::view::view::{RenderMethod, ScrollInfo, View};
use dc_bundle::legacy_definition::view::view_style::ViewStyle;
use log::error;
use unicode_segmentation::UnicodeSegmentation;

// If an Auto content preview widget specifies a "Hug contents" sizing policy, this
// overrides a fixed size sizing policy on its parent to allow it to grow to fit
// all of its child nodes.
fn check_child_size_override(node: &figma_schema::Node) -> Option<LayoutType> {
    for child in node.children.iter() {
        if child.is_widget() {
            let plugin_data = child.shared_plugin_data.get("designcompose");
            if let Some(vsw_data) = plugin_data {
                if let Some(text_layout) = vsw_data.get("vsw-extended-auto-layout") {
                    let extended_auto_layout: Option<ExtendedAutoLayout> =
                        serde_json::from_str(text_layout.as_str()).ok();
                    if let Some(extended_auto_layout) = extended_auto_layout {
                        if extended_auto_layout.auto_layout_data.size_policy == SizePolicy::Hug {
                            return Some(extended_auto_layout.layout);
                        }
                    }
                }
            }
        }
    }
    None
}

fn check_text_node_string_res(node: &figma_schema::Node) -> Option<String> {
    let plugin_data = node.shared_plugin_data.get("designcompose");
    if let Some(vsw_data) = plugin_data {
        return vsw_data.get("vsw-string-res").cloned();
    }
    None
}

// Map Figma's new flexbox-based Auto Layout properties to our own flexbox-based layout
// properties.
fn compute_layout(
    node: &figma_schema::Node,
    parent: Option<&figma_schema::Node>,
) -> Result<ViewStyle, Error> {
    let mut style = ViewStyle::default();

    // Determine if the parent is using Auto Layout (and thus is a Flexbox parent) or if it isn't.
    let parent_frame = parent.and_then(|p| p.frame());
    let parent_bounding_box = parent.and_then(|p| p.absolute_bounding_box);
    //let parent_is_root = parent.is_none();
    let parent_is_flexbox = if let Some(frame) = parent_frame {
        !frame.layout_mode.is_none()
    } else {
        // The root container is from our toolkit, and uses flexbox.
        //parent_is_root
        false
    };
    let mut hug_width = false;
    let mut hug_height = false;

    if let Some(bounds) = node.absolute_bounding_box {
        style.layout_style.bounding_box.width = bounds.width();
        style.layout_style.bounding_box.height = bounds.height();
    }

    // Frames can implement Auto Layout on their children.
    if let Some(frame) = node.frame() {
        style.layout_style.position_type = match frame.layout_positioning {
            LayoutPositioning::Absolute => PositionType::Absolute,
            LayoutPositioning::Auto => PositionType::Relative,
        };
        style.layout_style.width = DimensionProto::new_auto();
        style.layout_style.height = DimensionProto::new_auto();
        style.layout_style.flex_grow = frame.layout_grow;
        style.layout_style.flex_basis = if frame.layout_grow == 1.0 {
            // When layout_grow is 1, it means the node's width/height is set to FILL.
            // Figma doesn't explicitly provide this info, but we need flex_basis = 0
            // for it to grow from zero to fill the container.
            DimensionProto::new_points(0.0)
        } else {
            DimensionProto::new_undefined()
        };
        style.layout_style.flex_shrink = 0.0;
        style.node_style.horizontal_sizing = frame.layout_sizing_horizontal.into();
        style.node_style.vertical_sizing = frame.layout_sizing_vertical.into();

        // Check for a flex direction override, which can happen if this node has a child widget
        let flex_direction_override = check_child_size_override(node);
        if let Some(dir) = flex_direction_override {
            style.layout_style.flex_direction = match dir {
                LayoutType::Horizontal => FlexDirection::Row,
                LayoutType::Vertical => FlexDirection::Column,
                _ => FlexDirection::None,
            };
        } else {
            style.layout_style.flex_direction = match frame.layout_mode {
                figma_schema::LayoutMode::Horizontal => FlexDirection::Row,
                figma_schema::LayoutMode::Vertical => FlexDirection::Column,
                figma_schema::LayoutMode::None => FlexDirection::None,
            };
        }
        style.layout_style.padding = Some(DimensionRect {
            start: DimensionProto::new_points(frame.padding_left),
            end: DimensionProto::new_points(frame.padding_right),
            top: DimensionProto::new_points(frame.padding_top),
            bottom: DimensionProto::new_points(frame.padding_bottom),
        });

        style.layout_style.item_spacing = ItemSpacing::Fixed(frame.item_spacing as i32);

        match frame.layout_align {
            // Counter axis stretch
            Some(figma_schema::LayoutAlign::Stretch) => {
                style.layout_style.align_self = AlignSelf::Stretch;
            }
            _ => (),
        };
        style.layout_style.align_items = match frame.counter_axis_align_items {
            figma_schema::LayoutAlignItems::Center => AlignItems::Center,
            figma_schema::LayoutAlignItems::Max => AlignItems::FlexEnd,
            figma_schema::LayoutAlignItems::Min => AlignItems::FlexStart,
            figma_schema::LayoutAlignItems::SpaceBetween => AlignItems::FlexStart, // XXX
            figma_schema::LayoutAlignItems::Baseline => AlignItems::FlexStart,
        };
        style.layout_style.justify_content = match frame.primary_axis_align_items {
            figma_schema::LayoutAlignItems::Center => JustifyContent::Center,
            figma_schema::LayoutAlignItems::Max => JustifyContent::FlexEnd,
            figma_schema::LayoutAlignItems::Min => JustifyContent::FlexStart,
            figma_schema::LayoutAlignItems::SpaceBetween => JustifyContent::SpaceBetween,
            figma_schema::LayoutAlignItems::Baseline => JustifyContent::FlexStart,
        };
        // The toolkit picks "Stretch" as a sensible default, but we don't
        // want that for Figma elements.
        style.layout_style.align_content = AlignContent::FlexStart;

        let align_self_stretch = style.layout_style.align_self == AlignSelf::Stretch;
        if let Some(bounds) = node.absolute_bounding_box {
            // If align_self is set to stretch, we want width/height to be Auto, even if the
            // frame's primary or counter axis sizing mode is set to Fixed.
            let dim_points_or_auto = |points| {
                if align_self_stretch {
                    //|| parent_is_root {
                    DimensionProto::new_auto()
                } else {
                    DimensionProto::new_points(points)
                }
            };
            match frame.layout_mode {
                figma_schema::LayoutMode::Horizontal => {
                    hug_width =
                        frame.primary_axis_sizing_mode == figma_schema::LayoutSizingMode::Auto;
                    hug_height =
                        frame.counter_axis_sizing_mode == figma_schema::LayoutSizingMode::Auto;
                    style.layout_style.width = match frame.primary_axis_sizing_mode {
                        figma_schema::LayoutSizingMode::Fixed => {
                            dim_points_or_auto(bounds.width().ceil())
                        }
                        figma_schema::LayoutSizingMode::Auto => DimensionProto::new_auto(),
                    };
                    style.layout_style.height = match frame.counter_axis_sizing_mode {
                        figma_schema::LayoutSizingMode::Fixed => {
                            dim_points_or_auto(bounds.height().ceil())
                        }
                        figma_schema::LayoutSizingMode::Auto => DimensionProto::new_auto(),
                    };
                    if hug_width {
                        style.layout_style.min_width = DimensionProto::new_auto();
                    }
                }
                figma_schema::LayoutMode::Vertical => {
                    hug_width =
                        frame.counter_axis_sizing_mode == figma_schema::LayoutSizingMode::Auto;
                    hug_height =
                        frame.primary_axis_sizing_mode == figma_schema::LayoutSizingMode::Auto;
                    style.layout_style.width = match frame.counter_axis_sizing_mode {
                        figma_schema::LayoutSizingMode::Fixed => {
                            dim_points_or_auto(bounds.width().ceil())
                        }
                        figma_schema::LayoutSizingMode::Auto => DimensionProto::new_auto(),
                    };
                    style.layout_style.height = match frame.primary_axis_sizing_mode {
                        figma_schema::LayoutSizingMode::Fixed => {
                            dim_points_or_auto(bounds.height().ceil())
                        }
                        figma_schema::LayoutSizingMode::Auto => DimensionProto::new_auto(),
                    };
                    if hug_height {
                        style.layout_style.min_height = DimensionProto::new_auto();
                    }
                }
                _ => {
                    // Frame is not autolayout, so use the layout sizing mode
                    // to determine size. If we have a node size specified, use
                    // that instead of the bounds since the node size is the
                    // size without rotation and scale.
                    let (width, height) = if let Some(size) = &node.size {
                        (size.x(), size.y())
                    } else {
                        (bounds.width().ceil(), bounds.height().ceil())
                    };

                    if frame.layout_sizing_horizontal == figma_schema::LayoutSizing::Fill {
                        style.layout_style.width = DimensionProto::new_auto();
                    } else {
                        style.layout_style.width = DimensionProto::new_points(width);
                    }

                    if frame.layout_sizing_vertical == figma_schema::LayoutSizing::Fill {
                        style.layout_style.height = DimensionProto::new_auto();
                    } else {
                        style.layout_style.height = DimensionProto::new_points(height);
                    }
                }
            }

            if frame.layout_mode != figma_schema::LayoutMode::None {
                let width_points = bounds.width().ceil();
                let height_points = bounds.height().ceil();
                style.layout_style.width = match frame.layout_sizing_horizontal {
                    figma_schema::LayoutSizing::Fixed => DimensionProto::new_points(width_points),
                    figma_schema::LayoutSizing::Fill => DimensionProto::new_auto(),
                    figma_schema::LayoutSizing::Hug => DimensionProto::new_auto(),
                };
                style.layout_style.height = match frame.layout_sizing_vertical {
                    figma_schema::LayoutSizing::Fixed => DimensionProto::new_points(height_points),
                    figma_schema::LayoutSizing::Fill => DimensionProto::new_auto(),
                    figma_schema::LayoutSizing::Hug => DimensionProto::new_auto(),
                };
            }
        }
    }

    // Setup widget size to expand to its parent
    let mut is_widget_or_parent_widget = node.is_widget();
    if let Some(parent) = parent {
        if !is_widget_or_parent_widget {
            is_widget_or_parent_widget = parent.is_widget();
        }
    }
    if is_widget_or_parent_widget {
        style.layout_style.position_type = PositionType::Absolute;
        style.layout_style.width = DimensionProto::new_auto();
        style.layout_style.height = DimensionProto::new_auto();
        style.layout_style.left = DimensionProto::new_points(0.0);
        style.layout_style.right = DimensionProto::new_points(0.0);
        style.layout_style.top = DimensionProto::new_points(0.0);
        style.layout_style.bottom = DimensionProto::new_points(0.0);
    }

    // Vector layers have some layout properties for themselves, but not for their children.
    if let Some(vector) = node.vector() {
        match vector.layout_align {
            // Counter axis stretch
            Some(figma_schema::LayoutAlign::Stretch) => {
                style.layout_style.align_self = AlignSelf::Stretch;
            }
            _ => (),
        };
        style.layout_style.position_type = match vector.layout_positioning {
            LayoutPositioning::Absolute => PositionType::Absolute,
            LayoutPositioning::Auto => PositionType::Relative,
        };
        style.layout_style.width = DimensionProto::new_auto();
        style.layout_style.height = DimensionProto::new_auto();
    }
    if let Some(bounds) = node.absolute_bounding_box {
        if !hug_width {
            style.layout_style.min_width = DimensionProto::new_points(bounds.width().ceil());
        }
        if !hug_height {
            style.layout_style.min_height = DimensionProto::new_points(bounds.height().ceil());
        }
    }

    if let Some(size) = &node.size {
        if size.is_valid() {
            if !hug_width {
                style.layout_style.min_width = DimensionProto::new_points(size.x());
            }
            if !hug_height {
                style.layout_style.min_height = DimensionProto::new_points(size.y());
            }
            // Set fixed vector size
            // TODO need to change to support scale?
            if node.vector().is_some() {
                style.layout_style.width = DimensionProto::new_points(size.x());
                style.layout_style.height = DimensionProto::new_points(size.y());
            }

            style.node_style.node_size.width = size.x();
            style.node_style.node_size.height = size.y();
        }
    }

    // For text we want to force the width.
    if let figma_schema::NodeData::Text { vector, style: text_style, .. } = &node.data {
        style.layout_style.flex_grow = vector.layout_grow;
        if vector.layout_grow == 1.0 {
            // When the value of layout_grow is 1, it is because the node has
            // its width or height set to FILL. Figma's node properties don't
            // specify this, but flex_basis needs to be set to 0 so that it
            // starts out small and grows to fill the container
            style.layout_style.flex_basis = DimensionProto::new_points(0.0);
        }
        style.node_style.horizontal_sizing = vector.layout_sizing_horizontal.into();
        style.node_style.vertical_sizing = vector.layout_sizing_vertical.into();

        // The text style also contains some layout information. We previously exposed
        // auto-width text in our plugin.
        match text_style.text_auto_resize {
            figma_schema::TextAutoResize::Height => {
                if vector.layout_sizing_horizontal == figma_schema::LayoutSizing::Fill {
                    style.layout_style.width = DimensionProto::new_auto();
                } else {
                    style.layout_style.width = style.layout_style.min_width;
                }
                style.layout_style.height = DimensionProto::new_auto();
            }
            figma_schema::TextAutoResize::WidthAndHeight => {
                style.layout_style.min_width = DimensionProto::new_auto();
                style.layout_style.width = DimensionProto::new_auto();
                style.layout_style.height = DimensionProto::new_auto();
            }
            // TextAutoResize::Truncate is deprecated
            // Use fixed width and height
            _ => {
                style.layout_style.width = style.layout_style.min_width;
                style.layout_style.height = style.layout_style.min_height;
            }
        }
    }

    if !parent_is_flexbox || style.layout_style.position_type == PositionType::Absolute {
        match (node.absolute_bounding_box, parent_bounding_box) {
            (Some(bounds), Some(parent_bounds)) => {
                style.layout_style.position_type = PositionType::Absolute;

                // Figure out all the values we might need when calculating the layout constraints.
                let (width, height) = if let Some(size) = &node.size {
                    (size.x(), size.y())
                } else {
                    (bounds.width().ceil(), bounds.height().ceil())
                };
                let left = (bounds.x() - parent_bounds.x()).round();
                let right = parent_bounds.width().ceil() - (left + bounds.width().ceil()); // px from our right edge to parent's right edge
                let top = (bounds.y() - parent_bounds.y()).round();
                let bottom = parent_bounds.height().ceil() - (top + bounds.height().ceil());

                match node.constraints().map(|c| c.horizontal) {
                    Some(figma_schema::HorizontalLayoutConstraintValue::Left) | None => {
                        style.layout_style.left = DimensionProto::new_percent(0.0);
                        style.layout_style.right = DimensionProto::new_auto();
                        style.layout_style.margin.set_start(Dimension::Points(left))?;

                        if !hug_width && !node.is_text() {
                            style.layout_style.width = DimensionProto::new_points(width);
                        }
                    }
                    Some(figma_schema::HorizontalLayoutConstraintValue::Right) => {
                        style.layout_style.left = DimensionProto::new_auto();
                        style.layout_style.right = DimensionProto::new_percent(0.0);
                        style.layout_style.margin.set_end(Dimension::Points(right))?;
                        if !hug_width && !node.is_text() {
                            style.layout_style.width = DimensionProto::new_points(width);
                        }
                    }
                    Some(figma_schema::HorizontalLayoutConstraintValue::LeftRight) => {
                        style.layout_style.left = DimensionProto::new_percent(0.0);
                        style.layout_style.right = DimensionProto::new_percent(0.0);
                        style.layout_style.margin.set_start(Dimension::Points(left))?;
                        style.layout_style.margin.set_end(Dimension::Points(right))?;
                        style.layout_style.width = DimensionProto::new_auto();
                    }
                    Some(figma_schema::HorizontalLayoutConstraintValue::Center) => {
                        // Centering with absolute positioning isn't directly possible; instead we
                        // give our style a left/top margin of 50%, which centers the left/top edge
                        // within the parent, then we apply the delta to move it to the correct
                        // location using the left/top property. All of this adds up to position the
                        // component where it was in Figma, but anchored to the horizontal/vertical
                        // centerpoint.
                        style.layout_style.left = DimensionProto::new_percent(0.5);
                        style.layout_style.right = DimensionProto::new_auto();
                        style.layout_style.margin.set_start(Dimension::Points(
                            left - parent_bounds.width().ceil() / 2.0,
                        ))?;
                        if !hug_width && !node.is_text() {
                            style.layout_style.width = DimensionProto::new_points(width);
                        }
                    }
                    Some(figma_schema::HorizontalLayoutConstraintValue::Scale) => {
                        let is_zero: bool = parent_bounds.width() == 0.0;
                        style.layout_style.left = DimensionProto::new_percent(if is_zero {
                            0.0
                        } else {
                            left / parent_bounds.width().ceil()
                        });
                        style.layout_style.right = DimensionProto::new_percent(if is_zero {
                            0.0
                        } else {
                            right / parent_bounds.width().ceil()
                        });
                        style.layout_style.width = DimensionProto::new_auto();
                        style.layout_style.min_width = DimensionProto::new_auto();
                    }
                }

                match node.constraints().map(|c| c.vertical) {
                    Some(figma_schema::VerticalLayoutConstraintValue::Top) | None => {
                        style.layout_style.top = DimensionProto::new_percent(0.0);
                        style.layout_style.bottom = DimensionProto::new_auto();
                        style.layout_style.margin.set_top(Dimension::Points(top))?;
                        if !hug_height && !node.is_text() {
                            style.layout_style.height = DimensionProto::new_points(height);
                        }
                    }
                    Some(figma_schema::VerticalLayoutConstraintValue::Bottom) => {
                        style.layout_style.top = DimensionProto::new_auto();
                        style.layout_style.bottom = DimensionProto::new_percent(0.0);
                        style.layout_style.margin.set_bottom(Dimension::Points(bottom))?;
                        if !hug_height && !node.is_text() {
                            style.layout_style.height = DimensionProto::new_points(height);
                        }
                    }
                    Some(figma_schema::VerticalLayoutConstraintValue::TopBottom) => {
                        style.layout_style.top = DimensionProto::new_percent(0.0);
                        style.layout_style.bottom = DimensionProto::new_percent(0.0);
                        style.layout_style.margin.set_top(Dimension::Points(top))?;
                        style.layout_style.margin.set_bottom(Dimension::Points(bottom))?;
                        style.layout_style.height = DimensionProto::new_auto();
                    }
                    Some(figma_schema::VerticalLayoutConstraintValue::Center) => {
                        style.layout_style.top = DimensionProto::new_percent(0.5);
                        style.layout_style.bottom = DimensionProto::new_auto();
                        style.layout_style.margin.set_top(Dimension::Points(
                            top - parent_bounds.height().ceil() / 2.0,
                        ))?;
                        if !hug_height && !node.is_text() {
                            style.layout_style.height = DimensionProto::new_points(height);
                        }
                    }
                    Some(figma_schema::VerticalLayoutConstraintValue::Scale) => {
                        let is_zero: bool = parent_bounds.height() == 0.0;
                        let top = if is_zero { 0.0 } else { top / parent_bounds.height().ceil() };
                        let bottom =
                            if is_zero { 0.0 } else { bottom / parent_bounds.height().ceil() };
                        style.layout_style.top = DimensionProto::new_percent(top);
                        style.layout_style.bottom = DimensionProto::new_percent(bottom);
                        style.layout_style.height = DimensionProto::new_auto();
                        style.layout_style.min_height = DimensionProto::new_auto();
                    }
                }
            }
            _ => {}
        }
    }

    Ok(style)
}

fn convert_transform(transform: &figma_schema::Transform) -> LayoutTransform {
    LayoutTransform::row_major_2d(
        transform[0][0].unwrap_or(1.0),
        transform[1][0].unwrap_or(0.0),
        transform[0][1].unwrap_or(0.0),
        transform[1][1].unwrap_or(1.0),
        transform[0][2].unwrap_or(0.0),
        transform[1][2].unwrap_or(0.0),
    )
}

fn convert_blend_mode(blend_mode: Option<figma_schema::BlendMode>) -> BlendMode {
    match blend_mode {
        Some(figma_schema::BlendMode::PassThrough) | None => BlendMode::PassThrough,
        Some(figma_schema::BlendMode::Normal) => BlendMode::Normal,
        Some(figma_schema::BlendMode::Darken) => BlendMode::Darken,
        Some(figma_schema::BlendMode::Multiply) => BlendMode::Multiply,
        Some(figma_schema::BlendMode::LinearBurn) => BlendMode::LinearBurn,
        Some(figma_schema::BlendMode::ColorBurn) => BlendMode::ColorBurn,
        Some(figma_schema::BlendMode::Lighten) => BlendMode::Lighten,
        Some(figma_schema::BlendMode::Screen) => BlendMode::Screen,
        Some(figma_schema::BlendMode::LinearDodge) => BlendMode::LinearDodge,
        Some(figma_schema::BlendMode::ColorDodge) => BlendMode::ColorDodge,
        Some(figma_schema::BlendMode::Overlay) => BlendMode::Overlay,
        Some(figma_schema::BlendMode::SoftLight) => BlendMode::SoftLight,
        Some(figma_schema::BlendMode::HardLight) => BlendMode::HardLight,
        Some(figma_schema::BlendMode::Difference) => BlendMode::Difference,
        Some(figma_schema::BlendMode::Exclusion) => BlendMode::Exclusion,
        Some(figma_schema::BlendMode::Hue) => BlendMode::Hue,
        Some(figma_schema::BlendMode::Saturation) => BlendMode::Saturation,
        Some(figma_schema::BlendMode::Color) => BlendMode::Color,
        Some(figma_schema::BlendMode::Luminosity) => BlendMode::Luminosity,
    }
}

fn compute_background(
    last_paint: &figma_schema::Paint,
    images: &mut ImageContext,
    node_name: &String,
) -> Background {
    if let figma_schema::PaintData::Solid { color, bound_variables } = &last_paint.data {
        let solid_bg = bound_variables_color(bound_variables, color, last_paint.opacity);
        Background::new(background::BackgroundType::Solid(solid_bg))
    } else if let figma_schema::PaintData::Image {
        image_ref: Some(image_ref),
        filters,
        scale_mode,
        image_transform,
        scaling_factor,
        rotation,
        ..
    } = &last_paint.data
    {
        let mut image_filter_list = Vec::new();

        if let Some(image_filters) = filters {
            if let Some(sat) = image_filters.saturation {
                image_filter_list
                    .push(FilterOp::new(filter_op::FilterOpType::Grayscale(sat * -1.0)));
            }

            if let Some(contrast) = image_filters.contrast {
                image_filter_list
                    .push(FilterOp::new(filter_op::FilterOpType::Contrast(contrast + 1.0)));
            }

            if let Some(exposure) = image_filters.exposure {
                let exp_adj: f32;
                //Below 0, it's linear to -1.  But don't let it get set to -1 or it goes black.

                if exposure > 0.0 {
                    exp_adj = 1.0 + exposure * 6.0; //webrender's scaling for brightness isn't linear
                } else {
                    exp_adj = 0.1 + (1.0 + exposure);
                }

                image_filter_list.push(FilterOp::new(filter_op::FilterOpType::Brightness(exp_adj)));
            }
        }

        // Figma's image transform is inverted from what most graphics APIs expect, so invert
        // it here to avoid doing matrix inversions at render time.
        let mut transform = image_transform
            .map(|tx| convert_transform(&tx).inverse().unwrap_or(LayoutTransform::identity()))
            .unwrap_or(LayoutTransform::identity());

        // Figma has already applied the rotation in "stretch" mode.
        if *rotation != 0.0 && *scale_mode != figma_schema::ScaleMode::Stretch {
            transform =
                transform.pre_rotate(0.0, 0.0, 1.0, euclid::Angle::degrees(*rotation).get());
        }

        if let Some(scale_factor) = *scaling_factor {
            transform = transform.post_scale(scale_factor, scale_factor, scale_factor);
        }

        let bg_scale_mode = match scale_mode {
            figma_schema::ScaleMode::Tile => background::ScaleMode::Tile,
            figma_schema::ScaleMode::Fill => background::ScaleMode::Fill,
            figma_schema::ScaleMode::Fit => background::ScaleMode::Fit,
            figma_schema::ScaleMode::Stretch => background::ScaleMode::Stretch,
        };

        if let Some(fill) = images.image_fill(image_ref, node_name) {
            Background::new(background::BackgroundType::Image(background::Image {
                key: Some(fill),
                filters: image_filter_list,
                transform: Some(transform.to_2d()),
                scale_mode: bg_scale_mode as i32,
                opacity: last_paint.opacity,
                res_name: images.image_res(image_ref),
            }))
        } else if !image_filter_list.is_empty() {
            // There's no image but we have filters, so store those with no image in case there's
            // a runtime customization that specifies an image source.
            Background::new(background::BackgroundType::Image(background::Image {
                key: None,
                filters: image_filter_list,
                transform: Some(transform.to_2d()),
                scale_mode: bg_scale_mode as i32,
                opacity: last_paint.opacity,
                res_name: None,
            }))
        } else {
            Background::new(background::BackgroundType::None(()))
        }
    } else if let figma_schema::PaintData::GradientLinear { gradient } = &last_paint.data {
        let start_x = gradient.gradient_handle_positions[0].x();
        let start_y = gradient.gradient_handle_positions[0].y();
        let end_x = gradient.gradient_handle_positions[1].x();
        let end_y = gradient.gradient_handle_positions[1].y();

        let mut g_stops: Vec<background::ColorStop> = Vec::new();

        let stops = &gradient.gradient_stops;

        if stops.is_empty() {
            error!("No stops found for linear gradient {:?}", gradient);
            Background::new(background::BackgroundType::None(()))
        } else {
            for s in stops {
                let c = bound_variables_color(&s.bound_variables, &s.color, last_paint.opacity);
                let g = background::ColorStop { position: s.position, color: Some(c) };
                g_stops.push(g);
            }

            Background::new(background::BackgroundType::LinearGradient(
                background::LinearGradient { start_x, start_y, end_x, end_y, color_stops: g_stops },
            ))
        }
    } else if let figma_schema::PaintData::GradientAngular { gradient } = &last_paint.data {
        let center_x = gradient.gradient_handle_positions[0].x();
        let center_y = gradient.gradient_handle_positions[0].y();
        let end_x = gradient.gradient_handle_positions[1].x();
        let end_y = gradient.gradient_handle_positions[1].y();
        let cross_x = gradient.gradient_handle_positions[2].x();
        let cross_y = gradient.gradient_handle_positions[2].y();

        let angle = (f32::atan2(center_x - end_x, end_y - center_y) + PI) % (2.0 * PI);
        let scale = f32::sqrt(f32::powf(end_x - center_x, 2.0) + f32::powf(end_y - center_y, 2.0))
            / f32::sqrt(f32::powf(cross_x - center_x, 2.0) + f32::powf(cross_y - center_y, 2.0));

        let mut g_stops: Vec<background::ColorStop> = Vec::new();

        let stops = &gradient.gradient_stops;

        if stops.is_empty() {
            error!("No stops found for angular gradient {:?}", gradient);
            Background::new(background::BackgroundType::None(()))
        } else {
            for s in stops {
                let c = bound_variables_color(&s.bound_variables, &s.color, last_paint.opacity);
                let g = background::ColorStop { position: s.position, color: Some(c) };
                g_stops.push(g);
            }

            Background::new(background::BackgroundType::AngularGradient(
                background::AngularGradient {
                    center_x: center_x,
                    center_y: center_y,
                    angle: angle,
                    scale: scale,
                    color_stops: g_stops,
                },
            ))
        }
    } else if let figma_schema::PaintData::GradientRadial { gradient } = &last_paint.data {
        let center_x = gradient.gradient_handle_positions[0].x();
        let center_y = gradient.gradient_handle_positions[0].y();
        let end_x = gradient.gradient_handle_positions[1].x();
        let end_y = gradient.gradient_handle_positions[1].y();
        let width_x = gradient.gradient_handle_positions[2].x();
        let width_y = gradient.gradient_handle_positions[2].y();

        let angle = (f32::atan2(center_x - end_x, end_y - center_y) + PI) % (2.0 * PI);
        let radius = (
            f32::sqrt(f32::powf(end_x - center_x, 2.0) + f32::powf(end_y - center_y, 2.0)),
            f32::sqrt(f32::powf(width_x - center_x, 2.0) + f32::powf(width_y - center_y, 2.0)),
        );

        let mut g_stops: Vec<background::ColorStop> = Vec::new();

        let stops = &gradient.gradient_stops;

        if stops.is_empty() {
            error!("No stops found for radial gradient {:?}", gradient);
            Background::new(background::BackgroundType::None(()))
        } else {
            for s in stops {
                let c = bound_variables_color(&s.bound_variables, &s.color, last_paint.opacity);
                let g = background::ColorStop { position: s.position, color: Some(c) };
                g_stops.push(g);
            }

            Background::new(background::BackgroundType::RadialGradient(
                background::RadialGradient {
                    center_x: center_x,
                    center_y: center_y,
                    angle: angle,
                    radius_x: radius.0,
                    radius_y: radius.1,
                    color_stops: g_stops,
                },
            ))
        }
    } else if let figma_schema::PaintData::GradientDiamond { gradient } = &last_paint.data {
        let center_x = gradient.gradient_handle_positions[0].x();
        let center_y = gradient.gradient_handle_positions[0].y();
        let end_x = gradient.gradient_handle_positions[1].x();
        let end_y = gradient.gradient_handle_positions[1].y();
        let width_x = gradient.gradient_handle_positions[2].x();
        let width_y = gradient.gradient_handle_positions[2].y();

        let angle = (f32::atan2(center_x - end_x, end_y - center_y) + PI) % (2.0 * PI);
        let radius = (
            f32::sqrt(f32::powf(end_x - center_x, 2.0) + f32::powf(end_y - center_y, 2.0)),
            f32::sqrt(f32::powf(width_x - center_x, 2.0) + f32::powf(width_y - center_y, 2.0)),
        );

        let mut g_stops: Vec<background::ColorStop> = Vec::new();

        let stops = &gradient.gradient_stops;
        if stops.is_empty() {
            error!("No stops found for diamond gradient {:?}", gradient);
            Background::new(background::BackgroundType::None(()))
        } else {
            for s in stops {
                let c = bound_variables_color(&s.bound_variables, &s.color, last_paint.opacity);
                let g = background::ColorStop { position: s.position, color: Some(c) };
                g_stops.push(g);
            }

            Background::new(background::BackgroundType::RadialGradient(
                background::RadialGradient {
                    center_x: center_x,
                    center_y: center_y,
                    angle: angle,
                    radius_x: radius.0,
                    radius_y: radius.1,
                    color_stops: g_stops,
                },
            ))
        }
    } else {
        Background::new(background::BackgroundType::None(()))
    }
}

// We have a 1:1 correspondence between Nodes and Views which is super nice.
fn visit_node(
    node: &figma_schema::Node,
    parent: Option<&figma_schema::Node>,
    component_map: &HashMap<String, figma_schema::Component>,
    component_set_map: &HashMap<String, figma_schema::ComponentSet>,
    component_context: &mut ComponentContext,
    images: &mut ImageContext,
    parent_plugin_data: Option<&HashMap<String, String>>,
) -> Result<View, Error> {
    // See if we have any plugin data. If we have plugin data passed in, it came from a parent
    // widget, so use it. Otherwise look for it in the shared plugin data.
    let mut plugin_data = None;
    let mut child_plugin_data = None;
    if parent_plugin_data.is_some() {
        plugin_data = parent_plugin_data;
    } else if node.shared_plugin_data.contains_key("designcompose") {
        plugin_data = node.shared_plugin_data.get("designcompose");
        if node.is_widget() {
            child_plugin_data = plugin_data;
            plugin_data = None;
        }
    }

    // First determine our layout style, then accumulate our visual style.
    let mut style = compute_layout(node, parent)?;

    let mut component_info = None;
    // If this is an instance of a component, then link to the original component so that we can substitute
    // Figma components for alternative implementations when the toolkit_schema tree gets mapped to actual
    // views.
    if let figma_schema::NodeData::Instance { component_id, .. } = &node.data {
        if let Some(component_metadata) = component_map.get(component_id) {
            component_info = Some(ComponentInfo {
                name: component_metadata.name.clone(),
                id: component_id.clone(),
                component_set_name: component_set_map
                    .get(&component_metadata.component_set_id)
                    .unwrap_or(&figma_schema::ComponentSet::default())
                    .name
                    .clone(),
                overrides: None,
            });
            // Make sure we convert this component node so that we can compute any variant style delta
            // later on.
            component_context.add_component_info(component_id);
        }
    }
    // If this is a component, also set the component info so that if the variant changes at runtime, the
    // variant node has this component info and can use it to change to the other variant.
    if let figma_schema::NodeData::Component { frame: _ } = &node.data {
        if let Some(component_metadata) = component_map.get(&node.id) {
            component_info = Some(ComponentInfo {
                name: component_metadata.name.clone(),
                id: node.id.clone(),
                component_set_name: component_set_map
                    .get(&component_metadata.component_set_id)
                    .unwrap_or(&figma_schema::ComponentSet::default())
                    .name
                    .clone(),
                overrides: None,
            });
        }
    }

    // Check relative transform to see if there is a rotation.
    if let Some(transform) = node.relative_transform {
        let parent_bounding_box = parent.and_then(|p| p.absolute_bounding_box);
        let bounds = node.absolute_bounding_box;

        fn get(transform: [[Option<f32>; 3]; 2], a: usize, b: usize) -> f32 {
            assert!(a <= 1);
            assert!(b <= 2);
            transform[a][b].unwrap_or(0.0)
        }

        if let (Some(parent_bounds), Some(bounds)) = (parent_bounding_box, bounds) {
            let r = LayoutTransform::row_major_2d(
                get(transform, 0, 0),
                get(transform, 1, 0),
                get(transform, 0, 1),
                get(transform, 1, 1),
                get(transform, 0, 2),
                get(transform, 1, 2),
            );

            // Provide an unaltered transform from the last relevant parent.
            style.node_style.relative_transform = Some(r.clone());
            // And an additional transform with translation removed.
            let r = r.post_translate(
                -(bounds.x() - parent_bounds.x()),
                -(bounds.y() - parent_bounds.y()),
                0.0,
            );
            style.node_style.transform = Some(r);
        }
    }

    // If there's interaction info from our plugin that syncs interactive data then
    // parse it out.
    let reactions: Option<Vec<Reaction>> = {
        if let Some(vsw_data) = plugin_data {
            if let Some(reactions) = vsw_data.get("vsw-reactions") {
                serde_json::from_str(reactions.as_str())
                    .map(|mut x: Vec<ReactionJson>| {
                        // Convert from ReactionJson to Reaction
                        let reaction: Vec<Reaction> = x
                            .drain(..)
                            .map(|r| Into::<Option<Reaction>>::into(r))
                            .filter(|maybe_reaction| {
                                if maybe_reaction.is_none() {
                                    println!(
                                        "Warning: reaction has empty action. Json: {}",
                                        reactions
                                    );
                                }
                                maybe_reaction.is_some()
                            })
                            .map(|some_reaction| some_reaction.unwrap())
                            .collect();

                        // Tell the component context about these reactions, too.
                        component_context.add_destination_nodes(&reaction);

                        Some(reaction)
                    })
                    .unwrap_or_else(|e| {
                        println!("Error parsing reaction: {}. Json: {}", e, reactions);
                        None
                    })
            } else {
                None
            }
        } else {
            None
        }
    };

    // We have some extra info for frames from the plugin which isn't present
    // in the REST response.
    let frame_extras: Option<FrameExtras> = {
        if let Some(vsw_data) = plugin_data {
            if let Some(extras) = vsw_data.get("vsw-frame-extras") {
                let json: Option<FrameExtrasJson> = serde_json::from_str(extras.as_str()).ok();
                json.map(|j| j.into())
            } else {
                None
            }
        } else {
            None
        }
    };

    let mut scroll_info = ScrollInfo::default();

    let extended_auto_layout: Option<ExtendedAutoLayout> = {
        if let Some(vsw_data) = plugin_data {
            if let Some(text_layout) = vsw_data.get("vsw-extended-auto-layout") {
                serde_json::from_str(text_layout.as_str()).ok()
            } else {
                None
            }
        } else {
            None
        }
    };

    let mut has_extended_auto_layout = false;
    let mut is_widget_list = false;
    let mut size_policy = SizePolicy::default();

    // Apply the extra auto layout properties.
    if let Some(extended_auto_layout) = extended_auto_layout {
        let layout = extended_auto_layout.layout;
        // Only consider extended auto layout if the layout is not None. This filters out old
        // extended layout data that may have been written to Figma nodes using an old widget
        // or our old extended layout plugin.
        has_extended_auto_layout = layout != LayoutType::None;
        style.node_style.flex_wrap =
            if extended_auto_layout.wrap { FlexWrap::Wrap } else { FlexWrap::NoWrap };

        style.node_style.grid_layout = match layout {
            LayoutType::AutoColumns => Some(GridLayoutType::AutoColumns),
            LayoutType::FixedColumns => Some(GridLayoutType::FixedColumns),
            LayoutType::FixedRows => Some(GridLayoutType::FixedRows),
            LayoutType::AutoRows => Some(GridLayoutType::AutoRows),
            LayoutType::Horizontal => Some(GridLayoutType::Horizontal),
            LayoutType::Vertical => Some(GridLayoutType::Vertical),
            _ => None,
        };

        if layout.is_grid() {
            let cld = &extended_auto_layout.common_data;
            let gld = &extended_auto_layout.grid_layout_data;
            let horizontal =
                layout == LayoutType::FixedColumns || layout == LayoutType::AutoColumns;

            style.node_style.grid_adaptive_min_size = gld.adaptive_min_size;
            style.node_style.grid_columns_rows = extended_auto_layout.grid_layout_data.columns_rows;

            if let Some(padding) = style.layout_style.padding.as_mut() {
                padding.start = DimensionProto::new_points(cld.margin_left);
                padding.end = DimensionProto::new_points(cld.margin_right);
                padding.top = DimensionProto::new_points(cld.margin_top);
                padding.bottom = DimensionProto::new_points(cld.margin_bottom);
            }

            style.layout_style.item_spacing = if gld.auto_spacing {
                if horizontal {
                    ItemSpacing::Auto(gld.horizontal_spacing, gld.auto_spacing_item_size)
                } else {
                    ItemSpacing::Auto(gld.vertical_spacing, gld.auto_spacing_item_size)
                }
            } else if horizontal {
                ItemSpacing::Fixed(gld.horizontal_spacing)
            } else {
                ItemSpacing::Fixed(gld.vertical_spacing)
            };
            style.node_style.cross_axis_item_spacing = if horizontal {
                gld.vertical_spacing as f32
            } else {
                gld.horizontal_spacing as f32
            };

            if cld.scrolling {
                scroll_info.overflow = if horizontal {
                    OverflowDirection::VerticalScrolling
                } else {
                    OverflowDirection::HorizontalScrolling
                };
            }

            for grid_span in extended_auto_layout.grid_layout_data.span_content {
                // Parse variant name with format "property1=value1, property2=value2" into variant_hash
                let mut variant_hash: HashMap<String, String> = HashMap::new();
                let props_and_names = grid_span.node_name.split(',');
                for prop_var in props_and_names {
                    let prop_variant = prop_var.to_string();
                    let variant_parts: Vec<&str> = prop_variant.split('=').collect();
                    if variant_parts.len() == 2 {
                        variant_hash
                            .insert(variant_parts[0].to_string(), variant_parts[1].to_string());
                    } else {
                        println!("Invalid grid span variant: {:?}", variant_parts);
                    }
                }

                style.node_style.grid_span_content.push(GridSpan {
                    node_name: grid_span.node_name.clone(),
                    node_variant: variant_hash,
                    span: grid_span.span,
                    max_span: grid_span.max_span,
                })
            }
        } else if layout.is_row_or_column() {
            // For a horizontal or vertical autolayout, the widget uses the
            // standard Figma autolayout properties which is already handled by the frame layout
            // code above, so all we need to do is set the scrolling info if it is checked and the
            // space between boolean.
            if extended_auto_layout.common_data.scrolling {
                scroll_info.overflow = if layout == LayoutType::Vertical {
                    OverflowDirection::VerticalScrolling
                } else {
                    OverflowDirection::HorizontalScrolling
                };
            }
            if extended_auto_layout.auto_layout_data.space_between {
                style.layout_style.justify_content = JustifyContent::SpaceBetween;
            }
            is_widget_list = true;
            size_policy = extended_auto_layout.auto_layout_data.size_policy;
        }
        // Store the extra scrolling properties in scroll_info
        scroll_info.paged_scrolling = extended_auto_layout.paged_scrolling;

        // Set parameters that limit the number of children for this frame
        if extended_auto_layout.limit_content {
            let lcd = extended_auto_layout.limit_content_data;
            style.node_style.max_children = Some(lcd.max_num_items);
            style.node_style.overflow_node_id = Some(lcd.overflow_node_id);
            style.node_style.overflow_node_name = Some(lcd.overflow_node_name);
        }
    }

    if !has_extended_auto_layout && !is_widget_list {
        let plugin_data = node.shared_plugin_data.get("designcompose");
        if let Some(plugin_data) = plugin_data {
            if let Some(auto_layout_str) = plugin_data.get("vsw-extended-auto-layout") {
                let extended_auto_layout: Option<ExtendedAutoLayout> =
                    serde_json::from_str(auto_layout_str.as_str()).ok();
                if let Some(extended_auto_layout) = extended_auto_layout {
                    is_widget_list = extended_auto_layout.layout.is_row_or_column();
                    size_policy = extended_auto_layout.auto_layout_data.size_policy;
                }
            }
        }
    }

    // If this node is a widget or a child of a widget with a horizontal or vertical layout,
    // we need to set our size depending on whether the size policy is hug or fixed.
    if is_widget_list {
        if size_policy == SizePolicy::Hug {
            style.layout_style.position_type = PositionType::Relative;
            style.layout_style.width = DimensionProto::new_auto();
            style.layout_style.height = DimensionProto::new_auto();
        } else {
            style.layout_style.position_type = PositionType::Absolute;
        }
    }

    // Blend mode is common to all elements.
    style.node_style.blend_mode = convert_blend_mode(node.blend_mode);

    for stroke in node.strokes.iter().filter(|paint| paint.visible) {
        style.node_style.stroke.strokes.push(compute_background(stroke, images, &node.name));
    }

    // Copy out the common styles from frames and supported content.
    style.node_style.opacity = if node.opacity < 1.0 { Some(node.opacity) } else { None };
    if let Some(individual_stroke_weights) = node.individual_stroke_weights {
        style.node_style.stroke.stroke_weight = Some(StrokeWeight {
            stroke_weight_type: Some(stroke_weight::StrokeWeightType::Individual(
                stroke_weight::Individual {
                    top: individual_stroke_weights.top,
                    right: individual_stroke_weights.right,
                    bottom: individual_stroke_weights.bottom,
                    left: individual_stroke_weights.left,
                },
            )),
        });
    } else if let Some(stroke_weight) = node.stroke_weight {
        style.node_style.stroke.stroke_weight = Some(StrokeWeight {
            stroke_weight_type: Some(stroke_weight::StrokeWeightType::Uniform(stroke_weight)),
        });
    }
    style.node_style.stroke.stroke_align = match node.stroke_align {
        Some(figma_schema::StrokeAlign::Inside) => StrokeAlign::Inside as i32,
        Some(figma_schema::StrokeAlign::Center) => StrokeAlign::Center as i32,
        Some(figma_schema::StrokeAlign::Outside) | None => StrokeAlign::Outside as i32,
    };

    // Pull out the visual style for "frame-ish" nodes.
    if let Some(frame) = node.frame() {
        style.node_style.overflow =
            if frame.clips_content { Overflow::Hidden } else { Overflow::Visible };
        // Don't overwrite scroll behavior if it was already set from grid layout
        if scroll_info.overflow == OverflowDirection::None {
            scroll_info.overflow = frame.overflow_direction.into();
        }
    } else if let figma_schema::NodeData::Text {
        characters,
        style: text_style,
        character_style_overrides,
        style_override_table,
        ..
    } = &node.data
    {
        if let Some(text_fill) = node.fills.iter().filter(|paint| paint.visible).last() {
            style.node_style.text_color = compute_background(text_fill, images, &node.name);
        }
        style.node_style.font_size = if let Some(vars) = &node.bound_variables {
            NumOrVarType::from_var(vars, "fontSize", text_style.font_size)
        } else {
            NumOrVarType::Num(text_style.font_size)
        };

        style.node_style.font_weight = if let Some(vars) = &node.bound_variables {
            FontWeight::new(NumOrVarType::from_var(vars, "fontWeight", text_style.font_weight))
        } else {
            FontWeight::from_num(text_style.font_weight)
        };
        if text_style.italic {
            style.node_style.font_style = FontStyle::Italic;
        }
        style.node_style.text_decoration = match text_style.text_decoration {
            figma_schema::TextDecoration::None => {
                dc_bundle::definition::element::TextDecoration::None
            }
            figma_schema::TextDecoration::Underline => {
                dc_bundle::definition::element::TextDecoration::Underline
            }
            figma_schema::TextDecoration::Strikethrough => {
                dc_bundle::definition::element::TextDecoration::Strikethrough
            }
        };
        style.node_style.letter_spacing = Some(text_style.letter_spacing.clone());
        style.node_style.font_family = text_style.font_family.clone();
        match text_style.text_align_horizontal {
            figma_schema::TextAlignHorizontal::Center => {
                style.node_style.text_align = TextAlign::Center
            }
            figma_schema::TextAlignHorizontal::Left => {
                style.node_style.text_align = TextAlign::Left
            }
            figma_schema::TextAlignHorizontal::Right => {
                style.node_style.text_align = TextAlign::Right
            }
            figma_schema::TextAlignHorizontal::Justified => {
                style.node_style.text_align = TextAlign::Center
            } // XXX
        }
        style.node_style.text_align_vertical = match text_style.text_align_vertical {
            figma_schema::TextAlignVertical::Center => TextAlignVertical::Center,
            figma_schema::TextAlignVertical::Top => TextAlignVertical::Top,
            figma_schema::TextAlignVertical::Bottom => TextAlignVertical::Bottom,
        };
        style.node_style.line_height = match text_style.line_height_unit {
            // It's a percentage of the font size.
            figma_schema::LineHeightUnit::FontSizePercentage => LineHeight::Pixels(
                text_style.font_size * text_style.line_height_percent_font_size / 100.0,
            ),
            // It's a percentage of the intrinsic line height of the font itself.
            figma_schema::LineHeightUnit::IntrinsicPercentage => {
                LineHeight::Percent(text_style.line_height_percent_font_size / 100.0)
            }
            // It's an absolute value in pixels.
            figma_schema::LineHeightUnit::Pixels => LineHeight::Pixels(text_style.line_height_px),
        };
        style.node_style.opacity = if node.opacity < 1.0 { Some(node.opacity) } else { None };
        let convert_opentype_flags = |flags: &HashMap<String, u32>| -> Vec<FontFeature> {
            let mut font_features = Vec::new();
            for (flag, value) in flags {
                let flag_ascii = flag.to_ascii_lowercase();
                if flag_ascii.len() == 4 {
                    // Smoke check to see if the flag is valid
                    font_features.push(FontFeature { tag: flag_ascii, enabled: *value == 1 });
                } else {
                    println!("Unsupported OpenType flag: {}", flag)
                }
            }
            font_features
        };
        style.node_style.font_features = convert_opentype_flags(&text_style.opentype_flags);

        // We can map the "drop shadow" effect to our text shadow functionality, but we can't
        // map the other effects. (We either implement them on Rects, or we don't implement in
        // a compatible way).
        for effect in &node.effects {
            if !effect.visible {
                continue;
            }
            match effect.effect_type {
                figma_schema::EffectType::DropShadow => {
                    let shadow_color =
                        bound_variables_color(&effect.bound_variables, &effect.color, 1.0);
                    style.node_style.text_shadow = Some(TextShadow {
                        blur_radius: effect.radius,
                        color: Some(shadow_color),
                        offset_x: effect.offset.x(),
                        offset_y: effect.offset.y(),
                    });
                }
                _ => {}
            }
        }

        if text_style.text_truncation == figma_schema::TextTruncation::Ending {
            style.node_style.text_overflow = TextOverflow::Ellipsis;
        }

        if text_style.max_lines > 0 {
            style.node_style.line_count = Some(text_style.max_lines as _);
        }
        if let Some(hl) = text_style.hyperlink.clone() {
            if hl.url.is_empty() {
                style.node_style.hyperlink = None
            } else {
                style.node_style.hyperlink = Some(hl.into());
            }
        }

        return if character_style_overrides.is_empty() {
            // No character customization, so this is just a plain styled text object
            Ok(View::new_text(
                &node.id,
                &node.name,
                style,
                component_info,
                reactions,
                characters,
                check_text_node_string_res(node),
                node.absolute_bounding_box.map(|r| (&r).into()),
                RenderMethod::None,
                node.explicit_variable_modes.clone(),
            ))
        } else {
            // Build some runs of custom styled text out of the style overrides. We need to be able to iterate
            // over unicode graphemes.
            //
            // XXX: This current method doesn't handle zero width joiners in compound emojis correctly; the modifier
            //      and zwj and emoji are treated as separate characters
            let graphemes: Vec<&str> = characters.graphemes(true).collect();
            let mut last_style: Option<usize> = None;
            let mut current_run = String::new();
            let mut runs: Vec<StyledTextRun> = Vec::new();
            let sub_style = text_style.to_sub_type_style();

            let mut flush = |current_run: &mut String, last_style: &mut Option<usize>| {
                if current_run.is_empty() {
                    return;
                }

                let sub_style = last_style.map_or(&sub_style, |id| {
                    style_override_table.get(&id.to_string()).unwrap_or(&sub_style)
                });

                // Use the color from the substyle fills, or fall back to the style color. This is pretty clunky.
                let text_color = if let Some(text_fill) =
                    sub_style.fills.iter().filter(|paint| paint.visible).last()
                {
                    let substyle_fill = compute_background(text_fill, images, &node.name);
                    if substyle_fill.is_some() {
                        substyle_fill
                    } else {
                        style.node_style.text_color.clone()
                    }
                } else {
                    style.node_style.text_color.clone()
                };
                let font_size = if let Some(fs) = sub_style.font_size {
                    NumOrVarType::Num(fs)
                } else {
                    style.node_style.font_size.clone()
                };
                let font_family = if sub_style.font_family.is_some() {
                    sub_style.font_family.clone()
                } else {
                    style.node_style.font_family.clone()
                };
                let font_weight = if let Some(fw) = sub_style.font_weight {
                    FontWeight::from_num(fw)
                } else {
                    style.node_style.font_weight.clone()
                };
                let font_style = if let Some(true) = sub_style.italic {
                    FontStyle::Italic
                } else {
                    style.node_style.font_style.clone()
                };
                let text_decoration = match sub_style.text_decoration {
                    Some(figma_schema::TextDecoration::Strikethrough) => {
                        dc_bundle::definition::element::TextDecoration::Strikethrough
                    }
                    Some(figma_schema::TextDecoration::Underline) => {
                        dc_bundle::definition::element::TextDecoration::Underline
                    }
                    Some(figma_schema::TextDecoration::None) => {
                        style.node_style.text_decoration.clone()
                    }
                    None => style.node_style.text_decoration.clone(),
                };
                let hyperlink = if let Some(hl) = sub_style.hyperlink.clone() {
                    if hl.url.is_empty() {
                        None
                    } else {
                        Some(hl.into())
                    }
                } else {
                    None
                };
                let style = TextStyle {
                    text_color,
                    font_size,
                    font_family,
                    font_weight,
                    font_style, // Italic or Normal
                    font_stretch: style.node_style.font_stretch.clone(), // Not in SubTypeStyle.
                    letter_spacing: sub_style
                        .letter_spacing
                        .unwrap_or(style.node_style.letter_spacing.unwrap_or(0.0)),
                    text_decoration,
                    line_height: style.node_style.line_height.clone(),
                    font_features: convert_opentype_flags(
                        &sub_style
                            .opentype_flags
                            .clone()
                            .unwrap_or(text_style.opentype_flags.clone()),
                    ),
                    hyperlink,
                };
                let mut has_handled = false;
                if runs.len() > 0 {
                    let last_run = runs.get(runs.len() - 1);
                    if let Some(lr) = last_run {
                        let last_run_style = lr.style.clone();
                        if last_run_style == style {
                            error!(
                                "The two styles are the same. This might fail to match the
                                localization plugin generated string resource. We will merge
                                them together."
                            );
                            let to_be_merged = runs.pop();
                            let mut new_run = String::new();
                            if let Some(tbm) = to_be_merged {
                                new_run.push_str(&tbm.text);
                                new_run.push_str(current_run);
                                runs.push(StyledTextRun { text: new_run, style: style.clone() });
                                has_handled = true;
                            }
                        }
                    }
                }
                if !has_handled {
                    runs.push(StyledTextRun { text: current_run.clone(), style: style.clone() });
                }
                *current_run = String::new();
                *last_style = None;
            };

            for (idx, g) in graphemes.iter().enumerate() {
                let style_id = if character_style_overrides.len() > idx {
                    Some(character_style_overrides[idx])
                } else {
                    None
                };
                // Do we need to flush the current run?
                if style_id != last_style && !current_run.is_empty() {
                    // Flush!
                    flush(&mut current_run, &mut last_style);
                    current_run = g.to_string();
                } else {
                    current_run = current_run + g;
                }
                last_style = style_id;
            }
            flush(&mut current_run, &mut last_style);

            Ok(View::new_styled_text(
                &node.id,
                &node.name,
                style,
                component_info,
                reactions,
                runs,
                check_text_node_string_res(node),
                node.absolute_bounding_box.map(|r| (&r).into()),
                RenderMethod::None,
            ))
        };
    }

    for fill in node.fills.iter().filter(|paint| paint.visible) {
        style.node_style.background.push(compute_background(fill, images, &node.name));
    }

    // Convert any path data we have; we'll use it for non-frame types.
    let fill_paths = if let Some(fills) = &node.fill_geometry {
        fills.iter().filter_map(parse_path).collect()
    } else {
        Vec::new()
    };

    // Normally the client will compute stroke paths itself, because Figma returns incorrect
    // stroke geometry for shapes with an area (e.g.: not lines) with Outside or Inside
    // stroke treatment. However, when a shape has no area (e.g.: it is a line), then the
    // fill geometry will be empty so we *have* to use the stroke geometry.
    let mut stroke_paths = if let Some(strokes) = &node.stroke_geometry {
        strokes.iter().filter_map(parse_path).collect()
    } else {
        Vec::new()
    };

    // Get the raw number values for the corner radii. If any are non-zero, set the boolean
    // has_corner_radius to true.
    let (corner_radius_values, mut has_corner_radius) =
        if let Some(border_radii) = node.rectangle_corner_radii {
            // rectangle_corner_radii is set if the corner radii are not all the same
            (
                [border_radii[0], border_radii[1], border_radii[2], border_radii[3]],
                border_radii[0] > 0.0
                    || border_radii[1] > 0.0
                    || border_radii[2] > 0.0
                    || border_radii[3] > 0.0,
            )
        } else if let Some(border_radius) = node.corner_radius {
            // corner_radius is set if all four corners are set to the same value
            ([border_radius, border_radius, border_radius, border_radius], border_radius > 0.0)
        } else {
            ([0.0, 0.0, 0.0, 0.0], false)
        };

    // Collect the corner radii values to be saved into the view. If the corner radii are set
    // to variables, they will be set to NumOrVar::Var. Otherwise they will be set to NumOrVar::Num.
    let corner_radius = if let Some(vars) = &node.bound_variables {
        let top_left = NumOrVarType::from_var(vars, "topLeftRadius", corner_radius_values[0]);
        let top_right = NumOrVarType::from_var(vars, "topRightRadius", corner_radius_values[1]);
        let bottom_right =
            NumOrVarType::from_var(vars, "bottomRightRadius", corner_radius_values[2]);
        let bottom_left = NumOrVarType::from_var(vars, "bottomLeftRadius", corner_radius_values[3]);
        if vars.has_var("topLeftRadius")
            || vars.has_var("topRightRadius")
            || vars.has_var("bottomRightRadius")
            || vars.has_var("bottomLeftRadius")
        {
            has_corner_radius = true;
        }
        [top_left, top_right, bottom_right, bottom_left]
    } else {
        [
            NumOrVarType::Num(corner_radius_values[0]),
            NumOrVarType::Num(corner_radius_values[1]),
            NumOrVarType::Num(corner_radius_values[2]),
            NumOrVarType::Num(corner_radius_values[3]),
        ]
    };

    let to_vector = |array: &[NumOrVarType; 4]| -> Vec<definition::element::NumOrVar> {
        array
            .clone()
            .map(|i| definition::element::NumOrVar { num_or_var_type: Some(i) })
            .into_iter()
            .collect()
    };

    let make_rect = |is_mask| -> ViewShape {
        if has_corner_radius {
            ViewShape::new_round_rect(RoundRect {
                corner_radii: to_vector(&corner_radius),
                corner_smoothing: 0.0, // Not in Figma REST API
                is_mask,
            })
        } else {
            ViewShape::new_rect(view_shape::Box { is_mask })
        }
    };

    // Check to see if there is additional plugin data to set this node up as
    // a type of meter (dials/gauges/progress bars)
    if let Some(vsw_data) = plugin_data {
        if let Some(data) = vsw_data.get("vsw-meter-data") {
            let meter_data: Option<MeterDataSchema> = serde_json::from_str(data.as_str()).ok();
            if let Some(meter_data) = meter_data {
                if let MeterDataSchema::ProgressVectorData(vector_data) = &meter_data {
                    // If this is a progress vector node, we read in data as a ProgressVectorMeterDataSchema,
                    // which contains vector drawing instructions as a string. We convert this into vector
                    // drawing instructions in a ProgressVectorMeterData struct and replace the normal stroke
                    // vector data with it.
                    if vector_data.enabled {
                        stroke_paths = vector_data.paths.iter().filter_map(parse_path).collect();
                    }
                }
                style.node_style.meter_data = Some(meter_data.into());
            }
        }
    }

    // Figure out the ViewShape from the node type.
    let view_shape = match &node.data {
        figma_schema::NodeData::BooleanOperation { vector, .. }
        | figma_schema::NodeData::Line { vector }
        | figma_schema::NodeData::RegularPolygon { vector }
        | figma_schema::NodeData::Star { vector }
        | figma_schema::NodeData::Vector { vector } => {
            ViewShape::new_path(view_shape::VectorPath {
                paths: fill_paths,
                strokes: stroke_paths,
                stroke_cap: node.stroke_cap.to_proto(),
                is_mask: vector.is_mask,
            })
        }
        // Rectangles get turned into a VectorRect instead of a Rect, RoundedRect or Path in order
        // to support progress bars. If this node is set up as a progress bar, the renderer will
        // construct the rectangle, modified by progress bar parameters. Otherwise, it will be
        // rendered as a ViewShape::Path.
        figma_schema::NodeData::Rectangle { vector } => {
            ViewShape::new_vector_rect(view_shape::VectorRect {
                paths: fill_paths.into(),
                strokes: stroke_paths.into(),
                corner_radii: to_vector(&corner_radius),
                is_mask: vector.is_mask,
            })
        }
        // Ellipses get turned into an Arc in order to support dials/gauges with an arc type
        // meter customization. If this node is setup as an arc type meter, the renderer will
        // construct the ellipse, modified by the arc parameters. Otherwise it will be rendered
        // as a ViewShape::Path.
        figma_schema::NodeData::Ellipse { vector, arc_data, .. } => {
            ViewShape::new_arc(view_shape::VectorArc {
                paths: fill_paths.into(),
                strokes: stroke_paths.into(),
                stroke_cap: node.stroke_cap.to_proto(),
                start_angle_degrees: euclid::Angle::radians(arc_data.starting_angle).to_degrees(),
                sweep_angle_degrees: euclid::Angle::radians(arc_data.ending_angle).to_degrees(),
                inner_radius: arc_data.inner_radius,
                // corner radius for arcs in dials & gauges does not support variables yet
                corner_radius: 0.0,
                is_mask: vector.is_mask,
            })
        }
        figma_schema::NodeData::Frame { frame }
        | figma_schema::NodeData::Group { frame }
        | figma_schema::NodeData::Component { frame }
        | figma_schema::NodeData::ComponentSet { frame }
        | figma_schema::NodeData::Instance { frame, .. } => make_rect(frame.is_mask),
        _ => make_rect(false),
    };

    for effect in &node.effects {
        if !effect.visible {
            continue;
        }
        match effect.effect_type {
            figma_schema::EffectType::DropShadow => {
                let shadow_color =
                    bound_variables_color(&effect.bound_variables, &effect.color, 1.0);
                style.node_style.box_shadow.push(BoxShadow::outset(
                    effect.radius,
                    effect.spread,
                    shadow_color,
                    (effect.offset.x(), effect.offset.y()),
                ))
            }
            figma_schema::EffectType::InnerShadow => {
                let shadow_color =
                    bound_variables_color(&effect.bound_variables, &effect.color, 1.0);
                style.node_style.box_shadow.push(BoxShadow::inset(
                    effect.radius,
                    effect.spread,
                    shadow_color,
                    (effect.offset.x(), effect.offset.y()),
                ))
            }
            figma_schema::EffectType::LayerBlur => {
                style
                    .node_style
                    .filter
                    .push(FilterOp::new(filter_op::FilterOpType::Blur(effect.radius / 2.0)));
            }
            figma_schema::EffectType::BackgroundBlur => {
                style
                    .node_style
                    .backdrop_filter
                    .push(FilterOp::new(filter_op::FilterOpType::Blur(effect.radius / 2.0)));
            }
        }
    }

    // Create our view node, now that we've finished populating style.
    let mut view = View::new_rect(
        &node.id,
        &node.name,
        view_shape,
        style,
        component_info,
        reactions,
        scroll_info,
        frame_extras,
        node.absolute_bounding_box.map(|r| (&r).into()),
        RenderMethod::None,
        node.explicit_variable_modes.clone(),
    );

    // Iterate over our visible children, but not vectors because they always
    // present their children's content themselves (e.g.: they are boolean products
    // of their children).
    if node.vector().is_none() {
        for child in node.children.iter() {
            if child.visible {
                view.add_child(visit_node(
                    child,
                    Some(node),
                    component_map,
                    component_set_map,
                    component_context,
                    images,
                    child_plugin_data,
                )?);
            }
        }
    }
    Ok(view)
}

/// Transform a node in the Figma schema to a View in the toolkit schema. From here, a
/// real toolkit View can be created, or the tree can be serialized and read in by
/// another process.
///
///  * `component`: The Figma node to start at.
///  * `component_map`: The map of Component ID to Component metadata from the document.
///  * `images`: ImageManager used for fetching image data referenced by the component.
///
pub fn create_component_flexbox(
    component: &figma_schema::Node,
    component_map: &HashMap<String, figma_schema::Component>,
    component_set_map: &HashMap<String, figma_schema::ComponentSet>,
    component_context: &mut ComponentContext,
    image_context: &mut ImageContext,
) -> core::result::Result<dc_bundle::legacy_definition::view::view::View, Error> {
    visit_node(
        component,
        None,
        component_map,
        component_set_map,
        component_context,
        image_context,
        None,
    )
}

fn parse_path(path: &figma_schema::Path) -> Option<Path> {
    let mut output = Path::new();
    for segment in svgtypes::SimplifyingPathParser::from(path.path.as_str()) {
        match segment {
            Ok(svgtypes::SimplePathSegment::MoveTo { x, y }) => {
                output.move_to(x as f32, y as f32);
            }
            Ok(svgtypes::SimplePathSegment::LineTo { x, y }) => {
                output.line_to(x as f32, y as f32);
            }
            Ok(svgtypes::SimplePathSegment::CurveTo { x1, y1, x2, y2, x, y }) => {
                output.cubic_to(x1 as f32, y1 as f32, x2 as f32, y2 as f32, x as f32, y as f32);
            }
            Ok(svgtypes::SimplePathSegment::Quadratic { x1, y1, x, y }) => {
                output.quad_to(x1 as f32, y1 as f32, x as f32, y as f32);
            }
            Ok(svgtypes::SimplePathSegment::ClosePath) => {
                output.close();
            }
            Err(_) => return None,
        }
    }
    output.with_winding_rule(path.winding_rule.into());
    Some(output)
}

impl Into<LayoutSizing> for figma_schema::LayoutSizing {
    fn into(self) -> LayoutSizing {
        match self {
            figma_schema::LayoutSizing::Fill => LayoutSizing::Fill,
            figma_schema::LayoutSizing::Fixed => LayoutSizing::Fixed,
            figma_schema::LayoutSizing::Hug => LayoutSizing::Hug,
        }
    }
}
