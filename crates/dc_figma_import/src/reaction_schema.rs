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

use crate::figma_schema::FigmaColor;
use dc_bundle::{
    color::FloatColor,
    frame_extras::{
        FrameExtras, OverlayBackground, OverlayBackgroundInteraction, OverlayPositionType,
    },
    geometry::Vector,
    reaction::{
        action::{self, node::Navigation, ActionUrl, Action_type},
        trigger::{KeyDown, MouseDown, MouseEnter, MouseLeave, MouseUp, Timeout, Trigger_type},
        Action, Reaction, Trigger,
    },
    transition::{
        easing::{Bezier, Easing_type, Spring},
        transition::{
            Dissolve, MoveIn, MoveOut, Push, ScrollAnimate, SlideIn, SlideOut, SmartAnimate,
            TransitionDirection, Transition_type,
        },
        Easing, Transition,
    },
};

use serde::{de::DeserializeOwned, Deserialize, Deserializer, Serialize};

// This module deserializes Figma's prototype interaction payloads:
// - Figma REST API `Node.interactions` (`InteractionJson`, carrying `trigger` + `actions` array),
// - Legacy `vsw-reactions` plugin storage (`ReactionJson`), and
// - `vsw-frame-extras` plugin storage (`FrameExtrasJson`).
//
// Figma REST `Node.interactions` is preferred when present on a node, with `vsw-reactions` kept
// as a fallback when `Node.interactions` is empty.
//
// The Figma documentation that these definitions correspond to is here:
//  https://www.figma.com/plugin-docs/api/Reaction/

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct BezierJson {
    pub x1: f32,
    pub y1: f32,
    pub x2: f32,
    pub y2: f32,
}

impl Into<Bezier> for BezierJson {
    fn into(self) -> Bezier {
        Bezier { x1: self.x1, y1: self.y1, x2: self.x2, y2: self.y2, ..Default::default() }
    }
}
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct SpringJson {
    pub mass: f32,
    pub stiffness: f32,
    pub damping: f32,
}

impl Into<Spring> for SpringJson {
    fn into(self) -> Spring {
        Spring {
            mass: self.mass,
            stiffness: self.stiffness,
            damping: self.damping,
            ..Default::default()
        }
    }
}

/// The type of easing to perform in a transition.
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq, Default)]
#[serde(tag = "type", rename_all = "SCREAMING_SNAKE_CASE")]
pub enum EasingJson {
    // Cubic beziers
    EaseIn,
    EaseOut,
    EaseInAndOut,
    #[default]
    Linear,
    EaseInBack,
    EaseOutBack,
    EaseInAndOutBack,

    // Manually specified cubic bezier
    CustomCubicBezier {
        #[serde(rename = "easingFunctionCubicBezier")]
        bezier: BezierJson,
    },

    // Springs
    Gentle,
    Quick,
    Bouncy,
    Slow,

    // Manually specified spring
    CustomSpring {
        #[serde(rename = "easingFunctionSpring")]
        spring: SpringJson,
    },
}

// We flatten the Easing type to a bezier for the toolkit. These values were taken from
// https://easings.net/ and verified against Figma optically.
impl Into<Easing_type> for EasingJson {
    fn into(self) -> Easing_type {
        match self {
            EasingJson::EaseIn => Easing_type::Bezier(Bezier {
                x1: 0.12,
                y1: 0.0,
                x2: 0.39,
                y2: 0.0,
                ..Default::default()
            }),
            EasingJson::EaseOut => Easing_type::Bezier(Bezier {
                x1: 0.61,
                y1: 1.0,
                x2: 0.88,
                y2: 1.0,
                ..Default::default()
            }),
            EasingJson::EaseInAndOut => Easing_type::Bezier(Bezier {
                x1: 0.37,
                y1: 0.0,
                x2: 0.63,
                y2: 1.0,
                ..Default::default()
            }),
            EasingJson::Linear => Easing_type::Bezier(Bezier {
                x1: 0.0,
                y1: 0.0,
                x2: 1.0,
                y2: 1.0,
                ..Default::default()
            }),
            EasingJson::EaseInBack => Easing_type::Bezier(Bezier {
                x1: 0.36,
                y1: 0.0,
                x2: 0.66,
                y2: -0.56,
                ..Default::default()
            }),
            EasingJson::EaseOutBack => Easing_type::Bezier(Bezier {
                x1: 0.34,
                y1: 1.56,
                x2: 0.64,
                y2: 1.0,
                ..Default::default()
            }),
            EasingJson::EaseInAndOutBack => Easing_type::Bezier(Bezier {
                x1: 0.68,
                y1: -0.6,
                x2: 0.32,
                y2: 1.6,
                ..Default::default()
            }),
            EasingJson::CustomCubicBezier { bezier } => Easing_type::Bezier(bezier.into()),
            EasingJson::Gentle => Easing_type::Spring(Spring {
                mass: 1.0,
                damping: 15.0,
                stiffness: 100.0,
                ..Default::default()
            }),
            EasingJson::Quick => Easing_type::Spring(Spring {
                mass: 1.0,
                damping: 20.0,
                stiffness: 300.0,
                ..Default::default()
            }),
            EasingJson::Bouncy => Easing_type::Spring(Spring {
                mass: 1.0,
                damping: 15.0,
                stiffness: 600.0,
                ..Default::default()
            }),
            EasingJson::Slow => Easing_type::Spring(Spring {
                mass: 1.0,
                damping: 20.0,
                stiffness: 80.0,
                ..Default::default()
            }),
            EasingJson::CustomSpring { spring } => Easing_type::Spring(spring.into()),
        }
    }
}

impl Into<Easing> for EasingJson {
    fn into(self) -> Easing {
        Easing { easing_type: Some(self.into()), ..Default::default() }
    }
}

#[derive(Clone, Copy, PartialEq, Eq, Debug, Hash, Serialize, Deserialize, Default)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum TransitionDirectionJson {
    #[default]
    Unspecified = 0,
    Left = 1,
    Right = 2,
    Top = 3,
    Bottom = 4,
}

impl Into<TransitionDirection> for TransitionDirectionJson {
    fn into(self) -> TransitionDirection {
        match self {
            TransitionDirectionJson::Unspecified => {
                TransitionDirection::TRANSITION_DIRECTION_UNSPECIFIED
            }
            TransitionDirectionJson::Left => TransitionDirection::TRANSITION_DIRECTION_LEFT,
            TransitionDirectionJson::Right => TransitionDirection::TRANSITION_DIRECTION_RIGHT,
            TransitionDirectionJson::Top => TransitionDirection::TRANSITION_DIRECTION_TOP,
            TransitionDirectionJson::Bottom => TransitionDirection::TRANSITION_DIRECTION_BOTTOM,
        }
    }
}

