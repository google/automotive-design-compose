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

FROM gcr.io/kokoro-containers/ubuntu/ubuntu2004-full:latest as init

SHELL ["/bin/bash", "-c"]


# This is the beginnings of a dockerfile to run CI. Not very useful yet, but it can be in the future.
# This can be used to test a run in Kokoro's DOCKER cluster.

# Build with: (from the root of the repo)
# docker build --pull --rm -f "kokoro/docker/Dockerfile" -t designcompose:latest .
# Run with:
# docker run --rm -it --device=/dev/kvm:/dev/kvm -v `pwd`/.git:/tmpfs/src/git/designcompose/.git:ro designcompose:latest

ENV KOKORO_JOB_CLUSTER="GCP_UBUNTU_DOCKER"
ENV KOKORO_ARTIFACTS_DIR="/tmpfs/src"
ENV LOCAL_KOKORO_TEST_ENV=true
ENV PATH="/root/.cargo/bin:${PATH}"

ADD . ${KOKORO_ARTIFACTS_DIR}/git/designcompose

WORKDIR ${KOKORO_ARTIFACTS_DIR}/git/designcompose
# Mark our scripts executable
RUN find kokoro/  -iname "*.sh" -exec chmod a+x {} \;
# Mark our project and anything inside as safe. Needed or else we can't use Rust dependencies that fetch git repos.
RUN git config --global --add safe.directory .
RUN source kokoro/gcp_ubuntu/kokoro-setup.sh

RUN cargo fetch

CMD kokoro/gcp_ubuntu/main-project-build.sh
