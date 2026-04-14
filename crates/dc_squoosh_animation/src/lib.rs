use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Arc;

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
    CubicBezier(f32, f32, f32, f32),
}

impl Default for Easing {
    fn default() -> Self {
        Easing::Inherit
    }
}

impl Easing {
    pub fn apply(&self, t: f32) -> f32 {
        match self {
            Easing::Inherit => t,
            Easing::Linear => t,
            Easing::CubicBezier(_, _, _, _) => {
                static WARNED: std::sync::atomic::AtomicBool = std::sync::atomic::AtomicBool::new(false);
                if !WARNED.load(std::sync::atomic::Ordering::Relaxed) {
                    log::warn!("CubicBezier easing is not implemented, falling back to linear.");
                    WARNED.store(true, std::sync::atomic::Ordering::Relaxed);
                }
                t
            }
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
            }
        }
    }
}

#[derive(Debug, Clone, PartialEq, Copy, Serialize, Deserialize)]
pub struct Rgba {
    pub r: u8,
    pub g: u8,
    pub b: u8,
    pub a: u8,
}

impl Rgba {
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
    pub starting_angle: f32,
    pub ending_angle: f32,
    pub inner_radius: f32,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum KeyframeValue {
    CornerRadii([f32; 4]),
    Scalar(f32),
    Color(Rgba),
    Gradient(Vec<GradientStop>),
    Arc(ArcData),
    None,
}

impl KeyframeValue {
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
                    // FIXME: If number of gradient stops changes between keyframes, we fallback to the
                    // starting gradient to avoid glitches. Consider interpolating matching stops or padding.
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
    pub easing: Easing,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct ParsedTimelineData {
    pub target_easing: Easing,
    pub keyframes: Vec<ParsedKeyframe>,
}

impl ParsedTimelineData {
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

        if let Some(first) = self.keyframes.first() {
            if fraction < first.fraction {
                kf2_fraction = first.fraction;
                kf2_value = &first.value;
                easing = first.easing;
            } else {
                let mut found = false;
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

// --- Property Lookup ---

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
                let parts: Vec<&str> = s.split('.').collect();
                if let [prefix, idx_str, type_str] = parts.as_slice() {
                    if let Ok(idx) = idx_str.parse::<usize>() {
                        match (*prefix, *type_str) {
                            ("fills", "solid") => return Ok(AnimatableProperty::FillSolid(idx)),
                            ("fills", "gradient") => return Ok(AnimatableProperty::FillGradient(idx)),
                            ("strokes", "solid") => return Ok(AnimatableProperty::StrokeSolid(idx)),
                            ("strokes", "gradient") => return Ok(AnimatableProperty::StrokeGradient(idx)),
                            _ => {}
                        }
                    }
                }
                Ok(AnimatableProperty::Other(s.to_string()))
            }
        }
    }
}

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

#[derive(Clone, Debug, PartialEq)]
pub struct PropertyLookup {
    pub timelines: HashMap<String, Arc<NodeTimelines>>,
}

impl PropertyLookup {
    /// Retrieves all parsed timeline data for a specific node.
    pub fn get_for_node(&self, node: &str) -> Option<Arc<NodeTimelines>> {
        self.timelines.get(node).cloned()
    }

