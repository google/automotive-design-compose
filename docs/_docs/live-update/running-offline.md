---
title: 'Running Offline'
layout: page
parent: Live Update
nav_order: 2
---

# Saving cached Figma files for offline use

This method will allow you to add a pre-fetched Figma file to your DesignCompose app's assets. This allows the app to run and display your design without a network connection and without a Figma authentication token set.

1. Launch the app as normal in an emulator, set the Figma Token if needed, and allow the Figma file to be fetched and displayed

1. Create a directory in your app's `src/main` directory named `assets/figma`. (See `reference-apps/helloworld/src/main/assets/figma` for an example)

1. Open a terminal and navigate to that directory. One way to do this is to right-click the directory in Android Studio and select **Open In -> Terminal**

    ![Open in terminal](<open-in-terminal.png>)

1. From that directory, use the Android Debug Bridge (adb) to locate the pre-fetched `.dcf` files corresponding to your document.

    1. Gain root access

        ```shell
        $ adb root
        restarting adbd as root
        ```

    1. Locate the pre-fetched Figma files on your device.

        ```shell
        $ adb shell find /data -name *.dcf
        /data/user/0/com.android.designcompose.testapp.helloworld/files/HelloWorldDoc_pxVlixodJqZL95zo2RzTHl.dcf
        ```

        This should return paths similar to this format: `/data/user/0/<your app's applicationId>/files/....dcf`. The path may differ based on yur Android system and if the system uses multiple users. The above command shows the result for the HelloWorld app on a current tablet emulator.

        - Alternatively you can search for your file's ID specifically by replacing `*.dcf` with `*YOUR_DOC_ID.dcf`

1. From the same `assets/figma` directory, pull the file to your local machine

    ```shell
    $ adb pull <path to the file on your device>
    /data/user/0/com.android.designcompose.testapp....led, 0 skipped. 3.4 MB/s (2774 bytes in 0.001s)
    ```

1. Rebuild and launch your app

    The app will immediately display the cached design after launching.

1. Optional: Disable Live Update and the DesignSwitcher

    In your app's main activity, find and delete the line `DesignSettings.enableLiveUpdates(this))`

    This will disable the DesignSwitcher widget and the rest of the Live Update system.
