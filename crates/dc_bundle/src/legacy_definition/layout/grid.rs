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
use serde::{Deserialize, Serialize};

use crate::legacy_definition::proto;
use crate::Error;

#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub enum ItemSpacing {
    Fixed(i32),
    // Fixed space between columns/rows
    Auto(i32, i32), // Min space between columns/rows, item width/height
}

impl Default for ItemSpacing {
    fn default() -> Self {
        ItemSpacing::Fixed(0)
    }
}

impl TryFrom<proto::layout::ItemSpacing> for ItemSpacing {
    type Error = Error;

    fn try_from(proto: proto::layout::ItemSpacing) -> Result<Self, Self::Error> {
        match proto
            .r#type
            .as_ref()
            .ok_or(Error::MissingFieldError { field: "ItemSpacing".to_string() })?
        {
            proto::layout::item_spacing::Type::Fixed(s) => Ok(ItemSpacing::Fixed(*s)),
            proto::layout::item_spacing::Type::Auto(s) => Ok(ItemSpacing::Auto(s.width, s.height)),
        }
    }
}
