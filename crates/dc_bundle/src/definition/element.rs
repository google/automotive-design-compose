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

use protobuf::well_known_types::empty::Empty;
use protobuf::MessageField;

use crate::background::{background, Background};
use crate::color::{Color, FloatColor};
use crate::font::{FontFeature, FontStretch, FontStyle, FontWeight, TextDecoration};
use crate::geometry::dimension_proto::Dimension;
use crate::geometry::{DimensionProto, DimensionRect};
use crate::path::path::WindingRule;
use crate::path::Path;
use crate::variable::num_or_var::NumOrVarType;
use crate::variable::{color_or_var, ColorOrVar, NumOrVar};
use crate::view_shape::{view_shape, ViewShape};
use crate::Error;
use std::fmt;
use std::fmt::{Debug, Display, Formatter};
use std::hash::{Hash, Hasher};

impl Display for FontStyle {
    fn fmt(&self, f: &mut Formatter) -> fmt::Result {
        Debug::fmt(self, f)
    }
}
impl FontFeature {
    pub fn new_with_tag(tag: String) -> Self {
        FontFeature { tag, enabled: true, ..Default::default() }
    }
}

impl Display for TextDecoration {
    fn fmt(&self, f: &mut Formatter) -> fmt::Result {
        Debug::fmt(self, f)
    }
}

/// Compare two finite f32 values
/// This is not meant for situations dealing with NAN and INFINITY
#[inline]
fn f32_eq(a: &f32, b: &f32) -> bool {
    assert!(a.is_finite());
    assert!(b.is_finite());
    (a - b).abs() < f32::EPSILON
}

impl Color {
    pub fn from_u8s(r: u8, g: u8, b: u8, a: u8) -> Color {
        Color { r: r as u32, g: g as u32, b: b as u32, a: a as u32, ..Default::default() }
    }
    pub fn from_u8_tuple(color: (u8, u8, u8, u8)) -> Color {
        Color {
            r: color.0 as u32,
            g: color.1 as u32,
            b: color.2 as u32,
            a: color.3 as u32,
            ..Default::default()
        }
    }
    pub fn from_f32s(r: f32, g: f32, b: f32, a: f32) -> Color {
        let tou32 = |c| (c * 255.0) as u32;
        Color { r: tou32(r), g: tou32(g), b: tou32(b), a: tou32(a), ..Default::default() }
    }
    /// 0xAARRGGBB
    pub fn from_u32(c: u32) -> Color {
        Color {
            r: ((c & 0x00FF_0000u32) >> 16) as u32,
            g: ((c & 0x0000_FF00u32) >> 8) as u32,
            b: (c & 0x0000_00FFu32) as u32,
            a: ((c & 0xFF00_0000u32) >> 24) as u32,
            ..Default::default()
        }
    }
    pub fn from_f32_tuple(color: (f32, f32, f32, f32)) -> Color {
        Color::from_f32s(color.0, color.1, color.2, color.3)
    }

    /// Returns the H,S,V (Hue, Saturation, Value) representation
    /// of the (Red, Green, Blue) color argument.
    ///
    /// # Arguments
    ///
    /// * `color` - a tuple of r,g,b values between 0.0 and 1.0
    ///
    pub fn hsv_from_u8(color: (u8, u8, u8)) -> (f32, f32, f32) {
        // After: https://math.stackexchange.com/questions/556341/rgb-to-hsv-color-conversion-algorithm
        let r = color.0;
        let g = color.1;
        let b = color.2;

        let r = r as f32 / 255.0f32;
        let g = g as f32 / 255.0f32;
        let b = b as f32 / 255.0f32;

        let maxc = f32::max(f32::max(r, g), b);
        let minc = f32::min(f32::min(r, g), b);
        let v = maxc;
        if f32_eq(&minc, &maxc) {
            return (0.0, 0.0, v);
        }
        let s = (maxc - minc) / maxc;
        let rc = (maxc - r) / (maxc - minc);
        let gc = (maxc - g) / (maxc - minc);
        let bc = (maxc - b) / (maxc - minc);
        let h = if f32_eq(&r, &maxc) {
            bc - gc
        } else if f32_eq(&g, &maxc) {
            2.0 + rc - bc
        } else {
            4.0 + gc - rc
        };
        let h = (h / 6.0) % 1.0;
        let mut h = h * 2.0 * std::f32::consts::PI;
        // make sure h is positive and within [0:2_PI)
        if h < 0.0 {
            h += 2.0 * std::f32::consts::PI;
        } else if h >= 2.0 * std::f32::consts::PI {
            h -= 2.0 * std::f32::consts::PI;
        }
        (h, s, v)
    }

