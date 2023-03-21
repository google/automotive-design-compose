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

use std::collections::HashMap;
use std::f32::consts::PI;

use crate::toolkit_font_style::{FontStyle, FontWeight};
use crate::toolkit_layout_style::{
    AlignContent, AlignItems, AlignSelf, Dimension, FlexDirection, FlexWrap, JustifyContent,
    Overflow, PositionType,
};

use crate::toolkit_schema::ViewShape;
use crate::toolkit_style::{
    Background, FilterOp, FontFeature, GridLayoutType, GridSpan, ItemSpacing, LayoutTransform,
    LineHeight, ShadowBox, StyledTextRun, TextAlign, TextOverflow, TextStyle, ViewStyle,
};

use crate::figma_schema::TextAutoResize;
use crate::vector_schema;
use crate::{
    component_context::ComponentContext,
    extended_layout_schema::{ExtendedAutoLayout, ExtendedTextLayout, LayoutType, SizePolicy},
    figma_schema::{
        BlendMode, Component, ComponentSet, EffectType, HorizontalLayoutConstraintValue,
        LayoutAlign, LayoutAlignItems, LayoutMode, LayoutSizingMode, LineHeightUnit, Node,
        NodeData, PaintData, StrokeAlign, TextAlignHorizontal, TextAlignVertical,
        VerticalLayoutConstraintValue,
    },
    image_context::ImageContext,
    reaction_schema::{FrameExtras, Reaction, ReactionJson},
    toolkit_schema::{ComponentInfo, OverflowDirection, ScrollInfo, View},
};

use euclid;
use unicode_segmentation::UnicodeSegmentation;

// If an Auto content preview widget specifies a "Hug contents" sizing policy, this
// overrides a fixed size sizing policy on its parent to allow it to grow to fit
// all of its child nodes.
fn check_child_size_override(node: &Node) -> Option<LayoutType> {
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
    return None;
}

