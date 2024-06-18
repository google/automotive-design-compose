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

use serde::{Deserialize, Serialize};
use layout::layout_style::LayoutStyle;
use crate::toolkit_schema::ColorOrVar;

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, Default)]
pub enum ScaleMode {
    Fill,
    Fit,
    #[default]
    Tile,
    Stretch,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, Default)]
pub enum Background {
    #[default]
    None,
    Solid(ColorOrVar),
    LinearGradient {
        start_x: f32,
        start_y: f32,
        end_x: f32,
        end_y: f32,
        color_stops: Vec<(f32, ColorOrVar)>,
    },
    AngularGradient {
        center_x: f32,
        center_y: f32,
        angle: f32,
        scale: f32,
        color_stops: Vec<(f32, ColorOrVar)>,
    },
    RadialGradient {
        center_x: f32,
        center_y: f32,
        angle: f32,
        radius: (f32, f32),
        color_stops: Vec<(f32, ColorOrVar)>,
    },
    // DiamondGradient support possibly in the future.
    Image {
        key: Option<figma_import::image_context::ImageKey>,
        filters: Vec<FilterOp>,
        transform: Option<AffineTransform>,
        scale_mode: ScaleMode,
        opacity: f32,
    },
    Clear, // Clear all the pixels underneath, used for hole-punch compositing.
}

impl Background {
    pub fn is_some(&self) -> bool {
        match self {
            Background::None => false,
            _ => true,
        }
    }
    pub fn from_image_key(key: figma_import::image_context::ImageKey) -> Background {
        Background::Image {
            key: Some(key),
            filters: Vec::new(),
            transform: None,
            scale_mode: ScaleMode::Tile,
            opacity: 1.0,
        }
    }
}

/// Shadows can be applied to the border box, or the stroke box.
///
/// The border box is the box outside of the border for Outset shadows, and the box inside of the border for
/// inset shadows.
///
/// The stroke box is always the the outer edge of any stroke defined, or the edge of the view (ignoring the
/// border) if no strokes are present on the view.
#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize)]
pub enum ShadowBox {
    /// The shadow applies to the border box, either the inside of the border for inset shadows, or the outside
    /// of the border for outset shadows.
    BorderBox,
    /// The shadow applies to the stroke box. This is the outer edge of the stroke for both inset and ouset shadows.
    StrokeBox,
}

impl Default for ShadowBox {
    fn default() -> Self {
        Self::BorderBox
    }
}

/// BoxShadow defines a CSS-style box shadow, either outset or inset.
#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize)]
pub enum BoxShadow {
    Outset {
        blur_radius: f32,
        spread_radius: f32,
        color: Color,
        offset: (f32, f32),
        shadow_box: ShadowBox,
    },
    Inset {
        blur_radius: f32,
        spread_radius: f32,
        color: Color,
        offset: (f32, f32),
        shadow_box: ShadowBox,
    },
}

impl BoxShadow {
    /// Create an outset box shadow.
    pub fn outset(
        blur_radius: f32,
        spread_radius: f32,
        color: Color,
        offset: (f32, f32),
    ) -> BoxShadow {
        BoxShadow::Outset {
            blur_radius,
            spread_radius,
            color,
            offset,
            shadow_box: ShadowBox::BorderBox,
        }
    }
    /// Create an inset shadow.
    pub fn inset(
        blur_radius: f32,
        spread_radius: f32,
        color: Color,
        offset: (f32, f32),
    ) -> BoxShadow {
        BoxShadow::Inset {
            blur_radius,
            spread_radius,
            color,
            offset,
            shadow_box: ShadowBox::BorderBox,
        }
    }
}

/// Filters -- these can be applied to a view and its children (via the "filter" style),
/// or to the elements behind a view (via the "backdrop_filter" style).
#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize)]
pub enum FilterOp {
    /// Gaussian blur, radius in px.
    Blur(f32),
    //Saturation(f32),
    Contrast(f32),
    Grayscale(f32),
    Brightness(f32),
}

/// Horizontal text alignment. This value aligns the text within its bounds.
#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize, Default)]
pub enum TextAlign {
    #[default]
    Left,
    Center,
    Right,
}

/// Vertical text alignment. This value aligns the text vertically within its bounds.
#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize, Default)]
pub enum TextAlignVertical {
    #[default]
    Top,
    Center,
    Bottom,
}

#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize, Default)]
pub enum TextOverflow {
    #[default]
    Clip,
    Ellipsis,
}

/// How is a stroke aligned to its containing box?
#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize)]
pub enum StrokeAlign {
    /// The stroke is entirely within the containing view. The stroke's outer edge matches the
    /// outer edge of the containing view.
    Inside,
    /// The stroke is centered on the edge of the containing view, and extends into the view
    /// on the inside, and out of the view on the outside.
    Center,
    /// The stroke is entirely outside of the view. The stroke's inner edge is the outer edge
    /// of the containing view.
    Outside,
}

/// Stroke weight is either a uniform value for all sides, or individual
/// weights for each side.
#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize)]
pub enum StrokeWeight {
    /// One weight is used for all sides.
    Uniform(f32),
    /// Individual weights for each side (typically only applied on boxes).
    Individual { top: f32, right: f32, bottom: f32, left: f32 },
}

#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize, Default)]
pub enum PointerEvents {
    Auto,
    None,
    #[default]
    Inherit,
}

#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize, Default)]
pub enum GridLayoutType {
    #[default]
    FixedColumns,
    FixedRows,
    AutoColumns,
    AutoRows,
    Horizontal,
    Vertical,
}

#[derive(Serialize, Deserialize, Clone, Debug, Default, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct RotationMeterData {
    pub enabled: bool,
    pub start: f32,
    pub end: f32,
    pub discrete: bool,
    pub discrete_value: f32,
}

#[derive(Serialize, Deserialize, Clone, Debug, Default, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct ArcMeterData {
    pub enabled: bool,
    pub start: f32,
    pub end: f32,
    pub discrete: bool,
    pub discrete_value: f32,
    pub corner_radius: f32,
}

#[derive(Serialize, Deserialize, Clone, Debug, Default, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct ProgressBarMeterData {
    pub enabled: bool,
    pub discrete: bool,
    pub discrete_value: f32,
    #[serde(default)]
    pub vertical: bool,
    #[serde(default)]
    pub end_x: f32,
    #[serde(default)]
    pub end_y: f32,
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

#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize, Default)]
pub enum BlendMode {
    ///Normal blends:
    #[default]
    PassThrough,
    ///(only applicable to objects with children)
    Normal,

    ///Darken:
    Darken,
    Multiply,
    LinearBurn,
    ColorBurn,

    ///Lighten:
    Lighten,
    Screen,
    LinearDodge,
    ColorDodge,

    ///Contrast:
    Overlay,
    SoftLight,
    HardLight,

    ///Inversion:
    Difference,
    Exclusion,

    ///Component:
    Hue,
    Saturation,
    Color,
    Luminosity,
}

#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize)]
pub enum LineHeight {
    Pixels(f32),
    Percent(f32),
}

impl Default for LineHeight {
    fn default() -> Self {
        LineHeight::Percent(1.0)
    }
}

#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub enum MeterData {
    ArcData(ArcMeterData),
    RotationData(RotationMeterData),
    ProgressBarData(ProgressBarMeterData),
    ProgressMarkerData(ProgressMarkerMeterData),
}
