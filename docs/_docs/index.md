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