/// This represents the Figma "Transition" type.
/// https://www.figma.com/plugin-docs/api/Transition/
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(tag = "type", rename_all = "SCREAMING_SNAKE_CASE")]
pub enum TransitionJson {
    Dissolve {
        #[serde(default, deserialize_with = "deserialize_lossy_default")]
        easing: EasingJson,
        #[serde(default, deserialize_with = "deserialize_null_default")]
        duration: f32,
    },
    SmartAnimate {
        #[serde(default, deserialize_with = "deserialize_lossy_default")]
        easing: EasingJson,
        #[serde(default, deserialize_with = "deserialize_null_default")]
        duration: f32,
    },
    ScrollAnimate {
        #[serde(default, deserialize_with = "deserialize_lossy_default")]
        easing: EasingJson,
        #[serde(default, deserialize_with = "deserialize_null_default")]
        duration: f32,
    },
    MoveIn {
        #[serde(default, deserialize_with = "deserialize_lossy_default")]
        easing: EasingJson,
        #[serde(default, deserialize_with = "deserialize_null_default")]
        duration: f32,
        #[serde(default, deserialize_with = "deserialize_lossy_default")]
        direction: TransitionDirectionJson,
        #[serde(rename = "matchLayers", default, deserialize_with = "deserialize_null_default")]
        match_layers: bool,
    },
    MoveOut {
        #[serde(default, deserialize_with = "deserialize_lossy_default")]
        easing: EasingJson,
        #[serde(default, deserialize_with = "deserialize_null_default")]
        duration: f32,
        #[serde(default, deserialize_with = "deserialize_lossy_default")]
        direction: TransitionDirectionJson,
        #[serde(rename = "matchLayers", default, deserialize_with = "deserialize_null_default")]
        match_layers: bool,
    },
    Push {
        #[serde(default, deserialize_with = "deserialize_lossy_default")]
        easing: EasingJson,
        #[serde(default, deserialize_with = "deserialize_null_default")]
        duration: f32,
        #[serde(default, deserialize_with = "deserialize_lossy_default")]
        direction: TransitionDirectionJson,
        #[serde(rename = "matchLayers", default, deserialize_with = "deserialize_null_default")]
        match_layers: bool,
    },
    SlideIn {
        #[serde(default, deserialize_with = "deserialize_lossy_default")]
        easing: EasingJson,
        #[serde(default, deserialize_with = "deserialize_null_default")]
        duration: f32,
        #[serde(default, deserialize_with = "deserialize_lossy_default")]
        direction: TransitionDirectionJson,
        #[serde(rename = "matchLayers", default, deserialize_with = "deserialize_null_default")]
        match_layers: bool,
    },
    SlideOut {
        #[serde(default, deserialize_with = "deserialize_lossy_default")]
        easing: EasingJson,
        #[serde(default, deserialize_with = "deserialize_null_default")]
        duration: f32,
        #[serde(default, deserialize_with = "deserialize_lossy_default")]
        direction: TransitionDirectionJson,
        #[serde(rename = "matchLayers", default, deserialize_with = "deserialize_null_default")]
        match_layers: bool,
    },
}

impl Into<Transition_type> for TransitionJson {
    fn into(self) -> Transition_type {
        match self {
            TransitionJson::Dissolve { easing, duration } => Transition_type::Dissolve(Dissolve {
                easing: Some(easing.into()).into(),
                duration,
                ..Default::default()
            }),
            TransitionJson::SmartAnimate { easing, duration } => {
                Transition_type::SmartAnimate(SmartAnimate {
                    easing: Some(easing.into()).into(),
                    duration,
                    ..Default::default()
                })
            }
            TransitionJson::ScrollAnimate { easing, duration } => {
                Transition_type::ScrollAnimate(ScrollAnimate {
                    easing: Some(easing.into()).into(),
                    duration,
                    ..Default::default()
                })
            }
            TransitionJson::MoveIn { easing, duration, direction, match_layers } => {
                Transition_type::MoveIn(MoveIn {
                    easing: Some(easing.into()).into(),
                    duration,
                    direction: TransitionDirection::from(direction.into()).into(),
                    match_layers,
                    ..Default::default()
                })
            }
            TransitionJson::MoveOut { easing, duration, direction, match_layers } => {
                Transition_type::MoveOut(MoveOut {
                    easing: Some(easing.into()).into(),
                    duration,
                    direction: TransitionDirection::from(direction.into()).into(),
                    match_layers,
                    ..Default::default()
                })
            }
            TransitionJson::Push { easing, duration, direction, match_layers } => {
                Transition_type::Push(Push {
                    easing: Some(easing.into()).into(),
                    duration,
                    direction: TransitionDirection::from(direction.into()).into(),
                    match_layers,
                    ..Default::default()
                })
            }
            TransitionJson::SlideIn { easing, duration, direction, match_layers } => {
                Transition_type::SlideIn(SlideIn {
                    easing: Some(easing.into()).into(),
                    duration,
                    direction: TransitionDirection::from(direction.into()).into(),
                    match_layers,
                    ..Default::default()
                })
            }
            TransitionJson::SlideOut { easing, duration, direction, match_layers } => {
                Transition_type::SlideOut(SlideOut {
                    easing: Some(easing.into()).into(),
                    duration,
                    direction: TransitionDirection::from(direction.into()).into(),
                    match_layers,
                    ..Default::default()
                })
            }
        }
    }
}

#[derive(Serialize, Deserialize, PartialEq, Clone, Debug, Default)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum NavigationJson {
    #[default]
    Unspecified,
    Navigate,
    Swap,
    Overlay,
    ScrollTo,
    ChangeTo,
}

impl Into<Navigation> for NavigationJson {
    fn into(self) -> Navigation {
        match self {
            NavigationJson::Unspecified => Navigation::NAVIGATION_UNSPECIFIED,
            NavigationJson::Navigate => Navigation::NAVIGATION_NAVIGATE,
            NavigationJson::Swap => Navigation::NAVIGATION_SWAP,
            NavigationJson::Overlay => Navigation::NAVIGATION_OVERLAY,
            NavigationJson::ScrollTo => Navigation::NAVIGATION_SCROLL_TO,
            NavigationJson::ChangeTo => Navigation::NAVIGATION_CHANGE_TO,
        }
    }
}

#[derive(Serialize, Deserialize, PartialEq, Clone, Debug)]
pub struct VectorJson {
    pub x: f32,
    pub y: f32,
}

impl Into<Vector> for VectorJson {
    fn into(self) -> Vector {
        Vector { x: self.x, y: self.y, ..Default::default() }
    }
}

fn deserialize_optional_lossy<'de, T, D>(deserializer: D) -> Result<Option<T>, D::Error>
where
    T: DeserializeOwned,
    D: Deserializer<'de>,
{
    let val = Option::<serde_json::Value>::deserialize(deserializer)?;
    Ok(val.and_then(|v| match serde_json::from_value::<T>(v) {
        Ok(parsed) => Some(parsed),
        Err(e) => {
            log::debug!("Ignoring unsupported Figma interaction field: {}", e);
            None
        }
    }))
}

fn deserialize_null_default<'de, T, D>(deserializer: D) -> Result<T, D::Error>
where
    T: Default + DeserializeOwned,
    D: Deserializer<'de>,
{
    Ok(Option::<T>::deserialize(deserializer)?.unwrap_or_default())
}

fn deserialize_lossy_default<'de, T, D>(deserializer: D) -> Result<T, D::Error>
where
    T: Default + DeserializeOwned,
    D: Deserializer<'de>,
{
    Ok(deserialize_optional_lossy(deserializer)?.unwrap_or_default())
}

pub(crate) fn deserialize_lossy_vec<'de, T, D>(deserializer: D) -> Result<Vec<T>, D::Error>
where
    T: DeserializeOwned,
    D: Deserializer<'de>,
{
    struct LossyVecVisitor<T>(std::marker::PhantomData<T>);

    impl<'de, T: DeserializeOwned> serde::de::Visitor<'de> for LossyVecVisitor<T> {
        type Value = Vec<T>;

        fn expecting(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
            formatter.write_str("an optional sequence of interaction items")
        }

        fn visit_none<E>(self) -> Result<Self::Value, E> {
            Ok(Vec::new())
        }

        fn visit_unit<E>(self) -> Result<Self::Value, E> {
            Ok(Vec::new())
        }

        fn visit_some<D>(self, deserializer: D) -> Result<Self::Value, D::Error>
        where
            D: Deserializer<'de>,
        {
            deserializer.deserialize_any(self)
        }

        fn visit_seq<A>(self, mut seq: A) -> Result<Self::Value, A::Error>
        where
            A: serde::de::SeqAccess<'de>,
        {
            let mut items = Vec::with_capacity(seq.size_hint().unwrap_or(0));
            while let Some(elem) = seq.next_element::<Option<serde_json::Value>>()? {
                if let Some(v) = elem {
                    match serde_json::from_value::<T>(v) {
                        Ok(parsed) => items.push(parsed),
                        Err(e) => {
                            log::debug!("Ignoring unsupported Figma interaction item: {}", e);
                        }
                    }
                }
            }
            Ok(items)
        }
    }

    deserializer.deserialize_option(LossyVecVisitor(std::marker::PhantomData))
}

fn deserialize_key_codes<'de, D>(deserializer: D) -> Result<Vec<u8>, D::Error>
where
    D: Deserializer<'de>,
{
    let raw = Option::<Vec<Option<i64>>>::deserialize(deserializer)?;
    let mut non_null_count = 0usize;
    let mut codes = Vec::new();
    for code in raw.unwrap_or_default().into_iter().flatten() {
        non_null_count += 1;
        if let Ok(byte) = u8::try_from(code) {
            codes.push(byte);
        }
    }
    // Empty `key_codes` is a wildcard ("trigger on any key"); fail deserialization if all supplied
    // non-null entries are outside `u8` range so invalid key codes do not become wildcards.
    if non_null_count > 0 && codes.is_empty() {
        return Err(serde::de::Error::custom("OnKeyDown keyCodes contained no valid u8 values"));
    }
    Ok(codes)
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(tag = "type", rename_all = "SCREAMING_SNAKE_CASE")]
pub enum ActionJson {
    /// Navigate the top-level frame back.
    Back,
    /// Close the top-most overlay.
    Close,
    /// Open a URL.
    Url { url: String },
    /// Do something with a destination node.
    Node {
        /// Node that we should navigate to, change to, open or swap as an overlay, or
        /// scroll to reveal.
        #[serde(rename = "destinationId", default)]
        destination_id: Option<String>,

        /// The kind of navigation (really the kind of action) to perform with the destination
        /// node (if it's not null).
        #[serde(default, deserialize_with = "deserialize_lossy_default")]
        navigation: NavigationJson,

        /// The transition to perform for this animation, if any.
        #[serde(default, deserialize_with = "deserialize_optional_lossy")]
        transition: Option<TransitionJson>,

        /// For "Navigate", should we open the new node with the current frame's scroll position?
        #[serde(
            rename = "preserveScrollPosition",
            default,
            deserialize_with = "deserialize_null_default"
        )]
        preserve_scroll_position: bool,

        /// For overlays that have been manually positioned.
        #[serde(
            rename = "overlayRelativePosition",
            default,
            deserialize_with = "deserialize_optional_lossy"
        )]
        overlay_relative_position: Option<VectorJson>,
    },
}

impl Into<Action_type> for ActionJson {
    fn into(self) -> Action_type {
        match self {
            ActionJson::Back => Action_type::Back(().into()),
            ActionJson::Close => Action_type::Close(().into()),
            ActionJson::Url { url } => Action_type::Url(ActionUrl { url, ..Default::default() }),
            ActionJson::Node {
                destination_id,
                navigation,
                transition,
                preserve_scroll_position,
                overlay_relative_position,
            } => {
                let nav: Navigation = navigation.into();
                let pos: Option<Vector> = overlay_relative_position.map(|v| v.into());
                Action_type::Node(action::Node {
                    destination_id,
                    navigation: nav.into(),
                    transition: transition
                        .map(|t| Transition {
                            transition_type: Some(t.into()),
                            ..Default::default()
                        })
                        .into(),
                    preserve_scroll_position,
                    overlay_relative_position: pos.into(),
                    ..Default::default()
                })
            }
        }
    }
}

/// Trigger describes the input needed to make a Reaction happen.
#[derive(Serialize, Debug, Clone, PartialEq)]
#[serde(tag = "type", rename_all = "SCREAMING_SNAKE_CASE")]
pub enum TriggerJson {
    OnClick,
    /// OnHover reverts Navigate and ChangeTo actions when hovering ends.
    OnHover,
    /// OnPress reverts Navigate and ChangeTo actions when hovering ends.
    OnPress,
    OnDrag,
    /// OnKeyDown has a list of JavaScript key codes. Multiple key codes are
    /// interpreted as all the keys pressed at the same time. An empty vector
    /// is interpreted as any key can trigger the action.
    OnKeyDown {
        #[serde(rename = "keyCodes")]
        key_codes: Vec<u8>,
    },
    AfterTimeout {
        timeout: f32,
    },
    MouseEnter {
        delay: f32,
    },
    MouseLeave {
        delay: f32,
    },
    MouseUp {
        delay: f32,
    },
    MouseDown {
        delay: f32,
    },
}

/// Wire deserialization representation for `TriggerJson`.
///
/// `TriggerJson::deserialize` delegates here so non-`KEYBOARD` `ON_KEY_DOWN` `device` payloads are
/// rejected during deserialization without exposing a wire-only `device` field publicly.
#[derive(Deserialize)]
#[serde(tag = "type", rename_all = "SCREAMING_SNAKE_CASE")]
enum TriggerJsonWire {
    OnClick,
    OnHover,
    OnPress,
    // Figma REST API emits "DRAG" whereas Plugin API vsw-reactions emits "ON_DRAG".
    #[serde(alias = "DRAG")]
    OnDrag,
    OnKeyDown {
        #[serde(default)]
        device: Option<String>,
        #[serde(rename = "keyCodes", default, deserialize_with = "deserialize_key_codes")]
        key_codes: Vec<u8>,
    },
    AfterTimeout {
        #[serde(default, deserialize_with = "deserialize_null_default")]
        timeout: f32,
    },
    MouseEnter {
        #[serde(default, deserialize_with = "deserialize_null_default")]
        delay: f32,
    },
    MouseLeave {
        #[serde(default, deserialize_with = "deserialize_null_default")]
        delay: f32,
    },
    MouseUp {
        #[serde(default, deserialize_with = "deserialize_null_default")]
        delay: f32,
    },
    MouseDown {
        #[serde(default, deserialize_with = "deserialize_null_default")]
        delay: f32,
    },
}

impl<'de> Deserialize<'de> for TriggerJson {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        match TriggerJsonWire::deserialize(deserializer)? {
            TriggerJsonWire::OnClick => Ok(TriggerJson::OnClick),
            TriggerJsonWire::OnHover => Ok(TriggerJson::OnHover),
            TriggerJsonWire::OnPress => Ok(TriggerJson::OnPress),
            TriggerJsonWire::OnDrag => Ok(TriggerJson::OnDrag),
            TriggerJsonWire::OnKeyDown { device, key_codes } => {
                if let Some(dev) = device.as_deref() {
                    if !dev.eq_ignore_ascii_case("KEYBOARD") {
                        return Err(serde::de::Error::custom(format!(
                            "Unsupported non-keyboard ON_KEY_DOWN device: {}",
                            dev
                        )));
                    }
                }
                Ok(TriggerJson::OnKeyDown { key_codes })
            }
            TriggerJsonWire::AfterTimeout { timeout } => Ok(TriggerJson::AfterTimeout { timeout }),
            TriggerJsonWire::MouseEnter { delay } => Ok(TriggerJson::MouseEnter { delay }),
            TriggerJsonWire::MouseLeave { delay } => Ok(TriggerJson::MouseLeave { delay }),
            TriggerJsonWire::MouseUp { delay } => Ok(TriggerJson::MouseUp { delay }),
            TriggerJsonWire::MouseDown { delay } => Ok(TriggerJson::MouseDown { delay }),
        }
    }
}

