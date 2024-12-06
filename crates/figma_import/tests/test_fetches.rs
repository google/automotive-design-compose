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
#![cfg(feature = "fetch")]

use figma_import::tools::fetch::{build_definition, load_figma_token};
use figma_import::{Document, ProxyConfig};

// Simply fetches and serializes a doc
fn run_test(doc_id: &str, queries: &[&str]) {
    let queries: Vec<String> = queries.iter().map(|s| s.to_string()).collect();

    let figma_token = load_figma_token().unwrap();

    let mut doc: Document = Document::new(
        figma_token.as_str(),
        doc_id.to_string(),
        String::new(),
        &ProxyConfig::None,
        None,
    )
    .unwrap();

    let dc_definition = build_definition(&mut doc, &queries).unwrap();

    bincode::serialize(&dc_definition).unwrap();
}

#[test]
#[cfg_attr(not(feature = "test_fetches"), ignore)]
fn fetch_design_switcher() {
    run_test(
        "Ljph4e3sC0lHcynfXpoh9f",
        &[
            "SettingsView",
            "FigmaDoc",
            "Message",
            "MessageFailed",
            "LoadingSpinner",
            "Checkbox",
            "NodeNamesCheckbox",
            "MiniMessagesCheckbox",
            "ShowRecompositionCheckbox",
            "UseLocalResCheckbox",
            "DesignViewMain",
            "LiveMode",
            "TopStatusBar",
        ],
    )
}

#[test]
#[cfg_attr(not(feature = "test_fetches"), ignore)]
fn fetch_variable_modes() {
    run_test("HhGxvL4aHhP8ALsLNz56TP", &["#stage", "#Box"]);
}

#[test]
#[cfg_attr(not(feature = "test_fetches"), ignore)]
fn fetch_dials_gauges() {
    run_test(
        "lZj6E9GtIQQE4HNLpzgETw",
        &[
            "#stage",
            "#stage-vector-progress",
            "#stage-constraints",
            "#arc-angle",
            "#needle-rotation",
            "#progress-bar",
            "#progress-indicator",
        ],
    );
}

#[test]
#[cfg_attr(not(feature = "test_fetches"), ignore)]
fn fetch_styled_text_runs() {
    run_test("mIYV4YsYYaMTsBMCVskA4N", &["#MainFrame"])
}
