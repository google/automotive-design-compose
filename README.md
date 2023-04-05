# Automotive Design for Compose

- [Automotive Design for Compose](#automotive-design-for-compose)
  - [Introduction](#introduction)
  - [Getting Started](#getting-started)
    - [Android Studio](#android-studio)
    - [Android NDK](#android-ndk)
    - [Rust](#rust)
    - [Rust Toolchains](#rust-toolchains)
    - [Python](#python)
    - [Installing the Automotive Design for Compose Figma Plugin and Widget](#installing-the-automotive-design-for-compose-figma-plugin-and-widget)
  - [Running the Tutorial App](#running-the-tutorial-app)
  - [Building your own app](#building-your-own-app)
  - [Source Layout](#source-layout)
  - [Get in touch](#get-in-touch)

## Introduction

Automotive Design for Compose (also called DesignCompose in the source) is an extension to [Jetpack Compose](https://developer.android.com/jetpack/compose) that allows every screen, component, and overlay of your Android App to be defined in [Figma](https://www.figma.com), and lets you see the latest changes to your Figma design in your app, immediately!

To use Automotive Design for Compose in an app, a developer specifies the Composables that they’d like to be defined by Figma, and a designer uses Figma to draw them. Most Figma features, including Auto Layout, Interactions, Variants, and Blend Modes are fully supported. This repo includes the DesignCompose library, an interactive tutorial app (in reference-apps/Tutorial), and a sample customizable Media Center for Android Automotive OS (in reference-apps/aaos-unbundled).

Find our documentation on the
[Android Automotive partner website](https://docs.partner.android.com/automotive/customize/designcompose).

## Getting Started

Automotive Design for Compose is currently only available as source code. Stay with us as we get closer to our 1.0 release and add more documentation, begin publishing releases, and make things even easier to use!

If you'd like to begin working with Automotive Design for Compose or would like to run the Tutorial App, you will need to perform some initial setup:

### Android Studio

Currently testing with Android Studio Electric Eel | 2022.1.1

### Android NDK

Version `25.2.9519653` is required. It can be installed via [Android Studio's SDK Manager](https://developer.android.com/studio/projects/install-ndk#specific-version) or on the command line using the `sdkmanager` binary.

```shell
"${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --install "ndk;25.2.9519653"
```

### Rust

Rust 1.63.0 is required to compile DesignCompose's Live Update service. Install at [rustup.rs](https://rustup.rs/). For working with the code, we recommend the `rust-analyzer` plugins for VS Code and Android Studio / IntelliJ.

Android Studio will not pick up on a new Rust install without restarting the IDE. In addition, if you launch Android Studio via desktop shortcut or similar (i.e. not from the command line), then it still may not detect the install. In this case you can log out of your computer and back in, or simply restart your computer.

### Rust Toolchains

The DesignCompose library includes a Rust JNI library, which requires Rust build target toolchains. Install them by running the following from the root of the repository

```shell
./install-rust-toolchains.sh
```

### Python

Python3 must be installed and available on your path as `python`. You can configure this by installing `python-is-python3`:

```shell
apt install python-is-python3
```

### Installing the Automotive Design for Compose Figma Plugin and Widget

The DesignCompose Plugin and Auto Content Preview widget are needed to enable additional layout options and features. Check your Figma account to see if they've already been installed for you. If not, you'll need to build and install them. You can build on any system, but Figma only supports plugin installation via the [Figma Desktop App](https://www.figma.com/downloads/), which only runs on macOS and Windows.

There are two packages that are needed, the Extended Layout plugin and Auto Content Preview widget. Both are located in the `support-figma` directory. Build each by running the following: (you'll need `nodejs` and `npm` installed on your system)

```shell
npm install
npm run build
```

Then open the Figma Desktop app, go to **Plugins** -> **Development** -> **Import plugin from Manifest** and select the `manifest.json` file to import.

## Running the Tutorial App

The DesignCompose Tutorial app shows you the capabilities of DesignCompose through a series of interactive examples.  You will need a Figma account and [personal access token](https://help.figma.com/hc/en-us/articles/8085703771159-Manage-personal-access-tokens) to view the Tutorial Figma file and a large-screen device to run it on.

You'll work with your own copy of the Tutorial Figma file. Create it by importing `reference-apps/tutorial/DesignComposeTutorial.fig` to your Figma account. Once uploaded, you'll need to identify the Figma Document ID from your new file's URL. It's the alphanumeric string between `file/` and the name of the document For example:

<pre><code>figma.com/file/<b>ABCDEFG123</b>/File-name</code></pre>

The app's Gradle project is `:reference_apps:tutorial` within the main DesignCompose Gradle project. Build and launch it on your device, then set your Figma Access Token on the app by running:

```shell
FIGMA_ACCESS_TOKEN=<YOUR_ACCESS_TOKEN> \
    ./gradlew :reference_apps:tutorial:setFigmaTokenDebug
```

Next,  switch the app to use your copy of the Tutorial file by clicking the dropdown arrow in the upper right. This will open the Design Switcher.

![Collapsed Design Switcher](docs/design-switcher-collapsed.png)

Click the **Change** button to switch document IDs, and enter the ID of your copy of the Tutorial Figma File. Click **Load** and the app will start fetching your file (it'll take about a minute)

While that's loading, open your copy of the Tutorial file on Figma and find the **Getting Started** box (it's at the top with a pointer pointing to it).

![Find Getting Started](docs/TutorialGettingStarted.png)

Zoom in on it and begin your tutorial!

![Getting Started Frame](docs/GettingStartedFrame.svg)

## Building your own app

Automotive Design for Compose is not yet being published in Maven. For now you can use the `publishAllPublicationsToLocalDirRepository` Gradle task to build a local copy of the libraries or include the project in a [Gradle composite build](https://docs.gradle.org/current/userguide/composite_builds.html).

We'll be adding more documentation and guides soon!

## Source Layout

Automotive Design for Compose consists of several components:

- The Jetpack Compose renderer of Automotive Design for Compose documents consists of several modules:

  - `annotation` contains the Kotlin annotation definitions like `@DesignDoc` and `@DesignComponent`.

  - `codegen` contains the Kotlin compiler plugin that processes the annotations and generates stub Composables that use the Automotive Design for Compose runtime.

  - `designcompose` contains the code that interprets Automotive Design for Compose documents and renders them using Jetpack Compose. It also contains the code that uses the Figma Import JNI library to fetch documents from the Figma webservice.

- Figma Import, in `crates/figma_import`, is a library implemented in Rust that fetches documents and resources from the Figma API and generates a serialized document containing only the information that Automotive Design for Compose needs. The JNI interface used by the Android runtime is in `crates/live_update`.

- Figma plugins that give designers more control and a better experience using Figma with Automotive Design for Compose:

  - The Extended Layout Plugin, in `support-figma/extended-layout-plugin` , provides a panel for formatting text, a panel to provide a JSON file with keyword details, a panel to validate keyword usages against the provided JSON file, and a command to sync Figma's prototype settings to the main document.

  - Auto Content Preview Widget, in `support-figma/auto-content-preview-widget` provides a Figma widget that uses the JSON file and allows designers to create and preview complex list layouts.

- A Validation app in `integration-tests` is used for visually validating changes

- The Tutorial app in `reference-apps` provides an overview of DesignCompose

- The `reference-apps/aaos-unbundled` directory contains a separate Gradle project that includes demonstrations of DesignCompose with Android Automotive OS Apps, such as a MediaCenter app.

## Get in touch

- Report a bug: <https://github.com/google/automotive-design-compose/issues>
- Say hello:
    [aae-design-compose@google.com](mailto:aae-design-compose@google.com)