impl Into<Trigger_type> for TriggerJson {
    fn into(self) -> Trigger_type {
        match self {
            TriggerJson::OnClick => Trigger_type::Click(().into()),
            TriggerJson::OnHover => Trigger_type::Hover(().into()),
            TriggerJson::OnPress => Trigger_type::Press(().into()),
            TriggerJson::OnDrag => Trigger_type::Drag(().into()),
            TriggerJson::OnKeyDown { key_codes } => {
                Trigger_type::KeyDown(KeyDown { key_codes, ..Default::default() })
            }
            TriggerJson::AfterTimeout { timeout } => {
                Trigger_type::AfterTimeout(Timeout { timeout, ..Default::default() })
            }
            TriggerJson::MouseEnter { delay } => {
                Trigger_type::MouseEnter(MouseEnter { delay, ..Default::default() })
            }
            TriggerJson::MouseLeave { delay } => {
                Trigger_type::MouseLeave(MouseLeave { delay, ..Default::default() })
            }
            TriggerJson::MouseUp { delay } => {
                Trigger_type::MouseUp(MouseUp { delay, ..Default::default() })
            }
            TriggerJson::MouseDown { delay } => {
                Trigger_type::MouseDown(MouseDown { delay, ..Default::default() })
            }
        }
    }
}

/// Reaction describes interactivity for a node. It's a pair of Action ("what happens?") and
/// Trigger ("how do you make it happen?")
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct ReactionJson {
    pub action: Option<ActionJson>,
    pub trigger: TriggerJson,
}

/// Figma REST API interaction item on `Node.interactions`.
///
/// Unlike legacy `vsw-reactions` (`ReactionJson` with singular `action`), REST API payloads carry
/// `actions` as an array (which may contain `null` entries or unsupported action types like
/// `SET_VARIABLE`) and `trigger` may be `null` or an unsupported trigger type.
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq, Default)]
pub struct InteractionJson {
    #[serde(default, deserialize_with = "deserialize_optional_lossy")]
    pub trigger: Option<TriggerJson>,
    #[serde(default, deserialize_with = "deserialize_optional_lossy")]
    pub action: Option<ActionJson>,
    #[serde(default, deserialize_with = "deserialize_lossy_vec")]
    pub actions: Vec<ActionJson>,
}

impl InteractionJson {
    /// Iterates over all parsed actions in this interaction.
    ///
    /// Falls back to singular `action` whenever `actions` is empty (including when every item in
    /// `actions` was `null` or an unsupported action type filtered out during deserialization).
    pub fn iter_actions(&self) -> std::slice::Iter<'_, ActionJson> {
        if !self.actions.is_empty() {
            self.actions.iter()
        } else if let Some(action) = &self.action {
            std::slice::from_ref(action).iter()
        } else {
            [].iter()
        }
    }

    fn build_trigger(trigger: TriggerJson) -> Trigger {
        Trigger { trigger_type: Some(trigger.into()), ..Default::default() }
    }

    fn build_reaction(action: ActionJson, trigger: Trigger) -> Reaction {
        Reaction {
            action: Some(Action { action_type: Some(action.into()), ..Default::default() }).into(),
            trigger: Some(trigger).into(),
            ..Default::default()
        }
    }

    /// Converts this REST interaction into one or more protobuf `Reaction` entries, using the
    /// same `actions`-before-`action` fallback as [`Self::iter_actions`].
    pub fn to_reactions(&self) -> Vec<Reaction> {
        let Some(trigger) = &self.trigger else {
            return Vec::new();
        };
        let mut actions = self.iter_actions();
        let Some(first) = actions.next() else {
            return Vec::new();
        };
        let proto_trigger = Self::build_trigger(trigger.clone());
        let mut out = Vec::with_capacity(1 + actions.size_hint().0);
        let mut prev = first;
        for next in actions {
            out.push(Self::build_reaction(prev.clone(), proto_trigger.clone()));
            prev = next;
        }
        out.push(Self::build_reaction(prev.clone(), proto_trigger));
        out
    }

    /// Converts this REST interaction into one or more protobuf `Reaction` entries, using the
    /// same `actions`-before-`action` fallback as [`Self::iter_actions`].
    pub fn into_reactions(mut self) -> Vec<Reaction> {
        let Some(trigger) = self.trigger else {
            return Vec::new();
        };
        let Some(last_action) = self.actions.pop() else {
            return self
                .action
                .map(|action| vec![Self::build_reaction(action, Self::build_trigger(trigger))])
                .unwrap_or_default();
        };
        let proto_trigger = Self::build_trigger(trigger);
        let mut out = Vec::with_capacity(self.actions.len() + 1);
        for action in self.actions {
            out.push(Self::build_reaction(action, proto_trigger.clone()));
        }
        out.push(Self::build_reaction(last_action, proto_trigger));
        out
    }
}

impl Into<Option<Reaction>> for ReactionJson {
    fn into(self) -> Option<Reaction> {
        if let Some(action) = self.action {
            Some(Reaction {
                action: Some(Action { action_type: Some(action.into()), ..Default::default() })
                    .into(),
                trigger: Some(Trigger {
                    trigger_type: Some(self.trigger.into()),
                    ..Default::default()
                })
                .into(),
                ..Default::default()
            })
        } else {
            None
        }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq, Copy)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum OverlayPositionJson {
    Center,
    TopLeft,
    TopCenter,
    TopRight,
    BottomLeft,
    BottomCenter,
    BottomRight,
    Manual, // then we look at the Action
}

impl Into<OverlayPositionType> for OverlayPositionJson {
    fn into(self) -> OverlayPositionType {
        match self {
            OverlayPositionJson::Center => OverlayPositionType::OVERLAY_POSITION_TYPE_CENTER,
            OverlayPositionJson::TopLeft => OverlayPositionType::OVERLAY_POSITION_TYPE_TOP_LEFT,
            OverlayPositionJson::TopCenter => OverlayPositionType::OVERLAY_POSITION_TYPE_TOP_CENTER,
            OverlayPositionJson::TopRight => OverlayPositionType::OVERLAY_POSITION_TYPE_TOP_RIGHT,
            OverlayPositionJson::BottomLeft => {
                OverlayPositionType::OVERLAY_POSITION_TYPE_BOTTOM_LEFT
            }
            OverlayPositionJson::BottomCenter => {
                OverlayPositionType::OVERLAY_POSITION_TYPE_BOTTOM_CENTER
            }
            OverlayPositionJson::BottomRight => {
                OverlayPositionType::OVERLAY_POSITION_TYPE_BOTTOM_RIGHT
            }
            OverlayPositionJson::Manual => OverlayPositionType::OVERLAY_POSITION_TYPE_MANUAL,
        }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct OverlayBackgroundJson {
    pub color: Option<FigmaColor>,
}

impl Into<OverlayBackground> for OverlayBackgroundJson {
    fn into(self) -> OverlayBackground {
        let float_color: Option<FloatColor> = self.color.map(|c| (&c).into());
        OverlayBackground { color: float_color.into(), ..Default::default() }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq, Copy)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum OverlayBackgroundInteractionJson {
    None,
    CloseOnClickOutside,
}

impl Into<OverlayBackgroundInteraction> for OverlayBackgroundInteractionJson {
    fn into(self) -> OverlayBackgroundInteraction {
        match self {
            OverlayBackgroundInteractionJson::None => {
                OverlayBackgroundInteraction::OVERLAY_BACKGROUND_INTERACTION_NONE
            }
            OverlayBackgroundInteractionJson::CloseOnClickOutside => {
                OverlayBackgroundInteraction::OVERLAY_BACKGROUND_INTERACTION_CLOSE_ON_CLICK_OUTSIDE
            }
        }
    }
}

/// Some frame properties are only available through the plugin API and are needed to
/// implement Reactions properly. They're included in this FrameExtras struct.
#[derive(PartialEq, Debug, Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct FrameExtrasJson {
    pub number_of_fixed_children: usize,
    pub overlay_position_type: OverlayPositionJson,
    pub overlay_background: OverlayBackgroundJson,
    pub overlay_background_interaction: OverlayBackgroundInteractionJson,
}

impl Into<FrameExtras> for FrameExtrasJson {
    fn into(self) -> FrameExtras {
        let bg: OverlayBackground = self.overlay_background.into();
        FrameExtras {
            fixed_children: self.number_of_fixed_children as u32,
            overlay_position_type: OverlayPositionType::from(self.overlay_position_type.into())
                .into(), //It's confusing but it works? Need to convert one
            overlay_background: Some(bg).into(),
            overlay_background_interaction: OverlayBackgroundInteraction::from(
                self.overlay_background_interaction.into(),
            )
            .into(),
            ..Default::default()
        }
    }
}

#[test]
fn parse_frame_extras() {
    use serde_json;

    let def = r#"{
    "numberOfFixedChildren": 0,
    "overlayPositionType": "CENTER",
    "overlayBackground": {
        "type": "NONE"
    },
    "overlayBackgroundInteraction": "NONE",
    "overflowDirection": "NONE"
}"#;

    let click_to_close = r#"{
    "numberOfFixedChildren": 0,
    "overlayPositionType": "BOTTOM_CENTER",
    "overlayBackground": {
        "type": "SOLID_COLOR",
        "color": {
            "r": 0,
            "g": 0,
            "b": 0,
            "a": 0.25
        }
    },
    "overlayBackgroundInteraction": "CLOSE_ON_CLICK_OUTSIDE",
    "overflowDirection": "HORIZONTAL_AND_VERTICAL_SCROLLING"
}"#;

