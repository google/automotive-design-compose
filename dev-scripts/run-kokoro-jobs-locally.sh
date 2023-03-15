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


GIT_ROOT=$(git rev-parse --show-toplevel)

source "$GIT_ROOT"/dev-scripts/setup-kokoro-sim-dir.sh

# Add the word fast to use your existing build caches
if [[ ! "$1" == "fast" ]]; then
    echo "Running without cache! Restart the script with the 'fast' argument to run with cache."
    export GRADLE_USER_HOME="$KOKORO_ARTIFACTS_DIR/.gradle"
    cd "$DESIGNCOMPOSE_DIR" || exit
    ./gradlew clean
    cd "$DESIGNCOMPOSE_DIR/reference-apps/aaos-unbundled" || exit
    ./gradlew clean
fi

dc_main_project_job
dc_aaos_apps_job
dc_test_release_job

echo "Test run complete, everything appears to have passed. Artifacts dir:"
echo "$KOKORO_ARTIFACTS_DIR"