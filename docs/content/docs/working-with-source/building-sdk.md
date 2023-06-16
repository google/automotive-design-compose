---
layout: 'layouts/docs.njk'
eleventyNavigation:
  key: Building the SDK
  parent: Working with Source
  order: 1
---

# Build the SDK from source

This page outlines how to download and build the Automotive Design for Compose
source.
## Install required dependencies {:#InstallDependencies}

Additional dependencies are required to compile the SDK.

### Android Studio {:#AndroidStudio}

The source requires Android Studio Flamingo or later.

### Android NDK {:#AndroidNDK}

Version `25.2.9519653` is required. You can install the NDK with Android
Studio's SDK Manager or on the command line using the sdkmanager binary:

```posix-terminal
"${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" \
    --install "ndk;25.2.9519653"
```

### Rust {:#InstallRust}

Rust 1.63.0 is required to compile DesignCompose's Live Update service.
Installation instructions are on the [Rust installer
page](https://rustup.rs){:.external}.

To complete the Rust installation the Android Studio IDE must be aware of the
new install. The simplest way to do this is to reboot your computer.

### Rust toolchains {: #RustToolchains}

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

## Check out the code {:#GetSource}

To check out the code, run the following:

```posix-terminal
git clone https://github.com/google/automotive-design-compose.git
```

## Build the code {:#BuildCode}

Instructions are provided for building DesignCompose and it's reference apps
using both Android Studio and the command line. The examples below describe how
to build the Tutorial app.

### Android Studio {:#BuildInAndroidStudio}

To build the code in Android Studio:

1.  Open the root of the DesignCompose repository in Android Studio.

    Android Studio syncs with the code and populates the **run configurations**
    menu with entries for each of the included apps.

1.  Build the Tutorial app using either the basic or advanced instructions in
    [Build and run your app](/studio/run) The Tutorial app is listed in the
    **run configurations** menu as `reference-apps:tutorial`.

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

    The apps are built in the normal build output directories. For example, the
    Tutorial app's debug variant are located in
    `reference-apps/tutorial/build/outputs/apk/debug/tutorial-debug.apk`