    pub fn hsv_from_u32(color: (u32, u32, u32)) -> (f32, f32, f32) {
        Color::hsv_from_u8((color.0 as u8, color.1 as u8, color.2 as u8))
    }

    /// Returns the H,S,V (Hue, Saturation, Value) representation
    /// of color.
    ///
    pub fn as_hsv(&self) -> (f32, f32, f32) {
        Color::hsv_from_u32((self.r, self.g, self.b))
    }

    pub fn as_u8_tuple(&self) -> (u8, u8, u8, u8) {
        (self.r as u8, self.g as u8, self.b as u8, self.a as u8)
    }
    pub fn as_f32_tuple(&self) -> (f32, f32, f32, f32) {
        let tof32 = |c| c as f32 / 255.0;
        (tof32(self.r), tof32(self.g), tof32(self.b), tof32(self.a))
    }
    pub fn as_f32_array(&self) -> [f32; 4] {
        let c = self.as_f32_tuple();
        [c.0, c.1, c.2, c.3]
    }
    pub fn as_u32(&self) -> u32 {
        (self.r << 24) | (self.g << 16) | (self.b << 8) | self.a
    }

    pub fn set_red(&mut self, r: u8) {
        self.r = r as u32;
    }
    pub fn set_green(&mut self, g: u8) {
        self.g = g as u32;
    }
    pub fn set_blue(&mut self, b: u8) {
        self.b = b as u32;
    }
    pub fn set_alpha(&mut self, a: u8) {
        self.a = a as u32;
    }

    pub fn r(&self) -> u8 {
        self.r as u8
    }
    pub fn g(&self) -> u8 {
        self.g as u8
    }
    pub fn b(&self) -> u8 {
        self.b as u8
    }
    pub fn a(&self) -> u8 {
        self.a as u8
    }

    pub fn white() -> Color {
        Color { r: 255, g: 255, b: 255, a: 255, ..Default::default() }
    }
    pub fn black() -> Color {
        Color { r: 0, g: 0, b: 0, a: 255, ..Default::default() }
    }
    pub fn red() -> Color {
        Color { r: 255, g: 0, b: 0, a: 255, ..Default::default() }
    }
    pub fn green() -> Color {
        Color { r: 0, g: 255, b: 0, a: 255, ..Default::default() }
    }
    pub fn blue() -> Color {
        Color { r: 0, g: 0, b: 255, a: 255, ..Default::default() }
    }
    pub fn yellow() -> Color {
        Color { r: 255, g: 255, b: 0, a: 255, ..Default::default() }
    }
    pub fn magenta() -> Color {
        Color { r: 255, g: 0, b: 255, a: 255, ..Default::default() }
    }
    pub fn cyan() -> Color {
        Color { r: 0, g: 255, b: 255, a: 255, ..Default::default() }
    }
    pub fn gray() -> Color {
        Color { r: 128, g: 128, b: 128, a: 255, ..Default::default() }
    }
    pub fn hot_pink() -> Color {
        Color { r: 255, g: 105, b: 180, a: 255, ..Default::default() }
    }
}

impl Into<Color> for &FloatColor {
    fn into(self) -> Color {
        Color::from_f32s(self.r, self.g, self.b, self.a)
    }
}

