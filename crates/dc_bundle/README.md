# dc_bundle

`dc_bundle` is a crate that provides the core data structures for DesignCompose.

## Purpose

This crate defines the design document structures used by DesignCompose to represent design
definitions. It includes design definitions for elements, layouts, modifiers, and views.

## Modules

- `definition.rs`: Contains the core rust implementation for design document definitions.
- `definition_file.rs`: Handles the encoding-decoding, saving and loading of definition files.
- `proto/`: Proto definition of design document, elements, layouts, views and modifiers.
- `definition/`: Contains the core rust implementation for elements, layouts, modifiers and views.

## Functionality

- **Design Document Definitions:** Defines the design document structures for DesignCompose.
- **Element Definitions:** Contains definitions for various design elements.
- **Layout Definitions:** Contains definitions for layout styles and properties.
- **Modifier Definitions:** Includes definitions for design modifiers.
- **View Definitions:** Contains definitions for views and their properties.
- **Error Handling**: Provides custom error types to handle specific errors.

## Usage

This crate is primarily used internally by DesignCompose for processing and managing design data.

## Dependencies

- `protobuf`: Used for protocol buffer handling.
- `thiserror`: Used for custom error handling.
- `serde`: Used for serialization.
- `serde_bytes`: Used for byte serialization.
- `log`: Used for logging.

## Build Dependencies

- `protobuf-codegen`: Used for building protocol buffer definitions.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](https://www.apache.org/licenses/LICENSE-2.0) for details.
