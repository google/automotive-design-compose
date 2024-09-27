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

use crate::definition::element::color_or_var::ColorOrVar;
use crate::definition::modifier::AffineTransform;
use crate::legacy_definition::modifier::filter::FilterOp;
use serde::{Deserialize, Serialize};

/// Instead of keeping decoded images in ViewStyle objects, we keep keys to the images in the
/// ImageContext and then fetch decoded images when rendering. This means we can serialize the
/// whole ImageContext, and always get the right image when we render.
#[derive(Clone, PartialEq, Eq, Hash, Serialize, Deserialize, Debug)]
pub struct ImageKey(String);

impl ImageKey {
    pub fn key(&self) -> String {
        self.0.clone()
    }

    pub fn new(str: String) -> ImageKey {
        ImageKey(str)
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
        key: Option<ImageKey>,
        filters: Vec<FilterOp>,
        transform: Option<AffineTransform>,
        scale_mode: ScaleMode,
        opacity: f32,
        res_name: Option<String>,
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
    pub fn from_image_key(key: ImageKey) -> Background {
        Background::Image {
            key: Some(key),
            filters: Vec::new(),
            transform: None,
            scale_mode: ScaleMode::Tile,
            opacity: 1.0,
            res_name: None,
        }
    }
}
