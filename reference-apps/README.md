# Automotive Design for Compose Reference Apps

The directories here contain reference apps for DesignCompose. They are:

- Tutorial: A walkthrough of the DesignCompose SDK and it's features
- HelloWorld: A simple app showing the minimum for a working DesignCompose app
- AAOS-Unbundled: Contains the MediaCompose demonstration app, demonstrating a Media Center on Android Automotive OS (AAOS). This uses APIs from AAOS, which are released as part of the (Unbundled Apps)[https://source.android.com/docs/devices/automotive/unbundled_apps/integration] repository, and ******************************




# Building

Each project is largely self-contained in it's own Gradle project. Tutorial and HelloWorld are both included in the main DesignCompose Gradle project, and can be built there. The AAOS-Unbundled project depends on Unbundled apps which requires a specific version of the Android Gradle Plugin, and therefore cannot  be included in the DesignCompose Gradle project.

## Depending on the DesignCompose SDK

There are three ways that the reference apps can use the SDK:

### Fetch from published SDK

This is the default build configuration, the libraries for the SDK will be fetched from the Google Maven repository. Note that the reference apps may not be compatible with the latest released SDK

### Use a specified pre-built local Maven repository

This is primarily for CI testing. Set the `DesignComposeMavenRepo` Gradle property to the location of the local Maven repository and run the build with the `local-design-compose-repo.init.gradle.kts` init script. Example for the tutorial app:
```
cd reference-apps/tutorial
./gradlew --init-script=../local-design-compose-repo.init.gradle.kts build
```

### Rebuild and use the SDK from source (AAOS Unbundled Only)

As stated above, the Unbundled project cannot be included in the DesignCompose Gradle project. To facilitate testing with the source from DesignCompose, set the `designComposeAAOSUnbundledUseSource` Gradle property to `true` and set `DesignComposeMavenRepo`. The SDK will be re-built each time the reference app's Gradle project is used.
