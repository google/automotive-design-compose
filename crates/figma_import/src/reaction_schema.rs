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

use crate::figma_schema;

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

/// The kind of navigation that an Action can perform
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq, Eq, Hash)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum Navigation {
    /// Navigate the top level frame to show the destination node.
    Navigate,

    /// Swap the currently open overlay to show the destination node, or
    /// change the current top-level frame without creating an entry in
    /// the navigation stack if there is no overlay.
    Swap,

    /// Open the destination node as an overlay.
    Overlay,

    /// Scroll to reveal the destination node.
    ScrollTo,

    /// Change the component to show this variant.
    ChangeTo,
}

/// Bezier curve for custom easing functions.
#[derive(Deserialize, Serialize, Debug, Clone, Copy, PartialEq)]
pub struct Bezier {
    pub x1: f32,
    pub y1: f32,
    pub x2: f32,
    pub y2: f32,
}
/// The type of easing to perform in a transition.
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[serde(tag = "type", rename_all = "SCREAMING_SNAKE_CASE")]
pub enum EasingJson {
    EaseIn,
    EaseOut,
    EaseInAndOut,
    Linear,
    EaseInBack,
    EaseOutBack,
    EaseInAndOutBack,
    CustomCubicBezier {
        #[serde(rename = "easingFunctionCubicBezier")]
        bezier: Bezier,
    },
}

/// Some transitions define a direction.
#[derive(Deserialize, Serialize, Debug, Clone, Copy, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum TransitionDirection {
    Left,
    Right,
    Top,
    Bottom,
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
        overlay_relative_position: Option<figma_schema::Vector>,
    },
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

/// Reaction describes interactivity for a node. It's a pair of Action ("what happens?") and
/// Trigger ("how do you make it happen?")
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct ReactionJson {
    pub action: ActionJson,
    pub trigger: TriggerJson,
}

