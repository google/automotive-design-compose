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

DOC_ID=Ljph4e3sC0lHcynfXpoh9f

function warn() {
    echo -e "\033[1;33mWARNING: $*\033[0m" >&2 
}

warn "If you made changes to the queries of the DesignSwitcher, update the nodes in the script too!"

script_path=$(readlink -f "$0")
script_dir=$(dirname "$script_path")

cargo run --bin fetch --features=fetch -- \
--doc-id="$DOC_ID" \
--api-key="$FIGMA_ACCESS_TOKEN" \
--nodes="#SettingsView" \
--nodes="#FigmaDoc" \
--nodes="#Message" \
--nodes="#MessageFailed" \
--nodes="#LoadingSpinner" \
--nodes="#Checkbox" \
--nodes="#NodeNamesCheckbox" \
--nodes="#MiniMessagesCheckbox" \
--nodes="#ShowRecompositionCheckbox" \
--nodes="#UseLocalStringResCheckbox" \
--nodes="#DesignViewMain" \
--nodes="#LiveMode" \
--nodes="#TopStatusBar" \
--output="$script_dir"/../designcompose/src/main/assets/figma/DesignSwitcherDoc_"$DOC_ID".dcf
