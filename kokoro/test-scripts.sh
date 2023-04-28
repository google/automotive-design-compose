#!/bin/bash
# Copyright 2023 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


# Fail out on any error
set -e

# Make sure we do our wrapup before exiting
trap dc_kokoro_wrapup EXIT

# Set up paths that are required for certain gradle tasks
export ORG_GRADLE_PROJECT_DesignComposeMavenRepo="${KOKORO_ARTIFACTS_DIR}/build/designcompose_m2repo"
export ORG_GRADLE_PROJECT_unbundledAAOSDir="${KOKORO_ARTIFACTS_DIR}/git/ub-automotive-master"

#Convenience
export DESIGNCOMPOSE_DIR="$KOKORO_ARTIFACTS_DIR/git/designcompose"

# Make sure we always have an artifacts directory
mkdir -p "${KOKORO_ARTIFACTS_DIR}/artifacts"

# Used with `dc_organize_gradle_test_results`, append directories to this to add them to the list
# directories to be parsed
export GRADLE_TEST_RESULT_DIRS=()

# Kokoro VM's don't have hardware graphics so we need to set them to use SwiftShader
export GRADLE_TESTOPTIONS_OPTS="-Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect"

###### Utility and Setup functions

# Find test results that were produced by Gradle, place into a common directory and
# rename so that Sponge2 can find and parse them
# Intended to be run after Gradle tests. Tests that create gradle test results
# should add the base of their gradle project to `$GRADLE_TEST_RESULT_DIRS`
dc_organize_gradle_test_results() (
    # Subshell this function to keep the shopts from leaking

    shopt -s nullglob    # Non-matching globs are removed  ('*.foo' => '')
    shopt -s nocaseglob  # Case insensitive globs
    shopt -s dotglob     # Wildcards match dotfiles ("*.sh" => ".foo.sh")
    shopt -s globstar    # Allow ** for recursive matches ('lib/**/*.rb' => 'lib/a/b/c.rb')

    # Sponge can parse our test results, but only if they're XML and their name ends with
    # "_sponge_log.xml". The below copies the results that we want to a test log directory
    # and appends "_sponge_log.xml"
    TEST_LOG_DIR="${KOKORO_ARTIFACTS_DIR}/gradle-test-results"
    mkdir -p "${TEST_LOG_DIR}"

    for dir in "${GRADLE_TEST_RESULT_DIRS[@]}"; do
        cd "$dir" || exit
        for result in **/build/{outputs/androidTest-results,reports}/**/*.xml; do
            PROJ_DIR="${result%build*}"
            NEW_NAME=$(basename "$result")"_sponge_log.xml"

        mkdir -p "$TEST_LOG_DIR/$PROJ_DIR"
        cp "$result" "$TEST_LOG_DIR/$PROJ_DIR/$NEW_NAME"
        done
    done
)

# Fetch the latest release of AAOS Unbundled for use by other tasks
dc_fetch_aaos_unbundled() {
    if [[ -d "$ORG_GRADLE_PROJECT_unbundledAAOSDir" ]]; then
        echo "Test configuration warning, Unbundled AAOS Dir already exists"
        return
    fi

    # Download the Unbundled AAOS repo
    HOST=https://android.googlesource.com/platform/superproject
    BRANCH=ub-automotive-master-20230303
    mkdir "$ORG_GRADLE_PROJECT_unbundledAAOSDir"
    git clone "$HOST" "$ORG_GRADLE_PROJECT_unbundledAAOSDir" --branch="$BRANCH" \
    --single-branch --filter=blob:none --recurse-submodules --jobs=4
}

# Utility function, call in job tasks that require AAOS Unbundled to make sure that
# the unbundled repo has been set up (to some extent)
dc_aaos_ub_setup_check() {
    if [[ ! -d "$ORG_GRADLE_PROJECT_unbundledAAOSDir" ]]; then
        echo "Test configuration error, Unbundled AAOS project not found"
        exit 1
    fi
}

