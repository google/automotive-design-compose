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
    pub nanos: f64,
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
#[serde(default)]
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
#[serde(default)]
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
    #[serde(alias = "Cancel")]
    None,
    /// Resets the animation to its starting state.
    ResetToStart,
    /// Immediately jumps to the final state of the animation.
    #[serde(alias = "JumpToEnd")]
    Complete,
    /// Stops the animation at its current state.
    Stop,
}

/// A keyframe within a custom property timeline.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
pub struct CustomKeyframe {
    pub fraction: f32,
    #[serde(alias = "value")]
    pub value_json: serde_json::Value,
    pub easing: Easing,
}

/// A sequence of keyframes for an arbitrary property.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
pub struct CustomTimeline {
    #[serde(alias = "targetEasing")]
    pub target_easing: Easing,
    pub keyframes: Vec<CustomKeyframe>,
}

/// Represents a raw custom timeline from JSON which can be either a typed struct or stringified JSON.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
#[serde(untagged)]
pub enum CustomTimelineRaw {
    Typed(CustomTimeline),
    Stringified(String),
}

impl CustomTimelineRaw {
    /// Decodes the raw timeline into a typed `CustomTimeline`.
    pub fn into_timeline(self) -> Option<CustomTimeline> {
        match self {
            CustomTimelineRaw::Typed(ct) => Some(ct),
            CustomTimelineRaw::Stringified(s) => serde_json::from_str::<CustomTimeline>(&s).ok(),
        }
    }
}

/// Helper function to deserialize a map of timelines from either typed structs or stringified JSON.
pub fn deserialize_custom_timelines_map<'de, D>(
    deserializer: D,
) -> Result<std::collections::HashMap<String, CustomTimeline>, D::Error>
where
    D: Deserializer<'de>,
{
    let raw_map =
        std::collections::HashMap::<String, CustomTimelineRaw>::deserialize(deserializer)?;
    let mut result = std::collections::HashMap::new();
    for (key, val) in raw_map {
        if let Some(timeline) = val.into_timeline() {
            result.insert(key, timeline);
        } else {
            log::warn!("Failed to decode custom timeline JSON string for key: {}", key);
        }
    }
    Ok(result)
}

/// The detailed specification for a custom animation, present when "Custom" is
/// selected in the top-level "Animation" dropdown.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Default)]
#[serde(default)]
pub struct AnimationSpec {
    /// The delay before the animation starts.
    pub initial_delay: Duration,
    /// The core animation definition, which is either a smooth or keyframe animation.
    pub animation: Animations,
    /// The behavior of the animation upon interruption.
    pub interrupt_type: Option<StopType>,
    /// Optional dictionary containing Squoosh arbitrary layer property values to animate from
    /// strings directly written by the UI plugin format over into matching types in Compose
    #[serde(
        rename = "customKeyframeData",
        default,
        deserialize_with = "deserialize_custom_timelines_map"
    )]
    pub custom_keyframe_data: std::collections::HashMap<String, CustomTimeline>,
    /// Timelines for custom properties
    #[serde(default, deserialize_with = "deserialize_custom_timelines_map")]
    pub timelines: std::collections::HashMap<String, CustomTimeline>,
}

/// Represents a single transition specification between variant states (Option A).
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Default)]
pub struct TransitionSpecJson {
    /// Origin variant state or wildcard ("*")
    pub from: String,
    /// Destination variant state
    pub to: String,
    /// Custom animation identifier (defaults to "Default")
    #[serde(default = "default_animation_name")]
    pub name: String,
    /// Optional animation specification (delay, duration, easing, etc.)
    pub spec: Option<AnimationSpec>,
    /// Timelines for custom properties
    #[serde(default, deserialize_with = "deserialize_custom_timelines_map")]
    pub timelines: std::collections::HashMap<String, CustomTimeline>,
    /// Legacy/plugin field for custom property keyframe timelines exported as stringified JSON
    #[serde(
        rename = "customKeyframeData",
        default,
        deserialize_with = "deserialize_custom_timelines_map"
    )]
    pub custom_keyframe_data: std::collections::HashMap<String, CustomTimeline>,
}

fn default_animation_name() -> String {
    "Default".to_string()
}