impl DimensionProto {
    pub fn new_auto() -> MessageField<Self> {
        Some(DimensionProto { Dimension: Some(Dimension::Auto(().into())), ..Default::default() })
            .into()
    }
    pub fn new_points(value: f32) -> MessageField<Self> {
        Some(DimensionProto { Dimension: Some(Dimension::Points(value)), ..Default::default() })
            .into()
    }
    pub fn new_percent(value: f32) -> MessageField<Self> {
        Some(DimensionProto { Dimension: Some(Dimension::Percent(value)), ..Default::default() })
            .into()
    }
    pub fn new_undefined() -> MessageField<Self> {
        Some(DimensionProto {
            Dimension: Some(Dimension::Undefined(().into())),
            ..Default::default()
        })
        .into()
    }
}

pub trait DimensionExt {
    fn is_points(&self) -> Result<bool, Error>;
}
impl DimensionExt for Option<DimensionProto> {
    fn is_points(&self) -> Result<bool, Error> {
        match self {
            Some(DimensionProto { Dimension: Some(Dimension::Points(_)), .. }) => Ok(true),
            Some(_) => Ok(false), // Other Dimension variants are not Points
            None => Err(Error::MissingFieldError { field: "DimensionProto".to_string() }),
        }
    }
}

impl DimensionRect {
    pub fn new_with_default_value() -> Self {
        DimensionRect {
            start: DimensionProto::new_undefined(),
            end: DimensionProto::new_undefined(),
            top: DimensionProto::new_undefined(),
            bottom: DimensionProto::new_undefined(),
            ..Default::default()
        }
    }
    // Sets the value of start to the given DimensionView value
    pub fn set_start(&mut self, start: Dimension) {
        self.start = Some(DimensionProto { Dimension: Some(start), ..Default::default() }).into();
    }
    // Sets the value of end to the given DimensionView value
    pub fn set_end(&mut self, end: Dimension) {
        self.end = Some(DimensionProto { Dimension: Some(end), ..Default::default() }).into();
    }
    // Sets the value of top to the given DimensionView value
    pub fn set_top(&mut self, top: Dimension) {
        self.top = Some(DimensionProto { Dimension: Some(top), ..Default::default() }).into();
    }
    // Sets the value of bottom to the given DimensionView value
    pub fn set_bottom(&mut self, bottom: Dimension) {
        self.bottom = Some(DimensionProto { Dimension: Some(bottom), ..Default::default() }).into();
    }
}

// Define an extension trait
pub trait DimensionRectExt {
    fn set_start(&mut self, start: Dimension) -> Result<(), Error>;
    fn set_end(&mut self, end: Dimension) -> Result<(), Error>;
    fn set_top(&mut self, top: Dimension) -> Result<(), Error>;
    fn set_bottom(&mut self, bottom: Dimension) -> Result<(), Error>;
}

// Implement the extension trait for Option<DimensionRect>
impl DimensionRectExt for Option<DimensionRect> {
    fn set_start(&mut self, start: Dimension) -> Result<(), Error> {
        if let Some(rect) = self.as_mut() {
            rect.set_start(start);
            Ok(())
        } else {
            Err(Error::MissingFieldError { field: "DimensionRect->start".to_string() })
        }
    }

    fn set_end(&mut self, end: Dimension) -> Result<(), Error> {
        if let Some(rect) = self.as_mut() {
            rect.set_end(end);
            Ok(())
        } else {
            Err(Error::MissingFieldError { field: "DimensionRect->end".to_string() })
        }
    }

    fn set_top(&mut self, top: Dimension) -> Result<(), Error> {
        if let Some(rect) = self.as_mut() {
            rect.set_top(top);
            Ok(())
        } else {
            Err(Error::MissingFieldError { field: "DimensionRect->top".to_string() })
        }
    }

    fn set_bottom(&mut self, bottom: Dimension) -> Result<(), Error> {
        if let Some(rect) = self.as_mut() {
            rect.set_bottom(bottom);
            Ok(())
        } else {
            Err(Error::MissingFieldError { field: "DimensionRect->bottom".to_string() })
        }
    }
}

