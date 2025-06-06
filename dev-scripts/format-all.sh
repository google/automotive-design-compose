#!/bin/bash
#
# Copyright 2024 Google LLC
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
#

set -e

# Runs all formatter jobs that we have, getting the repo roughly back to a clean state

GIT_ROOT=$(git rev-parse --show-toplevel)

# For addlicense: Set GOPATH to the default location if not set
GOPATH="${GOPATH:-$HOME/go/bin}"
if ! which "$GOPATH"/addlicense; then
    echo "addlicense not found. Install with 'go install github.com/google/addlicense@latest'"
    exit 1
fi

# Run in a subshell so that we don't end up in the wrong directory if the script fails
(
    cd "$GIT_ROOT" || exit
    "$GOPATH"/addlicense \
        -ignore "**/.idea/**" \
        -ignore "**/.vscode/**" \
        -ignore "**/.gradle/**" \
        -ignore "**/build/**" \
        -ignore "**/target/**" \
        -ignore "**/gen_strings.xml" \
        -c "Google LLC" \
        -l apache \
        "$GIT_ROOT"

    cargo fix --allow-dirty --allow-staged --workspace --all-features
    cargo fmt

    # Format each Gradle project individually, so that Gradle doesn't complain that build files
    # changed while we were running the format
    cd "$GIT_ROOT/build-logic" || exit
    ./gradlew spotlessApply --no-configuration-cache
    cd "$GIT_ROOT/plugins" || exit
    ./gradlew spotlessApply --no-configuration-cache
    cd "$GIT_ROOT" || exit
    ./gradlew spotlessApply --no-configuration-cache
)

if which protolint >/dev/null; then
    echo "Running protolint"
    protolint -fix proto/
else
    echo "protolint not found. Skipping protobuf formatting"
fi
