# Gemini Code Assistant Project Overview

This document provides a high-level overview of the Automotive Design for Compose (DesignCompose) project, intended to be used by the Gemini code assistant.

## Project Description

Automotive Design for Compose (DesignCompose) is an extension for Jetpack Compose that enables defining Android application UIs in Figma. It supports live updates from Figma designs, allowing for a more iterative and collaborative design and development process.

The core of DesignCompose consists of a Jetpack Compose library, a Kotlin compiler plugin, and a Rust-based service for fetching and serializing Figma documents.

## Key Technologies

* **Frontend:** Jetpack Compose (Kotlin)
* **Backend/Figma Integration:** Rust
* **Build System:** Gradle
* **Dependency Management:** Gradle, Cargo (for Rust)
* **Protos:** The project uses a git submodule for its proto files, located at `crates/dc_bundle/src/proto`.

## Project Structure

The project is a multi-module Gradle project with a mix of Kotlin, Java, and Rust code.

### Core Modules

* `designcompose`: The main runtime library that interprets and renders Figma documents.
* `annotation`: Contains Kotlin annotations like `@DesignDoc` and `@DesignComponent`.
* `codegen`: A Kotlin compiler plugin that processes the annotations and generates Composables.
* `common`: Contains code shared between other modules.

### Rust Crates

The Rust code is located in the `crates/` directory and is responsible for interacting with the Figma API.

* `dc_bundle`: Handles the bundling of Figma data.
* `dc_jni`: Provides the Java Native Interface (JNI) for the Android runtime to communicate with the Rust code.
* `dc_layout`: Manages layout information from Figma.
* `dc_figma_import`: Fetches and serializes Figma documents.

### Reference Applications

The `reference-apps/` directory contains several example applications that demonstrate how to use DesignCompose.

* `helloworld`: A basic "Hello, World!" application.
* `tutorial`: An interactive tutorial application.
* `aaos-unbundled`: A sample media center for Android Automotive OS.
* `cluster-demo`: A demonstration of a dashboard cluster.

### Testing and Validation

* `integration-tests/`: Contains various integration tests.
* `integration-tests/benchmarks/`: Contains benchmarking applications.
* `test/`: Contains unit tests.
* `validation/`: Contains a visual validation app.

### Benchmarking

The `designcompose` library has a `benchmark` variant that is used by the benchmark apps in `integration-tests/benchmarks/`. These apps are used for performance testing of various DesignCompose features.

### Build System

* `build-logic/`: Contains custom Gradle build logic.
* `plugins/`: Contains custom Gradle plugins for handling the Rust and Android parts of the build.
* `build.gradle.kts`, `settings.gradle.kts`: The main Gradle build files.
* `gradle/libs.versions.toml`: Defines the project's dependencies.

### Documentation

* `README.md`: The main entry point for understanding the project.
* `docs/`: Contains the project's documentation website, built with Jekyll.

## Code Formatting

The project uses specific formatters to maintain a consistent code style.

* **Kotlin:** `ktfmt` with the `--kotlinlang-style` flag.
* **Rust:** `rustfmt`

The easiest way to ensure all files are formatted correctly is to run the `format-all.sh` script located in the `dev-scripts/` directory.

## Build Process

The project is built using Gradle. The build process involves compiling the Kotlin/Java code, the Rust code, and then packaging everything into Android libraries and applications.

### Key Build Scripts

* `./gradlew build`: Builds the entire project.
* `./gradlew test`: Runs all unit tests.
* `./gradlew connectedCheck`: Runs all instrumented tests on a connected device or emulator.
* `./dev-scripts/test-all.sh`: A script that runs all tests, including formatting checks.

### Dependencies

* **Android NDK:** Version 27.0.12077973 is required for the Rust JNI library.
* **Rust:** Version 1.68.0 is required.
* **Protobuf Compiler:** Required for compiling the protobuf messages.

## Development Workflow

1. **Initial Setup:**
    * Clone the repository with `--recurse-submodules`.
    * Install the required dependencies (Android Studio, NDK, Rust, Protobuf compiler).
    .
    * Run `./install-rust-toolchains.sh` to install the necessary Rust toolchains.
2. **Branching:**

    * Create a new branch from `main` using the format `wb/<user>/<branch-name>`.
3. **Making Changes:**
    * Modify the code in the appropriate module.
    * If changing the Figma serialization format, update the serialized files.
4. **Testing:**
    * Run `./dev-scripts/format-all.sh` to format the code.
    * Run `./dev-scripts/test-all.sh` to run all tests.
5. **Submitting Changes:**
    * Create a pull request.

## Code Coverage

To generate a code coverage report for the Kotlin code, run the following command:

`./gradlew jacocoTestReport`

This will generate an XML report at `build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`.

## Task Continuation and Notes

To ensure that work can be continued across sessions, the agent will use a notes system.

* **Creating Notes:** For any task that will require large amounts of planning or file changes, the agent will create a markdown file in the `.gemini/` directory (e.g., `.gemini/task-name-notes.md`). This file will contain a summary of the work completed, the plan for what is left to do, and any other relevant context.

* **Continuing Tasks:** At the beginning of a new session, the agent will look for any files in the `.gemini/` directory (ignoring the `.gitignore` file, so it is important to use the correct tool parameter to see all files). If the agent finds any, it will read them and ask the user if they want to continue the tasks described in them.

## Figma Tools

This section describes the command-line tools available for interacting with Figma.

### fetch

The `fetch` tool downloads a Figma document, extracts specified nodes, and saves the output to a `.dcf` file. This tool requires a Figma access token, which can be provided via the `FIGMA_ACCESS_TOKEN` environment variable or a file at `~/.config/figma_access_token`.

**Usage:**

To fetch a single node:
```bash
cargo run --features="fetch" --bin fetch -- --doc-id <DOCUMENT_ID> --nodes <NODE_NAME> --output <OUTPUT_FILE.dcf>
```

To fetch multiple nodes, repeat the `--nodes` argument for each node ID:
```bash
cargo run --features="fetch" --bin fetch -- --doc-id <DOCUMENT_ID> --nodes <NODE_NAME_1> --nodes <NODE_NAME_2> --nodes <NODE_NAME_3> --output <OUTPUT_FILE.dcf>
```

**Example:**

Fetching a single node:
```bash
cargo run --features="fetch" --bin fetch -- --doc-id 2aM4SczJzWg1rov2qqBMpe --nodes "#MainFrame" --output output.dcf
```

Fetching multiple nodes:
```bash
cargo run --features="fetch" --bin fetch -- --doc-id dGS6HEmsfv8QSHgAGBwEH7 --nodes "#root" --nodes "#DynamicRoot" --nodes "#Item" --output ListPaginationTestDoc_dGS6HEmsfv8QSHgAGBwEH7.dcf
```

**Important:** Do NOT pass multiple node IDs as a comma-separated string to a single `--nodes` argument. Use a separate `--nodes` argument for each node ID.

### dcf_info

The `dcf_info` tool inspects a `.dcf` file and prints its contents, including header information and variable data.

**Usage:**

```bash
cargo run --features="dcf_info" --bin dcf_info -- <INPUT_FILE.dcf> --varinfo
```

**Example:**

```bash
cargo run --features="dcf_info" --bin dcf_info -- output.dcf --varinfo
```
