use base64::{engine::general_purpose, Engine as _};
use serde::{de, Deserialize, Serialize};
use std::collections::HashMap;

// --- High Level Runtime Types ---

/// Supported easing functions for interpolation.
#[derive(Debug, Clone, PartialEq, Copy, Serialize, Deserialize)]
pub enum Easing {
    Inherit,
    Linear,
    Instant,
    EaseIn,
    EaseOut,
    EaseInOut,
    EaseInCubic,
    EaseOutCubic,
    EaseInOutCubic,
}

impl Default for Easing {
    fn default() -> Self {
        Easing::Inherit
    }
}

impl Easing {
    /// Applies the easing function to a linear progress value `t` (0.0 to 1.0).
    pub fn apply(&self, t: f32) -> f32 {
        match self {
            Easing::Inherit => t,
            Easing::Linear => t,
            Easing::Instant => {
                if t >= 1.0 {
                    1.0
                } else {
                    0.0
                }
            }
            Easing::EaseIn => t * t,
            Easing::EaseOut => t * (2.0 - t),
            Easing::EaseInOut => {
                if t < 0.5 {
                    2.0 * t * t
                } else {
                    -1.0 + (4.0 - 2.0 * t) * t
                }
            }
            Easing::EaseInCubic => t * t * t,
            Easing::EaseOutCubic => {
                let t = t - 1.0;
                t * t * t + 1.0
            }
            Easing::EaseInOutCubic => {
                if t < 0.5 {
                    4.0 * t * t * t
                } else {
                    let t = t - 1.0;
                    4.0 * t * t * t + 1.0
                }
            } // Corrected math
        }
    }
}

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
                Ok(Rgba::from_hex(value))
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
    pub fn from_hex(hex: &str) -> Self {
        let hex = hex.trim_start_matches('#');
        if hex.len() == 6 {
            let r = u8::from_str_radix(&hex[0..2], 16).unwrap_or(0);
            let g = u8::from_str_radix(&hex[2..4], 16).unwrap_or(0);
            let b = u8::from_str_radix(&hex[4..6], 16).unwrap_or(0);
            Rgba { r, g, b, a: 255 }
        } else if hex.len() == 8 {
            let r = u8::from_str_radix(&hex[0..2], 16).unwrap_or(0);
            let g = u8::from_str_radix(&hex[2..4], 16).unwrap_or(0);
            let b = u8::from_str_radix(&hex[4..6], 16).unwrap_or(0);
            let a = u8::from_str_radix(&hex[6..8], 16).unwrap_or(255);
            Rgba { r, g, b, a }
        } else {
            Rgba { r: 0, g: 0, b: 0, a: 255 }
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

impl KeyframeValue {
    /// Interpolates between this value and another value at fraction `t`.
    pub fn interpolate(&self, other: &KeyframeValue, t: f32) -> KeyframeValue {
        match (self, other) {
            (KeyframeValue::Scalar(a), KeyframeValue::Scalar(b)) => {
                KeyframeValue::Scalar(a + (b - a) * t)
            }
            (KeyframeValue::Color(a), KeyframeValue::Color(b)) => {
                KeyframeValue::Color(a.lerp(b, t))
            }
            (KeyframeValue::CornerRadii(a), KeyframeValue::CornerRadii(b)) => {
                KeyframeValue::CornerRadii([
                    a[0] + (b[0] - a[0]) * t,
                    a[1] + (b[1] - a[1]) * t,
                    a[2] + (b[2] - a[2]) * t,
                    a[3] + (b[3] - a[3]) * t,
                ])
            }
            (KeyframeValue::Arc(a), KeyframeValue::Arc(b)) => KeyframeValue::Arc(ArcData {
                starting_angle: a.starting_angle + (b.starting_angle - a.starting_angle) * t,
                ending_angle: a.ending_angle + (b.ending_angle - a.ending_angle) * t,
                inner_radius: a.inner_radius + (b.inner_radius - a.inner_radius) * t,
            }),
            (KeyframeValue::Gradient(a), KeyframeValue::Gradient(b)) => {
                if a.len() != b.len() {
                    return KeyframeValue::Gradient(a.clone());
                }
                let stops = a
                    .iter()
                    .zip(b.iter())
                    .map(|(s1, s2)| GradientStop {
                        position: s1.position + (s2.position - s1.position) * t,
                        color: s1.color.lerp(&s2.color, t),
                    })
                    .collect();
                KeyframeValue::Gradient(stops)
            }
            _ => self.clone(),
        }
    }
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
    /// Parses a pipe-separated custom keyframe string or a JSON payload.
    pub fn parse(encoded_data: &str) -> Result<Self, String> {
        // Attempt new JSON parsing first
        if encoded_data.starts_with('{') {
            if let Ok(mut parsed) = serde_json::from_str::<ParsedTimelineData>(encoded_data) {
                // Ensure sorted
                parsed.keyframes.sort_by(|a, b| {
                    a.fraction.partial_cmp(&b.fraction).unwrap_or(std::cmp::Ordering::Equal)
                });
                return Ok(parsed);
            }
        }

        // LEGACY PARSING
        let parts: Vec<&str> = encoded_data.split('|').collect();
        if parts.is_empty() {
            return Err("Empty data".to_string());
        }

        let target_easing = parse_easing(parts[0]);
        let mut keyframes = Vec::new();

        if parts.len() > 1 && !parts[1].is_empty() {
            for kf_str in parts[1].split(';') {
                let kf_parts: Vec<&str> = kf_str.split(',').collect();
                if kf_parts.len() != 3 {
                    continue;
                }

                let fraction: f32 = kf_parts[0].parse().unwrap_or(0.0);
                let easing = parse_easing(kf_parts[2]);

                // Decode Value
                let decoded_bytes =
                    general_purpose::STANDARD.decode(kf_parts[1]).map_err(|e| e.to_string())?;
                let json_str = String::from_utf8(decoded_bytes).map_err(|e| e.to_string())?;

                let value = parse_keyframe_value(&json_str);

                keyframes.push(ParsedKeyframe { fraction, value, easing });
            }
        }

        // Ensure sorted
        keyframes.sort_by(|a, b| {
            a.fraction.partial_cmp(&b.fraction).unwrap_or(std::cmp::Ordering::Equal)
        });

        Ok(ParsedTimelineData { target_easing, keyframes })
    }

    /// Finds the surrounding keyframes and the eased fraction between them.
    /// This allows the caller to use their own interpolator on the keyframe values.
    pub fn get_keyframe_segment<'a>(
        &'a self,
        start_value: &'a KeyframeValue,
        end_value: &'a KeyframeValue,
        fraction: f32,
    ) -> (&'a KeyframeValue, &'a KeyframeValue, f32) {
        if fraction <= 0.0 {
            return (start_value, start_value, 0.0);
        }
        if fraction >= 1.0 {
            return (end_value, end_value, 1.0);
        }

        let mut kf1_fraction = 0.0;
        let mut kf1_value = start_value;
        let mut kf2_fraction = 1.0;
        let mut kf2_value = end_value;
        let mut easing = self.target_easing;

        // Optimized Linear Search (Small N)
        // Check if before first custom keyframe
        if let Some(first) = self.keyframes.first() {
            if fraction < first.fraction {
                kf2_fraction = first.fraction;
                kf2_value = &first.value;
                easing = first.easing;
            } else {
                // Check internal
                let mut found = false;
                // window over keyframes
                for i in 0..self.keyframes.len() - 1 {
                    if fraction >= self.keyframes[i].fraction
                        && fraction < self.keyframes[i + 1].fraction
                    {
                        kf1_fraction = self.keyframes[i].fraction;
                        kf1_value = &self.keyframes[i].value;
                        kf2_fraction = self.keyframes[i + 1].fraction;
                        kf2_value = &self.keyframes[i + 1].value;
                        easing = self.keyframes[i + 1].easing;
                        found = true;
                        break;
                    }
                }

                if !found {
                    // Must be after last
                    let last = self.keyframes.last().unwrap();
                    kf1_fraction = last.fraction;
                    kf1_value = &last.value;
                    kf2_fraction = 1.0;
                    kf2_value = end_value;
                    easing = self.target_easing;
                }
            }
        }

        if easing == Easing::Instant {
            if fraction < kf2_fraction {
                return (kf1_value, kf1_value, 0.0);
            } else {
                return (kf2_value, kf2_value, 1.0);
            }
        }

        let duration = kf2_fraction - kf1_fraction;
        if duration <= 0.00001 {
            return (kf2_value, kf2_value, 1.0);
        }

        let t = ((fraction - kf1_fraction) / duration).clamp(0.0, 1.0);
        let eased_t = easing.apply(t);

        (kf1_value, kf2_value, eased_t)
    }

    /// Interpolates between a start and end value at the given fraction (0.0 to 1.0),
    /// respecting the custom keyframes and easings defined in this timeline.
    pub fn interpolate(
        &self,
        start_value: &KeyframeValue,
        end_value: &KeyframeValue,
        fraction: f32,
    ) -> KeyframeValue {
        let (kf1, kf2, t) = self.get_keyframe_segment(start_value, end_value, fraction);
        kf1.interpolate(kf2, t)
    }
}

fn parse_easing(s: &str) -> Easing {
    match s {
        "Linear" => Easing::Linear,
        "EaseIn" => Easing::EaseIn,
        "EaseOut" => Easing::EaseOut,
        "EaseInOut" => Easing::EaseInOut,
        "EaseInCubic" => Easing::EaseInCubic,
        "EaseOutCubic" => Easing::EaseOutCubic,
        "EaseInOutCubic" => Easing::EaseInOutCubic,
        "Instant" => Easing::Instant,
        "Inherit" => Easing::Inherit,
        _ => Easing::Inherit, // Default -> Inherit
    }
}

fn parse_keyframe_value(json_str: &str) -> KeyframeValue {
    if let Ok(v) = serde_json::from_str::<f32>(json_str) {
        return KeyframeValue::Scalar(v);
    }
    if let Ok(v) = serde_json::from_str::<Vec<f32>>(json_str) {
        if v.len() == 4 {
            return KeyframeValue::CornerRadii([v[0], v[1], v[2], v[3]]);
        }
    }
    // Gradients (Array of objects)
    #[derive(Deserialize)]
    struct RawStop {
        position: f32,
        color: String,
    }
    if let Ok(raw_stops) = serde_json::from_str::<Vec<RawStop>>(json_str) {
        let stops = raw_stops
            .into_iter()
            .map(|s| GradientStop { position: s.position, color: Rgba::from_hex(&s.color) })
            .collect();
        return KeyframeValue::Gradient(stops);
    }
    // Arc
    #[derive(Deserialize)]
    struct RawArc {
        #[serde(rename = "startingAngle")]
        starting_angle: f32,
        #[serde(rename = "endingAngle")]
        ending_angle: f32,
        #[serde(rename = "innerRadius")]
        inner_radius: f32,
    }
    if let Ok(a) = serde_json::from_str::<RawArc>(json_str) {
        return KeyframeValue::Arc(ArcData {
            starting_angle: a.starting_angle,
            ending_angle: a.ending_angle,
            inner_radius: a.inner_radius,
        });
    }

    // Fallback: Raw Color String
    // Note: The primary persistence format is a pure escaped JSON string.
    // Base64 encoding is only maintained for legacy pipe-separated payloads
    // to prevent embedded delimiters from corrupting older exported files.
    let clean_str = json_str.trim().trim_matches('"');
    if clean_str.starts_with('#') {
        return KeyframeValue::Color(Rgba::from_hex(clean_str));
    }

    KeyframeValue::None
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

/// Represents properties that can be animated via custom timelines.
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub enum AnimatableProperty {
    Opacity,
    X,
    Y,
    Width,
    Height,
    CornerRadius,
    TopLeftRadius,
    TopRightRadius,
    BottomLeftRadius,
    BottomRightRadius,
    FillSolid(usize),
    FillGradient(usize),
    StrokeSolid(usize),
    StrokeGradient(usize),
    StrokeWeight,
    ArcData,
    Rotation,
    Other(String),
}

impl std::str::FromStr for AnimatableProperty {
    type Err = std::convert::Infallible;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "opacity" => Ok(AnimatableProperty::Opacity),
            "x" => Ok(AnimatableProperty::X),
            "y" => Ok(AnimatableProperty::Y),
            "width" => Ok(AnimatableProperty::Width),
            "height" => Ok(AnimatableProperty::Height),
            "cornerRadius" => Ok(AnimatableProperty::CornerRadius),
            "topLeftRadius" => Ok(AnimatableProperty::TopLeftRadius),
            "topRightRadius" => Ok(AnimatableProperty::TopRightRadius),
            "bottomLeftRadius" => Ok(AnimatableProperty::BottomLeftRadius),
            "bottomRightRadius" => Ok(AnimatableProperty::BottomRightRadius),
            "arcData" => Ok(AnimatableProperty::ArcData),
            "strokeWeight" => Ok(AnimatableProperty::StrokeWeight),
            "rotation" => Ok(AnimatableProperty::Rotation),
            _ => {
                if s.starts_with("fills.") {
                    let parts: Vec<&str> = s.split('.').collect();
                    if parts.len() == 3 {
                        if let Ok(idx) = parts[1].parse::<usize>() {
                            match parts[2] {
                                "solid" => return Ok(AnimatableProperty::FillSolid(idx)),
                                "gradient" => return Ok(AnimatableProperty::FillGradient(idx)),
                                _ => {}
                            }
                        }
                    }
                } else if s.starts_with("strokes.") {
                    let parts: Vec<&str> = s.split('.').collect();
                    if parts.len() == 3 {
                        if let Ok(idx) = parts[1].parse::<usize>() {
                            match parts[2] {
                                "solid" => return Ok(AnimatableProperty::StrokeSolid(idx)),
                                "gradient" => return Ok(AnimatableProperty::StrokeGradient(idx)),
                                _ => {}
                            }
                        }
                    }
                }
                Ok(AnimatableProperty::Other(s.to_string()))
            }
        }
    }
}

