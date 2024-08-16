// Copyright 2023 Google LLC
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

//! `toolkit_style` contains all of the style-related types that `toolkit_schema::View`
//! uses.

use crate::figma_schema;
use dc_bundle::legacy_definition::plugin::meter_data::{
    ArcMeterData, MeterData, ProgressBarMeterData, ProgressMarkerMeterData,
    ProgressVectorMeterData, RotationMeterData,
};
use serde::{Deserialize, Serialize};

// Schema for progress vector data that we read from Figma plugin data
#[derive(Serialize, Deserialize, Clone, Debug, Default, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct ProgressVectorMeterDataSchema {
    pub enabled: bool,
    pub discrete: bool,
    pub discrete_value: f32,
    pub paths: Vec<figma_schema::Path>,
}

// Schema for dials & gauges Figma plugin data
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub enum MeterDataSchema {
    ArcData(ArcMeterData),
    RotationData(RotationMeterData),
    ProgressBarData(ProgressBarMeterData),
    ProgressMarkerData(ProgressMarkerMeterData),
    ProgressVectorData(ProgressVectorMeterDataSchema),
}

impl Into<MeterData> for MeterDataSchema {
    fn into(self) -> MeterData {
        match self {
            MeterDataSchema::ArcData(data) => MeterData::ArcData(data),
            MeterDataSchema::RotationData(data) => MeterData::RotationData(data),
            MeterDataSchema::ProgressBarData(data) => MeterData::ProgressBarData(data),
            MeterDataSchema::ProgressMarkerData(data) => MeterData::ProgressMarkerData(data),
            MeterDataSchema::ProgressVectorData(data) => {
                MeterData::ProgressVectorData(ProgressVectorMeterData {
                    enabled: data.enabled,
                    discrete: data.discrete,
                    discrete_value: data.discrete_value,
                })
            }
        }
    }
}
