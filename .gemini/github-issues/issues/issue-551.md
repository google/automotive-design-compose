# Issue #551: DesignCompose does not download images larger than a certain size

## 1. Issue Metadata
- **Issue #:** 551
- **Title:** DesignCompose does not download images larger than a certain size
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/551](https://github.com/google/automotive-design-compose/issues/551)
- **Author:** @rylin8
- **Created Date:** 2023-11-13T18:21:27Z
- **Updated Date:** 2024-01-10T21:46:01Z
- **Labels:** `:green_circle:  P2`
- **Priority / Type:** P2 / Bug & Error Diagnosis (Internal Ref: `b/310338161`)
- **Status:** Open (Actionable Image Fetch Enhancement)

---

## 2. Problem Statement & User Impact
When importing Figma designs that contain large high-resolution bitmap assets (e.g. 4K/8K background textures, album covers, or cluster maps), image downloading fails silently or aborts without an informative error message:
- The node renders as a blank rectangle or transparent background.
- The developer receives no diagnostic feedback explaining whether the failure was caused by Figma API payload size limits, HTTP connection timeout, or memory exhaustion.
- Downstream build tasks (such as `figmaFileFetch`) succeed partially but leave the serialized document without necessary image blobs.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`crates/dc_figma_import/src/image_context.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/image_context.rs#L60-L130)
  - [`crates/dc_figma_import/src/http_client.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/http_client.rs)
  - [`crates/dc_figma_import/src/fetch.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/fetch.rs)

- **Detailed Root Cause:**
  1. **Figma Image API Limits:**
     Figma's `GET /v1/images/:file_key` endpoint renders vector/raster images on Figma's servers. If an image requires excessive rendering time or exceeds Figma's internal rasterization budget (often 25-50 MB or 4096x4096px), Figma returns an HTTP 500 error, HTTP 504 Gateway Timeout, or returns a null URL in the JSON response payload.
  2. **Insufficient Error Logging in Rust Fetcher:**
     In [`image_context.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/image_context.rs), when an image URL is missing or fails during `http_fetch_image`, the code currently discards the response details or logs a generic message:
     ```rust
     Err(e) => {
         eprintln!("Failed to fetch image: {}", e);
         None
     }
     ```
  3. **No Automatic Downscaling or Chunking:**
     DesignCompose requests images at scale 1.0 or scale 2.0 without inspecting if the resolution exceeds Android device texture limits (typically 2048x2048 or 4096x4096 on automotive hardware).

---

## 4. Proposed Fix / Implementation Plan
1. **Explicit Error Diagnosis & Logging:**
   In [`image_context.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/image_context.rs), inspect the HTTP status code and response body from Figma. If Figma returns an error or null image URL, output an actionable error message specifying:
   - Node name and node ID.
   - Image fill dimensions in Figma.
   - Recommendation to downsample the image in Figma or optimize export settings.
2. **Configurable Image Scale / Compression:**
   Add a configuration option in `FigmaFetchConfig` to allow clamping maximum image export dimension (e.g., `--max-image-dimension=2048`) or adjusting export scale when fetching large background assets.
3. **Retry with Fallback Scale:**
   If fetching at 2.0x fails with a timeout or 504, automatically retry once at 1.0x.

---

## 5. Validation & Reproduction Plan
1. **Reproduction Test:**
   Create a test Figma file with an 8000x8000 pixel uncompressed PNG fill. Run `fetch` tool:
   ```bash
   cargo run --bin fetch -- --file-id <TEST_FILE_ID> --nodes <NODE_ID>
   ```
2. **Verify Error Reporting:**
   Confirm that the CLI outputs a clear warning with node ID and troubleshooting steps instead of a silent failure.

---

## 6. Drafted GitHub Comment
```markdown
### Analysis of Large Image Download Failures:

Figma's image rendering API (`GET /v1/images/:key`) enforces timeout and memory budgets when rasterizing nodes. When an image exceeds these internal limits, Figma returns an HTTP 500/504 or omits the image URL from the response dictionary.

### Proposed Improvement:
1. **Diagnostic Warnings:** Enhance `crates/dc_figma_import/src/image_context.rs` to detect when Figma fails to return an image URL, outputting the node name, ID, and guidance to downscale the asset in Figma.
2. **Fallback Scale:** Add an automatic retry with lower export scale (e.g. falling back from 2x to 1x) if a large asset times out.
3. **Max Dimension Parameter:** Allow specifying `--max-image-dimension` during fetch to keep assets within automotive GPU texture limits.
```