/// Represents all known timelines for a single node.
#[derive(Clone, Debug, PartialEq, Default)]
pub struct NodeTimelines {
    pub opacity: Option<ParsedTimelineData>,
    pub x: Option<ParsedTimelineData>,
    pub y: Option<ParsedTimelineData>,
    pub width: Option<ParsedTimelineData>,
    pub height: Option<ParsedTimelineData>,
    pub corner_radius: Option<ParsedTimelineData>,
    pub top_left_radius: Option<ParsedTimelineData>,
    pub top_right_radius: Option<ParsedTimelineData>,
    pub bottom_left_radius: Option<ParsedTimelineData>,
    pub bottom_right_radius: Option<ParsedTimelineData>,
    pub fill_solid: HashMap<usize, ParsedTimelineData>,
    pub fill_gradient: HashMap<usize, ParsedTimelineData>,
    pub stroke_solid: HashMap<usize, ParsedTimelineData>,
    pub stroke_gradient: HashMap<usize, ParsedTimelineData>,
    pub stroke_weight: Option<ParsedTimelineData>,
    pub arc_data: Option<ParsedTimelineData>,
    pub rotation: Option<ParsedTimelineData>,
    pub other: HashMap<String, ParsedTimelineData>,
}

impl NodeTimelines {
    pub fn insert(&mut self, prop: AnimatableProperty, timeline: ParsedTimelineData) {
        match prop {
            AnimatableProperty::Opacity => self.opacity = Some(timeline),
            AnimatableProperty::X => self.x = Some(timeline),
            AnimatableProperty::Y => self.y = Some(timeline),
            AnimatableProperty::Width => self.width = Some(timeline),
            AnimatableProperty::Height => self.height = Some(timeline),
            AnimatableProperty::CornerRadius => self.corner_radius = Some(timeline),
            AnimatableProperty::TopLeftRadius => self.top_left_radius = Some(timeline),
            AnimatableProperty::TopRightRadius => self.top_right_radius = Some(timeline),
            AnimatableProperty::BottomLeftRadius => self.bottom_left_radius = Some(timeline),
            AnimatableProperty::BottomRightRadius => self.bottom_right_radius = Some(timeline),
            AnimatableProperty::FillSolid(idx) => {
                self.fill_solid.insert(idx, timeline);
            }
            AnimatableProperty::FillGradient(idx) => {
                self.fill_gradient.insert(idx, timeline);
            }
            AnimatableProperty::StrokeSolid(idx) => {
                self.stroke_solid.insert(idx, timeline);
            }
            AnimatableProperty::StrokeGradient(idx) => {
                self.stroke_gradient.insert(idx, timeline);
            }
            AnimatableProperty::StrokeWeight => self.stroke_weight = Some(timeline),
            AnimatableProperty::ArcData => self.arc_data = Some(timeline),
            AnimatableProperty::Rotation => self.rotation = Some(timeline),
            AnimatableProperty::Other(name) => {
                self.other.insert(name, timeline);
            }
        }
    }

