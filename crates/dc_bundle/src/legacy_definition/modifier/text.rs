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