    let def: FrameExtrasJson = serde_json::from_str(def).unwrap();
    let click_to_close: FrameExtrasJson = serde_json::from_str(click_to_close).unwrap();

    assert_eq!(def.number_of_fixed_children, 0);
    assert_eq!(def.overlay_position_type, OverlayPositionJson::Center);
    assert_eq!(def.overlay_background, OverlayBackgroundJson { color: None });
    assert_eq!(def.overlay_background_interaction, OverlayBackgroundInteractionJson::None);

    assert_eq!(click_to_close.number_of_fixed_children, 0);
    assert_eq!(click_to_close.overlay_position_type, OverlayPositionJson::BottomCenter);
    assert_eq!(
        click_to_close.overlay_background,
        OverlayBackgroundJson { color: Some(FigmaColor { r: 0.0, g: 0.0, b: 0.0, a: 0.25 }) }
    );
    assert_eq!(
        click_to_close.overlay_background_interaction,
        OverlayBackgroundInteractionJson::CloseOnClickOutside
    );
}

#[test]
fn parse_reactions() {
    use serde_json;
    use serde_json::Result;

    let multiple_json_text = r#"[{"action":{"type":"NODE","destinationId":"13:1","navigation":"NAVIGATE","transition":{"type":"SMART_ANIMATE","easing":{"type":"EASE_IN_AND_OUT"},"duration":0.6000000238418579},"preserveScrollPosition":false},"trigger":{"type":"ON_CLICK"}},{"action":{"type":"NODE","destinationId":"13:1","navigation":"OVERLAY","transition":{"type":"MOVE_IN","direction":"RIGHT","matchLayers":false,"easing":{"type":"EASE_OUT"},"duration":0.30000001192092896},"preserveScrollPosition":false},"trigger":{"type":"ON_DRAG"}},{"action":{"type":"NODE","destinationId":"13:1","navigation":"SWAP","transition":{"type":"SMART_ANIMATE","easing":{"type":"EASE_OUT"},"duration":0.30000001192092896},"preserveScrollPosition":false},"trigger":{"type":"ON_KEY_DOWN","keyCodes":[60]}}]"#;
    let scroll_json_text = r#"[{"action":{"type":"NODE","destinationId":"241:2","navigation":"SCROLL_TO","transition":{"type":"SCROLL_ANIMATE","easing":{"type":"EASE_OUT"},"duration":0.30000001192092896},"preserveScrollPosition":false},"trigger":{"type":"ON_HOVER"}}]"#;
    let overlay_json_text = r#"[{"action":{"type":"NODE","destinationId":"222:27","navigation":"OVERLAY","transition":{"type":"MOVE_IN","direction":"TOP","matchLayers":false,"easing":{"type":"EASE_IN_AND_OUT"},"duration":0.30000001192092896},"preserveScrollPosition":false},"trigger":{"type":"ON_CLICK","keyCodes":[]}}]"#;

    let maybe_multiple: Result<Vec<ReactionJson>> = serde_json::from_str(multiple_json_text);
    let maybe_scroll: Result<Vec<ReactionJson>> = serde_json::from_str(scroll_json_text);
    let maybe_overlay: Result<Vec<ReactionJson>> = serde_json::from_str(overlay_json_text);

    let mut multiple_json = maybe_multiple.unwrap();
    let mut scroll_json = maybe_scroll.unwrap();
    let mut overlay_json = maybe_overlay.unwrap();

    // We should check that `into` did what we expected it to do here.

    let multiple: Vec<Reaction> =
        multiple_json.drain(..).map(|json| Into::<Option<Reaction>>::into(json).unwrap()).collect();
    let scroll: Vec<Reaction> =
        scroll_json.drain(..).map(|json| Into::<Option<Reaction>>::into(json).unwrap()).collect();
    let overlay: Vec<Reaction> =
        overlay_json.drain(..).map(|json| Into::<Option<Reaction>>::into(json).unwrap()).collect();

    // assert if the parsed objects have the correct properties
    assert_eq!(multiple.len(), 3);
    assert_eq!(scroll.len(), 1);
    assert_eq!(overlay.len(), 1);

    // Multiple assertions
    assert_eq!(
        multiple[0].action.as_ref().unwrap().action_type.as_ref().unwrap(),
        &Action_type::Node(action::Node {
            destination_id: Some("13:1".to_string()),
            navigation: Navigation::NAVIGATION_NAVIGATE.into(),
            transition: Some(Transition {
                transition_type: Some(Transition_type::SmartAnimate(SmartAnimate {
                    easing: Some(Easing {
                        easing_type: Some(Easing_type::Bezier(Bezier {
                            x1: 0.37,
                            y1: 0.0,
                            x2: 0.63,
                            y2: 1.0,
                            ..Default::default()
                        })),
                        ..Default::default()
                    })
                    .into(),
                    duration: 0.6000000238418579,
                    ..Default::default()
                })),
                ..Default::default()
            })
            .into(),
            preserve_scroll_position: false,
            overlay_relative_position: None.into(),
            ..Default::default()
        })
    );
    assert_eq!(
        multiple[0].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::Click(().into())
    );

    assert_eq!(
        multiple[1].action.as_ref().unwrap().action_type.as_ref().unwrap(),
        &Action_type::Node(action::Node {
            destination_id: Some("13:1".to_string()),
            navigation: Navigation::NAVIGATION_OVERLAY.into(),
            transition: Some(Transition {
                transition_type: Some(Transition_type::MoveIn(MoveIn {
                    easing: Some(Easing {
                        easing_type: Some(Easing_type::Bezier(Bezier {
                            x1: 0.61,
                            y1: 1.0,
                            x2: 0.88,
                            y2: 1.0,
                            ..Default::default()
                        })),
                        ..Default::default()
                    })
                    .into(),
                    duration: 0.30000001192092896,
                    direction: TransitionDirection::TRANSITION_DIRECTION_RIGHT.into(),
                    match_layers: false,
                    ..Default::default()
                })),
                ..Default::default()
            })
            .into(),
            preserve_scroll_position: false,
            overlay_relative_position: None.into(),
            ..Default::default()
        })
    );
    assert_eq!(
        multiple[1].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::Drag(().into())
    );

    assert_eq!(
        multiple[2].action.as_ref().unwrap().action_type.as_ref().unwrap(),
        &Action_type::Node(action::Node {
            destination_id: Some("13:1".to_string()),
            navigation: Navigation::NAVIGATION_SWAP.into(),
            transition: Some(Transition {
                transition_type: Some(Transition_type::SmartAnimate(SmartAnimate {
                    easing: Some(Easing {
                        easing_type: Some(Easing_type::Bezier(Bezier {
                            x1: 0.61,
                            y1: 1.0,
                            x2: 0.88,
                            y2: 1.0,
                            ..Default::default()
                        })),
                        ..Default::default()
                    })
                    .into(),
                    duration: 0.30000001192092896,
                    ..Default::default()
                })),
                ..Default::default()
            })
            .into(),
            preserve_scroll_position: false,
            overlay_relative_position: None.into(),
            ..Default::default()
        })
    );
    assert_eq!(
        multiple[2].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::KeyDown(KeyDown { key_codes: vec![60], ..Default::default() })
    );

    // Scroll assertions
    assert_eq!(
        scroll[0].action.as_ref().unwrap().action_type.as_ref().unwrap(),
        &Action_type::Node(action::Node {
            destination_id: Some("241:2".to_string()),
            navigation: Navigation::NAVIGATION_SCROLL_TO.into(),
            transition: Some(Transition {
                transition_type: Some(Transition_type::ScrollAnimate(ScrollAnimate {
                    easing: Some(Easing {
                        easing_type: Some(Easing_type::Bezier(Bezier {
                            x1: 0.61,
                            y1: 1.0,
                            x2: 0.88,
                            y2: 1.0,
                            ..Default::default()
                        })),
                        ..Default::default()
                    })
                    .into(),
                    duration: 0.30000001192092896,
                    ..Default::default()
                })),
                ..Default::default()
            })
            .into(),
            preserve_scroll_position: false,
            overlay_relative_position: None.into(),
            ..Default::default()
        })
    );
    assert_eq!(
        scroll[0].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::Hover(().into())
    );

    // Overlay assertions
    assert_eq!(
        overlay[0].action.as_ref().unwrap().action_type.as_ref().unwrap(),
        &Action_type::Node(action::Node {
            destination_id: Some("222:27".to_string()),
            navigation: Navigation::NAVIGATION_OVERLAY.into(),
            transition: Some(Transition {
                transition_type: Some(Transition_type::MoveIn(MoveIn {
                    easing: Some(Easing {
                        easing_type: Some(Easing_type::Bezier(Bezier {
                            x1: 0.37,
                            y1: 0.0,
                            x2: 0.63,
                            y2: 1.0,
                            ..Default::default()
                        })),
                        ..Default::default()
                    })
                    .into(),
                    duration: 0.30000001192092896,
                    direction: TransitionDirection::TRANSITION_DIRECTION_TOP.into(),
                    match_layers: false,
                    ..Default::default()
                })),
                ..Default::default()
            })
            .into(),
            preserve_scroll_position: false,
            overlay_relative_position: None.into(),
            ..Default::default()
        })
    );
    assert_eq!(
        overlay[0].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::Click(().into())
    );
}

#[test]
fn parse_rest_interactions_with_nulls_and_unsupported_types() {
    let json = r#"[
        {
            "trigger": { "type": "ON_CLICK" },
            "actions": [null]
        },
        {
            "trigger": { "type": "ON_MEDIA_END" },
            "actions": [
                {
                    "type": "NODE",
                    "destinationId": "1:2",
                    "navigation": "NAVIGATE",
                    "transition": null
                }
            ]
        },
        {
            "trigger": { "type": "ON_HOVER" },
            "actions": [
                { "type": "SET_VARIABLE", "variableId": "v1" },
                {
                    "type": "NODE",
                    "destinationId": "10:20",
                    "navigation": "CHANGE_TO",
                    "transition": {
                        "type": "SMART_ANIMATE",
                        "easing": {
                            "type": "CUSTOM_CUBIC_BEZIER",
                            "easingFunctionCubicBezier": {
                                "x1": 0.2,
                                "y1": 0.0,
                                "x2": 0.0,
                                "y2": 1.0
                            }
                        },
                        "duration": 0.2
                    },
                    "resetVideoPosition": false
                }
            ]
        },
        {
            "trigger": { "type": "ON_KEY_DOWN", "device": "KEYBOARD", "keyCodes": [65, 300] },
            "actions": [
                { "type": "BACK" }
            ]
        },
        {
            "trigger": { "type": "DRAG" },
            "actions": [
                { "type": "CLOSE" }
            ]
        }
    ]"#;

