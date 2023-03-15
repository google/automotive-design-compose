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


# This script should essentially set up an environment that matches 
# the result of sourcing kokoro/gcp_ubuntu/kokoro-setup.sh"
GIT_ROOT=$(git rev-parse --show-toplevel)

export KOKORO_ARTIFACTS_DIR=$(mktemp -d -t kokoro_sim.XXXXX)
mkdir -p "$KOKORO_ARTIFACTS_DIR"/git

# Symlink in the DesignCompose repo
ln -s "$GIT_ROOT" "$KOKORO_ARTIFACTS_DIR"/git/designcompose

# Symlink in the AAOS Unbundled repo
# If the unbundledAAOSDir var is unset, then try to use a checkout that's next to the git repo
UB_DIR=${ORG_GRADLE_PROJECT_unbundledAAOSDir:-"$GIT_ROOT/../ub-automotive-master"}
ln -s "$UB_DIR" "$KOKORO_ARTIFACTS_DIR"/git/ub-automotive-master

source "${KOKORO_ARTIFACTS_DIR}/git/designcompose/kokoro/test-scripts.sh"
