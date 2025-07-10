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
use serde::de::{self, Deserializer};
use serde::{Deserialize, Serialize};

/// Represents a duration of time. In the Figma plugin, this is typically
/// specified in milliseconds and converted to seconds and nanoseconds.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Default)]
pub struct Duration {
    /// The whole number of seconds.
    pub secs: i64,
    /// The number of nanoseconds.
    pub nanos: i32,
}

/// Defines a cubic bezier curve for custom easing.
/// `p0` and `p3` are implicitly (0.0, 0.0) and (1.0, 1.0) respectively.
/// The UI provides fields for P1 and P2.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Default)]
pub struct BezierCurve {
    /// The x coordinate of the first control point.
    pub p0: f32,
    /// The y coordinate of the first control point.
    pub p1: f32,
    /// The x coordinate of the second control point.
    pub p2: f32,
    /// The y coordinate of the second control point.
    pub p3: f32,
}

/// Defines the easing of an animation. This can be a predefined curve
/// or a custom cubic bezier curve. This corresponds to the "Easing" dropdown
/// in the plugin UI.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
#[serde(untagged)]
pub enum Easing {
    /// A predefined easing curve, like "Linear", "EaseIn", etc.
    String(String),
    /// A custom cubic bezier curve, selected when "Custom Bezier" is chosen.
    Bezier {
        /// The bezier curve definition.
        #[serde(rename = "Bezier")]
        bezier: BezierCurve,
    },
}

impl Default for Easing {
    fn default() -> Self {
        Easing::String("Linear".to_string())
    }
}

/// A single step in a keyframe animation, defining a target value and the
/// time it should take to reach it.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Default)]
pub struct KeyFrame {
    /// The target value for this keyframe.
    pub value: f32,
    /// The time to take to transition to this keyframe's value.
    pub duration: Duration,
}

/// Defines the repetition behavior of an animation, corresponding to the
/// "Repeat Type" dropdown in the plugin.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
#[serde(untagged)]
pub enum RepeatType {
    /// A string indicating no repetition ("NoRepeat") or infinite looping ("LoopForever").
    String(String),
    /// Repeats the animation a specific number of times.
    Repeat {
        /// The number of times to repeat the animation.
        #[serde(rename = "Repeat")]
        repeat: u32,
    },
}

impl Default for RepeatType {
    fn default() -> Self {
        RepeatType::String("NoRepeat".to_string())
    }
}

/// Defines a smooth animation between two values.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Default)]
pub struct SmoothAnimation {
    /// The total duration of one iteration of the animation.
    pub duration: Duration,
    /// The repetition behavior of the animation.
    pub repeat_type: RepeatType,
    /// The easing curve to use for the animation.
    pub easing: Easing,
}

/// Defines an animation composed of multiple keyframes.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Default)]
pub struct KeyFrameAnimation {
    /// The sequence of keyframes that make up the animation.
    pub steps: Vec<KeyFrame>,
    /// The repetition behavior of the animation.
    pub repeat_type: RepeatType,
}

/// Represents one of two types of animations, corresponding to the "Animation Type"
/// dropdown in the plugin. Only one of the fields, `smooth` or `key_frame`, will be set.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Default)]
pub struct Animations {
    /// A smooth animation with a single duration and easing curve.
    #[serde(rename = "Smooth", skip_serializing_if = "Option::is_none")]
    pub smooth: Option<SmoothAnimation>,
    /// An animation defined by a series of keyframes.
    #[serde(rename = "KeyFrame", skip_serializing_if = "Option::is_none")]
    pub key_frame: Option<KeyFrameAnimation>,
}

/// Defines how an animation should behave when interrupted, corresponding to the
/// "Interrupt Type" dropdown in the plugin.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
pub enum StopType {
    /// No specific interruption behavior.
    None,
    /// Resets the animation to its starting state.
    ResetToStart,
    /// Immediately jumps to the final state of the animation.
    Complete,
    /// Stops the animation at its current state.
    Stop,
}

