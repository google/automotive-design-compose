# dc_jni

`dc_jni` is a crate that provides the JNI (Java Native Interface) bindings for DesignCompose.

## Purpose

This crate handles the communication between the Rust and Java/Kotlin parts of the DesignCompose project.

## Functionality

- **JNI Bindings:** Provides the necessary JNI bindings to interface with the Java/Kotlin code.
- **Native Library Loading:** Facilitates loading the native library into the Java/Kotlin environment.

## Usage

This crate is primarily used internally by DesignCompose to enable communication between Rust and Java/Kotlin code.

## Dependencies

- `jni`: Used for JNI bindings.
- `log`: Used for logging.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](https://www.apache.org/licenses/LICENSE-2.0) for details.
