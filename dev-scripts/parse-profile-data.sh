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
shopt -s globstar

# Adapted from AndroidX's profile parsing. Pases key metrics from the Gradle profile into a
# more easily parsed json.

GIT_ROOT=$(git rev-parse --show-toplevel)

cd "$GIT_ROOT" || exit

PROFILE_FILES="**/build/reports/profile/*.html"
for PROFILE in $PROFILE_FILES; do
  PROFILE_JSON="${PROFILE%.html}.json"
  ./dev-scripts/parse-profile-html.py --input-profile "$PROFILE" --output-summary "$PROFILE_JSON"
done
