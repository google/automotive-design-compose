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

use std::convert::TryFrom;

use dc_bundle::legacy_definition::modifier::blend::BlendMode;
use serde::{Deserialize, Serialize};

use crate::toolkit_style::{AffineTransform, Background, BoxShadow, Stroke};

// This is a simple vector representation that should reproduce Figma or
// another design tool's rendering. It simplifies the renderer's task by
// precomputing the stroke path, and using the same definitions as the
// toolkit schema.
//
// The serialized form should be somewhat compact and fast to deserialize
// because we're storing different types (commands, values) in big arrays,
// reducing the number of objects that need to be constructed during
// deserialization.

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

#[derive(Clone, Copy, PartialEq, Debug, Serialize, Deserialize, Hash)]
#[serde(rename_all = "UPPERCASE")]
pub enum WindingRule {
    NonZero,
    EvenOdd,
    #[serde(other)]
    None,
}

fn default_winding_rule() -> WindingRule {
    WindingRule::None
}

#[derive(Clone, PartialEq, Serialize, Deserialize)]
pub struct Path {
    commands: Vec<u8>,
    data: Vec<f32>,
    #[serde(default = "default_winding_rule")]
    winding_rule: WindingRule,
}
impl Path {
    pub fn new() -> Path {
        Path { commands: Vec::new(), data: Vec::new(), winding_rule: WindingRule::NonZero }
    }
    pub fn winding_rule(&mut self, winding_rule: WindingRule) -> &mut Path {
        self.winding_rule = winding_rule;
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
    pub fn iter(&self) -> PathIterator<'_> {
        PathIterator(self.commands.iter(), self.data.iter())
    }
}

/// A fused Command + Data type, making it easier to work with paths.
#[derive(Copy, Clone, Debug, PartialEq)]
pub enum PathElement {
    MoveTo { x: f32, y: f32 },
    LineTo { x: f32, y: f32 },
    CubicTo { c1_x: f32, c1_y: f32, c2_x: f32, c2_y: f32, x: f32, y: f32 },
    QuadTo { c1_x: f32, c1_y: f32, x: f32, y: f32 },
    Close,
}

pub struct PathIterator<'a>(std::slice::Iter<'a, u8>, std::slice::Iter<'a, f32>);

/// Allow iteration over a path with commands and data fused.
impl<'a> Iterator for PathIterator<'a> {
    type Item = PathElement;

    fn next(&mut self) -> Option<Self::Item> {
        match self.0.next().map(|val| PathCommand::try_from(*val)) {
            Some(Ok(PathCommand::MoveTo)) => {
                let x = *self.1.next()?;
                let y = *self.1.next()?;
                Some(PathElement::MoveTo { x, y })
            }
            Some(Ok(PathCommand::LineTo)) => {
                let x = *self.1.next()?;
                let y = *self.1.next()?;
                Some(PathElement::LineTo { x, y })
            }
            Some(Ok(PathCommand::CubicTo)) => {
                let c1_x = *self.1.next()?;
                let c1_y = *self.1.next()?;
                let c2_x = *self.1.next()?;
                let c2_y = *self.1.next()?;
                let x = *self.1.next()?;
                let y = *self.1.next()?;
                Some(PathElement::CubicTo { c1_x, c1_y, c2_x, c2_y, x, y })
            }
            Some(Ok(PathCommand::QuadTo)) => {
                let c1_x = *self.1.next()?;
                let c1_y = *self.1.next()?;
                let x = *self.1.next()?;
                let y = *self.1.next()?;
                Some(PathElement::QuadTo { c1_x, c1_y, x, y })
            }
            Some(Ok(PathCommand::Close)) => Some(PathElement::Close),
            _ => None,
        }
    }
}

impl std::fmt::Debug for Path {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("Path")
            .field("winding_rule", &self.winding_rule)
            .field("d", &self.iter().collect::<Vec<PathElement>>())
            .finish()
    }
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct RenderStyle {
    pub fill: Vec<Background>,
    pub blend_mode: BlendMode,
    pub transform: AffineTransform,
    pub opacity: f32,
    pub shadows: Vec<BoxShadow>, // inner shadow + stroke might be broken
    pub is_mask: bool,           // subsequent siblings are masked by this vector.
}

// Now for the things that can be rendered
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum RenderCommand {
    FillPath {
        fill: Vec<Path>,
        stroke: Stroke,
        style: RenderStyle,
    },
    // Applies a blend mode or opacity to its children
    Group {
        blend_mode: BlendMode,
        transform: AffineTransform,
        opacity: f32,
        children: Vec<RenderCommand>,
    },
}
