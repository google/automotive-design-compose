/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use crate::{
    filter::{filter_op, FilterOp},
    shadow::{box_shadow, BoxShadow, ShadowBox},
    variable::ColorOrVar,
};

pub mod affine_transform;
pub mod layout_transform;

impl FilterOp {
    pub fn new_with_op(op_type: filter_op::FilterOpType) -> Self {
        FilterOp { FilterOpType: Some(op_type), ..Default::default() }
    }
}

impl BoxShadow {
    /// Create an outset box shadow.
    pub fn new_with_outset(
        blur_radius: f32,
        spread_radius: f32,
        color: ColorOrVar,
        offset: (f32, f32),
    ) -> BoxShadow {
        BoxShadow {
            shadow_box: Some(box_shadow::Shadow_box::Outset(box_shadow::Shadow {
                blur_radius,
                spread_radius,
                color: Some(color).into(),
                offset_x: offset.0,
                offset_y: offset.1,
                shadow_box: ShadowBox::SHADOW_BOX_BORDER_BOX.into(),
                ..Default::default()
            })),
            ..Default::default()
        }
    }
    /// Create an inset shadow.
    pub fn new_with_inset(
        blur_radius: f32,
        spread_radius: f32,
        color: ColorOrVar,
        offset: (f32, f32),
    ) -> BoxShadow {
        BoxShadow {
            shadow_box: Some(box_shadow::Shadow_box::Inset(box_shadow::Shadow {
                blur_radius,
                spread_radius,
                color: Some(color).into(),
                offset_x: offset.0,
                offset_y: offset.1,
                shadow_box: ShadowBox::SHADOW_BOX_BORDER_BOX.into(),
                ..Default::default()
            })),
            ..Default::default()
        }
    }
}
