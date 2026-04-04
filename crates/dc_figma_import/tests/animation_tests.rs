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
    let doc_path = "../../../../utils/gui-playground-app/data/ENJKMeYc2vE5pQfgN9aOrY";
    let (header, doc) = load_design_def(doc_path).expect("Failed to load doc");

    println!("Loaded document: {}", header.id);

    let mut found_keyframes = false;

    for (name, view) in &doc.views {
        if let Some(style) = view.style.as_ref() {
            if let Some(node_style) = style.node_style.as_ref() {
                if let Some(override_anim) = node_style.animation_override.as_ref() {
                    if let Some(custom_value) = &override_anim.animation_override {
                        match custom_value {
                            animation_override::Animation_override::Custom(custom) => {
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
                            _ => {}
                        }
                    }
                }
            }
        }
    }

    assert!(found_keyframes, "No keyframes found in the document!");
}