// Unfortunately we can't serialize `ReactionJson` using bincode and deserialize it again
// because bincode doesn't support taggged enums. So we define these alternative structs
// and enums which don't use tagging and convert to them from the JSON versions.
//
// There are quite a few tickets against serde to better support this kind of thing, such as:
// https://github.com/serde-rs/serde/issues/1310
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub enum Trigger {
    OnClick,
    /// OnHover reverts Navigate and ChangeTo actions when hovering ends.
    OnHover,
    /// OnPress reverts Navigate and ChangeTo actions when hovering ends.
    OnPress,
    OnDrag,
    // Not documented
    OnKeyDown {
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
impl From<TriggerJson> for Trigger {
    fn from(json: TriggerJson) -> Self {
        match json {
            TriggerJson::OnClick => Self::OnClick,
            TriggerJson::OnHover => Self::OnHover,
            TriggerJson::OnPress => Self::OnPress,
            TriggerJson::OnDrag => Self::OnDrag,
            TriggerJson::OnKeyDown { key_codes } => Self::OnKeyDown { key_codes },
            TriggerJson::AfterTimeout { timeout } => Self::AfterTimeout { timeout },
            TriggerJson::MouseEnter { delay } => Self::MouseEnter { delay },
            TriggerJson::MouseLeave { delay } => Self::MouseLeave { delay },
            TriggerJson::MouseUp { delay } => Self::MouseUp { delay },
            TriggerJson::MouseDown { delay } => Self::MouseDown { delay },
        }
    }
}

// We flatten the Easing type to a bezier for the toolkit. These values were taken from
// https://easings.net/ and verified against Figma optically.
impl From<EasingJson> for Bezier {
    fn from(json: EasingJson) -> Self {
        match json {
            EasingJson::EaseIn => Self { x1: 0.12, y1: 0.0, x2: 0.39, y2: 0.0 },
            EasingJson::EaseOut => Self { x1: 0.61, y1: 1.0, x2: 0.88, y2: 1.0 },
            EasingJson::EaseInAndOut => Self { x1: 0.37, y1: 0.0, x2: 0.63, y2: 1.0 },
            EasingJson::Linear => Self { x1: 0.0, y1: 0.0, x2: 1.0, y2: 1.0 },
            EasingJson::EaseInBack => Self { x1: 0.36, y1: 0.0, x2: 0.66, y2: -0.56 },
            EasingJson::EaseOutBack => Self { x1: 0.34, y1: 1.56, x2: 0.64, y2: 1.0 },
            EasingJson::EaseInAndOutBack => Self { x1: 0.68, y1: -0.6, x2: 0.32, y2: 1.6 },
            EasingJson::CustomCubicBezier { bezier } => bezier,
        }
    }
}

/// This represents the Figma "Transition" type.
/// https://www.figma.com/plugin-docs/api/Transition/
#[derive(Deserialize, Serialize, Debug, Clone, Copy, PartialEq)]
pub enum Transition {
    Dissolve { easing: Bezier, duration: f32 },
    SmartAnimate { easing: Bezier, duration: f32 },
    ScrollAnimate { easing: Bezier, duration: f32 },
    MoveIn { easing: Bezier, duration: f32, direction: TransitionDirection, match_layers: bool },
    MoveOut { easing: Bezier, duration: f32, direction: TransitionDirection, match_layers: bool },
    Push { easing: Bezier, duration: f32, direction: TransitionDirection, match_layers: bool },
    SlideIn { easing: Bezier, duration: f32, direction: TransitionDirection, match_layers: bool },
    SlideOut { easing: Bezier, duration: f32, direction: TransitionDirection, match_layers: bool },
}
impl Transition {
    /// Return the duration of the transiton (in seconds) (XXX: Make a Duration?)
    pub fn duration(&self) -> f32 {
        match self {
            Transition::Dissolve { duration, .. } => *duration,
            Transition::SmartAnimate { duration, .. } => *duration,
            Transition::ScrollAnimate { duration, .. } => *duration,
            Transition::MoveIn { duration, .. } => *duration,
            Transition::MoveOut { duration, .. } => *duration,
            Transition::Push { duration, .. } => *duration,
            Transition::SlideIn { duration, .. } => *duration,
            Transition::SlideOut { duration, .. } => *duration,
        }
    }
    /// Return the easing curve.
    pub fn easing(&self) -> Bezier {
        match self {
            Transition::Dissolve { easing, .. } => *easing,
            Transition::SmartAnimate { easing, .. } => *easing,
            Transition::ScrollAnimate { easing, .. } => *easing,
            Transition::MoveIn { easing, .. } => *easing,
            Transition::MoveOut { easing, .. } => *easing,
            Transition::Push { easing, .. } => *easing,
            Transition::SlideIn { easing, .. } => *easing,
            Transition::SlideOut { easing, .. } => *easing,
        }
    }
}
impl From<TransitionJson> for Transition {
    fn from(json: TransitionJson) -> Self {
        match json {
            TransitionJson::Dissolve { easing, duration } => {
                Self::Dissolve { easing: easing.into(), duration }
            }
            TransitionJson::SmartAnimate { easing, duration } => {
                Self::SmartAnimate { easing: easing.into(), duration }
            }
            TransitionJson::ScrollAnimate { easing, duration } => {
                Self::ScrollAnimate { easing: easing.into(), duration }
            }
            TransitionJson::MoveIn { easing, duration, direction, match_layers } => {
                Self::MoveIn { easing: easing.into(), duration, direction, match_layers }
            }
            TransitionJson::MoveOut { easing, duration, direction, match_layers } => {
                Self::MoveOut { easing: easing.into(), duration, direction, match_layers }
            }
            TransitionJson::Push { easing, duration, direction, match_layers } => {
                Self::Push { easing: easing.into(), duration, direction, match_layers }
            }
            TransitionJson::SlideIn { easing, duration, direction, match_layers } => {
                Self::SlideIn { easing: easing.into(), duration, direction, match_layers }
            }
            TransitionJson::SlideOut { easing, duration, direction, match_layers } => {
                Self::SlideOut { easing: easing.into(), duration, direction, match_layers }
            }
        }
    }
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub enum Action {
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
        destination_id: Option<String>,

        /// The kind of navigation (really the kind of action) to perform with the destination
        /// node (if it's not null).
        navigation: Navigation,

        /// The transition to perform for this animation, if any.
        transition: Option<Transition>,

        /// For "Navigate", should we open the new node with the current frame's scroll position?
        preserve_scroll_position: bool,

        /// For overlays that have been manually positioned.
        overlay_relative_position: Option<figma_schema::Vector>,
    },
}
impl From<ActionJson> for Action {
    fn from(json: ActionJson) -> Self {
        match json {
            ActionJson::Back => Self::Back,
            ActionJson::Close => Self::Close,
            ActionJson::Url { url } => Self::Url { url },
            ActionJson::Node {
                destination_id,
                navigation,
                transition,
                preserve_scroll_position,
                overlay_relative_position,
            } => Self::Node {
                destination_id,
                navigation,
                transition: transition.map(|t| t.into()),
                preserve_scroll_position,
                overlay_relative_position,
            },
        }
    }
}

/// Reaction describes interactivity for a node. It's a pair of Action ("what happens?") and
/// Trigger ("how do you make it happen?")
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct Reaction {
    pub action: Action,
    pub trigger: Trigger,
}
impl From<ReactionJson> for Reaction {
    fn from(json: ReactionJson) -> Self {
        Reaction { action: json.action.into(), trigger: json.trigger.into() }
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

/// Some frame properties are only available through the plugin API and are needed to
/// implement Reactions properly. They're included in this FrameExtras struct.
#[derive(PartialEq, Debug, Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct FrameExtras {
    pub number_of_fixed_children: usize,
    pub overlay_position_type: OverlayPositionType,
    pub overlay_background: OverlayBackground,
    pub overlay_background_interaction: OverlayBackgroundInteraction,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq, Copy)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum OverlayPositionType {
    Center,
    TopLeft,
    TopCenter,
    TopRight,
    BottomLeft,
    BottomCenter,
    BottomRight,
    Manual, // then we look at the Action
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq, Copy)]
pub struct OverlayBackground {
    pub color: Option<figma_schema::FigmaColor>,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq, Copy)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum OverlayBackgroundInteraction {
    None,
    CloseOnClickOutside,
}

#[test]
fn parse_frame_extras() {
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
        OverlayBackground {
            color: Some(figma_schema::FigmaColor { r: 0.0, g: 0.0, b: 0.0, a: 0.25 })
        }
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
