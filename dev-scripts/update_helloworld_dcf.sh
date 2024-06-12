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

DOC_ID=pxVlixodJqZL95zo2RzTHl

echo "Update the dcf file for the reference app: helloworld..."

function warn() {
    echo -e "\033[1;33mWARNING: $*\033[0m" >&2 
}
warn "Remember to update the nodes in the script to keep them in sync with the queries in HelloWorld_gen.kt!"

script_path=$(readlink -f "$0")
script_dir=$(dirname "$script_path")

cargo run --bin fetch --features=fetch -- \
--doc-id="$DOC_ID" \
--api-key="$FIGMA_ACCESS_TOKEN" \
--nodes="#MainFrame" \
--output="$script_dir"/../reference-apps/helloworld/helloworld-app/src/main/assets/figma/HelloWorldDoc_"$DOC_ID".dcf
