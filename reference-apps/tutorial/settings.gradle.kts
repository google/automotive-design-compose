rootProject.name = "DesignCompose Tutorial App"


@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    // Use the same version catalog that we use for the core SDK
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
            // Use the latest published version of the SDK
            version("designcompose", "+")
        }
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(":app")