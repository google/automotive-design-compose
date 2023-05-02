settingsEvaluated {
    val DesignComposeMavenRepo: String by settings

    dependencyResolutionManagement {
        // repositories.clear()
        repositories {
            // val dcrepo = maven(uri(DesignComposeMavenRepo))
            maven(uri(DesignComposeMavenRepo))
        }

        // addFirst{maven(uri(DesignComposeMavenRepo)) }}
        // repositories {
        //     maven(uri(DesignComposeMavenRepo))
        //     google()
        //     mavenCentral()
        // }
    }
}
