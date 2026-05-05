// Copyright 2026 Google LLC
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

use dc_bundle::definition_file::load_design_def;
use dc_bundle::view::view_data::View_data_type;

fn main() {
    let args: Vec<String> = std::env::args().collect();
    for path in &args[1..] {
        analyze(path);
    }
}

fn analyze(path: &str) {
    let size = std::fs::metadata(path).map(|m| m.len()).unwrap_or(0);
    println!("\n=== {} ({:.2} MB) ===", path, size as f64 / 1048576.0);
    let (h, d) = load_design_def(path).expect("load failed");
    println!(
        "Header: v={} name={} id={} modified={} ver={}",
        h.dc_version, h.name, h.id, h.last_modified, h.response_version
    );
    println!("Views: {}", d.views.len());
    let mut names: Vec<_> = d.views.keys().collect();
    names.sort();
    for n in &names {
        let v = &d.views[*n];
        let b = protobuf::Message::write_to_bytes(v).unwrap_or_default().len();
        let ch = v
            .data
            .as_ref()
            .and_then(|x| x.view_data_type.as_ref())
            .map(|t| match t {
                View_data_type::Container(c) => c.children.len(),
                _ => 0,
            })
            .unwrap_or(0);
        println!("  {:60} {:>8}B {:>4}ch", n, b, ch);
    }
    println!("Component sets: {}", d.component_sets.len());
    let img_total: usize = d.images.values().map(|i| i.len()).sum();
    println!("Images: {} ({:.2} MB)", d.images.len(), img_total as f64 / 1048576.0);
    let vars = d.variable_map.as_ref().map(|m| m.variables_by_id.len()).unwrap_or(0);
    let colls = d.variable_map.as_ref().map(|m| m.collections_by_id.len()).unwrap_or(0);
    println!("Variables: {} (collections: {})", vars, colls);
    let vb: usize = d
        .views
        .values()
        .map(|v| protobuf::Message::write_to_bytes(v).unwrap_or_default().len())
        .sum();
    let varb = d
        .variable_map
        .as_ref()
        .map(|m| protobuf::Message::write_to_bytes(m).unwrap_or_default().len())
        .unwrap_or(0);
    println!(
        "Size breakdown: views={:.2}MB images={:.2}MB vars={:.2}MB file={:.2}MB",
        vb as f64 / 1048576.0,
        img_total as f64 / 1048576.0,
        varb as f64 / 1048576.0,
        size as f64 / 1048576.0
    );
}
