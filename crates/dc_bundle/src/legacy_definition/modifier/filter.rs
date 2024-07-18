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
