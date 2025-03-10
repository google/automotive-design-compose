# layout

The `layout` crate provides the core layout engine for Android applications using Figma designs. It's a critical component for rendering Figma designs correctly on Android.

## Key Features

*   **Layout Management:** The crate provides the `LayoutManager` struct that manages how the layout of Figma designs is computed and applied.
*   **Taffy Integration:** It leverages the `taffy` library for efficient and accurate layout calculations.
*   **Android Compatibility:** The crate is designed for seamless integration with Android applications.
*   **Customizable Layout:** It supports various layout customizations and styling options.
*   **Dependencies:** It relies on `dc_bundle`, `log`, `android_logger`, `serde`, `taffy`, `protobuf`, and `thiserror`.
*   **Debug Tools:** It includes debug features to print and inspect the layout.

## Usage

### Adding as a dependency

To use this crate, add the following to your `Cargo.toml` file:
