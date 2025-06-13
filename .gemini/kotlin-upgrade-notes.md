# KSP 2.0 and Compose Gradle Plugin Migration Plan

This document outlines the plan for updating the project to KSP 2.0 and the corresponding Kotlin and Compose versions.

The migration will be performed in four-phases:
1.  **Phase 1: Research and Version Selection**
2.  **Phase 2: Build System & Dependency Updates**
3.  **Phase 3: KSP Codegen Migration Analysis**
4.  **Phase 4: Implementation and Verification**

---

**Note:** After completing each major milestone in this plan, I will summarize the changes, and then pause and wait for your confirmation before committing the changes and proceeding to the next phase.

## Running Log

### Phase 1: Research and Version Selection (Completed)

*   **Kotlin:** `2.1.21`
*   **KSP:** `2.1.21-2.0.1`
*   **JetBrains Compose Gradle Plugin:** `org.jetbrains.kotlin.plugin.compose` version `2.1.21`

### Phase 2: Build System & Dependency Updates (Completed)

*   Updated `gradle/libs.versions.toml` with the new versions.
*   Added the `org.jetbrains.kotlin.plugin.compose` plugin to the relevant modules (`designcompose`, `helloworld-app`, `tutorial-app`, `cluster-demo`, `validation`, `battleship-app`, `battleship-lib`).
*   Removed the `composeOptions` and `buildFeatures` blocks from the relevant modules.
*   Added the `jetbrains.compose` plugin to the root `build.gradle.kts` with `apply false`.

### Phase 3: KSP Codegen Migration Analysis (Completed)
*   Searched for KSP 1.0 to 2.0 migration guides. The main takeaway is that the API is mostly backward compatible and the main changes are in the build configuration.
*   Reviewed `codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt` and found no immediate issues.

### Phase 4: Implementation and Verification (In Progress)
*   Attempted to build the project.
*   The build failed multiple times due to `Unresolved reference: compiler` in various `build.gradle.kts` files.
*   Fixed the following files:
    *   `integration-tests/benchmarks/battleship/battleship-app/build.gradle.kts`
    *   `integration-tests/benchmarks/battleship/lib/build.gradle.kts`
*   The build is now failing with a new error: `java.io.IOException: Unable to create debug keystore in /Users/froeht/.android because it is not writable.` This is a sandbox issue. Pausing for user to adjust seatbelt config.
