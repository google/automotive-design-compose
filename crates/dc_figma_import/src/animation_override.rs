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

//! This file contains the conversions from the JSON animation specification to the protobuf
//! animation specification. The JSON structs are defined in `animation_spec_schema.rs`.
//! The protobuf structs are generated from `animations.proto`.

use crate::animation_spec_schema::{
    AnimationMatrixJson, AnimationOverrideJson, AnimationSpec as AnimationSpecJson,
    Animations as AnimationsJson, BezierCurve as BezierCurveJson,
    CustomKeyframe as CustomKeyframeJson, CustomTimeline as CustomTimelineJson, Duration,
    Easing as EasingJson, KeyFrame as KeyFrameJson, KeyFrameAnimation as KeyFrameAnimationJson,
    RepeatType as RepeatTypeJson, SmoothAnimation as SmoothAnimationJson, StopType as StopTypeJson,
    TransitionSpecJson,
};
use crate::figma_schema;
use crate::reaction_schema::{
    ActionJson, EasingJson as ReactionEasingJson, NavigationJson, TransitionJson, TriggerJson,
};
use crate::HiddenNodePolicy;
use dc_bundle::animationspec;
use dc_bundle::animationspec::{
    animation_override, animations, easing, repeat_type, stop_type, AnimationSpec, Animations,
    BezierCurve, CustomKeyframe, CustomTimeline, Easing, KeyFrame, KeyFrameAnimation, RepeatType,
    SmoothAnimation, StopType,
};
use dc_bundle::transition::easing::Easing_type;
use dc_bundle::view::view_data::View_data_type;
use dc_bundle::view::View;
use std::collections::{HashMap, HashSet};

/// Protobuf `google.protobuf.Duration` valid range maximum (~10,000 years in seconds).
const MAX_PROTO_DURATION_SECS: f64 = 315_576_000_000.0;

/// Converts from the JSON `AnimationOverrideJson` to the protobuf `AnimationOverride`.
pub struct AnimationOverride(animationspec::AnimationOverride);

impl From<AnimationOverride> for animationspec::AnimationOverride {
    fn from(val: AnimationOverride) -> Self {
        val.0
    }
}

impl From<&AnimationOverrideJson> for AnimationOverride {
    fn from(json: &AnimationOverrideJson) -> Self {
        AnimationOverride(match json {
            AnimationOverrideJson::Default => animationspec::AnimationOverride {
                animation_override: Some(animation_override::Animation_override::NoOverride(
                    ::protobuf::well_known_types::empty::Empty::new(),
                )),
                ..Default::default()
            },
            AnimationOverrideJson::DisableAnimations => animationspec::AnimationOverride {
                animation_override: Some(
                    animation_override::Animation_override::DisableAnimations(
                        ::protobuf::well_known_types::empty::Empty::new(),
                    ),
                ),
                ..Default::default()
            },
            AnimationOverrideJson::Custom(spec) => animationspec::AnimationOverride {
                animation_override: Some(animation_override::Animation_override::Custom(
                    spec.clone().into(),
                )),
                ..Default::default()
            },
            AnimationOverrideJson::Matrix(matrix) => animationspec::AnimationOverride {
                animation_override: Some(animation_override::Animation_override::Matrix(
                    matrix.clone().into(),
                )),
                ..Default::default()
            },
        })
    }
}

#[derive(serde::Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
struct GradientStopJson {
    position: f32,
    color: crate::figma_schema::FigmaColor,
}

#[derive(serde::Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
struct ArcDataJson {
    #[serde(alias = "starting_angle")]
    starting_angle: f32,
    #[serde(alias = "ending_angle")]
    ending_angle: f32,
    #[serde(alias = "inner_radius")]
    inner_radius: f32,
}

#[derive(serde::Deserialize)]
#[serde(untagged)]
enum CustomKeyframeValueJson {
    Scalar(f64),
    Radii([f64; 4]),
    String(String),
    Color(crate::figma_schema::FigmaColor),
    Gradient(Vec<GradientStopJson>),
    Arc(ArcDataJson),
}

/// Converts from the JSON `CustomKeyframeJson` to the protobuf `CustomKeyframe`.
impl From<CustomKeyframeJson> for CustomKeyframe {
    fn from(json: CustomKeyframeJson) -> Self {
        let mut typed_value = animationspec::CustomKeyframeValue::default();

        if let Ok(val) = serde_json::from_value::<CustomKeyframeValueJson>(json.value_json.clone())
        {
            match val {
                CustomKeyframeValueJson::Scalar(scalar) => {
                    typed_value.value =
                        Some(animationspec::custom_keyframe_value::Value::Scalar(scalar as f32));
                }
                CustomKeyframeValueJson::Radii(radii) => {
                    typed_value.value = Some(animationspec::custom_keyframe_value::Value::Radii(
                        animationspec::CornerRadiiValue {
                            top_left: radii[0] as f32,
                            top_right: radii[1] as f32,
                            bottom_right: radii[2] as f32,
                            bottom_left: radii[3] as f32,
                            ..Default::default()
                        },
                    ));
                }
                CustomKeyframeValueJson::String(s) => {
                    if s.starts_with('#') {
                        let hex = &s[1..];
                        if hex.len() == 6 {
                            if let (Ok(r), Ok(g), Ok(b)) = (
                                u32::from_str_radix(&hex[0..2], 16),
                                u32::from_str_radix(&hex[2..4], 16),
                                u32::from_str_radix(&hex[4..6], 16),
                            ) {
                                typed_value.value =
                                    Some(animationspec::custom_keyframe_value::Value::Color(
                                        animationspec::RgbaValue {
                                            r,
                                            g,
                                            b,
                                            a: 255,
                                            ..Default::default()
                                        },
                                    ));
                            }
                        } else if hex.len() == 8 {
                            if let (Ok(r), Ok(g), Ok(b), Ok(a)) = (
                                u32::from_str_radix(&hex[0..2], 16),
                                u32::from_str_radix(&hex[2..4], 16),
                                u32::from_str_radix(&hex[4..6], 16),
                                u32::from_str_radix(&hex[6..8], 16),
                            ) {
                                typed_value.value =
                                    Some(animationspec::custom_keyframe_value::Value::Color(
                                        animationspec::RgbaValue {
                                            r,
                                            g,
                                            b,
                                            a,
                                            ..Default::default()
                                        },
                                    ));
                            }
                        }
                    }
                }
                CustomKeyframeValueJson::Color(color) => {
                    typed_value.value = Some(animationspec::custom_keyframe_value::Value::Color(
                        animationspec::RgbaValue {
                            r: (color.r * 255.0) as u32,
                            g: (color.g * 255.0) as u32,
                            b: (color.b * 255.0) as u32,
                            a: (color.a * 255.0) as u32,
                            ..Default::default()
                        },
                    ));
                }
                CustomKeyframeValueJson::Gradient(stops) => {
                    let proto_stops = stops
                        .into_iter()
                        .map(|s| animationspec::GradientStopValue {
                            position: s.position,
                            color: ::protobuf::MessageField::some(animationspec::RgbaValue {
                                r: (s.color.r * 255.0) as u32,
                                g: (s.color.g * 255.0) as u32,
                                b: (s.color.b * 255.0) as u32,
                                a: (s.color.a * 255.0) as u32,
                                ..Default::default()
                            }),
                            ..Default::default()
                        })
                        .collect();
                    typed_value.value =
                        Some(animationspec::custom_keyframe_value::Value::Gradient(
                            animationspec::GradientValue {
                                stops: proto_stops,
                                ..Default::default()
                            },
                        ));
                }
                CustomKeyframeValueJson::Arc(arc) => {
                    typed_value.value = Some(animationspec::custom_keyframe_value::Value::Arc(
                        animationspec::ArcDataValue {
                            starting_angle: arc.starting_angle,
                            ending_angle: arc.ending_angle,
                            inner_radius: arc.inner_radius,
                            ..Default::default()
                        },
                    ));
                }
            }
        }

        if typed_value.value.is_none() {
            log::warn!("Failed to parse custom keyframe value JSON: {:?}", json.value_json);
        }

        CustomKeyframe {
            fraction: json.fraction,
            value: ::protobuf::MessageField::some(typed_value),
            easing: Some(json.easing.into()).into(),
            ..Default::default()
        }
    }
}

/// Converts from the JSON `CustomTimelineJson` to the protobuf `CustomTimeline`.
impl From<CustomTimelineJson> for CustomTimeline {
    fn from(json: CustomTimelineJson) -> Self {
        CustomTimeline {
            target_easing: Some(json.target_easing.into()).into(),
            keyframes: json.keyframes.into_iter().map(|k| k.into()).collect(),
            ..Default::default()
        }
    }
}

/// Converts from the JSON `AnimationSpecJson` to the protobuf `AnimationSpec`.
impl From<AnimationSpecJson> for AnimationSpec {
    fn from(json: AnimationSpecJson) -> Self {
        let mut custom_keyframe_data: std::collections::HashMap<
            String,
            animationspec::CustomTimeline,
        > = json.custom_keyframe_data.into_iter().map(|(k, v)| (k, v.into())).collect();
        for (k, v) in json.timelines {
            custom_keyframe_data.entry(k).or_insert_with(|| v.into());
        }
        AnimationSpec {
            initial_delay: Some(json.initial_delay.into()).into(),
            animation: Some(json.animation.into()).into(),
            interrupt_type: json.interrupt_type.map(|x| x.into()).into(),
            custom_keyframe_data,
            ..Default::default()
        }
    }
}

/// Normalizes a comma-separated variant string by trimming parts and sorting "Property=Value"
/// assignments alphabetically.
/// For example: "#heartbeat=on, #debug=on" and "#debug=on, #heartbeat=on" both normalize to
/// "#debug=on, #heartbeat=on", and " State=A, " normalizes to "State=A".
/// Empty strings and wildcards ("*") are returned unchanged.
pub(crate) fn normalize_variant_string(s: &str) -> String {
    if s.is_empty() || s == "*" {
        return s.to_string();
    }
    let mut parts: Vec<&str> =
        s.split(',').map(|part| part.trim()).filter(|part| !part.is_empty()).collect();
    if parts.len() <= 1 {
        return parts.into_iter().next().unwrap_or_default().to_string();
    }
    parts.sort_unstable();
    parts.join(", ")
}

/// Converts from the JSON `TransitionSpecJson` to the protobuf `TransitionSpec`.
impl From<TransitionSpecJson> for animationspec::TransitionSpec {
    fn from(json: TransitionSpecJson) -> Self {
        let mut timelines: std::collections::HashMap<String, animationspec::CustomTimeline> =
            json.timelines.into_iter().map(|(k, v)| (k, v.into())).collect();
        for (k, v) in json.custom_keyframe_data {
            timelines.insert(k, v.into());
        }
        animationspec::TransitionSpec {
            source_variant: normalize_variant_string(&json.from),
            target_variant: normalize_variant_string(&json.to),
            animation_name: json.name,
            spec: json.spec.map(|s| s.into()).into(),
            timelines,
            ..Default::default()
        }
    }
}

/// Converts from the JSON `AnimationMatrixJson` to the protobuf `AnimationMatrix`.
impl From<AnimationMatrixJson> for animationspec::AnimationMatrix {
    fn from(json: AnimationMatrixJson) -> Self {
        let mut root_timelines: std::collections::HashMap<String, animationspec::CustomTimeline> =
            json.timelines.into_iter().map(|(k, v)| (k, v.into())).collect();
        for (k, v) in json.custom_keyframe_data {
            root_timelines.insert(k, v.into());
        }

        let transitions = json
            .transitions
            .into_iter()
            .map(|t_json| {
                let mut proto: animationspec::TransitionSpec = t_json.into();
                for (k, proto_timeline) in &root_timelines {
                    if !proto.timelines.contains_key(k) {
                        proto.timelines.insert(k.clone(), proto_timeline.clone());
                    }
                }
                proto
            })
            .collect();

        animationspec::AnimationMatrix {
            default_spec: json.default_spec.map(|s| s.into()).into(),
            transitions,
            ..Default::default()
        }
    }
}

/// Converts from the JSON `AnimationsJson` to the protobuf `Animations`.
impl From<AnimationsJson> for Animations {
    fn from(json: AnimationsJson) -> Self {
        let mut anim = Animations::new();
        if let Some(smooth) = json.smooth {
            anim.type_ = Some(animations::Type::Smooth(smooth.into()));
        } else if let Some(key_frame) = json.key_frame {
            anim.type_ = Some(animations::Type::KeyFrame(key_frame.into()));
        }
        anim
    }
}

/// Converts from the JSON `SmoothAnimationJson` to the protobuf `SmoothAnimation`.
impl From<SmoothAnimationJson> for SmoothAnimation {
    fn from(json: SmoothAnimationJson) -> Self {
        SmoothAnimation {
            duration: Some(json.duration.into()).into(),
            repeat_type: Some(json.repeat_type.into()).into(),
            easing: Some(json.easing.into()).into(),
            ..Default::default()
        }
    }
}

