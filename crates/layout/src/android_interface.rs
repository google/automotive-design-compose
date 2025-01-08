use std::collections::HashMap;

include!(concat!(env!("OUT_DIR"), "/designcompose.layout_interface.rs"));

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

impl LayoutChangedResponse {
    pub fn unchanged(layout_state: i32) -> Self {
        LayoutChangedResponse { layout_state, changed_layouts: HashMap::new() }
    }
}
