---
title: Testing
layout: page
parent: Working with Source
nav_order: 2
---

{% include toc.md %}

# Android Tests

We have multiple types of Android Tests, grouped by how they're run. They are triggered by different
Gradle commands and each type should be focused on different things

## AndroidInstrumentedTests

These run on an Android device, which can be a physical device or emulator. These tests are slow to
run and currently not being run on CI, so they should be written only when a full Android device is
needed for the test. The source for the tests is stored in the `<package>/src/androidTest` directory of the Gradle package.

There are two groups of tests, standard ones and Figma Integration tests. The Figma tests should be
located in a `figmaIntegrationTests` sub-package. These tests require
a [working Figma Token](../live-update/setup.md) and will interact with Figma.com to test certain
interactions, including fetching documents. (Gradle will attempt to read the token automatically).

### Running on a physical device or specific emulator

When developing / working on a test you should run them on a physical device or running emulator (
aka Android Virtual Device aka AVD). Running the full suite is slower, but it's much easier to debug
individual runs.

Run with `./gradlew connectedCheck`

### Running on Gradle Managed Devices (much faster)

When simply running tests to confirm that they pass, you should
use [Gradle Managed Devices (aka GMD)](https://developer.android.com/studio/test/gradle-managed-devices)
to run them. These will cause Gradle to launch emulators to run the tests on. One set uses
an [ATD image](https://developer.android.com/studio/test/gradle-managed-devices#gmd-atd), which is
optimized for instrumented tests, and runs faster than the other GMDs.

If you have a sufficiently powerful machine you can use the below Gradle property to launch multiple
emulators and spread the tests among them. I recommend 3 shards for now, I haven't seen much better
performance with 4. Set in your personal `~/.gradle/gradle.properties`, do not set in any of the
checked-in properties files.

```gradle.properties
android.experimental.androidTest.numManagedDeviceShards=3
```

Run the tests on GMDs by running:

```shell
./gradlew gmdTestQuick # Run on the ATDs, for the fastest run
./gradlew gmdTestStandard # Run on ATDs and the most current Android image
./gradlew gmdTestAll # run on all configured GMDs, including all Android images that we test against
```

Note: The first run will have some significant first-time setup as the GMDs are created.

## Local unit tests

Most tests should be written as unit tests, stored in `<package>/src/testDebug`. These are run on your local system using your JVM and are much faster than
Instrumented tests, though it's harder to debug graphics output because there isn't a visible
screen to check the output on (that I'm aware of).

Tests that need Android components, such as Compose, will
use [Robolectric](https://robolectric.org/). It mocks most of the Android core, allowing those tests
to be run locally.

The tests can be run with `./gradlew test`, and will be run as part of `./gradlew check`
and `./gradlew build`

## Screenshot tests

[Roborazzi](https://github.com/takahirom/roborazzi) is used to support screenshot testing. It uses
Robolectric to generate the screenshots with your local JVM, allowing it to run very quickly. The
tests depend on the serialized Figma files in the `<package>/src/main/assets/figma` directory being
up to date. If they are out of date then you may not get an accurate test result.

Roborazzi is implemented as part of the unit tests in `<package>/src/testDebug`. The tests
themselves will run if you run `./gradlew test`, but screenshots will only be checked if you
run `./gradlew verifyRoborazziDebug`. This command has been added to the `./dev-scripts/test-all.sh`
command, so you don't need to run it separately if you run `test-all`.

If `verifyRoborazziDebug` fails then you can run `compareRoborazziDebug` to generate image diffs. If
after reviewing these you determine that the change is acceptable you can regenerate the screenshots
using `recordRoborazziDebug`. (Note that this will regenerate all images from all tests).

## Testing Figma file rendering

Start with
the [Compose testing documentation](https://developer.android.com/jetpack/compose/testing) for the
basics of Compose testing. The below should apply to both unit and integration (AndroidInstrumented)
tests.

The tests require a `composeTestRule`, though there are two types. The basic one, `createComposeRule()` should be the default choice, as it starts up faster. The alternate is `createAndroidComposeRule<ComponentActivity>()`, for tests (such as ones with Roborazzi) that require access to a running activity.

The DesignDoc to be tested is set via `composeTestRule.setContent()`. For example, HelloWorld's
basic test uses this line:

```kotlin
class RenderHelloWorld {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testHello() {
        composeTestRule.setContent { HelloWorldDoc.mainFrame(name = "Testers!") }
        ...
    }
}
```

To check that the doc was rendered (meaning was read, deserialized, and displayed): assert that a
node with the semantic `docIdSemanticsKey` with the value equalling your file's fileId exists. From
HelloWorld:

```kotlin
composeTestRule
    .onNode(SemanticsMatcher.expectValue(docIdSemanticsKey, helloWorldDocId))
    .assertExists()
```

### Marking screenshots

Add `.captureRoboImage(<filename>)` to any compose `onNode()` result to take a screenshot there. For
example, to capture a screenshot of the HelloWorld doc, add this:

```kotlin
composeTestRule
    .onNode(SemanticsMatcher.expectValue(docIdSemanticsKey, helloWorldDocId))
    .captureRoboImage("helloWorld.png")
```

All tests should output images to `<package>/src/testDebug/roborazzi`. They should also capture an
image of the last state of the test. These can be accomplished with a `RoborazziRule`,
see `reference-apps/helloworld/src/testDebug/kotlin/RenderHelloWorld.kt` for the current options
used. Copy that code block into any tests you create that use Roborazzi, otherwise the screenshots
will be saved in the default Roborazzi location.

**Remember**, screenshots are **only captured** when running `./gradlew captureRoborazziDebug` and checked
when running `./gradlew verifyRoborazziDebug`.

# Rust tests

Simply run `cargo test` to run the unit tests for the rust code. There are no integration tests at this time.

# Running all tests

The `./dev-scripts/test-all.sh` script will trigger all tests in the repo. This script must pass
before a release candidate can be cut.

## Prerequisites

- Make sure your system can run Android Emulators
- Check out the current supported branch of the AAOS Unbundled repo (see the "Check out the
  Unbundled AAOS Repo" job in `.github/workflows/main` for the correct branch)
- Set `$FIGMA_ACCESS_TOKEN` to your Figma token or have it set in ~/.config/figma_access_token

The test-all script takes an optional `-s` flag to skip all emulator tests. It's intended for
situations where emulators can't be started.

# What tests to run

Here's a tl;dr:

## To run unit tests

```shell
cargo test && ./gradlew test
```

## To test code that modifies anything related to fetching or serializing docs

`./gradlew gmdTestQuick`

## To test code related to rendering docs

Make sure your saved serialized files (`src/main/assets/figma/*`) are up to date, then run:

`./gradlew verifyRoborazziDebug`

If you there are differences and they're acceptable, run:

`./gradlew captureRoborazziDebug`

## For a reasonable amount of tests that will finish in a reasonable amount of time

```shell
cargo test && ./gradlew test gmdTestQuick verifyRoborazziDebug
```

## When your code is ready for review

First run `./dev-scripts/format-all.sh` to make sure everything is formatted.

Then run:

```shell
./dev-scripts/test-all.sh
```
