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

impl From<super::LayoutChangedResponse> for crate::LayoutChangedResponse {
    fn from(proto: super::LayoutChangedResponse) -> Self {
        Self {
            layout_state: proto.layout_state,
            changed_layouts: proto
                .changed_layouts
                .into_iter()
                .map(|(key, value)| (key, value.into()))
                .collect(),
        }
    }
}

impl From<crate::LayoutChangedResponse> for super::LayoutChangedResponse {
    fn from(proto: crate::LayoutChangedResponse) -> Self {
        Self {
            layout_state: proto.layout_state,
            changed_layouts: proto
                .changed_layouts
                .into_iter()
                .map(|(key, value)| (key, value.into()))
                .collect(),
        }
    }
}

impl From<super::Layout> for crate::types::Layout {
    fn from(proto: super::Layout) -> Self {
        Self {
            order: proto.order,
            width: proto.width,
            height: proto.height,
            left: proto.left,
            top: proto.top,
        }
    }
}

impl From<crate::types::Layout> for super::Layout {
    fn from(layout: crate::types::Layout) -> Self {
        super::Layout {
            order: layout.order,
            width: layout.width,
            height: layout.height,
            left: layout.left,
            top: layout.top,
        }
    }
}
