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