/// Converts from the JSON `KeyFrameAnimationJson` to the protobuf `KeyFrameAnimation`.
impl From<KeyFrameAnimationJson> for KeyFrameAnimation {
    fn from(json: KeyFrameAnimationJson) -> Self {
        KeyFrameAnimation {
            steps: json.steps.into_iter().map(|x| x.into()).collect(),
            repeat_type: Some(json.repeat_type.into()).into(),
            ..Default::default()
        }
    }
}

/// Converts from the JSON `KeyFrameJson` to the protobuf `KeyFrame`.
impl From<KeyFrameJson> for KeyFrame {
    fn from(json: KeyFrameJson) -> Self {
        KeyFrame {
            value: json.value,
            duration: Some(json.duration.into()).into(),
            ..Default::default()
        }
    }
}

/// Converts from the JSON `RepeatTypeJson` to the protobuf `RepeatType`.
impl From<RepeatTypeJson> for RepeatType {
    fn from(json: RepeatTypeJson) -> Self {
        let mut repeat_type = RepeatType::new();
        match json {
            RepeatTypeJson::String(s) => {
                if s == "LoopForever" {
                    repeat_type.type_ = Some(repeat_type::Type::LoopForever(
                        ::protobuf::well_known_types::empty::Empty::new(),
                    ));
                } else {
                    repeat_type.type_ = Some(repeat_type::Type::NoRepeat(
                        ::protobuf::well_known_types::empty::Empty::new(),
                    ));
                }
            }
            RepeatTypeJson::Repeat { repeat: r } => {
                repeat_type.type_ = Some(repeat_type::Type::Repeat(r));
            }
        }
        repeat_type
    }
}

/// Converts from the JSON `EasingJson` to the protobuf `Easing`.
impl From<EasingJson> for Easing {
    fn from(json: EasingJson) -> Self {
        let mut easing = Easing::new();
        match json {
            EasingJson::String(s) => {
                // TODO: Do not use these predefined fallback templates. Create interactive UI Bezier graph inputs
                // inside the standalone Squoosh editing panel to retrieve precise designer curve parameters directly.
                let s_norm = s.to_lowercase().replace("_", "").replace("-", "");
                if s_norm == "linear" {
                    easing.type_ = Some(easing::Type::Linear(
                        ::protobuf::well_known_types::empty::Empty::new(),
                    ));
                } else if s_norm == "easein" {
                    easing.type_ = Some(easing::Type::Bezier(Default::default()));
                    if let Some(easing::Type::Bezier(ref mut b)) = easing.type_ {
                        b.p0 = 0.42;
                        b.p1 = 0.0;
                        b.p2 = 1.0;
                        b.p3 = 1.0;
                    }
                } else if s_norm == "easeout" {
                    easing.type_ = Some(easing::Type::Bezier(Default::default()));
                    if let Some(easing::Type::Bezier(ref mut b)) = easing.type_ {
                        b.p0 = 0.0;
                        b.p1 = 0.0;
                        b.p2 = 0.58;
                        b.p3 = 1.0;
                    }
                } else if s_norm == "easeinout" || s_norm == "easeinandout" {
                    easing.type_ = Some(easing::Type::Bezier(Default::default()));
                    if let Some(easing::Type::Bezier(ref mut b)) = easing.type_ {
                        b.p0 = 0.42;
                        b.p1 = 0.0;
                        b.p2 = 0.58;
                        b.p3 = 1.0;
                    }
                } else if s_norm == "easeincubic" {
                    easing.type_ = Some(easing::Type::Bezier(Default::default()));
                    if let Some(easing::Type::Bezier(ref mut b)) = easing.type_ {
                        b.p0 = 0.32;
                        b.p1 = 0.0;
                        b.p2 = 0.67;
                        b.p3 = 0.0;
                    }
                } else if s_norm == "easeoutcubic" {
                    easing.type_ = Some(easing::Type::Bezier(Default::default()));
                    if let Some(easing::Type::Bezier(ref mut b)) = easing.type_ {
                        b.p0 = 0.33;
                        b.p1 = 1.0;
                        b.p2 = 0.68;
                        b.p3 = 1.0;
                    }
                } else if s_norm == "easeinoutcubic" {
                    easing.type_ = Some(easing::Type::Bezier(Default::default()));
                    if let Some(easing::Type::Bezier(ref mut b)) = easing.type_ {
                        b.p0 = 0.645;
                        b.p1 = 0.045;
                        b.p2 = 0.355;
                        b.p3 = 1.0;
                    }
                } else {
                    // Default to linear if entirely unknown Token
                    easing.type_ = Some(easing::Type::Linear(
                        ::protobuf::well_known_types::empty::Empty::new(),
                    ));
                }
            }
            EasingJson::Bezier { bezier: b } => {
                easing.type_ = Some(easing::Type::Bezier(b.into()));
            }
        }
        easing
    }
}

/// Converts from the JSON `BezierCurveJson` to the protobuf `BezierCurve`.
impl From<BezierCurveJson> for BezierCurve {
    fn from(json: BezierCurveJson) -> Self {
        BezierCurve { p0: json.p0, p1: json.p1, p2: json.p2, p3: json.p3, ..Default::default() }
    }
}

/// Converts from the JSON `StopTypeJson` to the protobuf `StopType`.
impl From<StopTypeJson> for StopType {
    fn from(json: StopTypeJson) -> Self {
        let mut stop_type = StopType::new();
        match json {
            StopTypeJson::None => {}
            StopTypeJson::ResetToStart => {
                stop_type.type_ = Some(stop_type::Type::ResetToStart(
                    ::protobuf::well_known_types::empty::Empty::new(),
                ));
            }
            StopTypeJson::Complete => {
                stop_type.type_ = Some(stop_type::Type::Complete(
                    ::protobuf::well_known_types::empty::Empty::new(),
                ));
            }
            StopTypeJson::Stop => {
                stop_type.type_ =
                    Some(stop_type::Type::Stop(::protobuf::well_known_types::empty::Empty::new()));
            }
        }
        stop_type
    }
}

/// Converts from the JSON `Duration` to the protobuf `Duration`.
impl From<Duration> for ::protobuf::well_known_types::duration::Duration {
    fn from(json: Duration) -> Self {
        ::protobuf::well_known_types::duration::Duration {
            seconds: json.secs,
            nanos: json.nanos as i32,
            ..Default::default()
        }
    }
}

/// Converts a `reaction_schema::EasingJson` to a protobuf `animationspec::Easing`.
///
/// Since `animationspec::Easing` supports only `Linear` and cubic `BezierCurve`, Figma spring
/// presets (`Gentle`, `Quick`, `Bouncy`, `Slow`, `CustomSpring`) are approximated with a smooth
/// ease-out cubic bezier curve so `SMART_ANIMATE` transitions still animate instead of snapping.
pub(crate) fn easing_json_to_anim_easing(easing_json: &ReactionEasingJson) -> Easing {
    let easing_type: Easing_type = easing_json.clone().into();
    let mut anim_easing = Easing::new();
    match easing_type {
        Easing_type::Bezier(b) => {
            if b.x1 == 0.0 && b.y1 == 0.0 && b.x2 == 1.0 && b.y2 == 1.0 {
                anim_easing.type_ =
                    Some(easing::Type::Linear(::protobuf::well_known_types::empty::Empty::new()));
            } else {
                anim_easing.type_ = Some(easing::Type::Bezier(BezierCurve {
                    p0: b.x1,
                    p1: b.y1,
                    p2: b.x2,
                    p3: b.y2,
                    ..Default::default()
                }));
            }
        }
        Easing_type::Spring(_) => {
            log::debug!(
                "Approximating Figma spring easing {:?} with ease-out cubic bezier for TransitionSpec",
                easing_json
            );
            anim_easing.type_ = Some(easing::Type::Bezier(BezierCurve {
                p0: 0.22,
                p1: 1.0,
                p2: 0.36,
                p3: 1.0,
                ..Default::default()
            }));
        }
        _ => {
            anim_easing.type_ =
                Some(easing::Type::Linear(::protobuf::well_known_types::empty::Empty::new()));
        }
    }
    anim_easing
}

/// Converts a duration in seconds (as returned by Figma REST/Plugin APIs) to protobuf `Duration`.
///
/// Rounds to the nearest microsecond so `f32` literals such as `0.2` or `0.3000000119` produce
/// clean millisecond/microsecond values without IEEE-754 nanosecond jitter, and clamps extreme
/// positive finite inputs to `MAX_PROTO_DURATION_SECS` to prevent integer overflow.
pub(crate) fn seconds_to_proto_duration(
    seconds: f32,
) -> ::protobuf::well_known_types::duration::Duration {
    let clamped = if seconds.is_finite() && seconds > 0.0 {
        (seconds as f64).min(MAX_PROTO_DURATION_SECS)
    } else {
        0.0
    };
    let total_micros = (clamped * 1_000_000.0).round() as i64;
    ::protobuf::well_known_types::duration::Duration {
        seconds: total_micros / 1_000_000,
        nanos: ((total_micros % 1_000_000) * 1_000) as i32,
        ..Default::default()
    }
}

fn build_smart_animate_transition_spec(
    source_variant: &str,
    target_variant: &str,
    easing: &ReactionEasingJson,
    duration_secs: f32,
) -> animationspec::TransitionSpec {
    let mut repeat_type = RepeatType::new();
    repeat_type.type_ =
        Some(repeat_type::Type::NoRepeat(::protobuf::well_known_types::empty::Empty::new()));

    let mut animations = Animations::new();
    animations.type_ = Some(animations::Type::Smooth(SmoothAnimation {
        duration: Some(seconds_to_proto_duration(duration_secs)).into(),
        repeat_type: Some(repeat_type).into(),
        easing: Some(easing_json_to_anim_easing(easing)).into(),
        ..Default::default()
    }));

    let spec = AnimationSpec {
        initial_delay: Some(::protobuf::well_known_types::duration::Duration::default()).into(),
        animation: Some(animations).into(),
        ..Default::default()
    };

    animationspec::TransitionSpec {
        source_variant: normalize_variant_string(source_variant),
        target_variant: normalize_variant_string(target_variant),
        animation_name: "Default".to_string(),
        spec: Some(spec).into(),
        ..Default::default()
    }
}

struct VariantTransitionCollector<'a> {
    variant_id_to_name: &'a HashMap<&'a str, &'a str>,
    hidden_node_policy: HiddenNodePolicy,
    transitions: Vec<animationspec::TransitionSpec>,
    seen_explicit: HashSet<(String, String)>,
    reverse_candidates: Vec<animationspec::TransitionSpec>,
}

impl<'a> VariantTransitionCollector<'a> {
    fn new(
        variant_id_to_name: &'a HashMap<&'a str, &'a str>,
        hidden_node_policy: HiddenNodePolicy,
    ) -> Self {
        Self {
            variant_id_to_name,
            hidden_node_policy,
            transitions: Vec::new(),
            seen_explicit: HashSet::new(),
            reverse_candidates: Vec::new(),
        }
    }

    fn collect(
        &mut self,
        node: &figma_schema::Node,
        from_variant_id: &str,
        from_variant_name: &str,
        is_root: bool,
    ) {
        if self.hidden_node_policy == HiddenNodePolicy::Skip && !node.visible {
            return;
        }
        // Nested ComponentSets and inner instances of this ComponentSet own their own transitions.
        if !is_root && matches!(node.data, figma_schema::NodeData::ComponentSet { .. }) {
            return;
        }
        if !is_root
            && matches!(
                &node.data,
                figma_schema::NodeData::Instance { component_id, .. }
                    if self.variant_id_to_name.contains_key(component_id.as_str())
            )
        {
            return;
        }

        for interaction in &node.interactions {
            let is_reversible = matches!(
                interaction.trigger,
                Some(TriggerJson::OnHover) | Some(TriggerJson::OnPress)
            );
            for action in interaction.iter_actions() {
                let ActionJson::Node {
                    destination_id: Some(dest_id),
                    navigation: NavigationJson::ChangeTo,
                    transition: Some(TransitionJson::SmartAnimate { easing, duration }),
                    ..
                } = action
                else {
                    continue;
                };
                if dest_id == from_variant_id {
                    continue;
                }
                let Some(&to_variant_name) = self.variant_id_to_name.get(dest_id.as_str()) else {
                    continue;
                };
                if is_reversible {
                    self.reverse_candidates.push(build_smart_animate_transition_spec(
                        to_variant_name,
                        from_variant_name,
                        easing,
                        *duration,
                    ));
                }
                let spec = build_smart_animate_transition_spec(
                    from_variant_name,
                    to_variant_name,
                    easing,
                    *duration,
                );
                let pair = (spec.source_variant.clone(), spec.target_variant.clone());
                if self.seen_explicit.insert(pair) {
                    self.transitions.push(spec);
                }
            }
        }

        for child in &node.children {
            self.collect(child, from_variant_id, from_variant_name, false);
        }
    }

    fn finish(mut self) -> Option<animationspec::AnimationMatrix> {
        for rev in self.reverse_candidates {
            let pair = (rev.source_variant.clone(), rev.target_variant.clone());
            if self.seen_explicit.insert(pair) {
                self.transitions.push(rev);
            }
        }
        (!self.transitions.is_empty()).then_some(animationspec::AnimationMatrix {
            transitions: self.transitions,
            ..Default::default()
        })
    }
}

