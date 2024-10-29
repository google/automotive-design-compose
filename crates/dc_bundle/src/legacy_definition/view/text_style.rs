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

use crate::definition::element::num_or_var::NumOrVarType;

use crate::definition::element::line_height::LineHeight;
use crate::definition::element::ColorOrVar;
use crate::definition::element::{background, Background};
use crate::definition::element::{Color, FontStretch, FontWeight};
use crate::definition::element::{FontFeature, FontStyle, Hyperlink, TextDecoration};
use serde::{Deserialize, Serialize};

// These are the style properties that apply to text, so we can use them on subsections of
// a longer string. We then assume that every style transition is a potential line break (and
// also run the linebreaking algorithm on the content of every style for the normal case where
// we need to break text that's all in one style).
#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct TextStyle {
    pub text_color: Background, // also text shadow?
    pub font_size: NumOrVarType,
    pub font_family: Option<String>,
    pub font_weight: FontWeight,
    pub font_style: FontStyle,
    pub font_stretch: FontStretch,
    pub letter_spacing: f32,
    pub text_decoration: TextDecoration,
    pub line_height: LineHeight,
    pub font_features: Vec<FontFeature>,
    pub hyperlink: Option<Hyperlink>,
}

impl Default for TextStyle {
    fn default() -> Self {
        TextStyle {
            text_color: Background::new(background::BackgroundType::Solid(ColorOrVar::new_color(
                Color::BLACK,
            ))),
            font_size: NumOrVarType::Num(18.0),
            font_family: None,
            font_weight: FontWeight::NORMAL,
            font_style: FontStyle::Normal,
            font_stretch: FontStretch::NORMAL,
            letter_spacing: 0.0,
            text_decoration: TextDecoration::None,
            line_height: LineHeight::Percent(1.0),
            font_features: Vec::new(),
            hyperlink: None,
        }
    }
}

// Text can be either a string, or a list of styled runs.
#[derive(Clone, PartialEq, Debug, Deserialize, Serialize)]
pub struct StyledTextRun {
    pub text: String,
    pub style: TextStyle,
}

impl StyledTextRun {
    pub fn new(label: impl ToString) -> StyledTextRun {
        StyledTextRun { text: label.to_string(), style: Default::default() }
    }
    pub fn bold(self) -> Self {
        StyledTextRun {
            style: TextStyle { font_weight: FontWeight::BOLD, ..self.style },
            text: self.text,
        }
    }
    pub fn italic(self) -> Self {
        StyledTextRun {
            style: TextStyle { font_style: FontStyle::Italic, ..self.style },
            text: self.text,
        }
    }
    pub fn underline(self) -> Self {
        StyledTextRun {
            style: TextStyle { text_decoration: TextDecoration::Underline, ..self.style },
            text: self.text,
        }
    }
    pub fn strikethrough(self) -> Self {
        StyledTextRun {
            style: TextStyle { text_decoration: TextDecoration::Strikethrough, ..self.style },
            text: self.text,
        }
    }
    pub fn size(self, size: f32) -> Self {
        StyledTextRun {
            style: TextStyle { font_size: NumOrVarType::Num(size), ..self.style },
            text: self.text,
        }
    }
    pub fn fill(self, text_color: Background) -> Self {
        StyledTextRun { style: TextStyle { text_color, ..self.style }, text: self.text }
    }
    pub fn family(self, family_name: impl ToString) -> Self {
        StyledTextRun {
            style: TextStyle { font_family: Some(family_name.to_string()), ..self.style },
            text: self.text,
        }
    }
    pub fn feature(self, feature: FontFeature) -> Self {
        let mut font_features = self.style.font_features;
        font_features.push(feature);
        StyledTextRun { style: TextStyle { font_features, ..self.style }, text: self.text }
    }
    pub fn hyperlink(self, hyperlink: Hyperlink) -> Self {
        StyledTextRun {
            style: TextStyle { hyperlink: Some(hyperlink), ..self.style },
            text: self.text,
        }
    }
}
