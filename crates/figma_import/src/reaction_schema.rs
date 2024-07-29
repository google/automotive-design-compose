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

use serde::{Deserialize, Serialize};

use dc_bundle::legacy_definition::element::reactions::{
    Action, Bezier, Easing, Navigation, Reaction, Spring, Transition, TransitionDirection, Trigger,
};
use dc_bundle::legacy_definition::element::vector::Vector;

#[cfg(test)]
use dc_bundle::legacy_definition::element::color::FloatColor;

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
impl Into<Easing> for EasingJson {
    fn into(self) -> Easing {
        match self {
            EasingJson::EaseIn => Easing::Bezier(Bezier { x1: 0.12, y1: 0.0, x2: 0.39, y2: 0.0 }),
            EasingJson::EaseOut => Easing::Bezier(Bezier { x1: 0.61, y1: 1.0, x2: 0.88, y2: 1.0 }),
            EasingJson::EaseInAndOut => {
                Easing::Bezier(Bezier { x1: 0.37, y1: 0.0, x2: 0.63, y2: 1.0 })
            }
            EasingJson::Linear => Easing::Bezier(Bezier { x1: 0.0, y1: 0.0, x2: 1.0, y2: 1.0 }),
            EasingJson::EaseInBack => {
                Easing::Bezier(Bezier { x1: 0.36, y1: 0.0, x2: 0.66, y2: -0.56 })
            }
            EasingJson::EaseOutBack => {
                Easing::Bezier(Bezier { x1: 0.34, y1: 1.56, x2: 0.64, y2: 1.0 })
            }
            EasingJson::EaseInAndOutBack => {
                Easing::Bezier(Bezier { x1: 0.68, y1: -0.6, x2: 0.32, y2: 1.6 })
            }
            EasingJson::CustomCubicBezier { bezier } => Easing::Bezier(bezier),
            EasingJson::Gentle => {
                Easing::Spring(Spring { mass: 1.0, damping: 15.0, stiffness: 100.0 })
            }
            EasingJson::Quick => {
                Easing::Spring(Spring { mass: 1.0, damping: 20.0, stiffness: 300.0 })
            }
            EasingJson::Bouncy => {
                Easing::Spring(Spring { mass: 1.0, damping: 15.0, stiffness: 600.0 })
            }
            EasingJson::Slow => {
                Easing::Spring(Spring { mass: 1.0, damping: 20.0, stiffness: 80.0 })
            }
            EasingJson::CustomSpring { spring } => Easing::Spring(spring),
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

impl Into<Transition> for TransitionJson {
    fn into(self) -> Transition {
        match self {
            TransitionJson::Dissolve { easing, duration } => {
                Transition::Dissolve { easing: easing.into(), duration }
            }
            TransitionJson::SmartAnimate { easing, duration } => {
                Transition::SmartAnimate { easing: easing.into(), duration }
            }
            TransitionJson::ScrollAnimate { easing, duration } => {
                Transition::ScrollAnimate { easing: easing.into(), duration }
            }
            TransitionJson::MoveIn { easing, duration, direction, match_layers } => {
                Transition::MoveIn { easing: easing.into(), duration, direction, match_layers }
            }
            TransitionJson::MoveOut { easing, duration, direction, match_layers } => {
                Transition::MoveOut { easing: easing.into(), duration, direction, match_layers }
            }
            TransitionJson::Push { easing, duration, direction, match_layers } => {
                Transition::Push { easing: easing.into(), duration, direction, match_layers }
            }
            TransitionJson::SlideIn { easing, duration, direction, match_layers } => {
                Transition::SlideIn { easing: easing.into(), duration, direction, match_layers }
            }
            TransitionJson::SlideOut { easing, duration, direction, match_layers } => {
                Transition::SlideOut { easing: easing.into(), duration, direction, match_layers }
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

impl Into<Action> for ActionJson {
    fn into(self) -> Action {
        match self {
            ActionJson::Back => Action::Back,
            ActionJson::Close => Action::Close,
            ActionJson::Url { url } => Action::Url { url },
            ActionJson::Node {
                destination_id,
                navigation,
                transition,
                preserve_scroll_position,
                overlay_relative_position,
            } => Action::Node {
                destination_id,
                navigation,
                transition: transition.map(|t| t.into()),
                preserve_scroll_position,
                overlay_relative_position,
            },
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

impl Into<Trigger> for TriggerJson {
    fn into(self) -> Trigger {
        match self {
            TriggerJson::OnClick => Trigger::OnClick,
            TriggerJson::OnHover => Trigger::OnHover,
            TriggerJson::OnPress => Trigger::OnPress,
            TriggerJson::OnDrag => Trigger::OnDrag,
            TriggerJson::OnKeyDown { key_codes } => Trigger::OnKeyDown { key_codes },
            TriggerJson::AfterTimeout { timeout } => Trigger::AfterTimeout { timeout },
            TriggerJson::MouseEnter { delay } => Trigger::MouseEnter { delay },
            TriggerJson::MouseLeave { delay } => Trigger::MouseLeave { delay },
            TriggerJson::MouseUp { delay } => Trigger::MouseUp { delay },
            TriggerJson::MouseDown { delay } => Trigger::MouseDown { delay },
        }
    }
}

/// Reaction describes interactivity for a node. It's a pair of Action ("what happens?") and
/// Trigger ("how do you make it happen?")
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct ReactionJson {
    pub action: ActionJson,
    pub trigger: TriggerJson,
}

impl Into<Reaction> for ReactionJson {
    fn into(self) -> Reaction {
        Reaction { action: self.action.into(), trigger: self.trigger.into() }
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

    let multiple: Vec<Reaction> = multiple_json.drain(..).map(|json| json.into()).collect();
    let scroll: Vec<Reaction> = scroll_json.drain(..).map(|json| json.into()).collect();
    let overlay: Vec<Reaction> = overlay_json.drain(..).map(|json| json.into()).collect();

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
    use dc_bundle::legacy_definition::element::reactions::{
        FrameExtras, OverlayBackground, OverlayBackgroundInteraction, OverlayPositionType,
    };
    use serde_json;

    let def = "{\"numberOfFixedChildren\":0,\"overlayPositionType\":\"CENTER\",\"overlayBackground\":{\"type\":\"NONE\"},\"overlayBackgroundInteraction\":\"NONE\",\"overflowDirection\":\"NONE\"}";
    let click_to_close = "{\"numberOfFixedChildren\":0,\"overlayPositionType\":\"BOTTOM_CENTER\",\"overlayBackground\":{\"type\":\"SOLID_COLOR\",\"color\":{\"r\":0,\"g\":0,\"b\":0,\"a\":0.25}},\"overlayBackgroundInteraction\":\"CLOSE_ON_CLICK_OUTSIDE\",\"overflowDirection\":\"HORIZONTAL_AND_VERTICAL_SCROLLING\"}";

    let def: FrameExtras = serde_json::from_str(def).unwrap();
    let click_to_close: FrameExtras = serde_json::from_str(click_to_close).unwrap();

    assert_eq!(def.number_of_fixed_children, 0);
    assert_eq!(def.overlay_position_type, OverlayPositionType::Center);
    assert_eq!(def.overlay_background, OverlayBackground { color: None });
    assert_eq!(def.overlay_background_interaction, OverlayBackgroundInteraction::None);

    assert_eq!(click_to_close.number_of_fixed_children, 0);
    assert_eq!(click_to_close.overlay_position_type, OverlayPositionType::BottomCenter);
    assert_eq!(
        click_to_close.overlay_background,
        OverlayBackground { color: Some(FloatColor { r: 0.0, g: 0.0, b: 0.0, a: 0.25 }) }
    );
    assert_eq!(
        click_to_close.overlay_background_interaction,
        OverlayBackgroundInteraction::CloseOnClickOutside
    );

    assert_eq!(def, bincode::deserialize(bincode::serialize(&def).unwrap().as_slice()).unwrap());
    assert_eq!(
        click_to_close,
        bincode::deserialize(bincode::serialize(&click_to_close).unwrap().as_slice()).unwrap()
    );
}