    pub fn get(&self, prop: &AnimatableProperty) -> Option<&ParsedTimelineData> {
        match prop {
            AnimatableProperty::Opacity => self.opacity.as_ref(),
            AnimatableProperty::X => self.x.as_ref(),
            AnimatableProperty::Y => self.y.as_ref(),
            AnimatableProperty::Width => self.width.as_ref(),
            AnimatableProperty::Height => self.height.as_ref(),
            AnimatableProperty::CornerRadius => self.corner_radius.as_ref(),
            AnimatableProperty::TopLeftRadius => self.top_left_radius.as_ref(),
            AnimatableProperty::TopRightRadius => self.top_right_radius.as_ref(),
            AnimatableProperty::BottomLeftRadius => self.bottom_left_radius.as_ref(),
            AnimatableProperty::BottomRightRadius => self.bottom_right_radius.as_ref(),
            AnimatableProperty::FillSolid(idx) => self.fill_solid.get(idx),
            AnimatableProperty::FillGradient(idx) => self.fill_gradient.get(idx),
            AnimatableProperty::StrokeSolid(idx) => self.stroke_solid.get(idx),
            AnimatableProperty::StrokeGradient(idx) => self.stroke_gradient.get(idx),
            AnimatableProperty::StrokeWeight => self.stroke_weight.as_ref(),
            AnimatableProperty::ArcData => self.arc_data.as_ref(),
            AnimatableProperty::Rotation => self.rotation.as_ref(),
            AnimatableProperty::Other(name) => self.other.get(name),
        }
    }
}