dc_kokoro_wrapup() {

    echo "
*****************************************
END OF TEST SCRIPT
ANY FAILURE SHOULD BE REPORTED ABOVE HERE

BEGINNING TEST WRAP UP
*****************************************
"
    # If you want the VM to stay up after test completion so that you can SSH into it
    # then uncomment the below:

    # if [ -n "$KOKORO_DEBUG_ACCESS_KEY" ]; then sleep 30m; fi

    # Prep the test results for Sponge parsing
    dc_organize_gradle_test_results

    # Delete everything that we don't want to upload, to avoid spending time
    # transferring things we don't want
    # Put this behind a var that should only be set in an actual Kokoro run
    if [ -n "$KOKORO_BUILD_ID" ]; then
        rm -rf "${KOKORO_ARTIFACTS_DIR}/git/"
        rm -rf "${KOKORO_ARTIFACTS_DIR}/build/"
    fi
}

###### Testing tasks

# Build and test the full DesignCompose Gradle project
dc_test_designcompose() {
    GRADLE_TEST_RESULT_DIRS+=("$DESIGNCOMPOSE_DIR")
    cd "$DESIGNCOMPOSE_DIR" || exit

    # Run linting first, then tests, then the full build (which runs all non-instrumented tests and checks).
    # Takes a bit longer, but
    # I think failures should probably happen most likely in linting, then testing, then build
    ./gradlew lint ktfmtCheck
    ./gradlew test
    # ./gradlew tabletAtdApi30Setup test "$GRADLE_TESTOPTIONS_OPTS"
    # The new kokoro Docker host can't handle multiple Gradle managed device tests running at once, so disable parallelization while running them.
    # ./gradlew -Porg.gradle.parallel=false tabletAtdApi30DebugAndroidTest "$GRADLE_TESTOPTIONS_OPTS"
    ./gradlew build
}

# Build, test and check formating all of our crates
dc_test_rust() {
    cd "$DESIGNCOMPOSE_DIR" || exit
    cargo-fmt --all --check
    cargo build --all-targets --all-features
    cargo test --all-targets --all-features
}

# Test our reference apps that depend on AAOS Unbundled
dc_test_aaos_reference_apps() {
    GRADLE_TEST_RESULT_DIRS+=("$DESIGNCOMPOSE_DIR")
    dc_aaos_ub_setup_check

    cd "$DESIGNCOMPOSE_DIR/reference-apps/aaos-unbundled" || exit

    ./gradlew lint ktfmtCheck
    ./gradlew test
    ./gradlew build
}

dc_test_validation_standalone() {
    TMP_VAL_DIR=$(mktemp -d)

    dc_build_designcompose_m2repo

    python3 "$DESIGNCOMPOSE_DIR/reference-apps/standalone-projects/export-standalone-project.py" \
        "$DESIGNCOMPOSE_DIR/reference-apps/standalone-projects/validation" \
        -o "$TMP_VAL_DIR/validation"

    # Add it to the list of directories to collect test reports from.
    GRADLE_TEST_RESULT_DIRS+=("$TMP_VAL_DIR")

    cd "$TMP_VAL_DIR" || exit
    # Rename the directory to separate the test results from the results of the normal test
    mv validation validation-standalone

    cd validation-standalone || exit
    ./gradlew build
    # ./gradlew tabletAtdApi30Setup build "$GRADLE_TESTOPTIONS_OPTS"
    # ./gradlew -Porg.gradle.parallel=false tabletAtdApi30DebugAndroidTest "$GRADLE_TESTOPTIONS_OPTS"

}

###### Build jobs - Builds release artifacts

