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


# If the job was started with a debug SSH key (go/kokoro-ssh-vm) then report our IP address
# so we can find the VM.
if [ -n "$KOKORO_DEBUG_ACCESS_KEY" ]; then
    echo "$KOKORO_DEBUG_ACCESS_KEY" >> ~/.ssh/authorized_keys

    external_ip=$(curl -s -H "Metadata-Flavor: Google" http://metadata/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip)
    echo "INSTANCE_EXTERNAL_IP=${external_ip}"
    
    echo "Don't forget to add a sleep so that the VM stays up and waiting for you to log in"
fi

source "${KOKORO_ARTIFACTS_DIR}/git/designcompose/kokoro/test-scripts.sh"

export PYTHON_VERSION=3.9.5

# Move Android Homes to /tmpfs, where there's enough space to hold the emulators
export ANDROID_USER_HOME=/tmpfs/tmp/.android
export ANDROID_EMULATOR_HOME="$ANDROID_USER_HOME"
export ANDROID_AVD_HOME=/tmpfs/tmp/.android/avd

# Trick the Android emulator into thinking that we have Android Studio 3.0 installed, 
# so that it'll allow us to make snapshots of AVDs. Otherwise Gradle Managed Devices doesn't work
mkdir ~/.AndroidStudio3.0

# Before updating the Java version, update the Android SDK command line tools
yes | /opt/android-sdk/current/tools/bin/sdkmanager --licenses > /dev/null
/opt/android-sdk/current/tools/bin/sdkmanager "cmdline-tools;latest"> /dev/null

# Set Java version to 11
sudo apt-get install -y openjdk-11-jdk
sudo update-java-alternatives --set java-1.11.0-openjdk-amd64
export JAVA_HOME=/usr/lib/jvm/java-1.11.0-openjdk-amd64
java -version

# On Kokoro instances /dev/kvm is owned by root:root.
# Set the permissions so that KVM is owned by the `kvm` group
readonly KVM_GID="$(getent group kvm | cut -d: -f3)"
sudo chown "$USER:$KVM_GID" /dev/kvm

# Install rust
sudo apt-get install -y curl
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
export PATH="${HOME}/.cargo/bin:${PATH}"

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
