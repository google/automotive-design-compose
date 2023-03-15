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

# This script tests the standalone projects using the DesignCompose pre-built libraries.
# It ensures that the libraries are rebuilt before each run

# The remainder of the command line is passed to the subprojects 

if [[ "$#" == 0 ]]; then
    GRADLE_CMDS="build"
else
    GRADLE_CMDS=("$@")
fi

GIT_ROOT=$(git rev-parse --show-toplevel)
TMP_DCM2_DIR=$(mktemp -d)
export ORG_GRADLE_PROJECT_DesignComposeMavenRepo="$TMP_DCM2_DIR"/designcompose_m2repo

"$GIT_ROOT"/gradlew publishAllPublicationsToLocalDirRepository

PROJECTS=('reference-apps/standalone-projects/validation' 'reference-apps/aaos-unbundled/standalone-projects/mediacompose')

for project in "${PROJECTS[@]}"; do
    echo "Running $project/gradlew -p $project ${GRADLE_CMDS[*]}"
    "$GIT_ROOT/$project"/gradlew -p "$GIT_ROOT/$project" "${GRADLE_CMDS[@]}"
done
