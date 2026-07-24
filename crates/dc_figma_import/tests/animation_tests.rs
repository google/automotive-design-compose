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

use dc_bundle::animationspec::animation_override;
use dc_bundle::definition_file::load_design_def;

#[test]
fn test_keyframes_present() {
    let doc_path = "tests/ENJKMeYc2vE5pQfgN9aOrY";
    let (header, doc) = load_design_def(doc_path).expect("Failed to load doc");

    println!("Loaded document: {}", header.id);

    let mut found_keyframes = false;

    for (name, view) in &doc.views {
        if let Some(style) = view.style.as_ref() {
            if let Some(node_style) = style.node_style.as_ref() {
                if let Some(override_anim) = node_style.animation_override.as_ref() {
                    if let Some(animation_override::Animation_override::Custom(custom)) =
                        &override_anim.animation_override
                    {
                        for (prop, timeline) in &custom.custom_keyframe_data {
                            println!("Found timeline for prop: {} in view: {}", prop, name);
                            for kf in &timeline.keyframes {
                                println!(
                                    "  Keyframe: fraction={}, value={:?}",
                                    kf.fraction, kf.value
                                );
                                found_keyframes = true;
                            }
                        }
                    }
                }
            }
        }
    }

    assert!(found_keyframes, "No keyframes found in the document!");
}

#[test]
fn test_animation_matrix_dcf_serialization() {
    use dc_bundle::animationspec::animation_override;
    use dc_bundle::definition_file::{load_design_def, save_design_def};
    use dc_figma_import::animation_override::AnimationOverride;
    use dc_figma_import::animation_spec_schema::AnimationOverrideJson;
    use tempfile::tempdir;

    let doc_path = "tests/ENJKMeYc2vE5pQfgN9aOrY";
    let (header, mut doc) = load_design_def(doc_path).expect("Failed to load doc");

    let matrix_json_str = r#"{
        "default_spec": {
            "initial_delay": { "secs": 0, "nanos": 0 },
            "animation": {
                "Smooth": {
                    "duration": { "secs": 0, "nanos": 300000000 },
                    "repeat_type": "NoRepeat",
                    "easing": "Linear"
                }
            },
            "interrupt_type": "None"
        },
        "transitions": [
            {
                "from": "VariantA",
                "to": "VariantB",
                "name": "Transition1",
                "spec": {
                    "initial_delay": { "secs": 0, "nanos": 0 },
                    "animation": {
                        "Smooth": {
                            "duration": { "secs": 0, "nanos": 500000000 },
                            "repeat_type": "NoRepeat",
                            "easing": "EaseInOut"
                        }
                    },
                    "interrupt_type": "None"
                },
                "timelines": {
                    "PropA": {
                        "target_easing": "Linear",
                        "keyframes": [
                            { "fraction": 0.5, "value_json": 12.5, "easing": "EaseIn" }
                        ]
                    }
                }
            }
        ]
    }"#;

    let anim_json: AnimationOverrideJson =
        serde_json::from_str(matrix_json_str).expect("Failed to parse matrix JSON");
    let anim_proto: dc_bundle::animationspec::AnimationOverride =
        AnimationOverride::from(&anim_json).into();

    let target_view_name = doc.views.keys().next().expect("Doc has no views").clone();
    if let Some(view) = doc.views.get_mut(&target_view_name) {
        let style = view.style.mut_or_insert_default();
        let node_style = style.node_style.mut_or_insert_default();
        node_style.animation_override = Some(anim_proto).into();
    }

    let tmp_dir = tempdir().expect("Failed to create temp dir");
    let tmp_dcf_path = tmp_dir.path().join("test_matrix.dcf");
    save_design_def(&tmp_dcf_path, &header, &doc).expect("Failed to save .dcf with matrix");

    let (loaded_header, loaded_doc) =
        load_design_def(&tmp_dcf_path).expect("Failed to reload saved .dcf");
    assert_eq!(loaded_header.id, header.id);

    let reloaded_view = loaded_doc
        .views
        .get(&target_view_name)
        .expect("Reloaded doc missing target view");

    let reloaded_override = reloaded_view
        .style
        .as_ref()
        .unwrap()
        .node_style
        .as_ref()
        .unwrap()
        .animation_override
        .as_ref()
        .unwrap();

    if let Some(animation_override::Animation_override::Matrix(matrix)) =
        &reloaded_override.animation_override
    {
        assert!(matrix.default_spec.is_some());
        assert_eq!(matrix.transitions.len(), 1);
        let transition = &matrix.transitions[0];
        assert_eq!(transition.from_variant, "VariantA");
        assert_eq!(transition.to_variant, "VariantB");
        assert_eq!(transition.animation_name, "Transition1");
        assert!(transition.custom_keyframe_data.contains_key("PropA"));
        let timeline = &transition.custom_keyframe_data["PropA"];
        assert_eq!(timeline.keyframes.len(), 1);
        assert_eq!(timeline.keyframes[0].fraction, 0.5);
    } else {
        panic!("Expected Animation_override::Matrix after reloading .dcf");
    }
}

