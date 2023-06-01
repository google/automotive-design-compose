---
layout: page
title: Tutorial
# permalink: /tutorial
---

# DesignCompose Tutorial App

<img src="./tutorial-doc-1x.png" class="attempt-right" srcset="1x ./tutorial-doc-1x.png, 2x ./tutorial-doc-2x.png">

This page explains how to download and configure the DesignCompose tutorial app.
This is written as a tutorial, so it's important to follow each step.

## About this tutorial {:#about}

The DesignCompose tutorial app shows you the capabilities of DesignCompose
through a series of interactive examples. You need a Figma account to run the
tutorial and a large-screen device to run it on.

## Initial setup {:#InitialSetup}

1.  Acquire an Android device or emulator with a large, portrait-orientation
    screen. If you don't have a physical device handy then you can use a virtual
    device (Android emulator) in Android Studio. We recommend a Nexus 10 device
    (in the Tablet category), running the Android 12 (API 31) release system
    image with the startup orientation set to Portrait.

    ![Creating a Nexus 10
    device](/automotive/customize/designcompose/getting-started/tablet-virt-dev.png)

    **Figure 1.** Android Studio virtual device definition screen.

1.  DesignCompose is only available as source. Follow the instructions in
    [Getting Started](/automotive/customize/designcompose/getting-started/index)
    to download the source and install the required development tools.

1.  The tutorial uses the Live Update feature to synchronize with your Figma
    files. Follow the instructions in [Set Up Figma
    Authentication](/automotive/customize/designcompose/live-update/setup) to
    create a Figma authentication token and be ready to use it.

## Create your copy of the Figma tutorial file {:#CopyTutorialFile}

The tutorial app demonstrates how you can modify a Figma design and see the
results in your running Android app. First, create your own copy of the file to
work with:

1.  Go to the [published Tutorial design file](https://www.figma.com/community/file/1228110686419863535/Tutorial-for-Automotive-Design-for-Compose){:.external}
    and if necessary, log in to your Figma account.

1.  Click **Open in Figma** to create your own copy of the file.

1.  Wait for your copy of the file to open, then note the **Figma Document ID**
    in the URL between `file/` and the name of the document.

    For example, the **ID** of the document at
    `https://www.figma.com/file/aabbccdd/Tutorial` is `aabbccdd`.

## Launch and configure the tutorial {:#LaunchTutorial}

1.  Build and launch the tutorial app on your device. You can do this with
    [Android Studio](https://developer.android.com/studio/run){:.external} or
    manually using Gradle.

1.  Set your Figma access token in the app by running the `./gradlew
    :reference-apps:tutorial:setFigmaTokenDebug` task.

    *   The task reads the token from the `$FIGMA_ACCESS_TOKEN` environment
        variable that was set in [Store Your Figma Access
        Token](/automotive/customize/designcompose/live-update/setup#StoreFigmaToken).

    *   Alternatively, you can set the token in one command by running:

    ```posix-terminal
    FIGMA_ACCESS_TOKEN=<YOUR_ACCESS_TOKEN> \
        ./gradlew :reference-apps:tutorial:setFigmaTokenDebug
    ```

1.  Use the Design Switcher to load your version of the tutorial file.

    *   Click the dropdown arrow in the upper right corner of the tutorial app
        to open the Live Update Design Switcher panel. It should display *Design
    Switcher Online* if your key was set correctly in the previous step.

        ![Dropdown
            arrow](/automotive/customize/designcompose/getting-started/LiveUpdateDropdown.png){:.screenshot}

        **Figure 3.** The Design Switcher settings button.

    *   Click the **Change** button to switch document IDs, entering the ID for
    your copy of the Figma tutorial file .

    *   Click **Load**.  It takes about a minute to do the initial sync.

While that’s loading, open your copy of the tutorial file on Figma and find the
Getting Started box.

![Getting Started](TutorialGettingStarted.png)

**Figure 4.** The location of the tutorial Figma file's Getting Started frame.

Zoom in and begin your tutorial!

![Getting Started Frame](GettingStartedFrame.svg)

**Figure 5.** The Getting Started frame.
