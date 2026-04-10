---
title: Getting Started
layout: page
nav_order: 1
has_children: true
has_toc: false
---


# Getting Started

The best introduction to Automotive Design for Compose is provided by following
the [Tutorial]({% link _docs/tutorial/index.md %}). Begin by
following the steps below.

## Install JDK 17 {#InstallJDK}

Automotive Design for Compose requires **JDK 17** or later for command-line
Gradle builds. Android Studio typically bundles a compatible JDK, but if you are
building from the command line you may need to install one separately.

### Verify your JDK version

```shell
java -version
```

You should see output indicating version **17** or later (e.g. `openjdk version
"17.0.x"`). If your version is older or Java is not installed, follow the
instructions below for your platform.

### macOS

```shell
brew install openjdk@17
```

After installing, ensure it's on your PATH by following the instructions printed
by Homebrew, or add the following to your shell configuration:

```shell
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
```

### Linux

Install OpenJDK 17 using your distribution's package manager:

```shell
# Debian/Ubuntu
sudo apt-get install openjdk-17-jdk

# Fedora
sudo dnf install java-17-openjdk-devel
```

### Windows

Download and install [Eclipse Temurin JDK 17](https://adoptium.net/){:.external}
from the Adoptium project.

### Set JAVA_HOME

If Gradle still picks up the wrong JDK, set `JAVA_HOME` explicitly:

```shell
export JAVA_HOME=$(/usr/libexec/java_home -v 17)  # macOS
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk     # Linux
```

## Check out the code {#GetSource}

To check out the code, which includes the Tutorial Android App, run the
following:

```shell
git clone https://github.com/google/automotive-design-compose.git
```

## Update Android Studio {#AndroidStudio}

The Tutorial Android App requires [Android Studio Flamingo](https://developer.android.com/studio) or later.

## Supported Android SDK levels {#SDKSupport}

DesignCompose supports the following Android SDK levels:

| Property | Value | Android Version |
|---|---|---|
| **minSdk** | 26 | Android 8.0 (Oreo) |
| **targetSdk** | 34 | Android 14 |
| **compileSdk** | 35 | Android 15 |

- **minSdk 26** is set for broad Android Automotive compatibility, allowing
  DesignCompose to run on a wide range of automotive head units. Note that primary
  development and testing targets API 34+.
- **compileSdk 35** means the SDK is compiled against the latest Android 15 APIs.
- **targetSdk 34** indicates compatibility testing has been validated up to
  Android 14 behavioral changes.

For complete details on Android API levels, see the
[Android API level documentation](https://developer.android.com/tools/releases/platforms){:.external}.