/// Synthesizes an `AnimationMatrix` for a `ComponentSet` from Figma REST `CHANGE_TO`
/// `SMART_ANIMATE` prototype interactions on its child variant components and their layers.
///
/// Reversible triggers (`ON_HOVER`, `ON_PRESS`) also synthesize a fallback return transition when
/// no explicit transition covers the reverse variant pair. Hidden nodes are ignored when
/// `hidden_node_policy` is `HiddenNodePolicy::Skip`.
pub(crate) fn synthesize_component_set_animation_matrix(
    component_set: &figma_schema::Node,
    hidden_node_policy: HiddenNodePolicy,
) -> Option<animationspec::AnimationMatrix> {
    if !matches!(component_set.data, figma_schema::NodeData::ComponentSet { .. }) {
        return None;
    }
    if hidden_node_policy == HiddenNodePolicy::Skip && !component_set.visible {
        return None;
    }

    let variants: Vec<&figma_schema::Node> = component_set
        .children
        .iter()
        .filter(|child| {
            matches!(child.data, figma_schema::NodeData::Component { .. })
                && (hidden_node_policy == HiddenNodePolicy::Keep || child.visible)
        })
        .collect();
    if variants.is_empty() {
        return None;
    }

    let variant_id_to_name: HashMap<&str, &str> =
        variants.iter().map(|child| (child.id.as_str(), child.name.as_str())).collect();
    let mut collector = VariantTransitionCollector::new(&variant_id_to_name, hidden_node_policy);
    for variant in variants {
        collector.collect(variant, &variant.id, &variant.name, true);
    }
    collector.finish()
}

/// Returns true when `override_val` represents an active animation override rather than
/// `NoOverride`/empty.
fn is_explicit_animation_override(override_val: &animationspec::AnimationOverride) -> bool {
    !matches!(
        override_val.animation_override,
        None | Some(animation_override::Animation_override::NoOverride(_))
    )
}

/// Parses `shared_plugin_data["designcompose"]["squoosh"]` (or falls back to
/// `node.animation_override`) into a protobuf `AnimationOverride`.
pub(crate) fn parse_squoosh_plugin_override(
    node: &figma_schema::Node,
) -> Option<animationspec::AnimationOverride> {
    if let Some(plugin_data) = node.shared_plugin_data.get("designcompose") {
        if let Some(anim_str) = plugin_data.get("squoosh") {
            log::debug!(
                "Figma Import: Found squoosh animation plugin data string on node {}: {}",
                node.id,
                anim_str
            );
            match serde_json::from_str::<AnimationOverrideJson>(anim_str) {
                Ok(anim) => {
                    log::debug!(
                        "Figma Import: Successfully parsed squoosh animation format for node {}, name: {}",
                        node.id,
                        node.name
                    );
                    return Some(AnimationOverride::from(&anim).into());
                }
                Err(e) => {
                    log::error!(
                        "Figma Import: Failed to parse squoosh animation plugin data for node {}: {:?}",
                        node.id,
                        e
                    );
                }
            }
        } else {
            log::debug!("Figma Import: No animation plugin data found for node {}", node.id);
        }
    }
    node.animation_override.as_ref().map(|o| AnimationOverride::from(o).into())
}

/// Merges a lower-precedence `AnimationMatrix` into an optional `AnimationOverride` without
/// overriding explicit `DisableAnimations`, `Custom`, unrecognized variants, existing
/// `default_spec`, or existing `(source_variant, target_variant)` matrix pairs.
fn merge_matrix_into_override(
    target: &mut Option<animationspec::AnimationOverride>,
    lower_matrix: &animationspec::AnimationMatrix,
) {
    if lower_matrix.transitions.is_empty() && lower_matrix.default_spec.is_none() {
        return;
    }
    match target.as_mut().and_then(|o| o.animation_override.as_mut()) {
        Some(animation_override::Animation_override::DisableAnimations(_))
        | Some(animation_override::Animation_override::Custom(_)) => {}
        Some(animation_override::Animation_override::Matrix(existing)) => {
            if existing.default_spec.is_none() && lower_matrix.default_spec.is_some() {
                existing.default_spec = lower_matrix.default_spec.clone();
            }
            let existing_pairs: HashSet<(String, String)> = existing
                .transitions
                .iter()
                .map(|t| (t.source_variant.clone(), t.target_variant.clone()))
                .collect();
            for t in &lower_matrix.transitions {
                if !existing_pairs.contains(&(t.source_variant.clone(), t.target_variant.clone())) {
                    existing.transitions.push(t.clone());
                }
            }
        }
        None | Some(animation_override::Animation_override::NoOverride(_)) => {
            *target = Some(animationspec::AnimationOverride {
                animation_override: Some(animation_override::Animation_override::Matrix(
                    lower_matrix.clone(),
                )),
                ..Default::default()
            });
        }
        Some(_) => {
            log::debug!("Preserving unrecognized Animation_override variant");
        }
    }
}

/// Composes a higher-precedence explicit `AnimationOverride` over a lower-precedence one.
///
/// `DisableAnimations` or `Custom` at the higher tier replaces lower tiers outright; when both
/// tiers are `Matrix`, unrepresented transitions and `default_spec` from `lower` are merged as
/// fallbacks into `higher`. Non-explicit (`None`/`NoOverride`) higher tiers defer to `lower`.
fn merge_override_tiers(
    higher: Option<animationspec::AnimationOverride>,
    lower: Option<&animationspec::AnimationOverride>,
) -> Option<animationspec::AnimationOverride> {
    if !higher.as_ref().is_some_and(is_explicit_animation_override) {
        return lower.cloned().or(higher);
    }
    let high = higher?;
    if let (
        Some(animation_override::Animation_override::Matrix(_)),
        Some(animationspec::AnimationOverride {
            animation_override: Some(animation_override::Animation_override::Matrix(lower_matrix)),
            ..
        }),
    ) = (&high.animation_override, lower)
    {
        let mut wrapped = Some(high);
        merge_matrix_into_override(&mut wrapped, lower_matrix);
        return wrapped;
    }
    Some(high)
}

/// Single-source-of-truth resolver and recursive propagator for 4-tier `AnimationOverride`
/// precedence across `ComponentSet`, standalone `NodeVariant`, and nested `Instance` views:
///
/// `Instance squoosh > Variant Component squoosh > ComponentSet squoosh > synthesized REST AnimationMatrix`.
///
/// Must execute strictly before `Document::compute_component_overrides(&mut views)` so
/// unmodified instances (and unmodified nested template children) match their `NodeVariant`
/// templates without bloating `ComponentInfo.overrides_table` or clobbering `DisableAnimations`/custom specs.
pub(crate) struct AnimationOverridePropagator<'a> {
    id_index: &'a HashMap<String, &'a figma_schema::Node>,
    component_id_index: &'a HashMap<String, &'a figma_schema::Node>,
    hidden_node_policy: HiddenNodePolicy,
    squoosh_cache: HashMap<String, Option<animationspec::AnimationOverride>>,
    cs_matrix_cache: HashMap<String, Option<animationspec::AnimationMatrix>>,
    effective_override_cache: HashMap<(String, String), Option<animationspec::AnimationOverride>>,
}

impl<'a> AnimationOverridePropagator<'a> {
    pub(crate) fn new(
        id_index: &'a HashMap<String, &'a figma_schema::Node>,
        component_id_index: &'a HashMap<String, &'a figma_schema::Node>,
        hidden_node_policy: HiddenNodePolicy,
    ) -> Self {
        Self {
            id_index,
            component_id_index,
            hidden_node_policy,
            squoosh_cache: HashMap::new(),
            cs_matrix_cache: HashMap::new(),
            effective_override_cache: HashMap::new(),
        }
    }

    fn get_parsed_squoosh(
        &mut self,
        node: &figma_schema::Node,
    ) -> Option<animationspec::AnimationOverride> {
        self.squoosh_cache
            .entry(node.id.clone())
            .or_insert_with(|| parse_squoosh_plugin_override(node))
            .clone()
    }

    /// Returns (and memoizes) the synthesized REST `AnimationMatrix` for `cs_node`.
    pub(crate) fn get_or_synthesize_matrix(
        &mut self,
        cs_node: &figma_schema::Node,
    ) -> Option<animationspec::AnimationMatrix> {
        self.cs_matrix_cache
            .entry(cs_node.id.clone())
            .or_insert_with(|| {
                synthesize_component_set_animation_matrix(cs_node, self.hidden_node_policy)
            })
            .clone()
    }

    /// Resolves the effective `AnimationOverride` for a variant component within `cs_node` using
    /// tiers 2–4: `Variant Component squoosh > ComponentSet squoosh > synthesized REST AnimationMatrix`.
    pub(crate) fn resolve_effective_variant_override(
        &mut self,
        variant_id: &str,
        cs_node: &figma_schema::Node,
    ) -> Option<animationspec::AnimationOverride> {
        let cache_key = (cs_node.id.clone(), variant_id.to_string());
        if let Some(cached) = self.effective_override_cache.get(&cache_key) {
            return cached.clone();
        }

        let variant_override = self
            .id_index
            .get(variant_id)
            .copied()
            .and_then(|node| self.get_parsed_squoosh(node))
            .filter(is_explicit_animation_override);
        let cs_override = self.get_parsed_squoosh(cs_node).filter(is_explicit_animation_override);
        let mut effective = merge_override_tiers(variant_override, cs_override.as_ref());

        if let Some(matrix) = self.get_or_synthesize_matrix(cs_node) {
            merge_matrix_into_override(&mut effective, &matrix);
        }

        self.effective_override_cache.insert(cache_key, effective.clone());
        effective
    }

    /// Walks the `;`-delimited Figma instance template chain starting after `I<outer>;`, returning:
    /// - `Some(explicit)` when a template ancestor carries an active override,
    /// - `Some(no_override)` only when an intermediate template explicitly resets an active override
    ///   further up the chain (keeping intermediate template and instance descendants identical), or
    /// - `None` when no active override exists anywhere along the remaining template chain.
    fn resolve_template_chain(
        &mut self,
        mut rest: &str,
    ) -> Option<animationspec::AnimationOverride> {
        loop {
            let candidate_id =
                if rest.contains(';') { format!("I{rest}") } else { rest.to_string() };
            let next_rest = rest.split_once(';').map(|(_, tail)| tail);
            if let Some(parsed) = self
                .id_index
                .get(&candidate_id)
                .copied()
                .and_then(|node| self.get_parsed_squoosh(node))
            {
                if is_explicit_animation_override(&parsed) {
                    return Some(parsed);
                }
                // `NoOverride` ("Default") suppresses ancestors above it, and is retained only
                // when an active override exists further up the template chain.
                return next_rest
                    .and_then(|tail| self.resolve_template_chain(tail))
                    .filter(is_explicit_animation_override)
                    .map(|_| parsed);
            }
            let Some(tail) = next_rest else {
                return None;
            };
            rest = tail;
        }
    }

    fn resolve_explicit_or_template_override(
        &mut self,
        view: &View,
    ) -> Option<animationspec::AnimationOverride> {
        let direct_raw = view
            .style
            .as_ref()
            .and_then(|s| s.node_style.as_ref())
            .and_then(|ns| ns.animation_override.clone().into_option());
        let template_rest =
            view.id.strip_prefix('I').and_then(|s| s.split_once(';')).map(|(_, t)| t);
        match direct_raw {
            Some(ovr) if is_explicit_animation_override(&ovr) => Some(ovr),
            Some(no_ovr) => {
                // Retain `NoOverride` ("Default") only when resetting an active `;`-template
                // override so `compute_component_overrides` emits the reset when lower tiers are
                // `None`; otherwise canonicalize `NoOverride` to `None`.
                let has_explicit_template = template_rest
                    .and_then(|rest| self.resolve_template_chain(rest))
                    .is_some_and(|ovr| is_explicit_animation_override(&ovr));
                has_explicit_template.then_some(no_ovr)
            }
            None => template_rest.and_then(|rest| self.resolve_template_chain(rest)),
        }
    }

    fn resolve_view_override(&mut self, view: &View) -> Option<animationspec::AnimationOverride> {
        let comp_id =
            view.component_info.as_ref().map(|ci| ci.id.as_str()).filter(|id| !id.is_empty());

        // Variant Component views (whether or not ComponentInfo was populated) resolve Tiers 2-4 directly.
        if comp_id.is_none() || comp_id == Some(view.id.as_str()) {
            if let Some(cs_node) = self.component_id_index.get(view.id.as_str()).copied() {
                return self.resolve_effective_variant_override(&view.id, cs_node);
            }
        }

        let explicit = self.resolve_explicit_or_template_override(view);
        if let Some(comp_id) = comp_id {
            if let Some(cs_node) = self.component_id_index.get(comp_id).copied() {
                let lower = self.resolve_effective_variant_override(comp_id, cs_node);
                return merge_override_tiers(explicit, lower.as_ref());
            }
            return explicit;
        }

        if let Some(cs_node) = self
            .id_index
            .get(view.id.as_str())
            .copied()
            .filter(|n| matches!(n.data, figma_schema::NodeData::ComponentSet { .. }))
        {
            let mut effective = explicit;
            if let Some(matrix) = self.get_or_synthesize_matrix(cs_node) {
                merge_matrix_into_override(&mut effective, &matrix);
            }
            return effective;
        }

        explicit
    }