    let interactions: Vec<InteractionJson> = serde_json::from_str(json).unwrap();
    assert_eq!(interactions.len(), 5);

    let reactions: Vec<Reaction> =
        interactions.into_iter().flat_map(|i| i.into_reactions()).collect();
    assert_eq!(reactions.len(), 3);

    assert_eq!(
        reactions[0].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::Hover(().into())
    );
    match reactions[0].action.as_ref().unwrap().action_type.as_ref().unwrap() {
        Action_type::Node(node_action) => {
            assert_eq!(node_action.destination_id.as_deref(), Some("10:20"));
            assert_eq!(
                node_action.navigation.enum_value_or_default(),
                Navigation::NAVIGATION_CHANGE_TO
            );
            let trans = node_action.transition.as_ref().unwrap();
            match trans.transition_type.as_ref().unwrap() {
                Transition_type::SmartAnimate(sa) => {
                    assert!((sa.duration - 0.2).abs() < 1e-6);
                    match sa.easing.as_ref().unwrap().easing_type.as_ref().unwrap() {
                        Easing_type::Bezier(b) => {
                            assert!((b.x1 - 0.2).abs() < 1e-6);
                            assert!((b.y1 - 0.0).abs() < 1e-6);
                            assert!((b.x2 - 0.0).abs() < 1e-6);
                            assert!((b.y2 - 1.0).abs() < 1e-6);
                        }
                        other => panic!("Unexpected easing: {:?}", other),
                    }
                }
                other => panic!("Unexpected transition: {:?}", other),
            }
        }
        other => panic!("Unexpected action: {:?}", other),
    }

    assert_eq!(
        reactions[1].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::KeyDown(KeyDown { key_codes: vec![65], ..Default::default() })
    );
    assert_eq!(
        reactions[1].action.as_ref().unwrap().action_type.as_ref().unwrap(),
        &Action_type::Back(().into())
    );
    assert_eq!(
        reactions[2].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::Drag(().into())
    );
    assert_eq!(
        reactions[2].action.as_ref().unwrap().action_type.as_ref().unwrap(),
        &Action_type::Close(().into())
    );
}

