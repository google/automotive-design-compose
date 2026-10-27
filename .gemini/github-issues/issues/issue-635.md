# Issue #635: Allow `fetch` to get its nodes list from the JSON emitted by the KSP processor

## 1. Issue Metadata
- **Issue #:** 635
- **Title:** Allow `fetch` to get its nodes list from the JSON emitted by the KSP processor
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/635](https://github.com/google/automotive-design-compose/issues/635)
- **Author:** @iamralpht
- **Created Date:** 2023-12-14T09:27:03Z
- **Updated Date:** 2026-03-17T16:36:33Z
- **Labels:** `Type: New Feature`, `qol`, `engProd`, `:green_circle:  P2`
- **Priority / Type:** P2 / Engineering Productivity / CLI Feature Request
- **Status:** Open (Actionable CLI Enhancement)

---

## 2. Problem Statement & User Impact
Partners and internal teams use the standalone Rust `fetch` CLI tool (`crates/dc_figma_import/src/tools/fetch.rs`) in CI/CD pipelines and automated offline pre-caching workflows.

Currently, `fetch` requires developers to manually specify all Figma node IDs via command-line arguments:
```bash
fetch --file-id <DOC_ID> --nodes "1:2,3:4,5:6"
```
When an application uses dozens of `@DesignComponent`s across multiple files, developers must manually keep the list of node IDs in sync with their Kotlin code.

However, the DesignCompose KSP compiler plugin already scans all `@DesignDoc` and `@DesignComponent` annotations and generates a JSON metadata artifact containing the exact set of required node queries. Connecting `fetch` to this JSON output would eliminate manual synchronization and prevent missing node errors during offline compilation.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`crates/dc_figma_import/src/tools/fetch.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/tools/fetch.rs#L80-L160)
  - [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt)
  - [`plugins/gradle-plugin/src/main/kotlin/com/android/designcompose/gradle/FetchTask.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/plugins/gradle-plugin/src/main/kotlin/com/android/designcompose/gradle/FetchTask.kt)

- **Detailed Root Cause:**
  1. `fetch.rs` uses `clap` for argument parsing and only defines `--nodes <NODES>` (a comma-separated string) and `--file-id <FILE_ID>`.
  2. KSP emits a serialized JSON file containing document definitions and component node names/IDs in `build/generated/ksp/`.
  3. No command-line flag exists in `fetch` (such as `--ksp-json <PATH>`) to parse this file and extract the node list automatically.

---

## 4. Proposed Fix / Implementation Plan
1. **Extend CLI Arguments in `fetch.rs`:**
   Add an optional argument in `crates/dc_figma_import/src/tools/fetch.rs`:
   ```rust
   #[derive(Parser, Debug)]
   struct Args {
       #[arg(long)]
       file_id: Option<String>,
       #[arg(long)]
       nodes: Option<String>,
       /// Path to JSON metadata emitted by DesignCompose KSP processor
       #[arg(long)]
       ksp_json: Option<PathBuf>,
   }
   ```
2. **Implement JSON Schema Deserialization:**
   Define a struct representing the KSP JSON manifest:
   ```rust
   #[derive(Deserialize)]
   struct KspManifest {
       doc_id: Option<String>,
       nodes: Vec<String>,
   }
   ```
   If `--ksp-json` is provided, parse the file, extract the `doc_id` and list of node IDs, and append them to the fetch query.
3. **Integrate with Gradle `FetchTask`:**
   Wire Gradle task inputs so `FetchTask` can pass the KSP output directory directly to the native `fetch` binary.

---

## 5. Validation & Reproduction Plan
1. **Integration Test:**
   Run KSP code generation on the tutorial app to produce the build JSON:
   ```bash
   ./gradlew :reference-apps:tutorial:kspDebugKotlin
   ```
2. **Fetch Execution:**
   Run `fetch` pointing to the generated JSON file:
   ```bash
   cargo run --bin fetch -- --ksp-json reference-apps/tutorial/build/generated/ksp/manifest.json
   ```
   Verify that all annotated nodes are downloaded and serialized into the output `.dcf` file.

---

## 6. Drafted GitHub Comment
```markdown
### Implementation Plan for `--ksp-json` in `fetch`:

We can add a `--ksp-json` argument to the standalone `fetch` tool in `crates/dc_figma_import/src/tools/fetch.rs`.

When supplied:
1. `fetch` parses the JSON manifest emitted by the KSP compiler plugin.
2. It automatically extracts the `doc_id` and all component node IDs.
3. It fetches the document and assets with the exact node scope used by the app at runtime, removing the need to maintain manual `--nodes` arguments in CI scripts.
```
