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

use dc_bundle::definition::element::FloatColor;
use dc_bundle::definition::interaction::{Action, Reaction, Trigger};
use serde::{Deserialize, Serialize};

use dc_bundle::definition::element::Vector;

use dc_bundle::definition::interaction::action::node::Navigation;
use dc_bundle::definition::interaction::action::{ActionType, ActionUrl};
use dc_bundle::definition::interaction::easing::{Bezier, EasingType, Spring};
use dc_bundle::definition::interaction::transition::{
    Dissolve, MoveIn, MoveOut, Push, ScrollAnimate, SlideIn, SlideOut, SmartAnimate,
    TransitionDirection, TransitionType,
};
use dc_bundle::definition::interaction::trigger::{
    KeyDown, MouseDown, MouseEnter, MouseLeave, MouseUp, Timeout, TriggerType,
};
use dc_bundle::definition::interaction::{action, Easing, Transition};
use dc_bundle::definition::plugin::{
    FrameExtras, OverlayBackground, OverlayBackgroundInteraction, OverlayPositionType,
};
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
        bezier: Bezier,
    },

    // Springs
    Gentle,
    Quick,
    Bouncy,
    Slow,

    // Manually specified spring
    CustomSpring {
        #[serde(rename = "easingFunctionSpring")]
        spring: Spring,
    },
}

// We flatten the Easing type to a bezier for the toolkit. These values were taken from
// https://easings.net/ and verified against Figma optically.
impl Into<EasingType> for EasingJson {
    fn into(self) -> EasingType {
        match self {
            EasingJson::EaseIn => {
                EasingType::Bezier(Bezier { x1: 0.12, y1: 0.0, x2: 0.39, y2: 0.0 })
            }
            EasingJson::EaseOut => {
                EasingType::Bezier(Bezier { x1: 0.61, y1: 1.0, x2: 0.88, y2: 1.0 })
            }
            EasingJson::EaseInAndOut => {
                EasingType::Bezier(Bezier { x1: 0.37, y1: 0.0, x2: 0.63, y2: 1.0 })
            }
            EasingJson::Linear => EasingType::Bezier(Bezier { x1: 0.0, y1: 0.0, x2: 1.0, y2: 1.0 }),
            EasingJson::EaseInBack => {
                EasingType::Bezier(Bezier { x1: 0.36, y1: 0.0, x2: 0.66, y2: -0.56 })
            }
            EasingJson::EaseOutBack => {
                EasingType::Bezier(Bezier { x1: 0.34, y1: 1.56, x2: 0.64, y2: 1.0 })
            }
            EasingJson::EaseInAndOutBack => {
                EasingType::Bezier(Bezier { x1: 0.68, y1: -0.6, x2: 0.32, y2: 1.6 })
            }
            EasingJson::CustomCubicBezier { bezier } => EasingType::Bezier(bezier),
            EasingJson::Gentle => {
                EasingType::Spring(Spring { mass: 1.0, damping: 15.0, stiffness: 100.0 })
            }
            EasingJson::Quick => {
                EasingType::Spring(Spring { mass: 1.0, damping: 20.0, stiffness: 300.0 })
            }
            EasingJson::Bouncy => {
                EasingType::Spring(Spring { mass: 1.0, damping: 15.0, stiffness: 600.0 })
            }
            EasingJson::Slow => {
                EasingType::Spring(Spring { mass: 1.0, damping: 20.0, stiffness: 80.0 })
            }
            EasingJson::CustomSpring { spring } => EasingType::Spring(spring),
        }
    }
}
impl Into<Easing> for EasingJson {
    fn into(self) -> Easing {
        Easing { easing_type: Some(self.into()) }
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
        direction: TransitionDirection,
        #[serde(rename = "matchLayers")]
        match_layers: bool,
    },
    MoveOut {
        easing: EasingJson,
        duration: f32,
        direction: TransitionDirection,
        #[serde(rename = "matchLayers")]
        match_layers: bool,
    },
    Push {
        easing: EasingJson,
        duration: f32,
        direction: TransitionDirection,
        #[serde(rename = "matchLayers")]
        match_layers: bool,
    },
    SlideIn {
        easing: EasingJson,
        duration: f32,
        direction: TransitionDirection,
        #[serde(rename = "matchLayers")]
        match_layers: bool,
    },
    SlideOut {
        easing: EasingJson,
        duration: f32,
        direction: TransitionDirection,
        #[serde(rename = "matchLayers")]
        match_layers: bool,
    },
}

impl Into<TransitionType> for TransitionJson {
    fn into(self) -> TransitionType {
        match self {
            TransitionJson::Dissolve { easing, duration } => {
                TransitionType::Dissolve(Dissolve { easing: Some(easing.into()), duration })
            }
            TransitionJson::SmartAnimate { easing, duration } => {
                TransitionType::SmartAnimate(SmartAnimate { easing: Some(easing.into()), duration })
            }
            TransitionJson::ScrollAnimate { easing, duration } => {
                TransitionType::ScrollAnimate(ScrollAnimate {
                    easing: Some(easing.into()),
                    duration,
                })
            }
            TransitionJson::MoveIn { easing, duration, direction, match_layers } => {
                TransitionType::MoveIn(MoveIn {
                    easing: Some(easing.into()),
                    duration,
                    direction: direction as i32,
                    match_layers,
                })
            }
            TransitionJson::MoveOut { easing, duration, direction, match_layers } => {
                TransitionType::MoveOut(MoveOut {
                    easing: Some(easing.into()),
                    duration,
                    direction: direction as i32,
                    match_layers,
                })
            }
            TransitionJson::Push { easing, duration, direction, match_layers } => {
                TransitionType::Push(Push {
                    easing: Some(easing.into()),
                    duration,
                    direction: direction as i32,
                    match_layers,
                })
            }
            TransitionJson::SlideIn { easing, duration, direction, match_layers } => {
                TransitionType::SlideIn(SlideIn {
                    easing: Some(easing.into()),
                    duration,
                    direction: direction as i32,
                    match_layers,
                })
            }
            TransitionJson::SlideOut { easing, duration, direction, match_layers } => {
                TransitionType::SlideOut(SlideOut {
                    easing: Some(easing.into()),
                    duration,
                    direction: direction as i32,
                    match_layers,
                })
            }
        }
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
        navigation: Navigation,

        /// The transition to perform for this animation, if any.
        transition: Option<TransitionJson>,

        /// For "Navigate", should we open the new node with the current frame's scroll position?
        #[serde(rename = "preserveScrollPosition", default)]
        preserve_scroll_position: bool,

        /// For overlays that have been manually positioned.
        #[serde(rename = "overlayRelativePosition", default)]
        overlay_relative_position: Option<Vector>,
    },
}

