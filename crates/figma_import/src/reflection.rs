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

use dc_bundle::legacy_definition::element::geometry::Dimension;
use serde_reflection::{Samples, Tracer, TracerConfig};

pub fn registry() -> serde_reflection::Result<serde_reflection::Registry> {
    let mut tracer = Tracer::new(TracerConfig::default());
    let samples = Samples::new();

    tracer
        .trace_type::<dc_bundle::legacy_definition::layout::positioning::AlignContent>(&samples)
        .expect("couldn't trace AlignContent");
    tracer
        .trace_type::<dc_bundle::legacy_definition::layout::positioning::AlignItems>(&samples)
        .expect("couldn't trace AlignItems");
    tracer
        .trace_type::<dc_bundle::legacy_definition::layout::positioning::AlignSelf>(&samples)
        .expect("couldn't trace AlignSelf");
    tracer
        .trace_type::<crate::toolkit_style::ScaleMode>(&samples)
        .expect("couldn't trace ScaleMode");
    tracer
        .trace_type::<crate::toolkit_style::Background>(&samples)
        .expect("couldn't trace Background");
    tracer
        .trace_type::<dc_bundle::legacy_definition::modifier::blend::BlendMode>(&samples)
        .expect("couldn't trace BlendMode");
    tracer
        .trace_type::<crate::toolkit_style::BoxShadow>(&samples)
        .expect("couldn't trace BoxShadow");
    tracer.trace_type::<Dimension>(&samples).expect("couldn't trace Dimension");
    tracer
        .trace_type::<crate::toolkit_layout_style::Display>(&samples)
        .expect("couldn't trace Display");
    tracer
        .trace_type::<dc_bundle::legacy_definition::modifier::filter::FilterOp>(&samples)
        .expect("couldn't trace FilterOp");
    tracer
        .trace_type::<dc_bundle::legacy_definition::layout::positioning::FlexDirection>(&samples)
        .expect("couldn't trace FlexDirection");
    tracer
        .trace_type::<dc_bundle::definition::layout::FlexWrap>(&samples)
        .expect("couldn't trace FlexWrap");
    tracer
        .trace_type::<dc_bundle::legacy_definition::element::font::FontStyle>(&samples)
        .expect("couldn't trace FontStyle");
    tracer
        .trace_type::<dc_bundle::legacy_definition::element::font::TextDecoration>(&samples)
        .expect("couldn't trace TextDecoration");
    tracer
        .trace_type::<dc_bundle::legacy_definition::layout::positioning::JustifyContent>(&samples)
        .expect("couldn't trace JustifyContent");
    tracer
        .trace_type::<dc_bundle::legacy_definition::element::path::LineHeight>(&samples)
        .expect("couldn't trace LineHeight");
    tracer
        .trace_type::<crate::toolkit_layout_style::Number>(&samples)
        .expect("couldn't trace Number");
    tracer
        .trace_type::<crate::toolkit_layout_style::Overflow>(&samples)
        .expect("couldn't trace Overflow");
    tracer
        .trace_type::<crate::toolkit_style::PointerEvents>(&samples)
        .expect("couldn't trace PointerEvents");
    tracer
        .trace_type::<dc_bundle::legacy_definition::layout::grid::ItemSpacing>(&samples)
        .expect("couldn't trace ItemSpacing");
    tracer
        .trace_type::<crate::toolkit_style::GridLayoutType>(&samples)
        .expect("couldn't trace GridLayoutType");
    tracer
        .trace_type::<crate::toolkit_style::RotationMeterData>(&samples)
        .expect("couldn't trace RotationMeterData");
    tracer
        .trace_type::<crate::toolkit_style::ArcMeterData>(&samples)
        .expect("couldn't trace ArcMeterData");
    tracer
        .trace_type::<crate::toolkit_style::ProgressBarMeterData>(&samples)
        .expect("couldn't trace ProgressBarMeterData");
    tracer
        .trace_type::<crate::toolkit_style::MeterData>(&samples)
        .expect("couldn't trace MeterData");
    tracer
        .trace_type::<dc_bundle::legacy_definition::layout::positioning::PositionType>(&samples)
        .expect("couldn't trace PositionType");
    tracer
        .trace_type::<crate::toolkit_style::ShadowBox>(&samples)
        .expect("couldn't trace ShadowBox");
    tracer
        .trace_type::<dc_bundle::legacy_definition::element::path::StrokeAlign>(&samples)
        .expect("couldn't trace StrokeAlign");
    tracer
        .trace_type::<dc_bundle::legacy_definition::element::path::StrokeWeight>(&samples)
        .expect("couldn't trace StrokeWeight");
    tracer
        .trace_type::<dc_bundle::legacy_definition::modifier::text::TextAlign>(&samples)
        .expect("couldn't trace TextAlign");
    tracer
        .trace_type::<dc_bundle::legacy_definition::modifier::text::TextAlignVertical>(&samples)
        .expect("couldn't trace TextAlignVertical");
    tracer
        .trace_type::<dc_bundle::legacy_definition::modifier::text::TextOverflow>(&samples)
        .expect("couldn't trace TextOverflow");
    tracer
        .trace_type::<crate::toolkit_layout_style::LayoutSizing>(&samples)
        .expect("couldn't trace LayoutSizing");
    tracer
        .trace_type::<crate::toolkit_style::ViewStyle>(&samples)
        .expect("couldn't trace ViewStyle");

    tracer.trace_type::<crate::reaction_schema::Action>(&samples).expect("couldn't trace Action");
    tracer.trace_type::<crate::reaction_schema::Trigger>(&samples).expect("couldn't trace Trigger");
    tracer
        .trace_type::<crate::reaction_schema::Transition>(&samples)
        .expect("couldn't trace transition");
    tracer
        .trace_type::<crate::reaction_schema::Navigation>(&samples)
        .expect("couldn't trace Navigation");
    tracer
        .trace_type::<crate::reaction_schema::OverlayBackgroundInteraction>(&samples)
        .expect("couldn't trace OverlayBackgroundInteraction");
    tracer
        .trace_type::<crate::reaction_schema::OverlayPositionType>(&samples)
        .expect("couldn't trace OverlayPositionType");
    tracer
        .trace_type::<crate::reaction_schema::TransitionDirection>(&samples)
        .expect("couldn't trace TransitionDirection");
    tracer.trace_type::<crate::reaction_schema::Easing>(&samples).expect("couldn't trace Easing");

    tracer
        .trace_type::<crate::vector_schema::WindingRule>(&samples)
        .expect("couldn't trace WindingRule");
    tracer.trace_type::<crate::vector_schema::Path>(&samples).expect("couldn't trace Path");
    tracer
        .trace_type::<crate::vector_schema::RenderStyle>(&samples)
        .expect("couldn't trace RenderStyle");
    tracer
        .trace_type::<crate::vector_schema::RenderCommand>(&samples)
        .expect("couldn't trace RenderCommand");

    tracer
        .trace_type::<crate::toolkit_schema::ComponentContentOverride>(&samples)
        .expect("couldn't trace ComponentContentOverride");
    tracer
        .trace_type::<crate::toolkit_schema::ComponentInfo>(&samples)
        .expect("couldn't trace ComponentInfo");
    tracer
        .trace_type::<dc_bundle::legacy_definition::layout::grid::OverflowDirection>(&samples)
        .expect("couldn't trace OverflowDirection");
    tracer
        .trace_type::<crate::toolkit_schema::RenderMethod>(&samples)
        .expect("couldn't trace RenderMethod");
    tracer
        .trace_type::<dc_bundle::legacy_definition::element::view_shape::StrokeCap>(&samples)
        .expect("couldn't trace StrokeCap");
    tracer
        .trace_type::<dc_bundle::legacy_definition::element::variable::Mode>(&samples)
        .expect("couldn't trace Mode");
    tracer
        .trace_type::<dc_bundle::legacy_definition::element::variable::Collection>(&samples)
        .expect("couldn't trace Collection");
    tracer
        .trace_type::<dc_bundle::legacy_definition::element::variable::VariableType>(&samples)
        .expect("couldn't trace VariableType");
    tracer
        .trace_type::<crate::figma_schema::FigmaColor>(&samples)
        .expect("couldn't trace FigmaColor");
    tracer
        .trace_type::<dc_bundle::legacy_definition::element::variable::VariableAlias>(&samples)
        .expect("couldn't trace VariableAlias");
    tracer
        .trace_type::<dc_bundle::legacy_definition::element::variable::NumOrVar>(&samples)
        .expect("couldn't trace NumOrVar");
    tracer
        .trace_type::<dc_bundle::legacy_definition::element::variable::ColorOrVar>(&samples)
        .expect("couldn't trace ColorOrVar");
    tracer
        .trace_type::<dc_bundle::legacy_definition::element::variable::VariableValue>(&samples)
        .expect("couldn't trace VariableValue");
    tracer
        .trace_type::<dc_bundle::legacy_definition::element::variable::Variable>(&samples)
        .expect("couldn't trace Variable");
    tracer
        .trace_type::<dc_bundle::legacy_definition::element::variable::VariableMap>(&samples)
        .expect("couldn't trace VariableMap");
    tracer
        .trace_type::<crate::toolkit_schema::ViewShape>(&samples)
        .expect("couldn't trace ViewShape");
    tracer
        .trace_type::<crate::toolkit_schema::ViewData>(&samples)
        .expect("couldn't trace ViewData");
    tracer.trace_type::<crate::toolkit_schema::View>(&samples).expect("couldn't trace View");
    tracer.trace_type::<layout::types::Layout>(&samples).expect("couldn't trace Layout");
    tracer
        .trace_type::<layout::layout_manager::LayoutChangedResponse>(&samples)
        .expect("couldn't trace LayoutChangedResponse");
    tracer
        .trace_type::<layout::layout_node::LayoutNode>(&samples)
        .expect("couldn't trace LayoutNode");
    tracer
        .trace_type::<layout::layout_node::LayoutNodeList>(&samples)
        .expect("couldn't trace LayoutNodeList");

    tracer
        .trace_type::<crate::image_context::EncodedImageMap>(&samples)
        .expect("couldn't trace EncodedImageMap");
    tracer.trace_type::<crate::NodeQuery>(&samples).expect("couldn't trace NodeQuery");
    tracer
        .trace_type::<crate::DesignComposeDefinitionHeader>(&samples)
        .expect("couldn't trace SerializedDesignDocHeader");
    tracer
        .trace_type::<crate::DesignComposeDefinition>(&samples)
        .expect("couldn't trace SerializedDesignDoc");
    tracer.trace_type::<crate::ServerFigmaDoc>(&samples).expect("couldn't trace ServerFigmaDoc");
    tracer.trace_type::<crate::ConvertResponse>(&samples).expect("couldn't trace ConvertResponse");

    tracer.registry()
}
