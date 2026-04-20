# dc_figma_import

This library performs conversion from a Figma document to a serialized DesignCompose
file (`.dcf`) that can be rendered by the DesignCompose Android library.

The Figma API provides access to documents, images, vectors, and interactions in
Figma's own format. A single Figma document can reference resources from other
documents (via Figma's Component Library and Design System features). This library
fetches all necessary data, rasterizes complex vectors, and packages everything into
a single self-contained document (.dcf file). It also supports incremental updates to avoid
refetching assets the client already has.

## Architecture

### Parsing Figma documents (.dcf files)

* **`figma_schema`** — Definitions for Figma document contents as returned by the
  Figma REST API. The Figma API implementation does not always match the
  documentation, so `figma_schema` is generally more accurate about which fields
  are optional or omitted. Unknown enum variants are handled gracefully via
  `#[serde(other)]` fallbacks.
* **`reaction_schema`** — Definitions for Figma interaction data. The Figma API
  does not return these values directly; they are only available to JavaScript
  plugins. Our Figma plugin copies them to the "plugin area", which _can_ be
  accessed via the REST API. These definitions mirror Figma's Plugin API
  interaction types.
* **`extended_layout_schema`** — Values written by our Figma plugin for layout
  features that Figma does not expose natively (e.g., maximum line count in text
  nodes). These are stored in the plugin data area.
* **`meter_schema`** — Definitions for dial, gauge, and progress bar data written
  by the Dials and Gauges Figma plugin.
* **`shader_schema`** — Definitions for custom shader data.

### Generating data for the UI toolkit

`dc_figma_import` uses the Protocol Buffers serialization format (defined in
[`dc_bundle`](../dc_bundle/)) to encode processed documents for the Android client.

* Protobuf schema files live in `dc_bundle/src/proto/` and define the core
  structures (views, styles, layout, fonts) that make up a serialized document.
* The `design_definition` module provides high‑level types for the serialized
  output.

### Converting from Figma schema → toolkit schema

* **`document`** — Responsible for fetching document definitions from Figma and
  orchestrating the conversion. It checks whether a document has changed since the
  last fetch and initiates image/vector processing.
* **`transform_flexbox`** — Core conversion algorithm. Iterates over all Figma
  nodes, determines which ones should appear in the output, resolves component
  references, and converts node properties from `figma_schema` types to the
  protobuf toolkit types.
* **`image_context`** — Manages all images in the document. Fetches image URLs
  returned by the Figma API, stores the raw bytes and dimensions, and supports
  encoding/decoding its image list for incremental updates.
  * **`svg`** — Rasterizes complex vectors at 1×, 2×, and 3× pixel densities so
    the Android client can select the best resolution. We avoid Figma's own vector
    rasterization API because it clips edges.
* **`component_context`** — Tracks component definitions and instances across
  documents, resolving cross-file component references.
* **`variable_utils`** — Handles Figma Variables (design tokens) and their
  resolution.

### Application binaries

* **`dcf_info`** — Deserializes a `.dcf` file and prints metadata.

  ```shell
  cargo run --bin dcf_info --features=dcf_info <path>
  ```

* **`fetch`** — Queries Figma for a file, processes the response, and saves it as
  a `.dcf` file.

  ```shell
  cargo run --bin fetch --features=fetch -- \
    --doc-id=<figma doc ID> \
    --api-key=<figma API key> \
    --output=<output file> \
    --nodes=<nodes to retrieve>
  ```
