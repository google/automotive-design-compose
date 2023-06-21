---
title: Media Center App
layout: page
has_children: true
nav_order: 5
has_toc: false
---

{% include toc.md %}

# Media Center Demonstration App

This page explains how to use the Media Center demonstration app, which uses the
DesignCompose library to create a fully functional Android Automotive OS (AAOS)
Media Center app.

Note: The Media Center demonstration app is a way for you to get a quick start
to using DesignCompose. If you're looking for documentation about Media Center,
see [Media Center Example Designs][1].

The Media Center demonstration app uses the DesignCompose library to create a
fully functional AAOS Media Center app. Use this app to view demonstrations of
the following:

*   DesignCompose *Live Update* feature, which automatically downloads and
    displays Figma designs as they are updated in Figma. This enables designers,
    engineers, and product managers to quickly refine designs and try out new
    ideas in a car, on a test bench, or in an emulator.

*   Full integration with the AAOS Car Media libraries, allowing control and
    browsing access of installed media apps. To learn more, see [Customizing
    Media][2]{:.external}

## Prerequisites {#prerequisites}

If this is your first time trying out Automotive Design For Compose, we
recommend reading the [Tutorial app guide][3], which describes the initial setup
and shows you what DesignCompose can do.

To run the MediaCompose demonstration app, you need the prerequisites for
[DesignCompose][4] as well as these items:

*   Access to an Android Automotive OS emulator or test device.

    The Media Center demonstration app requires an AAOS image to run. In
    addition, it's intended to be displayed on an 11-inch (or larger) screen
    with a resolution of at least 1024 x 1028. Ensure your emulator's device
    definition meets these minimum requirements.

*   A platform signing key for your Android emulator or test device (see [Enable
    browsing of other media apps][5].

*   The current release of the [Android Automotive OS unbundled
    repository][6]{:.external} (see below)

### AAOS unbundled setup {#aaos-ub-setup}

The Media Center app depends on the AAOS libraries, which are part of the
Android platform. These libraries are available outside of AAOS as part of the
AAOS unbundled repository. Follow these instructions to download them and make
them available to the Media Center app.

1.  Install the `repo` tool, following the instructions in [Installing
    Repo][7]{:.external}
1.  Create a directory and download the AAOS unbundled repositories:

    ```posix-terminal
    mkdir aaos-unbundled

    cd aaos-unbundled

    repo init -u https://android.googlesource.com/platform/manifest -b ub-automotive-master

    repo sync -cq -j16
    ```

    For more information, see the [Unbundled Apps Integration Guide][8]{:.external}

1.  The AAOS unbundled project requires the Android SDK location be set. You can
    do this in one of two ways:
    *   Create
        `aaos-unbundled/packages/apps/Car/libs/aaos-apps-gradle-project/local.properties`
        and then set the `sdk.dir` property it contains. Android Studio can do
        this automatically when opening a project. For example, use
        `sdk.dir=/Users/MY-USERNAME/Library/Android/sdk`.

    *   Set the `ANDROID_SDK_ROOT` environment variable with the path to the
        Android SDK.

1.  Create a [Gradle property][9] called `unbundledAAOSDir` and set it to the
    absolute path to your downloaded repository. The Media Center's Gradle
    project uses this property to find the unbundled libraries.

    A common place to create the property is in your global `gradle.properties`
    file in `~/.gradle/gradle.properties`. (You might need to create the file.)
    For example, if you downloaded the repositories to
    `/data/git/aaos-unbundled`, you would add:

    ```
    unbundledAAOSDir=/data/git/aaos-unbundled
    ```

### Build the DesignCompose SDK {#buildSDK}

The MediaCenter demonstration app uses the compiled DesignCompose SDK. Follow
these instructions to compile it:

1.  Create a Gradle property called `DesignComposeMavenRepo` in the same file as
    the [`unbundledAAOSDir` property][10].

1.  Set the property to the absolute path of a location on your file system of
    your choosing. For example: `/data/git/designcompose/maven-repo`.

1.  Navigate to the root of the DesignCompose repository and run `./gradlew
    publishAllPublicationsToLocalDirRepository`. The task publishes the Maven
    repository to the location defined in `DesignComposeMavenRepo`.

Note: The same Gradle property is used by the MediaCompose Reference app to
locate the built DesignCompose SDK. No additional configuration is needed here.

## Set up Figma {#setUpFigma}

This section describes how to create a Figma Access Token (if one is not already
at hand) and upload the MediaCompose design to your Figma account.

### Create a Figma access token {#CreateToken}

If you haven't already done so (for example, while following the [tutorial
guide][3], create a Figma access token. Follow the instructions in [Setting Up
Figma Authentication ][12].

