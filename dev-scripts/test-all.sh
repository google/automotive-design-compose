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

set -e
usage() {
cat  <<END
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

OPTIND=1         # Reset in case getopts has been used previously in the shell.
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

if [[ -z "$FIGMA_ACCESS_TOKEN" ]]; then
  if [[ -r ~/.config/figma_access_token ]]; then
    FIGMA_ACCESS_TOKEN=$(cat ~/.config/figma_access_token)
  else
    echo "FIGMA_ACCESS_TOKEN must be set"
    usage
    exit 1
  fi
fi
GRADLE_OPTS="-Dorg.gradle.project.designcompose.cargoPlugin.allowAbiOverride=true "
GRADLE_OPTS+="-Dorg.gradle.project.designcompose.cargoPlugin.abiFilter=x86,x86_64"
export ORG_GRADLE_PROJECT_DesignComposeMavenRepo="$GIT_ROOT/build/test-all/designcompose_m2repo"

cd "$GIT_ROOT" || exit

if [[ $RUN_FORMAT == "true" ]]; then ./gradlew ktfmtFormat; fi

# if https://github.com/rhysd/actionlint is installed then run it. This is a low priority check so it's fine to not run it
if which actionlint > /dev/null; then actionlint; fi
cargo-fmt --all --check
./gradlew  ktfmtCheck ktfmtCheckBuildScripts --no-configuration-cache
cargo build --all-targets --all-features
cargo test --all-targets --all-features

cd "$GIT_ROOT/build-logic" || exit
./gradlew build
cd "$GIT_ROOT/plugins" || exit
./gradlew build

cd "$GIT_ROOT" || exit
./gradlew build publishAllPublicationsToLocalDirRepository verifyRoborazziDebug

if [[ $run_emulator_tests == 1 ]]; then
  ./gradlew gmdTestStandard -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
fi

cd "$GIT_ROOT/reference-apps/tutorial" || exit
./gradlew --init-script ../local-design-compose-repo.init.gradle.kts check

cd "$GIT_ROOT/support-figma/extended-layout-plugin" || exit
npm ci
npm run build

cd "$GIT_ROOT/support-figma/auto-content-preview-widget" || exit
npm ci
npm run build

cd "$GIT_ROOT/reference-apps/aaos-unbundled" || exit
./gradlew build
