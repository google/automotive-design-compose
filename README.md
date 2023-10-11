# Automotive Design for Compose

- [Automotive Design for Compose](#automotive-design-for-compose)
    - [Introduction](#introduction)
    - [Getting Started](#getting-started)
    - [Building your own app](#building-your-own-app)
- [Working with the Source](#working-with-the-source)
    - [SDK build dependencies](#sdk-build-dependencies)
        - [Android Studio](#android-studio)
        - [Android NDK](#android-ndk)
        - [Rust](#rust)
        - [Rust Toolchains](#rust-toolchains)
    - [Source Layout](#source-layout)
    - [Building additional resources](#building-additional-resources)
        - [Building the Automotive Design for Compose Figma Plugin and Widget](#building-the-automotive-design-for-compose-figma-plugin-and-widget)
    - [Get in touch](#get-in-touch)

[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/google/automotive-design-compose/badge)](https://api.securityscorecards.dev/projects/github.com/google/automotive-design-compose)

## Introduction

Automotive Design for Compose (also called DesignCompose in the source) is an extension
to [Jetpack Compose](https://developer.android.com/jetpack/compose) that allows every screen,
component, and overlay of your Android App to be defined in [Figma](https://www.figma.com), and lets
you see the latest changes to your Figma design in your app, immediately!

To use Automotive Design for Compose in an app, a developer specifies the Composables that they’d
like to be defined by Figma, and a designer uses Figma to draw them. Most Figma features, including
Auto Layout, Interactions, Variants, and Blend Modes are fully supported. This repo includes the
DesignCompose library, an interactive tutorial app (in reference-apps/Tutorial), and a sample
customizable Media Center for Android Automotive OS (in reference-apps/aaos-unbundled).

[Click here for our documentation!](https://google.github.io/automotive-design-compose/)

## Getting Started

The DesignCompose Tutorial app shows you the capabilities of DesignCompose through a series of
interactive examples. You will need a Figma account
and [personal access token](https://google.github.io/automotive-design-compose/docs/live-update/setup)
to view the Tutorial Figma file and a large-screen device to run it on.

You'll work with your own copy of
the [Tutorial Figma file](https://www.figma.com/community/file/1228110686419863535/Tutorial-for-Automotive-Design-for-Compose).
Create your own by clicking **Open in Figma**, which creates a copy of the file in your account.
Once open, identify the Figma Document ID from your new file's URL. It's the alphanumeric string
between `file/` and the name of the document. For example:

<pre><code>figma.com/file/<b>ABCDEFG123</b>/File-name</code></pre>

The app's Gradle project is located in `reference-apps/tutorial`. Build and launch it on your
device, then set your Figma Access Token on the app by running:

```shell
./gradlew setFigmaTokenDebug
```

Next, switch the app to use your copy of the Tutorial file by clicking the dropdown arrow in the
upper right. This will open the Design Switcher.

![Collapsed Design Switcher](docs/design-switcher-collapsed.png)

Click the **Change** button to switch document IDs, and enter the ID of your copy of the Tutorial
Figma File. Click **Load** and the app will start fetching your file (it'll take about a minute)

While that's loading, open your copy of the Tutorial file on Figma and find the **Getting Started**
box (it's at the top with a pointer pointing to it).

![Find Getting Started](docs/TutorialGettingStarted.png)

Zoom in on it and begin your tutorial!

![Getting Started Frame](docs/GettingStartedFrame.svg)

## Building your own app

We'll be adding more documentation and guides soon! For now you can look
to `reference-apps/helloworld` for an an example of a basic app.

# Working with the Source

## SDK build dependencies

DesignCompose's Live Update system uses a native library built in Rust to fetch and serialize your
Figma Documents. You'll need the following to build it and the rest of the SDK.

### Android Studio

Currently testing with Android Studio Flamingo | 2022.2.1

### Android NDK

Version `25.2.9519653` is required. It can be installed
via [Android Studio's SDK Manager](https://developer.android.com/studio/projects/install-ndk#specific-version)
or on the command line using the `sdkmanager` binary.

```shell
"${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --install "ndk;25.2.9519653"
```

### Rust

Rust 1.68.0 is required to compile DesignCompose's Live Update service. Install
at [rustup.rs](https://rustup.rs/). For working with the code, we recommend the `rust-analyzer`
plugins for VS Code and Android Studio / IntelliJ.

Android Studio will not pick up on a new Rust install without restarting the IDE. In addition, if
you launch Android Studio via desktop shortcut or similar (i.e. not from the command line), then it
still may not detect the install. In this case you can log out of your computer and back in, or
simply restart your computer.

### Rust Toolchains

The DesignCompose library includes a Rust JNI library, which requires Rust build target toolchains.
Install them by running the following from the root of the repository

```shell
./install-rust-toolchains.sh
```

## Build optimizations and Gradle properties

### Controlling the architectures to build the Rust libraries for

Release builds of DesignCompose need to include versions of the Rust libraries compiled for all four
current Android ABIs - `x86`, `x86_64`, `armeabi-v7a` and `arm64-v8a`. However local development typically only needs a single ABI, corresponding to their test device / AVD. 
Cargo can (unfortunately) only build a single ABI at a time, so building all four ABIs adds significant time to a build (at least a minute). .

The `designcompose.cargoPlugin.allowAbiOverride` Gradle property enables the ability to override the list of ABIs to build. If set to `true` then the `designcompose.cargoPlugin.abiOverride` property will be read (if set) and used to filter the configured list of ABIs that is configured in the Gradle project. It takes a comma seperated list of ABIs.[gradle.properties](..%2F..%2F..%2F.gradle%2Fgradle.properties)
When building in Android Studio, the `allowAbiOverride` property will allow the build to check the ABI of the device that you are building for (when installing the app or running an instrumented test or similar) and only build the ABI needed for the device. This has priority over the `abiOverride` list that may be provided.

Note: The abi overrides are ignored when building for release. 

### Gradle optimizations

The properties file that is committed already includes the following optimizations:
    - Parallel builds
    - Build caching
    - Configuration caching
        - Android's Jetifier is disabled
    - Increased the JVM memory allotment to 4GB

You can optionally add these to your personal gradle.properties:

    - `org.gradle.configureondemand`: Incubating feature feature that attempts to configure only the projects that are required for a build.
    - `android.experimental.androidTest.numManagedDeviceShards`: When running Gradle Managed Device tests, starts up additional VMs to execute the tests on in parallel


## Source Layout

Automotive Design for Compose consists of several components:

- The Jetpack Compose renderer of Automotive Design for Compose documents consists of several
  modules:

    - `annotation` contains the Kotlin annotation definitions like `@DesignDoc`
      and `@DesignComponent`.

    - `codegen` contains the Kotlin compiler plugin that processes the annotations and generates
      stub Composables that use the Automotive Design for Compose runtime.

    - `designcompose` contains the code that interprets Automotive Design for Compose documents and
      renders them using Jetpack Compose. It also contains the code that uses the Figma Import JNI
      library to fetch documents from the Figma webservice.

- Figma Import, in `crates/figma_import`, is a library implemented in Rust that fetches documents
  and resources from the Figma API and generates a serialized document containing only the
  information that Automotive Design for Compose needs. The JNI interface used by the Android
  runtime is in `crates/live_update`.

- Figma plugins that give designers more control and a better experience using Figma with Automotive
  Design for Compose:

    - The Extended Layout Plugin, in `support-figma/extended-layout-plugin` , provides a panel for
      formatting text, a panel to provide a JSON file with keyword details, a panel to validate
      keyword usages against the provided JSON file, and a command to sync Figma's prototype
      settings to the main document.

    - Auto Content Preview Widget, in `support-figma/auto-content-preview-widget` provides a Figma
      widget that uses the JSON file and allows designers to create and preview complex list
      layouts.

- A Validation app in `integration-tests/validation` is used for visually validating changes

- The Tutorial app in `reference-apps/tutorial` provides an overview of DesignCompose

- The `reference-apps/aaos-unbundled` directory contains a separate Gradle project that includes
  demonstrations of DesignCompose with Android Automotive OS Apps, such as a MediaCenter app.

## Building additional resources

### Building the Automotive Design for Compose Figma Plugin and Widget

The DesignCompose Plugin and Auto Content Preview widget are needed to enable additional layout
options and features. The latest releases are available in the Figma Community and on
our [Figma Community profile](https://www.figma.com/@designcompose). To build and install an older
or customized version, follow these instructions:

You can build on any system, but Figma only supports plugin installation via
the [Figma Desktop App](https://www.figma.com/downloads/), which only runs on macOS and Windows.

There are two packages that are needed, the Extended Layout plugin and Auto Content Preview widget.
Both are located in the `support-figma` directory. Build each by running the following: (you'll
need `nodejs` and `npm` installed on your system)

```shell
npm install
npm run build
```

Then open the Figma Desktop app, go to **Plugins** -> **Development** -> **Import plugin from
Manifest** and select the `manifest.json` file to import.

# Development process

## Updating serialized Figma files

Any changes that include an update to the Figma serialized file version (set in
crates/figma_import/src/serialized_document.rs) will require the committed serialized files to be
updated with new versions that have been fetched with the updated version. At a minimum this
includes any files that are being tested with AndroidIntegratedTests, and most especially the Design
Switcher and the Tutorial's welcome screen, which are special cases.

To update the Design Switcher, temporarily set the `DISABLE_LIVE_MODE` flag in `DesignSwitcher.kt`
to false, then launch any app. If the app did not already have a Figma Token set then you may need
to re-launch it after setting the token. The app will fetch the latest version of the DesignSwitcher
into it's on-device app storage. Replace the current committed DesignSwitcher
file (`designcompose/src/main/assets/figma/DesignSwitcherDoc_Ljph4e3sC0lHcynfXpoh9f.dcf`) with the
new one.

The Tutorial's DesignDoc is set to the main development Figma file, to assist in development of the
app, but the committed serialized file is a separate file that presents a "welcome" page. This
file's ID is `BX9UyUa5lkuSP3dEnqBdJf`. To update the file, fetch the `BX9UyUa5lkuSP3dEnqBdJf` file,
then replace `reference-apps/tutorial/app/src/main/assets/figma/TutorialDoc_3z4xExq0INrL9vxPhj9tl7`
with the serialized file. **The file name will remain the same**,
the `TutorialDoc_3z4xExq0INrL9vxPhj9tl7` fill will contain the serialized `BX9UyUa5lkuSP3dEnqBdJf`
file. The Tutorial app project has an AndroidIntegratedTest to ensure that the correct file is set,
and it will be run as part of running the `./dev-scripts/test-all.sh` script.

## Running Instrumented Tests

All instrumented tests can be run on a running emulator by running "./gradlew connectedCheck".
Additionally they can be run
on [Gradle Managed Devices](https://developer.android.com/studio/test/gradle-managed-devices). These
devices are the standard test targets and all tests must pass on them prior to a release. They are
collected into three gradle tasks:

- `gmdTestQuick` run the tests on
  an [ATD image](https://developer.android.com/studio/test/gradle-managed-devices#gmd-atd), which is
  optimized for instrumented tests.
- `gmdTestStandard` runs the tests on both the ATD and the most current Android image
- `gmdTestAll` runs the tests on all configured Gradle Managed devices,including the above and any
  additional APIs that have been chosen to be tested against.

Note: The first run will have some significant first-time setup as the GMDs are created.

These tests can be accelerated on a sufficiently powerful workstation by enabling test sharding in
your gradle.properties. This will launch multiple instances of the GMDs, allowing the tests to run
in parallel.

```bash
android.experimental.androidTest.numManagedDeviceShards=3 #Up to 4 are supported, though more than 3 may not provide much benefit
```

This can provide significant speedup for instrumented tests. Having shards set to 4 can reduce the
time to run instrumented tests by 75% (Sample test run: `g connectedCheck` ran in
4m24s, `g gmdTestsQuick` with 3 shards ran in 1m10s)

## Roborazzi screenshot tests

[Roborazzi](https://github.com/takahirom/roborazzi) is a new framework that allows for screenshot
testing of Android Apps on your local system. It
uses [Robolectric](https://github.com/robolectric/robolectric), the standard unit testing framework
for Android, to render DesignCompose locally, allowing screenshots to be generated. The screenshots
won't be one-to-one with actual Android devices, but they'll be very close and stable enough for
changes to be detected. The comparison is run using `./gradlew verifyRoborazziDebug`.
See [our documentation](https://google.github.io/automotive-design-compose/docs/working-with-source/writing-tests)
for more information.

## Testing the standalone version of the Tutorial app

The Tutorial app is currently part of two projects: The root project in the root of the repository,
and the tutorial project in `reference-apps/tutorial`. The root project is the one that contains the
entire SDK and our apps and is where you typically develop. The second project is the one that users
following the Tutorial are directed to use. It fetches DesignCompose from gMaven, which means that
it builds much faster and doesn't compile rust code (and doesn't require the rust SDK to be
installed). This means that the standalone Tutorial needs some extra configuration if you want the
standalone Tutorial project to use any unpublished changes to the libraries and plugin.

First, set `$ORG_GRADLE_PROJECT_DesignComposeMavenRepo` to a location such as ~
/.m2repo/designComposeRepo. This will set the location for the SDK and Plugin to be built to and
where the Tutorial all will read them from.
Run  `./gradlew publishAllPublicationsToLocalDirRepository` to build the SDK and plugin to that
location.

Now you can use the `reference-apps/local-design-compose-repo.init.gradle.kts` init script to tell
the Tutorial app where to find the libraries that you built. The script
reads `ORG_GRADLE_PROJECT_DesignComposeMavenRepo` and uses it to configure Gradle to look for the
libraries and plugin from there. You run init scripts by adding `--init-script <path to script>` to
your Gradle command. For
example, `./gradlew --init-script ../local-design-compose-repo.init.gradle.kts assembleDebug`.

NOTE: Android Studio doesn't have a great way to use init scripts. You'll have to build and use the
Tutorial app using the command line. That's fine for quick tests. Any actual development should be
done with the proper root Gradle project, which doesn't need the init script.

## Running all tests

The `./dev-scripts/test-all.sh` script will trigger all tests in the repo. This script must pass
before a release candidate can be cut.

### Prerequisites

- Make sure your system can run Android AVDs
- Have the build dependencies below installed
- Check out the current supported branch of the AAOS Unbundled repo (see the "Check out the
  Unbundled AAOS Repo" job in `.github/workflows/main` for the correct branch)
- Set `$FIGMA_ACCESS_TOKEN` to your Figma token

The test-all script takes an optional `-s` flag to skip all emulator tests. It's intended for
situations where emulators can't be started or when running tests before updating serialized files.

## Before you submit for review

To check that you can pass presubmits and emulator tests before pushing to PR you can do the
following:

  ```shell
  ./dev-scripts/clean-all.sh #Optional, but will ensure you're testing a clean environment.
  ./dev-scripts/format-all.sh
  ./dev-scripts/test-all.sh
  ```

## Release process

1. Create a new release in GitHub, set the tag to "v\<version>" (see previous releases for
   examples). This will trigger a release build action. \
   You can watch it's
   progress [here](https://github.com/google/automotive-design-compose/actions/workflows/release.yml).
2. Once complete the release artifacts will be uploaded to the GitHub release automatically
3. Stage the SDK
    1. On a Google workstation, download the designcompose_m2repo.zip from the release and copy it
       to `/x20/teams/designcompose/release_staging/`
    2. Run gmaven_publisher to stage it:

        ```bash
        /google/bin/releases/android-devtools/gmaven/publisher/gmaven-publisher stage --gfile /x20/teams/designcompose/release_staging/<the m2repo.zip>
        ```

    3. The staged release will be available for additional testing (see go/gmaven for more info).
       You will receive an email explaining how the release can be submitted for publishing and then
       approval. The release will be published publicly immediately after approval
4. Update the widget and plugin
    1. Download the widget and plugin artifacts from the GitHub release onto a system with the Figma
       Desktop app and unzip them
    2. Open the Desktop app and open any doc
    3. Update the Plugin
        1. Open the **Resources** menu(right of the `T` text menu) and switch to the **Plugins** tab
        2. Change the dropdown under the search bar to **Development**
        3. If you already have an entry for the plugin then hover over it, click the `...` menu and
           click **Remove local version**
        4. The plugin will now have a **Locate local version** option, click it, then navigate to
           the `manifest.json` of the plugin artifact you downloaded from GitHub
        5. Click the `...` menu again and click Publish. Add any release notes and click **Publish
           new version**
    4. Do the same as above for the widget (except use the widget menu)
5. Update the Tutorial
    1. Open the Tutorial Figma File and create a new branch
    2. Find each instance of the widget and replace it with the newly published version, matching
       the settings to the current one
    3. Merge the Tutorial branch into the main branch.
    4. Create a new branch of the Tutorial file named after version you're releasing
    5. From the original file, click the **Share** button in the upper right, switch to the *
       *Publish** tab of the window that pops up and **Publish update**
    6. Make any changes necessary, then click **Save**

# Get in touch

- Report a bug: <https://github.com/google/automotive-design-compose/issues>
- Say hello:
  [aae-design-compose@google.com](mailto:aae-design-compose@google.com)