use std::sync::Arc;

/// Efficient lookup for parsed timeline data.
#[derive(Clone, Debug, PartialEq)]
pub struct PropertyLookup {
    /// Map of NodeName -> Arc<NodeTimelines>
    pub timelines: HashMap<String, Arc<NodeTimelines>>,
}

impl PropertyLookup {
    /// Creates a new lookup from a raw hashmap.
    pub fn from_map(data: &HashMap<String, String>) -> Self {
        let mut temp_timelines: HashMap<String, NodeTimelines> = HashMap::new();
        for (key, val) in data {
            // Keys can be "NodeName-propertyName" or "NodeName-propertyName-custom-id"
            // First, strip the "-custom-xyz" suffix if it exists.
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

        PropertyLookup { timelines }
    }

    /// Creates a new lookup from a parsed Variant.
    pub fn new(variant: &Variant) -> Self {
        if let Some(anim) = &variant.animation {
            if let Some(data) = &anim.custom_keyframe_data {
                return Self::from_map(data);
            }
        }
        PropertyLookup { timelines: HashMap::new() }
    }

    /// Retrieves all parsed timeline data for a specific node.
    pub fn get_for_node(&self, node: &str) -> Option<Arc<NodeTimelines>> {
        self.timelines.get(node).cloned()
    }

    /// Retrieves the parsed timeline data for a specific node and property.
    pub fn get(&self, node: &str, prop: AnimatableProperty) -> Option<&ParsedTimelineData> {
        self.timelines.get(node).and_then(|nt| nt.get(&prop))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_optimized_interpolation() {
        // Test Color Interpolation (0-Alloc)
        let c1 = Rgba::from_hex("#000000");
        let c2 = Rgba::from_hex("#ffffff");
        let mid = c1.lerp(&c2, 0.5);
        assert_eq!(mid.r, 127); // Approx

        // Test Parsing & Interpolation
        let encoded_radii = format!(
            "Linear|0.5,{},Linear",
            general_purpose::STANDARD.encode("[50.0, 50.0, 50.0, 50.0]")
        );
        let timeline = ParsedTimelineData::parse(&encoded_radii).unwrap();

        let start = KeyframeValue::CornerRadii([0.0, 0.0, 0.0, 0.0]);
        let end = KeyframeValue::CornerRadii([100.0, 100.0, 100.0, 100.0]);

        let val_25 = timeline.interpolate(&start, &end, 0.25);
        if let KeyframeValue::CornerRadii(v) = val_25 {
            assert!((v[0] - 25.0).abs() < 0.1);
        } else {
            panic!("Wrong type");
        }

        let val_75 = timeline.interpolate(&start, &end, 0.75);
        if let KeyframeValue::CornerRadii(v) = val_75 {
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
        let r1 = Rgba::from_hex("#FF0000");
        assert_eq!(r1, Rgba { r: 255, g: 0, b: 0, a: 255 });
        let r2 = Rgba::from_hex("#FF0000AA");
        assert_eq!(r2, Rgba { r: 255, g: 0, b: 0, a: 170 });
        let r3 = Rgba::from_hex("invalid");
        assert_eq!(r3, Rgba { r: 0, g: 0, b: 0, a: 255 });
        let r4 = Rgba::from_hex("123456"); // No #
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
    fn test_keyframe_value_interpolate() {
        let s1 = KeyframeValue::Scalar(0.0);
        let s2 = KeyframeValue::Scalar(10.0);
        assert_eq!(s1.interpolate(&s2, 0.5), KeyframeValue::Scalar(5.0));

        let c1 = KeyframeValue::Color(Rgba { r: 0, g: 0, b: 0, a: 255 });
        let c2 = KeyframeValue::Color(Rgba { r: 255, g: 255, b: 255, a: 255 });
        if let KeyframeValue::Color(c) = c1.interpolate(&c2, 0.5) {
            assert_eq!(c.r, 127);
        } else {
            panic!();
        }

        let r1 = KeyframeValue::CornerRadii([0.0, 0.0, 0.0, 0.0]);
        let r2 = KeyframeValue::CornerRadii([10.0, 20.0, 30.0, 40.0]);
        assert_eq!(r1.interpolate(&r2, 0.5), KeyframeValue::CornerRadii([5.0, 10.0, 15.0, 20.0]));

        let a1 = KeyframeValue::Arc(ArcData {
            starting_angle: 0.0,
            ending_angle: 0.0,
            inner_radius: 0.0,
        });
        let a2 = KeyframeValue::Arc(ArcData {
            starting_angle: 10.0,
            ending_angle: 20.0,
            inner_radius: 30.0,
        });
        assert_eq!(
            a1.interpolate(&a2, 0.5),
            KeyframeValue::Arc(ArcData {
                starting_angle: 5.0,
                ending_angle: 10.0,
                inner_radius: 15.0
            })
        );

        let g1 = KeyframeValue::Gradient(vec![GradientStop {
            position: 0.0,
            color: Rgba::from_hex("#000000"),
        }]);
        let g2 = KeyframeValue::Gradient(vec![GradientStop {
            position: 1.0,
            color: Rgba::from_hex("#ffffff"),
        }]);
        // They have same length (1), so they should interpolate!
        assert_eq!(
            g1.interpolate(&g2, 0.5),
            KeyframeValue::Gradient(vec![GradientStop {
                position: 0.5,
                color: Rgba { r: 127, g: 127, b: 127, a: 255 }
            }])
        );

        // Test length mismatch!
        let g3 = KeyframeValue::Gradient(vec![GradientStop {
            position: 0.0,
            color: Rgba::from_hex("#000000"),
        }]);
        let g4 = KeyframeValue::Gradient(vec![
            GradientStop { position: 0.0, color: Rgba::from_hex("#000000") },
            GradientStop { position: 1.0, color: Rgba::from_hex("#ffffff") },
        ]);
        // Lengths are different (1 and 2), should return clone of g3!
        assert_eq!(g3.interpolate(&g4, 0.5), g3);

        // Test mismatched types returns clone of self!
        let s = KeyframeValue::Scalar(1.0);
        let c = KeyframeValue::Color(Rgba::from_hex("#000000"));
        assert_eq!(s.interpolate(&c, 0.5), s);
    }

    #[test]
    fn test_legacy_parse() {
        let encoded = general_purpose::STANDARD.encode("[110.0, 10.0, 10.0, 10.0]");
        let legacy = format!("Linear|0.5,{},Linear", encoded);
        let parsed = ParsedTimelineData::parse(&legacy).unwrap();
        assert_eq!(parsed.target_easing, Easing::Linear);
        assert_eq!(parsed.keyframes.len(), 1);
        assert_eq!(parsed.keyframes[0].fraction, 0.5);
        if let KeyframeValue::CornerRadii(r) = parsed.keyframes[0].value {
            assert_eq!(r[0], 110.0);
        } else {
            panic!();
        }
    }

    #[test]
    fn test_property_parsing() {
        assert_eq!("opacity".parse::<AnimatableProperty>().unwrap(), AnimatableProperty::Opacity);
        assert_eq!(
            "fills.0.solid".parse::<AnimatableProperty>().unwrap(),
            AnimatableProperty::FillSolid(0)
        );
        assert_eq!(
            "strokes.1.gradient".parse::<AnimatableProperty>().unwrap(),
            AnimatableProperty::StrokeGradient(1)
        );
        assert_eq!(
            "unknown".parse::<AnimatableProperty>().unwrap(),
            AnimatableProperty::Other("unknown".to_string())
        );
    }

    #[test]
    fn test_property_lookup() {
        let mut map = HashMap::new();
        map.insert("Node1-opacity".to_string(), "Linear|0.5,MQ==,Linear".to_string()); // 1.0 base64 is MQ==
        let lookup = PropertyLookup::from_map(&map);
        let nt = lookup.get_for_node("Node1").unwrap();
        assert!(nt.opacity.is_some());

        let mut map2 = HashMap::new();
        map2.insert("Node1-opacity-custom-id".to_string(), "Linear|0.5,MQ==,Linear".to_string());
        let lookup2 = PropertyLookup::from_map(&map2);
        let nt2 = lookup2.get_for_node("Node1").unwrap();
        assert!(nt2.opacity.is_some());
    }

    #[test]
    fn test_get_keyframe_segment() {
        let timeline = ParsedTimelineData {
            target_easing: Easing::Linear,
            keyframes: vec![
                ParsedKeyframe {
                    fraction: 0.2,
                    value: KeyframeValue::Scalar(20.0),
                    easing: Easing::Linear,
                },
                ParsedKeyframe {
                    fraction: 0.8,
                    value: KeyframeValue::Scalar(80.0),
                    easing: Easing::Linear,
                },
            ],
        };

        let start = KeyframeValue::Scalar(0.0);
        let end = KeyframeValue::Scalar(100.0);

        let (k1, k2, _t) = timeline.get_keyframe_segment(&start, &end, 0.1);
        assert_eq!(k1, &start);
        assert_eq!(k2, &KeyframeValue::Scalar(20.0));

        let (k1, k2, _t) = timeline.get_keyframe_segment(&start, &end, 0.5);
        assert_eq!(k1, &KeyframeValue::Scalar(20.0));
        assert_eq!(k2, &KeyframeValue::Scalar(80.0));

        let (k1, k2, _t) = timeline.get_keyframe_segment(&start, &end, 0.9);
        assert_eq!(k1, &KeyframeValue::Scalar(80.0));
        assert_eq!(k2, &end);
    }
}
