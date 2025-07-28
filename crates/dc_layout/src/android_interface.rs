// Copyright 2025 Google LLC
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

use dc_bundle::jni_layout::{self, Layout, LayoutChangedResponse};

pub trait FromTaffyLayout {
    fn from_taffy_layout(l: &taffy::prelude::Layout) -> jni_layout::Layout;
}

impl FromTaffyLayout for Layout {
    fn from_taffy_layout(l: &taffy::prelude::Layout) -> jni_layout::Layout {
        Layout {
            order: l.order,
            width: l.size.width,
            height: l.size.height,
            left: l.location.x,
            top: l.location.y,
            content_width: l.content_size.width,
            content_height: l.content_size.height,
            ..Default::default()
        }
    }
}

pub trait LayoutChangedResponseUnchangedWithState {
    fn unchanged_with_state(layout_state: i32) -> jni_layout::LayoutChangedResponse;
}

impl LayoutChangedResponseUnchangedWithState for LayoutChangedResponse {
    fn unchanged_with_state(layout_state: i32) -> jni_layout::LayoutChangedResponse {
        LayoutChangedResponse {
            layout_state,
            changed_layouts: HashMap::new(),
            ..Default::default()
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use taffy::geometry::Point;
    use taffy::prelude::{Layout as TaffyLayout, Size};

    #[test]
    fn test_from_taffy_layout() {
        let taffy_layout = TaffyLayout {
            order: 1,
            size: Size { width: 100.0, height: 200.0 },
            location: Point { x: 10.0, y: 20.0 },
            content_size: Size { width: 90.0, height: 190.0 },
            ..Default::default()
        };
        let layout = Layout::from_taffy_layout(&taffy_layout);
        assert_eq!(layout.order, 1);
        assert_eq!(layout.width, 100.0);
        assert_eq!(layout.height, 200.0);
        assert_eq!(layout.left, 10.0);
        assert_eq!(layout.top, 20.0);
        assert_eq!(layout.content_width, 90.0);
        assert_eq!(layout.content_height, 190.0);
    }

    #[test]
    fn test_layout_changed_response_unchanged_with_state() {
        let response = LayoutChangedResponse::unchanged_with_state(123);
        assert_eq!(response.layout_state, 123);
        assert!(response.changed_layouts.is_empty());
    }
}
