# KSP 2.0 and Compose Gradle Plugin Migration Plan

This document outlines the plan for updating the project to KSP 2.0 and the corresponding Kotlin and Compose versions.

The migration will be performed in four-phases:
1.  **Phase 1: Research and Version Selection**
2.  **Phase 2: Build System & Dependency Updates**
3.  **Phase 3: KSP Codegen Migration Analysis**
4.  **Phase 4: Implementation and Verification**

---

## Phase 1: Research and Version Selection

The first step is to identify a compatible set of the latest stable versions for the core components. I will use web searches to find the correct versions.

1.  **Kotlin Symbol Processing (KSP):** Find the latest stable (non-beta, non-RC) release of KSP 2.0.
2.  **Kotlin:** Find the specific Kotlin version that the selected KSP version is designed for. There is a strict compatibility matrix.
3.  **JetBrains Compose Gradle Plugin:** Find the version of the `org.jetbrains.compose` plugin that is compatible with the selected Kotlin version.
4.  **Compose Compiler:** The Compose Compiler version is tied to the Kotlin version. The JetBrains Compose plugin will manage this, but I will verify the required version to ensure everything is consistent.

## Phase 2: Build System & Dependency Updates

This phase involves modifying the Gradle build files to use the new versions and the new JetBrains Compose Gradle plugin.

1.  **Update `gradle/libs.versions.toml`:**
    *   Update the properties for Kotlin, KSP, and any existing Compose libraries to the versions identified in Phase 1.
    *   Add a new property for the JetBrains Compose Gradle plugin.

2.  **Update Root and Module `build.gradle.kts` files:**
    *   In the root `build.gradle.kts` (and any relevant subproject `build.gradle.kts` files), I will replace the AndroidX-centric Compose setup with the new JetBrains Compose plugin.
    *   This means removing the `composeOptions` block from the `android` configuration block where it sets `kotlinCompilerExtensionVersion`.
    *   I will then apply the `org.jetbrains.compose` plugin in the `plugins` block of the relevant modules (like `designcompose`, `reference-apps`, etc.). This plugin will manage the Compose compiler and runtime dependencies.

## Phase 3: KSP Codegen Migration Analysis

With the build system updated, I will analyze the KSP processor code for necessary migrations. The primary file is `codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt`.

1.  **Research KSP 2.0 Breaking Changes:** I will search for the official KSP 1.0 to 2.0 migration guide.
2.  **Analyze `BuilderProcessor.kt`:** I will review the code against the migration guide, paying close attention to:
    *   **API Changes:** How symbols are resolved (`resolver.getSymbolsWithAnnotation`, etc.).
    *   **Type System:** Any changes to how types are represented (`KSType`, `KSClassDeclaration`, etc.) and how their nullability is handled.
    *   **Code Generation:** Changes in the `CodeGenerator` API for creating new files. The current implementation uses `codeGenerator.createNewFile`. I will verify this is still correct.
    *   **Visitor Pattern:** Check for any changes in the `KSVisitorVoid` or other visitor APIs.
    *   **Dependencies:** The `Dependencies` class used when creating new files might have changed semantics.

## Phase 4: Implementation and Verification

This is the execution phase, where I will apply the changes and iteratively fix issues.

1.  **Apply Changes:** I will apply the version and build script changes from Phase 2, followed by any necessary code changes identified in Phase 3.
2.  **Iterative Build and Fix:** I will attempt to build the project using `./gradlew build`. I expect this to fail initially. I will analyze the compilation errors and fix them, repeating the process until the project builds successfully.
3.  **Run All Tests:** Once the project builds, I will run the full test suite to ensure correctness and catch any runtime regressions.
    *   Start with `./gradlew test` for unit tests.
    *   Then run `./gradlew connectedCheck` for instrumented tests.
    *   Finally, execute the full validation script: `./dev-scripts/test-all.sh`.

---

## Running Log
*I will keep a log of my actions and findings here to track progress and facilitate recovery from any issues.*

**Note:** After completing each major milestone in this plan, I will commit the changes to the current branch to save our progress.

### Phase 1: Research and Version Selection (Completed)

*   **Kotlin:** `2.1.21`
*   **KSP:** `2.1.21-2.0.1`
*   **JetBrains Compose Gradle Plugin:** `org.jetbrains.kotlin.plugin.compose` version `2.1.21`

### Phase 2: Build System & Dependency Updates (Completed)

*   Updated `gradle/libs.versions.toml` with the new versions.
*   Added the `org.jetbrains.kotlin.plugin.compose` plugin to the relevant modules.
*   Removed the `composeOptions` and `buildFeatures` blocks from the relevant modules.

**Note:** After completing each major milestone in this plan, I will commit the changes to the current branch to save our progress.
