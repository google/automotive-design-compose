// Copyright 2024 Google LLC
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

use serde::{Deserialize, Serialize};

/// The final result of a layout algorithm for a single taffy Node
#[derive(Clone, PartialEq, Debug, Serialize, Deserialize)]
pub struct Layout {
    // Relative ordering of the node. Render nodes with a higher order on top
    // of nodes with lower order
    pub order: u32,
    pub width: f32,
    pub height: f32,
    // The top-left corner of the node
    pub left: f32,
    pub top: f32,
    // For nodes with children, this contains the size of all the child content.
    // This is used to calculate the scrollable area when scrolling is enabled.
    pub content_width: f32,
    pub content_height: f32,
}

impl Layout {
    pub fn from_taffy_layout(l: &taffy::prelude::Layout) -> Layout {
        Layout {
            order: l.order,
            width: l.size.width,
            height: l.size.height,
            left: l.location.x,
            top: l.location.y,
            content_width: l.content_size.width,
            content_height: l.content_size.height,
        }
    }
}
