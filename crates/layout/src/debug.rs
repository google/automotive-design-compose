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

use taffy::{
    tree::{NodeId, TaffyTree},
    AlignContent, AlignItems, Dimension, Display, FlexDirection, FlexWrap, LengthPercentage,
    LengthPercentageAuto, Position, Style, TaffyError,
};

trait DumpAsCss {
    fn to_css_value(&self) -> String;
}
impl DumpAsCss for LengthPercentageAuto {
    fn to_css_value(&self) -> String {
        match self {
            LengthPercentageAuto::Auto => "auto".to_string(),
            LengthPercentageAuto::Length(v) => format!("{}px", v),
            LengthPercentageAuto::Percent(v) => format!("{}%", v),
        }
    }
}
impl DumpAsCss for LengthPercentage {
    fn to_css_value(&self) -> String {
        match self {
            LengthPercentage::Length(x) => format!("{}px", x),
            LengthPercentage::Percent(x) => format!("{}%", x),
        }
    }
}
impl DumpAsCss for Dimension {
    fn to_css_value(&self) -> String {
        match self {
            Dimension::Auto => "auto".to_string(),
            Dimension::Length(x) => format!("{}px", x),
            Dimension::Percent(x) => format!("{}%", x),
        }
    }
}
impl DumpAsCss for Option<AlignItems> {
    fn to_css_value(&self) -> String {
        match self {
            None => "normal",
            Some(AlignItems::Baseline) => "baseline",
            Some(AlignItems::Center) => "center",
            Some(AlignItems::End) => "end",
            Some(AlignItems::FlexEnd) => "flex-end",
            Some(AlignItems::FlexStart) => "flex-start",
            Some(AlignItems::Start) => "start",
            Some(AlignItems::Stretch) => "stretch",
        }
        .to_string()
    }
}
impl DumpAsCss for Option<AlignContent> {
    fn to_css_value(&self) -> String {
        match self {
            None => "normal",
            Some(AlignContent::Center) => "center",
            Some(AlignContent::End) => "end",
            Some(AlignContent::FlexEnd) => "flex-end",
            Some(AlignContent::FlexStart) => "flex-start",
            Some(AlignContent::SpaceAround) => "space-around",
            Some(AlignContent::SpaceBetween) => "space-between",
            Some(AlignContent::SpaceEvenly) => "space-evenly",
            Some(AlignContent::Start) => "start",
            Some(AlignContent::Stretch) => "stretch",
        }
        .to_string()
    }
}
impl DumpAsCss for FlexDirection {
    fn to_css_value(&self) -> String {
        match self {
            FlexDirection::Column => "column",
            FlexDirection::ColumnReverse => "column-reverse",
            FlexDirection::Row => "row",
            FlexDirection::RowReverse => "row-reverse",
        }
        .to_string()
    }
}
impl DumpAsCss for FlexWrap {
    fn to_css_value(&self) -> String {
        match self {
            FlexWrap::NoWrap => "nowrap",
            FlexWrap::Wrap => "wrap",
            FlexWrap::WrapReverse => "wrap-reverse",
        }
        .to_string()
    }
}
impl DumpAsCss for taffy::Display {
    fn to_css_value(&self) -> String {
        match self {
            //Display::Block => "block",
            Display::Flex => "flex",
            Display::None => "none",
            _ => "none",
        }
        .to_string()
    }
}
impl DumpAsCss for taffy::Overflow {
    fn to_css_value(&self) -> String {
        match self {
            taffy::Overflow::Hidden => "hidden",
            taffy::Overflow::Scroll => "scroll",
            taffy::Overflow::Clip => "clip",
            taffy::Overflow::Visible => "visible",
        }
        .to_string()
    }
}
impl DumpAsCss for Position {
    fn to_css_value(&self) -> String {
        match self {
            Position::Absolute => "absolute",
            Position::Relative => "relative",
        }
        .to_string()
    }
}
impl DumpAsCss for Style {
    fn to_css_value(&self) -> String {
        let mut css = Vec::new();

        css.push(format!("display: {}", self.display.to_css_value()));
        css.push(format!("overflow-x: {}", self.overflow.x.to_css_value()));
        css.push(format!("overflow-y: {}", self.overflow.y.to_css_value()));
        css.push(format!("position: {}", self.position.to_css_value()));
        css.push(format!("top: {}", self.inset.top.to_css_value()));
        css.push(format!("left: {}", self.inset.left.to_css_value()));
        css.push(format!("bottom: {}", self.inset.bottom.to_css_value()));
        css.push(format!("right: {}", self.inset.right.to_css_value()));
        css.push(format!("width: {}", self.size.width.to_css_value()));
        css.push(format!("height: {}", self.size.height.to_css_value()));
        css.push(format!("min-width: {}", self.min_size.width.to_css_value()));
        css.push(format!("min-height: {}", self.min_size.height.to_css_value()));
        css.push(format!("max-width: {}", self.max_size.width.to_css_value()));
        css.push(format!("max-height: {}", self.max_size.height.to_css_value()));
        // skip aspect-ratio
        css.push(format!("margin-top: {}", self.margin.top.to_css_value()));
        css.push(format!("margin-left: {}", self.margin.left.to_css_value()));
        css.push(format!("margin-bottom: {}", self.margin.bottom.to_css_value()));
        css.push(format!("margin-right: {}", self.margin.right.to_css_value()));
        css.push(format!("padding-top: {}", self.padding.top.to_css_value()));
        css.push(format!("padding-left: {}", self.padding.left.to_css_value()));
        css.push(format!("padding-bottom: {}", self.padding.bottom.to_css_value()));
        css.push(format!("padding-right: {}", self.padding.right.to_css_value()));
        // skip border (unused by DC)
        css.push(format!("align-items: {}", self.align_items.to_css_value()));
        css.push(format!("align-self: {}", self.align_self.to_css_value()));
        css.push(format!("align-content: {}", self.align_content.to_css_value()));
        // skip justify_items, justify_self (CSS grid, unused by DC)
        css.push(format!("justify-content: {}", self.justify_content.to_css_value()));
        css.push(format!(
            "gap: {} {}",
            self.gap.height.to_css_value(),
            self.gap.width.to_css_value()
        ));
        css.push(format!("flex-direction: {}", self.flex_direction.to_css_value()));
        css.push(format!("flex-wrap: {}", self.flex_wrap.to_css_value()));
        css.push(format!("flex-basis: {}", self.flex_basis.to_css_value()));
        css.push(format!("flex-grow: {}", self.flex_grow));
        css.push(format!("flex-shrink: {}", self.flex_shrink));

        css.join("; ")
    }
}

fn dump_node_as_html<T>(
    tree: &TaffyTree<T>,
    node_id: NodeId,
    depth: String,
) -> Result<String, TaffyError> {
    let css = tree.style(node_id)?.to_css_value();
    let mut output = Vec::new();
    output.push(format!("{}<div style=\"{}\">", depth, css));
    for child_id in tree.children(node_id)? {
        output.push(dump_node_as_html(tree, child_id, format!("{} ", depth))?);
    }
    output.push(format!("{}</div>", depth));
    Ok(output.join("\n"))
}

pub(crate) fn print_tree_as_html<T>(
    tree: &TaffyTree<T>,
    root_node_id: NodeId,
    print_func: fn(String) -> (),
) {
    // We use a simple prelude that outlines boxes in red.
    print_func("<html><style>div {{ box-sizing: border-box; border: 1px solid red; }}; body {{ margin: 20px; }}</style><body>".to_string());
    print_func(dump_node_as_html(tree, root_node_id, String::new()).unwrap());
    print_func("</body></html>".to_string());
}
