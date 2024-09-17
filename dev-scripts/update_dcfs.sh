#!/bin/bash
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

set -euo pipefail
GIT_ROOT=$(git rev-parse --show-toplevel)

source "$GIT_ROOT/dev-scripts/common-functions.sh"

# Parse arguments for the following flags:
# -c: Run the clean-all.sh script before running any of the fetches
# -r: Record new screenshots for the Roborazzi tests
# -t: Run the test-all.sh script after running all of the fetches
CLEAN=false
RECORD_SCREENSHOTS=false
TEST=false

while getopts "cfrt" opt; do
    case "$opt" in
    c)
        CLEAN=true
        ;;
    r)
        RECORD_SCREENSHOTS=true
        ;;
    t)
        TEST=true
        ;;
    *)
        echo "Invalid option: $opt"
        exit 1
        ;;
    esac
done

OS_IS_LINUX=$(uname -s | grep -q Linux && echo true || echo false)

# If not on Linux then warn the user that the screenshots will not match those taken on a Linux system.
# Ask the user if they want to continue anyway.
if [[ "$RECORD_SCREENSHOTS" == "true" && $OS_IS_LINUX != "true" ]]; then
    echo "WARNING: You are running this script on a non-Linux system. The screenshots will not match those taken on a Linux system."
    echo "Do you want to continue? (y/n)"
    read -r answer
    if [[ "$answer" != "y" ]]; then
        echo "Exiting."
        exit 1
    fi
fi

ensure_figma_token_set

# If the clean flag was passed then run the clean-all.sh script
if [[ "$CLEAN" == "true" ]]; then
    echo "Running clean-all.sh"
    "$GIT_ROOT/dev-scripts/clean-all.sh"
fi

# Run this by itself to make sure that everything's working first
fetch_design_switcher

# Run the below fetches in parallel
fetch_helloworld &
fetch_tutorial &
fetch_layout_tests &
fetch_state_customizations &

"$GIT_ROOT/gradlew" fetchAndUpdateFigmaFiles

if [[ "$RECORD_SCREENSHOTS" == "true" ]]; then
    "$GIT_ROOT/gradlew" recordRoborazziDebug
fi

# If the test flag was passed then run the test-all.sh script
if [[ "$TEST" == "true" ]]; then
    echo "Running test-all.sh"
    # Skip emulator checks
    TEST_ARGS="-s "
    # Skip Roborazzi checks if not on Linux
    if [[ $OS_IS_LINUX != "true" ]]; then
        TEST_ARGS+="-r "
    fi
    # shellcheck disable=SC2086
    # We do want word splitting here, otherwise the args get passed as one string
    "$GIT_ROOT/dev-scripts/test-all.sh" $TEST_ARGS
fi