// Map Figma's new flexbox-based Auto Layout properties to our own flexbox-based layout
// properties.
fn compute_layout(node: &Node, parent: Option<&Node>) -> ViewStyle {
    let mut style = ViewStyle::default();

    // Determine if the parent is using Auto Layout (and thus is a Flexbox parent) or if it isn't.
    let parent_frame = parent.map_or(None, |p| p.frame());
    let parent_bounding_box = parent.map_or(None, |p| p.absolute_bounding_box);
    let parent_is_root = parent.is_none();
    let parent_is_flexbox = if let Some(frame) = parent_frame {
        !frame.layout_mode.is_none()
    } else {
        // The root container is from our toolkit, and uses flexbox.
        parent_is_root
    };

    // Frames can implement Auto Layout on their children.
    if let Some(frame) = node.frame() {
        style.position_type = PositionType::Relative;
        style.width = Dimension::Auto;
        style.height = Dimension::Auto;
        style.flex_grow = frame.layout_grow;
        style.flex_shrink = 0.0;

        // Check for a flex direction override, which can happen if this node has a child widget
        let flex_direction_override = check_child_size_override(node);
        if let Some(dir) = flex_direction_override {
            style.flex_direction = match dir {
                LayoutType::Horizontal => FlexDirection::Row,
                LayoutType::Vertical => FlexDirection::Column,
                _ => FlexDirection::None,
            };
        } else {
            style.flex_direction = match frame.layout_mode {
                LayoutMode::Horizontal => FlexDirection::Row,
                LayoutMode::Vertical => FlexDirection::Column,
                LayoutMode::None => FlexDirection::None,
            };
        }
        style.padding.start = Dimension::Points(frame.padding_left);
        style.padding.end = Dimension::Points(frame.padding_right);
        style.padding.top = Dimension::Points(frame.padding_top);
        style.padding.bottom = Dimension::Points(frame.padding_bottom);

        style.item_spacing = ItemSpacing::Fixed(frame.item_spacing as i32);

        match frame.layout_align {
            // Counter axis stretch
            Some(LayoutAlign::Stretch) => {
                style.align_self = AlignSelf::Stretch;
            }
            _ => (),
        };
        style.align_items = match frame.counter_axis_align_items {
            LayoutAlignItems::Center => AlignItems::Center,
            LayoutAlignItems::Max => AlignItems::FlexEnd,
            LayoutAlignItems::Min => AlignItems::FlexStart,
            LayoutAlignItems::SpaceBetween => AlignItems::FlexStart, // XXX
        };
        style.justify_content = match frame.primary_axis_align_items {
            LayoutAlignItems::Center => JustifyContent::Center,
            LayoutAlignItems::Max => JustifyContent::FlexEnd,
            LayoutAlignItems::Min => JustifyContent::FlexStart,
            LayoutAlignItems::SpaceBetween => JustifyContent::SpaceBetween,
        };
        // The toolkit picks "Stretch" as a sensible default, but we don't
        // want that for Figma elements.
        style.align_content = AlignContent::FlexStart;

        let align_self_stretch = style.align_self == AlignSelf::Stretch;
        if let Some(bounds) = node.absolute_bounding_box {
            // If align_self is set to stretch, we want width/height to be Auto, even if the
            // frame's primary or counter axis sizing mode is set to Fixed.
            let dim_points_or_auto = |points| {
                if align_self_stretch || parent_is_root {
                    Dimension::Auto
                } else {
                    Dimension::Points(points)
                }
            };
            match frame.layout_mode {
                LayoutMode::Horizontal => {
                    style.width = match frame.primary_axis_sizing_mode {
                        LayoutSizingMode::Fixed => dim_points_or_auto(bounds.width().ceil()),
                        LayoutSizingMode::Auto => Dimension::Auto,
                    };
                    style.height = match frame.counter_axis_sizing_mode {
                        LayoutSizingMode::Fixed => dim_points_or_auto(bounds.height().ceil()),
                        LayoutSizingMode::Auto => Dimension::Auto,
                    };
                }
                _ => {
                    style.width = match frame.counter_axis_sizing_mode {
                        LayoutSizingMode::Fixed => dim_points_or_auto(bounds.width().ceil()),
                        LayoutSizingMode::Auto => Dimension::Auto,
                    };
                    style.height = match frame.primary_axis_sizing_mode {
                        LayoutSizingMode::Fixed => dim_points_or_auto(bounds.height().ceil()),
                        LayoutSizingMode::Auto => Dimension::Auto,
                    };
                }
            }
        }
    }

    // Setup widget size to expand to its parent
    if node.is_widget() {
        style.position_type = PositionType::Absolute;
        style.width = Dimension::Auto;
        style.height = Dimension::Auto;
        style.left = Dimension::Points(0.0);
        style.right = Dimension::Points(0.0);
        style.top = Dimension::Points(0.0);
        style.bottom = Dimension::Points(0.0);
    }

    // Vector layers have some layout properties for themselves, but not for their children.
    if let Some(vector) = node.vector() {
        match vector.layout_align {
            // Counter axis stretch
            Some(LayoutAlign::Stretch) => {
                style.align_self = AlignSelf::Stretch;
            }
            _ => (),
        };
        style.position_type = PositionType::Relative;
        style.width = Dimension::Auto;
        style.height = Dimension::Auto;
    }
    if let Some(bounds) = node.absolute_bounding_box {
        style.min_width = Dimension::Points(bounds.width().ceil());
        style.min_height = Dimension::Points(bounds.height().ceil());
    }

    if let Some(size) = &node.size {
        if size.is_valid() {
            style.min_width = Dimension::Points(size.x());
            style.min_height = Dimension::Points(size.y());
        }
    }

    // For text we want to force the width.
    if let NodeData::Text { vector, .. } = &node.data {
        style.flex_grow = vector.layout_grow;
        if style.flex_grow <= 0.0 {
            style.width = style.min_width;
        }
    }

    if !parent_is_flexbox {
        match (node.absolute_bounding_box, parent_bounding_box) {
            (Some(bounds), Some(parent_bounds)) => {
                style.position_type = PositionType::Absolute;

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
                    Some(HorizontalLayoutConstraintValue::Left) | None => {
                        style.left = Dimension::Points(left);
                        style.width = Dimension::Points(width);
                    }
                    Some(HorizontalLayoutConstraintValue::Right) => {
                        style.right = Dimension::Points(right);
                        style.width = Dimension::Points(width);
                    }
                    Some(HorizontalLayoutConstraintValue::LeftRight) => {
                        style.left = Dimension::Points(left);
                        style.right = Dimension::Points(right);
                        style.width = Dimension::Auto;
                    }
                    Some(HorizontalLayoutConstraintValue::Center) => {
                        // Centering with absolute positioning isn't directly possible; instead we
                        // give our style a left/top margin of 50%, which centers the left/top edge
                        // within the parent, then we apply the delta to move it to the correct
                        // location using the left/top property. All of this adds up to position the
                        // component where it was in Figma, but anchored to the horizontal/vertical
                        // centerpoint.
                        style.margin.start = Dimension::Percent(0.5);
                        style.left = Dimension::Points(left - parent_bounds.width().ceil() / 2.0);
                        style.width = Dimension::Points(width);
                    }
                    Some(HorizontalLayoutConstraintValue::Scale) => {
                        style.left = Dimension::Percent(left / parent_bounds.width().ceil());
                        style.right = Dimension::Percent(right / parent_bounds.width().ceil());
                        style.width = Dimension::Auto;
                    }
                }

                match node.constraints().map(|c| c.vertical) {
                    Some(VerticalLayoutConstraintValue::Top) | None => {
                        style.top = Dimension::Points(top);
                        style.height = Dimension::Points(height);
                    }
                    Some(VerticalLayoutConstraintValue::Bottom) => {
                        style.bottom = Dimension::Points(bottom);
                        style.height = Dimension::Points(height);
                    }
                    Some(VerticalLayoutConstraintValue::TopBottom) => {
                        style.top = Dimension::Points(top);
                        style.bottom = Dimension::Points(bottom);
                        style.height = Dimension::Auto;
                    }
                    Some(VerticalLayoutConstraintValue::Center) => {
                        style.margin.top = Dimension::Percent(0.5);
                        style.top = Dimension::Points(top - parent_bounds.height().ceil() / 2.0);
                        style.height = Dimension::Points(height);
                    }
                    Some(VerticalLayoutConstraintValue::Scale) => {
                        style.top = Dimension::Percent(top / parent_bounds.height().ceil());
                        style.bottom = Dimension::Percent(bottom / parent_bounds.height().ceil());
                        style.height = Dimension::Auto;
                    }
                }
            }
            _ => {}
        }
    }

    style
}

