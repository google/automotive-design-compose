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

use std::f32::consts::PI;

use crate::toolkit_layout_style::compute_layout;

use crate::toolkit_style::{
    Background, FilterOp, LayoutTransform
    ,
};

use crate::vector_schema;
use crate::{

    //ExtendedTextLayout
    figma_schema::{
        BlendMode

        , PaintData
        ,
    },
    image_context::ImageContext

    ,
};

//::{Taffy, Dimension, JustifyContent, Size, AvailableSpace, FlexDirection};

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
            .map(|tx| compute_layout::convert_transform(&tx).inverse().unwrap_or(LayoutTransform::identity()))
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

        Background::LinearGradient { start_x, start_y, end_x, end_y, color_stops: g_stops }
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

        Background::AngularGradient { center_x, center_y, angle, scale, color_stops: g_stops }
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

        Background::RadialGradient { center_x, center_y, radius, angle, color_stops: g_stops }
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

        Background::RadialGradient { center_x, center_y, radius, angle, color_stops: g_stops }
    } else {
        Background::None
    }
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
