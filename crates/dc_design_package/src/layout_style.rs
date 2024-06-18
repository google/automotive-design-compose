use serde::{Deserialize, Serialize};
use layout::styles::{AlignContent, AlignItems, AlignSelf, FlexDirection, ItemSpacing, JustifyContent, PositionType};
use layout::types::{Dimension, Rect, Size};

#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct LayoutStyle {
    pub margin: Rect<Dimension>,
    pub padding: Rect<Dimension>,
    pub item_spacing: ItemSpacing,
    pub top: Dimension,
    pub left: Dimension,
    pub bottom: Dimension,
    pub right: Dimension,
    pub width: Dimension,
    pub height: Dimension,
    pub min_width: Dimension,
    pub max_width: Dimension,
    pub min_height: Dimension,
    pub max_height: Dimension,
    pub bounding_box: Size<f32>,
    pub flex_grow: f32,
    pub flex_shrink: f32,
    pub flex_basis: Dimension,
    pub align_self: AlignSelf,
    pub align_content: AlignContent,
    pub align_items: AlignItems,
    pub flex_direction: FlexDirection,
    pub justify_content: JustifyContent,
    pub position_type: PositionType,
}

impl Default for LayoutStyle {
    fn default() -> LayoutStyle {
        LayoutStyle {
            margin: Rect::<Dimension>::default(),
            padding: Rect::<Dimension>::default(),
            item_spacing: ItemSpacing::default(),
            top: Dimension::default(),
            left: Dimension::default(),
            bottom: Dimension::default(),
            right: Dimension::default(),
            width: Dimension::default(),
            height: Dimension::default(),
            min_width: Dimension::default(),
            max_width: Dimension::default(),
            min_height: Dimension::default(),
            max_height: Dimension::default(),
            bounding_box: Size::default(),
            flex_grow: 0.0,
            flex_shrink: 0.0,
            flex_basis: Dimension::default(),
            align_self: AlignSelf::Auto,
            align_content: AlignContent::Stretch,
            align_items: AlignItems::Stretch,
            flex_direction: FlexDirection::Row,
            justify_content: JustifyContent::FlexStart,
            position_type: PositionType::Relative,
        }
    }
}
