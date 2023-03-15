TODO: We need a new Readme!

# Enabling LiveUpdate

To enable LiveUpdate in an app, add this line to the MainActivity of the app:

```
DesignSettings.enableLiveUpdates(this)
```

See `reference-apps/helloworld/src/main/java/com/android/designcompose/testapp/helloworld/MainActivity.kt` for an example.

## Setting the Figma API Key

The Figma API Key must be set using an explicit intent directed to the app. The intent's `action` is `setApiKey`, and it requires extra string data, key `ApiKey` and value `<your Figma Token>`. It may be easiest to store your API Key locally in a shell variable named `$FIGMA_AUTH_TOKEN`. Both the app's main activity and a service that is included in DesignCompose can receive the intent.  To start the service, run:

```
adb shell am startservice -n "<YOUR_APP_ID>/com.android.designcompose.ApiKeyService" -a setApiKey -e ApiKey $FIGMA_AUTH_TOKEN
```

Alternatively, launch the main activity with the ApiKey intent:

```
adb shell am start -n "<YOUR_APP_ID>/.<YOUR_MAIN_ACTIVITY>" -a SetApiKey -e ApiKey $FIGMA_AUTH_TOKEN
```

### Working examples

Launch the helloworld app, setting the key:

```
adb shell am start -n "com.android.designcompose.testapp.helloworld/.MainActivity" -a SetApiKey -e ApiKey $FIGMA_AUTH_TOKEN
````

Set the key for Validation via the service, without necessarily launching the app:

```
adb shell am startservice -n "com.android.designcompose.testapp.validation/com.android.designcompose.ApiKeyService" -a setApiKey -e ApiKey $FIGMA_AUTH_TOKEN
```

# Developer Software Requirements

## Rust

Rust is required to compile DesignCompose. A relatively recent version is required. Install at [rustup.rs](https://rustup.rs/).

The Rust compiler, cargo tool, etc. must be on your path. Typically you'll want to add `if [ -f "$HOME/.cargo/env" ]; then . "$HOME/.cargo/env"; fi` to your `$HOME/.profile`. You can also run that in your current terminal window.

**Android Studio** will not pick up on a new Rust install without a restart, and if you launch it via GUI/Start menu/Dock/whatever, it will likely not see the new PATH entry. You can launch Android Studio from a command line that has the updated path. Or, if all else fails, logging out of your desktop and back in (or restarting thhe computer) will definitely work.

### Rust Toolchains

The DesignCompose library includes a Rust JNI library, which requires Rust build target toolchains. Install them by running the following;

```
./install-rust-toolchains.sh
```

## Android NDK

NDK version `25.2.9519653` is required. It can be installed via [Android Studio's SDK Manager]](https://developer.android.com/studio/projects/install-ndk#specific-version) or on the command line using the `sdkmanager` binary.

```
"${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --install "ndk;25.2.9519653"
```

## Python

Version 3.9.5 or later is required for the `export-standalone-project.py` script. In addition, you must have the `python3` binary in your path, with the filename `python`. You can run `which python` to confirm it's there and `python --version` to confirm the version you have installed. If `which python` does not return a result, you can often fix this by running `apt install python-is-python3` or installing a similar package for your OS.

## addlicense

[addlicense](https://github.com/google/addlicense) is used to keep our license headers up to date. It can be installed with:

```
go install github.com/google/addlicense@latest
```

It will install into the $GOPATH directory, which if unset, defaults to $HOME/go/bin.
