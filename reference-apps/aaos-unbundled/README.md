# Android Auto OS Unbundled Apps

The apps contained in this project require Android libraries that are contained in the public Android Auto OS Unbundled releases. 

## Included Apps

### MediaCompose

Provides a demonstration of a Media Center app for an Android Automotive OS device

## Initial setup

The releases can be downloaded by following the instructions on the [Integration Guide](https://source.android.com/docs/automotive/unbundled_apps/integration#check-out). 

Navigate to `packages/apps/Car/libs/aaos-apps-gradle-project` within the Unbundled checkout. Run `./gradlew publishAllPublicationsToLocalRepository` to build the libraries.

The Unbundled apps such as MediaCompose locate the libraries using the `unbundledAAOSDir` Gradle property, which is most commonly set in `~/.gradle/gradle.properties`. Set it to the absolute path of the root of the AAOS Unbundled checkout (not to the path to `aaos-apps-gradle-project`).