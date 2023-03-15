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

use serde::{Deserialize, Serialize};
use std::collections::HashMap;

use crate::{
    color::Color,
    toolkit_font_style::{FontStretch, FontStyle, FontWeight},
    toolkit_layout_style::{
        AlignContent, AlignItems, AlignSelf, Dimension, Display, FlexDirection, FlexWrap,
        JustifyContent, Number, Overflow, PositionType, Rect,
    },
};

pub trait Dimensionable {
    fn dimension(self) -> Dimension;
}
impl Dimensionable for Dimension {
    fn dimension(self) -> Dimension {
        self
    }
}
impl Dimensionable for f32 {
    fn dimension(self) -> Dimension {
        Dimension::Points(self)
    }
}
impl Dimensionable for i32 {
    fn dimension(self) -> Dimension {
        Dimension::Points(self as f32)
    }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize, Default)]
pub enum ScaleMode {
    Fill,
    Fit,
    #[default]
    Tile,
    Stretch,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub enum Background {
    None,
    Solid(Color),
    LinearGradient {
        start_x: f32,
        start_y: f32,
        end_x: f32,
        end_y: f32,
        color_stops: Vec<(f32, Color)>,
    },
    AngularGradient {
        center_x: f32,
        center_y: f32,
        angle: f32,
        scale: f32,
        color_stops: Vec<(f32, Color)>,
    },
    RadialGradient {
        center_x: f32,
        center_y: f32,
        angle: f32,
        radius: (f32, f32),
        color_stops: Vec<(f32, Color)>,
    },
    // DiamondGradient support possibly in the future.
    Image {
        key: Option<crate::image_context::ImageKey>,
        filters: Vec<FilterOp>,
        transform: Option<AffineTransform>,
        scale_mode: ScaleMode,
        opacity: f32,
    },
    Clear, // Clear all the pixels underneath, used for hole-punch compositing.
}
impl Default for Background {
    fn default() -> Self {
        Background::None
    }
}
impl Background {
    pub fn is_some(&self) -> bool {
        match self {
            Background::None => false,
            _ => true,
        }
    }
    pub fn from_image_key(key: crate::image_context::ImageKey) -> Background {
        Background::Image {
            key: Some(key),
            filters: Vec::new(),
            transform: None,
            scale_mode: ScaleMode::Tile,
            opacity: 1.0,
        }
    }
}

/// This structure represents an OpenType feature, for example "tnum" controls tabular numbers
/// in fonts that support the feature.
#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct FontFeature {
    pub tag: [u8; 4],
    pub enabled: bool,
}
impl FontFeature {
    pub fn new(tag: &[u8; 4]) -> Self {
        FontFeature {
            tag: *tag,
            enabled: true,
        }
    }
}

