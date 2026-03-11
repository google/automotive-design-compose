use base64::{engine::general_purpose, Engine as _};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

// --- High Level Runtime Types ---

/// Supported easing functions for interpolation.
#[derive(Debug, Clone, PartialEq, Copy)]
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
    // "Unknown" removed for runtime performance; fallback to Linear during parsing
}

impl Easing {
    /// Applies the easing function to a linear progress value `t` (0.0 to 1.0).
    pub fn apply(&self, t: f32) -> f32 {
        match self {
            Easing::Linear | Easing::Inherit => t,
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

#[derive(Debug, Clone, PartialEq, Copy)]
pub struct Rgba {
    pub r: u8,
    pub g: u8,
    pub b: u8,
    pub a: u8,
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

#[derive(Debug, Clone, PartialEq)]
pub struct GradientStop {
    pub position: f32,
    pub color: Rgba,
}

#[derive(Debug, Clone, PartialEq, Copy)]
pub struct ArcData {
    pub starting_angle: f32,
    pub ending_angle: f32,
    pub inner_radius: f32,
}

// Optimized Enum: Uses f32 for rendering, Fixed arrays, Pre-parsed Colors
/// Represents the value of a property at a specific keyframe.
#[derive(Debug, Clone, PartialEq)]
pub enum KeyframeValue {
    /// A single scalar value (e.g., x, y, width, opacity).
    Scalar(f32),
    /// Four corner radii [TL, TR, BR, BL].
    CornerRadii([f32; 4]),
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

#[derive(Debug, Clone, PartialEq)]
pub struct ParsedKeyframe {
    pub fraction: f32,
    pub value: KeyframeValue,
    pub easing: Easing,
}

/// Represents a parsed timeline of custom keyframes for a single property.
#[derive(Debug, Clone, PartialEq)]
pub struct ParsedTimelineData {
    pub target_easing: Easing,
    pub keyframes: Vec<ParsedKeyframe>,
}

// --- Parsing Logic ---

impl ParsedTimelineData {
    /// Parses a pipe-separated custom keyframe string.
    pub fn parse(encoded_data: &str) -> Result<Self, String> {
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

<<<<<<< HEAD
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

    // Fallback: Color String
    // The input string might be a raw hex string inside quotes (JSON string) or just raw chars?
    // Base64 decoding gives us the raw bytes.
    // If the original was JSON string `" #ffffff "`, serde_json from_str would strip quotes.
    // If it fails, we assume it's a raw color hex.
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
            "Opacity" => Ok(AnimatableProperty::Opacity),
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
        // Find the node's NodeTimelines, then find the property within it.
        // Needs a workaround since we return an Arc and want to return a reference to its contents. Wait, the old getter returned a reference. If we return a reference tied to `self`, that's fine.
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
}
