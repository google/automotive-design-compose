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

use crate::legacy_definition::proto;
use crate::Error;

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

#[derive(Copy, Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum Dimension {
    Undefined,
    Auto,
    Points(f32),
    Percent(f32),
}

impl Default for Dimension {
    fn default() -> Self {
        Self::Undefined
    }
}

impl Dimension {
    pub fn is_points(&self) -> bool {
        match self {
            Dimension::Points(_) => true,
            _ => false,
        }
    }
    pub fn points(&self) -> f32 {
        match self {
            Dimension::Points(p) => *p,
            _ => 0.0,
        }
    }
}

impl TryFrom<Option<proto::element::DimensionProto>> for Dimension {
    type Error = Error;

    fn try_from(proto: Option<proto::element::DimensionProto>) -> Result<Self, Self::Error> {
        Ok(
            match proto
                .ok_or(Error::MissingFieldError { field: "DimensionProto".to_string() })?
                .dimension
                .ok_or(Error::MissingFieldError { field: "Dimension".to_string() })?
            {
                proto::element::dimension_proto::Dimension::Auto(_) => Dimension::Auto,
                proto::element::dimension_proto::Dimension::Points(p) => Dimension::Points(p),
                proto::element::dimension_proto::Dimension::Percent(p) => Dimension::Percent(p),
                proto::element::dimension_proto::Dimension::Undefined(_) => Dimension::Undefined,
            },
        )
    }
}

#[derive(Debug, Copy, Clone, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct Rect<T> {
    pub start: T,
    pub end: T,
    pub top: T,
    pub bottom: T,
}

impl Default for Rect<Dimension> {
    fn default() -> Self {
        Self {
            start: Default::default(),
            end: Default::default(),
            top: Default::default(),
            bottom: Default::default(),
        }
    }
}

impl TryFrom<proto::element::DimensionRect> for Rect<Dimension> {
    type Error = Error;
    fn try_from(proto: proto::element::DimensionRect) -> Result<Self, Self::Error> {
        let rect = Rect {
            start: proto.start.try_into()?,
            end: proto.end.try_into()?,
            top: proto.top.try_into()?,
            bottom: proto.bottom.try_into()?,
        };
        Ok(rect)
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
