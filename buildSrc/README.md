# Gradle buildSrc directory

This is a buildSrc directory, which is used to collect common Gradle configuration and tasks that can be applied to one or more subprojects.

[Gradle's documentation](https://docs.gradle.org/current/userguide/sharing_build_logic_between_subprojects.html) recommends sharing build logic via [pre-compiled script plugins](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins) stored in the buildSrc directory. Gradle refers to these as [convention plugins](https://docs.gradle.org/current/samples/sample_convention_plugins.html#compiling_convention_plugins).

BuildSrc is essentially an includedBuild, and everything here will be compiled before configuring the rest of the project. This does add a bit of initial started time to the build, unfortunately.

The individual script plugins are `*.gradle.kts` files. They're written with the [Kotlin Gradle DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html) and you can essentially think of them as configuration that'll be applied directly to the projects.

Each `.gradle.kts` file gets compiled into it's own plugin that can be applied separately. The name of the plugin is determined by the script's package and file name. For instance, `buildSrc/src/main/kotlin/designcompose/conventions/publish/common.gradle.kts` has the package declaration `package designcompose.conventions.publish` so it's plugin name is `designcompose.conventions.publish.common`. It can be applied to any project in plugins block of that project. For instance, the `designcompose` project includes this to use the android version of the plugin:

```kotlinscript
plugins {
    id("designcompose.conventions.publish.android")
}
```

Some functionality is too complicated to define using the DSL, so the plugin is written in a regular `.kt` file. The compiled plugin is applied to the subprojects by an accompanying `.gradle.kts` file. An example is `buildSrc/src/main/kotlin/designcompose/conventions/AndroidTestDevicesPlugin.kt` and `buildSrc/src/main/kotlin/designcompose/conventions/android-test-devices.gradle.kts`.

## Our plugins

- `designcompose.conventions.publish`
    Common configuration for publishing libraries, split into common, jvm and android versions.
  - `.common`
        Definition of the library group, version and the LocalDir repository. Uses some Gradle-fu to access and use the DesignCompose version in `gradle/libs.versions.toml` within the script.
  - `.jvm` and `.android`
        Includes the `.common` plugin and finished configuration with the JVM and Android-specific components.
        The Android plugin also includes Dokka configuration.
- `android-test-devices`
    Configures multiple Gradle-managed test devices which provide a way to run InstrumentationTests on known Emulator configurations. <https://developer.android.com/studio/test/gradle-managed-devices>
