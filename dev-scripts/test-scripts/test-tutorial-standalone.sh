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

GIT_ROOT=$(git rev-parse --show-toplevel)

export ORG_GRADLE_PROJECT_DesignComposeMavenRepo="$GIT_ROOT/build/test-tutorial-standalone/designcompose_m2repo"

cd "$GIT_ROOT" || exit
./dev-scripts/clean-all.sh
./gradlew publishAllPublicationsToLocalDirRepository

cd "$GIT_ROOT/reference-apps/tutorial" || exit
./gradlew --init-script ../local-design-compose-repo.init.gradle.kts check
./gradlew --init-script ../local-design-compose-repo.init.gradle.kts tasks | grep setFigmaToken

echo "Test passed"