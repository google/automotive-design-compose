/*
 * Copyright 2024 Google LLC
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

use serde::{Deserialize, Serialize};
use figma_import::figma_schema;

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

/// Some transitions define a direction.
#[derive(Deserialize, Serialize, Debug, Clone, Copy, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum TransitionDirection {
    Left,
    Right,
    Top,
    Bottom,
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

#[derive(Deserialize, Serialize, Debug, Copy, Clone, PartialEq)]
pub enum Easing {
    Bezier(Bezier),
    Spring(Spring),
}

// We flatten the Easing type to a bezier for the toolkit. These values were taken from
// https://easings.net/ and verified against Figma optically.
impl From<EasingJson> for Easing {
    fn from(json: EasingJson) -> Self {
        match json {
            EasingJson::EaseIn => Self::Bezier(Bezier { x1: 0.12, y1: 0.0, x2: 0.39, y2: 0.0 }),
            EasingJson::EaseOut => Self::Bezier(Bezier { x1: 0.61, y1: 1.0, x2: 0.88, y2: 1.0 }),
            EasingJson::EaseInAndOut => {
                Self::Bezier(Bezier { x1: 0.37, y1: 0.0, x2: 0.63, y2: 1.0 })
            }
            EasingJson::Linear => Self::Bezier(Bezier { x1: 0.0, y1: 0.0, x2: 1.0, y2: 1.0 }),
            EasingJson::EaseInBack => {
                Self::Bezier(Bezier { x1: 0.36, y1: 0.0, x2: 0.66, y2: -0.56 })
            }
            EasingJson::EaseOutBack => {
                Self::Bezier(Bezier { x1: 0.34, y1: 1.56, x2: 0.64, y2: 1.0 })
            }
            EasingJson::EaseInAndOutBack => {
                Self::Bezier(Bezier { x1: 0.68, y1: -0.6, x2: 0.32, y2: 1.6 })
            }
            EasingJson::CustomCubicBezier { bezier } => Self::Bezier(bezier),
            EasingJson::Gentle => {
                Self::Spring(Spring { mass: 1.0, damping: 15.0, stiffness: 100.0 })
            }
            EasingJson::Quick => {
                Self::Spring(Spring { mass: 1.0, damping: 20.0, stiffness: 300.0 })
            }
            EasingJson::Bouncy => {
                Self::Spring(Spring { mass: 1.0, damping: 15.0, stiffness: 600.0 })
            }
            EasingJson::Slow => Self::Spring(Spring { mass: 1.0, damping: 20.0, stiffness: 80.0 }),
            EasingJson::CustomSpring { spring } => Self::Spring(spring),
        }
    }
}

/// This represents the Figma "Transition" type.
/// https://www.figma.com/plugin-docs/api/Transition/
#[derive(Deserialize, Serialize, Debug, Clone, Copy, PartialEq)]
pub enum Transition {
    Dissolve { easing: Easing, duration: f32 },
    SmartAnimate { easing: Easing, duration: f32 },
    ScrollAnimate { easing: Easing, duration: f32 },
    MoveIn { easing: Easing, duration: f32, direction: TransitionDirection, match_layers: bool },
    MoveOut { easing: Easing, duration: f32, direction: TransitionDirection, match_layers: bool },
    Push { easing: Easing, duration: f32, direction: TransitionDirection, match_layers: bool },
    SlideIn { easing: Easing, duration: f32, direction: TransitionDirection, match_layers: bool },
    SlideOut { easing: Easing, duration: f32, direction: TransitionDirection, match_layers: bool },
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
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum OverlayBackgroundInteraction {
    None,
    CloseOnClickOutside,
}

/// Bezier curve for custom easing functions.
#[derive(Deserialize, Serialize, Debug, Clone, Copy, PartialEq)]
pub struct Bezier {
    pub x1: f32,
    pub y1: f32,
    pub x2: f32,
    pub y2: f32,
}

/// Spring coefficients
#[derive(Deserialize, Serialize, Debug, Clone, Copy, PartialEq)]
pub struct Spring {
    pub mass: f32,
    pub stiffness: f32,
    pub damping: f32,
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
pub struct OverlayBackground {
    pub color: Option<figma_schema::FigmaColor>,
}
