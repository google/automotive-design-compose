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
Pre-requisites: 
    Must be run on a system that can run emulators
    Have \$ORG_GRADLE_PROJECT_unbundledAAOSDir set to your AAOS Unbundled repo
    Have \$FIGMA_ACCESS_TOKEN set to your actual Figma token
END
exit 1
}

GIT_ROOT=$(git rev-parse --show-toplevel)

if [[ -z "$ORG_GRADLE_PROJECT_unbundledAAOSDir" ]]; then
  echo "ORG_GRADLE_PROJECT_unbundledAAOSDir must be set"
  usage
fi
if [[ -z "$FIGMA_ACCESS_TOKEN" ]]; then
  echo "FIGMA_ACCESS_TOKEN must be set"
  usage
fi

export ORG_GRADLE_PROJECT_DesignComposeMavenRepo="$GIT_ROOT/build/test-all/designcompose_m2repo"

cd "$GIT_ROOT" || exit
# if https://github.com/rhysd/actionlint is installed then run it. This is a low priority check so it's fine to not run it
if which actionlint > /dev/null; then actionlint; fi
cargo-fmt --all --check
./gradlew  ktfmtCheck ktfmtCheckBuildScripts --no-configuration-cache
cargo build --all-targets --all-features
cargo test --all-targets --all-features
./gradlew check publishAllPublicationsToLocalDirRepository tabletAtdApi30Check -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect

cd "$GIT_ROOT/reference-apps/tutorial"
./gradlew --init-script ../local-design-compose-repo.init.gradle.kts check tabletAtdApi30Check

cd "$GIT_ROOT/support-figma/extended-layout-plugin"
npm ci
npm run build

cd "$GIT_ROOT/support-figma/auto-content-preview-widget"
npm ci
npm run build

cd "$GIT_ROOT/reference-apps/aaos-unbundled"
./gradlew check