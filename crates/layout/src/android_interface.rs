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
