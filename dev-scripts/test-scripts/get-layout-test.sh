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

# This script should fetch the layout test Figma file with all the nodes that are needed for testing.
# Pre-requisites:
#    Have \$FIGMA_ACCESS_TOKEN set to your actual Figma token

cargo run --bin fetch --features=fetch -- \
--doc-id=OGUIhtwHL3z8wWZqnxYM9P \
--api-key=$FIGMA_ACCESS_TOKEN \
--output=crates/figma_import/tests/layout-unit-tests.dcf \
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