/// Root animation matrix structure for Option A transition matrix storage.
#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Default)]
pub struct AnimationMatrixJson {
    /// Default animation spec fallback across all transitions
    pub default_spec: Option<AnimationSpec>,
    /// Array of explicit transition specifications
    #[serde(default)]
    pub transitions: Vec<TransitionSpecJson>,
    /// Node timelines stored at the matrix root level
    #[serde(default, deserialize_with = "deserialize_custom_timelines_map")]
    pub timelines: std::collections::HashMap<String, CustomTimeline>,
    /// Legacy/alias name for node timelines stored at the matrix root level
    #[serde(
        rename = "customKeyframeData",
        default,
        deserialize_with = "deserialize_custom_timelines_map"
    )]
    pub custom_keyframe_data: std::collections::HashMap<String, CustomTimeline>,
}

impl TransitionSpecJson {
    /// Validates the transition spec fields.
    pub fn validate(&self) -> Result<(), String> {
        let from_is_wild = self.from.is_empty() || self.from == "*";
        let to_is_wild = self.to.is_empty() || self.to == "*";
        if from_is_wild && to_is_wild {
            return Err(
                "TransitionSpec 'from' and 'to' cannot both be wildcards (* -> *)".to_string()
            );
        }
        for (prop_name, timeline) in self.timelines.iter().chain(self.custom_keyframe_data.iter()) {
            for keyframe in &timeline.keyframes {
                if !(0.0..=1.0).contains(&keyframe.fraction) {
                    return Err(format!(
                        "Keyframe fraction {} for property '{}' is out of range [0.0, 1.0]",
                        keyframe.fraction, prop_name
                    ));
                }
            }
        }
        Ok(())
    }
}

impl AnimationMatrixJson {
    /// Validates the animation matrix structure and all transition specs.
    pub fn validate(&self) -> Result<(), String> {
        for (idx, transition) in self.transitions.iter().enumerate() {
            transition.validate().map_err(|e| format!("Transition [{}] invalid: {}", idx, e))?;
        }
        Ok(())
    }
}

/// This is the top-level structure that the plugin saves to a Figma node.
#[derive(Serialize, Clone, Debug, PartialEq)]
pub enum AnimationOverrideJson {
    /// Use the default animation behavior.
    Default,
    /// Use a custom animation specification.
    Custom(AnimationSpec),
    /// Use a transition matrix specification (Option A).
    Matrix(AnimationMatrixJson),
    /// Disable all animations.
    DisableAnimations,
}

impl Default for AnimationOverrideJson {
    fn default() -> Self {
        AnimationOverrideJson::Default
    }
}

