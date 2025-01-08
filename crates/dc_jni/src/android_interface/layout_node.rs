// // Copyright 2024 Google LLC
// //
// // Licensed under the Apache License, Version 2.0 (the "License");
// // you may not use this file except in compliance with the License.
// // You may obtain a copy of the License at
// //
// //     http://www.apache.org/licenses/LICENSE-2.0
// //
// // Unless required by applicable law or agreed to in writing, software
// // distributed under the License is distributed on an "AS IS" BASIS,
// // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// // See the License for the specific language governing permissions and
// // limitations under the License.

// use crate::android_interface::layout_changed_response::Layout;
// use crate::android_interface::{LayoutNode, LayoutChangedResponse, LayoutParentChildren, LayoutNodeList};


// impl TryFrom<LayoutNode> for layout::layout_node::LayoutNode {
//     type Error = dc_bundle::Error;

//     fn try_from(proto: LayoutNode) -> Result<Self, Self::Error> {
//         Ok(layout::layout_node::LayoutNode {
//             layout_id: proto.layout_id,
//             parent_layout_id: proto.parent_layout_id,
//             child_index: proto.child_index,
//             style: proto
//                 .style
//                 .ok_or(dc_bundle::Error::MissingFieldError { field: "style".to_string() })?,
//             name: proto.name,
//             use_measure_func: proto.use_measure_func,
//             fixed_width: proto.fixed_width,
//             fixed_height: proto.fixed_height,
//         })
//     }
// }

// impl From<LayoutParentChildren> for layout::layout_node::LayoutParentChildren {
//     fn from(proto: LayoutParentChildren) -> Self {
//         layout::layout_node::LayoutParentChildren {
//             parent_layout_id: proto.parent_layout_id,
//             child_layout_ids: proto.child_layout_ids,
//         }
//     }
// }

// impl TryFrom<LayoutNodeList> for layout::layout_node::LayoutNodeList {
//     type Error = dc_bundle::Error;

//     fn try_from(proto: LayoutNodeList) -> Result<Self, Self::Error> {
//         Ok(layout::layout_node::LayoutNodeList {
//             layout_nodes: {
//                 let mut layout_nodes = Vec::new();
//                 for node in proto.layout_nodes {
//                     layout_nodes.push(node.try_into()?)
//                 }
//                 layout_nodes
//             },
//             parent_children: proto
//                 .parent_children
//                 .into_iter()
//                 .map(layout::layout_node::LayoutParentChildren::from)
//                 .collect(),
//         })
//     }
// }

// impl From<layout::LayoutChangedResponse> for LayoutChangedResponse {
//     fn from(proto: layout::LayoutChangedResponse) -> Self {
//         Self {
//             layout_state: proto.layout_state,
//             changed_layouts: proto
//                 .changed_layouts
//                 .into_iter()
//                 .map(|(key, value)| (key, value.into()))
//                 .collect(),
//         }
//     }
// }

// impl From<LayoutChangedResponse> for layout::LayoutChangedResponse {
//     fn from(proto: LayoutChangedResponse) -> Self {
//         Self {
//             layout_state: proto.layout_state,
//             changed_layouts: proto
//                 .changed_layouts
//                 .into_iter()
//                 .map(|(key, value)| (key, value.into()))
//                 .collect(),
//         }
//     }
// }

// impl From<Layout> for layout::types::Layout {
//     fn from(proto: Layout) -> Self {
//         Self {
//             order: proto.order,
//             width: proto.width,
//             height: proto.height,
//             left: proto.left,
//             top: proto.top,
//             content_width: proto.content_width,
//             content_height: proto.content_height,
//         }
//     }
// }

// impl From<layout::types::Layout> for Layout {
//     fn from(layout: layout::types::Layout) -> Self {
//         Layout {
//             order: layout.order,
//             width: layout.width,
//             height: layout.height,
//             left: layout.left,
//             top: layout.top,
//             content_width: layout.content_width,
//             content_height: layout.content_height,
//         }
//     }
// }
