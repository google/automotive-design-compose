layout: page
title: "Getting Started"
permalink: /getting-started

# Getting Started

This page outlines how to download and build the Automotive Design for Compose
source.

## Check out the code {:#GetSource}

To check out the code, run the following:

```posix-terminal
git clone https://github.com/google/automotive-design-compose.git
```

## Install required dependencies {:#InstallDependencies}

DesignCompose is currently only available as source, so there are a number of
dependencies to install.

### Android Studio {:#AndroidStudio}

We are currently testing with Android Studio Electric Eel | 2022.1.1.

### Android NDK {:#AndroidNDK}

Version `25.2.9519653` is required. You can install the NDK with Android
Studio's SDK Manager or on the command line using the sdkmanager binary:

```posix-terminal
"${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" \
    --install "ndk;25.2.9519653"
```

### Rust {:#InstallRust}

Rust 1.63.0 is required to compile DesignCompose’s Live Update service.
Installation instructions are on the [Rust installer
page](https://rustup.rs){:.external}.

To complete the Rust installation the Android Studio IDE must be aware of the
new install. The simplest way to do this is to reboot your computer.

### Rust toolchains {:RustToolchains}

The DesignCompose library includes a Rust JNI library, which must be compiled
for multiple system types. Install the toolchains that DesignCompose needs by
running the following:

```posix-terminal
rustup target add \
    armv7-linux-androideabi \
    i686-linux-android \
    aarch64-linux-android \
    x86_64-linux-android
```

### Python {:InstallPython}

Python3 must be installed and available on your path as `python`. On Linux
systems you can configure this by installing `python-is-python3`:

```shell
apt install python-is-python3
```

### NodeJS {:InstallNodeJS}

Use `apt-get install nodejs npm` to install NodeJS, which is required to compile
Figma plugins.

## Build the code {:#BuildCode}

Instructions are provided for building DesignCompose and it's reference apps via
both Android Studio and the command line. In the examples we will build the
Tutorial app.

### Android Studio {:#BuildInAndroidStudio}

To build the code in Android Studio:

1.  Open the root of the DesignCompose repository in Android Studio.

    Android Studio syncs with the code and populates the **run configurations**
    menu with entries for each of the included apps.

1.  Build the Tutorial app using either the basic or advanced instructions in
    [Build and run your app](https://developer.android.com/studio/run){:.external}
     The Tutorial app will be listed in the **run configurations** menu as
    `reference-apps:tutorial`.

### Command line {:#BuildWithCommandLine}

To build the code from the command line:

1.  Set the Android SDK location in one of these two ways:

    *   Create `local.properties` in the root of the repository and then set the
    `sdk.dir property` it contains. Android Studio can do this automatically
        when opening a project. For example, use `sdk.dir=$HOME/Android/Sdk`.

    *   Set the `ANDROID_SDK_ROOT` environment variable with the path to the
        Android SDK.

1.  Open a command prompt or a shell window.

1.  Go to the root of the DesignCompose repository.

1.  Run the following command:

    ```posix-terminal
    ./gradlew reference-apps:tutorial:assembleDebug
    ```

    The apps will be built in the normal build output directories. For example,
    the Tutorial app's debug variant will be located in
    `reference-apps/tutorial/build/outputs/apk/debug/tutorial-debug.apk`