    /// Recursively applies effective `AnimationOverride` resolution onto `view` and its subtree.
    pub(crate) fn propagate(&mut self, view: &mut View) {
        match self.resolve_view_override(view) {
            Some(resolved) => {
                view.style_mut().node_style_mut().animation_override = Some(resolved).into();
            }
            None => {
                if let Some(ns) = view.style.as_mut().and_then(|s| s.node_style.as_mut()) {
                    ns.animation_override = None.into();
                }
            }
        }

        if let Some(View_data_type::Container(container)) =
            view.data.as_mut().and_then(|d| d.view_data_type.as_mut())
        {
            for child in &mut container.children {
                self.propagate(child);
            }
        }
    }
}

#[cfg(test)]
pub(crate) mod test_support {
    use super::*;
    use crate::reaction_schema::{ActionJson, InteractionJson, NavigationJson, TransitionJson};
    use std::sync::LazyLock;

    pub(crate) const SQUOOSH_DISABLE: &str = r#"{"override":"None"}"#;
    pub(crate) const SQUOOSH_DEFAULT: &str = r#"{"override":"Default"}"#;

    static DEFAULT_FRAME_COMMON: LazyLock<figma_schema::FrameCommon> = LazyLock::new(|| {
        serde_json::from_str(r#"{"constraints":{"vertical":"TOP","horizontal":"LEFT"}}"#)
            .expect("Default FrameCommon JSON must deserialize")
    });

    fn smooth_json_spec(millis: i64) -> serde_json::Value {
        serde_json::json!({
            "initial_delay": { "secs": 0, "nanos": 0 },
            "animation": {
                "Smooth": {
                    "duration": { "secs": millis / 1000, "nanos": (millis % 1000) * 1_000_000 },
                    "repeat_type": "NoRepeat",
                    "easing": "Linear"
                }
            }
        })
    }

    pub(crate) fn squoosh_custom_ms(millis: i64) -> String {
        serde_json::json!({
            "override": "Custom",
            "spec": smooth_json_spec(millis),
        })
        .to_string()
    }

    pub(crate) fn squoosh_matrix_json(
        default_millis: Option<i64>,
        transitions: &[(&str, &str, i64)],
    ) -> String {
        let transitions_json: Vec<serde_json::Value> = transitions
            .iter()
            .map(|(from, to, millis)| {
                serde_json::json!({
                    "from": from,
                    "to": to,
                    "spec": smooth_json_spec(*millis),
                })
            })
            .collect();
        let mut obj = serde_json::Map::new();
        if let Some(ms) = default_millis {
            obj.insert("default_spec".to_string(), smooth_json_spec(ms));
        }
        obj.insert("transitions".to_string(), serde_json::Value::Array(transitions_json));
        serde_json::Value::Object(obj).to_string()
    }

    pub(crate) fn test_node(
        id: &str,
        name: &str,
        data: figma_schema::NodeData,
        children: Vec<figma_schema::Node>,
    ) -> figma_schema::Node {
        figma_schema::Node {
            id: id.to_string(),
            name: name.to_string(),
            fills: Vec::new(),
            strokes: Vec::new(),
            children,
            visible: true,
            blend_mode: None,
            stroke_weight: None,
            individual_stroke_weights: None,
            stroke_align: None,
            corner_radius: None,
            rectangle_corner_radii: None,
            opacity: 1.0,
            effects: Vec::new(),
            export_settings: None,
            absolute_bounding_box: Some(figma_schema::Rectangle {
                x: Some(0.0),
                y: Some(0.0),
                width: Some(100.0),
                height: Some(50.0),
            }),
            absolute_render_bounds: None,
            shared_plugin_data: HashMap::new(),
            interactions: Vec::new(),
            animation_override: None,
            data,
            relative_transform: None,
            size: None,
            fill_geometry: None,
            stroke_geometry: None,
            stroke_cap: figma_schema::StrokeCap::None,
            bound_variables: None,
            explicit_variable_modes: None,
            min_width: None,
            min_height: None,
            max_width: None,
            max_height: None,
        }
    }

    pub(crate) fn test_frame(
        id: &str,
        name: &str,
        children: Vec<figma_schema::Node>,
    ) -> figma_schema::Node {
        test_node(
            id,
            name,
            figma_schema::NodeData::Frame { frame: DEFAULT_FRAME_COMMON.clone() },
            children,
        )
    }

    pub(crate) fn test_component(
        id: &str,
        name: &str,
        children: Vec<figma_schema::Node>,
    ) -> figma_schema::Node {
        test_node(
            id,
            name,
            figma_schema::NodeData::Component { frame: DEFAULT_FRAME_COMMON.clone() },
            children,
        )
    }

    pub(crate) fn test_component_set(
        id: &str,
        name: &str,
        children: Vec<figma_schema::Node>,
    ) -> figma_schema::Node {
        test_node(
            id,
            name,
            figma_schema::NodeData::ComponentSet { frame: DEFAULT_FRAME_COMMON.clone() },
            children,
        )
    }

    pub(crate) fn test_instance(
        id: &str,
        name: &str,
        component_id: &str,
        children: Vec<figma_schema::Node>,
    ) -> figma_schema::Node {
        test_node(
            id,
            name,
            figma_schema::NodeData::Instance {
                frame: DEFAULT_FRAME_COMMON.clone(),
                component_id: component_id.to_string(),
            },
            children,
        )
    }

    pub(crate) fn with_visible(mut node: figma_schema::Node, visible: bool) -> figma_schema::Node {
        node.visible = visible;
        node
    }

    pub(crate) fn with_interactions(
        mut node: figma_schema::Node,
        interactions: Vec<InteractionJson>,
    ) -> figma_schema::Node {
        node.interactions = interactions;
        node
    }

    pub(crate) fn with_squoosh(
        mut node: figma_schema::Node,
        squoosh_json: &str,
    ) -> figma_schema::Node {
        let mut dc_map = HashMap::new();
        dc_map.insert("squoosh".to_string(), squoosh_json.to_string());
        node.shared_plugin_data.insert("designcompose".to_string(), dc_map);
        node
    }

    pub(crate) fn smart_animate_change_to(
        trigger: TriggerJson,
        destination_id: &str,
        duration: f32,
    ) -> InteractionJson {
        smart_animate_change_to_with_easing(
            trigger,
            destination_id,
            ReactionEasingJson::Linear,
            duration,
        )
    }

    pub(crate) fn smart_animate_change_to_with_easing(
        trigger: TriggerJson,
        destination_id: &str,
        easing: ReactionEasingJson,
        duration: f32,
    ) -> InteractionJson {
        InteractionJson {
            trigger: Some(trigger),
            action: None,
            actions: vec![ActionJson::Node {
                destination_id: Some(destination_id.to_string()),
                navigation: NavigationJson::ChangeTo,
                transition: Some(TransitionJson::SmartAnimate { easing, duration }),
                preserve_scroll_position: false,
                overlay_relative_position: None,
            }],
        }
    }

    pub(crate) fn navigate_interaction(
        trigger: TriggerJson,
        destination_id: &str,
    ) -> InteractionJson {
        InteractionJson {
            trigger: Some(trigger),
            action: None,
            actions: vec![ActionJson::Node {
                destination_id: Some(destination_id.to_string()),
                navigation: NavigationJson::Navigate,
                transition: None,
                preserve_scroll_position: false,
                overlay_relative_position: None,
            }],
        }
    }

    pub(crate) fn test_file_response(
        doc_name: &str,
        page_children: Vec<figma_schema::Node>,
        extra_components: &[(&str, &str, &str, &str)],
        extra_component_sets: &[(&str, &str, &str)],
    ) -> figma_schema::FileResponse {
        let mut components = HashMap::new();
        let mut component_sets = HashMap::new();

        fn register_sets(
            nodes: &[figma_schema::Node],
            components: &mut HashMap<String, figma_schema::Component>,
            component_sets: &mut HashMap<String, figma_schema::ComponentSet>,
        ) {
            for node in nodes {
                if matches!(node.data, figma_schema::NodeData::ComponentSet { .. }) {
                    component_sets.insert(
                        node.id.clone(),
                        figma_schema::ComponentSet {
                            key: format!("cs_{}", node.id),
                            name: node.name.clone(),
                            description: String::new(),
                        },
                    );
                    for child in &node.children {
                        if matches!(child.data, figma_schema::NodeData::Component { .. }) {
                            components.insert(
                                child.id.clone(),
                                figma_schema::Component {
                                    key: format!("k_{}", child.id),
                                    name: child.name.clone(),
                                    description: String::new(),
                                    component_set_id: node.id.clone(),
                                },
                            );
                        }
                    }
                }
                register_sets(&node.children, components, component_sets);
            }
        }
        register_sets(&page_children, &mut components, &mut component_sets);

        for (id, key, name, cs_id) in extra_components {
            components.insert(
                (*id).to_string(),
                figma_schema::Component {
                    key: (*key).to_string(),
                    name: (*name).to_string(),
                    description: String::new(),
                    component_set_id: (*cs_id).to_string(),
                },
            );
        }
        for (id, key, name) in extra_component_sets {
            component_sets.insert(
                (*id).to_string(),
                figma_schema::ComponentSet {
                    key: (*key).to_string(),
                    name: (*name).to_string(),
                    description: String::new(),
                },
            );
        }

        let canvas = test_node(
            "0:1",
            "Page 1",
            figma_schema::NodeData::Canvas {
                background_color: None,
                prototype_start_node_id: None,
            },
            page_children,
        );
        let document =
            test_node("0:0", "Document", figma_schema::NodeData::Document {}, vec![canvas]);
        figma_schema::FileResponse {
            components,
            component_sets,
            document,
            version: String::new(),
            last_modified: String::new(),
            name: doc_name.to_string(),
            branches: None,
        }
    }

    pub(crate) fn view_animation_override(
        view: &View,
    ) -> Option<&animation_override::Animation_override> {
        view.style
            .as_ref()?
            .node_style
            .as_ref()?
            .animation_override
            .as_ref()?
            .animation_override
            .as_ref()
    }

    pub(crate) fn overrides_table_anim_override<'a>(
        view: &'a View,
        key: &str,
    ) -> Option<&'a animation_override::Animation_override> {
        view.component_info
            .as_ref()?
            .overrides_table
            .get(key)?
            .style
            .as_ref()?
            .node_style
            .as_ref()?
            .animation_override
            .as_ref()?
            .animation_override
            .as_ref()
    }

    pub(crate) fn smooth_duration(spec: &AnimationSpec) -> (i64, i32) {
        let Some(animations::Type::Smooth(smooth)) =
            spec.animation.as_ref().and_then(|a| a.type_.as_ref())
        else {
            panic!("Expected Smooth animation");
        };
        let dur = smooth.duration.as_ref().expect("Smooth animation must have duration");
        (dur.seconds, dur.nanos)
    }

    pub(crate) fn smooth_nanos(spec: &AnimationSpec) -> i32 {
        let (secs, nanos) = smooth_duration(spec);
        assert_eq!(secs, 0, "Expected sub-second Smooth animation duration");
        nanos
    }

    pub(crate) fn matrix_pair_nanos(
        matrix: &animationspec::AnimationMatrix,
        from: &str,
        to: &str,
    ) -> (i64, i32) {
        let t = matrix
            .transitions
            .iter()
            .find(|t| t.source_variant == from && t.target_variant == to)
            .unwrap_or_else(|| panic!("Missing transition {} -> {}", from, to));
        smooth_duration(t.spec.as_ref().unwrap())
    }

    pub(crate) fn build_multi_tier_button_cs(
        cs_id: &str,
        cs_name: &str,
        b_to_c_trigger: TriggerJson,
    ) -> figma_schema::Node {
        let cs_squoosh =
            squoosh_matrix_json(Some(500), &[("S=B", "S=A", 600), ("S=A", "S=B", 700)]);
        let variant_a_squoosh = squoosh_matrix_json(None, &[("S=A", "S=B", 900)]);

        with_squoosh(
            test_component_set(
                cs_id,
                cs_name,
                vec![
                    with_interactions(
                        with_squoosh(test_component("20:1", "S=A", vec![]), &variant_a_squoosh),
                        vec![smart_animate_change_to(TriggerJson::OnClick, "20:2", 0.2)],
                    ),
                    with_interactions(
                        test_component("20:2", "S=B", vec![]),
                        vec![smart_animate_change_to(b_to_c_trigger, "20:3", 0.25)],
                    ),
                    test_component("20:3", "S=C", vec![]),
                ],
            ),
            &cs_squoosh,
        )
    }
}

#[cfg(test)]
mod tests {
    use super::test_support::*;
    use super::*;
    use crate::animation_spec_schema::{
        AnimationOverrideJson, AnimationSpec as AnimationSpecJson, Animations as AnimationsJson,
        BezierCurve as BezierCurveJson, Duration, Easing as EasingJson, KeyFrame as KeyFrameJson,
        KeyFrameAnimation as KeyFrameAnimationJson, RepeatType as RepeatTypeJson,
        SmoothAnimation as SmoothAnimationJson, StopType as StopTypeJson,
    };
    use dc_bundle::animationspec::{
        animation_override, animations, easing, repeat_type, stop_type,
    };

