use serde::{Deserialize, Serialize};
use crate::toolkit_layout_style::{AlignContent, AlignItems, AlignSelf, Dimension, FlexDirection, FlexWrap, JustifyContent, LayoutSizing, Overflow, PositionType, Rect, Size};
use crate::toolkit_style::{GridLayoutType, GridSpan, ItemSpacing};
use taffy::prelude as taffy;

#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct LayoutStyle {
pub padding: Rect<Dimension>,

    pub flex_grow: f32,
    pub flex_shrink: f32,
    pub flex_basis: Dimension,
    pub item_spacing: ItemSpacing,

    pub align_content: AlignContent,
    pub justify_content: JustifyContent,
    pub align_items: AlignItems,
    pub flex_direction: FlexDirection,
    pub align_self: AlignSelf,

    pub width: Dimension,
    pub height: Dimension,
    pub min_width: Dimension,
    pub max_width: Dimension,
    pub min_height: Dimension,
    pub max_height: Dimension,

    pub bounding_box: Size<f32>,

    pub position_type: PositionType,
    pub top: Dimension,
    pub left: Dimension,
    pub bottom: Dimension,
    pub right: Dimension,

    pub margin: Rect<Dimension>,

    pub flex_wrap: FlexWrap,
    pub grid_layout: Option<GridLayoutType>,
    pub grid_columns_rows: u32,
    pub grid_adaptive_min_size: u32,
    pub grid_span_content: Vec<GridSpan>,
    pub overflow: Overflow,
    pub max_children: Option<u32>,
    pub overflow_node_id: Option<String>,
    pub overflow_node_name: Option<String>,

    pub node_size: Size<f32>,

    pub cross_axis_item_spacing: f32,

    pub horizontal_sizing: LayoutSizing,
    pub vertical_sizing: LayoutSizing,


}

impl Default for LayoutStyle {
    fn default() -> LayoutStyle {
        LayoutStyle{
            padding: Rect::<Dimension>::default(),

            position_type: PositionType::default(),
            flex_direction: FlexDirection::default(),

            align_items: AlignItems::default(),
            align_self: AlignSelf::default(),
            align_content: AlignContent::default(),
            justify_content: JustifyContent::default(),
            top: Dimension::default(),
            left: Dimension::default(),
            bottom: Dimension::default(),
            right: Dimension::default(),
            margin: Rect::<Dimension>::default(),
            item_spacing: ItemSpacing::default(),

            flex_grow: 0.0,
            flex_shrink: 0.0,
            flex_basis: Dimension::default(),
            bounding_box: Size::default(),

            width: Dimension::default(),
            height: Dimension::default(),
            min_width: Dimension::default(),
            max_width: Dimension::default(),
            min_height: Dimension::default(),
            max_height: Dimension::default(),

            flex_wrap: FlexWrap::default(),
            grid_layout: None,
            grid_columns_rows: 0,
            grid_adaptive_min_size: 1,
            grid_span_content: vec![],
            overflow: Overflow::default(),
            max_children: None,
            overflow_node_id: None,
            overflow_node_name: None,

            cross_axis_item_spacing: 0.0,
            node_size: Size::default(),

            horizontal_sizing: LayoutSizing::default(),
            vertical_sizing: LayoutSizing::default(),
        }
    }
}

