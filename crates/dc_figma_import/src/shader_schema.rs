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
use crate::image_context::ImageContext;
use dc_bundle::color::Color;
use dc_bundle::shader::shader_uniform_value::Value_type::{
    FloatColorValue, FloatValue, FloatVecValue, ImageRefValue, IntValue, IntVecValue,
};
use dc_bundle::shader::shader_uniform_value::{FloatVec, ImageRef, IntVec};
use dc_bundle::shader::{ShaderData, ShaderUniform, ShaderUniformValue};
use log::error;
use protobuf::MessageField;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct ShaderDataJson {
    pub shader: Option<String>,
    #[serde(rename = "shaderFallbackColor")]
    pub shader_fallback_color: Option<FigmaColor>,
    #[serde(rename = "shaderUniforms")]
    pub shader_uniforms: Vec<ShaderUniformJson>,
}

impl ShaderDataJson {
    pub fn into_shader_data(self, images: &mut ImageContext) -> Option<ShaderData> {
        return if let Some(shader) = self.shader {
            // Shader fallback color is the color used when shader isn't supported on lower sdks.
            let shader_fallback_color: MessageField<Color> =
                self.shader_fallback_color.as_ref().map(|figma_color| figma_color.into()).into();
            // Shader uniforms: float, float array, color and color with alpha
            let shader_uniforms: HashMap<String, ShaderUniform> = self
                .shader_uniforms
                .into_iter()
                .map(|json| json.into_shader_uniform(images))
                .collect();
            Some(ShaderData {
                shader,
                shader_fallback_color,
                shader_uniforms,
                ..Default::default()
            })
        } else {
            None
        };
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct ShaderUniformJson {
    #[serde(rename = "uniformName")]
    pub uniform_name: String,
    #[serde(rename = "uniformType")]
    pub uniform_type: String,
    #[serde(rename = "uniformValue")]
    pub uniform_value: serde_json::Value,
    pub extras: Option<ShaderExtrasJson>,
}

impl ShaderUniformJson {
    pub fn into_shader_uniform(self, images: &mut ImageContext) -> (String, ShaderUniform) {
        let uniform_value = match self.uniform_type.as_str() {
            "float" | "half" | "iTime" => {
                if let Some(float_val) = self.uniform_value.as_f64() {
                    Some(ShaderUniformValue {
                        value_type: Some(FloatValue(float_val as f32)),
                        ..Default::default()
                    })
                } else {
                    error!("Error parsing float for shader float uniform {}", self.uniform_name);
                    None
                }
            }
            "float2" | "float3" | "float4" | "half2" | "half3" | "half4" | "mat2" | "mat3"
            | "mat4" | "half2x2" | "half3x3" | "half4x4" => {
                if let Some(uniform_array) = self.uniform_value.as_array() {
                    let float_array: Vec<f32> = uniform_array
                        .iter()
                        .filter_map(|value| value.as_f64().map(|v| v as f32))
                        .collect();
                    match float_array.len() {
                        2 if self.uniform_type == "float2" || self.uniform_type == "half2" => {
                            Some(float_array)
                        }
                        3 if self.uniform_type == "float3" || self.uniform_type == "half3" => {
                            Some(float_array)
                        }
                        4 if self.uniform_type == "float4"
                            || self.uniform_type == "half4"
                            || self.uniform_type == "mat2"
                            || self.uniform_type == "half2x2" =>
                        {
                            Some(float_array)
                        }
                        9 if self.uniform_type == "mat3" || self.uniform_type == "half3x3" => {
                            Some(float_array)
                        }
                        16 if self.uniform_type == "mat4" || self.uniform_type == "half4x4" => {
                            Some(float_array)
                        }
                        _ => None,
                    }
                    .map(|float_vec| ShaderUniformValue {
                        value_type: Some(FloatVecValue(FloatVec {
                            floats: float_vec,
                            ..Default::default()
                        })),
                        ..Default::default()
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
                        ..Default::default()
                    })
            }
            "int" => {
                if let Some(int_val) = self.uniform_value.as_i64() {
                    Some(ShaderUniformValue {
                        value_type: Some(IntValue(int_val as i32)),
                        ..Default::default()
                    })
                } else {
                    error!("Error parsing integer for shader int uniform {}", self.uniform_name);
                    None
                }
            }
            "int2" | "int3" | "int4" => {
                if let Some(uniform_array) = self.uniform_value.as_array() {
                    let int_array: Vec<i32> = uniform_array
                        .iter()
                        .filter_map(|value| value.as_i64().map(|v| v as i32))
                        .collect();
                    match int_array.len() {
                        2 if self.uniform_type == "int2" => Some(int_array),
                        3 if self.uniform_type == "int3" => Some(int_array),
                        4 if self.uniform_type == "int4" => Some(int_array),
                        _ => None,
                    }
                    .map(|int_vec| ShaderUniformValue {
                        value_type: Some(IntVecValue(IntVec {
                            ints: int_vec,
                            ..Default::default()
                        })),
                        ..Default::default()
                    })
                } else {
                    error!(
                        "Error parsing int array for shader {} uniform {}",
                        self.uniform_type, self.uniform_name
                    );
                    None
                }
            }
            "shader" => {
                if let Some(str_val) = self.uniform_value.as_str() {
                    // We use an empty string as the node name to skip the ignore check.
                    if let Some(fill) = images.image_fill(str_val, &"".to_string()) {
                        Some(ShaderUniformValue {
                            value_type: Some(ImageRefValue(ImageRef {
                                key: fill,
                                res_name: images.image_res(str_val),
                                ..Default::default()
                            })),
                            ..Default::default()
                        })
                    } else {
                        error!(
                            "No image found for image ref {} for shader {}",
                            str_val, self.uniform_name
                        );
                        None
                    }
                } else {
                    error!(
                        "Error parsing image key for shader image uniform {}",
                        self.uniform_name
                    );
                    None
                }
            }
            _ => None,
        };

        (
            self.uniform_name.clone(),
            ShaderUniform {
                name: self.uniform_name.clone(),
                type_: self.uniform_type,
                value: uniform_value.into(),
                ignore: self.extras.unwrap_or_default().ignore.unwrap_or(false),
                ..Default::default()
            },
        )
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct ShaderExtrasJson {
    pub ignore: Option<bool>,
}
impl Default for ShaderExtrasJson {
    fn default() -> Self {
        ShaderExtrasJson { ignore: Some(false) }
    }
}
