#!/bin/bash
# Copyright 2025 Google LLC
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


set -euo pipefail
IFS=$'\n\t'

VERSION="$1"
echo "Fetching release $VERSION"

VERSION_FAMILY=$(echo "$VERSION" | cut -d. -f1,2)".x"

pushd /google/data/rw/teams/designcompose/release_staging
mkdir -p "$VERSION_FAMILY"
cd "$VERSION_FAMILY"

wget "https://github.com/google/automotive-design-compose/releases/download/v$VERSION/designcompose_m2repo-v$VERSION.zip"

echo "Staging..."
temp_file=$(mktemp)
/google/bin/releases/android-devtools/gmaven/publisher/gmaven-publisher \
    stage --gfile \
    /x20/teams/designcompose/release_staging/"$VERSION_FAMILY"/designcompose_m2repo-v"$VERSION".zip |
    tee "$temp_file"

RELEASE_ID=$(grep "Release ID = " "$temp_file" | cut -d'=' -f2 | tr -d ' ')

echo "Release ID: $RELEASE_ID"
rm "$temp_file"

/google/bin/releases/android-devtools/gmaven/publisher/gmaven-publisher \
    publish "$RELEASE_ID"

echo "Release $VERSION published. Ready to approve by running:"
echo "\`/google/bin/releases/android-devtools/gmaven/publisher/gmaven-publisher approve $RELEASE_ID\`"

popd
