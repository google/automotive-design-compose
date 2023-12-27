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

set -euo pipefail

usage() {
  cat <<END
This script should run all of the tests that CI will run. (It'll need to be kept up to date though)
Options:
  -f: Run a basic format of the Kotlin code before testing.
    Won't catch everything but shouldn't cause everything to rebuild either.
  -s: Skip emulator tests
  -u: Set Unbundled AAOS path
Pre-requisites:
    Have \$FIGMA_ACCESS_TOKEN set to your actual Figma token
    To run the full suite your system must be able to run emulator tests.
      Use the -s argument to skip emulator tests.
END
}

OPTIND=1 # Reset in case getopts has been used previously in the shell.
run_emulator_tests=1
RUN_FORMAT=false
while getopts "fsu:" opt; do
  case "$opt" in
  s)
    run_emulator_tests=0
    ;;
  u)
    ORG_GRADLE_PROJECT_unbundledAAOSDir=$(realpath "${OPTARG}")
    ;;
  f)
    RUN_FORMAT=true
    ;;
  *)
    usage
    exit 0
    ;;

  esac
done

GIT_ROOT=$(git rev-parse --show-toplevel)

if [[ -z "$ORG_GRADLE_PROJECT_unbundledAAOSDir" ]]; then
  echo "ORG_GRADLE_PROJECT_unbundledAAOSDir must be set"
  usage
  exit 1
fi

# Clumsy way to check if the variable is unset and load it if it is.
if [[ -z "${FIGMA_ACCESS_TOKEN:-''}" ]]; then
  if [[ -r ~/.config/figma_access_token ]]; then
    FIGMA_ACCESS_TOKEN=$(cat ~/.config/figma_access_token)
  else
    echo "FIGMA_ACCESS_TOKEN must be set"
    usage
    exit 1
  fi
fi

export GRADLE_OPTS=" -Dorg.gradle.project.designcompose.cargoPlugin.allowAbiOverride=true"
# Both are needed for the GMD Tests
export GRADLE_OPTS+=" -Dorg.gradle.project.designcompose.cargoPlugin.abiOverride=x86,x86_64"
export ORG_GRADLE_PROJECT_DesignComposeMavenRepo="$GIT_ROOT/build/test-all/designcompose_m2repo"

cd "$GIT_ROOT" || exit

TEST_RESULTS=""
export TEST_RESULTS

function run_cmd {
  TEST_NAME="$1"
  shift
  TEST_DIR="$1"
  shift
  TEST_CMD="$1"
  shift
  TEST_CMD_ARGS=$@

  cd "$GIT_ROOT/$TEST_DIR" || exit

  echo -e "\e[36mRUNNING $TEST_NAME"
  echo -e "------------\e[0m]"

  if "$TEST_CMD" $TEST_CMD_ARGS; then
    RESULT="\e[32m$TEST_NAME Passed\e[0m"
  else
    RESULT="\e[31m$TEST_NAME Failed\e[0m"
  fi

  echo -e "$RESULT"
  TEST_RESULTS="$TEST_RESULTS\n$RESULT"
}

function output_results {
  echo "Tests complete:"
  echo -e "$TEST_RESULTS"
}

trap output_results EXIT

if [[ $RUN_FORMAT == "true" ]]; then ./gradlew ktfmtFormat; fi

# The tests:
# Roughly ordered by importance and speed. Less imporant, but quick, checks can go early.

# if https://github.com/rhysd/actionlint is installed then run it. This is a low priority check so it's fine to not run it
if which actionlint >/dev/null; then
  run_cmd "GitHub Action Lint" . actionlint
else
  echo "actionlint not installed, skipping."
fi

run_cmd "KTFmt Check" . ./gradlew ktfmtCheck ktfmtCheckBuildScripts --no-configuration-cache
run_cmd "Cargo Fmt" . cargo-fmt --all --check

run_cmd "Cargo Test" . cargo test --all-targets --all-features

run_cmd "Main Project: AssembleDebug (quick build smoke check)" . \
  ./gradlew assembleDebug

run_cmd "Main Project: Verify Screenshots" . \
  ./gradlew verifyRoborazziDebug

run_cmd "Main Project: Publish" . \
  ./gradlew publishAllPublicationsToLocalDirRepository

run_cmd "Build HelloWorld" reference-apps/helloworld \
  ./gradlew --init-script ../local-design-compose-repo.init.gradle.kts build

run_cmd "Build Tutorial" reference-apps/tutorial \
  ./gradlew --init-script ../local-design-compose-repo.init.gradle.kts build

run_cmd "Build MediaCompose" reference-apps/aaos-unbundled \
  ./gradlew --init-script ../local-design-compose-repo.init.gradle.kts build

run_cmd "Cargo build" . cargo build --all-targets --all-features

run_cmd "Main Project: Build" . \
  ./gradlew assemble assembleAndroidTest assembleBenchmark

run_cmd "Build and Test Build Logic" build-logic ./gradlew build
run_cmd "Build and Test Plugins" plugins ./gradlew build

run_cmd "Build Unbundled projects" reference-apps/aaos-unbundled ./gradlew build

run_cmd "Build plugin" support-figma/extended-layout-plugin \
  npm ci run build

run_cmd "Build widget" support-figma/auto-content-preview-widget \
  npm ci run build

run_cmd "Main Project: Test Figma File Fetches" . \
  ./gradlew fetchFigmaFiles

if [[ $run_emulator_tests == 1 ]]; then
  run_cmd "Run AndroidInstrumentedTests on Gradle Managed Devices" . \
    ./gradlew gmdTestStandard -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
fi
