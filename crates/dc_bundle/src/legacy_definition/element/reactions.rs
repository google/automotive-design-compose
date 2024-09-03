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

use crate::definition::element::FloatColor;
use crate::legacy_definition::element::vector::Vector;
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

/// Spring coefficients
#[derive(Deserialize, Serialize, Debug, Clone, Copy, PartialEq)]
pub struct Spring {
    pub mass: f32,
    pub stiffness: f32,
    pub damping: f32,
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

#[derive(Deserialize, Serialize, Debug, Copy, Clone, PartialEq)]
pub enum Easing {
    Bezier(Bezier),
    Spring(Spring),
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
        overlay_relative_position: Option<Vector>,
    },
}

/// Reaction describes interactivity for a node. It's a pair of Action ("what happens?") and
/// Trigger ("how do you make it happen?")
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct Reaction {
    pub action: Action,
    pub trigger: Trigger,
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

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct OverlayBackground {
    pub color: Option<FloatColor>,
}

#[derive(Deserialize, Serialize, Debug, Clone, PartialEq, Copy)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum OverlayBackgroundInteraction {
    None,
    CloseOnClickOutside,
}
