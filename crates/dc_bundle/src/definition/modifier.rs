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
use crate::definition::element::ColorOrVar;

include!(concat!(env!("OUT_DIR"), "/designcompose.definition.modifier.rs"));
pub mod affine_transform;
pub mod layout_transform;

impl FilterOp {
    pub fn new(op_type: filter_op::FilterOpType) -> Self {
        FilterOp { filter_op_type: Some(op_type) }
    }
}

impl BoxShadow {
    /// Create an outset box shadow.
    pub fn outset(
        blur_radius: f32,
        spread_radius: f32,
        color: ColorOrVar,
        offset: (f32, f32),
    ) -> BoxShadow {
        BoxShadow {
            shadow_box: Some(box_shadow::ShadowBox::Outset(box_shadow::Shadow {
                blur_radius,
                spread_radius,
                color: Some(color),
                offset_x: offset.0,
                offset_y: offset.1,
                shadow_box: ShadowBox::BorderBox.into(),
            })),
        }
    }
    /// Create an inset shadow.
    pub fn inset(
        blur_radius: f32,
        spread_radius: f32,
        color: ColorOrVar,
        offset: (f32, f32),
    ) -> BoxShadow {
        BoxShadow {
            shadow_box: Some(box_shadow::ShadowBox::Inset(box_shadow::Shadow {
                blur_radius,
                spread_radius,
                color: Some(color),
                offset_x: offset.0,
                offset_y: offset.1,
                shadow_box: ShadowBox::BorderBox.into(),
            })),
        }
    }
}