#[test]
fn test_null_safety_and_unsupported_interaction_resilience() {
    use dc_bundle::transition::transition::TransitionDirection;

    let node_null_interactions: crate::figma_schema::Node = serde_json::from_str(
        r#"{
            "id": "1:1",
            "name": "NullInteractions",
            "type": "FRAME",
            "constraints": { "vertical": "TOP", "horizontal": "LEFT" },
            "interactions": null
        }"#,
    )
    .expect("Node with interactions: null must deserialize");
    assert!(node_null_interactions.interactions.is_empty());

    let node_array_of_nulls: crate::figma_schema::Node = serde_json::from_str(
        r#"{
            "id": "1:2",
            "name": "NullItemInteractions",
            "type": "FRAME",
            "constraints": { "vertical": "TOP", "horizontal": "LEFT" },
            "interactions": [
                null,
                {
                    "trigger": null,
                    "actions": null
                },
                {
                    "trigger": { "type": "ON_KEY_DOWN", "device": "KEYBOARD", "keyCodes": [300] },
                    "actions": [{ "type": "BACK" }]
                },
                {
                    "trigger": { "type": "ON_KEY_DOWN", "device": "XBOX_ONE", "keyCodes": [0] },
                    "actions": [{ "type": "BACK" }]
                },
                {
                    "trigger": { "type": "ON_KEY_DOWN", "device": "PS4", "keyCodes": [] },
                    "actions": [{ "type": "CLOSE" }]
                },
                {
                    "trigger": { "type": "ON_KEY_DOWN", "device": "KEYBOARD", "keyCodes": null },
                    "actions": [
                        null,
                        {
                            "type": "NODE",
                            "destinationId": null,
                            "navigation": null,
                            "preserveScrollPosition": null,
                            "overlayRelativePosition": null,
                            "transition": {
                                "type": "MOVE_IN",
                                "easing": { "type": "LINEAR" },
                                "duration": null,
                                "direction": null,
                                "matchLayers": null
                            }
                        }
                    ]
                },
                {
                    "trigger": { "type": "AFTER_TIMEOUT", "timeout": null },
                    "actions": [{ "type": "CLOSE" }]
                },
                {
                    "trigger": { "type": "MOUSE_ENTER", "delay": null },
                    "actions": [{ "type": "BACK" }]
                },
                {
                    "trigger": { "type": "MOUSE_LEAVE", "delay": null },
                    "actions": [{ "type": "BACK" }]
                },
                {
                    "trigger": { "type": "MOUSE_UP", "delay": null },
                    "actions": [{ "type": "BACK" }]
                },
                {
                    "trigger": { "type": "MOUSE_DOWN", "delay": null },
                    "actions": [{ "type": "BACK" }]
                },
                {
                    "trigger": { "type": "ON_MEDIA_END" },
                    "actions": [{ "type": "SET_VARIABLE", "variableId": "v1" }]
                },
                {
                    "trigger": { "type": "DRAG" },
                    "actions": [{ "type": "CLOSE" }]
                }
            ]
        }"#,
    )
    .expect("Node with mixed null/unsupported REST interactions must deserialize");

    let reactions: Vec<Reaction> =
        node_array_of_nulls.interactions.into_iter().flat_map(|i| i.into_reactions()).collect();

    // Expect: ON_KEY_DOWN(null keyCodes), AFTER_TIMEOUT(null), MOUSE_ENTER/LEAVE/UP/DOWN(null), DRAG = 7 reactions.
    // Rejected/skipped: null item, trigger:null, [300] out-of-range, XBOX_ONE, PS4, ON_MEDIA_END.
    assert_eq!(reactions.len(), 7);

    assert_eq!(
        reactions[0].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::KeyDown(KeyDown { key_codes: vec![], ..Default::default() })
    );
    match reactions[0].action.as_ref().unwrap().action_type.as_ref().unwrap() {
        Action_type::Node(node_action) => {
            assert_eq!(node_action.destination_id, None);
            assert_eq!(
                node_action.navigation.enum_value_or_default(),
                Navigation::NAVIGATION_UNSPECIFIED
            );
            assert!(!node_action.preserve_scroll_position);
            assert!(node_action.overlay_relative_position.is_none());
            match node_action.transition.as_ref().unwrap().transition_type.as_ref().unwrap() {
                Transition_type::MoveIn(mi) => {
                    assert_eq!(mi.duration, 0.0);
                    assert_eq!(
                        mi.direction.enum_value_or_default(),
                        TransitionDirection::TRANSITION_DIRECTION_UNSPECIFIED
                    );
                    assert!(!mi.match_layers);
                }
                other => panic!("Expected MoveIn transition, got {:?}", other),
            }
        }
        other => panic!("Expected Action_type::Node, got {:?}", other),
    }

    assert_eq!(
        reactions[1].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::AfterTimeout(Timeout { timeout: 0.0, ..Default::default() })
    );
    assert_eq!(
        reactions[2].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::MouseEnter(MouseEnter { delay: 0.0, ..Default::default() })
    );
    assert_eq!(
        reactions[3].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::MouseLeave(MouseLeave { delay: 0.0, ..Default::default() })
    );
    assert_eq!(
        reactions[4].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::MouseUp(MouseUp { delay: 0.0, ..Default::default() })
    );
    assert_eq!(
        reactions[5].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::MouseDown(MouseDown { delay: 0.0, ..Default::default() })
    );
    assert_eq!(
        reactions[6].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::Drag(().into())
    );
}

#[test]
fn test_unsupported_actions_array_falls_back_to_singular_action() {
    use dc_bundle::transition::transition::TransitionDirection;

    let interaction: InteractionJson = serde_json::from_str(
        r#"{
            "trigger": { "type": "ON_CLICK" },
            "actions": [
                { "type": "SET_VARIABLE", "variableId": "v1" },
                null
            ],
            "action": {
                "type": "NODE",
                "destinationId": "42:1",
                "navigation": "OVERLAY",
                "preserveScrollPosition": true,
                "transition": {
                    "type": "PUSH",
                    "direction": "LEFT",
                    "matchLayers": true,
                    "easing": { "type": "EASE_IN_AND_OUT" },
                    "duration": 0.4
                }
            }
        }"#,
    )
    .expect("InteractionJson must deserialize");

    assert!(
        interaction.actions.is_empty(),
        "Unsupported/null actions must filter directly out of Vec<ActionJson>"
    );
    assert_eq!(interaction.iter_actions().count(), 1);

    let reactions = interaction.into_reactions();
    assert_eq!(reactions.len(), 1);
    assert_eq!(
        reactions[0].trigger.as_ref().unwrap().trigger_type.as_ref().unwrap(),
        &Trigger_type::Click(().into())
    );
    match reactions[0].action.as_ref().unwrap().action_type.as_ref().unwrap() {
        Action_type::Node(node_action) => {
            assert_eq!(node_action.destination_id.as_deref(), Some("42:1"));
            assert_eq!(
                node_action.navigation.enum_value_or_default(),
                Navigation::NAVIGATION_OVERLAY
            );
            assert!(node_action.preserve_scroll_position);
            match node_action.transition.as_ref().unwrap().transition_type.as_ref().unwrap() {
                Transition_type::Push(push) => {
                    assert_eq!(
                        push.direction.enum_value_or_default(),
                        TransitionDirection::TRANSITION_DIRECTION_LEFT
                    );
                    assert!(push.match_layers);
                    assert!((push.duration - 0.4).abs() < 1e-6);
                    match push.easing.as_ref().unwrap().easing_type.as_ref().unwrap() {
                        Easing_type::Bezier(b) => {
                            assert!((b.x1 - 0.37).abs() < 1e-6);
                            assert!((b.y1 - 0.0).abs() < 1e-6);
                            assert!((b.x2 - 0.63).abs() < 1e-6);
                            assert!((b.y2 - 1.0).abs() < 1e-6);
                        }
                        other => panic!("Expected Bezier easing, got {:?}", other),
                    }
                }
                other => panic!("Expected Push transition, got {:?}", other),
            }
        }
        other => panic!("Expected Action_type::Node, got {:?}", other),
    }
}