impl AnimationOverrideJson {
    /// Validates the animation override.
    pub fn validate(&self) -> Result<(), String> {
        match self {
            AnimationOverrideJson::Matrix(matrix) => matrix.validate(),
            _ => Ok(()),
        }
    }
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
            #[serde(rename = "customKeyframeData", default)]
            custom_keyframe_data_raw: std::collections::HashMap<String, CustomTimelineRaw>,
            #[serde(rename = "timelines", default)]
            timelines_raw: std::collections::HashMap<String, CustomTimelineRaw>,
            default_spec: Option<AnimationSpec>,
            transitions: Option<Vec<TransitionSpecJson>>,
        }

        let tmp = Tmp::deserialize(deserializer)?;
        let mut custom_keyframe_data = std::collections::HashMap::new();
        for (k, v) in tmp.custom_keyframe_data_raw.into_iter().chain(tmp.timelines_raw.into_iter())
        {
            if let Some(ct) = v.into_timeline() {
                custom_keyframe_data.insert(k, ct);
            }
        }

        if tmp.transitions.is_some() || tmp.default_spec.is_some() {
            let matrix = AnimationMatrixJson {
                default_spec: tmp.default_spec,
                transitions: tmp.transitions.unwrap_or_default(),
                custom_keyframe_data: custom_keyframe_data.clone(),
                timelines: custom_keyframe_data,
            };
            Ok(AnimationOverrideJson::Matrix(matrix))
        } else if tmp.override_type == "Custom"
            || (tmp.override_type.is_empty() && tmp.spec.is_some())
        {
            if let Some(mut spec) = tmp.spec {
                spec.custom_keyframe_data = custom_keyframe_data;
                Ok(AnimationOverrideJson::Custom(spec))
            } else {
                Err(de::Error::missing_field("spec"))
            }
        } else if tmp.override_type == "None" {
            Ok(AnimationOverrideJson::DisableAnimations)
        } else {
            Ok(AnimationOverrideJson::Default)
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
    fn test_deserialize_custom_with_keyframe_data() {
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
            },
            "customKeyframeData": {
                "Right-x": {
                    "target_easing": "Linear",
                    "keyframes": [
                        { "fraction": 0.275, "value_json": "123", "easing": "Linear" }
                    ]
                }
            }
        }"#;
        let spec: AnimationOverrideJson = serde_json::from_str(json).unwrap();
        if let AnimationOverrideJson::Custom(custom_spec) = spec {
            assert_eq!(custom_spec.interrupt_type, Some(StopType::Complete));
            assert_eq!(custom_spec.custom_keyframe_data.len(), 1);
            assert_eq!(
                custom_spec.custom_keyframe_data.get("Right-x").unwrap().keyframes[0].fraction,
                0.275
            );
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

    #[test]
    fn test_parse_squoosh_json() {
        let json_str = r#"{
            "spec": {
                "initial_delay": { "secs": 0, "nanos": 0 },
                "animation": {
                    "Smooth": {
                        "duration": { "secs": 0, "nanos": 1000000000 },
                        "repeat_type": "NoRepeat",
                        "easing": "Linear"
                    }
                },
                "interrupt_type": "None"
            },
            "customKeyframeData": {
                "Right-x": {
                    "target_easing": "Linear",
                    "keyframes": []
                }
            }
        }"#;
        let anim = serde_json::from_str::<AnimationOverrideJson>(json_str);
        assert!(anim.is_ok(), "Failed to parse JSON: {:?}", anim.err());
        println!("Successfully parsed: {:?}", anim.unwrap());
    }

    #[test]
    fn test_deserialize_animation_matrix() {
        let json_str = r#"{
            "default_spec": {
                "initial_delay": { "secs": 0, "nanos": 0 },
                "animation": {
                    "Smooth": {
                        "duration": { "secs": 0, "nanos": 300000000 },
                        "repeat_type": "NoRepeat",
                        "easing": "Linear"
                    }
                },
                "interrupt_type": "None"
            },
            "transitions": [
                {
                    "from": "VariantA",
                    "to": "VariantB",
                    "name": "Default",
                    "spec": {
                        "initial_delay": { "secs": 0, "nanos": 0 },
                        "animation": {
                            "Smooth": {
                                "duration": { "secs": 0, "nanos": 500000000 },
                                "repeat_type": "NoRepeat",
                                "easing": "EaseInOut"
                            }
                        },
                        "interrupt_type": "None"
                    },
                    "timelines": {
                        "PRNDState-x": {
                            "target_easing": "Linear",
                            "keyframes": [
                                { "fraction": 0.5, "value_json": 100.0, "easing": "EaseIn" }
                            ]
                        }
                    }
                },
                {
                    "from": "*",
                    "to": "VariantB",
                    "name": "AlertPop",
                    "timelines": {}
                }
            ]
        }"#;
        let anim = serde_json::from_str::<AnimationOverrideJson>(json_str);
        assert!(anim.is_ok(), "Failed to parse Option A matrix JSON: {:?}", anim.err());
        let anim_val = anim.unwrap();
        assert!(anim_val.validate().is_ok(), "Validation failed: {:?}", anim_val.validate());
        if let AnimationOverrideJson::Matrix(matrix) = anim_val {
            assert!(matrix.default_spec.is_some());
            assert_eq!(matrix.transitions.len(), 2);
            assert_eq!(matrix.transitions[0].from, "VariantA");
            assert_eq!(matrix.transitions[0].to, "VariantB");
            assert_eq!(matrix.transitions[0].name, "Default");
            assert_eq!(matrix.transitions[1].from, "*");
            assert_eq!(matrix.transitions[1].name, "AlertPop");
        } else {
            panic!("Expected AnimationOverrideJson::Matrix");
        }
    }

    #[test]
    fn test_validate_invalid_transition() {
        let invalid_transition = TransitionSpecJson {
            from: "".to_string(),
            to: "".to_string(),
            name: "Invalid".to_string(),
            ..Default::default()
        };
        assert!(invalid_transition.validate().is_err());

        let invalid_wildcard_transition = TransitionSpecJson {
            from: "*".to_string(),
            to: "*".to_string(),
            name: "InvalidWildcard".to_string(),
            ..Default::default()
        };
        assert!(invalid_wildcard_transition.validate().is_err());

        let mut valid_transition = TransitionSpecJson {
            from: "A".to_string(),
            to: "B".to_string(),
            name: "Valid".to_string(),
            ..Default::default()
        };
        assert!(valid_transition.validate().is_ok());

        let timeline = CustomTimeline {
            target_easing: Easing::String("Linear".to_string()),
            keyframes: vec![CustomKeyframe {
                fraction: 1.5,
                value_json: serde_json::Value::Null,
                easing: Easing::String("Linear".to_string()),
            }],
        };
        valid_transition.timelines.insert("Prop".to_string(), timeline);
        assert!(valid_transition.validate().is_err());
    }

    #[test]
    fn test_validate_animation_matrix() {
        let matrix = AnimationMatrixJson {
            default_spec: None,
            transitions: vec![TransitionSpecJson {
                from: "".to_string(),
                to: "".to_string(),
                name: "Invalid".to_string(),
                ..Default::default()
            }],
            ..Default::default()
        };
        let err = matrix.validate().unwrap_err();
        assert!(err.contains("Transition [0] invalid:"));
    }

    #[test]
    fn test_validate_animation_override() {
        let default_override = AnimationOverrideJson::Default;
        assert!(default_override.validate().is_ok());

        let custom_override = AnimationOverrideJson::Custom(AnimationSpec::default());
        assert!(custom_override.validate().is_ok());

        let disable_override = AnimationOverrideJson::DisableAnimations;
        assert!(disable_override.validate().is_ok());

        let invalid_matrix = AnimationOverrideJson::Matrix(AnimationMatrixJson {
            default_spec: None,
            transitions: vec![TransitionSpecJson {
                from: "".to_string(),
                to: "".to_string(),
                name: "Invalid".to_string(),
                ..Default::default()
            }],
            ..Default::default()
        });
        assert!(invalid_matrix.validate().is_err());
    }

    #[test]
    fn test_validate_transition_invalid_keyframe_fraction() {
        let mut timelines = std::collections::HashMap::new();
        timelines.insert(
            "Prop".to_string(),
            CustomTimeline {
                target_easing: Easing::String("Linear".to_string()),
                keyframes: vec![CustomKeyframe {
                    fraction: 1.5,
                    value_json: serde_json::Value::Null,
                    easing: Easing::String("Linear".to_string()),
                }],
            },
        );
        let invalid_transition = TransitionSpecJson {
            from: "A".to_string(),
            to: "B".to_string(),
            name: "Test".to_string(),
            spec: None,
            timelines,
            ..Default::default()
        };
        assert!(invalid_transition.validate().is_err());

        let mut timelines_neg = std::collections::HashMap::new();
        timelines_neg.insert(
            "Prop".to_string(),
            CustomTimeline {
                target_easing: Easing::String("Linear".to_string()),
                keyframes: vec![CustomKeyframe {
                    fraction: -0.1,
                    value_json: serde_json::Value::Null,
                    easing: Easing::String("Linear".to_string()),
                }],
            },
        );
        let invalid_transition_neg = TransitionSpecJson {
            from: "A".to_string(),
            to: "B".to_string(),
            name: "Test".to_string(),
            spec: None,
            timelines: timelines_neg,
            ..Default::default()
        };
        assert!(invalid_transition_neg.validate().is_err());
    }

    #[test]
    fn test_validate_transition_valid() {
        let mut timelines = std::collections::HashMap::new();
        timelines.insert(
            "Prop".to_string(),
            CustomTimeline {
                target_easing: Easing::String("Linear".to_string()),
                keyframes: vec![CustomKeyframe {
                    fraction: 1.0,
                    value_json: serde_json::Value::Null,
                    easing: Easing::String("Linear".to_string()),
                }],
            },
        );
        let valid_transition = TransitionSpecJson {
            from: "A".to_string(),
            to: "B".to_string(),
            name: "Test".to_string(),
            spec: None,
            timelines,
            ..Default::default()
        };
        assert!(valid_transition.validate().is_ok());
    }

    #[test]
    fn test_validate_matrix_invalid_transition() {
        let matrix = AnimationMatrixJson {
            default_spec: None,
            transitions: vec![TransitionSpecJson {
                from: "".to_string(),
                to: "".to_string(),
                name: "Invalid".to_string(),
                ..Default::default()
            }],
            ..Default::default()
        };
        assert!(matrix.validate().is_err());
    }

    #[test]
    fn test_validate_matrix_valid() {
        let matrix = AnimationMatrixJson {
            default_spec: None,
            transitions: vec![TransitionSpecJson {
                from: "A".to_string(),
                to: "B".to_string(),
                name: "Valid".to_string(),
                ..Default::default()
            }],
            ..Default::default()
        };
        assert!(matrix.validate().is_ok());
    }

    #[test]
    fn test_validate_animation_override_extended() {
        let invalid_matrix = AnimationOverrideJson::Matrix(AnimationMatrixJson {
            default_spec: None,
            transitions: vec![TransitionSpecJson {
                from: "".to_string(),
                to: "".to_string(),
                name: "Invalid".to_string(),
                ..Default::default()
            }],
            ..Default::default()
        });
        assert!(invalid_matrix.validate().is_err());

        let valid_matrix = AnimationOverrideJson::Matrix(AnimationMatrixJson {
            default_spec: None,
            transitions: vec![TransitionSpecJson {
                from: "A".to_string(),
                to: "B".to_string(),
                name: "Valid".to_string(),
                ..Default::default()
            }],
            ..Default::default()
        });
        assert!(valid_matrix.validate().is_ok());

        assert!(AnimationOverrideJson::Default.validate().is_ok());
        assert!(AnimationOverrideJson::DisableAnimations.validate().is_ok());
        assert!(AnimationOverrideJson::Custom(AnimationSpec::default()).validate().is_ok());
    }

    #[test]
    fn test_deserialize_matrix_default_animation_name() {
        let json_str = r#"{
            "transitions": [
                {
                    "from": "A",
                    "to": "B"
                }
            ]
        }"#;
        let anim = serde_json::from_str::<AnimationOverrideJson>(json_str).unwrap();
        if let AnimationOverrideJson::Matrix(matrix) = anim {
            assert_eq!(matrix.transitions[0].name, "Default");
        } else {
            unreachable!("Expected Matrix");
        }
    }

    #[test]
    fn test_deserialize_matrix_missing_transitions_field() {
        let json_str = r#"{
            "default_spec": {
                "initial_delay": { "secs": 0, "nanos": 0 },
                "animation": {
                    "Smooth": {
                        "duration": { "secs": 0, "nanos": 300000000 },
                        "repeat_type": "NoRepeat",
                        "easing": "Linear"
                    }
                },
                "interrupt_type": "None"
            }
        }"#;
        let anim = serde_json::from_str::<AnimationOverrideJson>(json_str).unwrap();
        if let AnimationOverrideJson::Matrix(matrix) = anim {
            assert_eq!(matrix.transitions.len(), 0);
            assert!(matrix.default_spec.is_some());
        } else {
            unreachable!("Expected Matrix");
        }
    }

    #[test]
    fn test_animation_override_json_default() {
        let def = AnimationOverrideJson::default();
        assert!(matches!(def, AnimationOverrideJson::Default));
    }

    #[test]
    fn test_deserialize_matrix_custom_keyframe_data_stringified() {
        let json_str = r##"{
            "transitions": [
                {
                    "from": "#driving/shift-state=N",
                    "to": "#driving/shift-state=D",
                    "name": "SportMode",
                    "spec": {
                        "initial_delay": { "secs": 0, "nanos": 0 },
                        "animation": {
                            "Smooth": {
                                "duration": { "secs": 1, "nanos": 500000000 },
                                "repeat_type": "NoRepeat",
                                "easing": "EaseOut"
                            }
                        },
                        "interrupt_type": "None"
                    },
                    "customKeyframeData": {
                        "D-opacity": "{\"targetEasing\": \"EaseOut\", \"keyframes\": [{\"fraction\": 0.2, \"value\": 0.0, \"easing\": \"EaseIn\"}, {\"fraction\": 1.0, \"value\": 1.0, \"easing\": \"EaseOut\"}]}"
                    }
                }
            ]
        }"##;
        let anim = serde_json::from_str::<AnimationOverrideJson>(json_str).unwrap();
        if let AnimationOverrideJson::Matrix(matrix) = anim {
            assert_eq!(matrix.transitions.len(), 1);
            let t = &matrix.transitions[0];
            assert_eq!(t.name, "SportMode");
            assert!(
                t.custom_keyframe_data.contains_key("D-opacity"),
                "Expected D-opacity in custom_keyframe_data map"
            );
            let timeline = &t.custom_keyframe_data["D-opacity"];
            assert_eq!(timeline.keyframes.len(), 2);
            assert_eq!(timeline.keyframes[0].fraction, 0.2);
        } else {
            unreachable!("Expected Matrix");
        }
    }
}
