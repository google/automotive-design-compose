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

use dc_bundle::definition::element::Color;
use dc_bundle::definition::element::FloatColor;
use serde::{Deserialize, Serialize};

// We use serde to decode Figma's JSON documents into Rust structures.
// These structures were derived from Figma's public API documentation, which has more information
// on what each field means: https://www.figma.com/developers/api#files
//
// We reorganize Figma's responses a bit to pull mostly common fields (like absolute_bounding_box)
// into common structures, otherwise we ought to be able to round-trip a response from Figma without
// changing it (although currently there are a few fields that we don't map).
#[derive(Deserialize, Serialize, Debug, Clone, Copy, PartialEq, Default)]
pub struct FigmaColor {
    pub r: f32,
    pub g: f32,
    pub b: f32,
    pub a: f32,
}

impl Into<Color> for &FigmaColor {
    fn into(self) -> Color {
        Color::from_f32s(self.r, self.g, self.b, self.a)
    }
}

impl Into<FloatColor> for &FigmaColor {
    fn into(self) -> FloatColor {
        FloatColor { r: self.r, g: self.g, b: self.b, a: self.a }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum ExportFormat {
    Jpg,
    Png,
    Svg,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum ConstraintType {
    Scale,
    Width,
    Height,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct Constraint {
    #[serde(rename = "type")]
    pub constraint_type: ConstraintType,
    pub value: f32,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct ExportSetting {
    pub suffix: String,
    pub format: ExportFormat,
    pub constraint: Constraint,
}

#[derive(Deserialize, Serialize, PartialEq, Debug, Clone, Copy)]
pub struct Rectangle {
    pub x: Option<f32>,
    pub y: Option<f32>,
    pub width: Option<f32>,
    pub height: Option<f32>,
}
impl Rectangle {
    pub fn x(&self) -> f32 {
        self.x.unwrap_or(0.0)
    }
    pub fn y(&self) -> f32 {
        self.y.unwrap_or(0.0)
    }
    pub fn width(&self) -> f32 {
        self.width.unwrap_or(0.0)
    }
    pub fn height(&self) -> f32 {
        self.height.unwrap_or(0.0)
    }
}

// Generate an implementation of Into that converts this Rectangle to the one in dc_bundle
impl Into<dc_bundle::definition::element::Rectangle> for &Rectangle {
    fn into(self) -> dc_bundle::definition::element::Rectangle {
        dc_bundle::definition::element::Rectangle {
            x: Some(self.x()),
            y: Some(self.y()),
            width: Some(self.width()),
            height: Some(self.height()),
        }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, Copy, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
#[derive(Default)]
pub enum BlendMode {
    #[default]
    PassThrough,
    Normal,
    Darken,
    Multiply,
    LinearBurn,
    ColorBurn,
    Lighten,
    Screen,
    LinearDodge,
    ColorDodge,
    Overlay,
    SoftLight,
    HardLight,
    Difference,
    Exclusion,
    Hue,
    Saturation,
    Color,
    Luminosity,
}

#[derive(Deserialize, Serialize, Debug, Clone, Copy)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum EasingType {
    EaseIn,
    EaseOut,
    EaseInAndOut,
    Linear,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq, Copy)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum VerticalLayoutConstraintValue {
    Top,
    Bottom,
    Center,
    TopBottom,
    Scale,
}
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq, Copy)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum HorizontalLayoutConstraintValue {
    Left,
    Right,
    Center,
    LeftRight,
    Scale,
}

#[derive(Deserialize, Serialize, Debug, Clone, Copy)]
pub struct LayoutConstraint {
    pub vertical: VerticalLayoutConstraintValue,
    pub horizontal: HorizontalLayoutConstraintValue,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum LayoutGridPattern {
    Columns,
    Rows,
    Grid,
}
#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum LayoutGridAlignment {
    Min,
    Stretch,
    Center,
}
#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct LayoutGrid {
    pub pattern: LayoutGridPattern,
    pub section_size: f32,
    pub visible: bool,
    pub color: FigmaColor,
    pub alignment: LayoutGridAlignment,
    pub gutter_size: f32,
    pub offset: f32,
    pub count: i32, // can be -1 ???
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum EffectType {
    InnerShadow,
    DropShadow,
    LayerBlur,
    BackgroundBlur,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct Effect {
    #[serde(rename = "type")]
    pub effect_type: EffectType,
    pub visible: bool,
    pub radius: f32,
    #[serde(default)]
    pub color: FigmaColor,
    #[serde(default)]
    pub blend_mode: BlendMode,
    #[serde(default)]
    pub offset: Vector,
    #[serde(default)]
    pub spread: f32,
    #[serde(rename = "boundVariables")]
    pub bound_variables: Option<BoundVariables>,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum HyperlinkType {
    Url,
    Node,
}
#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct Hyperlink {
    //#[serde(rename = "type")]
    //pub hyperlink_type: HyperlinkType,
    #[serde(default)]
    pub url: String,
    #[serde(default)]
    pub node_id: String, // XXX: This is "nodeID" in Figma; we might not be deserializing ok...
}

impl Into<dc_bundle::definition::element::Hyperlink> for Hyperlink {
    fn into(self) -> dc_bundle::definition::element::Hyperlink {
        dc_bundle::definition::element::Hyperlink { value: self.url }
    }
}

// XXX ColorStop, Transform
#[derive(Deserialize, Serialize, Debug, Clone, Default, PartialEq)]
pub struct Vector {
    pub x: Option<f32>,
    pub y: Option<f32>,
}
impl Vector {
    pub fn is_valid(&self) -> bool {
        self.x.is_some() && self.y.is_some()
    }
    pub fn x(&self) -> f32 {
        self.x.unwrap_or(0.0)
    }
    pub fn y(&self) -> f32 {
        self.y.unwrap_or(0.0)
    }
}

impl Into<dc_bundle::definition::element::Vector> for Vector {
    fn into(self) -> dc_bundle::definition::element::Vector {
        dc_bundle::definition::element::Vector { x: self.x(), y: self.y() }
    }
}
#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct Size {
    pub width: f32,
    pub height: f32,
}
// XXX: Transform is an array of two arrays of 3 numbers
//  [[a, b, c], [d, e, f]]
//#[derive (Deserialize, Serialize, Debug, Clone)]
pub type Transform = [[Option<f32>; 3]; 2];

#[derive(Clone, Copy, PartialEq, Debug, Serialize, Deserialize, Hash)]
#[serde(rename_all = "UPPERCASE")]
pub enum WindingRule {
    NonZero,
    EvenOdd,
    #[serde(other)]
    None,
}

impl Into<dc_bundle::definition::element::path::WindingRule> for WindingRule {
    fn into(self) -> dc_bundle::definition::element::path::WindingRule {
        match self {
            WindingRule::NonZero => dc_bundle::definition::element::path::WindingRule::NonZero,
            WindingRule::EvenOdd => dc_bundle::definition::element::path::WindingRule::EvenOdd,
            WindingRule::None => dc_bundle::definition::element::path::WindingRule::None,
        }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, Hash, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct Path {
    pub path: String,
    pub winding_rule: WindingRule,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct ImageFilters {
    pub exposure: Option<f32>,    //Status: Needs scaling work.
    pub contrast: Option<f32>,    //Status: Supported
    pub saturation: Option<f32>,  //Status: Supported
    pub temperature: Option<f32>, //horiontal axis, blue to amber
    pub tint: Option<f32>,        //vertical axis, green to magenta
    pub highlights: Option<f32>,  //adjust only lighter areas of image
    pub shadows: Option<f32>,     //adjust only darker areas of image
}

#[derive(Deserialize, Serialize, Debug, Clone)]
// NOT snake case
pub struct FrameOffset {
    pub node_id: String,
    pub node_offset: Vector,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct ColorStop {
    pub position: f32,
    pub color: FigmaColor,
    #[serde(rename = "boundVariables")]
    pub bound_variables: Option<BoundVariables>,
}

#[derive(Deserialize, Serialize, Debug, Clone, Copy)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
#[derive(Default)]
pub enum TextCase {
    #[default]
    Original,
    Upper,
    Lower,
    Title,
    SmallCaps,
    SmallCapsForced,
}

#[derive(Deserialize, Serialize, Debug, Clone, Copy)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
#[derive(Default)]
pub enum TextDecoration {
    #[default]
    None,
    Strikethrough,
    Underline,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
#[derive(Default)]
pub enum TextAutoResize {
    #[default]
    None,
    Height,
    WidthAndHeight,
    Truncate,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
#[derive(Default)]
pub enum TextTruncation {
    #[default]
    Disabled,
    Ending,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
#[derive(Default)]
pub enum TextAlignHorizontal {
    #[default]
    Left,
    Right,
    Center,
    Justified,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
#[derive(Default)]
pub enum TextAlignVertical {
    #[default]
    Top,
    Center,
    Bottom,
}

fn default_line_height_percent() -> f32 {
    100.0
}
fn default_paragraph_spacing() -> f32 {
    0.0
}
fn default_paragraph_indent() -> f32 {
    0.0
}
fn default_max_lines() -> i32 {
    -1
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum LineHeightUnit {
    Pixels,
    #[serde(rename = "FONT_SIZE_%")]
    FontSizePercentage,
    #[serde(rename = "INTRINSIC_%")]
    IntrinsicPercentage,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct TypeStyle {
    pub font_family: Option<String>,
    pub font_post_script_name: Option<String>,
    #[serde(default = "default_paragraph_spacing")]
    pub paragraph_spacing: f32,
    #[serde(default = "default_paragraph_indent")]
    pub paragraph_indent: f32,
    #[serde(default)]
    pub italic: bool,
    pub font_weight: f32,
    pub font_size: f32,
    #[serde(default)]
    pub text_case: TextCase,
    #[serde(default)]
    pub text_decoration: TextDecoration,
    #[serde(default)]
    pub text_auto_resize: TextAutoResize,
    #[serde(default)]
    pub text_truncation: TextTruncation,
    #[serde(default = "default_max_lines")]
    pub max_lines: i32,
    #[serde(default)]
    pub text_align_horizontal: TextAlignHorizontal,
    #[serde(default)]
    pub text_align_vertical: TextAlignVertical,
    pub letter_spacing: f32,
    #[serde(default)]
    pub fills: Vec<Paint>,
    pub hyperlink: Option<Hyperlink>,
    #[serde(default)]
    pub opentype_flags: HashMap<String, u32>,
    pub line_height_px: f32,
    #[serde(default = "default_line_height_percent")]
    pub line_height_percent: f32,
    #[serde(default = "default_line_height_percent")]
    pub line_height_percent_font_size: f32,
    pub line_height_unit: LineHeightUnit,
}

impl TypeStyle {
    pub fn to_sub_type_style(&self) -> SubTypeStyle {
        SubTypeStyle {
            font_family: self.font_family.clone(),
            italic: Some(self.italic),
            font_weight: Some(self.font_weight),
            font_size: Some(self.font_size),
            text_case: Some(self.text_case),
            text_decoration: Some(self.text_decoration),
            letter_spacing: Some(self.letter_spacing),
            fills: self.fills.clone(),
            hyperlink: self.hyperlink.clone(),
            opentype_flags: Some(self.opentype_flags.clone()),
        }
    }
}

// This type doesn't exist in the Figma docs, but the struct that they pack
// for character customizations has mostly optional fields, and we should use
// the parent TypeStyle where we don't have a value.
#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct SubTypeStyle {
    pub font_family: Option<String>,
    pub italic: Option<bool>,
    pub font_weight: Option<f32>,
    pub font_size: Option<f32>,
    pub text_case: Option<TextCase>,
    pub text_decoration: Option<TextDecoration>,
    pub letter_spacing: Option<f32>,
    #[serde(default)]
    pub fills: Vec<Paint>,
    pub hyperlink: Option<Hyperlink>,
    #[serde(default)]
    pub opentype_flags: Option<HashMap<String, u32>>,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum ScaleMode {
    Fill,
    Fit,
    Tile,
    Stretch,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum PaintType {
    Solid,
    GradientLinear,
    GradientRadial,
    GradientAngular,
    GradientDiamond,
    Image,
    Emoji,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct Gradient {
    #[serde(default, rename = "blendMode")]
    pub blend_mode: BlendMode,
    #[serde(rename = "gradientHandlePositions")]
    pub gradient_handle_positions: Vec<Vector>,
    #[serde(rename = "gradientStops")]
    pub gradient_stops: Vec<ColorStop>,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(tag = "type", rename_all = "SCREAMING_SNAKE_CASE")]
pub enum PaintData {
    Solid {
        color: FigmaColor,
        #[serde(rename = "boundVariables")]
        bound_variables: Option<BoundVariables>,
    },
    GradientLinear {
        #[serde(flatten)]
        gradient: Gradient,
    },
    GradientRadial {
        #[serde(flatten)]
        gradient: Gradient,
    },
    GradientAngular {
        #[serde(flatten)]
        gradient: Gradient,
    },
    GradientDiamond {
        #[serde(flatten)]
        gradient: Gradient,
    },
    Image {
        #[serde(rename = "scaleMode")]
        scale_mode: ScaleMode,
        #[serde(rename = "imageTransform", default)]
        image_transform: Option<Transform>, // only if scale_mode is STRETCH
        #[serde(rename = "scalingFactor", default)]
        scaling_factor: Option<f32>, // only if scale_mode is TILE
        #[serde(default)] // not present?
        rotation: f32,
        #[serde(rename = "imageRef")]
        image_ref: Option<String>, // sometimes this appears in the character type mapping table and is null
        #[serde(rename = "gifRef", default)]
        gif_ref: Option<String>,
        filters: Option<ImageFilters>,
    },
    Emoji,
}

fn default_opacity() -> f32 {
    1.0
}
fn default_visible() -> bool {
    true
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct Paint {
    #[serde(default = "default_visible")]
    pub visible: bool,
    #[serde(default = "default_opacity")]
    pub opacity: f32,
    #[serde(flatten)]
    pub data: PaintData,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct Component {
    pub key: String,
    pub name: String,
    pub description: String,
    #[serde(default)]
    pub component_set_id: String,
}

#[derive(Deserialize, Serialize, Debug, Clone, Default)]
#[serde(rename_all = "camelCase")]
pub struct ComponentSet {
    pub key: String,
    pub name: String,
    pub description: String,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum StyleType {
    Fill,
    Text,
    Effect,
    Grid,
}
#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct Style {
    pub key: String,
    pub name: String,
    pub description: String,
    pub style_type: StyleType,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
#[derive(Default)]
pub enum StrokeAlign {
    Inside,
    Outside,
    #[default]
    Center,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
#[derive(Default)]
pub enum LayoutMode {
    #[default]
    None,
    Horizontal,
    Vertical,
}

impl LayoutMode {
    pub fn is_none(&self) -> bool {
        match self {
            LayoutMode::None => true,
            _ => false,
        }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
#[derive(Default)]
pub enum LayoutSizingMode {
    Fixed,
    #[default]
    Auto,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
#[derive(Default)]
pub enum LayoutAlignItems {
    #[default]
    Min,
    Center,
    Max,
    SpaceBetween,
    Baseline, // Not supported
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum LayoutAlign {
    Inherit,
    Stretch,
    // These below are old?
    Min,
    Center,
    Max,
}

#[derive(Deserialize, Serialize, Debug, Clone, Copy, PartialEq, Eq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
#[derive(Default)]
pub enum OverflowDirection {
    #[default]
    None,
    HorizontalScrolling,
    VerticalScrolling,
    HorizontalAndVerticalScrolling,
}

impl OverflowDirection {
    pub fn scrolls_horizontal(&self) -> bool {
        match self {
            OverflowDirection::HorizontalScrolling => true,
            OverflowDirection::HorizontalAndVerticalScrolling => true,
            _ => false,
        }
    }
    pub fn scrolls_vertical(&self) -> bool {
        match self {
            OverflowDirection::VerticalScrolling => true,
            OverflowDirection::HorizontalAndVerticalScrolling => true,
            _ => false,
        }
    }
    pub fn scrolls(&self) -> bool {
        match self {
            OverflowDirection::None => false,
            _ => true,
        }
    }
}

impl Into<dc_bundle::definition::layout::OverflowDirection> for OverflowDirection {
    fn into(self) -> dc_bundle::definition::layout::OverflowDirection {
        match self {
            OverflowDirection::None => dc_bundle::definition::layout::OverflowDirection::None,
            OverflowDirection::HorizontalScrolling => {
                dc_bundle::definition::layout::OverflowDirection::HorizontalScrolling
            }
            OverflowDirection::VerticalScrolling => {
                dc_bundle::definition::layout::OverflowDirection::VerticalScrolling
            }
            OverflowDirection::HorizontalAndVerticalScrolling => {
                dc_bundle::definition::layout::OverflowDirection::HorizontalAndVerticalScrolling
            }
        }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum BooleanOperation {
    Union,
    Intersect,
    Subtract,
    Exclude,
}

fn default_locked() -> bool {
    false
}
fn default_effects() -> Vec<Effect> {
    Vec::new()
}
fn default_clips_content() -> bool {
    false
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq, Default, Copy)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum LayoutSizing {
    #[default]
    Fixed,
    Hug,
    Fill,
}
impl Into<dc_bundle::definition::layout::LayoutSizing> for LayoutSizing {
    fn into(self) -> dc_bundle::definition::layout::LayoutSizing {
        match self {
            LayoutSizing::Fixed => dc_bundle::definition::layout::LayoutSizing::Fixed,
            LayoutSizing::Hug => dc_bundle::definition::layout::LayoutSizing::Hug,
            LayoutSizing::Fill => dc_bundle::definition::layout::LayoutSizing::Fill,
        }
    }
}
impl LayoutSizing {
    pub fn into_proto_val(self) -> i32 {
        let proto_type: dc_bundle::definition::layout::LayoutSizing = self.into();
        proto_type.into()
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq, Default, Copy)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum LayoutPositioning {
    #[default]
    Auto,
    Absolute,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct VectorCommon {
    #[serde(default = "default_locked")]
    pub locked: bool,
    #[serde(default)]
    pub preserve_ratio: bool,
    pub layout_align: Option<LayoutAlign>,
    #[serde(default)]
    pub layout_grow: f32,
    pub relative_transform: Option<Transform>,
    pub constraints: LayoutConstraint,
    #[serde(default)]
    pub is_mask: bool,
    #[serde(default)]
    pub layout_sizing_horizontal: LayoutSizing,
    #[serde(default)]
    pub layout_sizing_vertical: LayoutSizing,
    #[serde(default)]
    pub layout_positioning: LayoutPositioning,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct FrameCommon {
    #[serde(default = "default_locked")]
    locked: bool,
    #[serde(default)]
    pub preserve_ratio: bool,
    pub constraints: LayoutConstraint,
    pub layout_align: Option<LayoutAlign>,
    #[serde(default)]
    pub layout_grow: f32,
    pub relative_transform: Option<Transform>,
    #[serde(default = "default_clips_content")]
    pub clips_content: bool,
    #[serde(default)]
    pub layout_mode: LayoutMode,
    #[serde(default)]
    pub primary_axis_sizing_mode: LayoutSizingMode, // FIXED, AUTO
    #[serde(default)]
    pub counter_axis_sizing_mode: LayoutSizingMode, // FIXED, AUTO
    #[serde(default)]
    pub layout_sizing_horizontal: LayoutSizing,
    #[serde(default)]
    pub layout_sizing_vertical: LayoutSizing,
    #[serde(default)]
    pub primary_axis_align_items: LayoutAlignItems, // MIN, CENTER, MAX, SPACE_BETWEEN
    #[serde(default)]
    pub counter_axis_align_items: LayoutAlignItems, // MIN, CENTER, MAX,
    #[serde(default)]
    pub padding_left: f32,
    #[serde(default)]
    pub padding_right: f32,
    #[serde(default)]
    pub padding_top: f32,
    #[serde(default)]
    pub padding_bottom: f32,
    #[serde(default)]
    pub horizontal_padding: f32,
    #[serde(default)]
    pub vertical_padding: f32,
    #[serde(default)]
    pub item_spacing: f32,
    #[serde(default)]
    layout_grids: Vec<LayoutGrid>,
    #[serde(default)]
    pub overflow_direction: OverflowDirection,
    #[serde(default)]
    pub is_mask: bool,
    #[serde(default)]
    is_mask_outline: bool,
    #[serde(default)]
    pub layout_positioning: LayoutPositioning,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ArcData {
    pub starting_angle: f32,
    pub ending_angle: f32,
    pub inner_radius: f32,
}

#[derive(Deserialize, Serialize, PartialEq, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum StrokeCap {
    None,
    Round,
    Square,
    LineArrow,
    TriangleArrow,
    CircleFilled,
    DiamondFilled, // Not supported
}

fn default_stroke_cap() -> StrokeCap {
    StrokeCap::None
}

impl Into<dc_bundle::definition::element::view_shape::StrokeCap> for StrokeCap {
    fn into(self) -> dc_bundle::definition::element::view_shape::StrokeCap {
        match self {
            StrokeCap::None => dc_bundle::definition::element::view_shape::StrokeCap::None,
            StrokeCap::Round => dc_bundle::definition::element::view_shape::StrokeCap::Round,
            StrokeCap::Square => dc_bundle::definition::element::view_shape::StrokeCap::Square,
            StrokeCap::LineArrow => {
                dc_bundle::definition::element::view_shape::StrokeCap::LineArrow
            }
            StrokeCap::TriangleArrow => {
                dc_bundle::definition::element::view_shape::StrokeCap::TriangleArrow
            }
            StrokeCap::CircleFilled => {
                dc_bundle::definition::element::view_shape::StrokeCap::CircleFilled
            }
            StrokeCap::DiamondFilled => {
                dc_bundle::definition::element::view_shape::StrokeCap::DiamondFilled
            }
        }
    }
}

impl StrokeCap {
    pub fn to_proto(&self) -> i32 {
        //Need to do in two steps because we need to convert to the proto-genned type and then to i32
        let a: dc_bundle::definition::element::view_shape::StrokeCap = self.clone().into();
        a.into()
    }
}

#[derive(Deserialize, Serialize, PartialEq, Debug, Clone, Copy)]
pub struct StrokeWeights {
    pub top: f32,
    pub right: f32,
    pub bottom: f32,
    pub left: f32,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(tag = "type", rename_all = "SCREAMING_SNAKE_CASE")]
pub enum NodeData {
    Document {},
    Canvas {
        background_color: Option<FigmaColor>,
        #[serde(rename = "prototypeStartNodeID")]
        prototype_start_node_id: Option<String>,
    },
    Group {
        #[serde(flatten)]
        frame: FrameCommon,
    },
    Frame {
        #[serde(flatten)]
        frame: FrameCommon,
    },
    Widget {},
    Text {
        #[serde(flatten)]
        vector: VectorCommon,
        characters: String,
        style: TypeStyle,
        #[serde(rename = "characterStyleOverrides")]
        character_style_overrides: Vec<usize>,
        #[serde(rename = "styleOverrideTable")]
        style_override_table: HashMap<String, SubTypeStyle>, // Figma docs says this is a number, but it is quoted as a string.
    },
    Vector {
        #[serde(flatten)]
        vector: VectorCommon,
    },
    Line {
        #[serde(flatten)]
        vector: VectorCommon,
    },
    RegularPolygon {
        #[serde(flatten)]
        vector: VectorCommon,
    },
    Ellipse {
        #[serde(flatten)]
        vector: VectorCommon,
        #[serde(rename = "arcData")]
        arc_data: ArcData,
    },
    Star {
        #[serde(flatten)]
        vector: VectorCommon,
    },
    BooleanOperation {
        #[serde(flatten)]
        vector: VectorCommon,
        #[serde(rename = "booleanOperation")]
        boolean_operation: BooleanOperation,
    },
    Rectangle {
        #[serde(flatten)]
        vector: VectorCommon,
    },
    Slice {
        size: Option<Vector>,
        #[serde(rename = "relativeTransform")]
        relative_transform: Option<Transform>,
    },
    Component {
        #[serde(flatten)]
        frame: FrameCommon,
    },
    ComponentSet {
        #[serde(flatten)]
        frame: FrameCommon,
    },
    Instance {
        #[serde(flatten)]
        frame: FrameCommon,
        #[serde(rename = "componentId")]
        component_id: String,
    },
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct Node {
    pub id: String,
    pub name: String,
    #[serde(default)]
    pub fills: Vec<Paint>,
    #[serde(default)]
    pub strokes: Vec<Paint>,
    #[serde(default)]
    pub children: Vec<Node>,
    #[serde(default = "default_visible")]
    pub visible: bool,
    pub blend_mode: Option<BlendMode>,
    pub stroke_weight: Option<f32>,
    pub individual_stroke_weights: Option<StrokeWeights>,
    pub stroke_align: Option<StrokeAlign>,
    pub corner_radius: Option<f32>,
    pub rectangle_corner_radii: Option<[f32; 4]>,
    #[serde(default = "default_opacity")]
    pub opacity: f32,
    #[serde(default = "default_effects")]
    pub effects: Vec<Effect>,
    pub export_settings: Option<Vec<ExportSetting>>,
    #[serde(default)]
    pub absolute_bounding_box: Option<Rectangle>,
    #[serde(default)]
    pub absolute_render_bounds: Option<Rectangle>,
    #[serde(default)]
    pub shared_plugin_data: HashMap<String, HashMap<String, String>>,
    #[serde(flatten)]
    pub data: NodeData,
    pub relative_transform: Option<Transform>,
    pub size: Option<Vector>,
    pub fill_geometry: Option<Vec<Path>>,
    pub stroke_geometry: Option<Vec<Path>>,
    #[serde(default = "default_stroke_cap")]
    pub stroke_cap: StrokeCap,
    pub bound_variables: Option<BoundVariables>,
    pub explicit_variable_modes: Option<HashMap<String, String>>,
    pub min_width: Option<f32>,
    pub min_height: Option<f32>,
    pub max_width: Option<f32>,
    pub max_height: Option<f32>,
}

impl Node {
    pub fn frame(&self) -> Option<&FrameCommon> {
        match &self.data {
            NodeData::Frame { frame, .. } => Some(frame),
            NodeData::Group { frame, .. } => Some(frame),
            NodeData::Component { frame, .. } => Some(frame),
            NodeData::ComponentSet { frame, .. } => Some(frame),
            NodeData::Instance { frame, .. } => Some(frame),
            _ => None,
        }
    }
    pub fn vector(&self) -> Option<&VectorCommon> {
        match &self.data {
            NodeData::Vector { vector, .. } => Some(vector),
            NodeData::Line { vector, .. } => Some(vector),
            NodeData::RegularPolygon { vector, .. } => Some(vector),
            NodeData::Ellipse { vector, .. } => Some(vector),
            NodeData::Star { vector, .. } => Some(vector),
            NodeData::BooleanOperation { vector, .. } => Some(vector),
            NodeData::Rectangle { vector, .. } => Some(vector),
            _ => None,
            // Text inherits all of the properties of a vector node, but we treat
            // it differently because we treat text differently from other kinds
            // of vectors in regular graphics.
            //NodeData::Text { vector, .. } => Some(vector),
        }
    }
    pub fn is_widget(&self) -> bool {
        match &self.data {
            NodeData::Widget {} => true,
            _ => false,
        }
    }
    pub fn is_text(&self) -> bool {
        match &self.data {
            NodeData::Text { .. } => true,
            _ => false,
        }
    }
    pub fn constraints(&self) -> Option<&LayoutConstraint> {
        if let Some(frame) = self.frame() {
            Some(&frame.constraints)
        } else if let Some(vector) = self.vector() {
            Some(&vector.constraints)
        } else if let NodeData::Text { vector, .. } = &self.data {
            Some(&vector.constraints)
        } else {
            None
        }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct FileResponse {
    pub components: HashMap<String, Component>,
    pub component_sets: HashMap<String, ComponentSet>,
    pub document: Node,
    pub version: String,
    pub last_modified: String,
    pub name: String,
    pub branches: Option<Vec<HashMap<String, Option<String>>>>,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct FileHeadResponse {
    pub version: String,
    pub last_modified: String,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct ImageResponse {
    pub err: Option<String>,
    // Map from Node ID to URL, or None/null if the image wasn't available.
    pub images: HashMap<String, Option<String>>,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct ImageFillResponse {
    pub meta: ImageResponse,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct ComponentKeyResponse {
    pub error: bool,
    pub status: i32,
    pub meta: ComponentKeyMeta,
}
impl ComponentKeyResponse {
    pub fn parent_id(&self) -> Option<String> {
        self.meta
            .containing_frame
            .containing_state_group
            .as_ref()
            .map(|key_state_group| key_state_group.node_id.clone())
    }
}

#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct ComponentKeyMeta {
    pub key: String,
    pub file_key: String,
    pub node_id: String,
    pub thumbnail_url: String,
    pub name: String,
    pub description: String,
    pub created_at: String,
    pub updated_at: String,
    pub containing_frame: ComponentKeyContainingFrame,
    pub user: ComponentKeyUser,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ComponentKeyContainingFrame {
    pub name: Option<String>,
    pub node_id: Option<String>,
    pub page_id: String,
    pub page_name: String,
    pub background_color: Option<String>,
    pub containing_state_group: Option<ComponentKeyStateGroup>,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ComponentKeyStateGroup {
    pub name: String,
    pub node_id: String,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct ComponentKeyUser {
    pub id: String,
    pub handle: String,
    pub img_url: String,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct NodesResponse {
    pub name: String,
    pub last_modified: String,
    pub thumbnail_url: String,
    pub version: String,
    pub role: String,
    pub nodes: HashMap<String, NodeResponseData>,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct NodeResponseData {
    pub document: Node,
    pub components: HashMap<String, Component>,
    pub schema_version: i32,
    pub styles: HashMap<String, SubTypeStyle>,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ProjectFilesResponse {
    pub name: String,
    pub files: Vec<HashMap<String, String>>,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ModeBinding {
    pub mode_id: String,
    pub name: String,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct VariableCollection {
    pub default_mode_id: String,
    pub id: String,
    pub name: String,
    pub remote: bool,
    pub modes: Vec<ModeBinding>,
    pub key: String,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum VariableType {
    Boolean,
    Float,
    String,
    Color,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct VariableAlias {
    pub r#type: String,
    pub id: String,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(untagged)]
pub enum VariableAliasOrList {
    Alias(VariableAlias),
    List(Vec<VariableAlias>),
    Map(HashMap<String, VariableAlias>),
}
impl VariableAliasOrList {
    fn get_name(&self) -> Option<String> {
        match self {
            VariableAliasOrList::Alias(alias) => {
                return Some(alias.id.clone());
            }
            VariableAliasOrList::List(list) => {
                let alias = list.first();
                if let Some(alias) = alias {
                    return Some(alias.id.clone());
                }
            }
            VariableAliasOrList::Map(_) => {
                panic!("We don't support Maps of Variables!");
            }
        }
        None
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct BoundVariables {
    #[serde(flatten)]
    variables: HashMap<String, VariableAliasOrList>,
}
impl BoundVariables {
    pub(crate) fn has_var(&self, var_name: &str) -> bool {
        self.variables.contains_key(var_name)
    }

    pub(crate) fn get_variable(&self, var_name: &str) -> Option<String> {
        let var = self.variables.get(var_name);
        if let Some(var) = var {
            return var.get_name();
        }
        None
    }

    pub(crate) fn get_var_from_hash(&self, hash_name: &str, var_name: &str) -> Option<String> {
        let var_hash = self.variables.get(hash_name);
        if let Some(var) = var_hash {
            if let VariableAliasOrList::Map(map) = var {
                let var = map.get(var_name);
                if let Some(var) = var {
                    return Some(var.id.clone());
                }
            }
        }
        None
    }
}

// We use the "untagged" serde attribute because the value of a variable is
// described in a hash where we don't know the format of the value based on the
// key. The value format is different depending on the type of variable and if
// it is an alias to another variable. For example, this is a string value
//
// "valuesByMode": {
//   "1:0": "Hello"
// }
//
// And this is an alias to another variable:
//
// "valuesByMode": {
//   "1:0": {
//     "type": "VARIABLE_ALIAS",
//     "id": "VariableID:1:234"
//   },
// }
//
#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(untagged)]
pub enum VariableValue {
    Boolean(bool),
    Float(f32),
    String(String),
    Color(FigmaColor),
    Alias(VariableAlias),
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct VariableCommon {
    pub id: String,
    pub name: String,
    pub remote: bool,
    pub key: String,
    pub variable_collection_id: String,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")] // Because "resolvedType" is in this format
#[serde(tag = "resolvedType")] // Maps to different enum values based on "resolvedType"
pub enum Variable {
    #[serde(rename_all = "camelCase")] // Because the members in each enum are in this format
    Boolean {
        #[serde(flatten)]
        common: VariableCommon,
        values_by_mode: HashMap<String, VariableValue>,
    },
    #[serde(rename_all = "camelCase")]
    Float {
        #[serde(flatten)]
        common: VariableCommon,
        values_by_mode: HashMap<String, VariableValue>,
    },
    #[serde(rename_all = "camelCase")]
    String {
        #[serde(flatten)]
        common: VariableCommon,
        values_by_mode: HashMap<String, VariableValue>,
    },
    #[serde(rename_all = "camelCase")]
    Color {
        #[serde(flatten)]
        common: VariableCommon,
        //#[serde(deserialize_with = "value_or_alias")]
        values_by_mode: HashMap<String, VariableValue>,
    },
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct VariablesMeta {
    pub variable_collections: HashMap<String, VariableCollection>,
    pub variables: HashMap<String, Variable>,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct VariablesResponse {
    pub error: bool,
    pub status: i32,
    pub meta: VariablesMeta,
}
