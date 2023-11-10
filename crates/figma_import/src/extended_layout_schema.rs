// Copyright 2023 Google LLC
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

// This module deserializes our "vsw Extended Layout" plugin output. The plugin writes one of two
// values: "vsw-extended-text-layout" and "vsw-extended-auto-layout". We define a struct for each
// so we don't have to deal with serde/bincode tagged enum issues.

/// ExtendedTextLayout is an extra set of fields set by our plugin that help us to deal with
/// dynamic text content. We need to know if it should wrap, or be truncated or ellipsized,
/// and Figma doesn't have any properties for those details because Figma only deals with
/// static text.
#[derive(Serialize, Deserialize, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct ExtendedTextLayout {
    /// The number of lines to let the text wrap to, or zero for an unlimited number of lines.
    pub line_count: u32,
    /// Should the text be ellipsized when it runs out of space, or just truncated?
    pub ellipsize: bool,
}

#[derive(Serialize, Deserialize, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct SpanData {
    /// The node name of an item that goes into a grid layout
    #[serde(default)]
    pub node_name: String,
    /// The number of rows or columns this item occupies in the grid layout
    #[serde(default)]
    pub span: u32,
    /// If true, spans all columns or rows in the grid layout
    #[serde(default)]
    pub max_span: bool,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
#[derive(Default)]
pub enum LayoutType {
    #[default]
    None,
    FixedColumns,
    FixedRows,
    AutoColumns,
    AutoRows,
    Horizontal,
    Vertical,
}
impl LayoutType {
    pub(crate) fn is_grid(&self) -> bool {
        match self {
            LayoutType::FixedColumns
            | LayoutType::FixedRows
            | LayoutType::AutoColumns
            | LayoutType::AutoRows => true,
            _ => false,
        }
    }
    pub(crate) fn is_row_or_column(&self) -> bool {
        match self {
            LayoutType::Horizontal | LayoutType::Vertical => true,
            _ => false,
        }
    }
}

#[derive(Deserialize, Serialize, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
#[derive(Default)]
pub enum Alignment {
    #[default]
    Start,
    Center,
    End,
}

#[derive(Serialize, Deserialize, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct CommonLayoutData {
    /// Left margin of widget layout
    #[serde(default)]
    pub margin_left: f32,
    /// Right margin of widget layout
    #[serde(default)]
    pub margin_right: f32,
    /// Top margin of widget layout
    #[serde(default)]
    pub margin_top: f32,
    /// Bottom margin of widget layout
    #[serde(default)]
    pub margin_bottom: f32,
    /// Scrolling enabled
    #[serde(default)]
    pub scrolling: bool,
}
impl Default for CommonLayoutData {
    fn default() -> CommonLayoutData {
        CommonLayoutData {
            margin_left: 0.0,
            margin_right: 0.0,
            margin_top: 0.0,
            margin_bottom: 0.0,
            scrolling: false,
        }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
#[derive(Default)]
pub enum SizePolicy {
    Hug,
    #[default]
    Fixed,
}

#[derive(Serialize, Deserialize, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct AutoLayoutData {
    /// Spacing in pixels between items
    #[serde(default)]
    pub item_spacing: f32,
    /// Horizontal alignment
    #[serde(default)]
    pub horizontal_alignment: Alignment,
    /// Vertical alignment
    #[serde(default)]
    pub vertical_alignment: Alignment,
    /// Space between instead of fixed item spacing
    #[serde(default)]
    pub space_between: bool,
    /// Size policy: hug contents or fixed
    #[serde(default)]
    pub size_policy: SizePolicy,
}
impl Default for AutoLayoutData {
    fn default() -> AutoLayoutData {
        AutoLayoutData {
            item_spacing: 0.0,
            horizontal_alignment: Alignment::Start,
            vertical_alignment: Alignment::Start,
            space_between: false,
            size_policy: SizePolicy::Fixed,
        }
    }
}

#[derive(Serialize, Deserialize, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct GridLayoutData {
    /// The number of fixed columns if horizontal is true, or rows if horizontal is false
    #[serde(default)]
    pub columns_rows: u32,
    /// The minimum width or height of a column or row when using adaptive columns/rows
    #[serde(default)]
    pub adaptive_min_size: u32,
    /// Automatic column/row edge-to-edge spacing
    #[serde(default)]
    pub auto_spacing: bool,
    /// Item size for auto spacing
    #[serde(default)]
    pub auto_spacing_item_size: i32,
    /// Vertical spacing in pixels between items
    #[serde(default)]
    pub vertical_spacing: i32,
    /// Horizontal spacing in pixels between items
    #[serde(default)]
    pub horizontal_spacing: i32,
    /// The number of columns or rows that each type of item that can go into this grid occupies
    #[serde(default)]
    pub span_content: Vec<SpanData>,
}

impl Default for GridLayoutData {
    fn default() -> GridLayoutData {
        GridLayoutData {
            columns_rows: 2,
            adaptive_min_size: 100,
            auto_spacing: false,
            auto_spacing_item_size: 1,
            vertical_spacing: 0,
            horizontal_spacing: 0,
            span_content: vec![],
        }
    }
}

#[derive(Serialize, Deserialize, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
#[derive(Default)]
pub struct LimitContentData {
    /// maximum number of children a frame can have
    #[serde(default)]
    pub max_num_items: u32,
    /// name of overflow node to use as last child when there are more children than max_num_items
    #[serde(default)]
    pub overflow_node_name: String,
    /// id of overflow node to use as last child when there are more children than max_num_items
    #[serde(default)]
    pub overflow_node_id: String,
}

#[derive(Serialize, Deserialize, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct ExtendedAutoLayout {
    /// Should the layout wrap when its children overflow the available space? This
    /// corresponds to the "flex-wrap" property and is useful for making grids.
    #[serde(default)]
    pub wrap: bool,
    /// For frames with scrolling enabled, this is true if scrolling is page-based,
    /// meaning the width/height of the frame is considered a page and scrolling snaps
    /// to the next page
    #[serde(default)]
    pub paged_scrolling: bool,
    /// Layout type: Grid (column or row, fixed or adaptive) or Autolayout (horizontal or vertical)
    #[serde(default)]
    pub layout: LayoutType,
    /// Various parameters shared between grid and auto layouts
    #[serde(default)]
    pub common_data: CommonLayoutData,
    /// Various parameters for grid layouts
    #[serde(default)]
    pub grid_layout_data: GridLayoutData,
    /// Various parameters for horizontal/vertical layouts from the preview widget
    #[serde(default)]
    pub auto_layout_data: AutoLayoutData,
    /// For frames with autolayout, limit_content specifies that the maximum number of
    /// child items under the frame is max_num_items.
    #[serde(default)]
    pub limit_content: bool,
    /// Parameters for limiting the number of items and using an overflow node
    #[serde(default)]
    pub limit_content_data: LimitContentData,
}