impl Into<ActionType> for ActionJson {
    fn into(self) -> ActionType {
        match self {
            ActionJson::Back => ActionType::Back(()),
            ActionJson::Close => ActionType::Close(()),
            ActionJson::Url { url } => ActionType::Url(ActionUrl { url }),
            ActionJson::Node {
                destination_id,
                navigation,
                transition,
                preserve_scroll_position,
                overlay_relative_position,
            } => ActionType::Node(action::Node {
                destination_id,
                navigation: navigation as i32,
                transition: transition.map(|t| Transition { transition_type: Some(t.into()) }),
                preserve_scroll_position,
                overlay_relative_position,
            }),
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

impl Into<TriggerType> for TriggerJson {
    fn into(self) -> TriggerType {
        match self {
            TriggerJson::OnClick => TriggerType::Click(()),
            TriggerJson::OnHover => TriggerType::Hover(()),
            TriggerJson::OnPress => TriggerType::Press(()),
            TriggerJson::OnDrag => TriggerType::Drag(()),
            TriggerJson::OnKeyDown { key_codes } => TriggerType::KeyDown(KeyDown { key_codes }),
            TriggerJson::AfterTimeout { timeout } => TriggerType::AfterTimeout(Timeout { timeout }),
            TriggerJson::MouseEnter { delay } => TriggerType::MouseEnter(MouseEnter { delay }),
            TriggerJson::MouseLeave { delay } => TriggerType::MouseLeave(MouseLeave { delay }),
            TriggerJson::MouseUp { delay } => TriggerType::MouseUp(MouseUp { delay }),
            TriggerJson::MouseDown { delay } => TriggerType::MouseDown(MouseDown { delay }),
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
                action: Some(Action { action_type: Some(action.into()) }),
                trigger: Some(Trigger { trigger_type: Some(self.trigger.into()) }),
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
            OverlayPositionJson::Center => OverlayPositionType::Center,
            OverlayPositionJson::TopLeft => OverlayPositionType::TopLeft,
            OverlayPositionJson::TopCenter => OverlayPositionType::TopCenter,
            OverlayPositionJson::TopRight => OverlayPositionType::TopRight,
            OverlayPositionJson::BottomLeft => OverlayPositionType::BottomLeft,
            OverlayPositionJson::BottomCenter => OverlayPositionType::BottomCenter,
            OverlayPositionJson::BottomRight => OverlayPositionType::BottomRight,
            OverlayPositionJson::Manual => OverlayPositionType::Manual,
        }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct OverlayBackgroundJson {
    pub color: Option<FloatColor>,
}

impl Into<Option<OverlayBackground>> for OverlayBackgroundJson {
    fn into(self) -> Option<OverlayBackground> {
        self.color.map(|c| OverlayBackground { color: Some(c) })
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
            OverlayBackgroundInteractionJson::None => OverlayBackgroundInteraction::None,
            OverlayBackgroundInteractionJson::CloseOnClickOutside => {
                OverlayBackgroundInteraction::CloseOnClickOutside
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
        FrameExtras {
            fixed_children: self.number_of_fixed_children as u32,
            overlay_position_type: OverlayPositionType::from(self.overlay_position_type.into())
                .into(), //It's confusing but it works? Need to convert one
            overlay_background: self.overlay_background.into(),
            overlay_background_interaction: OverlayBackgroundInteraction::from(
                self.overlay_background_interaction.into(),
            )
            .into(),
        }
    }
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

    let bincoded_multiple: Vec<Reaction> =
        bincode::deserialize(bincode::serialize(&multiple).unwrap().as_slice()).unwrap();
    let bincoded_scroll: Vec<Reaction> =
        bincode::deserialize(bincode::serialize(&scroll).unwrap().as_slice()).unwrap();
    let bincoded_overlay: Vec<Reaction> =
        bincode::deserialize(bincode::serialize(&overlay).unwrap().as_slice()).unwrap();

    // Check that bincode didn't encounter any problems with the serialization & deserialization.
    assert_eq!(multiple, bincoded_multiple);
    assert_eq!(scroll, bincoded_scroll);
    assert_eq!(overlay, bincoded_overlay);
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
        OverlayBackgroundJson { color: Some(FloatColor { r: 0.0, g: 0.0, b: 0.0, a: 0.25 }) }
    );
    assert_eq!(
        click_to_close.overlay_background_interaction,
        OverlayBackgroundInteractionJson::CloseOnClickOutside
    );

    assert_eq!(def, bincode::deserialize(bincode::serialize(&def).unwrap().as_slice()).unwrap());
    assert_eq!(
        click_to_close,
        bincode::deserialize(bincode::serialize(&click_to_close).unwrap().as_slice()).unwrap()
    );
}