### Import Media Center Figma documents {#ImportDocsSection}

The following steps describe how to import the Media Center design documents:

1.  Visit [figma.com][13]{:.external} and log in to your account.

1.  Click **Import File** on the main Figma screen. Locate and import the
    `Media1_V2.fig` file in the
    `reference-apps/aaos-unbundled/mediacompose/figma_files` directory.


1.  Open the uploaded design documents and note the **Figma Document ID** in the
    URLs between `file/` and the name of the document.

    For example, the **ID** of the document at
    `https://www.figma.com/file/aabbccdd/Tutorial` is `aabbccdd`.

## Configure the MediaCompose reference app {#ConfigureApp}

In Android Studio, open the
`reference-apps/aaos-unbundled/standalone-projects/mediacompose` directory
(don't create a new project) and wait for the initial synchronization of the
project to finish.

### Enable browsing of other media apps {#MediaContentControl}

The Media Center demonstration app uses third-party media services, which are
secured with the `MEDIA_CONTENT_CONTROL` permission. This permission can be
granted in two ways, by installing the app in the system image as a privileged
app, or signing the app with the [platform key][14]{:.external}
 for the Android system. Installing the app as a privileged app grants access
only to a few built-in media content providers, and the installation procedure
can drastically differ depending on the Android image. Instead, follow the
instructions below to sign the app with a platform key. This enables access to
all other media apps that are installed on the system.

Note: If you're using a generic AOSP Android image then the key that is included
with the Media Center source might work for you. Skip to [Configure the Figma
Document ID in the Media Center app][15].

#### Add the platform signing key {#PlatformKeySection}

Typically, a key is used only to sign a release build. However, the Media Center
app doesn't function without the permissions granted by a platform key.
Therefore, all variants must be signed.

1.  Acquire a copy of a [keystore file][5] that contains the platform signing
    key for your test device or emulator. The key is typically provided as a
    `.keystore` file, with a password for the file and a separate password for
    the key file.

1.  In
    `reference-apps/aaos-unbundled/standalone-projects/mediacompose/app/build.gradle.kts`,
    find the `signingConfigs` block in the `android{}` block. Replace the
    contents of the `signingConfigs` block with the path to your key and the
    key's passwords and alias. You can also change the name of the
    `signingConfig`, which is set in the source to `platform_UNSECURE`.

    ```kotlin
    signingConfigs {
        register("platform_UNSECURE") {
            // You need to specify either an absolute path or include the
            // keystore file in the same directory as the build.gradle.kts file.
            storeFile = rootProject.file('my-platform-key.jks')
            storePassword = *******
            keyAlias = "platform"
            keyPassword = ********
        }
    }
    ```

1.  The Media Center's app's source already sets `signingConfig` to be used by
    all builds with the block below. If you changed the name of the config from
    `platform_UNSECURE`, then update it here as well.

    ```kotlin
    buildTypes {
        all {
            signingConfig = signingConfigs.getByName("platform_UNSECURE")
        }
    }
    ```

To learn more about signing configurations, see [Configure Gradle to sign your
app][17].

### Configure the Figma document ID in the Media Center app {#SetDocID}

Update the Media Center app to load your copy of the Figma Design:

1.  Open the app's `MainActivity.kt` file, located within the Android project at
    `MediaCompose/mediacompose/java/com/android/designcompose/reference/mediacompose/MainActivity.kt`.

    The actual file location is
    `reference-apps/aaos-unbundled/standalone-projects/mediacompose/app/src/main/java/com/android/designcompose/reference/mediacompose/MainActivity.kt`.

1.  Find the following line:

    ```kotlin
    @DesignDoc(id = "<A Figma Document ID>", version = "0.1")
    ```

1.  Replace the document ID with the **Figma Document ID** of the Media Center
    Figma File that you uploaded previously. (See [Import Media Center Figma
    documents][18] for the document IDs).

## Run the Media Center app and set your Figma access token {#RunApp}

1.  Build and launch the app on your device. You can do this with [Android
    Studio][19]{:.external} or manually using Gradle.

    Warning: If the app launches and immediately crashes, this might indicate
    that the proper platform key hasn't been configured to sign the app. If this
    is the case, then the crash log in logcat includes the error
    `java.lang.SecurityException: requires
    android.permission.MEDIA_CONTENT_CONTROL`.

1.  Set your Figma access token in the app by running the `./gradlew
    setFigmaTokenDebug` task from the
    ``reference-apps/aaos-unbundled/standalone-projects/mediacompose` directory.

    *   The task reads the token from the `$FIGMA_ACCESS_TOKEN` environment
        variable that was set in [Storing Your Figma Access Token][20].

    *   Alternatively, you can set the token in one command by running:

    ```posix-terminal
    FIGMA_ACCESS_TOKEN=<YOUR_ACCESS_TOKEN> ./gradlew setFigmaTokenDebug
    ```

1.  After this, the app fetches the Figma Design document. The rendered design
    typically appears within one minute.

### Explore the app! {#ExploreApp}

The app is up and running. You're welcome to experiment with it by playing
different audio, or changing audio sources. Open the Media Center Figma file
that you uploaded previously on Figma.com, make modifications, and see how
they're rendered in the app.

You can also load and see the alternative designs that are included with the
source. See [Media Center Example Designs][21] for more on about those designs.

## Convert the app to static production mode {#ProductionMode}

When the design of an app is stable, you can convert it to a static,
production-ready app that doesn't support Live Update and doesn't need network
access to run.

### Add the Figma documents to the app source {#AddDocsToSource}

A Live Update-enabled app fetches the Figma documents and stores them as a
serialized file in the app's storage. You can download these files and add them
to the app's source for inclusion in the APK.

1.  Build and run the app. If necessary, sign in to the app and allow the app to
    fetch and render the Figma document to the screen.

1.  If the app uses multiple Figma documents, then you must navigate to each
    screen that uses a separate document and allow the document to fetch and
    render.

1.  Create a directory in
    `reference-apps/aaos-unbundled/standalone-projects/mediacompose/app/src/main`
    named `assets` and a directory in `assets` named `figma`.

1.  Open the **Device File Explorer** tool window in Android Studio and go to
    `/data/user`. This directory contains one or more numbered directories.

1.  Open the directory named `10`. If a `10` directory doesn't exist, open the
    directory named `0`. Within this directory, locate the folder for the Media
    Center app, which is named after the app's package name
    ('com.android.designcompose.reference.mediacompose`). See below if the Media
    Center directory can't be found.

    Alternatively, use `adb` to directly access a shell on the device and run
    the `find` command, searching for a portion of the app's package name. For
    example:

    ```posix-terminal
    $ adb root
    restarting adbd as root
    $ adb shell
    generic_car_x86_64:/ # find /data/user -name "*designcompose*"
    /data/user/10/com.android.designcompose.reference.mediacompose
    ```

