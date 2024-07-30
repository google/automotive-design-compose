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

#[derive(Serialize, Deserialize, Clone, Debug, Default, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct RotationMeterData {
    pub enabled: bool,
    pub start: f32,
    pub end: f32,
    pub discrete: bool,
    pub discrete_value: f32,
}

#[derive(Serialize, Deserialize, Clone, Debug, Default, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct ArcMeterData {
    pub enabled: bool,
    pub start: f32,
    pub end: f32,
    pub discrete: bool,
    pub discrete_value: f32,
    pub corner_radius: f32,
}

#[derive(Serialize, Deserialize, Clone, Debug, Default, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct ProgressBarMeterData {
    pub enabled: bool,
    pub discrete: bool,
    pub discrete_value: f32,
    #[serde(default)]
    pub vertical: bool,
    #[serde(default)]
    pub end_x: f32,
    #[serde(default)]
    pub end_y: f32,
}

#[derive(Serialize, Deserialize, Clone, Debug, Default, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct ProgressMarkerMeterData {
    pub enabled: bool,
    pub discrete: bool,
    pub discrete_value: f32,
    #[serde(default)]
    pub vertical: bool,
    #[serde(default)]
    pub start_x: f32,
    #[serde(default)]
    pub end_x: f32,
    #[serde(default)]
    pub start_y: f32,
    #[serde(default)]
    pub end_y: f32,
}

// Schema for progress vector data that we write to serialized data
#[derive(Serialize, Deserialize, Clone, Debug, Default, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct ProgressVectorMeterData {
    pub enabled: bool,
    pub discrete: bool,
    pub discrete_value: f32,
}

// Schema for dials & gauges data that we write to serialized data
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub enum MeterData {
    ArcData(ArcMeterData),
    RotationData(RotationMeterData),
    ProgressBarData(ProgressBarMeterData),
    ProgressMarkerData(ProgressMarkerMeterData),
    ProgressVectorData(ProgressVectorMeterData),
}
