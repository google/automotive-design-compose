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

use crate::layout_node::{self};

use super::Error;

impl TryFrom<super::LayoutNode> for layout_node::LayoutNode {
    type Error = Error;

    fn try_from(proto: super::LayoutNode) -> Result<Self, Self::Error> {
        Ok(layout_node::LayoutNode {
            layout_id: proto.layout_id,
            parent_layout_id: proto.parent_layout_id,
            child_index: proto.child_index,
            style: proto
                .style
                .ok_or(Error::MissingFieldError { field: "style".to_string() })?
                .try_into()?,
            name: proto.name,
            use_measure_func: proto.use_measure_func,
            fixed_width: proto.fixed_width,
            fixed_height: proto.fixed_height,
        })
    }
}

impl From<super::LayoutParentChildren> for layout_node::LayoutParentChildren {
    fn from(proto: super::LayoutParentChildren) -> Self {
        layout_node::LayoutParentChildren {
            parent_layout_id: proto.parent_layout_id,
            child_layout_ids: proto.child_layout_ids,
        }
    }
}

impl TryFrom<super::LayoutNodeList> for layout_node::LayoutNodeList {
    type Error = super::Error;

    fn try_from(proto: super::LayoutNodeList) -> Result<Self, Self::Error> {
        Ok(layout_node::LayoutNodeList {
            layout_nodes: {
                let mut layout_nodes = Vec::new();
                for node in proto.layout_nodes {
                    layout_nodes.push(node.try_into()?)
                }
                layout_nodes
            },
            parent_children: proto
                .parent_children
                .into_iter()
                .map(layout_node::LayoutParentChildren::from)
                .collect(),
        })
    }
}
