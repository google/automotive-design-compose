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
set -x

# This is a very rough test for bug #96, checking that the Figma Auth token stays active after
# switching from one user to another and back.

# This was tested with this Automotive emulator from the sdkmanager:
# system-images;android-32;android-automotive-playstore;x86_64

GIT_ROOT=$(git rev-parse --show-toplevel)
cd $GIT_ROOT

# Reset to a clean state
adb shell am switch-user 10
PASS_USER_LINE=$(adb shell pm list users | grep "Passenger" || true)
if [[ -n "$PASS_USER_LINE" ]]; then
  PASS_UID_TO_DELETE=$(echo "$PASS_USER_LINE" | sed -n 's/^.*UserInfo{\([[:digit:]]\+\).*$/\1/p' )
  adb shell pm remove-user "$PASS_UID_TO_DELETE"
fi

# Setup
./gradlew helloworld-app:assDebug
NEW_PASS_UID_LINE=$(adb shell pm create-user "Passenger")
NEW_PASS_UID=$(echo "$NEW_PASS_UID_LINE" | sed -n 's/^.*user id \([[:digit:]]\+\).*$/\1/p' )

adb uninstall reference-apps/helloworld/build/outputs/apk/debug/helloworld-app-debug.apk || true
adb install --user current reference-apps/helloworld/build/outputs/apk/debug/helloworld-app-debug.apk
adb shell am start -n "com.android.designcompose.testapp.helloworld/.MainActivity"
./gradlew helloworld-app:setFigmaTokenDebug
# Confirm that Live Update is fetching docs
read -p "Fetching working? (Y/N): " confirm && [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]] || exit 1

adb shell am switch-user "$NEW_PASS_UID"
sleep 5
adb install --user current reference-apps/helloworld/build/outputs/apk/debug/helloworld-app-debug.apk
adb shell am start -n "com.android.designcompose.testapp.helloworld/.MainActivity"
./gradlew helloworld-app:setFigmaTokenDebug
read -p "Fetching working? (Y/N): " confirm && [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]] || exit 1

adb shell am switch-user 10
sleep 5
adb shell am start -n "com.android.designcompose.testapp.helloworld/.MainActivity"
read -p "Fetching working? (Y/N): " confirm && [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]] || exit 1

adb shell am switch-user "$NEW_PASS_UID"
sleep 5
adb shell am start -n "com.android.designcompose.testapp.helloworld/.MainActivity"
read -p "Fetching working? (Y/N): " confirm && [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]] || exit 1