#[repr(u8)]
#[derive(Clone, Copy, PartialEq, Debug)]
pub enum PathCommand {
    MoveTo = 0,  // 1 Point
    LineTo = 1,  // 1 Point
    CubicTo = 2, // 3 Points
    QuadTo = 3,  // 2 Points
    Close = 4,   // 0 Points
}
impl TryFrom<u8> for PathCommand {
    type Error = &'static str;

    fn try_from(value: u8) -> Result<Self, Self::Error> {
        match value {
            0 => Ok(PathCommand::MoveTo),
            1 => Ok(PathCommand::LineTo),
            2 => Ok(PathCommand::CubicTo),
            3 => Ok(PathCommand::QuadTo),
            4 => Ok(PathCommand::Close),
            _ => Err("PathCommand out of range"),
        }
    }
}

impl Path {
    pub fn new_default() -> Path {
        Path {
            commands: Vec::new(),
            data: Vec::new(),
            winding_rule: WindingRule::WINDING_RULE_NON_ZERO.into(),
            ..Default::default()
        }
    }
    pub fn with_winding_rule(&mut self, winding_rule: WindingRule) -> &mut Path {
        self.winding_rule = winding_rule.into();
        self
    }
    pub fn move_to(&mut self, x: f32, y: f32) -> &mut Path {
        self.commands.push(PathCommand::MoveTo as u8);
        self.data.push(x);
        self.data.push(y);
        self
    }
    pub fn line_to(&mut self, x: f32, y: f32) -> &mut Path {
        self.commands.push(PathCommand::LineTo as u8);
        self.data.push(x);
        self.data.push(y);
        self
    }
    pub fn cubic_to(
        &mut self,
        c1_x: f32,
        c1_y: f32,
        c2_x: f32,
        c2_y: f32,
        x: f32,
        y: f32,
    ) -> &mut Path {
        self.commands.push(PathCommand::CubicTo as u8);
        self.data.push(c1_x);
        self.data.push(c1_y);
        self.data.push(c2_x);
        self.data.push(c2_y);
        self.data.push(x);
        self.data.push(y);
        self
    }
    pub fn quad_to(&mut self, c1_x: f32, c1_y: f32, x: f32, y: f32) -> &mut Path {
        self.commands.push(PathCommand::QuadTo as u8);
        self.data.push(c1_x);
        self.data.push(c1_y);
        self.data.push(x);
        self.data.push(y);
        self
    }
    pub fn close(&mut self) -> &mut Path {
        self.commands.push(PathCommand::Close as u8);
        self
    }
}

impl Background {
    pub fn new_with_background(bg_type: background::Background_type) -> Self {
        Background { background_type: Some(bg_type), ..Default::default() }
    }
    pub fn new_none() -> Self {
        Background {
            background_type: Some(background::Background_type::None(Empty::new())),
            ..Default::default()
        }
    }
    pub fn is_some(&self) -> bool {
        if let Some(bg) = &self.background_type {
            match bg {
                background::Background_type::None(_) => false,
                _ => true,
            }
        } else {
            false
        }
    }
}

impl ColorOrVar {
    pub fn new_color(color: Color) -> Self {
        ColorOrVar {
            ColorOrVarType: Some(color_or_var::ColorOrVarType::Color(color)),
            ..Default::default()
        }
    }
    pub fn new_var(id: String, fallback: Option<Color>) -> Self {
        ColorOrVar {
            ColorOrVarType: Some(color_or_var::ColorOrVarType::Var(color_or_var::ColorVar {
                id,
                fallback: fallback.into(),
                ..Default::default()
            })),
            ..Default::default()
        }
    }
}

impl ViewShape {
    pub fn new_rect(bx: view_shape::Box) -> Self {
        ViewShape { shape: Some(view_shape::Shape::Rect(bx)), ..Default::default() }
    }