    #[test]
    fn test_animation_override_from_json() {
        // Test Default
        let json_default = AnimationOverrideJson::Default;
        let proto_override: AnimationOverride = (&json_default).into();
        assert!(matches!(
            proto_override.0,
            animationspec::AnimationOverride {
                animation_override: Some(animation_override::Animation_override::NoOverride(_)),
                ..
            }
        ));

        // Test DisableAnimations
        let json_disable = AnimationOverrideJson::DisableAnimations;
        let proto_override: AnimationOverride = (&json_disable).into();
        assert!(matches!(
            proto_override.0,
            animationspec::AnimationOverride {
                animation_override: Some(
                    animation_override::Animation_override::DisableAnimations(_)
                ),
                ..
            }
        ));

        // Test Custom
        let json_custom = AnimationOverrideJson::Custom(AnimationSpecJson::default());
        let proto_override: AnimationOverride = (&json_custom).into();
        assert!(matches!(
            proto_override.0,
            animationspec::AnimationOverride {
                animation_override: Some(animation_override::Animation_override::Custom(_)),
                ..
            }
        ));
    }

    #[test]
    fn test_animation_spec_from_json() {
        let json_spec = AnimationSpecJson {
            initial_delay: Duration { secs: 1, nanos: 500_000_000.0 },
            animation: AnimationsJson::default(),
            interrupt_type: Some(StopTypeJson::Complete),
            ..Default::default()
        };
        let proto_spec: AnimationSpec = json_spec.into();
        assert_eq!(proto_spec.initial_delay.get_or_default().seconds, 1);
        assert_eq!(proto_spec.initial_delay.get_or_default().nanos, 500_000_000);
        assert!(proto_spec.animation.is_some());
        assert!(matches!(
            proto_spec.interrupt_type.as_ref().unwrap().type_,
            Some(stop_type::Type::Complete(_))
        ));

        // Test with no interrupt_type
        let json_spec_no_interrupt = AnimationSpecJson {
            initial_delay: Duration { secs: 1, nanos: 500_000_000.0 },
            animation: AnimationsJson::default(),
            interrupt_type: None,
            ..Default::default()
        };
        let proto_spec_no_interrupt: AnimationSpec = json_spec_no_interrupt.into();
        assert!(proto_spec_no_interrupt.interrupt_type.is_none());
    }

    #[test]
    fn test_animations_from_json() {
        // Test Smooth
        let json_smooth =
            AnimationsJson { smooth: Some(SmoothAnimationJson::default()), key_frame: None };
        let proto_anim: Animations = json_smooth.into();
        assert!(matches!(proto_anim.type_, Some(animations::Type::Smooth(_))));

        // Test KeyFrame
        let json_keyframe =
            AnimationsJson { smooth: None, key_frame: Some(KeyFrameAnimationJson::default()) };
        let proto_anim: Animations = json_keyframe.into();
        assert!(matches!(proto_anim.type_, Some(animations::Type::KeyFrame(_))));

        // Test None
        let json_none = AnimationsJson { smooth: None, key_frame: None };
        let proto_anim: Animations = json_none.into();
        assert!(proto_anim.type_.is_none());
    }

    #[test]
    fn test_smooth_animation_from_json() {
        let json_smooth = SmoothAnimationJson {
            duration: Duration { secs: 2, nanos: 0.0 },
            repeat_type: RepeatTypeJson::String("LoopForever".to_string()),
            easing: EasingJson::String("Linear".to_string()),
        };
        let proto_smooth: SmoothAnimation = json_smooth.into();
        assert_eq!(proto_smooth.duration.get_or_default().seconds, 2);
        assert_eq!(proto_smooth.duration.get_or_default().nanos, 0);
        assert!(matches!(
            proto_smooth.repeat_type.get_or_default().type_,
            Some(repeat_type::Type::LoopForever(_))
        ));
        assert!(matches!(
            proto_smooth.easing.get_or_default().type_,
            Some(easing::Type::Linear(_))
        ));
    }

    #[test]
    fn test_key_frame_animation_from_json() {
        let json_keyframe = KeyFrameAnimationJson {
            steps: vec![KeyFrameJson::default()],
            repeat_type: RepeatTypeJson::Repeat { repeat: 3 },
        };
        let proto_keyframe: KeyFrameAnimation = json_keyframe.into();
        assert_eq!(proto_keyframe.steps.len(), 1);
        assert!(matches!(
            proto_keyframe.repeat_type.get_or_default().type_,
            Some(repeat_type::Type::Repeat(3))
        ));
    }

    #[test]
    fn test_key_frame_from_json() {
        let json_keyframe =
            KeyFrameJson { value: 0.5, duration: Duration { secs: 0, nanos: 100_000_000.0 } };
        let proto_keyframe: KeyFrame = json_keyframe.into();
        assert_eq!(proto_keyframe.value, 0.5);
        assert_eq!(proto_keyframe.duration.get_or_default().seconds, 0);
        assert_eq!(proto_keyframe.duration.get_or_default().nanos, 100_000_000);
    }

    #[test]
    fn test_repeat_type_from_json() {
        // Test LoopForever
        let json_loop = RepeatTypeJson::String("LoopForever".to_string());
        let proto_repeat: RepeatType = json_loop.into();
        assert!(matches!(proto_repeat.type_, Some(repeat_type::Type::LoopForever(_))));

        // Test NoRepeat
        let json_no_repeat = RepeatTypeJson::String("NoRepeat".to_string());
        let proto_repeat: RepeatType = json_no_repeat.into();
        assert!(matches!(proto_repeat.type_, Some(repeat_type::Type::NoRepeat(_))));

        // Test Repeat(n)
        let json_repeat = RepeatTypeJson::Repeat { repeat: 5 };
        let proto_repeat: RepeatType = json_repeat.into();
        assert!(matches!(proto_repeat.type_, Some(repeat_type::Type::Repeat(5))));
    }

    #[test]
    fn test_easing_from_json() {
        // Test Linear
        let json_linear = EasingJson::String("Linear".to_string());
        let proto_easing: Easing = json_linear.into();
        assert!(matches!(proto_easing.type_, Some(easing::Type::Linear(_))));

        // Test Bezier
        let json_bezier =
            EasingJson::Bezier { bezier: BezierCurveJson { p0: 0.1, p1: 0.2, p2: 0.3, p3: 0.4 } };
        let proto_easing: Easing = json_bezier.into();
        match proto_easing.type_ {
            Some(easing::Type::Bezier(b)) => {
                assert_eq!(b.p0, 0.1);
                assert_eq!(b.p1, 0.2);
                assert_eq!(b.p2, 0.3);
                assert_eq!(b.p3, 0.4);
            }
            _ => panic!("Expected Bezier easing type, but got {:?}", proto_easing.type_),
        }
    }

    #[test]
    fn test_bezier_curve_from_json() {
        let json_bezier = BezierCurveJson { p0: 0.1, p1: 0.2, p2: 0.3, p3: 0.4 };
        let proto_bezier: BezierCurve = json_bezier.into();
        assert_eq!(proto_bezier.p0, 0.1);
        assert_eq!(proto_bezier.p1, 0.2);
        assert_eq!(proto_bezier.p2, 0.3);
        assert_eq!(proto_bezier.p3, 0.4);
    }

    #[test]
    fn test_stop_type_from_json() {
        // Test None
        let json_none = StopTypeJson::None;
        let proto_stop: StopType = json_none.into();
        assert!(proto_stop.type_.is_none());

        // Test ResetToStart
        let json_reset = StopTypeJson::ResetToStart;
        let proto_stop: StopType = json_reset.into();
        assert!(matches!(proto_stop.type_, Some(stop_type::Type::ResetToStart(_))));

        // Test Complete
        let json_complete = StopTypeJson::Complete;
        let proto_stop: StopType = json_complete.into();
        assert!(matches!(proto_stop.type_, Some(stop_type::Type::Complete(_))));

        // Test Stop
        let json_stop = StopTypeJson::Stop;
        let proto_stop: StopType = json_stop.into();
        assert!(matches!(proto_stop.type_, Some(stop_type::Type::Stop(_))));
    }

    #[test]
    fn test_duration_from_json() {
        let json_duration = Duration { secs: 10, nanos: 123.0 };
        let proto_duration: ::protobuf::well_known_types::duration::Duration = json_duration.into();
        assert_eq!(proto_duration.seconds, 10);
        assert_eq!(proto_duration.nanos, 123);
    }

    #[test]
    fn test_custom_keyframe_value_json_variants() {
        // Test Radii
        let json_radii = CustomKeyframeJson {
            fraction: 0.5,
            value_json: serde_json::json!([1.0, 2.0, 3.0, 4.0]),
            easing: EasingJson::String("Linear".to_string()),
        };
        let proto_radii: CustomKeyframe = json_radii.into();
        match proto_radii.value.as_ref().and_then(|v| v.value.as_ref()) {
            Some(animationspec::custom_keyframe_value::Value::Radii(r)) => {
                assert_eq!(r.top_left, 1.0);
                assert_eq!(r.top_right, 2.0);
                assert_eq!(r.bottom_right, 3.0);
                assert_eq!(r.bottom_left, 4.0);
            }
            other => panic!("Expected Radii variant, got {:?}", other),
        }
    }

    #[test]
    fn test_transition_custom_keyframe_override_precedence() {
        let root_tl = CustomTimelineJson {
            target_easing: EasingJson::String("Linear".to_string()),
            keyframes: vec![CustomKeyframeJson {
                fraction: 0.0,
                value_json: serde_json::json!(10.0),
                easing: EasingJson::String("Linear".to_string()),
            }],
        };
        let transition_tl = CustomTimelineJson {
            target_easing: EasingJson::String("Linear".to_string()),
            keyframes: vec![CustomKeyframeJson {
                fraction: 0.0,
                value_json: serde_json::json!(99.0),
                easing: EasingJson::String("Linear".to_string()),
            }],
        };

        let mut custom_keyframe_data = std::collections::HashMap::new();
        custom_keyframe_data.insert("node-opacity".to_string(), transition_tl);

        let mut root_timelines = std::collections::HashMap::new();
        root_timelines.insert("node-opacity".to_string(), root_tl);

        let transition_json = TransitionSpecJson {
            from: "A".to_string(),
            to: "B".to_string(),
            name: String::new(),
            spec: None,
            timelines: root_timelines,
            custom_keyframe_data,
        };
        let proto_transition: animationspec::TransitionSpec = transition_json.into();
        let tl = proto_transition.timelines.get("node-opacity").expect("timeline should exist");
        let first_kf = &tl.keyframes[0];
        match first_kf.value.as_ref().and_then(|v| v.value.as_ref()) {
            Some(animationspec::custom_keyframe_value::Value::Scalar(val)) => {
                assert_eq!(
                    *val, 99.0,
                    "Transition override (99.0) should take precedence over root timeline (10.0)"
                );
            }
            other => panic!("Expected Scalar(99.0), got {:?}", other),
        }
    }

    #[test]
    fn test_root_timeline_deduplication_and_merging() {
        let root_tl = CustomTimelineJson {
            target_easing: EasingJson::String("Linear".to_string()),
            keyframes: vec![CustomKeyframeJson {
                fraction: 0.0,
                value_json: serde_json::json!(5.0),
                easing: EasingJson::String("Linear".to_string()),
            }],
        };
        let override_tl = CustomTimelineJson {
            target_easing: EasingJson::String("Linear".to_string()),
            keyframes: vec![CustomKeyframeJson {
                fraction: 0.0,
                value_json: serde_json::json!(42.0),
                easing: EasingJson::String("Linear".to_string()),
            }],
        };

        let mut root_timelines = std::collections::HashMap::new();
        root_timelines.insert("shared-prop".to_string(), root_tl);

        let transition1 = TransitionSpecJson {
            from: "A".to_string(),
            to: "B".to_string(),
            name: String::new(),
            spec: None,
            timelines: std::collections::HashMap::new(),
            custom_keyframe_data: std::collections::HashMap::new(),
        };

        let mut t2_timelines = std::collections::HashMap::new();
        t2_timelines.insert("shared-prop".to_string(), override_tl);
        let transition2 = TransitionSpecJson {
            from: "B".to_string(),
            to: "C".to_string(),
            name: String::new(),
            spec: None,
            timelines: t2_timelines,
            custom_keyframe_data: std::collections::HashMap::new(),
        };

        let matrix_json = AnimationMatrixJson {
            default_spec: None,
            transitions: vec![transition1, transition2],
            timelines: root_timelines,
            custom_keyframe_data: std::collections::HashMap::new(),
        };

        let proto_matrix: animationspec::AnimationMatrix = matrix_json.into();
        assert_eq!(proto_matrix.transitions.len(), 2);

        let t1 = &proto_matrix.transitions[0];
        let t1_tl = t1.timelines.get("shared-prop").expect("t1 should inherit root timeline");
        match t1_tl.keyframes[0].value.as_ref().and_then(|v| v.value.as_ref()) {
            Some(animationspec::custom_keyframe_value::Value::Scalar(val)) => {
                assert_eq!(*val, 5.0);
            }
            other => panic!("Expected Scalar(5.0), got {:?}", other),
        }

        let t2 = &proto_matrix.transitions[1];
        let t2_tl = t2.timelines.get("shared-prop").expect("t2 should have override timeline");
        match t2_tl.keyframes[0].value.as_ref().and_then(|v| v.value.as_ref()) {
            Some(animationspec::custom_keyframe_value::Value::Scalar(val)) => {
                assert_eq!(*val, 42.0);
            }
            other => panic!("Expected Scalar(42.0), got {:?}", other),
        }
    }