impl LayoutStyle {
    /// Compute the difference between this style and the given style, returning a style
    /// that can be applied to this style to make it equal the given style using apply_non_default.
    pub fn difference(&self, other: &LayoutStyle) -> LayoutStyle {
        let mut delta = LayoutStyle::default();

        if self.position_type != other.position_type {
            delta.position_type = other.position_type;
        }
        if self.flex_direction != other.flex_direction {
            delta.flex_direction = other.flex_direction;
        }
        if self.node_size != other.node_size {
            delta.node_size = other.node_size;
        }
        if self.align_items != other.align_items {
            delta.align_items = other.align_items;
        }
        if self.align_content != other.align_content {
            delta.align_content = other.align_content;
        }
        if self.justify_content != other.justify_content {
            delta.justify_content = other.justify_content;
        }
        if self.top != other.top {
            delta.top = other.top;
        }
        if self.left != other.left {
            delta.left = other.left;
        }
        if self.bottom != other.bottom {
            delta.bottom = other.bottom;
        }
        if self.right != other.right {
            delta.right = other.right;
        }
        if self.margin != other.margin {
            delta.margin = other.margin;
        }
        if self.padding != other.padding {
            delta.padding = other.padding;
        }
        if self.item_spacing != other.item_spacing {
            delta.item_spacing = other.item_spacing.clone();
        }
        if self.cross_axis_item_spacing != other.cross_axis_item_spacing {
            delta.cross_axis_item_spacing = other.cross_axis_item_spacing;
        }
        if self.flex_grow != other.flex_grow {
            delta.flex_grow = other.flex_grow;
        }
        if self.flex_shrink != other.flex_shrink {
            delta.flex_shrink = other.flex_shrink;
        }
        if self.flex_basis != other.flex_basis {
            delta.flex_basis = other.flex_basis;
        }
        if self.width != other.width {
            delta.width = other.width;
        }
        if self.height != other.height {
            delta.height = other.height;
        }
        if self.max_width != other.max_width {
            delta.max_width = other.max_width;
        }
        if self.max_height != other.max_height {
            delta.max_height = other.max_height;
        }
        if self.min_width != other.min_width {
            delta.min_width = other.min_width;
        }
        if self.min_height != other.min_height {
            delta.min_height = other.min_height;
        }
        delta
    }
    pub fn uniform(measurement: f32) -> Rect<Dimension> {
        Rect {
            start: Dimension::Points(measurement),
            end: Dimension::Points(measurement),
            top: Dimension::Points(measurement),
            bottom: Dimension::Points(measurement),
        }
    }
}

impl Into<taffy::Style> for &LayoutStyle {
    fn into(self) -> taffy::Style {
        let mut tstyle = taffy::Style::default();

        tstyle.padding.left = (&self.padding.start).into();
        tstyle.padding.right = (&self.padding.end).into();
        tstyle.padding.top = (&self.padding.top).into();
        tstyle.padding.bottom = (&self.padding.bottom).into();

        tstyle.flex_grow = self.flex_grow;
        tstyle.flex_shrink = self.flex_shrink;
        tstyle.flex_basis = (&self.flex_basis).into();
        tstyle.gap.width = (&self.item_spacing).into();
        tstyle.gap.height = (&self.item_spacing).into();

        tstyle.align_content = Some((&self.align_content).into());
        tstyle.justify_content = Some((&self.justify_content).into());
        tstyle.align_items = Some((&self.align_items).into());
        tstyle.flex_direction = (&self.flex_direction).into();
        tstyle.align_self = (&self.align_self).into();

        tstyle.size.width = (&self.width).into();
        tstyle.size.height = (&self.height).into();
        tstyle.min_size.width = (&self.min_width).into();
        tstyle.min_size.height = (&self.min_height).into();
        tstyle.max_size.width = (&self.max_width).into();
        tstyle.max_size.height = (&self.max_height).into();

        // If we have a fixed size, use the bounding box since that takes into
        // account scale and rotation, and disregard min/max sizes.
        // TODO support this with non-fixed sizes also!
        if self.width.is_points() {
            tstyle.size.width = taffy::Dimension::Points(self.bounding_box.width);
            tstyle.min_size.width = taffy::Dimension::Auto;
            tstyle.max_size.width = taffy::Dimension::Auto;
        }
        if self.height.is_points() {
            tstyle.size.height = taffy::Dimension::Points(self.bounding_box.height);
            tstyle.min_size.height = taffy::Dimension::Auto;
            tstyle.max_size.height = taffy::Dimension::Auto;
        }

        tstyle.position = (&self.position_type).into();
        tstyle.inset.left = (&self.left).into();
        tstyle.inset.right = (&self.right).into();
        tstyle.inset.top = (&self.top).into();
        tstyle.inset.bottom = (&self.bottom).into();

        tstyle.margin.left = (&self.margin.start).into();
        tstyle.margin.right = (&self.margin.end).into();
        tstyle.margin.top = (&self.margin.top).into();
        tstyle.margin.bottom = (&self.margin.bottom).into();

        tstyle.display = taffy::Display::Flex; // TODO set to None to hide
        tstyle
    }
}
