# Gemini Code Assistant Project Overview

This document provides a high-level overview of the Automotive Design for Compose (DesignCompose) project, intended to be used by the Gemini code assistant.

## Project Description

Automotive Design for Compose (DesignCompose) is an extension for Jetpack Compose that enables defining Android application UIs in Figma. It supports live updates from Figma designs, allowing for a more iterative and collaborative design and development process.

The core of DesignCompose consists of a Jetpack Compose library, a Kotlin compiler plugin, and a Rust-based service for fetching and serializing Figma documents.

## Key Technologies

*   **Frontend:** Jetpack Compose (Kotlin)
*   **Backend/Figma Integration:** Rust
*   **Build System:** Gradle
*   **Dependency Management:** Gradle, Cargo (for Rust)
*   **Protos:** The project uses a git submodule for its proto files, located at `crates/dc_bundle/src/proto`.

## Project Structure

The project is a multi-module Gradle project with a mix of Kotlin, Java, and Rust code.

### Core Modules

*   `designcompose`: The main runtime library that interprets and renders Figma documents.
*   `annotation`: Contains Kotlin annotations like `@DesignDoc` and `@DesignComponent`.
*   `codegen`: A Kotlin compiler plugin that processes the annotations and generates Composables.
*   `common`: Contains code shared between other modules.

### Rust Crates

The Rust code is located in the `crates/` directory and is responsible for interacting with the Figma API.

*   `dc_bundle`: Handles the bundling of Figma data.
*   `dc_jni`: Provides the Java Native Interface (JNI) for the Android runtime to communicate with the Rust code.
*   `dc_layout`: Manages layout information from Figma.
*   `figma_import`: Fetches and serializes Figma documents.

### Reference Applications

The `reference-apps/` directory contains several example applications that demonstrate how to use DesignCompose.

*   `helloworld`: A basic "Hello, World!" application.
*   `tutorial`: An interactive tutorial application.
*   `aaos-unbundled`: A sample media center for Android Automotive OS.
*   `cluster-demo`: A demonstration of a dashboard cluster.

### Testing and Validation

*   `integration-tests/`: Contains various integration tests.
*   `test/`: Contains unit tests.
*   `validation/`: Contains a visual validation app.

### Build System

*   `build-logic/`: Contains custom Gradle build logic.
*   `plugins/`: Contains custom Gradle plugins for handling the Rust and Android parts of the build.
*   `build.gradle.kts`, `settings.gradle.kts`: The main Gradle build files.
*   `gradle/libs.versions.toml`: Defines the project's dependencies.

### Documentation

*   `README.md`: The main entry point for understanding the project.
*   `docs/`: Contains the project's documentation website, built with Jekyll.

## Code Formatting

The project uses specific formatters to maintain a consistent code style.

*   **Kotlin:** `ktfmt` with the `--kotlinlang-style` flag.
*   **Rust:** `rustfmt`

The easiest way to ensure all files are formatted correctly is to run the `format-all.sh` script located in the `dev-scripts/` directory.

## Build Process

The project is built using Gradle. The build process involves compiling the Kotlin/Java code, the Rust code, and then packaging everything into Android libraries and applications.

### Key Build Scripts

*   `./gradlew build`: Builds the entire project.
*   `./gradlew test`: Runs all unit tests.
*   `./gradlew connectedCheck`: Runs all instrumented tests on a connected device or emulator.
*   `./dev-scripts/test-all.sh`: A script that runs all tests, including formatting checks.

### Dependencies

*   **Android NDK:** Version 27.0.12077973 is required for the Rust JNI library.
*   **Rust:** Version 1.68.0 is required.
*   **Protobuf Compiler:** Required for compiling the protobuf messages.

## Development Workflow

1.  **Initial Setup:**
    *   Clone the repository with `--recurse-submodules`.
    *   Install the required dependencies (Android Studio, NDK, Rust, Protobuf compiler).
    .
    *   Run `./install-rust-toolchains.sh` to install the necessary Rust toolchains.
2.  **Making Changes:**
    *   Modify the code in the appropriate module.
    *   If changing the Figma serialization format, update the serialized files.
3.  **Testing:**
    *   Run `./dev-scripts/format-all.sh` to format the code.
    *   Run `./dev-scripts/test-all.sh` to run all tests.
4.  **Submitting Changes:**
    *   Create a pull request.