// These are the style properties that apply to text, so we can use them on subsections of
// a longer string. We then assume that every style transition is a potential line break (and
// also run the linebreaking algorithm on the content of every style for the normal case where
// we need to break text that's all in one style).
#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct TextStyle {
    pub text_color: Background, // also text shadow?
    pub font_size: f32,
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
            text_color: Background::Solid(Color::BLACK),
            font_size: 18.0,
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
        StyledTextRun {
            text: label.to_string(),
            style: Default::default(),
        }
    }
    pub fn bold(self) -> Self {
        StyledTextRun {
            style: TextStyle {
                font_weight: FontWeight::BOLD,
                ..self.style
            },
            text: self.text,
        }
    }
    pub fn italic(self) -> Self {
        StyledTextRun {
            style: TextStyle {
                font_style: FontStyle::Italic,
                ..self.style
            },
            text: self.text,
        }
    }
    pub fn size(self, size: f32) -> Self {
        StyledTextRun {
            style: TextStyle {
                font_size: size,
                ..self.style
            },
            text: self.text,
        }
    }
    pub fn fill(self, text_color: Background) -> Self {
        StyledTextRun {
            style: TextStyle {
                text_color,
                ..self.style
            },
            text: self.text,
        }
    }
    pub fn family(self, family_name: impl ToString) -> Self {
        StyledTextRun {
            style: TextStyle {
                font_family: Some(family_name.to_string()),
                ..self.style
            },
            text: self.text,
        }
    }
    pub fn feature(self, feature: FontFeature) -> Self {
        let mut font_features = self.style.font_features;
        font_features.push(feature);
        StyledTextRun {
            style: TextStyle {
                font_features,
                ..self.style
            },
            text: self.text,
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
#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize)]
pub enum TextAlign {
    Left,
    Center,
    Right,
}
impl Default for TextAlign {
    fn default() -> TextAlign {
        TextAlign::Left
    }
}

/// Vertical text alignment. This value aligns the text vertically within its bounds.
#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize)]
pub enum TextAlignVertical {
    Top,
    Center,
    Bottom,
}
impl Default for TextAlignVertical {
    fn default() -> TextAlignVertical {
        TextAlignVertical::Top
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize)]
pub enum TextOverflow {
    Clip,
    Ellipsis,
}
impl Default for TextOverflow {
    fn default() -> TextOverflow {
        TextOverflow::Clip
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize)]
pub struct TextShadow {
    pub blur_radius: f32,
    pub color: Color,
    pub offset: (f32, f32),
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
/// A stroke is similar to a border, except that it does not change layout (a border insets
/// the children by the border size), it may be inset, centered or outset from the view bounds
/// and there can be multiple strokes on a view.
#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct Stroke {
    /// The alignment of strokes on this view.
    pub stroke_align: StrokeAlign,
    /// The thickness of strokes on this view (in pixels).
    pub stroke_weight: f32,
    /// The stroke colors/fills
    pub strokes: Vec<Background>,
}
impl Default for Stroke {
    fn default() -> Self {
        Stroke {
            stroke_align: StrokeAlign::Center,
            stroke_weight: 0.0,
            strokes: Vec::new(),
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize)]
pub enum BlendMode {
    ///Normal blends:
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
impl Default for BlendMode {
    fn default() -> Self {
        BlendMode::PassThrough
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize)]
pub enum PointerEvents {
    Auto,
    None,
    Inherit,
}
impl Default for PointerEvents {
    fn default() -> Self {
        PointerEvents::Inherit
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

#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub enum ItemSpacing {
    Fixed(i32),     // Fixed space between columns/rows
    Auto(i32, i32), // Min space between columns/rows, item width/height
}
impl Default for ItemSpacing {
    fn default() -> Self {
        ItemSpacing::Fixed(0)
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Deserialize, Serialize)]
pub enum GridLayoutType {
    FixedColumns,
    FixedRows,
    AutoColumns,
    AutoRows,
}
impl Default for GridLayoutType {
    fn default() -> Self {
        GridLayoutType::FixedColumns
    }
}

/// ToolkitStyle contains all of the styleable parameters accepted by the Rect and Text components.
///
#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct ViewStyle {
    pub border_radius: [f32; 4],
    pub text_color: Background,
    pub font_size: f32,
    pub font_family: Option<String>,
    pub font_weight: FontWeight,
    pub font_style: FontStyle,
    pub font_stretch: FontStretch,
    pub background: Vec<Background>,
    pub box_shadow: Vec<BoxShadow>,
    pub border_color: Color,
    pub stroke: Stroke,
    pub local_opacity: f32, // local opacity does not apply to children
    pub opacity: Option<f32>,
    pub transform: Option<LayoutTransform>,
    pub transition_size: Option<(f32, f32)>, // to add or subtract from the layout size without causing a re-layout.
    pub text_align: TextAlign,
    pub text_align_vertical: TextAlignVertical,
    pub text_overflow: TextOverflow,
    pub text_shadow: Option<TextShadow>,
    pub line_height: LineHeight,
    pub line_count: Option<usize>, // None means no limit on # lines.
    pub font_features: Vec<FontFeature>,
    pub filter: Vec<FilterOp>,
    pub backdrop_filter: Vec<FilterOp>,
    pub blend_mode: BlendMode,

    pub display_type: Display,
    pub position_type: PositionType,
    pub flex_direction: FlexDirection,
    pub flex_wrap: FlexWrap,
    pub grid_layout: Option<GridLayoutType>,
    pub grid_columns_rows: u32,
    pub grid_adaptive_min_size: u32,
    pub grid_span_content: Vec<GridSpan>,
    pub overflow: Overflow,
    pub max_children: Option<u32>,
    pub overflow_node_id: Option<String>,
    pub overflow_node_name: Option<String>,
    pub align_items: AlignItems,
    pub align_self: AlignSelf,
    pub align_content: AlignContent,
    pub justify_content: JustifyContent,
    pub top: Dimension,
    pub left: Dimension,
    pub bottom: Dimension,
    pub right: Dimension,
    pub margin: Rect<Dimension>,
    pub padding: Rect<Dimension>,
    pub border: Rect<Dimension>,
    pub item_spacing: ItemSpacing,
    pub cross_axis_item_spacing: f32,
    pub flex_grow: f32,
    pub flex_shrink: f32,
    pub flex_basis: Dimension,
    pub width: Dimension,
    pub height: Dimension,
    pub min_width: Dimension,
    pub max_width: Dimension,
    pub min_height: Dimension,
    pub max_height: Dimension,
    pub aspect_ratio: Number,
    pub pointer_events: PointerEvents,
}
impl Default for ViewStyle {
    fn default() -> ViewStyle {
        ViewStyle {
            border_radius: [0.0, 0.0, 0.0, 0.0],
            text_color: Background::Solid(Color::from_u8s(0, 0, 0, 255)),
            font_size: 18.0,
            font_family: None,
            font_weight: FontWeight::NORMAL,
            font_style: FontStyle::Normal,
            font_stretch: FontStretch::NORMAL,
            background: Vec::new(),
            box_shadow: Vec::new(),
            border_color: Color::from_u8s(0, 0, 0, 0),
            stroke: Stroke::default(),
            local_opacity: 1.0,
            opacity: None,
            transform: None,
            transition_size: None,
            text_align: TextAlign::Left,
            text_align_vertical: TextAlignVertical::Top,
            text_overflow: TextOverflow::Clip,
            text_shadow: None,
            line_height: LineHeight::Percent(1.0),
            line_count: None,
            font_features: Vec::new(),
            filter: Vec::new(),
            backdrop_filter: Vec::new(),
            blend_mode: BlendMode::default(),
            display_type: Display::default(),
            position_type: PositionType::default(),
            flex_direction: FlexDirection::default(),
            flex_wrap: FlexWrap::default(),
            grid_layout: None,
            grid_columns_rows: 0,
            grid_adaptive_min_size: 1,
            grid_span_content: vec![],
            overflow: Overflow::default(),
            max_children: None,
            overflow_node_id: None,
            overflow_node_name: None,
            align_items: AlignItems::default(),
            align_self: AlignSelf::default(),
            align_content: AlignContent::default(),
            justify_content: JustifyContent::default(),
            top: Dimension::default(),
            left: Dimension::default(),
            bottom: Dimension::default(),
            right: Dimension::default(),
            margin: Rect::<Dimension>::default(),
            padding: Rect::<Dimension>::default(),
            border: Rect::<Dimension>::default(),
            item_spacing: ItemSpacing::default(),
            cross_axis_item_spacing: 0.0,
            flex_grow: 0.0,
            flex_shrink: 0.0,
            flex_basis: Dimension::default(),
            width: Dimension::default(),
            height: Dimension::default(),
            min_width: Dimension::default(),
            max_width: Dimension::default(),
            min_height: Dimension::default(),
            max_height: Dimension::default(),
            aspect_ratio: Number::default(),
            pointer_events: PointerEvents::default(),
        }
    }
}
impl ViewStyle {
    /// Compute the difference between this style and the given style, returning a style
    /// that can be applied to this style to make it equal the given style using apply_non_default.
    pub fn difference(&self, other: &ViewStyle) -> ViewStyle {
        let mut delta = ViewStyle::default();
        if self.border_radius != other.border_radius {
            delta.border_radius = other.border_radius;
        }
        if self.text_color != other.text_color {
            delta.text_color = other.text_color.clone();
        }
        if self.font_size != other.font_size {
            delta.font_size = other.font_size;
        }
        if self.font_family != other.font_family {
            delta.font_family = other.font_family.clone();
        }
        if self.font_weight != other.font_weight {
            delta.font_weight = other.font_weight;
        }
        if self.font_style != other.font_style {
            delta.font_style = other.font_style;
        }
        if self.font_stretch != other.font_stretch {
            delta.font_stretch = other.font_stretch;
        }
        if self.background != other.background {
            delta.background = other.background.clone();
        }
        if self.box_shadow != other.box_shadow {
            delta.box_shadow = other.box_shadow.clone();
        }
        if self.border_color != other.border_color {
            delta.border_color = other.border_color;
        }
        if self.stroke != other.stroke {
            delta.stroke = other.stroke.clone();
        }
        if self.local_opacity != other.local_opacity {
            delta.local_opacity = other.local_opacity;
        }
        if self.opacity != other.opacity {
            delta.opacity = other.opacity;
        }
        if self.transform != other.transform {
            delta.transform = other.transform;
        }
        if self.transition_size != other.transition_size {
            delta.transition_size = other.transition_size;
        }
        if self.text_align != other.text_align {
            delta.text_align = other.text_align;
        }
        if self.text_align_vertical != other.text_align_vertical {
            delta.text_align_vertical = other.text_align_vertical;
        }
        if self.text_overflow != other.text_overflow {
            delta.text_overflow = other.text_overflow;
        }
        if self.text_shadow != other.text_shadow {
            delta.text_shadow = other.text_shadow;
        }
        if self.line_height != other.line_height {
            delta.line_height = other.line_height;
        }
        if self.line_count != other.line_count {
            delta.line_count = other.line_count;
        }
        if self.font_features != other.font_features {
            delta.font_features = other.font_features.clone();
        }
        if self.filter != other.filter {
            delta.filter = other.filter.clone();
        }
        if self.backdrop_filter != other.backdrop_filter {
            delta.backdrop_filter = other.backdrop_filter.clone();
        }
        if self.blend_mode != other.blend_mode {
            delta.blend_mode = other.blend_mode;
        }
        if self.display_type != other.display_type {
            delta.display_type = other.display_type;
        }
        if self.position_type != other.position_type {
            delta.position_type = other.position_type;
        }
        if self.flex_direction != other.flex_direction {
            delta.flex_direction = other.flex_direction;
        }
        if self.flex_wrap != other.flex_wrap {
            delta.flex_wrap = other.flex_wrap;
        }
        if self.grid_layout != other.grid_layout {
            delta.grid_layout = other.grid_layout;
        }
        if self.grid_columns_rows != other.grid_columns_rows {
            delta.grid_columns_rows = other.grid_columns_rows;
        }
        if self.grid_adaptive_min_size != other.grid_adaptive_min_size {
            delta.grid_adaptive_min_size = other.grid_adaptive_min_size;
        }
        if self.grid_span_content != other.grid_span_content {
            delta.grid_span_content = other.grid_span_content.clone();
        }
        if self.overflow != other.overflow {
            delta.overflow = other.overflow;
        }
        if self.max_children != other.max_children {
            delta.max_children = other.max_children;
        }
        if self.overflow_node_id != other.overflow_node_id {
            delta.overflow_node_id = other.overflow_node_id.clone();
        }
        if self.overflow_node_name != other.overflow_node_name {
            delta.overflow_node_name = other.overflow_node_name.clone();
        }
        if self.align_items != other.align_items {
            delta.align_items = other.align_items;
        }
        if self.align_content != other.align_content {
            delta.align_content = other.align_content;
        }
        if self.justify_content != other.justify_content {
            delta.justify_content = other.justify_content;
        }
        if self.top != other.top {
            delta.top = other.top;
        }
        if self.left != other.left {
            delta.left = other.left;
        }
        if self.bottom != other.bottom {
            delta.bottom = other.bottom;
        }
        if self.right != other.right {
            delta.right = other.right;
        }
        if self.margin != other.margin {
            delta.margin = other.margin;
        }
        if self.padding != other.padding {
            delta.padding = other.padding;
        }
        if self.border != other.border {
            delta.border = other.border;
        }
        if self.item_spacing != other.item_spacing {
            delta.item_spacing = other.item_spacing.clone();
        }
        if self.cross_axis_item_spacing != other.cross_axis_item_spacing {
            delta.cross_axis_item_spacing = other.cross_axis_item_spacing;
        }
        if self.flex_grow != other.flex_grow {
            delta.flex_grow = other.flex_grow;
        }
        if self.flex_shrink != other.flex_shrink {
            delta.flex_shrink = other.flex_shrink;
        }
        if self.flex_basis != other.flex_basis {
            delta.flex_basis = other.flex_basis;
        }
        if self.width != other.width {
            delta.width = other.width;
        }
        if self.height != other.height {
            delta.height = other.height;
        }
        if self.max_width != other.max_width {
            delta.max_width = other.max_width;
        }
        if self.max_height != other.max_height {
            delta.max_height = other.max_height;
        }
        if self.min_width != other.min_width {
            delta.min_width = other.min_width;
        }
        if self.min_height != other.min_height {
            delta.min_height = other.min_height;
        }
        if self.aspect_ratio != other.aspect_ratio {
            delta.aspect_ratio = other.aspect_ratio;
        }
        if self.pointer_events != other.pointer_events {
            delta.pointer_events = other.pointer_events;
        }
        delta
    }
    pub fn uniform(measurement: f32) -> Rect<Dimension> {
        Rect {
            start: Dimension::Points(measurement),
            end: Dimension::Points(measurement),
            top: Dimension::Points(measurement),
            bottom: Dimension::Points(measurement),
        }
    }
}
