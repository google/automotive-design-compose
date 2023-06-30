#!/bin/bash

GIT_ROOT=$(git rev-parse --show-toplevel)

export ORG_GRADLE_PROJECT_DesignComposeMavenRepo="$GIT_ROOT/build/test-tutorial-standalone/designcompose_m2repo"

cd "$GIT_ROOT" || exit
./dev-scripts/clean-all.sh
./gradlew publishAllPublicationsToLocalDirRepository

cd "$GIT_ROOT/reference-apps/tutorial" || exit
./gradlew --init-script ../local-design-compose-repo.init.gradle.kts check