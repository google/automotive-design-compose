// Copyright 2026 Google LLC
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

use dc_squoosh_animation::Easing;
use serde::{de, Deserialize, Serialize};
use std::collections::HashMap;

#[derive(Debug, Clone, PartialEq, Copy, Serialize)]
pub struct Rgba {
    pub r: u8,
    pub g: u8,
    pub b: u8,
    pub a: u8,
}

impl<'de> Deserialize<'de> for Rgba {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        struct RgbaVisitor;

        impl<'de> de::Visitor<'de> for RgbaVisitor {
            type Value = Rgba;

            fn expecting(&self, formatter: &mut std::fmt::Formatter) -> std::fmt::Result {
                formatter.write_str("a hex string or a map with r, g, b, a")
            }

            fn visit_str<E>(self, value: &str) -> Result<Self::Value, E>
            where
                E: de::Error,
            {
                Rgba::from_hex(value).map_err(E::custom)
            }

            fn visit_map<A>(self, mut map: A) -> Result<Self::Value, A::Error>
            where
                A: de::MapAccess<'de>,
            {
                let mut r = None;
                let mut g = None;
                let mut b = None;
                let mut a = None;

                while let Some(key) = map.next_key::<String>()? {
                    match key.as_str() {
                        "r" => r = Some(map.next_value()?),
                        "g" => g = Some(map.next_value()?),
                        "b" => b = Some(map.next_value()?),
                        "a" => a = Some(map.next_value()?),
                        _ => {
                            let _: serde_json::Value = map.next_value()?;
                        } // Ignore unknown
                    }
                }

                let r = r.unwrap_or(0);
                let g = g.unwrap_or(0);
                let b = b.unwrap_or(0);
                let a = a.unwrap_or(255);

                Ok(Rgba { r, g, b, a })
            }
        }

        deserializer.deserialize_any(RgbaVisitor)
    }
}

impl Rgba {
    pub fn from_hex(hex: &str) -> Result<Self, String> {
        let cleaned = hex.trim_start_matches('#');
        if cleaned.len() == 6 {
            let r = u8::from_str_radix(&cleaned[0..2], 16)
                .map_err(|e| format!("Invalid Red channel: {}", e))?;
            let g = u8::from_str_radix(&cleaned[2..4], 16)
                .map_err(|e| format!("Invalid Green channel: {}", e))?;
            let b = u8::from_str_radix(&cleaned[4..6], 16)
                .map_err(|e| format!("Invalid Blue channel: {}", e))?;
            Ok(Rgba { r, g, b, a: 255 })
        } else if cleaned.len() == 8 {
            let r = u8::from_str_radix(&cleaned[0..2], 16)
                .map_err(|e| format!("Invalid Red channel: {}", e))?;
            let g = u8::from_str_radix(&cleaned[2..4], 16)
                .map_err(|e| format!("Invalid Green channel: {}", e))?;
            let b = u8::from_str_radix(&cleaned[4..6], 16)
                .map_err(|e| format!("Invalid Blue channel: {}", e))?;
            let a = u8::from_str_radix(&cleaned[6..8], 16)
                .map_err(|e| format!("Invalid Alpha channel: {}", e))?;
            Ok(Rgba { r, g, b, a })
        } else {
            Err(format!("Invalid hex length: {}", hex))
        }
    }