    pub fn new_round_rect(rect: view_shape::RoundRect) -> Self {
        ViewShape { shape: Some(view_shape::Shape::RoundRect(rect)), ..Default::default() }
    }

    pub fn new_path(path: view_shape::VectorPath) -> Self {
        ViewShape { shape: Some(view_shape::Shape::Path(path)), ..Default::default() }
    }

    pub fn new_arc(arc: view_shape::VectorArc) -> Self {
        ViewShape { shape: Some(view_shape::Shape::Arc(arc)), ..Default::default() }
    }

    pub fn new_vector_rect(rect: view_shape::VectorRect) -> Self {
        ViewShape { shape: Some(view_shape::Shape::VectorRect(rect)), ..Default::default() }
    }
}

impl FontStretch {
    /// Ultra-condensed width (50%), the narrowest possible.
    pub fn ultra_condensed() -> Self {
        FontStretch { value: 0.5, ..Default::default() }
    }
    /// Extra-condensed width (62.5%).
    pub fn extra_condensed() -> Self {
        FontStretch { value: 0.625, ..Default::default() }
    }
    /// Condensed width (75%).
    pub fn condensed() -> Self {
        FontStretch { value: 0.75, ..Default::default() }
    }
    /// Semi-condensed width (87.5%).
    pub fn semi_condensed() -> Self {
        FontStretch { value: 0.875, ..Default::default() }
    }
    /// Normal width (100%).
    pub fn normal() -> Self {
        FontStretch { value: 1.0, ..Default::default() }
    }
    /// Semi-expanded width (112.5%).
    pub fn semi_expanded() -> Self {
        FontStretch { value: 1.125, ..Default::default() }
    }
    /// Expanded width (125%).
    pub fn expanded() -> Self {
        FontStretch { value: 1.25, ..Default::default() }
    }
    /// Extra-expanded width (150%).
    pub fn extra_expanded() -> Self {
        FontStretch { value: 1.5, ..Default::default() }
    }
    /// Ultra-expanded width (200%), the widest possible.
    pub fn ultra_expanded() -> Self {
        FontStretch { value: 2.0, ..Default::default() }
    }
}

impl Hash for FontStretch {
    fn hash<H: Hasher>(&self, state: &mut H) {
        let x = (self.value * 100.0) as i32;
        x.hash(state);
    }
}

impl NumOrVar {
    pub fn from_num(num: f32) -> Self {
        NumOrVar { NumOrVarType: Some(NumOrVarType::Num(num)), ..Default::default() }
    }
}

impl FontWeight {
    pub fn new_with_num_or_var_type(num_or_var_type: NumOrVarType) -> Self {
        FontWeight {
            weight: Some(NumOrVar { NumOrVarType: Some(num_or_var_type), ..Default::default() })
                .into(),
            ..Default::default()
        }
    }

    pub fn from_num(weight: f32) -> Self {
        FontWeight { weight: Some(NumOrVar::from_num(weight)).into(), ..Default::default() }
    }

    /// Thin weight (100), the thinnest value.
    pub fn thin() -> Self {
        FontWeight::from_num(100.0)
    }
    /// Extra light weight (200).
    pub fn extra_light() -> Self {
        FontWeight::from_num(200.0)
    }
    /// Light weight (300).
    pub fn light() -> Self {
        FontWeight::from_num(300.0)
    }
    /// Normal (400).
    pub fn normal() -> Self {
        FontWeight::from_num(400.0)
    }
    /// Medium weight (500, higher than normal).
    pub fn medium() -> Self {
        FontWeight::from_num(500.0)
    }
    /// Semibold weight (600).
    pub fn semibold() -> Self {
        FontWeight::from_num(600.0)
    }
    /// Bold weight (700).
    pub fn bold() -> Self {
        FontWeight::from_num(700.0)
    }
    /// Extra-bold weight (800).
    pub fn extra_bold() -> Self {
        FontWeight::from_num(800.0)
    }
    /// Black weight (900), the thickest value.
    pub fn black() -> Self {
        FontWeight::from_num(900.0)
    }
}
