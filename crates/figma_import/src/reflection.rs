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

use dc_bundle::definition::element::dimension_proto::Dimension;

use dc_bundle::legacy_definition::EncodedImageMap;
use serde_reflection::{Samples, Tracer, TracerConfig};

pub fn registry() -> serde_reflection::Result<serde_reflection::Registry> {
    let mut tracer = Tracer::new(TracerConfig::default());
    let samples = Samples::new();

    tracer
        .trace_type::<dc_bundle::definition::layout::AlignContent>(&samples)
        .expect("couldn't trace AlignContent");
    tracer
        .trace_type::<dc_bundle::definition::layout::AlignItems>(&samples)
        .expect("couldn't trace AlignItems");
    tracer
        .trace_type::<dc_bundle::definition::layout::AlignSelf>(&samples)
        .expect("couldn't trace AlignSelf");
    tracer
        .trace_type::<dc_bundle::definition::element::background::ScaleMode>(&samples)
        .expect("couldn't trace ScaleMode");
    tracer
        .trace_type::<dc_bundle::definition::element::background::BackgroundType>(&samples)
        .expect("couldn't trace BackgroundType");
    tracer
        .trace_type::<dc_bundle::definition::element::Background>(&samples)
        .expect("couldn't trace Background");
    tracer
        .trace_type::<dc_bundle::definition::modifier::BlendMode>(&samples)
        .expect("couldn't trace BlendMode");
    tracer
        .trace_type::<dc_bundle::definition::modifier::BoxShadow>(&samples)
        .expect("couldn't trace BoxShadow");
    tracer.trace_type::<Dimension>(&samples).expect("couldn't trace Dimension");
    tracer
        .trace_type::<dc_bundle::definition::element::DimensionProto>(&samples)
        .expect("couldn't trace DimensionProto");
    tracer
        .trace_type::<dc_bundle::definition::element::DimensionRect>(&samples)
        .expect("couldn't trace DimensionRect");
    tracer
        .trace_type::<dc_bundle::definition::view::Display>(&samples)
        .expect("couldn't trace Display");
    tracer
        .trace_type::<dc_bundle::definition::modifier::FilterOp>(&samples)
        .expect("couldn't trace FilterOp");
    tracer
        .trace_type::<dc_bundle::definition::modifier::filter_op::FilterOpType>(&samples)
        .expect("couldn't trace FilterOpType");
    tracer
        .trace_type::<dc_bundle::definition::layout::FlexDirection>(&samples)
        .expect("couldn't trace FlexDirection");
    tracer
        .trace_type::<dc_bundle::definition::layout::FlexWrap>(&samples)
        .expect("couldn't trace FlexWrap");
    tracer
        .trace_type::<dc_bundle::definition::element::FontStyle>(&samples)
        .expect("couldn't trace FontStyle");
    tracer
        .trace_type::<dc_bundle::definition::element::TextDecoration>(&samples)
        .expect("couldn't trace TextDecoration");
    tracer
        .trace_type::<dc_bundle::definition::layout::JustifyContent>(&samples)
        .expect("couldn't trace JustifyContent");
    tracer
        .trace_type::<dc_bundle::definition::element::line_height::LineHeightType>(&samples)
        .expect("couldn't trace LineHeight");
    tracer
        .trace_type::<dc_bundle::definition::layout::Overflow>(&samples)
        .expect("couldn't trace Overflow");
    tracer
        .trace_type::<dc_bundle::definition::interaction::PointerEvents>(&samples)
        .expect("couldn't trace PointerEvents");
    tracer
        .trace_type::<dc_bundle::definition::layout::ItemSpacing>(&samples)
        .expect("couldn't trace ItemSpacing");
    tracer
        .trace_type::<dc_bundle::definition::layout::GridLayoutType>(&samples)
        .expect("couldn't trace GridLayoutType");
    tracer
        .trace_type::<dc_bundle::definition::plugin::RotationMeterData>(&samples)
        .expect("couldn't trace RotationMeterData");
    tracer
        .trace_type::<dc_bundle::definition::plugin::ArcMeterData>(&samples)
        .expect("couldn't trace ArcMeterData");
    tracer
        .trace_type::<dc_bundle::definition::plugin::ProgressBarMeterData>(&samples)
        .expect("couldn't trace ProgressBarMeterData");
    tracer
        .trace_type::<dc_bundle::definition::plugin::ProgressVectorMeterData>(&samples)
        .expect("couldn't trace ProgressVectorMeterData");
    tracer
        .trace_type::<dc_bundle::definition::plugin::meter_data::MeterDataType>(&samples)
        .expect("couldn't trace MeterData");
    tracer
        .trace_type::<dc_bundle::definition::layout::PositionType>(&samples)
        .expect("couldn't trace PositionType");
    tracer
        .trace_type::<dc_bundle::definition::modifier::box_shadow::ShadowBox>(&samples)
        .expect("couldn't trace ShadowBox");
    tracer
        .trace_type::<dc_bundle::definition::element::StrokeAlign>(&samples)
        .expect("couldn't trace StrokeAlign");
    tracer
        .trace_type::<dc_bundle::definition::element::StrokeWeight>(&samples)
        .expect("couldn't trace StrokeWeight");
    tracer
        .trace_type::<dc_bundle::definition::element::stroke_weight::StrokeWeightType>(&samples)
        .expect("couldn't trace StrokeWeightType");
    tracer
        .trace_type::<dc_bundle::definition::modifier::TextAlign>(&samples)
        .expect("couldn't trace TextAlign");
    tracer
        .trace_type::<dc_bundle::definition::modifier::TextAlignVertical>(&samples)
        .expect("couldn't trace TextAlignVertical");
    tracer
        .trace_type::<dc_bundle::definition::modifier::TextOverflow>(&samples)
        .expect("couldn't trace TextOverflow");
    tracer
        .trace_type::<dc_bundle::definition::layout::LayoutSizing>(&samples)
        .expect("couldn't trace LayoutSizing");
    tracer
        .trace_type::<dc_bundle::definition::view::ViewStyle>(&samples)
        .expect("couldn't trace ViewStyle");

    tracer
        .trace_type::<dc_bundle::definition::interaction::action::ActionType>(&samples)
        .expect("couldn't trace Action");
    tracer
        .trace_type::<dc_bundle::definition::interaction::trigger::TriggerType>(&samples)
        .expect("couldn't trace Trigger");
    tracer
        .trace_type::<dc_bundle::definition::interaction::transition::TransitionType>(&samples)
        .expect("couldn't trace transition");
    tracer
        .trace_type::<dc_bundle::definition::interaction::action::node::Navigation>(&samples)
        .expect("couldn't trace Navigation");
    tracer
        .trace_type::<dc_bundle::definition::plugin::OverlayBackgroundInteraction>(&samples)
        .expect("couldn't trace OverlayBackgroundInteraction");
    tracer
        .trace_type::<dc_bundle::definition::plugin::OverlayPositionType>(&samples)
        .expect("couldn't trace OverlayPositionType");
    tracer
        .trace_type::<dc_bundle::definition::interaction::transition::TransitionDirection>(&samples)
        .expect("couldn't trace TransitionDirection");
    tracer
        .trace_type::<dc_bundle::definition::interaction::easing::EasingType>(&samples)
        .expect("couldn't trace Easing");

    tracer
        .trace_type::<dc_bundle::definition::element::path::WindingRule>(&samples)
        .expect("couldn't trace WindingRule");
    tracer
        .trace_type::<dc_bundle::definition::element::Path>(&samples)
        .expect("couldn't trace Path");
    tracer
        .trace_type::<dc_bundle::definition::view::component_overrides::ComponentContentOverride>(
            &samples,
        )
        .expect("couldn't trace ComponentContentOverride");
    tracer
        .trace_type::<dc_bundle::definition::view::ComponentInfo>(&samples)
        .expect("couldn't trace ComponentInfo");
    tracer
        .trace_type::<dc_bundle::definition::layout::OverflowDirection>(&samples)
        .expect("couldn't trace OverflowDirection");
    tracer
        .trace_type::<dc_bundle::definition::view::view::RenderMethod>(&samples)
        .expect("couldn't trace RenderMethod");
    tracer
        .trace_type::<dc_bundle::definition::element::view_shape::StrokeCap>(&samples)
        .expect("couldn't trace StrokeCap");
    tracer
        .trace_type::<dc_bundle::definition::element::Mode>(&samples)
        .expect("couldn't trace Mode");
    tracer
        .trace_type::<dc_bundle::definition::element::Collection>(&samples)
        .expect("couldn't trace Collection");
    tracer
        .trace_type::<dc_bundle::definition::element::variable::VariableType>(&samples)
        .expect("couldn't trace VariableType");
    tracer
        .trace_type::<dc_bundle::definition::element::FloatColor>(&samples)
        .expect("couldn't trace FloatColor");
    tracer
        .trace_type::<dc_bundle::definition::element::num_or_var::NumOrVarType>(&samples)
        .expect("couldn't trace NumOrVar");
    tracer
        .trace_type::<dc_bundle::definition::element::ColorOrVar>(&samples)
        .expect("couldn't trace ColorOrVar");
    tracer
        .trace_type::<dc_bundle::definition::element::color_or_var::ColorOrVarType>(&samples)
        .expect("couldn't trace ColorOrVarType");
    tracer
        .trace_type::<dc_bundle::definition::element::VariableValue>(&samples)
        .expect("couldn't trace VariableValue");
    tracer
        .trace_type::<dc_bundle::definition::element::variable_value::Value>(&samples)
        .expect("couldn't trace variable_value::Value");
    tracer
        .trace_type::<dc_bundle::definition::element::Variable>(&samples)
        .expect("couldn't trace Variable");
    tracer
        .trace_type::<dc_bundle::definition::element::VariableMap>(&samples)
        .expect("couldn't trace VariableMap");
    tracer
        .trace_type::<dc_bundle::definition::element::ViewShape>(&samples)
        .expect("couldn't trace ViewShape");
    tracer
        .trace_type::<dc_bundle::definition::element::view_shape::Shape>(&samples)
        .expect("couldn't trace Shape");
    tracer
        .trace_type::<dc_bundle::definition::view::ViewData>(&samples)
        .expect("couldn't trace ViewData");
    tracer
        .trace_type::<dc_bundle::definition::view::view_data::ViewDataType>(&samples)
        .expect("couldn't trace ViewDataType");
    tracer
        .trace_type::<dc_bundle::definition::view::view_data::StyledTextRuns>(&samples)
        .expect("couldn't trace StyledTextRuns");
    tracer
        .trace_type::<dc_bundle::definition::view::view_data::Container>(&samples)
        .expect("couldn't trace Container");
    tracer
        .trace_type::<dc_bundle::definition::view::view_data::Text>(&samples)
        .expect("couldn't trace Text");
    tracer.trace_type::<dc_bundle::definition::view::View>(&samples).expect("couldn't trace View");
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
    tracer.trace_type::<EncodedImageMap>(&samples).expect("couldn't trace EncodedImageMap");
    tracer.trace_type::<crate::NodeQuery>(&samples).expect("couldn't trace NodeQuery");
    tracer
        .trace_type::<crate::DesignComposeDefinitionHeader>(&samples)
        .expect("couldn't trace SerializedDesignDocHeader");
    tracer
        .trace_type::<crate::DesignComposeDefinition>(&samples)
        .expect("couldn't trace SerializedDesignDoc");
    tracer.trace_type::<crate::ServerFigmaDoc>(&samples).expect("couldn't trace ServerFigmaDoc");
    tracer.trace_type::<crate::ConvertResponse>(&samples).expect("couldn't trace ConvertResponse");
    tracer
        .trace_type::<dc_bundle::definition::layout::item_spacing::ItemSpacingType>(&samples)
        .expect("couldn't trace ItemSpacingType");

    tracer.registry()
}