    // Linear interpolation for colors
    pub fn lerp(&self, other: &Rgba, t: f32) -> Rgba {
        Rgba {
            r: (self.r as f32 + (other.r as f32 - self.r as f32) * t) as u8,
            g: (self.g as f32 + (other.g as f32 - self.g as f32) * t) as u8,
            b: (self.b as f32 + (other.b as f32 - self.b as f32) * t) as u8,
            a: (self.a as f32 + (other.a as f32 - self.a as f32) * t) as u8,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct GradientStop {
    pub position: f32,
    pub color: Rgba,
}

#[derive(Debug, Clone, PartialEq, Copy, Serialize, Deserialize)]
pub struct ArcData {
    #[serde(rename = "startingAngle")]
    pub starting_angle: f32,
    #[serde(rename = "endingAngle")]
    pub ending_angle: f32,
    #[serde(rename = "innerRadius")]
    pub inner_radius: f32,
}

// Optimized Enum: Uses f32 for rendering, Fixed arrays, Pre-parsed Colors
/// Represents the value of a property at a specific keyframe.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(untagged)]
pub enum KeyframeValue {
    /// Four corner radii [TL, TR, BR, BL].
    CornerRadii([f32; 4]),
    /// A single scalar value (e.g., x, y, width, opacity).
    Scalar(f32),
    /// A solid color in RGBA.
    Color(Rgba),
    /// A gradient with multiple stops.
    Gradient(Vec<GradientStop>),
    /// Arc data for ellipses.
    Arc(ArcData),
    None, // Fallback
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct ParsedKeyframe {
    pub fraction: f32,
    pub value: KeyframeValue,
    #[serde(default)]
    pub easing: Easing,
}

/// Represents a parsed timeline of custom keyframes for a single property.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct ParsedTimelineData {
    #[serde(rename = "targetEasing")]
    pub target_easing: Easing,
    pub keyframes: Vec<ParsedKeyframe>,
}

// --- Parsing Logic ---

impl ParsedTimelineData {
    /// Parses an escaped JSON payload describing a custom timeline.
    /// Legacy base64 and pipe-separated payload structures are no longer supported.
    pub fn parse(encoded_data: &str) -> Result<dc_squoosh_animation::ParsedTimelineData, String> {
        if let Ok(mut parsed) = serde_json::from_str::<ParsedTimelineData>(encoded_data) {
            // Ensure keyframes are sorted sequentially
            parsed.keyframes.sort_by(|a, b| {
                a.fraction.partial_cmp(&b.fraction).unwrap_or(std::cmp::Ordering::Equal)
            });
            Ok(parsed.into())
        } else {
            Err("Invalid JSON payload: custom timelines must be encoded as pure JSON".to_string())
        }
    }
}

impl From<Rgba> for dc_squoosh_animation::Rgba {
    fn from(r: Rgba) -> Self {
        dc_squoosh_animation::Rgba { r: r.r, g: r.g, b: r.b, a: r.a }
    }
}

impl From<GradientStop> for dc_squoosh_animation::GradientStop {
    fn from(g: GradientStop) -> Self {
        dc_squoosh_animation::GradientStop { position: g.position, color: g.color.into() }
    }
}

impl From<ArcData> for dc_squoosh_animation::ArcData {
    fn from(a: ArcData) -> Self {
        dc_squoosh_animation::ArcData {
            starting_angle: a.starting_angle,
            ending_angle: a.ending_angle,
            inner_radius: a.inner_radius,
        }
    }
}

impl From<KeyframeValue> for dc_squoosh_animation::KeyframeValue {
    fn from(k: KeyframeValue) -> Self {
        match k {
            KeyframeValue::CornerRadii(v) => dc_squoosh_animation::KeyframeValue::CornerRadii(v),
            KeyframeValue::Scalar(v) => dc_squoosh_animation::KeyframeValue::Scalar(v),
            KeyframeValue::Color(v) => dc_squoosh_animation::KeyframeValue::Color(v.into()),
            KeyframeValue::Gradient(v) => dc_squoosh_animation::KeyframeValue::Gradient(
                v.into_iter().map(Into::into).collect(),
            ),
            KeyframeValue::Arc(v) => dc_squoosh_animation::KeyframeValue::Arc(v.into()),
            KeyframeValue::None => dc_squoosh_animation::KeyframeValue::None,
        }
    }
}

impl From<ParsedKeyframe> for dc_squoosh_animation::ParsedKeyframe {
    fn from(k: ParsedKeyframe) -> Self {
        dc_squoosh_animation::ParsedKeyframe {
            fraction: k.fraction,
            value: k.value.into(),
            easing: k.easing,
        }
    }
}

impl From<ParsedTimelineData> for dc_squoosh_animation::ParsedTimelineData {
    fn from(t: ParsedTimelineData) -> Self {
        dc_squoosh_animation::ParsedTimelineData {
            target_easing: t.target_easing,
            keyframes: t.keyframes.into_iter().map(Into::into).collect(),
        }
    }
}

// --- DTOs for reading the big JSON ---

#[derive(Serialize, Deserialize, Debug)]
pub struct VariantAnimation {
    #[serde(rename = "customKeyframeData", default)]
    pub custom_keyframe_data: Option<HashMap<String, String>>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct Variant {
    pub name: String,
    pub animation: Option<VariantAnimation>,
}

// --- Property Lookup ---

pub use dc_squoosh_animation::{AnimatableProperty, NodeTimelines};

use std::sync::Arc;

/// Wrapper around dc_squoosh_animation::PropertyLookup for parsing.
#[derive(Clone, Debug, PartialEq)]
pub struct PropertyLookup(pub dc_squoosh_animation::PropertyLookup);

impl PropertyLookup {
    /// Creates a new lookup from a raw hashmap.
    pub fn from_map(data: &HashMap<String, String>) -> Self {
        let mut temp_timelines: HashMap<String, NodeTimelines> = HashMap::new();
        for (key, val) in data {
            let mut clean_key = key.as_str();
            if let Some(custom_idx) = clean_key.find("-custom-") {
                clean_key = &clean_key[0..custom_idx];
            }

            if let Some((node, prop)) = clean_key.rsplit_once('-') {
                let property = prop.parse::<AnimatableProperty>().unwrap();
                if let Ok(parsed) = ParsedTimelineData::parse(val) {
                    temp_timelines
                        .entry(node.to_string())
                        .or_insert_with(NodeTimelines::default)
                        .insert(property, parsed);
                }
            }
        }

        let mut timelines = HashMap::new();
        for (k, v) in temp_timelines {
            timelines.insert(k, Arc::new(v));
        }

        PropertyLookup(dc_squoosh_animation::PropertyLookup { timelines })
    }

    /// Creates a new lookup from a parsed Variant.
    pub fn new(variant: &Variant) -> Self {
        if let Some(anim) = &variant.animation {
            if let Some(data) = &anim.custom_keyframe_data {
                return Self::from_map(data);
            }
        }
        PropertyLookup(dc_squoosh_animation::PropertyLookup { timelines: HashMap::new() })
    }

    /// Retrieves all parsed timeline data for a specific node.
    pub fn get_for_node(&self, node: &str) -> Option<Arc<NodeTimelines>> {
        self.0.timelines.get(node).cloned()
    }

    /// Retrieves the parsed timeline data for a specific node and property.
    pub fn get(
        &self,
        node: &str,
        prop: AnimatableProperty,
    ) -> Option<&dc_squoosh_animation::ParsedTimelineData> {
        self.0.timelines.get(node).and_then(|nt| nt.get(&prop))
    }

    /// Returns the inner clean struct.
    pub fn into_inner(self) -> dc_squoosh_animation::PropertyLookup {
        self.0
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_optimized_interpolation() {
        // Test Color Interpolation (0-Alloc)
        let c1 = Rgba::from_hex("#000000").unwrap();
        let c2 = Rgba::from_hex("#ffffff").unwrap();
        let mid = c1.lerp(&c2, 0.5);
        assert_eq!(mid.r, 127); // Approx

        // Test Parsing & Interpolation
        let json_payload = r##"{"targetEasing":"Linear","keyframes":[{"fraction":0.5,"value":[50.0,50.0,50.0,50.0],"easing":"Linear"}]}"##;
        let timeline = ParsedTimelineData::parse(json_payload).unwrap();

        let start = KeyframeValue::CornerRadii([0.0, 0.0, 0.0, 0.0]);
        let end = KeyframeValue::CornerRadii([100.0, 100.0, 100.0, 100.0]);

        let val_25 = timeline.interpolate(&start.clone().into(), &end.clone().into(), 0.25);
        if let dc_squoosh_animation::KeyframeValue::CornerRadii(v) = val_25 {
            assert!((v[0] - 25.0).abs() < 0.1);
        } else {
            panic!("Wrong type");
        }

        let val_75 = timeline.interpolate(&start.into(), &end.into(), 0.75);
        if let dc_squoosh_animation::KeyframeValue::CornerRadii(v) = val_75 {
            assert!((v[0] - 75.0).abs() < 0.1);
        } else {
            panic!("Wrong type");
        }
    }

    #[test]
    fn test_json_parsing_string_value() {
        let json_str = r##"{"targetEasing":"Inherit","keyframes":[{"fraction":0,"value":"#FF0000"},{"fraction":1,"value":"#00FF00"}]}"##;
        let parsed: Result<ParsedTimelineData, _> = serde_json::from_str(json_str);
        assert!(parsed.is_ok(), "Failed to parse JSON: {:?}", parsed.err());
        let data = parsed.unwrap();
        assert_eq!(data.keyframes.len(), 2);
        assert_eq!(
            data.keyframes[0].value,
            KeyframeValue::Color(Rgba { r: 255, g: 0, b: 0, a: 255 })
        );
    }

    #[test]
    fn test_easings() {
        assert_eq!(Easing::Inherit.apply(0.5), 0.5);
        assert_eq!(Easing::Linear.apply(0.5), 0.5);
        assert_eq!(Easing::Instant.apply(0.5), 0.0);
        assert_eq!(Easing::Instant.apply(1.0), 1.0);
        assert_eq!(Easing::EaseIn.apply(0.5), 0.25);
        assert_eq!(Easing::EaseOut.apply(0.5), 0.75);
        assert_eq!(Easing::EaseInOut.apply(0.25), 0.125);
        assert_eq!(Easing::EaseInOut.apply(0.75), 0.875);
        assert_eq!(Easing::EaseInCubic.apply(0.5), 0.125);
        assert_eq!(Easing::EaseOutCubic.apply(0.5), 0.875);
        assert_eq!(Easing::EaseInOutCubic.apply(0.25), 0.0625);
        assert_eq!(Easing::EaseInOutCubic.apply(0.75), 0.9375);
    }

    #[test]
    fn test_rgba_from_hex() {
        let r1 = Rgba::from_hex("#FF0000").unwrap();
        assert_eq!(r1, Rgba { r: 255, g: 0, b: 0, a: 255 });
        let r2 = Rgba::from_hex("#FF0000AA").unwrap();
        assert_eq!(r2, Rgba { r: 255, g: 0, b: 0, a: 170 });
        let r3 = Rgba::from_hex("invalid");
        assert!(r3.is_err());
        let r4 = Rgba::from_hex("123456").unwrap(); // No #
        assert_eq!(r4, Rgba { r: 18, g: 52, b: 86, a: 255 });
    }

    #[test]
    fn test_rgba_deserialize() {
        let json_str = r##""#FF0000""##;
        let c: Rgba = serde_json::from_str(json_str).unwrap();
        assert_eq!(c, Rgba { r: 255, g: 0, b: 0, a: 255 });

        let json_map = r##"{"r": 255, "g": 0, "b": 0, "a": 128}"##;
        let c2: Rgba = serde_json::from_str(json_map).unwrap();
        assert_eq!(c2, Rgba { r: 255, g: 0, b: 0, a: 128 });

        let json_map_partial = r##"{"r": 255}"##;
        let c3: Rgba = serde_json::from_str(json_map_partial).unwrap();
        assert_eq!(c3, Rgba { r: 255, g: 0, b: 0, a: 255 });
    }

    #[test]
    fn test_property_lookup() {
        let mut map = HashMap::new();
        map.insert("Node1-opacity".to_string(), r##"{"targetEasing":"Linear","keyframes":[{"fraction":0.5,"value":1.0,"easing":"Linear"}]}"##.to_string());
        let lookup = PropertyLookup::from_map(&map);
        let nt = lookup.get_for_node("Node1").unwrap();
        assert!(nt.get(&AnimatableProperty::Opacity).is_some());

        let mut map2 = HashMap::new();
        map2.insert("Node1-opacity-custom-id".to_string(), r##"{"targetEasing":"Linear","keyframes":[{"fraction":0.5,"value":1.0,"easing":"Linear"}]}"##.to_string());
        let lookup2 = PropertyLookup::from_map(&map2);
        let nt2 = lookup2.get_for_node("Node1").unwrap();
        assert!(nt2.get(&AnimatableProperty::Opacity).is_some());
    }
}
