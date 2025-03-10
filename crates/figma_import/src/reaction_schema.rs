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

use serde::{Deserialize, Serialize};

// This module can deserialize Figma's "reactions" struct, which is used to define the
// interactivity of interactive components. It's in a separate module from `figma_schema`
// because it's not yet part of Figma's REST API. We get access to it via a custom plugin
// which copies the reactions array into plugin storage in the node which we then fetch
// via the REST API.
//
// Once Figma returns reactions to the REST API we'll move these definitions into
// `figma_schema` under the `Node` type (because reactions can be added to most nodes).
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
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(tag = "type", rename_all = "SCREAMING_SNAKE_CASE")]
pub enum EasingJson {
    // Cubic beziers
    EaseIn,
    EaseOut,
    EaseInAndOut,
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

#[derive(Clone, Copy, PartialEq, Eq, Debug, Hash, Serialize, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum TransitionDirectionJson {
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
        easing: EasingJson,
        duration: f32,
    },
    SmartAnimate {
        easing: EasingJson,
        duration: f32,
    },
    ScrollAnimate {
        easing: EasingJson,
        duration: f32,
    },
    MoveIn {
        easing: EasingJson,
        duration: f32,
        direction: TransitionDirectionJson,
        #[serde(rename = "matchLayers")]
        match_layers: bool,
    },
    MoveOut {
        easing: EasingJson,
        duration: f32,
        direction: TransitionDirectionJson,
        #[serde(rename = "matchLayers")]
        match_layers: bool,
    },
    Push {
        easing: EasingJson,
        duration: f32,
        direction: TransitionDirectionJson,
        #[serde(rename = "matchLayers")]
        match_layers: bool,
    },
    SlideIn {
        easing: EasingJson,
        duration: f32,
        direction: TransitionDirectionJson,
        #[serde(rename = "matchLayers")]
        match_layers: bool,
    },
    SlideOut {
        easing: EasingJson,
        duration: f32,
        direction: TransitionDirectionJson,
        #[serde(rename = "matchLayers")]
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

#[derive(Serialize, Deserialize, PartialEq, Clone, Debug)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum NavigationJson {
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
        #[serde(rename = "destinationId")]
        destination_id: Option<String>,

        /// The kind of navigation (really the kind of action) to perform with the destination
        /// node (if it's not null).
        navigation: NavigationJson,

        /// The transition to perform for this animation, if any.
        transition: Option<TransitionJson>,

        /// For "Navigate", should we open the new node with the current frame's scroll position?
        #[serde(rename = "preserveScrollPosition", default)]
        preserve_scroll_position: bool,

        /// For overlays that have been manually positioned.
        #[serde(rename = "overlayRelativePosition", default)]
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
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
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