fn convert_transform(transform: &crate::figma_schema::Transform) -> LayoutTransform {
    LayoutTransform::row_major_2d(
        transform[0][0].unwrap_or(1.0),
        transform[1][0].unwrap_or(0.0),
        transform[0][1].unwrap_or(0.0),
        transform[1][1].unwrap_or(1.0),
        transform[0][2].unwrap_or(0.0),
        transform[1][2].unwrap_or(0.0),
    )
}

fn convert_blend_mode(
    blend_mode: Option<crate::figma_schema::BlendMode>,
) -> crate::toolkit_style::BlendMode {
    match blend_mode {
        Some(BlendMode::PassThrough) | None => crate::toolkit_style::BlendMode::PassThrough,
        Some(BlendMode::Normal) => crate::toolkit_style::BlendMode::Normal,
        Some(BlendMode::Darken) => crate::toolkit_style::BlendMode::Darken,
        Some(BlendMode::Multiply) => crate::toolkit_style::BlendMode::Multiply,
        Some(BlendMode::LinearBurn) => crate::toolkit_style::BlendMode::LinearBurn,
        Some(BlendMode::ColorBurn) => crate::toolkit_style::BlendMode::ColorBurn,
        Some(BlendMode::Lighten) => crate::toolkit_style::BlendMode::Lighten,
        Some(BlendMode::Screen) => crate::toolkit_style::BlendMode::Screen,
        Some(BlendMode::LinearDodge) => crate::toolkit_style::BlendMode::LinearDodge,
        Some(BlendMode::ColorDodge) => crate::toolkit_style::BlendMode::ColorDodge,
        Some(BlendMode::Overlay) => crate::toolkit_style::BlendMode::Overlay,
        Some(BlendMode::SoftLight) => crate::toolkit_style::BlendMode::SoftLight,
        Some(BlendMode::HardLight) => crate::toolkit_style::BlendMode::HardLight,
        Some(BlendMode::Difference) => crate::toolkit_style::BlendMode::Difference,
        Some(BlendMode::Exclusion) => crate::toolkit_style::BlendMode::Exclusion,
        Some(BlendMode::Hue) => crate::toolkit_style::BlendMode::Hue,
        Some(BlendMode::Saturation) => crate::toolkit_style::BlendMode::Saturation,
        Some(BlendMode::Color) => crate::toolkit_style::BlendMode::Color,
        Some(BlendMode::Luminosity) => crate::toolkit_style::BlendMode::Luminosity,
    }
}