    /// Retrieves the parsed timeline data for a specific node and property.
    pub fn get(&self, node: &str, prop: AnimatableProperty) -> Option<&ParsedTimelineData> {
        self.timelines.get(node).and_then(|nt| nt.get(&prop))
    }
}

// --- Mapping from Proto ---

// Assume dc_bundle exposes these types.
// We might need to adjust the paths based on actual generation.

use dc_bundle::animationspec as proto;

impl From<proto::RgbaValue> for Rgba {
    fn from(p: proto::RgbaValue) -> Self {
        Rgba { r: p.r as u8, g: p.g as u8, b: p.b as u8, a: p.a as u8 }
    }
}

impl From<proto::GradientStopValue> for GradientStop {
    fn from(p: proto::GradientStopValue) -> Self {
        GradientStop {
            position: p.position,
            color: p.color.map(Rgba::from).unwrap_or(Rgba { r: 0, g: 0, b: 0, a: 255 }),
        }
    }
}

impl From<proto::ArcDataValue> for ArcData {
    fn from(p: proto::ArcDataValue) -> Self {
        ArcData {
            starting_angle: p.starting_angle,
            ending_angle: p.ending_angle,
            inner_radius: p.inner_radius,
        }
    }
}

impl From<proto::CustomKeyframeValue> for KeyframeValue {
    fn from(p: proto::CustomKeyframeValue) -> Self {
        match p.value {
            Some(proto::custom_keyframe_value::Value::Scalar(v)) => KeyframeValue::Scalar(v),
            Some(proto::custom_keyframe_value::Value::Radii(r)) => {
                KeyframeValue::CornerRadii([r.top_left, r.top_right, r.bottom_right, r.bottom_left])
            }
            Some(proto::custom_keyframe_value::Value::Color(c)) => KeyframeValue::Color(c.into()),
            Some(proto::custom_keyframe_value::Value::Gradient(g)) => {
                KeyframeValue::Gradient(g.stops.into_iter().map(Into::into).collect())
            }
            Some(proto::custom_keyframe_value::Value::Arc(a)) => KeyframeValue::Arc(a.into()),
            Some(_) => KeyframeValue::None, // Fallback for unknown oneof variants
            None => KeyframeValue::None,
        }
    }
}

impl From<proto::Easing> for Easing {
    fn from(p: proto::Easing) -> Self {
        match p.type_ {
            Some(proto::easing::Type::Linear(_)) => Easing::Linear,
            Some(proto::easing::Type::Bezier(b)) => Easing::CubicBezier(b.p0, b.p1, b.p2, b.p3),
            Some(_) => Easing::Inherit, // Fallback for unknown easing types
            None => Easing::Inherit,
        }
    }
}

impl From<proto::CustomKeyframe> for ParsedKeyframe {
    fn from(p: proto::CustomKeyframe) -> Self {
        ParsedKeyframe {
            fraction: p.fraction,
            value: p
                .value
                .as_ref()
                .map(|v| KeyframeValue::from(v.clone()))
                .unwrap_or(KeyframeValue::None),
            easing: p.easing.as_ref().map(|e| Easing::from(e.clone())).unwrap_or(Easing::Inherit),
        }
    }
}

impl From<proto::CustomTimeline> for ParsedTimelineData {
    fn from(p: proto::CustomTimeline) -> Self {
        ParsedTimelineData {
            target_easing: p
                .target_easing
                .as_ref()
                .map(|e| Easing::from(e.clone()))
                .unwrap_or(Easing::Inherit),
            keyframes: p.keyframes.into_iter().map(Into::into).collect(),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_interpolation() {
        let timeline = ParsedTimelineData {
            target_easing: Easing::Linear,
            keyframes: vec![ParsedKeyframe {
                fraction: 0.5,
                value: KeyframeValue::CornerRadii([50.0, 50.0, 50.0, 50.0]),
                easing: Easing::Linear,
            }],
        };

        let start_val = KeyframeValue::CornerRadii([0.0, 0.0, 0.0, 0.0]);
        let end_val = KeyframeValue::CornerRadii([100.0, 100.0, 100.0, 100.0]);

        let val = timeline.interpolate(&start_val, &end_val, 0.25);
        assert_eq!(val, KeyframeValue::CornerRadii([25.0, 25.0, 25.0, 25.0]));

        let val = timeline.interpolate(&start_val, &end_val, 0.5);
        assert_eq!(val, KeyframeValue::CornerRadii([50.0, 50.0, 50.0, 50.0]));

        let val = timeline.interpolate(&start_val, &end_val, 0.75);
        assert_eq!(val, KeyframeValue::CornerRadii([75.0, 75.0, 75.0, 75.0]));
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
            color: Rgba { r: 0, g: 0, b: 0, a: 255 },
        }]);
        let g2 = KeyframeValue::Gradient(vec![GradientStop {
            position: 1.0,
            color: Rgba { r: 255, g: 255, b: 255, a: 255 },
        }]);
        assert_eq!(
            g1.interpolate(&g2, 0.5),
            KeyframeValue::Gradient(vec![GradientStop {
                position: 0.5,
                color: Rgba { r: 127, g: 127, b: 127, a: 255 }
            }])
        );

        let g3 = KeyframeValue::Gradient(vec![GradientStop {
            position: 0.0,
            color: Rgba { r: 0, g: 0, b: 0, a: 255 },
        }]);
        let g4 = KeyframeValue::Gradient(vec![
            GradientStop { position: 0.0, color: Rgba { r: 0, g: 0, b: 0, a: 255 } },
            GradientStop { position: 1.0, color: Rgba { r: 255, g: 255, b: 255, a: 255 } },
        ]);
        assert_eq!(g3.interpolate(&g4, 0.5), g3);

        let s = KeyframeValue::Scalar(1.0);
        let c = KeyframeValue::Color(Rgba { r: 0, g: 0, b: 0, a: 255 });
        assert_eq!(s.interpolate(&c, 0.5), s);
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
