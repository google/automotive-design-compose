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

echo "Update the dcf file for the reference app: helloworld..."

warn "Remember to update the nodes in the script to keep them in sync with the queries in HelloWorld_gen.kt!"

fetch_helloworld
