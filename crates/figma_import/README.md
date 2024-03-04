# figma_import

=======

This library (figma_import) and binaries perform conversion from a Figma document to a self-contained serialized doc that can be interpreted and rendered by the DesignCompose library.

The Figma API provides access to documents, images, vectors, and interactions through a variety of access points, in a format that is specific to Figma. Additionally, one Figma document can refer to resources in other Figma documents (this is how Figma's "Component Library" or "Design System" features are implemented). This library will perform all of the neccessary fetches, will render complex vectors to rasters, and will package all of these resources up into a single document. The library can also fetch incremental changes to a document, avoiding refetching and reprocessing assets that the DesignCompose client already has.

## Architecture

### Parsing Figma docs

* `figma_schema` contains definitions for Figma document contents as returned by the Figma API. The Figma API implementation does not match the documentation, so `figma_schema` is generally more accurate on which fields are omitted.
* `reaction_schema` covers the definitions for Figma document contents relating to interactions. The Figma API doesn't return these values directly; they are only available to Figma JavaScript plugins, so our plugin copies them to the "plugin area" which can be accessed using the Figma API. These definitions in our code correspond to the interaction definitions in Figma's JavaScript Plugin API.
* `extended_layout_schema` covers values written by our plugin for layout situations that Figma doesn't provide controls for, such as maximum line counts in text. These are returned in the "plugin area", and the code here implements the schema that our Figma plugin generates.

### Generating data for a UI toolkit

Currently `figma_import` uses the Rust `bincode` serialization format to encode processed documents for the client. In the future we plan to migrate to protobuf because protobuf is better supported within Google.

* `toolkit_schema` includes the core structures that make up a serialized document.
* `toolkit_style` includes the structures relating to style.
  * `toolkit_font_style` includes the font related types which were originally derived from the `font-kit` library.
  * `toolkit_layout_style` includes the layout related types which which were originally derived from the `stretch` library.
* `serialized_document` includes types that wrap the serialized doc response and add some metadata.

### Converting from figma_schema to toolkit_schema

* `document` is responsible for fetching document definitions from Figma and processing them. It knows how to check if a document hasn't changed since the last time it was converted. It also initiates image and vector processing for a document.
* `transform_flexbox` implements the core conversion algorithm. It iterates over all of the nodes that make up the Figma document, decides which ones should be represented in the output, looks up previously encountered component definitions, and converts node properties from `figma_schema` to `toolkit_schema`.
* `image_context` manages all of the images in the document. Figma's API takes a big set of "image references" and then returns a document containing URLs for each of those image references. `image_context` then fetches the images, and stores the network bytes and dimensions. It can encode and decode its list of fetched images and sizes -- this is how the list of previously fetched images is sent to the client and reused for the next request (avoiding having `image_context` fetch images that the client already has).
  * `svg` is used by `document` and `image_context` to rasterize complex vectors to rasters (storing the resulting rasters as images in `image_context`). Currently vectors get rasterized at 1x, 2x, and 3x sizes so that the DesignCompose client can choose the best image based on the device's pixel density. Figma provides an API to rasterize vectors, which we don't use because it clips the edges off of rasterized vectors.

### Application binaries

* `reflection` generates Java code to deserialize the `bincode` encoded `toolkit_schema` and `serialized_document` types.
* `dcf_info` deserializes a serialized DesignCompose file (.dcf) and prints out some basic data about the file.
  * Usage: `cargo run --bin dcf_info --features=dcf_info <path>`
* `fetch` queries Figma for a file with specified nodes, then processes and serializes the response, and saves it into a .dcf file.
  * Usage: `cargo run --bin fetch --features=fetch -- --doc-id=<figma doc ID> --api-key=<figma API key>--output=<output file> --nodes=<nodes to retrieve>`