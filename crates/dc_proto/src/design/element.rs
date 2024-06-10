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

use crate::design::element::variable_alias_or_list::VariableAliasOrList::{Alias, List};
include!(concat!(env!("OUT_DIR"), "/com.android.designcompose.proto.design.element.rs"));

impl variable_alias_or_list::VariableAliasOrList {
    pub fn get_name(&self) -> Option<String> {
        match self {
            Alias(alias) => {
                return Some(alias.id.clone());
            }
            List(list) => {
                let alias = list.list.first();
                if let Some(alias) = alias {
                    return Some(alias.id.clone());
                }
            }
        }
        None
    }
}

