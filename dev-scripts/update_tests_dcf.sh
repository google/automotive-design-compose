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

DOC_ID=pghyUUhlzJNoxxSK86ngiw

function warn() {
    echo -e "\033[1;33mWARNING: $*\033[0m" >&2 
}

warn "If you made changes to the queries of the DesignSwitcher, update the nodes in the script too!"

script_path=$(readlink -f "$0")
script_dir=$(dirname "$script_path")

cargo run --bin fetch --features=fetch -- \
--doc-id="$DOC_ID" \
--api-key="$FIGMA_ACCESS_TOKEN" \
--nodes="#MainFrame" \
--nodes="#state" \
--nodes="#AllSameName" \
--output="$script_dir"/../integration-tests/validation/src/main/assets/figma/VariantAnimationTestDoc_"$DOC_ID".dcf

DOC_ID=RW3lFurXCoVDeqY2Y7bf4v

cargo run --bin fetch --features=fetch -- \
--doc-id="$DOC_ID" \
--api-key="$FIGMA_ACCESS_TOKEN" \
--nodes="#MainFrame" \
--nodes="#RotationAnimationStage" \
--nodes="#RotationTestState" \
--nodes="#RotationOpacityStage" \
--nodes="#OpacityTestState" \
--nodes="#OneInstance" \
--output="$script_dir"/../integration-tests/validation/src/main/assets/figma/SmartAnimateTestDoc_"$DOC_ID".dcf

DOC_ID=POWyniB6moGRmhZTJyejwa

cargo run --bin fetch --features=fetch -- \
--doc-id="$DOC_ID" \
--api-key="$FIGMA_ACCESS_TOKEN" \
--nodes="#root" \
--output="$script_dir"/../integration-tests/validation/src/main/assets/figma/StateCustomizationsDoc_"$DOC_ID".dcf

DOC_ID=4P7zDdrQxj7FZsKJoIQcx1

cargo run --bin fetch --features=fetch -- \
--doc-id="$DOC_ID" \
--api-key="$FIGMA_ACCESS_TOKEN" \
--nodes="#MainFrame" \
--nodes="#bg1" \
--nodes="#bg2" \
--nodes="BorderType" \
--nodes="#SquareColor" \
--nodes="#comp1" \
--nodes="#comp2" \
--nodes="#comp3" \
--nodes="#border" \
--nodes="#shade" \
--nodes="#SquareBorder" \
--nodes="#SquareShadow" \
--output="$script_dir"/../integration-tests/validation/src/main/assets/figma/VariantPropertiesTestDoc_"$DOC_ID".dcf

DOC_ID=8Zg9viyjYTnyN29pbkR1CE

cargo run --bin fetch --features=fetch -- \
--doc-id="$DOC_ID" \
--api-key="$FIGMA_ACCESS_TOKEN" \
--nodes="Start Here" \
--output="$script_dir"/../integration-tests/validation/src/main/assets/figma/InteractionTestDoc_"$DOC_ID".dcf

DOC_ID=lZj6E9GtIQQE4HNLpzgETw

cargo run --bin fetch --features=fetch -- \
--doc-id="$DOC_ID" \
--api-key="$FIGMA_ACCESS_TOKEN" \
--nodes="#stage" \
--nodes="#stage-vector-progress" \
--output="$script_dir"/../integration-tests/validation/src/main/assets/figma/DialsGaugesTestDoc_"$DOC_ID".dcf

DOC_ID=pxVlixodJqZL95zo2RzTHl

cargo run --bin fetch --features=fetch -- \
--doc-id="$DOC_ID" \
--api-key="$FIGMA_ACCESS_TOKEN" \
--nodes="#MainFrame" \
--output="$script_dir"/../integration-tests/validation/src/main/assets/figma/HelloWorldDoc_"$DOC_ID".dcf

DOC_ID=gQeYHGCSaBE4zYSFpBrhre

cargo run --bin fetch --features=fetch -- \
--doc-id="$DOC_ID" \
--api-key="$FIGMA_ACCESS_TOKEN" \
--nodes="#Main" \
--nodes="prnd" \
--nodes="charging" \
--nodes="regen" \
--output="$script_dir"/../integration-tests/validation/src/main/assets/figma/VariantAsteriskTestDoc_"$DOC_ID".dcf

DOC_ID=mIYV4YsYYaMTsBMCVskA4N

cargo run --bin fetch --features=fetch -- \
--doc-id="$DOC_ID" \
--api-key="$FIGMA_ACCESS_TOKEN" \
--nodes="#MainFrame" \
--output="$script_dir"/../integration-tests/validation/src/main/assets/figma/StyledTextRunsDoc_"$DOC_ID".dcf

DOC_ID=uBExbEg4lcRa0xN2yaLTX8

cargo run --bin fetch --features=fetch -- \
--doc-id="$DOC_ID" \
--api-key="$FIGMA_ACCESS_TOKEN" \
--nodes="#root" \
--output="$script_dir"/../integration-tests/validation/src/main/assets/figma/HyperlinkValidationDoc_"$DOC_ID".dcf
