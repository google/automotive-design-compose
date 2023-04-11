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

source "${KOKORO_ARTIFACTS_DIR}/git/designcompose/kokoro/test-scripts.sh"

export PYTHON_VERSION=3.9.5

# Move Android Homes to /tmpfs, where there's enough space to hold the emulators
export ANDROID_USER_HOME=/tmpfs/tmp/.android
export ANDROID_EMULATOR_HOME="$ANDROID_USER_HOME"
export ANDROID_AVD_HOME=/tmpfs/tmp/.android/avd

# Allow git pulls into the designcompose directory. Required
# by Rust compilation
git config --global --add safe.directory "$DESIGNCOMPOSE_DIR"

# Trick the Android emulator into thinking that we have Android Studio 3.0 installed,
# so that it'll allow us to make snapshots of AVDs. Otherwise Gradle Managed Devices doesn't work
mkdir -p ~/.AndroidStudio3.0

# Install rust
wget -q https://static.rust-lang.org/rustup/dist/x86_64-unknown-linux-gnu/rustup-init
chmod +x rustup-init
./rustup-init -q -y
source "$HOME/.cargo/env"

# Install the rust toolchains needed for the JNI library
"$DESIGNCOMPOSE_DIR"/install-rust-toolchains.sh

# Android SDK updates and licenses
yes | "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null
"${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --update > /dev/null
"${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --install "ndk;25.2.9519653" > /dev/null

# repo gets confused by pyenv, so set a default version.
if command -v pyenv >/dev/null; then
  echo "Selecting Python $PYTHON_VERSION"
  pyenv global "$PYTHON_VERSION"
fi
