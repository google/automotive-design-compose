//! This file contains the conversions from the JSON animation specification to the protobuf
//! animation specification. The JSON structs are defined in `animation_spec_schema.rs`.
//! The protobuf structs are generated from `animations.proto`.

use crate::animation_spec_schema::{
    AnimationOverrideJson, AnimationSpec as AnimationSpecJson, Animations as AnimationsJson,
    BezierCurve as BezierCurveJson, Duration, Easing as EasingJson, KeyFrame as KeyFrameJson,
    KeyFrameAnimation as KeyFrameAnimationJson, RepeatType as RepeatTypeJson,
    SmoothAnimation as SmoothAnimationJson, StopType as StopTypeJson,
};
use dc_bundle::animationspec;
use dc_bundle::animationspec::{
    animation_override, animations, easing, repeat_type, stop_type, AnimationSpec, Animations,
    BezierCurve, Easing, KeyFrame, KeyFrameAnimation, RepeatType, SmoothAnimation, StopType,
};

/// Converts from the JSON `AnimationOverrideJson` to the protobuf `AnimationOverride`.
pub struct AnimationOverride(animationspec::AnimationOverride);

impl Into<animationspec::AnimationOverride> for AnimationOverride {
    fn into(self) -> animationspec::AnimationOverride {
        self.0
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
        })
    }
}

/// Converts from the JSON `AnimationSpecJson` to the protobuf `AnimationSpec`.
impl From<AnimationSpecJson> for AnimationSpec {
    fn from(json: AnimationSpecJson) -> Self {
        AnimationSpec {
            initial_delay: Some(json.initial_delay.into()).into(),
            animation: Some(json.animation.into()).into(),
            interrupt_type: json.interrupt_type.map(|x| x.into()).into(),
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
            EasingJson::String(_) => {
                easing.type_ =
                    Some(easing::Type::Linear(::protobuf::well_known_types::empty::Empty::new()));
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
            nanos: json.nanos,
            ..Default::default()
        }
    }
}

#[cfg(test)]
mod tests {
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
            initial_delay: Duration { secs: 1, nanos: 500_000_000 },
            animation: AnimationsJson::default(),
            interrupt_type: Some(StopTypeJson::Complete),
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
            initial_delay: Duration { secs: 1, nanos: 500_000_000 },
            animation: AnimationsJson::default(),
            interrupt_type: None,
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
            duration: Duration { secs: 2, nanos: 0 },
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
            KeyFrameJson { value: 0.5, duration: Duration { secs: 0, nanos: 100_000_000 } };
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
        let json_duration = Duration { secs: 10, nanos: 123 };
        let proto_duration: ::protobuf::well_known_types::duration::Duration = json_duration.into();
        assert_eq!(proto_duration.seconds, 10);
        assert_eq!(proto_duration.nanos, 123);
    }
}
