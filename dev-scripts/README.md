# Scripts for running the Kokoro test jobs on your local machine

These scripts allow you to run the same tests that Kokoro runs in presubmits, continuous and release jobs locally.

tl;dr: Run all tests by running kokoro/user-scripts/test-all.sh. You can let it re-use cached results by adding the "fast" argument.

## Prerequisites

### AAOS Unbundled Dir

Set the shell variable `$ORG_GRADLE_PROJECT_unbundledAAOSDir` to the root of the `ub-automotive-master` checkout. ([See this to checkout the repo](https://source.android.com/docs/devices/automotive/unbundled_apps/integration#check-out))

### Keystore secrets dir (optional)

The App IDs and API Key are being kept in go/keystore for Kokoro builds. Those secrets are written to files during CI startup and read in by the build scripts. If you'd like the test-all script to generate a functioning Tutorial App then you can set up your own keystore directory that can be found from the build scripts:

Create a local directory (not checked in) that will contain the secrets needed to build our release artifacts. Set the shell variable `$KOKORO_KEYSTORE_DIR` to the path to this directory. Currently the directory needs to contain two files:

    - `77278_tutorialAppID`, containing the Tutorial App's AppID (`TutorialAppClientID` from your gradle.properties)
    - `77278_LiveUpdateApiKey`, containing the Live Update system's API key(`DesignComposeApiKey` from your gradle.properties)

There should be no extraneous whitespace or newlines in the files, but the script should trim any that are there.

## Script descriptions

### test-all.sh

Run this to set up a rough approximation of Kokoro's environment and run all tests. Each of the job scripts will be called and run. Append the argument "fast" to run the tests using your existing Gradle Caches and build results.

### setup-kokoro-sim-dir.sh

Essentially a replacement for the `kokoro/gcp_ubuntu/kokoro-setup.sh` script. Creates a temporary directory and symlinks in the DesignCompose and AAOS Unbundled repos into it, emulating the directory layout of the Kokoro jobs.

You can source this to set up a test environment to experiment with, but it's best used as part of the test-all script