/// The detailed specification for a custom animation, present when "Custom" is
/// selected in the top-level "Animation" dropdown.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Default)]
pub struct AnimationSpec {
    /// The delay before the animation starts.
    pub initial_delay: Duration,
    /// The core animation definition, which is either a smooth or keyframe animation.
    pub animation: Animations,
    /// The behavior of the animation upon interruption.
    pub interrupt_type: Option<StopType>,
}

/// This is the top-level structure that the plugin saves to a Figma node.
#[derive(Serialize, Clone, Debug, PartialEq, Default)]
pub enum AnimationOverrideJson {
    /// Use the default animation behavior.
    #[default]
    Default,
    /// Use a custom animation specification.
    Custom(AnimationSpec),
    /// Disable all animations.
    DisableAnimations,
}

impl<'de> Deserialize<'de> for AnimationOverrideJson {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        // Tmp structure to deserialize the format from the API and handle cases hard to describe
        // with serde attributes.
        #[derive(Deserialize)]
        struct Tmp {
            #[serde(rename = "override", default)]
            override_type: String,
            spec: Option<AnimationSpec>,
        }

        let tmp = Tmp::deserialize(deserializer)?;
        match tmp.override_type.as_str() {
            "Custom" => {
                if let Some(spec) = tmp.spec {
                    Ok(AnimationOverrideJson::Custom(spec))
                } else {
                    Err(de::Error::missing_field("spec"))
                }
            }
            "None" => Ok(AnimationOverrideJson::DisableAnimations),
            _ => Ok(AnimationOverrideJson::Default),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_deserialize_custom() {
        let json = r#"{
            "override": "Custom",
            "spec": {
                "initial_delay": { "secs": 0, "nanos": 0 },
                "animation": {
                    "Smooth": {
                        "duration": { "secs": 0, "nanos": 1000000000 },
                        "repeat_type": "NoRepeat",
                        "easing": "Linear"
                    }
                },
                "interrupt_type": "Complete"
            }
        }"#;
        let spec: AnimationOverrideJson = serde_json::from_str(json).unwrap();
        if let AnimationOverrideJson::Custom(custom_spec) = spec {
            assert_eq!(custom_spec.interrupt_type, Some(StopType::Complete));
        } else {
            panic!("Wrong spec type, expected Custom");
        }
    }

    #[test]
    fn test_deserialize_keyframe_custom() {
        let json = r#"{"override":"Custom","spec":{"initial_delay":{"secs":0,"nanos":0},"animation":{"KeyFrame":{"steps":[{"value":0,"duration":{"secs":0,"nanos":100000000}},{"value":0.5,"duration":{"secs":0,"nanos":110000000}},{"value":1,"duration":{"secs":0,"nanos":120000000}}],"repeat_type":"NoRepeat"}},"interrupt_type":null}}"#;
        let spec: AnimationOverrideJson = serde_json::from_str(json).unwrap();
        if let AnimationOverrideJson::Custom(custom_spec) = spec {
            assert_eq!(custom_spec.interrupt_type, None);
            if let Some(key_frame_animation) = custom_spec.animation.key_frame {
                assert_eq!(key_frame_animation.steps.len(), 3);
                assert_eq!(key_frame_animation.steps[1].value, 0.5);
            } else {
                panic!("Wrong animation type, expected KeyFrame");
            }
        } else {
            panic!("Wrong spec type, expected Custom");
        }
    }

    #[test]
    fn test_deserialize_none() {
        let json = r#"{"override":"None","disable":true}"#;
        let spec: AnimationOverrideJson = serde_json::from_str(json).unwrap();
        assert!(matches!(spec, AnimationOverrideJson::DisableAnimations));
    }

    #[test]
    fn test_deserialize_default_no_disable() {
        let json = r#"{"override":"Default"}"#;
        let spec: AnimationOverrideJson = serde_json::from_str(json).unwrap();
        assert!(matches!(spec, AnimationOverrideJson::Default));
    }

    #[test]
    fn test_deserialize_default_empty() {
        let json = r#"{}"#;
        let spec: AnimationOverrideJson = serde_json::from_str(json).unwrap();
        assert!(matches!(spec, AnimationOverrideJson::Default));
    }
}