    #[test]
    fn test_all_easing_variants_and_duration_edge_cases() {
        use crate::reaction_schema::{BezierJson, SpringJson};

        let linear = easing_json_to_anim_easing(&ReactionEasingJson::Linear);
        assert!(matches!(linear.type_, Some(easing::Type::Linear(_))));

        let linear_bezier = easing_json_to_anim_easing(&ReactionEasingJson::CustomCubicBezier {
            bezier: BezierJson { x1: 0.0, y1: 0.0, x2: 1.0, y2: 1.0 },
        });
        assert!(matches!(linear_bezier.type_, Some(easing::Type::Linear(_))));

        let custom = easing_json_to_anim_easing(&ReactionEasingJson::CustomCubicBezier {
            bezier: BezierJson { x1: 0.4, y1: 0.0, x2: 0.2, y2: 1.0 },
        });
        match custom.type_ {
            Some(easing::Type::Bezier(b)) => {
                assert!((b.p0 - 0.4).abs() < 1e-6);
                assert!((b.p1 - 0.0).abs() < 1e-6);
                assert!((b.p2 - 0.2).abs() < 1e-6);
                assert!((b.p3 - 1.0).abs() < 1e-6);
            }
            other => panic!("Expected Bezier, got {:?}", other),
        }

        let cubic_cases = [
            (ReactionEasingJson::EaseIn, (0.12_f32, 0.0_f32, 0.39_f32, 0.0_f32)),
            (ReactionEasingJson::EaseOut, (0.61, 1.0, 0.88, 1.0)),
            (ReactionEasingJson::EaseInAndOut, (0.37, 0.0, 0.63, 1.0)),
            (ReactionEasingJson::EaseInBack, (0.36, 0.0, 0.66, -0.56)),
            (ReactionEasingJson::EaseOutBack, (0.34, 1.56, 0.64, 1.0)),
            (ReactionEasingJson::EaseInAndOutBack, (0.68, -0.6, 0.32, 1.6)),
        ];
        for (variant, (p0, p1, p2, p3)) in cubic_cases {
            let converted = easing_json_to_anim_easing(&variant);
            match converted.type_ {
                Some(easing::Type::Bezier(b)) => {
                    assert!((b.p0 - p0).abs() < 1e-6, "p0 mismatch for {:?}", variant);
                    assert!((b.p1 - p1).abs() < 1e-6, "p1 mismatch for {:?}", variant);
                    assert!((b.p2 - p2).abs() < 1e-6, "p2 mismatch for {:?}", variant);
                    assert!((b.p3 - p3).abs() < 1e-6, "p3 mismatch for {:?}", variant);
                }
                other => panic!("Expected Bezier for {:?}, got {:?}", variant, other),
            }
        }

        let spring_variants = [
            ReactionEasingJson::Gentle,
            ReactionEasingJson::Quick,
            ReactionEasingJson::Bouncy,
            ReactionEasingJson::Slow,
            ReactionEasingJson::CustomSpring {
                spring: SpringJson { mass: 1.0, stiffness: 220.0, damping: 18.0 },
            },
        ];
        for variant in spring_variants {
            let converted = easing_json_to_anim_easing(&variant);
            match converted.type_ {
                Some(easing::Type::Bezier(b)) => {
                    assert!((b.p0 - 0.22).abs() < 1e-6);
                    assert!((b.p1 - 1.0).abs() < 1e-6);
                    assert!((b.p2 - 0.36).abs() < 1e-6);
                    assert!((b.p3 - 1.0).abs() < 1e-6);
                }
                other => {
                    panic!("Expected ease-out Bezier for spring {:?}, got {:?}", variant, other)
                }
            }
        }

        // IEEE-754 f32 duration rounding, non-positive / non-finite clamping, and huge f32 bounds.
        let d_ieee = seconds_to_proto_duration(0.3000000119);
        assert_eq!(d_ieee.seconds, 0);
        assert_eq!(d_ieee.nanos, 300_000_000);

        let d_decimal = seconds_to_proto_duration(1.022);
        assert_eq!(d_decimal.seconds, 1);
        assert_eq!(d_decimal.nanos, 22_000_000);

        for zero_input in [0.0_f32, -0.5, f32::NAN, f32::INFINITY, f32::NEG_INFINITY] {
            let d = seconds_to_proto_duration(zero_input);
            assert_eq!(d.seconds, 0, "Expected 0 seconds for {:?}", zero_input);
            assert_eq!(d.nanos, 0, "Expected 0 nanos for {:?}", zero_input);
        }

        let d_huge = seconds_to_proto_duration(1e16_f32);
        assert_eq!(d_huge.seconds, MAX_PROTO_DURATION_SECS as i64);
        assert_eq!(d_huge.nanos, 0);
    }

    #[test]
    fn test_synthesize_matrix_isolation_destinations_and_hidden_policy() {
        use crate::reaction_schema::{ActionJson, InteractionJson, NavigationJson, TransitionJson};

        let make_change_to = |dest_id: &str, duration: f32| ActionJson::Node {
            destination_id: Some(dest_id.to_string()),
            navigation: NavigationJson::ChangeTo,
            transition: Some(TransitionJson::SmartAnimate {
                easing: ReactionEasingJson::Linear,
                duration,
            }),
            preserve_scroll_position: false,
            overlay_relative_position: None,
        };

        let inner_set = test_component_set(
            "410:0",
            "InnerSet",
            vec![
                with_interactions(
                    test_component("410:1", "Inner=One", vec![]),
                    vec![smart_animate_change_to(TriggerJson::OnClick, "410:2", 0.5)],
                ),
                test_component("410:2", "Inner=Two", vec![]),
            ],
        );
        let variant_a = with_interactions(
            test_component(
                "400:1",
                "Theme=Dark, State=A",
                vec![
                    with_visible(
                        with_interactions(
                            test_frame("400:11", "HiddenLayer", vec![]),
                            vec![smart_animate_change_to(TriggerJson::OnClick, "400:2", 0.8)],
                        ),
                        false,
                    ),
                    with_interactions(
                        test_frame("400:12", "VisibleLayer", vec![]),
                        vec![smart_animate_change_to(TriggerJson::OnClick, "400:2", 0.25)],
                    ),
                    inner_set,
                ],
            ),
            vec![InteractionJson {
                trigger: Some(TriggerJson::OnClick),
                action: None,
                actions: vec![
                    make_change_to("400:1", 0.1),
                    make_change_to("999:9", 0.15),
                    make_change_to("400:3", 0.12),
                ],
            }],
        );
        let cs_node = test_component_set(
            "400:0",
            "OuterSet",
            vec![
                variant_a,
                test_component("400:2", "State=B, Theme=Dark", vec![]),
                with_visible(
                    with_interactions(
                        test_component("400:3", "State=Hidden", vec![]),
                        vec![smart_animate_change_to(TriggerJson::OnClick, "400:1", 0.6)],
                    ),
                    false,
                ),
            ],
        );

        // HiddenNodePolicy::Skip: hidden layer does not emit or shadow VisibleLayer (0.25s),
        // hidden variant State=Hidden is excluded, inner ComponentSet does not leak, self/external ignored,
        // and multi-property variant names normalize alphabetically.
        let skip_matrix =
            synthesize_component_set_animation_matrix(&cs_node, HiddenNodePolicy::Skip).unwrap();
        assert_eq!(skip_matrix.transitions.len(), 1);
        assert_eq!(
            matrix_pair_nanos(&skip_matrix, "State=A, Theme=Dark", "State=B, Theme=Dark"),
            (0, 250_000_000)
        );

        // HiddenNodePolicy::Keep: hidden layer (first child, 0.8s) and State=Hidden participate,
        // while inner ComponentSet, self destination, and external destination stay excluded.
        let keep_matrix =
            synthesize_component_set_animation_matrix(&cs_node, HiddenNodePolicy::Keep).unwrap();
        assert_eq!(keep_matrix.transitions.len(), 3);
        assert_eq!(
            matrix_pair_nanos(&keep_matrix, "State=A, Theme=Dark", "State=B, Theme=Dark"),
            (0, 800_000_000)
        );
        assert!(keep_matrix.transitions.iter().any(
            |t| t.source_variant == "State=A, Theme=Dark" && t.target_variant == "State=Hidden"
        ));
        assert!(keep_matrix.transitions.iter().any(
            |t| t.source_variant == "State=Hidden" && t.target_variant == "State=A, Theme=Dark"
        ));
        assert!(!keep_matrix.transitions.iter().any(|t| t.source_variant.starts_with("Inner=")));
    }

    #[test]
    fn test_merge_synthesized_matrix_preserves_custom_and_upgrades_no_override() {
        let synth = animationspec::AnimationMatrix {
            transitions: vec![animationspec::TransitionSpec {
                source_variant: "State=A".to_string(),
                target_variant: "State=B".to_string(),
                animation_name: "Default".to_string(),
                ..Default::default()
            }],
            ..Default::default()
        };

        // Custom AnimationOverride must remain untouched.
        let custom_node =
            with_squoosh(test_frame("500:1", "CustomNode", vec![]), &squoosh_custom_ms(400));
        let mut custom_override = parse_squoosh_plugin_override(&custom_node);
        merge_matrix_into_override(&mut custom_override, &synth);
        assert!(matches!(
            custom_override.as_ref().and_then(|o| o.animation_override.as_ref()),
            Some(animation_override::Animation_override::Custom(_))
        ));

        // NoOverride ("override": "Default") must adopt the synthesized Matrix.
        let default_node =
            with_squoosh(test_frame("500:2", "DefaultNode", vec![]), SQUOOSH_DEFAULT);
        let mut default_override = parse_squoosh_plugin_override(&default_node);
        assert!(matches!(
            default_override.as_ref().and_then(|o| o.animation_override.as_ref()),
            Some(animation_override::Animation_override::NoOverride(_))
        ));
        merge_matrix_into_override(&mut default_override, &synth);
        match default_override.as_ref().and_then(|o| o.animation_override.as_ref()) {
            Some(animation_override::Animation_override::Matrix(m)) => {
                assert_eq!(m.transitions.len(), 1);
                assert_eq!(m.transitions[0].source_variant, "State=A");
                assert_eq!(m.transitions[0].target_variant, "State=B");
            }
            other => panic!("Expected Matrix after merging into NoOverride, got {:?}", other),
        }

        // DisableAnimations must remain untouched.
        let disabled_node =
            with_squoosh(test_frame("500:3", "DisabledNode", vec![]), SQUOOSH_DISABLE);
        let mut disabled_override = parse_squoosh_plugin_override(&disabled_node);
        merge_matrix_into_override(&mut disabled_override, &synth);
        assert!(matches!(
            disabled_override.as_ref().and_then(|o| o.animation_override.as_ref()),
            Some(animation_override::Animation_override::DisableAnimations(_))
        ));
    }

    fn make_smooth_spec_proto(secs: i64, nanos: i32) -> animationspec::AnimationSpec {
        let mut repeat_type = RepeatType::new();
        repeat_type.type_ =
            Some(repeat_type::Type::NoRepeat(::protobuf::well_known_types::empty::Empty::new()));
        let mut anim_easing = Easing::new();
        anim_easing.type_ =
            Some(easing::Type::Linear(::protobuf::well_known_types::empty::Empty::new()));
        let mut animations = Animations::new();
        animations.type_ = Some(animations::Type::Smooth(SmoothAnimation {
            duration: Some(::protobuf::well_known_types::duration::Duration {
                seconds: secs,
                nanos,
                ..Default::default()
            })
            .into(),
            repeat_type: Some(repeat_type).into(),
            easing: Some(anim_easing).into(),
            ..Default::default()
        }));
        AnimationSpec { animation: Some(animations).into(), ..Default::default() }
    }

    fn make_test_view(
        id: &str,
        name: &str,
        component_info: Option<dc_bundle::view::ComponentInfo>,
        anim_override: Option<animationspec::AnimationOverride>,
    ) -> View {
        use dc_bundle::positioning::ScrollInfo;
        use dc_bundle::view::view::RenderMethod;
        use dc_bundle::view_shape::ViewShape;
        use dc_bundle::view_style::ViewStyle;

        let mut style = ViewStyle::new_default();
        style.node_style_mut().animation_override = anim_override.into();
        View::new_rect(
            &id.to_string(),
            &name.to_string(),
            ViewShape::default(),
            style,
            component_info,
            None,
            ScrollInfo::new_default(),
            None,
            None,
            RenderMethod::RENDER_METHOD_NONE,
            HashMap::new(),
        )
    }

