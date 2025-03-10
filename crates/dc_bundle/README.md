# dc_bundle

The `dc_bundle` crate provides the core data structures for defining and working with DesignCompose Bundles and Definitions. It acts as a foundational component for the larger DesignCompose ecosystem.

## Key Features

*   **DesignCompose Definition:** The crate provides `DesignComposeDefinition` and `DesignComposeDefinitionHeader` structs to define the structure and metadata of a DesignCompose document.
*   **Node Query:** It provides a `NodeQuery` enum to find nodes by id, name, or variant.
*   **Encoded Image Map:** The crate uses `EncodedImageMap` to map image keys to network bytes.
*   **Data Structures:** The crate provides `definition` and `definition_file` modules that define how DesignCompose works.
*   **Error Handling:** It offers robust error handling through its `Error` enum, ensuring that applications can gracefully handle common issues.
*   **Protocol Buffer Support:** It uses a protocol buffer (`proto`) for data exchange.
*   **Dependencies:** It relies on `protobuf`, `thiserror`, `serde`, `serde_bytes`, and `log` for its operation, and `protobuf-codegen` for build-time code generation.

## Usage

### Adding as a dependency

To use this crate, add the following to your `Cargo.toml` file:
