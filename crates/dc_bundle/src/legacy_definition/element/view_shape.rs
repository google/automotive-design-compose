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
use crate::definition::element::num_or_var::NumOrVar;
use crate::definition::element::Path;
use serde::{Deserialize, Serialize};

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

/// Shape of a view, either a rect or a path of some kind.
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub enum ViewShape {
    Rect {
        is_mask: bool,
    },
    RoundRect {
        corner_radius: [NumOrVar; 4],
        corner_smoothing: f32,
        is_mask: bool,
    },
    Path {
        path: Vec<Path>,
        stroke: Vec<Path>,
        stroke_cap: StrokeCap,
        is_mask: bool,
    },
    Arc {
        path: Vec<Path>,
        stroke: Vec<Path>,
        stroke_cap: StrokeCap,
        start_angle_degrees: f32,
        sweep_angle_degrees: f32,
        inner_radius: f32,
        corner_radius: f32,
        is_mask: bool,
    },
    VectorRect {
        path: Vec<Path>,
        stroke: Vec<Path>,
        corner_radius: [NumOrVar; 4],
        is_mask: bool,
    },
}
