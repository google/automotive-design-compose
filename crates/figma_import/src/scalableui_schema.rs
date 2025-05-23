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

use dc_bundle::scalable;
use dc_bundle::scalable::ScalableUIComponentSet;
use serde::{Deserialize, Serialize};

//
// Schema data for component sets
//

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
struct Event {
    event_name: String,
    event_tokens: String,
    from_variant_id: String,
    from_variant_name: String,
    to_variant_id: String,
    to_variant_name: String,
}

impl Into<scalable::Event> for &Event {
    fn into(self) -> scalable::Event {
        scalable::Event {
            event_name: self.event_name.clone(),
            event_tokens: self.event_tokens.clone(),
            from_variant_id: self.from_variant_id.clone(),
            from_variant_name: self.from_variant_name.clone(),
            to_variant_id: self.to_variant_id.clone(),
            to_variant_name: self.to_variant_name.clone(),
            ..Default::default()
        }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
struct Keyframe {
    frame: i32,
    variant_name: String,
}

impl Into<scalable::Keyframe> for &Keyframe {
    fn into(self) -> scalable::Keyframe {
        scalable::Keyframe {
            frame: self.frame,
            variant_name: self.variant_name.clone(),
            ..Default::default()
        }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
struct KeyframeVariant {
    name: String,
    keyframes: Vec<Keyframe>,
}

impl Into<scalable::KeyframeVariant> for &KeyframeVariant {
    fn into(self) -> scalable::KeyframeVariant {
        scalable::KeyframeVariant {
            name: self.name.clone(),
            keyframes: self.keyframes.iter().map(|kf| kf.into()).collect(),
            ..Default::default()
        }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ComponentSetDataJson {
    id: String,
    name: String,
    role: String,
    default_variant_id: String,
    default_variant_name: String,
    event_list: Vec<Event>,
    keyframe_variants: Vec<KeyframeVariant>,
}

impl Into<ScalableUIComponentSet> for ComponentSetDataJson {
    fn into(self) -> ScalableUIComponentSet {
        ScalableUIComponentSet {
            id: self.id,
            name: self.name,
            role: self.role,
            default_variant_id: self.default_variant_id,
            default_variant_name: self.default_variant_name,
            events: self.event_list.iter().map(|e| e.into()).collect(),
            keyframe_variants: self.keyframe_variants.iter().map(|kfv| kfv.into()).collect(),
            variant_ids: vec![],
            ..Default::default()
        }
    }
}

//
// Schema data for variants
//

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(rename_all = "camelCase")]
pub(crate) struct VariantDataJson {
    id: String,
    name: String,
    is_default: bool,
    layer: i32,
}

impl Into<scalable::ScalableUiVariant> for VariantDataJson {
    fn into(self) -> scalable::ScalableUiVariant {
        scalable::ScalableUiVariant {
            id: self.id,
            name: self.name,
            is_default: self.is_default,
            is_visible: true,
            bounds: None.into(),
            alpha: 1.0,
            layer: self.layer,
            ..Default::default()
        }
    }
}

//
// ScalableUiDataJson represents the schema for any node that has scalable ui data
//
#[derive(Deserialize, Serialize, Debug, Clone)]
#[serde(untagged)]
pub(crate) enum ScalableUiDataJson {
    Set(ComponentSetDataJson),
    Variant(VariantDataJson),
}

impl Into<scalable::ScalableUIData> for ScalableUiDataJson {
    fn into(self) -> scalable::ScalableUIData {
        scalable::ScalableUIData {
            data: Some(match self {
                ScalableUiDataJson::Set(set) => scalable::scalable_uidata::Data::Set(set.into()),
                ScalableUiDataJson::Variant(var) => {
                    scalable::scalable_uidata::Data::Variant(var.into())
                }
            }),
            ..Default::default()
        }
    }
}
