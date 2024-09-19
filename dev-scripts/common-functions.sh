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

# set -euo pipefail

GIT_ROOT=$(git rev-parse --show-toplevel)

function ensure_figma_token_set() {
    # If FIGMA_ACCESS_TOKEN isn't already set then try to load it from ~/.config/figma_access_token. If it's still unset then error out.
    if [[ -z "${FIGMA_ACCESS_TOKEN:-''}" ]]; then
        if [[ -r ~/.config/figma_access_token ]]; then
            FIGMA_ACCESS_TOKEN=$(cat ~/.config/figma_access_token)
        else
            echo "FIGMA_ACCESS_TOKEN must be set"
            exit 1
        fi
    fi
    export FIGMA_ACCESS_TOKEN
}

function warn() {
    echo -e "\033[1;33mWARNING: $*\033[0m" >&2
}

function fetch_design_switcher {
    DOC_ID=Ljph4e3sC0lHcynfXpoh9f

    warn "If you made changes to the queries of the DesignSwitcher, update the nodes in the script too!"

    cargo run --bin fetch --features=fetch -- \
        --doc-id="$DOC_ID" \
        --nodes="#SettingsView" \
        --nodes="#FigmaDoc" \
        --nodes="#Message" \
        --nodes="#MessageFailed" \
        --nodes="#LoadingSpinner" \
        --nodes="#Checkbox" \
        --nodes="#NodeNamesCheckbox" \
        --nodes="#MiniMessagesCheckbox" \
        --nodes="#ShowRecompositionCheckbox" \
        --nodes="#UseLocalResCheckbox" \
        --nodes="#DesignViewMain" \
        --nodes="#LiveMode" \
        --nodes="#TopStatusBar" \
        --output="$GIT_ROOT/designcompose/src/main/assets/figma/DesignSwitcherDoc_$DOC_ID.dcf"
}

function fetch_helloworld {
    DOC_ID=pxVlixodJqZL95zo2RzTHl
    cargo run --bin fetch --features=fetch -- \
        --doc-id="$DOC_ID" \
        --nodes="#MainFrame" \
        --output="$GIT_ROOT/reference-apps/helloworld/helloworld-app/src/main/assets/figma/HelloWorldDoc_$DOC_ID.dcf"

}

function fetch_tutorial {
    DOC_ID=3z4xExq0INrL9vxPhj9tl7
    WELCOME_BRANCH=BX9UyUa5lkuSP3dEnqBdJf

    echo "Update the dcf file for the reference app: tutorial..."

    cargo run --bin fetch --features=fetch -- \
        --doc-id="$WELCOME_BRANCH" \
        --nodes="#stage" \
        --nodes="#media/now-playing/play-state-button" \
        --nodes="#Track" \
        --nodes="#GridItem" \
        --nodes="#GridItem2" \
        --nodes="#bluebutton" \
        --nodes="#purplebutton" \
        --nodes="#greenbutton" \
        --nodes="#orangesubmitbutton" \
        --nodes="#redbutton" \
        --output="$GIT_ROOT/reference-apps/tutorial/app/src/main/assets/figma/TutorialDoc_$DOC_ID.dcf"
}

function fetch_layout_tests {
    cargo run --bin fetch --features=fetch -- \
        --doc-id=OGUIhtwHL3z8wWZqnxYM9P \
        --output="$GIT_ROOT/crates/figma_import/tests/layout-unit-tests.dcf" \
        --nodes='VerticalAutoLayout' \
        --nodes='ReplacementAutoLayout' \
        --nodes='ReplacementFixedLayout' \
        --nodes='VerticalFill' \
        --nodes='HorizontalFill' \
        --nodes='ConstraintsLayoutLR' \
        --nodes='ConstraintsLayoutTB' \
        --nodes='ConstraintsLayoutLRTB' \
        --nodes='ConstraintsLayoutCenter' \
        --nodes='ConstraintsLayoutWidget' \
        --nodes='VectorScale'
}

# This doc is difficult to fetch because the test that would fetch it can only run on older APIs
function fetch_state_customizations {
  DOC_ID=POWyniB6moGRmhZTJyejwa

  cargo run --bin fetch --features=fetch -- \
      --doc-id="$DOC_ID" \
      --nodes="#root" \
      --output="$GIT_ROOT/integration-tests/validation/src/main/assets/figma/StateCustomizationsDoc_$DOC_ID.dcf"
}