# Build the Standalone MediaCompose project.
dc_build_mediacompose_standalone() {
    TMP_MC_DIR=$(mktemp -d)

    python3 "$DESIGNCOMPOSE_DIR/reference-apps/standalone-projects/export-standalone-project.py" \
        "$DESIGNCOMPOSE_DIR/reference-apps/aaos-unbundled/standalone-projects/mediacompose" \
        -o "$TMP_MC_DIR/mediacompose"

    tar czvf "${KOKORO_ARTIFACTS_DIR}/artifacts/mediacompose.tgz" -C "$TMP_MC_DIR" mediacompose/
}

# Build the DesignCompose Maven Repo
dc_build_designcompose_m2repo() {
    TMP_DCM2_DIR=$(mktemp -d)
    export ORG_GRADLE_PROJECT_DesignComposeMavenRepo="$TMP_DCM2_DIR"/designcompose_m2repo

    cd "$DESIGNCOMPOSE_DIR" || exit
    ./gradlew publishAllPublicationsToLocalDirRepository

    tar czvf "${KOKORO_ARTIFACTS_DIR}/artifacts/designcompose_m2repo.tgz" -C "$TMP_DCM2_DIR" designcompose_m2repo/
}

# Build the Tutorial App
dc_build_tutorial() {
    cd "$DESIGNCOMPOSE_DIR" || exit
    ./gradlew ref:tutorial:assembleRelease

    cp "$DESIGNCOMPOSE_DIR/reference-apps/tutorial/build/outputs/apk/release/tutorial-release.apk" "${KOKORO_ARTIFACTS_DIR}/artifacts/"
}

dc_build_figma_tool() {
    TOOL_NAME="$1"
    cd "$DESIGNCOMPOSE_DIR/support-figma/$TOOL_NAME"|| exit
    npm install
    npm run build
    tar czvf "${KOKORO_ARTIFACTS_DIR}/artifacts/figma-$TOOL_NAME.tgz" manifest.json ui.html code.js
}

dc_build_figma_tools() {
    dc_build_figma_tool extended-layout-plugin
    dc_build_figma_tool auto-content-preview-widget
}

###### Scripts for the individual jobs. Each of the below are called by different Kokoro Jobs

# Tests for the main project, does not use AAOS Unbundled
dc_main_project_job() {
    dc_test_designcompose
    dc_test_rust
    dc_test_validation_standalone
}

# Tests for our reference apps that use AAOS Unbundled
dc_aaos_apps_job() {
    # Just one discrete test for now, but keeping it in it's own function anyways
    dc_test_aaos_reference_apps
}

# The release job
dc_release_job() {
    export ORG_GRADLE_PROJECT_liveUpdateJNIReleaseBuild=true
    dc_build_figma_tools
    dc_build_designcompose_m2repo
    dc_build_mediacompose_standalone
    dc_build_tutorial
    cp "$DESIGNCOMPOSE_DIR/reference-apps/tutorial/DesignComposeTutorial.fig" "${KOKORO_ARTIFACTS_DIR}/artifacts"
    unset ORG_GRADLE_PROJECT_liveUpdateJNIReleaseBuild
}

# Test the results release job
dc_test_release_job() {
    dc_aaos_ub_setup_check

    # Run the release job to generate the artifacts first...
    dc_release_job

    # Set up a temp dir for the test
    TMP_MCTEST_DIR=$(mktemp -d)
    cd "$TMP_MCTEST_DIR" || exit

    # Add it to the list of directories to collect test reports from.
    GRADLE_TEST_RESULT_DIRS+=("$TMP_MCTEST_DIR")

    # Unpack the results of the release job, rename mediacompose directory to separate the test results from the results of the normal test
    tar xf "${KOKORO_ARTIFACTS_DIR}/artifacts/designcompose_m2repo.tgz"
    tar xf "${KOKORO_ARTIFACTS_DIR}/artifacts/mediacompose.tgz"
    mv mediacompose mediacompose-standalone

    cd mediacompose-standalone || exit
    ./gradlew build "$GRADLE_TESTOPTIONS_OPTS"
}