fn compute_background(
    last_paint: &crate::figma_schema::Paint,
    images: &mut ImageContext,
    node_name: &String,
) -> crate::toolkit_style::Background {
    if let PaintData::Solid { color } = &last_paint.data {
        Background::Solid(crate::Color::from_f32s(
            color.r,
            color.g,
            color.b,
            color.a * last_paint.opacity,
        ))
    } else if let PaintData::Image {
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
                image_filter_list.push(FilterOp::Grayscale(sat * -1.0));
            }

            if let Some(contrast) = image_filters.contrast {
                image_filter_list.push(FilterOp::Contrast(contrast + 1.0));
            }

            if let Some(exposure) = image_filters.exposure {
                let exp_adj: f32;
                //Below 0, it's linear to -1.  But don't let it get set to -1 or it goes black.

                if exposure > 0.0 {
                    exp_adj = 1.0 + exposure * 6.0; //webrender's scaling for brightness isn't linear
                } else {
                    exp_adj = 0.1 + (1.0 + exposure);
                }

                image_filter_list.push(FilterOp::Brightness(exp_adj));
            }
        }

        // Figma's image transform is inverted from what most graphics APIs expect, so invert
        // it here to avoid doing matrix inversions at render time.
        let mut transform = image_transform
            .map(|tx| convert_transform(&tx).inverse().unwrap_or(LayoutTransform::identity()))
            .unwrap_or(LayoutTransform::identity());

        // Figma has already applied the rotation in "stretch" mode.
        if *rotation != 0.0 && *scale_mode != crate::figma_schema::ScaleMode::Stretch {
            transform = transform.pre_rotate(0.0, 0.0, 1.0, euclid::Angle::degrees(*rotation));
        }

        if let Some(scale_factor) = *scaling_factor {
            transform = transform.post_scale(scale_factor, scale_factor, scale_factor);
        }

        let bg_scale_mode = match scale_mode {
            crate::figma_schema::ScaleMode::Tile => crate::toolkit_style::ScaleMode::Tile,
            crate::figma_schema::ScaleMode::Fill => crate::toolkit_style::ScaleMode::Fill,
            crate::figma_schema::ScaleMode::Fit => crate::toolkit_style::ScaleMode::Fit,
            crate::figma_schema::ScaleMode::Stretch => crate::toolkit_style::ScaleMode::Stretch,
        };

        if let Some(fill) = images.image_fill(image_ref, node_name) {
            Background::Image {
                key: Some(fill),
                filters: image_filter_list,
                transform: Some(transform.to_2d()),
                opacity: last_paint.opacity,
                scale_mode: bg_scale_mode,
            }
        } else if !image_filter_list.is_empty() {
            // There's no image but we have filters, so store those with no image in case there's
            // a runtime customization that specifies an image source.
            Background::Image {
                key: None,
                filters: image_filter_list,
                transform: Some(transform.to_2d()),
                opacity: last_paint.opacity,
                scale_mode: bg_scale_mode,
            }
        } else {
            Background::None
        }
    } else if let PaintData::GradientLinear { gradient } = &last_paint.data {
        let start_x = gradient.gradient_handle_positions[0].x();
        let start_y = gradient.gradient_handle_positions[0].y();
        let end_x = gradient.gradient_handle_positions[1].x();
        let end_y = gradient.gradient_handle_positions[1].y();

        let mut g_stops: Vec<(f32, crate::Color)> = Vec::new();

        let stops = &gradient.gradient_stops;

        for s in stops {
            let c = crate::Color::from_f32s(
                s.color.r,
                s.color.g,
                s.color.b,
                s.color.a * last_paint.opacity,
            );

            let g = (s.position, c);

            g_stops.push(g);
        }

        Background::LinearGradient {
            start_x: start_x,
            start_y: start_y,
            end_x: end_x,
            end_y: end_y,
            color_stops: g_stops,
        }
    } else if let PaintData::GradientAngular { gradient } = &last_paint.data {
        let center_x = gradient.gradient_handle_positions[0].x();
        let center_y = gradient.gradient_handle_positions[0].y();
        let end_x = gradient.gradient_handle_positions[1].x();
        let end_y = gradient.gradient_handle_positions[1].y();
        let cross_x = gradient.gradient_handle_positions[2].x();
        let cross_y = gradient.gradient_handle_positions[2].y();

        let angle = (f32::atan2(center_x - end_x, end_y - center_y) + PI) % (2.0 * PI);
        let scale = f32::sqrt(f32::powf(end_x - center_x, 2.0) + f32::powf(end_y - center_y, 2.0))
            / f32::sqrt(f32::powf(cross_x - center_x, 2.0) + f32::powf(cross_y - center_y, 2.0));

        let mut g_stops: Vec<(f32, crate::Color)> = Vec::new();

        let stops = &gradient.gradient_stops;

        for s in stops {
            let c = crate::Color::from_f32s(
                s.color.r,
                s.color.g,
                s.color.b,
                s.color.a * last_paint.opacity,
            );

            let g = (s.position, c);

            g_stops.push(g);
        }

        Background::AngularGradient {
            center_x: center_x,
            center_y: center_y,
            angle: angle,
            scale: scale,
            color_stops: g_stops,
        }
    } else if let PaintData::GradientRadial { gradient } = &last_paint.data {
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

        let mut g_stops: Vec<(f32, crate::Color)> = Vec::new();

        let stops = &gradient.gradient_stops;

        for s in stops {
            let c = crate::Color::from_f32s(
                s.color.r,
                s.color.g,
                s.color.b,
                s.color.a * last_paint.opacity,
            );

            let g = (s.position, c);

            g_stops.push(g);
        }

        Background::RadialGradient {
            center_x: center_x,
            center_y: center_y,
            radius: radius,
            angle: angle,
            color_stops: g_stops,
        }
    } else if let PaintData::GradientDiamond { gradient } = &last_paint.data {
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

        let mut g_stops: Vec<(f32, crate::Color)> = Vec::new();

        let stops = &gradient.gradient_stops;

        for s in stops {
            let c = crate::Color::from_f32s(
                s.color.r,
                s.color.g,
                s.color.b,
                s.color.a * last_paint.opacity,
            );

            let g = (s.position, c);

            g_stops.push(g);
        }

        Background::RadialGradient {
            center_x: center_x,
            center_y: center_y,
            radius: radius,
            angle: angle,
            color_stops: g_stops,
        }
    } else {
        Background::None
    }
}