    #[test]
    fn normalize_variant_string_trims_single_property_and_trailing_commas() {
        assert_eq!(normalize_variant_string("State=A, "), "State=A");
        assert_eq!(normalize_variant_string(" State=A "), "State=A");
        assert_eq!(normalize_variant_string(" , "), "");
        assert_eq!(normalize_variant_string(""), "");
        assert_eq!(normalize_variant_string("*"), "*");
        assert_eq!(normalize_variant_string(" Theme=Dark , State=A , "), "State=A, Theme=Dark");
    }

    #[test]
    fn merge_matrix_into_override_merges_default_spec_when_transitions_empty() {
        let lower_default = make_smooth_spec_proto(0, 500_000_000);
        let lower_matrix = animationspec::AnimationMatrix {
            default_spec: Some(lower_default).into(),
            transitions: Vec::new(),
            ..Default::default()
        };

        // None -> Matrix with default_spec and empty transitions.
        let mut target_none: Option<animationspec::AnimationOverride> = None;
        merge_matrix_into_override(&mut target_none, &lower_matrix);
        match target_none.as_ref().and_then(|o| o.animation_override.as_ref()) {
            Some(animation_override::Animation_override::Matrix(m)) => {
                assert!(m.transitions.is_empty());
                assert_eq!(smooth_duration(m.default_spec.as_ref().unwrap()), (0, 500_000_000));
            }
            other => panic!("Expected Matrix, got {:?}", other),
        }

        // NoOverride -> Matrix with default_spec.
        let mut target_no_override = Some(animationspec::AnimationOverride {
            animation_override: Some(animation_override::Animation_override::NoOverride(
                ::protobuf::well_known_types::empty::Empty::new(),
            )),
            ..Default::default()
        });
        merge_matrix_into_override(&mut target_no_override, &lower_matrix);
        match target_no_override.as_ref().and_then(|o| o.animation_override.as_ref()) {
            Some(animation_override::Animation_override::Matrix(m)) => {
                assert!(m.transitions.is_empty());
                assert_eq!(smooth_duration(m.default_spec.as_ref().unwrap()), (0, 500_000_000));
            }
            other => panic!("Expected Matrix, got {:?}", other),
        }

        // Existing Matrix without default_spec inherits lower default_spec and keeps transitions.
        let mut target_matrix_no_default = Some(animationspec::AnimationOverride {
            animation_override: Some(animation_override::Animation_override::Matrix(
                animationspec::AnimationMatrix {
                    default_spec: None.into(),
                    transitions: vec![animationspec::TransitionSpec {
                        source_variant: "State=A".to_string(),
                        target_variant: "State=B".to_string(),
                        animation_name: "Default".to_string(),
                        spec: Some(make_smooth_spec_proto(0, 200_000_000)).into(),
                        ..Default::default()
                    }],
                    ..Default::default()
                },
            )),
            ..Default::default()
        });
        merge_matrix_into_override(&mut target_matrix_no_default, &lower_matrix);
        match target_matrix_no_default.as_ref().and_then(|o| o.animation_override.as_ref()) {
            Some(animation_override::Animation_override::Matrix(m)) => {
                assert_eq!(m.transitions.len(), 1);
                assert_eq!(smooth_duration(m.default_spec.as_ref().unwrap()), (0, 500_000_000));
            }
            other => panic!("Expected Matrix, got {:?}", other),
        }

        // Existing Matrix with its own default_spec keeps the higher-tier default_spec.
        let mut target_matrix_with_default = Some(animationspec::AnimationOverride {
            animation_override: Some(animation_override::Animation_override::Matrix(
                animationspec::AnimationMatrix {
                    default_spec: Some(make_smooth_spec_proto(0, 900_000_000)).into(),
                    transitions: Vec::new(),
                    ..Default::default()
                },
            )),
            ..Default::default()
        });
        merge_matrix_into_override(&mut target_matrix_with_default, &lower_matrix);
        match target_matrix_with_default.as_ref().and_then(|o| o.animation_override.as_ref()) {
            Some(animation_override::Animation_override::Matrix(m)) => {
                assert_eq!(smooth_duration(m.default_spec.as_ref().unwrap()), (0, 900_000_000));
            }
            other => panic!("Expected Matrix, got {:?}", other),
        }
    }

    #[test]
    fn propagator_synthesizes_reversible_on_press_with_explicit_precedence() {
        let cs_node = test_component_set(
            "20:0",
            "PressSet",
            vec![
                with_interactions(
                    test_component("20:1", "State=Idle", vec![]),
                    vec![
                        smart_animate_change_to(TriggerJson::OnPress, "20:2", 0.2),
                        smart_animate_change_to(TriggerJson::OnPress, "20:3", 0.3),
                    ],
                ),
                test_component("20:2", "State=Pressed", vec![]),
                with_interactions(
                    test_component("20:3", "State=ExplicitBack", vec![]),
                    vec![smart_animate_change_to(TriggerJson::OnClick, "20:1", 0.45)],
                ),
            ],
        );

        let mut id_index = HashMap::new();
        id_index.insert(cs_node.id.clone(), &cs_node);
        let mut component_id_index = HashMap::new();
        for child in &cs_node.children {
            id_index.insert(child.id.clone(), child);
            component_id_index.insert(child.id.clone(), &cs_node);
        }

        let mut propagator = AnimationOverridePropagator::new(
            &id_index,
            &component_id_index,
            HiddenNodePolicy::Skip,
        );
        let mut inst_view = make_test_view(
            "10:1",
            "#PressPill",
            Some(dc_bundle::view::ComponentInfo {
                id: "20:1".to_string(),
                name: "State=Idle".to_string(),
                component_set_name: "PressSet".to_string(),
                ..Default::default()
            }),
            None,
        );
        propagator.propagate(&mut inst_view);

        let Some(animation_override::Animation_override::Matrix(matrix)) =
            view_animation_override(&inst_view)
        else {
            panic!("Expected synthesized Matrix on instance view");
        };

        assert_eq!(matrix.transitions.len(), 4);
        assert_eq!(matrix_pair_nanos(matrix, "State=Idle", "State=Pressed"), (0, 200_000_000));
        assert_eq!(
            matrix_pair_nanos(matrix, "State=Pressed", "State=Idle"),
            (0, 200_000_000),
            "ON_PRESS without explicit reverse must synthesize reversible return transition"
        );
        assert_eq!(matrix_pair_nanos(matrix, "State=Idle", "State=ExplicitBack"), (0, 300_000_000));
        assert_eq!(
            matrix_pair_nanos(matrix, "State=ExplicitBack", "State=Idle"),
            (0, 450_000_000),
            "Explicit ON_CLICK reverse transition must take precedence over ON_PRESS fallback"
        );
    }

    #[test]
    fn propagator_composes_multi_tier_matrices_and_isolates_remote_name_collision() {
        let cs_node = build_multi_tier_button_cs("20:0", "Button", TriggerJson::OnClick);

        let mut id_index = HashMap::new();
        id_index.insert(cs_node.id.clone(), &cs_node);
        let mut component_id_index = HashMap::new();
        for child in &cs_node.children {
            id_index.insert(child.id.clone(), child);
            component_id_index.insert(child.id.clone(), &cs_node);
        }

        let mut propagator = AnimationOverridePropagator::new(
            &id_index,
            &component_id_index,
            HiddenNodePolicy::Skip,
        );

        let btn_info = || {
            Some(dc_bundle::view::ComponentInfo {
                id: "20:1".to_string(),
                name: "S=A".to_string(),
                component_set_name: "Button".to_string(),
                ..Default::default()
            })
        };

        // 1. Instance Matrix (S=A->S=B 300ms, S=C->S=A 350ms, no default_spec) composes Variant (S=A->S=B 900ms),
        //    ComponentSet (S=B->S=A 600ms, default_spec 500ms), and REST (S=B->S=C 250ms) without overwriting higher tiers.
        let inst_matrix_squoosh =
            squoosh_matrix_json(None, &[("S=A", "S=B", 300), ("S=C", "S=A", 350)]);
        let inst_matrix_node = with_squoosh(
            test_instance("10:1", "#InstMatrix", "20:1", vec![]),
            &inst_matrix_squoosh,
        );
        let mut inst_matrix_view = make_test_view(
            "10:1",
            "#InstMatrix",
            btn_info(),
            parse_squoosh_plugin_override(&inst_matrix_node),
        );
        propagator.propagate(&mut inst_matrix_view);

        let Some(animation_override::Animation_override::Matrix(composed_matrix)) =
            view_animation_override(&inst_matrix_view)
        else {
            panic!("Expected composed Matrix on #InstMatrix");
        };
        assert_eq!(
            smooth_duration(composed_matrix.default_spec.as_ref().unwrap()),
            (0, 500_000_000),
            "Instance Matrix without default_spec must inherit ComponentSet default_spec"
        );
        assert_eq!(composed_matrix.transitions.len(), 4);
        assert_eq!(matrix_pair_nanos(composed_matrix, "S=A", "S=B"), (0, 300_000_000));
        assert_eq!(matrix_pair_nanos(composed_matrix, "S=C", "S=A"), (0, 350_000_000));
        assert_eq!(matrix_pair_nanos(composed_matrix, "S=B", "S=A"), (0, 600_000_000));
        assert_eq!(matrix_pair_nanos(composed_matrix, "S=B", "S=C"), (0, 250_000_000));

        // 2. Instance Matrix with its own default_spec keeps its higher-tier default_spec (150ms).
        let inst_with_default = animationspec::AnimationOverride {
            animation_override: Some(animation_override::Animation_override::Matrix(
                animationspec::AnimationMatrix {
                    default_spec: Some(make_smooth_spec_proto(0, 150_000_000)).into(),
                    transitions: Vec::new(),
                    ..Default::default()
                },
            )),
            ..Default::default()
        };
        let mut inst_default_matrix_view =
            make_test_view("10:2", "#InstDefaultMatrix", btn_info(), Some(inst_with_default));
        propagator.propagate(&mut inst_default_matrix_view);
        let Some(animation_override::Animation_override::Matrix(m_with_def)) =
            view_animation_override(&inst_default_matrix_view)
        else {
            panic!("Expected Matrix");
        };
        assert_eq!(smooth_duration(m_with_def.default_spec.as_ref().unwrap()), (0, 150_000_000));
        assert_eq!(m_with_def.transitions.len(), 3);

        // 3. Instance Custom replaces lower-tier Matrices outright.
        let mut inst_custom_view = make_test_view(
            "10:3",
            "#InstCustom",
            btn_info(),
            Some(animationspec::AnimationOverride {
                animation_override: Some(animation_override::Animation_override::Custom(
                    make_smooth_spec_proto(0, 400_000_000),
                )),
                ..Default::default()
            }),
        );
        propagator.propagate(&mut inst_custom_view);
        match view_animation_override(&inst_custom_view) {
            Some(animation_override::Animation_override::Custom(spec)) => {
                assert_eq!(smooth_duration(spec), (0, 400_000_000));
            }
            other => panic!("Expected Custom override, got {:?}", other),
        }

        // 4. Instance NoOverride ("Default") inherits composed Variant > ComponentSet > REST Matrix.
        let mut inst_no_override_view = make_test_view(
            "10:4",
            "#InstNoOverride",
            btn_info(),
            Some(animationspec::AnimationOverride {
                animation_override: Some(animation_override::Animation_override::NoOverride(
                    ::protobuf::well_known_types::empty::Empty::new(),
                )),
                ..Default::default()
            }),
        );
        propagator.propagate(&mut inst_no_override_view);
        let Some(animation_override::Animation_override::Matrix(no_override_matrix)) =
            view_animation_override(&inst_no_override_view)
        else {
            panic!("Expected Matrix on NoOverride instance");
        };
        assert_eq!(
            smooth_duration(no_override_matrix.default_spec.as_ref().unwrap()),
            (0, 500_000_000)
        );
        assert_eq!(no_override_matrix.transitions.len(), 3);
        assert_eq!(matrix_pair_nanos(no_override_matrix, "S=A", "S=B"), (0, 900_000_000));

        // 5. Unresolved remote componentId ("999:1") whose component_set_name collides with local
        //    "Button" ComponentSet must NOT inherit the local ComponentSet's override.
        let mut remote_inst_view = make_test_view(
            "10:5",
            "#RemoteButton",
            Some(dc_bundle::view::ComponentInfo {
                id: "999:1".to_string(),
                name: "S=A".to_string(),
                component_set_name: "Button".to_string(),
                ..Default::default()
            }),
            None,
        );
        propagator.propagate(&mut remote_inst_view);
        assert!(
            view_animation_override(&remote_inst_view).is_none(),
            "Unresolved remote instance must not inherit local ComponentSet override by name collision"
        );
    }

