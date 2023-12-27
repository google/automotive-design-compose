# Automotive Design for Compose Reference Apps

The directories here contain reference apps for DesignCompose. They are:

- Tutorial: A walkthrough of the DesignCompose SDK and it's features
- HelloWorld: A simple app showing the minimum for a working DesignCompose app
- AAOS-Unbundled: Contains the MediaCompose demonstration app, demonstrating a Media Center on Android Automotive OS (AAOS). This uses APIs from AAOS, which are released as part of the (Unbundled Apps)[https://source.android.com/docs/devices/automotive/unbundled_apps/integration] repository, and requires an AAOS device or emulator to run.

# Building

All three apps are included in the core DesignCompose Gradle project and should be built from and developed there.

Each app also has their own standalone project, allowing them to be built using published DesignCompose releases and without needing to build the DesignCompose SDK.

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