// We have a 1:1 correspondence between Nodes and Views which is super nice.
fn visit_node(
    node: &Node,
    parent: Option<&Node>,
    component_map: &HashMap<String, Component>,
    component_set_map: &HashMap<String, ComponentSet>,
    component_context: &mut ComponentContext,
    images: &mut ImageContext,
    parent_plugin_data: Option<&HashMap<String, String>>,
) -> View {
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
    let mut style = compute_layout(node, parent);

    let mut component_info = None;
    // If this is an instance of a component, then link to the original component so that we can substitute
    // Figma components for alternative implementations when the toolkit_schema tree gets mapped to actual
    // views.
    if let NodeData::Instance { component_id, .. } = &node.data {
        if let Some(component_metadata) = component_map.get(component_id) {
            component_info = Some(ComponentInfo {
                name: component_metadata.name.clone(),
                id: component_id.clone(),
                component_set_name: component_set_map
                    .get(&component_metadata.component_set_id)
                    .unwrap_or(&ComponentSet::default())
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
    if let NodeData::Component { frame: _ } = &node.data {
        if let Some(component_metadata) = component_map.get(&node.id) {
            component_info = Some(ComponentInfo {
                name: component_metadata.name.clone(),
                id: node.id.clone(),
                component_set_name: component_set_map
                    .get(&component_metadata.component_set_id)
                    .unwrap_or(&ComponentSet::default())
                    .name
                    .clone(),
                overrides: None,
            });
        }
    }

    // Check relative transform to see if there is a rotation.
    if let Some(transform) = node.relative_transform {
        let parent_bounding_box = parent.map_or(None, |p| p.absolute_bounding_box);
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
            let r = r.post_translate(euclid::vec3(
                -(bounds.x() - parent_bounds.x()),
                -(bounds.y() - parent_bounds.y()),
                0.0,
            ));
            style.transform = Some(r);
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
                        let reaction: Vec<Reaction> = x.drain(..).map(|r| r.into()).collect();

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
                serde_json::from_str(extras.as_str()).ok()
            } else {
                None
            }
        } else {
            None
        }
    };

    let mut scroll_info = ScrollInfo::default();

    // We have a plugin to set layout information that Figma doesn't have an interface for.
    // It sets values on two keys.
    let extended_text_layout: Option<ExtendedTextLayout> = {
        if let Some(vsw_data) = plugin_data {
            if let Some(text_layout) = vsw_data.get("vsw-extended-text-layout") {
                serde_json::from_str(text_layout.as_str()).ok()
            } else {
                None
            }
        } else {
            None
        }
    };
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

    let has_extended_auto_layout = extended_auto_layout.is_some();
    let mut is_widget_list = false;
    let mut size_policy = SizePolicy::default();

    // Apply the extra auto layout properties.
    if let Some(extended_auto_layout) = extended_auto_layout {
        let layout = extended_auto_layout.layout;
        style.flex_wrap = if extended_auto_layout.wrap { FlexWrap::Wrap } else { FlexWrap::NoWrap };
        if layout.is_grid() {
            let cld = &extended_auto_layout.common_data;
            let gld = &extended_auto_layout.grid_layout_data;
            let horizontal =
                layout == LayoutType::FixedColumns || layout == LayoutType::AutoColumns;

            style.grid_layout = Some(match layout {
                LayoutType::AutoColumns => GridLayoutType::AutoColumns,
                LayoutType::FixedRows => GridLayoutType::FixedRows,
                LayoutType::AutoRows => GridLayoutType::AutoRows,
                _ => GridLayoutType::FixedColumns,
            });

            style.grid_adaptive_min_size = gld.adaptive_min_size;
            style.grid_columns_rows = extended_auto_layout.grid_layout_data.columns_rows;
            style.padding.start = Dimension::Points(cld.margin_left);
            style.padding.end = Dimension::Points(cld.margin_right);
            style.padding.top = Dimension::Points(cld.margin_top);
            style.padding.bottom = Dimension::Points(cld.margin_bottom);
            style.item_spacing = if gld.auto_spacing {
                if horizontal {
                    ItemSpacing::Auto(gld.horizontal_spacing, gld.auto_spacing_item_size)
                } else {
                    ItemSpacing::Auto(gld.vertical_spacing, gld.auto_spacing_item_size)
                }
            } else {
                if horizontal {
                    ItemSpacing::Fixed(gld.horizontal_spacing)
                } else {
                    ItemSpacing::Fixed(gld.vertical_spacing)
                }
            };
            style.cross_axis_item_spacing = if horizontal {
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
                let props_and_names = grid_span.node_name.split(",");
                for prop_var in props_and_names {
                    let prop_variant = prop_var.to_string();
                    let variant_parts: Vec<&str> = prop_variant.split("=").collect();
                    if variant_parts.len() == 2 {
                        variant_hash
                            .insert(variant_parts[0].to_string(), variant_parts[1].to_string());
                    } else {
                        println!("Invalid grid span variant: {:?}", variant_parts);
                    }
                }

                style.grid_span_content.push(GridSpan {
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
                style.justify_content = JustifyContent::SpaceBetween;
            }
            is_widget_list = true;
            size_policy = extended_auto_layout.auto_layout_data.size_policy;
        }
        // Store the extra scrolling properties in scroll_info
        scroll_info.paged_scrolling = extended_auto_layout.paged_scrolling;

        // Set parameters that limit the number of children for this frame
        if extended_auto_layout.limit_content {
            let lcd = extended_auto_layout.limit_content_data;
            style.max_children = Some(lcd.max_num_items);
            style.overflow_node_id = Some(lcd.overflow_node_id);
            style.overflow_node_name = Some(lcd.overflow_node_name);
        }
    }

    if !has_extended_auto_layout && !is_widget_list {
        let plugin_data = node.shared_plugin_data.get("designcompose");
        if let Some(plugin_data) = plugin_data {
            if let Some(auto_layout_str) = plugin_data.get("vsw-extended-auto-layout") {
                let extended_auto_layout: Option<ExtendedAutoLayout> =
                    serde_json::from_str(auto_layout_str.as_str()).ok();
                if let Some(extended_auto_layout) = extended_auto_layout {
                    is_widget_list = !extended_auto_layout.layout.is_grid();
                    size_policy = extended_auto_layout.auto_layout_data.size_policy;
                }
            }
        }
    }

    // If this node is a widget or a child of a widget with a horizontal or vertical layout,
    // we need to set our size depending on whether the size policy is hug or fixed.
    if is_widget_list {
        if size_policy == SizePolicy::Hug {
            style.position_type = PositionType::Relative;
            style.width = Dimension::Auto;
            style.height = Dimension::Auto;
        } else {
            style.position_type = PositionType::Absolute;
        }
    }

    // Blend mode is common to all elements.
    style.blend_mode = convert_blend_mode(node.blend_mode);

    // Pull out the visual style for "frame-ish" nodes.
    if let Some(frame) = node.frame() {
        style.overflow = if frame.clips_content { Overflow::Hidden } else { Overflow::Visible };
        // Don't overwrite scroll behavior if it was already set from grid layout
        if scroll_info.overflow == OverflowDirection::None {
            scroll_info.overflow = frame.overflow_direction;
        }
    } else if let NodeData::Text {
        characters,
        style: text_style,
        character_style_overrides,
        style_override_table,
        ..
    } = &node.data
    {
        if let Some(text_fill) = node.fills.iter().filter(|paint| paint.visible).last() {
            style.text_color = compute_background(text_fill, images, &node.name);
        }
        style.font_size = text_style.font_size;
        style.font_weight = FontWeight(text_style.font_weight);
        if text_style.italic {
            style.font_style = FontStyle::Italic;
        }
        style.font_family = text_style.font_family.clone();
        match text_style.text_align_horizontal {
            TextAlignHorizontal::Center => style.text_align = TextAlign::Center,
            TextAlignHorizontal::Left => style.text_align = TextAlign::Left,
            TextAlignHorizontal::Right => style.text_align = TextAlign::Right,
            TextAlignHorizontal::Justified => style.text_align = TextAlign::Center, // XXX
        }
        style.text_align_vertical = match text_style.text_align_vertical {
            TextAlignVertical::Center => crate::toolkit_style::TextAlignVertical::Center,
            TextAlignVertical::Top => crate::toolkit_style::TextAlignVertical::Top,
            TextAlignVertical::Bottom => crate::toolkit_style::TextAlignVertical::Bottom,
        };
        style.line_height = match text_style.line_height_unit {
            // It's a percentage of the font size.
            LineHeightUnit::FontSizePercentage => LineHeight::Pixels(
                text_style.font_size * text_style.line_height_percent_font_size / 100.0,
            ),
            // It's a percentage of the intrinsic line height of the font itself.
            LineHeightUnit::IntrinsicPercentage => {
                LineHeight::Percent(text_style.line_height_percent_font_size / 100.0)
            }
            // It's an absolute value in pixels.
            LineHeightUnit::Pixels => LineHeight::Pixels(text_style.line_height_px),
        };
        style.opacity = if node.opacity < 1.0 { Some(node.opacity) } else { None };
        let convert_opentype_flags = |flags: &HashMap<String, u32>| -> Vec<FontFeature> {
            let mut font_features = Vec::new();
            for (flag, value) in flags {
                let flag_ascii = flag.to_ascii_lowercase();
                let flag_bytes = flag_ascii.as_bytes();
                if flag_bytes.len() == 4 {
                    let tag = [flag_bytes[0], flag_bytes[1], flag_bytes[2], flag_bytes[3]];
                    font_features.push(FontFeature { tag: tag, enabled: *value == 1 });
                } else {
                    println!("Unsupported OpenType flag: {}", flag)
                }
            }
            font_features
        };
        style.font_features = convert_opentype_flags(&text_style.opentype_flags);

        // We can map the "drop shadow" effect to our text shadow functionality, but we can't
        // map the other effects. (We either implement them on Rects, or we don't implement in
        // a compatible way).
        for effect in &node.effects {
            if !effect.visible {
                continue;
            }
            match effect.effect_type {
                EffectType::DropShadow => {
                    style.text_shadow = Some(crate::toolkit_style::TextShadow {
                        blur_radius: effect.radius,
                        color: crate::Color::from_f32s(
                            effect.color.r,
                            effect.color.g,
                            effect.color.b,
                            effect.color.a,
                        ),
                        offset: (effect.offset.x(), effect.offset.y()),
                    });
                }
                _ => {}
            }
        }

        // Apply any extra text properties specified by our plugin.
        if let Some(extended_text_layout) = extended_text_layout {
            if extended_text_layout.line_count > 0 {
                style.line_count = Some(extended_text_layout.line_count as _);
            }
            style.text_overflow = if extended_text_layout.ellipsize {
                TextOverflow::Ellipsis
            } else {
                TextOverflow::Clip
            };
        }

        // The text style also contains some layout information. We previously exposed
        // auto-width text in our plugin.
        match text_style.text_auto_resize {
            TextAutoResize::None => {}
            TextAutoResize::Height => {
                style.height = Dimension::Undefined;
            }
            TextAutoResize::WidthAndHeight => {
                style.min_width = Dimension::Undefined;
                style.width = Dimension::Undefined;
                style.height = Dimension::Undefined;
            }
        }

        if character_style_overrides.is_empty() {
            // No character customization, so this is just a plain styled text object
            return View::new_text(
                &node.id,
                &node.name,
                style,
                component_info,
                reactions,
                characters,
                node.absolute_bounding_box,
            );
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
                if current_run.len() == 0 {
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
                        style.text_color.clone()
                    }
                } else {
                    style.text_color.clone()
                };
                runs.push(StyledTextRun {
                    text: current_run.clone(),
                    style: TextStyle {
                        text_color,
                        font_size: sub_style.font_size.unwrap_or(style.font_size),
                        font_family: sub_style.font_family.clone(),
                        font_weight: crate::toolkit_font_style::FontWeight(
                            sub_style.font_weight.unwrap_or(style.font_weight.0),
                        ),
                        font_style: crate::toolkit_font_style::FontStyle::Normal, //sub_style.font_style.unwrap_or(style.font_style),
                        font_stretch: style.font_stretch, // Not in SubTypeStyle.
                        letter_spacing: sub_style.letter_spacing.unwrap_or(1.0), // no letter_spacing on ViewStyle.
                        line_height: style.line_height,
                        font_features: convert_opentype_flags(
                            &sub_style
                                .opentype_flags
                                .clone()
                                .unwrap_or(text_style.opentype_flags.clone()),
                        ),
                    },
                });
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
                if style_id != last_style && current_run.len() > 0 {
                    // Flush!
                    flush(&mut current_run, &mut last_style);
                    current_run = g.to_string();
                } else {
                    current_run = current_run + g;
                }
                last_style = style_id;
            }
            flush(&mut current_run, &mut last_style);

            return View::new_styled_text(
                &node.id,
                &node.name,
                style,
                component_info,
                reactions,
                runs,
                node.absolute_bounding_box,
            );
        }
    }

    for fill in node.fills.iter().filter(|paint| paint.visible) {
        style.background.push(compute_background(fill, images, &node.name));
    }

    for stroke in node.strokes.iter().filter(|paint| paint.visible) {
        style.stroke.strokes.push(compute_background(stroke, images, &node.name));
    }

    // Copy out the common styles from frames and supported content.
    style.opacity = if node.opacity < 1.0 { Some(node.opacity) } else { None };
    if let Some(stroke_weight) = node.stroke_weight {
        style.stroke.stroke_weight = stroke_weight;
    }
    style.stroke.stroke_align = match node.stroke_align {
        Some(StrokeAlign::Inside) => crate::toolkit_style::StrokeAlign::Inside,
        Some(StrokeAlign::Center) => crate::toolkit_style::StrokeAlign::Center,
        Some(StrokeAlign::Outside) | None => crate::toolkit_style::StrokeAlign::Outside,
    };

    // Convert any path data we have; we'll use it for non-frame types.
    let fill_paths = if let Some(fills) = &node.fill_geometry {
        fills.iter().map(|figma_path| parse_path(figma_path)).flatten().collect()
    } else {
        Vec::new()
    };

    // Normally the client will compute stroke paths itself, because Figma returns incorrect
    // stroke geometry for shapes with an area (e.g.: not lines) with Outside or Inside
    // stroke treatment. However, when a shape has no area (e.g.: it is a line), then the
    // fill geometry will be empty so we *have* to use the stroke geometry.
    let stroke_paths = if let Some(strokes) = &node.stroke_geometry {
        strokes.iter().map(|figma_path| parse_path(figma_path)).flatten().collect()
    } else {
        Vec::new()
    };

    let (corner_radius, has_corner_radius) = if let Some(border_radii) = node.rectangle_corner_radii
    {
        (
            [border_radii[0], border_radii[1], border_radii[2], border_radii[3]],
            border_radii[0] > 0.0
                || border_radii[1] > 0.0
                || border_radii[2] > 0.0
                || border_radii[3] > 0.0,
        )
    } else if let Some(border_radius) = node.corner_radius {
        ([border_radius, border_radius, border_radius, border_radius], border_radius > 0.0)
    } else {
        ([0.0, 0.0, 0.0, 0.0], false)
    };

    // Figure out the ViewShape from the node type.
    let view_shape = match node.data {
        NodeData::BooleanOperation { .. }
        | NodeData::Ellipse { .. }
        | NodeData::Line { .. }
        | NodeData::Rectangle { .. }
        | NodeData::RegularPolygon { .. }
        | NodeData::Star { .. }
        | NodeData::Vector { .. } => ViewShape::Path { path: fill_paths, stroke: stroke_paths },
        _ => {
            if has_corner_radius {
                ViewShape::RoundRect {
                    corner_radius,
                    corner_smoothing: 0.0, // Not in Figma REST API
                }
            } else {
                ViewShape::Rect
            }
        }
    };

    for effect in &node.effects {
        if !effect.visible {
            continue;
        }
        match effect.effect_type {
            EffectType::DropShadow => {
                style.box_shadow.push(crate::toolkit_style::BoxShadow::Outset {
                    blur_radius: effect.radius,
                    spread_radius: effect.spread,
                    color: crate::Color::from_f32s(
                        effect.color.r,
                        effect.color.g,
                        effect.color.b,
                        effect.color.a,
                    ),
                    offset: (effect.offset.x(), effect.offset.y()),
                    shadow_box: ShadowBox::StrokeBox,
                });
            }
            EffectType::InnerShadow => {
                style.box_shadow.push(crate::toolkit_style::BoxShadow::Inset {
                    blur_radius: effect.radius,
                    spread_radius: effect.spread,
                    color: crate::Color::from_f32s(
                        effect.color.r,
                        effect.color.g,
                        effect.color.b,
                        effect.color.a,
                    ),
                    offset: (effect.offset.x(), effect.offset.y()),
                    shadow_box: ShadowBox::StrokeBox,
                });
            }
            EffectType::LayerBlur => {
                style.filter.push(FilterOp::Blur(effect.radius / 2.0));
            }
            EffectType::BackgroundBlur => {
                style.backdrop_filter.push(FilterOp::Blur(effect.radius / 2.0));
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
        node.absolute_bounding_box,
    );

    // Iterate over our visible children, but not vectors because they always
    // present their childrens content themselves (e.g.: they are boolean products
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
                ));
            }
        }
    }
    view
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
    component: &Node,
    component_map: &HashMap<String, Component>,
    component_set_map: &HashMap<String, ComponentSet>,
    component_context: &mut ComponentContext,
    image_context: &mut ImageContext,
) -> View {
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

fn parse_path(path: &crate::figma_schema::Path) -> Option<vector_schema::Path> {
    let mut output = vector_schema::Path::new();
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
    output.winding_rule(path.winding_rule);
    Some(output)
}
