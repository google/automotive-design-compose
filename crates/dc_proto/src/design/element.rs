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

include!(concat!(env!("OUT_DIR"), "/com.android.designcompose.proto.design.element.rs"));
// Re-export the one-of types
pub use crate::design::element::variable_alias_or_list_msg::VariableAliasOrList;


use crate::design::element::variable_alias_or_list_msg::VariableAliasOrList::{Alias, List};

impl VariableAliasOrList {
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

impl NumOrVar {
    pub(crate) fn from_var(
        bound_variables: &figma_schema::BoundVariables,
        var_name: &str,
        num: f32,
    ) -> NumOrVar {
        let var = bound_variables.get_variable(var_name);
        if let Some(var) = var {
            NumOrVar::Var(var)
        } else {
            NumOrVar::Num(num)
        }
    }
}