1.  Open the `files` directory, which contains the cached Figma documents, named
    `<Doc Name>_<Doc ID>` with no file extension.

1.  Download the cached documents and copy them into the new assets directory
    created earlier
    (`reference-apps/aaos-unbundled/standalone-projects/mediacompose/app/src/main/assets/figma`).

### Deactivate Live Update {#DisableLiveUpdate}

You can deactivate Live Update.

1.  Open your project's `MainActivity` file. (See [Configure the Figma document
    ID in the Media Center app][22]).

1.  Remove the line `DesignSettings.enableLiveUpdates(this)` from the
    `MainActivity.onCreate` function.

1.  Build and run the app. The app displays the rendered design and the Document
    Switcher widget in the upper right corner is not displayed. The project's
    APK files can be installed on any device, with or without internet access.

## Common issues {#Troubleshooting}

### App crashes on startup with `requires android.permission.MEDIA_CONTENT_CONTROL` {#StartupCrash}

This error occurs when the app was not signed with the platform key or otherwise
authenticated as a system app. Ensure that the key being used to sign the app is
the platform key for the emulator or device that you are installing on.

[1]: /docs/media-center-demo/example-designs
[2]: https://source.android.com/docs/devices/automotive/hmi/media/
[3]: /docs/tutorial/index
[4]: /docs/index
[5]: #MediaContentControl
[6]: https://source.android.com/docs/devices/automotive/unbundled_apps/integration
[7]: https://source.android.com/docs/setup/download#installing-repo
[8]: https://source.android.com/docs/devices/automotive/unbundled_apps/integration#check-out
[9]: https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties
[10]: #aaos-ub-setup
[12]: /docs/live-update/setup
[13]: https://www.figma.com
[14]: https://source.android.com/devices/tech/ota/sign_builds#certificates-keys
[15]: #SetDocID
[17]: https://developer.android.com/studio/build/building-cmdline#gradle_signing
[18]: #ImportDocsSection
[19]: https://developer.android.com/studio/run
[20]: /docs/live-update/setup#StoreFigmaToken
[21]: /docs/media-center-demo/example-designs
[22]: #SetDocID
