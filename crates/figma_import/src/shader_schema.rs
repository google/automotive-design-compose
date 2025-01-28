/*
 * Copyright 2025 Google LLC
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
use crate::figma_schema::FigmaColor;
use dc_bundle::definition::view::shader_uniform_value::FloatVec;
use dc_bundle::definition::view::shader_uniform_value::ValueType::{
    FloatColorValue, FloatValue, FloatVecValue, IntValue,
};
use dc_bundle::definition::view::{ShaderUniform, ShaderUniformValue};
use log::error;
use serde::{Deserialize, Serialize};

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct ShaderUniformJson {
    #[serde(rename = "uniformName")]
    pub uniform_name: String,
    #[serde(rename = "uniformType")]
    pub uniform_type: String,
    #[serde(rename = "uniformValue")]
    pub uniform_value: serde_json::Value,
}

impl Into<(String, ShaderUniform)> for ShaderUniformJson {
    fn into(self) -> (String, ShaderUniform) {
        let uniform_value = match self.uniform_type.as_str() {
            "float" | "iTime" => {
                if let Some(float_val) = self.uniform_value.as_f64() {
                    Some(ShaderUniformValue { value_type: Some(FloatValue(float_val as f32)) })
                } else {
                    error!("Error parsing float for shader float uniform {}", self.uniform_name);
                    None
                }
            }
            "float2" | "float3" | "float4" => {
                if let Some(uniform_array) = self.uniform_value.as_array() {
                    let float_array: Vec<f32> = uniform_array
                        .iter()
                        .filter_map(|value| value.as_f64().map(|v| v as f32))
                        .collect();
                    match float_array.len() {
                        2 if self.uniform_type == "float2" => Some(float_array),
                        3 if self.uniform_type == "float3" => Some(float_array),
                        4 if self.uniform_type == "float4" => Some(float_array),
                        _ => None,
                    }
                    .map(|float_vec| ShaderUniformValue {
                        value_type: Some(FloatVecValue(FloatVec { floats: float_vec })),
                    })
                } else {
                    error!(
                        "Error parsing float array for shader {} uniform {}",
                        self.uniform_type, self.uniform_name
                    );
                    None
                }
            }
            "color3" | "color4" => {
                serde_json::from_str::<FigmaColor>(self.uniform_value.to_string().as_str())
                    .ok()
                    .map(|figma_color| (&figma_color).into())
                    .map(|parsed_color| ShaderUniformValue {
                        value_type: Some(FloatColorValue(parsed_color)),
                    })
            }
            "int" => {
                if let Some(int_val) = self.uniform_value.as_i64() {
                    Some(ShaderUniformValue { value_type: Some(IntValue(int_val as i32)) })
                } else {
                    error!("Error parsing integer for shader int uniform {}", self.uniform_name);
                    None
                }
            }
            _ => None,
        };

        (
            self.uniform_name.clone(),
            ShaderUniform {
                name: self.uniform_name.clone(),
                r#type: self.uniform_type,
                value: uniform_value,
            },
        )
    }
}
