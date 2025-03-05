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

#[cfg(test)]
mod serialized_document_tests {

    use dc_bundle::definition_file::{load_design_def, save_design_def};
    use std::fs::File;
    use std::io::Write;
    use std::path::PathBuf;
    use testdir::testdir;

    #[test]
    fn load_save_load() {
        //Load a test doc.
        let mut doc_path = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
        doc_path.push("../../reference-apps/helloworld/helloworld-app/src/main/assets/figma/HelloWorldDoc_pxVlixodJqZL95zo2RzTHl.dcf");
        let (header, doc) = load_design_def(doc_path).expect("Failed to load design bundle.");

        // Dump some info
        println!("Deserialized header: {}", &header);
        println!("Deserialized doc: {}", &doc);

        // Re-save the test doc into a temporary file in a temporary directory.
        let tmp_dir = testdir!();
        let tmp_doc_path = PathBuf::from(&tmp_dir).join("tmp_pxVlixodJqZL95zo2RzTHl.dcf");
        save_design_def(&tmp_doc_path, &header, &doc)
            .expect("Failed to save temporary DesignCompose Definition.");

        // Re-load the temporary file
        let (tmp_header, tmp_doc) =
            load_design_def(&tmp_doc_path).expect("Failed to load tmp DesignCompose Definition.");
        println!("Tmp deserialized header: {}", &tmp_header);
        println!("Tmp deserialized doc: {}", &tmp_doc);
    }

    #[test]
    #[should_panic]
    fn load_missing_doc() {
        // Try to load a doc which doesn't exist. This should fail with a clean error.
        let mut doc_path = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
        doc_path.push("this.doc.does.not.exist.dcf");
        let (_tmp_header, _tmp_doc) =
            load_design_def(&doc_path).expect("Failed to load tmp DesignCompose Definition.");
    }

    #[test]
    #[should_panic]
    fn load_bad_doc() {
        // Create a garbage binary doc in a temporary directory and load it, hopefully seeing a failure.
        let tmp_dir = testdir!();
        let garbage_doc_path = PathBuf::from(&tmp_dir).join("tmp.garbage.file.dcf");
        let mut file =
            File::create(&garbage_doc_path).expect("Failed to create new garbage binary doc file.");
        let data: Vec<u8> = (0..48).map(|v| v).collect();
        file.write_all(&data).expect("Failed to write garbage data to garbage file.");

        let (_tmp_header, _tmp_doc) = load_design_def(&garbage_doc_path)
            .expect("Failed to load garbage DesignCompose Definition.");
    }
}