#[test]
fn transition_smart_animate_null_and_unknown_easing_default_to_linear() {
    let json = r#"[
        {
            "trigger": { "type": "ON_CLICK" },
            "actions": [
                {
                    "type": "NODE",
                    "destinationId": "1:2",
                    "navigation": "CHANGE_TO",
                    "transition": {
                        "type": "SMART_ANIMATE",
                        "easing": null,
                        "duration": 0.3
                    }
                },
                {
                    "type": "NODE",
                    "destinationId": "1:3",
                    "navigation": "CHANGE_TO",
                    "transition": {
                        "type": "SMART_ANIMATE",
                        "easing": { "type": "FUTURE_EASING", "unknownField": 42 },
                        "duration": 0.45
                    }
                }
            ]
        }
    ]"#;

    let interactions: Vec<InteractionJson> =
        serde_json::from_str(json).expect("Interactions with null/unknown easing must deserialize");
    assert_eq!(interactions.len(), 1);
    assert_eq!(interactions[0].actions.len(), 2);

    for (idx, (expected_dest, expected_duration)) in
        [("1:2", 0.3_f32), ("1:3", 0.45_f32)].iter().enumerate()
    {
        match &interactions[0].actions[idx] {
            ActionJson::Node { destination_id, navigation, transition, .. } => {
                assert_eq!(destination_id.as_deref(), Some(*expected_dest));
                assert_eq!(*navigation, NavigationJson::ChangeTo);
                assert_eq!(
                    transition.as_ref(),
                    Some(&TransitionJson::SmartAnimate {
                        easing: EasingJson::Linear,
                        duration: *expected_duration,
                    }),
                    "SmartAnimate with null or unrecognized easing must preserve transition with Linear easing"
                );
            }
            other => panic!("Expected ActionJson::Node, got {:?}", other),
        }
    }

    let reactions: Vec<Reaction> =
        interactions.into_iter().flat_map(|i| i.into_reactions()).collect();
    assert_eq!(reactions.len(), 2);
    for (idx, expected_duration) in [0.3_f32, 0.45_f32].iter().enumerate() {
        let Action_type::Node(node_action) =
            reactions[idx].action.as_ref().unwrap().action_type.as_ref().unwrap()
        else {
            panic!("Expected Action_type::Node");
        };
        let Some(Transition_type::SmartAnimate(sa)) =
            node_action.transition.as_ref().and_then(|t| t.transition_type.as_ref())
        else {
            panic!("Expected preserved SmartAnimate transition on reaction {}", idx);
        };
        assert!((sa.duration - expected_duration).abs() < 1e-6);
        match sa.easing.as_ref().and_then(|e| e.easing_type.as_ref()) {
            Some(Easing_type::Bezier(b)) => {
                assert_eq!((b.x1, b.y1, b.x2, b.y2), (0.0, 0.0, 1.0, 1.0));
            }
            other => panic!("Expected Linear Bezier(0,0,1,1), got {:?}", other),
        }
    }
}

#[test]
fn parse_custom_spring_transition_and_url_action() {
    let custom_spring_json = r#"{
        "type": "SMART_ANIMATE",
        "easing": {
            "type": "CUSTOM_SPRING",
            "easingFunctionSpring": {
                "mass": 1.5,
                "stiffness": 240.0,
                "damping": 18.0
            }
        },
        "duration": 0.35
    }"#;
    let parsed_transition: TransitionJson =
        serde_json::from_str(custom_spring_json).expect("CustomSpring transition must parse");
    assert_eq!(
        parsed_transition,
        TransitionJson::SmartAnimate {
            easing: EasingJson::CustomSpring {
                spring: SpringJson { mass: 1.5, stiffness: 240.0, damping: 18.0 },
            },
            duration: 0.35,
        }
    );

    let url_action_json = r#"{"type": "URL", "url": "https://developer.android.com/design"}"#;
    let parsed_url: ActionJson =
        serde_json::from_str(url_action_json).expect("ActionJson::Url must parse");
    assert_eq!(
        parsed_url,
        ActionJson::Url { url: "https://developer.android.com/design".to_string() }
    );

    let interactions_json = r#"[
        {
            "trigger": { "type": "ON_CLICK" },
            "actions": [
                { "type": "URL", "url": "https://developer.android.com/design" }
            ]
        },
        {
            "trigger": { "type": "ON_HOVER" },
            "actions": [
                {
                    "type": "NODE",
                    "destinationId": "5:1",
                    "navigation": "CHANGE_TO",
                    "transition": {
                        "type": "SMART_ANIMATE",
                        "easing": {
                            "type": "CUSTOM_SPRING",
                            "easingFunctionSpring": {
                                "mass": 1.5,
                                "stiffness": 240.0,
                                "damping": 18.0
                            }
                        },
                        "duration": 0.35
                    }
                }
            ]
        }
    ]"#;
    let interactions: Vec<InteractionJson> =
        serde_json::from_str(interactions_json).expect("REST interactions must deserialize");
    let reactions: Vec<Reaction> =
        interactions.into_iter().flat_map(|i| i.into_reactions()).collect();
    assert_eq!(reactions.len(), 2);

    assert_eq!(
        reactions[0].action.as_ref().unwrap().action_type.as_ref().unwrap(),
        &Action_type::Url(ActionUrl {
            url: "https://developer.android.com/design".to_string(),
            ..Default::default()
        })
    );

    let Action_type::Node(node_action) =
        reactions[1].action.as_ref().unwrap().action_type.as_ref().unwrap()
    else {
        panic!("Expected Action_type::Node");
    };
    let Some(Transition_type::SmartAnimate(sa)) =
        node_action.transition.as_ref().and_then(|t| t.transition_type.as_ref())
    else {
        panic!("Expected SmartAnimate transition");
    };
    assert!((sa.duration - 0.35).abs() < 1e-6);
    match sa.easing.as_ref().and_then(|e| e.easing_type.as_ref()) {
        Some(Easing_type::Spring(s)) => {
            assert!((s.mass - 1.5).abs() < 1e-6);
            assert!((s.stiffness - 240.0).abs() < 1e-6);
            assert!((s.damping - 18.0).abs() < 1e-6);
        }
        other => panic!("Expected Spring easing, got {:?}", other),
    }
}

#[test]
fn malformed_non_null_scalars_fail_deserialization_instead_of_coercing() {
    // Malformed non-null scalar values must fail strict scalar deserialization rather than
    // silently coercing to 0.0 / false.
    assert!(serde_json::from_str::<TransitionJson>(
        r#"{"type":"SMART_ANIMATE","easing":{"type":"LINEAR"},"duration":"abc"}"#,
    )
    .is_err());
    assert!(
        serde_json::from_str::<TransitionJson>(
            r#"{"type":"MOVE_IN","easing":{"type":"LINEAR"},"duration":0.2,"direction":"LEFT","matchLayers":"yes"}"#,
        )
        .is_err()
    );
    assert!(
        serde_json::from_str::<TriggerJson>(r#"{"type":"AFTER_TIMEOUT","timeout":"abc"}"#).is_err()
    );
    assert!(serde_json::from_str::<TriggerJson>(r#"{"type":"MOUSE_ENTER","delay":"abc"}"#).is_err());
    assert!(
        serde_json::from_str::<ActionJson>(
            r#"{"type":"NODE","destinationId":"1:2","navigation":"NAVIGATE","preserveScrollPosition":"yes"}"#,
        )
        .is_err()
    );

    // Null scalar fields still default cleanly.
    assert_eq!(
        serde_json::from_str::<TransitionJson>(
            r#"{"type":"SMART_ANIMATE","easing":{"type":"LINEAR"},"duration":null}"#,
        )
        .unwrap(),
        TransitionJson::SmartAnimate { easing: EasingJson::Linear, duration: 0.0 }
    );

    // Inside InteractionJson, malformed TransitionJson.duration drops the invalid transition
    // rather than coercing it to SmartAnimate { duration: 0.0 }, and malformed actions are dropped.
    let interaction: InteractionJson = serde_json::from_str(
        r#"{
            "trigger": { "type": "ON_CLICK" },
            "actions": [
                {
                    "type": "NODE",
                    "destinationId": "1:2",
                    "navigation": "NAVIGATE",
                    "transition": {
                        "type": "SMART_ANIMATE",
                        "easing": { "type": "LINEAR" },
                        "duration": "abc"
                    }
                },
                {
                    "type": "NODE",
                    "destinationId": "1:3",
                    "navigation": "NAVIGATE",
                    "preserveScrollPosition": "yes"
                }
            ]
        }"#,
    )
    .expect("Outer InteractionJson stays resilient via lossy container helpers");
    assert_eq!(interaction.actions.len(), 1);
    match &interaction.actions[0] {
        ActionJson::Node { destination_id, transition, .. } => {
            assert_eq!(destination_id.as_deref(), Some("1:2"));
            assert!(
                transition.is_none(),
                "Malformed transition duration must not coerce to duration: 0.0"
            );
        }
        other => panic!("Expected ActionJson::Node, got {:?}", other),
    }
}