    #[test]
    fn propagator_tier1_instance_matrix_over_tier2_or_tier3_disable_or_custom_does_not_leak_rest() {
        let partial_inst_matrix = || {
            Some(animationspec::AnimationOverride {
                animation_override: Some(animation_override::Animation_override::Matrix(
                    animationspec::AnimationMatrix {
                        transitions: vec![animationspec::TransitionSpec {
                            source_variant: "S=A".to_string(),
                            target_variant: "S=B".to_string(),
                            animation_name: "Default".to_string(),
                            spec: Some(make_smooth_spec_proto(0, 300_000_000)).into(),
                            ..Default::default()
                        }],
                        ..Default::default()
                    },
                )),
                ..Default::default()
            })
        };

        let base_set = |cs_id: &str, v1_id: &str, v2_id: &str, v3_id: &str| {
            test_component_set(
                cs_id,
                "SwitchSet",
                vec![
                    with_interactions(
                        test_component(v1_id, "S=A", vec![]),
                        vec![smart_animate_change_to(TriggerJson::OnClick, v2_id, 0.2)],
                    ),
                    with_interactions(
                        test_component(v2_id, "S=B", vec![]),
                        vec![smart_animate_change_to(TriggerJson::OnClick, v3_id, 0.25)],
                    ),
                    test_component(v3_id, "S=C", vec![]),
                ],
            )
        };

        // Case 1: Tier-3 ComponentSet DisableAnimations; Case 2: Tier-3 ComponentSet Custom.
        let cs_tier3_disable =
            with_squoosh(base_set("20:0", "20:1", "20:2", "20:3"), SQUOOSH_DISABLE);
        let cs_tier3_custom =
            with_squoosh(base_set("30:0", "30:1", "30:2", "30:3"), &squoosh_custom_ms(400));

        // Case 3: Tier-2 Variant Component DisableAnimations; Case 4: Tier-2 Variant Component Custom.
        let mut cs_tier2_disable = base_set("40:0", "40:1", "40:2", "40:3");
        cs_tier2_disable.children[0] =
            with_squoosh(cs_tier2_disable.children[0].clone(), SQUOOSH_DISABLE);
        let mut cs_tier2_custom = base_set("50:0", "50:1", "50:2", "50:3");
        cs_tier2_custom.children[0] =
            with_squoosh(cs_tier2_custom.children[0].clone(), &squoosh_custom_ms(450));

        let sets = [cs_tier3_disable, cs_tier3_custom, cs_tier2_disable, cs_tier2_custom];
        let mut id_index = HashMap::new();
        let mut component_id_index = HashMap::new();
        for cs in &sets {
            id_index.insert(cs.id.clone(), cs);
            for child in &cs.children {
                id_index.insert(child.id.clone(), child);
                component_id_index.insert(child.id.clone(), cs);
            }
        }

        let mut propagator = AnimationOverridePropagator::new(
            &id_index,
            &component_id_index,
            HiddenNodePolicy::Skip,
        );

        for (label, comp_id) in [
            ("tier3_disable", "20:1"),
            ("tier3_custom", "30:1"),
            ("tier2_disable", "40:1"),
            ("tier2_custom", "50:1"),
        ] {
            let mut inst_view = make_test_view(
                &format!("inst_{}", comp_id),
                "#PartialMatrixInst",
                Some(dc_bundle::view::ComponentInfo {
                    id: comp_id.to_string(),
                    name: "S=A".to_string(),
                    component_set_name: "SwitchSet".to_string(),
                    ..Default::default()
                }),
                partial_inst_matrix(),
            );
            propagator.propagate(&mut inst_view);

            let Some(animation_override::Animation_override::Matrix(m)) =
                view_animation_override(&inst_view)
            else {
                panic!("[{}] Expected Tier-1 Matrix override", label);
            };
            assert!(
                m.default_spec.is_none(),
                "[{}] Tier-1 partial Matrix over DisableAnimations/Custom must not inherit default_spec",
                label
            );
            assert_eq!(
                m.transitions.len(),
                1,
                "[{}] Tier-4 REST transitions (S=B->S=C) must NOT leak through Tier-2/3 DisableAnimations or Custom",
                label
            );
            assert_eq!(matrix_pair_nanos(m, "S=A", "S=B"), (0, 300_000_000));
        }
    }

    #[test]
    fn variant_transition_collector_synthesizes_hover_return_after_same_pair_on_click() {
        let cs_node = test_component_set(
            "60:0",
            "HoverAfterClick",
            vec![
                with_interactions(
                    test_component(
                        "60:1",
                        "State=Idle",
                        vec![with_interactions(
                            test_frame("60:11", "HitTarget", vec![]),
                            vec![smart_animate_change_to(TriggerJson::OnPress, "60:3", 0.4)],
                        )],
                    ),
                    vec![
                        smart_animate_change_to(TriggerJson::OnClick, "60:2", 0.2),
                        smart_animate_change_to(TriggerJson::OnHover, "60:2", 0.35),
                        smart_animate_change_to(TriggerJson::OnClick, "60:3", 0.15),
                    ],
                ),
                test_component("60:2", "State=Active", vec![]),
                test_component("60:3", "State=Pressed", vec![]),
            ],
        );

        let matrix =
            synthesize_component_set_animation_matrix(&cs_node, HiddenNodePolicy::Skip).unwrap();
        assert_eq!(matrix.transitions.len(), 4);
        assert_eq!(matrix_pair_nanos(&matrix, "State=Idle", "State=Active"), (0, 200_000_000));
        assert_eq!(
            matrix_pair_nanos(&matrix, "State=Active", "State=Idle"),
            (0, 350_000_000),
            "Same-variant ON_CLICK followed by ON_HOVER on the same target must still synthesize hover return transition"
        );
        assert_eq!(matrix_pair_nanos(&matrix, "State=Idle", "State=Pressed"), (0, 150_000_000));
        assert_eq!(
            matrix_pair_nanos(&matrix, "State=Pressed", "State=Idle"),
            (0, 400_000_000),
            "Root ON_CLICK followed by child ON_PRESS on the same target must still synthesize press return transition"
        );
    }

    #[test]
    fn propagator_canonicalizes_default_no_override_and_retains_template_chain_resets() {
        use dc_bundle::view::view_data::View_data_type;

        let no_override_proto = || {
            Some(animationspec::AnimationOverride {
                animation_override: Some(animation_override::Animation_override::NoOverride(
                    ::protobuf::well_known_types::empty::Empty::new(),
                )),
                ..Default::default()
            })
        };

        // StaticPillSet (30:0) has NO REST transitions and NO Squoosh overrides.
        let static_pill_set = with_squoosh(
            test_component_set(
                "30:0",
                "StaticPillSet",
                vec![
                    with_squoosh(
                        test_component("30:1", "Pill=DefaultVar", vec![]),
                        SQUOOSH_DEFAULT,
                    ),
                    test_component("30:2", "Pill=Plain", vec![]),
                ],
            ),
            SQUOOSH_DEFAULT,
        );

        // CardSet (20:0) has template child 20:11 with active DisableAnimations and sibling 20:12
        // without an active override (annotated with "Default").
        let card_set = test_component_set(
            "20:0",
            "CardSet",
            vec![test_component(
                "20:1",
                "Card=One",
                vec![
                    with_squoosh(
                        test_instance("20:11", "#InnerPillReset", "30:2", vec![]),
                        SQUOOSH_DISABLE,
                    ),
                    with_squoosh(
                        test_instance("20:12", "#InnerPillPlain", "30:2", vec![]),
                        SQUOOSH_DEFAULT,
                    ),
                ],
            )],
        );

        // DeepCardSet (25:0) wraps Card=One (20:1) in #MidCard (25:11), resetting I25:11;20:11 to
        // "Default" and leaving I25:11;20:12 at "Default".
        let deep_card_set = test_component_set(
            "25:0",
            "DeepCardSet",
            vec![test_component(
                "25:1",
                "Deep=One",
                vec![test_instance(
                    "25:11",
                    "#MidCard",
                    "20:1",
                    vec![
                        with_squoosh(
                            test_instance("I25:11;20:11", "#InnerPillReset", "30:2", vec![]),
                            SQUOOSH_DEFAULT,
                        ),
                        with_squoosh(
                            test_instance("I25:11;20:12", "#InnerPillPlain", "30:2", vec![]),
                            SQUOOSH_DEFAULT,
                        ),
                    ],
                )],
            )],
        );

        let all_sets = [&static_pill_set, &card_set, &deep_card_set];
        let mut id_index = HashMap::new();
        let mut component_id_index = HashMap::new();
        fn index_tree<'a>(
            node: &'a figma_schema::Node,
            id_index: &mut HashMap<String, &'a figma_schema::Node>,
        ) {
            id_index.insert(node.id.clone(), node);
            for child in &node.children {
                index_tree(child, id_index);
            }
        }
        for cs in all_sets {
            index_tree(cs, &mut id_index);
            for child in &cs.children {
                component_id_index.insert(child.id.clone(), cs);
            }
        }

        let mut propagator = AnimationOverridePropagator::new(
            &id_index,
            &component_id_index,
            HiddenNodePolicy::Skip,
        );

        let pill_info = |id: &str, name: &str| {
            Some(dc_bundle::view::ComponentInfo {
                id: id.to_string(),
                name: name.to_string(),
                component_set_name: "StaticPillSet".to_string(),
                ..Default::default()
            })
        };

        // 1. ComponentSet, standalone variants (both without and with ComponentInfo), and top-level
        //    instances with {"override":"Default"} and no REST transitions canonicalize to None.
        let mut cs_view = make_test_view("30:0", "StaticPillSet", None, no_override_proto());
        propagator.propagate(&mut cs_view);
        assert!(view_animation_override(&cs_view).is_none());

        let mut standalone_var_no_ci =
            make_test_view("30:1", "Pill=DefaultVar", None, no_override_proto());
        propagator.propagate(&mut standalone_var_no_ci);
        assert!(view_animation_override(&standalone_var_no_ci).is_none());

        let mut standalone_var_with_ci = make_test_view(
            "30:1",
            "Pill=DefaultVar",
            pill_info("30:1", "Pill=DefaultVar"),
            no_override_proto(),
        );
        propagator.propagate(&mut standalone_var_with_ci);
        assert!(view_animation_override(&standalone_var_with_ci).is_none());

        let mut top_inst_default = make_test_view(
            "10:1",
            "#TopPillDefault",
            pill_info("30:2", "Pill=Plain"),
            no_override_proto(),
        );
        propagator.propagate(&mut top_inst_default);
        assert!(
            view_animation_override(&top_inst_default).is_none(),
            "Top-level instance with 'Default' and no REST transitions must canonicalize to None"
        );

        // 2. ';'-delimited instance child resetting active template DisableAnimations without a
        //    synthesized REST matrix retains Some(NoOverride), while sibling resetting non-explicit
        //    template canonicalizes to None.
        let mut outer_inst = make_test_view(
            "10:2",
            "#OuterCard",
            Some(dc_bundle::view::ComponentInfo {
                id: "20:1".to_string(),
                name: "Card=One".to_string(),
                component_set_name: "CardSet".to_string(),
                ..Default::default()
            }),
            None,
        );
        outer_inst.add_child(make_test_view(
            "I10:2;20:11",
            "#InnerPillReset",
            pill_info("30:2", "Pill=Plain"),
            no_override_proto(),
        ));
        outer_inst.add_child(make_test_view(
            "I10:2;20:12",
            "#InnerPillPlain",
            pill_info("30:2", "Pill=Plain"),
            no_override_proto(),
        ));
        propagator.propagate(&mut outer_inst);
        let Some(View_data_type::Container(outer_children)) =
            outer_inst.data.as_ref().and_then(|d| d.view_data_type.as_ref())
        else {
            panic!("Expected Container");
        };
        assert!(
            matches!(
                view_animation_override(&outer_children.children[0]),
                Some(animation_override::Animation_override::NoOverride(_))
            ),
            "';'-delimited child resetting active template DisableAnimations must retain Some(NoOverride)"
        );
        assert!(
            view_animation_override(&outer_children.children[1]).is_none(),
            "';'-delimited child with 'Default' over non-explicit template must canonicalize to None"
        );

        // 3. Intermediate template I25:11;20:11 and unmodified deep nested descendant I10:3;25:11;20:11
        //    both resolve to Some(NoOverride), while I25:11;20:12 and I10:3;25:11;20:12 resolve to None.
        let mut mid_template_reset = make_test_view(
            "I25:11;20:11",
            "#InnerPillReset",
            pill_info("30:2", "Pill=Plain"),
            no_override_proto(),
        );
        let mut deep_unmodified_reset = make_test_view(
            "I10:3;25:11;20:11",
            "#InnerPillReset",
            pill_info("30:2", "Pill=Plain"),
            None,
        );
        propagator.propagate(&mut mid_template_reset);
        propagator.propagate(&mut deep_unmodified_reset);
        assert!(matches!(
            view_animation_override(&mid_template_reset),
            Some(animation_override::Animation_override::NoOverride(_))
        ));
        assert_eq!(
            deep_unmodified_reset.style().node_style().animation_override,
            mid_template_reset.style().node_style().animation_override,
            "Unmodified deep nested reset descendant must match its intermediate template's Some(NoOverride)"
        );

        let mut mid_template_plain = make_test_view(
            "I25:11;20:12",
            "#InnerPillPlain",
            pill_info("30:2", "Pill=Plain"),
            no_override_proto(),
        );
        let mut deep_unmodified_plain = make_test_view(
            "I10:3;25:11;20:12",
            "#InnerPillPlain",
            pill_info("30:2", "Pill=Plain"),
            None,
        );
        propagator.propagate(&mut mid_template_plain);
        propagator.propagate(&mut deep_unmodified_plain);
        assert!(view_animation_override(&mid_template_plain).is_none());
        assert!(view_animation_override(&deep_unmodified_plain).is_none());
    }
}
